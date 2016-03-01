package ivengine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import de.matthiasmann.twl.utils.PNGDecoder;
import de.matthiasmann.twl.utils.PNGDecoder.Format;

public class Util 
{
	public static String readFile(String path)
	{
			InputStream is=Util.class.getResourceAsStream(path);
			java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
			return s.hasNext() ? s.next() : "";
	}
	public static int loadPNGTexture(String filename, int textureUnit) {
		ByteBuffer buf = null;
		int tWidth = 0;
		int tHeight = 0;
		
		try {
			// Open the PNG file as an InputStream
			InputStream in = new FileInputStream(filename);
			// Link the PNG decoder to this stream
			PNGDecoder decoder = new PNGDecoder(in);
			
			// Get the width and height of the texture
			tWidth = decoder.getWidth();
			tHeight = decoder.getHeight();
			
			// Decode the PNG file in a ByteBuffer
			buf = ByteBuffer.allocateDirect(
					4 * decoder.getWidth() * decoder.getHeight());
			decoder.decode(buf, decoder.getWidth() * 4, Format.RGBA);
			buf.flip();
			
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		// Create a new texture object in memory and bind it
		int texId = GL11.glGenTextures();
		GL13.glActiveTexture(textureUnit);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
		
		// All RGB bytes are aligned to each other and each component is 1 byte
		GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
		
		// Upload the texture data and generate mip maps (for scaling)
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, tWidth, tHeight, 0, 
				GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf);
		//GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
		
		// Setup the ST coordinate system
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
		
		// Setup what to do when the texture has to be scaled
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, 
				GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, 
				GL11.GL_NEAREST);
		
		return texId;
	}
	
	public static int loadTextureAtlasIntoTextureArray(File[] filenames,int magfilter,int minfilter,boolean mipmap)
	{
		int tex = GL11.glGenTextures();
		GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, tex);
		GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST_MIPMAP_LINEAR);
		GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
		GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
		
		ByteBuffer buf=null;
		PNGDecoder decoder=null;
		try {
			InputStream in = new FileInputStream(filenames[0]);
			decoder = new PNGDecoder(in);

			buf = BufferUtils.createByteBuffer(4 * decoder.getWidth() * decoder.getHeight());
			
			decoder.decode(buf, decoder.getWidth() * 4, Format.RGBA);
			buf.flip();
			
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		int tileWidth=decoder.getWidth(); System.out.println(tileWidth);
		int tileHeight=decoder.getHeight(); System.out.println(tileHeight);
		
		GL12.glTexImage3D(GL30.GL_TEXTURE_2D_ARRAY, 0, GL11.GL_RGBA, tileWidth, tileHeight, filenames.length, 0, GL11.GL_RGBA,
				GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);

		for(int i=0;i<filenames.length;i++)
		{
			GL12.glTexSubImage3D(GL30.GL_TEXTURE_2D_ARRAY,0, /*tileWidth*x*/0, /*tileHeight*y*/0, i,  tileWidth, tileHeight, 1,GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf);
			
			System.out.println("ERRTEXT"+i+" "+GL11.glGetError());
			
			buf.rewind();
			if(i<filenames.length-1) loadTexture(filenames[i+1],buf);
		}
		System.out.println("ERRTEXT"+GL11.glGetError());
		if(mipmap) GL30.glGenerateMipmap(GL30.GL_TEXTURE_2D_ARRAY);
		GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0);
		
		System.out.println("ERRTEXT"+GL11.glGetError());
		
		return tex;
	}
	
	private static ByteBuffer loadTexture(File filename,ByteBuffer buf)
	{
		try {
			InputStream in = new FileInputStream(filename);
			PNGDecoder decoder = new PNGDecoder(in);

			decoder.decode(buf, decoder.getWidth() * 4, Format.RGBA);
			System.out.println(decoder.hasAlpha());
			buf.flip();
			
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		return buf;
	}
}

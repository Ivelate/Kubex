package ivengine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.EXTTextureFilterAnisotropic;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

import de.matthiasmann.twl.utils.PNGDecoder;
import de.matthiasmann.twl.utils.PNGDecoder.Format;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Some useful methods, like reading files into strings, or loading textures
 */
public class Util 
{
	/**
	 * Loads all text of the file with path <path> into a String, and returns it
	 */
	public static String readFile(String path)
	{
			InputStream is=Util.class.getResourceAsStream(path);
			java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
			return s.hasNext() ? s.next() : "";
	}
	
	/**
	 * Loads a texture from a URL <filename> into a texture unit <textureUnit>
	 */
	public static int loadPNGTexture(URL filename, int textureUnit) {
		ByteBuffer buf = null;
		int tWidth = 0;
		int tHeight = 0;
		
		try {
			// Open the PNG file as an InputStream
			InputStream in = filename.openStream();
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
	
	/**
	 * Stores a variable amount of textures defined by URLS <filenames> into a texture array. Uses a magfilter <magfilter>, a minfilter <minfilter>.
	 * If <mipmap> is true, mipmaps will be activated.
	 * If <anisotropic> is true, anisotropic filtering will be activated, if supported.
	 */
	public static int loadTextureAtlasIntoTextureArray(URL[] filenames,int magfilter,int minfilter,boolean mipmap,boolean anisotropic)
	{
		int tex = GL11.glGenTextures();
		GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, tex);
		GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_MAG_FILTER, magfilter);
		GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_MIN_FILTER, minfilter);
		GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
		GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
		
		ByteBuffer buf=null;
		PNGDecoder decoder=null;
		try {
			InputStream in = filenames[0].openStream();
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
						
			buf.rewind();
			if(i<filenames.length-1) loadTexture(filenames[i+1],buf);
		}
		if(mipmap) GL30.glGenerateMipmap(GL30.GL_TEXTURE_2D_ARRAY);
		if(anisotropic)
		{
			if(GLContext.getCapabilities().GL_EXT_texture_filter_anisotropic) {
				float maxanis=GL11.glGetFloat(EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
				System.out.println("Anisotropic filtering activated with a resolution of "+maxanis);
				System.out.println(GL11.glGetError());
				GL11.glTexParameterf(GL30.GL_TEXTURE_2D_ARRAY, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, maxanis);
				System.out.println(GL11.glGetError());
			}
			else{
				System.err.println("WARNING - Anisotropic filtering not supported by this graphics card. Setting it as disabled");
			}
		}
		GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0);
				
		return tex;
	}
	
	private static ByteBuffer loadTexture(URL filename,ByteBuffer buf)
	{
		try {
			InputStream in = filename.openStream();
			PNGDecoder decoder = new PNGDecoder(in);

			decoder.decode(buf, decoder.getWidth() * 4, Format.RGBA);
			buf.flip();
			
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		return buf;
	}
}

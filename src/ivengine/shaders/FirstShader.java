package ivengine.shaders;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetAttribLocation;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

/**
 * 
 * @author Ivelate
 * @since 7-12-2013 17:42
 * 
 * Test implementation of SimpleShaderProgram
 *
 */
public class FirstShader extends SimpleShaderProgram 
{
	private int posAttrib;
	private int colAttrib;
	private int texAttrib;
	
	public FirstShader()
	{
		this(false);
	}
	public FirstShader(boolean verbose)
	{
		super("/shaders/vertexShader.vshader","/shaders/fragmentShader.fshader",verbose);
		
		this.posAttrib = glGetAttribLocation(this.getID(), "position");
		this.colAttrib = glGetAttribLocation(this.getID(), "color");
		this.texAttrib = glGetAttribLocation(this.getID(), "texcoord");
		
	}
	@Override
	public void setupAttributes() {
		glVertexAttribPointer(posAttrib,3,GL_FLOAT,false,32,0);
		glEnableVertexAttribArray(posAttrib);
		glVertexAttribPointer(colAttrib,3,GL_FLOAT,false,32,12);
		glEnableVertexAttribArray(colAttrib);
		glVertexAttribPointer(texAttrib,2,GL_FLOAT,false,32,24);
		glEnableVertexAttribArray(texAttrib);
	}
	@Override
	protected void dispose() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public int getSize() {
		return 8;
	}

}

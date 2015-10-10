package monecruft.shaders;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetAttribLocation;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import ivengine.shaders.SimpleShaderProgram;

public class BasicColorShaderProgram extends SimpleShaderProgram{
	
	private int locAttrib;
	private int colorUniform;
	
	public BasicColorShaderProgram()
	{
		this(false);
	}
	public BasicColorShaderProgram(boolean verbose)
	{
		super("/shaders/basicColorShader.vshader","/shaders/basicColorShader.fshader",verbose);
		
		this.locAttrib = glGetAttribLocation(this.getID(), "location");
		this.colorUniform = glGetUniformLocation(this.getID(),"color");
	}
	public int getColorUniformLocation()
	{
		return this.colorUniform;
	}
	@Override
	public void setupAttributes() {
		glVertexAttribPointer(locAttrib,2,GL_FLOAT,false,2*4,0);
		glEnableVertexAttribArray(locAttrib);
	}
	@Override
	protected void dispose() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public int getSize() {
		return 2;
	}
}

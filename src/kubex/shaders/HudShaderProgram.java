package kubex.shaders;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetAttribLocation;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import ivengine.shaders.SimpleShaderProgram;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Hud shader program. Used to render a gray dot
 */
public class HudShaderProgram extends SimpleShaderProgram{
	
	private int locAttrib;
	private int colorAttrib;
	private int hudUniform;
	
	public HudShaderProgram()
	{
		this(false);
	}
	public HudShaderProgram(boolean verbose)
	{
		super("/shaders/hudShader.vshader","/shaders/hudShader.fshader",verbose);
		
		this.locAttrib = glGetAttribLocation(this.getID(), "location");
		this.colorAttrib = glGetAttribLocation(this.getID(), "color");
		this.hudUniform= glGetUniformLocation(this.getID(),"hud");
	}
	public int getHudTextureLocation()
	{
		return this.hudUniform;
	}
	@Override
	public void setupAttributes() {
		glVertexAttribPointer(locAttrib,2,GL_FLOAT,false,6*4,0);
		glEnableVertexAttribArray(locAttrib);
		glVertexAttribPointer(colorAttrib,4,GL_FLOAT,false,6*4,2*4);
		glEnableVertexAttribArray(colorAttrib);
	}
	@Override
	protected void dispose() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public int getSize() {
		return 6;
	}
}

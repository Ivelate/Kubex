package kubex.shaders;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetAttribLocation;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Depth peeling shader
 */
public class DepthPeelingShaderProgram extends VoxelShaderProgram{
	
	public DepthPeelingShaderProgram()
	{
		this(false);
	}
	public DepthPeelingShaderProgram(boolean verbose)
	{
		this("/shaders/depthPeelingShader.vshader","/shaders/depthPeelingShader.fshader",verbose);
	}
	public DepthPeelingShaderProgram(String vbufroute,String fbufroute,boolean verbose)
	{
		super(vbufroute,fbufroute,verbose);
	}
	@Override
	public void setupAttributes() 
	{
		int loc=glGetAttribLocation(this.getID(), "location");
		glVertexAttribPointer(loc,3,GL_FLOAT,false,getSize()*4,0);
		glEnableVertexAttribArray(loc);
		int normal=glGetAttribLocation(this.getID(), "normal");
		glVertexAttribPointer(normal,3,GL_FLOAT,false,getSize()*4,3*4);
		glEnableVertexAttribArray(normal);
	}
	@Override
	public boolean isParticipatingMedia() {
		return false;
	}
	@Override
	public boolean supportShadows() {
		return false;
	}
	@Override
	public boolean supportLighting() {
		return false;
	}
}

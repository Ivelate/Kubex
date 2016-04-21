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
 * Simple shader which only renders the depth of the image. Used when rendering shadow maps.
 */
public class DepthVoxelShaderProgram extends VoxelShaderProgram{
	
	public DepthVoxelShaderProgram()
	{
		this(false);
	}
	public DepthVoxelShaderProgram(boolean verbose)
	{
		this("/shaders/basicShader.vshader","/shaders/depthShader.fshader",verbose);
	}
	public DepthVoxelShaderProgram(String vbufroute,String fbufroute,boolean verbose)
	{
		super(vbufroute,fbufroute,verbose);
	}
	@Override
	public void setupAttributes() 
	{
		int loc=glGetAttribLocation(this.getID(), "location");
		glVertexAttribPointer(loc,3,GL_FLOAT,false,getSize()*4,0);
		
		int propAttrib = glGetAttribLocation(this.getID(), "properties");
		glVertexAttribPointer(propAttrib,1,GL_FLOAT,false,getSize()*4,3*4);
		
		
		glEnableVertexAttribArray(propAttrib);
		glEnableVertexAttribArray(loc);
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

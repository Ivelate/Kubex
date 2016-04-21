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
 * Default first pass shader which renders the color of the scene, its brightness, its normal and its depth to textures, to be later used by second and third pass deferred render shaders.
 * Only renders the basic color of the scene (From the cubes textures), without lighting or shadows of any sort.
 */
public class TerrainVoxelShaderProgram extends VoxelShaderProgram{

	
	public TerrainVoxelShaderProgram()
	{
		this("/shaders/voxelShader.vshader","/shaders/voxelShader.fshader",false);
	}
	public TerrainVoxelShaderProgram(boolean verbose)
	{
		this("/shaders/voxelShader.vshader","/shaders/voxelShader.fshader",verbose);
	}
	public TerrainVoxelShaderProgram(String vbufroute,String fbufroute,boolean verbose)
	{
		super(vbufroute,fbufroute,verbose);
	}
	
	@Override
	public void setupAttributes() {
		int locAttrib = glGetAttribLocation(this.getID(), "location");
		int propAttrib = glGetAttribLocation(this.getID(), "properties");
		int brightnessAttrib = glGetAttribLocation(this.getID(), "brightness");
		
		glVertexAttribPointer(locAttrib,3,GL_FLOAT,false,getSize()*4,0);
		glEnableVertexAttribArray(locAttrib);
		glVertexAttribPointer(propAttrib,1,GL_FLOAT,false,getSize()*4,3*4);
		glEnableVertexAttribArray(propAttrib);
		glVertexAttribPointer(brightnessAttrib,2,GL_FLOAT,false,getSize()*4,4*4);
		glEnableVertexAttribArray(brightnessAttrib);
	}

	@Override
	protected void dispose() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public boolean isParticipatingMedia() {
		return false;
	}
	@Override
	public boolean supportShadows() {
		return true;
	}
	@Override
	public boolean supportLighting() {
		return true;
	}
}

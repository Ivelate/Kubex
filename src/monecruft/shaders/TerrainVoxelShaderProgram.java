package monecruft.shaders;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetAttribLocation;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import ivengine.shaders.SimpleShaderProgram;

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

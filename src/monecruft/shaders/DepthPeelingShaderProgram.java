package monecruft.shaders;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetAttribLocation;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

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

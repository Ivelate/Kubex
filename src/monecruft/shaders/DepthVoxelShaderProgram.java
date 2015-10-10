package monecruft.shaders;

public class DepthVoxelShaderProgram extends VoxelShaderProgram{
	
	public DepthVoxelShaderProgram()
	{
		this(false);
	}
	public DepthVoxelShaderProgram(boolean verbose)
	{
		this("/shaders/voxelShader.vshader","/shaders/depthShader.fshader",verbose);
	}
	public DepthVoxelShaderProgram(String vbufroute,String fbufroute,boolean verbose)
	{
		super(vbufroute,fbufroute,verbose);
	}
}

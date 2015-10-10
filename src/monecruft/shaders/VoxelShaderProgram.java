package monecruft.shaders;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetAttribLocation;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import ivengine.shaders.SimpleShaderProgram;

public class VoxelShaderProgram extends SimpleShaderProgram{

	private int modelMatrixLoc;
	private int vpMatrixLoc;
	
	private int locAttrib;
	private int propAttrib;
	private int brightnessAttrib;
	private int tilesUniform;
	private int daylightAmountUniform;
	
	public VoxelShaderProgram()
	{
		this("/shaders/voxelShader.vshader","/shaders/voxelShader.fshader",false);
	}
	public VoxelShaderProgram(boolean verbose)
	{
		this("/shaders/voxelShader.vshader","/shaders/voxelShader.fshader",verbose);
	}
	public VoxelShaderProgram(String vbufroute,String fbufroute,boolean verbose)
	{
		super(vbufroute,fbufroute,verbose);
		
		this.locAttrib = glGetAttribLocation(this.getID(), "location");
		this.propAttrib = glGetAttribLocation(this.getID(), "properties");
		this.brightnessAttrib = glGetAttribLocation(this.getID(), "brightness");
		this.tilesUniform= glGetUniformLocation(this.getID(),"tiles");
		
		this.daylightAmountUniform=glGetUniformLocation(this.getID(),"daylightAmount");
		
		this.modelMatrixLoc=glGetUniformLocation(this.getID(),"modelMatrix");
		this.vpMatrixLoc=glGetUniformLocation(this.getID(),"vpMatrix");
	}
	public int getModelMatrixLoc() {
		return modelMatrixLoc;
	}
	public int getViewProjectionMatrixLoc() {
		return vpMatrixLoc;
	}
	public int getTilesTextureLocation()
	{
		return this.tilesUniform;
	}
	public int getDaylightAmountLocation()
	{
		return this.daylightAmountUniform;
	}
	@Override
	public void setupAttributes() {
		glVertexAttribPointer(locAttrib,3,GL_FLOAT,false,getSize()*4,0);
		glEnableVertexAttribArray(locAttrib);
		glVertexAttribPointer(propAttrib,1,GL_FLOAT,false,getSize()*4,3*4);
		glEnableVertexAttribArray(propAttrib);
		glVertexAttribPointer(brightnessAttrib,2,GL_FLOAT,false,getSize()*4,4*4);
		glEnableVertexAttribArray(brightnessAttrib);
	}
	@Override
	public int getSize()
	{
		return 6;
	}
	@Override
	protected void dispose() {
		// TODO Auto-generated method stub
		
	}
}

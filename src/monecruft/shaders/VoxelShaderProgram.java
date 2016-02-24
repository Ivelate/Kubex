package monecruft.shaders;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetAttribLocation;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import ivengine.shaders.SimpleShaderProgram;

public abstract class VoxelShaderProgram extends SimpleShaderProgram{

	private int modelMatrixLoc;
	private int vpMatrixLoc;
	
	public VoxelShaderProgram(String vbufroute,String fbufroute,boolean verbose)
	{
		super(vbufroute,fbufroute,verbose);
		
		this.modelMatrixLoc=glGetUniformLocation(this.getID(),"modelMatrix");
		this.vpMatrixLoc=glGetUniformLocation(this.getID(),"vpMatrix");
	}
	public final int getModelMatrixLoc() {
		return modelMatrixLoc;
	}
	public final int getViewProjectionMatrixLoc() {
		return vpMatrixLoc;
	}
	
	@Override
	public abstract void setupAttributes();
	
	
	@Override
	public final int getSize()
	{
		return 6;
	}
	@Override
	protected void dispose() {
		// TODO Auto-generated method stub
		
	}
	
	public abstract boolean isParticipatingMedia();
	public abstract boolean supportShadows();
	public abstract boolean supportLighting();
}


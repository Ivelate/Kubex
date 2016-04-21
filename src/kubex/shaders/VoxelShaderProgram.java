package kubex.shaders;

import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import ivengine.shaders.SimpleShaderProgram;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Abstract class. Defines the common characteristics of the shaders used to render the world cubes (TerrainVoxelShaderProgram, DepthVoxelShaderProgram, DepthPeelingShaderProgram).
 * It's almost unused now because all its functionality had been moved to deferred shaders.
 */
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
		
	}
	
	public abstract boolean isParticipatingMedia();
	public abstract boolean supportShadows();
	public abstract boolean supportLighting();
}


package monecruft.shaders;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetAttribLocation;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import ivengine.shaders.SimpleShaderProgram;

public class UnderwaterVoxelShaderProgram extends VoxelShaderProgram
{
	private int currentLightLoc;
	public UnderwaterVoxelShaderProgram()
	{
		this(false);
	}
	public UnderwaterVoxelShaderProgram(boolean verbose)
	{
		super("/shaders/voxelShader.vshader","/shaders/underwaterVoxelShader.fshader",verbose);
		this.currentLightLoc=glGetUniformLocation(this.getID(),"currentLight");
	}
	public int getCurrentLightUniformLocation()
	{
		return this.currentLightLoc;
	}
}

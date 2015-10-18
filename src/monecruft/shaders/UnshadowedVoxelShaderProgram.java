package monecruft.shaders;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetAttribLocation;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import ivengine.shaders.SimpleShaderProgram;

public class UnshadowedVoxelShaderProgram extends VoxelShaderProgram{
	
	public UnshadowedVoxelShaderProgram()
	{
		super("/shaders/voxelShaderUnshadow.vshader","/shaders/voxelShaderUnshadow.fshader",false);
	}
	public UnshadowedVoxelShaderProgram(boolean verbose)
	{
		super("/shaders/voxelShaderUnshadow.vshader","/shaders/voxelShaderUnshadow.fshader",verbose);
	}
}

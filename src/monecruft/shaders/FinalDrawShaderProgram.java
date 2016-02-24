package monecruft.shaders;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetAttribLocation;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

import org.lwjgl.opengl.GL11;

import ivengine.shaders.SimpleShaderProgram;

public class FinalDrawShaderProgram extends SimpleShaderProgram{
	
	private int locAttrib;
	
	public final int colorTex;
	public final int liquidLayersTex;
	public final int baseFboDepthTex;
	public final int liquidLayersTexLength;
	public final int invProjZ;
	/*public final int normalAndLightTex;
	public final int positionTex;
	public final int shadowMap;*/
	
	public FinalDrawShaderProgram()
	{
		this(false);
	}
	public FinalDrawShaderProgram(boolean verbose)
	{
		super("/shaders/finalDrawShader.vshader","/shaders/finalDrawShader.fshader",verbose);
		
		this.locAttrib = glGetAttribLocation(this.getID(), "location");
		
		this.colorTex=glGetUniformLocation(this.getID(),"colorTex");
		this.liquidLayersTex=glGetUniformLocation(this.getID(),"liquidLayersTex");
		this.baseFboDepthTex=glGetUniformLocation(this.getID(),"baseFboDepthTex");
		this.liquidLayersTexLength=glGetUniformLocation(this.getID(),"liquidLayersTexLength");
		this.invProjZ=glGetUniformLocation(this.getID(),"invProjZ");
		/*this.normalAndLightTex=glGetUniformLocation(this.getID(),"normalAndLightTex");
		this.positionTex=glGetUniformLocation(this.getID(),"positionTex");
		this.shadowMap=glGetUniformLocation(this.getID(),"shadowMap");*/
		
	}
	
	@Override
	public void setupAttributes() {
		glVertexAttribPointer(locAttrib,2,GL11.GL_FLOAT,false,4*2,0);
		glEnableVertexAttribArray(locAttrib);
	}
	@Override
	protected void dispose() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public int getSize() {
		return 2;
	}
}

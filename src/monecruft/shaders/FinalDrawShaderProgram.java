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
	private int farFaceLocAttrib;
	private int farFaceCamViewLocation;
	
	public final int colorTex;
	public final int brightnessNormalTex;
	public final int liquidLayersTex;
	public final int baseFboDepthTex;
	public final int liquidLayersTexLength;
	public final int cfar;
	public final int cnear;
	public final int cwidth;
	public final int cheight;
	public final int daylightAmount;
	public final int  sunNormal;
	public final int  splitDistances;
	public final int shadowMatrixes;
	public final int shadowMap;
	public final int viewMatrix;
	public final int projectionMatrix;
	
	public FinalDrawShaderProgram()
	{
		this(false);
	}
	public FinalDrawShaderProgram(boolean verbose)
	{
		super("/shaders/finalDrawShader.vshader","/shaders/finalDrawShader.fshader",verbose);
		
		this.locAttrib = glGetAttribLocation(this.getID(), "location");
		this.farFaceLocAttrib = glGetAttribLocation(this.getID(), "farFaceLocation"); 
		this.farFaceCamViewLocation = glGetAttribLocation(this.getID(), "farFaceCamViewLocation");
		
		this.colorTex=glGetUniformLocation(this.getID(),"colorTex");
		this.liquidLayersTex=glGetUniformLocation(this.getID(),"liquidLayersTex");
		this.brightnessNormalTex=glGetUniformLocation(this.getID(),"brightnessNormalTex");
		this.baseFboDepthTex=glGetUniformLocation(this.getID(),"baseFboDepthTex");
		this.daylightAmount=glGetUniformLocation(this.getID(),"daylightAmount");
		this.liquidLayersTexLength=glGetUniformLocation(this.getID(),"liquidLayersTexLength");
		this.sunNormal=glGetUniformLocation(this.getID(),"sunNormal");
		this.splitDistances=glGetUniformLocation(this.getID(),"splitDistances");
		this.shadowMatrixes=glGetUniformLocation(this.getID(),"shadowMatrixes");
		this.shadowMap=glGetUniformLocation(this.getID(),"shadowMap");
		this.cfar=glGetUniformLocation(this.getID(),"cfar");
		this.cnear=glGetUniformLocation(this.getID(),"cnear");
		this.cwidth=glGetUniformLocation(this.getID(),"cwidth");
		this.cheight=glGetUniformLocation(this.getID(),"cheight");
		this.viewMatrix=glGetUniformLocation(this.getID(),"viewMatrix");
		this.projectionMatrix=glGetUniformLocation(this.getID(),"projectionMatrix");
		
		//this.invProjZ=glGetUniformLocation(this.getID(),"invProjZ");
		/*this.normalAndLightTex=glGetUniformLocation(this.getID(),"normalAndLightTex");
		this.positionTex=glGetUniformLocation(this.getID(),"positionTex");
		this.shadowMap=glGetUniformLocation(this.getID(),"shadowMap");*/
		
	}
	
	@Override
	public void setupAttributes() {
		glVertexAttribPointer(locAttrib,2,GL11.GL_FLOAT,false,4*this.getSize(),0);
		glEnableVertexAttribArray(locAttrib);
		glVertexAttribPointer(farFaceLocAttrib,3,GL11.GL_FLOAT,false,4*this.getSize(),2*4);
		glEnableVertexAttribArray(farFaceLocAttrib);
		glVertexAttribPointer(farFaceCamViewLocation,3,GL11.GL_FLOAT,false,4*this.getSize(),5*4);
		glEnableVertexAttribArray(farFaceCamViewLocation);
	}
	@Override
	protected void dispose() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public int getSize() {
		return 8;
	}
}

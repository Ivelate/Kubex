package monecruft.gui;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glBufferSubData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniform1f;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import ivengine.view.MatrixHelper;
import monecruft.MonecruftGame;
import monecruft.shaders.FinalDrawShaderProgram;
import monecruft.storage.FloatBufferPool;

public class FinalDrawManager 
{
	private static final Vector4f xpypProjection=new Vector4f(1,1,1,1);
	private static final Vector4f xmypProjection=new Vector4f(-1,1,1,1);
	private static final Vector4f xpymProjection=new Vector4f(1,-1,1,1);
	private static final Vector4f xmymProjection=new Vector4f(-1,-1,1,1);
	
	private int finalDrawVbo;
	
	private float[] vboContent={	-1,-1,	0,0,0, 0,0,0,
									 1,-1,  0,0,0, 0,0,0,
									 1,1,	0,0,0, 0,0,0,
									-1,-1,	0,0,0, 0,0,0,
									 1,1, 	0,0,0, 0,0,0,
									-1,1,	0,0,0, 0,0,0};
	
	private FloatBuffer vbobuffer=BufferUtils.createFloatBuffer(vboContent.length);
	
	private Vector4f xmym=new Vector4f();
	private Vector4f xpym=new Vector4f();
	private Vector4f xmyp=new Vector4f();
	private Vector4f xpyp=new Vector4f();
	
	private FinalDrawShaderProgram FDSP;
	private LiquidRenderer liquidRenderer;
	private ShadowsManager shadowsManager;
	private World world;
	private Sky sky;
	private float cnear,cfar;
	
	public FinalDrawManager(FinalDrawShaderProgram FDSP,World world,Sky sky,ShadowsManager shadowsManager, LiquidRenderer liquidRenderer,Matrix4f projCameraInverseMatrix,float cnear,float cfar)
	{
		this.FDSP=FDSP;
		this.world=world;
		this.sky=sky;
		this.shadowsManager=shadowsManager;
		this.liquidRenderer=liquidRenderer;
		this.cnear=cnear;
		this.cfar=cfar;
		
		this.finalDrawVbo=glGenBuffers();
		glBindBuffer(GL15.GL_ARRAY_BUFFER,this.finalDrawVbo);
		glBufferData(GL15.GL_ARRAY_BUFFER,(vboContent.length*4),GL15.GL_DYNAMIC_DRAW);
		glBindBuffer(GL15.GL_ARRAY_BUFFER,0);
		
		Matrix4f.transform(projCameraInverseMatrix, xpypProjection, xpyp);
		Matrix4f.transform(projCameraInverseMatrix, xmypProjection, xmyp);
		Matrix4f.transform(projCameraInverseMatrix, xpymProjection, xpym);
		Matrix4f.transform(projCameraInverseMatrix, xmymProjection, xmym);
		
		vboContent[5]=xmym.x/xmym.w; vboContent[6]=xmym.y/xmym.w; vboContent[7]=xmym.z/xmym.w;
		vboContent[13]=xpym.x/xpym.w; vboContent[14]=xpym.y/xpym.w; vboContent[15]=xpym.z/xpym.w;
		vboContent[21]=xpyp.x/xpyp.w; vboContent[22]=xpyp.y/xpyp.w; vboContent[23]=xpyp.z/xpyp.w;
		vboContent[29]=xmym.x/xmym.w; vboContent[30]=xmym.y/xmym.w; vboContent[31]=xmym.z/xmym.w;
		vboContent[37]=xpyp.x/xpyp.w; vboContent[38]=xpyp.y/xpyp.w; vboContent[39]=xpyp.z/xpyp.w;
		vboContent[45]=xmyp.x/xmyp.w; vboContent[46]=xmyp.y/xmyp.w; vboContent[47]=xmyp.z/xmyp.w;		
	}
	public void draw(Matrix4f projWorldInverseMatrix,Matrix4f viewMatrix,Matrix4f projectionMatrix,float xres,float yres)
	{		
		Matrix4f.transform(projWorldInverseMatrix, xpypProjection, xpyp);
		Matrix4f.transform(projWorldInverseMatrix, xmypProjection, xmyp);
		Matrix4f.transform(projWorldInverseMatrix, xpymProjection, xpym);
		Matrix4f.transform(projWorldInverseMatrix, xmymProjection, xmym);
		
		vboContent[2]=xmym.x/xmym.w; vboContent[3]=xmym.y/xmym.w; vboContent[4]=xmym.z/xmym.w;
		vboContent[10]=xpym.x/xpym.w; vboContent[11]=xpym.y/xpym.w; vboContent[12]=xpym.z/xpym.w;
		vboContent[18]=xpyp.x/xpyp.w; vboContent[19]=xpyp.y/xpyp.w; vboContent[20]=xpyp.z/xpyp.w;
		vboContent[26]=xmym.x/xmym.w; vboContent[27]=xmym.y/xmym.w; vboContent[28]=xmym.z/xmym.w;
		vboContent[34]=xpyp.x/xpyp.w; vboContent[35]=xpyp.y/xpyp.w; vboContent[36]=xpyp.z/xpyp.w;
		vboContent[42]=xmyp.x/xmyp.w; vboContent[43]=xmyp.y/xmyp.w; vboContent[44]=xmyp.z/xmyp.w;
		
		this.vbobuffer.put(vboContent);
		this.vbobuffer.flip();
		
		glBindBuffer(GL15.GL_ARRAY_BUFFER,this.finalDrawVbo);
		glBufferSubData(GL15.GL_ARRAY_BUFFER,0,this.vbobuffer);
		
		this.vbobuffer.clear();
		
		//Ready. Drawing
		this.FDSP.enable();
		this.FDSP.setupAttributes();
		
		MatrixHelper.uploadMatrix(viewMatrix, this.FDSP.viewMatrix);
		MatrixHelper.uploadMatrix(projectionMatrix, this.FDSP.projectionMatrix);
		glUniform1i(this.FDSP.colorTex,MonecruftGame.BASEFBO_COLOR_TEXTURE_LOCATION);
		glUniform1i(this.FDSP.liquidLayersTex,MonecruftGame.LIQUIDLAYERS_TEXTURE_LOCATION);
		glUniform1i(this.FDSP.baseFboDepthTex,MonecruftGame.BASEFBO_DEPTH_TEXTURE_LOCATION);
		glUniform1i(this.FDSP.brightnessNormalTex,MonecruftGame.BASEFBO_NORMALS_BRIGHTNESS_TEXTURE_LOCATION);
		glUniform1i(this.FDSP.liquidLayersTexLength,this.liquidRenderer.getNumLayers());
		glUniform1f(this.FDSP.cfar,cfar);
		glUniform1f(this.FDSP.cnear,cnear);
		glUniform1f(this.FDSP.cwidth,xres);
		glUniform1f(this.FDSP.cheight,yres);
		GL20.glUniform1f(this.FDSP.daylightAmount, this.world.getDaylightAmount());
		
		//SHADOWS
		
		Vector3f sunNormal=this.sky.getSunNormal();
		GL20.glUniform3f(this.FDSP.sunNormal, sunNormal.x, sunNormal.y, sunNormal.z);
		GL20.glUniform1i(this.FDSP.shadowMap, MonecruftGame.SHADOW_TEXTURE_LOCATION);

		float[] dsplits=this.shadowsManager.getSplitDistances();
		switch(this.shadowsManager.getNumberSplits())
		{
		case 1: GL20.glUniform4f(this.FDSP.splitDistances, dsplits[1],0,0,0);
			break;
		case 2: GL20.glUniform4f(this.FDSP.splitDistances, dsplits[1],dsplits[2],0,0);
			break;
		case 3: GL20.glUniform4f(this.FDSP.splitDistances, dsplits[1],dsplits[2],dsplits[3],0);
			break;
		case 4: GL20.glUniform4f(this.FDSP.splitDistances, dsplits[1],dsplits[2],dsplits[3],dsplits[4]);
			break;
		}

		for(int i=0;i<this.shadowsManager.getNumberSplits();i++){
			MatrixHelper.uploadMatrix(this.shadowsManager.getOrthoProjectionForSplitScreenAdjusted(i), this.FDSP.shadowMatrixes+(i));
		}
		
		glDrawArrays(GL_TRIANGLES, 0, 6);
		
		glBindBuffer(GL15.GL_ARRAY_BUFFER,0);
		this.FDSP.disable();
		
	}
}

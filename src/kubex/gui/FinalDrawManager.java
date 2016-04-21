package kubex.gui;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glBufferSubData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
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
import kubex.KubexGame;
import kubex.entity.Player;
import kubex.shaders.DeferredShaderProgram;
import kubex.storage.FloatBufferPool;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Generic class. Manages final deferred passes, using generic fbos and shaders
 */
public class FinalDrawManager 
{
	//Mantains precalculated values used to find the far plane frustrum limits, to be interpolated via shader and find the world position of the pixel using only the depth buffer.
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
	
	private LiquidRenderer liquidRenderer;
	private ShadowsManager shadowsManager;
	private World world;
	private Sky sky;
	private float cnear,cfar;
	
	public FinalDrawManager(World world,Sky sky,ShadowsManager shadowsManager, LiquidRenderer liquidRenderer,Matrix4f projCameraInverseMatrix,float cnear,float cfar)
	{
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
		
		//On initcialization, performs the correspondent opperations to find the camera position far plane frustrum points multiplying them by the inverse projection matrix, constant in all execution
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
	
	/**
	 * Renders all the specified deferred passes. If the last fbo proportioned is 0, it will draw to the screen at the end.
	 */
	public final void draw(DeferredShaderProgram[] programs,int[] fbos,Matrix4f projWorldInverseMatrix,Matrix4f viewMatrix,Matrix4f projectionMatrix,float xres,float yres)
	{		
		//Calculates the world space points of the far plane frustrum.
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
		
		//Those arguments will be placed on the drawed quad via GPU vertex parameters.
		this.vbobuffer.put(vboContent);
		this.vbobuffer.flip();
		
		glBindBuffer(GL15.GL_ARRAY_BUFFER,this.finalDrawVbo);
		glBufferSubData(GL15.GL_ARRAY_BUFFER,0,this.vbobuffer);
		
		this.vbobuffer.clear();
		
		//Draws each specified deferred shading rendering pass
		for(int i=0;i<programs.length;i++)
		{
			glBindFramebuffer(GL_FRAMEBUFFER, fbos[i]);
			programs[i].enable();
			programs[i].setupAttributes();
			uploadToShader(programs[i],viewMatrix,projectionMatrix,xres,yres); //Uploads all needed uniforms to the current shader
		
			glDrawArrays(GL_TRIANGLES, 0, 6);
		
			programs[i].disable();
		}
		
		glBindBuffer(GL15.GL_ARRAY_BUFFER,0);
	}
	
	/**
	 * Uploads to the deferred shader the uniforms it needs. For it, consults the shader capabilities.
	 */
	protected void uploadToShader(DeferredShaderProgram DSP,Matrix4f viewMatrix,Matrix4f projectionMatrix,float xres,float yres)
	{		
		//Those uniforms are universal for all deferred shaders, they will be uploaded for each one of them
		MatrixHelper.uploadMatrix(viewMatrix, glGetUniformLocation(DSP.getID(),"viewMatrix"));
		MatrixHelper.uploadMatrix(projectionMatrix, glGetUniformLocation(DSP.getID(),"projectionMatrix"));
		glUniform1i(glGetUniformLocation(DSP.getID(),"colorTex"),DSP.colorTexLocation()); //The color texture of the shader can vary
		glUniform1i(glGetUniformLocation(DSP.getID(),"liquidLayersTex"),KubexGame.LIQUIDLAYERS_TEXTURE_LOCATION);
		glUniform1i(glGetUniformLocation(DSP.getID(),"baseFboDepthTex"),KubexGame.BASEFBO_DEPTH_TEXTURE_LOCATION);
		glUniform1i(glGetUniformLocation(DSP.getID(),"brightnessNormalTex"),KubexGame.BASEFBO_NORMALS_BRIGHTNESS_TEXTURE_LOCATION);
		glUniform1i(glGetUniformLocation(DSP.getID(),"liquidLayersTexLength"),this.liquidRenderer.getNumLayers());
		if(DSP.miscTexLocation()!=-1) glUniform1i(glGetUniformLocation(DSP.getID(),"miscTex"),DSP.miscTexLocation()); //Deferred shaders support up to 2 "misc" textures, if requested.
		if(DSP.miscTex2Location()!=-1) glUniform1i(glGetUniformLocation(DSP.getID(),"miscTex2"),DSP.miscTex2Location()); //they are called misc to generalize them and upload them in different shaders
																														 //with different purposes each.
		glUniform1f(glGetUniformLocation(DSP.getID(),"cfar"),cfar);
		glUniform1f(glGetUniformLocation(DSP.getID(),"cnear"),cnear);
		glUniform1f(glGetUniformLocation(DSP.getID(),"cwidth"),xres);
		glUniform1f(glGetUniformLocation(DSP.getID(),"cheight"),yres);
		
		glUniform1f(glGetUniformLocation(DSP.getID(), "time"),(float)(System.currentTimeMillis()%1000000)/1000); //Uploads current time to the shaders, for them to do things like water flow.
																												 //The value can't grow forever as the float precission is moderate, so it will be truncated
																												 //to 1.000 sec maximum, moment in which the time will reset to 0 and the flow in the scene will blink
																												 //it will not be very noticeable and it will happen once each 20 min.
																												 //with one 0 more it will hapen once each 3 hours, but im scared of the float precision errors.
		
		GL20.glUniform1f(glGetUniformLocation(DSP.getID(),"daylightAmount"), this.world.getDaylightAmount());
		Vector3f sunNormal=this.sky.getSunNormal();
		GL20.glUniform3f(glGetUniformLocation(DSP.getID(),"sunNormal"), sunNormal.x, sunNormal.y, sunNormal.z);
		
		if(DSP.supportWorldPosition()) //If the shader needs to know the world position of the camera, upload it truncating it to a 500 val max, to avoid floating precision errors.
		{
			GL20.glUniform3f(glGetUniformLocation(DSP.getID(),"WorldPosition"), (float)(this.world.getCameraCenter().x%500),(float)(this.world.getCameraCenter().y%500),(float)(this.world.getCameraCenter().z%500));
		}
		
		//if shadows supported, upload the needed uniforms
		if(DSP.supportShadows())
		{
			GL20.glUniform1i(glGetUniformLocation(DSP.getID(),"shadowMap"), KubexGame.SHADOW_TEXTURE_LOCATION);

			float[] dsplits=this.shadowsManager.getSplitDistances();
			int splitDistances=glGetUniformLocation(DSP.getID(),"splitDistances");
			switch(this.shadowsManager.getNumberSplits())
			{
			case 1: GL20.glUniform4f(splitDistances, dsplits[1],0,0,0);
				break;
			case 2: GL20.glUniform4f(splitDistances, dsplits[1],dsplits[2],0,0);
				break;
			case 3: GL20.glUniform4f(splitDistances, dsplits[1],dsplits[2],dsplits[3],0);
				break;
			case 4: GL20.glUniform4f(splitDistances, dsplits[1],dsplits[2],dsplits[3],dsplits[4]);
				break;
			}

			int shadowMatrixes=glGetUniformLocation(DSP.getID(),"shadowMatrixes");
			for(int i=0;i<this.shadowsManager.getNumberSplits();i++){
				MatrixHelper.uploadMatrix(this.shadowsManager.getOrthoProjectionForSplitScreenAdjusted(i), shadowMatrixes+(i));
			}
		}
		
		if(DSP.supportSkyParameters()) //if sky supported, upload all sky parameters
		{
			this.sky.uploadToShader(DSP);
		}
		if(DSP.supportPlayerLighting()) //If player lighting supported, upload players average lighting surrounding him.
		{
			int currentLightLoc=glGetUniformLocation(DSP.getID(),"currentLight");
			GL20.glUniform1f(currentLightLoc, this.world.getAverageLightExposed());
		}
	}
}

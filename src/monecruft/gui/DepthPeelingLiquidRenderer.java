package monecruft.gui;

import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL30.GL_DEPTH_ATTACHMENT;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glGenFramebuffers;

import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import monecruft.MonecruftGame;
import monecruft.shaders.DepthPeelingShaderProgram;

public class DepthPeelingLiquidRenderer extends LiquidRenderer
{
	private int[] layersFbos;
	private DepthPeelingShaderProgram DPS;
	public DepthPeelingLiquidRenderer(int layers)
	{
		super(layers);
		this.layersFbos=new int[layers];
		this.DPS=new DepthPeelingShaderProgram(true);
	}
	@Override
	public void initResources(int layersTex) 
	{
		for(int i=0;i<this.layersFbos.length;i++)
		{
			this.layersFbos[i]=glGenFramebuffers();
			glBindFramebuffer(GL_FRAMEBUFFER, this.layersFbos[i]);
			GL30.glFramebufferTextureLayer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, layersTex, 0, i);
	
			IntBuffer drawBuffers = BufferUtils.createIntBuffer(0);

			GL20.glDrawBuffers(drawBuffers);
			
		}
	}

	@Override
	public void renderLayers(World w,int xres,int yres) 
	{
		this.DPS.enable();
		int upperLimitDepth=GL20.glGetUniformLocation(this.DPS.getID(),"upperLimitDepth");
		int lowerLimitDepth=GL20.glGetUniformLocation(this.DPS.getID(),"lowerLimitDepth");
		int upperLimitIndex=GL20.glGetUniformLocation(this.DPS.getID(),"upperLimitIndex");
		
		GL20.glUniform1i(upperLimitDepth, MonecruftGame.LIQUIDLAYERS_TEXTURE_LOCATION);
		GL20.glUniform1i(lowerLimitDepth, MonecruftGame.BASEFBO_DEPTH_TEXTURE_LOCATION);
		
		GL20.glUniform1i(GL20.glGetUniformLocation(this.DPS.getID(),"xres"), xres);
		GL20.glUniform1i(GL20.glGetUniformLocation(this.DPS.getID(),"yres"), yres);
		
		w.overrideCurrentShader(this.DPS);

		for(int i=0;i<this.getNumLayers();i++)
		{
			GL20.glUniform1i(upperLimitIndex, i-1);
			glBindFramebuffer(GL_FRAMEBUFFER, this.layersFbos[i]); 
			glClear(GL11.GL_DEPTH_BUFFER_BIT); 
			GL11.glViewport(0,0,xres,yres);
			//glClearColor(0.6f, 0.8f, 1.0f, 0f);
			//tilesTexture.bind();
			//this.sunCam.updateProjection(this.shadowsManager.getOrthoProjectionForSplit(1));*/

			w.drawLiquids();
		}
		
		this.DPS.disable();
	}

}

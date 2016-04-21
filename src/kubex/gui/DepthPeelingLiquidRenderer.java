package kubex.gui;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
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

import kubex.KubexGame;
import kubex.shaders.DepthPeelingShaderProgram;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Perform depth peeling over the scene liquids, with as many layers as specified
 */
public class DepthPeelingLiquidRenderer extends LiquidRenderer
{
	private int[] layersFbos; //One fbo for each layer
	private DepthPeelingShaderProgram DPS;
	public DepthPeelingLiquidRenderer(int layers)
	{
		super(layers);
		this.layersFbos=new int[layers];
		this.DPS=new DepthPeelingShaderProgram(true);
	}
	
	/**
	 * Resources had been initcialized on KubexGame class, but here they are propperly configured.
	 */
	@Override
	public void initResources(int layersTex,int currentNormalTex) 
	{
		for(int i=0;i<this.layersFbos.length;i++)
		{
			this.layersFbos[i]=glGenFramebuffers();
			glBindFramebuffer(GL_FRAMEBUFFER, this.layersFbos[i]);
			GL30.glFramebufferTextureLayer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, layersTex, 0, i); //assign one layer of the texture array to each layer fbo
	
			IntBuffer drawBuffers = null;
			
			if(i==0){ //The first layer will render the normal of the water to a texture, too.
				drawBuffers=BufferUtils.createIntBuffer(1);
				GL30.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, currentNormalTex, 0);
				
				drawBuffers.put(GL_COLOR_ATTACHMENT0);
				drawBuffers.flip();
			}
			else{
				drawBuffers=BufferUtils.createIntBuffer(0);
			}

			GL20.glDrawBuffers(drawBuffers);
			
		}
	}

	/**
	 * Performs the depth peeling rendering
	 */
	@Override
	public void renderLayers(World w,int xres,int yres) 
	{
		this.DPS.enable();
		int upperLimitDepth=GL20.glGetUniformLocation(this.DPS.getID(),"upperLimitDepth");
		int lowerLimitDepth=GL20.glGetUniformLocation(this.DPS.getID(),"lowerLimitDepth");
		int upperLimitIndex=GL20.glGetUniformLocation(this.DPS.getID(),"upperLimitIndex");
		
		GL20.glUniform1i(upperLimitDepth, KubexGame.LIQUIDLAYERS_TEXTURE_LOCATION);
		GL20.glUniform1i(lowerLimitDepth, KubexGame.BASEFBO_DEPTH_TEXTURE_LOCATION);
		
		GL20.glUniform1i(GL20.glGetUniformLocation(this.DPS.getID(),"xres"), xres);
		GL20.glUniform1i(GL20.glGetUniformLocation(this.DPS.getID(),"yres"), yres);
		
		w.overrideCurrentShader(this.DPS); //Makes world use the depth peeling shader when drawing chunks

		//Writes each liquid layer to a texture
		for(int i=0;i<this.getNumLayers();i++)
		{
			GL20.glUniform1i(upperLimitIndex, i-1);
			glBindFramebuffer(GL_FRAMEBUFFER, this.layersFbos[i]); 
			glClear(GL11.GL_DEPTH_BUFFER_BIT); 
			GL11.glViewport(0,0,xres,yres);

			w.drawLiquids();
		}
		
		this.DPS.disable();
	}

}

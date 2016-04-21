package kubex.gui;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Abstract class of the Liquid Renderer. Per now the only liquid renderin mecanism supported is Depth Peeling, but it could be extended in the future to some 
 * newer methods existing in OpenGL 4.0
 */
public abstract class LiquidRenderer 
{
	protected final int numLayers;
	public LiquidRenderer(int numLayers)
	{
		this.numLayers=numLayers;
	}
	public int getNumLayers()
	{
		return this.numLayers;
	}
	public abstract void initResources(int layersTex,int currentNormalTex);
	

	public abstract void renderLayers(World w, int xres, int yres);
}

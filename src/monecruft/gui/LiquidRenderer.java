package monecruft.gui;

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

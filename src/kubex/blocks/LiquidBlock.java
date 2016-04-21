package kubex.blocks;

/**
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Standard liquid block
 */
public abstract class LiquidBlock extends Block{

	protected int heightLiquidLevel;
	protected int maxLiquidLevel;
	
	public LiquidBlock(int level,int maxlevel)
	{
		this.heightLiquidLevel=level;
		this.maxLiquidLevel=maxlevel;
	}
	@Override
	public boolean isOpaque() {
		return false;
	}

	@Override
	public boolean isSolid(){
		return false;
	}
	
	@Override
	public boolean isDrawable() {
		return true;
	}

	@Override
	public boolean isLiquid() {
		return true;
	}
	
	@Override
	public byte getLightProduced() {
		return 0;
	}
	@Override
	public boolean isPartnerGrouped(){
		return true;
	}
	@Override
	public boolean canSeeTrough()
	{
		return true;
	}
	@Override
	public boolean isCrossSectional()
	{
		return false;
	}
	
	@Override
	public int getLiquidLevel()
	{
		return this.heightLiquidLevel;
	}
	
	@Override
	public int getLiquidMaxLevel()
	{
		return this.maxLiquidLevel;
	}
	@Override
	public boolean occludesNaturalLight()
	{
		return true;
	}
}

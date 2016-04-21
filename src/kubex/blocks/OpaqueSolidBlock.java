package kubex.blocks;

/**
 * @author V�ctor Arellano Vicente (Ivelate)
 * 
 * Typical opaque solid block. Light can't pass through it, and players collide with it.
 */
public abstract class OpaqueSolidBlock extends Block{

	@Override
	public boolean isOpaque() {
		return true;
	}

	@Override
	public boolean isSolid()
	{
		return true;
	}
	
	@Override
	public boolean isDrawable() {
		return true;
	}

	@Override
	public boolean isLiquid() {
		return false;
	}
	
	@Override
	public byte getLightProduced() {
		return 0;
	}
	@Override
	public boolean isPartnerGrouped()
	{
		return false;
	}
	@Override
	public boolean canSeeTrough()
	{
		return false;
	}
	@Override
	public boolean isCrossSectional()
	{
		return false;
	}
	@Override
	public boolean occludesNaturalLight()
	{
		return true;
	}
}

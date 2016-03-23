package monecruft.blocks;

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

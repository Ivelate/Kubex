package monecruft.blocks;

public abstract class LiquidBlock implements Block{

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
}

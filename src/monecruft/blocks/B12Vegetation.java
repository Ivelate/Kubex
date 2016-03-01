package monecruft.blocks;

public class B12Vegetation extends Block{

	@Override
	public boolean isOpaque() {
		return false;
	}

	@Override
	public boolean isSolid()
	{
		return false;
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
	public byte getUpTex() {
		return 9;
	}

	@Override
	public byte getLatTex() {
		return 8;
	}

	@Override
	public byte getDownTex() {
		return 2;
	}

	@Override
	public String getCubeName() {
		return "Vegetation";
	}

	@Override
	public boolean isPartnerGrouped() {
		return true;
	}

	@Override
	public boolean canSeeTrough() {
		return true;
	}

	@Override
	public boolean isCrossSectional() {
		// TODO Auto-generated method stub
		return true;
	}
}

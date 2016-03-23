package monecruft.blocks;

public class B16Leaves extends Block{

	@Override
	public boolean isOpaque() {
		return false;
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
	public byte getUpTex() {
		return 19;
	}

	@Override
	public byte getLatTex() {
		return 19;
	}

	@Override
	public byte getDownTex() {
		return 19;
	}

	@Override
	public String getCubeName() {
		return "Leaves block";
	}

	@Override
	public boolean isPartnerGrouped() {
		return false;
	}

	@Override
	public boolean canSeeTrough() {
		return true;
	}

	@Override
	public boolean isCrossSectional() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean occludesNaturalLight() {
		// TODO Auto-generated method stub
		return true;
	}
}

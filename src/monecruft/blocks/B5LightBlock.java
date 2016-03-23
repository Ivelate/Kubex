package monecruft.blocks;

public class B5LightBlock extends Block{

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
		return 15;
	}

	@Override
	public byte getUpTex() {
		return 13;
	}

	@Override
	public byte getLatTex() {
		return 13;
	}

	@Override
	public byte getDownTex() {
		return 13;
	}

	@Override
	public String getCubeName() {
		return "Light block";
	}

	@Override
	public boolean isPartnerGrouped() {
		return true;
	}

	@Override
	public boolean canSeeTrough() {
		return false;
	}

	@Override
	public boolean isCrossSectional() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean occludesNaturalLight() {
		// TODO Auto-generated method stub
		return false;
	}
}

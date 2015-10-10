package monecruft.blocks;

public class B5LightBlock implements Block{

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
		return 5;
	}

	@Override
	public byte getLatTex() {
		return 5;
	}

	@Override
	public byte getDownTex() {
		return 5;
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
}

package monecruft.blocks;

public class B6Glass extends Block{

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
		return 6;
	}

	@Override
	public byte getLatTex() {
		return 6;
	}

	@Override
	public byte getDownTex() {
		return 6;
	}

	@Override
	public String getCubeName() {
		return "Glass block";
	}

	@Override
	public boolean isPartnerGrouped() {
		return true;
	}

	@Override
	public boolean canSeeTrough() {
		return true;
	}
}

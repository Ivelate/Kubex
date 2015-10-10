package monecruft.blocks;

public class B4Water extends LiquidBlock{

	@Override
	public byte getUpTex() {
		return 4;
	}

	@Override
	public byte getLatTex() {
		return 4;
	}

	@Override
	public byte getDownTex() {
		return 4;
	}

	@Override
	public String getCubeName() {
		return "Water block";
	}
}

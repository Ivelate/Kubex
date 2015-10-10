package monecruft.blocks;

public class B2Dirt extends OpaqueSolidBlock{

	@Override
	public byte getUpTex() {
		return 2;
	}

	@Override
	public byte getLatTex() {
		return 2;
	}

	@Override
	public byte getDownTex() {
		return 2;
	}

	@Override
	public String getCubeName() {
		return "Dirt block";
	}
}

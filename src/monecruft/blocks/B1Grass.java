package monecruft.blocks;

public class B1Grass extends OpaqueSolidBlock{

	@Override
	public byte getUpTex() {
		return 1;
	}

	@Override
	public byte getLatTex() {
		return -128;
	}

	@Override
	public byte getDownTex() {
		return 2;
	}

	@Override
	public String getCubeName() {
		return "Grass block";
	}
}

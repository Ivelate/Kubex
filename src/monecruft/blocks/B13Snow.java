package monecruft.blocks;

public class B13Snow extends OpaqueSolidBlock{

	@Override
	public byte getUpTex() {
		return 14;
	}

	@Override
	public byte getLatTex() {
		return 14;
	}

	@Override
	public byte getDownTex() {
		return 2;
	}
	
	@Override
	public String getCubeName() {
		return "Snow block";
	}
}

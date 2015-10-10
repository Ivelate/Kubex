package monecruft.blocks;

public class B3Stone extends OpaqueSolidBlock{

	@Override
	public byte getUpTex() {
		return 3;
	}

	@Override
	public byte getLatTex() {
		return 3;
	}

	@Override
	public byte getDownTex() {
		return 3;
	}
	
	@Override
	public String getCubeName() {
		return "Stone block";
	}
}

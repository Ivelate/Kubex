package monecruft.blocks;

public class B11Lava extends OpaqueSolidBlock{

	@Override
	public byte getUpTex() {
		return 4;
	}

	@Override
	public byte getLatTex() {
		return 5;
	}

	@Override
	public byte getDownTex() {
		return 2;
	}
	
	@Override
	public String getCubeName() {
		return "Lava block";
	}
}

package monecruft.blocks;

public class B15Wood extends OpaqueSolidBlock{

	@Override
	public byte getUpTex() {
		return 20;
	}

	@Override
	public byte getLatTex() {
		return 18;
	}

	@Override
	public byte getDownTex() {
		return 20;
	}
	
	@Override
	public String getCubeName() {
		return "Wood block";
	}
}

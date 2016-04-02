package monecruft.blocks;

public class B17DebugFunBlock extends OpaqueSolidBlock
{

	@Override
	public String getCubeName() {
		return "Does things - Use with caution";
	}

	@Override
	public byte getUpTex() 
	{
		return 9;
	}

	@Override
	public byte getLatTex() {
		return 10;
	}

	@Override
	public byte getDownTex() 
	{
		return 13;
	}

}

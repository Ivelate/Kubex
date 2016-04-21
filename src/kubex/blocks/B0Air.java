package kubex.blocks;

/**
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Air block. Can't be drawn, isnt solid, isn't liquid, doesn't produces light... it doesn't interact with anything in the world in any way.
 */
public class B0Air extends Block{

	@Override
	public boolean isOpaque() {
		return false;
	}

	@Override
	public boolean isDrawable() {
		return false;
	}

	@Override 
	public boolean isSolid()
	{
		return false;
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
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public byte getLatTex() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public byte getDownTex() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getCubeName() {
		return "Air block";
	}

	@Override
	public boolean isPartnerGrouped() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canSeeTrough() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean isCrossSectional() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean occludesNaturalLight() {
		// TODO Auto-generated method stub
		return false;
	}

}

package kubex.blocks;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Glass block. Almost all of it its transparent
 */
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
		return 12;
	}

	@Override
	public byte getLatTex() {
		return 12;
	}

	@Override
	public byte getDownTex() {
		return 12;
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

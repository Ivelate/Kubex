package kubex.blocks;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Vegetation grass block. Non solid. Needs a grass cube on down of it to live.
 */
public class B12VegetationGrass extends Block{

	@Override
	public boolean isOpaque() {
		return false;
	}

	@Override
	public boolean isSolid()
	{
		return false;
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
		return 9;
	}

	@Override
	public byte getLatTex() {
		return 8;
	}

	@Override
	public byte getDownTex() {
		return 2;
	}

	@Override
	public String getCubeName() {
		return "Vegetation - Grass";
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
		return true;
	}

	@Override
	public boolean occludesNaturalLight() {
		// TODO Auto-generated method stub
		return false;
	}
}

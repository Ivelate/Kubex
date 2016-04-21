package kubex.blocks;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Bright brick block, more white than its counterpart
 */
public class B18ClearBrickBlock extends OpaqueSolidBlock{

	@Override
	public byte getUpTex() {
		return 15;
	}

	@Override
	public byte getLatTex() {
		return 15;
	}

	@Override
	public byte getDownTex() {
		return 15;
	}
	
	@Override
	public String getCubeName() {
		return "Bright Brick block";
	}
}

package kubex.blocks;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Brick block
 */
public class B17BrickBlock extends OpaqueSolidBlock{

	@Override
	public byte getUpTex() {
		return 10;
	}

	@Override
	public byte getLatTex() {
		return 10;
	}

	@Override
	public byte getDownTex() {
		return 10;
	}
	
	@Override
	public String getCubeName() {
		return "Brick block";
	}
}

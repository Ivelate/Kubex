package kubex.blocks;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Rusty stone block
 */
public class B19RustyStoneBlock extends OpaqueSolidBlock{

	@Override
	public byte getUpTex() {
		return 17;
	}

	@Override
	public byte getLatTex() {
		return 17;
	}

	@Override
	public byte getDownTex() {
		return 17;
	}
	
	@Override
	public String getCubeName() {
		return "Rusty Stone Block";
	}
}

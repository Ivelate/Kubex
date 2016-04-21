package kubex.blocks;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Grass block. It is generated on top of the terrain on low heights
 */
public class B1Grass extends OpaqueSolidBlock{

	@Override
	public byte getUpTex() {
		return 1;
	}

	@Override
	public byte getLatTex() {
		return 0;
	}

	@Override
	public byte getDownTex() {
		return 2;
	}

	@Override
	public String getCubeName() {
		return "Grass block";
	}
}

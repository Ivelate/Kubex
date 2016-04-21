package kubex.blocks;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Indestructible block. Occupies the last world layer, so players cant mine outside the world and fall forever. It can be removed if using flying mode.
 */
public class B20Bedrock extends OpaqueSolidBlock{

	@Override
	public byte getUpTex() {
		return 22;
	}

	@Override
	public byte getLatTex() {
		return 22;
	}

	@Override
	public byte getDownTex() {
		return 22;
	}
	
	@Override
	public String getCubeName() {
		return "Indestructible Block";
	}
}

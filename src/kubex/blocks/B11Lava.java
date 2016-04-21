package kubex.blocks;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Lava block. Purelly aesthetic, doesn't inflict any damage or any effect if standing on it
 */
public class B11Lava extends OpaqueSolidBlock{

	@Override
	public byte getUpTex() {
		return 4;
	}

	@Override
	public byte getLatTex() {
		return 5;
	}

	@Override
	public byte getDownTex() {
		return 2;
	}
	
	@Override
	public String getCubeName() {
		return "Lava block";
	}
}

package kubex.blocks;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * TNT block. It explodes!
 */
public class B7TNT extends OpaqueSolidBlock{

	@Override
	public byte getUpTex() {
		return 21;
	}

	@Override
	public byte getLatTex() {
		return 21;
	}

	@Override
	public byte getDownTex() {
		return 21;
	}

	@Override
	public String getCubeName() {
		return "TNT block";
	}
}

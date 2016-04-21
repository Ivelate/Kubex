package kubex.blocks;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Stone block. Used for construction
 */
public class B3Stone extends OpaqueSolidBlock{

	@Override
	public byte getUpTex() {
		return 3;
	}

	@Override
	public byte getLatTex() {
		return 3;
	}

	@Override
	public byte getDownTex() {
		return 3;
	}
	
	@Override
	public String getCubeName() {
		return "Stone block";
	}
}

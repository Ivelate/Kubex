package kubex.blocks;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Wood trunk block
 */
public class B15Wood extends OpaqueSolidBlock{

	@Override
	public byte getUpTex() {
		return 20;
	}

	@Override
	public byte getLatTex() {
		return 18;
	}

	@Override
	public byte getDownTex() {
		return 20;
	}
	
	@Override
	public String getCubeName() {
		return "Wood Trunk block";
	}
}

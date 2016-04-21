package kubex.blocks;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Sand block
 */
public class B8SandBlock extends OpaqueSolidBlock{

	@Override
	public byte getUpTex() {
		return 11;
	}

	@Override
	public byte getLatTex() {
		return 11;
	}

	@Override
	public byte getDownTex() {
		return 11;
	}
	
	@Override
	public String getCubeName() {
		return "Sand block";
	}
}

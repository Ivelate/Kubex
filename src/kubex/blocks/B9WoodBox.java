package kubex.blocks;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Wood block box. The trunk wood block after passing through a carpenter. Used for construction
 */
public class B9WoodBox extends OpaqueSolidBlock{

	@Override
	public byte getUpTex() {
		return 6;
	}

	@Override
	public byte getLatTex() {
		return 6;
	}

	@Override
	public byte getDownTex() {
		return 6;
	}
	
	@Override
	public String getCubeName() {
		return "Wood Box block";
	}
}

package kubex.blocks;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author V�ctor Arellano Vicente (Ivelate)
 * 
 * Snow block. It is generated on top of the terrain in high heights
 */
public class B13Snow extends OpaqueSolidBlock{

	@Override
	public byte getUpTex() {
		return 14;
	}

	@Override
	public byte getLatTex() {
		return 14;
	}

	@Override
	public byte getDownTex() {
		return 2;
	}
	
	@Override
	public String getCubeName() {
		return "Snow block";
	}
}

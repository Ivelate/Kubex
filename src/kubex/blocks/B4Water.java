package kubex.blocks;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Water block. It expands to its sides!
 */
public class B4Water extends LiquidBlock{

	public B4Water(int level) {
		super(level,7);
	}

	@Override
	public byte getUpTex() {
		return 4;
	}

	@Override
	public byte getLatTex() {
		return 4;
	}

	@Override
	public byte getDownTex() {
		return 4;
	}

	@Override
	public String getCubeName() {
		return "Water block (Level "+this.heightLiquidLevel+")";
	}
}

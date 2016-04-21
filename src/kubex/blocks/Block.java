package kubex.blocks;

/**
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Class each new block has to extend. Lets each block implement its properties as functions, to be later used independently of the block type.
 */
public abstract class Block 
{
	public enum facecode{YP,YM,XP,XM,ZP,ZM};
	
	/**
	 * Cube name, for printing it when selecting the block
	 */
	public abstract String getCubeName();
	
	/**
	 * Does the player collides if he tries to pass trough the block?
	 */
	public abstract boolean isSolid(); 
	
	/**
	 * Does the block prevents the light from passing?
	 */
	public abstract boolean isOpaque();
	
	/**
	 * Is the block semi transparent? Can you see trough it?
	 */
	public abstract boolean canSeeTrough();
	
	/**
	 * Only for transparent blocks: If the block is positioned together with the same block type, and isPartnerGrouped() is true, faces between that two blocks will be culled
	 */
	public abstract boolean isPartnerGrouped();
	
	/**
	 * For vegetation. Instead of being drawed as a cube, the block will be drawn as a cross section
	 */
	public abstract boolean isCrossSectional();
	
	/**
	 * Can the cube be drawed? If not, the cube is completely transparent.
	 */
	public abstract boolean isDrawable();
	
	/**
	 * Is the cube a liquid?
	 */
	public abstract boolean isLiquid();
	
	/**
	 * Returns the amount of light produced by this block (With a maximum value of 15)
	 */
	public abstract byte getLightProduced();
	
	/**
	 * Returns the texture identifier of the top texture of the block
	 */
	public abstract byte getUpTex();
	
	/**
	 * Returns the texture identifier of the lateral texture of the block
	 */
	public abstract byte getLatTex();
	
	/**
	 * Returns the texture identifier of the down texture of the block
	 */
	public abstract byte getDownTex();
	
	/**
	 * Returns true if the block occludes natural light rays from going down, but it doesn't occludes normal indirect light to pass 
	 */
	public abstract boolean occludesNaturalLight();
	
	/**
	 * Only implemented in case of liquids. Returns the liquid level of the block
	 */
	public int getLiquidLevel() //Auto override if needed
	{
		return 0;
	}
	/**
	 * Only implemented in case of liquids. Returns the max liquid level of this liquid.
	 */
	public int getLiquidMaxLevel() //Auto override if needed
	{
		return 0;
	}
}

package monecruft.blocks;

public abstract class Block 
{
	public enum facecode{YP,YM,XP,XM,ZP,ZM};
	public abstract String getCubeName();
	public abstract boolean isSolid(); //Can pass trough it
	public abstract boolean isOpaque(); //Letting light pass/not
	public abstract boolean canSeeTrough(); //Can you see trough them? For culling
	public abstract boolean isPartnerGrouped(); //Only for non-opaque blocks: Draw ignored if is placed together with a same ID block
	public abstract boolean isCrossSectional(); //Instead of drawed as a cube, it is drawed as a cross section
	public abstract boolean isDrawable(); //Can be drawed
	public abstract boolean isLiquid(); //Is liquid
	public abstract byte getLightProduced();
	public abstract byte getUpTex();
	public abstract byte getLatTex();
	public abstract byte getDownTex();
	public abstract boolean occludesNaturalLight();
	
	public int getLiquidLevel() //Auto override if needed
	{
		return 0;
	}
	public int getLiquidMaxLevel() //Auto override if needed
	{
		return 0;
	}
}

package monecruft.blocks;

public interface Block 
{
	public enum facecode{YP,YM,XP,XM,ZP,ZM};
	public String getCubeName();
	public boolean isSolid(); //Can pass trough it
	public boolean isOpaque(); //Letting light pass/not
	public boolean canSeeTrough(); //Can you see trough them? For culling
	public boolean isPartnerGrouped(); //Only for non-opaque blocks: Draw ignored if is placed together with a same ID block
	public boolean isDrawable(); //Can be drawed
	public boolean isLiquid(); //Is liquid
	public byte getLightProduced();
	public byte getUpTex();
	public byte getLatTex();
	public byte getDownTex();
}

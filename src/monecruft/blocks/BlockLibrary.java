package monecruft.blocks;

public class BlockLibrary 
{
	private static Block[] blockList={
		new B0Air(),
		new B1Grass(),
		new B2Dirt(),
		new B3Stone(),
		new B4Water(),
		new B5LightBlock(),
		new B6Glass(),
		new B7TNT()
	};
	private static Block defaultBlock=new B0Air();
	
	public static boolean isOpaque(byte blockID){
		if(blockID<0||blockID>=blockList.length) return defaultBlock.isOpaque();
		return blockList[blockID].isOpaque();
	}
	public static boolean isPartnerGrouped(byte blockID){
		if(blockID<0||blockID>=blockList.length) return defaultBlock.isPartnerGrouped();
		return blockList[blockID].isPartnerGrouped();
	}
	public static boolean canSeeTrough(byte blockID){
		if(blockID<0||blockID>=blockList.length) return defaultBlock.canSeeTrough();
		return blockList[blockID].canSeeTrough();
	}
	public static boolean isSolid(byte blockID){
		if(blockID<0||blockID>=blockList.length) return defaultBlock.isSolid();
	return blockList[blockID].isSolid();
	}
	public static boolean isDrawable(byte blockID){
		if(blockID<0||blockID>=blockList.length) return defaultBlock.isDrawable();
		return blockList[blockID].isDrawable();
	}
	public static boolean isLiquid(byte blockID){
		if(blockID<0||blockID>=blockList.length) return defaultBlock.isLiquid();
		return blockList[blockID].isLiquid();
	}
	public static boolean isLightSource(byte blockID){
		if(blockID<0||blockID>=blockList.length) return defaultBlock.getLightProduced()!=0;
		return blockList[blockID].getLightProduced()!=0;
	}
	public static byte getLightProduced(byte blockID){
		if(blockID<0||blockID>=blockList.length) return defaultBlock.getLightProduced();
		return blockList[blockID].getLightProduced();
	}
	public static byte getUpTex(byte blockID){
		if(blockID<0||blockID>=blockList.length) return defaultBlock.getUpTex();
		return blockList[blockID].getUpTex();
	}
	public static byte getLatTex(byte blockID){
		if(blockID<0||blockID>=blockList.length) return defaultBlock.getLatTex();
		return blockList[blockID].getLatTex();
	}
	public static byte getDownTex(byte blockID){
		if(blockID<0||blockID>=blockList.length) return defaultBlock.getDownTex();
		return blockList[blockID].getDownTex();
	}
	public static String getName(byte blockID)
	{
		if(blockID<0||blockID>=blockList.length) return defaultBlock.getCubeName();
		return blockList[blockID].getCubeName();
	}
	public static int size(){
		return blockList.length;
	}
}

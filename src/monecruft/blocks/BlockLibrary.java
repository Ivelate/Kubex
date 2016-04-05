package monecruft.blocks;

public class BlockLibrary 
{
	private static Block[] blockList={
		new B0Air(),
		new B1Grass(),
		new B2Dirt(),
		new B3Stone(),
		new B4Water(7),new B4Water(6),new B4Water(5),new B4Water(4),new B4Water(3),new B4Water(2),new B4Water(1),new B4Water(0),
		new B5LightBlock(),
		new B6Glass(),
		new B7TNT(),
		new B8RainMaker(),
		new B9SuperTower(),
		new B10Adoquin(),
		new B11Lava(),
		new B12VegetationGrass(),
		new B13Snow(),
		new B14VegetationDandellion(),
		new B15Wood(),
		new B16Leaves(),
		new B17DebugWaterAntagonist(),
		new B17DebugFunBlock()
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
	public static boolean occludesNaturalLight(byte blockID)
	{
		if(blockID<0||blockID>=blockList.length) return defaultBlock.occludesNaturalLight();
		return blockList[blockID].occludesNaturalLight();
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
	public static boolean isCrossSectional(byte blockID){
		if(blockID<0||blockID>=blockList.length) return defaultBlock.isCrossSectional();
		return blockList[blockID].isCrossSectional();
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
	public static int getLiquidLevel(byte blockID)
	{
		if(blockID<0||blockID>=blockList.length) return defaultBlock.getLiquidLevel();
		return blockList[blockID].getLiquidLevel();
	}
	public static int getLiquidMaxLevel(byte blockID)
	{
		if(blockID<0||blockID>=blockList.length) return defaultBlock.getLiquidMaxLevel();
		return blockList[blockID].getLiquidMaxLevel();
	}
	public static byte getLiquidBlockWithLevel(byte baseCube,int level)
	{
		return (byte)(baseCube+(BlockLibrary.getLiquidMaxLevel(baseCube)-level));
	}
	public static byte getLiquidBaseCube(byte cube)
	{
		return (byte)(cube+BlockLibrary.getLiquidLevel(cube)-BlockLibrary.getLiquidMaxLevel(cube));
	}
	public static boolean isSameBlock(byte blockID1,byte blockID2){
		if(isLiquid(blockID1)&&isLiquid(blockID2)){
			return getLiquidLevel(blockID1)+blockID1 == getLiquidLevel(blockID2)+blockID2;
		}
		else return blockID1==blockID2;
	}
	public static int size(){
		return blockList.length;
	}
}

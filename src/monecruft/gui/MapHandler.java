package monecruft.gui;

import monecruft.blocks.BlockLibrary;
import monecruft.gui.Chunk.Direction;
import monecruft.gui.MapGenerator.ChunkGenerationResult;
import monecruft.storage.ByteArrayPool;

public class MapHandler 
{
	public static final int BOUND_XR=0;
	public static final int BOUND_XL=1;
	public static final int BOUND_YR=2;
	public static final int BOUND_YL=3;
	public static final int BOUND_ZR=4;
	public static final int BOUND_ZL=5;
	
	private MapGenerator mg;
	private WorldFacade wf;
	public MapHandler(int bbegin,int bend,int mapcode,WorldFacade wf)
	{
		this.mg=new MapGenerator(bbegin,bend,mapcode);
		this.wf=wf;
	}
	//|TODO load from arch
	/*public boolean getChunk(int x,int y,int z){
			return this.getChunk(x, y, z,ByteArrayPool.getArrayUncleaned());
	}*/
	public ChunkGenerationResult getChunk(int x,int y,int z,byte[][][] c){
		return this.mg.generateChunk(x, y, z,c);
	}
	/*public byte[][][] getBounds(int x,int y,int z)
	{
		byte[][][] bounds=new byte[6][Chunk.CHUNK_DIMENSION][Chunk.CHUNK_DIMENSION];
		Chunk c=this.wf.getChunkByIndex(x-1,y,z);
		
		if(c==null) c=this.wf.getChunkInAddList(x-1,y,z);
		if(c==null){
			bounds[BOUND_XR]=this.mg.generateBoundFromChunk(x-1,y,z,BOUND_XR);
		} else {
			for(int cy=0;cy<Chunk.CHUNK_DIMENSION;cy++)
			{
				for(int cz=0;cz<Chunk.CHUNK_DIMENSION;cz++)
				{
					bounds[BOUND_XR][cy][cz]=c.getCubeAt(Chunk.CHUNK_DIMENSION-1, cy, cz);
				}
			}
		}
		c=this.wf.getChunkByIndex(x+1,y,z);
		if(c==null) c=this.wf.getChunkInAddList(x+1,y,z);
		if(c==null){
			bounds[BOUND_XL]=this.mg.generateBoundFromChunk(x+1,y,z,BOUND_XL);
		} else {
			for(int cy=0;cy<Chunk.CHUNK_DIMENSION;cy++)
			{
				for(int cz=0;cz<Chunk.CHUNK_DIMENSION;cz++)
				{
					bounds[BOUND_XL][cy][cz]=c.getCubeAt(0, cy, cz);
				}
			}
		}
		c=this.wf.getChunkByIndex(x,y-1,z);
		if(c==null) c=this.wf.getChunkInAddList(x,y-1,z);
		if(c==null){
			bounds[BOUND_YR]=this.mg.generateBoundFromChunk(x,y-1,z,BOUND_YR);
		} else {
			for(int cx=0;cx<Chunk.CHUNK_DIMENSION;cx++)
			{
				for(int cz=0;cz<Chunk.CHUNK_DIMENSION;cz++)
				{
					bounds[BOUND_YR][cx][cz]=c.getCubeAt(cx, Chunk.CHUNK_DIMENSION-1, cz);
				}
			}
		}
		c=this.wf.getChunkByIndex(x,y+1,z);
		if(c==null) c=this.wf.getChunkInAddList(x,y+1,z);
		if(c==null){
			bounds[BOUND_YL]=this.mg.generateBoundFromChunk(x,y+1,z,BOUND_YL);
		} else {
			for(int cx=0;cx<Chunk.CHUNK_DIMENSION;cx++)
			{
				for(int cz=0;cz<Chunk.CHUNK_DIMENSION;cz++)
				{
					bounds[BOUND_YL][cx][cz]=c.getCubeAt(cx, 0, cz);
				}
			}
		}
		c=this.wf.getChunkByIndex(x,y,z-1);
		if(c==null) c=this.wf.getChunkInAddList(x,y,z-1);
		if(c==null){
			bounds[BOUND_ZR]=this.mg.generateBoundFromChunk(x,y,z-1,BOUND_ZR);
		} else {
			for(int cx=0;cx<Chunk.CHUNK_DIMENSION;cx++)
			{
				for(int cy=0;cy<Chunk.CHUNK_DIMENSION;cy++)
				{
					bounds[BOUND_ZR][cx][cy]=c.getCubeAt(cx, cy, Chunk.CHUNK_DIMENSION-1);
				}
			}
		}
		c=this.wf.getChunkByIndex(x,y,z+1);
		if(c==null) c=this.wf.getChunkInAddList(x,y,z+1);
		if(c==null){
			bounds[BOUND_ZL]=this.mg.generateBoundFromChunk(x,y,z+1,BOUND_ZL);
		} else {
			for(int cx=0;cx<Chunk.CHUNK_DIMENSION;cx++)
			{
				for(int cy=0;cy<Chunk.CHUNK_DIMENSION;cy++)
				{
					bounds[BOUND_ZL][cx][cy]=c.getCubeAt(cx, cy, 0);
				}
			}
		}
		return bounds;
	}*/
	public boolean shouldDraw(byte blockCode,byte neighbourBlockCode,boolean liquidTag,Direction d){
		if(liquidTag)
			if(BlockLibrary.isLiquid(blockCode)||BlockLibrary.isSolid(blockCode)){
				/*if(BlockLibrary.isSameBlock(blockCode, neighbourBlockCode)){
					int maxh=BlockLibrary.getLiquidMaxLevel(blockCode);
					if(d==Direction.YP){
						if(BlockLibrary.getLiquidLevel(neighbourBlockCode)<maxh) return true;
						else return false;
					}
					else if(d==Direction.YM){
						if(BlockLibrary.getLiquidLevel(blockCode)<maxh) return true;
						else return false;
					}
				}*/
				return false;
			}
			else return true;
		else
			return (BlockLibrary.canSeeTrough(blockCode)) && !(blockCode==neighbourBlockCode&&BlockLibrary.isPartnerGrouped(blockCode));
	}
}

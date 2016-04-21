package kubex.gui;

import kubex.blocks.BlockLibrary;
import kubex.gui.Chunk.Direction;
import kubex.gui.MapGenerator.ChunkGenerationResult;
import kubex.storage.ByteArrayPool;
import kubex.storage.CubeStorage;
import kubex.storage.FileManager;
import kubex.storage.FileManager.ChunkLoadResult;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Acts like a facade for chunk loading / store, decoupling it from the program logic.
 * In practice, looks for the ckunks in the FileManager and, if they don't exist, generates them using the MapGenerator.
 */
public class MapHandler 
{	
	public class ChunkData
	{
		public final ChunkGenerationResult chunkGenerationResult;
		public final boolean initcializedFlag;
		public ChunkData(ChunkGenerationResult chunkGenerationResult,boolean initcializedFlag)
		{
			this.chunkGenerationResult=chunkGenerationResult;
			this.initcializedFlag=initcializedFlag;
		}
	}
	private MapGenerator mg;
	private FileManager fm;
	private WorldFacade wf;
	private ChunkStorer chunkStorer;
	
	public MapHandler(int bbegin,int bend,int mapcode,long seed,WorldFacade wf,ChunkStorer chunkStorer,FileManager fm)
	{
		this.chunkStorer=chunkStorer;
		this.mg=new MapGenerator(bbegin,bend,mapcode,seed);
		this.fm=fm;
		this.wf=wf;
	}
	
	/**
	 * Return the chunk cubes for the chunk pos x,y,z , loading them from a file, if they are already stored, or generating them.
	 */
	public ChunkData getChunk(int x,int y,int z,byte[][][] c)
	{
		ChunkLoadResult res=this.fm.loadChunk(c, x, y, z);
		switch(res)
		{
		case CHUNK_NORMAL_INITCIALIZED:
			return new ChunkData(ChunkGenerationResult.CHUNK_NORMAL,true);
		case CHUNK_NORMAL_NOT_INITCIALIZED:
			return new ChunkData(ChunkGenerationResult.CHUNK_NORMAL,false);
		case CHUNK_FULL:
			return new ChunkData(ChunkGenerationResult.CHUNK_NORMAL,true);
		case CHUNK_NOT_FOUND:
			return new ChunkData(this.mg.generateChunk(x, y, z,c),false);
		case CHUNK_EMPTY:
			return new ChunkData(ChunkGenerationResult.CHUNK_EMPTY,true);
		}
		
		return new ChunkData(ChunkGenerationResult.CHUNK_EMPTY,true); //Never reached
	}
	
	/**
	 * Stores a chunk c in a file (Only sends the request to the ChunkStorer thread)
	 */
	public void storeChunk(int x,int y,int z,CubeStorage c,boolean initcializedFlag)
	{
			this.chunkStorer.addChunkStoreRequest(new ChunkStoreRequest(c, x, y, z,initcializedFlag));
	}
	
	/**
	 * Performs the second pass chunk generation over chunk <c>
	 */
	public void generateChunkObjects(Chunk c)
	{
		this.mg.generateChunkObjects(c);
	}
	
	public FileManager getFileManager()
	{
		return this.fm;
	}
	
	/**
	 * Returns true if and only if a face of <blockCode>, collindant with a <neighbourBlockCode>, needs to be drawn. (If a block has no air collindant to it it can't be seen, so its not needed to draw it).
	 * On liquid blocks, with <liquidTag> true, the algorithm is a little different.
	 */
	public boolean shouldDraw(byte blockCode,byte neighbourBlockCode,boolean liquidTag,Direction d){
		if(liquidTag)
			if(BlockLibrary.isSolid(blockCode)){
				return BlockLibrary.canSeeTrough(blockCode);
			}
			else if(BlockLibrary.isLiquid(blockCode)){
				return false;
			}
			else return true;
		else
			return (BlockLibrary.canSeeTrough(blockCode)) && !(blockCode==neighbourBlockCode&&BlockLibrary.isPartnerGrouped(blockCode));
	}
}

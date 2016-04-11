package monecruft.gui;

import monecruft.storage.CubeStorage;

/**
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Chunk Store Request, containing the position of a chunk, its content, and if it has been initcialized (Second pass generation performed). 
 * Used as a wrapper class, to use later to store a chunk in a file.
 */
public class ChunkStoreRequest 
{
	public final int chunkx;
	public final int chunky;
	public final int chunkz;
	public final CubeStorage chunkCubes;
	public final boolean initcializedFlag;
	
	public ChunkStoreRequest(CubeStorage chunkCubes,int cx,int cy,int cz,boolean initcializedFlag)
	{
		this.chunkCubes=chunkCubes;
		this.chunkx=cx;
		this.chunky=cy;
		this.chunkz=cz;
		this.initcializedFlag=initcializedFlag;
	}
}

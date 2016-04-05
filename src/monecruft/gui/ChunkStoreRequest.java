package monecruft.gui;

import monecruft.storage.CubeStorage;

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

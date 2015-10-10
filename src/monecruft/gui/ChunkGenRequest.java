package monecruft.gui;

public class ChunkGenRequest 
{
	private int chunkx;
	private int chunky;
	private int chunkz;
	public ChunkGenRequest(int cx,int cy,int cz)
	{
		this.chunkx=cx;
		this.chunky=cy;
		this.chunkz=cz;
	}
	public int getChunkx() {
		return chunkx;
	}
	public int getChunky() {
		return chunky;
	}
	public int getChunkz() {
		return chunkz;
	}
}

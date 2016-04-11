package monecruft.gui;

/**
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Chunk Gen Request, containing the position of a chunk. 
 * Used as a wrapper class, to use later to generate a chunk.
 */
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

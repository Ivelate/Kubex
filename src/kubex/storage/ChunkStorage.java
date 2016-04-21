package kubex.storage;


import ivengine.properties.Cleanable;
import kubex.gui.Chunk;
import kubex.gui.World;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Chunk Storage class, in which chunks are stored, using a circular three-dimensional list adapted to the maximum chunks number loaded at once defined by the chunk render distance. 
 * The access and remove time is O(1)
 */
public class ChunkStorage implements Cleanable{
	private Chunk[][][] chunkList;
	private int size;
	private int iterx=0;
	private int itery=0;
	private int iterz=0;
	static boolean add=false;
	
	public ChunkStorage(int size)
	{
		this.chunkList=new Chunk[size][World.HEIGHT][size];
		this.size=size;
	}
	
	/**
	 * Adds a chunk <c> with position <x><y><z> to the chunk storage list
	 */
	public void addChunk(int x,int y,int z,Chunk c)
	{
		if(x<0)x=this.size + x%this.size;
		if(z<0)z=this.size + z%this.size;
		
		//If a chunk already existed here, we dispose it. Normally the old chunks are removed before the new ones are inserted, so this shouldn't happen anyways
		if(chunkList[x%this.size][y][z%this.size]!=null)
		{
			chunkList[x%this.size][y][z%this.size].fullClean();
		}
		chunkList[x%this.size][y][z%this.size]=c;
	}
	
	/**
	 * Gets the chunk at the position <xf><yf><zf>
	 */
	public Chunk getChunk(double xf,double yf,double zf)
	{
		int x=(int)Math.floor(xf);int y=(int)Math.floor(yf);int z=(int)Math.floor(zf);
		if(y<0||y>=World.HEIGHT) return null;
		if(x<0)x=this.size + x%this.size;
		if(z<0)z=this.size + z%this.size;
		return this.chunkList[x%this.size][y][z%this.size];
	}
	
	/**
	 * Removes the chunk at the position <x><y><f>
	 */
	public void removeChunk(int x,int y,int z)
	{
		int normx=x%this.size;
		int normz=z%this.size;
		
		if(normx<0)normx=this.size + normx;
		if(normz<0)normz=this.size + normz;
		if(this.chunkList[normx][y][normz]!=null)
		{
			this.chunkList[normx][y][normz].fullClean();
			this.chunkList[normx][y][normz]=null;
		}
	}
	
	
	/**
	 * Inits the iterator
	 */
	public boolean initIter()
	{
		this.iterx=-1;
		this.itery=0;
		this.iterz=0;
		return true;
	}

	/**
	 * Returns the next element in the iterator
	 */
	public Chunk next()
	{
		boolean done=false;
		while(!done)
		{
			this.iterx++;
			if(this.iterx>=this.size)
			{
				this.iterx=0;
				this.iterz++;
				if(this.iterz>=this.size)
				{
					iterz=0;
					this.itery++;
					if(this.itery>=this.chunkList[0].length)
					{
						return null;
					}
				}
			}
			Chunk r=this.chunkList[this.iterx][this.itery][this.iterz];
			if(r!=null) return r;
		}
		return null;
	}
	
	/**
	 * Gets the chunk with the index <x><y><z>. This is a circular list, so some other chunk could be returned (Although its unlikely, as old chunks are always removed after a new one is inserted)
	 * Always safe than sorry, so the method double checks if the chunk is the right one. If so, returns it. If the chunk is not the right one, or the list doesn't contains a chunk in that position,
	 * returns false
	 */
	public Chunk getChunkByIndex(int x,int y,int z)
	{
		int normx=x%this.size;
		int normz=z%this.size;
		
		if(normx<0)normx=this.size + normx;
		if(normz<0)normz=this.size + normz;
		if(y>=this.chunkList[0].length||y<0) return null;
		
		Chunk ret=this.chunkList[normx][y][normz];
		if(ret!=null && ret.getX()==x&&ret.getY()==y&&ret.getZ()==z) return ret;
		else return null;
	}
	
	/**
	 * Disposes this storage, disposing all chunks in them (Action which will cause them to be stored in disk)
	 */
	@Override
	public void fullClean() {
		for(int cx=0;cx<this.chunkList.length;cx++)
		{
			for(int cy=0;cy<this.chunkList[0].length;cy++)
			{
				for(int cz=0;cz<this.chunkList[0][0].length;cz++)
				{
					if(this.chunkList[cx][cy][cz]!=null) this.chunkList[cx][cy][cz].fullClean();
				}
			}
		}
		this.chunkList=null;
	}
	
}

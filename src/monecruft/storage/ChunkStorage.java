package monecruft.storage;


import ivengine.properties.Cleanable;
import monecruft.gui.Chunk;
import monecruft.gui.World;

public class ChunkStorage implements Cleanable{
	private Chunk[][][] chunkList;
	private boolean[][][] updateRequested; 
	private int size;
	private int iterx=0;
	private int itery=0;
	private int iterz=0;
	static boolean add=false;
	
	public ChunkStorage(int size)
	{
		this.chunkList=new Chunk[size][World.HEIGHT][size];
		this.updateRequested=new boolean[size][World.HEIGHT][size];
		this.size=size;
	}
	public void addChunk(int x,int y,int z,Chunk c)
	{
		//if(add){ return;}
		//else add=!add;
		if(x<0)x=this.size + x%this.size;
		if(z<0)z=this.size + z%this.size;
		chunkList[x%this.size][y][z%this.size]=c;
	}
	public Chunk getChunk(float xf,float yf,float zf)
	{
		int x=(int)Math.floor(xf);int y=(int)Math.floor(yf);int z=(int)Math.floor(zf);
		if(y<0||y>=World.HEIGHT) return null;
		if(x<0)x=this.size + x%this.size;
		if(z<0)z=this.size + z%this.size;
		return this.chunkList[x%this.size][y][z%this.size];
	}
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
	/*				 ITERADORES 			*/
	/**
	 * Inicia el iterador
	 * @return false si la tabla es vacia, true en otro caso
	 */
	public boolean initIter()
	{
		this.iterx=-1;
		this.itery=0;
		this.iterz=0;
		return true;
	}
	/**
	 * @return false no hay siguiente elemento en el iterador o este es nulo, true en otro caso
	 * broken for now
	 */
	/*public boolean hasNext()
	{
		if(this.chunkList.currentEl()==null) return false;
		else if(this.chunkList.currentEl())
		return true;
	}*/
	/**
	 * @return el siguiente elemento del iterador
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
	public boolean isLoaded(int x,int z)
	{
		int normx=x%this.size;
		int normz=z%this.size;
		
		if(normx<0)normx=this.size + normx;
		if(normz<0)normz=this.size + normz;
		Chunk c=this.chunkList[normx][0][normz];
		if(c!=null) return true;
		return false;
	}
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
	@Override
	public void fullClean() {
		this.chunkList=null;
	}
	
}

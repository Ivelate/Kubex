package kubex.gui;

import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import kubex.utils.Vector3d;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Facade class to abstract all world class functionality in only some methods. Only the methods not existing in the world class are commented
 */
public class WorldFacade 
{
	private World world;
	public WorldFacade(World world)
	{
		this.world=world;
	}
	public float getWorldGravity()
	{
		return world.getGravity();
	}

	public float getDaylightAmount()
	{
		return world.getDaylightAmount();
	}

	public byte getContent(double x,double y,double z)
	{
		return world.getContent(x, y, z);
	}
	
	public void requestChunkUpdate(Chunk c)
	{
		this.world.getChunkUpdater().updateChunk(c);
	}
	
	public float getContentNaturalLight(float x,float y,float z)
	{
		return world.getContentNaturalLight(x, y, z) *(world.getDaylightAmount()-0.2f)*1.25f;
	}
	
	public float getContentMaxLight(float x,float y,float z)
	{
		float art=this.getContentArtificialLight(x, y, z);
		float nat=this.getContentNaturalLight(x, y, z);

		if(art>nat) return art;
		else return nat;
	}
	
	public float getContentArtificialLight(float x,float y,float z)
	{
		return world.getContentArtificialLight(x, y, z);
	}
	
	public void addChunkAddPetition(Chunk c)
	{
		synchronized(this.world)
		{
			this.world.getAddList().add(c);
		}
	}
	
	public void insertChunkInUpdateList(Chunk c)
	{
		this.world.addChunkToUpdateList(c);
	}
	
	public Chunk getChunkInAddList(int x,int y,int z)
	{
		synchronized(this.world)
		{
			for(Chunk ch:this.world.getAddList())
			{
				if(ch.getX()==x&&ch.getY()==y&&ch.getZ()==z)
				{
					return ch;
				}
			}
			return null;
		}
	}
	
	/**
	 * Returns in an int format which neighbours of the chunk <c> have been already added to the world, being 1 added and 0 not added. 
	 * Each chunk has a total of 26 neighbours, so a "all neighbours added" return value would be equal to 00000111111111111101111111111111
	 */
	public int getNeighboursAdded(Chunk c)
	{
		int res=0;
		for(int x=-1;x<=1;x++)
		{
			for(int y=-1;y<=1;y++)
			{
				for(int z=-1;z<=1;z++)
				{
					if(x!=0||y!=0||z!=0) {
						Chunk cn=this.world.getChunkStorage().getChunkByIndex(c.getX()+x,c.getY()+y,c.getZ()+z);
						if((cn!=null&&cn.isLightCalculated()) || (y==-1&&c.getY()==0) || (y==1&&c.getY()==World.HEIGHT-1)) {
							int index=x*9 + z*3 + y + 13; //(x+1)*9 + (z+1)*3 + (y+1)
							res=res | (1<<index);
						}
					}
				}
			}
		}
		return res;
	}
	
	/**
	 * Returns all the LINEAR neighbours of the chunk <c>, not counting diagonal neighbours
	 */
	public Chunk[] getNeighbours(Chunk c)
	{
		Chunk[] neigh=new Chunk[6];
		neigh[Chunk.Direction.XP.ordinal()]=getChunkByIndex(c.getX()+1,c.getY(),c.getZ());
		neigh[Chunk.Direction.XM.ordinal()]=getChunkByIndex(c.getX()-1,c.getY(),c.getZ());
		neigh[Chunk.Direction.YP.ordinal()]=getChunkByIndex(c.getX(),c.getY()+1,c.getZ());
		neigh[Chunk.Direction.YM.ordinal()]=getChunkByIndex(c.getX(),c.getY()-1,c.getZ());
		neigh[Chunk.Direction.ZP.ordinal()]=getChunkByIndex(c.getX(),c.getY(),c.getZ()+1);
		neigh[Chunk.Direction.ZM.ordinal()]=getChunkByIndex(c.getX(),c.getY(),c.getZ()-1);
		return neigh;
	}
	
	public int getAddListSize()
	{
		return this.world.getAddList().size();
	}
	
	public void reloadPlayerFOV(int x,int y,int z)
	{
		this.world.reloadChunks(x,y,z);
	}
	
	public MapHandler getMapHandler()
	{
		return this.world.getMapGenerator();
	}
	
	public Chunk getChunkByIndex(int x,int y,int z)
	{
		return this.world.getChunkStorage().getChunkByIndex(x, y,z);
	}

	public Vector3d getCameraCenter()
	{
		return this.world.getCameraCenter();
	}
	public void updateCameraCenter(double x,double y,double z)
	{
		this.world.updateCameraCenter(x,y,z);
	}
	
	/**
	 * Notifies existing neighbours of the chunk <c> that the chunk <c> have been added to the world.
	 */
	public void notifyNeighbours(Chunk c)
	{
		for(int x=-1;x<=1;x++)
		{
			for(int y=-1;y<=1;y++)
			{
				for(int z=-1;z<=1;z++)
				{
					if(x!=0||y!=0||z!=0) {
						Chunk cn=this.world.getChunkStorage().getChunkByIndex(c.getX()+x,c.getY()+y,c.getZ()+z);
						if(cn!=null) cn.notifyNeighbourAdded(-x,-y,-z);
					}
				}
			}
		}
	}
	
	/**
	 * Notifies existing neighbours of the chunk <c> that the chunk <c> have been removed of the world.
	 */
	public void notifyNeighboursRemove(Chunk c)
	{
		for(int x=-1;x<=1;x++)
		{
			for(int y=-1;y<=1;y++)
			{
				for(int z=-1;z<=1;z++)
				{
					if(x!=0||y!=0||z!=0) {
						Chunk cn=this.world.getChunkStorage().getChunkByIndex(c.getX()+x,c.getY()+y,c.getZ()+z);
						if(cn!=null) cn.notifyNeighbourRemoved(-x,-y,-z);
					}
				}
			}
		}
	}
}

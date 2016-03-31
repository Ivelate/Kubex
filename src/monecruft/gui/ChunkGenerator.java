package monecruft.gui;

import java.util.LinkedList;

import monecruft.shaders.VoxelShaderProgram;
import monecruft.storage.ByteArrayPool;

public class ChunkGenerator extends Thread
{
	public static final int MAX_CHUNKS_LOADED=9;
	private LinkedList<ChunkGenRequest> requestList=new LinkedList<ChunkGenRequest>();
	private LinkedList<ChunkStoreRequest> cleanList=new LinkedList<ChunkStoreRequest>();
	
	private WorldFacade WF;
	private boolean endRequested=false;
	
	public ChunkGenerator(WorldFacade wf)
	{
		this.WF=wf;
	}
	@Override
	public void run()
	{
		threadMainLoop:
		while(!endRequested){
			ChunkGenRequest request=null;
			ChunkStoreRequest cleanChunk=null;
			synchronized(this){
				while((requestList.size()==0||WF.getAddListSize()>=MAX_CHUNKS_LOADED)&&this.cleanList.size()==0){
					try {
						if(ByteArrayPool.getUncleanedArraysCount()>0) ByteArrayPool.recycleCleanArray(ByteArrayPool.cleanArray(ByteArrayPool.getArrayUncleaned()));
						else wait();
						if(endRequested) break threadMainLoop;
					} catch (InterruptedException e) {}
				}
				if(requestList.size()>this.cleanList.size()) request=requestList.getFirst();
				else cleanChunk=cleanList.poll();
			}
			
			if(request!=null){
				Chunk c=new Chunk(request.getChunkx(),request.getChunky(),request.getChunkz(),this.WF);
				//c.genUpdateBuffer()
				boolean addPet=false;
				synchronized(this){
					if(this.requestList.size()==0||!request.equals(requestList.getFirst())) c.fullClean();
					else{
						this.requestList.removeFirst();
						addPet=true;
					}
				}
				if(addPet)this.WF.addChunkAddPetition(c);
				request=null;
			}
			else{
				performChunkStore(cleanChunk);
			}
		}
	
		//If we are exiting, store remaining chunks
		for(ChunkStoreRequest req: this.cleanList) performChunkStore(req);
	
		this.dispose();
		
	}
	private void performChunkStore(ChunkStoreRequest cleanChunk)
	{
		if(cleanChunk.chunkCubes.isTrueStorage()){
			this.WF.getMapHandler().getFileManager().storeChunk(cleanChunk.chunkCubes.getArray(), cleanChunk.chunkx,cleanChunk.chunky,cleanChunk.chunkz,cleanChunk.initcializedFlag);
		}
		else this.WF.getMapHandler().getFileManager().storeChunk(cleanChunk.chunkCubes.get(0, 0,0), cleanChunk.chunkx,cleanChunk.chunky,cleanChunk.chunkz,cleanChunk.initcializedFlag);
		
		cleanChunk.chunkCubes.dispose();
	}
	public synchronized void addChunkStoreRequest(ChunkStoreRequest req)
	{
		cleanList.add(req);
		notifyAll();
	}
	public synchronized void generateChunk(int x,int y,int z)
	{
		boolean exists=false;
		for(ChunkGenRequest cgr : this.requestList) {
			if(cgr.getChunkx()==x&&cgr.getChunky()==y&&cgr.getChunkz()==z){
				exists=true;
				break;
			}
		}
		if(!exists)
		{
			this.requestList.add(new ChunkGenRequest(x,y,z));
			notifyAll();
		}
	}
	public synchronized void removeChunk(int x,int y,int z)
	{
		int i=-1;
		for(ChunkGenRequest cgr : this.requestList) {
			i++;
			if(cgr.getChunkx()==x&&cgr.getChunky()==y&&cgr.getChunkz()==z){
				this.requestList.remove(i);
				break;
			}
		}
	}
	public synchronized void fullClean(boolean block)
	{
		this.endRequested=true;
		this.notifyAll();
		if(block){
			try {
				wait();
			} catch (InterruptedException e) {}
		}
	}
	private void dispose()
	{
		this.requestList.clear();
		this.WF=null;
		this.requestList=null;
		synchronized(this){
			this.notifyAll();
		}
	}
}

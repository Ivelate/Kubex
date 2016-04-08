package monecruft.gui;

import java.util.LinkedList;

public class ChunkStorer extends Thread
{
	private LinkedList<ChunkStoreRequest> cleanList=new LinkedList<ChunkStoreRequest>();
	private boolean endRequested=false;
	private WorldFacade WF;
	
	public ChunkStorer(WorldFacade WF)
	{
		this.WF=WF;
	}
	
	@Override
	public void run()
	{
		threadMainLoop:
		while(!endRequested)
		{
			ChunkStoreRequest cleanChunk=null;
			synchronized(this)
			{
				while(this.cleanList.size()==0)
				{
					try {
						wait();
					} catch (InterruptedException e) {}
					if(endRequested) break threadMainLoop;
				}
				cleanChunk=cleanList.poll();
			}
			
			performChunkStore(cleanChunk);
		}
		
		//If we are exiting, store remaining chunks
		
		synchronized(this)
		{
			while(this.cleanList.size()>0){
				ChunkStoreRequest req=this.cleanList.poll();
				performChunkStore(req);
			}
			notifyAll();
		}
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
}

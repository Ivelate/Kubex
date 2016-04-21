package kubex.gui;

import java.util.LinkedList;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Chunk Storer thread. Manages all I/O over the FileManager in a different thread than the main one, preventing blocks.
 */
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
			
			//Stores a chunk store request
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
	
	/**
	 * Stores a chunk store request in a file, managing if it contains an array or just a uniform byte value, and acts accordingly.
	 */
	private void performChunkStore(ChunkStoreRequest cleanChunk)
	{
		if(cleanChunk.chunkCubes.isTrueStorage()){
			this.WF.getMapHandler().getFileManager().storeChunk(cleanChunk.chunkCubes.getArray(), cleanChunk.chunkx,cleanChunk.chunky,cleanChunk.chunkz,cleanChunk.initcializedFlag);
		}
		else this.WF.getMapHandler().getFileManager().storeChunk(cleanChunk.chunkCubes.get(0, 0,0), cleanChunk.chunkx,cleanChunk.chunky,cleanChunk.chunkz,cleanChunk.initcializedFlag);
		
		cleanChunk.chunkCubes.dispose();
	}
	
	/**
	 * Adds a chunk store petition to the request list
	 */
	public synchronized void addChunkStoreRequest(ChunkStoreRequest req)
	{
		cleanList.add(req);
		notifyAll();
	}
	
	/**
	 * Disposes this thread, waits until its termination if <block> is especified.
	 */
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

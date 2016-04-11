package monecruft.gui;

import java.util.LinkedList;

import monecruft.shaders.VoxelShaderProgram;
import monecruft.storage.ByteArrayPool;

/**
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Chunk Generator thread. Initcializes chunks in a different thread than the main one to prevent blocks
 */
public class ChunkGenerator extends Thread
{
	public static final int MAX_CHUNKS_LOADED=9;
	private LinkedList<ChunkGenRequest> requestList=new LinkedList<ChunkGenRequest>();
	
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
			synchronized(this){
				while((requestList.size()==0||WF.getAddListSize()>=MAX_CHUNKS_LOADED)){
					try {
						if(ByteArrayPool.getUncleanedArraysCount()>0) ByteArrayPool.recycleCleanArray(ByteArrayPool.cleanArray(ByteArrayPool.getArrayUncleaned())); //This thread spends most of its time idle
																																									//on 2D maps, so this free time cam be used to
																																									//Reset at 0 some pooled dirty arrays.
						else wait();
						if(endRequested) break threadMainLoop;
					} catch (InterruptedException e) {}
				}
				request=requestList.getFirst();
			}
			

			//Creates a new chunk based on the data specified in the request. This will initcialize it with the values provided by the map generator or the data provided by the file manager.
			Chunk c=new Chunk(request.getChunkx(),request.getChunky(),request.getChunkz(),this.WF);

			boolean addPet=false;
			synchronized(this){
				//In the time spent creating the chunk, we may have received a request to delete it. If so, its simply doesn't added to the chunk add list of the world, and cleaned in the act.
				if(this.requestList.size()==0||!request.equals(requestList.getFirst())) c.fullClean();
				else{
					this.requestList.removeFirst();
					addPet=true;
				}
			}
			if(addPet)this.WF.addChunkAddPetition(c);
			request=null;
		}
	
		this.dispose();
		
	}
	
	/**
	 * Adds a chunk generation request for the chunk x,y,z
	 */
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
	
	/**
	 * If the chunk x,y,z is in the request list, removes it so it never gets generated.
	 */
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
	
	/**
	 * Disposes the thread, waits to its termination if <block> is true
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
	
	/**
	 * Frees all resources used by this thread
	 */
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

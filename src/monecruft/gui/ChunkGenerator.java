package monecruft.gui;

import java.util.LinkedList;

import monecruft.shaders.VoxelShaderProgram;

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
			ChunkGenRequest request;
			synchronized(this){
				while(requestList.size()==0||WF.getAddListSize()>=MAX_CHUNKS_LOADED){
					try {
						wait();
						if(endRequested) break threadMainLoop;
					} catch (InterruptedException e) {}
				}
				request=requestList.getFirst();
			}
			Chunk c=new Chunk(request.getChunkx(),request.getChunky(),request.getChunkz(),this.WF);
			//c.genUpdateBuffer();
			synchronized(this){
				if(this.requestList.size()==0||!request.equals(requestList.getFirst())) c.fullClean();
				else{
					this.requestList.removeFirst();
					this.WF.addChunkAddPetition(c);
				}
			}
		}
		this.dispose();
		
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

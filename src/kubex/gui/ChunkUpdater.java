package kubex.gui;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.ListIterator;

import kubex.entity.Player;
import kubex.shaders.VoxelShaderProgram;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Chunk Update Thread. Generates, for each requested Chunk, it's drawing vertex buffer, so main thread will only have to upload it.
 * Generates first the buffers of the chunks nearest to the player.
 */
public class ChunkUpdater extends Thread
{
	private LinkedList<Chunk> requestList=new LinkedList<Chunk>();
	private Player player;
	private double savedX,savedZ;
	
	private boolean endRequested=false;
	
	public ChunkUpdater(Player p)
	{
		this.player=p;
		this.savedX=p.getX();
		this.savedZ=p.getZ();
	}
	@Override
	public void run()
	{
		threadMainLoop:
		while(!endRequested){
			Chunk request;
			synchronized(this){
				while(requestList.size()==0){
					try {
						wait();
						if(endRequested) break threadMainLoop;
					} catch (InterruptedException e) {}
				}
				do{
				request=requestList.getFirst();
				this.requestList.removeFirst();
				request.setUpdateFlag(false);
				}while(request.isDeleted()&&requestList.size()>0); //If the chunk is deleted from the world, we don't need to generate its buffer. We get the next request instead.
			}
			
			if(!request.isDeleted()){
				request.genUpdateBuffer(); //Generates the chunk buffer.
			}
		}
		this.dispose();
		
	}
	
	/**
	 * Inserts a chunk into the update list, wich puts it at a queue to generate its vertex buffer.
	 * Inserts the chunk in that list in function of the distance to the player, priorizing nearest.
	 * If the player has gone far from the saved player position, sorts the list again in function of the new position.
	 */
	public synchronized void updateChunk(Chunk c)
	{
		c.setUpdateFlag(true); //The chunk's buffer is going to be generated, so all changes in this chunk from now will not make it to be added to this list again.
	
		if(this.requestList.size()==0) this.requestList.addFirst(c); //If list is empty, we simply add the chunk
		else if(this.requestList.size()==1){ //If size is only 1, we order them easily
			Chunk co=this.requestList.getFirst();
			if(getSemiDist(this.player,co)>getSemiDist(this.player,c)) this.requestList.addFirst(c);
			else this.requestList.addLast(c);
		}
		else{

			Chunk cf=this.requestList.getFirst();
			Chunk cl=this.requestList.getLast();
			double dc=getSemiDist(this.player,c);

			if(getSemiDist(this.player,cf)>dc) this.requestList.addFirst(c); //If the distance from this chunk to the player is less than the distance of the nearest chunk to date, this chunk will be first.
			else if(getSemiDist(this.player,cl)>dc) {//If the distance from this chunk to the player is less than the maximum distance, the chunk is in between
				double dist=(this.savedX-player.getX())*(this.savedX-player.getX()) + (this.savedZ-player.getZ())*(this.savedZ-player.getZ());
				if(dist>1024){ //We don't re sort the list every time a new chunk is added, we only do it when the player has gone away from the original saved point, so we assume the list is very unordered.
							   //Before this distance is reached, the list may be bad ordered, but not too bad, is almost unnoticeable.
					this.requestList.add(c);
					Collections.sort(this.requestList, new Comparator<Chunk>(){ //Sorts the entire list in function of the current distance to the player
						@Override
						public int compare(Chunk o1, Chunk o2) {
							double d1=getSemiDist(player,o1);
							double d2=getSemiDist(player,o2);
							return d1>d2?1:(d1==d2?0:-1);
						}
						
					});

					
					//Clean rubbish
					ListIterator<Chunk> it=this.requestList.listIterator(this.requestList.size());
					while(it.hasPrevious()&&(it.previous().isDeleted())){ //If the farthest chunks away had been removed (Unloaded from world) generating its buffer is unnecesary: We clear them from the list
																		  //to save space.
						it.remove();
					}
					this.savedX=this.player.getX();
					this.savedZ=this.player.getZ();
				}
				else{ 
					//If we dont sort the list, we simply add the chunk, ordering it in function to the distance to the player, assuming (Maybe incorrectly, but we dont care) a ordered list.
					ListIterator<Chunk> iter=this.requestList.listIterator();
					while(getSemiDist(this.player,iter.next())<dc);
					iter.previous();iter.add(c);
				}
			}
			else this.requestList.addLast(c); //If the distance is greater than the maximum distance to date, adds it last.
		}
		
		notifyAll();
	}
	
	/**
	 * Distance from the player to the chunk, without sqrt. We do it only based on x and z distances: y distances will only be used to make higher chunks upload first.
	 */
	private double getSemiDist(Player p,Chunk c)
	{
		double xcomp=p.getX()-c.getX()*Chunk.CHUNK_DIMENSION;
		double zcomp=p.getZ()-c.getZ()*Chunk.CHUNK_DIMENSION;
		return xcomp*xcomp + zcomp*zcomp - 0.01f*c.getY();
	}
	
	/**
	 * Marks this thread to be closed
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
	 * Disposes its thread resources
	 */
	private void dispose()
	{
		//this.requestList.clear();
		this.requestList=null;
		synchronized(this){
			this.notifyAll();
		}
	}
}

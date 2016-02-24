package monecruft.gui;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.ListIterator;

import monecruft.entity.Player;
import monecruft.shaders.VoxelShaderProgram;
import monecruft.utils.MiddleSavingLinkedList;

public class ChunkUpdater extends Thread
{
	static int cont=0;
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
				}while(request.isDeleted()&&requestList.size()>0);
			}
			
			if(!request.isDeleted()){
				request.genUpdateBuffer();
			}
		}
		this.dispose();
		
	}
	public synchronized void updateChunk(Chunk c)
	{
		this.updateChunk(c,false);
	}
	public synchronized void updateChunk(Chunk c,boolean first)
	{
		c.setUpdateFlag(true);
		/*boolean exists=false;
		/*for(Chunk cr : this.requestList) {
			if(cr==c){
				exists=true;
				break;
			}
		}*/
		/*if(!exists)
		{
			if(first)this.requestList.addFirst(c);
			else this.requestList.add(c);
			notifyAll();
		}*/
		/*if(this.requestList.size()==0) this.requestList.addFirst(c);
		else if(this.requestList.size()==1){
			Chunk co=this.requestList.getFirst();
			if(getSemiDist(this.player,co)>getSemiDist(this.player,c)) this.requestList.addFirst(c);
			else this.requestList.addLast(c);
		}
		else{
			Chunk cf=this.requestList.getFirst();
			Chunk cl=this.requestList.getLast();
			float dc=getSemiDist(this.player,c);
			if(getSemiDist(this.player,cf)>dc) this.requestList.addFirst(c);
			else if(getSemiDist(this.player,cl)>dc) this.requestList.addMiddle(c);
			else this.requestList.addLast(c);
		}*/
	
		if(this.requestList.size()==0) this.requestList.addFirst(c);
		else if(this.requestList.size()==1){
			Chunk co=this.requestList.getFirst();
			if(getSemiDist(this.player,co)>getSemiDist(this.player,c)) this.requestList.addFirst(c);
			else this.requestList.addLast(c);
		}
		else{

			Chunk cf=this.requestList.getFirst();
			Chunk cl=this.requestList.getLast();
			double dc=getSemiDist(this.player,c);

			if(getSemiDist(this.player,cf)>dc) this.requestList.addFirst(c);
			else if(getSemiDist(this.player,cl)>dc) {
				double dist=(this.savedX-player.getX())*(this.savedX-player.getX()) + (this.savedZ-player.getZ())*(this.savedZ-player.getZ());
				if(dist>1024){
					this.requestList.add(c);
					Collections.sort(this.requestList, new Comparator<Chunk>(){
						@Override
						public int compare(Chunk o1, Chunk o2) {
							double d1=getSemiDist(player,o1);
							double d2=getSemiDist(player,o2);
							return d1>d2?1:(d1==d2?0:-1);
						}
						
					});

					/*LinkedList<Chunk> ll=new LinkedList<Chunk>();
					while(this.requestList.size()>0)
					{
						Chunk ch=this.requestList.removeFirst();
						int pos=0;
						while(pos<ll.size()&& getSemiDist(player,ch)>getSemiDist(player,ll.get(pos))){
							pos++;
						}
						ll.add(pos,ch);
					}
					this.requestList=ll;*/
					//Clean rubbish
					ListIterator<Chunk> it=this.requestList.listIterator(this.requestList.size());
					while(it.hasPrevious()&&(it.previous().isDeleted())){
						it.remove();
					}
					this.savedX=this.player.getX();
					this.savedZ=this.player.getZ();
				}
				else{
					ListIterator<Chunk> iter=this.requestList.listIterator();
					while(getSemiDist(this.player,iter.next())<dc);
					iter.previous();iter.add(c);
					Chunk current=null;
					while(iter.hasNext()&& getSemiDist(this.player,(current=iter.next()))>=dc);
				}
			}
			else this.requestList.addLast(c);
		}
		
		//Chunk c=this.requestList.getFirst()
		notifyAll();
	}
	private double getSemiDist(Player p,Chunk c)
	{
		double xcomp=p.getX()-c.getX()*Chunk.CHUNK_DIMENSION;
		double zcomp=p.getZ()-c.getZ()*Chunk.CHUNK_DIMENSION;
		return xcomp*xcomp + zcomp*zcomp;
		//return Math.abs(p.getX()-c.getX()*Chunk.CHUNK_DIMENSION) + Math.abs(p.getZ()-c.getZ()*Chunk.CHUNK_DIMENSION);
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
		//this.requestList.clear();
		this.requestList=null;
		synchronized(this){
			this.notifyAll();
		}
	}
}

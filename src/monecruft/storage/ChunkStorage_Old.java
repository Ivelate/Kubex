package monecruft.storage;


import ivengine.properties.Cleanable;
import monecruft.gui.Chunk;

public class ChunkStorage_Old implements Cleanable{
	private OrderedLinkedList<OrderedLinkedList<OrderedLinkedList<Chunk>>> chunkList=new OrderedLinkedList<OrderedLinkedList<OrderedLinkedList<Chunk>>>();
	
	private boolean iterChanged=false;
	private boolean iterChanged2=false;
	
	public ChunkStorage_Old()
	{
		
	}
	public void addChunk(int x,int y,int z,Chunk c)
	{
		OrderedLinkedList<OrderedLinkedList<Chunk>> aux;
		if((aux=chunkList.getElementWithKey(x))==null) chunkList.add((aux=new OrderedLinkedList<OrderedLinkedList<Chunk>>()), x);
		
		OrderedLinkedList<Chunk> aux2;
		if((aux2=aux.getElementWithKey(z))==null) aux.add((aux2=new OrderedLinkedList<Chunk>()), z);
		
		aux2.add(c, y);
	}
	public Chunk getChunk(float xf,float yf,float zf)
	{
		int x=(int)Math.floor(xf);int y=(int)Math.floor(yf);int z=(int)Math.floor(zf);
		OrderedLinkedList<OrderedLinkedList<Chunk>> aux;
		if((aux=chunkList.getElementWithKey(x))==null) return null;
		
		OrderedLinkedList<Chunk> aux2;
		if((aux2=aux.getElementWithKey(z))==null) return null;
		
		return aux2.getElementWithKey(y);
	}
	public void removeChunk(int x,int y,int z)
	{
		OrderedLinkedList<OrderedLinkedList<Chunk>> aux;
		if((aux=chunkList.getElementWithKey(x))!=null)
		{
			OrderedLinkedList<Chunk> aux2;
			if((aux2=aux.getElementWithKey(z))!=null)
			{
				Chunk ch;
				if((ch=aux2.getElementWithKey(y))!=null)
				{
					ch.fullClean();
					aux2.removeLastGetted();
					if(aux2.getSize()==0) 
					{
						aux.removeLastGetted();
						if(aux.getSize()==0) this.chunkList.removeLastGetted();
					}
				}
			
			}
		}
	}
	/*				 ITERADORES 			*/
	/**
	 * Inicia el iterador
	 * @return false si la tabla es vacia, true en otro caso
	 */
	public boolean initIter()
	{
		if(this.chunkList.initIter())
		{
			this.chunkList.next();
			iterChanged=true;
			iterChanged2=true;
			return true;
		}
		return false;
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
		if(this.chunkList.currentEl()==null) return null;
		else
		{
			if(iterChanged){
				this.chunkList.currentEl().initIter();
				this.chunkList.currentEl().next();
				iterChanged=false;
			}
			if(this.chunkList.currentEl().currentEl()==null) 
			{
				this.chunkList.next();
				this.iterChanged=true;
				this.iterChanged2=true;
				return next();
			}
			else
			{
				if(iterChanged2){
					this.chunkList.currentEl().currentEl().initIter();
					this.chunkList.currentEl().currentEl().next();
					iterChanged2=false;
				}
				if(this.chunkList.currentEl().currentEl().currentEl()==null) 
				{
					this.chunkList.currentEl().next();
					this.iterChanged2=true;
					return next();
				}
				else
				{
					Chunk c=this.chunkList.currentEl().currentEl().currentEl();
					this.chunkList.currentEl().currentEl().next();
					return c;
				}
				
			}
		}
	}
	@Override
	public void fullClean() {
		this.chunkList.fullClean();
		this.chunkList=null;
	}
	
}

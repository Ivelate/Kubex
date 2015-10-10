package monecruft.storage;

import ivengine.properties.Cleanable;


/**
 * Dynamic linked list, with constant cost operations referred to the iterator position. 
 * @author Ivelate
 */
public class OrderedLinkedList<E extends Cleanable> implements Cleanable
{
	private int size;
	protected KeyCelda<E> first=null;
	protected KeyCelda<E> itObj=null;
	private KeyCelda<E> getPoint=null;
	private boolean initedIterator=false;
	public OrderedLinkedList()
	{
		this.size=0;
	}
	/**
	 * Añade un elemento ordenado por su clave
	 */
	public void add(E e,int key)
	{
		if(this.first==null)
		{
			KeyCelda<E> c=new KeyCelda<E>(e,key);
			this.first=c;
			this.size=1;
		}
		else
		{
			KeyCelda<E> aux=first;
			if(first.getKey()>key){
				this.first=new KeyCelda<E>(e,key);
				this.first.setNext(aux);
				aux.setAnt(this.first);
				this.size++;
			}
			else if(first.getKey()<key){
				boolean insertado=false;
				while(aux.getNext()!=null)
				{
					aux=aux.getNext();
					if(aux.getKey()>key){
						KeyCelda<E> ains=new KeyCelda<E>(e,key);
						aux.getAnt().setNext(ains);ains.setAnt(aux.getAnt());
						aux.setAnt(ains);ains.setNext(aux);
						this.size++;
						insertado=true;
					}
					else if(aux.getKey()==key){insertado=true;}
				}
				if(!insertado){
					KeyCelda<E> ains=new KeyCelda<E>(e,key);
					aux.setNext(ains);ains.setAnt(aux);
					this.size++;
				}
			}
		}
	}
	/**
	 * Borra el elemento apuntado por el iterador actual (Coste cte)
	 */
	public void removeCurrentEl()
	{
		if(this.itObj.getAnt()==null)
		{
			this.first=this.itObj.getNext();
			if(this.first!=null) this.itObj.getNext().setAnt(null);
			this.itObj.fullClean();
			this.itObj=this.first;
			this.size--;
		}
		else if(this.itObj.getNext()==null)
		{
			this.itObj.getAnt().setNext(null);
			this.itObj.fullClean();
			this.itObj=null;
			this.size--;
		}
		else
		{
			KeyCelda<E> aux=this.itObj;
			this.itObj.getAnt().setNext(this.itObj.getNext());
			this.itObj.getNext().setAnt(this.itObj.getAnt());
			this.itObj=this.itObj.getNext();
			aux.fullClean();
			aux=null;
			this.size--;
		}
		this.getPoint=null;
	}
	public E getElementWithKey(int key)
	{
		if(this.first==null) return null;
		KeyCelda<E> aux=this.first;
		do
		{
			if(aux.getKey()>key){
				return null;
			}
			else if(aux.getKey()==key){this.getPoint=aux;return aux.getEl();}
		}while((aux=aux.getNext())!=null);
		return null;
	}
	public void removeLastGetted()
	{
		if(this.getPoint!=null)
		{
			if(this.getPoint.getAnt()==null)
			{
				this.first=this.getPoint.getNext();
				if(this.first!=null) this.getPoint.getNext().setAnt(null);
				this.getPoint.fullClean();
				this.getPoint=this.first;
				this.size--;
			}
			else if(this.getPoint.getNext()==null)
			{
				this.getPoint.getAnt().setNext(null);
				this.getPoint.fullClean();
				this.getPoint=null;
				this.size--;
			}
			else
			{
				KeyCelda<E> aux=this.getPoint;
				this.getPoint.getAnt().setNext(this.getPoint.getNext());
				this.getPoint.getNext().setAnt(this.getPoint.getAnt());
				this.getPoint=this.getPoint.getNext();
				aux.fullClean();
				aux=null;
				this.size--;
			}
			this.getPoint=null;
		}
	}
	/**
	 * Obtiene el tamaño de la tabla (Coste cte)
	 */
	public int getSize()
	{
		return this.size;
	}
	/*				 ITERADORES 			*/
	/**
	 * Inicia el iterador
	 * @return false si la tabla es vacia, true en otro caso
	 */
	public boolean initIter()
	{
		this.itObj=null;
		if(this.first==null) return false;
		this.initedIterator=true;
		return true;
	}
	/**
	 * @return false no hay siguiente elemento en el iterador o este es nulo, true en otro caso
	 */
	public boolean hasNext()
	{
		if(this.initedIterator&&this.first!=null) return true;
		else if(this.itObj==null) return false;
		else if(this.itObj.getNext()==null) return false;
		return true;
	}
	/**
	 * @return el siguiente elemento del iterador
	 */
	public E next()
	{
		if(this.initedIterator)
		{
			this.initedIterator=false;
			this.itObj=this.first;
		}
		else 
		{
			if(this.itObj==null) return null;
			this.itObj=this.itObj.getNext();
		}
		if(this.itObj==null) return null;
		return this.itObj.getEl();
	}
	/**
	 * @return el elemento actual del iterador
	 */
	public E currentEl()
	{
		if(this.itObj!=null) return this.itObj.getEl();		
		return null;
	}
	/**
	 * Vacia la lista completamente (Coste lineal)
	 */
	@Override
	public void fullClean()
	{
		if(this.first!=null)
		{
			fullClean(this.first);
		}
		this.first=null;
		this.itObj=null;
		this.getPoint=null;
	}
	private void fullClean(KeyCelda<E> c)
	{
		c.setAnt(null);
		if(c.getEl()!=null) c.getEl().fullClean();
		c.setEl(null);
		if(c.getNext()!=null) fullClean(c.getNext());
		c.setNext(null);
	}
}

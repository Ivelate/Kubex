package monecruft.storage;

import ivengine.properties.Cleanable;


/**
 * Tool used for Dynamic lists
 * @author Ivelate
 */
public class KeyCelda<E> implements Cleanable
{
	private E MyEl;
	private int key;
	private KeyCelda<E> next;
	private KeyCelda<E> ant;
	public KeyCelda(E e,int key)
	{
		this.ant=null;
		this.next=null;
		this.key=key;
		this.MyEl=e;
	}
	public KeyCelda(E e,int key,KeyCelda<E> c)
	{
		this.ant=c;
		this.MyEl=e;
		this.key=key;
		this.next=null;
	}
	public E getEl()
	{
		return this.MyEl;
	}
	public KeyCelda<E> getNext() {
		return this.next;
	}
	public KeyCelda<E> getAnt() {
		return this.ant;
	}
	@Override
	public void fullClean()
	{
		this.MyEl=null;
		this.next=null;
		this.ant=null;
	}
	public void setNext(KeyCelda<E> c)
	{
		this.next=c;
	}
	public void setAnt(KeyCelda<E> c)
	{
		this.ant=c;
	}
	public void setEl(E myEl) {
		MyEl = myEl;
	}
	public int getKey()
	{
		return this.key;
	}
}

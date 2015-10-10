package monecruft.utils;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class MiddleSavingLinkedList<T>
{
	private class Node
	{
		public Node front=null;
		public Node back=null;
		
		public T value;
		
		public Node(T value){
			this.value=value;
		}
		
		/*public void setFront(Node<T> front){
			this.front=front;
		}
		public void setBack(Node<T> back){
			this.back=back;
		}
		public Node<T> getFront(){
			return this.front;
		}
		public Node<T> getBack(){
			return this.back;
		}
		public T value()
		{
			return 
		}*/
	}
	private Node first=null;
	private Node middle=null;
	private Node last=null;	
	private int size=0;
	
	private boolean firstIter=true;
	private Node currentIter;
	
	public MiddleSavingLinkedList()
	{
		
	}
	public void addFirst(T data){
		if(this.size==0){
			Node n=new Node(data);
			this.first=n;
			this.middle=n;
			this.last=n;
		}
		else if(this.size==1){
			Node n=new Node(data);
			n.front=this.last;
			this.last.back=n;
			
			this.first=n;
			this.middle=n;
		}
		else{
			Node n=new Node(data);
			n.front=this.first;
			this.first.back=n;
			this.first=n;
			
			if(this.size%2==1) this.middle=this.middle.back;
		}
		this.size++;
	}
	public void addLast(T data){
		if(this.size==0){
			Node n=new Node(data);
			this.first=n;
			this.middle=n;
			this.last=n;
		}
		else if(this.size==1){
			Node n=new Node(data);
			n.back=this.first;
			this.first.front=n;
			
			this.last=n;
		}
		else{
			Node n=new Node(data);
			n.back=this.last;
			this.last.front=n;
			this.last=n;
			
			if(this.size%2==0) this.middle=this.middle.front;
		}
		this.size++;
	}
	public void addMiddle(T data){
		if(this.size==0){
			Node n=new Node(data);
			this.first=n;
			this.middle=n;
			this.last=n;
		}
		else if(this.size==1){
			Node n=new Node(data);
			n.front=this.last;
			this.last.back=n;
			
			this.first=n;
			this.middle=n;
		}
		else{
			Node n=new Node(data);
			n.back=this.middle;
			n.front=this.middle.front;
			this.middle.front.back=n;
			this.middle.front=n;
			
			if(this.size%2==0) this.middle=n;
		}
		this.size++;
	}
	public T getFirst(){
		return this.first==null?null:this.first.value;
	}
	public T getLast(){
		return this.last==null?null:this.last.value;
	}
	public T getMiddle(){
		return this.middle==null?null:this.middle.value;
	}
	public void removeFirst(){
		if(this.size==0) return;
		else if(this.size==1){
			this.first=null;
			this.middle=null;
			this.last=null;
		}
		else if(this.size==2){
			this.first=this.last;
			this.middle=this.last;
			this.last.back=null;
		}
		else{
			this.first=this.first.front;
			this.first.back=null;
			
			if(this.size%2==0) this.middle=this.middle.front;
		}
		this.size--;
	}
	public void removeLast(){
		if(this.size==0) return;
		else if(this.size==1){
			this.first=null;
			this.middle=null;
			this.last=null;
		}
		else if(this.size==2){
			this.last=this.first;
			this.first.front=null;
		}
		else{
			this.last=this.last.back;
			this.last.front=null;
			
			if(this.size%2==1) this.middle=this.middle.back;
		}
		this.size--;
	}
	public int size(){
		return this.size;
	}
	/*public String toString()
	{
		String toRet="";
		Node n=this.first;
		while(n!=null){
			toRet=toRet+n.value +" - ";
			n=n.front;
		}
		return toRet;
	}*/
	/*public static void main(String[] args){
		MiddleSavingLinkedList<Integer> table=new MiddleSavingLinkedList<Integer>();
		for(int i=0;i<100;i++){
			double r=Math.random();
			if(r<0.2){
				System.out.println("ADD FIRST "+i);
				table.addFirst(i);
			}
			else if(r<0.4){
				System.out.println("ADD LAST "+i);
				table.addLast(i);
			}
			else if(r<0.6){
				System.out.println("ADD MIDDLE "+i);
				table.addMiddle(i);
			}
			else if(r<0.8){
				System.out.println("REM FIRST");
				table.deleteFirst();
			}
			else{
				System.out.println("REM LAST");
				table.deleteLast();
			}
			System.out.println(table.getMiddle());System.out.println(table);
		}
	}*/
	/*public void initIter() {
		firstIter=true;
		currentIter=this.first;
	}
	public void hasNext() {
		firstIter=true;
		currentIter=this.first;
	}*/
	/*private class MiddleSavingLinkedListIterator implements ListIterator<T>
	{
		private boolean first=true;
		private Node current;
		public MiddleSavingLinkedListIterator(Node first)
		{
			this.current=first;
		}
		@Override
		public boolean hasNext() {
			return first?current!=null:this.current.front!=null;
		}

		@Override
		public T next() {
			if(first){
				first=false;
			}
			else{
				current=current.front;
			}
			return current.value;
		}
		public void remove() {
		    current.back=current.front;
		}
		@Override
		public void add(T arg0) {
			//NOT SUPPORTED
		}
		@Override
		public boolean hasPrevious() {
			return current.back!=null;
		}
		@Override
		public int nextIndex() {
			//NOT SUPPORTED
			return -1;
		}
		@Override
		public T previous() {
			return current.back
		}
		@Override
		public int previousIndex() {
			// TODO Auto-generated method stub
			return 0;
		}
		@Override
		public void set(T arg0) {
			// TODO Auto-generated method stub
			
		}
	}*/
}

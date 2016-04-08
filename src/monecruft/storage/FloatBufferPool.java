package monecruft.storage;

import java.nio.FloatBuffer;
import java.util.LinkedList;

import org.lwjgl.BufferUtils;

import monecruft.gui.Chunk;

public class FloatBufferPool 
{
	private static int defSize=0;
	private static int maxCap=0;
	private static float bufferNum=0;
	
	private static LinkedList<FloatBuffer> content=null;
	
	private static boolean ended=false;
	
	public static void init(int defaultSize,int maxCapacity){
		if(defSize!=defaultSize){
			content=new LinkedList<FloatBuffer>();
		}
		defSize=defaultSize;
		maxCap=maxCapacity;
		bufferNum=0;
	}
	public static FloatBuffer getBuffer(){
		//System.out.println(content.size()+" "+bufferNum);
		synchronized(content) {
		if(content.size()==0&&bufferNum<maxCap){
			bufferNum++;
			return BufferUtils.createFloatBuffer(defSize);
		}
		while(content.size()==0){
			try {
				if(ended) return null;
				content.wait();
			} catch (InterruptedException e) {}
		}
		return content.removeFirst();
		}
	}
	public static void recycleBuffer(FloatBuffer fb){
		//if(content.size()<maxCap){
		synchronized(content) {
			fb.clear();
			content.add(fb);
			content.notify();
		}
		//}
	}
	public static void fullClean(){
		synchronized(content){
			ended=true;
			content.notifyAll();
		}
		
	}
}

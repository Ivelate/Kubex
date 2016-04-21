package kubex.storage;

import java.nio.FloatBuffer;
import java.util.LinkedList;

import org.lwjgl.BufferUtils;

import kubex.gui.Chunk;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Static float buffer pool
 */
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
	public static FloatBuffer getBuffer()
	{
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
		synchronized(content) {
			fb.clear();
			content.add(fb);
			content.notify();
		}
	}
	public static void fullClean(){
		synchronized(content){
			ended=true;
			content.notifyAll();
		}
		
	}
}

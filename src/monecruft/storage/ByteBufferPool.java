package monecruft.storage;

import java.nio.ByteBuffer;
import java.util.LinkedList;

import org.lwjgl.BufferUtils;

public class ByteBufferPool 
{
	private static int defSize;
	private static int maxCap;
	private static LinkedList<ByteBuffer> content;
	public static void init(int defaultSize,int maxCapacity){
		defSize=defaultSize;
		maxCap=maxCapacity;
		content=new LinkedList<ByteBuffer>();
	}
	public synchronized static ByteBuffer getBuffer(){
		if(content.size()==0){
			return BufferUtils.createByteBuffer(defSize);
		}
		else return content.removeFirst();
	}
	public synchronized static void recycleBuffer(ByteBuffer fb){
		if(content.size()<maxCap){
			fb.clear();
			content.add(fb);
		}
	}
}

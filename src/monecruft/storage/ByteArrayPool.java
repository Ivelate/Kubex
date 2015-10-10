package monecruft.storage;

import java.nio.FloatBuffer;
import java.util.LinkedList;

import org.lwjgl.BufferUtils;

import monecruft.gui.Chunk;

public class ByteArrayPool 
{
	private static int defSize=0;
	private static int maxCap=0;
	private static int bufferNum=0;
	
	private static LinkedList<byte[][][]> content=null;
	public static void init(int defaultSize,int maxCapacity){
		if(defSize!=defaultSize){
			content=new LinkedList<byte[][][]>();
		}
		defSize=defaultSize;
		maxCap=maxCapacity;
		bufferNum=0;
	}
	public static byte[][][] getArray(){
		synchronized(content) {
		if(content.size()==0&&bufferNum<maxCap){
			bufferNum++;
			return new byte[defSize][defSize][defSize];
		}
		while(content.size()==0){
			try {
				content.wait();
			} catch (InterruptedException e) {}
		}
		byte[][][] ret=content.removeFirst();
		for(int x=0;x<ret.length;x++){
			for(int y=0;y<ret[0].length;y++){
				for(int z=0;z<ret[0][0].length;z++){
					ret[x][y][z]=0;
				}
			}
				
		}
		return ret;
		}
	}
	public static byte[][][] getArrayUncleaned(){
		synchronized(content) {
		if(content.size()==0&&bufferNum<maxCap){
			bufferNum++;
			return new byte[defSize][defSize][defSize];
		}
		while(content.size()==0){
			try {
				content.wait();
			} catch (InterruptedException e) {}
		}
		return content.removeFirst();
		}
	}
	public static void recycleArray(byte[][][] array){
		//if(content.size()<maxCap){
		synchronized(content) {
			content.add(array);
			content.notify();
		}
		//}
	}
}

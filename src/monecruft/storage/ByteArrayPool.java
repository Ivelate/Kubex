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
	private static LinkedList<byte[][][]> cleanedContent=null;
	
	public static void init(int defaultSize,int maxCapacity){
		if(defSize!=defaultSize){
			content=new LinkedList<byte[][][]>();
			cleanedContent=new LinkedList<byte[][][]>();
		}
		defSize=defaultSize;
		maxCap=maxCapacity;
		bufferNum=0;
	}
	public static byte[][][] getArray()
	{
		synchronized(content) 
		{			
			if(cleanedContent.size()!=0) return cleanedContent.removeFirst();
			if(content.size()!=0) return cleanArray(content.removeFirst());
			
			if(bufferNum>=maxCap)
			{
				while(content.size()==0&&cleanedContent.size()==0){
					try {
						content.wait();
					} catch (InterruptedException e) {}
				}
				if(cleanedContent.size()!=0) return cleanedContent.removeFirst();
				if(content.size()!=0) return cleanArray(content.removeFirst());
			}
			else bufferNum++; //And getting out to get the new array
		}
		
		return new byte[defSize][defSize][defSize];
	}
	public static byte[][][] cleanArray(byte[][][] ret)
	{
		for(int x=0;x<ret.length;x++){
			for(int y=0;y<ret[0].length;y++){
				for(int z=0;z<ret[0][0].length;z++){
					ret[x][y][z]=0;
				}
			}
			
		}
		return ret;
	}
	public static byte[][][] getArrayUncleaned()
	{
		System.out.println(bufferNum-content.size()-cleanedContent.size());
		synchronized(content) 
		{
			if(content.size()!=0) return content.removeFirst();
			if(cleanedContent.size()!=0) return cleanedContent.removeFirst();
			
			if(bufferNum>=maxCap)
			{
				while(content.size()==0&&cleanedContent.size()==0){
					try {
						content.wait();
					} catch (InterruptedException e) {}
				}
				if(content.size()!=0) return content.removeFirst();
				if(cleanedContent.size()!=0) return cleanedContent.removeFirst();
			}
			else bufferNum++; //And getting out to get the new array
		}
		
		return new byte[defSize][defSize][defSize];
	}
	
	public static int getUncleanedArraysCount()
	{
		return content.size();
	}
	
	public static void recycleArray(byte[][][] array)
	{
		synchronized(content) {
			content.add(array);
			System.out.println(bufferNum-content.size()-cleanedContent.size());
			content.notify();
		}
	}
	public static void recycleCleanArray(byte[][][] array)
	{
		synchronized(content) {
			cleanedContent.add(array);
			System.out.println(bufferNum-content.size()-cleanedContent.size());
			content.notify();
		}
	}
}

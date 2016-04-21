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
 * Static pool which contains three-dimensional arrays of bytes, both clean (Filled with 0s) and dirty ones, returning the most appropiate one in each ocassion.
 */
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
	
	/**
	 * Gets a clean array from the pool. If no clean arrays are availlable, gets a dirty one instead. If the maximum number of arrays had been reached, waits here instead. 
	 * If the maximum number hadn't been reached yet but there is no arrays on the pool, creates one new
	 */
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
	
	/**
	 * Fills with 0s an array <ret> and returns it.
	 */
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
	
	/**
	 * Gets a dirty array. If no dirty arrays are availlable, gets a clean one instead. If the maximum number of arrays had been reached, waits here instead. 
	 * If the maximum number hadn't been reached yet but there is no arrays on the pool, creates one new 
	 */
	public static byte[][][] getArrayUncleaned()
	{
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
	
	/**
	 * Recycles an array. If the array is clean it will be stored in the clean arrays list. If it isn't, it will be stored on the dirty array list.
	 */
	public static void recycleArray(byte[][][] array)
	{
		synchronized(content) {
			content.add(array);
			content.notify();
		}
	}
	public static void recycleCleanArray(byte[][][] array)
	{
		synchronized(content) {
			cleanedContent.add(array);
			content.notify();
		}
	}
}

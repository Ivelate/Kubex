package kubex.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Region File class. Contains both the file stream, the original file, its chunk coordinates and the lookup table, cached in a buffer
 */
public class RegionFile
{
	public RandomAccessFile file;
	public final File originFile;
	public final int x,y,z;
	
	public byte[] lookupCache;
	
	public RegionFile(File originFile,int x,int y,int z) throws FileNotFoundException{
		this.originFile=originFile;
		this.x=x;
		this.y=y;
		this.z=z;
		this.lookupCache=null;
		updateRandomAccessFile();
	}
	
	/**
	 * Reopens the random access file stream and stores the lookup in the cache
	 */
	public void updateRandomAccessFile() throws FileNotFoundException
	{
		this.file=new RandomAccessFile(this.originFile,"rw");
		storeLookupInCache();
	}
	
	/**
	 * Loads the entire lookup into the cache
	 */
	public void storeLookupInCache()
	{
		try {
			if(this.lookupCache!=null){
				this.file.seek(0);
				this.file.read(this.lookupCache,0,FileManager.LOOKUP_SIZE);
			}
		} catch (IOException e) {}
	}
	
	/**
	 * Writes a buffer of data <data> in the pos <pos> of the cached lookup. This will not store that data in disk, only in the local cache
	 */
	public void writeInLookup(int pos,byte[] data)
	{
		for(int i=0;i<data.length;i++)
		{
			this.lookupCache[pos+i]=data[i];
		}
	}
	
	/**
	 * Reads into a buffer of data <data> the pos <pos> of the cached lookup.
	 */
	public void loadFromLookup(int pos,byte[] data)
	{
		for(int i=0;i<data.length;i++)
		{
			data[i]=this.lookupCache[pos+i];
		}
	}
	
	/**
	 * Disposes all the resources used, closing the file handle.
	 */
	public void fullClean()
	{
		try {
			this.file.close();
		} catch (IOException e) {}
	}
}
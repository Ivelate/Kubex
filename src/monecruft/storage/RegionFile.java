package monecruft.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class RegionFile
	{
		public RandomAccessFile file;
		public final File originFile;
		public final int x,y,z;
		
		public byte[] lookupCache;
		
		public RegionFile(File originFile,int x,int y,int z) throws FileNotFoundException{
			//this.file=new RandomAccessFile(originFile,"rw");
			this.originFile=originFile;
			this.x=x;
			this.y=y;
			this.z=z;
			this.lookupCache=null;
			updateRandomAccessFile();
		}
		public void updateRandomAccessFile() throws FileNotFoundException
		{
			this.file=new RandomAccessFile(this.originFile,"rw");
			storeLookupInCache();
		}
		public void storeLookupInCache()
		{
			try {
				if(this.lookupCache!=null){
					this.file.seek(0);
					this.file.read(this.lookupCache,0,FileManager.LOOKUP_SIZE);
				}
			} catch (IOException e) {}
		}
		public void writeInLookup(int pos,byte[] data)
		{
			for(int i=0;i<data.length;i++)
			{
				this.lookupCache[pos+i]=data[i];
			}
		}
		public void loadFromLookup(int pos,byte[] data)
		{
			for(int i=0;i<data.length;i++)
			{
				data[i]=this.lookupCache[pos+i];
			}
		}
		public void fullClean()
		{
			try {
				this.file.close();
			} catch (IOException e) {}
		}
	}
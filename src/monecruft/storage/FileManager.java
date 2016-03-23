package monecruft.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import monecruft.gui.Chunk;

public class FileManager 
{
	public static void main(String[] args) throws IOException
	{
		File f=new File("patata");
		f.mkdir();
		FileManager fm=new FileManager(f);
		byte[][][] data=new byte[1][1][1]; 
		data[0][0][0]=1;System.out.println("DATA "+data[0][0][0]);
		fm.storeChunk(data, 0, 0, 0);
		fm.storeChunk(data, 1, 0, 0);
		fm.storeChunk(data, 2, 0, 0);
		data[0][0][0]=4;
		fm.storeChunk(data, 3, 0, 0);
		fm.storeChunk((byte)10, 2, 0, 0); 
		fm.storeChunk(data, -1, 0, 0);
		fm.loadChunk(data, 0, 0, 0); System.out.println("DATA "+data[0][0][0]);
		fm.loadChunk(data, 2, 0, 0); System.out.println("DATA "+data[0][0][0]);
		fm.loadChunk(data, 3, 0, 0); System.out.println("DATA "+data[0][0][0]);
		fm.loadChunk(data, -1, 0, 0); System.out.println("DATA "+data[0][0][0]);
		fm.loadChunk(data, -17897, 0, 0); System.out.println("DATA "+data[0][0][0]);
	}
	private static final int REGION_SIZE=16;
	private static final int SECTOR_SIZE=1024;
	private static final int LOOKUP_SIZE=4*REGION_SIZE*REGION_SIZE*REGION_SIZE;
	private static final byte[] BYTE_ZERO_BUFFER=new byte[1024];
	
	private final byte[] chunkBuffer=new byte[32768]; //Max chunk size
	private final byte[] lookupBuffer=new byte[16384];
	private final byte[] sectorBuffer=new byte[SECTOR_SIZE];
	private File baseRoute;
	private RegionFile currentFile=null;
	
	public FileManager(File baseRoute)
	{
		this.baseRoute=baseRoute;
	}
	
	public boolean loadChunk(byte[][][] chunkCubes,int chunkx,int chunky,int chunkz)
	{
		int x=(int)Math.floor((float)(chunkx)/REGION_SIZE); int y=(int)Math.floor((float)(chunky)/REGION_SIZE); int z=(int)Math.floor((float)(chunkz)/REGION_SIZE); 
		int cx=posMod(chunkx,REGION_SIZE); int cy=posMod(chunky,REGION_SIZE); int cz=posMod(chunkz,REGION_SIZE);
		if(this.currentFile==null||this.currentFile.x!=x||this.currentFile.y!=y||this.currentFile.z!=z)
		{
			this.currentFile=openRegionFile(x,y,z);
		}
		
		//Get file direction for chunk in lookup table
		try {
			this.currentFile.file.seek((cx*REGION_SIZE*REGION_SIZE + cy*REGION_SIZE + cz)*4);
			byte[] lookupData=new byte[4];
			this.currentFile.file.read(lookupData);
			
			byte chunkSize=lookupData[0];
			System.out.println("CHUNK SISE "+chunkSize);
			if(chunkSize==0) return false;
			else if(chunkSize==-1){
				//Fill the return array with the default values specified.
				for(int chx=0;chx<chunkCubes.length;chx++)
				{
					for(int chy=0;chy<chunkCubes.length;chy++)
					{
						for(int chz=0;chz<chunkCubes.length;chz++)
						{
							chunkCubes[chx][chy][chz]=lookupData[3];
						}
					}
				}
			}
			else{
			
				int chunkLocation=(lookupData[1]<<16 | lookupData[2]<<8 | lookupData[3])& 0xFFFFFF; 
				System.out.println("CHUNK LOC "+chunkLocation);
			
				this.currentFile.file.seek(LOOKUP_SIZE+(chunkLocation)*SECTOR_SIZE);
				byte[] sizebuf=new byte[3];
				this.currentFile.file.read(sizebuf); //|TODO testing ayyy lmao
				int chunksize=((sizebuf[0] << 16) | (sizebuf[0] << 8) | (sizebuf[0])) & 0xFFFFFF;
				this.currentFile.file.read(chunkBuffer, 0, chunksize);
				this.decompress(chunkCubes,chunkBuffer,chunksize);
			}
			return true;
		} 
		catch (IOException e)
		{
			System.err.println("Error when reading region file");
			e.printStackTrace();
			System.exit(1);
		} catch (DataFormatException e) {
			System.err.println("Error when decompressing file");
			e.printStackTrace();
			System.exit(1);
		}
		
		return false; //Never reached
	}
	
	/**
	 * Returns an always-positive mod of a value <val>, with a mod <mod>
	 */
	private int posMod(int val,int mod)
	{
		int m=val%mod;
		if(m<0) m+=mod;
		return m;
	}
	
	/**
	 * Stores the chunk specified by the provided indexes, with the value of <chunkCubes>
	 */
	public void storeChunk(byte[][][] chunkCubes,int chunkx,int chunky,int chunkz){
		this.storeChunk(chunkCubes, (byte)0,chunkx, chunky, chunkz);
	}
	/**
	 * Stores the chunk specified by the provided indexes, with a constant value <constantValue>
	 */
	public void storeChunk(byte constantValue,int chunkx,int chunky,int chunkz){
		this.storeChunk(null, constantValue,chunkx, chunky, chunkz);
	}
	/**
	 * Stores the chunk specified by the provided indexes, with the value of <chunkCubes>. If <chunkCubes> is null, stores it
	 * with a constant value of <constantValue>
	 */
	public void storeChunk(byte[][][] chunkCubes,byte constantValue,int chunkx,int chunky,int chunkz)
	{
		int x=(int)Math.floor((float)(chunkx)/REGION_SIZE); int y=(int)Math.floor((float)(chunky)/REGION_SIZE); int z=(int)Math.floor((float)(chunkz)/REGION_SIZE);
		int cx=posMod(chunkx,REGION_SIZE); int cy=posMod(chunky,REGION_SIZE); int cz=posMod(chunkz,REGION_SIZE);
		if(this.currentFile==null||this.currentFile.x!=x||this.currentFile.y!=y||this.currentFile.z!=z)
		{
			this.currentFile=openRegionFile(x,y,z);
		}
		
		//Get file direction for chunk in lookup table
		try {
			this.currentFile.file.seek((cx*REGION_SIZE*REGION_SIZE + cy*REGION_SIZE + cz)*4);
			byte[] lookupData=new byte[4];
			this.currentFile.file.read(lookupData);
			
			boolean updateLookup=false;
			byte chunkSize=lookupData[0];
			int chunkLocation=(lookupData[1]<<16 | lookupData[2]<<8 | lookupData[3])& 0xFFFFFF; 
			
			int compressedSize=-1;
			if(chunkCubes!=null){
				compressedSize=this.compress(chunkCubes, this.chunkBuffer);
				if(compressedSize==-1) constantValue=chunkCubes[0][0][0];
			}
			
			if(compressedSize<0)
			{
				if(chunkSize!=0&&chunkSize!=-1)
				{
					//Fuck
					this.currentFile.file.close();
					rewriteFileCroppingSector(this.currentFile.originFile,chunkLocation,chunkSize);
					this.currentFile.updateRandomAccessFile();
					updateLookup=true;
				}
				
				if(chunkSize!=-1 || chunkLocation!= (constantValue &0xFF)){
					chunkSize=-1;
					chunkLocation=constantValue & 0xFF;
					updateLookup=true;
				}
			}
			else
			{
				if(chunkSize==0) {
					//If size is 0, we append it to the end of the file
					chunkLocation=(int)((this.currentFile.file.length()-LOOKUP_SIZE)/SECTOR_SIZE);
					chunkSize=1;
					updateLookup=true;
				}
				
				int sectorsSize=(3+compressedSize)/SECTOR_SIZE;
				if(sectorsSize!=chunkSize && chunkSize!=0) {
					this.currentFile.file.close();
					rewriteFileExtendingSector(this.currentFile.originFile,chunkLocation,chunkSize,(byte)sectorsSize);
					this.currentFile.updateRandomAccessFile();
				}
				
				//Move cursor to chunk sector
				this.currentFile.file.seek(LOOKUP_SIZE+(chunkLocation)*SECTOR_SIZE);
				byte[] sizeBuf=new byte[3];
				sizeBuf[0]=(byte)((sectorsSize >> 16) &0xFF);
				sizeBuf[1]=(byte)((sectorsSize >> 8) &0xFF);
				sizeBuf[2]=(byte)((sectorsSize) &0xFF);
				this.currentFile.file.write(sizeBuf);
				this.currentFile.file.write(this.chunkBuffer, 0, compressedSize);
				
				//Write 
				//this.sectorBuffer[0]=chunkCubes[0][0][0]; //|TODO testing ayy lmao
				//this.currentFile.file.write(this.sectorBuffer,0,1);
				//Write size of chunk
			}

			if(updateLookup)
			{
				this.currentFile.file.seek((cx*REGION_SIZE*REGION_SIZE + cy*REGION_SIZE + cz)*4);
				lookupData[0]=chunkSize;
				lookupData[1]=(byte)((chunkLocation >> 16) &0xFF);
				lookupData[2]=(byte)((chunkLocation >> 8) &0xFF);
				lookupData[3]=(byte)((chunkLocation) &0xFF);
				this.currentFile.file.write(lookupData);
			}
		} 
		catch (IOException e)
		{
			System.err.println("Error when storing region file");
			e.printStackTrace();
			System.exit(1);
		} catch (DataFormatException e) {
			System.err.println("Error when compressing file");
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Opens a region file, if exists. If it doesn't exists, creates it.
	 */
	private RegionFile openRegionFile(int x,int y,int z)
	{
		File sectorFile=new File(baseRoute,"f_"+x+"_"+y+"_"+z+".kxw");
		if(!sectorFile.exists()) createRegionFile(sectorFile);
		try {
			return new RegionFile(sectorFile,x,y,z);
		} 
		catch (FileNotFoundException e) {
			//Should not happen
			System.err.println("Error when opening region file");
			e.printStackTrace();
			System.exit(1);
		}
		
		return null; //Never reached
	}
	
	private void createRegionFile(File file)
	{
		FileOutputStream out=null;
		try {
			file.createNewFile();
			out=new FileOutputStream(file);
			
			//Inits file with lookup table full of zeroes
			int remainingLookupSize=LOOKUP_SIZE;
			while(remainingLookupSize>0){
				if(remainingLookupSize<1024) out.write(BYTE_ZERO_BUFFER, 0, remainingLookupSize);
				else out.write(BYTE_ZERO_BUFFER);
				
				remainingLookupSize-=1024;
			}

		} catch (IOException e) {
			System.err.println("Error when creating region file");
			e.printStackTrace();
			System.exit(1);
		}
		finally{
			try {
				out.close();
			} catch (IOException e) {}
		}
		
	}
	
	/**
	 * Rewrites <originFile> completelly, erasing the sector occuped by the chunk in <location> and size <bsize>, and updating the 
	 * lookup table accordingly
	 */
	private void rewriteFileCroppingSector(File originFile,int location,byte bsize)
	{
		int size=bsize<0?bsize+128:bsize;
		
		File newFile=new File(originFile.getAbsolutePath()+"tmp");
		FileInputStream in=null;
		FileOutputStream out=null;
		try {
			newFile.createNewFile();
			in=new FileInputStream(originFile);
			out=new FileOutputStream(newFile);
			in.read(this.lookupBuffer);
			
			//Parse lookup buffer and readjust it
			for(int b=0;b<this.lookupBuffer.length;b+=4)
			{
				byte len=this.lookupBuffer[b];
				
				int loc=(lookupBuffer[b+1]<<16 | lookupBuffer[b+2]<<8 | lookupBuffer[b+3])& 0xFFFFFF; 
				if(loc>location){
					if(len==0||len==-1) continue;
					loc=loc-size;
					lookupBuffer[b+1]=(byte)((loc >> 16) & 0xFF);
					lookupBuffer[b+2]=(byte)((loc >> 8) & 0xFF);
					lookupBuffer[b+3]=(byte)((loc) & 0xFF);
				}
				else if(loc==location){
					lookupBuffer[b]=(byte)(-1);
					lookupBuffer[b+1]=0;
					lookupBuffer[b+2]=0;
					lookupBuffer[b+3]=0;
				}
			}
			out.write(this.lookupBuffer);
			
			//Rewrite all sectors, excepting the ones deleted
			int sectorCont=0;
			boolean end=false;
			while(!end){
				int sectorReaded=0;
				while(sectorReaded<SECTOR_SIZE){
					int dataReaded=in.read(this.sectorBuffer,sectorReaded,this.sectorBuffer.length-sectorReaded);
					if(dataReaded==-1) {end=true;break;}
					sectorReaded+=dataReaded;
				}
				if(!end&&(sectorCont<location||sectorCont>=location+size)){
					out.write(this.sectorBuffer);
				}
			}
			
			//Rename file
			out.close();
			in.close();
			originFile.delete();
			newFile.renameTo(originFile);
		} 
		catch (IOException e) {
			System.err.println("Error when rewriting region file");
			e.printStackTrace();
			System.exit(1);
		}
		finally
		{
			try{in.close();}catch(Exception e){}
			try{out.close();}catch(Exception e){}
		}
	}
	
	/**
	 * Rewrites <originFile> completelly, extending or shorting the sector occuped by the chunk in <location> 
	 * and size <blastsize> (new size <bcurrentsize>), and updating the lookup table accordingly
	 */
	private void rewriteFileExtendingSector(File originFile,int location,byte blastsize,byte bcurrentsize)
	{
		int lastsize=blastsize<0?blastsize+128:blastsize;
		int currentsize=bcurrentsize<0?bcurrentsize+128:bcurrentsize;
		int diff=currentsize-lastsize;
		
		File newFile=new File(originFile.getAbsolutePath()+"tmp");
		FileInputStream in=null;
		FileOutputStream out=null;
		try {
			newFile.createNewFile();
			in=new FileInputStream(originFile);
			out=new FileOutputStream(newFile);
			in.read(this.lookupBuffer);
			
			//Parse lookup buffer and readjust it
			for(int b=0;b<this.lookupBuffer.length;b+=4)
			{
				byte len=this.lookupBuffer[b];
				if(len==0||len==-1) continue;
				
				int loc=(lookupBuffer[b+1]<<16 | lookupBuffer[b+2]<<8 | lookupBuffer[b+3])& 0xFFFFFF; 
				if(loc>location){
					loc=loc+diff;
					lookupBuffer[b+1]=(byte)((loc >> 16) & 0xFF);
					lookupBuffer[b+2]=(byte)((loc >> 8) & 0xFF);
					lookupBuffer[b+3]=(byte)((loc) & 0xFF);
				}
				else if(loc==location){
					lookupBuffer[b]=bcurrentsize;
				}
			}
			out.write(this.lookupBuffer);
			
			//Rewrite all sectors, excepting the ones deleted
			int sectorCont=0;
			boolean end=false;
			while(!end){
				int sectorReaded=0;
				while(sectorReaded<SECTOR_SIZE){
					int dataReaded=in.read(this.sectorBuffer,sectorReaded,this.sectorBuffer.length-sectorReaded);
					if(dataReaded==-1) {end=true;break;}
					sectorReaded+=dataReaded;
				}
				if(!end){
					//If diff in size is > 0 and it needs to be expanded...
					if(sectorReaded==location+lastsize-1 &&diff>=0){
						out.write(this.sectorBuffer);
						for(int i=1;i<diff;i++) out.write(BYTE_ZERO_BUFFER);
					}
					//else write normally except if diff is < 0 and this part needs to be culled
					else if(sectorReaded<location+currentsize||sectorReaded>=location+lastsize)
					{
						out.write(this.sectorBuffer);
					}
					
				}
			}
			
			//Rename file
			out.close();
			in.close();
			originFile.delete();
			newFile.renameTo(originFile);
		} 
		catch (IOException e) {
			System.err.println("Error when rewriting region file");
			e.printStackTrace();
			System.exit(1);
		}
		finally
		{
			try{in.close();}catch(Exception e){}
			try{out.close();}catch(Exception e){}
		}
	}
	
	private void decompress(byte[][][] chunkCubes,byte[] buff,int chunkSize) throws DataFormatException
	{
		//ZLIB
		Inflater inflater=new Inflater();
		inflater.setInput(buff, 0, chunkSize);

		int size=inflater.inflate(buff);
		inflater.end();
		//RLE
		//Order -> y,x,z
		
		int x=0;int y=0;int z=0;
		for(int i=0;i<size;i+=2)
		{
			for(int c=0;c<buff[i];c++)
			{
				chunkCubes[x][y][z]=buff[i+1];
				
				y++;
				if(y>chunkCubes.length){
					y=0;
					x++;
					if(x>chunkCubes.length){
						x=0;
						z++;
					}
				}
			}
		}
		
		//Done
	}
	
	/**
	 * Performs RLE and ZLIB compression on chunkCubes, returns them on buff, and the size of the compressed data
	 * Returns -1 if all chunkCubes are filled with the same type of cube.
	 */
	private int compress(byte[][][] chunkCubes,byte[] buff) throws DataFormatException
	{
		//RLE
		//Order -> y,x,z
		int currentSize=0;
		byte numEqualCubes=0;
		byte groupingCube=0;
		boolean sameCube=true;
		
		for(int y=0;y<chunkCubes.length;y++)
		{
			for(int x=0;x<chunkCubes.length;x++)
			{
				for(int z=0;z<chunkCubes.length;z++)
				{
					if(numEqualCubes==0){
						groupingCube=chunkCubes[x][y][z];
						numEqualCubes=1;
					}
					else{
						if(chunkCubes[x][y][z]==groupingCube){
							numEqualCubes++;
							//Overflow!
							if(numEqualCubes==0){
								buff[currentSize]=(byte)(numEqualCubes-1);
								buff[currentSize+1]=groupingCube;
								currentSize+=2;
								numEqualCubes=1;
							}
						}
						else
						{
							buff[currentSize]=numEqualCubes;
							buff[currentSize+1]=groupingCube;
							currentSize+=2;
							numEqualCubes=1;
							groupingCube=chunkCubes[x][y][z];
							sameCube=false;
						}
					}
				}
			}
		}
		
		if(sameCube) return -1;
				
		//ZLIB
		Deflater deflater = new Deflater();
		deflater.setInput(buff, 0, currentSize);
		deflater.finish();
		
		int size=deflater.deflate(buff);
		deflater.end();
		
		
		return size;
		//Done
	}
	
	private class RegionFile
	{
		public RandomAccessFile file;
		public final File originFile;
		public final int x,y,z;
		public RegionFile(File originFile,int x,int y,int z) throws FileNotFoundException{
			this.file=new RandomAccessFile(originFile,"rw");
			this.originFile=originFile;
			this.x=x;
			this.y=y;
			this.z=z;
		}
		public void updateRandomAccessFile() throws FileNotFoundException
		{
			this.file=new RandomAccessFile(this.originFile,"rw");
		}
	}
}

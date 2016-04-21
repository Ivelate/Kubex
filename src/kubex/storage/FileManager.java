package kubex.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import kubex.KubexSettings;
import kubex.gui.Chunk;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * File manager class. Manages all file I/O existing in the project (Well, excepting the main settings file storing / loading, managed in KubexGame)
 * Stores and loads all chunks using a Region file approximation, compressing them using RLE and ZLIB
 * The lookup tables in the region files will be cached for perfomance
 */
public class FileManager 
{
	public enum ChunkLoadResult{CHUNK_EMPTY,CHUNK_NOT_FOUND,CHUNK_FULL,CHUNK_NORMAL_NOT_INITCIALIZED,CHUNK_NORMAL_INITCIALIZED};
	public static final int REGION_SIZE=8;
	public static final int SECTOR_SIZE=1024;
	public static final int LOOKUP_SIZE=4*REGION_SIZE*REGION_SIZE*REGION_SIZE;
	private static final byte[] BYTE_ZERO_BUFFER=new byte[SECTOR_SIZE];
	
	private final byte[] chunkBuffer=new byte[Chunk.CHUNK_DIMENSION*Chunk.CHUNK_DIMENSION*Chunk.CHUNK_DIMENSION*2]; //Max chunk size
	private final byte[] chunkBuffer2=new byte[chunkBuffer.length];
	
	private final byte[] lookupBuffer=new byte[LOOKUP_SIZE];
	private final byte[] sectorBuffer=new byte[SECTOR_SIZE];
	private File baseRoute; //Current map folder
	
	private RegionFileStorage activeFiles; //Contains all region files opened at once
	
	public FileManager(File baseRoute,int renderDistance)
	{
		this.baseRoute=baseRoute;
		
		int regionfilestoragesize=((renderDistance*2 + 1) / REGION_SIZE) + 2; //Some regions files will be remained open at once to minimize I/O calls. The number of them is equal to
																			  //the number of region files that can be opened at once in a full rendered map in the worst case.
		this.activeFiles=new RegionFileStorage(regionfilestoragesize,1,regionfilestoragesize);
	}
	
	/**
	 * Loads the settings from the settings file located in the map folder into the <settings> object provided
	 */
	public synchronized void getSettingsFromFile(KubexSettings settings)
	{
		File settingsFile=new File(baseRoute,"settings.txt");
		if(settingsFile.exists())
		{
			try {
				Scanner s=new Scanner(settingsFile);
				while(s.hasNextLine())
				{
					String line=s.nextLine();
					String[] content=line.split(":");
					if(content[0].equals("MAP_SEED")){
						settings.MAP_SEED=Long.parseLong(content[1]);
					}
					else if(content[0].equals("PLAYER_X")){
						settings.PLAYER_X=Double.parseDouble(content[1]);
					}
					else if(content[0].equals("PLAYER_Y")){
						settings.PLAYER_Y=Double.parseDouble(content[1]);
					}
					else if(content[0].equals("PLAYER_Z")){
						settings.PLAYER_Z=Double.parseDouble(content[1]);
					}
					else if(content[0].equals("MAP_CODE")){
						settings.MAP_CODE=Integer.parseInt(content[1]);
					}
					else if(content[0].equals("DAY_TIME")){
						settings.DAY_TIME=Float.parseFloat(content[1]);
					}
					else if(content[0].equals("CAM_PITCH")){
						settings.CAM_PITCH=Float.parseFloat(content[1]);
					}
					else if(content[0].equals("CAM_YAW")){
						settings.CAM_YAW=Float.parseFloat(content[1]);
					}
					else if(content[0].equals("DAY_SPEED")){
						settings.DAY_SPEED=Integer.parseInt(content[1]);
					}
					else if(content[0].equals("CUBE_SHORTCUTS")){
						Scanner lineScanner=new Scanner(content[1]);
						for(int i=0;i<settings.CUBE_SHORTCUTS.length;i++){
							settings.CUBE_SHORTCUTS[i]=lineScanner.nextByte();
						}
						lineScanner.close();
					}
					else if(content[0].equals("CUBE_SELECTED")){
						settings.CUBE_SELECTED=Byte.parseByte(content[1]);
					}
				}
				s.close();
			} 
			catch (FileNotFoundException e) {
				System.err.println("Error loading settings file");
				e.printStackTrace();
				System.exit(1);
			}
		}
		else{
			//It doesn't exist! create it / store it
			storeSettingsInFile(settings);
		}
	}
	
	/**
	 * Stores the settings of the <settings> object provided into the settings file located in the map folder. 
	 */
	public synchronized void storeSettingsInFile(KubexSettings settings)
	{
		File settingsFile=new File(baseRoute,"settings.txt");
		
		try 
		{
			if(!settingsFile.exists()) settingsFile.createNewFile();
			PrintWriter f=new PrintWriter(settingsFile,"UTF-8");
			f.println("MAP_SEED:"+settings.MAP_SEED);
			f.println("PLAYER_X:"+settings.PLAYER_X);
			f.println("PLAYER_Y:"+settings.PLAYER_Y);
			f.println("PLAYER_Z:"+settings.PLAYER_Z);
			f.println("MAP_CODE:"+settings.MAP_CODE);
			f.println("DAY_TIME:"+settings.DAY_TIME);
			f.println("CAM_PITCH:"+settings.CAM_PITCH);
			f.println("CAM_YAW:"+settings.CAM_YAW);
			f.println("CUBE_SELECTED:"+settings.CUBE_SELECTED);
			String cubeShortcuts="CUBE_SHORTCUTS:";
			for(int i=0;i<settings.CUBE_SHORTCUTS.length;i++){
				cubeShortcuts=cubeShortcuts+settings.CUBE_SHORTCUTS[i]+" ";
			}
			f.println(cubeShortcuts);
			f.println("DAY_SPEED:"+settings.DAY_SPEED);
			f.close();
		} 
		catch (IOException e) {
			System.err.println("Error storing settings file");
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Loads the chunk <chunkx> <chunky> <chunkz> from disk, inserting its cubes in the array <chunkCubes>
	 */
	public synchronized ChunkLoadResult loadChunk(byte[][][] chunkCubes,int chunkx,int chunky,int chunkz)
	{
		int x=(int)Math.floor((float)(chunkx)/REGION_SIZE); int y=(int)Math.floor((float)(chunky)/REGION_SIZE); int z=(int)Math.floor((float)(chunkz)/REGION_SIZE); 
		int cx=posMod(chunkx,REGION_SIZE); int cy=posMod(chunky,REGION_SIZE); int cz=posMod(chunkz,REGION_SIZE);
		
		RegionFile currentFile=this.activeFiles.getRegionFile(x, y, z);
		if(currentFile==null)
		{
			currentFile=this.activeFiles.setRegionFile(openRegionFile(x,y,z)); //If the region file corresponding to that chunk isnt opened yet, opens it
		}
		
		//Get the file direction for that chunk in the lookup table
		try {
			byte[] lookupData=new byte[4];
			currentFile.loadFromLookup((cx*REGION_SIZE*REGION_SIZE + cy*REGION_SIZE + cz)*4, lookupData);
			
			byte chunkSize=lookupData[0];
			//If chunk isn't exists in file, returns it
			if(chunkSize==0) return ChunkLoadResult.CHUNK_NOT_FOUND;
			//If the chunk has a constant value, fills the return array with the default values specified.
			else if(chunkSize==-1){
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
				if(lookupData[3]==0) return ChunkLoadResult.CHUNK_EMPTY; //If the chunk is filled with 0, it is empty.
				else return ChunkLoadResult.CHUNK_FULL; //If the chunk is not filled with 0s, it is full
			}
			//The chunk is stored in disk
			else{
			
				int chunkLocation=((lookupData[1]&0xFF)<<16 | (lookupData[2]&0xFF)<<8 | (lookupData[3]&0xFF))& 0xFFFFFF; //Gets the location sector of the start of the chunk
				currentFile.file.seek(LOOKUP_SIZE+(chunkLocation)*SECTOR_SIZE); //Goes to that location
				byte[] sizebuf=new byte[3];
				currentFile.file.read(sizebuf); //Reads the size of the chunk, in bytes, and if the chunk has been initcialized (Trees and vegetation generated, or not)
				int chunksize=(((sizebuf[0]&0x7F) << 16) | ((sizebuf[1]&0xFF) << 8) | (sizebuf[2] & 0xFF)) & 0xFFFFFF;
				boolean initcializedFlag= (sizebuf[0] & 0x80)==0x80;
				currentFile.file.read(chunkBuffer, 0, chunksize); //Reads all the bytes of the compressed chunk from disk
				this.decompress(chunkCubes,chunkBuffer,chunksize); //Decompress the chunk
				return initcializedFlag? ChunkLoadResult.CHUNK_NORMAL_INITCIALIZED : ChunkLoadResult.CHUNK_NORMAL_NOT_INITCIALIZED; //returns if the chunk has been initcialized before, or not
			}
		} 
		catch (IOException e)
		{
			System.err.println("Error when reading region file");
			e.printStackTrace();
			System.exit(1); //We are in some thread out there, we cant propagate an exception properly. We simply exit the game showing an error
		} catch (DataFormatException e) {
			System.err.println("Error when decompressing file");
			e.printStackTrace();
			System.exit(1); //We are in some thread out there, we cant propagate an exception properly. We simply exit the game showing an error
		}
		
		return ChunkLoadResult.CHUNK_NOT_FOUND; //Never reached
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
	public void storeChunk(byte[][][] chunkCubes,int chunkx,int chunky,int chunkz,boolean initcializedFlag){
		this.storeChunk(chunkCubes, (byte)0,chunkx, chunky, chunkz,initcializedFlag);
	}
	
	/**
	 * Stores the chunk specified by the provided indexes, with a constant value <constantValue>
	 */
	public void storeChunk(byte constantValue,int chunkx,int chunky,int chunkz,boolean initcializedFlag){
		this.storeChunk(null, constantValue,chunkx, chunky, chunkz,initcializedFlag);
	}
	
	/**
	 * Stores the chunk specified by the provided indexes, with the value of <chunkCubes>. If <chunkCubes> is null, stores it
	 * with a constant value of <constantValue>
	 */
	public synchronized void storeChunk(byte[][][] chunkCubes,byte constantValue,int chunkx,int chunky,int chunkz,boolean initcializedFlag)
	{
		int x=(int)Math.floor((float)(chunkx)/REGION_SIZE); int y=(int)Math.floor((float)(chunky)/REGION_SIZE); int z=(int)Math.floor((float)(chunkz)/REGION_SIZE);
		int cx=posMod(chunkx,REGION_SIZE); int cy=posMod(chunky,REGION_SIZE); int cz=posMod(chunkz,REGION_SIZE);
		
		RegionFile currentFile=this.activeFiles.getRegionFile(x, y, z);
		if(currentFile==null)
		{
			currentFile=this.activeFiles.setRegionFile(openRegionFile(x,y,z));
		}
		
		//Get file direction for chunk in lookup table
		try {
			
			//Load lookup data from cached lookup
			byte[] lookupData=new byte[4];
			currentFile.loadFromLookup((cx*REGION_SIZE*REGION_SIZE + cy*REGION_SIZE + cz)*4, lookupData);
			
			boolean updateLookup=false;
			byte chunkSize=lookupData[0];
			int chunkLocation=((lookupData[1]&0xFF)<<16 | (lookupData[2]&0xFF)<<8 | (lookupData[3]&0xFF))& 0xFFFFFF; 
			
			int compressedSize=-1;
			if(chunkCubes!=null){
				compressedSize=this.compress(chunkCubes, this.chunkBuffer);
				if(compressedSize==-1) constantValue=chunkCubes[0][0][0];
			}
			
			if(compressedSize<0)
			{
				//If the compressed size of the chunk is -1 (Simbolizing it is now an homogeneous chunk) we will crop the region file if the chunk wasn't homogeneous or non existant before,
				//Because we had reserved 1 sector which is now unused
				if(chunkSize!=0&&chunkSize!=-1)
				{
					currentFile.file.close();
					rewriteFileCroppingSector(currentFile.originFile,chunkLocation,chunkSize);
					currentFile.updateRandomAccessFile();
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
				int sectorsSize=((2+compressedSize)/SECTOR_SIZE)+1;
				if(chunkSize==0||chunkSize==-1) {
					//If size is 0, we append it to the end of the file
					chunkLocation=(int)((currentFile.file.length()-LOOKUP_SIZE-1)/SECTOR_SIZE)+1;
					chunkSize=(byte)(sectorsSize);
					updateLookup=true;
				}
				
				if(sectorsSize!=chunkSize && chunkSize!=0 && chunkSize!=-1) {
					currentFile.file.close();
					rewriteFileExtendingSector(currentFile.originFile,chunkLocation,chunkSize,(byte)sectorsSize);
					currentFile.updateRandomAccessFile();
				}
				
				//Move cursor to chunk sector
				currentFile.file.seek(LOOKUP_SIZE+(chunkLocation)*SECTOR_SIZE);
				byte[] sizeBuf=new byte[3];
				sizeBuf[0]=(byte)(((compressedSize >> 16) &0x7F)| (initcializedFlag?0x80:0));
				sizeBuf[1]=(byte)((compressedSize >> 8) &0xFF);
				sizeBuf[2]=(byte)((compressedSize) &0xFF);
				currentFile.file.write(sizeBuf,0,3);
				currentFile.file.write(this.chunkBuffer, 0, compressedSize);
			}

			if(updateLookup)
			{
				long pos=(cx*REGION_SIZE*REGION_SIZE + cy*REGION_SIZE + cz)*4;
				currentFile.file.seek(pos);
				lookupData[0]=chunkSize;
				lookupData[1]=(byte)((chunkLocation >> 16) &0xFF);
				lookupData[2]=(byte)((chunkLocation >> 8) &0xFF);
				lookupData[3]=(byte)((chunkLocation) &0xFF);
				currentFile.file.write(lookupData,0,4);
				currentFile.writeInLookup((int)pos,lookupData); //Lookups are updated both in file and in the cache
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
	
	/**
	 * Creates a region file in <file>, filling the new lookup table with 0s
	 */
	private void createRegionFile(File file)
	{
		FileOutputStream out=null;
		try {
			file.createNewFile();
			out=new FileOutputStream(file);
			
			//Inits file with lookup table full of zeroes
			int remainingLookupSize=LOOKUP_SIZE;
			while(remainingLookupSize>0){
				if(remainingLookupSize<BYTE_ZERO_BUFFER.length) out.write(BYTE_ZERO_BUFFER, 0, remainingLookupSize);
				else out.write(BYTE_ZERO_BUFFER);
				
				remainingLookupSize-=BYTE_ZERO_BUFFER.length;
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
				
				int loc=((lookupBuffer[b+1]&0xFF)<<16 | (lookupBuffer[b+2]&0xFF)<<8 | (lookupBuffer[b+3]&0xFF))& 0xFFFFFF; 
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
			out.write(this.lookupBuffer,0,LOOKUP_SIZE);
			
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
				if(sectorReaded>0&&(sectorCont<location||sectorCont>=location+size)){
					out.write(this.sectorBuffer,0,sectorReaded);
				}
				sectorCont++;
			}
			
			//Rename file
			out.close();
			in.close();
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
				
				int loc=((lookupBuffer[b+1]&0xFF)<<16 | (lookupBuffer[b+2]&0xFF)<<8 | (lookupBuffer[b+3]&0xFF))& 0xFFFFFF; 
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
			out.write(this.lookupBuffer,0,LOOKUP_SIZE);
			
			//Rewrite all sectors, excepting the ones deleted
			int sectorCont=0;
			boolean end=false;
			while(!end){
				int sectorReaded=0;
				while(sectorReaded<SECTOR_SIZE){
					int dataReaded=in.read(this.sectorBuffer,sectorReaded,SECTOR_SIZE-sectorReaded);
					if(dataReaded==-1) {end=true;break;}
					sectorReaded+=dataReaded;
				}
				//If diff in size is > 0 and it needs to be expanded...
				if(sectorCont==location+lastsize-1 &&diff>=0){
					out.write(this.sectorBuffer,0,sectorReaded);
					for(int i=0;i<diff;i++) out.write(BYTE_ZERO_BUFFER,0,SECTOR_SIZE);
				}
				//else write normally except if diff is < 0 and this part needs to be culled
				else if(sectorCont<location+currentsize||sectorCont>=location+lastsize)
				{
					out.write(this.sectorBuffer,0,sectorReaded);
				}
				sectorCont++;
			}
			
			//Rename file
			out.close();
			in.close();
			System.out.println("DEL "+originFile.delete());
			System.out.println("REN "+newFile.renameTo(originFile));
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
	 * Decompresses the compressed data <compressedData> with size <chunkSize> applying ZLIB and then RLE
	 */
	private void decompress(byte[][][] chunkCubes,byte[] compressedData,int chunkSize) throws DataFormatException
	{
		byte[] buff=this.chunkBuffer2;
		//ZLIB
		Inflater inflater=new Inflater();
		inflater.setInput(compressedData, 0, chunkSize);

		int size=inflater.inflate(buff);
		inflater.end();

		//RLE
		//Order -> y,x,z
		int x=0;int y=0;int z=0;
		for(int i=0;i<size;i+=2)
		{
			int cend=buff[i]>0?buff[i] : (int)(buff[i])+256;
			for(int c=0;c<cend;c++)
			{
				chunkCubes[x][y][z]=buff[i+1];
				
				y++;
				if(y>=chunkCubes.length){
					y=0;
					x++;
					if(x>=chunkCubes.length){
						x=0;
						z++;
					}
				}
			}
		}
		
		//Done
	}
	
	/**
	 * Performs RLE and ZLIB compression on <chunkCubes>, stores the result on <compressedStorage>, and returns the size of the compressed data
	 * Returns -1 if all chunkCubes are filled with the same type of cube.
	 */
	private int compress(byte[][][] chunkCubes,byte[] compressedStorage) throws DataFormatException
	{
		//RLE
		//Order -> y,x,z
		int currentSize=0;
		byte numEqualCubes=0;
		byte groupingCube=0;
		boolean sameCube=true;
		byte[] buff=this.chunkBuffer2;
		
		for(int z=0;z<chunkCubes.length;z++)
		{
			for(int x=0;x<chunkCubes.length;x++)
			{
				for(int y=0;y<chunkCubes.length;y++)
				{
					if(numEqualCubes==0){
						groupingCube=chunkCubes[x][y][z];
						numEqualCubes=1;
					}
					else{
						if(chunkCubes[x][y][z]==groupingCube){
							numEqualCubes++;
							//Overflow!
							if(numEqualCubes==-1){
								buff[currentSize]=numEqualCubes;
								buff[currentSize+1]=groupingCube;
								currentSize+=2;
								numEqualCubes=0;
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
		
		if(numEqualCubes!=0){
			buff[currentSize]=numEqualCubes;
			buff[currentSize+1]=groupingCube;
			currentSize+=2;
		}		
		
		if(sameCube) return -1;
		
		//ZLIB
		Deflater deflater = new Deflater();
		deflater.setInput(buff, 0, currentSize);
		deflater.finish();
		
		int size=deflater.deflate(compressedStorage);
		deflater.end();
		
		
		return size;
		//Done
	}
	
	/**
	 * Disposes the resources used, closing all the region files
	 */
	public void fullClean()
	{
		this.activeFiles.fullClean();
	}
}

package monecruft.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.Formatter;
import java.util.Scanner;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import monecruft.MonecruftSettings;
import monecruft.gui.Chunk;

public class FileManager 
{
	/*public static void main(String[] args) throws IOException
	{
		File f=new File("patata");
		f.mkdir();
		FileManager fm=new FileManager(f);
		byte[][][] data=new byte[32][32][32]; 
		byte[][][] data2=new byte[32][32][32]; 
		
		/*data[0][0][0]=1;System.out.println("DATA "+data[0][0][0]);
		fm.storeChunk(data, 0, 0, 0);
		fm.storeChunk(data, 1, 0, 0);
		fm.storeChunk(data, 2, 0, 0);
		data[0][0][0]=4;
		fm.storeChunk(data, 3, 0, 0);
		fm.storeChunk((byte)10, 2, 0, 0); 
		
		data[1][0][0]=5;
		fm.storeChunk(data, -1, 0, 0);
		System.out.println(fm.loadChunk(data, 0, 0, 0)); System.out.println("DATA "+data[0][0][0]);
		System.out.println(fm.loadChunk(data, 2, 0, 0)); System.out.println("DATA "+data[0][0][0]);
		System.out.println(fm.loadChunk(data, 3, 0, 0)); System.out.println("DATA "+data[0][0][0]);
		System.out.println(fm.loadChunk(data, -1, 0, 0)); System.out.println("DATA "+data[0][0][0]);*/

		/*for(int w=0;w<10;w++){
		for(int j=0;j<10;j++){
		for(int i=0;i<10;i++){
			byte dataval=0;
			for(int x=0;x<32;x++) for(int y=0;y<32;y++) for(int z=0;z<32;z++) {
				if(Math.random()<0.05) dataval=(byte)(Math.random()*10);
				data[x][y][z]=dataval;
			}
			if(Math.random()<0.5f)fm.storeChunk(data, j,i,w);
			else fm.storeChunk((byte)(Math.random()*10), j,i,w);
		}
		}
		}
		//for(int x=0;x<32;x++) for(int y=0;y<32;y++) for(int z=0;z<32;z++) data[x][y][z]=(byte)(Math.random()*10);
		//fm.storeChunk(data, 1, 0, 0);

		//for(int x=0;x<32;x++) for(int y=0;y<32;y++) for(int z=0;z<32;z++) data[x][y][z]=(byte)(Math.random()*10);
		//fm.storeChunk(data, 2, 0, 0);
		
		//fm.storeChunk((byte)8, 1, 0, 0);
		//fm.storeChunk(data, 1, 0, 0);
		//data[1][0][0]=2;
		//fm.storeChunk(data, 1, 0, 0);
		fm.loadChunk(data2, 2, 2, 2);
		
		/*System.out.println();
		for(int x=0;x<data2.length;x++)
		{
			for(int y=0;y<data2.length;y++)
			{
				for(int z=0;z<data2.length;z++)
				{
					if(data2[x][y][z]!= data[x][y][z]){
						throw new RuntimeException("AAUUAU");
					}
				}
			}
		}*/
		
/*fm.loadChunk(data, 1, 0, 0);
		
		System.out.println();
		for(int x=0;x<data.length;x++)
		{
			for(int y=0;y<data.length;y++)
			{
				for(int z=0;z<data.length;z++)
				{
					System.out.println(x+" "+y+" "+z+" "+data[x][y][z]);
				}
			}
		}
		
fm.loadChunk(data, 2, 0, 0);
		
		System.out.println();
		for(int x=0;x<data.length;x++)
		{
			for(int y=0;y<data.length;y++)
			{
				for(int z=0;z<data.length;z++)
				{
					System.out.println(x+" "+y+" "+z+" "+data[x][y][z]);
				}
			}
		}*/
		//fm.loadChunk(data, -17897, 0, 0); System.out.println("DATA "+data[0][0][0]);
	//}
	public enum ChunkLoadResult{CHUNK_EMPTY,CHUNK_NOT_FOUND,CHUNK_FULL,CHUNK_NORMAL_NOT_INITCIALIZED,CHUNK_NORMAL_INITCIALIZED};
	public static final int REGION_SIZE=8;
	public static final int SECTOR_SIZE=1024;
	public static final int LOOKUP_SIZE=4*REGION_SIZE*REGION_SIZE*REGION_SIZE;
	private static final byte[] BYTE_ZERO_BUFFER=new byte[SECTOR_SIZE];
	
	private final byte[] chunkBuffer=new byte[Chunk.CHUNK_DIMENSION*Chunk.CHUNK_DIMENSION*Chunk.CHUNK_DIMENSION*2]; //Max chunk size
	private final byte[] chunkBuffer2=new byte[chunkBuffer.length];
	
	private final byte[] lookupBuffer=new byte[LOOKUP_SIZE];
	private final byte[] sectorBuffer=new byte[SECTOR_SIZE];
	private File baseRoute;
	
	private RegionFileStorage activeFiles;
	//private RegionFile currentFile=null;
	
	public FileManager(File baseRoute,int renderDistance)
	{
		this.baseRoute=baseRoute;
		
		int regionfilestoragesize=((renderDistance*2 + 1) / 8) + 2;
		System.out.println(regionfilestoragesize);
		this.activeFiles=new RegionFileStorage(regionfilestoragesize,1,regionfilestoragesize);
	}
	
	public synchronized void getSettingsFromFile(MonecruftSettings settings)
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
	public synchronized void storeSettingsInFile(MonecruftSettings settings)
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
	public synchronized ChunkLoadResult loadChunk(byte[][][] chunkCubes,int chunkx,int chunky,int chunkz)
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
			//currentFile.file.seek((cx*REGION_SIZE*REGION_SIZE + cy*REGION_SIZE + cz)*4);
			byte[] lookupData=new byte[4];
			//currentFile.file.read(lookupData);
			currentFile.loadFromLookup((cx*REGION_SIZE*REGION_SIZE + cy*REGION_SIZE + cz)*4, lookupData);
			
			byte chunkSize=lookupData[0];
			if(chunkSize==0) return ChunkLoadResult.CHUNK_NOT_FOUND;
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
				if(lookupData[3]==0) return ChunkLoadResult.CHUNK_EMPTY;
				else return ChunkLoadResult.CHUNK_FULL;
			}
			else{
			
				int chunkLocation=((lookupData[1]&0xFF)<<16 | (lookupData[2]&0xFF)<<8 | (lookupData[3]&0xFF))& 0xFFFFFF; 
				currentFile.file.seek(LOOKUP_SIZE+(chunkLocation)*SECTOR_SIZE);
				byte[] sizebuf=new byte[3];
				currentFile.file.read(sizebuf); //|TODO testing ayyy lmao
				int chunksize=(((sizebuf[0]&0x7F) << 16) | ((sizebuf[1]&0xFF) << 8) | (sizebuf[2] & 0xFF)) & 0xFFFFFF;
				boolean initcializedFlag= (sizebuf[0] & 0x80)==0x80;
				currentFile.file.read(chunkBuffer, 0, chunksize);
				this.decompress(chunkCubes,chunkBuffer,chunksize);
				return initcializedFlag? ChunkLoadResult.CHUNK_NORMAL_INITCIALIZED : ChunkLoadResult.CHUNK_NORMAL_NOT_INITCIALIZED;
			}
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
			
			//currentFile.file.seek((cx*REGION_SIZE*REGION_SIZE + cy*REGION_SIZE + cz)*4);
			byte[] lookupData=new byte[4]; //************************************************* LOAD FROM LOOKUP INSTEAD
			//currentFile.file.read(lookupData);
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
				//System.out.println("hey"+chunkSize+" "+chunky);
				if(chunkSize!=0&&chunkSize!=-1)
				{
					//System.out.println("afas");
					//Fuck
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
				//for(int i=0;i<compressedSize;i++) System.out.println("CBUFF "+i+" "+chunkBuffer[i]);
				currentFile.file.write(this.chunkBuffer, 0, compressedSize);
				
				//Write 
				//this.sectorBuffer[0]=chunkCubes[0][0][0]; //|TODO testing ayy lmao
				//this.currentFile.file.write(this.sectorBuffer,0,1);
				//Write size of chunk
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
				currentFile.writeInLookup((int)pos,lookupData);
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
		System.out.println("CROP!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"); //if(1==1)throw new RuntimeException(); //System.exit(0);
		//System.exit(0);
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
	 * Rewrites <originFile> completelly, extending or shorting the sector occuped by the chunk in <location> 
	 * and size <blastsize> (new size <bcurrentsize>), and updating the lookup table accordingly
	 */
	private void rewriteFileExtendingSector(File originFile,int location,byte blastsize,byte bcurrentsize)
	{
		System.out.println("EXTEND!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");//if(1==1)throw new RuntimeException();// System.exit(0);
		//System.out.println("RWRT "+location+" "+blastsize);
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
		//for(int i=0;i<size;i+=2) System.out.println("DECOMPR "+buff[i]+" "+buff[i+1]);
		int x=0;int y=0;int z=0;
		for(int i=0;i<size;i+=2)
		{
			int cend=buff[i]>0?buff[i] : (int)(buff[i])+256;
			//System.out.println(chunkSize+" "+cend+" "+buff[i+1]);
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
	 * Performs RLE and ZLIB compression on chunkCubes, returns them on buff, and the size of the compressed data
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
		
		//for(int i=0;i<currentSize;i+=2) System.out.println("COMPR "+buff[i]+" "+buff[i+1]);
		//ZLIB
		Deflater deflater = new Deflater();
		deflater.setInput(buff, 0, currentSize);
		deflater.finish();
		
		int size=deflater.deflate(compressedStorage);
		deflater.end();
		
		
		return size;
		//Done
	}
	
	public void fullClean()
	{
		this.activeFiles.fullClean();
	}
}

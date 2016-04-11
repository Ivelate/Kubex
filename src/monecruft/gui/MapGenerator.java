package monecruft.gui;

import monecruft.blocks.BlockLibrary;
import ivengine.gen.SimplexNoise;

/**
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Forms world terrain using some Simplex Noise functions, and different mixing algorithm for them in function of the map type selected. 
 * Fills the chunks with the cubes corresponding to that procedural world.
 * 
 * Uses both 3D (For map types 3, 4, 5 and 6) and 2D (For 0, 1 and 2) simplex functions. For 2 ones, generates a heightmap for a chunk x,z coordinates, wich only is needed to do once for
 * each chunk column. That's why 2D generation is way faster than 3D, in which this approximation can't be taken and each particular cube has to be sampled separatelly.
 * 
 *  All three simplex functions have a different purpose each:
 *  	- mapBase : Smooth, low res noise. Creates smooth, long hills, but no mountains or terrain details.
 *  	- mapElevation: High res noise, represents the map elevation. If only this function was applied, terrain would be full of mountains or deep water holes.
 *  	- elevationCoef: Low res noise, represents how much map elevation is applied in a particular terrain point. If this function returns a low value, smooth hills caused by
 *  					 mapBase will appear. If it returns a high value, this smooth hills would be disturbed by the mapElevation value, causing scarpate mountains.
 *  
 *  This three simplex noise functions combined will generate a smooth world with some mountains, but not too many.
 *  
 *  This generator is heavily optimized for perfomance: If it detects that a chunk is fully empty, or above the world, it will not bother generating it and return a result like CHUNK_EMPTY, or CHUNK_HIGHER_THAN_HEIGHTMAP.
 *  This will improve world generation speed significantly.
 */
public class MapGenerator 
{
	public static final int SEA_HEIGHT=64; //By default, sea is placed at this height
	public enum ChunkGenerationResult{CHUNK_EMPTY,CHUNK_HIGHER_THAN_HEIGHTMAP,CHUNK_NORMAL}; 
	private SimplexNoise mapBase;
	private SimplexNoise mapElevation;
	private SimplexNoise elevationCoef;
	
	private int beginBlockHeight;
	private int endBlockHeight;
	
	private int[][] savedHeightMap=new int[Chunk.CHUNK_DIMENSION+2][Chunk.CHUNK_DIMENSION+2];
	private int savedHeightMapX=-1;private int savedHeightMapZ=-1;
	private int savedHeightMapMaxY=-1;
	
	private int mapcode=0;
	
	public MapGenerator(int bbegin,int bend,int mapcode,long seed)
	{
		this.mapcode=mapcode;
		this.beginBlockHeight=bbegin;this.endBlockHeight=bend;

		long seed1=seed>>43;
		long seed2=seed>>21;
		long seed3=seed;
		
		this.mapBase=new SimplexNoise(3,0.5,(int)seed1,100);
		this.mapElevation=new SimplexNoise(3,0.5,(int)seed2,10);
		this.elevationCoef=new SimplexNoise(3,0.8,(int)seed3,100);
	}
	
	/**
	 * Fills a chunk array <c> with the world cube values, corresponding to the chunk located in chunk pos x,y,z
	 */
	public ChunkGenerationResult generateChunk(int x,int y,int z,byte[][][] c)
	{
		boolean empty=true;
		byte[][][] ret=c;
		if(mapcode<4){
			generateHeightMap(x,z);
			if(y*Chunk.CHUNK_DIMENSION>savedHeightMapMaxY&&y*Chunk.CHUNK_DIMENSION>SEA_HEIGHT) return ChunkGenerationResult.CHUNK_HIGHER_THAN_HEIGHTMAP; //If the chunk column heightmap max y is less than this current chunk initial y, 
																																						 //We know in advance it's gonna be empty.
		}
		else if(mapcode==4){ //For 3D world functions, there is fixed Y values instead, based on the function used to generate the world, from when all chunks will be empty.
			if(y*Chunk.CHUNK_DIMENSION>200) return ChunkGenerationResult.CHUNK_HIGHER_THAN_HEIGHTMAP;
		}
		else if(mapcode==6){
			if(y*Chunk.CHUNK_DIMENSION > 240) return ChunkGenerationResult.CHUNK_HIGHER_THAN_HEIGHTMAP;
		}
		
		
		for(int cx=0;cx<Chunk.CHUNK_DIMENSION;cx++)
		{
			for(int cy=0;cy<((y==World.HEIGHT-1)?Chunk.CHUNK_DIMENSION-1:Chunk.CHUNK_DIMENSION);cy++)
			{
				for(int cz=0;cz<Chunk.CHUNK_DIMENSION;cz++)
				{
					//Generates the cube using a 2D function if mapcode is a 2D world, or a 3D one if mapcode represents a 3D world.
					if(mapcode<3)ret[cx][cy][cz]=getCubeFromHeightMap(cx,cy+(y*Chunk.CHUNK_DIMENSION),cz);
					else {
						ret[cx][cy][cz]=get3dValue(cx+(x*Chunk.CHUNK_DIMENSION),
							cy+(y*Chunk.CHUNK_DIMENSION), 
							cz+(z*Chunk.CHUNK_DIMENSION))<0 ? (byte)1 : (byte)(0);
							
						//From all 3D worlds, only the Underwater Ruins one has water.
						if(this.mapcode==5 && cy + y*Chunk.CHUNK_DIMENSION<=SEA_HEIGHT+15){
							if(ret[cx][cy][cz]==0)ret[cx][cy][cz]=4;
							if(ret[cx][cy][cz]==1 &&Math.random()<0.01)  ret[cx][cy][cz]=12; //Generate some lights randomly
						}
					}
					
					//Generate some vegetation randomly on top of the grass
					double rand=Math.random();
					if(ret[cx][cy][cz]==0&&cy>1&&ret[cx][cy-1][cz]==1) {
						if(rand<0.1) ret[cx][cy][cz]=(byte)19;
						else if(rand<0.12) ret[cx][cy][cz]=(byte)21;
					}
							
					if(empty&&ret[cx][cy][cz]!=0) empty=false; 
				}
			}
		}
		
		//The last block of the world will be always air
		if(y==World.HEIGHT-1){
			for(int cx=0;cx<Chunk.CHUNK_DIMENSION;cx++){
				for(int cz=0;cz<Chunk.CHUNK_DIMENSION;cz++){
					ret[cx][Chunk.CHUNK_DIMENSION-1][cz]=0;
				}
			}
		}
		return empty?ChunkGenerationResult.CHUNK_EMPTY:ChunkGenerationResult.CHUNK_NORMAL; //If all blocks generated had been equal to 0, return CHUNK_EMPTY
	}
	
	/**
	 * Generate all second-pass objects needed in a chunk (Just trees, but more could be added)
	 */
	public void generateChunkObjects(Chunk c)
	{
		for(int x=0;x<Chunk.CHUNK_DIMENSION;x++)
		{
			for(int z=0;z<Chunk.CHUNK_DIMENSION;z++)
			{
				byte lastCube=c.getCubeAt(x, -1, z);
				if(!BlockLibrary.isSolid(lastCube)) continue;
				for(int y=0;y<Chunk.CHUNK_DIMENSION;y++)
				{
					byte cube=c.getCubeAt(x, y, z);
					if(cube==0&&lastCube==1){
						if(Math.random()<0.005) generateTreeAt(x,y,z,c);
						break;
					}
					lastCube=cube;
				}
			}
		}
	}
	
	/**
	 * Generates a random tree at pos x,y,z of the chunk c
	 */
	private void generateTreeAt(int x,int y,int z,Chunk c)
	{
		if(c.getNaturalLightLevelAt(x, y, z)<(byte)14) return; //Trees need light to grow
		
		int height=(int)(Math.random()*6)+3;
		for(int cy=0;cy<height;cy++)
		{
			c.setCubeAt(x, y+cy, z, (byte)22);
			if(cy==height-1) c.setCubeAt(x, y+height, z, (byte)23);
			int dx=0;int dz=0; int armL=0;
			if(Math.random()*(cy/(float)(height))>0.3f){
				armL=(int)((Math.random()*height*2)/3+1 - (cy*cy/height));
				double dir=Math.random();
				dx=dir<0.25?-1:dir<0.5?1:0;
				dz=dir>0.5?(dir<0.75?-1:1):0;
				for(int i=0;i<armL;i++){
					c.setCubeAt(x+(i*dx), y+cy, z+(i*dz), (byte)22);
				}
			}
			
			int width=cy/(height/2);
			for(int cz=-width-(dz<0?armL:0);cz<=width+(dz>0?armL:0);cz++)
			{
				int dxz=Math.abs(dz<0?cz+armL:dz>0?cz-armL:cz);
				for(int cx=-width+dxz-(dx<0?armL:0);cx<=width-dxz+(dx>0?armL:0);cx++)
				{
					if(c.getCubeAt(x+cx, y+cy, z+cz)==0)c.setCubeAt(x+cx, y+cy, z+cz, (byte)23);
					else if(c.getCubeAt(x+cx, y+cy, z+cz)==22) c.setCubeAt(x+cx, y+cy+1, z+cz, (byte)23);
				}
			}
		}
	}
	
	/**
	 * Generates and stores the heightmap of the chunk position x,z , to improve generation speed in 2D worlds
	 */
	public void generateHeightMap(int x,int z){
		if(x!=this.savedHeightMapX||z!=this.savedHeightMapZ){
			int minY=this.endBlockHeight;
			int maxY=this.beginBlockHeight;
			for(int w=0;w<Chunk.CHUNK_DIMENSION+2;w++)
			{
				for(int h=0;h<Chunk.CHUNK_DIMENSION+2;h++)
				{
					this.savedHeightMap[w][h]=getHeight((x*Chunk.CHUNK_DIMENSION)+(w-1), (z*Chunk.CHUNK_DIMENSION)+(h-1));
					if(this.savedHeightMap[w][h]>maxY) maxY=this.savedHeightMap[w][h];
					if(this.savedHeightMap[w][h]<minY) minY=this.savedHeightMap[w][h];
				}
			}
		this.savedHeightMapX=x;savedHeightMapZ=z;
		this.savedHeightMapMaxY=maxY;
		}
	}
	
	/**
	 * Sampling the current saved height map, gets the cube for the position x,y,z
	 */
	public byte getCubeFromHeightMap(int x,int y,int z)
	{
		int height=this.savedHeightMap[x+1][z+1];
		byte cubeCode;
		if(y>height) cubeCode=0; //If y is greater than the height, the block is air
		else if(y==height&&y>=SEA_HEIGHT){
			//If y is the topmost cube of this x,z terrain coordinate, generates grass or snow based on the height.
			if(y>95) cubeCode=20; 
			else if(y>85&& (y-85)/10f>Math.random()) {
				cubeCode=20;
			}
			else cubeCode=1; 
		}
		else cubeCode=2; //If the cube is inside the terrain, generates dirt
		
		if(cubeCode==0&&y<=SEA_HEIGHT) cubeCode=4; //If the cube is air but it is under the sea height, it will be water instead
		
		return cubeCode;
	}

	/**
	 * Gets the map height in the x,z terrain coordinate, using the 2D simplex functions explained above and some different mixing algorithms between them depending of the map type.
	 */
	public int getHeight(int x,int z){
		double elevmult=this.elevationCoef.getNormalizedNoise(x, z);
		double baseMap=this.mapBase.getNormalizedNoise(x, z);
		double detail=this.mapElevation.getNormalizedNoise(x, z);
		
		elevmult=elevmult*elevmult; // Makes elevmult quadratic, so mountains are more rare, but more escarpate.
		
		int height=	mapcode==0? (int)(((baseMap+(detail*elevmult))*(56))+30): 
					mapcode==2? (int)((baseMap*56)+33): //The plains map doesnt have any elevation, so only baseMap is used
								(int)(((baseMap+(detail*elevmult))*(112))+30); //The snowy mountains map is much more elevated than the islands one.
					
		//Prevents mountain tops to be too pointy, smoothing them.
		if(height>120)
		{
			height=(int)(120+((height-120)/((float)(22)/8)));
		}
		
		return height;
	}
	
	/**
	 * For 3D maps, this function gets a [-1,1] value for each coordinate x,y,z. It is later interpreted as <0 = dirt, >0 = air.
	 */
	public float get3dValue(int x,int y,int z)
	{
		double detail=this.mapElevation.getNoise(x,y, z);
		double h=0;
		switch(this.mapcode)
		{
		case 3:
			h=((float)(y)/13) -10;
			h=y<70? -1 : 1/(1+h*h);
			break;
		case 4:
			h=((float)(y - 130)/40);
			h=1.25f - 1/(1+h*h) - (y>32?0:(2.25- y/14.22f)); //Floating islands map will have two parts that generate dirt, the -1/(1+h*h), which have a maximum value at y=130, and the ground one, which will
															 //Make a linear degradating ground structure
			break;
		case 5:
			h=((float)(y)/13) -10;
			h=1/(1+h*h) + (float)(y)/320 - (y>3?0:(1- y/3f)); 
			break;
		case 6:
			h=-1 + (float)(y)/120;
			break;
		}
		
		return (float)(detail +h);
	}
}

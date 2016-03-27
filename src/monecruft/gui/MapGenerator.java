package monecruft.gui;

import java.util.Random;

import monecruft.blocks.BlockLibrary;
import ivengine.gen.SimplexNoise;

public class MapGenerator 
{
	public static final int MAP_UNDERHEIGHT=1;
	public static final int MAP_OVERHEIGHT=1;
	public static final int SEA_HEIGHT=64;
	public enum ChunkGenerationResult{CHUNK_EMPTY,CHUNK_HIGHER_THAN_HEIGHTMAP,CHUNK_NORMAL};
	
	private SimplexNoise mapBase;
	private SimplexNoise mapElevation;
	private SimplexNoise elevationCoef;
	
	private int beginBlockHeight;
	private int endBlockHeight;
	
	private int[][] savedHeightMap=new int[Chunk.CHUNK_DIMENSION+2][Chunk.CHUNK_DIMENSION+2];
	private int savedHeightMapX=-1;private int savedHeightMapZ=-1;
	private int savedHeightMapMinY=-1; private int savedHeightMapMaxY=-1;
	private boolean liquidTag=false;
	
	private int mapcode=0; //|TODO likely only debug
	
	public MapGenerator(int bbegin,int bend,int mapcode,long seed)
	{
		this.mapcode=mapcode;
		this.beginBlockHeight=bbegin;this.endBlockHeight=bend;
		//this.mapNoise=new PerlinNoise(65431245*244233);
		long seed1=seed>>43;
		long seed2=seed>>21;
		long seed3=seed;
		
		this.mapBase=new SimplexNoise(3,0.5,(int)seed1,100); //,147
		this.mapElevation=new SimplexNoise(3,0.5,(int)seed2,10);//11);
		this.elevationCoef=new SimplexNoise(3,0.8,(int)seed3,100);
				//100);
	}
	public ChunkGenerationResult generateChunk(int x,int y,int z,byte[][][] c)
	{
		boolean empty=true;
		byte[][][] ret=c;
		generateHeightMap(x,z);
		if(y*Chunk.CHUNK_DIMENSION>savedHeightMapMaxY&&y*Chunk.CHUNK_DIMENSION>SEA_HEIGHT) return ChunkGenerationResult.CHUNK_HIGHER_THAN_HEIGHTMAP; //|TODO full of water here
		for(int cx=0;cx<Chunk.CHUNK_DIMENSION;cx++)
		{
			for(int cy=0;cy<((y==World.HEIGHT-1)?Chunk.CHUNK_DIMENSION-1:Chunk.CHUNK_DIMENSION);cy++)
			{
				for(int cz=0;cz<Chunk.CHUNK_DIMENSION;cz++)
				{
					//ret[cx][cy][cz]=cy+(y*Chunk.CHUNK_DIMENSION)==50?(byte)1:0;
					if(mapcode<2)ret[cx][cy][cz]=getCubeFromHeightMap(cx,cy+(y*Chunk.CHUNK_DIMENSION),cz);
					else {
						ret[cx][cy][cz]=get3dValue(cx+(x*Chunk.CHUNK_DIMENSION),
							cy+(y*Chunk.CHUNK_DIMENSION), 
							cz+(z*Chunk.CHUNK_DIMENSION))<0 ? (byte)1: (cy+(y*Chunk.CHUNK_DIMENSION)<70? (byte)(1): (byte)(0));
						if(cy + y*Chunk.CHUNK_DIMENSION<=SEA_HEIGHT+15){
							if(ret[cx][cy][cz]==0)ret[cx][cy][cz]=4;
						}
					}
					double rand=Math.random();
					if(ret[cx][cy][cz]==0&&cy>1&&ret[cx][cy-1][cz]==1) {
						if(rand<0.1) ret[cx][cy][cz]=(byte)19;
						else if(rand<0.12) ret[cx][cy][cz]=(byte)21;
					}
							
					if(empty&&ret[cx][cy][cz]!=0) empty=false; 
					//else ret[cx][cy][cz]=(byte)0;
					/*int dist=cy-((cx-16)*(cx-16) + (cz-16)*(cz-16))/100;
					ret[cx][cy][cz]=cy>16?(byte)0:(dist<10?(byte)1:4);
					if(y<3) ret[cx][cy][cz]=1;*/
					//ret[cx][cy][cz]=cy==0?(byte)1:0;
				}
			}
		}
		if(y==World.HEIGHT-1){
			for(int cx=0;cx<Chunk.CHUNK_DIMENSION;cx++){
				for(int cz=0;cz<Chunk.CHUNK_DIMENSION;cz++){
					ret[cx][Chunk.CHUNK_DIMENSION-1][cz]=0;
				}
			}
		}
		return empty?ChunkGenerationResult.CHUNK_EMPTY:ChunkGenerationResult.CHUNK_NORMAL;
	}
	public void generateChunkObjects(Chunk c)
	{
		//if(1==1) return;
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
	private void generateTreeAt(int x,int y,int z,Chunk c)
	{
		/*for(int dx=-3;dx<=3;dx++)
		{
			if(Math.abs(dx)==3) continue;
			for(int dz=-3+Math.abs(dx);dz<=3-Math.abs(dx);dz++)
			{
				if(Math.abs(dz)==3) continue;
				int dxz=Math.abs(dz)+Math.abs(dx);
				for(int dy=0;dy<=3-dxz;dy++)
				{
					if(c.getCubeAt(x+dx, y+dy+3, z+dz)==0)c.setCubeAt(x+dx, y+dy+3, z+dz, (byte)23);
				}
			}
		}
		c.setCubeAt(x, y, z, (byte)22);
		c.setCubeAt(x, y+1, z, (byte)22);
		c.setCubeAt(x, y+2, z, (byte)22);
		c.setCubeAt(x, y+3, z, (byte)22);
		c.setCubeAt(x, y+4, z, (byte)22);*/
		int height=(int)(Math.random()*6)+3;
		for(int cy=0;cy<height;cy++)
		{
			c.setCubeAt(x, y+cy, z, (byte)22);
			if(cy==height-1) c.setCubeAt(x, y+height, z, (byte)23);
			int dx=0;int dz=0; int armL=0;
			if(Math.random()*(cy/(float)(height))>0.3f){
				armL=(int)(Math.random()*(height/2)+1);
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
				//if(Math.abs(cz)==width) continue;
				int dxz=Math.abs(dz<0?cz+armL:dz>0?cz-armL:cz);
				for(int cx=-width+dxz-(dx<0?armL:0);cx<=width-dxz+(dx>0?armL:0);cx++)
				{
					if(c.getCubeAt(x+cx, y+cy, z+cz)==0)c.setCubeAt(x+cx, y+cy, z+cz, (byte)23);
					else if(c.getCubeAt(x+cx, y+cy, z+cz)==22) c.setCubeAt(x+cx, y+cy+1, z+cz, (byte)23);
				}
			}
		}
	}
	/*public byte getCubeAt(int absx,int absy,int absz)
	{
		int h=getHeight(absx,absz);
		byte toRet=0;
		if(h>=absy){
			if(h/(float)(70) + Math.random()>2) toRet=19;
			else toRet=1;
		}
		else if(absy<=SEA_HEIGHT){
			toRet=4;
		}
		return toRet;
	}*/
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
		this.savedHeightMapMinY=minY;this.savedHeightMapMaxY=maxY;
		}
	}
	public byte getCubeFromHeightMap(int x,int y,int z){
		//if(y>=64) return 0;
		//else if(1==1) return 1;
		int height=this.savedHeightMap[x+1][z+1];
		byte cubeCode;
		if(y>height) cubeCode=0;
		else if(y==height&&y>=SEA_HEIGHT){
			//if(y<55) cubeCode=1;
			//else if(y<100) cubeCode=(byte) ((this.mapBase.getNoise(x, z)+1+((y-55)/(float)(22)))>2?2:1);
			//else cubeCode=2;
			if(y>95) cubeCode=20;
			else if(y>85&& (y-85)/10f>Math.random()) {
				cubeCode=20;
			}
			else cubeCode=1;
		}
		else cubeCode=2;
		
		/*if(y>100){
			if(this.mapBase.noise(x, z))
		}*/
		if(cubeCode==0&&y<=SEA_HEIGHT) cubeCode=4;
		
		//int cubecode=this.savedHeightMap[x+1][z+1]>=y?1:(y<=SEA_HEIGHT?4:0);
		//int cubecode=(x+y+z)%2==1?3:0;
		return cubeCode;
	}
	public int getHeightMap(int x,int y,int z,int[][] hmap)
	{
		if(y*Chunk.CHUNK_DIMENSION<this.beginBlockHeight) return MAP_UNDERHEIGHT;
		else if(y*Chunk.CHUNK_DIMENSION>this.endBlockHeight) return MAP_OVERHEIGHT;
		if(x!=this.savedHeightMapX||z!=this.savedHeightMapZ)
		{
			int minY=this.endBlockHeight;
			int maxY=this.beginBlockHeight;
			for(int w=0;w<Chunk.CHUNK_DIMENSION;w++)
			{
				for(int h=0;h<Chunk.CHUNK_DIMENSION;h++)
				{
					this.savedHeightMap[w][h]=getHeight((x*Chunk.CHUNK_DIMENSION)+w, (z*Chunk.CHUNK_DIMENSION)+h);
					if(this.savedHeightMap[w][h]>maxY) maxY=this.savedHeightMap[w][h];
					if(this.savedHeightMap[w][h]<minY) minY=this.savedHeightMap[w][h];
				}
			}
			this.savedHeightMapX=x;savedHeightMapZ=z;
			this.savedHeightMapMinY=minY;this.savedHeightMapMaxY=maxY;
		}
		//if((y+1)*Chunk.CHUNK_DIMENSION<this.savedHeightMapMinY) return MAP_UNDERHEIGHT;
		//else if(y*Chunk.CHUNK_DIMENSION>this.savedHeightMapMaxY) return MAP_OVERHEIGHT;
		for(int w=0;w<hmap.length;w++)
		{
			for(int h=0;h<hmap[0].length;h++)
			{
				hmap[w][h]=savedHeightMap[w][h]-(y*Chunk.CHUNK_DIMENSION);
			}
		}
		return 0;
	}
	public boolean shouldDraw(int x,int y,int z){
		return shouldDraw(getCubeFromHeightMap(x,y,z));
	}
	public boolean shouldDraw(byte blockCode){
		if(liquidTag)
			return !(BlockLibrary.isLiquid(blockCode)||BlockLibrary.isSolid(blockCode));	
		else
			return !BlockLibrary.isSolid(blockCode);
	}
	public boolean isOcupped(int bx,int by,int bz)
	{
		return getHeight(bx,bz)>=by;
	}
	public boolean hasWater(int bx,int by,int bz)
	{
		return by<=40;
	}
	/*private int getHeight(int x,int z){
		return (int)(this.beginBlockHeight+
				(this.mapBase.getNormalizedNoise(x, z)*0)+
				(this.mapElevation.getNormalizedNoise(x, z)*this.elevationCoef.getNormalizedNoise(x, z)*128));
	}*/
	public int getHeight(int x,int z){
		double elevmult=this.elevationCoef.getNormalizedNoise(x, z);
		double baseMap=this.mapBase.getNormalizedNoise(x, z);
		double detail=this.mapElevation.getNormalizedNoise(x, z);
		//double elevmult=1;
		//double baseMap=1;
		//double detail=1;
		
		//elevmult=Math.signum(elevmult-0.5)*(elevmult-0.5)*(elevmult-0.5) +0.5;
		//double elevmult=0.5;
		/*detail=detail+(detail*(Math.abs(elevmult-0.27)));
		elevmult=0.2+((elevmult+0.2)*(elevmult+0.2) /2);
		if(elevmult>1) elevmult=1;
		baseMap=0.2+(baseMap*0.8);
		if(elevmult>1) elevmult=1;*/
		//baseMap=0.4+(baseMap*baseMap);
		//if(baseMap>1) baseMap=1;
		//detail=detail*(elevmult*elevmult);
		//int height=(int)(40+(((detail*16)+(baseMap*48))*elevmult));
		//int height=(int)(45+(detail*11)+baseMap*48*elevmult);
		//double willy=1;
		//willy+=0.2;
		//willy=(willy*willy);
		//if(willy>1) willy=1;
		//int h= (int)((((this.mapBase.getNormalizedNoise(x, z)+0.3)*37*willy)+(this.mapElevation.getNormalizedNoise(x, z)*16)))+40;
		//elevmult-=0.3;
		//if(elevmult<0)elevmult=0;
		//else elevmult*=1/0.7;
		elevmult=elevmult*elevmult;
		int height=(int)(((baseMap+(detail*elevmult))*(56*(mapcode+1)))+30);
		if(height>120)
		{
			height=(int)(120+((height-120)/((float)(22)/8)));
		}
		
		return height;
	}
	public float get3dValue(int x,int y,int z)
	{
		/*double elevmult=this.elevationCoef.getNoise(x, y,z);
		elevmult=(elevmult+1)/2;
		double baseMap=this.mapBase.getNoise(x,y, z);
		baseMap=(baseMap+1)/2;*/
		double detail=this.mapElevation.getNoise(x,y, z);
		//elevmult=elevmult*elevmult;
		double h=((float)(y)/13) -10;
		h=1/(1+h*h);
		float res=(float)(detail +h);
		
		return res;
	}
	/*private int getHeight(int x,int z){
		return 1;
	}*/
	//|TODO gen
	/*byte[][] generateBoundFromChunk(int x,int y,int z,int pos)
	{
		int absx=x*Chunk.CHUNK_DIMENSION;
		int absy=y*Chunk.CHUNK_DIMENSION;
		int absz=z*Chunk.CHUNK_DIMENSION;
		byte[][] toRet=new byte[Chunk.CHUNK_DIMENSION][Chunk.CHUNK_DIMENSION];
		switch(pos)
		{
		case MapHandler.BOUND_XL:
			for(int cz=0;cz<Chunk.CHUNK_DIMENSION;cz++)
			{
				for(int cy=0;cy<Chunk.CHUNK_DIMENSION;cy++)
				{
					toRet[cy][cz]=getCubeAt(absx, absy+cy, absz+cz);
				}
			}
			break;
		case MapHandler.BOUND_XR:
			for(int cz=0;cz<Chunk.CHUNK_DIMENSION;cz++)
			{
				for(int cy=0;cy<Chunk.CHUNK_DIMENSION;cy++)
				{
					toRet[cy][cz]=getCubeAt(absx+Chunk.CHUNK_DIMENSION-1, absy+cy, absz+cz);
				}
			}
			break;
		case MapHandler.BOUND_YL:
			for(int cx=0;cx<Chunk.CHUNK_DIMENSION;cx++)
			{
				for(int cz=0;cz<Chunk.CHUNK_DIMENSION;cz++)
				{
					toRet[cx][cz]=getCubeAt(absx+cx, absy, absz+cz);
				}
			}
			break;
		case MapHandler.BOUND_YR:
			for(int cx=0;cx<Chunk.CHUNK_DIMENSION;cx++)
			{
				for(int cz=0;cz<Chunk.CHUNK_DIMENSION;cz++)
				{
					toRet[cx][cz]=getCubeAt(absx+cx, absy+Chunk.CHUNK_DIMENSION-1, absz+cz);
				}
			}
			break;
		case MapHandler.BOUND_ZL:
			for(int cx=0;cx<Chunk.CHUNK_DIMENSION;cx++)
			{
				for(int cy=0;cy<Chunk.CHUNK_DIMENSION;cy++)
				{
					toRet[cx][cy]=getCubeAt(absx+cx, absy+cy, absz);
				}
			}
			break;
		case MapHandler.BOUND_ZR:
			for(int cx=0;cx<Chunk.CHUNK_DIMENSION;cx++)
			{
				for(int cy=0;cy<Chunk.CHUNK_DIMENSION;cy++)
				{
					toRet[cx][cy]=getCubeAt(absx+cx, absy+cy, absz+Chunk.CHUNK_DIMENSION-1);
				}
			}
			break;
		}
		/*for(int w=0;w<toRet.length;w++)
		{
			for(int h=0;h<toRet.length;h++)
			{
				toRet[w][h]=1;
			}
		}*/
		/*return toRet;
	}*/
}

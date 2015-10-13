package monecruft.gui;

import java.util.Random;

import monecruft.blocks.BlockLibrary;
import ivengine.gen.SimplexNoise;

public class MapGenerator 
{
	public static final int MAP_UNDERHEIGHT=1;
	public static final int MAP_OVERHEIGHT=1;
	public static final int SEA_HEIGHT=64;
	
	private SimplexNoise mapBase;
	private SimplexNoise mapElevation;
	private SimplexNoise elevationCoef;
	
	private int beginBlockHeight;
	private int endBlockHeight;
	
	private int[][] savedHeightMap=new int[Chunk.CHUNK_DIMENSION+2][Chunk.CHUNK_DIMENSION+2];
	private int savedHeightMapX=-1;private int savedHeightMapZ=-1;
	private int savedHeightMapMinY=-1; private int savedHeightMapMaxY=-1;
	private boolean liquidTag=false;
	
	public MapGenerator(int bbegin,int bend)
	{
		this.beginBlockHeight=bbegin;this.endBlockHeight=bend;
		//this.mapNoise=new PerlinNoise(65431245*244233);
		long seed=(new Random()).nextLong();
		long seed1=seed>>43;
		long seed2=seed>>21;
		long seed3=seed;
		
		this.mapBase=new SimplexNoise(3,0.5,(int)seed1,100); //,147
		this.mapElevation=new SimplexNoise(3,0.5,(int)seed2,10);//11);
		this.elevationCoef=new SimplexNoise(3,0.8,(int)seed3,100);
				//100);
	}
	public byte[][][] generateChunk(int x,int y,int z,byte[][][] c)
	{
		byte[][][] ret=c==null?new byte[Chunk.CHUNK_DIMENSION][Chunk.CHUNK_DIMENSION][Chunk.CHUNK_DIMENSION]:c;
		generateHeightMap(x,z);
		for(int cx=0;cx<Chunk.CHUNK_DIMENSION;cx++)
		{
			for(int cy=0;cy<((y==World.HEIGHT-1)?Chunk.CHUNK_DIMENSION-1:Chunk.CHUNK_DIMENSION);cy++)
			{
				for(int cz=0;cz<Chunk.CHUNK_DIMENSION;cz++)
				{
					//ret[cx][cy][cz]=cy+(y*Chunk.CHUNK_DIMENSION)==50?(byte)1:0;
					ret[cx][cy][cz]=getCubeFromHeightMap(cx,cy+(y*Chunk.CHUNK_DIMENSION),cz);
					/*ret[cx][cy][cz]=get3dValue(cx+(x*Chunk.CHUNK_DIMENSION),
							cy+(y*Chunk.CHUNK_DIMENSION), 
							cz+(z*Chunk.CHUNK_DIMENSION))<0 ? (byte)6: (cy+(y*Chunk.CHUNK_DIMENSION)<70? (byte)(4): (byte)(0));*/
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
		return ret;
	}
	public byte getCubeAt(int absx,int absy,int absz)
	{
		int h=getHeight(absx,absz);
		return (byte)(h>=absy ?1 : (absy<=SEA_HEIGHT? 4:0));
	}
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
		int height=this.savedHeightMap[x+1][z+1];
		byte cubeCode;
		if(y>height) cubeCode=0;
		else if(y==height&&y>=SEA_HEIGHT){
			//if(y<55) cubeCode=1;
			//else if(y<100) cubeCode=(byte) ((this.mapBase.getNoise(x, z)+1+((y-55)/(float)(22)))>2?2:1);
			//else cubeCode=2;
			cubeCode=1;
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
		int height=(int)(((baseMap+(detail*elevmult))*56)+30);
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
	byte[][] generateBoundFromChunk(int x,int y,int z,int pos)
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
		return toRet;
	}
}

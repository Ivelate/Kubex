package monecruft.gui;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glBufferSubData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import ivengine.properties.Cleanable;
import ivengine.shaders.SimpleShaderProgram;
import ivengine.view.Camera;
import ivengine.view.MatrixHelper;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;

import monecruft.blocks.BlockLibrary;
import monecruft.gui.MapGenerator.ChunkGenerationResult;
import monecruft.shaders.DepthVoxelShaderProgram;
import monecruft.shaders.VoxelShaderProgram;
import monecruft.storage.ArrayCubeStorage;
import monecruft.storage.ByteArrayPool;
import monecruft.storage.ConstantValueCubeStorage;
import monecruft.storage.CubeStorage;
import monecruft.storage.FloatBufferPool;
import monecruft.utils.BoundaryChecker;
import monecruft.utils.BoundingBoxBoundaryChecker;

import monecruft.utils.Vector3d;

import org.lwjgl.opengl.GL15;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

public class Chunk implements Cleanable
{
	public enum Direction{YP,YM,XP,XM,ZP,ZM};
	public enum CubeStorageType{CUBES_STORAGE,LIGHT_STORAGE};
	
	public static final int CHUNK_DIMENSION=32;
	public static final float MAX_LIGHT_LEVEL=16;
	private static final double CHUNK_RADIUS=CHUNK_DIMENSION*Math.sqrt(3);
	private static final Vector4f CENTER_VECTOR=new Vector4f(CHUNK_DIMENSION/2,CHUNK_DIMENSION/2,CHUNK_DIMENSION/2,1.0f);
	//Numero de argumentos del shader
	private static final int SAN=6;
	private static final int NORMALIZE_WATER_EACH=4;
	
	private static final byte NATURAL_LIGHT_FULL_VAL=(byte)((15 << 4)&0xF0);
	
	private int vbo=-1;
	private WorldFacade WF;
	private int triangleNum=0;
	private int triangleLiquidNum=0;
	private CubeStorage chunkCubes=null; 
	private CubeStorage chunkCubesLight=new ConstantValueCubeStorage((byte)0,this,CubeStorageType.LIGHT_STORAGE){
		@Override public void set(int x, int y, int z, byte val) {}
	};
	private class CubePosition{public int x;public int y;public int z; public CubePosition(int x,int y,int z){this.x=x;this.y=y;this.z=z;}}
	private List<CubePosition> updateCubes=new LinkedList<CubePosition>();
	private int neighborsAdded=0;
	private FloatBuffer toUpload=null;
	private FloatBuffer toUploadLiquid=null;
	private boolean lightCalculated=false;
	private boolean drawed=false;
	private boolean changed=true;
	private boolean updateFlag=false;
	private boolean solidEmpty=true;
	private boolean liquidEmpty=true;
	private boolean deleted=false;
	private Semaphore updateAccessSemaphore=new Semaphore(1);
	private int liquidCounter=NORMALIZE_WATER_EACH;
	
	private int chunkx;
	private int chunky;
	private int chunkz;
	private Matrix4f chunkModelMatrix;
	
	public Chunk(int chunkxpos,int chunkypos,int chunkzpos,WorldFacade WF)
	{
		this.WF=WF;
		this.chunkx=chunkxpos;
		this.chunky=chunkypos;
		this.chunkz=chunkzpos;
		this.chunkModelMatrix=new Matrix4f();
		//Matrix4f.translate(new Vector3f(chunkx*Chunk.CHUNK_DIMENSION,chunky*Chunk.CHUNK_DIMENSION,chunkz*Chunk.CHUNK_DIMENSION), this.chunkModelMatrix, this.chunkModelMatrix);
		byte[][][] cubes=ByteArrayPool.getArrayUncleaned();
		ChunkGenerationResult arrayEmpty=WF.getMapHandler().getChunk(chunkxpos, chunkypos,chunkzpos,cubes);
		
		if(arrayEmpty!=ChunkGenerationResult.CHUNK_NORMAL) {
			this.chunkCubes=new ConstantValueCubeStorage((byte)0,this,CubeStorageType.CUBES_STORAGE);
			if(arrayEmpty==ChunkGenerationResult.CHUNK_EMPTY) ByteArrayPool.recycleCleanArray(cubes);
			else ByteArrayPool.recycleArray(cubes);
		}
		else this.chunkCubes=new ArrayCubeStorage(cubes,false);
		/*for(int i=0;i<4;i++)
		{
			int x=(int)(Math.random()*CHUNK_DIMENSION);
			int y=(int)(Math.random()*((chunkypos==World.HEIGHT-1)?CHUNK_DIMENSION-1:CHUNK_DIMENSION));
			int z=(int)(Math.random()*CHUNK_DIMENSION);
			//if(x>20) x-=10; if(y>30) y--; if(z>30) z--;
			//if(x<1) x++; if(y<1) y++; if(z<1) z++; 
			this.chunkCubes[x][y][z]=5;
			/*this.chunkCubes[x+1][y][z]=5;
			this.chunkCubes[x-1][y][z]=5;
			this.chunkCubes[x][y+1][z]=5;
			this.chunkCubes[x][y-1][z]=5;
			this.chunkCubes[x][y][z+1]=5;
			this.chunkCubes[x][y][z-1]=5;*/
			//this.chunkCubes[x][y][z]=5;
			//this.chunkCubes[x+10][y][z]=5;
		//}
		//Neighbours
		//this.neighborsAdded=WF.getNeighboursAdded(this);
	}
	public void initChunk()
	{
		this.vbo=glGenBuffers();
		/*glBindBuffer(GL15.GL_ARRAY_BUFFER,this.vbo);
	
		VSP.enable();
		VSP.setupAttributes();*/

		update();
	
		//glBindBuffer(GL15.GL_ARRAY_BUFFER,0);
	}
	public void genUpdateBuffer(){
		this.genUpdateBuffer(true);
	}
	public void genUpdateBuffer(boolean safeAccess)
	{
		this.changed=false;
		if(!this.lightCalculated) createLightMap();
		boolean canDraw=true;
		
		int bufferCont=0;
		int liquidCont=0;
		
		//byte[][][] bounds=this.WF.getMapHandler().getBounds(this.chunkx,this.chunky,this.chunkz)
		//ONLY DRAWS IF ALL NEIGHBORS IN ALL DIRECTIONS (DIAGONALS INCLUDED) HAS BEEN ADDED
		if(this.neighborsAdded==134209535) //00000111111111111101111111111111 (I hope)
		{
		
		Chunk[] neighbours=this.WF.getNeighbours(this);
		
		if(neighbours[Direction.XM.ordinal()]==null || !neighbours[Direction.XM.ordinal()].lightCalculated) {canDraw=false; this.notifyNeighbourRemoved(-1, 0, 0);}
		if(neighbours[Direction.XP.ordinal()]==null || !neighbours[Direction.XP.ordinal()].lightCalculated) {canDraw=false; this.notifyNeighbourRemoved(1, 0, 0);}
		if(neighbours[Direction.ZM.ordinal()]==null || !neighbours[Direction.ZM.ordinal()].lightCalculated) {canDraw=false; this.notifyNeighbourRemoved(0, 0, -1); }
		if(neighbours[Direction.ZP.ordinal()]==null || !neighbours[Direction.ZP.ordinal()].lightCalculated) {canDraw=false; this.notifyNeighbourRemoved(0, 0, 1);}
		if((neighbours[Direction.YM.ordinal()]==null || !neighbours[Direction.YM.ordinal()].lightCalculated)&&this.getY()>0) {canDraw=false; this.notifyNeighbourRemoved(0, -1, 0);}
		if((neighbours[Direction.YP.ordinal()]==null || !neighbours[Direction.YP.ordinal()].lightCalculated)&&this.getY()<World.HEIGHT-1) {canDraw=false; this.notifyNeighbourRemoved(0, 1, 0);}
		
		
		if(canDraw){
			if(safeAccess){
				try {
					this.getUpdateAccessSemaphore().acquire();
				} catch (InterruptedException e) {
					System.err.println("Update buffer interrupted!");
					return;
				}
			}
			if(this.toUpload!=null) {FloatBufferPool.recycleBuffer(this.toUpload); this.toUpload=null;}
			if(this.toUploadLiquid!=null) {FloatBufferPool.recycleBuffer(this.toUploadLiquid); this.toUploadLiquid=null;}
			
		FloatBuffer toUpload=FloatBufferPool.getBuffer();
		FloatBuffer toUploadLiquid=FloatBufferPool.getBuffer();
		//Get map generator
		MapHandler mg=WF.getMapHandler();
		boolean overrideDrawTop=false;
		boolean overrideDrawBot=false;
		for(byte z=0;z<CHUNK_DIMENSION;z++)
		{
			for(byte x=0;x<CHUNK_DIMENSION;x++)
			{
				overrideDrawTop=false;
				overrideDrawBot=false;
				if(neighbours[Direction.YM.ordinal()]!=null){
					byte cod=neighbours[Direction.YM.ordinal()].getCubeAt(x, CHUNK_DIMENSION-1, z);
					if(BlockLibrary.isLiquid(cod)&&(neighbours[Direction.YM.ordinal()].getCubeHeight(x,CHUNK_DIMENSION-1,z,cod)<0.99f
							||neighbours[Direction.YM.ordinal()].getCubeHeight(x+1,CHUNK_DIMENSION-1,z,cod)<0.99f
							||neighbours[Direction.YM.ordinal()].getCubeHeight(x,CHUNK_DIMENSION-1,z+1,cod)<0.99f
							||neighbours[Direction.YM.ordinal()].getCubeHeight(x+1,CHUNK_DIMENSION-1,z+1,cod)<0.99f)) overrideDrawBot=true;
				}
				for(byte y=0;y<CHUNK_DIMENSION;y++)
				{
					if(BlockLibrary.isDrawable(this.chunkCubes.get(x,y,z)))
					{
						FloatBuffer writeTarget;
						boolean liquidTag=false;
						if(BlockLibrary.isLiquid(this.chunkCubes.get(x,y,z))){
							writeTarget=toUploadLiquid;
							liquidTag=true;
						}else{
							writeTarget=toUpload;
						}
						float[] cube=new float[SAN*6];
						byte cubeC=this.chunkCubes.get(x,y,z);
						float heightxmzm=1;float heightxpzm=1;float heightxmzp=1;float heightxpzp=1;
						if(liquidTag){
							heightxmzm=getCubeHeight(x,y,z,cubeC);
							heightxpzm=getCubeHeight(x+1,y,z,cubeC);
							heightxmzp=getCubeHeight(x,y,z+1,cubeC);
							heightxpzp=getCubeHeight(x+1,y,z+1,cubeC);
							
							if(!(heightxpzp>0.99f&&heightxmzm>0.99f&&heightxpzm>0.99f&&heightxmzp>0.99f)) overrideDrawTop=true;
						}
						int c=0;
						
						if(BlockLibrary.isCrossSectional(cubeC))
						{
							//heightxmzm=0.75f; heightxmzp=0.75f; heightxpzm=0.75f; heightxpzp=0.75f;
							cube[0+(SAN*0)]=x;		cube[1+(SAN*0)]=y;		cube[2+(SAN*0)]=z;		
							cube[3+(SAN*0)]=6000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*0)]=this.getNaturalBrightnessFaceYAverageAt(x, y, z); cube[5+(SAN*0)]=this.getArtificialBrightnessFaceYAverageAt(x, y, z);
							cube[0+(SAN*1)]=x+1;				cube[1+(SAN*1)]=y;				cube[2+(SAN*1)]=(byte)(z+1);	
							cube[3+(SAN*1)]=6000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*1)]=this.getNaturalBrightnessFaceYAverageAt(x+1, y, z+1); cube[5+(SAN*1)]=this.getArtificialBrightnessFaceYAverageAt(x+1, y, z+1);					
							cube[0+(SAN*2)]=x;				cube[1+(SAN*2)]=y+heightxmzm;	cube[2+(SAN*2)]=z;
							cube[3+(SAN*2)]=6000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*2)]=this.getNaturalBrightnessFaceYAverageAt(x, y+1, z); cube[5+(SAN*2)]=this.getArtificialBrightnessFaceYAverageAt(x, y+1, z);				
							cube[0+(SAN*3)]=x;				cube[1+(SAN*3)]=y+heightxmzm;	cube[2+(SAN*3)]=z;
							cube[3+(SAN*3)]=6000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*3)]=this.getNaturalBrightnessFaceYAverageAt(x, y+1, z); cube[5+(SAN*3)]=this.getArtificialBrightnessFaceYAverageAt(x, y+1, z);					
							cube[0+(SAN*4)]=x+1;				cube[1+(SAN*4)]=y;				cube[2+(SAN*4)]=(byte)(z+1);
							cube[3+(SAN*4)]=6000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*4)]=this.getNaturalBrightnessFaceYAverageAt(x+1, y, z+1); cube[5+(SAN*4)]=this.getArtificialBrightnessFaceYAverageAt(x+1, y, z+1);					
							cube[0+(SAN*5)]=x+1;				cube[1+(SAN*5)]=y+heightxmzp;	cube[2+(SAN*5)]=(byte)(z+1);
							cube[3+(SAN*5)]=6000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*5)]=this.getNaturalBrightnessFaceYAverageAt(x+1, y+1, z+1); cube[5+(SAN*5)]=this.getArtificialBrightnessFaceYAverageAt(x+1, y+1, z+1);
							writeTarget.put(cube);c+=SAN*6;
							cube[0+(SAN*0)]=x;		cube[1+(SAN*0)]=y;		cube[2+(SAN*0)]=z;		
							cube[3+(SAN*0)]=7000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*0)]=this.getNaturalBrightnessFaceYAverageAt(x, y, z); cube[5+(SAN*0)]=this.getArtificialBrightnessFaceYAverageAt(x, y, z);
							cube[0+(SAN*1)]=x;				cube[1+(SAN*1)]=y+heightxmzm;			cube[2+(SAN*1)]=z;	
							cube[3+(SAN*1)]=7000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*1)]=this.getNaturalBrightnessFaceYAverageAt(x, y+1, z); cube[5+(SAN*1)]=this.getArtificialBrightnessFaceYAverageAt(x, y+1, z);					
							cube[0+(SAN*2)]=x+1;				cube[1+(SAN*2)]=y;	cube[2+(SAN*2)]=(byte)(z+1);
							cube[3+(SAN*2)]=7000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*2)]=this.getNaturalBrightnessFaceYAverageAt(x+1, y, z+1); cube[5+(SAN*2)]=this.getArtificialBrightnessFaceYAverageAt(x+1, y, z+1);				
							cube[0+(SAN*3)]=x;				cube[1+(SAN*3)]=y+heightxmzm;	cube[2+(SAN*3)]=z;
							cube[3+(SAN*3)]=7000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*3)]=this.getNaturalBrightnessFaceYAverageAt(x, y+1, z); cube[5+(SAN*3)]=this.getArtificialBrightnessFaceYAverageAt(x, y+1, z);					
							cube[0+(SAN*4)]=x+1;				cube[1+(SAN*4)]=y+heightxmzp;				cube[2+(SAN*4)]=(byte)(z+1);
							cube[3+(SAN*4)]=7000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*4)]=this.getNaturalBrightnessFaceYAverageAt(x+1, y+1, z+1); cube[5+(SAN*4)]=this.getArtificialBrightnessFaceYAverageAt(x+1, y+1, z+1);					
							cube[0+(SAN*5)]=x+1;				cube[1+(SAN*5)]=y;	cube[2+(SAN*5)]=(byte)(z+1);
							cube[3+(SAN*5)]=7000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*5)]=this.getNaturalBrightnessFaceYAverageAt(x+1, y, z+1); cube[5+(SAN*5)]=this.getArtificialBrightnessFaceYAverageAt(x+1, y, z+1);
							writeTarget.put(cube);c+=SAN*6;
							
							cube[0+(SAN*0)]=x+1;		cube[1+(SAN*0)]=y;		cube[2+(SAN*0)]=z;		
							cube[3+(SAN*0)]=8000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*0)]=this.getNaturalBrightnessFaceYAverageAt(x+1, y, z); cube[5+(SAN*0)]=this.getArtificialBrightnessFaceYAverageAt(x+1, y, z);
							cube[0+(SAN*1)]=x;				cube[1+(SAN*1)]=y;				cube[2+(SAN*1)]=(byte)(z+1);	
							cube[3+(SAN*1)]=8000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*1)]=this.getNaturalBrightnessFaceYAverageAt(x, y, z+1); cube[5+(SAN*1)]=this.getArtificialBrightnessFaceYAverageAt(x, y, z+1);					
							cube[0+(SAN*2)]=x+1;				cube[1+(SAN*2)]=y+heightxmzm;	cube[2+(SAN*2)]=z;
							cube[3+(SAN*2)]=8000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*2)]=this.getNaturalBrightnessFaceYAverageAt(x+1, y+1, z); cube[5+(SAN*2)]=this.getArtificialBrightnessFaceYAverageAt(x+1, y+1, z);				
							cube[0+(SAN*3)]=x+1;				cube[1+(SAN*3)]=y+heightxmzm;	cube[2+(SAN*3)]=z;
							cube[3+(SAN*3)]=8000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*3)]=this.getNaturalBrightnessFaceYAverageAt(x+1, y+1, z); cube[5+(SAN*3)]=this.getArtificialBrightnessFaceYAverageAt(x+1, y+1, z);					
							cube[0+(SAN*4)]=x;				cube[1+(SAN*4)]=y;				cube[2+(SAN*4)]=(byte)(z+1);
							cube[3+(SAN*4)]=8000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*4)]=this.getNaturalBrightnessFaceYAverageAt(x, y, z+1); cube[5+(SAN*4)]=this.getArtificialBrightnessFaceYAverageAt(x, y, z+1);					
							cube[0+(SAN*5)]=x;				cube[1+(SAN*5)]=y+heightxmzp;	cube[2+(SAN*5)]=(byte)(z+1);
							cube[3+(SAN*5)]=8000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*5)]=this.getNaturalBrightnessFaceYAverageAt(x, y+1, z+1); cube[5+(SAN*5)]=this.getArtificialBrightnessFaceYAverageAt(x, y+1, z+1);				
							writeTarget.put(cube);c+=SAN*6;
							cube[0+(SAN*0)]=x+1;		cube[1+(SAN*0)]=y;		cube[2+(SAN*0)]=z;		
							cube[3+(SAN*0)]=9000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*0)]=this.getNaturalBrightnessFaceYAverageAt(x+1, y, z); cube[5+(SAN*0)]=this.getArtificialBrightnessFaceYAverageAt(x+1, y, z);
							cube[0+(SAN*1)]=x+1;				cube[1+(SAN*1)]=y+heightxmzm;				cube[2+(SAN*1)]=(byte)(z);	
							cube[3+(SAN*1)]=9000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*1)]=this.getNaturalBrightnessFaceYAverageAt(x+1, y+1, z); cube[5+(SAN*1)]=this.getArtificialBrightnessFaceYAverageAt(x+1, y+1, z);					
							cube[0+(SAN*2)]=x;				cube[1+(SAN*2)]=y;	cube[2+(SAN*2)]=z+1;
							cube[3+(SAN*2)]=9000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*2)]=this.getNaturalBrightnessFaceYAverageAt(x, y, z+1); cube[5+(SAN*2)]=this.getArtificialBrightnessFaceYAverageAt(x, y, z+1);				
							cube[0+(SAN*3)]=x+1;				cube[1+(SAN*3)]=y+heightxmzm;	cube[2+(SAN*3)]=z;
							cube[3+(SAN*3)]=9000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*3)]=this.getNaturalBrightnessFaceYAverageAt(x+1, y+1, z); cube[5+(SAN*3)]=this.getArtificialBrightnessFaceYAverageAt(x+1, y+1, z);					
							cube[0+(SAN*4)]=x;				cube[1+(SAN*4)]=y+heightxmzp;			cube[2+(SAN*4)]=(byte)(z+1);
							cube[3+(SAN*4)]=9000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*4)]=this.getNaturalBrightnessFaceYAverageAt(x, y+1, z+1); cube[5+(SAN*4)]=this.getArtificialBrightnessFaceYAverageAt(x, y+1, z+1);					
							cube[0+(SAN*5)]=x;				cube[1+(SAN*5)]=y;	cube[2+(SAN*5)]=(byte)(z+1);
							cube[3+(SAN*5)]=9000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*5)]=this.getNaturalBrightnessFaceYAverageAt(x, y, z+1); cube[5+(SAN*5)]=this.getArtificialBrightnessFaceYAverageAt(x, y, z+1);				
							writeTarget.put(cube);c+=SAN*6;
							
							
						}
						else
						{
						// X axis faces
						if((x==0&&mg.shouldDraw(neighbours[Direction.XM.ordinal()].getCubeAt(CHUNK_DIMENSION-1, y, z),this.chunkCubes.get(x,y,z),liquidTag,Direction.XM))
								||(x!=0&&mg.shouldDraw(this.chunkCubes.get(x-1,y,z),this.chunkCubes.get(x,y,z),liquidTag,Direction.XM))){
						cube[0+(SAN*0)]=x;		cube[1+(SAN*0)]=y;		cube[2+(SAN*0)]=z;		
						cube[3+(SAN*0)]=BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*0)]=this.getNaturalBrightnessFaceXAverageAt(x-1, y, z); cube[5+(SAN*0)]=this.getArtificialBrightnessFaceXAverageAt(x-1, y, z);
						cube[0+(SAN*1)]=x;				cube[1+(SAN*1)]=y;				cube[2+(SAN*1)]=(byte)(z+1);	
						cube[3+(SAN*1)]=BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*1)]=this.getNaturalBrightnessFaceXAverageAt(x-1, y, z+1); cube[5+(SAN*1)]=this.getArtificialBrightnessFaceXAverageAt(x-1, y, z+1);					
						cube[0+(SAN*2)]=x;				cube[1+(SAN*2)]=y+heightxmzm;	cube[2+(SAN*2)]=z;
						cube[3+(SAN*2)]=BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*2)]=this.getNaturalBrightnessFaceXAverageAt(x-1, y+1, z); cube[5+(SAN*2)]=this.getArtificialBrightnessFaceXAverageAt(x-1, y+1, z);				
						cube[0+(SAN*3)]=x;				cube[1+(SAN*3)]=y+heightxmzm;	cube[2+(SAN*3)]=z;
						cube[3+(SAN*3)]=BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*3)]=this.getNaturalBrightnessFaceXAverageAt(x-1, y+1, z); cube[5+(SAN*3)]=this.getArtificialBrightnessFaceXAverageAt(x-1, y+1, z);					
						cube[0+(SAN*4)]=x;				cube[1+(SAN*4)]=y;				cube[2+(SAN*4)]=(byte)(z+1);
						cube[3+(SAN*4)]=BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*4)]=this.getNaturalBrightnessFaceXAverageAt(x-1, y, z+1); cube[5+(SAN*4)]=this.getArtificialBrightnessFaceXAverageAt(x-1, y, z+1);					
						cube[0+(SAN*5)]=x;				cube[1+(SAN*5)]=y+heightxmzp;	cube[2+(SAN*5)]=(byte)(z+1);
						cube[3+(SAN*5)]=BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*5)]=this.getNaturalBrightnessFaceXAverageAt(x-1, y+1, z+1); cube[5+(SAN*5)]=this.getArtificialBrightnessFaceXAverageAt(x-1, y+1, z+1);				
						writeTarget.put(cube);c+=SAN*6;}
						if((x==CHUNK_DIMENSION-1&&mg.shouldDraw(neighbours[Direction.XP.ordinal()].getCubeAt(0, y, z),this.chunkCubes.get(x,y,z),liquidTag,Direction.XP))
							||(x!=CHUNK_DIMENSION-1&&mg.shouldDraw(this.chunkCubes.get(x+1,y,z),this.chunkCubes.get(x,y,z),liquidTag,Direction.XP))){
						cube[0+(SAN*0)]=(byte)(x+1);	cube[1+(SAN*0)]=y;				cube[2+(SAN*0)]=z;
						cube[3+(SAN*0)]=1000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*0)]=this.getNaturalBrightnessFaceXAverageAt(x+1, y, z); cube[5+(SAN*0)]=this.getArtificialBrightnessFaceXAverageAt(x+1, y, z);
						cube[0+(SAN*1)]=(byte)(x+1);	cube[1+(SAN*1)]=y+heightxpzm;	cube[2+(SAN*1)]=z;
						cube[3+(SAN*1)]=1000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*1)]=this.getNaturalBrightnessFaceXAverageAt(x+1, y+1, z); cube[5+(SAN*1)]=this.getArtificialBrightnessFaceXAverageAt(x+1, y+1, z);
						cube[0+(SAN*2)]=(byte)(x+1);	cube[1+(SAN*2)]=y;				cube[2+(SAN*2)]=(byte)(z+1);
						cube[3+(SAN*2)]=1000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*2)]=this.getNaturalBrightnessFaceXAverageAt(x+1, y, z+1); cube[5+(SAN*2)]=this.getArtificialBrightnessFaceXAverageAt(x+1, y, z+1);
						cube[0+(SAN*3)]=(byte)(x+1);	cube[1+(SAN*3)]=y;				cube[2+(SAN*3)]=(byte)(z+1);
						cube[3+(SAN*3)]=1000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*3)]=this.getNaturalBrightnessFaceXAverageAt(x+1, y, z+1); cube[5+(SAN*3)]=this.getArtificialBrightnessFaceXAverageAt(x+1, y, z+1);
						cube[0+(SAN*4)]=(byte)(x+1);	cube[1+(SAN*4)]=y+heightxpzm;	cube[2+(SAN*4)]=z;	
						cube[3+(SAN*4)]=1000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*4)]=this.getNaturalBrightnessFaceXAverageAt(x+1, y+1, z); cube[5+(SAN*4)]=this.getArtificialBrightnessFaceXAverageAt(x+1, y+1, z);
						cube[0+(SAN*5)]=(byte)(x+1);	cube[1+(SAN*5)]=y+heightxpzp;	cube[2+(SAN*5)]=(byte)(z+1);
						cube[3+(SAN*5)]=1000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*5)]=this.getNaturalBrightnessFaceXAverageAt(x+1, y+1, z+1); cube[5+(SAN*5)]=this.getArtificialBrightnessFaceXAverageAt(x+1, y+1, z+1);
						writeTarget.put(cube);c+=SAN*6;}
						
						// Y axis faces
						if((y==0&&this.getY()!=0&&(mg.shouldDraw(neighbours[Direction.YM.ordinal()].getCubeAt(x, CHUNK_DIMENSION-1, z),this.chunkCubes.get(x,y,z),liquidTag,Direction.YM)||overrideDrawBot))
								||(y!=0&&(mg.shouldDraw(this.chunkCubes.get(x,y-1,z),this.chunkCubes.get(x,y,z),liquidTag,Direction.YM)||overrideDrawBot))){
						cube[0+(SAN*0)]=x;				cube[1+(SAN*0)]=y;				cube[2+(SAN*0)]=z;
						cube[3+(SAN*0)]=2000+BlockLibrary.getDownTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*0)]=this.getNaturalBrightnessFaceYAverageAt(x, y-1, z); cube[5+(SAN*0)]=this.getArtificialBrightnessFaceYAverageAt(x, y-1, z);
						cube[0+(SAN*1)]=(byte)(x+1);	cube[1+(SAN*1)]=y;				cube[2+(SAN*1)]=z;
						cube[3+(SAN*1)]=2000+BlockLibrary.getDownTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*1)]=this.getNaturalBrightnessFaceYAverageAt(x+1, y-1, z); cube[5+(SAN*1)]=this.getArtificialBrightnessFaceYAverageAt(x+1, y-1, z);
						cube[0+(SAN*2)]=x;				cube[1+(SAN*2)]=y;				cube[2+(SAN*2)]=(byte)(z+1);
						cube[3+(SAN*2)]=2000+BlockLibrary.getDownTex(this.chunkCubes.get(x,y,z)); cube[4+(SAN*2)]=this.getNaturalBrightnessFaceYAverageAt(x, y-1, z+1); cube[5+(SAN*2)]=this.getArtificialBrightnessFaceYAverageAt(x, y-1, z+1);					
						cube[0+(SAN*3)]=x;				cube[1+(SAN*3)]=y;				cube[2+(SAN*3)]=(byte)(z+1);
						cube[3+(SAN*3)]=2000+BlockLibrary.getDownTex(this.chunkCubes.get(x,y,z)); cube[4+(SAN*3)]=this.getNaturalBrightnessFaceYAverageAt(x, y-1, z+1); cube[5+(SAN*3)]=this.getArtificialBrightnessFaceYAverageAt(x, y-1, z+1);	
						cube[0+(SAN*4)]=(byte)(x+1);	cube[1+(SAN*4)]=y;				cube[2+(SAN*4)]=z;	
						cube[3+(SAN*4)]=2000+BlockLibrary.getDownTex(this.chunkCubes.get(x,y,z)); cube[4+(SAN*4)]=this.getNaturalBrightnessFaceYAverageAt(x+1, y-1, z); cube[5+(SAN*4)]=this.getArtificialBrightnessFaceYAverageAt(x+1, y-1, z);
						cube[0+(SAN*5)]=(byte)(x+1);	cube[1+(SAN*5)]=y;				cube[2+(SAN*5)]=(byte)(z+1);
						cube[3+(SAN*5)]=2000+BlockLibrary.getDownTex(this.chunkCubes.get(x,y,z)); cube[4+(SAN*5)]=this.getNaturalBrightnessFaceYAverageAt(x+1, y-1, z+1); cube[5+(SAN*5)]=this.getArtificialBrightnessFaceYAverageAt(x+1, y-1, z+1);
						writeTarget.put(cube);c+=SAN*6;}
						if((y==CHUNK_DIMENSION-1&&(overrideDrawTop||this.getY()==World.HEIGHT-1||mg.shouldDraw(neighbours[Direction.YP.ordinal()].getCubeAt(x, 0, z),this.chunkCubes.get(x,y,z),liquidTag,Direction.YP)))
								||(y!=CHUNK_DIMENSION-1&&(overrideDrawTop||mg.shouldDraw(this.chunkCubes.get(x,y+1,z),this.chunkCubes.get(x,y,z),liquidTag,Direction.YP)))){
						cube[0+(SAN*0)]=x;				cube[1+(SAN*0)]=y+heightxmzm;	cube[2+(SAN*0)]=z;
						cube[3+(SAN*0)]=3000+BlockLibrary.getUpTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*0)]=this.getNaturalBrightnessFaceYAverageAt(x, y+1, z); cube[5+(SAN*0)]=this.getArtificialBrightnessFaceYAverageAt(x, y+1, z);
						cube[0+(SAN*1)]=x;				cube[1+(SAN*1)]=y+heightxmzp;	cube[2+(SAN*1)]=(byte)(z+1);
						cube[3+(SAN*1)]=3000+BlockLibrary.getUpTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*1)]=this.getNaturalBrightnessFaceYAverageAt(x, y+1, z+1); cube[5+(SAN*1)]=this.getArtificialBrightnessFaceYAverageAt(x, y+1, z+1);
						cube[0+(SAN*2)]=(byte)(x+1);	cube[1+(SAN*2)]=y+heightxpzm;	cube[2+(SAN*2)]=z;
						cube[3+(SAN*2)]=3000+BlockLibrary.getUpTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*2)]=this.getNaturalBrightnessFaceYAverageAt(x+1, y+1, z); cube[5+(SAN*2)]=this.getArtificialBrightnessFaceYAverageAt(x+1, y+1, z);
						cube[0+(SAN*3)]=(byte)(x+1);	cube[1+(SAN*3)]=y+heightxpzm;	cube[2+(SAN*3)]=z;
						cube[3+(SAN*3)]=3000+BlockLibrary.getUpTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*3)]=this.getNaturalBrightnessFaceYAverageAt(x+1, y+1, z); cube[5+(SAN*3)]=this.getArtificialBrightnessFaceYAverageAt(x+1, y+1, z);
						cube[0+(SAN*4)]=x;				cube[1+(SAN*4)]=y+heightxmzp;	cube[2+(SAN*4)]=(byte)(z+1);
						cube[3+(SAN*4)]=3000+BlockLibrary.getUpTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*4)]=this.getNaturalBrightnessFaceYAverageAt(x, y+1, z+1); cube[5+(SAN*4)]=this.getArtificialBrightnessFaceYAverageAt(x, y+1, z+1);
						cube[0+(SAN*5)]=(byte)(x+1);	cube[1+(SAN*5)]=y+heightxpzp;	cube[2+(SAN*5)]=(byte)(z+1);
						cube[3+(SAN*5)]=3000+BlockLibrary.getUpTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*5)]=this.getNaturalBrightnessFaceYAverageAt(x+1, y+1, z+1); cube[5+(SAN*5)]=this.getArtificialBrightnessFaceYAverageAt(x+1, y+1, z+1);
						writeTarget.put(cube);c+=SAN*6;}
						
						// Z axis faces
						if((z==0&&mg.shouldDraw(neighbours[Direction.ZM.ordinal()].getCubeAt(x, y, CHUNK_DIMENSION-1),this.chunkCubes.get(x,y,z),liquidTag,Direction.ZM))
								||(z!=0&&mg.shouldDraw(this.chunkCubes.get(x,y,z-1),this.chunkCubes.get(x,y,z),liquidTag,Direction.ZM))){
						cube[0+(SAN*0)]=x;				cube[1+(SAN*0)]=y;				cube[2+(SAN*0)]=z;
						cube[3+(SAN*0)]=4000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*0)]=this.getNaturalBrightnessFaceZAverageAt(x, y, z-1); cube[5+(SAN*0)]=this.getArtificialBrightnessFaceZAverageAt(x, y, z-1);
						cube[0+(SAN*1)]=x;				cube[1+(SAN*1)]=y+heightxmzm;	cube[2+(SAN*1)]=z;
						cube[3+(SAN*1)]=4000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*1)]=this.getNaturalBrightnessFaceZAverageAt(x, y+1, z-1); cube[5+(SAN*1)]=this.getArtificialBrightnessFaceZAverageAt(x, y+1, z-1);
						cube[0+(SAN*2)]=(byte)(x+1);	cube[1+(SAN*2)]=y;				cube[2+(SAN*2)]=z;
						cube[3+(SAN*2)]=4000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*2)]=this.getNaturalBrightnessFaceZAverageAt(x+1, y, z-1); cube[5+(SAN*2)]=this.getArtificialBrightnessFaceZAverageAt(x+1, y, z-1);
						cube[0+(SAN*3)]=(byte)(x+1);	cube[1+(SAN*3)]=y;				cube[2+(SAN*3)]=z;
						cube[3+(SAN*3)]=4000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*3)]=this.getNaturalBrightnessFaceZAverageAt(x+1, y, z-1); cube[5+(SAN*3)]=this.getArtificialBrightnessFaceZAverageAt(x+1, y, z-1);
						cube[0+(SAN*4)]=x;				cube[1+(SAN*4)]=y+heightxmzm;	cube[2+(SAN*4)]=z;
						cube[3+(SAN*4)]=4000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*4)]=this.getNaturalBrightnessFaceZAverageAt(x, y+1, z-1); cube[5+(SAN*4)]=this.getArtificialBrightnessFaceZAverageAt(x, y+1, z-1);
						cube[0+(SAN*5)]=(byte)(x+1);	cube[1+(SAN*5)]=y+heightxpzm;	cube[2+(SAN*5)]=z;
						cube[3+(SAN*5)]=4000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*5)]=this.getNaturalBrightnessFaceZAverageAt(x+1, y+1, z-1); cube[5+(SAN*5)]=this.getArtificialBrightnessFaceZAverageAt(x+1, y+1, z-1);
						writeTarget.put(cube);c+=SAN*6;}
						if((z==CHUNK_DIMENSION-1&&mg.shouldDraw(neighbours[Direction.ZP.ordinal()].getCubeAt(x, y, 0),this.chunkCubes.get(x,y,z),liquidTag,Direction.ZP))
								||(z!=CHUNK_DIMENSION-1&&mg.shouldDraw(this.chunkCubes.get(x,y,z+1),this.chunkCubes.get(x,y,z),liquidTag,Direction.ZP))){
						cube[0+(SAN*0)]=x;				cube[1+(SAN*0)]=y;				cube[2+(SAN*0)]=(byte)(z+1);
						cube[3+(SAN*0)]=5000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*0)]=this.getNaturalBrightnessFaceZAverageAt(x, y, z+1); cube[5+(SAN*0)]=this.getArtificialBrightnessFaceZAverageAt(x, y, z+1);
						cube[0+(SAN*1)]=(byte)(x+1);	cube[1+(SAN*1)]=y;				cube[2+(SAN*1)]=(byte)(z+1);
						cube[3+(SAN*1)]=5000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*1)]=this.getNaturalBrightnessFaceZAverageAt(x+1, y, z+1); cube[5+(SAN*1)]=this.getArtificialBrightnessFaceZAverageAt(x+1, y, z+1);
						cube[0+(SAN*2)]=x;				cube[1+(SAN*2)]=y+heightxmzp;	cube[2+(SAN*2)]=(byte)(z+1);
						cube[3+(SAN*2)]=5000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*2)]=this.getNaturalBrightnessFaceZAverageAt(x, y+1, z+1); cube[5+(SAN*2)]=this.getArtificialBrightnessFaceZAverageAt(x, y+1, z+1);
						cube[0+(SAN*3)]=x;				cube[1+(SAN*3)]=y+heightxmzp;	cube[2+(SAN*3)]=(byte)(z+1);
						cube[3+(SAN*3)]=5000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*3)]=this.getNaturalBrightnessFaceZAverageAt(x, y+1, z+1); cube[5+(SAN*3)]=this.getArtificialBrightnessFaceZAverageAt(x, y+1, z+1);
						cube[0+(SAN*4)]=(byte)(x+1);	cube[1+(SAN*4)]=y;				cube[2+(SAN*4)]=(byte)(z+1);
						cube[3+(SAN*4)]=5000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*4)]=this.getNaturalBrightnessFaceZAverageAt(x+1, y, z+1); cube[5+(SAN*4)]=this.getArtificialBrightnessFaceZAverageAt(x+1, y, z+1);
						cube[0+(SAN*5)]=(byte)(x+1);	cube[1+(SAN*5)]=y+heightxpzp;	cube[2+(SAN*5)]=(byte)(z+1);
						cube[3+(SAN*5)]=5000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*5)]=this.getNaturalBrightnessFaceZAverageAt(x+1, y+1, z+1); cube[5+(SAN*5)]=this.getArtificialBrightnessFaceZAverageAt(x+1, y+1, z+1);
						writeTarget.put(cube);c+=SAN*6;} 
						
						}
						
						if(overrideDrawTop) overrideDrawBot=true;
						else overrideDrawBot=false;
						
						overrideDrawTop=false;
						if(liquidTag)
							liquidCont+=c;
						else
							bufferCont+=c;
					}
				}
			}
		}
		toUpload.flip();
		toUploadLiquid.flip();

		if(this.deleted){
			FloatBufferPool.recycleBuffer(toUpload);
			FloatBufferPool.recycleBuffer(toUploadLiquid);
		}
		else{
			this.toUpload=toUpload;
			this.toUploadLiquid=toUploadLiquid;
		}
		if(safeAccess){
			this.getUpdateAccessSemaphore().release();
		}
		}
		}
		this.triangleLiquidNum=liquidCont/SAN;
		this.triangleNum=bufferCont/SAN;
	}
	public void update()
	{
		if(this.changed) {
			if(this.updateFlag) this.changed=false;
			else{
				/*if(this.lightCalculatedFlag)*/ this.WF.requestChunkUpdatePrioritary(this);
				//else this.WF.requestChunkUpdate(this);
			}
		}
		if(this.toUpload!=null) //|TODO Graphic card overload
		{
			if(this.getUpdateAccessSemaphore().tryAcquire()){
				if(this.triangleNum==0&&this.triangleLiquidNum==0){
					//Bye array!
					if(this.chunkCubes.isTrueStorage()){
						byte defval=this.chunkCubes.get(0, 0, 0);
						this.chunkCubes.dispose();
						this.chunkCubes=new ConstantValueCubeStorage(defval,this,CubeStorageType.CUBES_STORAGE);
					}
					if(this.vbo!=-1) {
						glDeleteBuffers(this.vbo);
						this.vbo=-1;
					}
				}
				else{
					if(this.vbo==-1) this.vbo=glGenBuffers();
					if(!this.chunkCubes.isTrueStorage()){
						this.notifyCubeStorageUpdate(this.chunkCubes.get(0, 0, 0), CubeStorageType.CUBES_STORAGE);
					}
					glBindBuffer(GL15.GL_ARRAY_BUFFER,this.vbo);
					glBufferData(GL15.GL_ARRAY_BUFFER,(this.triangleNum+this.triangleLiquidNum)*SAN*4,GL15.GL_STATIC_DRAW);
					glBufferSubData(GL15.GL_ARRAY_BUFFER,0,this.toUpload);
					glBufferSubData(GL15.GL_ARRAY_BUFFER,this.triangleNum*SAN*4,this.toUploadLiquid);
					glBindBuffer(GL15.GL_ARRAY_BUFFER,0);
				}
				FloatBufferPool.recycleBuffer(this.toUpload);
				FloatBufferPool.recycleBuffer(this.toUploadLiquid);
				this.toUpload=null;
				this.toUploadLiquid=null;
				this.solidEmpty=false;
				this.liquidEmpty=false;
				if(this.triangleNum==0) this.solidEmpty=true;
				if(this.triangleLiquidNum==0) this.liquidEmpty=true;
				this.getUpdateAccessSemaphore().release();
			}
		}
	}
	public void draw(Camera c,VoxelShaderProgram VSP,BoundaryChecker bc)
	{
		if(changed||this.toUpload!=null) update();
		if(this.solidEmpty&&this.liquidEmpty) return;
		this.drawed=true;
		if(bc==null){
			Matrix4f mvp=new Matrix4f();
			//Matrix4f.translate(this.WF.getWorldCenterVector(), this.chunkModelMatrix, mvp); System.out.println(this.WF.getWorldCenterVector());System.out.println(this.chunkModelMatrix); System.out.println(mvp); System.out.println("_-_-_-_");
			//mvp.m30=-this.WF.getCameraCenter().x;mvp.m31=-this.WF.getCameraCenter().y;mvp.m32=-this.WF.getCameraCenter().z;
			//Matrix4f.sub(this.chunkModelMatrix, mvp, mvp);
			//float m30=this.chunkModelMatrix.m30; float m31=this.chunkModelMatrix.m31; float m32=this.chunkModelMatrix.m32; 
			this.chunkModelMatrix.m30=(float)(chunkx*Chunk.CHUNK_DIMENSION-this.WF.getCameraCenter().x); this.chunkModelMatrix.m31=(float)(chunky*Chunk.CHUNK_DIMENSION-this.WF.getCameraCenter().y); this.chunkModelMatrix.m32=(float)(chunkz*Chunk.CHUNK_DIMENSION-this.WF.getCameraCenter().z);
			Matrix4f.mul(c.getProjectionViewMatrix(), this.chunkModelMatrix, mvp);
			//this.chunkModelMatrix.m30=m30; this.chunkModelMatrix.m31=m31; this.chunkModelMatrix.m32=m32;
		
			Vector4f coords=MatrixHelper.multiply(mvp, CENTER_VECTOR);
			float xc=coords.x/(coords.w);
			float yc=coords.y/(coords.w);
			double normDiam=CHUNK_RADIUS/Math.abs(coords.w);
			if(Math.abs(xc)>1+normDiam || Math.abs(yc)>1+normDiam || coords.z<-CHUNK_RADIUS){
				this.drawed=false;
			}
		}
		else{
			if(!bc.sharesBoundariesWith((float)(this.getX()*Chunk.CHUNK_DIMENSION + CENTER_VECTOR.x-this.WF.getCameraCenter().x), 
										(float)(this.getY()*Chunk.CHUNK_DIMENSION + CENTER_VECTOR.y-this.WF.getCameraCenter().y), 
										(float)(this.getZ()*Chunk.CHUNK_DIMENSION+ CENTER_VECTOR.z-this.WF.getCameraCenter().z), 
										(float)CHUNK_RADIUS)) 									this.drawed=false;
		}
		if(drawed){
			if(this.solidEmpty) return;
			glBindBuffer(GL15.GL_ARRAY_BUFFER,this.vbo);
			VSP.setupAttributes();
			MatrixHelper.uploadTranslationMatrix(this.chunkModelMatrix,chunkx*Chunk.CHUNK_DIMENSION,chunky*Chunk.CHUNK_DIMENSION,chunkz*Chunk.CHUNK_DIMENSION,this.WF.getCameraCenter(),VSP.getModelMatrixLoc());
			glDrawArrays(GL_TRIANGLES, 0, this.triangleNum);
			glBindBuffer(GL15.GL_ARRAY_BUFFER,0);
		}
	}
	public void drawLiquids(Camera c,VoxelShaderProgram VSP)
	{
		if(this.liquidEmpty) return;
		if(this.drawed){
			glBindBuffer(GL15.GL_ARRAY_BUFFER,this.vbo);
			VSP.setupAttributes();
			//this.chunkModelMatrix=Matrix4f.translate(new Vector3f(0f,-0.3f,0f), this.chunkModelMatrix, this.chunkModelMatrix);
			MatrixHelper.uploadTranslationMatrix(this.chunkModelMatrix,chunkx*Chunk.CHUNK_DIMENSION,chunky*Chunk.CHUNK_DIMENSION,chunkz*Chunk.CHUNK_DIMENSION,this.WF.getCameraCenter(),VSP.getModelMatrixLoc());
			//this.chunkModelMatrix=Matrix4f.translate(new Vector3f(0f,0.3f,0f), this.chunkModelMatrix, this.chunkModelMatrix);
			glDrawArrays(GL_TRIANGLES, this.triangleNum,this.triangleLiquidNum);
			glBindBuffer(GL15.GL_ARRAY_BUFFER,0);
		}
	}
	
	public boolean updateChunkCubes(float tEl)
	{
		if(this.updateCubes.size()>0)
		{
			List<CubePosition> updateCubesLocal=this.updateCubes;
			this.updateCubes=new LinkedList<CubePosition>();
			for(CubePosition cp:updateCubesLocal)
			{
				byte cube=this.getCubeAt(cp.x, cp.y, cp.z);
				if(BlockLibrary.isLiquid(cube))
				{
					//if(1==2/2)continue; //|TODO DEBUG
					byte belowCube=getCubeAt(cp.x,cp.y-1,cp.z);
					
					if(!BlockLibrary.isDrawable(belowCube)){
						setCubeAt(cp.x,cp.y-1,cp.z,cube);
						//setCubeAt(cp.x,cp.y,cp.z,(byte)0);
					}
					else if(1==2/2)continue;
					else if(BlockLibrary.isSameBlock(cube, belowCube) && BlockLibrary.getLiquidLevel(belowCube)<BlockLibrary.getLiquidMaxLevel(belowCube)){
						int liqlev=BlockLibrary.getLiquidLevel(belowCube)+BlockLibrary.getLiquidLevel(cube) + 1;
						if(liqlev>BlockLibrary.getLiquidMaxLevel(cube)){
							setCubeAt(cp.x,cp.y-1,cp.z,(byte)(cube+BlockLibrary.getLiquidLevel(cube)-BlockLibrary.getLiquidMaxLevel(cube)));
							setCubeAt(cp.x,cp.y,cp.z,(byte)(cube+BlockLibrary.getLiquidLevel(cube)-(liqlev-BlockLibrary.getLiquidMaxLevel(cube))));
						}
						else{
							setCubeAt(cp.x,cp.y-1,cp.z,(byte)(cube+BlockLibrary.getLiquidLevel(cube)-liqlev));
							setCubeAt(cp.x,cp.y,cp.z,(byte)0);
						}
					}
					else{
						byte baseCube=(byte)(cube+BlockLibrary.getLiquidLevel(cube)-BlockLibrary.getLiquidMaxLevel(cube));
						int adj=0;
						int xp=-1;int xm=-1;int zp=-1; int zm=-1;
						int liquidlevel=BlockLibrary.getLiquidLevel(cube);
						
						if(!BlockLibrary.isDrawable(getCubeAt(cp.x+1,cp.y,cp.z))){
							adj++; xp=0;
						}
						else if(BlockLibrary.isSameBlock(cube, getCubeAt(cp.x+1,cp.y,cp.z))){
							adj++; xp=1; liquidlevel+=BlockLibrary.getLiquidLevel(getCubeAt(cp.x+1,cp.y,cp.z));
						}
						
						if(!BlockLibrary.isDrawable(getCubeAt(cp.x-1,cp.y,cp.z))){
							adj++;xm=0;
						}
						else if(BlockLibrary.isSameBlock(cube, getCubeAt(cp.x-1,cp.y,cp.z))){
							adj++;xm=1;liquidlevel+=BlockLibrary.getLiquidLevel(getCubeAt(cp.x-1,cp.y,cp.z));
						}
						
						if(!BlockLibrary.isDrawable(getCubeAt(cp.x,cp.y,cp.z+1))){
							adj++;zp=0;
						}
						else if(BlockLibrary.isSameBlock(cube, getCubeAt(cp.x,cp.y,cp.z+1))){
							adj++;zp=1;liquidlevel+=BlockLibrary.getLiquidLevel(getCubeAt(cp.x,cp.y,cp.z+1));
						}
						
						if(!BlockLibrary.isDrawable(getCubeAt(cp.x,cp.y,cp.z-1))){
							adj++;zm=0;
						}
						else if(BlockLibrary.isSameBlock(cube, getCubeAt(cp.x,cp.y,cp.z-1))){
							adj++;zm=1;liquidlevel+=BlockLibrary.getLiquidLevel(getCubeAt(cp.x,cp.y,cp.z-1));
						}
						
						int divlevel=liquidlevel/(adj+1); 
						int extra=liquidlevel%(adj+1); 
						boolean del=false;
						
						if(xp==0 ){
							if(extra==0&&divlevel>0) {divlevel--;extra=adj;}
							if(extra>0){
								extra--;
								setCubeAt(cp.x+1,cp.y,cp.z,(byte)(baseCube+(BlockLibrary.getLiquidMaxLevel(cube))));
								xp=1;
							}
							else del=true;
						}
						if(xm==0 ){ 
							if(extra==0&&divlevel>0) {divlevel--;extra=adj;}
							if(extra>0){
								extra--;
								setCubeAt(cp.x-1,cp.y,cp.z,(byte)(baseCube+(BlockLibrary.getLiquidMaxLevel(cube))));
								xm=1;
							}
							else del=true;
						}
						if(zp==0 ){ 
							if(extra==0&&divlevel>0) {divlevel--;extra=adj;}
							if(extra>0){
								extra--;
								setCubeAt(cp.x,cp.y,cp.z+1,(byte)(baseCube+(BlockLibrary.getLiquidMaxLevel(cube))));
								zp=1;
							}
							else del=true;
						}
						if(zm==0 ){ 
							if(extra==0&&divlevel>0) {divlevel--;extra=adj;}
							if(extra>0){
								extra--;
								setCubeAt(cp.x,cp.y,cp.z-1,(byte)(baseCube+(BlockLibrary.getLiquidMaxLevel(cube))));
								zm=1;
							}
							else del=true;
						}
						
						if(extra==1||extra==adj){
							this.liquidCounter--;
							if(liquidCounter<=0){
								liquidCounter=NORMALIZE_WATER_EACH;
								if(extra==1) extra=0; //|TODO MEECCC
								else if(extra==adj){extra=0; divlevel++;}
							}
						}
						/*if(del) setCubeAt(cp.x,cp.y,cp.z,(byte)0);
						else*/ if(adj!=0){
							int specificLiqLevel=divlevel;
							if(extra>0) {specificLiqLevel++; extra--;}
							byte cubeCode=(byte)(baseCube+(BlockLibrary.getLiquidMaxLevel(cube)-specificLiqLevel));
							if(getCubeAt(cp.x,cp.y,cp.z)!=cubeCode) setCubeAt(cp.x,cp.y,cp.z,cubeCode);
							
							if(BlockLibrary.isSameBlock(cube, getCubeAt(cp.x,cp.y+1,cp.z))&&BlockLibrary.getLiquidLevel(cubeCode)<BlockLibrary.getLiquidMaxLevel(cubeCode)) this.markCubeToUpdate(cp.x, cp.y+1, cp.z);
						}
						if(xp>=0){
							int specificLiqLevel=divlevel+xp;
							if(extra>0) {specificLiqLevel++; extra--;}
							if(specificLiqLevel>0){
							byte cubeCode=(byte)(baseCube+(BlockLibrary.getLiquidMaxLevel(cube)+1-specificLiqLevel));
							if(getCubeAt(cp.x+1,cp.y,cp.z)!=cubeCode) setCubeAt(cp.x+1,cp.y,cp.z,cubeCode);}
						}
						if(xm>=0){
							int specificLiqLevel=divlevel+xm;
							if(extra>0) {specificLiqLevel++; extra--;}
							if(specificLiqLevel>0){
							byte cubeCode=(byte)(baseCube+(BlockLibrary.getLiquidMaxLevel(cube)+1-specificLiqLevel));
							if(getCubeAt(cp.x-1,cp.y,cp.z)!=cubeCode) setCubeAt(cp.x-1,cp.y,cp.z,cubeCode);}
						}
						if(zp>=0){
							int specificLiqLevel=divlevel+zp;
							if(extra>0) {specificLiqLevel++; extra--;}
							if(specificLiqLevel>0){
							byte cubeCode=(byte)(baseCube+(BlockLibrary.getLiquidMaxLevel(cube)+1-specificLiqLevel));
							if(getCubeAt(cp.x,cp.y,cp.z+1)!=cubeCode) setCubeAt(cp.x,cp.y,cp.z+1,cubeCode);}
						}
						if(zm>=0){
							int specificLiqLevel=divlevel+zm;
							if(extra>0) {specificLiqLevel++; extra--;}
							if(specificLiqLevel>0){
							byte cubeCode=(byte)(baseCube+(BlockLibrary.getLiquidMaxLevel(cube)+1-specificLiqLevel));
							if(getCubeAt(cp.x,cp.y,cp.z-1)!=cubeCode) setCubeAt(cp.x,cp.y,cp.z-1,cubeCode);}
						}
					}
				}
				else if(!BlockLibrary.isDrawable(cube)){
					if(BlockLibrary.isLiquid(getCubeAt(cp.x,cp.y+1,cp.z))){
						markCubeToUpdate(cp.x,cp.y+1,cp.z);
					}
					else if(BlockLibrary.isLiquid(getCubeAt(cp.x+1,cp.y,cp.z))){
						markCubeToUpdate(cp.x+1,cp.y,cp.z);
					}
					else if(BlockLibrary.isLiquid(getCubeAt(cp.x-1,cp.y,cp.z))){
						markCubeToUpdate(cp.x-1,cp.y,cp.z);
					}
					else if(BlockLibrary.isLiquid(getCubeAt(cp.x,cp.y,cp.z+1))){
						markCubeToUpdate(cp.x,cp.y,cp.z+1);
					}
					else if(BlockLibrary.isLiquid(getCubeAt(cp.x,cp.y,cp.z-1))){
						markCubeToUpdate(cp.x,cp.y,cp.z-1);
					}
				}
				else if(cube==14){ //|TODO please change me
					int expPower=20;
					int expPower2=expPower*expPower;
					for(int x=-expPower;x<expPower;x++){
						
						int posy=(int)Math.sqrt(expPower2 - x*x);
						for(int y=-posy;y<posy;y++){
							int posz=(int)Math.sqrt(expPower2 - x*x - y*y);
							for(int z=-posz;z<posz;z++){
								if(!(x==0&&y==0&&z==0)&&getCubeAt(cp.x+x,cp.y+y,cp.z+z)==7) setCubeAt(cp.x+x,cp.y+y,cp.z+z,(byte)7);
								else setCubeAt(cp.x+x,cp.y+y,cp.z+z,(byte)0);
							}
						}
					}
				}
				else if(cube==15){ //|TODO please change me
					setCubeAt(cp.x,cp.y,cp.z,(byte)0);
					
					int cloudvol=3;
					int cloudheight=40;
					for(int x=-cloudvol;x<cloudvol;x++)
					{
						for(int y=-cloudvol;y<cloudvol;y++)
						{
							for(int z=-cloudvol;z<cloudvol;z++)
							{
								setCubeAt(cp.x+x,cp.y+cloudheight+y,cp.z+z,(byte)4);
							}
						}
					}
				}
				else if(cube==16){
					setCubeAt(cp.x,cp.y,cp.z,(byte)1);
					for(int i=1;i<Chunk.CHUNK_DIMENSION*(World.HEIGHT-this.chunky)-cp.y;i++)
					{
						if(getCubeAt(cp.x,cp.y+i,cp.z)==0)setCubeAt(cp.x,cp.y+i,cp.z,(byte)1);
						else break;
					}
				}
				else if(cube==17){
					setCubeAt(cp.x,cp.y,cp.z,(byte)0);
					int radius=5;
					int expPower2=radius*radius;
					int offsetx=5;
					int ppx=cp.x;
					int ppy=cp.y;
					int ppz=cp.z+10;
					for(int x=-radius;x<radius;x++){
						
						int posy=(int)Math.sqrt(expPower2 - x*x);
						for(int y=-posy;y<posy;y++){
							int posz=(int)Math.sqrt(expPower2 - x*x - y*y);
							for(int z=-posz;z<posz;z++){
								setCubeAt(ppx+x+offsetx,ppy+y,ppz+z,(byte)3);
							}
						}
					}
					offsetx=-5;
					for(int x=-radius;x<radius;x++){
						
						int posy=(int)Math.sqrt(expPower2 - x*x);
						for(int y=-posy;y<posy;y++){
							int posz=(int)Math.sqrt(expPower2 - x*x - y*y);
							for(int z=-posz;z<posz;z++){
								setCubeAt(ppx+x+offsetx,ppy+y,ppz+z,(byte)3);
							}
						}
					}
					radius=3;
					for(int x=-radius;x<radius;x++){
						
						int posy=(int)Math.sqrt(radius*radius - x*x);
						for(int z=-posy;z<posy;z++){
							for(int y=0;y<40;y++) setCubeAt(ppx+x,ppy+y,ppz+z+3,(byte)3);
						}
					}
					radius=4;
					for(int x=-radius;x<radius;x++){
						
						int posy=(int)Math.sqrt(radius*radius - x*x);
						for(int z=-posy;z<posy;z++){
							for(int y=30;y<37;y++) setCubeAt(ppx+x,ppy+y,ppz+z+3,(byte)3);
						}
					}
					radius=2;
					for(int x=-radius;x<radius;x++){
						
						int posy=(int)Math.sqrt(radius*radius - x*x);
						for(int z=-posy;z<posy;z++){
							setCubeAt(ppx+x,ppy+40,ppz+z+3,(byte)3);
						}
					}
					
					for(int y=41;y<300;y++)
					{
						int offset=y-41;
						for(int x=-offset;x<offset;x++){
							for(int z=-offset;z<offset;z++){
								if(Math.random()<0.2/offset) setCubeAt(ppx+x,ppy+y,ppz+z+3,(byte)12);
							}
						}
					}
				}
			}
			
			return this.updateCubes.size()>0;
		}
		return false;
	}
	
	/**
	 * Called when the current ConstantValueCubeStorage detects a set() with different value than the constant. The chunk then has to switch to a ArrayCubeStorage
	 */
	public void notifyCubeStorageUpdate(byte defaultVal,CubeStorageType notifierType){
		this.notifyCubeStorageUpdate(-1, -1, -1, (byte)0, defaultVal, notifierType);
	}
	public void notifyCubeStorageUpdate(int x,int y,int z, byte val,byte defaultVal,CubeStorageType notifierType)
	{
		boolean set=x>=0&&y>=0&&z>=0;
		switch(notifierType)
		{
		case CUBES_STORAGE:
			//If air
			if(defaultVal==0){
				this.chunkCubes.dispose();
				this.chunkCubes=new ArrayCubeStorage(ByteArrayPool.getArray(),true);
				if(set) this.chunkCubes.set(x, y, z, val);
			}
			//If liquid
			else if (BlockLibrary.isLiquid(defaultVal)){
				byte[][][] cubeArray=ByteArrayPool.getArrayUncleaned();
				for(int lx=0;lx<cubeArray.length;lx++)
				{
					for(int ly=0;ly<cubeArray.length;ly++)
					{
						for(int lz=0;lz<cubeArray.length;lz++)
						{
							cubeArray[lx][ly][lz]=defaultVal;
						}
					}
				}
				this.chunkCubes.dispose();
				this.chunkCubes=new ArrayCubeStorage(cubeArray,false);
				if(set) this.chunkCubes.set(x, y, z, val);
			}
			//If solid
			else{
				byte[][][] cubes=ByteArrayPool.getArrayUncleaned();
				ChunkGenerationResult arrayEmpty=WF.getMapHandler().getChunk(this.getX(), this.getY(),this.getZ(),cubes);
				if(arrayEmpty==ChunkGenerationResult.CHUNK_HIGHER_THAN_HEIGHTMAP){
					ByteArrayPool.recycleArray(cubes);
					cubes=ByteArrayPool.getArray();
				}
				this.chunkCubes.dispose();
				this.chunkCubes=new ArrayCubeStorage(cubes,arrayEmpty!=ChunkGenerationResult.CHUNK_NORMAL);
				if(set) this.chunkCubes.set(x, y, z, val);
			}
			break;
		case LIGHT_STORAGE:
			byte[][][] lightarray=null;
			if(defaultVal==0) lightarray=ByteArrayPool.getArray(); 
			else 
			{
				lightarray=ByteArrayPool.getArrayUncleaned();
				for(int lx=0;lx<lightarray.length;lx++)
				{
					for(int ly=0;ly<lightarray.length;ly++)
					{
						for(int lz=0;lz<lightarray.length;lz++)
						{
							lightarray[lx][ly][lz]=defaultVal;
						}
					}
				}
			}
			this.chunkCubesLight.dispose();
			this.chunkCubesLight=new ArrayCubeStorage(lightarray,defaultVal==0);
			if(set) this.chunkCubesLight.set(x, y, z, val);
			break;
		}
	}
	
	public byte getCubeAt(int x,int y,int z)
	{
		if( x<CHUNK_DIMENSION&&x>=0&&
				y<CHUNK_DIMENSION&&y>=0&&
				z<CHUNK_DIMENSION&&z>=0	) 
		{
			return this.chunkCubes.get(x,y,z);
		}
		else
		{
			int cx=this.chunkx;
			int cy=this.chunky;
			int cz=this.chunkz;
			if(x>=CHUNK_DIMENSION) {cx++;x=x-CHUNK_DIMENSION;} else if(x<0) {cx--;x=x+CHUNK_DIMENSION;}
			if(y>=CHUNK_DIMENSION) {cy++;y=y-CHUNK_DIMENSION;} else if(y<0) {cy--;y=y+CHUNK_DIMENSION;}
			if(z>=CHUNK_DIMENSION) {cz++;z=z-CHUNK_DIMENSION;} else if(z<0) {cz--;z=z+CHUNK_DIMENSION;}
			Chunk c=this.WF.getChunkByIndex(cx, cy, cz);
			if(c==null) return (byte)(0);
			else return c.getCubeAt(x, y, z);
		}
	}
	public void markCubeToUpdate(int x,int y,int z)
	{
		if( x<CHUNK_DIMENSION&&x>=0&&
				y<CHUNK_DIMENSION&&y>=0&&
				z<CHUNK_DIMENSION&&z>=0	) 
		{
			//Cube update
			Iterator<CubePosition> i=this.updateCubes.iterator();
			while(i.hasNext())
			{
				CubePosition cp=i.next();
				if(cp.x==x&&cp.y==y&&cp.z==z) return;//i.remove();
			}
			if(this.updateCubes.size()==0) this.WF.insertChunkInUpdateList(this);
			this.updateCubes.add(new CubePosition(x,y,z));
		}
		else
		{
			int cx=this.chunkx;
			int cy=this.chunky;
			int cz=this.chunkz;
			if(x>=CHUNK_DIMENSION) {cx++;x=x-CHUNK_DIMENSION;} else if(x<0) {cx--;x=x+CHUNK_DIMENSION;}
			if(y>=CHUNK_DIMENSION) {cy++;y=y-CHUNK_DIMENSION;} else if(y<0) {cy--;y=y+CHUNK_DIMENSION;}
			if(z>=CHUNK_DIMENSION) {cz++;z=z-CHUNK_DIMENSION;} else if(z<0) {cz--;z=z+CHUNK_DIMENSION;}
			Chunk c=this.WF.getChunkByIndex(cx, cy, cz);
			if(c!=null) c.markCubeToUpdate(x, y, z);
		}
	}
	public float getCubeHeight(int x,int y,int z,byte cube)
	{
		if(!BlockLibrary.isLiquid(cube)) return 1f;
		
		float neigh=0;
		float accumHeight=0;
		byte cubeNeigh=this.getCubeAt(x, y, z);
		if(BlockLibrary.isSameBlock(cube, cubeNeigh)){
			neigh++;
			accumHeight+=BlockLibrary.getLiquidLevel(cubeNeigh);
		}
		cubeNeigh=this.getCubeAt(x-1, y, z);
		if(BlockLibrary.isSameBlock(cube, cubeNeigh)){
			neigh++;
			accumHeight+=BlockLibrary.getLiquidLevel(cubeNeigh);
		}
		cubeNeigh=this.getCubeAt(x, y, z-1);
		if(BlockLibrary.isSameBlock(cube, cubeNeigh)){
			neigh++;
			accumHeight+=BlockLibrary.getLiquidLevel(cubeNeigh);
		}
		cubeNeigh=this.getCubeAt(x-1, y, z-1);
		if(BlockLibrary.isSameBlock(cube, cubeNeigh)){
			neigh++;
			accumHeight+=BlockLibrary.getLiquidLevel(cubeNeigh);
		}
		
		return (accumHeight+(neigh/3))/(neigh*(BlockLibrary.getLiquidMaxLevel(cube)+0.33f));
	}
	public void setCubeAt(int x,int y,int z,byte val)
	{
		if( x<CHUNK_DIMENSION&&x>=0&&
				y<CHUNK_DIMENSION&&y>=0&&
				z<CHUNK_DIMENSION&&z>=0	) 
		{
			if(this.chunky==World.HEIGHT-1 && y==CHUNK_DIMENSION-1) return;
			
			if(x==0) 
			{
				Chunk c=this.WF.getChunkByIndex(this.chunkx-1, this.chunky, this.chunkz);
				if(c!=null) c.changed=true;
			}
			else if(x==CHUNK_DIMENSION-1) 
			{
				Chunk c=this.WF.getChunkByIndex(this.chunkx+1, this.chunky, this.chunkz);
				if(c!=null) c.changed=true;
			}
			if(y==0) 
			{
				Chunk c=this.WF.getChunkByIndex(this.chunkx, this.chunky-1, this.chunkz);
				if(c!=null) c.changed=true;
			}
			else if(y==CHUNK_DIMENSION-1) 
			{
				Chunk c=this.WF.getChunkByIndex(this.chunkx, this.chunky+1, this.chunkz);
				if(c!=null) c.changed=true;
			}
			if(z==0) 
			{
				Chunk c=this.WF.getChunkByIndex(this.chunkx, this.chunky, this.chunkz-1);
				if(c!=null) c.changed=true;
			}
			else if(z==CHUNK_DIMENSION-1) 
			{
				Chunk c=this.WF.getChunkByIndex(this.chunkx, this.chunky, this.chunkz+1);
				if(c!=null) c.changed=true;
			}
			this.changed=true;
			
			boolean lightDeleted=BlockLibrary.isLightSource(this.chunkCubes.get(x,y,z));
			
			markCubeToUpdate(x,y,z);
			
			this.chunkCubes.set(x,y,z,val);
			if(this.isLightCalculated())
			{
				if(lightDeleted) {
					removeArtificialBrightnessAt(x,y,z);
				}
			
				if(!BlockLibrary.isOpaque(val))
				{
					recalculateBrightnessOfCube(x,y,z);
				}
				else{
					removeArtificialBrightnessAt(x,y,z);
					byte exNaturalBright=getNaturalLightLevelAt(x,y,z);
					removeNaturalBrightnessAt(x,y,z);
					if((this.chunky==World.HEIGHT-1&&y==CHUNK_DIMENSION-1)||exNaturalBright==15)
					{
						ArrayList<Chunk> downChunks=new ArrayList<Chunk>();
						for(int yc=this.chunky-1;yc>=0;yc--){
							Chunk chu=this.WF.getChunkByIndex(this.chunkx, yc, this.chunkz);
							downChunks.add(chu);
						}
						this.destroyNaturalLightInRay(x,y-1,z,downChunks,0);
						this.recalculateLightInRay(x, y-1, z, downChunks, 0);
					}	
				}
			}
		}
		else
		{
			int cx=this.chunkx;
			int cy=this.chunky;
			int cz=this.chunkz;
			if(x>=CHUNK_DIMENSION) {cx++;x=x-CHUNK_DIMENSION;} else if(x<0) {cx--;x=x+CHUNK_DIMENSION;}
			if(y>=CHUNK_DIMENSION) {cy++;y=y-CHUNK_DIMENSION;} else if(y<0) {cy--;y=y+CHUNK_DIMENSION;}
			if(z>=CHUNK_DIMENSION) {cz++;z=z-CHUNK_DIMENSION;} else if(z<0) {cz--;z=z+CHUNK_DIMENSION;}
			Chunk c=this.WF.getChunkByIndex(cx, cy, cz);
			if(c!=null) c.setCubeAt(x, y, z,val);
		}
	}
	
	public boolean isLightCalculated()
	{
		return this.lightCalculated;
	}
	public boolean isAllLightEqualTo(byte val)
	{
		if(!this.isLightCalculated() || this.chunkCubesLight.isTrueStorage() || this.chunkCubesLight.get(0, 0, 0)!=val) return false;
		return true;
	}
	public boolean isAllCubesEqualTo(byte val)
	{
		if(this.chunkCubes.isTrueStorage() || this.chunkCubes.get(0, 0, 0)!=val) return false;
		return true;
	}
	/**
	 * Returns true if there was no artifcial light to begin with.
	 */
	public boolean fillNaturalLightWith(byte val)
	{
		byte normVal=(byte)((val << 4) & 0xF0);
		boolean noArt=true;
		for(int x=0;x<Chunk.CHUNK_DIMENSION;x++)
		{
			for(int y=0;y<Chunk.CHUNK_DIMENSION;y++)
			{
				for(int z=0;z<Chunk.CHUNK_DIMENSION;z++)
				{
					this.setNaturalLightIn(x, y, z, val);
					if(noArt&&normVal!=this.chunkCubesLight.get(x, y, z)) noArt=false;
				}
			}
		}
		
		return noArt;
	}
	public void createLightMap()
	{
		//System.out.println("GEN "+getX()+" "+getY()+" "+getZ());
		if(this.chunkCubesLight!=null) this.chunkCubesLight.dispose();
		this.chunkCubesLight=new ArrayCubeStorage(ByteArrayPool.getArray(),true);
		boolean noNaturalLight=true;
		boolean fullNaturalLight=true;

		//boolean 
		
		//NATURAL LIGHT
		
		Chunk[] neighbours=this.WF.getNeighbours(this);
		boolean topFullNL=this.chunky==World.HEIGHT-1 || (neighbours[Direction.YP.ordinal()]!=null&&neighbours[Direction.YP.ordinal()].isAllLightEqualTo(NATURAL_LIGHT_FULL_VAL));
		boolean xpFullNL=(neighbours[Direction.XP.ordinal()]!=null&&neighbours[Direction.XP.ordinal()].isAllLightEqualTo(NATURAL_LIGHT_FULL_VAL));
		boolean xmFullNL=(neighbours[Direction.XM.ordinal()]!=null&&neighbours[Direction.XM.ordinal()].isAllLightEqualTo(NATURAL_LIGHT_FULL_VAL));
		boolean zpFullNL=(neighbours[Direction.ZP.ordinal()]!=null&&neighbours[Direction.ZP.ordinal()].isAllLightEqualTo(NATURAL_LIGHT_FULL_VAL));
		boolean zmFullNL=(neighbours[Direction.ZM.ordinal()]!=null&&neighbours[Direction.ZM.ordinal()].isAllLightEqualTo(NATURAL_LIGHT_FULL_VAL));
		
		boolean currentFullNL=false;
		//if(this.getX()==0&&this.getZ()==0&&this.getY()<5) System.out.println(this.getY()+" "+topFullNL+" "+neighbours[Direction.YP.ordinal()].chunkCubesLight.isTrueStorage()+" "+neighbours[Direction.YP.ordinal()].chunkCubesLight.get(0, 0, 0)+" "+this.NATURAL_LIGHT_FULL_VAL);
		if(topFullNL)
		{
			//GETTING ALL CHUNKS WITH LIGHT CALCULATED IN DOWN OF THIS ONE
			List<Chunk> downChunks=new ArrayList<Chunk>();
			int miny=this.getY();
			for(int y=this.getY()-1;y>=0;y--){
				Chunk chu=this.WF.getChunkByIndex(this.chunkx, y, this.chunkz);
				if(chu==null||!chu.lightCalculated) break;
				
				miny=y;
				downChunks.add(chu);
			}
			
			//ASSUME ALL CHUNKS WITH NO CUBES IN DOWN OF THIS ONE AS CHUNKS WITH FULL NATURAL LIGHT
			Chunk current=this;
			Iterator<Chunk> it=downChunks.iterator();
			while(current!=null&&current.isAllCubesEqualTo((byte)0))
			{
				noNaturalLight=false;
				current=it.hasNext()?it.next():null;
			}
			
			//For the remaining chunks, light rays needs to be computed separatelly
			int maxstopy=current==null?miny-1:-1;
			int minstopy=this.getY();
			if(current!=null)
			{
				for(int x=0;x<CHUNK_DIMENSION;x++)
				{
					for(int z=0;z<CHUNK_DIMENSION;z++)
					{
						if(!BlockLibrary.isOpaque(getCubeAt(x,CHUNK_DIMENSION-1,z)))
						{
							noNaturalLight=false;
							int stop=current.propagateNaturalLightRay(x,z,downChunks,0);
							if(stop>maxstopy) maxstopy=stop;
							if(stop<minstopy) minstopy=stop;
						}
					}
				}
			}
			
			//Check how many chunks are now full of natural light
			if(maxstopy>=this.getY()) fullNaturalLight=false;
			else{
				this.chunkCubesLight.dispose();
				this.chunkCubesLight=new ConstantValueCubeStorage(NATURAL_LIGHT_FULL_VAL,this,CubeStorageType.LIGHT_STORAGE);
				currentFullNL=true;
				for(Chunk c:downChunks){
					if(maxstopy<c.getY()) {
						//If there was no light here, all light is natural
						if(c.isAllLightEqualTo((byte)0)){
							c.chunkCubesLight.dispose();
							c.chunkCubesLight=new ConstantValueCubeStorage(NATURAL_LIGHT_FULL_VAL,c,CubeStorageType.LIGHT_STORAGE);
						}
						//If there was any light, manually update the light. If this light was not artificial, all light checked as natural too.
						else if(!c.isAllLightEqualTo(NATURAL_LIGHT_FULL_VAL)){
							//System.out.println(c.getX()+" "+c.getY()+" "+c.getZ());
							//System.out.println(getX()+" "+getY()+" "+getZ());
							if(c.fillNaturalLightWith(NATURAL_LIGHT_FULL_VAL)){
								c.chunkCubesLight.dispose();
								c.chunkCubesLight=new ConstantValueCubeStorage(NATURAL_LIGHT_FULL_VAL,c,CubeStorageType.LIGHT_STORAGE);
							}
						}
					}
					else break;
				}
			}
			
			//Extend natural light naturally with flood algorithm
			Chunk dc=this;
			Chunk topChunk=null;
			it=downChunks.iterator();
			while(dc!=null){
				if(dc.getY()<minstopy) break; //All chunks from here have no max value natural light
				if(xpFullNL&&xmFullNL&&zpFullNL&&zmFullNL) break;
				for(int x=0;x<CHUNK_DIMENSION;x++)
				{
					for(int z=0;z<CHUNK_DIMENSION;z++)
					{
						if(topChunk==null||topChunk.getNaturalLightIn(x, 0, z)==(byte)15)
						{
							for(int y=CHUNK_DIMENSION-1;y>=0;y--)
							{
								byte lp=dc.getNaturalLightIn(x,y,z);
								if(lp==15)
								{
									dc.setNaturalBrightnessAt(x,y,z,(byte)15);
								}
								else break;
							}
						}
					}
				}
				topChunk=dc;
				dc=it.hasNext()?it.next():null;
			}
		}
		else if(neighbours[Direction.YP.ordinal()]!=null && !neighbours[Direction.YP.ordinal()].isAllLightEqualTo((byte)0)){
			Chunk top=neighbours[Direction.YP.ordinal()];
			if(top!=null&&top.lightCalculated)
			{
				List<Chunk> downChunks=new ArrayList<Chunk>();
				for(int y=this.chunky-1;y>=0;y--){
					Chunk chu=this.WF.getChunkByIndex(this.chunkx, y, this.chunkz);
					if(chu==null||!chu.lightCalculated) break;
					downChunks.add(chu);
				}
				int maxstopy=-1;
				int minstopy=this.getY();
				for(int x=0;x<CHUNK_DIMENSION;x++)
				{
					for(int z=0;z<CHUNK_DIMENSION;z++)
					{
						if(top.getNaturalLightIn(x, 0, z)==(byte)15 && !BlockLibrary.isOpaque(getCubeAt(x,CHUNK_DIMENSION-1,z))) 
						{
							noNaturalLight=false;
							int stop=propagateNaturalLightRay(x,z,downChunks,0);
							if(stop>maxstopy) maxstopy=stop;
							if(stop<minstopy) minstopy=stop;
						}
						else {
							fullNaturalLight=false;
							maxstopy=this.getY();
						}
					}
				}
				if(maxstopy>=this.getY()) fullNaturalLight=false;
				else{
					for(Chunk c:downChunks){
						if(maxstopy<c.getY()) {
							c.chunkCubesLight.dispose();
							c.chunkCubesLight=new ConstantValueCubeStorage(NATURAL_LIGHT_FULL_VAL,c,CubeStorageType.LIGHT_STORAGE);
						}
						else break;
					}
				}
				
				Chunk dc=this;
				Chunk topChunk=top;
				Iterator<Chunk> it=downChunks.iterator();
				while(dc!=null){
					if(dc.getY()<minstopy) break; //All chunks from here have no max value natural light
					for(int x=0;x<CHUNK_DIMENSION;x++)
					{
						for(int z=0;z<CHUNK_DIMENSION;z++)
						{
							if(topChunk.getNaturalLightIn(x, 0, z)==(byte)15)
							{
								for(int y=CHUNK_DIMENSION-1;y>=0;y--)
								{
									byte lp=dc.getNaturalLightIn(x,y,z);
									if(lp==15)
									{
										dc.setNaturalBrightnessAt(x,y,z,(byte)15);
									}
									else break;
								}
							}
						}
					}
					topChunk=dc;
					dc=it.hasNext()?it.next():null;
				}
			}
			else fullNaturalLight=false;
			/*c=this.WF.getChunkByIndexStrict(this.chunkx,this.chunky+1,this.chunkz);
			if(c!=null)
			{
				ArrayList<Chunk> downChunks=new ArrayList<Chunk>();
				for(int y=this.chunky-1;y>=0;y--){
					downChunks.add(this.WF.getChunkByIndexStrict(this.chunkx, y, this.chunkz));
				}
				for(int x=0;x<CHUNK_DIMENSION;x++)
				{
					for(int z=0;z<CHUNK_DIMENSION;z++)
					{
						byte ll=c.getNaturalLightLevelAt(x, 0, z);
						if(ll==15)
						{
							propagateNaturalLightRay(x,z,downChunks,0);
						}
					}
				}
			}*/
		}
		else fullNaturalLight=false;
		//Propagate indirect natural light
		/*for(int x=0;x<CHUNK_DIMENSION;x++)
		{
			for(int y=0;y<CHUNK_DIMENSION;y++)
			{
				for(int z=0;z<CHUNK_DIMENSION;z++)
				{
					byte lp=getNaturalLightIn(x,y,z);
					if(lp==15)
					{
						setNaturalBrightnessAt(x,y,z,(byte)15);
					}
				}
			}
		}*/
		
		//NEIGHBORS
		
		//X+ CHUNK
		Chunk c=neighbours[Direction.XP.ordinal()];
		if(c!=null &&c.lightCalculated &&(!xpFullNL||!currentFullNL))
		{
			for(int z=0;z<CHUNK_DIMENSION;z++)
			{
				for(int y=0;y<CHUNK_DIMENSION;y++)
				{
					byte ll=c.getArtificialLightLevelAt(0, y, z);
					if(ll>1)
					{
						noNaturalLight=false; fullNaturalLight=false;
						setArtificialBrightnessAt(CHUNK_DIMENSION-1,y,z,(byte)(ll-1));
					}
					ll=c.getNaturalLightLevelAt(0, y, z);
					if(ll>1)
					{
						noNaturalLight=false;
						setNaturalBrightnessAt(CHUNK_DIMENSION-1,y,z,(byte)(ll-1));
					}
				}
			}
		}
		//X- CHUNK
		c=neighbours[Direction.XM.ordinal()];
		if(c!=null &&c.lightCalculated &&(!xmFullNL||!currentFullNL))
		{
			for(int z=0;z<CHUNK_DIMENSION;z++)
			{
				for(int y=0;y<CHUNK_DIMENSION;y++)
				{
					byte ll=c.getArtificialLightLevelAt(CHUNK_DIMENSION-1, y, z);
					if(ll>1)
					{
						noNaturalLight=false; fullNaturalLight=false;
						setArtificialBrightnessAt(0,y,z,(byte)(ll-1));
					}
					ll=c.getNaturalLightLevelAt(CHUNK_DIMENSION-1, y, z);
					if(ll>1)
					{
						noNaturalLight=false;
						setNaturalBrightnessAt(0,y,z,(byte)(ll-1));
					}
				}
			}
		}
		//Z+ CHUNK
		c=neighbours[Direction.ZP.ordinal()];
		if(c!=null &&c.lightCalculated  &&(!zpFullNL||!currentFullNL))
		{
			for(int x=0;x<CHUNK_DIMENSION;x++)
			{
				for(int y=0;y<CHUNK_DIMENSION;y++)
				{
					byte ll=c.getArtificialLightLevelAt(x, y, 0);
					if(ll>1)
					{
						noNaturalLight=false; fullNaturalLight=false;
						setArtificialBrightnessAt(x,y,CHUNK_DIMENSION-1,(byte)(ll-1));
					}
					ll=c.getNaturalLightLevelAt(x, y, 0);
					if(ll>1)
					{
						noNaturalLight=false;
						setNaturalBrightnessAt(x,y,CHUNK_DIMENSION-1,(byte)(ll-1));
					}
				}
			}
		}
		//Z- CHUNK
		c=neighbours[Direction.ZM.ordinal()];
		if(c!=null &&c.lightCalculated  &&(!zmFullNL||!currentFullNL))
		{
			for(int x=0;x<CHUNK_DIMENSION;x++)
			{
				for(int y=0;y<CHUNK_DIMENSION;y++)
				{
					byte ll=c.getArtificialLightLevelAt(x, y, CHUNK_DIMENSION-1);
					if(ll>1)
					{
						noNaturalLight=false; fullNaturalLight=false;
						setArtificialBrightnessAt(x,y,0,(byte)(ll-1));
					}
					ll=c.getNaturalLightLevelAt(x, y, CHUNK_DIMENSION-1);
					if(ll>1)
					{
						noNaturalLight=false;
						setNaturalBrightnessAt(x,y,0,(byte)(ll-1));
					}
				}
			}
		}
		//Y+ CHUNK
		c=neighbours[Direction.YP.ordinal()];
		if(c!=null &&c.lightCalculated &&(!topFullNL||!currentFullNL))
		{
			for(int x=0;x<CHUNK_DIMENSION;x++)
			{
				for(int z=0;z<CHUNK_DIMENSION;z++)
				{
					byte ll=c.getArtificialLightLevelAt(x, 0, z);
					if(ll>1)
					{
						noNaturalLight=false; fullNaturalLight=false;
						setArtificialBrightnessAt(x,CHUNK_DIMENSION-1,z,(byte)(ll-1));
					}
					ll=c.getNaturalLightLevelAt(x, 0, z);
					if(ll>1)
					{
						/*if(ll==15) {
							//HOLYF ITS LIGHT
							LinkedList<Chunk> downChunks=new LinkedList<Chunk>();
							for(int y=this.chunky-1;y>=0;y--){
								Chunk chu=this.WF.getChunkByIndex(this.chunkx, y, this.chunkz);
								downChunks.add(chu);
							}

							propagateNaturalLightRay(x,z,downChunks,0);
							if(getNaturalLightIn(x,CHUNK_DIMENSION-1,z)!=15) setNaturalBrightnessAt(x,CHUNK_DIMENSION-1,z,(byte)(14));
							else
							{
								downChunks.addFirst(this);
								for(Chunk dc:downChunks)
								{
									if(dc==null ||(dc!=this && !dc.lightCalculated)) break;
									for(int y=CHUNK_DIMENSION-1;y>=0;y--)
									{
										byte lp=dc.getNaturalLightIn(x,y,z);
										if(lp==15)
										{
											dc.setNaturalBrightnessAt(x,y,z,(byte)15);
										}
										else break;
									}
								}
							}
						}*/
						if(ll!=15 || getNaturalBrightnessAt(x,CHUNK_DIMENSION-1,z)!=15) {
							noNaturalLight=false;
							setNaturalBrightnessAt(x,CHUNK_DIMENSION-1,z,(byte)(ll-1));
						}
					}
				}
			}
		}
		//Y- CHUNK
		c=neighbours[Direction.YM.ordinal()];
		if(c!=null &&c.lightCalculated)
		{
			for(int x=0;x<CHUNK_DIMENSION;x++)
			{
				for(int z=0;z<CHUNK_DIMENSION;z++)
				{
					byte ll=c.getArtificialLightLevelAt(x, CHUNK_DIMENSION-1,z);
					if(ll>1)
					{
						noNaturalLight=false; fullNaturalLight=false;
						setArtificialBrightnessAt(x,0,z,(byte)(ll-1));
					}
					ll=c.getNaturalLightLevelAt(x, CHUNK_DIMENSION-1,z);
					if(ll>1)
					{
						noNaturalLight=false;
						setNaturalBrightnessAt(x,0,z,(byte)(ll-1));
					}
				}
			}
		}
		
		if(noNaturalLight) {
			//System.out.println("No nat at "+getX()+" "+getY()+" "+getZ());
			this.chunkCubesLight.dispose();
			this.chunkCubesLight=new ConstantValueCubeStorage((byte)0,this,CubeStorageType.LIGHT_STORAGE);
		}
		else if(fullNaturalLight){
			//System.out.println("FULL nat at "+getX()+" "+getY()+" "+getZ());
			this.chunkCubesLight.dispose();
			this.chunkCubesLight=new ConstantValueCubeStorage(NATURAL_LIGHT_FULL_VAL,this,CubeStorageType.LIGHT_STORAGE);
		}
		//Lightmap calculated: Lets check for neighbors added (And inform near neighbors that this chunk has been added too)
		this.neighborsAdded=this.WF.getNeighboursAdded(this);
		this.WF.notifyNeighbours(this);
		this.lightCalculated=true;
	}
	//|TODO BUG INCOMING (Prop to down pls)
	private int propagateNaturalLightRay(int x,int z,List<Chunk> downChunk,int stage)
	{
		return this.propagateNaturalLightRay(x, z, CHUNK_DIMENSION-1,downChunk, stage);
	}
	private int propagateNaturalLightRay(int x,int z,int yi,List<Chunk> downChunk,int stage)
	{
		if(getNaturalLightIn(x,yi,z)==15) return getY();
		for(int y=yi;y>=0;y--)
		{
			if(BlockLibrary.isOpaque(this.chunkCubes.get(x,y,z))||BlockLibrary.isLiquid(this.chunkCubes.get(x,y,z))) return getY();
			setNaturalLightIn(x,y,z,(byte)15);
		}
		if(downChunk.size()>stage&&downChunk.get(stage)!=null)
		{
			return downChunk.get(stage).propagateNaturalLightRay(x, z,downChunk,stage+1);
		}
		return getY()-1;
	}
	private void destroyNaturalLightInRay(int x,int yi,int z,List<Chunk> downChunk,int stage)
	{
		if(yi>=0){
		if(getNaturalLightIn(x,yi,z)!=15) return;
		for(int y=yi;y>=0;y--)
		{
			if(BlockLibrary.isOpaque(this.chunkCubes.get(x,y,z))||BlockLibrary.isLiquid(this.chunkCubes.get(x,y,z))) return;
			this.removeNaturalBrightnessAt(x, y, z);
		}
		}
		if(downChunk.size()>stage&&downChunk.get(stage)!=null)
		{
			downChunk.get(stage).destroyNaturalLightInRay(x, CHUNK_DIMENSION-1,z,downChunk,stage+1);
		}
	}
	private void recalculateLightInRay(int x,int yi,int z,List<Chunk> downChunk,int stage)
	{
		if(yi>=0){
		for(int y=yi;y>=0;y--)
		{
			if(BlockLibrary.isOpaque(this.chunkCubes.get(x,y,z))||BlockLibrary.isLiquid(this.chunkCubes.get(x,y,z))) return;
			this.recalculateBrightnessOfCube(x, y, z);
		}
		}
		if(downChunk.size()>stage&&downChunk.get(stage)!=null)
		{
			downChunk.get(stage).recalculateLightInRay(x, CHUNK_DIMENSION-1,z,downChunk,stage+1);
		}
	}
	public byte getArtificialLightLevelAt(int x,int y,int z)
	{
		if( x<CHUNK_DIMENSION&&x>=0&&
			y<CHUNK_DIMENSION&&y>=0&&
			z<CHUNK_DIMENSION&&z>=0	) 
		{
			return getArtificialLightIn(x,y,z);
		}
		else
		{
			int cx=this.chunkx;
			int cy=this.chunky;
			int cz=this.chunkz;
			if(x>=CHUNK_DIMENSION) {cx++;x=x-CHUNK_DIMENSION;} else if(x<0) {cx--;x=x+CHUNK_DIMENSION;}
			if(y>=CHUNK_DIMENSION) {cy++;y=y-CHUNK_DIMENSION;} else if(y<0) {cy--;y=y+CHUNK_DIMENSION;}
			if(z>=CHUNK_DIMENSION) {cz++;z=z-CHUNK_DIMENSION;} else if(z<0) {cz--;z=z+CHUNK_DIMENSION;}
			Chunk c=this.WF.getChunkByIndex(cx, cy, cz);
			if(c==null) return (byte)(0);
			else return c.getArtificialLightLevelAt(x, y, z);
		}
	}
	public byte getNaturalLightLevelAt(int x,int y,int z)
	{
		if( x<CHUNK_DIMENSION&&x>=0&&
			y<CHUNK_DIMENSION&&y>=0&&
			z<CHUNK_DIMENSION&&z>=0	) 
		{
			return getNaturalLightIn(x,y,z);
		}
		else
		{
			int cx=this.chunkx;
			int cy=this.chunky;
			int cz=this.chunkz;
			if(x>=CHUNK_DIMENSION) {cx++;x=x-CHUNK_DIMENSION;} else if(x<0) {cx--;x=x+CHUNK_DIMENSION;}
			if(y>=CHUNK_DIMENSION) {cy++;y=y-CHUNK_DIMENSION;} else if(y<0) {cy--;y=y+CHUNK_DIMENSION;}
			if(z>=CHUNK_DIMENSION) {cz++;z=z-CHUNK_DIMENSION;} else if(z<0) {cz--;z=z+CHUNK_DIMENSION;}
			Chunk c=this.WF.getChunkByIndex(cx, cy, cz);
			
			if(c==null) return (byte)(0);
			else return c.getNaturalLightLevelAt(x, y, z);
		}
	}
	//|TODO Limit luminosity bug if chunk not loaded
	public float getArtificialBrightnessAt(int x,int y,int z)
	{
		return getArtificialLightLevelAt(x,y,z)/MAX_LIGHT_LEVEL;
	}
	public float getNaturalBrightnessAt(int x,int y,int z)
	{
		return getNaturalLightLevelAt(x,y,z)/MAX_LIGHT_LEVEL;
	}
	public float getArtificialBrightnessAverageAt(int x,int y,int z)
	{
		return  (getArtificialBrightnessAt(x,y,z)+getArtificialBrightnessAt(x-1,y,z)+
				getArtificialBrightnessAt(x,y-1,z)+getArtificialBrightnessAt(x,y,z-1)+
				getArtificialBrightnessAt(x-1,y-1,z)+getArtificialBrightnessAt(x,y-1,z-1)+
				getArtificialBrightnessAt(x-1,y,z-1)+getArtificialBrightnessAt(x-1,y-1,z-1))/4;
	}
	public float getArtificialBrightnessFaceYAverageAt(int x,int y,int z)
	{
		return  (getArtificialBrightnessAt(x,y,z)+getArtificialBrightnessAt(x-1,y,z)+
				getArtificialBrightnessAt(x,y,z-1)+getArtificialBrightnessAt(x-1,y,z-1))/4;
	}
	public float getArtificialBrightnessFaceZAverageAt(int x,int y,int z)
	{
		return  (getArtificialBrightnessAt(x,y,z)+getArtificialBrightnessAt(x-1,y,z)+
				getArtificialBrightnessAt(x,y-1,z)+getArtificialBrightnessAt(x-1,y-1,z))/4;
	}
	public float getArtificialBrightnessFaceXAverageAt(int x,int y,int z)
	{
		return  (getArtificialBrightnessAt(x,y,z)+getArtificialBrightnessAt(x,y-1,z)+
				getArtificialBrightnessAt(x,y,z-1)+getArtificialBrightnessAt(x,y-1,z-1))/4;
	}
	public float getNaturalBrightnessFaceYAverageAt(int x,int y,int z)
	{
		//if (true) return getNaturalBrightnessAverageAt(x,y,z);
		return  (getNaturalBrightnessAt(x,y,z)+getNaturalBrightnessAt(x-1,y,z)+
				getNaturalBrightnessAt(x,y,z-1)+getNaturalBrightnessAt(x-1,y,z-1))/4;
	}
	public float getNaturalBrightnessFaceZAverageAt(int x,int y,int z)
	{
		//if (true) return getNaturalBrightnessAverageAt(x,y,z);
		return  (getNaturalBrightnessAt(x,y,z)+getNaturalBrightnessAt(x-1,y,z)+
				getNaturalBrightnessAt(x,y-1,z)+getNaturalBrightnessAt(x-1,y-1,z))/4;
	}
	public float getNaturalBrightnessFaceXAverageAt(int x,int y,int z)
	{
		//if (true) return getNaturalBrightnessAverageAt(x,y,z);
		return  (getNaturalBrightnessAt(x,y,z)+getNaturalBrightnessAt(x,y-1,z)+
				getNaturalBrightnessAt(x,y,z-1)+getNaturalBrightnessAt(x,y-1,z-1))/4;
	}
	public float getNaturalBrightnessAverageAt(int x,int y,int z)
	{
		return  (getNaturalBrightnessAt(x,y,z)+getNaturalBrightnessAt(x-1,y,z)+
				getNaturalBrightnessAt(x,y-1,z)+getNaturalBrightnessAt(x,y,z-1)+
				getNaturalBrightnessAt(x-1,y-1,z)+getNaturalBrightnessAt(x,y-1,z-1)+
				getNaturalBrightnessAt(x-1,y,z-1)+getNaturalBrightnessAt(x-1,y-1,z-1))/4;
	}
	public void setArtificialBrightnessAt(int x,int y,int z,byte val)
	{
		if(val<0) return;
		int cx=this.chunkx;
		int cy=this.chunky;
		int cz=this.chunkz;
		int tx=x;
		int ty=y;
		int tz=z;
		boolean changedChunk=false;
		if(x<0) {cx--; changedChunk=true;tx=CHUNK_DIMENSION-1;}
		else if(x>CHUNK_DIMENSION-1) {cx++; changedChunk=true;tx=0;}
		if(y<0) {cy--; changedChunk=true;ty=CHUNK_DIMENSION-1;}
		else if(y>CHUNK_DIMENSION-1) {cy++; changedChunk=true;ty=0;}
		if(z<0) {cz--; changedChunk=true;tz=CHUNK_DIMENSION-1;}
		else if(z>CHUNK_DIMENSION-1){cz++; changedChunk=true;tz=0;}
		
		if(!changedChunk)
		{
			if(val<=getArtificialLightIn(x,y,z)||
					BlockLibrary.isOpaque(this.chunkCubes.get(x,y,z))) return;
			setArtificialLightIn(x,y,z,val);
			this.changed=true;
			setArtificialBrightnessAt(x+1,y,z,(byte)(val-1));
			setArtificialBrightnessAt(x-1,y,z,(byte)(val-1));
			setArtificialBrightnessAt(x,y+1,z,(byte)(val-1));
			setArtificialBrightnessAt(x,y-1,z,(byte)(val-1));
			setArtificialBrightnessAt(x,y,z+1,(byte)(val-1));
			setArtificialBrightnessAt(x,y,z-1,(byte)(val-1));
		}
		else
		{
			Chunk c=this.WF.getChunkByIndex(cx, cy, cz);
			if(c!=null)
			{
				if(c.lightCalculated) c.setArtificialBrightnessAt(tx, ty, tz, val);
			}
		}
	}
	public void setNaturalBrightnessAt(int x,int y,int z,byte val)
	{
		if(val<0) return;
		int cx=this.chunkx;
		int cy=this.chunky;
		int cz=this.chunkz;
		int tx=x;
		int ty=y;
		int tz=z;
		boolean changedChunk=false;
		if(x<0) {cx--; changedChunk=true;tx=CHUNK_DIMENSION-1;}
		else if(x>CHUNK_DIMENSION-1) {cx++; changedChunk=true;tx=0;}
		if(y<0) {cy--; changedChunk=true;ty=CHUNK_DIMENSION-1;}
		else if(y>CHUNK_DIMENSION-1) {cy++; changedChunk=true;ty=0;}
		if(z<0) {cz--; changedChunk=true;tz=CHUNK_DIMENSION-1;}
		else if(z>CHUNK_DIMENSION-1){cz++; changedChunk=true;tz=0;}
		
		if(!changedChunk)
		{
			if(val!=15){
			if(val<=getNaturalLightIn(x,y,z)||
					BlockLibrary.isOpaque(this.chunkCubes.get(x,y,z))) return;
			setNaturalLightIn(x,y,z,val);
			}
			this.changed=true;
			setNaturalBrightnessAt(x+1,y,z,(byte)(val-1));
			setNaturalBrightnessAt(x-1,y,z,(byte)(val-1));
			setNaturalBrightnessAt(x,y+1,z,(byte)(val-1));
			setNaturalBrightnessAt(x,y-1,z,(byte)(val-1));
			setNaturalBrightnessAt(x,y,z+1,(byte)(val-1));
			setNaturalBrightnessAt(x,y,z-1,(byte)(val-1));
		}
		else
		{
			Chunk c=this.WF.getChunkByIndex(cx, cy, cz);
			if(c!=null)
			{
				if(c.lightCalculated) c.setNaturalBrightnessAt(tx, ty, tz, val);
			}
		}
	}
	public void removeArtificialBrightnessAt(int x,int y,int z)
	{
		int cx=this.chunkx;
		int cy=this.chunky;
		int cz=this.chunkz;
		int tx=x;
		int ty=y;
		int tz=z;
		boolean changedChunk=false;
		if(x<0) {cx--; changedChunk=true;tx=CHUNK_DIMENSION-1;}
		else if(x>CHUNK_DIMENSION-1) {cx++; changedChunk=true;tx=0;}
		if(y<0) {cy--; changedChunk=true;ty=CHUNK_DIMENSION-1;}
		else if(y>CHUNK_DIMENSION-1) {cy++; changedChunk=true;ty=0;}
		if(z<0) {cz--; changedChunk=true;tz=CHUNK_DIMENSION-1;}
		else if(z>CHUNK_DIMENSION-1){cz++; changedChunk=true;tz=0;}
		
		if(!changedChunk)
		{
			byte lastval=getArtificialLightIn(x,y,z);
			if(lastval==0) return;
			
			setArtificialLightIn(x,y,z,(byte)0);
			this.changed=true;
			removeArtificialBrightnessAtRec(x+1,y,z,lastval);
			removeArtificialBrightnessAtRec(x-1,y,z,lastval);
			removeArtificialBrightnessAtRec(x,y+1,z,lastval);
			removeArtificialBrightnessAtRec(x,y-1,z,lastval);
			removeArtificialBrightnessAtRec(x,y,z+1,lastval);
			removeArtificialBrightnessAtRec(x,y,z-1,lastval);
		}
		else
		{
			Chunk c=this.WF.getChunkByIndex(cx, cy, cz);
			if(c!=null)
			{
				c.removeArtificialBrightnessAt(tx, ty, tz);
			}
		}
	}
	private void removeArtificialBrightnessAtRec(int x,int y,int z,byte lastval)
	{
		int cx=this.chunkx;
		int cy=this.chunky;
		int cz=this.chunkz;
		int tx=x;
		int ty=y;
		int tz=z;
		boolean changedChunk=false;
		if(x<0) {cx--; changedChunk=true;tx=CHUNK_DIMENSION-1;}
		else if(x>CHUNK_DIMENSION-1) {cx++; changedChunk=true;tx=0;}
		if(y<0) {cy--; changedChunk=true;ty=CHUNK_DIMENSION-1;}
		else if(y>CHUNK_DIMENSION-1) {cy++; changedChunk=true;ty=0;}
		if(z<0) {cz--; changedChunk=true;tz=CHUNK_DIMENSION-1;}
		else if(z>CHUNK_DIMENSION-1){cz++; changedChunk=true;tz=0;}
		
		if(!changedChunk)
		{
			if(getArtificialLightIn(x,y,z)==lastval-1)
			{
				int maxadjbright=getMaxArtificialAdjacentLight(x,y,z);
				this.changed=true;
				if(maxadjbright==0){
					setArtificialLightIn(x,y,z,(byte)0);
				}
				else if(maxadjbright<lastval){
					setArtificialLightIn(x,y,z,(byte)(maxadjbright-1));
					removeArtificialBrightnessAtRec(x+1,y,z,(byte)(lastval-1));
					removeArtificialBrightnessAtRec(x-1,y,z,(byte)(lastval-1));
					removeArtificialBrightnessAtRec(x,y+1,z,(byte)(lastval-1));
					removeArtificialBrightnessAtRec(x,y-1,z,(byte)(lastval-1));
					removeArtificialBrightnessAtRec(x,y,z+1,(byte)(lastval-1));
					removeArtificialBrightnessAtRec(x,y,z-1,(byte)(lastval-1));
				}
				
			}
		}
		else
		{
			Chunk c=this.WF.getChunkByIndex(cx, cy, cz);
			if(c!=null)
			{
				c.removeArtificialBrightnessAtRec(tx, ty, tz, lastval);
			}
		}
	}
	public void removeNaturalBrightnessAt(int x,int y,int z)
	{
		int cx=this.chunkx;
		int cy=this.chunky;
		int cz=this.chunkz;
		int tx=x;
		int ty=y;
		int tz=z;
		boolean changedChunk=false;
		if(x<0) {cx--; changedChunk=true;tx=CHUNK_DIMENSION-1;}
		else if(x>CHUNK_DIMENSION-1) {cx++; changedChunk=true;tx=0;}
		if(y<0) {cy--; changedChunk=true;ty=CHUNK_DIMENSION-1;}
		else if(y>CHUNK_DIMENSION-1) {cy++; changedChunk=true;ty=0;}
		if(z<0) {cz--; changedChunk=true;tz=CHUNK_DIMENSION-1;}
		else if(z>CHUNK_DIMENSION-1){cz++; changedChunk=true;tz=0;}
		
		if(!changedChunk)
		{
			byte lastval=getNaturalLightIn(x,y,z);
			if(lastval==0) return;
			
			setNaturalLightIn(x,y,z,(byte)0);
			this.changed=true;
			removeNaturalBrightnessAtRec(x+1,y,z,lastval);
			removeNaturalBrightnessAtRec(x-1,y,z,lastval);
			removeNaturalBrightnessAtRec(x,y+1,z,lastval);
			removeNaturalBrightnessAtRec(x,y-1,z,lastval);
			removeNaturalBrightnessAtRec(x,y,z+1,lastval);
			removeNaturalBrightnessAtRec(x,y,z-1,lastval);
		}
		else
		{
			Chunk c=this.WF.getChunkByIndex(cx, cy, cz);
			if(c!=null)
			{
				c.removeNaturalBrightnessAt(tx, ty, tz);
			}
		}
	}
	private void removeNaturalBrightnessAtRec(int x,int y,int z,byte lastval)
	{
		int cx=this.chunkx;
		int cy=this.chunky;
		int cz=this.chunkz;
		int tx=x;
		int ty=y;
		int tz=z;
		boolean changedChunk=false;
		if(x<0) {cx--; changedChunk=true;tx=CHUNK_DIMENSION-1;}
		else if(x>CHUNK_DIMENSION-1) {cx++; changedChunk=true;tx=0;}
		if(y<0) {cy--; changedChunk=true;ty=CHUNK_DIMENSION-1;}
		else if(y>CHUNK_DIMENSION-1) {cy++; changedChunk=true;ty=0;}
		if(z<0) {cz--; changedChunk=true;tz=CHUNK_DIMENSION-1;}
		else if(z>CHUNK_DIMENSION-1){cz++; changedChunk=true;tz=0;}
		
		if(!changedChunk)
		{
			if(getNaturalLightIn(x,y,z)==lastval-1)
			{
				int maxadjbright=getMaxNaturalAdjacentLight(x,y,z);
				if(maxadjbright==0){
					setNaturalLightIn(x,y,z,(byte)0);
				}
				else if(maxadjbright<lastval){
					setNaturalLightIn(x,y,z,(byte)(maxadjbright-1));
					this.changed=true;
					removeNaturalBrightnessAtRec(x+1,y,z,(byte)(lastval-1));
					removeNaturalBrightnessAtRec(x-1,y,z,(byte)(lastval-1));
					removeNaturalBrightnessAtRec(x,y+1,z,(byte)(lastval-1));
					removeNaturalBrightnessAtRec(x,y-1,z,(byte)(lastval-1));
					removeNaturalBrightnessAtRec(x,y,z+1,(byte)(lastval-1));
					removeNaturalBrightnessAtRec(x,y,z-1,(byte)(lastval-1));
				}
				
			}
		}
		else
		{
			Chunk c=this.WF.getChunkByIndex(cx, cy, cz);
			if(c!=null)
			{
				c.removeNaturalBrightnessAtRec(tx, ty, tz, lastval);
			}
		}
	}
	private void recalculateBrightnessOfCube(int x,int y,int z)
	{
		byte max=getMaxArtificialAdjacentLight(x,y,z);
		if(BlockLibrary.isLightSource(this.chunkCubes.get(x,y,z))){
			byte light=BlockLibrary.getLightProduced(this.chunkCubes.get(x,y,z));
			if(light>max) max=light;
		}
		if(max>1)
		{
			setArtificialBrightnessAt(x,y,z,(byte)(max-1));
		}
		
		//Natural
		if((this.chunky==World.HEIGHT-1&&y==CHUNK_DIMENSION-1)||getNaturalLightLevelAt(x,y+1,z)==15)
		{
			ArrayList<Chunk> downChunks=new ArrayList<Chunk>();
			for(int yc=this.chunky-1;yc>=0;yc--){
				Chunk chu=this.WF.getChunkByIndex(this.chunkx, yc, this.chunkz);
				downChunks.add(chu);
			}
			this.propagateNaturalLightRay(x, z, y,downChunks, 0);
			
			if(extendNaturalLightFrom(x,z,y,this))
			{
				for(Chunk dc:downChunks)
				{
					if(dc==null||!extendNaturalLightFrom(x,z,CHUNK_DIMENSION-1,dc)) break;
				}
			}
		}
		else
		{
			max=getMaxNaturalAdjacentLight(x,y,z);
			if(max>1) this.setNaturalBrightnessAt(x, y, z, (byte)(max-1));
		}
		
	}
	private boolean extendNaturalLightFrom(int x,int z,int iy,Chunk c)
	{
		for(int y=iy;y>=0;y--)
		{
			byte lp=c.getNaturalLightIn(x,y,z);
			if(lp==15)
			{
				c.setNaturalBrightnessAt(x,y,z,(byte)15);
			}
			else {
				return false;
			}
		}
		return true;
	}
	private byte getMaxArtificialAdjacentLight(int x,int y,int z)
	{
		byte max=getArtificialLightLevelAt(x+1,y,z);
		byte aux=getArtificialLightLevelAt(x-1,y,z);
		max=aux>max? aux : max;
		aux=getArtificialLightLevelAt(x,y+1,z);
		max=aux>max? aux : max;
		aux=getArtificialLightLevelAt(x,y-1,z);
		max=aux>max? aux : max;
		aux=getArtificialLightLevelAt(x,y,z+1);
		max=aux>max? aux : max;
		aux=getArtificialLightLevelAt(x,y,z-1);
		max=aux>max? aux : max;
		return (byte)max;
	}
	private byte getMaxNaturalAdjacentLight(int x,int y,int z)
	{
		byte max=getNaturalLightLevelAt(x+1,y,z);
		byte aux=getNaturalLightLevelAt(x-1,y,z);
		max=aux>max? aux : max;
		aux=getNaturalLightLevelAt(x,y+1,z);
		max=aux>max? aux : max;
		aux=getNaturalLightLevelAt(x,y-1,z);
		max=aux>max? aux : max;
		aux=getNaturalLightLevelAt(x,y,z+1);
		max=aux>max? aux : max;
		aux=getNaturalLightLevelAt(x,y,z-1);
		max=aux>max? aux : max;
		return (byte)max;
	}
	public byte getArtificialLightIn(int x,int y,int z)
	{
		return (byte)(this.chunkCubesLight.get(x,y,z) & 0xF);
	}
	public void setArtificialLightIn(int x,int y,int z,byte val)
	{
		//System.out.println(val);
		if(val!=getArtificialLightIn(x,y,z)) this.changed=true;
		this.chunkCubesLight.set(x,y,z,(byte)((this.chunkCubesLight.get(x,y,z) & 0xF0 ) | (val & 0xF)));
	}
	public byte getNaturalLightIn(int x,int y,int z)
	{
		//System.out.println("GET!!! "+(byte)((this.chunkCubesLight.get(x,y,z) >> 4) & 0xF)+" "+Integer.toBinaryString((chunkCubesLight[x][y][z] >> 4) &0xF));
		return (byte)((this.chunkCubesLight.get(x,y,z) >> 4) &0xF);
	}
	public void setNaturalLightIn(int x,int y,int z,byte val)
	{
		//System.out.println(val);
		if(val!=getNaturalLightIn(x,y,z)) this.changed=true;
		//System.out.println("SET "+val+" ORI "+this.chunkCubesLight.get(x,y,z)+" SETTED "+(byte)((this.chunkCubesLight.get(x,y,z) &0xF) | (val << 4))+" ORD "+((this.chunkCubesLight.get(x,y,z) &0xF) | (val << 4)));
		this.chunkCubesLight.set(x,y,z,(byte)((this.chunkCubesLight.get(x,y,z) &0xF) | ((val << 4)&0xF0)));
		//System.out.println("SET "+Integer.toBinaryString(chunkCubesLight[x][y][z] &0xFF));
	}
	
	/**
	 * Notifies to this chunk that neighbour at direction x,y,z has been added. Performs bitwise calculations to include this information on neighborsAdded int
	 */
	public void notifyNeighbourAdded(int x,int y,int z)
	{
		int index=x*9 + z*3 + y + 13; //(x+1)*9 + (z+1)*3 + (y+1)
		int nadded=this.neighborsAdded | (1<<index);
		if(nadded!=this.neighborsAdded)
		{
			this.changed=true;
			this.neighborsAdded=nadded;
		}
	}
	
	/**
	 * Notifies to this chunk that neighbour at direction x,y,z has been removed. Performs bitwise calculations to include this information on neighborsAdded int
	 */
	public void notifyNeighbourRemoved(int x,int y,int z)
	{
		int index=x*9 + z*3 + y + 13; //(x+1)*9 + (z+1)*3 + (y+1)
		this.neighborsAdded=this.neighborsAdded & (~(1<<index));
	}
	
	@Override
	public void fullClean()
	{
		if(!this.deleted){
			this.deleted=true;
			this.WF.notifyNeighboursRemove(this);
			if(this.toUpload!=null){ FloatBufferPool.recycleBuffer(this.toUpload); this.toUpload=null;}
			if(this.toUploadLiquid!=null) {FloatBufferPool.recycleBuffer(this.toUploadLiquid); this.toUploadLiquid=null;}
			
			this.chunkCubes.dispose();
			if(this.lightCalculated) this.chunkCubesLight.dispose();
			
			this.updateCubes.clear();
			if(this.vbo!=-1) glDeleteBuffers(this.vbo);
		}
	}
	public int getX()
	{
		return this.chunkx;
	}
	public int getY()
	{
		return this.chunky;
	}
	public int getZ()
	{
		return this.chunkz;
	}
	public void setUpdateFlag(boolean value)
	{
		this.updateFlag=value;
	}
	public Semaphore getUpdateAccessSemaphore()
	{
		return this.updateAccessSemaphore;
	}
	public boolean isDeleted()
	{
		return this.deleted;
	}
}

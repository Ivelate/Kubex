package kubex.gui;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glBufferSubData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import ivengine.properties.Cleanable;
import ivengine.view.Camera;
import ivengine.view.MatrixHelper;
import kubex.blocks.BlockLibrary;
import kubex.gui.MapGenerator.ChunkGenerationResult;
import kubex.gui.MapHandler.ChunkData;
import kubex.shaders.VoxelShaderProgram;
import kubex.storage.ArrayCubeStorage;
import kubex.storage.ByteArrayPool;
import kubex.storage.ConstantValueCubeStorage;
import kubex.storage.CubeStorage;
import kubex.storage.FloatBufferPool;
import kubex.utils.BoundaryChecker;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.lwjgl.opengl.GL15;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Chunk class. Stores all data contained into a chunk of cubes (32x32x32 cubes), including the cubes per-se, light (Artificial and natural), drawing utilities (FloatBuffers, vbo's), etc.
 * Contans several methods used for drawing, updating and managing chunks.
 */
public class Chunk implements Cleanable
{
	public enum Direction{YP,YM,XP,XM,ZP,ZM};
	public enum CubeStorageType{CUBES_STORAGE,LIGHT_STORAGE};
	
	public static final int CHUNK_DIMENSION=32; //Chunks are 32x32x32, but it isn't hardcoded: If the value is changed here, it should work too.
	public static final float MAX_LIGHT_LEVEL=15;
	private static final double CHUNK_RADIUS=CHUNK_DIMENSION*Math.sqrt(3);
	private static final Vector4f CENTER_VECTOR=new Vector4f(CHUNK_DIMENSION/2,CHUNK_DIMENSION/2,CHUNK_DIMENSION/2,1.0f);
	//Numero de argumentos del shader
	private static final int SAN=6;
	private static final int NORMALIZE_WATER_EACH=4;
	
	private static final byte NATURAL_LIGHT_FULL_VAL=(byte)((15 << 4)&0xF0); //Natural light is stored on the first 4 bits of the light byte
	
	private int vbo=-1;
	private WorldFacade WF;
	private int triangleNum=0; //Number of triangles in the chunk
	private int triangleLiquidNum=0; //Number of triangles forming water in the chunk
	private CubeStorage chunkCubes=null; //Cubes in the chunk, abstracted. See CubeStorage class for more info
	private CubeStorage chunkCubesLight=new ConstantValueCubeStorage((byte)0,this,CubeStorageType.LIGHT_STORAGE){
		@Override public void set(int x, int y, int z, byte val) {}
	};
	private List<CubePosition> updateCubes=new LinkedList<CubePosition>(); //Particular cubes inside this chunk that needs to be updated each tick (Flowing water, TNT, etc)
		private class CubePosition{public int x;public int y;public int z; public CubePosition(int x,int y,int z){this.x=x;this.y=y;this.z=z;}}
		
	private int neighborsAdded=0; //Stores, in an int, which chunk neighbors had been added to the scene. Drawing will be only possible when all neighbors are added.
								  //For each bit, the value 1 will be equal to neighbor added and the value 0 will be equal to neighbor not added.
								  //There is a total of 26 neighbors (Counting diagonals), and each one of them will have a index in this int, represented by the opperation
								  //(x+1)*9 + (z+1)*3 + (y+1) , being x={-1,0,1}, y={-1,0,1}, z={-1,0,1}, representing the direction of the neighbor.
								  //Considering that, this chunk will not be drawed until this value isnt equal to 00000111111111111101111111111111
								  //Chunk neighbors are neccesary because second pass generation of them can change values in this chunk, and the illumination on the edges
								  //can only be calculated if we know the illumination of the edges of the neighbors.
	
	private FloatBuffer toUpload=null; //Buffer containing all vertex info to be uploaded to the graphics card
	private FloatBuffer toUploadLiquid=null;//Buffer containing all water vertex info to be uploaded to the graphics card
	
	private boolean lightCalculated=false; //Has the light map been created?
	private boolean drawed=false; //Has the chunk been drawed this frame?
	private boolean changed=true; //Has the chunk data changed since the last redraw? (If so, the data in the graphics card is not congruent with reality, and we need to redraw the chunk)
	private boolean updateFlag=false; //Marks if the chunk is already set for update. If so, changes in it will now set changed to true
	private boolean initcializedFlag=false; //Marks if the second step of the generation (Adding trees) has already been performed in this chunk
	private boolean solidEmpty=true; //Marks if this chunk has no non-liquid triangles to draw
	private boolean liquidEmpty=true; //Marks if this chunk has no iquid triangles to draw
	private boolean deleted=false; //Marks if this chunk has been marked to deletion
	private Semaphore updateAccessSemaphore=new Semaphore(1); //Prevent this chunk to be updated twice at the same time.
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
		
		byte[][][] cubes=ByteArrayPool.getArrayUncleaned();
		ChunkData chunkGenerationData=WF.getMapHandler().getChunk(chunkxpos, chunkypos,chunkzpos,cubes);
		
		if(chunkGenerationData.chunkGenerationResult!=ChunkGenerationResult.CHUNK_NORMAL) {
			this.chunkCubes=new ConstantValueCubeStorage((byte)0,this,CubeStorageType.CUBES_STORAGE);
			if(chunkGenerationData.chunkGenerationResult==ChunkGenerationResult.CHUNK_EMPTY) ByteArrayPool.recycleCleanArray(cubes);
			else ByteArrayPool.recycleArray(cubes);
		}
		else this.chunkCubes=new ArrayCubeStorage(cubes,false);
		
		this.initcializedFlag=chunkGenerationData.initcializedFlag;
	}
	
	/**
	 * Inits the chunk, creating its vbo and filling it with data
	 */
	public void initChunk()
	{
		this.vbo=glGenBuffers();
		
		update();
	}
	
	/**
	 * Returns the maximum between two values
	 */
	private float max(float v1,float v2)
	{
		if(v1>v2) return v1;
		return v2;
	}
	
	/**
	 * Stores all vertex data to be uploaded to the graphic card into the FloatBuffers toUpload and toUploadLiquid
	 * Only does so if the chunk is already surrounded by neighbours (Because, in its limits, it needs to know the adjacent
	 * cube data to perform light calculation or culling, and this cube would be in a neighbour chunk)
	 * 
	 * Buffer format (For each vertex):
	 * 				1st float: x pos
	 * 				2nd float: y pos
	 * 				3rd float: z pos
	 *				4th float: No water -> Texture to be used + normal of the cube (Compressed) | water -> X normal
	 *				5th float: No water -> Natural Brightness of the face 						| water -> Y normal
	 *				6th float: No water -> Artificial Brightness of the face 					| water -> Z normal
	 */
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
		
		//ONLY DRAWS IF ALL NEIGHBORS IN ALL DIRECTIONS (DIAGONALS INCLUDED) HAS BEEN ADDED
		if(this.neighborsAdded==134209535) //00000111111111111101111111111111
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
		if(toUpload==null||toUploadLiquid==null) return; //If the FloatBuffer returned by the pool is null, we are closing the program, so we return
		
		//If the chunk is full of air, it doesn't draw anything
		if(!(!this.chunkCubes.isTrueStorage()&&this.chunkCubes.get(0, 0, 0)==0)){
		
		//Get map generator
		MapHandler mg=WF.getMapHandler();
		boolean overrideDrawTop=false;
		boolean overrideDrawBot=false;
		
		//Water normal in a point will be equal to the average of all normals of the cubes surrounding this point.
		Vector3f waternormalxmzm=new Vector3f(0.5f,1,0.5f);
		Vector3f waternormalxpzp=new Vector3f(0.5f,1,0.5f);
		Vector3f waternormalxpzm=new Vector3f(0.5f,1,0.5f);
		Vector3f waternormalxmzp=new Vector3f(0.5f,1,0.5f);
		
		for(byte z=0;z<CHUNK_DIMENSION;z++)
		{
			for(byte x=0;x<CHUNK_DIMENSION;x++)
			{
				overrideDrawTop=false; //Only used for water. If the water isn't full height, we will draw the top water cube, if it is full height, the cube will be merged with the top one and
				overrideDrawBot=false; //we will not draw that face
				
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
						int c=0;
						
						float lp=BlockLibrary.getLightProduced(cubeC)/MAX_LIGHT_LEVEL;
						
						if(BlockLibrary.isCrossSectional(cubeC)) //For grass
						{
							float crossLength=0.707106f;
							float rem=(1-crossLength)/2;
							float irem=1-rem;
							//heightxmzm=0.75f; heightxmzp=0.75f; heightxpzm=0.75f; heightxpzp=0.75f;
							cube[0+(SAN*0)]=x+rem;		cube[1+(SAN*0)]=y;		cube[2+(SAN*0)]=z+rem;		
							cube[3+(SAN*0)]=6000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*0)]=this.getNaturalBrightnessFaceYAverageAt(x, y, z); cube[5+(SAN*0)]=max(lp,this.getArtificialBrightnessFaceYAverageAt(x, y, z));
							cube[0+(SAN*1)]=x+irem;				cube[1+(SAN*1)]=y;				cube[2+(SAN*1)]=z+irem;	
							cube[3+(SAN*1)]=6000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*1)]=this.getNaturalBrightnessFaceYAverageAt(x+1, y, z+1); cube[5+(SAN*1)]=max(lp,this.getArtificialBrightnessFaceYAverageAt(x+1, y, z+1));					
							cube[0+(SAN*2)]=x+rem;				cube[1+(SAN*2)]=y+heightxmzm;	cube[2+(SAN*2)]=z+rem;
							cube[3+(SAN*2)]=6000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*2)]=this.getNaturalBrightnessFaceYAverageAt(x, y+1, z); cube[5+(SAN*2)]=max(lp,this.getArtificialBrightnessFaceYAverageAt(x, y+1, z));				
							cube[0+(SAN*3)]=x+rem;				cube[1+(SAN*3)]=y+heightxmzm;	cube[2+(SAN*3)]=z+rem;
							cube[3+(SAN*3)]=6000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*3)]=this.getNaturalBrightnessFaceYAverageAt(x, y+1, z); cube[5+(SAN*3)]=max(lp,this.getArtificialBrightnessFaceYAverageAt(x, y+1, z));					
							cube[0+(SAN*4)]=x+irem;				cube[1+(SAN*4)]=y;				cube[2+(SAN*4)]=z+irem;
							cube[3+(SAN*4)]=6000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*4)]=this.getNaturalBrightnessFaceYAverageAt(x+1, y, z+1); cube[5+(SAN*4)]=max(lp,this.getArtificialBrightnessFaceYAverageAt(x+1, y, z+1));					
							cube[0+(SAN*5)]=x+irem;				cube[1+(SAN*5)]=y+heightxmzp;	cube[2+(SAN*5)]=z+irem;
							cube[3+(SAN*5)]=6000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*5)]=this.getNaturalBrightnessFaceYAverageAt(x+1, y+1, z+1); cube[5+(SAN*5)]=max(lp,this.getArtificialBrightnessFaceYAverageAt(x+1, y+1, z+1));
							writeTarget.put(cube);c+=SAN*6;
							cube[0+(SAN*0)]=x+rem;		cube[1+(SAN*0)]=y;		cube[2+(SAN*0)]=z+rem;		
							cube[3+(SAN*0)]=7000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*0)]=this.getNaturalBrightnessFaceYAverageAt(x, y, z); cube[5+(SAN*0)]=max(lp,this.getArtificialBrightnessFaceYAverageAt(x, y, z));
							cube[0+(SAN*1)]=x+rem;				cube[1+(SAN*1)]=y+heightxmzm;			cube[2+(SAN*1)]=z+rem;	
							cube[3+(SAN*1)]=7000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*1)]=this.getNaturalBrightnessFaceYAverageAt(x, y+1, z); cube[5+(SAN*1)]=max(lp,this.getArtificialBrightnessFaceYAverageAt(x, y+1, z));					
							cube[0+(SAN*2)]=x+irem;				cube[1+(SAN*2)]=y;	cube[2+(SAN*2)]=z+irem;
							cube[3+(SAN*2)]=7000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*2)]=this.getNaturalBrightnessFaceYAverageAt(x+1, y, z+1); cube[5+(SAN*2)]=max(lp,this.getArtificialBrightnessFaceYAverageAt(x+1, y, z+1));				
							cube[0+(SAN*3)]=x+rem;				cube[1+(SAN*3)]=y+heightxmzm;	cube[2+(SAN*3)]=z+rem;
							cube[3+(SAN*3)]=7000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*3)]=this.getNaturalBrightnessFaceYAverageAt(x, y+1, z); cube[5+(SAN*3)]=max(lp,this.getArtificialBrightnessFaceYAverageAt(x, y+1, z));					
							cube[0+(SAN*4)]=x+irem;				cube[1+(SAN*4)]=y+heightxmzp;				cube[2+(SAN*4)]=z+irem;
							cube[3+(SAN*4)]=7000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*4)]=this.getNaturalBrightnessFaceYAverageAt(x+1, y+1, z+1); cube[5+(SAN*4)]=max(lp,this.getArtificialBrightnessFaceYAverageAt(x+1, y+1, z+1));					
							cube[0+(SAN*5)]=x+irem;				cube[1+(SAN*5)]=y;	cube[2+(SAN*5)]=z+irem;
							cube[3+(SAN*5)]=7000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*5)]=this.getNaturalBrightnessFaceYAverageAt(x+1, y, z+1); cube[5+(SAN*5)]=max(lp,this.getArtificialBrightnessFaceYAverageAt(x+1, y, z+1));
							writeTarget.put(cube);c+=SAN*6;
							
							cube[0+(SAN*0)]=x+irem;		cube[1+(SAN*0)]=y;		cube[2+(SAN*0)]=z+rem;		
							cube[3+(SAN*0)]=8000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*0)]=this.getNaturalBrightnessFaceYAverageAt(x+1, y, z); cube[5+(SAN*0)]=max(lp,this.getArtificialBrightnessFaceYAverageAt(x+1, y, z));
							cube[0+(SAN*1)]=x+rem;				cube[1+(SAN*1)]=y;				cube[2+(SAN*1)]=z+irem;	
							cube[3+(SAN*1)]=8000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*1)]=this.getNaturalBrightnessFaceYAverageAt(x, y, z+1); cube[5+(SAN*1)]=max(lp,this.getArtificialBrightnessFaceYAverageAt(x, y, z+1));					
							cube[0+(SAN*2)]=x+irem;				cube[1+(SAN*2)]=y+heightxmzm;	cube[2+(SAN*2)]=z+rem;
							cube[3+(SAN*2)]=8000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*2)]=this.getNaturalBrightnessFaceYAverageAt(x+1, y+1, z); cube[5+(SAN*2)]=max(lp,this.getArtificialBrightnessFaceYAverageAt(x+1, y+1, z));				
							cube[0+(SAN*3)]=x+irem;				cube[1+(SAN*3)]=y+heightxmzm;	cube[2+(SAN*3)]=z+rem;
							cube[3+(SAN*3)]=8000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*3)]=this.getNaturalBrightnessFaceYAverageAt(x+1, y+1, z); cube[5+(SAN*3)]=max(lp,this.getArtificialBrightnessFaceYAverageAt(x+1, y+1, z));					
							cube[0+(SAN*4)]=x+rem;				cube[1+(SAN*4)]=y;				cube[2+(SAN*4)]=z+irem;
							cube[3+(SAN*4)]=8000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*4)]=this.getNaturalBrightnessFaceYAverageAt(x, y, z+1); cube[5+(SAN*4)]=max(lp,this.getArtificialBrightnessFaceYAverageAt(x, y, z+1));					
							cube[0+(SAN*5)]=x+rem;				cube[1+(SAN*5)]=y+heightxmzp;	cube[2+(SAN*5)]=z+irem;
							cube[3+(SAN*5)]=8000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*5)]=this.getNaturalBrightnessFaceYAverageAt(x, y+1, z+1); cube[5+(SAN*5)]=max(lp,this.getArtificialBrightnessFaceYAverageAt(x, y+1, z+1));				
							writeTarget.put(cube);c+=SAN*6;
							cube[0+(SAN*0)]=x+irem;		cube[1+(SAN*0)]=y;		cube[2+(SAN*0)]=z+rem;		
							cube[3+(SAN*0)]=9000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*0)]=this.getNaturalBrightnessFaceYAverageAt(x+1, y, z); cube[5+(SAN*0)]=max(lp,this.getArtificialBrightnessFaceYAverageAt(x+1, y, z));
							cube[0+(SAN*1)]=x+irem;				cube[1+(SAN*1)]=y+heightxmzm;				cube[2+(SAN*1)]=z+rem;	
							cube[3+(SAN*1)]=9000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*1)]=this.getNaturalBrightnessFaceYAverageAt(x+1, y+1, z); cube[5+(SAN*1)]=max(lp,this.getArtificialBrightnessFaceYAverageAt(x+1, y+1, z));					
							cube[0+(SAN*2)]=x+rem;				cube[1+(SAN*2)]=y;	cube[2+(SAN*2)]=z+irem;
							cube[3+(SAN*2)]=9000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*2)]=this.getNaturalBrightnessFaceYAverageAt(x, y, z+1); cube[5+(SAN*2)]=max(lp,this.getArtificialBrightnessFaceYAverageAt(x, y, z+1));				
							cube[0+(SAN*3)]=x+irem;				cube[1+(SAN*3)]=y+heightxmzm;	cube[2+(SAN*3)]=z+rem;
							cube[3+(SAN*3)]=9000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*3)]=this.getNaturalBrightnessFaceYAverageAt(x+1, y+1, z); cube[5+(SAN*3)]=max(lp,this.getArtificialBrightnessFaceYAverageAt(x+1, y+1, z));					
							cube[0+(SAN*4)]=x+rem;				cube[1+(SAN*4)]=y+heightxmzp;			cube[2+(SAN*4)]=z+irem;
							cube[3+(SAN*4)]=9000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*4)]=this.getNaturalBrightnessFaceYAverageAt(x, y+1, z+1); cube[5+(SAN*4)]=max(lp,this.getArtificialBrightnessFaceYAverageAt(x, y+1, z+1));					
							cube[0+(SAN*5)]=x+rem;				cube[1+(SAN*5)]=y;	cube[2+(SAN*5)]=z+irem;
							cube[3+(SAN*5)]=9000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*5)]=this.getNaturalBrightnessFaceYAverageAt(x, y, z+1); cube[5+(SAN*5)]=max(lp,this.getArtificialBrightnessFaceYAverageAt(x, y, z+1));				
							writeTarget.put(cube);c+=SAN*6;
							
							
						}
						else if(liquidTag)
						{
							heightxmzm=getCubeHeight(x,y,z,cubeC);
							heightxpzm=getCubeHeight(x+1,y,z,cubeC);
							heightxmzp=getCubeHeight(x,y,z+1,cubeC);
							heightxpzp=getCubeHeight(x+1,y,z+1,cubeC);
							waternormalxmzm=new Vector3f(0.5f,1,0.5f);
							waternormalxpzp=new Vector3f(0.5f,1,0.5f);
							waternormalxpzm=new Vector3f(0.5f,1,0.5f);
							waternormalxmzp=new Vector3f(0.5f,1,0.5f);
							if(!(heightxpzp>0.99f&&heightxmzm>0.99f&&heightxpzm>0.99f&&heightxmzp>0.99f)) { //Get the normal of each point of the cube
								overrideDrawTop=true;
								Vector3f centralVec=new Vector3f(1,heightxpzm-heightxmzp,-1);
								Vector3f.cross(new Vector3f(1,heightxpzm-heightxmzm,0), centralVec,waternormalxmzm);
								Vector3f.cross(centralVec,new Vector3f(0,heightxpzm-heightxpzp,-1),waternormalxpzp);
								waternormalxmzm.x=(waternormalxmzm.x/2)+0.5f; waternormalxmzm.y=(waternormalxmzm.y/2)+0.5f; waternormalxmzm.z=(waternormalxmzm.z/2)+0.5f;
								waternormalxpzp.x=(waternormalxpzp.x/2)+0.5f; waternormalxpzp.y=(waternormalxpzp.y/2)+0.5f; waternormalxpzp.z=(waternormalxpzp.z/2)+0.5f;
								//System.out.println(heightxmzm+" "+heightxpzm+" "+heightxmzp+" "+heightxpzp+" "+waternormalxmzm+" "+waternormalxpzp);
								waternormalxpzm.x=0; waternormalxpzm.y=0; waternormalxpzm.z=0; 
								Vector3f.add(waternormalxmzm, waternormalxpzp, waternormalxpzm);
								waternormalxpzm.x*=0.5f; waternormalxpzm.y*=0.5f; waternormalxpzm.z*=0.5f;
								waternormalxmzp=waternormalxpzm;
							}
							
							
							// X axis faces
							if((x==0&&mg.shouldDraw(neighbours[Direction.XM.ordinal()].getCubeAt(CHUNK_DIMENSION-1, y, z),this.chunkCubes.get(x,y,z),liquidTag,Direction.XM))
									||(x!=0&&mg.shouldDraw(this.chunkCubes.get(x-1,y,z),this.chunkCubes.get(x,y,z),liquidTag,Direction.XM))){
							cube[0+(SAN*0)]=x;		cube[1+(SAN*0)]=y;		cube[2+(SAN*0)]=z;		
							cube[3+(SAN*0)]=0;cube[4+(SAN*0)]=0.5f;cube[5+(SAN*0)]=0.5f;
							cube[0+(SAN*1)]=x;				cube[1+(SAN*1)]=y;				cube[2+(SAN*1)]=(byte)(z+1);							
							cube[3+(SAN*1)]=0;cube[4+(SAN*1)]=0.5f;cube[5+(SAN*1)]=0.5f;
							cube[0+(SAN*2)]=x;				cube[1+(SAN*2)]=y+heightxmzm;	cube[2+(SAN*2)]=z;						
							cube[3+(SAN*2)]=0;cube[4+(SAN*2)]=0.5f;cube[5+(SAN*2)]=0.5f;
							cube[0+(SAN*3)]=x;				cube[1+(SAN*3)]=y+heightxmzm;	cube[2+(SAN*3)]=z;						
							cube[3+(SAN*3)]=0;cube[4+(SAN*3)]=0.5f;cube[5+(SAN*3)]=0.5f;
							cube[0+(SAN*4)]=x;				cube[1+(SAN*4)]=y;				cube[2+(SAN*4)]=(byte)(z+1);						
							cube[3+(SAN*4)]=0;cube[4+(SAN*4)]=0.5f;cube[5+(SAN*4)]=0.5f;
							cube[0+(SAN*5)]=x;				cube[1+(SAN*5)]=y+heightxmzp;	cube[2+(SAN*5)]=(byte)(z+1);						
							cube[3+(SAN*5)]=0;cube[4+(SAN*5)]=0.5f;cube[5+(SAN*5)]=0.5f;
							writeTarget.put(cube);c+=SAN*6;}
							if((x==CHUNK_DIMENSION-1&&mg.shouldDraw(neighbours[Direction.XP.ordinal()].getCubeAt(0, y, z),this.chunkCubes.get(x,y,z),liquidTag,Direction.XP))
								||(x!=CHUNK_DIMENSION-1&&mg.shouldDraw(this.chunkCubes.get(x+1,y,z),this.chunkCubes.get(x,y,z),liquidTag,Direction.XP))){
							cube[0+(SAN*0)]=(byte)(x+1);	cube[1+(SAN*0)]=y;				cube[2+(SAN*0)]=z;						
							cube[3+(SAN*0)]=1;cube[4+(SAN*0)]=0.5f;cube[5+(SAN*0)]=0.5f;
							cube[0+(SAN*1)]=(byte)(x+1);	cube[1+(SAN*1)]=y+heightxpzm;	cube[2+(SAN*1)]=z;						
							cube[3+(SAN*1)]=1;cube[4+(SAN*1)]=0.5f;cube[5+(SAN*1)]=0.5f;
							cube[0+(SAN*2)]=(byte)(x+1);	cube[1+(SAN*2)]=y;				cube[2+(SAN*2)]=(byte)(z+1);						
							cube[3+(SAN*2)]=1;cube[4+(SAN*2)]=0.5f;cube[5+(SAN*2)]=0.5f;
							cube[0+(SAN*3)]=(byte)(x+1);	cube[1+(SAN*3)]=y;				cube[2+(SAN*3)]=(byte)(z+1);						
							cube[3+(SAN*3)]=1;cube[4+(SAN*3)]=0.5f;cube[5+(SAN*3)]=0.5f;
							cube[0+(SAN*4)]=(byte)(x+1);	cube[1+(SAN*4)]=y+heightxpzm;	cube[2+(SAN*4)]=z;							
							cube[3+(SAN*4)]=1;cube[4+(SAN*4)]=0.5f;cube[5+(SAN*4)]=0.5f;
							cube[0+(SAN*5)]=(byte)(x+1);	cube[1+(SAN*5)]=y+heightxpzp;	cube[2+(SAN*5)]=(byte)(z+1);						
							cube[3+(SAN*5)]=1;cube[4+(SAN*5)]=0.5f;cube[5+(SAN*5)]=0.5f;
							writeTarget.put(cube);c+=SAN*6;}
							
							// Y axis faces
							if((y==0&&this.getY()!=0&&(mg.shouldDraw(neighbours[Direction.YM.ordinal()].getCubeAt(x, CHUNK_DIMENSION-1, z),this.chunkCubes.get(x,y,z),liquidTag,Direction.YM)||overrideDrawBot))
									||(y!=0&&(mg.shouldDraw(this.chunkCubes.get(x,y-1,z),this.chunkCubes.get(x,y,z),liquidTag,Direction.YM)||overrideDrawBot))){
							cube[0+(SAN*0)]=x;				cube[1+(SAN*0)]=y;				cube[2+(SAN*0)]=z;						
							cube[3+(SAN*0)]=0.5f;cube[4+(SAN*0)]=0;cube[5+(SAN*0)]=0.5f;
							cube[0+(SAN*1)]=(byte)(x+1);	cube[1+(SAN*1)]=y;				cube[2+(SAN*1)]=z;						
							cube[3+(SAN*1)]=0.5f;cube[4+(SAN*1)]=0;cube[5+(SAN*1)]=0.5f;
							cube[0+(SAN*2)]=x;				cube[1+(SAN*2)]=y;				cube[2+(SAN*2)]=(byte)(z+1);						
							cube[3+(SAN*2)]=0.5f;cube[4+(SAN*2)]=0;cube[5+(SAN*2)]=0.5f;
							cube[0+(SAN*3)]=x;				cube[1+(SAN*3)]=y;				cube[2+(SAN*3)]=(byte)(z+1);						
							cube[3+(SAN*3)]=0.5f;cube[4+(SAN*3)]=0;cube[5+(SAN*3)]=0.5f;
							cube[0+(SAN*4)]=(byte)(x+1);	cube[1+(SAN*4)]=y;				cube[2+(SAN*4)]=z;							
							cube[3+(SAN*4)]=0.5f;cube[4+(SAN*4)]=0;cube[5+(SAN*4)]=0.5f;
							cube[0+(SAN*5)]=(byte)(x+1);	cube[1+(SAN*5)]=y;				cube[2+(SAN*5)]=(byte)(z+1);						
							cube[3+(SAN*5)]=0.5f;cube[4+(SAN*5)]=0;cube[5+(SAN*5)]=0.5f;
							writeTarget.put(cube);c+=SAN*6;}
							if((y==CHUNK_DIMENSION-1&&(overrideDrawTop||this.getY()==World.HEIGHT-1||mg.shouldDraw(neighbours[Direction.YP.ordinal()].getCubeAt(x, 0, z),this.chunkCubes.get(x,y,z),liquidTag,Direction.YP)))
									||(y!=CHUNK_DIMENSION-1&&(overrideDrawTop||mg.shouldDraw(this.chunkCubes.get(x,y+1,z),this.chunkCubes.get(x,y,z),liquidTag,Direction.YP)))){
							cube[0+(SAN*0)]=x;				cube[1+(SAN*0)]=y+heightxmzm;	cube[2+(SAN*0)]=z;						
							cube[3+(SAN*0)]=waternormalxmzm.x;cube[4+(SAN*0)]=waternormalxmzm.y;cube[5+(SAN*0)]=waternormalxmzm.z;
							cube[0+(SAN*1)]=x;				cube[1+(SAN*1)]=y+heightxmzp;	cube[2+(SAN*1)]=(byte)(z+1);						
							cube[3+(SAN*1)]=waternormalxmzp.x;cube[4+(SAN*1)]=waternormalxmzp.y;cube[5+(SAN*1)]=waternormalxmzp.z;
							cube[0+(SAN*2)]=(byte)(x+1);	cube[1+(SAN*2)]=y+heightxpzm;	cube[2+(SAN*2)]=z;						
							cube[3+(SAN*2)]=waternormalxpzm.x;cube[4+(SAN*2)]=waternormalxpzm.y;cube[5+(SAN*2)]=waternormalxpzm.z;
							cube[0+(SAN*3)]=(byte)(x+1);	cube[1+(SAN*3)]=y+heightxpzm;	cube[2+(SAN*3)]=z;						
							cube[3+(SAN*3)]=waternormalxpzm.x;cube[4+(SAN*3)]=waternormalxpzm.y;cube[5+(SAN*3)]=waternormalxpzm.z;
							cube[0+(SAN*4)]=x;				cube[1+(SAN*4)]=y+heightxmzp;	cube[2+(SAN*4)]=(byte)(z+1);						
							cube[3+(SAN*4)]=waternormalxmzp.x;cube[4+(SAN*4)]=waternormalxmzp.y;cube[5+(SAN*4)]=waternormalxmzp.z;
							cube[0+(SAN*5)]=(byte)(x+1);	cube[1+(SAN*5)]=y+heightxpzp;	cube[2+(SAN*5)]=(byte)(z+1);						
							cube[3+(SAN*5)]=waternormalxpzp.x;cube[4+(SAN*5)]=waternormalxpzp.y;cube[5+(SAN*5)]=waternormalxpzp.z;
							writeTarget.put(cube);c+=SAN*6;}
							
							// Z axis faces
							if((z==0&&mg.shouldDraw(neighbours[Direction.ZM.ordinal()].getCubeAt(x, y, CHUNK_DIMENSION-1),this.chunkCubes.get(x,y,z),liquidTag,Direction.ZM))
									||(z!=0&&mg.shouldDraw(this.chunkCubes.get(x,y,z-1),this.chunkCubes.get(x,y,z),liquidTag,Direction.ZM))){
							cube[0+(SAN*0)]=x;				cube[1+(SAN*0)]=y;				cube[2+(SAN*0)]=z;						
							cube[3+(SAN*0)]=0.5f;cube[4+(SAN*0)]=0.5f;cube[5+(SAN*0)]=0;
							cube[0+(SAN*1)]=x;				cube[1+(SAN*1)]=y+heightxmzm;	cube[2+(SAN*1)]=z;						
							cube[3+(SAN*1)]=0.5f;cube[4+(SAN*1)]=0.5f;cube[5+(SAN*1)]=0;
							cube[0+(SAN*2)]=(byte)(x+1);	cube[1+(SAN*2)]=y;				cube[2+(SAN*2)]=z;						
							cube[3+(SAN*2)]=0.5f;cube[4+(SAN*2)]=0.5f;cube[5+(SAN*2)]=0;
							cube[0+(SAN*3)]=(byte)(x+1);	cube[1+(SAN*3)]=y;				cube[2+(SAN*3)]=z;						
							cube[3+(SAN*3)]=0.5f;cube[4+(SAN*3)]=0.5f;cube[5+(SAN*3)]=0;
							cube[0+(SAN*4)]=x;				cube[1+(SAN*4)]=y+heightxmzm;	cube[2+(SAN*4)]=z;						
							cube[3+(SAN*4)]=0.5f;cube[4+(SAN*4)]=0.5f;cube[5+(SAN*4)]=0;
							cube[0+(SAN*5)]=(byte)(x+1);	cube[1+(SAN*5)]=y+heightxpzm;	cube[2+(SAN*5)]=z;						
							cube[3+(SAN*5)]=0.5f;cube[4+(SAN*5)]=0.5f;cube[5+(SAN*5)]=0;
							writeTarget.put(cube);c+=SAN*6;}
							if((z==CHUNK_DIMENSION-1&&mg.shouldDraw(neighbours[Direction.ZP.ordinal()].getCubeAt(x, y, 0),this.chunkCubes.get(x,y,z),liquidTag,Direction.ZP))
									||(z!=CHUNK_DIMENSION-1&&mg.shouldDraw(this.chunkCubes.get(x,y,z+1),this.chunkCubes.get(x,y,z),liquidTag,Direction.ZP))){
							cube[0+(SAN*0)]=x;				cube[1+(SAN*0)]=y;				cube[2+(SAN*0)]=(byte)(z+1);						
							cube[3+(SAN*0)]=0.5f;cube[4+(SAN*0)]=0.5f;cube[5+(SAN*0)]=1;
							cube[0+(SAN*1)]=(byte)(x+1);	cube[1+(SAN*1)]=y;				cube[2+(SAN*1)]=(byte)(z+1);						
							cube[3+(SAN*1)]=0.5f;cube[4+(SAN*1)]=0.5f;cube[5+(SAN*1)]=1;
							cube[0+(SAN*2)]=x;				cube[1+(SAN*2)]=y+heightxmzp;	cube[2+(SAN*2)]=(byte)(z+1);						
							cube[3+(SAN*2)]=0.5f;cube[4+(SAN*2)]=0.5f;cube[5+(SAN*2)]=1;
							cube[0+(SAN*3)]=x;				cube[1+(SAN*3)]=y+heightxmzp;	cube[2+(SAN*3)]=(byte)(z+1);						
							cube[3+(SAN*3)]=0.5f;cube[4+(SAN*3)]=0.5f;cube[5+(SAN*3)]=1;
							cube[0+(SAN*4)]=(byte)(x+1);	cube[1+(SAN*4)]=y;				cube[2+(SAN*4)]=(byte)(z+1);						
							cube[3+(SAN*4)]=0.5f;cube[4+(SAN*4)]=0.5f;cube[5+(SAN*4)]=1;
							cube[0+(SAN*5)]=(byte)(x+1);	cube[1+(SAN*5)]=y+heightxpzp;	cube[2+(SAN*5)]=(byte)(z+1);						
							cube[3+(SAN*5)]=0.5f;cube[4+(SAN*5)]=0.5f;cube[5+(SAN*5)]=1;
							writeTarget.put(cube);c+=SAN*6;} 
							
						}
						else //Normal blocks
						{
							// X axis faces
							if((x==0&&mg.shouldDraw(neighbours[Direction.XM.ordinal()].getCubeAt(CHUNK_DIMENSION-1, y, z),this.chunkCubes.get(x,y,z),liquidTag,Direction.XM))
									||(x!=0&&mg.shouldDraw(this.chunkCubes.get(x-1,y,z),this.chunkCubes.get(x,y,z),liquidTag,Direction.XM))){
							cube[0+(SAN*0)]=x;		cube[1+(SAN*0)]=y;		cube[2+(SAN*0)]=z;		
							cube[3+(SAN*0)]=BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*0)]=this.getNaturalBrightnessFaceXAverageAt(x-1, y, z); cube[5+(SAN*0)]=max(lp,this.getArtificialBrightnessFaceXAverageAt(x-1, y, z));
							cube[0+(SAN*1)]=x;				cube[1+(SAN*1)]=y;				cube[2+(SAN*1)]=(byte)(z+1);	
							cube[3+(SAN*1)]=BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*1)]=this.getNaturalBrightnessFaceXAverageAt(x-1, y, z+1); cube[5+(SAN*1)]=max(lp,this.getArtificialBrightnessFaceXAverageAt(x-1, y, z+1));											
							cube[0+(SAN*2)]=x;				cube[1+(SAN*2)]=y+heightxmzm;	cube[2+(SAN*2)]=z;
							cube[3+(SAN*2)]=BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*2)]=this.getNaturalBrightnessFaceXAverageAt(x-1, y+1, z); cube[5+(SAN*2)]=max(lp,this.getArtificialBrightnessFaceXAverageAt(x-1, y+1, z));										
							cube[0+(SAN*3)]=x;				cube[1+(SAN*3)]=y+heightxmzm;	cube[2+(SAN*3)]=z;
							cube[3+(SAN*3)]=BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*3)]=this.getNaturalBrightnessFaceXAverageAt(x-1, y+1, z); cube[5+(SAN*3)]=max(lp,this.getArtificialBrightnessFaceXAverageAt(x-1, y+1, z));											
							cube[0+(SAN*4)]=x;				cube[1+(SAN*4)]=y;				cube[2+(SAN*4)]=(byte)(z+1);
							cube[3+(SAN*4)]=BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*4)]=this.getNaturalBrightnessFaceXAverageAt(x-1, y, z+1); cube[5+(SAN*4)]=max(lp,this.getArtificialBrightnessFaceXAverageAt(x-1, y, z+1));											
							cube[0+(SAN*5)]=x;				cube[1+(SAN*5)]=y+heightxmzp;	cube[2+(SAN*5)]=(byte)(z+1);
							cube[3+(SAN*5)]=BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*5)]=this.getNaturalBrightnessFaceXAverageAt(x-1, y+1, z+1); cube[5+(SAN*5)]=max(lp,this.getArtificialBrightnessFaceXAverageAt(x-1, y+1, z+1));										
							writeTarget.put(cube);c+=SAN*6;}
							if((x==CHUNK_DIMENSION-1&&mg.shouldDraw(neighbours[Direction.XP.ordinal()].getCubeAt(0, y, z),this.chunkCubes.get(x,y,z),liquidTag,Direction.XP))
								||(x!=CHUNK_DIMENSION-1&&mg.shouldDraw(this.chunkCubes.get(x+1,y,z),this.chunkCubes.get(x,y,z),liquidTag,Direction.XP))){
							cube[0+(SAN*0)]=(byte)(x+1);	cube[1+(SAN*0)]=y;				cube[2+(SAN*0)]=z;
							cube[3+(SAN*0)]=1000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*0)]=this.getNaturalBrightnessFaceXAverageAt(x+1, y, z); cube[5+(SAN*0)]=max(lp,this.getArtificialBrightnessFaceXAverageAt(x+1, y, z));							
							cube[0+(SAN*1)]=(byte)(x+1);	cube[1+(SAN*1)]=y+heightxpzm;	cube[2+(SAN*1)]=z;
							cube[3+(SAN*1)]=1000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*1)]=this.getNaturalBrightnessFaceXAverageAt(x+1, y+1, z); cube[5+(SAN*1)]=max(lp,this.getArtificialBrightnessFaceXAverageAt(x+1, y+1, z));							
							cube[0+(SAN*2)]=(byte)(x+1);	cube[1+(SAN*2)]=y;				cube[2+(SAN*2)]=(byte)(z+1);
							cube[3+(SAN*2)]=1000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*2)]=this.getNaturalBrightnessFaceXAverageAt(x+1, y, z+1); cube[5+(SAN*2)]=max(lp,this.getArtificialBrightnessFaceXAverageAt(x+1, y, z+1));							
							cube[0+(SAN*3)]=(byte)(x+1);	cube[1+(SAN*3)]=y;				cube[2+(SAN*3)]=(byte)(z+1);
							cube[3+(SAN*3)]=1000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*3)]=this.getNaturalBrightnessFaceXAverageAt(x+1, y, z+1); cube[5+(SAN*3)]=max(lp,this.getArtificialBrightnessFaceXAverageAt(x+1, y, z+1));							
							cube[0+(SAN*4)]=(byte)(x+1);	cube[1+(SAN*4)]=y+heightxpzm;	cube[2+(SAN*4)]=z;	
							cube[3+(SAN*4)]=1000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*4)]=this.getNaturalBrightnessFaceXAverageAt(x+1, y+1, z); cube[5+(SAN*4)]=max(lp,this.getArtificialBrightnessFaceXAverageAt(x+1, y+1, z));							
							cube[0+(SAN*5)]=(byte)(x+1);	cube[1+(SAN*5)]=y+heightxpzp;	cube[2+(SAN*5)]=(byte)(z+1);
							cube[3+(SAN*5)]=1000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*5)]=this.getNaturalBrightnessFaceXAverageAt(x+1, y+1, z+1); cube[5+(SAN*5)]=max(lp,this.getArtificialBrightnessFaceXAverageAt(x+1, y+1, z+1));							
							writeTarget.put(cube);c+=SAN*6;}
							
							// Y axis faces
							if((y==0&&this.getY()!=0&&(mg.shouldDraw(neighbours[Direction.YM.ordinal()].getCubeAt(x, CHUNK_DIMENSION-1, z),this.chunkCubes.get(x,y,z),liquidTag,Direction.YM)||overrideDrawBot))
									||(y!=0&&(mg.shouldDraw(this.chunkCubes.get(x,y-1,z),this.chunkCubes.get(x,y,z),liquidTag,Direction.YM)||overrideDrawBot))){
							cube[0+(SAN*0)]=x;				cube[1+(SAN*0)]=y;				cube[2+(SAN*0)]=z;
							cube[3+(SAN*0)]=2000+BlockLibrary.getDownTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*0)]=this.getNaturalBrightnessFaceYAverageAt(x, y-1, z); cube[5+(SAN*0)]=max(lp,this.getArtificialBrightnessFaceYAverageAt(x, y-1, z));							
							cube[0+(SAN*1)]=(byte)(x+1);	cube[1+(SAN*1)]=y;				cube[2+(SAN*1)]=z;
							cube[3+(SAN*1)]=2000+BlockLibrary.getDownTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*1)]=this.getNaturalBrightnessFaceYAverageAt(x+1, y-1, z); cube[5+(SAN*1)]=max(lp,this.getArtificialBrightnessFaceYAverageAt(x+1, y-1, z));							
							cube[0+(SAN*2)]=x;				cube[1+(SAN*2)]=y;				cube[2+(SAN*2)]=(byte)(z+1);
							cube[3+(SAN*2)]=2000+BlockLibrary.getDownTex(this.chunkCubes.get(x,y,z)); cube[4+(SAN*2)]=this.getNaturalBrightnessFaceYAverageAt(x, y-1, z+1); cube[5+(SAN*2)]=max(lp,this.getArtificialBrightnessFaceYAverageAt(x, y-1, z+1));										
							cube[0+(SAN*3)]=x;				cube[1+(SAN*3)]=y;				cube[2+(SAN*3)]=(byte)(z+1);
							cube[3+(SAN*3)]=2000+BlockLibrary.getDownTex(this.chunkCubes.get(x,y,z)); cube[4+(SAN*3)]=this.getNaturalBrightnessFaceYAverageAt(x, y-1, z+1); cube[5+(SAN*3)]=max(lp,this.getArtificialBrightnessFaceYAverageAt(x, y-1, z+1));								
							cube[0+(SAN*4)]=(byte)(x+1);	cube[1+(SAN*4)]=y;				cube[2+(SAN*4)]=z;	
							cube[3+(SAN*4)]=2000+BlockLibrary.getDownTex(this.chunkCubes.get(x,y,z)); cube[4+(SAN*4)]=this.getNaturalBrightnessFaceYAverageAt(x+1, y-1, z); cube[5+(SAN*4)]=max(lp,this.getArtificialBrightnessFaceYAverageAt(x+1, y-1, z));							
							cube[0+(SAN*5)]=(byte)(x+1);	cube[1+(SAN*5)]=y;				cube[2+(SAN*5)]=(byte)(z+1);
							cube[3+(SAN*5)]=2000+BlockLibrary.getDownTex(this.chunkCubes.get(x,y,z)); cube[4+(SAN*5)]=this.getNaturalBrightnessFaceYAverageAt(x+1, y-1, z+1); cube[5+(SAN*5)]=max(lp,this.getArtificialBrightnessFaceYAverageAt(x+1, y-1, z+1));							
							writeTarget.put(cube);c+=SAN*6;}
							if((y==CHUNK_DIMENSION-1&&(overrideDrawTop||this.getY()==World.HEIGHT-1||mg.shouldDraw(neighbours[Direction.YP.ordinal()].getCubeAt(x, 0, z),this.chunkCubes.get(x,y,z),liquidTag,Direction.YP)))
									||(y!=CHUNK_DIMENSION-1&&(overrideDrawTop||mg.shouldDraw(this.chunkCubes.get(x,y+1,z),this.chunkCubes.get(x,y,z),liquidTag,Direction.YP)))){
							cube[0+(SAN*0)]=x;				cube[1+(SAN*0)]=y+heightxmzm;	cube[2+(SAN*0)]=z;
							cube[3+(SAN*0)]=3000+BlockLibrary.getUpTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*0)]=this.getNaturalBrightnessFaceYAverageAt(x, y+1, z); cube[5+(SAN*0)]=max(lp,this.getArtificialBrightnessFaceYAverageAt(x, y+1, z));							
							cube[0+(SAN*1)]=x;				cube[1+(SAN*1)]=y+heightxmzp;	cube[2+(SAN*1)]=(byte)(z+1);
							cube[3+(SAN*1)]=3000+BlockLibrary.getUpTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*1)]=this.getNaturalBrightnessFaceYAverageAt(x, y+1, z+1); cube[5+(SAN*1)]=max(lp,this.getArtificialBrightnessFaceYAverageAt(x, y+1, z+1));							
							cube[0+(SAN*2)]=(byte)(x+1);	cube[1+(SAN*2)]=y+heightxpzm;	cube[2+(SAN*2)]=z;
							cube[3+(SAN*2)]=3000+BlockLibrary.getUpTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*2)]=this.getNaturalBrightnessFaceYAverageAt(x+1, y+1, z); cube[5+(SAN*2)]=max(lp,this.getArtificialBrightnessFaceYAverageAt(x+1, y+1, z));							
							cube[0+(SAN*3)]=(byte)(x+1);	cube[1+(SAN*3)]=y+heightxpzm;	cube[2+(SAN*3)]=z;
							cube[3+(SAN*3)]=3000+BlockLibrary.getUpTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*3)]=this.getNaturalBrightnessFaceYAverageAt(x+1, y+1, z); cube[5+(SAN*3)]=max(lp,this.getArtificialBrightnessFaceYAverageAt(x+1, y+1, z));							
							cube[0+(SAN*4)]=x;				cube[1+(SAN*4)]=y+heightxmzp;	cube[2+(SAN*4)]=(byte)(z+1);
							cube[3+(SAN*4)]=3000+BlockLibrary.getUpTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*4)]=this.getNaturalBrightnessFaceYAverageAt(x, y+1, z+1); cube[5+(SAN*4)]=max(lp,this.getArtificialBrightnessFaceYAverageAt(x, y+1, z+1));							
							cube[0+(SAN*5)]=(byte)(x+1);	cube[1+(SAN*5)]=y+heightxpzp;	cube[2+(SAN*5)]=(byte)(z+1);
							cube[3+(SAN*5)]=3000+BlockLibrary.getUpTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*5)]=this.getNaturalBrightnessFaceYAverageAt(x+1, y+1, z+1); cube[5+(SAN*5)]=max(lp,this.getArtificialBrightnessFaceYAverageAt(x+1, y+1, z+1));							
							writeTarget.put(cube);c+=SAN*6;}
							
							// Z axis faces
							if((z==0&&mg.shouldDraw(neighbours[Direction.ZM.ordinal()].getCubeAt(x, y, CHUNK_DIMENSION-1),this.chunkCubes.get(x,y,z),liquidTag,Direction.ZM))
									||(z!=0&&mg.shouldDraw(this.chunkCubes.get(x,y,z-1),this.chunkCubes.get(x,y,z),liquidTag,Direction.ZM))){
							cube[0+(SAN*0)]=x;				cube[1+(SAN*0)]=y;				cube[2+(SAN*0)]=z;
							cube[3+(SAN*0)]=4000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*0)]=this.getNaturalBrightnessFaceZAverageAt(x, y, z-1); cube[5+(SAN*0)]=max(lp,this.getArtificialBrightnessFaceZAverageAt(x, y, z-1));							
							cube[0+(SAN*1)]=x;				cube[1+(SAN*1)]=y+heightxmzm;	cube[2+(SAN*1)]=z;
							cube[3+(SAN*1)]=4000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*1)]=this.getNaturalBrightnessFaceZAverageAt(x, y+1, z-1); cube[5+(SAN*1)]=max(lp,this.getArtificialBrightnessFaceZAverageAt(x, y+1, z-1));							
							cube[0+(SAN*2)]=(byte)(x+1);	cube[1+(SAN*2)]=y;				cube[2+(SAN*2)]=z;
							cube[3+(SAN*2)]=4000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*2)]=this.getNaturalBrightnessFaceZAverageAt(x+1, y, z-1); cube[5+(SAN*2)]=max(lp,this.getArtificialBrightnessFaceZAverageAt(x+1, y, z-1));							
							cube[0+(SAN*3)]=(byte)(x+1);	cube[1+(SAN*3)]=y;				cube[2+(SAN*3)]=z;
							cube[3+(SAN*3)]=4000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*3)]=this.getNaturalBrightnessFaceZAverageAt(x+1, y, z-1); cube[5+(SAN*3)]=max(lp,this.getArtificialBrightnessFaceZAverageAt(x+1, y, z-1));							
							cube[0+(SAN*4)]=x;				cube[1+(SAN*4)]=y+heightxmzm;	cube[2+(SAN*4)]=z;
							cube[3+(SAN*4)]=4000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*4)]=this.getNaturalBrightnessFaceZAverageAt(x, y+1, z-1); cube[5+(SAN*4)]=max(lp,this.getArtificialBrightnessFaceZAverageAt(x, y+1, z-1));							
							cube[0+(SAN*5)]=(byte)(x+1);	cube[1+(SAN*5)]=y+heightxpzm;	cube[2+(SAN*5)]=z;
							cube[3+(SAN*5)]=4000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*5)]=this.getNaturalBrightnessFaceZAverageAt(x+1, y+1, z-1); cube[5+(SAN*5)]=max(lp,this.getArtificialBrightnessFaceZAverageAt(x+1, y+1, z-1));							
							writeTarget.put(cube);c+=SAN*6;}
							if((z==CHUNK_DIMENSION-1&&mg.shouldDraw(neighbours[Direction.ZP.ordinal()].getCubeAt(x, y, 0),this.chunkCubes.get(x,y,z),liquidTag,Direction.ZP))
									||(z!=CHUNK_DIMENSION-1&&mg.shouldDraw(this.chunkCubes.get(x,y,z+1),this.chunkCubes.get(x,y,z),liquidTag,Direction.ZP))){
							cube[0+(SAN*0)]=x;				cube[1+(SAN*0)]=y;				cube[2+(SAN*0)]=(byte)(z+1);
							cube[3+(SAN*0)]=5000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*0)]=this.getNaturalBrightnessFaceZAverageAt(x, y, z+1); cube[5+(SAN*0)]=max(lp,this.getArtificialBrightnessFaceZAverageAt(x, y, z+1));							
							cube[0+(SAN*1)]=(byte)(x+1);	cube[1+(SAN*1)]=y;				cube[2+(SAN*1)]=(byte)(z+1);
							cube[3+(SAN*1)]=5000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*1)]=this.getNaturalBrightnessFaceZAverageAt(x+1, y, z+1); cube[5+(SAN*1)]=max(lp,this.getArtificialBrightnessFaceZAverageAt(x+1, y, z+1));							
							cube[0+(SAN*2)]=x;				cube[1+(SAN*2)]=y+heightxmzp;	cube[2+(SAN*2)]=(byte)(z+1);
							cube[3+(SAN*2)]=5000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*2)]=this.getNaturalBrightnessFaceZAverageAt(x, y+1, z+1); cube[5+(SAN*2)]=max(lp,this.getArtificialBrightnessFaceZAverageAt(x, y+1, z+1));							
							cube[0+(SAN*3)]=x;				cube[1+(SAN*3)]=y+heightxmzp;	cube[2+(SAN*3)]=(byte)(z+1);
							cube[3+(SAN*3)]=5000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*3)]=this.getNaturalBrightnessFaceZAverageAt(x, y+1, z+1); cube[5+(SAN*3)]=max(lp,this.getArtificialBrightnessFaceZAverageAt(x, y+1, z+1));							
							cube[0+(SAN*4)]=(byte)(x+1);	cube[1+(SAN*4)]=y;				cube[2+(SAN*4)]=(byte)(z+1);
							cube[3+(SAN*4)]=5000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*4)]=this.getNaturalBrightnessFaceZAverageAt(x+1, y, z+1); cube[5+(SAN*4)]=max(lp,this.getArtificialBrightnessFaceZAverageAt(x+1, y, z+1));							
							cube[0+(SAN*5)]=(byte)(x+1);	cube[1+(SAN*5)]=y+heightxpzp;	cube[2+(SAN*5)]=(byte)(z+1);
							cube[3+(SAN*5)]=5000+BlockLibrary.getLatTex(this.chunkCubes.get(x,y,z)); 	cube[4+(SAN*5)]=this.getNaturalBrightnessFaceZAverageAt(x+1, y+1, z+1); cube[5+(SAN*5)]=max(lp,this.getArtificialBrightnessFaceZAverageAt(x+1, y+1, z+1));	
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
	
	/**
	 * Updates this chunk graphics card data, for it to match the current chunk data. Normally called when some part of the chunk has changed
	 */
	public void update()
	{
		if(this.changed) {
			if(this.updateFlag) this.changed=false;
			else{
				this.WF.requestChunkUpdate(this); //If the cube has changed we request an update
			}
		}
		//If the buffer of triangles isn't null, we have new data to upload
		if(this.toUpload!=null) 
		{
			if(this.getUpdateAccessSemaphore().tryAcquire()){ //Prevents us to upload data to the graphic card and create a new triangle buffer at the same time
				if(this.triangleNum==0&&this.triangleLiquidNum==0){ //If there are no triangles to draw
					//Bye array!
					if(this.chunkCubes.isTrueStorage()){ //If there was triangles to draw before, but now there aren't, we can substitute the array of chunk cubes over a constant value, saving memory
						byte defval=this.chunkCubes.get(0, 0, 0); //Not only the air chunks arent draw: Underground ones can be fully culled too
						this.WF.getMapHandler().storeChunk(getX(), getY(), getZ(), this.chunkCubes,this.initcializedFlag); //IF the chunk was all air it wouldn't matter, but if it isnt drawed
																														   //Because its culled, it contains different cubes inside it, they just cant be seen
																														   //We dispose the chunk per now, until some of this cubes can be seen, and then we will load it again. Saving memory!
						
						this.chunkCubes=new ConstantValueCubeStorage(defval,this,CubeStorageType.CUBES_STORAGE); //If it is air, it will always return 0. If it is solid, any solid value will do.
					}
					//We are disposing the chunk till some part of it can be seen, so we delete the vbo per now too
					if(this.vbo!=-1) {
						glDeleteBuffers(this.vbo);
						this.vbo=-1;
					}
				}
				else{
					//If the chunk was disposed, we reload it. Finally some part of it is visible!
					if(this.vbo==-1) this.vbo=glGenBuffers();
					if(!this.chunkCubes.isTrueStorage()){
						this.notifyCubeStorageUpdate(this.chunkCubes.get(0, 0, 0), CubeStorageType.CUBES_STORAGE); //Reload the original values into chunkCubes
					}
					
					//Upload the buffer data to the graphics card
					glBindBuffer(GL15.GL_ARRAY_BUFFER,this.vbo);
					glBufferData(GL15.GL_ARRAY_BUFFER,(this.triangleNum+this.triangleLiquidNum)*SAN*4,GL15.GL_DYNAMIC_DRAW); //Yes, the content can indeed change: But it doesn't change very often, so GL_DYNAMIC_DRAW is an overkill.
					glBufferSubData(GL15.GL_ARRAY_BUFFER,0,this.toUpload);
					glBufferSubData(GL15.GL_ARRAY_BUFFER,this.triangleNum*SAN*4,this.toUploadLiquid);
					glBindBuffer(GL15.GL_ARRAY_BUFFER,0);
				}
				
				//Recycle buffers and dispose resources used
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
	
	/**
	 * Draws this chunk into the screen, used the data already uploaded to the graphics card, using a shader <VSP> and a camera <c>
	 * 
	 * If no boundary checker is especified, the check will use the camera view frustrum, and no draw will happen if the entire chunk is outside of it.
	 * If a boundary checker is especified (Used normally for shadows), the check will use the boundary checker, and no draw will happen if the entire chunk is outside the bounds specified by <bc>
	 */
	public void draw(Camera c,VoxelShaderProgram VSP,BoundaryChecker bc)
	{
		if(changed||this.toUpload!=null) update(); //We update this chunk before drawing if some part of it has changed
		
		if(this.solidEmpty&&this.liquidEmpty) return; //If it is empty, we dont bother drawing
		
		this.drawed=true;
		if(bc==null){ //If no boundary check is especified, we check over the standard view frustrum boundary
			Matrix4f mvp=new Matrix4f();

			this.chunkModelMatrix.m30=(float)(chunkx*Chunk.CHUNK_DIMENSION-this.WF.getCameraCenter().x); this.chunkModelMatrix.m31=(float)(chunky*Chunk.CHUNK_DIMENSION-this.WF.getCameraCenter().y); this.chunkModelMatrix.m32=(float)(chunkz*Chunk.CHUNK_DIMENSION-this.WF.getCameraCenter().z);
			Matrix4f.mul(c.getProjectionViewMatrix(), this.chunkModelMatrix, mvp);
		
			Vector4f coords=MatrixHelper.multiply(mvp, CENTER_VECTOR);
			float xc=coords.x/(coords.w);
			float yc=coords.y/(coords.w);
			double normDiam=CHUNK_RADIUS/Math.abs(coords.w);
			if(Math.abs(xc)>1+normDiam || Math.abs(yc)>1+normDiam || coords.z<-CHUNK_RADIUS){
				this.drawed=false; //If the sphere envolving the chunk is outside the view frustrum, we dont draw it (It cant be seen, anyways)
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
			
			//Coordinates are centered in the player, to avoid floating point errors at high distances
			MatrixHelper.uploadTranslationMatrix(this.chunkModelMatrix,chunkx*Chunk.CHUNK_DIMENSION,chunky*Chunk.CHUNK_DIMENSION,chunkz*Chunk.CHUNK_DIMENSION,this.WF.getCameraCenter(),VSP.getModelMatrixLoc());
			
			glDrawArrays(GL_TRIANGLES, 0, this.triangleNum);
			glBindBuffer(GL15.GL_ARRAY_BUFFER,0);
		}
	}
	
	/**
	 * Draws the chunk liquid triangles into the screen. They will be only drawed if the chunk is on the boundary check, checked before in draw(), which setted a flag this.drawed if so.
	 */
	public void drawLiquids(Camera c,VoxelShaderProgram VSP)
	{
		if(this.liquidEmpty) return;
		if(this.drawed){
			glBindBuffer(GL15.GL_ARRAY_BUFFER,this.vbo);
			VSP.setupAttributes();
			MatrixHelper.uploadTranslationMatrix(this.chunkModelMatrix,chunkx*Chunk.CHUNK_DIMENSION,chunky*Chunk.CHUNK_DIMENSION,chunkz*Chunk.CHUNK_DIMENSION,this.WF.getCameraCenter(),VSP.getModelMatrixLoc());

			glDrawArrays(GL_TRIANGLES, this.triangleNum,this.triangleLiquidNum);
			glBindBuffer(GL15.GL_ARRAY_BUFFER,0);
		}
	}
	
	/**
	 * Updates all cubes marked for update in this chunk
	 */
	public boolean updateChunkCubes(float tEl)
	{
		if(this.updateCubes.size()>0)
		{
			List<CubePosition> updateCubesLocal;
			synchronized(this.updateCubes){
				updateCubesLocal=this.updateCubes;
				this.updateCubes=new LinkedList<CubePosition>();
			}
			for(CubePosition cp:updateCubesLocal)
			{
				byte cube=this.getCubeAt(cp.x, cp.y, cp.z);
				if(BlockLibrary.isLiquid(cube)) //If the cube is a water cube, flows.
				{
					int liquidLevel= BlockLibrary.getLiquidLevel(cube);
					byte baseCube=BlockLibrary.getLiquidBaseCube(cube);

					byte belowCube=getCubeAt(cp.x,cp.y-1,cp.z);
					if(!BlockLibrary.isDrawable(belowCube) || BlockLibrary.isCrossSectional(belowCube)){ //Water removes vegetation too

						setCubeAt(cp.x,cp.y-1,cp.z,BlockLibrary.getLiquidBlockWithLevel(baseCube, BlockLibrary.getLiquidMaxLevel(baseCube)));
					}
					else if(BlockLibrary.isSameBlock(cube, belowCube)){
						if(BlockLibrary.getLiquidLevel(belowCube)<BlockLibrary.getLiquidMaxLevel(belowCube))
						{
							setCubeAt(cp.x,cp.y-1,cp.z,BlockLibrary.getLiquidBlockWithLevel(baseCube, BlockLibrary.getLiquidMaxLevel(baseCube)));
						}
					}
					else if(liquidLevel>0)
					{
						if(!BlockLibrary.isDrawable(getCubeAt(cp.x+1,cp.y,cp.z))|| BlockLibrary.isCrossSectional(getCubeAt(cp.x+1,cp.y,cp.z))){
							setCubeAt(cp.x+1,cp.y,cp.z,BlockLibrary.getLiquidBlockWithLevel(baseCube, liquidLevel-1));
						}
						else if(BlockLibrary.isSameBlock(cube, getCubeAt(cp.x+1,cp.y,cp.z))){
							int liqlev=BlockLibrary.getLiquidLevel(getCubeAt(cp.x+1,cp.y,cp.z));
							if(liqlev>liquidLevel+1){
								setCubeAt(cp.x+1,cp.y,cp.z,BlockLibrary.getLiquidBlockWithLevel(baseCube, liqlev-1));
							}
							else if(liquidLevel>liqlev+1){
								setCubeAt(cp.x+1,cp.y,cp.z,BlockLibrary.getLiquidBlockWithLevel(baseCube, liquidLevel-1));
							}
						}
						
						if(!BlockLibrary.isDrawable(getCubeAt(cp.x-1,cp.y,cp.z)) || BlockLibrary.isCrossSectional(getCubeAt(cp.x-1,cp.y,cp.z))){
							setCubeAt(cp.x-1,cp.y,cp.z,BlockLibrary.getLiquidBlockWithLevel(baseCube, liquidLevel-1));
						}
						else if(BlockLibrary.isSameBlock(cube, getCubeAt(cp.x-1,cp.y,cp.z))){
							int liqlev=BlockLibrary.getLiquidLevel(getCubeAt(cp.x-1,cp.y,cp.z));
							if(liqlev>liquidLevel+1){
								setCubeAt(cp.x-1,cp.y,cp.z,BlockLibrary.getLiquidBlockWithLevel(baseCube, liqlev-1));
							}
							else if(liquidLevel>liqlev+1){
								setCubeAt(cp.x-1,cp.y,cp.z,BlockLibrary.getLiquidBlockWithLevel(baseCube, liquidLevel-1));
							}
						}
						
						if(!BlockLibrary.isDrawable(getCubeAt(cp.x,cp.y,cp.z+1)) || BlockLibrary.isCrossSectional(getCubeAt(cp.x,cp.y,cp.z+1))){
							setCubeAt(cp.x,cp.y,cp.z+1,BlockLibrary.getLiquidBlockWithLevel(baseCube, liquidLevel-1));
						}
						else if(BlockLibrary.isSameBlock(cube, getCubeAt(cp.x,cp.y,cp.z+1))){
							int liqlev=BlockLibrary.getLiquidLevel(getCubeAt(cp.x,cp.y,cp.z+1));
							if(liqlev>liquidLevel+1){
								setCubeAt(cp.x,cp.y,cp.z+1,BlockLibrary.getLiquidBlockWithLevel(baseCube, liqlev-1));
							}
							else if(liquidLevel>liqlev+1){
								setCubeAt(cp.x,cp.y,cp.z+1,BlockLibrary.getLiquidBlockWithLevel(baseCube, liquidLevel-1));
							}
						}
						
						if(!BlockLibrary.isDrawable(getCubeAt(cp.x,cp.y,cp.z-1)) || BlockLibrary.isCrossSectional(getCubeAt(cp.x,cp.y,cp.z-1))){
							setCubeAt(cp.x,cp.y,cp.z-1,BlockLibrary.getLiquidBlockWithLevel(baseCube, liquidLevel-1));
						}
						else if(BlockLibrary.isSameBlock(cube, getCubeAt(cp.x,cp.y,cp.z-1))){
							int liqlev=BlockLibrary.getLiquidLevel(getCubeAt(cp.x,cp.y,cp.z-1));
							if(liqlev>liquidLevel+1){
								setCubeAt(cp.x,cp.y,cp.z-1,BlockLibrary.getLiquidBlockWithLevel(baseCube, liqlev-1));
							}
							else if(liquidLevel>liqlev+1){
								setCubeAt(cp.x,cp.y,cp.z-1,BlockLibrary.getLiquidBlockWithLevel(baseCube, liquidLevel-1));
							}
						}
					}
					if(1==2/2)continue;
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
						//byte baseCube=(byte)(cube+BlockLibrary.getLiquidLevel(cube)-BlockLibrary.getLiquidMaxLevel(cube));
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
				else if(!BlockLibrary.isDrawable(cube)){ //If the cube is an air cube, marks all water cubes collindant to it (And to grass if its on top) to update.
					if(BlockLibrary.isLiquid(getCubeAt(cp.x,cp.y+1,cp.z))||BlockLibrary.isCrossSectional(getCubeAt(cp.x,cp.y+1,cp.z))){
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
				else if(cube==14){ //TNT
					int expPower=5;
					int expPower2=expPower*expPower;
					for(int x=-expPower;x<expPower;x++){
						
						int posy=(int)Math.sqrt(expPower2 - x*x);
						for(int y=-posy;y<posy;y++){
							int posz=(int)Math.sqrt(expPower2 - x*x - y*y);
							for(int z=-posz;z<posz;z++){
								if(!(x==0&&y==0&&z==0)&&getCubeAt(cp.x+x,cp.y+y,cp.z+z)==7) setCubeAt(cp.x+x,cp.y+y,cp.z+z,(byte)7);
								else if(getCubeAt(cp.x+x,cp.y+y,cp.z+z)!=27) setCubeAt(cp.x+x,cp.y+y,cp.z+z,(byte)0); //If the block isn't indestructible, blow it up
							}
						}
					}
				}
				else if(cube==15){ //WATER TOWER
					/*setCubeAt(cp.x,cp.y,cp.z,(byte)0);
					
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
					}*/
				}
				/*else if(cube==16){ //HIGH TOWER
					setCubeAt(cp.x,cp.y,cp.z,(byte)1);
					for(int i=1;i<Chunk.CHUNK_DIMENSION*(World.HEIGHT-this.chunky)-cp.y;i++)
					{
						if(getCubeAt(cp.x,cp.y+i,cp.z)==0)setCubeAt(cp.x,cp.y+i,cp.z,(byte)1);
						else break;
					}
				}*/
				else if(cube==24){
					//Antagonize water bb
					/*byte wcube=this.getCubeAt(cp.x+1, cp.y, cp.z);
					if(BlockLibrary.isLiquid(wcube)) this.setCubeAt(cp.x+1, cp.y, cp.z,(byte)24);
					wcube=this.getCubeAt(cp.x-1, cp.y, cp.z);
					if(BlockLibrary.isLiquid(wcube)) this.setCubeAt(cp.x-1, cp.y, cp.z,(byte)24);
					wcube=this.getCubeAt(cp.x, cp.y+1, cp.z);
					if(BlockLibrary.isLiquid(wcube)) this.setCubeAt(cp.x, cp.y+1, cp.z,(byte)24);
					wcube=this.getCubeAt(cp.x, cp.y-1, cp.z);
					if(BlockLibrary.isLiquid(wcube)) this.setCubeAt(cp.x, cp.y-1, cp.z,(byte)24);
					wcube=this.getCubeAt(cp.x, cp.y, cp.z+1);
					if(BlockLibrary.isLiquid(wcube)) this.setCubeAt(cp.x, cp.y, cp.z+1,(byte)24);
					wcube=this.getCubeAt(cp.x+1, cp.y, cp.z-1);
					if(BlockLibrary.isLiquid(wcube)) this.setCubeAt(cp.x, cp.y, cp.z-1,(byte)24);
					
					this.setCubeAt(cp.x, cp.y, cp.z,(byte)0);*/
				}
				else if(cube==25){
					//Antagonize water bb
					/*performFun(this.getCubeAt(cp.x+1, cp.y, cp.z),cp.x+1,cp.y,cp.z);
					performFun(this.getCubeAt(cp.x-1, cp.y, cp.z),cp.x-1,cp.y,cp.z);
					performFun(this.getCubeAt(cp.x, cp.y+1, cp.z),cp.x,cp.y+1,cp.z);
					performFun(this.getCubeAt(cp.x, cp.y-1, cp.z),cp.x,cp.y-1,cp.z);
					performFun(this.getCubeAt(cp.x, cp.y, cp.z+1),cp.x,cp.y,cp.z+1);
					performFun(this.getCubeAt(cp.x, cp.y, cp.z-1),cp.x,cp.y,cp.z-1);
					
					this.setCubeAt(cp.x, cp.y, cp.z,(byte)0);*/
					
					/*for(int x=cp.x;x<cp.x+30;x++)
					{
						for(int y=cp.y;y>cp.y-20;y--)
						{
							for(int z=cp.z;z<30+cp.z;z++)
							{
								this.setCubeAt(x, y, z, (byte)0);
							}
						}
					}*/
				}
				else if(BlockLibrary.isCrossSectional(cube)){
					if(this.getCubeAt(cp.x, cp.y-1, cp.z)!=1) this.setCubeAt(cp.x, cp.y, cp.z, (byte)0); //Grass without grass on down of it cant exist
				}
				else if(cube==17&&2==1){
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
	 * Stupid debug. Delete pls
	 */
	private void performFun(byte b,int x,int y,int z)
	{
		double r=Math.random();
		if(r<0.2){
			this.setCubeAt(x, y, z, (byte)(25));
		}
		else if(r<0.4){
			if(b==0) this.setCubeAt(x, y, z, (byte)(1));
		}
		else if(r<0.6){
			if(BlockLibrary.isOpaque(b)) this.setCubeAt(x, y, z, (byte)(0));
		}
		else if(r<0.7){
			if(BlockLibrary.isOpaque(b)) this.setCubeAt(x, y, z, (byte)(7));
		}
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
			//If liquid lets see if errors go away doing this
			/*else if (BlockLibrary.isLiquid(defaultVal)){
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
			}*/
			//If solid
			else{
				byte[][][] cubes=ByteArrayPool.getArrayUncleaned();
				ChunkData chunkGenerationData=WF.getMapHandler().getChunk(this.getX(), this.getY(),this.getZ(),cubes);
				if(chunkGenerationData.chunkGenerationResult==ChunkGenerationResult.CHUNK_HIGHER_THAN_HEIGHTMAP){
					ByteArrayPool.recycleArray(cubes);
					cubes=ByteArrayPool.getArray();
				}
				this.chunkCubes.dispose();
				this.chunkCubes=new ArrayCubeStorage(cubes,chunkGenerationData.chunkGenerationResult!=ChunkGenerationResult.CHUNK_NORMAL);
				this.initcializedFlag=this.initcializedFlag||chunkGenerationData.initcializedFlag;
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
	
	/**
	 * Returns the cube at x,y,z . If this coordinates are outside the chunk, gets the cube in that chunk instead. If that outside chunk doesn't exist, returns 0 (Air block)
	 */
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
	
	/** 
	 * Marks the cube x,y,z to be updated. If the cube is outside the chunk, marks the cube in that chunk instead.
	 */
	public void markCubeToUpdate(int x,int y,int z)
	{
		if( x<CHUNK_DIMENSION&&x>=0&&
				y<CHUNK_DIMENSION&&y>=0&&
				z<CHUNK_DIMENSION&&z>=0	) 
		{
			//Cube update
			synchronized(this.updateCubes)
			{
				Iterator<CubePosition> i=this.updateCubes.iterator();
				while(i.hasNext())
				{
					CubePosition cp=i.next();
					if(cp.x==x&&cp.y==y&&cp.z==z) return;//i.remove();
				}
				if(this.updateCubes.size()==0) this.WF.insertChunkInUpdateList(this);
				this.updateCubes.add(new CubePosition(x,y,z));
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
			if(c!=null) c.markCubeToUpdate(x, y, z);
		}
	}
	
	/**
	 * Used for water cubes, height of the cube in one point is equal to the average of heights of all the blocks collindant to the point
	 */
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
	
	/**
	 * Sets the cube in x,y,z to the val <val>. If the cube is outside the chunk, sets the cube in the corresponding chunk instead.
	 * Check how much chunks had changed with this cube change and sets the changed flag accordingly.
	 * Recalculates the light in the area, considering this new cube can let light pass or occlude it.
	 * Marks the cube to update.
	 */
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
			
				if(BlockLibrary.isOpaque(val))
				{
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
				else if(BlockLibrary.occludesNaturalLight(val)){
					if(getNaturalLightLevelAt(x,y,z)==15){
						removeNaturalBrightnessAt(x,y,z);
						this.setNaturalBrightnessAt(x, y, z, (byte)14);
						ArrayList<Chunk> downChunks=new ArrayList<Chunk>();
						for(int yc=this.chunky-1;yc>=0;yc--){
							Chunk chu=this.WF.getChunkByIndex(this.chunkx, yc, this.chunkz);
							downChunks.add(chu);
						}
						this.destroyNaturalLightInRay(x,y-1,z,downChunks,0);
						this.recalculateLightInRay(x, y-1, z, downChunks, 0);
					}
				}
				else{
					recalculateBrightnessOfCube(x,y,z);
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
	
	/**
	 * Returns true if the lightmap of this chunk has already been calculated
	 */
	public boolean isLightCalculated()
	{
		return this.lightCalculated;
	}
	
	/**
	 * Returns true if all the light in the chunk is equal to a value <val>.
	 * Consider that the light value is compacted, so in one byte it is stored both natural light (First 4 bits) and artificial light (Last 4 bits).
	 * This method compares the raw light value, so this consideration has to be taken in account.
	 */
	public boolean isAllLightEqualTo(byte val)
	{
		if(!this.isLightCalculated() || this.chunkCubesLight.isTrueStorage() || this.chunkCubesLight.get(0, 0, 0)!=val) return false;
		return true;
	}
	
	/**
	 * Returns true if all cubes in the chunk are equal to <val>
	 */
	public boolean isAllCubesEqualTo(byte val)
	{
		if(this.chunkCubes.isTrueStorage() || this.chunkCubes.get(0, 0, 0)!=val) return false;
		return true;
	}
	
	/**
	 * Fills all natural light in this chunk with the value <val>
	 * 
	 * If the artificial light in the chunk was all 0, returns true
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
	
	/**
	 * Initcializes the light values of the chunk, taking into consideration:
	 * 			- Natural light propagated top to bot, in form of natural rays (If unoccluded)
	 * 			- Indirect natural light propagated from neightbour chunks.
	 * 			- Artificial light propagated from neighbour chunks.
	 * 			- Artificial light propagated from light blocks located in this chunk
	 * 
	 * Also, upon calculating the lightmap, notifies that chunk as a new added neighbour and,
	 * if all the neighbours had been added, performs the second pass generation for the chunk
	 */
	public void createLightMap()
	{
		
		if(this.chunkCubesLight!=null) this.chunkCubesLight.dispose();
		this.chunkCubesLight=new ArrayCubeStorage(ByteArrayPool.getArray(),true);
		boolean noNaturalLight=true; //If there isnt natural light in the chunk, we can optimize some calculations / memory usage
		boolean fullNaturalLight=true; //If the chunk is full of natural light, we can optimize some calculations / memory usage
 
		
		//NATURAL LIGHT
		
		Chunk[] neighbours=this.WF.getNeighbours(this);
		boolean topFullNL=this.chunky==World.HEIGHT-1 || (neighbours[Direction.YP.ordinal()]!=null&&neighbours[Direction.YP.ordinal()].isAllLightEqualTo(NATURAL_LIGHT_FULL_VAL));
		boolean xpFullNL=(neighbours[Direction.XP.ordinal()]!=null&&neighbours[Direction.XP.ordinal()].isAllLightEqualTo(NATURAL_LIGHT_FULL_VAL));
		boolean xmFullNL=(neighbours[Direction.XM.ordinal()]!=null&&neighbours[Direction.XM.ordinal()].isAllLightEqualTo(NATURAL_LIGHT_FULL_VAL));
		boolean zpFullNL=(neighbours[Direction.ZP.ordinal()]!=null&&neighbours[Direction.ZP.ordinal()].isAllLightEqualTo(NATURAL_LIGHT_FULL_VAL));
		boolean zmFullNL=(neighbours[Direction.ZM.ordinal()]!=null&&neighbours[Direction.ZM.ordinal()].isAllLightEqualTo(NATURAL_LIGHT_FULL_VAL));
		
		boolean currentFullNL=false;

		if(topFullNL) //If the top neightbour is full of natural light, we are receiving a full chunk size light rays, unoccluded.
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
			
			//ASSUME ALL CHUNKS WITH NO CUBES IN DOWN OF THIS ONE AS CHUNKS WITH FULL NATURAL LIGHT (Basically, increments current till a chunk not full of natural light is found)
			Chunk current=this;
			Iterator<Chunk> it=downChunks.iterator();
			while(current!=null&&current.isAllCubesEqualTo((byte)0))
			{
				noNaturalLight=false;
				current=it.hasNext()?it.next():null;
			}
			
			//For the remaining chunks, light rays needs to be computed separatelly
			int maxstopy=current==null?miny-1:-1; //The max dist in which some natural light ray collides first
			int minstopy=this.getY(); //The min y dist some natural light reach
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
				this.chunkCubesLight.dispose(); //If we have reached this line, this chunk is full of natural light. Disposing the light array and changing it for a constant value storage
				this.chunkCubesLight=new ConstantValueCubeStorage(NATURAL_LIGHT_FULL_VAL,this,CubeStorageType.LIGHT_STORAGE);
				currentFullNL=true;
				for(Chunk c:downChunks){
					if(maxstopy<c.getY()) { //If this current down chunk is entirely covered with light rays
						
						//If there was no light here, all light is natural
						if(c.isAllLightEqualTo((byte)0)){
							c.chunkCubesLight.dispose();
							c.chunkCubesLight=new ConstantValueCubeStorage(NATURAL_LIGHT_FULL_VAL,c,CubeStorageType.LIGHT_STORAGE);
						}
						//IF the light was already all natural, we do nothing. If it wasnt, we fill the chunk with natural light and, if there wasnt any artificial light in it, we change its array of light for a constant value storage
						else if(!c.isAllLightEqualTo(NATURAL_LIGHT_FULL_VAL)){

							if(c.fillNaturalLightWith(NATURAL_LIGHT_FULL_VAL)){
								c.chunkCubesLight.dispose();
								c.chunkCubesLight=new ConstantValueCubeStorage(NATURAL_LIGHT_FULL_VAL,c,CubeStorageType.LIGHT_STORAGE);
							}
						}
					}
					else break;
				}
			}
			
			//Extend natural light naturally with flood algorithm (This one extends indirect natural light to other chunks, from occluded cubes, etc. Since now, all light was direct light, pure rays
			//Unoccluded, or darkness.
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
		//Same calculations as above but with a not full of light topo chunk. Some calculations are more expensive.
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
			
			//If either the top chunk neighbour isnt loaded yet, or its light havent been calculated, or it doesnt have any natural light to propagate, 
			//its obvious that this chunk will have a 0 natural light, so it is not full of natural light
			else fullNaturalLight=false;
		}
		else fullNaturalLight=false;
		
		
		//We have already propagated the natural light coming from this chunk. We propagate now the light coming from other chunks to us
		
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
						//If the light value is equal to 15, the ray has already been propagated, so no need for doing that again
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
		
		//Propagate artificial light from light cubes inside the chunk
		if(!(!this.chunkCubes.isTrueStorage() && !BlockLibrary.isLightSource(this.chunkCubes.get(0, 0, 0))))
		{
			for(int x=0;x<CHUNK_DIMENSION;x++)
			{
				for(int z=0;z<CHUNK_DIMENSION;z++)
				{
					for(int y=0;y<CHUNK_DIMENSION;y++)
					{
						if(BlockLibrary.isLightSource(this.chunkCubes.get(x, y, z)))
						{
							noNaturalLight=false;
							fullNaturalLight=false;
							setArtificialBrightnessAt(x,y,z,BlockLibrary.getLightProduced(this.chunkCubes.get(x, y, z)));
						}
					}
				}
			}
		}
		
		if(noNaturalLight) { //If there is no natural light, we change the light array for a constant value storage with no light
			this.chunkCubesLight.dispose();
			this.chunkCubesLight=new ConstantValueCubeStorage((byte)0,this,CubeStorageType.LIGHT_STORAGE);
		}
		else if(fullNaturalLight){ //If the chunk is full with natural light, we change the light array for a constant value storage with full natural light
			this.chunkCubesLight.dispose();
			this.chunkCubesLight=new ConstantValueCubeStorage(NATURAL_LIGHT_FULL_VAL,this,CubeStorageType.LIGHT_STORAGE);
		}
		//Lightmap calculated: Lets check for neighbors added (And inform near neighbors that this chunk has been added too)
		this.neighborsAdded=this.WF.getNeighboursAdded(this);
		if(this.neighborsAdded==134209535&&!this.initcializedFlag) {
			this.initcializedFlag=true;
			this.WF.getMapHandler().generateChunkObjects(this);
		}
		this.WF.notifyNeighbours(this);
		this.lightCalculated=true;
	}
	
	/**
	 * Propagates a natural light ray, in the -y direction, until it collides with a opaque block
	 */
	private int propagateNaturalLightRay(int x,int z,List<Chunk> downChunk,int stage)
	{
		return this.propagateNaturalLightRay(x, z, CHUNK_DIMENSION-1,downChunk, stage);
	}
	private int propagateNaturalLightRay(int x,int z,int yi,List<Chunk> downChunk,int stage)
	{
		if(getNaturalLightIn(x,yi,z)==15) return getY();
		for(int y=yi;y>=0;y--)
		{
			if(BlockLibrary.isOpaque(this.chunkCubes.get(x,y,z))||BlockLibrary.occludesNaturalLight(this.chunkCubes.get(x,y,z))) return getY();
			setNaturalLightIn(x,y,z,(byte)15);
		}
		if(downChunk.size()>stage&&downChunk.get(stage)!=null)
		{
			return downChunk.get(stage).propagateNaturalLightRay(x, z,downChunk,stage+1);
		}
		return getY()-1;
	}
	
	/**
	 * Destroy a natural light ray from a certain position, and going all the way down. The result is inconsistent: recalculateLightInRay needs to be called when destroyNaturalLightInRay finishes
	 */
	private void destroyNaturalLightInRay(int x,int yi,int z,List<Chunk> downChunk,int stage)
	{
		if(yi>=0){
		if(getNaturalLightIn(x,yi,z)!=15) return;
		for(int y=yi;y>=0;y--)
		{
			if(BlockLibrary.isOpaque(this.chunkCubes.get(x,y,z))||BlockLibrary.occludesNaturalLight(this.chunkCubes.get(x,y,z))) return;
			this.removeNaturalBrightnessAt(x, y, z);
		}
		}
		if(downChunk.size()>stage&&downChunk.get(stage)!=null)
		{
			downChunk.get(stage).destroyNaturalLightInRay(x, CHUNK_DIMENSION-1,z,downChunk,stage+1);
		}
	}
	
	/**
	 * After destroying a light ray, recalculates the indirect light resulting of that action.
	 */
	private void recalculateLightInRay(int x,int yi,int z,List<Chunk> downChunk,int stage)
	{
		if(yi>=0){
		for(int y=yi;y>=0;y--)
		{
			if(BlockLibrary.isOpaque(this.chunkCubes.get(x,y,z))||BlockLibrary.occludesNaturalLight(this.chunkCubes.get(x,y,z))) return;
			this.recalculateBrightnessOfCube(x, y, z);
		}
		}
		if(downChunk.size()>stage&&downChunk.get(stage)!=null)
		{
			downChunk.get(stage).recalculateLightInRay(x, CHUNK_DIMENSION-1,z,downChunk,stage+1);
		}
	}
	
	/**
	 * Gets the artificial light in the position x,y,z . If those positions lies outside the chunk, it gets the light from the corresponding chunk instead.
	 */
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
	
	/**
	 * Gets the natural light in the position x,y,z . If those positions lies outside the chunk, it gets the light from the corresponding chunk instead.
	 */
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
	
	/**
	 * Gets the artificial light of a point, normalized in range [0,1]
	 */
	public float getArtificialBrightnessAt(int x,int y,int z)
	{
		return getArtificialLightLevelAt(x,y,z)/MAX_LIGHT_LEVEL;
	}
	
	/**
	 * Gets the natural light of a point, normalized in range [0,1]
	 */
	public float getNaturalBrightnessAt(int x,int y,int z)
	{
		return getNaturalLightLevelAt(x,y,z)/MAX_LIGHT_LEVEL;
	}
	
	/**
	 * Gets the average artificial brightness of a point x,y,z in space, resulting from the average of all the lights of all cubes collindant to that point
	 * The method is now unused, as it doesn't take into consideration the cube face being examined
	 */
	@Deprecated
	public float getArtificialBrightnessAverageAt(int x,int y,int z)
	{
		return  (getArtificialBrightnessAt(x,y,z)+getArtificialBrightnessAt(x-1,y,z)+
				getArtificialBrightnessAt(x,y-1,z)+getArtificialBrightnessAt(x,y,z-1)+
				getArtificialBrightnessAt(x-1,y-1,z)+getArtificialBrightnessAt(x,y-1,z-1)+
				getArtificialBrightnessAt(x-1,y,z-1)+getArtificialBrightnessAt(x-1,y-1,z-1))/4;
	}
	
	/**
	 * Gets the average artificial brightness of a point x,y,z in space, resulting from the average of all the lights of all cubes collindant to that point, and considering a Y plane
	 */
	public float getArtificialBrightnessFaceYAverageAt(int x,int y,int z)
	{
		return  (getArtificialBrightnessAt(x,y,z)+getArtificialBrightnessAt(x-1,y,z)+
				getArtificialBrightnessAt(x,y,z-1)+getArtificialBrightnessAt(x-1,y,z-1))/4;
	}
	
	/**
	 * Gets the average artificial brightness of a point x,y,z in space, resulting from the average of all the lights of all cubes collindant to that point, and considering a Z plane
	 */
	public float getArtificialBrightnessFaceZAverageAt(int x,int y,int z)
	{
		return  (getArtificialBrightnessAt(x,y,z)+getArtificialBrightnessAt(x-1,y,z)+
				getArtificialBrightnessAt(x,y-1,z)+getArtificialBrightnessAt(x-1,y-1,z))/4;
	}
	
	/**
	 * Gets the average artificial brightness of a point x,y,z in space, resulting from the average of all the lights of all cubes collindant to that point, and considering a X plane
	 */
	public float getArtificialBrightnessFaceXAverageAt(int x,int y,int z)
	{
		return  (getArtificialBrightnessAt(x,y,z)+getArtificialBrightnessAt(x,y-1,z)+
				getArtificialBrightnessAt(x,y,z-1)+getArtificialBrightnessAt(x,y-1,z-1))/4;
	}
	
	/**
	 * Gets the average natural brightness of a point x,y,z in space, resulting from the average of all the lights of all cubes collindant to that point, and considering a Y plane
	 */
	public float getNaturalBrightnessFaceYAverageAt(int x,int y,int z)
	{
		return  (getNaturalBrightnessAt(x,y,z)+getNaturalBrightnessAt(x-1,y,z)+
				getNaturalBrightnessAt(x,y,z-1)+getNaturalBrightnessAt(x-1,y,z-1))/4;
	}
	
	/**
	 * Gets the average natural brightness of a point x,y,z in space, resulting from the average of all the lights of all cubes collindant to that point, and considering a Z plane
	 */
	public float getNaturalBrightnessFaceZAverageAt(int x,int y,int z)
	{
		return  (getNaturalBrightnessAt(x,y,z)+getNaturalBrightnessAt(x-1,y,z)+
				getNaturalBrightnessAt(x,y-1,z)+getNaturalBrightnessAt(x-1,y-1,z))/4;
	}
	
	/**
	 * Gets the average natural brightness of a point x,y,z in space, resulting from the average of all the lights of all cubes collindant to that point, and considering a X plane
	 */
	public float getNaturalBrightnessFaceXAverageAt(int x,int y,int z)
	{
		return  (getNaturalBrightnessAt(x,y,z)+getNaturalBrightnessAt(x,y-1,z)+
				getNaturalBrightnessAt(x,y,z-1)+getNaturalBrightnessAt(x,y-1,z-1))/4;
	}
	
	/**
	 * Gets the average natural brightness of a point x,y,z in space, resulting from the average of all the lights of all cubes collindant to that point
	 * The method is now unused, as it doesn't take into consideration the cube face being examined
	 */
	@Deprecated
	public float getNaturalBrightnessAverageAt(int x,int y,int z)
	{
		return  (getNaturalBrightnessAt(x,y,z)+getNaturalBrightnessAt(x-1,y,z)+
				getNaturalBrightnessAt(x,y-1,z)+getNaturalBrightnessAt(x,y,z-1)+
				getNaturalBrightnessAt(x-1,y-1,z)+getNaturalBrightnessAt(x,y-1,z-1)+
				getNaturalBrightnessAt(x-1,y,z-1)+getNaturalBrightnessAt(x-1,y-1,z-1))/4;
	}
	
	/**
	 * Sets the artificial brightness at pos x,y,z to the value <val> and performs a flood algorithm filling the nearest cubes with decrements of this value based on the light distance.
	 * Considering a val of 3, the neighbour cubes will be set at 2, and the neighbors of the neighbors, to 1. This is an ADDING LIGHT method: It will not remove existing light, and it will be
	 * only set to the especified value if the current light is less than the <val> specified.
	 * If x,y,z is outside the current chunk, the changes will be applied in the corresponding chunk.
	 */
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
			//Decrementing val by 1 unit per block travelled.
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
	
	/**
	 * Sets the natural brightness at pos x,y,z to the value <val> and performs a flood algorithm filling the nearest cubes with decrements of this value based on the light distance.
	 * Considering a val of 3, the neighbour cubes will be set at 2, and the neighbors of the neighbors, to 1. This is an ADDING LIGHT method: It will not remove existing light, and it will be
	 * only set to the especified value if the current light is less than the <val> specified.
	 * If x,y,z is outside the current chunk, the changes will be applied in the corresponding chunk.
	 */
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
			//Decrementing val by 1 unit per block travelled.
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
	
	/**
	 * Sets the artificial brightness at pos x,y,z to 0, and perform a recursive flood removing algorithm, removing not only the light value at this position,
	 * but every light contribution this light could have apported to other near blocks.
	 * If x,y,z is outside the current chunk, the changes will be applied in the corresponding chunk.
	 */
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
			//Remembers light value before starting to remove it
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
				//Light will be removed if we had successfully removed all light collindant to it (If that happens, it was the light we removed which was apporting the light to this cube)
				if(maxadjbright==0){
					setArtificialLightIn(x,y,z,(byte)0);
				}
				//If the light value was more than what it should be if the light we removed was the one apporting most light to the cube, we dont remove any light.
				//If it is less, we set the new light value performing flood (Adjacent light - 1), and continue removing light.
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
	
	/**
	 * Sets the artificial brightness at pos x,y,z to 0, and perform a recursive flood removing algorithm, removing not only the light value at this position,
	 * but every light contribution this light could have apported to other near blocks.
	 * If x,y,z is outside the current chunk, the changes will be applied in the corresponding chunk.
	 */
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
			//Remembers light value before starting to remove it
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
				//Light will be removed if we had successfully removed all light collindant to it (If that happens, it was the light we removed which was apporting the light to this cube)
				if(maxadjbright==0){
					setNaturalLightIn(x,y,z,(byte)0);
				}
				//If the light value was more than what it should be if the light we removed was the one apporting most light to the cube, we dont remove any light.
				//If it is less, we set the new light value performing flood (Adjacent light - 1), and continue removing light.
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
	
	/**
	 * Recalculates light for the cube x,y,z and its surroudings (Called when a cube is inserted or deleted)
	 * If the cube inserted was opaque, it will occlude light, and some flood light removing will be performed
	 * If the cube inserted was transparent, it will let light pass and some flood light adding will be performed
	 * 
	 */
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
	
	/**
	 * Extends natural light in a vertical column of the chunk. Calculates all indirect natural light caused by a light ray passing down in the pos x,z
	 */
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
	
	/**
	 * Returns the max artificial adjacent light of the cube at pos x,y,z for all its 6 neighbours
	 */
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
	
	/**
	 * Returns the max natural adjacent light of the cube at pos x,y,z for all its 6 neighbours
	 */
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
	
	/**
	 * Gets the artificial light value in the position x,y,z. It is encoded on the last 4 bits of the light byte, so a bitwise will be automatically performed to extract it
	 */
	private byte getArtificialLightIn(int x,int y,int z)
	{
		return (byte)(this.chunkCubesLight.get(x,y,z) & 0xF);
	}
	
	/**
	 * Sets the artificial light value in the position x,y,z. It is encoded on the last 4 bits of the light byte, so a bitwise will be automatically performed to insert it
	 */
	private void setArtificialLightIn(int x,int y,int z,byte val)
	{
		if(val!=getArtificialLightIn(x,y,z)) this.changed=true;
		this.chunkCubesLight.set(x,y,z,(byte)((this.chunkCubesLight.get(x,y,z) & 0xF0 ) | (val & 0xF)));
	}
	
	/**
	 * Gets the natural light value in the position x,y,z. It is encoded on the first 4 bits of the light byte, so a bitwise will be automatically performed to extract it
	 */
	private byte getNaturalLightIn(int x,int y,int z)
	{
		return (byte)((this.chunkCubesLight.get(x,y,z) >> 4) &0xF);
	}
	
	/**
	 * Sets the natural light value in the position x,y,z. It is encoded on the first 4 bits of the light byte, so a bitwise will be automatically performed to insert it
	 */
	private void setNaturalLightIn(int x,int y,int z,byte val)
	{
		if(val!=getNaturalLightIn(x,y,z)) this.changed=true;
		this.chunkCubesLight.set(x,y,z,(byte)((this.chunkCubesLight.get(x,y,z) &0xF) | ((val << 4)&0xF0)));
	}
	
	/**
	 * Returns true if all chunk cubes are the same
	 */
	public boolean isConstantValue()
	{
		return !this.chunkCubes.isTrueStorage();
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
			if(this.neighborsAdded==134209535&&!this.initcializedFlag) {
				this.initcializedFlag=true;
				this.WF.getMapHandler().generateChunkObjects(this);
			}
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
	
	/**
	 * Disposes the chunk resources
	 */
	@Override
	public void fullClean()
	{
		//We don't dispose the chunk twice
		if(!this.deleted){
			this.deleted=true;
			
			this.WF.notifyNeighboursRemove(this); //We notify to this chunk neighbours this removal.
			
			if(this.toUpload!=null){ FloatBufferPool.recycleBuffer(this.toUpload); this.toUpload=null;} //Recycles back into the pool the float buffers in use, if they exist.
			if(this.toUploadLiquid!=null) {FloatBufferPool.recycleBuffer(this.toUploadLiquid); this.toUploadLiquid=null;}
			
			if(this.chunkCubes.isTrueStorage()||this.chunkCubes.get(0, 0, 0)==0) { //Stores the chunk cubes into the disk. A solid constant value chunk means that the chunk had been stored
																				   //in disk before to save memory, so we dont save it again in that case
				this.WF.getMapHandler().storeChunk(getX(), getY(), getZ(), this.chunkCubes,this.initcializedFlag);
			}

			if(this.lightCalculated) this.chunkCubesLight.dispose(); //Disposing the chunk light cubes storage if the light had been calculated.
			
			this.updateCubes.clear(); //Empties the list storing the cubes waiting for a cube event to be handled in them
			if(this.vbo!=-1) glDeleteBuffers(this.vbo); //If it exists, deletes the chunk vbo
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

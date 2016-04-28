package kubex.gui;

import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;

import ivengine.properties.Cleanable;
import ivengine.view.Camera;
import ivengine.view.MatrixHelper;
import kubex.KubexSettings;
import kubex.entity.EntityManager;
import kubex.entity.Player;
import kubex.shaders.VoxelShaderProgram;
import kubex.storage.ChunkStorage;
import kubex.storage.FileManager;
import kubex.storage.FloatBufferPool;
import kubex.utils.BoundaryChecker;
import kubex.utils.InputHandler;
import kubex.utils.KeyToggleListener;
import kubex.utils.SquareCorners;
import kubex.utils.Vector3d;
import kubex.utils.VoxelUtils;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * World class. All chunk insertion orders, generation orders, deletion orders, modification orders and rendering goes there. Also, gestionates player movement
 * and rendering centered in player, day time pass, etc.
 */
public class World implements Cleanable, KeyToggleListener
{
	private static final int MAX_CHUNK_LOADS_PER_TICK=10;
	private static final float[] DAY_TIME_SPEED={3600,200,50,20,10,5,1,0.5f};
	public int PLAYER_VIEW_FIELD=10;
	public static final int HEIGHT=8;
	public static final float CHUNK_UPDATE_TICK=0.3f;
	private int[] DIFTABLE;	//Table storing the rendering circle x width for each z coordinate
	
	private int vao;
	private EntityManager EM;
	private MapHandler MG;
	private ChunkStorage myChunks; //Chunk storage, where active chunks are located.
	private ChunkGenerator chunkGenerator;  //Chunk generator thread. Generates new chunks using noise functions or loading them from disk.
	private ChunkStorer chunkStorer; //Chunk storer thread. Stores chunks in disk, preventing blocks in the main thread.
	private ChunkUpdater chunkUpdater; //Chunk updater thread. Generates the float vertex buffer for the chunks, uploaded later to the GPU
	private List<Chunk> updateChunks=new LinkedList<Chunk>(); //All chunks which have cube events (water expanding, etc.) are stored here, too. This events will be managed until
															  //no more events happen in the chunk, moment in which it will be removed from this list
	private LinkedList<Chunk> updateChunksAdd=new LinkedList<Chunk>(); //Auxiliar list where the chunk which need to be inserted in updateChunks have to wait 
	private Camera cam;
	private Camera sunCamera;
	private Matrix4f customPVMatrix=null;
	private WorldFacade worldFacade;
	private Sky sky;
	private KubexSettings settings;
	
	private FileManager fileManager;
	
	private final SquareCorners worldCornersLow;
	private final SquareCorners worldCornersHigh;
	private Vector3d cameraCenterVector=new Vector3d();
	
	private float currentTime=12;
	private float chunkUpdateTickCont=0;
	private int currentDaySpeed;
	
	private VoxelShaderProgram VSP;
	private VoxelShaderProgram CustomVSP=null;
	
	private int lastChunkCenterX;private int lastChunkCenterZ;
	
	private LinkedList<Chunk> chunkAddList=new LinkedList<Chunk>();
	
	public World(VoxelShaderProgram VSP,Camera cam,Camera sunCamera,ShadowsManager shadowsManager,Sky sky,FileManager fileManager,KubexSettings settings)
	{
		this.settings=settings;
		this.currentDaySpeed=settings.DAY_SPEED;
		this.currentTime=settings.DAY_TIME;
		this.fileManager=fileManager;

		this.sunCamera=sunCamera;
		this.sky=sky;
		this.VSP=VSP;
		this.PLAYER_VIEW_FIELD=settings.RENDER_DISTANCE;
		
		DIFTABLE=createDiftable(PLAYER_VIEW_FIELD+1);
		//Create player
		Player p=new Player(settings.PLAYER_X,settings.PLAYER_Y,settings.PLAYER_Z,settings.CAM_PITCH,settings.CAM_YAW,settings.CUBE_SHORTCUTS,settings.CUBE_SELECTED,settings.MOUSE_SENSITIVITY,cam);
		
		float maxworldsize=(float)(Chunk.CHUNK_DIMENSION*(PLAYER_VIEW_FIELD+1.5f));
		this.worldCornersLow=new SquareCorners(	new Vector4f(-maxworldsize,0,-maxworldsize,1),
												new Vector4f(maxworldsize,0,-maxworldsize,1),
												new Vector4f(-maxworldsize,0,maxworldsize,1),
												new Vector4f(maxworldsize,0,maxworldsize,1));
		this.worldCornersHigh=new SquareCorners(new Vector4f(-maxworldsize,World.HEIGHT*Chunk.CHUNK_DIMENSION,-maxworldsize,1),
												new Vector4f(maxworldsize,World.HEIGHT*Chunk.CHUNK_DIMENSION,-maxworldsize,1),
												new Vector4f(-maxworldsize,World.HEIGHT*Chunk.CHUNK_DIMENSION,maxworldsize,1),
												new Vector4f(maxworldsize,World.HEIGHT*Chunk.CHUNK_DIMENSION,maxworldsize,1));
		
		//this.setWorldCenter(p.getX(),p.getY(), p.getZ());
		this.sunCamera.moveTo(0,0,0);
		this.worldFacade=new WorldFacade(this);
		this.EM=new EntityManager(p,worldFacade);
		this.cam=cam;
		this.myChunks=new ChunkStorage((PLAYER_VIEW_FIELD*2)+1);
		this.chunkGenerator=new ChunkGenerator(worldFacade);
		this.chunkStorer=new ChunkStorer(worldFacade);
		this.MG=new MapHandler(0,128,settings.MAP_CODE,settings.MAP_SEED,worldFacade,this.chunkStorer,fileManager);
		this.chunkGenerator.start();
		this.chunkStorer.start();
		this.chunkUpdater=new ChunkUpdater(p);
		this.chunkUpdater.start();
		
	
		 //Generates the map surrounding the player, in circle around of it with max distance equal to PLAYER_VIEW_FIELD (The rendering distance)
		for(int x=0, osc=1,val=1;x<=PLAYER_VIEW_FIELD;x+=osc,osc=(osc+val)*-1,val=-val)
		{
			for(int z=0,zosc=1,zval=1;z<=DIFTABLE[Math.abs(x)];z+=zosc,zosc=(zosc+zval)*-1,zval=-zval)
			{
				for(int y=HEIGHT-1;y>=0;y--)
				{
					this.chunkGenerator.generateChunk(x+(int)Math.floor(p.getX()/Chunk.CHUNK_DIMENSION),y,z+(int)Math.floor(p.getZ()/Chunk.CHUNK_DIMENSION));
				}
			}
		}
		this.lastChunkCenterX=(int)Math.floor(p.getX()/Chunk.CHUNK_DIMENSION);
		this.lastChunkCenterZ=(int)Math.floor(p.getZ()/Chunk.CHUNK_DIMENSION);

		this.sky.setCurrentTime(this.currentTime);
		
		InputHandler.addKeyToggleListener(InputHandler.P_VALUE, this); //For accelerating / decelerating time
		InputHandler.addKeyToggleListener(InputHandler.O_VALUE, this);
	}
	
	/**
	 * Setups the shader parameters before rendering. We are using a basic shader (All the complex things are managed by the deferred shaders) so only the upload of the viewProjection
	 * matrix will be neccesary
	 */
	private void setupShaderParameters()
	{
		this.getActiveShader().enable();

		MatrixHelper.uploadMatrix(getActivePVMatrix(), this.getActiveShader().getViewProjectionMatrixLoc());
	}
	
	/**
	 * Draws all the world chunks. By default, applies culling using the camera frustrum, if no BoundaryChecker is specified. 
	 * If a BoundaryChecker is specified, the chunk will test culling against it, checking if some part of it lies inside the BoundaryChecker or not.
	 * BoundaryCheckers are used when calculating shadows cascades, to minimize the amount of chunks drawed unnecesarily.
	 */
	public void draw()
	{
		this.draw(null);
	}
	public void draw(BoundaryChecker bc)
	{
		setupShaderParameters();
		
		GL11.glDisable( GL11.GL_BLEND );
		glEnable(GL11.GL_DEPTH_TEST);
		
		if(bc==null||bc.applyCullFace())GL11.glEnable(GL11.GL_CULL_FACE);
		else GL11.glDisable(GL11.GL_CULL_FACE);
		
		this.myChunks.initIter();
		Chunk c;
		while((c=this.myChunks.next())!=null) {
				c.draw(cam,this.getActiveShader(),bc);
			}
	}
	
	/**
	 * Draws the liquid blocks in the scene chunks.
	 */
	public void drawLiquids()
	{
		setupShaderParameters();
		
		GL11.glDisable(GL11.GL_CULL_FACE);
		
		this.myChunks.initIter();
		Chunk c;
		while((c=this.myChunks.next())!=null) {
			c.drawLiquids(cam,this.getActiveShader());
		}
		GL11.glEnable(GL11.GL_CULL_FACE);
	}
	
	/**
	 * After the first rendering pass, this method is called. Gets synchronously the already generated chunks provided by MapGenerator and inserts them in the final chunk storage.
	 */
	public void afterDrawTasks()
	{
		//New chunks inclusion
		boolean notifyLater=false;
		int rem=MAX_CHUNK_LOADS_PER_TICK;
		while(this.chunkAddList.size()>0&&rem>0){
			rem--;
			Chunk ca=null;
			synchronized(this){
				ca=this.chunkAddList.removeFirst();
				if(this.chunkAddList.size()==ChunkGenerator.MAX_CHUNKS_LOADED-1) {
					notifyLater=true;
				}
				this.myChunks.addChunk(ca.getX(), ca.getY(), ca.getZ(), ca);
			}
			ca.initChunk();
		}
		if(notifyLater) synchronized(this.chunkGenerator){ this.chunkGenerator.notifyAll();}
	}
	
	/**
	 * Performs all update pass needs in the map. Namely, managing player movement, making day time advance and inserting chunks to update list if they are requesting it.
	 * The chunk update list doesn't manages anything about render, neither is in another thread. It simply manages all cube events needed, like water expanding to the sides,
	 * TNT exploding and so on. Instead of calling managing the cube events of all chunks, we just manage the cube events of the chunks wich have said that inside of them there are
	 * already cubes in need for update.
	 */
	public void update(float tEl)
	{
		this.EM.update(tEl); //Moves player
		this.currentTime+=(tEl/DAY_TIME_SPEED[this.currentDaySpeed]); //Advances time. At midnight, time is 0 again
		if(this.currentTime>24) this.currentTime=0;
		
		this.sky.setCurrentTime(this.currentTime); //Sets the sun time to the day time
		
		this.chunkUpdateTickCont+=tEl; //Cube events will be handled every 0.3 seconds by default
		
		synchronized(this.updateChunks)
		{
			synchronized(this.updateChunksAdd)
			{
				//Inserts chunks in the update chunks list, if they dont exist there yet. Empties the updateChunksAdd list meanwhile, the auxiliar list in will chunks wich desire
				//manage its cube events are added temporally instead to updateChunks directly to prevent blocks.
				while(!this.updateChunksAdd.isEmpty()) {
					Chunk c=this.updateChunksAdd.poll();
					boolean canInsert=true;
					for(Chunk cl:this.updateChunks)
					{
						if(cl.getX()==c.getX()&&cl.getY()==c.getY()&&cl.getZ()==c.getZ())
						{
							canInsert=false;
							break;
						}
					}
					if(canInsert) this.updateChunks.add(c);
				}
			}
			
			//If the 0.3 seconds had been elapsed, manages all cube events of the chunks in updateChunks
			while(this.chunkUpdateTickCont>CHUNK_UPDATE_TICK)
			{
				this.chunkUpdateTickCont-=CHUNK_UPDATE_TICK;
				
				Iterator<Chunk> it=this.updateChunks.iterator();
				while(it.hasNext())
				{
					Chunk c=it.next();
					if(!c.updateChunkCubes(tEl)) it.remove(); //If there are no more cube events in this chunk, it is removed from the updateChunk list
				}
			}
		}
	}
	
	/**
	 * Returns true if player is underwater
	 */
	public boolean isUnderwater()
	{
		return this.EM.getPlayer().isUnderwater(this.worldFacade);
	}
	
	/**
	 * Returns the average light exposed by the player. Used while underwater, to calculate the scattering color (Brighter in function of the light)
	 */
	public float getAverageLightExposed()
	{
		return this.EM.getPlayer().getAverageLightExposed(this.worldFacade);
	}
	
	/**
	 * Returns the active shader used. It can be changed from the default when calculating shadows or whater layers, when different shaders are used
	 */
	public VoxelShaderProgram getActiveShader()
	{
		return this.CustomVSP==null?this.VSP : this.CustomVSP;
	}
	
	/**
	 * Returns the active projection view matrix. A custom one can be uploaded, when calculating shadows for example.
	 */
	public Matrix4f getActivePVMatrix()
	{
		return this.customPVMatrix==null?this.cam.getProjectionViewMatrix() : this.customPVMatrix;
	}
	
	/**
	 * Overrides the current shader, using a default one. If <customShader> is null, the default shader will be used from now on
	 */
	public void overrideCurrentShader(VoxelShaderProgram customShader)
	{
		this.CustomVSP=customShader;
	}
	
	/**
	 * Overrides the current projection view matrix, using a default one. If <pvmat> is null, the default matrix will be used from now on
	 */
	public void overrideCurrentPVMatrix(Matrix4f pvmat)
	{
		this.customPVMatrix=pvmat;
	}
	
	/**
	 * Gets the camera center vector
	 */
	public Vector3d getCameraCenter()
	{
		return this.cameraCenterVector;
	}
	
	/**
	 * Updates the camera center to match a position, being in this implementation the player position (Rendering centered in player)
	 */
	public void updateCameraCenter(double x,double y,double z) 
	{
		this.cameraCenterVector.x=x;this.cameraCenterVector.y=y;this.cameraCenterVector.z=z;
	}
	
	/**
	 * Gets the cube at the absolute position <x> <y> <z>
	 */
	public byte getContent(double x,double y,double z)
	{
		Chunk c;
		if((c=this.myChunks.getChunk(x/Chunk.CHUNK_DIMENSION, y/Chunk.CHUNK_DIMENSION, z/Chunk.CHUNK_DIMENSION))!=null){
			byte toRet= c.getCubeAt(VoxelUtils.trueMod(x,Chunk.CHUNK_DIMENSION),VoxelUtils.trueMod(y,Chunk.CHUNK_DIMENSION),VoxelUtils.trueMod(z,Chunk.CHUNK_DIMENSION));
			return toRet;
		}
		return 0;
	}
	
	/**
	 * Gets the artificial light at the cube in the position <x> <y> <z>. Those values should be changed to doubles if the max world distance increased from the one set (The int overflow),
	 * as the floats could have precision errors >1 unit in high values
	 */
	public float getContentArtificialLight(float x,float y,float z)
	{
		Chunk c;
		if((c=this.myChunks.getChunk(x/Chunk.CHUNK_DIMENSION, y/Chunk.CHUNK_DIMENSION, z/Chunk.CHUNK_DIMENSION))!=null && c.isLightCalculated()){
			float toRet= c.getArtificialBrightnessAt(VoxelUtils.trueMod(x,Chunk.CHUNK_DIMENSION),VoxelUtils.trueMod(y,Chunk.CHUNK_DIMENSION),VoxelUtils.trueMod(z,Chunk.CHUNK_DIMENSION));
			return toRet;
		}
		return -1;
	}
	
	/**
	 * Gets the natural light at the cube in the position <x> <y> <z>. Those values should be changed to doubles if the max world distance increased from the one set (The int overflow),
	 * as the floats could have precision errors >1 unit in high values
	 */
	public float getContentNaturalLight(float x,float y,float z)
	{
		Chunk c;
		if((c=this.myChunks.getChunk(x/Chunk.CHUNK_DIMENSION, y/Chunk.CHUNK_DIMENSION, z/Chunk.CHUNK_DIMENSION))!=null&& c.isLightCalculated()){
			float toRet= c.getNaturalBrightnessAt(VoxelUtils.trueMod(x,Chunk.CHUNK_DIMENSION),VoxelUtils.trueMod(y,Chunk.CHUNK_DIMENSION),VoxelUtils.trueMod(z,Chunk.CHUNK_DIMENSION));
			return toRet;
		}
		return -1;
	}
	
	/**
	 * Get the daylight illumination variable. In day its 1, at night is 0.45
	 */
	public float getDaylightAmount()
	{
		float altsin=(float)((Math.sin(this.sky.getSolarAltitude()+0.3f)));
		if(altsin<0) altsin=0;
		float ret=0.45f+altsin/1.65f;
		return ret;
	}
	
	/**
	 * Gets the world gravity. By default it is 15m/s
	 */
	public float getGravity()
	{
		return 15f;
	}

	/**
	 * Returns the map generator thread
	 */
	public MapHandler getMapGenerator()
	{
		return this.MG;
	}
	
	/**
	 * Returns the chunk updater thread
	 */
	public ChunkUpdater getChunkUpdater()
	{
		return this.chunkUpdater;
	}
	
	/**
	 * Returns the chunk storage
	 */
	public ChunkStorage getChunkStorage()
	{
		return this.myChunks;
	}
	
	/**
	 * Returns the chunk add list, in which chunks are placed by ChunkGenerator to be placed later into the final chunk storage
	 */
	public LinkedList<Chunk> getAddList()
	{
		return this.chunkAddList;
	}
	
	/**
	 * Adds a chunk <c> with cube events inside of it in the updateChunks list, for them to be managed in each cube events tick. Places it
	 * instead in the auxiliar list updateChunksAdd to prevent blocks. It will be inserted in the updateChunks list in the next tick.
	 */
	public void addChunkToUpdateList(Chunk c)
	{
		synchronized(this.updateChunksAdd)
		{
			boolean canInsert=true;
			for(Chunk cl:this.updateChunksAdd)
			{
				if(cl.getX()==c.getX()&&cl.getY()==c.getY()&&cl.getZ()==c.getZ())
				{
					canInsert=false;
					break;
				}
			}
			if(canInsert) this.updateChunksAdd.add(c);
		}
	}
	
	/**
	 * Removes the chunk at the position <x> <y> <z>
	 */
	private void removeChunk(int x,int y,int z)
	{
		//Remove from creation list (If exists)
		this.chunkGenerator.removeChunk(x,y,z);
		//Remove from adding list (If exists)
		synchronized(this){
			Iterator<Chunk> it=this.chunkAddList.iterator();
			while(it.hasNext()){
				Chunk c=it.next();
				if(c.getX()==x&&c.getY()==y&&c.getZ()==z) {
					c.fullClean();
					it.remove();
					break;
				}
			}
		}
		//If the chunks had cube events inside of it, removes it from the updateChunks list, as it is already removed and there is no need to manage cube events anymore
		synchronized(this.updateChunks)
		{
			Iterator<Chunk> it=this.updateChunks.iterator();
			while(it.hasNext())
			{
				Chunk itc=it.next();
				if(itc.getX()==x&&itc.getY()==y&&itc.getZ()==z) {itc.fullClean();it.remove(); break;}
			}
		}
		//Deletes it too from the auxiliar update chunks list in the case it exists there
		synchronized(this.updateChunksAdd)
		{
			Iterator<Chunk> it=this.updateChunksAdd.iterator();
			while(it.hasNext())
			{
				Chunk itc=it.next();
				if(itc.getX()==x&&itc.getY()==y&&itc.getZ()==z) {itc.fullClean();it.remove(); break;}
			}
		}
		//Remove from world (If exists)
		this.myChunks.removeChunk(x, y,z);
	}
	
	/**
	 * Reloads chunks around the player, removing the ones which are more far than the rendering distance and creating the ones that are less far than the rendering distance
	 */
	public void reloadChunks(int x,int y,int z)
	{
		if(z>this.lastChunkCenterZ){
			for(int cx=this.lastChunkCenterX, osc=0,val=-1;cx<=this.lastChunkCenterX+PLAYER_VIEW_FIELD;cx+=osc,osc=(osc+val)*-1,val=-val){
				for(int cy=World.HEIGHT-1;cy>=0;cy--){
					this.chunkGenerator.generateChunk(cx, cy, z+DIFTABLE[Math.abs(cx-this.lastChunkCenterX)]);
					this.removeChunk(cx, cy, this.lastChunkCenterZ-DIFTABLE[Math.abs(cx-this.lastChunkCenterX)]);
				}
			}
			this.lastChunkCenterZ=z;
		}else if(z<this.lastChunkCenterZ){
			for(int cx=this.lastChunkCenterX, osc=0,val=-1;cx<=this.lastChunkCenterX+PLAYER_VIEW_FIELD;cx+=osc,osc=(osc+val)*-1,val=-val){
				for(int cy=World.HEIGHT-1;cy>=0;cy--){
					this.chunkGenerator.generateChunk(cx, cy, z-DIFTABLE[Math.abs(cx-this.lastChunkCenterX)]);
					this.removeChunk(cx, cy, this.lastChunkCenterZ+DIFTABLE[Math.abs(cx-this.lastChunkCenterX)]);
				}
			}
			this.lastChunkCenterZ=z;
		}
		
		if(x>this.lastChunkCenterX){
			for(int cz=this.lastChunkCenterZ, osc=0,val=-1;cz<=this.lastChunkCenterZ+PLAYER_VIEW_FIELD;cz+=osc,osc=(osc+val)*-1,val=-val){
				for(int cy=World.HEIGHT-1;cy>=0;cy--){
					this.chunkGenerator.generateChunk(x+DIFTABLE[Math.abs(cz-this.lastChunkCenterZ)], cy, cz);
					this.removeChunk(this.lastChunkCenterX-DIFTABLE[Math.abs(cz-this.lastChunkCenterZ)], cy, cz);
				}
			}
			this.lastChunkCenterX=x;
		}else if(x<this.lastChunkCenterX){
			for(int cz=this.lastChunkCenterZ, osc=0,val=-1;cz<=this.lastChunkCenterZ+PLAYER_VIEW_FIELD;cz+=osc,osc=(osc+val)*-1,val=-val){
				for(int cy=World.HEIGHT-1;cy>=0;cy--){
					this.chunkGenerator.generateChunk(x-DIFTABLE[Math.abs(cz-this.lastChunkCenterZ)], cy, cz);
					this.removeChunk(this.lastChunkCenterX+DIFTABLE[Math.abs(cz-this.lastChunkCenterZ)], cy, cz);
				}
			}
			this.lastChunkCenterX=x;
		}
	}
	
	/**
	 * Gets world corners in world coordinates. The rendering is centered on the player, so they are constant
	 */
	public SquareCorners getWorldCornersHigh()
	{
		return this.worldCornersHigh;
	}
	public SquareCorners getWorldCornersLow()
	{
		return this.worldCornersLow;
	}
	
	/**
	 * Creates a table containing the width (in x coordinates) of each z coordinate (Being the center PLAYER_VIEW_FIELD+1) of the chunk render circle around the player.
	 * Used as a blueprint which informs to the places in which new chunks needs to be generated, or removed.
	 */
	private static int[] createDiftable(int viewfield)
	{
		int[] diftable=new int[viewfield+1];
		for(int v=0;v<=viewfield;v++)
		{
			boolean ended=false;
			for(int d=0;d<=viewfield;d++)
			{
				if(Math.sqrt(d*d + v*v)>viewfield)
				{
					diftable[v]=d-1;
					ended=true;
					break;
				}
			}
			//It should be equal to viewfield, but doing that a alone chunk is generated in one row. Subbing one we delete this alone chunk and the result is more aesthetic.
			if(!ended) diftable[v]=viewfield-1;
		}
		return diftable;
	}
	
	/**
	 * Performs all deleting operations over the world. This function works as some sort of destructor method, although it doesn't exist in java.
	 * Safelly ends all the threads, and saves all the chunks and settings in disk.
	 */
	@Override
	public void fullClean() 
	{
		//Saves settings
		this.settings.PLAYER_X=this.EM.getPlayer().getX();
		this.settings.PLAYER_Y=this.EM.getPlayer().getY();
		this.settings.PLAYER_Z=this.EM.getPlayer().getZ();
		this.settings.CAM_PITCH=this.cam.getPitch();
		this.settings.CAM_YAW=this.cam.getYaw();
		this.settings.DAY_TIME=this.currentTime;
		this.settings.DAY_SPEED=this.currentDaySpeed;
		this.settings.CUBE_SELECTED=this.EM.getPlayer().getCubeSelected();
		
		FloatBufferPool.fullClean(); //Free all possible locks to avoid an unlikely closing block
		this.chunkUpdater.fullClean(true);
		this.EM.fullClean();
		System.out.println("Saving setttings to disk...");
		this.fileManager.storeSettingsInFile(settings);
		System.out.println("Saving chunks to disk...");
		this.myChunks.fullClean();
		
		this.chunkGenerator.fullClean(true);
		this.chunkStorer.fullClean(true);
		
		this.MG=null;
		glDeleteVertexArrays(this.vao);
	}
	
	/**
	 * Used for the world to check if the player wants the time of the day to go faster or slower.
	 */
	@Override
	public void notifyKeyToggle(int code) 
	{
		switch(code)
		{
		case InputHandler.P_VALUE:
			if(this.currentDaySpeed<DAY_TIME_SPEED.length-1)
			{
				this.currentDaySpeed++;
			}
			GlobalTextManager.insertText("New time speed: 1 hour = "+DAY_TIME_SPEED[this.currentDaySpeed]+" seg");
			break;
		case InputHandler.O_VALUE:
			if(this.currentDaySpeed>0)
			{
				this.currentDaySpeed--;
			}
			GlobalTextManager.insertText("New time speed: 1 hour = "+DAY_TIME_SPEED[this.currentDaySpeed]+" seg");
			break;
		
		}
	}
}

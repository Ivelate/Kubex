package monecruft.gui;

import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glDepthFunc;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniformMatrix4;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;

import java.nio.FloatBuffer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import ivengine.properties.Cleanable;
import ivengine.shaders.SimpleShaderProgram;
import ivengine.view.Camera;
import ivengine.view.MatrixHelper;
import monecruft.MonecruftGame;
import monecruft.entity.EntityManager;
import monecruft.entity.Player;
import monecruft.shaders.DepthVoxelShaderProgram;
import monecruft.shaders.UnderwaterVoxelShaderProgram;
import monecruft.shaders.VoxelShaderProgram;
import monecruft.storage.ChunkStorage;
import monecruft.storage.FloatBufferPool;
import monecruft.utils.BoundaryChecker;
import monecruft.utils.SquareCorners;
import monecruft.utils.Vector3d;
import monecruft.utils.VoxelUtils;
import monecruftProperties.DrawableUpdatable;

public class World implements DrawableUpdatable, Cleanable
{
	private static final int MAX_CHUNK_LOADS_PER_TICK=10;
	public static final int PLAYER_VIEW_FIELD=9;
	public static final int HEIGHT=4;
	private static final float WATER_ALPHA=0.5f;
	private static final float CHUNK_UPDATE_TICK=0.3f;
	private static final int[] DIFTABLE=createDiftable(PLAYER_VIEW_FIELD+1);	
	
	private int vao;
	private EntityManager EM;
	private MapHandler MG;
	private ChunkStorage myChunks;
	private ChunkGenerator chunkGenerator;
	private ChunkUpdater chunkUpdater;
	private List<Chunk> updateChunks=new LinkedList<Chunk>();
	private LinkedList<Chunk> updateChunksAdd=new LinkedList<Chunk>();
	private Camera cam;
	private Camera sunCamera;
	private ShadowsManager shadowsManager;
	private Matrix4f customPVMatrix=null;
	private WorldFacade worldFacade;
	private Sky sky;
	
	private final SquareCorners worldCornersLow;
	private final SquareCorners worldCornersHigh;
	private SquareCorners currentWorldCornersLow;
	private SquareCorners currentWorldCornersHigh;
	private Vector3d cameraCenterVector=new Vector3d();
	
	private float currentTime=9;
	private float chunkUpdateTickCont=0;
	
	private VoxelShaderProgram VSP;
	private VoxelShaderProgram UVSP;
	private VoxelShaderProgram CustomVSP=null;
	
	private int lastChunkCenterX;private int lastChunkCenterZ;
	
	private LinkedList<Chunk> chunkAddList=new LinkedList<Chunk>();
	
	public World(VoxelShaderProgram VSP,VoxelShaderProgram UVSP,Camera cam,Camera sunCamera,ShadowsManager shadowsManager,Sky sky)
	{
		this.shadowsManager=shadowsManager;
		this.sunCamera=sunCamera;
		this.sky=sky;
		this.VSP=VSP;
		this.UVSP=UVSP;
		//Create player
		Player p=new Player(0,270.7f,0,cam);
		
		float maxworldsize=(float)(Chunk.CHUNK_DIMENSION*(World.PLAYER_VIEW_FIELD+1.5f));
		this.worldCornersLow=new SquareCorners(	new Vector4f(-maxworldsize,0,-maxworldsize,1),
												new Vector4f(maxworldsize,0,-maxworldsize,1),
												new Vector4f(-maxworldsize,0,maxworldsize,1),
												new Vector4f(maxworldsize,0,maxworldsize,1));
		this.worldCornersHigh=new SquareCorners(new Vector4f(-maxworldsize,World.HEIGHT*Chunk.CHUNK_DIMENSION,-maxworldsize,1),
												new Vector4f(maxworldsize,World.HEIGHT*Chunk.CHUNK_DIMENSION,-maxworldsize,1),
												new Vector4f(-maxworldsize,World.HEIGHT*Chunk.CHUNK_DIMENSION,maxworldsize,1),
												new Vector4f(maxworldsize,World.HEIGHT*Chunk.CHUNK_DIMENSION,maxworldsize,1));
		
		this.setWorldCenter(p.getX(),p.getY(), p.getZ());
		this.worldFacade=new WorldFacade(this);
		this.EM=new EntityManager(p,worldFacade);
		this.cam=cam;
		this.MG=new MapHandler(0,128,worldFacade);
		this.myChunks=new ChunkStorage((PLAYER_VIEW_FIELD*2)+1);
		this.chunkGenerator=new ChunkGenerator(worldFacade);
		this.chunkGenerator.start();
		this.chunkUpdater=new ChunkUpdater(p);
		this.chunkUpdater.start();
		
		//this.chunkGenerator.generateChunk(5000000/Chunk.CHUNK_DIMENSION,0,0);
		//this.chunkGenerator.generateChunk(0, 3, 0);
		for(int x=0, osc=1,val=1;x<=PLAYER_VIEW_FIELD;x+=osc,osc=(osc+val)*-1,val=-val)
		{
			for(int z=-DIFTABLE[Math.abs(x)];z<=DIFTABLE[Math.abs(x)];z++)
			{
				for(int y=0;y<HEIGHT;y++)
				{
					this.chunkGenerator.generateChunk(x+(int)Math.floor(p.getX()/Chunk.CHUNK_DIMENSION),y,z+(int)Math.floor(p.getZ()/Chunk.CHUNK_DIMENSION));
				}
			}
		}
		this.lastChunkCenterX=(int)Math.floor(p.getX()/Chunk.CHUNK_DIMENSION);
		this.lastChunkCenterZ=(int)Math.floor(p.getZ()/Chunk.CHUNK_DIMENSION);
		//this.chunkGenerator.generateChunk(0, 0, 0);
		//this.chunkGenerator.generateChunk(1, 0, 0);
		//this.vao=glGenVertexArrays();
		//glBindVertexArray(this.vao);
		//glBindVertexArray(0);
		this.sky.setCurrentTime(this.currentTime);
	}
	private void setupShaderParameters()
	{
		this.getActiveShader().enable();
		//if(InputHandler.isSHIFTPressed())newv.rotate((float)Math.PI, new Vector3f(0,1,0));
		MatrixHelper.uploadMatrix(getActivePVMatrix(), this.getActiveShader().getViewProjectionMatrixLoc());
		
		//glBindVertexArray(this.vao);
		
		//int alphaUniformLocation=glGetUniformLocation(this.getActiveShader().getID(),"alpha");
		
		if(this.getActiveShader().supportShadows()){
			int shadowTexLocation=glGetUniformLocation(this.getActiveShader().getID(),"shadowMap");
			int sunNormalLocation=glGetUniformLocation(this.getActiveShader().getID(),"sunNormal");
			int splitDistancesLocation=glGetUniformLocation(this.getActiveShader().getID(),"splitDistances");
			int shadowMatrixesLocation=glGetUniformLocation(this.getActiveShader().getID(),"shadowMatrixes");
			Vector3f sunNormal=this.sky.getSunNormal();
			GL20.glUniform3f(sunNormalLocation, sunNormal.x, sunNormal.y, sunNormal.z);
			GL20.glUniform1i(shadowTexLocation, MonecruftGame.SHADOW_TEXTURE_LOCATION);

			float[] dsplits=this.shadowsManager.getSplitDistances();
			switch(this.shadowsManager.getNumberSplits())
			{
			case 1: GL20.glUniform4f(splitDistancesLocation, dsplits[1],0,0,0);
				break;
			case 2: GL20.glUniform4f(splitDistancesLocation, dsplits[1],dsplits[2],0,0);
				break;
			case 3: GL20.glUniform4f(splitDistancesLocation, dsplits[1],dsplits[2],dsplits[3],0);
				break;
			case 4: GL20.glUniform4f(splitDistancesLocation, dsplits[1],dsplits[2],dsplits[3],dsplits[4]);
				break;
			}

			for(int i=0;i<this.shadowsManager.getNumberSplits();i++){
				MatrixHelper.uploadMatrix(this.shadowsManager.getOrthoProjectionForSplitScreenAdjusted(i), shadowMatrixesLocation+(i));
			}
		}
		
		if(this.getActiveShader().isParticipatingMedia())
		{
			int currentLightLoc=glGetUniformLocation(this.getActiveShader().getID(),"currentLight");
			GL20.glUniform1f(currentLightLoc, this.EM.getPlayer().getAverageLightExposed(this.worldFacade));
		}
		
		if(this.getActiveShader().supportLighting()){
			int daylightUniformLocation=glGetUniformLocation(this.getActiveShader().getID(),"daylightAmount");
			GL20.glUniform1f(daylightUniformLocation, this.getDaylightAmount());
		}
	}
	@Override
	public void draw()
	{
		this.draw(null);
	}
	public void draw(BoundaryChecker bc)
	{
		setupShaderParameters();
		
		float normLight=((this.getDaylightAmount()-0.15f)*1.17647f);
		glClearColor(0.2f*normLight, 0.4f*normLight, 0.75f*normLight, 0f);
		
		GL11.glDisable( GL11.GL_BLEND );
		glEnable(GL11.GL_DEPTH_TEST);
		
		if(bc==null||bc.applyCulling())GL11.glEnable(GL11.GL_CULL_FACE);
		else GL11.glDisable(GL11.GL_CULL_FACE);
		
		this.myChunks.initIter();
		Chunk c;
		while((c=this.myChunks.next())!=null) {
				c.draw(cam,this.getActiveShader(),bc);
				if(c.lightCalculatedFlag){
					c.lightCalculatedFlag=false;
					Chunk neighbour=this.myChunks.getChunk(c.getX()+1, c.getY(), c.getZ());
					if(neighbour!=null) neighbour.notifyNeighbourAdded(Chunk.Direction.XM);
						neighbour=this.myChunks.getChunk(c.getX()-1, c.getY(), c.getZ());
					if(neighbour!=null) neighbour.notifyNeighbourAdded(Chunk.Direction.XP);
						neighbour=this.myChunks.getChunk(c.getX(), c.getY()+1, c.getZ());
					if(neighbour!=null) neighbour.notifyNeighbourAdded(Chunk.Direction.YM);
						neighbour=this.myChunks.getChunk(c.getX(), c.getY()-1, c.getZ());
					if(neighbour!=null) neighbour.notifyNeighbourAdded(Chunk.Direction.YP);
						neighbour=this.myChunks.getChunk(c.getX(), c.getY(), c.getZ()+1);
					if(neighbour!=null) neighbour.notifyNeighbourAdded(Chunk.Direction.ZM);
						neighbour=this.myChunks.getChunk(c.getX(), c.getY(), c.getZ()-1);
					if(neighbour!=null) neighbour.notifyNeighbourAdded(Chunk.Direction.ZP);
				}
			}
		//glBindVertexArray(0);
	}
	public void drawLiquids()
	{
		setupShaderParameters();
		
		//U AINT GONNA DRAW SHIT
		/*GL11.glColorMask(false, false, false, false);
		glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDisable( GL11.GL_BLEND );
		glDepthFunc(GL11.GL_LEQUAL);
		GL11.glDisable(GL11.GL_CULL_FACE);
		this.myChunks.initIter();
		Chunk c;
		while((c=this.myChunks.next())!=null) {
				c.drawLiquids(cam,this.getActiveShader());
		}*/
				
		//NOW YISS
		//GL11.glColorMask(true, true, true, true);
		//GL20.glUniform1f(alphaUniformLocation, WATER_ALPHA);
		//glEnable( GL11.GL_BLEND ); 
		//GL11.glDepthFunc(GL11.GL_EQUAL);
		GL11.glDisable(GL11.GL_CULL_FACE);
		
		this.myChunks.initIter();
		Chunk c;
		while((c=this.myChunks.next())!=null) {
			c.drawLiquids(cam,this.getActiveShader());
		}
		GL11.glEnable(GL11.GL_CULL_FACE);
		//GL11.glDisable(GL11.GL_BLEND);
		
		
		if(this.getActiveShader().isParticipatingMedia()){
			float light=this.EM.getPlayer().getAverageLightExposed(this.worldFacade);
			this.sky.drawBackgroundColor(0.375f*light, 0.75f*light, 1.0f*light);
		}
	}
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
					//this.chunkGenerator.notifyAll();
				}
				this.myChunks.addChunk(ca.getX(), ca.getY(), ca.getZ(), ca);
			}
			ca.initChunk();
			//ca.createLightMap();
		}
		if(notifyLater) synchronized(this.chunkGenerator){ this.chunkGenerator.notifyAll();}
	}
	@Override
	public void update(float tEl)
	{
		this.EM.update(tEl);
		this.currentTime+=(tEl);
		if(this.currentTime>24) this.currentTime=0;
		this.setWorldCenter(this.EM.getPlayer().getX(), /*World.HEIGHT*Chunk.CHUNK_DIMENSION*/this.EM.getPlayer().getY(), this.EM.getPlayer().getZ());
		
		this.chunkUpdateTickCont+=tEl;
		
		synchronized(this.updateChunks)
		{
			synchronized(this.updateChunksAdd)
			{
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
			
			while(this.chunkUpdateTickCont>CHUNK_UPDATE_TICK)
			{
				this.chunkUpdateTickCont-=CHUNK_UPDATE_TICK;
				
				Iterator<Chunk> it=this.updateChunks.iterator();
				while(it.hasNext())
				{
					Chunk c=it.next();
					if(!c.updateChunkCubes(tEl)) it.remove();
				}
			}
		}
	}
	public VoxelShaderProgram getActiveShader()
	{
		return this.CustomVSP==null?this.EM.getPlayer().isUnderwater(this.worldFacade)?this.UVSP:this.VSP : this.CustomVSP;
	}
	public Matrix4f getActivePVMatrix()
	{
		return this.customPVMatrix==null?this.cam.getProjectionViewMatrix() : this.customPVMatrix;
	}
	public void overrideCurrentShader(VoxelShaderProgram customShader)
	{
		this.CustomVSP=customShader;
	}
	public void overrideCurrentPVMatrix(Matrix4f pvmat)
	{
		this.customPVMatrix=pvmat;
	}
	public void setWorldCenter(double xpos,double ypos,double zpos)
	{
		this.currentWorldCornersLow=SquareCorners.add(this.worldCornersLow,0,0,0);
		this.currentWorldCornersHigh=SquareCorners.add(this.worldCornersHigh,0,0,0);
		//this.currentWorldCornersLow=this.worldCornersLow;
		//this.currentWorldCornersHigh=this.worldCornersHigh;
		this.sunCamera.moveTo(0,0,0);
	}
	public Vector3d getCameraCenter()
	{
		return this.cameraCenterVector;
	}
	public void updateCameraCenter(double x,double y,double z) 
	{
		this.cameraCenterVector.x=x;this.cameraCenterVector.y=y;this.cameraCenterVector.z=z;
	}
	public byte getContent(double x,double y,double z)
	{
		Chunk c;
		if((c=this.myChunks.getChunk(x/Chunk.CHUNK_DIMENSION, y/Chunk.CHUNK_DIMENSION, z/Chunk.CHUNK_DIMENSION))!=null){
			byte toRet= c.getCubeAt(VoxelUtils.trueMod(x,Chunk.CHUNK_DIMENSION),VoxelUtils.trueMod(y,Chunk.CHUNK_DIMENSION),VoxelUtils.trueMod(z,Chunk.CHUNK_DIMENSION));
			return toRet;
		}
		return 0;
	}
	public float getContentArtificialLight(float x,float y,float z)
	{
		Chunk c;
		if((c=this.myChunks.getChunk(x/Chunk.CHUNK_DIMENSION, y/Chunk.CHUNK_DIMENSION, z/Chunk.CHUNK_DIMENSION))!=null){
			float toRet= c.getArtificialBrightnessAt(VoxelUtils.trueMod(x,Chunk.CHUNK_DIMENSION),VoxelUtils.trueMod(y,Chunk.CHUNK_DIMENSION),VoxelUtils.trueMod(z,Chunk.CHUNK_DIMENSION));
			return toRet;
		}
		return -1;
	}
	public float getContentNaturalLight(float x,float y,float z)
	{
		Chunk c;
		if((c=this.myChunks.getChunk(x/Chunk.CHUNK_DIMENSION, y/Chunk.CHUNK_DIMENSION, z/Chunk.CHUNK_DIMENSION))!=null){
			float toRet= c.getNaturalBrightnessAt(VoxelUtils.trueMod(x,Chunk.CHUNK_DIMENSION),VoxelUtils.trueMod(y,Chunk.CHUNK_DIMENSION),VoxelUtils.trueMod(z,Chunk.CHUNK_DIMENSION));
			return toRet;
		}
		return -1;
	}
	public float getDaylightAmount()
	{
		//int daytime=(int)(System.currentTimeMillis()%40000);
		//if(daytime>20000) daytime=40000-daytime;
		//float daytime=(float)(System.currentTimeMillis() &0x00FFFFFF);
		//return (float)(daytime/(float)(20000));
		float altsin=(float)((Math.sin(this.sky.getSolarAltitude()+0.3f)));
		if(altsin<0) altsin=0;
		float ret=0.17f+altsin;
		if(ret>1) return 1f;
		return ret;
		//return 1;
	}
	public float getGravity()
	{
		return 15f;
	}
	public float getAirFriction()
	{
		return 0.7f;
	}
	public MapHandler getMapGenerator()
	{
		return this.MG;
	}
	public ChunkUpdater getChunkUpdater()
	{
		return this.chunkUpdater;
	}
	public ChunkStorage getChunkStorage()
	{
		return this.myChunks;
	}
	public LinkedList<Chunk> getAddList()
	{
		return this.chunkAddList;
	}
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
		synchronized(this.updateChunks)
		{
			Iterator<Chunk> it=this.updateChunks.iterator();
			while(it.hasNext())
			{
				Chunk itc=it.next();
				if(itc.getX()==x&&itc.getY()==y&&itc.getZ()==z) {itc.fullClean();it.remove(); break;}
			}
		}
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
	public void reloadChunks(int x,int y,int z)
	{
		if(z>this.lastChunkCenterZ){
			for(int cx=this.lastChunkCenterX, osc=0,val=-1;cx<=this.lastChunkCenterX+PLAYER_VIEW_FIELD;cx+=osc,osc=(osc+val)*-1,val=-val){
				for(int cy=0;cy<World.HEIGHT;cy++){
					this.chunkGenerator.generateChunk(cx, cy, z+DIFTABLE[Math.abs(cx-this.lastChunkCenterX)]);
					this.removeChunk(cx, cy, this.lastChunkCenterZ-DIFTABLE[Math.abs(cx-this.lastChunkCenterX)]);
				}
			}
			this.lastChunkCenterZ=z;
		}else if(z<this.lastChunkCenterZ){
			for(int cx=this.lastChunkCenterX, osc=0,val=-1;cx<=this.lastChunkCenterX+PLAYER_VIEW_FIELD;cx+=osc,osc=(osc+val)*-1,val=-val){
				for(int cy=0;cy<World.HEIGHT;cy++){
					this.chunkGenerator.generateChunk(cx, cy, z-DIFTABLE[Math.abs(cx-this.lastChunkCenterX)]);
					this.removeChunk(cx, cy, this.lastChunkCenterZ+DIFTABLE[Math.abs(cx-this.lastChunkCenterX)]);
				}
			}
			this.lastChunkCenterZ=z;
		}
		
		if(x>this.lastChunkCenterX){
			for(int cz=this.lastChunkCenterZ, osc=0,val=-1;cz<=this.lastChunkCenterZ+PLAYER_VIEW_FIELD;cz+=osc,osc=(osc+val)*-1,val=-val){
				for(int cy=0;cy<World.HEIGHT;cy++){
					this.chunkGenerator.generateChunk(x+DIFTABLE[Math.abs(cz-this.lastChunkCenterZ)], cy, cz);
					this.removeChunk(this.lastChunkCenterX-DIFTABLE[Math.abs(cz-this.lastChunkCenterZ)], cy, cz);
				}
			}
			this.lastChunkCenterX=x;
		}else if(x<this.lastChunkCenterX){
			for(int cz=this.lastChunkCenterZ, osc=0,val=-1;cz<=this.lastChunkCenterZ+PLAYER_VIEW_FIELD;cz+=osc,osc=(osc+val)*-1,val=-val){
				for(int cy=0;cy<World.HEIGHT;cy++){
					this.chunkGenerator.generateChunk(x-DIFTABLE[Math.abs(cz-this.lastChunkCenterZ)], cy, cz);
					this.removeChunk(this.lastChunkCenterX+DIFTABLE[Math.abs(cz-this.lastChunkCenterZ)], cy, cz);
				}
			}
			this.lastChunkCenterX=x;
		}
	}
	public SquareCorners getWorldCornersHigh()
	{
		return this.currentWorldCornersHigh;
	}
	public SquareCorners getWorldCornersLow()
	{
		return this.currentWorldCornersLow;
	}
	private static int[] createDiftable(int viewfield)
	{
		int[] diftable=new int[viewfield+1];
		for(int v=0;v<=viewfield;v++)
		{
			boolean ended=false;
			for(int d=0;d<=viewfield;d++)
			{
				//System.out.println("DTB "+v+" "+d);
				if(Math.sqrt(d*d + v*v)>viewfield)
				{
					//System.out.println("END");
					diftable[v]=d-1;
					ended=true;
					break;
				}
			}
			//Deberia ser viewfield pero asi se elimina el chunk aislado
			if(!ended) diftable[v]=viewfield-1;
		}
		return diftable;
	}
	@Override
	public void fullClean() {
		//Prevents game to hang during shutdown if some error happens.
		Thread overShutdown=new Thread(){
			@Override 
			public void run(){try{Thread.sleep(3000);System.err.println("Over shutdown used (!!!)");System.exit(1);} catch(Exception e){}}
		};
		overShutdown.start();
		this.chunkGenerator.fullClean(true);
		this.chunkUpdater.fullClean(true);
		this.MG=null;
		this.EM.fullClean();
		this.myChunks.fullClean();
		glDeleteVertexArrays(this.vao);
		overShutdown.interrupt();
	}
}

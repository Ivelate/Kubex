package monecruft;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_COMPONENT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_RGB;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13.GL_TEXTURE2;
import static org.lwjgl.opengl.GL13.GL_TEXTURE3;
import static org.lwjgl.opengl.GL13.GL_TEXTURE4;
import static org.lwjgl.opengl.GL13.GL_TEXTURE5;
import static org.lwjgl.opengl.GL13.GL_TEXTURE6;
import static org.lwjgl.opengl.GL13.GL_TEXTURE7;
import static org.lwjgl.opengl.GL13.GL_TEXTURE8;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glBufferSubData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT1;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT2;
import static org.lwjgl.opengl.GL30.GL_COMPARE_REF_TO_TEXTURE;
import static org.lwjgl.opengl.GL30.GL_DEPTH_ATTACHMENT;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_RENDERBUFFER;
import static org.lwjgl.opengl.GL30.GL_RGB32F;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glBindRenderbuffer;
import static org.lwjgl.opengl.GL30.glFramebufferRenderbuffer;
import static org.lwjgl.opengl.GL30.glGenFramebuffers;
import static org.lwjgl.opengl.GL30.glGenRenderbuffers;
import static org.lwjgl.opengl.GL30.glRenderbufferStorage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Scanner;

import monecruft.gui.Chunk;
import monecruft.gui.DepthPeelingLiquidRenderer;
import monecruft.gui.FinalDrawManager;
import monecruft.gui.GlobalTextManager;
import monecruft.gui.Hud;
import monecruft.gui.LiquidRenderer;
import monecruft.gui.ShadowsManager;
import monecruft.gui.Sky;
import monecruft.gui.World;
import monecruft.menu.MonecruftMenu;
import monecruft.shaders.BasicColorShaderProgram;
import monecruft.shaders.DeferredNoReflectionsShaderProgram;
import monecruft.shaders.DeferredReflectionsShaderProgram;
import monecruft.shaders.DepthVoxelShaderProgram;
import monecruft.shaders.DeferredShaderProgram;
import monecruft.shaders.DeferredTerrainShaderProgram;
import monecruft.shaders.DeferredTerrainUnshadowShaderProgram;
import monecruft.shaders.DeferredUnderwaterFinalShaderProgram;
import monecruft.shaders.DeferredUnderwaterTerrainShaderProgram;
import monecruft.shaders.HudShaderProgram;
import monecruft.shaders.SkyShaderProgram;
import monecruft.shaders.TerrainVoxelShaderProgram;
import monecruft.shaders.UnderwaterVoxelShaderProgram;
import monecruft.shaders.UnshadowedVoxelShaderProgram;
import monecruft.shaders.VoxelShaderProgram;
import monecruft.storage.ByteArrayPool;
import monecruft.storage.FileManager;
import monecruft.storage.FloatBufferPool;
import monecruft.utils.InputHandler;
import monecruft.utils.TimeManager;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.vector.Matrix4f;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureImpl;
import org.newdawn.slick.opengl.TextureLoader;
import org.newdawn.slick.util.ResourceLoader;

import ivengine.IvEngine;
import ivengine.Util;
import ivengine.properties.Cleanable;
import ivengine.view.Camera;
import ivengine.view.CameraInverseProjEnvelope;
import ivengine.view.MatrixHelper;

public class MonecruftGame implements Cleanable
{
	public static final int[] TEXTURE_FETCH={GL_TEXTURE0,GL_TEXTURE1,GL_TEXTURE2,GL_TEXTURE3,GL_TEXTURE4,GL_TEXTURE5,GL_TEXTURE6,GL_TEXTURE7,GL_TEXTURE8};
	public static final int TILES_TEXTURE_LOCATION=0;
	public static final int CURRENT_LIQUID_NORMAL_TEXTURE_LOCATION=8;
	public static final int BASEFBO_NORMALS_BRIGHTNESS_TEXTURE_LOCATION=1;
	public static final int BASEFBO_COLOR_TEXTURE_LOCATION=2;
	public static final int BASEFBO_DEPTH_TEXTURE_LOCATION=3;
	public static final int SHADOW_TEXTURE_LOCATION=4; //Lookup from World
	public static final int LIQUIDLAYERS_TEXTURE_LOCATION=5;
	public static final int DEFERREDFBO_COLOR_TEXTURE_LOCATION=6;
	public static final int WATER_NORMAL_TEXTURE_LOCATION=7;
	
	private MonecruftSettings settings;
	
	private int X_RES=1400;
	private int Y_RES=900;
	private int SHADOW_XRES=2048;
	private int SHADOW_YRES=2048;
	private final int SHADOW_LAYERS=4;
	private final int LIQUID_LAYERS=3;
	
	private final float CAMERA_NEAR=0.1f;
	private final float CAMERA_FAR;
	
	private final float[] SHADOW_SPLITS;
	
	private TerrainVoxelShaderProgram VSP;
	private UnderwaterVoxelShaderProgram UVSP;
	private HudShaderProgram HSP;
	private SkyShaderProgram SSP;
	private BasicColorShaderProgram BCSP;
	private DepthVoxelShaderProgram DVSP;
	private DeferredShaderProgram DTSP;
	private DeferredShaderProgram DUTSP;
	private DeferredShaderProgram DUFSP;
	private DeferredShaderProgram DRSP;
	private TimeManager TM;
	private Camera cam; private CameraInverseProjEnvelope camInvProjEnv;
	private Camera sunCam;
	private World world;
	private Hud hud;
	private Sky sky;
	private GlobalTextManager textManager;
	private ShadowsManager shadowsManager;
	private LiquidRenderer liquidRenderer;
	private FinalDrawManager finalDrawManager;
	private FileManager fileManager;
	
	private int tilesTexture;
	private Texture nightDomeTexture;
	
	//TEXTURE HIERARCHY:
	//0 - Tiles / Sky / Text
	//2 - Base Fbo Color
	//3 - Base Fbo Depth
	//4 - Shadows (2D Array)
	private int baseFboDepthTexture;
	private int positionTexture;
	private int normalAndLightTexture;
	private int sunShadowTexture;
	private int colorTexture;
	
	private boolean hasFocus=true;
	private Thread shutdownHook;
	
	private int baseFbo;
	private int deferredFbo;
	private int[] shadowFbos;
	
	public MonecruftGame(MonecruftSettings settings) throws LWJGLException, IOException 
	{
		/*File outpfil=new File("outpfil.log");
		outpfil.createNewFile();
		System.setOut(new PrintStream(new FileOutputStream(outpfil)));*/
		this.settings=settings;
		
		if(settings.FULLSCREEN_ENABLED){
			DisplayMode dm=Display.getDesktopDisplayMode();
			X_RES=dm.getWidth();
			Y_RES=dm.getHeight();
			IvEngine.configDisplay(dm, "Monecruft", true, false, true);
		}
		else {
			this.X_RES=settings.WINDOW_XRES;
			this.Y_RES=settings.WINDOW_YRES;
			IvEngine.configDisplay(X_RES, Y_RES, "Monecruft", true, false, false);
		}
		
		//System.out.println(GL11.glGetString(GL11.GL_VERSION)); Get version, if wanted
		
		Thread.currentThread().setPriority(Thread.currentThread().getPriority()+1); //Faster than the others -> game experience > loading
		
		//Start all resources
		CAMERA_FAR=(float)Math.sqrt(World.HEIGHT*World.HEIGHT + settings.RENDER_DISTANCE*settings.RENDER_DISTANCE)*Chunk.CHUNK_DIMENSION;
		float[] ssplits=ShadowsManager.calculateSplits(1f, CAMERA_FAR, SHADOW_LAYERS, 0.3f);
		ssplits[0]=CAMERA_NEAR;
		ssplits[1]=ssplits[1]/4;
		ssplits[2]=ssplits[2]/2.5f;
		
		SHADOW_SPLITS=ssplits;
		for(Float f:ssplits)System.out.println(f);
		initResources();
		
		//Main loop
		boolean running=true;
		Mouse.setGrabbed(true);
		shutdownHook=new Thread() {
			   @Override
			   public void run() {
			    closeApp();
			   }
			};
		Runtime.getRuntime().addShutdownHook(shutdownHook);
		TM.getDeltaTime();
		try
		{
			while (running && !Display.isCloseRequested()) {
				Display.isActive();
				while(Keyboard.next()){
					handleKeyboardInput();
				}
				while(Mouse.next()){
					handleMouseInput();
				}
				int wheel=Mouse.getDWheel();
				if(wheel!=0) {
					wheel=wheel>0?1:-1;
					InputHandler.addWheel(wheel);
				}
				if(InputHandler.isESCPressed()) {
					running=false;
					InputHandler.setESC(false);
				}
				update(TM.getDeltaTime());
				render();
				if(Display.isActive()&&!hasFocus)
				{
					hasFocus=true;
					Mouse.setGrabbed(true);
				}
				else if(!Display.isActive()&&hasFocus)
				{
					hasFocus=false;
					Mouse.setGrabbed(false);
				}
				// Flip the buffers and sync to 60 FPS
				Display.update();
				Display.sync(60);
			
			}
		} catch (Exception e){
			e.printStackTrace();
		}
		
		closeApp();
	}
	private void update(float delta)
	{
		TM.updateFPS();
		if(delta>0.2)delta=0.2f;
		Display.setTitle("FPS: "+TM.getFPS());
		//Update cam pos
		this.world.update(delta);
		
		this.sky.update(delta);
		
		this.textManager.update(delta);
		//this.sunCam.setPitch((float)(Math.PI/4));
	}
	private void render()
	{
		if(this.settings.SHADOWS_ENABLED)
		{
			this.shadowsManager.calculateCascadeShadows(this.sunCam.getViewMatrix(), this.world.getWorldCornersLow(), this.world.getWorldCornersHigh());
			this.world.overrideCurrentShader(this.DVSP);

			for(int i=0;i<this.shadowsManager.getNumberSplits();i++)
			{
				glBindFramebuffer(GL_FRAMEBUFFER, this.shadowFbos[i]); 
				glClear(GL11.GL_DEPTH_BUFFER_BIT); 
				GL11.glViewport(0,0,SHADOW_XRES,SHADOW_YRES);
				//glClearColor(0.6f, 0.8f, 1.0f, 0f);
				//tilesTexture.bind();
				//this.sunCam.updateProjection(this.shadowsManager.getOrthoProjectionForSplit(1));*/
				this.world.overrideCurrentPVMatrix(this.shadowsManager.getOrthoProjectionForSplit(i));
				this.world.draw(this.shadowsManager.getBoundaryCheckerForSplit(i));
				//this.world.drawLiquids();
			}
		}

		glBindTexture(GL30.GL_TEXTURE_2D_ARRAY,this.tilesTexture);
		glBindFramebuffer(GL_FRAMEBUFFER, this.baseFbo);
		glClear(GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		GL11.glViewport(0,0,X_RES,Y_RES);
		
		//glClearColor(0.6f, 0.8f, 1.0f, 0f);
		
		//tilesTexture.bind();
		this.world.overrideCurrentShader(null);
		this.world.overrideCurrentPVMatrix(null);
		//this.world.overrideCurrentPVMatrix(this.shadowsManager.getOrthoProjectionForSplit(3));
		//this.world.overrideCurrentPVMatrix(this.shadowsManager.getOrthoProjectionForSplit(2));
		this.world.draw(null);

		this.liquidRenderer.renderLayers(this.world, X_RES, Y_RES);
		
		glBindFramebuffer(GL_FRAMEBUFFER, this.baseFbo);
		
		nightDomeTexture.bind();
		
		//this.sky.draw();
		
		//tilesTexture.bind();
		//this.world.drawLiquids();
		this.world.afterDrawTasks();

		//this.VSP.disable();
		
		glDisable(GL_DEPTH_TEST);
		int[] fbos={this.deferredFbo,0};
		DeferredShaderProgram[] programs={this.DTSP,this.DRSP};
		if(this.world.isUnderwater()) {
			programs=new DeferredShaderProgram[]{this.DUTSP,this.DUFSP};
			fbos=new int[]{this.deferredFbo,0};
		}
		//if(Math.random()>0.5f) programs[0]=this.DRSP;
		//glClear(GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		Matrix4f temp=Matrix4f.invert(this.cam.getProjectionViewMatrix(), null);
		this.finalDrawManager.draw(programs,fbos,temp,this.cam.getViewMatrix(),this.cam.getProjectionMatrix(),X_RES,Y_RES);
		/*this.FDSP.enable();
		glBindBuffer(GL15.GL_ARRAY_BUFFER,this.squarevbo);
		this.FDSP.setupAttributes();*/
		
		//GL20.glUniform4f(this.FDSP.invProjZ,this.camInvProjEnv.getInvProjMatrix().m03,this.camInvProjEnv.getInvProjMatrix().m13,this.camInvProjEnv.getInvProjMatrix().m23,this.camInvProjEnv.getInvProjMatrix().m33);
		/*glUniform1i(this.FDSP.positionTex,2);
		glUniform1i(this.FDSP.normalAndLightTex,3);
		glUniform1i(this.FDSP.shadowMap,4);*/
		/*glDrawArrays(GL_TRIANGLES, 0, Sky.NUM_COMPONENTS);
		this.FDSP.disable();*/
		
		this.HSP.enable();
		//glDisable(GL11.GL_CULL_FACE);
		//glDisable( GL11.GL_BLEND );
		this.hud.draw();
		this.HSP.disable();
		TextureImpl.bindNone();
		
		this.textManager.draw(X_RES,Y_RES);
	}
	private void initResources() throws IOException
	{
		FloatBufferPool.init(Chunk.CHUNK_DIMENSION*Chunk.CHUNK_DIMENSION*Chunk.CHUNK_DIMENSION*2*6*6,20);
		ByteArrayPool.init(Chunk.CHUNK_DIMENSION,(settings.RENDER_DISTANCE*2 +1)*World.HEIGHT*(settings.RENDER_DISTANCE*2 +1)*2);
		glEnable(GL11.GL_CULL_FACE);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		glEnable( GL11.GL_BLEND );
		glClearColor(0.6f, 0.8f, 1.0f, 0f);
		
		//Set-up shader program
		GL20.glUseProgram(0);
		textManager=new GlobalTextManager();
		this.UVSP=new UnderwaterVoxelShaderProgram(true);
		this.DVSP=new DepthVoxelShaderProgram(true);
		this.VSP=/*this.settings.SHADOWS_ENABLED?*/new TerrainVoxelShaderProgram(true)/*:new UnshadowedVoxelShaderProgram(true)*/;
		this.HSP=new HudShaderProgram(true);
		this.SSP=new SkyShaderProgram(true);
		this.BCSP=new BasicColorShaderProgram(true);
		this.DTSP=this.settings.SHADOWS_ENABLED?new DeferredTerrainShaderProgram(true):new DeferredTerrainUnshadowShaderProgram(true);
		this.DRSP=this.settings.REFLECTIONS_ENABLED?new DeferredReflectionsShaderProgram(true):new DeferredNoReflectionsShaderProgram(true);
		this.DUTSP=new DeferredUnderwaterTerrainShaderProgram(true);
		this.DUFSP=new DeferredUnderwaterFinalShaderProgram(true);
		this.TM=new TimeManager();
		this.cam=new Camera(CAMERA_NEAR,CAMERA_FAR,80f,(float)(X_RES*3/4)/Y_RES); //FOV more width than height by design
		this.camInvProjEnv=new CameraInverseProjEnvelope(this.cam);
		this.shadowsManager=new ShadowsManager(SHADOW_SPLITS,this.cam);
		this.liquidRenderer=new DepthPeelingLiquidRenderer(LIQUID_LAYERS);

		this.sunCam=/*new Camera(0.5f,1000,80f,X_RES/Y_RES);//*/new Camera(new Matrix4f());//World.HEIGHT*Chunk.CHUNK_DIMENSION, 0));
		this.sunCam.moveTo(0, 5, 0);
		this.sunCam.setPitch(0);
		
		this.sky=new Sky(cam,this.sunCam);
		File mapRoute=new File(this.settings.MAP_ROUTE);
		mapRoute.mkdir();
		this.fileManager=new FileManager(mapRoute,settings.RENDER_DISTANCE);
		this.fileManager.getSettingsFromFile(settings);
		this.world=new World(this.VSP,this.UVSP,this.cam,this.sunCam,this.shadowsManager,this.sky,fileManager,this.settings);
		this.finalDrawManager=new FinalDrawManager(this.world,this.sky,this.shadowsManager,this.liquidRenderer,this.camInvProjEnv.getInvProjMatrix(),CAMERA_NEAR,CAMERA_FAR);
		this.hud=new Hud(this.HSP,X_RES,Y_RES);
		//Load textures here
		
		glActiveTexture(TEXTURE_FETCH[TILES_TEXTURE_LOCATION]);
		/*System.out.println(ResourceLoader.getResource("/images/tiles"));
		File tilesFolder=new File(ResourceLoader.getResource("/images/tiles").getFile());
		File[] tileFiles=tilesFolder.listFiles();
		Arrays.sort(tileFiles, new Comparator<File>(){
			@Override
			public int compare(File arg0, File arg1) {
				String name0=arg0.getName().substring(0, 3);
				String name1=arg1.getName().substring(0, 3);
				return name0.compareTo(name1);
			}	
		});*/
		
		//GL_NEAREST for that blocky look
		tilesTexture = Util.loadTextureAtlasIntoTextureArray(FileLoader.loadTileImages(), GL11.GL_NEAREST, GL11.GL_NEAREST_MIPMAP_LINEAR, true,settings.ANISOTROPIC_FILTERING_ENABLED);//TextureLoader.getTexture("PNG", ResourceLoader.getResourceAsStream("images/imagenes2.png"));
		
		Util.loadPNGTexture(FileLoader.loadWaterNormalImage(), TEXTURE_FETCH[WATER_NORMAL_TEXTURE_LOCATION]);
		
		glActiveTexture(GL_TEXTURE0);
		// Setup the ST coordinate system
		/*GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);*/
		
		// Setup what to do when the texture has to be scaled
		/*GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, 
				GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, 
				GL11.GL_NEAREST_MIPMAP_LINEAR);
		GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);*/
		
		nightDomeTexture=TextureLoader.getTexture("JPG", ResourceLoader.getResourceAsStream("images/nightdome.jpg"));
		//nightDomeTexture=TextureLoader.getTexture("PMG", ResourceLoader.getResourceAsStream("images/tiles4.PNG"));
		
		//Util.loadPNGTexture("images/monecruft_tiles.png", GL13.GL_TEXTURE0);
		//glUniform1i(this.VSP.getTilesTextureLocation(), 0);
		
		//Text things
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glShadeModel(GL11.GL_SMOOTH);        
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_LIGHTING);                    
        GL11.glClearDepth(1);                                       
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glViewport(0,0,X_RES,Y_RES);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();
		GL11.glOrtho(0, X_RES, Y_RES, 0, 1, -1);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);

		//FBO THINGS: START
		this.baseFbo=glGenFramebuffers();
		glBindFramebuffer(GL_FRAMEBUFFER, this.baseFbo);
		
		int colorTexture=glGenTextures();
		int brightnessNormalsTexture=glGenTextures();
		//int normalTexture=glGenTextures();
		
		glActiveTexture(TEXTURE_FETCH[BASEFBO_COLOR_TEXTURE_LOCATION]);
		glBindTexture(GL_TEXTURE_2D, colorTexture);
		glTexImage2D(GL_TEXTURE_2D, 0,GL_RGB, X_RES, Y_RES, 0,GL_RGB, GL_UNSIGNED_BYTE, (FloatBuffer)null);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		GL30.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorTexture, 0);
		
		glActiveTexture(TEXTURE_FETCH[BASEFBO_NORMALS_BRIGHTNESS_TEXTURE_LOCATION]);
		glBindTexture(GL_TEXTURE_2D, brightnessNormalsTexture);
		glTexImage2D(GL_TEXTURE_2D, 0,GL11.GL_RGBA, X_RES, Y_RES, 0,GL11.GL_RGBA, GL_UNSIGNED_BYTE, (FloatBuffer)null);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		GL30.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT1, GL_TEXTURE_2D, brightnessNormalsTexture, 0);
		
		/*glActiveTexture(TEXTURE_FETCH[BASEFBO_BRIGHTNESS_TEXTURE_LOCATION]);
		glBindTexture(GL_TEXTURE_2D, normalTexture);
		glTexImage2D(GL_TEXTURE_2D, 0,GL_RGB, X_RES, Y_RES, 0,GL_RGB , GL_UNSIGNED_BYTE, (FloatBuffer)null); //Normals are pre-set (6 values max)
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		GL30.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT2, GL_TEXTURE_2D, brightnessTexture, 0);*/
		
		//FBO DEPTH IS A TEXTURE
		int baseFboDepth=glGenTextures();

		glActiveTexture(TEXTURE_FETCH[BASEFBO_DEPTH_TEXTURE_LOCATION]);
		glBindTexture(GL_TEXTURE_2D, baseFboDepth);System.out.println("ERR"+GL11.glGetError());
		glTexImage2D(GL_TEXTURE_2D, 0,GL14.GL_DEPTH_COMPONENT24, X_RES, Y_RES, 0,GL_DEPTH_COMPONENT, GL_UNSIGNED_INT, (FloatBuffer)null); //|TODO ASFA
		System.out.println("ERR"+GL11.glGetError());
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		GL30.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, baseFboDepth, 0);
		/*int depthbuf=glGenRenderbuffers();
		glBindRenderbuffer(GL_RENDERBUFFER, depthbuf);
		glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, X_RES, Y_RES);
		glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthbuf);*/
		
		IntBuffer drawBuffers = BufferUtils.createIntBuffer(2);
		
		drawBuffers.put(GL_COLOR_ATTACHMENT0);
		drawBuffers.put(GL_COLOR_ATTACHMENT1);
		/*drawBuffers.put(GL_COLOR_ATTACHMENT2);*/
		
		drawBuffers.flip();
		GL20.glDrawBuffers(drawBuffers);
		
		this.deferredFbo=glGenFramebuffers();
		glBindFramebuffer(GL_FRAMEBUFFER, this.deferredFbo);
		
		int deferredColorTex=glGenTextures();
		//int normalTexture=glGenTextures();
		
		glActiveTexture(TEXTURE_FETCH[DEFERREDFBO_COLOR_TEXTURE_LOCATION]);
		glBindTexture(GL_TEXTURE_2D, deferredColorTex);
		glTexImage2D(GL_TEXTURE_2D, 0,GL11.GL_RGBA, X_RES, Y_RES, 0,GL11.GL_RGBA, GL_UNSIGNED_BYTE, (FloatBuffer)null);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		GL30.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, deferredColorTex, 0);
		
		drawBuffers = BufferUtils.createIntBuffer(1);
		
		drawBuffers.put(GL_COLOR_ATTACHMENT0);
		
		drawBuffers.flip();
		GL20.glDrawBuffers(drawBuffers);
		
		//SUN FBO: START
		if(this.settings.SHADOWS_ENABLED)
		{
			 
			int shadowTexture=glGenTextures();


			glActiveTexture(TEXTURE_FETCH[SHADOW_TEXTURE_LOCATION]);

			glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, shadowTexture);System.out.println("ERR"+GL11.glGetError());
			GL12.glTexImage3D(GL30.GL_TEXTURE_2D_ARRAY, 0,GL14.GL_DEPTH_COMPONENT16, SHADOW_XRES, SHADOW_YRES,this.shadowsManager.getNumberSplits(), 0,GL_DEPTH_COMPONENT, GL_UNSIGNED_INT, (FloatBuffer)null); //|TODO ASFA
			System.out.println("ERR"+GL11.glGetError());
			glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
			glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
			glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL14.GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);
			
			this.shadowFbos=new int[this.shadowsManager.getNumberSplits()];
			for(int i=0;i<this.shadowsManager.getNumberSplits();i++)
			{
				this.shadowFbos[i]=glGenFramebuffers();
				glBindFramebuffer(GL_FRAMEBUFFER, this.shadowFbos[i]);
				GL30.glFramebufferTextureLayer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, shadowTexture, 0, i);
		
				drawBuffers = BufferUtils.createIntBuffer(0);

				GL20.glDrawBuffers(drawBuffers);
				
			}
		}

		//LIQUID LAYERS DEPTH GENERATION
		int liquidLayers=glGenTextures();

		glActiveTexture(TEXTURE_FETCH[LIQUIDLAYERS_TEXTURE_LOCATION]);
		glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, liquidLayers);System.out.println("ERR"+GL11.glGetError());
		GL12.glTexImage3D(GL30.GL_TEXTURE_2D_ARRAY, 0,GL14.GL_DEPTH_COMPONENT24, X_RES, Y_RES,this.liquidRenderer.getNumLayers(), 0,GL_DEPTH_COMPONENT, GL_UNSIGNED_INT, (FloatBuffer)null); //|TODO ASFA
		System.out.println("ERR"+GL11.glGetError());
		glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		
		int currentLiquidNormalTex=glGenTextures();

		glActiveTexture(MonecruftGame.TEXTURE_FETCH[MonecruftGame.CURRENT_LIQUID_NORMAL_TEXTURE_LOCATION]);
		glBindTexture(GL_TEXTURE_2D, currentLiquidNormalTex);
		glTexImage2D(GL_TEXTURE_2D, 0,GL11.GL_RGB, X_RES, Y_RES, 0,GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, (FloatBuffer)null);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		
		this.liquidRenderer.initResources(liquidLayers,currentLiquidNormalTex);
		
		//Reset active
		glActiveTexture(GL13.GL_TEXTURE0);
		glBindFramebuffer(GL_FRAMEBUFFER, 0);
	}
	private void handleKeyboardInput()
	{
		switch(Keyboard.getEventKey())
		{
		case Keyboard.KEY_W:
			InputHandler.setW(Keyboard.getEventKeyState());
			break;
		case Keyboard.KEY_A:
			InputHandler.setA(Keyboard.getEventKeyState());
			break;
		case Keyboard.KEY_S:
			InputHandler.setS(Keyboard.getEventKeyState());
			break;
		case Keyboard.KEY_D:
			InputHandler.setD(Keyboard.getEventKeyState());
			break;
		case Keyboard.KEY_SPACE:
			InputHandler.setSPACE(Keyboard.getEventKeyState());
			break;
		case Keyboard.KEY_LSHIFT:
			InputHandler.setSHIFT(Keyboard.getEventKeyState());
			break;
		case Keyboard.KEY_ESCAPE:
			InputHandler.setESC(Keyboard.getEventKeyState());
			break;
		case Keyboard.KEY_0:
			InputHandler.setNum(Keyboard.getEventKeyState(),0);
			break;
		case Keyboard.KEY_1:
			InputHandler.setNum(Keyboard.getEventKeyState(),1);
			break;
		case Keyboard.KEY_2:
			InputHandler.setNum(Keyboard.getEventKeyState(),2);
			break;
		case Keyboard.KEY_3:
			InputHandler.setNum(Keyboard.getEventKeyState(),3);
			break;
		case Keyboard.KEY_4:
			InputHandler.setNum(Keyboard.getEventKeyState(),4);
			break;
		case Keyboard.KEY_5:
			InputHandler.setNum(Keyboard.getEventKeyState(),5);
			break;
		case Keyboard.KEY_6:
			InputHandler.setNum(Keyboard.getEventKeyState(),6);
			break;
		case Keyboard.KEY_7:
			InputHandler.setNum(Keyboard.getEventKeyState(),7);
			break;
		case Keyboard.KEY_8:
			InputHandler.setNum(Keyboard.getEventKeyState(),8);
			break;
		case Keyboard.KEY_9:
			InputHandler.setNum(Keyboard.getEventKeyState(),9);
			break;
		case Keyboard.KEY_LCONTROL:
			InputHandler.setCtrl(Keyboard.getEventKeyState());
			break;
		case Keyboard.KEY_E:
			InputHandler.setE(Keyboard.getEventKeyState());
			break;	
		case Keyboard.KEY_P:
			InputHandler.setP(Keyboard.getEventKeyState());
			break;	
		case Keyboard.KEY_O:
			InputHandler.setO(Keyboard.getEventKeyState());
			break;	
		}
	}
	private void handleMouseInput()
	{
		switch(Mouse.getEventButton())
		{
		case 0:
			InputHandler.setMouseButton1(Mouse.isButtonDown(0));
			break;
		case 1:
			InputHandler.setMouseButton2(Mouse.isButtonDown(1));
			break;
		}
	}
	public static void main(String args[]) throws LWJGLException, IOException
	{
		/*boolean fullscreen=false;
		boolean noshadows=false;
		int mapcode=0;
		String maproute="default_kubex_map";
		long seed=1234567890;
		boolean selectingMap=false;
		boolean selectingSeed=false;
		boolean selectingMapRoute=false;
		boolean noreflections=false;
		for(String s:args){
			if(selectingMap){
				selectingMap=false;
				mapcode=Integer.parseInt(s);
			}
			else if(selectingMapRoute){
				selectingMapRoute=false;
				maproute=s;
			}
			else if(selectingSeed){
				selectingSeed=false;
				seed=Long.parseLong(s);
			}
			else if(s.equals("-fullscreen")) fullscreen=true;
			else if(s.equals("-noshadows")) noshadows=true;
			else if(s.equals("-map")) selectingMap=true;
			else if(s.equals("-noreflections")) noreflections=true;
			else if(s.equals("-maproute")) selectingMapRoute=true;
			else if(s.equals("-seed")) selectingSeed=true;
		}*/
		MonecruftSettings settings=new MonecruftSettings();
		/*settings.FULLSCREEN_ENABLED=fullscreen;
		settings.SHADOWS_ENABLED=!noshadows;
		settings.MAP_CODE=mapcode;
		settings.REFLECTIONS_ENABLED=!noreflections;
		settings.MAP_ROUTE=maproute;
		settings.MAP_SEED=seed;*/
		File defaultConfigFile=new File("kubex_conf.txt");
		loadDefaultConfigFile(settings,defaultConfigFile);
		
		File mapRoute=new File("kubex_maps");
		mapRoute.mkdir();
		MonecruftMenu menu=new MonecruftMenu(settings,mapRoute);
		
		
		if(menu.waitForClose()) {
			storeDefaultConfigFile(settings,defaultConfigFile);
			new MonecruftGame(settings);
		}
	}
	private static void loadDefaultConfigFile(MonecruftSettings settings,File f)
	{
		if(f.exists())
		{
			try {
				Scanner s=new Scanner(f);
				while(s.hasNextLine())
				{
					String line=s.nextLine();
					String[] content=line.split(":");
					if(content[0].equals("MAP_SEED")){
						settings.MAP_SEED=Long.parseLong(content[1]);
					}
					else if(content[0].equals("MAP_CODE")){
						settings.MAP_CODE=Integer.parseInt(content[1]);
					}
					else if(content[0].equals("MAP_ROUTE")){
						settings.MAP_ROUTE=content[1];
					}
					else if(content[0].equals("SHADOWS_ENABLED")){
						settings.SHADOWS_ENABLED=Boolean.parseBoolean(content[1]);
					}
					else if(content[0].equals("REFLECTIONS_ENABLED")){
						settings.REFLECTIONS_ENABLED=Boolean.parseBoolean(content[1]);
					}
					else if(content[0].equals("FULLSCREEN_ENABLED")){
						settings.FULLSCREEN_ENABLED=Boolean.parseBoolean(content[1]);
					}
					else if(content[0].equals("WINDOW_XRES")){
						settings.WINDOW_XRES=Integer.parseInt(content[1]);
					}
					else if(content[0].equals("WINDOW_YRES")){
						settings.WINDOW_YRES=Integer.parseInt(content[1]);
					}
					else if(content[0].equals("RENDER_DISTANCE")){
						settings.RENDER_DISTANCE=Integer.parseInt(content[1]);
					}
					else if(content[0].equals("ANISOTROPIC_FILTERING_ENABLED")){
						settings.ANISOTROPIC_FILTERING_ENABLED=Boolean.parseBoolean(content[1]);
					}
				}
				s.close();
			} 
			catch (FileNotFoundException e) {
				//Shouldn't happen, but no problem. Default config loaded.
			}
		}
	}
	private static void storeDefaultConfigFile(MonecruftSettings settings,File settingsFile)
	{
		try 
		{
			if(!settingsFile.exists()) settingsFile.createNewFile();
			PrintWriter f=new PrintWriter(settingsFile,"ISO-8859-1");
			f.println("MAP_SEED:"+settings.MAP_SEED);
			f.println("MAP_CODE:"+settings.MAP_CODE);
			f.println("MAP_ROUTE:"+settings.MAP_ROUTE);
			f.println("SHADOWS_ENABLED:"+settings.SHADOWS_ENABLED);
			f.println("REFLECTIONS_ENABLED:"+settings.REFLECTIONS_ENABLED);
			f.println("FULLSCREEN_ENABLED:"+settings.FULLSCREEN_ENABLED);
			f.println("WINDOW_XRES:"+settings.WINDOW_XRES);
			f.println("WINDOW_YRES:"+settings.WINDOW_YRES);
			f.println("RENDER_DISTANCE:"+settings.RENDER_DISTANCE);
			f.println("ANISOTROPIC_FILTERING_ENABLED:"+settings.ANISOTROPIC_FILTERING_ENABLED);
			f.close();
		} 
		catch (IOException e) {
			System.err.println("Error storing custom configs in file. Default configs will be loaded instead next time you open the game.");
		}
	}
	private void closeApp()
	{
		Runtime.getRuntime().removeShutdownHook(shutdownHook);
		this.fullClean();
		// Dispose any resources and destroy our window
		Display.destroy();
	}
	@Override
	public void fullClean() {
		world.fullClean();
		this.fileManager.fullClean();
		VSP.fullClean();
		this.TM=null;
		this.cam=null;
	}
}

package kubex;

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

import java.io.ByteArrayOutputStream;
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
import java.util.LinkedList;
import java.util.Scanner;

import javax.swing.JOptionPane;

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
import org.lwjgl.opengl.GLContext;
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
import kubex.gui.Chunk;
import kubex.gui.DepthPeelingLiquidRenderer;
import kubex.gui.FinalDrawManager;
import kubex.gui.GlobalTextManager;
import kubex.gui.Hud;
import kubex.gui.LiquidRenderer;
import kubex.gui.ShadowsManager;
import kubex.gui.Sky;
import kubex.gui.World;
import kubex.menu.MonecruftMenu;
import kubex.shaders.DeferredNoReflectionsShaderProgram;
import kubex.shaders.DeferredReflectionsShaderProgram;
import kubex.shaders.DeferredShaderProgram;
import kubex.shaders.DeferredTerrainShaderProgram;
import kubex.shaders.DeferredTerrainUnshadowShaderProgram;
import kubex.shaders.DeferredUnderwaterFinalShaderProgram;
import kubex.shaders.DeferredUnderwaterTerrainShaderProgram;
import kubex.shaders.DeferredUnderwaterUnshadowTerrainShaderProgram;
import kubex.shaders.DepthVoxelShaderProgram;
import kubex.shaders.HudShaderProgram;
import kubex.shaders.TerrainVoxelShaderProgram;
import kubex.shaders.VoxelShaderProgram;
import kubex.storage.ByteArrayPool;
import kubex.storage.FileManager;
import kubex.storage.FloatBufferPool;
import kubex.utils.InputHandler;
import kubex.utils.KubexException;
import kubex.utils.KubexGPUException;
import kubex.utils.KubexIOException;
import kubex.utils.TimeManager;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Main class. Manages all OpenGL low level work, like texture switching, rendering passes, shaders, etc.
 * Contains both the main render loop and the main update loop. Inits the OpenGL main window.
 */
public class KubexGame implements Cleanable
{
	public static final int[] TEXTURE_FETCH={GL_TEXTURE0,GL_TEXTURE1,GL_TEXTURE2,GL_TEXTURE3,GL_TEXTURE4,GL_TEXTURE5,GL_TEXTURE6,GL_TEXTURE7,GL_TEXTURE8};
	public static final int TILES_TEXTURE_LOCATION=0;						//Cube textures will be located on GL_TEXTURE0
	public static final int CURRENT_LIQUID_NORMAL_TEXTURE_LOCATION=8;		//The first water layer normal texture will be located on GL_TEXTURE8
	public static final int BASEFBO_NORMALS_BRIGHTNESS_TEXTURE_LOCATION=1;	//The texture which contains both the normal (Compressed) and the lighting of the first deferred rendering pass
																			//will be located on GL_TEXTURE1
	public static final int BASEFBO_COLOR_TEXTURE_LOCATION=2;				//The texture which contains the color of the first deferred rendering pass will be located on GL_TEXTURE2
	public static final int BASEFBO_DEPTH_TEXTURE_LOCATION=3;				//The texture which contains the depth of the first deferred rendering pass will be located on GL_TEXTURE3
	public static final int SHADOW_TEXTURE_LOCATION=4;						//The texture array which contains the shadow maps for each shadow cascade will be located on GL_TEXTURE4
	public static final int LIQUIDLAYERS_TEXTURE_LOCATION=5;				//The texture array which contains the liquid layers depth (For each liquid depth) will be located on GL_TEXTURE5
	public static final int DEFERREDFBO_COLOR_TEXTURE_LOCATION=6;			//The texture which contains the final color from the second deferred rendering pass will be located on GL_TEXTURE6
	public static final int WATER_NORMAL_TEXTURE_LOCATION=7; 				//The texture which contains the normal water perturbation texture LOADED FROM DISK will be located on GL_TEXTURE7
	
	private KubexSettings settings;
	
	private int X_RES=1400; //Window width. Uses settings width
	private int Y_RES=900; //Window height. Uses settings width
	private int SHADOW_XRES=2048; //Default shadow resolution for each shadow map. Per default, 2048x2048
	private int SHADOW_YRES=2048;
	private final int SHADOW_LAYERS=4; //Number of shadow layers used. The value is forced to 4
	private int LIQUID_LAYERS; //The number of water layers being depth sorted. Uses the settings value
	
	private final float CAMERA_NEAR=0.1f; //The camera near will be placed to 0.1m from the camera
	private final float CAMERA_FAR; //The camera far varies in function of the rendering distance specified in settings
	
	private final float[] SHADOW_SPLITS; //Split distances of the frustrum, used for cascaded shadow mapping
	
	//Shaders
	private TerrainVoxelShaderProgram VSP;
	private HudShaderProgram HSP;
	private DepthVoxelShaderProgram DVSP;
	private DeferredShaderProgram DTSP;
	private DeferredShaderProgram DUTSP;
	private DeferredShaderProgram DUFSP;
	private DeferredShaderProgram DRSP;
	
	private TimeManager TM; //Manages the time elapsed
	private Camera cam; private CameraInverseProjEnvelope camInvProjEnv; //Default camera
	private Camera sunCam; //Sun camera
	
	//Each one of this classes are very important and its recommended to look at its description
	private World world; 
	private Hud hud;
	private Sky sky;
	private GlobalTextManager textManager;
	private ShadowsManager shadowsManager;
	private LiquidRenderer liquidRenderer;
	private FinalDrawManager finalDrawManager;
	private FileManager fileManager;
	
	private int tilesTexture;
	private Texture nightDomeTexture; //The nightdome texture will be uploaded separately, using the Slick-Utils library. It will be switched with the tiles texture, so binding one or another
									  //depending on the rendering needs is neccesary
	
	private boolean hasFocus=true;	//If the screen has focus
	private Thread shutdownHook;	//If the program is closed via a way other than ESC, this thread will execute before app is closed
	
	private int baseFbo; //Default FrameBufferObject. Used for the default first pass draw
	private int deferredFbo; //Second pass deferred rendering fbo
	private int[] shadowFbos; //Each shadow map will have one separate fbo
	//Water layers fbos are not stored here. Third pass deferred rendering fbo will be the default fbo (0), which renders to screen
	
	public KubexGame(KubexSettings settings) throws LWJGLException, KubexException
	{
		this.settings=settings;
		LIQUID_LAYERS=this.settings.WATER_LAYERS;
		
		if(settings.FULLSCREEN_ENABLED){ //If fullscreen is enabled, sets it on on the desktop resolution and adapts X_RES and Y_RES
			DisplayMode dm=Display.getDesktopDisplayMode();
			X_RES=dm.getWidth();
			Y_RES=dm.getHeight();
			IvEngine.configDisplay(dm, "Monecruft", true, false, true);
		}
		else { //Windowed mode
			this.X_RES=settings.WINDOW_XRES;
			this.Y_RES=settings.WINDOW_YRES;
			IvEngine.configDisplay(X_RES, Y_RES, "Monecruft", true, false, false);
		}
		
		//"GPU not supported" error handling
		if(!GLContext.getCapabilities().OpenGL32) throw new KubexGPUException("Your GPU doesn't support OpenGL 3.2");
		else if(GL11.glGetInteger(GL20.GL_MAX_TEXTURE_IMAGE_UNITS)<8) throw new KubexGPUException("Your GPU doesn't support 8 texture units, and Kubex needs them to run correctly");
		
		Thread.currentThread().setPriority(Thread.currentThread().getPriority()+1); //Faster than the others -> game experience > loading
		
		//Sets the camera far distance in function of the maximum render distance (Considering height is fixed as 8x32 m)
		CAMERA_FAR=(float)Math.sqrt(World.HEIGHT*World.HEIGHT + settings.RENDER_DISTANCE*settings.RENDER_DISTANCE)*Chunk.CHUNK_DIMENSION;
		float[] ssplits=ShadowsManager.calculateSplits(1f, CAMERA_FAR, SHADOW_LAYERS, 0.3f);
		ssplits[0]=CAMERA_NEAR;
		ssplits[1]=ssplits[1]/4;
		ssplits[2]=ssplits[2]/2.5f;
		ssplits[3]=ssplits[3]/1.5f;
		
		SHADOW_SPLITS=ssplits;

		try{
			initResources(); //Inits the textures, shaders, objects, etc
		}
		catch(IOException e){throw new KubexIOException("Error reading/storing files in disk");}
		catch(KubexIOException e){throw e;}
		
		//Main loop
		boolean running=true;
		Mouse.setGrabbed(true); //Grabs the mouse
		
		shutdownHook=new Thread() { //If the app is closed unusually, safelly terminates it anyways (Saving chunks in disk)
			   @Override
			   public void run() {
			    closeApp();
			   }
			};
		Runtime.getRuntime().addShutdownHook(shutdownHook);
		
		TM.getDeltaTime();
		try
		{
			while (running && !Display.isCloseRequested()) { //Game loop
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
				else if(!Display.isActive()&&hasFocus) //If window loses focus, ungrab the mouse
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
	
	/**
	 * Update loop
	 */
	private void update(float delta)
	{
		TM.updateFPS();
		if(delta>0.2)delta=0.2f;
		Display.setTitle("FPS: "+TM.getFPS());
		//Update cam pos
		this.world.update(delta);
		
		this.sky.update(delta);
		
		this.textManager.update(delta);
	}
	
	/**
	 * Render loop
	 */
	private void render()
	{
		if(this.settings.SHADOWS_ENABLED) //If shadows are enabled, calculate each cascade shadow map
		{
			this.shadowsManager.calculateCascadeShadows(this.sunCam.getViewMatrix(), this.world.getWorldCornersLow(), this.world.getWorldCornersHigh());
			this.world.overrideCurrentShader(this.DVSP);

			for(int i=0;i<this.shadowsManager.getNumberSplits();i++)
			{
				glBindFramebuffer(GL_FRAMEBUFFER, this.shadowFbos[i]); 
				glClear(GL11.GL_DEPTH_BUFFER_BIT); 
				GL11.glViewport(0,0,SHADOW_XRES,SHADOW_YRES);
				this.world.overrideCurrentPVMatrix(this.shadowsManager.getOrthoProjectionForSplit(i));
				this.world.draw(this.shadowsManager.getBoundaryCheckerForSplit(i));
			}
		}

		glBindTexture(GL30.GL_TEXTURE_2D_ARRAY,this.tilesTexture);
		glBindFramebuffer(GL_FRAMEBUFFER, this.baseFbo);
		glClear(GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		GL11.glViewport(0,0,X_RES,Y_RES);
		this.world.overrideCurrentShader(null);
		this.world.overrideCurrentPVMatrix(null);
		this.world.draw(null); //Draws the world normally into the framebuffer basefbo, filling the color, normal, lighting and depth base textures

		this.liquidRenderer.renderLayers(this.world, X_RES, Y_RES); //Renders the liquid layers, one by one, into its respectives depth texutres
		
		glBindFramebuffer(GL_FRAMEBUFFER, this.baseFbo); //Binds the base fbo again for safeness (The liquid layers has binded their respective fbo each)
		
		nightDomeTexture.bind(); //Uses in GL_TEXTURE0 the night dome texture instead of the cubes textures from now on
		
		this.world.afterDrawTasks();
		
		glDisable(GL_DEPTH_TEST); //Depth test is no longer needed, we are drawing a texture
		glEnable(GL11.GL_CULL_FACE); //We only draw a quad looking to the screen, not its backface (Invisible)
		
		int[] fbos={this.deferredFbo,0}; //Only two passes of deferred rendering remaining, so we use for the second a fbo and for the third we render to the screen
		
		DeferredShaderProgram[] programs={this.DTSP,this.DRSP};
		if(this.world.isUnderwater()) { //Switching the shaders if we are underwater
			programs=new DeferredShaderProgram[]{this.DUTSP,this.DUFSP};
		}

		Matrix4f temp=Matrix4f.invert(this.cam.getProjectionViewMatrix(), null);
		this.finalDrawManager.draw(programs,fbos,temp,this.cam.getViewMatrix(),this.cam.getProjectionMatrix(),X_RES,Y_RES); //Draw the two remaining deferred passes

		this.HSP.enable();

		this.hud.draw(); //Draws the hud (Gray point on the center of the screen)
		this.HSP.disable();
		TextureImpl.bindNone();
		
		this.textManager.draw(X_RES,Y_RES); //Draws text, if needed
	}
	
	/**
	 * Inits the game resources (Images, pools, shaders, objects, etc.)
	 */
	private void initResources() throws IOException
	{
		//Inits static pools
		FloatBufferPool.init(Chunk.CHUNK_DIMENSION*Chunk.CHUNK_DIMENSION*Chunk.CHUNK_DIMENSION*2*6*6,20);
		ByteArrayPool.init(Chunk.CHUNK_DIMENSION,(settings.RENDER_DISTANCE*2 +1)*World.HEIGHT*(settings.RENDER_DISTANCE*2 +1)*2);
		glEnable(GL11.GL_CULL_FACE);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		glEnable( GL11.GL_BLEND );
		glClearColor(0.6f, 0.8f, 1.0f, 0f);
		
		//Inits shaders
		GL20.glUseProgram(0);
		textManager=new GlobalTextManager();
		this.DVSP=new DepthVoxelShaderProgram(true);
		this.VSP=new TerrainVoxelShaderProgram(true);
		this.HSP=new HudShaderProgram(true);
		this.DTSP=this.settings.SHADOWS_ENABLED?new DeferredTerrainShaderProgram(true):new DeferredTerrainUnshadowShaderProgram(true);
		this.DRSP=this.settings.REFLECTIONS_ENABLED?new DeferredReflectionsShaderProgram(true):new DeferredNoReflectionsShaderProgram(true);
		this.DUTSP=this.settings.SHADOWS_ENABLED?new DeferredUnderwaterTerrainShaderProgram(true):new DeferredUnderwaterUnshadowTerrainShaderProgram(true);
		this.DUFSP=new DeferredUnderwaterFinalShaderProgram(true);
		
		//Inits essential objects
		this.TM=new TimeManager();
		this.cam=new Camera(CAMERA_NEAR,CAMERA_FAR,80f,(float)(X_RES*3/4)/Y_RES); //FOV more width than height BY DESIGN, so blocks looks more "plane". Looks nicer that way, i think.
		this.camInvProjEnv=new CameraInverseProjEnvelope(this.cam);
		this.shadowsManager=new ShadowsManager(SHADOW_SPLITS,this.cam);
		this.liquidRenderer=new DepthPeelingLiquidRenderer(LIQUID_LAYERS);

		this.sunCam=new Camera(new Matrix4f());
		this.sunCam.moveTo(0, 5, 0);
		this.sunCam.setPitch(0);
		
		this.sky=new Sky(cam,this.sunCam);
		File mapRoute=new File(this.settings.MAP_ROUTE);
		mapRoute.mkdir(); //Creates the maps folder
		this.fileManager=new FileManager(mapRoute,settings.RENDER_DISTANCE);
		this.fileManager.getSettingsFromFile(settings); //Reads default settings from settings file.
		this.world=new World(this.VSP,this.cam,this.sunCam,this.shadowsManager,this.sky,fileManager,this.settings);
		this.finalDrawManager=new FinalDrawManager(this.world,this.sky,this.shadowsManager,this.liquidRenderer,this.camInvProjEnv.getInvProjMatrix(),CAMERA_NEAR,CAMERA_FAR);
		this.hud=new Hud(this.HSP,X_RES,Y_RES);
		
		//Load textures here
		glActiveTexture(TEXTURE_FETCH[TILES_TEXTURE_LOCATION]);
		
		//GL_NEAREST for that blocky look
		//loads the tiles textures into a array
		tilesTexture = Util.loadTextureAtlasIntoTextureArray(FileLoader.loadTileImages(), GL11.GL_NEAREST, GL11.GL_NEAREST_MIPMAP_LINEAR, true,settings.ANISOTROPIC_FILTERING_ENABLED);
		
		Util.loadPNGTexture(FileLoader.loadWaterNormalImage(), TEXTURE_FETCH[WATER_NORMAL_TEXTURE_LOCATION]); //loads the water normal texture
		
		glActiveTexture(GL_TEXTURE0);
		nightDomeTexture=TextureLoader.getTexture("JPG", ResourceLoader.getResourceAsStream("images/nightdome.jpg")); //loads the nightdome
		
		//Without this, text printing using Slick-Utils doesn't work. Seems it still uses some old mode OpenGL.
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

		//First pass deferred rendering
		this.baseFbo=glGenFramebuffers();
		glBindFramebuffer(GL_FRAMEBUFFER, this.baseFbo);
		
		int colorTexture=glGenTextures();
		int brightnessNormalsTexture=glGenTextures();
		
		//Creates and inits the base color texture as a RGB texture
		glActiveTexture(TEXTURE_FETCH[BASEFBO_COLOR_TEXTURE_LOCATION]);
		glBindTexture(GL_TEXTURE_2D, colorTexture);
		glTexImage2D(GL_TEXTURE_2D, 0,GL_RGB, X_RES, Y_RES, 0,GL_RGB, GL_UNSIGNED_BYTE, (FloatBuffer)null);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		GL30.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorTexture, 0);
		
		//Creates and inits the brightness and normals texture as a RGBA texture
		glActiveTexture(TEXTURE_FETCH[BASEFBO_NORMALS_BRIGHTNESS_TEXTURE_LOCATION]);
		glBindTexture(GL_TEXTURE_2D, brightnessNormalsTexture);
		glTexImage2D(GL_TEXTURE_2D, 0,GL11.GL_RGBA, X_RES, Y_RES, 0,GL11.GL_RGBA, GL_UNSIGNED_BYTE, (FloatBuffer)null);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		GL30.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT1, GL_TEXTURE_2D, brightnessNormalsTexture, 0);
			
		//The depth buffer of this FBO will be a texture, too. This will make depth sorting slower but we will be able to access depth values later.
		int baseFboDepth=glGenTextures();

		glActiveTexture(TEXTURE_FETCH[BASEFBO_DEPTH_TEXTURE_LOCATION]);
		glBindTexture(GL_TEXTURE_2D, baseFboDepth);
		glTexImage2D(GL_TEXTURE_2D, 0,GL14.GL_DEPTH_COMPONENT24, X_RES, Y_RES, 0,GL_DEPTH_COMPONENT, GL_UNSIGNED_INT, (FloatBuffer)null); 
		System.out.println("ERR"+GL11.glGetError());
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		GL30.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, baseFboDepth, 0); //Set the depth texture as the default render depth target
		
		IntBuffer drawBuffers = BufferUtils.createIntBuffer(2); //Drawing to 2 textures
		
		drawBuffers.put(GL_COLOR_ATTACHMENT0);
		drawBuffers.put(GL_COLOR_ATTACHMENT1);
		
		drawBuffers.flip();
		GL20.glDrawBuffers(drawBuffers);
		
		//Second pass deferred rendering
		this.deferredFbo=glGenFramebuffers();
		glBindFramebuffer(GL_FRAMEBUFFER, this.deferredFbo);
		
		int deferredColorTex=glGenTextures();

		//Only uses one texture, a RGBA color texture
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
		
		//If shadows are enabled, we init each shadow map texture, placed in an array
		if(this.settings.SHADOWS_ENABLED)
		{
			 
			int shadowTexture=glGenTextures();


			glActiveTexture(TEXTURE_FETCH[SHADOW_TEXTURE_LOCATION]);

			glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, shadowTexture);
			//Creates a texture array to place the shadows in
			GL12.glTexImage3D(GL30.GL_TEXTURE_2D_ARRAY, 0,GL14.GL_DEPTH_COMPONENT16, SHADOW_XRES, SHADOW_YRES,this.shadowsManager.getNumberSplits(), 0,GL_DEPTH_COMPONENT, GL_UNSIGNED_INT, (FloatBuffer)null);
			System.out.println("ERR"+GL11.glGetError());
			glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR); //Needed to do hardware PCF comparisons via shader
			glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR); //Needed to do hardware PCF comparisons via shader
			glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL14.GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE); //Needed to do hardware PCF comparisons via shader
			
			this.shadowFbos=new int[this.shadowsManager.getNumberSplits()];
			//Creates one framebuffer per shadow map
			for(int i=0;i<this.shadowsManager.getNumberSplits();i++)
			{
				this.shadowFbos[i]=glGenFramebuffers();
				glBindFramebuffer(GL_FRAMEBUFFER, this.shadowFbos[i]);
				GL30.glFramebufferTextureLayer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, shadowTexture, 0, i); //Each framebuffer will have one texture layer (one index of the texture array created before) assigned as render target
		
				drawBuffers = BufferUtils.createIntBuffer(0);

				GL20.glDrawBuffers(drawBuffers);
				
			}
		}

		//Liquid layers depth generation. Equal to the shadows depth textures generation.
		int liquidLayers=glGenTextures();

		glActiveTexture(TEXTURE_FETCH[LIQUIDLAYERS_TEXTURE_LOCATION]);
		glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, liquidLayers);
		GL12.glTexImage3D(GL30.GL_TEXTURE_2D_ARRAY, 0,GL14.GL_DEPTH_COMPONENT24, X_RES, Y_RES,this.liquidRenderer.getNumLayers(), 0,GL_DEPTH_COMPONENT, GL_UNSIGNED_INT, (FloatBuffer)null);

		glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST); //We will compare manually the depth in the shader, we will not perform PCF of any sort in this case
		glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		
		int currentLiquidNormalTex=glGenTextures();

		glActiveTexture(KubexGame.TEXTURE_FETCH[KubexGame.CURRENT_LIQUID_NORMAL_TEXTURE_LOCATION]);
		glBindTexture(GL_TEXTURE_2D, currentLiquidNormalTex);
		glTexImage2D(GL_TEXTURE_2D, 0,GL11.GL_RGB, X_RES, Y_RES, 0,GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, (FloatBuffer)null);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		
		this.liquidRenderer.initResources(liquidLayers,currentLiquidNormalTex);
		
		//Reset active texture and fbo to the default
		glActiveTexture(GL13.GL_TEXTURE0);
		glBindFramebuffer(GL_FRAMEBUFFER, 0);
	}
	
	/**
	 * Handles keyboard inputs, settings the appropiate values on the InputHandler
	 */
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
	
	/**
	 * Handles mouse inputs, setting the appropiate values on the input handler
	 */
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
	
	/************************************************************************************* MAIN METHOD **********************************************************************************/
	
	public static void main(String args[]) throws LWJGLException, IOException
	{
		KubexSettings settings=new KubexSettings(); //Inits a default settings file
		
		File defaultConfigFile=new File("kubex_conf.txt"); //Opens default settings file (contains only menu preferences)
		loadDefaultConfigFile(settings,defaultConfigFile);
		
		File mapRoute=new File("kubex_maps"); //Opens the maps folder (If it doesn't exists, creates it)
		mapRoute.mkdir();
		
		//Opens the main menu
		MonecruftMenu menu=new MonecruftMenu(settings,mapRoute);
		
		//Gets blocked in the menu until notified
		if(menu.waitForClose()) {
			try{
				storeDefaultConfigFile(settings,defaultConfigFile); //Stores the default menu configs in the file.
				new KubexGame(settings); //Inits the game
			}	
			//Error handling
			catch(KubexGPUException e){
				JOptionPane.showMessageDialog(null, "Your GPU can't run this game. Please update your drivers and try again\n\n"+e.getMessage(),"GPU Fatal error",JOptionPane.ERROR_MESSAGE);
			}
			catch(KubexIOException e){
				JOptionPane.showMessageDialog(null, "A disk reading/writing error has happened. Remember Kubex needs to write/read files in the same folder it's placed, so make sure you have admin rights in that folder.\n\n"+e.getMessage(),"Error reading data from disk",JOptionPane.ERROR_MESSAGE);
			}
			catch(Exception e){
				JOptionPane.showMessageDialog(null, "Fatal exception thrown. Kubex will exit now.\nPlease send me this log :)\n\n"+exceptionStacktraceToString(e),"Fatal Exception",JOptionPane.ERROR_MESSAGE);
			}
			catch(Error e){
				JOptionPane.showMessageDialog(null, "A fatal error happened. Kubex will exit now.\n\n"+e,"Fatal Error",JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
	/**
	 * Inserts a exception stack into a string
	 */
	private static String exceptionStacktraceToString(Exception e)
	{
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    PrintStream ps = new PrintStream(baos);
	    e.printStackTrace(ps);
	    ps.close();
	    return baos.toString();
	}
	
	/**
	 * Loads default file <f> settings into the settings object <settings>
	 */
	private static void loadDefaultConfigFile(KubexSettings settings,File f)
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
					else if(content[0].equals("WATER_LAYERS")){
						settings.WATER_LAYERS=Integer.parseInt(content[1]);
					}
					else if(content[0].equals("ANISOTROPIC_FILTERING_ENABLED")){
						settings.ANISOTROPIC_FILTERING_ENABLED=Boolean.parseBoolean(content[1]);
					}
					else if(content[0].equals("MOUSE_SENSITIVITY")){
						settings.MOUSE_SENSITIVITY=Float.parseFloat(content[1]);
					}
				}
				s.close();
			} 
			catch (FileNotFoundException e) {
				//Shouldn't happen, but no problem. Default config loaded.
			}
		}
	}
	
	/**
	 * Stores all default settings in <settings> (Only menu settings> into the file <settingsFile>
	 */
	private static void storeDefaultConfigFile(KubexSettings settings,File settingsFile)
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
			f.println("WATER_LAYERS:"+settings.WATER_LAYERS);
			f.println("MOUSE_SENSITIVITY:"+settings.MOUSE_SENSITIVITY);
			f.close();
		} 
		//If the file can't be created, there is some problem here. throws an exception
		catch (IOException e) {
			throw new KubexIOException("Error storing default config file in "+settingsFile.getAbsolutePath());
		}
	}
	
	/**
	 * Safelly closes the app, closing all open resources. Applies both for closeApp() and fullClean(), a submethod of it.
	 */
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

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
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.glBindBuffer;
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

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import monecruft.gui.Chunk;
import monecruft.gui.GlobalTextManager;
import monecruft.gui.Hud;
import monecruft.gui.ShadowsManager;
import monecruft.gui.Sky;
import monecruft.gui.World;
import monecruft.shaders.BasicColorShaderProgram;
import monecruft.shaders.DepthVoxelShaderProgram;
import monecruft.shaders.FinalDrawShaderProgram;
import monecruft.shaders.HudShaderProgram;
import monecruft.shaders.SkyShaderProgram;
import monecruft.shaders.UnderwaterVoxelShaderProgram;
import monecruft.shaders.UnshadowedVoxelShaderProgram;
import monecruft.shaders.VoxelShaderProgram;
import monecruft.storage.ByteArrayPool;
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
import ivengine.view.MatrixHelper;

public class MonecruftGame implements Cleanable
{
	public static final int SHADOW_TEXTURE_LOCATION=4; //Lookup from World
	
	private MonecruftSettings settings;
	
	private int X_RES=800;
	private int Y_RES=600;
	private int SHADOW_XRES=2048;
	private int SHADOW_YRES=2048;
	
	private final float CAMERA_NEAR=0.02f;
	private final float CAMERA_FAR=(float)Math.sqrt(World.HEIGHT*World.HEIGHT + World.PLAYER_VIEW_FIELD*World.PLAYER_VIEW_FIELD)*Chunk.CHUNK_DIMENSION;
	
	private final float[] SHADOW_SPLITS;
	
	private VoxelShaderProgram VSP;
	private UnderwaterVoxelShaderProgram UVSP;
	private HudShaderProgram HSP;
	private SkyShaderProgram SSP;
	private BasicColorShaderProgram BCSP;
	private DepthVoxelShaderProgram DVSP;
	private FinalDrawShaderProgram FDSP;
	private TimeManager TM;
	private Camera cam;
	private Camera sunCam;
	private World world;
	private Hud hud;
	private Sky sky;
	private GlobalTextManager textManager;
	private ShadowsManager shadowsManager;
	
	private Texture tilesTexture;
	private Texture nightDomeTexture;
	
	private int positionTexture;
	private int normalAndLightTexture;
	private int sunShadowTexture;
	private int colorTexture;
	
	private boolean hasFocus=true;
	private Thread shutdownHook;
	
	private int baseFbo;
	private int[] shadowFbos;
	
	private boolean changeContext=false; //To change with option menu.
	
	public MonecruftGame(MonecruftSettings settings) throws LWJGLException, IOException 
	{
		this.settings=settings;
		
		if(settings.FULLSCREEN_ENABLED){
			DisplayMode dm=Display.getDesktopDisplayMode();
			X_RES=dm.getWidth();
			Y_RES=dm.getHeight();
			IvEngine.configDisplay(dm, "Monecruft", true, false, true);
		}
		else IvEngine.configDisplay(X_RES, Y_RES, "Monecruft", true, false, false);
		
		Thread.currentThread().setPriority(Thread.currentThread().getPriority()+1); //Faster than the others -> game experience > loading
		
		//Start all resources
		float[] ssplits=ShadowsManager.calculateSplits(1f, CAMERA_FAR, 4, 0.3f);
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
		
		if(changeContext) {
			System.gc();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {}
			this.settings.FULLSCREEN_ENABLED=!this.settings.FULLSCREEN_ENABLED;
			new MonecruftGame(this.settings);
		}
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
			}
		}
		this.tilesTexture.bind();
		glBindFramebuffer(GL_FRAMEBUFFER, 0);
		glClear(GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		GL11.glViewport(0,0,X_RES,Y_RES);
		
		//glClearColor(0.6f, 0.8f, 1.0f, 0f);
		
		//tilesTexture.bind();
		this.world.overrideCurrentShader(null);
		this.world.overrideCurrentPVMatrix(null);
		//this.world.overrideCurrentPVMatrix(this.shadowsManager.getOrthoProjectionForSplit(3));
		//this.world.overrideCurrentPVMatrix(this.shadowsManager.getOrthoProjectionForSplit(0));
		this.world.draw(null);
		
		nightDomeTexture.bind();
		
		this.sky.draw();
		
		tilesTexture.bind();
		this.world.drawLiquids();
		this.world.afterDrawTasks();

		//this.VSP.disable();
		
		this.HSP.enable();
		glDisable(GL_DEPTH_TEST);
		//glDisable(GL11.GL_CULL_FACE);
		//glDisable( GL11.GL_BLEND );
		this.hud.draw();
		this.HSP.disable();
		TextureImpl.bindNone();
		
		this.textManager.draw(X_RES,Y_RES);
		/*glBindFramebuffer(GL_FRAMEBUFFER, 0);
		glClear(GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		this.FDSP.enable();
		glBindBuffer(GL15.GL_ARRAY_BUFFER,this.sky.getVbo());
		this.FDSP.setupAttributes();
		glUniform1i(this.FDSP.colorTex,1);
		glUniform1i(this.FDSP.positionTex,2);
		glUniform1i(this.FDSP.normalAndLightTex,3);
		glUniform1i(this.FDSP.shadowMap,4);
		glDrawArrays(GL_TRIANGLES, 0, Sky.NUM_COMPONENTS);
		this.FDSP.disable();*/
	}
	private void initResources() throws IOException
	{
		FloatBufferPool.init(Chunk.CHUNK_DIMENSION*Chunk.CHUNK_DIMENSION*Chunk.CHUNK_DIMENSION*2*6*6,20);
		ByteArrayPool.init(Chunk.CHUNK_DIMENSION,(World.PLAYER_VIEW_FIELD*2 +1)*World.HEIGHT*(World.PLAYER_VIEW_FIELD*2 +1)*2);
		glEnable(GL_DEPTH_TEST);
		glEnable(GL11.GL_CULL_FACE);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		glEnable( GL11.GL_BLEND );
		//(0.6, 0.8, 1.0, 1.0);
		glClearColor(0.6f, 0.8f, 1.0f, 0f);
		//glClearColor(0.5f, 0.7f, 1.0f, 0f);
		//Set-up shader program
		GL20.glUseProgram(0);
		textManager=new GlobalTextManager();
		this.UVSP=new UnderwaterVoxelShaderProgram(true);
		this.DVSP=new DepthVoxelShaderProgram(true);
		this.VSP=this.settings.SHADOWS_ENABLED?new VoxelShaderProgram(true):new UnshadowedVoxelShaderProgram(true);
		this.HSP=new HudShaderProgram(true);
		this.SSP=new SkyShaderProgram(true);
		this.BCSP=new BasicColorShaderProgram(true);
		this.FDSP=new FinalDrawShaderProgram(true);
		this.TM=new TimeManager();
		this.cam=new Camera(CAMERA_NEAR,CAMERA_FAR,80f,X_RES/Y_RES);
		this.shadowsManager=new ShadowsManager(SHADOW_SPLITS,this.cam);

		this.sunCam=/*new Camera(0.5f,1000,80f,X_RES/Y_RES);//*/new Camera(new Matrix4f());//World.HEIGHT*Chunk.CHUNK_DIMENSION, 0));
		this.sunCam.moveTo(0, 5, 0);
		this.sunCam.setPitch(0);
		
		this.sky=new Sky(SSP,BCSP,cam,this.sunCam);
		this.world=new World(this.VSP,this.UVSP,this.cam,this.sunCam,this.shadowsManager,this.sky);
		this.hud=new Hud(this.HSP,X_RES,Y_RES);
		//Load textures here
		
		tilesTexture = TextureLoader.getTexture("PNG", ResourceLoader.getResourceAsStream("images/monecruft_tiles.png"));
		// Setup the ST coordinate system
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
		
		// Setup what to do when the texture has to be scaled
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, 
				GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, 
				GL11.GL_NEAREST);
		
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
		/*this.baseFbo=glGenFramebuffers();
		glBindFramebuffer(GL_FRAMEBUFFER, this.baseFbo);
		
		int colorTexture=glGenTextures();
		int positionTexture=glGenTextures();
		int normalTexture=glGenTextures();
		
		glActiveTexture(GL_TEXTURE1); this.colorTexture=GL13.GL_TEXTURE1;
		glBindTexture(GL_TEXTURE_2D, colorTexture);
		glTexImage2D(GL_TEXTURE_2D, 0,GL_RGB, X_RES, Y_RES, 0,GL_RGB, GL_UNSIGNED_BYTE, (FloatBuffer)null);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		GL30.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorTexture, 0);
		
		glActiveTexture(GL_TEXTURE2); this.positionTexture=GL13.GL_TEXTURE2;
		glBindTexture(GL_TEXTURE_2D, positionTexture);
		glTexImage2D(GL_TEXTURE_2D, 0,GL_RGB32F, X_RES, Y_RES, 0,GL_RGB, GL_FLOAT, (FloatBuffer)null);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		GL30.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT1, GL_TEXTURE_2D, positionTexture, 0);
		
		glActiveTexture(GL_TEXTURE3); this.normalAndLightTexture=GL13.GL_TEXTURE3;
		glBindTexture(GL_TEXTURE_2D, normalTexture);
		glTexImage2D(GL_TEXTURE_2D, 0,GL_RGB, X_RES, Y_RES, 0,GL_RGB , GL_UNSIGNED_BYTE, (FloatBuffer)null); //Normals are pre-set (6 values max)
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		GL30.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT2, GL_TEXTURE_2D, normalTexture, 0);
		
		int depthbuf=glGenRenderbuffers();
		glBindRenderbuffer(GL_RENDERBUFFER, depthbuf);
		glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, X_RES, Y_RES);
		glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthbuf);
		
		IntBuffer drawBuffers = BufferUtils.createIntBuffer(3);
		
		drawBuffers.put(GL_COLOR_ATTACHMENT0);
		drawBuffers.put(GL_COLOR_ATTACHMENT1);
		drawBuffers.put(GL_COLOR_ATTACHMENT2);
		
		drawBuffers.flip();
		GL20.glDrawBuffers(drawBuffers);*/
		
		//SUN FBO: START
		if(this.settings.SHADOWS_ENABLED)
		{
			 
			int shadowTexture=glGenTextures();

			glActiveTexture(GL13.GL_TEXTURE4); this.sunShadowTexture=GL13.GL_TEXTURE4; 
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
		
				IntBuffer drawBuffers = BufferUtils.createIntBuffer(0);

				GL20.glDrawBuffers(drawBuffers);
				
			}
		}

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
		case Keyboard.KEY_F1:
			InputHandler.setESC(Keyboard.getEventKeyState());
			this.changeContext=true;
			break;
		case Keyboard.KEY_E:
			InputHandler.setE(Keyboard.getEventKeyState());
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
		boolean fullscreen=false;
		boolean noshadows=false;
		for(String s:args){
			if(s.equals("-fullscreen")) fullscreen=true;
			else if(s.equals("-noshadows")) noshadows=true;
		}
		MonecruftSettings settings=new MonecruftSettings();
		settings.FULLSCREEN_ENABLED=fullscreen;
		settings.SHADOWS_ENABLED=!noshadows;
		new MonecruftGame(settings);
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
		VSP.fullClean();
		this.TM=null;
		this.cam=null;
	}
}

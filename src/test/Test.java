package test;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL20.*;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.util.vector.Matrix4f;
import ivengine.*;
import ivengine.buffers.SimpleFloatVBO;
import ivengine.buffers.SimpleVAO;
import ivengine.shaders.FirstShader;
import ivengine.shaders.SimpleShaderProgram;
import ivengine.view.Camera;

public class Test 
{
	private SimpleVAO vao;
	private SimpleFloatVBO vbo;
	private SimpleShaderProgram ssp;
	
	private Camera cam;
	private int projectionMatrixLocation;
	private int viewMatrixLocation;
	private int modelMatrixLocation;
	private FloatBuffer matrix44Buffer=BufferUtils.createFloatBuffer(16);
	long lastFPS=getTime();
	int fps=0;
	private int tnum=1000000;
	public static void main(String[] args)
	{
		try {
			new Test();
		} catch (LWJGLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public Test() throws LWJGLException
	{
		IvEngine.configDisplay(800, 600, "IvEngine Test", true, false, false);
		initResources();
		
		boolean running = true;
		/*float randVert[]={0.0f,0.0f,0.0f, 1.0f,1.0f,1.0f, 0.0f,0.0f,
				1.0f,0.0f,0.0f, 1.0f,1.0f,1.0f, 1.0f,0.0f,
				0.0f,1.0f,0.0f, 1.0f,1.0f,1.0f, 0.0f,1.0f,
				0.0f,1.0f,0.0f, 1.0f,1.0f,1.0f, 0.0f,1.0f,
				1.0f,0.0f,0.0f, 1.0f,1.0f,1.0f, 1.0f,0.0f,
				1.0f,1.0f,0.0f, 1.0f,1.0f,1.0f, 1.0f,1.0f	
		};*/
		float randVert[]=new float[8*3*tnum];
		createTriangle(randVert,0,2f,2f,-1f,-1f,0f);
		for(int i=48;i<randVert.length;i+=48)
		{
			createTriangle(randVert,i);
		}
		
		this.vbo.enable();
		this.vbo.refreshContent(randVert);
		this.vao.enable();
		float val=cam.getZ();
		cam.setYaw(0.5f);
		while (running && !Display.isCloseRequested()) {
			
			val-=0.01;
			System.out.println(val);
			cam.moveTo(cam.getX(),cam.getY(),val);
			cam.setRoll(val);
			this.ssp.enable();
			Matrix4f viewMatrix=cam.getViewMatrix();
			viewMatrix.store(matrix44Buffer); matrix44Buffer.flip();
			glUniformMatrix4(viewMatrixLocation, false, matrix44Buffer);
			render();
			// Flip the buffers and sync to 60 FPS
			this.ssp.disable();
			Display.update();
			updateFPS();
			Display.sync(60);
		}
		
		// Dispose any resources and destroy our window
		dispose();
		Display.destroy();
	}
	private void initResources()
	{
		// 2D games generally won't require depth testing 
				glDisable(GL_DEPTH_TEST);
				glEnable(GL11.GL_TEXTURE_2D);
				
				// Enable blending
				glEnable(GL_BLEND);
				glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
				
				// Set clear to transparent black
				glClearColor(0f, 0f, 0f, 0f);
		//VAO
		this.vao=new SimpleVAO();
		vao.enable();

		//VBO & VERT LIST
		float vertices[] = {
			-0.5f, -0.5f, -0.0f, 1.0f,0.0f,0.0f,
			0.0f,1.0f,0.0f,0.0f,1.0f,0.0f,
			0.5f,-0.5f,0.0f,1.0f,0.0f,0.0f
		};
		this.vbo=new SimpleFloatVBO(GL_STATIC_DRAW);
		vbo.enable();
		vbo.refreshContent(vertices);
				
		this.ssp=new FirstShader(true);
		ssp.enable();
		ssp.setupAttributes();
		
		// TEXTURES
		
	    int texId=0;//Util.loadPNGTexture("D:/rotatetrans.png",GL13.GL_TEXTURE0); |TODO ehhmmmm fix
	    //System.out.println(Util.loadPNGTexture("D:/ballena.png",GL13.GL_TEXTURE1));
		System.out.println(texId);
	    glUniform1i(glGetUniformLocation(ssp.getID(), "text"), 0);
	    
	    projectionMatrixLocation=glGetUniformLocation(ssp.getID(), "projectionMatrix");
	    viewMatrixLocation=glGetUniformLocation(ssp.getID(), "viewMatrix");
	    modelMatrixLocation=glGetUniformLocation(ssp.getID(), "modelMatrix");
	    this.cam=new Camera(1, 5, 45, 800/600);
	    cam.moveTo(0,0.5f,0);
	    //cam.setPitch(1f);
	   // cam.setYaw(1f);
	    /*Matrix4f projectionMatrix=new Matrix4f();
	    projectionMatrix=MatrixHelper.createProjectionMatrix(1, 10, 45, 800/600);
	    Matrix4f viewMatrix=new Matrix4f();
	    Matrix4f.translate(new Vector3f(1,0,-5), viewMatrix, viewMatrix);*/
	    Matrix4f projectionMatrix=cam.getProjectionMatrix();
	    Matrix4f viewMatrix=cam.getViewMatrix();
	    Matrix4f modelMatrix=new Matrix4f();
	    projectionMatrix.store(matrix44Buffer); matrix44Buffer.flip();
	    glUniformMatrix4(projectionMatrixLocation, false, matrix44Buffer);
	    viewMatrix.store(matrix44Buffer); matrix44Buffer.flip();
	    glUniformMatrix4(viewMatrixLocation, false, matrix44Buffer);
	    modelMatrix.store(matrix44Buffer); matrix44Buffer.flip();
	    glUniformMatrix4(modelMatrixLocation, false, matrix44Buffer);
		
		vao.disable();
		vbo.disable();
		ssp.disable();
	}
	
	/**
	 * Get the accurate system time
	 * 
	 * @return The system time in milliseconds
	 */
	public long getTime() {
	    return (Sys.getTime() * 1000) / Sys.getTimerResolution();
	}
 
	/**
	 * Calculate the FPS and set it in the title bar
	 */
	public void updateFPS() {
		if (getTime() - lastFPS > 1000) {
			Display.setTitle("FPS: " + fps);
			fps = 0;
			lastFPS += 1000;
		}
		fps++;
	}
	private void render()
	{
		glClear(GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		glUniform1i(glGetUniformLocation(ssp.getID(), "text"), 1);
		glDrawArrays(GL_TRIANGLES, 0,6);
		//this.vao.enable();
		glUniform1i(glGetUniformLocation(ssp.getID(), "text"), 0);
		glDrawArrays(GL_TRIANGLES, 6, tnum);
	}
	private void dispose()
	{
		
	}
	private void createTriangle(float[] lista,int inicio)
	{
		float xi=(float)(Math.random()*2)-1;
		float yi=(float)(Math.random()*2)-1;
		float z=(float)Math.random()*2;
		//float width=(float)Math.random();
		//float height=(float)Math.random();
		float width=0.1f;
		float height=0.1f;
		createTriangle(lista,inicio,width,height,xi,yi,z);
	}
	private void createTriangle(float[] lista,int inicio,float width,float height,float xi,float yi,float z)
	{
		int c=0;
		lista[inicio+c]=xi; lista[inicio+c+1]=yi; lista[inicio+c+2]=z;
		lista[inicio+c+3]=(float)Math.random(); lista[inicio+c+4]=(float)Math.random(); lista[inicio+c+5]=(float)Math.random();
		lista[inicio+c+6]=0.0f;lista[inicio+c+7]=1.0f;
		c+=8;
		lista[inicio+c]=xi+width; lista[inicio+c+1]=yi; lista[inicio+c+2]=z;
		lista[inicio+c+3]=(float)Math.random(); lista[inicio+c+4]=(float)Math.random(); lista[inicio+c+5]=(float)Math.random();
		lista[inicio+c+6]=1.0f;lista[inicio+c+7]=1.0f;
		c+=8;
		lista[inicio+c]=xi; lista[inicio+c+1]=yi+height; lista[inicio+c+2]=z;
		lista[inicio+c+3]=(float)Math.random(); lista[inicio+c+4]=(float)Math.random(); lista[inicio+c+5]=(float)Math.random();
		lista[inicio+c+6]=0.0f;lista[inicio+c+7]=0.0f;
		c+=8;
		lista[inicio+c]=xi+width; lista[inicio+c+1]=yi; lista[inicio+c+2]=z;
		lista[inicio+c+3]=(float)Math.random(); lista[inicio+c+4]=(float)Math.random(); lista[inicio+c+5]=(float)Math.random();
		lista[inicio+c+6]=1.0f;lista[inicio+c+7]=1.0f;
		c+=8;
		lista[inicio+c]=xi+width; lista[inicio+c+1]=yi+height; lista[inicio+c+2]=z;
		lista[inicio+c+3]=(float)Math.random(); lista[inicio+c+4]=(float)Math.random(); lista[inicio+c+5]=(float)Math.random();
		lista[inicio+c+6]=1.0f;lista[inicio+c+7]=0.0f;
		c+=8;
		lista[inicio+c]=xi; lista[inicio+c+1]=yi+height; lista[inicio+c+2]=z;
		lista[inicio+c+3]=(float)Math.random(); lista[inicio+c+4]=(float)Math.random(); lista[inicio+c+5]=(float)Math.random();
		lista[inicio+c+6]=0.0f;lista[inicio+c+7]=0.0f;
		c+=8;
	}
}

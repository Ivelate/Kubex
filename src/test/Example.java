package test;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetAttribLocation;
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

import ivengine.Util;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.PixelFormat;

public class Example {
	// Entry point for the application
	public static void main(String[] args) {
		new Example();
	}
	
	// Setup variables
	private final String WINDOW_TITLE = "The Quad: glDrawArrays";
	private final int WIDTH = 320;
	private final int HEIGHT = 240;
	// Quad variables
	private int vaoId = 0;
	private int vboId = 0;
	private int vertexCount = 0;
	
	public Example() {
		// Initialize OpenGL (Display)
		this.setupOpenGL();
		
		this.setupQuad();
		
		while (!Display.isCloseRequested()) {
			// Do a single loop (logic/render)
			this.loopCycle();
			
			// Force a maximum FPS of about 60
			Display.sync(60);
			// Let the CPU synchronize with the GPU if GPU is tagging behind
			Display.update();
		}
		
		// Destroy OpenGL (Display)
		this.destroyOpenGL();
	}
	
	public void setupOpenGL() {
		// Setup an OpenGL context with API version 3.2
		try {
			PixelFormat pixelFormat = new PixelFormat();
			ContextAttribs contextAtrributes = new ContextAttribs(3, 2)
				.withForwardCompatible(true)
				.withProfileCore(true);
			
			Display.setDisplayMode(new DisplayMode(WIDTH, HEIGHT));
			Display.setTitle(WINDOW_TITLE);
			Display.create(pixelFormat, contextAtrributes);
			
			GL11.glViewport(0, 0, WIDTH, HEIGHT);
		} catch (LWJGLException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		// Setup an XNA like background color
		GL11.glClearColor(0.4f, 0.6f, 0.9f, 0f);
		
		// Map the internal OpenGL coordinate system to the entire screen
		GL11.glViewport(0, 0, WIDTH, HEIGHT);
		
		this.exitOnGLError("Error in setupOpenGL");
	}
	
	public void setupQuad() {		
		// OpenGL expects vertices to be defined counter clockwise by default
		float[] vertices = {
				// Left bottom triangle
				-0.5f, 0.5f, 0f,
				-0.5f, -0.5f, 0f,
				0.5f, -0.5f, 0f,
				// Right top triangle
				0.5f, -0.5f, 0f,
				0.5f, 0.5f, 0f,
				-0.5f, 0.5f, 0f
		};
		// Sending data to OpenGL requires the usage of (flipped) byte buffers
		FloatBuffer verticesBuffer = BufferUtils.createFloatBuffer(vertices.length);
		verticesBuffer.put(vertices);
		verticesBuffer.flip();
		
		vertexCount = 6;
		
		// Create a new Vertex Array Object in memory and select it (bind)
		// A VAO can have up to 16 attributes (VBO's) assigned to it by default
		vaoId = GL30.glGenVertexArrays();
		GL30.glBindVertexArray(vaoId);
		
		// Create a new Vertex Buffer Object in memory and select it (bind)
		// A VBO is a collection of Vectors which in this case resemble the location of each vertex.
		vboId = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, verticesBuffer, GL15.GL_STATIC_DRAW);
		// Put the VBO in the attributes list at index 0
		int shaderProgram=readShaders();
		int posAttrib = glGetAttribLocation(shaderProgram, "position");
		System.out.println(posAttrib);
		
		glVertexAttribPointer(posAttrib,3,GL_FLOAT,false,0,0);
		glEnableVertexAttribArray(posAttrib);
		//GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0);
		// Deselect (bind to 0) the VBO
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		
		// Deselect (bind to 0) the VAO
		GL30.glBindVertexArray(0);
		
		this.exitOnGLError("Error in setupQuad");
	}
	private int readShaders()
	{
		final String FRAGMENT = Util.readFile("/shaders/fragmentShader.fshader");
		final String VERTEX = Util.readFile("/shaders/vertexShader.vshader");
		
		int vShader = glCreateShader(GL_VERTEX_SHADER);
		glShaderSource(vShader, VERTEX);
		glCompileShader(vShader);
		String status = glGetShaderInfoLog(vShader, 1000); //Verbose
		System.out.println(status);

		int fShader = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(fShader, FRAGMENT);
		glCompileShader(fShader);
		status = glGetShaderInfoLog(fShader, 1000); //Verbose
		System.out.println(status);

		int shaderProgram = glCreateProgram();
		glAttachShader(shaderProgram, vShader);
		glAttachShader(shaderProgram, fShader);

		glLinkProgram(shaderProgram);
		status =  glGetProgramInfoLog(shaderProgram, GL_LINK_STATUS); //Verbose
		System.out.println(status);
		glUseProgram(shaderProgram);
		
		return shaderProgram;
	}
	public void loopCycle() {
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
		
		// Bind to the VAO that has all the information about the quad vertices
		GL30.glBindVertexArray(vaoId);
		GL20.glEnableVertexAttribArray(0);
		
		// Draw the vertices
		GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, vertexCount);
		
		// Put everything back to default (deselect)
		GL20.glDisableVertexAttribArray(0);
		GL30.glBindVertexArray(0);
		
		this.exitOnGLError("Error in loopCycle");
	}
	
	public void destroyOpenGL() {		
		// Disable the VBO index from the VAO attributes list
		GL20.glDisableVertexAttribArray(0);
		
		// Delete the VBO
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		GL15.glDeleteBuffers(vboId);
		
		// Delete the VAO
		GL30.glBindVertexArray(0);
		GL30.glDeleteVertexArrays(vaoId);
		
		Display.destroy();
	}
	
	public void exitOnGLError(String errorMessage) {
		int errorValue = GL11.glGetError();
		
		if (errorValue != GL11.GL_NO_ERROR) {
			/*String errorString = GLU.gluErrorString(errorValue);
			System.err.println("ERROR - " + errorMessage + ": " + errorString);
			*/
			if (Display.isCreated()) Display.destroy();
			System.exit(-1);
		}
	}
}
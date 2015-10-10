package test;
/**
 * Copyright (c) 2012, Matt DesLauriers All rights reserved.
 *
 *	Redistribution and use in source and binary forms, with or without
 *	modification, are permitted provided that the following conditions are met: 
 *
 *	* Redistributions of source code must retain the above copyright notice, this
 *	  list of conditions and the following disclaimer. 
 *
 *	* Redistributions in binary
 *	  form must reproduce the above copyright notice, this list of conditions and
 *	  the following disclaimer in the documentation and/or other materials provided
 *	  with the distribution. 
 *
 *	* Neither the name of the Matt DesLauriers nor the names
 *	  of his contributors may be used to endorse or promote products derived from
 *	  this software without specific prior written permission.
 *
 *	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *	AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *	IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *	ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 *	LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *	CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *	SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *	INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *	CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *	ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *	POSSIBILITY OF SUCH DAMAGE.
 */


import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;

import ivengine.shaders.FirstShader;
import ivengine.shaders.SimpleShaderProgram;

import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

/**
 * A bare-bones implementation of a LWJGL application.
 * @author davedes
 */
public class Game {
	
	// Whether to enable VSync in hardware.
	public static final boolean VSYNC = true;
	
	// Width and height of our window
	public static final int WIDTH = 800;
	public static final int HEIGHT = 600;
	
	// Whether to use fullscreen mode
	public static final boolean FULLSCREEN = false;
	
	// Whether our game loop is running
	protected boolean running = false;
	
	public static void main(String[] args) throws LWJGLException {
		new Game().start();
	}
	
	// Start our game
	public void start() throws LWJGLException {
		// Set up our display 
		Display.setTitle("Display example"); //title of our window
		Display.setResizable(true); //whether our window is resizable
		Display.setDisplayMode(new DisplayMode(WIDTH, HEIGHT)); //resolution of our display
		Display.setVSyncEnabled(VSYNC); //whether hardware VSync is enabled
		Display.setFullscreen(FULLSCREEN); //whether fullscreen is enabled

		//create and show our display
		Display.create();
		
		// Create our OpenGL context and initialize any resources
		create();
		
		// Call this before running to set up our initial size
		resize();
		running = true;
		// While we're still running and the user hasn't closed the window... 
		while (running && !Display.isCloseRequested()) {
			// If the game was resized, we need to update our projection
			if (Display.wasResized())
				resize();
			
			// Render the game
			render();
			
			// Flip the buffers and sync to 60 FPS
			Display.update();
			Display.sync(60);
		}
		
		// Dispose any resources and destroy our window
		dispose();
		Display.destroy();
	}
	// Exit our game loop and close the window
	public void exit() {
		running = false;
	}
	
	// Called to setup our game and context
	protected void create() {
		// 2D games generally won't require depth testing 
		glDisable(GL_DEPTH_TEST);
		
		// Enable blending
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		
		// Set clear to transparent black
		glClearColor(0f, 0f, 0f, 0f);
				
		//VAO
		int vao=glGenVertexArrays();
		glBindVertexArray(vao);

		//VBO & VERT LIST
		float vertices[] = {
			-0.5f, -0.5f, -0.0f, 1.0f,0.0f,0.0f,
			0.0f,1.0f,0.0f,0.0f,1.0f,0.0f,
			0.5f,-0.5f,0.0f,1.0f,0.0f,0.0f
			
		};
		int vbo=glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		FloatBuffer fb=BufferUtils.createFloatBuffer(vertices.length).put(vertices);
		fb.flip();
		glBufferData(GL_ARRAY_BUFFER, fb, GL_STATIC_DRAW);
		
		SimpleShaderProgram ssp=new FirstShader(true);
		ssp.enable();
	}
	
	// Called to render our game
	protected void render() {
		// Clear the screen
		glClear(GL_COLOR_BUFFER_BIT);

		glDrawArrays(GL_TRIANGLES, 0, 3);
		// ... render our game here ...
	}
	
	// Called to resize our game
	protected void resize() {
		glViewport(0, 0, Display.getWidth(), Display.getHeight());
		// ... update our projection matrices here ...
	}
	
	// Called to destroy our game upon exiting
	protected void dispose() {
		// ... dispose of any textures, etc ...
	}
}
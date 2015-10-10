package ivengine.buffers;

import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glGenBuffers;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

public class SimpleFloatVBO 
{
	private int bufferIndex;
	private int drawType; //GL_STATIC_DRAW for example
	
	public SimpleFloatVBO(int drawType)
	{
		this.bufferIndex=glGenBuffers();
		this.drawType=drawType;
	}
	public void refreshContent(float[] vertices)
	{
		FloatBuffer fb=BufferUtils.createFloatBuffer(vertices.length).put(vertices);
		fb.flip();
		glBufferData(GL_ARRAY_BUFFER, fb, this.drawType);
	}
	public void enable()
	{
		glBindBuffer(GL_ARRAY_BUFFER,this.bufferIndex);
	}
	public void disable()
	{
		glBindBuffer(GL_ARRAY_BUFFER,0);
	}
}

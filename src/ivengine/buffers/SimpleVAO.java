package ivengine.buffers;

import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class SimpleVAO 
{
	private int bufferIndex;
	public SimpleVAO()
	{
		this.bufferIndex=glGenVertexArrays();
	}
	public void enable()
	{
		glBindVertexArray(this.bufferIndex);
	}
	public void disable()
	{
		glBindVertexArray(0);
	}
}

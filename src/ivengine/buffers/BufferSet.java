package ivengine.buffers;

import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
/**
 * 
 * @author Ivelate
 * @since 7-12-2013 18:00
 *
 */
@Deprecated
public class BufferSet 
{
	private int myVAO;
	private int[] vbolist;
	private int numvbo=0;
	
	public BufferSet(int maxsize)
	{
		this.myVAO=glGenVertexArrays();
		glBindVertexArray(this.myVAO);
		this.vbolist=new int[maxsize];
	}
	public void addVbo()
	{
		int vbo=glGenBuffers();
		this.vbolist[numvbo]=vbo; numvbo++;
		glBindBuffer(GL_ARRAY_BUFFER, vbo);
	}
}

package monecruft.gui;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glBufferSubData;
import static org.lwjgl.opengl.GL15.glGenBuffers;

import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import monecruft.shaders.HudShaderProgram;
import monecruft.storage.FloatBufferPool;

public class Hud 
{
	private static final float CURSOR_SIZE=4;
	private static final float CURSOR_ALPHA=0.5f;
	private static final float CURSOR_R=1;
	private static final float CURSOR_G=1;
	private static final float CURSOR_B=1;
	private static final int CURSOR_VERT_NUM=8;
	
	private HudShaderProgram HSP;
	private int xres,yres;
	private int vbo;
	private int currentVert;
	public Hud(HudShaderProgram hsp,int xres,int yres)
	{
		this.xres=xres;
		this.yres=yres;
		this.HSP=hsp;
		this.vbo=glGenBuffers();
		glBindBuffer(GL15.GL_ARRAY_BUFFER,this.vbo);
	
		HSP.enable();
		HSP.setupAttributes();
		FloatBuffer toUpload=FloatBufferPool.getBuffer();
		float normx=CURSOR_SIZE/xres;
		float normy=CURSOR_SIZE/yres;
		/*float[] list={-normx,normy,		0,1,
					  -normx,-normy,	0,0,
					  normx,normy,		1,1,
					  normx,normy,		1,1,
					  -normx,-normy,	0,1,
					  normx,-normy,		1,0};*/
		float[] list=new float[CURSOR_VERT_NUM*3*6];
		insertCircle(0,0,normx,normy,CURSOR_VERT_NUM,CURSOR_R,CURSOR_G,CURSOR_B,CURSOR_ALPHA,list);
		//insertCircle(0,0,2,2,4,0,0,CURSOR_B,CURSOR_ALPHA,list);

		toUpload.put(list);
		toUpload.flip();
		glBufferData(GL15.GL_ARRAY_BUFFER,(list.length*4)+(36*4),GL15.GL_STATIC_DRAW);
		glBufferSubData(GL15.GL_ARRAY_BUFFER,0,toUpload);
		glBindBuffer(GL15.GL_ARRAY_BUFFER,0);
		FloatBufferPool.recycleBuffer(toUpload);
		this.currentVert=CURSOR_VERT_NUM*3;
	}
	private void insertCircle(float ix,float iy,float radx,float rady,int vert,float c_r,float c_g,float c_b,float c_a,float[] list)
	{
		float lastx=radx+ix;
		float lasty=iy;
		for(int i =1; i <= vert; i++)
		{
			double angle = 2 * Math.PI * i / vert;
			float x = (float) ((Math.cos(angle)*radx)+ix);
			float y = (float) ((Math.sin(angle)*rady)+iy);
			int cont=(i-1)*3*6;
			list[cont]=ix; list[cont+1]=iy; list[cont+2]=c_r; list[cont+3]=c_g; list[cont+4]=c_b; list[cont+5]=c_a;
			list[cont+6]=lastx; list[cont+7]=lasty; list[cont+8]=c_r; list[cont+9]=c_g; list[cont+10]=c_b; list[cont+11]=c_a;
			list[cont+12]=x; list[cont+13]=y; list[cont+14]=c_r; list[cont+15]=c_g; list[cont+16]=c_b; list[cont+17]=c_a;
			lastx=x;
			lasty=y;
		}
	}
	public void draw()
	{
		glEnable( GL11.GL_BLEND );
		glDisable(GL11.GL_DEPTH_TEST);
		glBindBuffer(GL15.GL_ARRAY_BUFFER,this.vbo);
		HSP.setupAttributes();
		glDrawArrays(GL_TRIANGLES, 0, currentVert);
		glBindBuffer(GL15.GL_ARRAY_BUFFER,0);
	}
}

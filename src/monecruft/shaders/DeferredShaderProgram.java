package monecruft.shaders;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetAttribLocation;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

import org.lwjgl.opengl.GL11;

import ivengine.shaders.SimpleShaderProgram;

public abstract class DeferredShaderProgram extends SimpleShaderProgram{
	
	private int locAttrib;
	private int farFaceLocAttrib;
	private int farFaceCamViewLocation;
	
	public DeferredShaderProgram(String vshader,String fshader)
	{
		this(vshader,fshader,false);
	}
	public DeferredShaderProgram(String vshader,String fshader,boolean verbose)
	{
		super(vshader,fshader,verbose);
		
		this.locAttrib = glGetAttribLocation(this.getID(), "location");
		this.farFaceLocAttrib = glGetAttribLocation(this.getID(), "farFaceLocation"); 
		this.farFaceCamViewLocation = glGetAttribLocation(this.getID(), "farFaceCamViewLocation");
	}
	
	@Override
	public void setupAttributes() {
		glVertexAttribPointer(locAttrib,2,GL11.GL_FLOAT,false,4*this.getSize(),0);
		glEnableVertexAttribArray(locAttrib);
		glVertexAttribPointer(farFaceLocAttrib,3,GL11.GL_FLOAT,false,4*this.getSize(),2*4);
		glEnableVertexAttribArray(farFaceLocAttrib);
		glVertexAttribPointer(farFaceCamViewLocation,3,GL11.GL_FLOAT,false,4*this.getSize(),5*4);
		glEnableVertexAttribArray(farFaceCamViewLocation);
		
		setupCustomAttributes();
	}
	
	protected abstract void setupCustomAttributes();
	
	public abstract boolean supportShadows();
	public abstract int colorTexLocation();
	public abstract int miscTexLocation();
	public abstract boolean supportSkyParameters();
	public abstract boolean supportWorldPosition();
	
	@Override
	protected void dispose() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public int getSize() {
		return 8;
	}
}

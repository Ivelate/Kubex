package monecruft.shaders;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetAttribLocation;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

import org.lwjgl.opengl.GL11;

import ivengine.shaders.SimpleShaderProgram;

public class SkyShaderProgram extends SimpleShaderProgram{
	
	private int locAttrib;
	
	private int invertedViewRotationMatrixUniform;
	private int perezcoeffUniform;
	private int zenitalAbsUniform;
	private int solarAzimuthUniform;
	private int solarZenithUniform;
	
	public SkyShaderProgram()
	{
		this(false);
	}
	public SkyShaderProgram(boolean verbose)
	{
		super("/shaders/skyShader.vshader","/shaders/skyShader.fshader",verbose);
		
		this.locAttrib = glGetAttribLocation(this.getID(), "location");
		
		this.invertedViewRotationMatrixUniform= glGetUniformLocation(this.getID(),"invertedViewRotationMatrix");
		this.perezcoeffUniform=glGetUniformLocation(this.getID(),"coeff");
		this.zenitalAbsUniform=glGetUniformLocation(this.getID(),"zenitalAbs");
		this.solarAzimuthUniform=glGetUniformLocation(this.getID(),"solar_azimuth");
		this.solarZenithUniform=glGetUniformLocation(this.getID(),"solar_zenith");
		
	}
	
	public int getInvertedViewRotationMatrixLoc() {
		return this.invertedViewRotationMatrixUniform;
	}
	public int getPerezcoeffUniform() {
		return perezcoeffUniform;
	}
	public int getZenitalAbsUniform() {
		return zenitalAbsUniform;
	}
	public int getSolarAzimuthUniform() {
		return solarAzimuthUniform;
	}
	public int getSolarZenithUniform() {
		return solarZenithUniform;
	}
	
	@Override
	public void setupAttributes() {
		glVertexAttribPointer(locAttrib,2,GL11.GL_FLOAT,false,4*2,0);
		glEnableVertexAttribArray(locAttrib);
	}
	@Override
	protected void dispose() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public int getSize() {
		return 2;
	}
}

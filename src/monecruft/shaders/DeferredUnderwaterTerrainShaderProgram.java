package monecruft.shaders;

import monecruft.MonecruftGame;

public class DeferredUnderwaterTerrainShaderProgram extends DeferredShaderProgram{

	public DeferredUnderwaterTerrainShaderProgram(boolean verbose)
	{
		super("/shaders/finalDrawShader.vshader","/shaders/finalDrawShaderUnderwater.fshader",true);
	}
	@Override
	protected void setupCustomAttributes() {
		
	}
	@Override
	public boolean supportShadows() {
		return true;
	}
	@Override
	public int colorTexLocation() {
		return MonecruftGame.BASEFBO_COLOR_TEXTURE_LOCATION;
	}
	@Override
	public int miscTexLocation() {
		return -1;
	}
	@Override
	public boolean supportSkyParameters() {
		return true;
	}
	@Override
	public boolean supportWorldPosition() {
		return false;
	}
	@Override
	public int miscTex2Location() {
		return -1;
	}
	@Override
	public boolean supportPlayerLighting() 
	{
		return true;
	}

}

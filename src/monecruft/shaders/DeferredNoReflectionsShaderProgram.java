package monecruft.shaders;

import monecruft.MonecruftGame;

public class DeferredNoReflectionsShaderProgram extends DeferredShaderProgram{

	public DeferredNoReflectionsShaderProgram(boolean verbose)
	{
		super("/shaders/finalDrawShader.vshader","/shaders/finalDrawShaderNoReflections.fshader",true);
	}
	@Override
	protected void setupCustomAttributes() {
		
	}
	@Override
	public boolean supportShadows() {
		return false;
	}
	@Override
	public int colorTexLocation() {
		return MonecruftGame.DEFERREDFBO_COLOR_TEXTURE_LOCATION;
	}
	@Override
	public int miscTexLocation() {
		return MonecruftGame.WATER_NORMAL_TEXTURE_LOCATION;
	}
	@Override
	public boolean supportSkyParameters() {
		return true;
	}
	@Override
	public boolean supportWorldPosition() {
		return true;
	}
	@Override
	public int miscTex2Location() {
		return MonecruftGame.CURRENT_LIQUID_NORMAL_TEXTURE_LOCATION;
	}
	@Override
	public boolean supportPlayerLighting() {
		return false;
	}

}

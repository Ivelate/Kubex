package monecruft.shaders;

import monecruft.MonecruftGame;

public class DeferredTerrainUnshadowShaderProgram extends DeferredShaderProgram{

	public DeferredTerrainUnshadowShaderProgram(boolean verbose)
	{
		super("/shaders/finalDrawShader.vshader","/shaders/finalDrawUnshadowShader.fshader",true);
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
		return MonecruftGame.BASEFBO_COLOR_TEXTURE_LOCATION;
	}
	@Override
	public int miscTexLocation() {
		return -1;
	}
	@Override
	public boolean supportSkyParameters() {
		return false;
	}
	@Override
	public boolean supportWorldPosition() {
		return false;
	}

}

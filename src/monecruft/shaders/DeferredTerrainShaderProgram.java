package monecruft.shaders;

import monecruft.MonecruftGame;

public class DeferredTerrainShaderProgram extends DeferredShaderProgram{

	public DeferredTerrainShaderProgram(boolean verbose)
	{
		super("/shaders/finalDrawShader.vshader","/shaders/finalDrawShader.fshader",true);
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
		return false;
	}
	@Override
	public boolean supportWorldPosition() {
		return false;
	}

}

package kubex.shaders;

import kubex.KubexGame;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Second pass deferred over water shader with shadows enabled
 */
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
		return KubexGame.BASEFBO_COLOR_TEXTURE_LOCATION;
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
	@Override
	public int miscTex2Location() {
		return -1;
	}
	@Override
	public boolean supportPlayerLighting() {
		return false;
	}

}

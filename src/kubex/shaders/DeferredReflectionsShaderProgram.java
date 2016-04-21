package kubex.shaders;

import kubex.KubexGame;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Third pass deferred over water shader with reflections enabled
 */
public class DeferredReflectionsShaderProgram extends DeferredShaderProgram{

	public DeferredReflectionsShaderProgram(boolean verbose)
	{
		super("/shaders/finalDrawShader.vshader","/shaders/finalDrawShaderReflections.fshader",true);
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
		return KubexGame.DEFERREDFBO_COLOR_TEXTURE_LOCATION;
	}
	@Override
	public int miscTexLocation() {
		return KubexGame.WATER_NORMAL_TEXTURE_LOCATION;
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
		return KubexGame.CURRENT_LIQUID_NORMAL_TEXTURE_LOCATION;
	}
	@Override
	public boolean supportPlayerLighting() {
		return false;
	}

}

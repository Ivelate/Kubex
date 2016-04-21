package kubex.utils;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Exception thrown if the current GPU does not support some of the features of the game, like the current version of OpenGL, the number of textures needed, etc.
 */
public class KubexGPUException extends KubexException
{
	public KubexGPUException(String s)
	{
		super(s);
	}
}

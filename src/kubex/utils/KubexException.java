package kubex.utils;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Generic exception. When some of those are thrown, it is assumed that the program failed for reasons not relationed with this project or coding directly
 */
public abstract class KubexException extends RuntimeException
{
	public KubexException(String s)
	{
		super(s);
	}
}

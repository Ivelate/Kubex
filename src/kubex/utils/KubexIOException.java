package kubex.utils;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Exception thrown if the program isn't able to read or write files in the current folder it's located
 */
public class KubexIOException extends KubexException
{
	public KubexIOException(String s)
	{
		super(s);
	}
}

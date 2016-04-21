package ivengine.properties;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * To specify that a class can be disposed, or cleaned.
 * Because fullClean() sounds way better than dispose() (And because dispose is already taken in some default implementations).
 */
public interface Cleanable {
	public void fullClean();
}

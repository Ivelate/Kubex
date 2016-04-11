package ivengine.properties;

/**
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * To specify that a class can be disposed, or cleaned.
 * Because fullClean() sounds way better than dispose() (And because dispose is already taken in some default implementations).
 */
public interface Cleanable {
	public void fullClean();
}

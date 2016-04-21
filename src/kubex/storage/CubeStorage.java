package kubex.storage;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Generic class used to abstract both the light and cubes data in a chunk
 */
public interface CubeStorage 
{
	/**
	 * Gets the value at <x><y><z>
	 */
	public byte get(int x,int y,int z);
	
	/**
	 * Sets the value in <x> <y> <z> to <val>
	 */
	public void set(int x,int y,int z,byte val);
	
	/**
	 * Disposes this storage, if needed
	 */
	public void dispose();
	
	/**
	 * Returns true if the storage is composed of an array, false if it is a constant value. Used because insteadof looks ugly
	 */
	public boolean isTrueStorage();
	
	/**
	 * If this storage uses an array, returns it. If not, returns null
	 */
	public byte[][][] getArray();
}

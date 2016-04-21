package kubex.storage;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Class which abstracts both the light and cubes data in a chunk. In this case, it just encapsulates a normal chunk.
 */
public class ArrayCubeStorage implements CubeStorage
{
	private byte[][][] cubes;
	private boolean clean;
	public ArrayCubeStorage(byte[][][] array,boolean clean)
	{
		cubes=array;
		this.clean=clean;
	}
	@Override
	public byte get(int x, int y, int z) {
		return cubes[x][y][z];
	}

	@Override
	public void set(int x, int y, int z, byte val) {
		if(this.clean&&val!=0) this.clean=false;
		cubes[x][y][z]=val;
	}

	/**
	 * Recycles the byte array in the byte array pool.
	 */
	@Override
	public void dispose() {
		if(clean)ByteArrayPool.recycleCleanArray(this.cubes);
		else ByteArrayPool.recycleArray(this.cubes);
	}
	@Override
	public boolean isTrueStorage() {
		return true;
	}
	@Override
	public byte[][][] getArray() {
		return this.cubes;
	}

}

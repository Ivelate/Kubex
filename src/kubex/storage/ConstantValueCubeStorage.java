package kubex.storage;

import kubex.gui.Chunk;
import kubex.gui.Chunk.CubeStorageType;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Class which abstracts both the light and cubes data in a chunk. In this case, it contains a byte value instead of an array, saving a lot of space and returning this 
 * constant value in each request. If some value of it needs to be updated, notifies the chunk, because its possible that this object needs to be switched in that case for a ArrayCubeStorage
 */
public class ConstantValueCubeStorage implements CubeStorage
{
	private byte val;
	private Chunk myChunk;
	private CubeStorageType cubeStorageType;
	
	public ConstantValueCubeStorage(byte val,Chunk chunk,CubeStorageType cubeStorageType)
	{
		this.val=val;
		this.myChunk=chunk;
		this.cubeStorageType=cubeStorageType;
	}
	@Override
	public byte get(int x, int y, int z) {
		return val;
	}

	@Override
	public void set(int x, int y, int z, byte val) {
		if(this.val!=val) this.myChunk.notifyCubeStorageUpdate(x, y, z, val, this.val, cubeStorageType);
	}

	@Override
	public void dispose() {
		this.myChunk=null;
	}
	@Override
	public boolean isTrueStorage() {
		return false;
	}
	@Override
	public byte[][][] getArray() {
		return null;
	}

}

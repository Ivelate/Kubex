package monecruft.storage;

import monecruft.gui.Chunk;
import monecruft.gui.Chunk.CubeStorageType;

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

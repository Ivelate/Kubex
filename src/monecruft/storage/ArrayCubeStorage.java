package monecruft.storage;

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

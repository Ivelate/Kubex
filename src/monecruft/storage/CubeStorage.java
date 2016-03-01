package monecruft.storage;

public interface CubeStorage 
{
	public byte get(int x,int y,int z);
	public void set(int x,int y,int z,byte val);
	public void dispose();
	public boolean isTrueStorage();
}

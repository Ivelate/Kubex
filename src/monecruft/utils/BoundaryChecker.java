package monecruft.utils;

public interface BoundaryChecker 
{
	public boolean sharesBoundariesWith(float x,float y,float z,float radius);
	public boolean applyCulling();
}

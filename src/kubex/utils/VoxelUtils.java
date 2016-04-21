package kubex.utils;

public class VoxelUtils 
{
	
	public static int trueMod(double val,int toMod)
	{
		if(val>0)
		{
			return (int)(val)%toMod;
		}
		else
		{
			int auxmod=toMod-(((int)(-val)%toMod)+1);
			return auxmod;
		}
	}
}

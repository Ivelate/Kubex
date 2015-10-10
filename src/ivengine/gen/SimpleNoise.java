package ivengine.gen;

public class SimpleNoise 
{
	public static float getValue(int x,int y,int suav)
	{
		if(suav==0){return rand(x,y);}
		float corners = ( getValue(x-1, y-1,suav-1)+getValue(x+1, y-1,suav-1)+getValue(x-1, y+1,suav-1)+getValue(x+1, y+1,suav-1) ) / 16;
		float sides   = ( getValue(x-1, y,suav-1)  +getValue(x+1, y,suav-1)  +getValue(x, y-1,suav-1)  +getValue(x, y+1,suav-1) ) /  8;
		float center  =  getValue(x, y,suav-1) / 4;
		return corners + sides + center;
	}
	private static float smooth(int x,int y)
	{
		float corners = ( rand(x-1, y-1)+rand(x+1, y-1)+rand(x-1, y+1)+rand(x+1, y+1) ) / 16;
		float sides   = ( rand(x-1, y)  +rand(x+1, y)  +rand(x, y-1)  +rand(x, y+1) ) /  8;
		float center  =  rand(x, y) / 4;
		return corners + sides + center;
	}
	public static float rand(int x,int y)
	{
		return (((x*(x*x + 17))+y*7683)/(float)(47))%1.0f;
	}
}

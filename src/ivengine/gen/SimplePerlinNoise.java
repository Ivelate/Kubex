package ivengine.gen;

/**
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Perlin noise implementation. Simplex noise is more efficient and better overall, so this class is considered deprecated. Anyways, is a completely valid and functional implementation,
 * but simplex noise simply beats it on each field.
 */
@Deprecated
public class SimplePerlinNoise {
	private static final float FLOAT_PI=(float)Math.PI;
	private final int bigprime;
	private final int mediumprime;
	private final int smallprime;
	private final float zoom;
	public SimplePerlinNoise(int bigprime,int mediumprime,int smallprime,float zoom){
		this.bigprime=bigprime;
		this.mediumprime=mediumprime;
		this.smallprime=smallprime;
		this.zoom=zoom;
	}
	public float noise(int x, int y)
	{
		int n = x + y * 57;
		n = (n<<13) ^ n;
		return ( ((n * (y * n * this.smallprime) + this.mediumprime) + this.bigprime)) / (float)(Integer.MAX_VALUE);  
	}

	private float smoothNoise(int x, int y)
	{
		float corners = ( noise(x-1, y-1)+noise(x+1, y-1)+noise(x-1, y+1)+noise(x+1, y+1) ) / 16;
		float sides   = ( noise(x-1, y)  +noise(x+1, y)  +noise(x, y-1)  +noise(x, y+1) ) /  8;
		float center  =  noise(x, y) / 4;
		return corners + sides + center;
	}

  	private float interpolatedNoise(float x, float y)
  	{
      int xint    = (int)(Math.floor(x));
      float xfrac = x - xint;

      int yint    = (int)(Math.floor(y));
      float yfrac = y - yint;

      float v1 = smoothNoise(xint,     yint);
      float v2 = smoothNoise(xint + 1, yint);
      float v3 = smoothNoise(xint,     yint + 1);
      float v4 = smoothNoise(xint + 1, yint + 1);

      float i1 = interpolate(v1 , v2 , xfrac);
      float i2 = interpolate(v3 , v4 , xfrac);

      return interpolate(i1 , i2 , yfrac);
  	}
  	private float interpolate(float a, float b, float x) {return  a*(1-x) + b*x;}
  	/*private float interpolate(float a, float b, float x)
  	{
  		float ft = x * FLOAT_PI;
  		float f = (1 - (float)(Math.cos(ft))) /2;

  		return  a*(1-f) + b*f;
  	}*/
  	public float getNoise(float x, float y)
  	{
  		return (interpolatedNoise(x/zoom, y/zoom));
  	}
  	public float getNormalizedNoise(float x,float y)
  	{
  		return (this.getNoise(x, y)+1)/2;
  	}
  	public static void main(String[] args)
	{
		double valTot=0;
		int[] div=new int[100];
		int out=0;
		SimplePerlinNoise pn=new SimplePerlinNoise(86457623,86457623,86457623,10);
		for(int w=0;w<2000;w++)
		{
			for(int h=0;h<2000;h++)
			{
				float noiseaux=(pn.getNoise(w,h)+1)/2;
				if(noiseaux>1||noiseaux<0) out++;
				else div[(int)(noiseaux*div.length)]++;
				valTot+=noiseaux/(2000*2000);
			}
		}
		System.out.println();
		System.out.println(valTot);
		for(int d=0;d<div.length;d++) System.out.println(d+" "+div[d]);
		System.out.println("OUT "+out);
	}
}

package ivengine.gen;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Perlin noise. Simplex noise is more efficient and better overall, so this class is considered deprecated. Anyways, is a completely valid and functional implementation,
 * but simplex noise simply beats it on each field.
 */
@Deprecated
public class PerlinNoise 
{
	private static final float PERSISTENCE=0.7f;
	private static final float zoom=60f;
	private static final float FLOAT_PI=(float)Math.PI;
	private static final int[] BIG_PRIME_LIST={179424691,179425907,179449687,179476993,179477461,179478037,
		179476471,	179477003,	179477509,	179478049,
		179476487,	179477009,	179477521,	179478053,
		179476537,	179477029,	179477527,	179478097};
	private static final int[] MEDIUM_PRIME_LIST={609407, 609421, 609437, 609443, 609461,
		 609487, 609503, 609509, 609517, 609527, 609533, 609541, 609571, 609589, 609593,
		 609599, 609601, 609607, 609613, 609617, 609619, 609641, 609673, 609683, 609701,
		 609709, 609743, 609751, 609757, 609779, 609781, 609803, 609809, 609821, 609859};
	private static final int[] SMALL_PRIME_LIST={67757,  67759,  67763,
		  67777,  67783,  67789,  67801,  67807,  67819,  67829,  67843,  67853,  67867,
		  67883,  67891,  67901,  67927,  67931,  67933,  67939,  67943,  67957,  67961,
		  67967,  67979,  67987,  67993,  68023,  68041,  68053,  68059,  68071,  68087};

	private int[][] noises=new int[4][3];
	public PerlinNoise(long seed)
	{
		long[] subseed=new long[4];
		subseed[0]=(seed); subseed[0]=Math.abs(subseed[0]<<42);
		subseed[1]=(seed>>16); subseed[1]=Math.abs(subseed[1]<<42);
		subseed[2]=(seed>>32); subseed[2]=Math.abs(subseed[2]<<42);
		subseed[3]=(seed>>48); subseed[3]=Math.abs(subseed[3]<<42);
		for(int s=0;s<subseed.length;s++)
		{
			noises[s][0]=BIG_PRIME_LIST[(int)(subseed[s]%BIG_PRIME_LIST.length)];
			noises[s][1]=MEDIUM_PRIME_LIST[(int)(subseed[s]/BIG_PRIME_LIST.length%MEDIUM_PRIME_LIST.length)];
			noises[s][2]=SMALL_PRIME_LIST[(int)(subseed[s]/(BIG_PRIME_LIST.length*MEDIUM_PRIME_LIST.length)%SMALL_PRIME_LIST.length)];
		}
		
	}
	private float noise(int x, int y,int wave)
	{
		int n = x + y * 57;
		n = (n<<13) ^ n;
		return ( (n * (n * n * this.noises[wave][2] + this.noises[wave][1]) + this.noises[wave][0])) / (float)(Integer.MAX_VALUE);  
	}

	private float smoothNoise(int x, int y,int wave)
	{
		float corners = ( noise(x-1, y-1,wave)+noise(x+1, y-1,wave)+noise(x-1, y+1,wave)+noise(x+1, y+1,wave) ) / 16;
		float sides   = ( noise(x-1, y,wave)  +noise(x+1, y,wave)  +noise(x, y-1,wave)  +noise(x, y+1,wave) ) /  8;
		float center  =  noise(x, y,wave) / 4;
		return corners + sides + center;
	}

  	private float interpolatedNoise(float x, float y,int wave)
  	{
      int xint    = (int)(Math.floor(x));
      float xfrac = x - xint;

      int yint    = (int)(Math.floor(y));
      float yfrac = y - yint;

      float v1 = smoothNoise(xint,     yint,wave);
      float v2 = smoothNoise(xint + 1, yint,wave);
      float v3 = smoothNoise(xint,     yint + 1,wave);
      float v4 = smoothNoise(xint + 1, yint + 1,wave);

      float i1 = interpolate(v1 , v2 , xfrac);
      float i2 = interpolate(v3 , v4 , xfrac);

      return interpolate(i1 , i2 , yfrac);
  	}
  	//private float interpolate(float a, float b, float x) {return  a*(1-x) + b*x;}
  	private float interpolate(float a, float b, float x)
  	{
  		float ft = x * FLOAT_PI;
  		float f = (1 - (float)(Math.cos(ft))) /2;

  		return  a*(1-f) + b*f;
  	}
  	public float getNoise(float x, float y)
  	{
  		float total = 0;
  		float p = PERSISTENCE;
  		int n = this.noises.length;
  		float contMax=0;
  		for(int i=1;i<=n;i++)
  		{
  			int frequency = 2*i;
  			float amplitude = p*i;
  			contMax+=amplitude;
  			total= total+(interpolatedNoise((x/zoom) * frequency, (y/zoom) * frequency,i-1) * amplitude);
      }
  		total=total/contMax;
      return total;

  	}
  	public float getNormalizedNoise(float x,float y)
  	{
  		return (this.getNoise(x, y)+1)/2;
  	}
	/*public static void main(String[] args)
	{
		double valTot=0;
		int[] div=new int[100];
		int out=0;
		PerlinNoise pn=new PerlinNoise(86457623);
		for(int w=0;w<2000;w++)
		{
			for(int h=0;h<2000;h++)
			{
				float noiseaux=(pn.PerlinNoise_2D(w,h)+1)/2;
				if(noiseaux>1||noiseaux<0) out++;
				else div[(int)(noiseaux*div.length)]++;
				valTot+=noiseaux/(2000*2000);
			}
		}
		System.out.println();
		System.out.println(valTot);
		for(int d=0;d<div.length;d++) System.out.println(d+" "+div[d]);
		System.out.println("OUT "+out);
	}*/
}

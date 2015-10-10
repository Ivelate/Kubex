package ivengine.gen;

public class PrimeFactory 
{
	private static final int[] SMALL_PRIMES={11819,13648,15477,17288,19103,20919,22742,24557,26362,28169,29974,31781,33594,35398,37204,39002,40798,42616,44411,46206,48005,49806,51605,53402,55196,57005,58804,60593,62387,64182,65975,67769,69555,71350,73150,74943,76727,78521,80313,82110,83896,85686,87469,89257,91051,92847,94638,96429,98208};
	private static final int[] MEDIUM_PRIMES={118180,136353,154512,172626,190752,208860,226956,245037,263100,281180,299210,317277,335349,353388,371399,389411,407425,425459,443491,461463,479484,497482,515463,533479,551446,569408,587419,605394,623368,641306,659268,677236,695202,713175,731121,749048,767019,784966,802920,820857,838804,856730,874671,892621,910558,928483,946401,964314,982223};
	private static final int[] BIG_PRIMES={1181181,1362098,1542951,1723646,1904291,2084831,2265299,2445639,2625969,2806235,2986449,3166596,3346735,3526706,3706755,3886725,4066666,4246552,4426383,4606232,4786020,4965867,5145590,5325295,5504969,5684530,5864143,6043805,6223353,6402920,6582460,6762009,6941539,7121065,7300502,7479910,7659320,7838684,8018090,8197427,8376772,8556151,8735514,8914787,9094042,9273351,9452592,9631854,9811079,9990191};
	public static int getRandomBigPrime(long val){
		return BIG_PRIMES[Math.abs((int)((17*val)+3)%BIG_PRIMES.length)];
	}
	public static int getRandomMediumPrime(long val){
		return MEDIUM_PRIMES[Math.abs((int)((31*val)+7)%MEDIUM_PRIMES.length)];
	}
	public static int getRandomSmallPrime(long val){
		return SMALL_PRIMES[Math.abs((int)((47*val)+5)%SMALL_PRIMES.length)];
	}
	/*public static void main(String[] args){
		int howmany=50;
		int tam=0;
		for(int p=1000000;p<10000000;p++){
			int to=(int) Math.sqrt(p);
			for(int v=2;v<=to;v++){
				if(p%v==0){
					tam++;
					break;
				}
			}
		}
		float amount=(float)howmany/tam;
		float amountc=0;
		for(int p=1000000;p<10000000;p++){
			int to=(int) Math.sqrt(p);
			for(int v=2;v<=to;v++){
				if(p%v==0){
					amountc+=amount;
					if(amountc>=1){
						amountc-=1;
						System.out.printf(p+",");
					}
					break;
				}
			}
		}
	}*/
}

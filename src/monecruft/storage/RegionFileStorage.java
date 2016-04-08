package monecruft.storage;

public class RegionFileStorage 
{
	private RegionFile[][][] files;
	
	private int sizex;
	private int sizey;
	private int sizez;
	
	public RegionFileStorage(int sizex, int sizey,int sizez)
	{
		this.sizex=sizex;
		this.sizey=sizey;
		this.sizez=sizez;
		this.files=new RegionFile[sizex][sizey][sizez];
	}
	private int posMod(int n,int mod)
	{
		int res=n%mod;
		if(res<0)res=res+mod;
		return res;
	}
	public RegionFile getRegionFile(int cx,int cy,int cz)
	{
		RegionFile candidate=this.files[posMod(cx,this.sizex)][posMod(cy,this.sizey)][posMod(cz,this.sizez)];
		
		if(candidate==null || candidate.x!=cx || candidate.y!=cy || candidate.z!=cz) return null;
		
		return candidate;	
	}
	public RegionFile setRegionFile(RegionFile file)
	{
		int posx=posMod(file.x,this.sizex);
		int posy=posMod(file.y,this.sizey);
		int posz=posMod(file.z,this.sizez);
		
		byte[] lookupCache=null;
		
		if(this.files[posx][posy][posz]!=null) {
			this.files[posx][posy][posz].fullClean();
			lookupCache=this.files[posx][posy][posz].lookupCache;
		}
		
		this.files[posx][posy][posz]=file;
		if(lookupCache==null) lookupCache=new byte[FileManager.LOOKUP_SIZE];

		this.files[posx][posy][posz].lookupCache=lookupCache;
		this.files[posx][posy][posz].storeLookupInCache();
		
		return file;
	}
	public void fullClean()
	{
		for(int x=0;x<this.sizex;x++)
		{
			for(int y=0;y<this.sizey;y++)
			{
				for(int z=0;z<this.sizez;z++)
				{
					if(this.files[x][y][z]!=null){
						this.files[x][y][z].fullClean();
						this.files[x][y][z]=null;
					}
				}
			}
		}
	}
}

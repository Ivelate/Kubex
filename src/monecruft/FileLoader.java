package monecruft;

import java.net.URL;

import org.newdawn.slick.util.ResourceLoader;

public class FileLoader 
{
	private static int IMAGE_NUMBER=16;
	private static String TILES_ROUTE="images/tiles/";
	public static URL[] loadTileImages()
	{
		URL[] tileImages=new URL[IMAGE_NUMBER];
		
		//System.out.println(FileLoader.class.getResource("/MyFile.png").getFile());
		tileImages[0]=FileLoader.class.getClassLoader().getResource(TILES_ROUTE+"000_hierba_lateral.png");
		tileImages[1]=FileLoader.class.getClassLoader().getResource(TILES_ROUTE+"001_hierba.png");
		tileImages[2]=FileLoader.class.getClassLoader().getResource(TILES_ROUTE+"002_tierra.png");
		tileImages[3]=FileLoader.class.getClassLoader().getResource(TILES_ROUTE+"003_piedra.png");
		tileImages[4]=FileLoader.class.getClassLoader().getResource(TILES_ROUTE+"004_lava.png");
		tileImages[5]=FileLoader.class.getClassLoader().getResource(TILES_ROUTE+"005_lava_lateral.png");
		tileImages[6]=FileLoader.class.getClassLoader().getResource(TILES_ROUTE+"006_madera.png");
		tileImages[7]=FileLoader.class.getClassLoader().getResource(TILES_ROUTE+"007_adoquin.png");
		tileImages[8]=FileLoader.class.getClassLoader().getResource(TILES_ROUTE+"008_planta.png");
		tileImages[9]=FileLoader.class.getClassLoader().getResource(TILES_ROUTE+"009_transparent.png");
		tileImages[10]=FileLoader.class.getClassLoader().getResource(TILES_ROUTE+"010_flores.png");
		tileImages[11]=FileLoader.class.getClassLoader().getResource(TILES_ROUTE+"011_tulipan.png");
		tileImages[12]=FileLoader.class.getClassLoader().getResource(TILES_ROUTE+"012_glass.png");
		tileImages[13]=FileLoader.class.getClassLoader().getResource(TILES_ROUTE+"013_lightblock.png");
		tileImages[14]=FileLoader.class.getClassLoader().getResource(TILES_ROUTE+"014_snow.png");
		tileImages[15]=FileLoader.class.getClassLoader().getResource(TILES_ROUTE+"015_snowlat.png");
		
		return tileImages;
	}
}

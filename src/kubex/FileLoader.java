package kubex;

import java.net.URL;

import org.newdawn.slick.util.ResourceLoader;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Contains methods to load the game resources from disk
 */
public class FileLoader 
{
	private static int IMAGE_NUMBER=23;
	private static String TILES_ROUTE="images/tiles/";
	private static String MISC_ROUTE="images/";
	
	/**
	 * Returns in an URL[] all game cube textures
	 */
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
		tileImages[10]=FileLoader.class.getClassLoader().getResource(TILES_ROUTE+"010_brick.png");
		tileImages[11]=FileLoader.class.getClassLoader().getResource(TILES_ROUTE+"011_sand.png");
		tileImages[12]=FileLoader.class.getClassLoader().getResource(TILES_ROUTE+"012_glass.png");
		tileImages[13]=FileLoader.class.getClassLoader().getResource(TILES_ROUTE+"013_lightblock.png");
		tileImages[14]=FileLoader.class.getClassLoader().getResource(TILES_ROUTE+"014_snow.png");
		tileImages[15]=FileLoader.class.getClassLoader().getResource(TILES_ROUTE+"015_clearbrick.png");
		tileImages[16]=FileLoader.class.getClassLoader().getResource(TILES_ROUTE+"016_flower_dandelion.png");
		tileImages[17]=FileLoader.class.getClassLoader().getResource(TILES_ROUTE+"017_stone_rusty.png");
		tileImages[18]=FileLoader.class.getClassLoader().getResource(TILES_ROUTE+"018_wood.png");
		tileImages[19]=FileLoader.class.getClassLoader().getResource(TILES_ROUTE+"019_leaves.png");
		tileImages[20]=FileLoader.class.getClassLoader().getResource(TILES_ROUTE+"020_wood_top.png");
		tileImages[21]=FileLoader.class.getClassLoader().getResource(TILES_ROUTE+"021_tnt.png");
		tileImages[22]=FileLoader.class.getClassLoader().getResource(TILES_ROUTE+"022_bedrock.png");
		
		return tileImages;
	}
	
	/**
	 * Returns the URL of the water normal texture
	 */
	public static URL loadWaterNormalImage()
	{
		return FileLoader.class.getClassLoader().getResource(MISC_ROUTE+"water_normal.png");
	}
}

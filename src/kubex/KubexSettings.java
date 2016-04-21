package kubex;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Contains the default game settings. Those settings will be loaded or stored in a file, to be reused in posterior executions
 */
public class KubexSettings 
{
	public boolean SHADOWS_ENABLED=true;
	public boolean ANISOTROPIC_FILTERING_ENABLED=true;
	public boolean REFLECTIONS_ENABLED=true;
	public boolean FULLSCREEN_ENABLED=false;
	public int MAP_CODE=0;
	public String MAP_ROUTE="";
	public long MAP_SEED=1234567890;
	public double PLAYER_X=1;
	public double PLAYER_Y=300;
	public double PLAYER_Z=1;
	public float DAY_TIME=12;
	public float CAM_PITCH=0;
	public float CAM_YAW=0;
	public int WINDOW_XRES=1400;
	public int WINDOW_YRES=900;
	public int RENDER_DISTANCE=10;
	public int WATER_LAYERS=3;
	public float MOUSE_SENSITIVITY=1;
	
	public byte CUBE_SELECTED=2;
	public byte[] CUBE_SHORTCUTS={(byte)4,(byte)24,(byte)25,(byte)3,(byte)12,(byte)13,(byte)16,(byte)17,(byte)26,(byte)14};
	
	public int DAY_SPEED=3;
}

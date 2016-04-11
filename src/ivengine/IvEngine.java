package ivengine;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

/**
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Wraps the needed opperations to create and configure a OpenGL display
 */
public class IvEngine 
{
	public static void configDisplay(int screenWidth,int screenHeight,String title,boolean VSYNC,boolean resizable,boolean fullscreen) throws LWJGLException
	{
		configDisplay(new DisplayMode(screenWidth,screenHeight),title,VSYNC,resizable,fullscreen);
	}
	public static void configDisplay(DisplayMode dm,String title,boolean VSYNC,boolean resizable,boolean fullscreen) throws LWJGLException
	{
		Display.setTitle(title); //title of our window
		Display.setResizable(resizable); //whether our window is resizable
		Display.setDisplayMode(dm); //resolution of our display
		Display.setVSyncEnabled(VSYNC); //whether hardware VSync is enabled
		Display.setFullscreen(fullscreen); //whether fullscreen is enabled
		
		Display.create();
	}
}

package kubex.menu;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.LinkedList;

import javax.swing.JFrame;
import javax.swing.JPanel;

import kubex.KubexSettings;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Menu main class, containing the JFrame and switching between different windows (JPanels) using a state system
 */
public class MonecruftMenu extends JFrame
{
	public enum MenuState{MAIN_MENU,CREATE_MAP,SHOW_CONTROLS,CLOSED,START}
	
	private final int X_RES=500;
	private final int Y_RES=400;
	
	private static Object lock = new Object(); //Lock
	private MenuState state; //Current menu state
	
	private boolean closed=false;
	private boolean canProcceed=false;
	
	private JPanel currentVisibleWindow=null;
	
	private KubexSettings properties;
	private File mapFolder;
	
	public MonecruftMenu(KubexSettings properties,File mapFolder)
	{
		this.properties=properties;
		this.mapFolder=mapFolder;
		
		Dimension size=new Dimension(X_RES,Y_RES);
		Toolkit tk=Toolkit.getDefaultToolkit();
		Dimension screenSize=tk.getScreenSize();
		this.setSize(size);
		setBounds((screenSize.width-this.getWidth())/2,(screenSize.height-this.getHeight())/2,this.getWidth(),this.getHeight());
		
		setTitle("Kubex Launcher");
		setResizable(false);
		
		//Shutdown hook
		//Strong reference to... me
		final MonecruftMenu me=this;
		addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
            	me.setVisible(false);
                me.setState(MenuState.CLOSED); //If the window closes, sets the window state to CLOSED. It will free the main thread of the menu lock, and it will dispose the menu
                							   //in a controlled way
                e.getWindow().dispose();
            }
        });
		
		setState(MenuState.MAIN_MENU);
		setVisible(true);
	}
	
	/**
	 * Sets the current menu state to <state>. In function of which state is selected, some actions or others will be performed
	 */
	public void setState(MenuState state)
	{
		this.state=state;
		
		switch(state)
		{
		case MAIN_MENU: //Opens the main menu screen
			//Gets all the kubex world maps existing in the maps folder
			LinkedList<String> maps=new LinkedList<String>();
			for(File f:this.mapFolder.listFiles())
			{
				if(f.isDirectory()&&(new File(f,"settings.txt")).exists()) maps.add(f.getName());
			}
			String[] mapList=new String[maps.size()];
			maps.toArray(mapList);
			setCurrentWindow(new MenuMainWindow(this,this.properties,mapFolder,mapList));
			break;
		case CREATE_MAP: //Opens the create map screen
			setCurrentWindow(new MenuCreateMapWindow(this,this.properties,mapFolder));
			break;
		case CLOSED: //Closes the game
			synchronized(lock)
			{
				fullClean();
				lock.notifyAll();
			}
			break;
		case START: //Starts the game
			synchronized(lock)
			{
				fullClean();
				canProcceed=true;
				lock.notifyAll();
			}
			break;
		case SHOW_CONTROLS: //Shows the controls window
			setCurrentWindow(new MenuShowControlsWindow(this));
			break;
		}
	}

	/**
	 * Sets the current visible window to <window>. The current one will be disposed.
	 */
	private void setCurrentWindow(JPanel window)
	{
		this.setContentPane(window);
		if(this.currentVisibleWindow!=null) this.currentVisibleWindow.invalidate();
		
		this.currentVisibleWindow=window;
		this.currentVisibleWindow.validate();
	}
	
	/**
	 * Disposes the menu
	 */
	public void fullClean()
	{
		this.closed=true;
		this.setVisible(false);
		this.dispose();
	}
	
	/**
	 * Lock method that other classes can use to wait for the menu to close. Returns true if the game can start and false if not.
	 */
	public boolean waitForClose()
	{
		synchronized(lock)
		{
			while(!this.closed)
			{
				try {
					lock.wait();
				} catch (InterruptedException e) {}
			}
		}
		
		return this.canProcceed;
	}
}



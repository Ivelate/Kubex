package monecruft.menu;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.LinkedList;

import javax.swing.JFrame;
import javax.swing.JPanel;

import monecruft.MonecruftSettings;

public class MonecruftMenu extends JFrame
{
	public enum MenuState{MAIN_MENU,CREATE_MAP,CLOSED,START}
	
	private final int X_RES=500;
	private final int Y_RES=400;
	
	private static Object lock = new Object(); //Lock
	private MenuState state;
	
	private boolean closed=false;
	private boolean canProcceed=false;
	
	private JPanel currentVisibleWindow=null;
	
	private MonecruftSettings properties;
	private File mapFolder;
	
	public MonecruftMenu(MonecruftSettings properties,File mapFolder)
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
                me.setState(MenuState.CLOSED);
                e.getWindow().dispose();
            }
        });
		
		setState(MenuState.MAIN_MENU);
		setVisible(true);
	}
	
	public void setState(MenuState state)
	{
		this.state=state;
		
		switch(state)
		{
		case MAIN_MENU:
			LinkedList<String> maps=new LinkedList<String>();
			for(File f:this.mapFolder.listFiles())
			{
				if(f.isDirectory()&&(new File(f,"settings.txt")).exists()) maps.add(f.getName());
			}
			String[] mapList=new String[maps.size()];
			maps.toArray(mapList);
			setCurrentWindow(new MenuMainWindow(this,this.properties,mapFolder,mapList));
			break;
		case CREATE_MAP:
			setCurrentWindow(new MenuCreateMapWindow(this,this.properties,mapFolder));
			break;
		case CLOSED:
			synchronized(lock)
			{
				fullClean();
				lock.notifyAll();
			}
			break;
		case START:
			synchronized(lock)
			{
				fullClean();
				canProcceed=true;
				lock.notifyAll();
			}
			break;
		}
	}

	private void setCurrentWindow(JPanel window)
	{
		this.setContentPane(window);
		if(this.currentVisibleWindow!=null) this.currentVisibleWindow.invalidate();
		
		this.currentVisibleWindow=window;
		this.currentVisibleWindow.validate();
	}
	
	public void fullClean()
	{
		this.closed=true;
		this.setVisible(false);
		this.dispose();
	}
	
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



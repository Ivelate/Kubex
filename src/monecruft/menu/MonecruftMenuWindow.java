package monecruft.menu;

import javax.swing.JPanel;

import monecruft.MonecruftSettings;

public abstract class MonecruftMenuWindow extends JPanel
{
	private MonecruftSettings monecruftSettings;
	private MonecruftMenu monecruftMenu;
	
	public MonecruftMenuWindow(MonecruftSettings ms,MonecruftMenu mm)
	{
		this.monecruftSettings=ms;
		this.monecruftMenu=mm;
	}
	
	protected MonecruftSettings getMonecruftSettings()
	{
		return this.monecruftSettings;
	}
	
	protected MonecruftMenu getMonecruftMenu()
	{
		return this.monecruftMenu;
	}
}

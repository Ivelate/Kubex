package monecruft.menu;

import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JSeparator;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;

import monecruft.MonecruftSettings;
import monecruft.menu.MonecruftMenu.MenuState;

import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.ImageIcon;
import javax.swing.DefaultComboBoxModel;

public class MenuMainWindow extends JPanel 
{
	/**
	 * Create the panel.
	 */
	private static class Resolution
	{
		public final int x, y;
		public Resolution(int x,int y){
			this.x=x;
			this.y=y;
		}
	}
	private static final Resolution[] SUPPORTED_RESOLUTIONS={
			new Resolution(800,600),
			new Resolution(1024,768),
			new Resolution(1280,1024),
			new Resolution(1366,768),
			new Resolution(1920,1080)
	};
	private static final String[] SUPPORTED_RESOLUTIONS_TEXT_TABLE=createSupportedResolutionsTable(SUPPORTED_RESOLUTIONS);
	
	
	private JCheckBox chckbxFullScreen;
	private JCheckBox chckbxDisableShadows;
	private JCheckBox chckbxAnisotropic;
	private JCheckBox chckbxNewCheckBox;
	private JComboBox<String> comboBox;
	private File mapsFolder;
	private JComboBox<String> windowResComboBox;
	private JSpinner renderDistanceSpinner;
	
	public MenuMainWindow(MonecruftMenu monecruftMenu,MonecruftSettings settings,File mapsFolder,String[] maps) 
	{
		this.mapsFolder=mapsFolder;
		setSize(500,370);
				setLayout(null);
		
		JButton btnCreateNewMap = new JButton("Create new map");
		btnCreateNewMap.setBounds(309, 309, 181, 23);
		btnCreateNewMap.addActionListener(new ActionListener() {
		       public void actionPerformed(ActionEvent ae){
		    	   storeSettings(settings);
		           monecruftMenu.setState(MenuState.CREATE_MAP);
		       }
		      });
		add(btnCreateNewMap);
		
		comboBox = new JComboBox<String>(maps);
		comboBox.setBounds(309, 244, 181, 20);
		int matchIndex=-1;
		String defaultMapName=settings.MAP_ROUTE.substring(settings.MAP_ROUTE.lastIndexOf("\\")+1);
		for(int i=0;i<maps.length;i++)
		{
			if(maps[i].equals(defaultMapName)){
				matchIndex=i;
				break;
			}
		}
		if(matchIndex>=0) comboBox.setSelectedIndex(matchIndex);
		add(comboBox);
		
		windowResComboBox = new JComboBox<String>();
		windowResComboBox.setModel(new DefaultComboBoxModel(SUPPORTED_RESOLUTIONS_TEXT_TABLE));
		windowResComboBox.setEnabled(!settings.FULLSCREEN_ENABLED);
		//Lets see what resolution we have stored
		int res=0;
		for(int i=0;i<SUPPORTED_RESOLUTIONS.length;i++)
		{
			if(	SUPPORTED_RESOLUTIONS[i].x == settings.WINDOW_XRES && 
				SUPPORTED_RESOLUTIONS[i].y == settings.WINDOW_YRES){
				res=i;
				break;
			}
		}
		windowResComboBox.setSelectedIndex(res);
		windowResComboBox.setBounds(135, 227, 98, 20);
		add(windowResComboBox);
		
		JLabel lblWindowResolution = new JLabel("Window resolution");
		lblWindowResolution.setBounds(10, 230, 123, 14);
		lblWindowResolution.setEnabled(!settings.FULLSCREEN_ENABLED);
		add(lblWindowResolution);
		
		chckbxFullScreen = new JCheckBox("Full Screen");
		chckbxFullScreen.setBounds(6, 283, 98, 23);
		chckbxFullScreen.setSelected(settings.FULLSCREEN_ENABLED);
		chckbxFullScreen.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
               if(e.getStateChange() == ItemEvent.SELECTED)
               {
            	   windowResComboBox.setEnabled(false);
            	   lblWindowResolution.setEnabled(false);
               }
               else {
            	   windowResComboBox.setEnabled(true);
            	   lblWindowResolution.setEnabled(true);
               }
            }
        });
		add(chckbxFullScreen);
		
		chckbxDisableShadows = new JCheckBox("Shadows");
		chckbxDisableShadows.setBounds(135, 309, 153, 23);
		chckbxDisableShadows.setSelected(settings.SHADOWS_ENABLED);
		add(chckbxDisableShadows);
		
		SpinnerNumberModel model1=new SpinnerNumberModel(10,3,20,1);
		renderDistanceSpinner = new JSpinner(model1);
		renderDistanceSpinner.setBounds(194, 256, 39, 20);
		renderDistanceSpinner.setValue(settings.RENDER_DISTANCE);
		add(renderDistanceSpinner);
		
		chckbxNewCheckBox = new JCheckBox("Reflections");
		chckbxNewCheckBox.setBounds(6, 309, 127, 23);
		chckbxNewCheckBox.setSelected(settings.REFLECTIONS_ENABLED);
		add(chckbxNewCheckBox);
		
		chckbxAnisotropic = new JCheckBox("Anisotropic filtering");
		chckbxAnisotropic.setBounds(135, 283, 153, 23);
		chckbxAnisotropic.setSelected(settings.ANISOTROPIC_FILTERING_ENABLED);
		add(chckbxAnisotropic);
		
		JLabel lblNewLabel = new JLabel("Kubex v1.0");
		lblNewLabel.setFont(new Font("Tahoma", Font.PLAIN, 44));
		lblNewLabel.setBounds(135, 45, 260, 84);
		add(lblNewLabel);
		

		JButton btnLoadSelectedMap = new JButton("Load selected map");
		btnLoadSelectedMap.setBounds(309, 275, 181, 23);
		btnLoadSelectedMap.addActionListener(new ActionListener() {
		       public void actionPerformed(ActionEvent ae){
		    	   storeSettings(settings);
		           monecruftMenu.setState(MenuState.START);
		       }
		    });
		add(btnLoadSelectedMap);
		
		JButton btnNewButton = new JButton(new ImageIcon(MenuMainWindow.class.getResource("/images/round_delete.png")));

		//btnNewButton.setForeground(Color.RED);
		//btnNewButton.setBackground(Color.BLACK);
		btnNewButton.setBounds(278, 244, 20, 20);
		btnNewButton.addActionListener(new ActionListener() {
		       public void actionPerformed(ActionEvent ae){
		    	   int res=JOptionPane.showConfirmDialog((Component) null, "If you accept, world "+(String)comboBox.getSelectedItem()+" will be deleted permanently\nDo you wish to continue?",
		    		        "Alerta", JOptionPane.OK_CANCEL_OPTION);
		    	   if(res==0)
		    	   {
		    		   //Delete
		    		   File f=new File(mapsFolder,(String)comboBox.getSelectedItem());
		    		   for(File fil:f.listFiles())
		    		   {
		    			   fil.delete();
		    		   }
		    		   f.delete();
		    		   //Reload menu
		    		   monecruftMenu.setState(MenuState.MAIN_MENU);
		    	   }
		       }
		    });
		add(btnNewButton);
		
		JLabel lblRenderDistance = new JLabel("Render distance (Chunks): ");
		lblRenderDistance.setBounds(10, 259, 181, 14);
		add(lblRenderDistance);
		
		if(maps.length==0) btnLoadSelectedMap.setEnabled(false);
		if(maps.length==0) btnNewButton.setEnabled(false);
	}

	private void storeSettings(MonecruftSettings settings)
	{
	   settings.ANISOTROPIC_FILTERING_ENABLED=this.chckbxAnisotropic.isSelected();
 	   settings.FULLSCREEN_ENABLED=chckbxFullScreen.isSelected();
 	   settings.SHADOWS_ENABLED=chckbxDisableShadows.isSelected();
 	   settings.REFLECTIONS_ENABLED=chckbxNewCheckBox.isSelected();
 	   settings.WINDOW_XRES=SUPPORTED_RESOLUTIONS[windowResComboBox.getSelectedIndex()].x;
 	   settings.WINDOW_YRES=SUPPORTED_RESOLUTIONS[windowResComboBox.getSelectedIndex()].y;
 	   settings.RENDER_DISTANCE=(int)renderDistanceSpinner.getValue();
 	  
 	   try
 	   {
 		   settings.MAP_ROUTE=(new File(mapsFolder,(String)comboBox.getSelectedItem())).getPath();
 	   }
 	   catch(Exception e){} //If a exception is catched, there was some problem storing the map route... we ignore it
	}
	
	private static String[] createSupportedResolutionsTable(Resolution[] supportedResolutions) {
		String[] ret=new String[supportedResolutions.length];
		
		for(int i=0;i<ret.length;i++)
		{
			ret[i]=supportedResolutions[i].x+"x"+supportedResolutions[i].y;
		}
		
		return ret;
	}
}

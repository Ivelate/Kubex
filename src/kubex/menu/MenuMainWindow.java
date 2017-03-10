package kubex.menu;

import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JSeparator;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import kubex.KubexSettings;
import kubex.menu.MonecruftMenu.MenuState;

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
import javax.swing.SpinnerModel;
import javax.swing.JSlider;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Menu class, window "Main Window". Created using WindowBuilder
 */
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
	private static final Resolution[] SUPPORTED_RESOLUTIONS={ //Supported window resolutions list. More can be added, but those are the most common in the market
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
	private JSpinner spinnerWaterLayers;
	private JSlider sliderMouse;
	
	public MenuMainWindow(MonecruftMenu monecruftMenu,KubexSettings settings,File mapsFolder,String[] maps) 
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
		
		JButton btnControls = new JButton("Controls");
		btnControls.setBounds(405, 11, 85, 23);
		btnControls.addActionListener(new ActionListener() {
		       public void actionPerformed(ActionEvent ae){
		    	   storeSettings(settings);
		    	   monecruftMenu.setState(MenuState.SHOW_CONTROLS);
		       }
		    });
		add(btnControls);
		
		windowResComboBox = new JComboBox<String>();
		windowResComboBox.setEnabled(!settings.FULLSCREEN_ENABLED);
		//Lets see what resolution we have stored
		int res=SUPPORTED_RESOLUTIONS.length;
		for(int i=0;i<SUPPORTED_RESOLUTIONS.length;i++)
		{
			if(	SUPPORTED_RESOLUTIONS[i].x == settings.WINDOW_XRES && 
				SUPPORTED_RESOLUTIONS[i].y == settings.WINDOW_YRES){
				res=i;
				break;
			}
		}
		String[] resTable=res==SUPPORTED_RESOLUTIONS.length?addIndexToTable(SUPPORTED_RESOLUTIONS_TEXT_TABLE,	settings.WINDOW_XRES+"x"+settings.WINDOW_YRES):SUPPORTED_RESOLUTIONS_TEXT_TABLE;
		windowResComboBox.setModel(new DefaultComboBoxModel(resTable));
		windowResComboBox.setSelectedIndex(res);
		windowResComboBox.setBounds(135, 198, 98, 20);
		add(windowResComboBox);
		
		JLabel lblWindowResolution = new JLabel("Window resolution");
		lblWindowResolution.setBounds(10, 201, 123, 14);
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
		
		SpinnerNumberModel model1=new SpinnerNumberModel(10,3,30,1);
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
		
		//Storing settings and starting the game
		btnLoadSelectedMap.addActionListener(new ActionListener() {
		       public void actionPerformed(ActionEvent ae){
		    	   storeSettings(settings);
		           monecruftMenu.setState(MenuState.START);
		       }
		    });
		add(btnLoadSelectedMap);
		
		JButton btnNewButton = new JButton(new ImageIcon(MenuMainWindow.class.getResource("/images/round_delete.png")));

		btnNewButton.setBounds(278, 244, 20, 20);
		btnNewButton.addActionListener(new ActionListener() {
		       public void actionPerformed(ActionEvent ae){
		    	   int res=JOptionPane.showConfirmDialog((Component) null, "If you accept, world "+(String)comboBox.getSelectedItem()+" will be deleted permanently\nDo you wish to continue?",
		    		        "Alert", JOptionPane.OK_CANCEL_OPTION);
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
		
		SpinnerNumberModel model2=new SpinnerNumberModel(3,1,30,2);
		spinnerWaterLayers = new JSpinner(model2);
		spinnerWaterLayers.setBounds(194, 229, 39, 20);
		spinnerWaterLayers.setValue(settings.WATER_LAYERS);
		add(spinnerWaterLayers);
		
		JLabel lblSimultaneousWaterLayers = new JLabel("Simultaneous Water Layers");
		lblSimultaneousWaterLayers.setBounds(10, 232, 181, 14);
		add(lblSimultaneousWaterLayers);
		
		JLabel label = new JLabel(settings.MOUSE_SENSITIVITY+"");
		label.setEnabled(true);
		label.setBounds(235, 170, 45, 14);
		add(label);
		
		int initialValue=Math.round(settings.MOUSE_SENSITIVITY*100);
		if(initialValue>200)initialValue=200;
		else if(initialValue<25) initialValue=25;
		sliderMouse = new JSlider(25,200,initialValue);
		sliderMouse.setBounds(135, 167, 98, 23);
		sliderMouse.addChangeListener(new ChangeListener() {
	        public void stateChanged(ChangeEvent ce) {
	            JSlider slider = (JSlider)ce.getSource();
	            	label.setText(slider.getValue()/(float)(100)+"");
	        }
	    });
		add(sliderMouse);
		
		JLabel lblMouseSensibility = new JLabel("Mouse Sensitivity");
		lblMouseSensibility.setEnabled(true);
		lblMouseSensibility.setBounds(10, 170, 123, 14);
		add(lblMouseSensibility);
		
		if(maps.length==0) btnLoadSelectedMap.setEnabled(false);
		if(maps.length==0) btnNewButton.setEnabled(false);
	}

	/**
	 * Stores all menu settings in <settings> in the default settings file
	 */
	private void storeSettings(KubexSettings settings)
	{
	   settings.ANISOTROPIC_FILTERING_ENABLED=this.chckbxAnisotropic.isSelected();
 	   settings.FULLSCREEN_ENABLED=chckbxFullScreen.isSelected();
 	   settings.SHADOWS_ENABLED=chckbxDisableShadows.isSelected();
 	   settings.REFLECTIONS_ENABLED=chckbxNewCheckBox.isSelected();
 	   if(!(windowResComboBox.getSelectedIndex()>=SUPPORTED_RESOLUTIONS.length)){
 	 	   settings.WINDOW_XRES=SUPPORTED_RESOLUTIONS[windowResComboBox.getSelectedIndex()].x;
 	 	   settings.WINDOW_YRES=SUPPORTED_RESOLUTIONS[windowResComboBox.getSelectedIndex()].y;
 	   }
 	   settings.RENDER_DISTANCE=(int)renderDistanceSpinner.getValue();
 	   settings.WATER_LAYERS=(int)spinnerWaterLayers.getValue();
 	   settings.MOUSE_SENSITIVITY=sliderMouse.getValue()/(float)(100);
 	  
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
	private static String[] addIndexToTable(String[] table,String index) {
		String[] ret=new String[table.length + 1];
		
		for(int i=0;i<table.length;i++)
		{
			ret[i]=table[i];
		}
		ret[table.length]=index;
		
		return ret;
	}
}

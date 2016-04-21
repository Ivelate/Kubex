package kubex.menu;

import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JSeparator;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;

import kubex.KubexSettings;
import kubex.menu.MonecruftMenu.MenuState;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Menu class, window "Create Map". Created using WindowBuilder
 */
public class MenuCreateMapWindow extends JPanel 
{
	private static final String[] MAP_TYPES={"Islands","Snowy hills","Plains","Buggy caves","Floating World","Underwater ruins"/*,"Deep Caves"*/};
	private static final String[] MAP_DESCRIPTIONS={"Default map. Large islands with mountains and hills.<br><font color=\"green\">Load time: LOW</font>",
													"High height map, more escarpate, with some lakes<br><font color=\"green\">Load time: LOW</font>",
													"Plain map, sabanna-like<br><font color=\"green\">Load time: LOW</font>",
													"Buggy 3D cave map, some terrain irregularities makes it unique.<br><font color=\"orange\">Load time: MEDIUM-HIGH</font>",
													"World with floating islands.<br><font color=\"red\">Load time: HIGH</font>",
													"Island map, with underwater caves, and lights.<br><font color=\"red\">Load time: HIGH</font>"
	};
	private JTextField mapNameField;
	private JTextField mapSeedField;
	/**
	 * Create the panel.
	 */
	public MenuCreateMapWindow(final MonecruftMenu monecruftMenu,final KubexSettings settings,File baseFolder) 
	{
		setSize(500,370);
				setLayout(null);
		
		JButton btnBack = new JButton("Back");
		btnBack.setBounds(10, 336, 87, 23);
		btnBack.addActionListener(new ActionListener() {
		       public void actionPerformed(ActionEvent ae){
		           monecruftMenu.setState(MenuState.MAIN_MENU);
		       }
		      });
		add(btnBack);
		
		JLabel mapDescriptionLabel = new JLabel("<html>"+MAP_DESCRIPTIONS[0]+"</html>");
		mapDescriptionLabel.setBounds(285, 194, 205, 50);
		mapDescriptionLabel.setVerticalAlignment(JLabel.TOP);
		add(mapDescriptionLabel);
		
		JComboBox<String> mapTypeComboBox = new JComboBox<String>(MAP_TYPES);
		mapTypeComboBox.setBounds(94, 194, 181, 20);
		mapTypeComboBox.addActionListener(new ActionListener() {
		       public void actionPerformed(ActionEvent ae){
		    	   mapDescriptionLabel.setText("<html>"+MAP_DESCRIPTIONS[mapTypeComboBox.getSelectedIndex()]+"</html>");
		       }
		});
		add(mapTypeComboBox);
		
		JLabel lblNewLabel = new JLabel("Kubex v1.0");
		lblNewLabel.setFont(new Font("Tahoma", Font.PLAIN, 44));
		lblNewLabel.setBounds(135, 45, 260, 84);
		add(lblNewLabel);
		
		JLabel lblNewLabel_1 = new JLabel("Map Type");
		lblNewLabel_1.setBounds(21, 197, 76, 14);
		add(lblNewLabel_1);
		
		JLabel lblMapName = new JLabel("Map Name");
		lblMapName.setBounds(21, 233, 76, 14);
		add(lblMapName);
		
		JLabel lblMapSeed = new JLabel("Map Seed");
		lblMapSeed.setBounds(21, 271, 76, 14);
		add(lblMapSeed);
		
		mapNameField = new JTextField();
		mapNameField.setBounds(94, 230, 181, 20);
		add(mapNameField);
		mapNameField.setColumns(10);
		
		mapSeedField = new JTextField();
		mapSeedField.setColumns(10);
		mapSeedField.setBounds(94, 268, 181, 20);
		add(mapSeedField);
		
		JLabel lblMapCreationWindow = new JLabel("Map Creation Window");
		lblMapCreationWindow.setFont(new Font("Tahoma", Font.PLAIN, 18));
		lblMapCreationWindow.setBounds(21, 140, 224, 31);
		add(lblMapCreationWindow);
		
		
		JButton btnCreateNewMap = new JButton("Create map");
		btnCreateNewMap.setBounds(309, 314, 181, 45);
		
		//Creating map!
		btnCreateNewMap.addActionListener(new ActionListener() {
		       public void actionPerformed(ActionEvent ae){
		    	   //Validate inputs
		    	   boolean valid=true;
		    	   String errorMsg="";
		    	   int mapType=0;
		    	   String mapName="";
		    	   long seed=0;
		    	   try
		    	   {
		    		   mapType=mapTypeComboBox.getSelectedIndex();
		    		   mapName=mapNameField.getText();
		    		   if(mapName.trim().isEmpty()){ //Map name cant be empty
		    			   errorMsg=errorMsg+"Map name is empty\n";
		    			   valid=false;
		    		   }
		    		   //Map name can't have invalid characters
		    		   //Thanks to http://stackoverflow.com/questions/1976007/what-characters-are-forbidden-in-windows-and-linux-directory-names
		    		   else if(mapName.contains("/")||mapName.contains("\\")||mapName.contains("<")||mapName.contains(">")||mapName.contains(":")||mapName.contains("\"")||mapName.contains("|")||mapName.contains("*")||mapName.contains("?")) {
		    			   errorMsg=errorMsg+"Map name contains any of those invalid characters: / \\ < > : \" | * ?\n";
		    			   valid=false;
		    		   }
		    		   
		    		   if(valid)
		    		   {
		    			   File mapFile=new File(baseFolder,mapName);
		    			   if(mapFile.exists()){ //Map name has to be unique
		    				   errorMsg=errorMsg+"Map name already in use\n";
		    				   valid=false;
		    			   }
		    			   //If the seed is a long, perse it "as it is"
		    			   try{
		    				   seed=Long.parseLong(mapSeedField.getText());
		    			   }
		    			   //If it isn't, the seed will be the hash code of the word
		    			   catch(NumberFormatException e){
		    				   seed=mapSeedField.getText().hashCode();
		    			   }
		    			   
		    			   if(valid)
		    			   {
			    			   if(mapFile.mkdir()){
			    				   mapFile.delete();
			    			   }
			    			   else{
			    				   valid=false;
			    				   errorMsg=errorMsg+"Unable to create map folder, try choosing another map name\n";
			    			   }
			    			   mapName=mapFile.getPath();
		    			   }
		    		   }
		    	   }
		    	   catch(Exception e){
		    		   errorMsg=errorMsg+"Exception'd\n";
		    		   valid=false;
		    	   }
		    	  
		    	   //If no error happened we create the world
		    	   if(valid) {
		    		   settings.MAP_CODE=mapType;
		    		   settings.MAP_SEED=seed;
		    		   settings.MAP_ROUTE=mapName;
		    		   if(settings.MAP_CODE>3) settings.PLAYER_Y=1000;
		    		   monecruftMenu.setState(MenuState.START);
		    	   }
		    	   //If some error happened, the map can't be created and we show it
		    	   else JOptionPane.showMessageDialog(null, "Invalid map: \n"+errorMsg,"Error creating map",JOptionPane.WARNING_MESSAGE);
		       }
		      });
		add(btnCreateNewMap);
		
		JButton btnRandomizeSeed = new JButton("Randomize seed");
		btnRandomizeSeed.setBounds(285, 267, 139, 23);
		btnRandomizeSeed.addActionListener(new ActionListener() {
		       public void actionPerformed(ActionEvent ae){
		    	   mapSeedField.setText(""+(long)(Math.random()*Long.MAX_VALUE));
		       }
		});
		add(btnRandomizeSeed);
	}
}

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
 * Menu class, window "Show Controls". Created using WindowBuilder
 */
public class MenuShowControlsWindow extends JPanel 
{
	/**
	 * Create the panel.
	 */
	public MenuShowControlsWindow(final MonecruftMenu monecruftMenu) 
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

		
		JLabel lblNewLabel = new JLabel("Kubex v1.0");
		lblNewLabel.setFont(new Font("Tahoma", Font.PLAIN, 44));
		lblNewLabel.setBounds(135, 45, 260, 84);
		add(lblNewLabel);
		
		JLabel lblNewLabel_1 = new JLabel("WASD: Player Movement");
		lblNewLabel_1.setBounds(21, 197, 204, 14);
		add(lblNewLabel_1);
		
		JLabel lblMapCreationWindow = new JLabel("Controls");
		lblMapCreationWindow.setFont(new Font("Tahoma", Font.PLAIN, 18));
		lblMapCreationWindow.setBounds(21, 140, 224, 31);
		add(lblMapCreationWindow);
		
		JLabel lblSpaceJump = new JLabel("SPACE: Jump");
		lblSpaceJump.setBounds(21, 222, 204, 14);
		add(lblSpaceJump);
		
		JLabel lblShiftToggleFlying = new JLabel("SHIFT: Toggle flying mode ON/OFF");
		lblShiftToggleFlying.setBounds(21, 247, 204, 14);
		add(lblShiftToggleFlying);
		
		JLabel lblMouseWheelUpdown = new JLabel("MOUSE WHEEL UP/DOWN: Select cube");
		lblMouseWheelUpdown.setBounds(21, 272, 308, 14);
		add(lblMouseWheelUpdown);
		
		JLabel lblMouseLeftClick = new JLabel("MOUSE LEFT CLICK: Place selected cube");
		lblMouseLeftClick.setBounds(235, 197, 255, 14);
		add(lblMouseLeftClick);
		
		JLabel lblMouseRightClick = new JLabel("MOUSE RIGHT CLICK: Remove cube");
		lblMouseRightClick.setBounds(235, 222, 255, 14);
		add(lblMouseRightClick);
		
		JLabel lblSelect = new JLabel("0..9 : Select cube assigned to shortcut");
		lblSelect.setBounds(235, 247, 255, 14);
		add(lblSelect);
		
		JLabel lblCtrl = new JLabel("CTRL + 0..9: Assign selected cube to shortcut");
		lblCtrl.setBounds(21, 297, 345, 14);
		add(lblCtrl);
	}
}

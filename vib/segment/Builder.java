package vib.segment;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class Builder extends JPanel {

	public Builder() {
		
	}
	
	public JButton makeJButton(String name, GridBagLayout layout, GridBagConstraints constr, JPanel pan, ActionListener listener, Color bkgColor) {
		JButton button = new JButton(name);
		button.addActionListener(listener);
		button.setBackground(bkgColor);
		layout.setConstraints(button, constr);
		pan.add(button);
		return button;
	}
	
	public JButton makeJButton(ImageIcon icon, GridBagLayout layout, GridBagConstraints constr, JPanel pan, ActionListener listener, Color bkgColor) {
		JButton button = new JButton(icon);
		button.addActionListener(listener);
		button.setBackground(bkgColor);
		layout.setConstraints(button, constr);
		pan.add(button);
		return button;
	}
	
	public JLabel makeJLabel(String txt, GridBagLayout layout, GridBagConstraints constr, JPanel pan, Color bkgColor) {
		JLabel label = new JLabel(txt);
		label.setBackground(bkgColor);
		layout.setConstraints(label, constr);
		pan.add(label);
		return label;
	}
	
	public JScrollPane makeJScrollPane (GridBagLayout layout, GridBagConstraints constr, JPanel panContainer, JList listContent, Color bkgColor) {
		JScrollPane scroll = new JScrollPane(listContent);
		scroll.setSize(new Dimension(160, 140));
		scroll.setPreferredSize(new Dimension(160, 140));
		scroll.setMaximumSize(new Dimension(160, 140));
		scroll.setBackground(bkgColor);
		layout.setConstraints(scroll, constr);
		panContainer.add(scroll);
		return scroll;
	}
	
	public Scrollbar makeScrollbar (GridBagLayout layout, GridBagConstraints constr, JPanel pan, int orientation, int value, int visible, int minimum, int maximum, Color bkgColor) {
		Scrollbar scroll = new Scrollbar(orientation, value, visible, minimum, maximum);
		scroll.setBackground(bkgColor);
		layout.setConstraints(scroll, constr);
		pan.add(scroll);
		return scroll;
	}
	
	public JTextField makeJTextField (String init, GridBagLayout layout, GridBagConstraints constr, JPanel pan, Color bkgColor) {
		JTextField text = new JTextField(init);
		text.setBackground(bkgColor);
		layout.setConstraints(text, constr);
		pan.add(text);
		return text;
	}

	public JCheckBox makeJCheckBox (String text, boolean selected, GridBagLayout layout, GridBagConstraints constr, JPanel pan, Color bkgColor) {
		JCheckBox check = new JCheckBox(text, selected);
		check.setBackground(bkgColor);
		layout.setConstraints(check, constr);
		pan.add(check);
		return check;
	}
}

/*
 * Created on 29-May-2006
 */
package gui;


import ij.IJ;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

public class GuiBuilder {
	public static JComponent createField(String label, String actionCommand,
			ActionListener controllor) {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		panel.add(new JLabel(label));

		JTextField b = new JTextField();
		panel.add(b);

		b.addActionListener(controllor);

		return panel;
	}

	public static JSpinner addLabeledNumericSpinner(Container c, String label, int initial,
			int min, int max)
	{
		SpinnerModel model = new SpinnerNumberModel(
				initial, // initial value
				min, // min
				max, // max
				1); // step
		JSpinner spinner = addLabeledSpinner(c, label, model);		
		
		return spinner;
	}
	
	public static JSpinner addLabeledSpinner(Container c, String label,
			SpinnerModel model) {
		JLabel l = new JLabel(label);
		c.add(l);

		JSpinner spinner = new JSpinner(model);
		l.setLabelFor(spinner);
		c.add(spinner);

		return spinner;
	}

	public static void addCommand(Container c, String label, String actionCmd, ActionListener controllor) {
		JButton b = new JButton(label);
		b.setActionCommand(actionCmd);
		b.addActionListener(controllor);
		
		c.add(b);
	}

	
}

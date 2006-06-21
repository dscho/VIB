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
import javax.swing.event.ChangeListener;

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
			int min, int max, ChangeListener controllor)
	{
		SpinnerModel model = new SpinnerNumberModel(
				initial, // initial value
				min, // min
				max, // max
				1); // step
		JSpinner spinner = addLabeledSpinner(c, label, model);

        spinner.addChangeListener(controllor);
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

    public static void add2Command(Container c, String label, String actionCmd, String label2, String actionCmd2, ActionListener controllor) {
		JPanel p  = new JPanel(new GridLayout(1,2));

        JButton b = new JButton(label);
		b.setActionCommand(actionCmd);
		b.addActionListener(controllor);

        JButton b2 = new JButton(label2);
		b2.setActionCommand(actionCmd2);
		b2.addActionListener(controllor);

        p.add(b);
        p.add(b2);

		c.add(p);
	}


}

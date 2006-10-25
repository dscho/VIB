package vib.app.gui.dialog;

import java.awt.*;
import java.awt.event.ActionListener;
import ij.gui.ImageLayout;
import ij.gui.StackWindow;
import ij.ImagePlus;
import ij.io.Opener;
import vib.segment.CustomStackWindow;

public class LabelPanel extends Panel {
	
	private CustomStackWindow window;

	public LabelPanel(ActionListener l) {
		Panel buttons = new Panel(new FlowLayout());
		Button okButton = new Button("OK");
		okButton.addActionListener(l);
		buttons.add(okButton);
		this.setLayout(new BorderLayout());
		this.setBackground(Color.ORANGE);
		add(buttons, BorderLayout.SOUTH);
	}

	public LabelPanel(ImagePlus imp, ActionListener l) {
		this(l);
		setImage(imp);
	}

	public void setImage(ImagePlus imp) {
		window = new CustomStackWindow(imp);
		window.setVisible(false);
		BorderLayout lm = (BorderLayout)window.getLayout(); 
		Component[] c = window.getComponents();
		for(int i = 0; i < c.length; i++) {
			c[i].setBackground(Color.ORANGE);
			this.add(c[i], lm.getConstraints(c[i]));
		}
	}

	public ImagePlus getLabels() {
		return window.getLabels();
	}

	public Insets getInsets() {
		return new Insets(20, 0, 0, 0);
	}

	public void paint(Graphics g) {
		if(window == null)
			return;
		g.translate(0, -20);
		window.drawInfo(g);
	}
}

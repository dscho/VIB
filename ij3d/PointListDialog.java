package ij3d;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Button;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.BorderLayout;
import java.awt.ScrollPane;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;

public class PointListDialog extends Dialog {

	GridBagConstraints c;
	GridBagLayout gridbag;
	ScrollPane scroll;
	Panel panel;

	public PointListDialog(Frame owner) {
		super(owner, "Point list");
		panel = new Panel();
		gridbag = new GridBagLayout();
		panel.setLayout(gridbag);

		panel.setBackground(Color.WHITE);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.weighty = 0.1f;
		c.fill = GridBagConstraints.NONE;
		scroll = new ScrollPane();
		scroll.add(panel);
		add(scroll);
	}

	public void addPointList(String name, PointListPanel plp) {
		if(!containsPointList(plp)) {
			plp.setName(name);
			gridbag.setConstraints(plp, c);
			panel.add(plp);
			c.gridx++;
			// if not displayed yet, do so now.
			if(!isVisible()) {
				setSize(250, 200);
				setVisible(true);
			}
		}
	}

	public void removePointList(PointListPanel plp) {
		if(containsPointList(plp)) {
			panel.remove(plp);
			c.gridx--;
			// hide if it is empty
			if(panel.getComponentCount() == 0)
				setVisible(false);
		}
	}

	public boolean containsPointList(PointListPanel plp) {
		Component[] c = panel.getComponents();
		for(int i = 0; i < c.length; i++) {
			if(c[i] == plp)
				return true;
		}
		return false;
	}

	public void addPanel(Panel p) {
		add(p, BorderLayout.SOUTH);
	}

	private void print() {
		Component[] c = panel.getComponents();
		for(int i = 0; i < c.length; i++) {
			System.out.println(c[i].getName());
		}
	}

	public void update() {
 		validateTree();
	}
}


package vib.segment;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;

import java.awt.Color;
import java.awt.List;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.AWTEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import vib.AmiraParameters;

public class MaterialList extends List implements ActionListener {
	PopupMenu popup;

	int materialCount;
	String[] materialNames;
	Color[] materialColors;

	public MaterialList() {
		super(6, false);
		materialNames = new String[256];
		materialColors = new Color[256];
		materialCount = 2;
		materialNames[0] = "Ext";
		materialColors[0] = Color.black;
		materialNames[1] = "Int";
		materialColors[1] = Color.red;
		fillList();

		createPopup();
		enableEvents(AWTEvent.MOUSE_EVENT_MASK);
	}

	public void processMouseEvent(MouseEvent e) {
		if (e.isPopupTrigger())
			popup.show(this, e.getX(), e.getY());
		else
			super.processMouseEvent(e);
	}

	MenuItem remove, add, rename;

	public void createPopup() {
		popup = new PopupMenu("");
		add = new MenuItem("Add Material");
		popup.add(add);
		remove = new MenuItem("Remove Material");
		popup.add(remove);
		rename = new MenuItem("Rename Material");
		popup.add(rename);
		add.addActionListener(this);
		remove.addActionListener(this);
		rename.addActionListener(this);
		add(popup);
	}

	public void initFrom(ImagePlus image) {
		AmiraParameters p = new AmiraParameters(image);
		materialCount = p.getMaterialCount();
		if (materialCount == 0) {
			materialCount = 2;
			materialNames[0] = "Exterior";
			materialColors[0] = Color.black;
			materialNames[1] = "Interior";
			materialColors[1] = Color.red;
		} else {
			for (int i = 0; i < materialCount; i++) {
				materialNames[i] = p.getMaterialName(i);
				double[] rgb = p.getMaterialColor(i);
				materialColors[i] = new Color((float)rgb[0],
						(float)rgb[1], (float)rgb[2]);
			}
		}
		fillList();
	}

	public void addMaterial() {
		int num = getItemCount();
		num++;
		add("Material"+num);
		select(num);
	}

	public void delMaterial() {
		int selected = getSelectedIndex();
		if (selected < 0)
			return;
		remove(selected);
	}

	private void renameMaterial() {
		int selected = getSelectedIndex();
		if (selected < 0)
			return;
		if (IJ.isMacOSX())
			IJ.wait(20);
		GenericDialog gd = new GenericDialog("Rename");
		gd.addStringField("name", getItem(selected));
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		replaceItem(gd.getNextString(), selected);
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == remove)
			delMaterial();
		else if (e.getSource() == add)
			addMaterial();
		else if (e.getSource() == rename)
			renameMaterial();
	}

	// TODO: color
	void fillList() {
		removeAll();
		for (int i = 0; i < materialCount; i++)
			add(materialNames[i]);
	}
}

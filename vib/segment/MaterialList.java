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
import java.util.Iterator;

import javax.naming.OperationNotSupportedException;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import vib.AmiraParameters;

public class MaterialList extends List implements ActionListener {
	PopupMenu popup;

	AmiraParameters params;

	public MaterialList(ImagePlus ip) {
		super(6, false);
		params = new AmiraParameters(ip);
		initFrom(ip);

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
		params = new AmiraParameters(image);
		if (params.getMaterialCount() == 0) {
			params.addMaterial("Exterior", 0,0,0);
			params.addMaterial("Interior", 255,0,0);
		}
		fillList();
	}

	public void addMaterial() {
		int num = getItemCount();
		num++;
		params.addMaterial("Material" + num, 255,0,0); // TODO change color
		fillList();
		select(num);
	}

	public void delMaterial() {
		throw new NotImplementedException();
//		int selected = getSelectedIndex();
//		if (selected < 0)
//			return;
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
		
		params.editMaterial(currentMaterialID(), gd.getNextString(),-1,-1,-1);
		
		fillList();
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
		String[] names = params.getMaterialList();
		for (int i = 0;i<names.length;i++){
			add(names[i]);
		}
	}
	
	public int currentMaterialID(){
		if(getSelectedIndex()==-1){
			return -1;
		}
		return params.getMaterialID(this.getSelectedItem());
	}
	
	public double[] currentMaterialColor(){
		int mID = currentMaterialID();
		if(mID == -1){
			return null;
		}
		return params.getMaterialColor(mID);
	}
}

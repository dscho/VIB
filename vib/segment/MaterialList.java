package vib.segment;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;

import java.awt.AWTEvent;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Label;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.ScrollPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Iterator;

import javax.naming.OperationNotSupportedException;

import vib.AmiraParameters;

public class MaterialList extends ScrollPane implements ActionListener {
	PopupMenu popup;

	ImagePlus labels;
	AmiraParameters params;

	Font font;
	int lineHeight, lineWidth;
	List list;

	public MaterialList(ImagePlus ip) {
		super();
		params = new AmiraParameters(ip);
		initFrom(ip);

		createPopup();

		font = new Font("Monospaced", Font.PLAIN, 12);
		lineHeight = font.getSize() + 1;
		lineWidth = 200;
		list = new List();
		add(list);
	}

	private int getSelectedIndex() {
		return list.selectedIndex;
	}

	private void select(int index) {
		list.selectedIndex = index;
	}

	public int getItemCount() {
		return params.getMaterialCount();
	}

	public String getItem(int index) {
		return params.getMaterialName(index);
	}

	public String getSelectedItem() {
		return getItem(getSelectedIndex());
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
		labels = image;
		params = new AmiraParameters(image);
		if (params.getMaterialCount() == 0) {
			params.addMaterial("Exterior", 0,0,0);
			params.addMaterial("Interior", 1,0,0);
		}
		if (list != null)
			list.repaint();
	}

	public void addMaterial() {
		int num = getItemCount();
		num++;
		params.addMaterial("Material" + num, 1,0,0); // TODO change color
		params.setParameters(labels);
		select(num);
		doLayout();
		list.repaint();
	}

	public void delMaterial() {
		int selected = getSelectedIndex();
		if (selected < 1) {
			IJ.error("Cannot delete first material!");
			return;
		}
		throw new RuntimeException("delete not yet implemented");
	}

	private void renameMaterial() {
		GenericDialog gd = new GenericDialog("Rename");
		gd.addStringField("name", getSelectedItem());
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		
		params.editMaterial(currentMaterialID(), gd.getNextString(),-1,-1,-1);
		params.setParameters(labels);
		list.repaint();
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == remove)
			delMaterial();
		else if (e.getSource() == add)
			addMaterial();
		else if (e.getSource() == rename)
			renameMaterial();
	}

	public int currentMaterialID(){
		if(getSelectedIndex()==-1){
			return -1;
		}
		return params.getMaterialID(getSelectedItem());
	}
	
	public double[] currentMaterialColor(){
		int mID = currentMaterialID();
		if(mID == -1){
			return null;
		}
		return params.getMaterialColor(mID);
	}
	
	public int getDefaultMaterialID(){
		return params.getMaterialID(getItem(0));
	}

	private class List extends Canvas {
		Color fgCol = Color.BLACK;
		Color bgCol = Color.LIGHT_GRAY;
		private int selectedIndex = 0;


		public List() {
			enableEvents(AWTEvent.MOUSE_EVENT_MASK |
					AWTEvent.KEY_EVENT_MASK);
		}

		public void processMouseEvent(MouseEvent e) {
			if (e.getID() == MouseEvent.MOUSE_RELEASED) {
				selectedIndex = e.getY() / lineHeight;
				repaint();
			}
			if (e.isPopupTrigger())
				popup.show(this, e.getX(), e.getY());
		}

		public void processKeyEvent(KeyEvent e) {
			if (e.getID() == KeyEvent.KEY_PRESSED) {
				int code = e.getKeyCode();
				if (code == e.VK_UP && selectedIndex > 0)
					ensureVisible(--selectedIndex, true);
				else if (code == e.VK_DOWN && selectedIndex
						< getItemCount() - 1)
					ensureVisible(++selectedIndex, true);
			}
		}

		public void ensureVisible(int index, boolean repaintAnyway) {
			Point p = getScrollPosition();
			Dimension d = getViewportSize();
			if (p.y + d.height < (index + 1) * lineHeight ||
					p.y > index * lineHeight) {
				setScrollPosition(p.x, index * lineHeight);
				repaint();
			} else if (repaintAnyway)
				repaint();
		}

		public void update(Graphics g) {
			paint(g);
		}

		public void paint(Graphics g) {
			g.setFont(font);
			for (int i = 0; i < getItemCount(); i++) {
				g.setColor(i == selectedIndex ? fgCol : bgCol);
				g.fillRect(0, i * lineHeight,
						lineWidth, lineHeight);
				double[] c = params.getMaterialColor(i);
				g.setColor(new Color((float)c[0], (float)c[1],
							(float)c[2]));
				g.fillRect(1, i * lineHeight + 1,
						lineHeight - 2, lineHeight - 2);
				g.setColor(i == selectedIndex ? bgCol : fgCol);
				g.drawString(getItem(i), lineHeight,
						(i + 1) * lineHeight - 1);
			}
		}

		public Dimension getPreferredSize() {
			return new Dimension(lineWidth, getItemCount() *
					(font.getSize() + 1) + 1);
		}
	}
}

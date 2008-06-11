package ij3d;
/**
 * @author Benjamin Schmid
 * 
 * @date 07.08.2006
 */

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GUI;
import ij.gui.GenericDialog;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.io.FileInfo;
import ij.plugin.PlugIn;

import vib.BenesNamedPoint;
import vib.PointList;

import java.awt.Component;
import java.text.DecimalFormat;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.MenuItem;
import java.awt.Panel;
import java.awt.Polygon;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


public class PointListPanel extends Panel
		implements ActionListener, PointList.PointListListener {
	
	private PointList points;
	private GridBagConstraints c;
	private final PopupMenu popup = createPopup();
	private BenesNamedPoint current;
	private DecimalFormat df = new DecimalFormat("00.000");
	private String header;
	
	public PointListPanel(String header, PointList points) {

		super();
		this.header = header;
		this.add(popup);
		this.points = points;
		points.addPointListListener(this);

		setLayout(new GridBagLayout());
		setBackground(Color.WHITE);
		c = new GridBagConstraints();

		updatePointsPanel();
	}
	
	private PopupMenu createPopup(){
		PopupMenu popup = new PopupMenu();
		MenuItem mi = new MenuItem("Up");
		mi.addActionListener(this);
		popup.add(mi);
		mi = new MenuItem("Down");
		mi.addActionListener(this);
		popup.add(mi);
		mi = new MenuItem("Rename");
		mi.addActionListener(this);
		popup.add(mi);
		mi = new MenuItem("Remove");
		mi.addActionListener(this);
		popup.add(mi);
		return popup;
	}
	
	private void updatePointsPanel(){
		removeAll();
		c = new GridBagConstraints();
		addHeader();
		int i = 0;
		if(points.size() == 0)
			addEmptyRow();
		for (BenesNamedPoint p : points)
			addRow(p, i++);
	}

	private void addHeader() {
		Label l = new Label(header);
		l.setFont(new Font("Verdana", Font.BOLD, 12));
		c.gridy = 0;
		c.gridwidth = 2;
		c.gridx = 0;
		add(l, c);
		c.gridwidth = 1;
	}

	private void addEmptyRow() {
		Label l = new Label("     No points set       ");
		c.gridx = 0;
		c.gridy = GridBagConstraints.RELATIVE;
		c.anchor = GridBagConstraints.LINE_START;			
		add(l, c);
	}

	private Color grey = Color.LIGHT_GRAY;
	private void addRow(final BenesNamedPoint p, int row){
		if(!p.isSet())
			return;
		c.gridx = 0;
		c.gridy = GridBagConstraints.RELATIVE;
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.LINE_START;			
		final Label label = new Label(p.getName() + "   ");
		label.setName(p.getName());
		label.setFont(new Font("Verdana", Font.BOLD, 12));
		label.setForeground(Color.BLUE);
		if(row % 2 == 1)
			label.setBackground(grey);
		label.addMouseListener(new MouseAdapter(){
			public void mousePressed(MouseEvent e){
				if(e.isPopupTrigger()){
					current = p;
					popup.show(label, e.getX(),e.getY());
				} else {
					highlightPoint(p);
				}
			}
			public void mouseReleased(MouseEvent e){
				if(e.isPopupTrigger()){
					current = p;
					popup.show(label, e.getX(),e.getY());
				}
			}
		});
		add(label,c);
		
		c.anchor = GridBagConstraints.LINE_START;
		c.gridx = 1;
		c.fill = GridBagConstraints.BOTH;
		Label coordinateLabel = new Label(df.format(p.x) + "    " + 
			df.format(p.y) + "    " + df.format(p.z));
		coordinateLabel.setFont(new Font("Verdana", Font.PLAIN, 12));
		if(row % 2 == 1)
			coordinateLabel.setBackground(grey);
		add(coordinateLabel,c);
	}
	
	public void removePoint(BenesNamedPoint p){
		points.remove(p);
		java.awt.Container parent = getParent();
		parent.validate();
		while(parent.getParent() != null) {
			parent = parent.getParent();
			validate();
		}
	}
	
	public void renamePoint(BenesNamedPoint p){
		GenericDialog gd = new GenericDialog("Rename point");
		gd.addStringField("New name ", p.getName());
		gd.showDialog();
		if(gd.wasCanceled())
			return;
		points.rename(p, gd.getNextString());
	}

	public void highlightPoint(BenesNamedPoint p) {
		points.highlight(p);
	}

	public void up(BenesNamedPoint p) {
		points.up(p);
		java.awt.Container parent = getParent();
		parent.validate();
		while(parent.getParent() != null) {
			parent = parent.getParent();
			validate();
		}
	}

	public void down(BenesNamedPoint p) {
		points.down(p);
		java.awt.Container parent = getParent();
		parent.validate();
		while(parent.getParent() != null) {
			parent = parent.getParent();
			validate();
		}
	}

	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if (command.equals("Rename")){
			renamePoint(current);
		} else if (command.equals("Remove")){
			removePoint(current);
		} else if (command.equals("Up")) { 
			up(current);
		} else if(command.equals("Down")) {
			down(current);
		}
	}

	// PointListListener interface
	public void added(BenesNamedPoint p) {
		int i = points.size();
		if(i == 1)
			updatePointsPanel();
		else
			addRow(p, points.size());
	}

	public void removed(BenesNamedPoint p) {
		updatePointsPanel();
	}

	public void renamed(BenesNamedPoint p) {
		updatePointsPanel();
	}

	public void highlighted(BenesNamedPoint p) {
	}

	public void reordered() {
		updatePointsPanel();
	}

	public void moved(BenesNamedPoint p) {
		Component[] c = getComponents();
		boolean found = false;
		for(int i = 0; i < c.length; i++) {
			if(c[i].getName().equals(p.getName()) && i<c.length-1) {
				Label coord = (Label)c[i+1];
				coord.setText(df.format(p.x) + "    " +
					df.format(p.y) + "    " + 
					df.format(p.z));
				found = true;
			}
		}
		// if not successful, just update the whole panel
		if(!found)
			updatePointsPanel();
	}
}

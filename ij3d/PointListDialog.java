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


public class PointListDialog extends Dialog 
		implements ActionListener, PointList.PointListListener {
	
	private PointList points;
	private Panel panel;
	private GridBagConstraints c;
	private final PopupMenu popup = createPopup();
	private BenesNamedPoint current;
	private DecimalFormat df = new DecimalFormat("00.000");
	
	public PointListDialog(PointList points) {
		this("Point list", points);
	}

	public PointListDialog(String title, PointList points) {
		
		super(IJ.getInstance(),title,false);
		
		this.add(popup);
		this.points = points;
		points.addPointListListener(this);

		setLayout(new BorderLayout());
		panel = new Panel();
		panel.setLayout(new GridBagLayout());
		panel.setBackground(Color.WHITE);
		c = new GridBagConstraints();

		updatePointsPanel();
		add(panel,BorderLayout.CENTER);
		
		
		Panel buttonsPanel = new Panel();
		Button button = new Button("Close");
		button.addActionListener(this);
// 		buttonsPanel.add(button);	
		add(buttonsPanel,BorderLayout.SOUTH);
	
		pack();
	}
	
	private PopupMenu createPopup(){
		PopupMenu popup = new PopupMenu();
		MenuItem mi = new MenuItem("Rename");
		mi.addActionListener(this);
		popup.add(mi);
		mi = new MenuItem("Remove");
		mi.addActionListener(this);
		popup.add(mi);
		return popup;
	}
	
	public void updatePointsPanel(){
		panel.removeAll();
		c = new GridBagConstraints();
		int i = 0;
		if(points.size() == 0)
			addEmptyRow();
		for (BenesNamedPoint p : (Iterable<BenesNamedPoint>)points) {
			addRow(p, i++);
		}
		this.pack();
	}

	private void addEmptyRow() {
		Label l = new Label("     No points set       ");
		c.gridx = 0;
		c.gridy = GridBagConstraints.RELATIVE;
		c.anchor = GridBagConstraints.LINE_START;			
		panel.add(l, c);
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
		panel.add(label,c);
		
		c.anchor = GridBagConstraints.LINE_START;
		c.gridx = 1;
		c.fill = GridBagConstraints.BOTH;
		Label coordinateLabel = new Label(df.format(p.x) + "    " + 
			df.format(p.y) + "    " + df.format(p.z));
		coordinateLabel.setFont(new Font("Verdana", Font.PLAIN, 12));
		if(row % 2 == 1)
			coordinateLabel.setBackground(grey);
		panel.add(coordinateLabel,c);
	}
	
	public void removePoint(BenesNamedPoint p){
		points.remove(p);
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

	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if(command.equals("Close")){
			dispose();
		} else if (command.equals("Rename")){
			renamePoint(current);
		} else if (command.equals("Remove")){
			removePoint(current);
		} 
	}

	// PointListListener interface
	public void added(int i) {
		updatePointsPanel();
	}

	public void removed(int i) {
		updatePointsPanel();
	}

	public void renamed(int i) {
		updatePointsPanel();
	}

	public void highlighted(int i) {
	}

	public void moved(int index) {
		BenesNamedPoint p = points.get(index);
		Component[] c = panel.getComponents();
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

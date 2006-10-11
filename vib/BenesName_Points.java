package vib;
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

import java.awt.BorderLayout;
import java.awt.Button;
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


class BenesPointsDialog extends Dialog implements ActionListener {
	
	private PointList points;
	private BenesName_Points plugin;	
	private Panel panel;
	private final PopupMenu popup = createPopup();
	private BenesNamedPoint current;
	
	public BenesPointsDialog(String title,
			    PointList points,
			    BenesName_Points plugin) {
		
		super(IJ.getInstance(),title,false);
		
		this.add(popup);
		this.plugin = plugin;
		this.points = points;
		
		setLayout(new BorderLayout());
		
		Panel instructionsPanel = new Panel();
		instructionsPanel.add(
				new Label( "Mark the current point selection as:" ),
				BorderLayout.WEST);
		add(instructionsPanel,BorderLayout.NORTH);
		
		panel = new Panel();
		updatePointsPanel();
		add(panel,BorderLayout.CENTER);
		
		
		Panel buttonsPanel = new Panel();
		Button button = new Button("Load");
		button.addActionListener(this);
		buttonsPanel.add(button);
		button = new Button("Add point");
		button.addActionListener(this);
		buttonsPanel.add(button);
		button = new Button("Save");
		button.addActionListener(this);
		buttonsPanel.add(button);	
		button = new Button("Reset");
		button.addActionListener(this);
		buttonsPanel.add(button);
		button = new Button("Close");
		button.addActionListener(this);
		buttonsPanel.add(button);	
		add(buttonsPanel,BorderLayout.SOUTH);
	
		GUI.center(this);
		pack();
		setVisible(true);
	}
	
	private PopupMenu createPopup(){
		PopupMenu popup = new PopupMenu();
		MenuItem mi = new MenuItem("Rename point");
		mi.addActionListener(this);
		popup.add(mi);
		mi = new MenuItem("Remove point");
		mi.addActionListener(this);
		popup.add(mi);
		mi = new MenuItem("Reset point");
		mi.addActionListener(this);
		popup.add(mi);
		return popup;
	}
	
	public void updatePointsPanel(){
		if(points.size() == 0)
			addEmptyPoint();
		panel.removeAll();
		panel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		for (BenesNamedPoint p : (Iterable<BenesNamedPoint>)points) {
			addRow(p,panel,c);
		}
		this.pack();
	}
	
	private void addRow(final BenesNamedPoint p, Panel panel, GridBagConstraints c){
		c.gridx = 0;
		c.gridy = GridBagConstraints.RELATIVE;
		c.anchor = GridBagConstraints.LINE_START;			
		final Button button = new Button(p.name);
		button.addMouseListener(new MouseAdapter(){
			public void mousePressed(MouseEvent e){
				if(e.isPopupTrigger()){
					current = p;
					popup.show(button, e.getX(),e.getY());
				}
			}
			public void mouseReleased(MouseEvent e){
				if(e.isPopupTrigger()){
					current = p;
					popup.show(button, e.getX(),e.getY());
				}
			}
		});
		button.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				BenesPointsDialog.this.plugin.mark(p);
			}
		});
		panel.add(button,c);
		
		c.anchor = GridBagConstraints.LINE_START;
		c.gridx = 1;
		Label coordinateLabel = 
			p.set ? new Label(p.coordinatesAsString())
					: new Label("     <unset>     ");
		panel.add(coordinateLabel,c);
		
		c.anchor = GridBagConstraints.LINE_START;
		c.gridx = 2;
		Button showB = new Button("Show");
		showB.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				BenesPointsDialog.this.plugin.show(p);
			}
		});
		showB.setEnabled(p.set);
		panel.add(showB,c);
	}
	
	public void addEmptyPoint(){
		BenesNamedPoint p = new BenesNamedPoint("point" + points.size());
		points.add(p);
		updatePointsPanel();
	}
	
	public void removePoint(BenesNamedPoint p){
		points.remove(p);
		updatePointsPanel();
	}
	
	public void renamePoint(BenesNamedPoint p){
		GenericDialog gd = new GenericDialog("Rename point");
		gd.addStringField("New name ", p.name);
		gd.showDialog();
		if(gd.wasCanceled())
			return;
		p.name = gd.getNextString();
		updatePointsPanel();
	}

	public void resetPoint(BenesNamedPoint p) {
		p.set = false;
		updatePointsPanel();
	}

	public void resetAll() {
		for(BenesNamedPoint p : (Iterable<BenesNamedPoint>)points) {
			p.set = false;
		}
		updatePointsPanel();
	}
	
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if(command.equals("Close")){
			dispose();
		} else if (command.equals("Save")) {
			plugin.save();
		} else if (command.equals("Reset")) {
			resetAll();
		} else if (command.equals("Add point")){
			addEmptyPoint();
		} else if (command.equals("Rename point")){
			renamePoint(current);
		} else if (command.equals("Remove point")){
			removePoint(current);
		} else if (command.equals("Reset point")){
			resetPoint(current);
		} else if (command.equals("Load")){
			BenesPointsDialog.this.plugin.load();
		} 
	}
}

public class BenesName_Points implements PlugIn {

	public void show(BenesNamedPoint p) {
		if(!p.set){
			IJ.error("Point is not set yet");
			return;
		}
		int slice = (int)p.z;
		if(slice < 0)
			slice = 0;
		if(slice > imp.getStackSize())
			slice = imp.getStackSize()-1;
		imp.setSlice(slice+1);
		Roi roi	= new PointRoi((int)p.x,
				       (int)p.y,
				       imp);
		imp.setRoi(roi);
	}
	
	public void load() {
		PointList newNamedPoints = PointList.load(imp);
		
		if(newNamedPoints==null)
			return;

		for (BenesNamedPoint current : (Iterable<BenesNamedPoint>)newNamedPoints) {
			boolean foundName = false;
			for(BenesNamedPoint p : (Iterable<BenesNamedPoint>)points) {
				if (current.name.equals(p.name)) {
					p.set(current.x, current.y, current.z);
					p.set = true;
					foundName = true;
				}
			}
			if (!foundName)
				points.add(current);
		}
		dialog.updatePointsPanel();
	}
	
	public void save() {

		FileInfo info = imp.getOriginalFileInfo();
		if( info == null ) {
			IJ.error("There's no original file name that these points refer to.");
			return;
		}
		String fileName = info.fileName + ".points";
		String directory = info.directory;
		IJ.showStatus("Saving point annotations to "+ directory + fileName);
		points.save(directory, fileName);
		IJ.showStatus("Saved point annotations.");
	}

	public void mark(BenesNamedPoint point) {
		Roi roi = imp.getRoi();
		if (roi!=null && roi.getType()==Roi.POINT) {
			Polygon p = roi.getPolygon();
			
			if(p.npoints > 1) {
				IJ.error("You can only have one point selected to mark.");
				return;
			}

			int x = p.xpoints[0];
			int y = p.ypoints[0];
			int z = imp.getCurrentSlice()-1;

			point.set(x,y,z);
			point.set = true;
			dialog.updatePointsPanel();

		} else {
			IJ.error("You must have a current point selection in "+
				 imp.getTitle()+" in order to mark points.");
		}
	}

	BenesPointsDialog dialog;
	ImagePlus imp;	
	PointList points;

	public void run(String arg) {
		/*
		String test1 = "one backslash '\\' and one double quote '\"'";
		System.out.println("escaping: "+test1);
		System.out.println("gives: "+escape(test1));
		*/

		imp = IJ.getImage();
		
		if(imp == null)
			IJ.error("There's no image to annotate.");

		points = new PointList();

		dialog = new BenesPointsDialog("Marking up: "+imp.getTitle(),
					points,
					this);

	}
}

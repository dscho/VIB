/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

import ij.*;
import ij.process.*;
import ij.io.*;
import ij.gui.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.text.*;

import ij.measure.Calibration;

import java.awt.Color;
import java.io.*;

import math3d.Point3d;

import java.util.ArrayList;
import java.util.Comparator;

import java.awt.*;
import java.awt.event.*;

import java.io.*;
import java.util.regex.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;

import vib.NamedPoint;

class PointsDialog extends Dialog implements ActionListener {
	
	Label[] coordinateLabels;
	Button[] markButtons;
	Button[] showButtons;      

	Name_Points plugin;
	
	public PointsDialog(String title,
			    ArrayList<NamedPoint> points,
			    Name_Points plugin) {
		
		super(IJ.getInstance(),title,false);
		
		this.plugin = plugin;
		
		coordinateLabels = new Label[points.size()];
		markButtons = new Button[points.size()];
	        showButtons = new Button[points.size()];
		
		setLayout(new BorderLayout());
		
		Panel instructionsPanel = new Panel();
		Panel pointsPanel = new Panel();
		Panel buttonsPanel = new Panel();
		
		Label instructions = new Label( "Mark the current point selection as:" );
		instructionsPanel.setLayout(new BorderLayout());
		instructionsPanel.add(instructions,BorderLayout.WEST);
		
		add(instructionsPanel,BorderLayout.NORTH);
		
		pointsPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		
		int counter = 0;
		Iterator i;
		for (i=points.listIterator();i.hasNext();) {
			NamedPoint p = (NamedPoint)i.next();
			c.gridx = 0;
			c.gridy = counter;
			c.anchor = GridBagConstraints.LINE_END;			
			markButtons[counter] = new Button(p.getName());
			markButtons[counter].addActionListener(this);
			pointsPanel.add(markButtons[counter],c);
			c.anchor = GridBagConstraints.LINE_START;
			c.gridx = 1;
			coordinateLabels[counter] = new Label("<unset>");
			pointsPanel.add(coordinateLabels[counter],c);
			c.anchor = GridBagConstraints.LINE_START;
			c.gridx = 2;
			showButtons[counter] = new Button("Show");
			showButtons[counter].addActionListener(this);
			showButtons[counter].setEnabled(false);
			pointsPanel.add(showButtons[counter],c);
			if (p.set)
				setCoordinateLabel(counter,
						   p.x,
						   p.y,
						   p.z);
			++counter;
		}
		
		add(pointsPanel,BorderLayout.CENTER);
		
		saveButton = new Button("Save");
		saveButton.addActionListener(this);
		resetButton = new Button("Reset");
		resetButton.addActionListener(this);
		closeButton = new Button("Close");
		closeButton.addActionListener(this);
		
		buttonsPanel.add(saveButton);		
		buttonsPanel.add(resetButton);
		buttonsPanel.add(closeButton);
	
		add(buttonsPanel,BorderLayout.SOUTH);
	
		pack();
		setVisible(true);
	}
	
	Button saveButton;
	Button resetButton;
	Button closeButton;	

	public void reset(int i) {
		assert i>0;
		assert i<coordinateLabels.length;
		coordinateLabels[i].setText("<unset>");
		showButtons[i].setEnabled(false);
		pack();		
	}

	public void resetAll() {
		for(int i = 0; i < coordinateLabels.length; ++i) {
			coordinateLabels[i].setText("<unset>");
			showButtons[i].setEnabled(false);
		}
		pack();
	}

	public void setCoordinateLabel(int i, double x, double y, double z) {
		String newText = "";
		newText += "x: " + x + ", y: " + y + ", z: " + z;
		coordinateLabels[i].setText(newText);
		showButtons[i].setEnabled(true);
		pack();
	}

	public void paint(Graphics g) {
		super.paint(g);
	}	
	
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		for (int i=0; i < markButtons.length; ++i) {
			if(source == markButtons[i]) {
				plugin.mark(i);
				break;
			}
		}
		for (int i=0; i < showButtons.length; ++i) {
			if(source == showButtons[i]) {
				plugin.show(i);
				break;
			}
		}
		if(source == closeButton) {
			dispose();
		} else if (source == saveButton) {
			plugin.save();
		} else if (source == resetButton) {
			plugin.reset();
		}
	}
}

public class Name_Points implements PlugIn {

	// FIXME: really we want different sets of points for
	// different applications.

	private String [] defaultPointNames = {
		"the centre of the ellipsoid body",
		"the left tip of the protocerebral bridge",
		"the right tip of the protocerebral bridge",
		"the most dorsal point of the left part of the protocerebral bridge",
		"the most dorsal point of the right part of the protocerebral bridge",
		"the top of the left alpha lobe of the mushroom body",
		"the top of the right alpha lobe of the mushroom body",
		"the most lateral part of the mushroom body on the left",
		"the most lateral part of the mushroom body on the right"
	};

	public void show(int i) {
		NamedPoint p = (NamedPoint)points.get(i);
		assert p.set;
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

		ArrayList<NamedPoint> newNamedPoints = NamedPoint.pointsForImage(imp);
		
		if(newNamedPoints==null)
			return;

		ListIterator i;
		for (i = newNamedPoints.listIterator();i.hasNext();) {
			NamedPoint current = (NamedPoint)i.next();
			boolean foundName = false;
			ListIterator j;
			for(j=points.listIterator();j.hasNext();) {
				NamedPoint p = (NamedPoint)j.next();
				if (current.getName().equals(p.getName())) {
					p.x = current.x;
					p.y = current.y;
					p.z = current.z;
					p.set = true;
					foundName = true;
				}
			}
			if (!foundName)
				points.add(current);
		}

	}

	public void save() {

		FileInfo info = imp.getOriginalFileInfo();
		if( info == null ) {
			IJ.error("There's no original file name that these points refer to.");
			return;
		}
		String fileName = info.fileName;
		String url = info.url;
		String directory = info.directory;

		String suggestedSaveFilename;

		suggestedSaveFilename = fileName+".points";

		SaveDialog sd = new SaveDialog("Save points annotation file as...",
					       directory,
					       suggestedSaveFilename,
					       ".points");

		String savePath;
		if(sd.getFileName()==null)
			return;
		else {
			savePath = sd.getDirectory()+sd.getFileName();
		}

		File file = new File(savePath);
		if ((file!=null)&&file.exists()) {
			if (!IJ.showMessageWithCancel(
				    "Save points annotation file", "The file "+
				    savePath+" already exists.\n"+
				    "Do you want to replace it?"))
				return;
		}

		IJ.showStatus("Saving point annotations to "+savePath);

		try {
			FileOutputStream fos = new FileOutputStream(savePath);
			Iterator i;
			for(i=points.listIterator();i.hasNext();) {
				NamedPoint p = (NamedPoint)i.next();
				if(p.set) {
					String line = p.toYAML() + "\n";
					fos.write(line.getBytes("UTF-8"));
				}
			}
			fos.close();
		} catch( IOException e ) {
			IJ.error("Error saving to: "+savePath+"\n"+e);
		}
				
		IJ.showStatus("Saved point annotations.");

	}

	public void reset() {
		dialog.resetAll();
	}

	public void mark(int i) {
		Roi roi = imp.getRoi();
		if (roi!=null && roi.getType()==Roi.POINT) {
			Polygon p = roi.getPolygon();
			ImageProcessor processor = imp.getProcessor();
			/*
			  Calibration cal = imp.getCalibration();
			  ip.setCalibrationTable(cal.getCTable());
			*/
			if(p.npoints > 1) {
				IJ.error("You can only have one point selected to mark.");
				return;
			}

			int x = p.xpoints[0];
			int y = p.ypoints[0];
			int z = imp.getCurrentSlice()-1;

			if( false ) {

				// Add a crosshair to the point we've just marked.
				
				processor.setColor(Toolbar.getForegroundColor());
				processor.setLineWidth(1);
				processor.moveTo(x+1,y);
				processor.lineTo(x+5,y);
				processor.moveTo(x-1,y);
				processor.lineTo(x-5,y);
				processor.moveTo(x,y+1);
				processor.lineTo(x,y+5);
				processor.moveTo(x,y-1);
				processor.lineTo(x,y-5);
				imp.updateAndDraw();
			}
				
			// System.out.println("Got x: " + x + ", y: " + y + ", z: " + z);

			dialog.setCoordinateLabel(i,x,y,z);

			NamedPoint point = (NamedPoint)points.get(i);
			point.x = x;
			point.y = y;
			point.z = z;
			point.set = true;

		} else {
			IJ.error("You must have a current point selection in "+
				 imp.getTitle()+" in order to mark points.");
		}

	}

	public Name_Points() {
		
	}

	PointsDialog dialog;
	ImagePlus imp;
	
	ArrayList<NamedPoint> points;

	public void run(String arg) {
		/*
		String test1 = "one backslash '\\' and one double quote '\"'";
		System.out.println("escaping: "+test1);
		System.out.println("gives: "+escape(test1));
		*/

		imp = IJ.getImage();
		
		if(imp == null)
			IJ.error("There's no image to annotate.");

		points = new ArrayList<NamedPoint>();
		for (int i = 0; i < defaultPointNames.length; ++i)
			points.add(new NamedPoint(defaultPointNames[i]));

		load();

		dialog = new PointsDialog("Marking up: "+imp.getTitle(),
					  points,this);

	}
}

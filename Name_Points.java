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

import Jama.*;

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

class PointsDialog extends Dialog implements ActionListener {
	
	Label[] coordinateLabels;
	Button[] markButtons;
	Button[] showButtons;      

	Name_Points plugin;
	
	public PointsDialog(String title,String[] pointNames,Name_Points plugin) {
		
		super(IJ.getInstance(),title,false);
		
		this.plugin = plugin;
		
		coordinateLabels = new Label[pointNames.length];
		markButtons = new Button[pointNames.length];
	        showButtons = new Button[pointNames.length];
		
		setLayout(new BorderLayout());
		
		Panel instructionsPanel = new Panel();
		Panel pointsPanel = new Panel();
		Panel buttonsPanel = new Panel();
		
		Label instructions = new Label( "Mark the current point selection as:" );	
		instructionsPanel.add(instructions);
		
		add(instructionsPanel,BorderLayout.NORTH);
		
		pointsPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.LINE_START;
		pointsPanel.add(instructions,c);
		
		int i;
		for (i=0; i < pointNames.length; ++i) {			
			c.gridx = 0;
			c.gridy = i + 1;
			c.anchor = GridBagConstraints.LINE_END;			
			markButtons[i] = new Button(pointNames[i]);
			markButtons[i].addActionListener(this);
			pointsPanel.add(markButtons[i],c);
			c.anchor = GridBagConstraints.LINE_START;
			c.gridx = 1;
			coordinateLabels[i] = new Label("<unset>");
			pointsPanel.add(coordinateLabels[i],c);
			c.anchor = GridBagConstraints.LINE_START;
			c.gridx = 2;
			showButtons[i] = new Button("Show");
			showButtons[i].addActionListener(this);
			showButtons[i].setEnabled(false);
			pointsPanel.add(showButtons[i],c);
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

class NamedPoint {

	public double x,y,z;
	String name;

	public NamedPoint(String name,
			  double x,
			  double y,
			  double z) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.name = name;
	}
}

public class Name_Points implements PlugIn {

	// FIXME: really we want different sets of points for
	// different applications.

	private String [] pointNames = {
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
		assert pointsSet[i];
		int slice = (int)pointPositions[i][2];
		if(slice < 0)
			slice = 0;
		if(slice > imp.getStackSize())
			slice = imp.getStackSize()-1;
		imp.setSlice(slice+1);
		Roi roi	= new PointRoi((int)pointPositions[i][0],
				       (int)pointPositions[i][1],
				       imp);
		imp.setRoi(roi);
	}
	
	public static String escape(String s) {
		String result = s.replaceAll("\\\\","\\\\\\\\");
		result = result.replaceAll("\\\"","\\\\\"");
		return result;
	}

	public static String unescape(String s) {
		// FIXME: actually write the unescaping code...
		return s;
	}

	public void load() {
	
		FileInfo info = imp.getOriginalFileInfo();
		if( info == null ) {
			return;
		}
		String fileName = info.fileName;
		String url = info.url;
		String directory = info.directory;

		String defaultFilename = directory+fileName+".points";

		/*
		System.out.println("Attempting to load default points file: "
				   +defaultFilename);
		*/
				   
		try {
			ArrayList<NamedPoint> newNamedPoints = new ArrayList<NamedPoint>();
			Pattern p_data =
				Pattern.compile("^\"(.*)\": *"+
						"\\[ *([eE0-9\\.\\-]+) *, *"+
						"([eE0-9\\.\\-]+) *, *"+
						"([eE0-9\\.\\-]+) *\\] *$");
			Pattern p_empty = Pattern.compile("^ *$");
			BufferedReader f = new BufferedReader(
				new FileReader(defaultFilename));
			String line;
			while ((line=f.readLine())!=null) {
				// System.out.println("Got line: "+line);
				Matcher m_data = p_data.matcher(line);
				Matcher m_empty = p_empty.matcher(line);
				if (m_data.matches()) {
					// System.out.println("Matched!");
					// System.out.println(m_data.group(1));
					// System.out.println(m_data.group(2));
					// System.out.println(m_data.group(3));
					// System.out.println(m_data.group(4));
					newNamedPoints.add(
						new NamedPoint(m_data.group(1),
							       Double.parseDouble(m_data.group(2)),
							       Double.parseDouble(m_data.group(3)),
							       Double.parseDouble(m_data.group(4)))
						);
				} else if (m_empty.matches()) {
					// Ignore empty lines...
				} else {
					IJ.error("There was a points file ("+
						 defaultFilename+") but this line was malformed:\n"+
						 line);
					break;
				}
				ListIterator i;
				for (i = newNamedPoints.listIterator();i.hasNext();) {
					NamedPoint current = (NamedPoint)i.next();
					int j;
					for(j=0;j<pointNames.length;++j) {
						if (current.name.equals(pointNames[j])) {
							pointPositions[j][0] = current.x;
							pointPositions[j][1] = current.y;
							pointPositions[j][2] = current.z;
							pointsSet[j] = true;
							i.remove();
						}

					}
				}
				int numberOfExtraPoints = newNamedPoints.size();
				// System.out.println("Extra points: "+numberOfExtraPoints);
				if( numberOfExtraPoints > 0 ) {
					int oldLength = pointNames.length;
					int newLength = numberOfExtraPoints + pointNames.length;
					String[] newPointNames = new String[newLength];
					double[][] newPointPositions = new double[newLength][3];
					boolean[] newPointsSet = new boolean[newLength];
					System.arraycopy(pointNames,0,newPointNames,0,oldLength);
					System.arraycopy(pointPositions,0,newPointPositions,0,oldLength);
					System.arraycopy(pointsSet,0,newPointsSet,0,oldLength);
					for(i=newNamedPoints.listIterator();i.hasNext();) {
						int indexInNewArray = i.nextIndex() + oldLength;
						// System.out.println("Copying over index: "+indexInNewArray);
						NamedPoint current = (NamedPoint)i.next();
						newPointNames[indexInNewArray] = current.name;
						newPointPositions[indexInNewArray][0] = current.x;
						newPointPositions[indexInNewArray][1] = current.y;
						newPointPositions[indexInNewArray][2] = current.z;
						newPointsSet[indexInNewArray] = true;
					}
					this.pointNames = newPointNames;
					this.pointPositions = newPointPositions;
					this.pointsSet = newPointsSet;
				}
			}
		} catch( IOException e ) {			

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
		// System.out.println("fileName was: "+fileName);
		// System.out.println("url was: "+url);
		// System.out.println("directory was: "+directory);

		String suggestedSaveFilename;

		// Look for an extension...

		/*
		int lastDot = fileName.lastIndexOf('.');
		if (lastDot == -1) {
			suggestedSaveFilename = fileName + ".points";
		} else if (lastDot == (fileName.length()-1)) { // The last character...
			suggestedSaveFilename = fileName + "points";
		} else {
			suggestedSaveFilename = fileName.substring(0,lastDot) + ".points";
		}
		*/

		// Just add ".points" to the filename, even if there's
		// an existing extension...

		suggestedSaveFilename = fileName+".points";

		// System.out.println("Suggested save filename is: "+suggestedSaveFilename);

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
			if (!IJ.showMessageWithCancel("Save points annotation file", "The file "+
						      savePath+" already exists.\nDo you want to replace it?"))
				return;
		}

		IJ.showStatus("Saving point annotations to "+savePath);

		try {
			FileOutputStream fos = new FileOutputStream(savePath);
			for(int i = 0; i<pointPositions.length; ++i) {
				if(pointsSet[i]) {
					String line = "\""+
						escape(pointNames[i])+
						"\": [ "+
						pointPositions[i][0]+", "+
						pointPositions[i][1]+", "+
						pointPositions[i][2]+" ]\n";
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
			pointPositions[i][0] = x;
			pointPositions[i][1] = y;
			pointPositions[i][2] = z;
			pointsSet[i] = true;
		} else {
			IJ.error("You must have a current point selection in "+
				 imp.getTitle()+" in order to mark points.");
		}

	}

	public Name_Points() {
		
	}

	PointsDialog dialog;
	ImagePlus imp;
	
	double[][] pointPositions;
	boolean[] pointsSet;

	public void run(String arg) {
		/*
		String test1 = "one backslash '\\' and one double quote '\"'";
		System.out.println("escaping: "+test1);
		System.out.println("gives: "+escape(test1));
		*/

		imp = IJ.getImage();
		
		if(imp == null)
			IJ.error("There's no image to annotate.");

		pointsSet = new boolean[pointNames.length];
		for(int i = 0; i < pointNames.length; ++i)
			pointsSet[i] = false;
		pointPositions = new double[pointNames.length][3];

		load();

		dialog = new PointsDialog("Marking up: "+imp.getTitle(),
					  pointNames,this);
		for (int i=0;i<pointNames.length;++i) {
			if (pointsSet[i]) {
				dialog.setCoordinateLabel(i,
							  pointPositions[i][0],
							  pointPositions[i][1],
							  pointPositions[i][2]);
			}
		}
	}
}

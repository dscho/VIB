/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package landmarks;

import ij.*;
import ij.process.*;
import ij.io.*;
import ij.gui.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.text.*;

import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.*;

import client.ArchiveClient;
import ij.measure.Calibration;
import java.util.Arrays;
import pal.math.ConjugateDirectionSearch;
import pal.math.MultivariateFunction;
import stacks.ThreePaneCrop;
import util.BatchOpener;
import vib.FastMatrix;
import vib.transforms.FastMatrixTransform;

class PointsDialog extends Dialog implements ActionListener, WindowListener {
	
	Label[] coordinateLabels;
	Button[] markButtons;
	Button[] showButtons;      
	Button[] fineTuneButtons;
	
        Label instructions;
        Panel pointsPanel;
        Panel buttonsPanel;
        Panel templatePanel;
        Panel optionsPanel;
        Checkbox tryManyRotations;

	Name_Points plugin;
	
	ArchiveClient archiveClient;
	
	Label templateFileName;
	Button chooseTemplate;
	
        String defaultInstructions = "Mark the current point selection as:";

	public PointsDialog(String title,
			    NamedPointSet points,
			    ArchiveClient archiveClient,
			    String loadedTemplateFilename,
			    Name_Points plugin) {
		
		super(IJ.getInstance(),title,false);
		
		this.plugin = plugin;
		this.archiveClient = archiveClient;
		
		coordinateLabels = new Label[points.size()];
		markButtons = new Button[points.size()];
	        showButtons = new Button[points.size()];
		fineTuneButtons = new Button[points.size()];
		
		setLayout(new GridBagLayout());
		
		GridBagConstraints outerc=new GridBagConstraints();
		
		Panel instructionsPanel = new Panel();
		pointsPanel = new Panel();
		buttonsPanel = new Panel();
		
		instructions = new Label( defaultInstructions );
		instructionsPanel.setLayout(new BorderLayout());
		instructionsPanel.add(instructions,BorderLayout.WEST);
		
		outerc.gridx = 0;
		outerc.gridy = 0;
		outerc.anchor = GridBagConstraints.LINE_START;
		add(instructionsPanel,outerc);
		
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
			c.anchor = GridBagConstraints.LINE_START;
                        c.gridx = 3;
			fineTuneButtons[counter] = new Button("Fine Tune");
			fineTuneButtons[counter].addActionListener(this);
			fineTuneButtons[counter].setEnabled(true);
			pointsPanel.add(fineTuneButtons[counter],c);
			
			if (p.set)
				setCoordinateLabel(counter,
						   p.x,
						   p.y,
						   p.z);
			++counter;
		}
		
		outerc.gridy = 1;
		outerc.anchor = GridBagConstraints.CENTER;
		add(pointsPanel,outerc);
		
		if( archiveClient == null ) {
			
			saveButton = new Button("Save");
			saveButton.addActionListener(this);
			resetButton = new Button("Reset");
			resetButton.addActionListener(this);
			closeButton = new Button("Close");
			closeButton.addActionListener(this);
			
			buttonsPanel.add(saveButton);		
			buttonsPanel.add(resetButton);
			buttonsPanel.add(closeButton);
			
		} else {
			
			getMyButton = new Button("Get My Most Recent Annotation");
			getMyButton.addActionListener(this);
			getAnyButton = new Button("Get Most Recent Annotation");
			getAnyButton.addActionListener(this);
			uploadButton = new Button("Upload");
			uploadButton.addActionListener(this);
			
			buttonsPanel.add(getMyButton);
			buttonsPanel.add(getAnyButton);
			buttonsPanel.add(uploadButton);
			
		}
		
		outerc.gridy = 2;
		add(buttonsPanel,outerc);
		
		templatePanel=new Panel();
		templatePanel.add(new Label("Template File:"));
		templateFileName = new Label("[None chosen]");
		if( loadedTemplateFilename != null )
			templateFileName.setText(loadedTemplateFilename);
		templatePanel.add(templateFileName);
		chooseTemplate = new Button("Choose");
		chooseTemplate.addActionListener(this);
		templatePanel.add(chooseTemplate);
		
		outerc.gridy = 3;
		outerc.anchor = GridBagConstraints.LINE_START;
		add(templatePanel,outerc);
		
                optionsPanel = new Panel();
                tryManyRotations=new Checkbox(" Try 24 initial starting rotations?",true);
                optionsPanel.add(tryManyRotations);
                outerc.gridy = 4;
                add(optionsPanel,outerc);

		pack();
		setVisible(true);
	}
	
	Button saveButton;
	Button resetButton;
	Button closeButton;	
	
	Button getMyButton;
	Button getAnyButton;
	Button uploadButton;
	
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

        public void setFineTuning( boolean busy ) {
            if( busy ) {
                instructions.setText("Fine tuning... (may take some time)");
                pointsPanel.setEnabled(false);
                buttonsPanel.setEnabled(false);
                templatePanel.setEnabled(false);
                optionsPanel.setEnabled(false);
            } else {
                instructions.setText(defaultInstructions);
                pointsPanel.setEnabled(true);
                buttonsPanel.setEnabled(true);                
                templatePanel.setEnabled(true);
                optionsPanel.setEnabled(true);
            }
        }
	
	@Override
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
		for (int i=0; i < fineTuneButtons.length; ++i) {
			if(source == fineTuneButtons[i]) {
				plugin.fineTune(i);
				break;
			}
		}		
		if(source == closeButton) {
			dispose();
		} else if (source == saveButton) {
			plugin.save();
		} else if (source == resetButton) {
			plugin.reset();
		} else if (source == uploadButton) {
			plugin.upload();
		} else if (source == getMyButton ) {
			plugin.get( true );
		} else if (source == getAnyButton ) {
			plugin.get( false );
		} else if (source == chooseTemplate ) {
			
			OpenDialog od;
			
			od = new OpenDialog("Select template image file...",
					    null,
					    null );
			
			String fileName = od.getFileName();
			String directory = od.getDirectory();
			
			if( fileName == null ) {
				return;
			}
			
			String fullFileName=directory+fileName;
			
			if( plugin.useTemplate(fullFileName) ) {
				templateFileName.setText(fullFileName);
				pack();
			}
			
		}
		
	}

	public void windowClosing( WindowEvent e ) {
		plugin.stopFineTuneThread();
		dispose();
	}
	
	public void windowActivated( WindowEvent e ) { }
	public void windowDeactivated( WindowEvent e ) { }
	public void windowClosed( WindowEvent e ) { }
	public void windowOpened( WindowEvent e ) { }
	public void windowIconified( WindowEvent e ) { }
	public void windowDeiconified( WindowEvent e ) { }    

}

public class Name_Points implements PlugIn {
	
        String templateImageFilename="/home/mark/arnim-brain/CantonF41c.grey";
	ImagePlus templateImage;
        NamedPointSet templatePoints;
	
        double x_spacing;
        double y_spacing;
        double z_spacing;

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
		points.showAsROI(i, imp);
	}
	
	void fineTune(int i) {

                if( fineTuneThread != null ) {
                    IJ.error("There's already a point being fine tuned at the moment.");
                    return;
                }
		
		NamedPoint p = points.get(i);
		if (p == null) {
			IJ.error("You must have set a point in order to fine-tune it.");
			return;
		}

		String pointName = p.getName();

                if( templatePoints == null ) {
                    IJ.error("You must have a template file loaded in order to fine tune.");
                    return;
                }
		
		NamedPoint pointInTemplate = templatePoints.getPoint(pointName);

		if( pointInTemplate == null ) {
			IJ.error("The point you want to fine-tune must be set both in this image and the template.  \""+pointName+"\" is not set in the template.");
			return;
		}

		// We need at least 3 points in common between the two point sets.

		ArrayList<String> namesInCommon = points.namesSharedWith(templatePoints);
		if( namesInCommon.size() < 3 ) {
			IJ.error("There must be at least 2 other points in common between the two images in order to fine-tune this point.");
			return;
		}

                dialog.setFineTuning(true);
		
		// Get a small image from around that point...
		Calibration c = templateImage.getCalibration();
		
		double x_spacing_template = c.pixelWidth;
		double y_spacing_template = c.pixelHeight;
		double z_spacing_template = c.pixelDepth;
		
		double real_x_template = pointInTemplate.x * x_spacing_template;
		double real_y_template = pointInTemplate.y * y_spacing_template;
		double real_z_template = pointInTemplate.z * z_spacing_template;
		
		double templateCubeSide = 50;
		
		double x_min_template = real_x_template - (templateCubeSide / 2);
		double x_max_template = real_x_template + (templateCubeSide / 2);
		double y_min_template = real_y_template - (templateCubeSide / 2);
		double y_max_template = real_y_template + (templateCubeSide / 2);
		double z_min_template = real_z_template - (templateCubeSide / 2);
		double z_max_template = real_z_template + (templateCubeSide / 2);
		
		int x_min_template_i = (int) (x_min_template / x_spacing_template);
		int x_max_template_i = (int) (x_max_template / x_spacing_template);
		int y_min_template_i = (int) (y_min_template / y_spacing_template);
		int y_max_template_i = (int) (y_max_template / y_spacing_template);
		int z_min_template_i = (int) (z_min_template / z_spacing_template);
		int z_max_template_i = (int) (z_max_template / z_spacing_template);
		
		ImagePlus cropped = ThreePaneCrop.performCrop(templateImage, x_min_template_i, x_max_template_i, y_min_template_i, y_max_template_i, z_min_template_i, z_max_template_i, false);
		
                double [] guessedRotation = null;
		
		if( ! dialog.tryManyRotations.getState() ) {
                    // We could try to pick the other points we want to use
                    // in a more subtle way, but for the moment just pick
                    // the first two which are in common.

                    String [] otherPoints=new String[2];

                    int addAtIndex = 0;
                    for( Iterator<String> nameIterator = namesInCommon.iterator();
                         nameIterator.hasNext(); ) {

			    String otherName = nameIterator.next();
			    if (pointName.equals(otherName)) {
				    continue;
			    }
			    otherPoints[addAtIndex++] = otherName;
			    if (addAtIndex >= 2) {
				    break;
			    }
                    }

                    System.out.println("... calculating vector to: "+otherPoints[0]);
                    System.out.println("... and: "+otherPoints[1]);

                    NamedPoint inThis1=points.getPoint(otherPoints[0]);
                    NamedPoint inThis2=points.getPoint(otherPoints[1]);

                    NamedPoint inTemplate1=templatePoints.getPoint(otherPoints[0]);
                    NamedPoint inTemplate2=templatePoints.getPoint(otherPoints[1]);

                    double inThisX = p.x * x_spacing;
                    double inThisY = p.y * y_spacing;
                    double inThisZ = p.z * z_spacing;
                    
                    double inThis1X = inThis1.x * x_spacing;
                    double inThis1Y = inThis1.y * y_spacing;
                    double inThis1Z = inThis1.z * z_spacing;

                    double inThis2X = inThis2.x * x_spacing;
                    double inThis2Y = inThis2.y * y_spacing;
                    double inThis2Z = inThis2.z * z_spacing;

                    double inTemplateX = pointInTemplate.x * x_spacing_template;
                    double inTemplateY = pointInTemplate.y * y_spacing_template;
                    double inTemplateZ = pointInTemplate.z * z_spacing_template;

                    double inTemplate1X = inTemplate1.x * x_spacing_template;
                    double inTemplate1Y = inTemplate1.y * y_spacing_template;
                    double inTemplate1Z = inTemplate1.z * z_spacing_template;

                    double inTemplate2X = inTemplate2.x * x_spacing_template;
                    double inTemplate2Y = inTemplate2.y * y_spacing_template;
                    double inTemplate2Z = inTemplate2.z * z_spacing_template;

                    double [] inThisTo1 = new double[3];
                    double [] inThisTo2 = new double[3];

                    double [] inTemplateTo1 = new double[3];
                    double [] inTemplateTo2 = new double[3];

                    inThisTo1[0] = inThis1.x - inThisX;
                    inThisTo1[1] = inThis1.y - inThisY;
                    inThisTo1[2] = inThis1.z - inThisZ;

                    inThisTo2[0] = inThis2.x - inThisX;
                    inThisTo2[1] = inThis2.y - inThisY;
                    inThisTo2[2] = inThis2.z - inThisZ;

                    inTemplateTo1[0] = inTemplate1.x - inTemplateX;
                    inTemplateTo1[1] = inTemplate1.y - inTemplateY;
                    inTemplateTo1[2] = inTemplate1.z - inTemplateZ;

                    inTemplateTo2[0] = inTemplate2.x - inTemplateX;
                    inTemplateTo2[1] = inTemplate2.y - inTemplateY;
                    inTemplateTo2[2] = inTemplate2.z - inTemplateZ;

                    FastMatrix r=FastMatrix.rotateToAlignVectors(inTemplateTo1, inTemplateTo2, inThisTo1, inThisTo2);

                    guessedRotation=new double[6];
                    r.guessEulerParameters(guessedRotation);

                    System.out.println("guessed euler 0 degrees: "+((180*guessedRotation[0])/Math.PI));
                    System.out.println("guessed euler 1 degrees: "+((180*guessedRotation[1])/Math.PI));
                    System.out.println("guessed euler 2 degrees: "+((180*guessedRotation[2])/Math.PI));

                    System.out.println("my inferred r is: "+r);

                    FastMatrix rAnotherWay = FastMatrix.rotateEuler(guessedRotation[0],
                                                                    guessedRotation[1],
                                                                    guessedRotation[2]);

                    System.out.println("another r is:   "+rAnotherWay);

		}

                fineTuneThread = new FineTuneThread(
                        CORRELATION,
                        templateCubeSide,
                        cropped,
                        templateImage,
                        pointInTemplate,
                        imp,
                        p,
                        dialog.tryManyRotations.getState() ? null : guessedRotation,
                        this);

                fineTuneThread.start();
                        
	}
		
        FineTuneThread fineTuneThread;

        void fineTuneResults( RegistrationResult [] results ) {
            dialog.setFineTuning(false);
            fineTuneThread = null;
        }

	static void printParameters( double [] parameters ) {
		System.out.println( "  z1: "+parameters[0] );
		System.out.println( "  x1: "+parameters[1] );
		System.out.println( "  z2: "+parameters[2] );
		System.out.println( "  z1 degrees: "+((180 *parameters[0])/Math.PI) );
		System.out.println( "  z1 degrees: "+((180 *parameters[1])/Math.PI) );
		System.out.println( "  z1 degrees: "+((180 *parameters[2])/Math.PI) );
		System.out.println( "  tx: "+parameters[3]);
		System.out.println( "  ty: "+parameters[4]);
		System.out.println( "  tz: "+parameters[5]);
	}
	
	public static final int MEAN_ABSOLUTE_DIFFERENCES     = 1;
	public static final int MEAN_SQUARED_DIFFERENCES      = 2;
	public static final int CORRELATION                   = 3;
	public static final int NORMALIZED_MUTUAL_INFORMATION = 4;
	
	public static final String [] methodName = {
		"UNSET!",
		"mean abs diffs",
		"mean squ diffs",
		"correlation",
		"norm mut inf"
	};
	
	static RegistrationResult mapImageWith( ImagePlus toTransform, ImagePlus toKeep, NamedPoint guessedPoint, double[] mapValues, double cubeSide, int similarityMeasure, boolean show, String imageTitle ) {
		
		double sumSquaredDifferences = 0;
                double sumAbsoluteDifferences = 0;
		long numberOfPoints = 0;
		double sumX = 0;
		double sumY = 0;
		double sumXY = 0;
		double sumXSquared = 0;
		double sumYSquared = 0;
		
		FastMatrix scalePointInToTransform = FastMatrix.fromCalibration(toTransform);
		FastMatrix scalePointInToKeep = FastMatrix.fromCalibration(toKeep);
		FastMatrix scalePointInToKeepInverse = scalePointInToKeep.inverse();
		
		FastMatrix backToOriginBeforeRotation = FastMatrix.translate(-cubeSide / 2, -cubeSide / 2, -cubeSide / 2);
		
		double z1 = mapValues[0];
		double x1 = mapValues[1];
		double z2 = mapValues[2];
		double tx = mapValues[3];
		double ty = mapValues[4];
		double tz = mapValues[5];
		
		FastMatrix rotateFromValues = FastMatrix.rotateEuler(z1, x1, z2);
		FastMatrix transformFromValues = FastMatrix.translate(tx, ty, tz);
		
		FastMatrixTransform m = new FastMatrixTransform(scalePointInToTransform);
		m = m.composeWithFastMatrix(backToOriginBeforeRotation);
		m = m.composeWithFastMatrix(rotateFromValues);
		m = m.composeWithFastMatrix(transformFromValues);
		m = m.composeWithFastMatrix(scalePointInToKeepInverse);
		
		// Now transform the corner points to find the maximum and minimum
		// extents of the transformed image.
		int w = toTransform.getWidth();
		int h = toTransform.getHeight();
		int d = toTransform.getStackSize();
		
		int[][] corners = {{0, 0, 0}, {w, 0, 0}, {0, h, 0}, {0, 0, d}, {w, 0, d}, {0, h, d}, {w, h, 0}, {w, h, d}};
		
		double xmin = Double.MAX_VALUE;
		double xmax = Double.MIN_VALUE;
		double ymin = Double.MAX_VALUE;
		double ymax = Double.MIN_VALUE;
		double zmin = Double.MAX_VALUE;
		double zmax = Double.MIN_VALUE;
		
		for (int i = 0; i < corners.length; ++i) {
			m.apply(corners[i][0], corners[i][1], corners[i][2]);
			if (m.x < xmin) {
				xmin = m.x;
			}
			if (m.x > xmax) {
				xmax = m.x;
			}
			if (m.y < ymin) {
				ymin = m.y;
			}
			if (m.y > ymax) {
				ymax = m.y;
			}
			if (m.z < zmin) {
				zmin = m.z;
			}
			if (m.z > zmax) {
				zmax = m.z;
			}
		}
		
		int transformed_x_min = (int) Math.floor(xmin);
		int transformed_y_min = (int) Math.floor(ymin);
		int transformed_z_min = (int) Math.floor(zmin);
		
		int transformed_x_max = (int) Math.ceil(xmax);
		int transformed_y_max = (int) Math.ceil(ymax);
		int transformed_z_max = (int) Math.ceil(zmax);
		
		/*
		System.out.println("x min, max: " + transformed_x_min + "," + transformed_x_max);
		System.out.println("y min, max: " + transformed_y_min + "," + transformed_y_max);
		System.out.println("z min, max: " + transformed_z_min + "," + transformed_z_max);
		*/

		int transformed_width = (transformed_x_max - transformed_x_min) + 1;
		int transformed_height = (transformed_y_max - transformed_y_min) + 1;
		int transformed_depth = (transformed_z_max - transformed_z_min) + 1;
		
		// System.out.println("transformed dimensions: " + transformed_width + "," + transformed_height + "," + transformed_depth);
		
		int k_width = toKeep.getWidth();
		int k_height = toKeep.getHeight();
		int k_depth = toKeep.getStackSize();
		
		byte[][] toKeepCroppedBytes = new byte[transformed_depth][transformed_height * transformed_width];
		
		ImageStack toKeepStack = toKeep.getStack();
		for (int z = 0; z < transformed_depth; ++z) {
			int z_uncropped = z + transformed_z_min;
			if ((z_uncropped < 0) || (z_uncropped >= k_depth)) {
				continue;
			}
			byte[] slice_pixels = (byte[]) toKeepStack.getPixels(z_uncropped+1);
			for (int y = 0; y < transformed_height; ++y) {
				for (int x = 0; x < transformed_width; ++x) {
					int x_uncropped = transformed_x_min + x;
					int y_uncropped = transformed_y_min + y;
					if ((x_uncropped < 0) || (x_uncropped >= k_width) || (y_uncropped < 0) || (y_uncropped >= k_height)) {
						continue;
					}
					toKeepCroppedBytes[z][y * transformed_width + x] = slice_pixels[y_uncropped * k_width + x_uncropped];
				}
			}
		}
		
		ImageStack toTransformStack=toTransform.getStack();
		byte [][] toTransformBytes=new byte[d][];
		for( int z_s = 0; z_s < d; ++z_s)
			toTransformBytes[z_s]=(byte[])toTransformStack.getPixels(z_s+1);
		
                FastMatrix back_to_template = m.inverse();
		
		byte [][] transformedBytes = new byte[transformed_depth][transformed_height * transformed_width];
		
		for( int z = 0; z < transformed_depth; ++z ) {
			for( int y = 0; y < transformed_height; ++y ) {
				for( int x = 0; x < transformed_width; ++x ) {
					
					int x_in_original = x + transformed_x_min;
					int y_in_original = y + transformed_y_min;
					int z_in_original = z + transformed_z_min;
					
					// System.out.println("in original: "+x_in_original+","+y_in_original+","+z_in_original);
					
					back_to_template.apply(
						x_in_original,
						y_in_original,
						z_in_original );
					
					int x_in_template = (int)back_to_template.x;
					int y_in_template = (int)back_to_template.y;
					int z_in_template = (int)back_to_template.z;
					
					// System.out.print("Got back *_in_template "+x_in_template+","+y_in_template+","+z_in_template);
					
					if( (x_in_template < 0) || (x_in_template >= w) ||
					    (y_in_template < 0) || (y_in_template >= h) ||
					    (z_in_template < 0) || (z_in_template >= d) ) {
						// System.out.println("skipping");
						continue;
					}
					// System.out.println("including");
					
					int value=toTransformBytes[z_in_template][y_in_template*w+x_in_template]&0xFF;
					
					transformedBytes[z][y*transformed_width+x]=(byte)value;
					
					int valueInOriginal = toKeepCroppedBytes[z][y*transformed_width+x] &0xFF;
					
					int difference = Math.abs( value - valueInOriginal );
					int differenceSquared = difference * difference;
					
					sumAbsoluteDifferences += difference;
					sumSquaredDifferences += differenceSquared;
					
					sumX += value;
					sumXSquared += value * value;
					
					sumY += valueInOriginal;
					sumYSquared += valueInOriginal * valueInOriginal;
					
					sumXY += value * valueInOriginal;			
					
					++numberOfPoints;
					
				}
			}
		}
		
		ImageStack overlayedStack=new ImageStack(transformed_width,transformed_height);		
		
		for (int z = 0; z < transformed_depth; ++z) {
			ColorProcessor cp=new ColorProcessor(transformed_width,transformed_height);
			cp.setRGB( transformedBytes[z],
				   toKeepCroppedBytes[z],
				   transformedBytes[z] );
			overlayedStack.addSlice("",cp);
		}
		
		ImagePlus overlayed=new ImagePlus(imageTitle,overlayedStack);
                if(show)
			overlayed.show();
		
                RegistrationResult result = new RegistrationResult();
		
		result.overlayed = overlayed;
		result.parameters = mapValues;
		
		double maximumValue = 0;
		
                switch(similarityMeasure) {
			
		case MEAN_ABSOLUTE_DIFFERENCES:
			maximumValue = 255;
			break;
			
		case MEAN_SQUARED_DIFFERENCES:
			maximumValue = 255 * 255;
			break;
			
		case CORRELATION:
			maximumValue = 2;
			break;
			
		case NORMALIZED_MUTUAL_INFORMATION:
			maximumValue = 1;
			break;
			
		default:
			assert false : "Unknown similarity measure: "+similarityMeasure;
			
		}
		
		double pointDrift;
		
		{
			// Map the centre of the cropped template with this
			// transformation and see how far away it is from the
			// guessed point.
			
			int centre_cropped_template_x = toTransform.getWidth() / 2;
			int centre_cropped_template_y = toTransform.getHeight() / 2;
			int centre_cropped_template_z = toTransform.getStackSize() / 2;
			
			m.apply( centre_cropped_template_x,
				 centre_cropped_template_y,
				 centre_cropped_template_z );
			
			double xdiff = m.x - guessedPoint.x;
			double ydiff = m.y - guessedPoint.y;
			double zdiff = m.z - guessedPoint.z;
			
			double pointDriftSquared =
				(xdiff * xdiff) + (ydiff * ydiff) + (zdiff * zdiff);
			
			pointDrift = Math.sqrt(pointDriftSquared);
			
		}
		
		// Now use the logistic function to scale up the penalty
		// as we get further away in translation...
		
		double minimumPenaltyAt = 0.8;
		double maximumPenaltyAt = 1.0;
		double midPoint = (minimumPenaltyAt + maximumPenaltyAt) / 2;
		
		double proportionOfCubeSideAway = pointDrift / cubeSide;
		
		// When t is 6 or more, the maximum applies...
		
		double scaleUpT = 6.0 / (maximumPenaltyAt - midPoint);
		
		double additionalTranslationalPenalty = 1 / (1 + Math.exp( -(proportionOfCubeSideAway - midPoint) * scaleUpT));
		additionalTranslationalPenalty *= maximumValue;
		
		/* Also use the logistic function to penalize the
		   rotation from getting too near to the extrema: 4PI
		   and -4PI. */
		
		double absz1 = Math.abs(z1);
		double absx1 = Math.abs(x1);
		double absz2 = Math.abs(z2);
		
		double mostExtremeAngle =  Math.max(Math.max(absz1,absx1),absz2);
		
		minimumPenaltyAt = (7 * Math.PI) / 2;
		maximumPenaltyAt = 4 * Math.PI;
		midPoint = (maximumPenaltyAt + minimumPenaltyAt) / 2;
		
		double angleFromMid = mostExtremeAngle - midPoint;
		
		scaleUpT = 6.0 / (Math.PI / 4);
		
		double additionalAnglePenalty = 1 / (1 + Math.exp( -angleFromMid * scaleUpT ) );
		additionalAnglePenalty *= maximumValue;
		
		if( numberOfPoints == 0 ) {
			// This should be unneccessary, since there
			// are heavy penalties for moving towards the
			// point of no overlap.
			result.score = maximumValue;
		} else {
			
			switch(similarityMeasure) {
				
			case MEAN_ABSOLUTE_DIFFERENCES:
				result.score = sumAbsoluteDifferences / numberOfPoints;
				break;
				
			case MEAN_SQUARED_DIFFERENCES:
				result.score = sumSquaredDifferences / numberOfPoints;
				break;
				
			case CORRELATION:
				double n2 = numberOfPoints * numberOfPoints;
				double numerator = (sumXY/numberOfPoints) - (sumX * sumY) / n2;
				double varX = (sumXSquared / numberOfPoints) - (sumX * sumX) / n2;
				double varY = (sumYSquared / numberOfPoints) - (sumY * sumY) / n2;
				double denominator = Math.sqrt(varX)*Math.sqrt(varY);
				if( denominator <= 0.00000001 ) {
					// System.out.println("Fixing near zero correlation denominator: "+denominator);
					result.score = 0;
				} else {
					result.score = numerator / denominator;
				}
				// System.out.println("raw correlation is: "+result.score);
				/* The algorithm tries to minimize the
				   score, and we want a correlation
				   close to 1, change the score somewhat:
				*/
				result.score = 1 - result.score;
				break;
				
			case NORMALIZED_MUTUAL_INFORMATION:
				assert false : "Mutual information measure not implemented yet";
				break;
				
			}
		}
		
		result.score += additionalAnglePenalty;
		result.score += additionalTranslationalPenalty;
		
		return result;
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
		
		if( ! points.savePointsFile( savePath ) )
			IJ.error("Error saving to: "+savePath+"\n");
		
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
			
			int x = canvas.offScreenX(p.xpoints[0]);
			int y = canvas.offScreenY(p.ypoints[0]);
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
			
			NamedPoint point = points.get(i);
			point.x = x;
			point.y = y;
			point.z = z;
			point.set = true;
			
		} else {
			IJ.error("You must have a current point selection in "+
				 imp.getTitle()+" in order to mark points.");
		}
		
	}
	
	public void get( boolean mineOnly ) {
		
		Hashtable<String,String> parameters = new Hashtable<String,String>();
		
		parameters.put("method","most-recent-annotation");
		parameters.put("type","points");
		parameters.put("variant","around-central-complex");
		parameters.put("md5sum",archiveClient.getValue("md5sum"));
		if( mineOnly )
			parameters.put("for_user",archiveClient.getValue("user"));
		else
			parameters.put("for_user","");
		
		// Need to included data too....
		
		ArrayList< String [] > tsv_results = archiveClient.synchronousRequest( parameters, null );
		
		String [] first_line = tsv_results.get(0);
		int urls_found;
		String bestUrl = null;
		if( first_line[0].equals("success") ) {
			urls_found = Integer.parseInt(first_line[1]);
			if( urls_found == 0 )
				IJ.error( "No anntation files by " + (mineOnly ? archiveClient.getValue("user") : "any user") + " found." );
			else {
				bestUrl = (tsv_results.get(1))[1];
				// IJ.error( "Got the URL: " + bestUrl );
			}
		} else if( first_line[0].equals("error") ) {
			IJ.error("There was an error while getting the most recent annotation: "+first_line[1]);
		} else {
			IJ.error("There was an unknown response to request for an annotation file: " + first_line[0]);
		}
		
		// Now fetch that file:
		
		// FIXME:
		
		if( bestUrl == null )
			return;
		
		String fileContents =  ArchiveClient.justGetFileAsString( bestUrl );
		
		if( fileContents != null )
			loadFromString(fileContents);
		
	}
	
	public void upload() {
		
		Hashtable<String,String> parameters = new Hashtable<String,String>();
		
		parameters.put("method","upload-annotation");
		parameters.put("type","points");	       
		parameters.put("variant","around-central-complex");
		parameters.put("md5sum",archiveClient.getValue("md5sum"));
		
		// Need to included data too....
		
		byte [] fileAsBytes = points.dataAsBytes( );
		
		ArrayList< String [] > tsv_results = archiveClient.synchronousRequest( parameters, fileAsBytes );
		
		String [] first_line = tsv_results.get(0);
		if( first_line[0].equals("success") ) {
			IJ.error("Annotations uploaded successfully!");
		} else if( first_line[0].equals("error") ) {
			IJ.error("There was an error while uploading the annotation file: "+first_line[1]);
		} else {
			IJ.error("There was an unknown response to the annotation file upload request: " + first_line[0]);
		}
		
	}
	
	public Name_Points() {
		
	}
	
	PointsDialog dialog;
	ImagePlus imp;
	
	NamedPointSet points;
	
	ArchiveClient archiveClient;
	
	ImageCanvas canvas;
	
	public void run( String arguments ) {
		
		Applet applet = IJ.getApplet();
		if( applet != null ) {
			archiveClient=new ArchiveClient( applet );
		}
		
		String macroOptions=Macro.getOptions();
		String templateParameter = null;
                if( macroOptions != null )
                    templateParameter = Macro.getValue(macroOptions,"template",null);
		
		/*
		  String test1 = "one backslash '\\' and one double quote '\"'";
		  System.out.println("escaping: "+test1);
		  System.out.println("gives: "+escape(test1));
		*/
		
		if( archiveClient != null ) {
			
			// We go for a channel that's tagged 'nc82'
			
			Hashtable<String,String> parameters = new Hashtable<String,String>();
			parameters.put("method","channel-tags");
			parameters.put("md5sum",archiveClient.getValue("md5sum"));
			
			ArrayList< String [] > tsv_results = archiveClient.synchronousRequest(parameters,null);
			int tags = Integer.parseInt((tsv_results.get(0))[1]); // FIXME error checking
			int nc82_channel = -1;
			for( int i = 0; i < tags; ++i ) {
				String [] row = tsv_results.get(i);
				if( "nc82".equals(row[1]) ) {
					nc82_channel = Integer.parseInt(row[0]);
					break;
				}
			}
			if( nc82_channel < 0 ) {
				
				imp = IJ.getImage();
				
				if(imp == null) {
					IJ.error("There's no image to annotate.");
					return;
				}
				
			} else {
				
				// Look for the one with the right name...
				String lookFor = "Ch"+(nc82_channel+1);
				
				int[] wList = WindowManager.getIDList();
				if (wList==null) {
					IJ.error("Name_Points: no images have been loaded");
					return;
				}
				
				for (int i=0; i<wList.length; i++) {
					ImagePlus tmpImp = WindowManager.getImage(wList[i]);
					String title = tmpImp!=null?tmpImp.getTitle():"";
					int indexOfChannel = title.indexOf(lookFor);
					if( indexOfChannel < 0 ) {
						tmpImp.close();
					}
				}
				
				imp = IJ.getImage();
				
				if(imp == null) {
					IJ.error("There's no image to annotate.");
					return;
				}
				
			}
			
		} else {
			
			imp = IJ.getImage();
			
			if(imp == null) {
				IJ.error("There's no image to annotate.");
				return;
			}
			
		}
		
                Calibration c=imp.getCalibration();
                this.x_spacing=c.pixelWidth;
                this.y_spacing=c.pixelHeight;
                this.z_spacing=c.pixelDepth;

		canvas = imp.getCanvas();
		
		/*
		  ImagePlus [] templateChannels=BatchOpener.open(templateImageFilename);
		  if( templateChannels != null ) {
		  templateImage = templateChannels[0];
		  templatePoints = NamedPointSet.forImage(templateImageFilename);
		  }
		*/
		
		points = new NamedPointSet();
		for (int i = 0; i < defaultPointNames.length; ++i)
			points.add(new NamedPoint(defaultPointNames[i]));
		
		if( applet == null )
			loadAtStart();
		
		boolean loadedTemplate = false;
		
		if( (templateParameter != null) && useTemplate(templateParameter) ) {
			loadedTemplate = true;
		}
		
		dialog = new PointsDialog( "Marking up: "+imp.getTitle(),
					   points,
					   archiveClient,
					   loadedTemplate ? templateParameter : null,
					   this );
		
	}
	
	public void loadAtStart() {
		
		NamedPointSet newNamedPoints = NamedPointSet.forImage(imp);
		
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
	
	public void loadFromString(String fileContents) {
		
		NamedPointSet newNamedPoints = NamedPointSet.fromString(fileContents);
		
		dialog.resetAll();
		
		ListIterator i;
		for (i = newNamedPoints.listIterator();i.hasNext();) {
			NamedPoint current = (NamedPoint)i.next();
			int foundIndex = -1;
			for( int j = 0; j < points.size(); ++j ) {
				NamedPoint p = points.get(j);
				if (current.getName().equals(p.getName())) {
					dialog.setCoordinateLabel(j,current.x,current.y,current.z);
					NamedPoint point = points.get(j);
					point.x = current.x;
					point.y = current.y;
					point.z = current.z;
					point.set = true;
					break;
				}
			}
			if( foundIndex < 0 )
				points.add(current);
		}
		
	}
	
	public boolean useTemplate( String templateImageFileName ) {
		
		File file=new File(templateImageFileName);
		if( ! file.exists() ) {
			IJ.error("The file "+templateImageFileName+" doesn't exist.");
			return false;
		}
		
		String pointsFileName=templateImageFileName+".points";
		
		File pointsFile=new File(pointsFileName);
		
		if( ! pointsFile.exists() ) {
			IJ.error("There's no corresponding points file for that image.  It must be called "+pointsFile.getAbsolutePath());
			return false;
		}
		
		NamedPointSet templatePointSet=NamedPointSet.forImage(templateImageFileName);
		System.out.println("point set was: "+templatePointSet);
		if( templatePointSet == null ) {
			return false;
		}
		ImagePlus [] channels = BatchOpener.open(templateImageFileName);
		if( channels == null ) {
			IJ.error("Couldn't open template image: "+templateImageFileName );
			return false;
		}
		
		this.templateImage = channels[0];
		this.templatePoints = templatePointSet;
		
		return true;
	}

        void stopFineTuneThread() {
            if( fineTuneThread != null )
                fineTuneThread.askToFinish();
        }
	
}

class TransformationAttempt implements MultivariateFunction {
	
        double cubeSide;
        ImagePlus croppedTemplate;
        NamedPoint templatePoint;
        ImagePlus newImage;
        NamedPoint guessedPoint;
	
        double minTranslation;
        double maxTranslation;
	
	int similarityMeasure;
	
        public TransformationAttempt( double cubeSide,
                                      ImagePlus croppedTemplate,
                                      NamedPoint templatePoint,
                                      ImagePlus newImage,
                                      NamedPoint guessedPoint,
				      int similarityMeasure ) {
		
                this.cubeSide = cubeSide;
                this.croppedTemplate = croppedTemplate;
                this.templatePoint = templatePoint;
                this.newImage = newImage;
                this.guessedPoint = guessedPoint;
		
		this.similarityMeasure = similarityMeasure;
		
                minTranslation = -cubeSide;
                // Find what the maximum translation could be:
                Calibration c=newImage.getCalibration();
                maxTranslation = 0;
                double max_x = newImage.getWidth() * c.pixelWidth;
                double max_y = newImage.getHeight() * c.pixelHeight;
                double max_z = newImage.getStackSize() * c.pixelDepth;
                if( max_x > maxTranslation ) maxTranslation = max_x;
                if( max_y > maxTranslation ) maxTranslation = max_y;
                if( max_z > maxTranslation ) maxTranslation = max_z;
                maxTranslation += cubeSide;	
        }
	
        public double evaluate( double[] argument ) {
		
		RegistrationResult r = Name_Points.mapImageWith( croppedTemplate, newImage, guessedPoint, argument, cubeSide, similarityMeasure, false, "");
		return r.score;
		
        }
	
        public int getNumArguments() {
                return 6;
        }
	
        public double getLowerBound(int n) {
                if( (n >= 0) && (n <= 2) ) {
			// i.e. it's an angle...
			return -4 * Math.PI;
                } else /* if( (n >= 3) && (n <= 5) ) */ {
			// i.e. it's a translation...
			return minTranslation;
                }   
        }
	
        public double getUpperBound(int n) {
                if( (n >= 0) && (n <= 2) ) {
			// i.e. it's an angle...
			return 4 * Math.PI;
                } else /* if( (n >= 3) && (n <= 5) ) */ {
			// i.e. it's a translation...
			return maxTranslation;
                }
        }
	
}

class RegistrationResult implements Comparable {
	
	double score;
	ImagePlus overlayed;
	double[] parameters;
	
	@Override
	public int compareTo(Object otherRegistrationResult) {
		RegistrationResult other = (RegistrationResult) otherRegistrationResult;
		return Double.compare(score, other.score);
	}
	
	@Override
	public String toString() {
		return "score: " + score + " for parameters: " + parameters;
	}
}


class FineTuneThread extends Thread {

        int method;
        double cubeSide;
        ImagePlus croppedTemplate;
        ImagePlus template;
        NamedPoint templatePoint;
        ImagePlus newImage;
        NamedPoint guessedPoint;
        double [] guessedRotation;
        Name_Points plugin;

        public FineTuneThread( 
		int method,
		double cubeSide,
                ImagePlus croppedTemplate,
                ImagePlus template,
                NamedPoint templatePoint,
                ImagePlus newImage,
                NamedPoint guessedPoint,
                double [] guessedRotation,
                Name_Points plugin ) {

                this.method = method;
                this.cubeSide = cubeSide;
                this.croppedTemplate = croppedTemplate;
                this.template = template;
                this.templatePoint = templatePoint;
                this.newImage = newImage;
                this.guessedPoint = guessedPoint;
                this.guessedRotation = guessedRotation;
                this.plugin = plugin;

        }

        public void setResults( RegistrationResult [] results ) {
            System.out.println("setResults was called with: "+results);
            plugin.fineTuneResults( results );
        }
	
	public void run() {

		croppedTemplate.show();
		
		FastMatrix scalePointInTemplate=FastMatrix.fromCalibration(croppedTemplate);
		FastMatrix scalePointInNewImage=FastMatrix.fromCalibration(newImage);
		FastMatrix inverseScalePointInNewImage=scalePointInNewImage.inverse();
		
		scalePointInTemplate.apply( templatePoint.x, templatePoint.y, templatePoint.z );
		scalePointInNewImage.apply( guessedPoint.x, guessedPoint.y, guessedPoint.z );
		
		double initial_trans_x = scalePointInNewImage.x;
		double initial_trans_y = scalePointInNewImage.y;
		double initial_trans_z = scalePointInNewImage.z;

		if( guessedRotation == null ) {
			
			RegistrationResult [] results = new RegistrationResult[24];
			
			ImagePlus bestSoFar = null;
			
			IJ.showProgress(0.01);
			
                        /* We want to generate all possible rigid rotations
                           of one axis onto another.  So, the x axis can be
                           mapped on to one of 6 axes.  Then the y axis can
                           be mapped on to one of 4 axes.  The z axis can
                           then only be mapped onto 1 axis if the handedness
                           is to be preserved. */

			int rotation;
			for( rotation = 0; rotation < 24; ++rotation ) {

                                int firstAxis = rotation / 8;
                                int firstAxisParity = 2 * ((rotation / 4) % 2) - 1;
                                int secondAxisInformation = rotation % 4;
                                int secondAxisIncrement = 1 + (secondAxisInformation / 2);
                                int secondAxisParity = 2 * (secondAxisInformation % 2) - 1;
                                int secondAxis = (firstAxis + secondAxisIncrement) % 3;
                                
                                double [] xAxisMappedTo = new double[3];
                                double [] yAxisMappedTo = new double[3];

                                xAxisMappedTo[firstAxis] = firstAxisParity;
                                yAxisMappedTo[secondAxis] = secondAxisParity;

                                double [] zAxisMappedTo = FastMatrix.crossProduct( xAxisMappedTo, yAxisMappedTo );
                                
                                System.out.println("x axis mapped to: "+xAxisMappedTo[0]+","+xAxisMappedTo[1]+","+xAxisMappedTo[2]);
                                System.out.println("y axis mapped to: "+yAxisMappedTo[0]+","+yAxisMappedTo[1]+","+yAxisMappedTo[2]);
                                System.out.println("z axis mapped to: "+zAxisMappedTo[0]+","+zAxisMappedTo[1]+","+zAxisMappedTo[2]);
                        
                                double [][] m = new double[3][4];
                                
                                m[0][0] = xAxisMappedTo[0];
                                m[1][0] = xAxisMappedTo[1];
                                m[2][0] = xAxisMappedTo[2];

                                m[0][1] = yAxisMappedTo[0];
                                m[1][1] = yAxisMappedTo[1];
                                m[2][1] = yAxisMappedTo[2];

                                m[0][2] = zAxisMappedTo[0];
                                m[1][2] = zAxisMappedTo[1];
                                m[2][2] = zAxisMappedTo[2];

                                FastMatrix rotationMatrix = new FastMatrix(m);
                                double [] eulerParameters = new double[6];
                                rotationMatrix.guessEulerParameters(eulerParameters);
        
				double z1 = eulerParameters[0];
				double x1 = eulerParameters[1];
				double z2 = eulerParameters[2];

				double [] startValues = { z1,
							  x1,
							  z2,
							  initial_trans_x,
							  initial_trans_y,
							  initial_trans_z };
				
				// Now create the optimizer, etc.
				
				ConjugateDirectionSearch optimizer = new ConjugateDirectionSearch();
				
				optimizer.step = 10;
				optimizer.scbd = 10.0;
				optimizer.illc = true;
				
				TransformationAttempt attempt = new TransformationAttempt(
					cubeSide,
					croppedTemplate,
					templatePoint,
					newImage,
					guessedPoint,
					method );

                                if( pleaseStop ) {
                                    setResults(null);
                                    return;
                                }
				
				optimizer.optimize(attempt, startValues, 2, 2);		
				
				// Now it should be optimized such that our result
				// is in startValues.
				
				System.out.println("startValues now: ");
				Name_Points.printParameters(startValues);

                                if( pleaseStop ) {
                                    setResults(null);
                                    return;
                                }
				
				// Now reproduce those results; they might be good...
				
				RegistrationResult r = Name_Points.mapImageWith(
					croppedTemplate,
					newImage,
					guessedPoint,
					startValues,
					cubeSide,
					method,
					false,
					"score: ");

                                if( pleaseStop ) {
                                    setResults(null);
                                    return;
                                }
				
				String scoreString = (method == Name_Points.CORRELATION) ? ("" + r.score) : ("" + (int)Math.round(r.score));
				
				r.overlayed.setTitle(Name_Points.methodName[method]+": "+scoreString);
				
				results[rotation] = r;
				
				IJ.showProgress( (rotation + 1) / 24.0 );
				
			}
			
			IJ.showProgress(1);
			
			Arrays.sort( results, 0, 24 );
			
			for( int i = 0; i < results.length; ++i ) {
				System.out.println("result "+i+" after sorting is: "+results[i]);
			}
			
			for( int i = 0; i < 5; ++i )
				if( results[i] != null )
					results[i].overlayed.show();
			
			setResults( results );
			
		} else { // Just use the initial rotation based on the nearby points...
			
                        guessedRotation[3] = initial_trans_x;
                        guessedRotation[4] = initial_trans_y;
                        guessedRotation[5] = initial_trans_z;

			RegistrationResult [] results = new RegistrationResult[1];
			
			IJ.showProgress(0);
			
                        double [] startValues=guessedRotation;
				
                        // Now create the optimizer, etc.

                        ConjugateDirectionSearch optimizer = new ConjugateDirectionSearch();

                        optimizer.step = 10;
                        optimizer.scbd = 10.0;
                        optimizer.illc = true;

                        TransformationAttempt attempt = new TransformationAttempt(
                                cubeSide,
                                croppedTemplate,
                                templatePoint,
                                newImage,
                                guessedPoint,
                                method );

                        if( pleaseStop ) {
                            setResults(null);
                            return;
                        }

                        optimizer.optimize(attempt, startValues, 2, 2);		

                        // Now it should be optimized such that our result
                        // is in startValues.

                        System.out.println("startValues now: ");
                        Name_Points.printParameters(startValues);

                        if( pleaseStop ) {
                            setResults(null);
                            return;
                        }

                        // Now reproduce those results; they might be good...

                        RegistrationResult r = Name_Points.mapImageWith(
                                croppedTemplate,
                                newImage,
                                guessedPoint,
                                startValues,
                                cubeSide,
                                method,
                                false,
                                "score: ");

                        String scoreString = (method == Name_Points.CORRELATION) ? ("" + r.score) : ("" + (int)Math.round(r.score));

                        r.overlayed.setTitle(Name_Points.methodName[method]+": "+scoreString);

                        results[0] = r;

                        IJ.showProgress( 1 );
			
			for( int i = 0; i < Math.min(5,results.length); ++i )
				if( results[i] != null )
					results[i].overlayed.show();
			
			setResults( results );
		}

	}

        boolean pleaseStop = false;

        public void askToFinish() {
            pleaseStop = true;
        }

}

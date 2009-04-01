/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007 Mark Longair */

/*
  This file is part of the ImageJ plugin "Simple Neurite Tracer".

  The ImageJ plugin "Simple Neurite Tracer" is free software; you
  can redistribute it and/or modify it under the terms of the GNU
  General Public License as published by the Free Software
  Foundation; either version 3 of the License, or (at your option)
  any later version.

  The ImageJ plugin "Simple Neurite Tracer" is distributed in the
  hope that it will be useful, but WITHOUT ANY WARRANTY; without
  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  PARTICULAR PURPOSE.  See the GNU General Public License for more
  details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package tracing;

import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.text.*;
import ij.measure.Calibration;
import ij.io.*;

import ij3d.Image3DUniverse;
import ij3d.Image3DMenubar;
import ij3d.Content;
import ij3d.Pipe;
import javax.vecmath.Color3f;
import ij.gui.GUI;

import java.applet.Applet;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.IndexColorModel;

import java.io.*;

import java.util.Set;
import java.util.ArrayList;
import java.util.Iterator;

import client.ArchiveClient;

import stacks.ThreePanes;

import util.BatchOpener;
import util.RGB_to_Luminance;

import features.GaussianGenerationCallback;
import features.ComputeCurvatures;

import amira.AmiraMeshDecoder;
import amira.AmiraParameters;

import features.Sigma_Palette;
import features.TubenessProcessor;

/* Note on terminology:

      "traces" files are made up of "paths".  Paths are non-branching
      sequences of adjacent points (including diagonals) in the image.
      Branches and joins are supported by attributes of paths that
      specify that they begin on (or end on) other paths.

 */

public class Simple_Neurite_Tracer extends ThreePanes
	implements PlugIn, SearchProgressCallback, FillerProgressCallback, GaussianGenerationCallback {

	public static final String PLUGIN_VERSION = "1.5.0";
	static final boolean verbose = false;

	PathAndFillManager pathAndFillManager;

	boolean use3DViewer;
	Image3DUniverse univ;
	Content imageContent;

	boolean unsavedPaths = false;

	public boolean pathsUnsaved() {
		return unsavedPaths;
	}

	public PathAndFillManager getPathAndFillManager() {
		return pathAndFillManager;
	}

	/* Just for convenience, keep casted references to the
	   superclass's InteractiveTracerCanvas objects: */

	InteractiveTracerCanvas xy_tracer_canvas;
	InteractiveTracerCanvas xz_tracer_canvas;
	InteractiveTracerCanvas zy_tracer_canvas;

	public ImagePlus getImagePlus() {
		return xy;
	}

	/* This overrides the method in ThreePanes... */

	@Override
	public InteractiveTracerCanvas createCanvas( ImagePlus imagePlus, int plane ) {
		return new InteractiveTracerCanvas( imagePlus, this, plane, pathAndFillManager );
	}

	public void cancelSearch( boolean cancelFillToo ) {
		if( currentSearchThread != null )
			currentSearchThread.requestStop();
		endJoin = null;
		endJoinPoint = null;
		if( cancelFillToo && filler != null )
			filler.requestStop();
	}

	public void threadStatus( SearchThread source, int status ) {
		// Ignore this information.
	}

	synchronized public void saveFill( ) {

		if( filler != null ) {
			// The filler must be paused while we save to
			// avoid concurrent modifications...

			if (verbose) System.out.println("["+Thread.currentThread()+"] going to lock filler in plugin.saveFill");
			synchronized(filler) {
				if (verbose) System.out.println("["+Thread.currentThread()+"] acquired it");
				if( FillerThread.PAUSED == filler.getThreadStatus() ) {
					// Then we can go ahead and save:
					pathAndFillManager.addFill( filler.getFill() );
					// ... and then stop filling:
					filler.requestStop();
					resultsDialog.changeState( NeuriteTracerResultsDialog.WAITING_TO_START_PATH );
					filler = null;
				} else {
					IJ.error("The filler must be paused before saving the fill.");
				}

			}
			if (verbose) System.out.println("["+Thread.currentThread()+"] left lock on filler");
		}
	}

	synchronized public void discardFill( ) {
		if( filler != null ) {
			synchronized(filler) {
				filler.requestStop();
				resultsDialog.changeState( NeuriteTracerResultsDialog.WAITING_TO_START_PATH );
				filler = null;
			}
		}
	}

	synchronized public void pauseOrRestartFilling( ) {
		if( filler != null ) {
			filler.pauseOrUnpause( );
		}
	}

	/* Now a couple of callback methods, which get information
	   about the progress of the search. */

	public void finished( SearchThread source, boolean success ) {

		/* This is called by both filler and currentSearchThread,
		   so distinguish these cases: */

		if( source == currentSearchThread ) {

			if( success ) {
				Path result = currentSearchThread.getResult();
				if( result == null ) {
					IJ.error("Bug! Succeeded, but null result.");
					return;
				}
				if( endJoin != null ) {
					result.setEndJoin( endJoin, endJoinPoint );
				}
				setTemporaryPath( result );

				resultsDialog.changeState(NeuriteTracerResultsDialog.QUERY_KEEP);

			} else {

				resultsDialog.changeState(NeuriteTracerResultsDialog.PARTIAL_PATH);
			}

			// Indicate in the dialog that we've finished...

			currentSearchThread = null;

		}

		removeThreadToDraw( source );
		repaintAllPanes();

	}

	public void pointsInSearch( SearchThread source, int inOpen, int inClosed ) {
		// Just use this signal to repaint the canvas, in case there's
		// been no mouse movement.
		repaintAllPanes();
	}

	/* FIXME, just for synchronization - replace this with
	   synchronization on the object it protects: */

	String nonsense = "unused";

	/* These member variables control what we're actually doing -
	   whether that's tracing, logging points or displaying values
	   of the Hessian at particular points.  Currently we only
	   support tracing, support for the others has been
	   removed. */

	boolean setupLog = false;
	boolean setupEv = false;
	boolean setupTrace = false;
	boolean setupPreprocess = false;

	/* If we're timing out the searches (probably not any longer...) */

	boolean setupTimeout = false;
	float   setupTimeoutValue = 0.0f;

	/* For the original file info - needed for loading the
	   corresponding labels file and checking if a "tubes.tif"
	   file already exists: */

	public FileInfo file_info;

	protected int width, height, depth;

	public void justDisplayNearSlices( boolean value, int eitherSide ) {

		xy_tracer_canvas.just_near_slices = value;
		if( ! single_pane ) {
			xz_tracer_canvas.just_near_slices = value;
			zy_tracer_canvas.just_near_slices = value;
		}

		xy_tracer_canvas.eitherSide = eitherSide;
		if( ! single_pane ) {
			xz_tracer_canvas.eitherSide = eitherSide;
			zy_tracer_canvas.eitherSide = eitherSide;
		}

		repaintAllPanes();

	}

	public void setCrosshair( double new_x, double new_y, double new_z ) {

		xy_tracer_canvas.setCrosshairs( new_x, new_y, new_z, true );
		if( ! single_pane ) {
			xz_tracer_canvas.setCrosshairs( new_x, new_y, new_z, true );
			zy_tracer_canvas.setCrosshairs( new_x, new_y, new_z, true );
		}

	}

	protected String [] materialList;
	byte [][] labelData;

	synchronized public void loadLabelsFile( String path ) {

		AmiraMeshDecoder d=new AmiraMeshDecoder();

		if( ! d.open(path) ) {
			IJ.error("Could not open the labels file '"+path+"'");
			return;
		}

		ImageStack stack = d.getStack();

		ImagePlus labels = new ImagePlus( "Label file for Tracer", stack );

		if( (labels.getWidth() != width) ||
		    (labels.getHeight() != height) ||
		    (labels.getStackSize() != depth) ) {
			IJ.error("The size of that labels file doesn't match the size of the image you're tracing.");
			return;
		}

		// We need to get the AmiraParameters object for that image...

		AmiraParameters parameters = d.parameters;

		int materials = parameters.getMaterialCount();

		materialList = parameters.getMaterialList();

		labelData = new byte[depth][];
		for( int z = 0; z < depth; ++z ) {
			labelData[z] = (byte []) stack.getPixels( z + 1 );
		}

	}

	synchronized public void loadLabels( ) {

		String fileName;
		String directory;

		if( file_info != null ) {

			fileName = file_info.fileName;
			directory = file_info.directory;

			File possibleLoadFile = new File(directory,fileName+".labels");

			String path = possibleLoadFile.getPath();

			if(possibleLoadFile.exists()) {

				YesNoCancelDialog d = new YesNoCancelDialog( IJ.getInstance(),
									     "Confirm",
									     "Load the default labels file? ("+path+")" );

				if( d.yesPressed() ) {

					loadLabelsFile(path);

					return;

				} else if( d.cancelPressed() ) {

					return;

				}
			}
		}

		//  Presumably "No" was pressed...

		OpenDialog od;

		od = new OpenDialog("Select labels file...",
				    null,
				    null );

		fileName = od.getFileName();
		directory = od.getDirectory();

		if( fileName != null ) {

			loadLabelsFile( directory + fileName );
			return;
		}

	}

	boolean loading = false;

	synchronized public void importSWC( ) {

		loading = true;

		String fileName = null;
		String directory = null;

		//  Presumably "No" was pressed...

		OpenDialog od;

		od = new OpenDialog("Select SWC file...",
				    directory,
				    null );

		fileName = od.getFileName();
		directory = od.getDirectory();

		if( fileName != null ) {

			if( pathAndFillManager.importSWC( directory + fileName ) )
				unsavedPaths = false;

			loading = false;
			return;
		}

		loading = false;
	}

	synchronized public void loadTracings( ) {

		loading = true;

		String fileName = null;
		String directory = null;

		if( file_info != null ) {

			fileName = file_info.fileName;
			directory = file_info.directory;

			File possibleLoadFile = null;

			int dotIndex = fileName.lastIndexOf(".");
			if( dotIndex >= 0 ) {
				possibleLoadFile = new File(directory,fileName.substring(0,dotIndex)+".traces");

				String path = possibleLoadFile.getPath();

				if(possibleLoadFile.exists()) {

					YesNoCancelDialog d = new YesNoCancelDialog( IJ.getInstance(),
										     "Confirm",
										     "Load the default traces file? ("+path+")" );

					if( d.yesPressed() ) {

						if( pathAndFillManager.load(path) )
							unsavedPaths = false;

						loading = false;
						return;

					} else if( d.cancelPressed() ) {

						loading = false;
						return;

					}
				}
			}
		}

		//  Presumably "No" was pressed...

		OpenDialog od;

		od = new OpenDialog("Select traces file...",
				    directory,
				    null );

		fileName = od.getFileName();
		directory = od.getDirectory();

		if( fileName != null ) {

			if( pathAndFillManager.load( directory + fileName ) )
				unsavedPaths = false;

			loading = false;
			return;
		}

		loading = false;
	}

	public void mouseMovedTo( double x_in_pane, double y_in_pane, int in_plane, boolean shift_key_down, boolean join_modifier_down ) {

		double x, y, z;

		double [] pd = new double[3];
		findPointInStackPrecise( x_in_pane, y_in_pane, in_plane, pd );
		x = pd[0];
		y = pd[1];
		z = pd[2];

		if( join_modifier_down && pathAndFillManager.anySelected() ) {

			PointInImage pointInImage = pathAndFillManager.nearestJoinPointOnSelectedPaths( x, y, z );
			if( pointInImage != null ) {
				x = pointInImage.x / x_spacing;
				y = pointInImage.y / y_spacing;
				z = pointInImage.z / z_spacing;
			}
		}

		int ix = (int)Math.round(x);
		int iy = (int)Math.round(y);
		int iz = (int)Math.round(z);

		if( shift_key_down )
			setSlicesAllPanes( ix, iy, iz );

		if( (xy_tracer_canvas != null) &&
		    ((xz_tracer_canvas != null) || single_pane) &&
		    ((zy_tracer_canvas != null) || single_pane) ) {

			setCrosshair( x, y, z );
			if( labelData != null ) {

				byte b = labelData[iz][iy*width+ix];
				int m = b & 0xFF;

				String material = materialList[m];
				IJ.showStatus( "Material at crosshairs is: "+material);
			}

			repaintAllPanes( ); // Or the crosshair isn't updated....
		}

		if( filler != null ) {
			synchronized (filler) {
				float distance = filler.getDistanceAtPoint(ix,iy,iz);
				resultsDialog.showMouseThreshold(distance);
			}
		}
	}

	boolean lastStartPointSet = false;

	int last_start_point_x;
	int last_start_point_y;
	int last_start_point_z;

	Path endJoin;
	PointInImage endJoinPoint;

	/* If we've finished searching for a path, but the user hasn't
	   confirmed that they want to keep it yet, temporaryPath is
	   non-null and holds the Path we just searched out. */

	// Any method that deals with these two fields should be synchronized.

	Path temporaryPath = null;
	Path currentPath = null;

	// When we set temporaryPath, we also want to update the display:

	synchronized public void setTemporaryPath( Path path ) {

		Path oldTemporaryPath = this.temporaryPath;

		xy_tracer_canvas.setTemporaryPath( path );
		if( ! single_pane ) {
			zy_tracer_canvas.setTemporaryPath( path );
			xz_tracer_canvas.setTemporaryPath( path );
		}

		temporaryPath = path;

		if( temporaryPath != null )
			temporaryPath.setName("Temporary Path");
		if( use3DViewer ) {
			if( oldTemporaryPath != null ) {
				oldTemporaryPath.removeFrom3DViewer(univ);
			}
			if( temporaryPath != null )
				temporaryPath.addTo3DViewer(univ,Color.BLUE);
		}
	}

	synchronized public void setCurrentPath( Path path ) {

		Path oldCurrentPath = this.currentPath;

		xy_tracer_canvas.setCurrentPath( path );
		if( ! single_pane ) {
			zy_tracer_canvas.setCurrentPath( path );
			xz_tracer_canvas.setCurrentPath( path );
		}

		currentPath = path;
		if( currentPath != null )
			currentPath.setName("Current Path");

		if( use3DViewer ) {
			if( oldCurrentPath != null ) {
				oldCurrentPath.removeFrom3DViewer(univ);
			}
			if( currentPath != null )
				currentPath.addTo3DViewer(univ,Color.RED);
		}
	}

	synchronized public Path getCurrentPath( ) {
		return currentPath;
	}

	/* pathUnfinished indicates that we have started to create a
	   path, but not yet finished it (in the sense of moving on to
	   a new path with a differen starting point.)  FIXME: this
	   may be redundant - check that.
	*/

	boolean pathUnfinished = false;

	public void setPathUnfinished( boolean unfinished ) {

		this.pathUnfinished = unfinished;
		xy_tracer_canvas.setPathUnfinished( unfinished );
		if( ! single_pane ) {
			zy_tracer_canvas.setPathUnfinished( unfinished );
			xz_tracer_canvas.setPathUnfinished( unfinished );
		}
	}

	void addThreadToDraw( SearchThread s ) {
		xy_tracer_canvas.addSearchThread(s);
		if( ! single_pane ) {
			zy_tracer_canvas.addSearchThread(s);
			xz_tracer_canvas.addSearchThread(s);
		}
	}

	void removeThreadToDraw( SearchThread s ) {
		xy_tracer_canvas.removeSearchThread(s);
		if( ! single_pane ) {
			zy_tracer_canvas.removeSearchThread(s);
			xz_tracer_canvas.removeSearchThread(s);
		}
	}

	int [] selectedPaths = null;

	/* Create a new 8 bit ImagePlus of the same dimensions as this
	   image, but with values set to either 255 (if there's a point
	   on a path there) or 0 */

	synchronized public void makePathVolume( ) {

		byte [][] snapshot_data = new byte[depth][];

		for( int i = 0; i < depth; ++i )
			snapshot_data[i] = new byte[width*height];

		pathAndFillManager.setPathPointsInVolume( snapshot_data, width, height, depth );

		ImageStack newStack = new ImageStack( width, height );

		for( int i = 0; i < depth; ++i ) {
			ByteProcessor thisSlice = new ByteProcessor( width, height );
			thisSlice.setPixels( snapshot_data[i] );
			newStack.addSlice( null, thisSlice );
		}

		ImagePlus ip = new ImagePlus( "Paths rendered in a Stack", newStack );
		ip.show( );
	}

	/* If non-null, holds a reference to the currently searching thread: */

	TracerThread currentSearchThread;

	/* Start a search thread looking for the goal in the arguments: */

	synchronized void testPathTo( int x_in_pane, int y_in_pane, int plane, PointInImage joinPoint ) {

		if( ! lastStartPointSet ) {
			IJ.showStatus( "No initial start point has been set.  Do that with a mouse click." +
				       " (Or a shift-click if the start of the path should join another neurite." );
			return;
		}

		if( temporaryPath != null ) {
			IJ.showStatus( "There's already a temporary path; use 'N' to cancel it or 'Y' to keep it." );
			return;
		}

		int [] p = new int[3];
		findPointInStack( x_in_pane, y_in_pane, plane, p );

		int x_end, y_end, z_end;
		if( joinPoint == null ) {
			x_end = p[0];
			y_end = p[1];
			z_end = p[2];
		} else {
			x_end = (int)Math.round(joinPoint.x / x_spacing);
			y_end = (int)Math.round(joinPoint.y / y_spacing);
			z_end = (int)Math.round(joinPoint.z / z_spacing);
			endJoin = joinPoint.onPath;
			endJoinPoint = joinPoint;
		}

		currentSearchThread = new TracerThread(
			xy,
			stackMin,
			stackMax,
			0, // timeout in seconds
			1000, // reportEveryMilliseconds
			last_start_point_x,
			last_start_point_y,
			last_start_point_z,
			x_end,
			y_end,
			z_end,
			true, // reciprocal
			singleSlice,
			(hessianEnabled ? hessian : null),
			resultsDialog.getMultiplier(),
			tubeness,
			hessianEnabled );

		addThreadToDraw( currentSearchThread );

		currentSearchThread.setDrawingColors( Color.CYAN, null );
		currentSearchThread.setDrawingThreshold( -1 );

		currentSearchThread.addProgressListener( this );

		currentSearchThread.start();

		repaintAllPanes();
	}

	synchronized public void confirmTemporary( ) {

		if( temporaryPath == null )
			// Just ignore the request to confirm a path (there isn't one):
			return;

		currentPath.add( temporaryPath );

		PointInImage last = currentPath.lastPoint();
		last_start_point_x = (int)Math.round(last.x / x_spacing);
		last_start_point_y = (int)Math.round(last.y / y_spacing);
		last_start_point_z = (int)Math.round(last.z / z_spacing);

		if( currentPath.endJoins == null ) {
			setTemporaryPath( null );
			resultsDialog.changeState( NeuriteTracerResultsDialog.PARTIAL_PATH );
			repaintAllPanes( );
		} else {
			setTemporaryPath( null );
			// Since joining onto another path for the end must finish the path:
			finishedPath( );
		}

		/* This has the effect of removing the path from the
		   3D viewer and adding it again: */
		setCurrentPath(currentPath);
	}

	synchronized public void cancelTemporary( ) {

		if( ! lastStartPointSet ) {
			IJ.error( "No initial start point has been set yet.  Do that with a mouse click." +
				  " (Or a control-click if the start of the path should join another neurite." );
			return;
		}

		if( temporaryPath == null ) {
			IJ.error( "There's no temporary path to cancel!" );
			return;
		}

		setTemporaryPath( null );

		endJoin = null;
		endJoinPoint = null;

		resultsDialog.changeState( NeuriteTracerResultsDialog.PARTIAL_PATH );
		repaintAllPanes( );
	}

	synchronized public void cancelPath( ) {

		setCurrentPath( null );
		setTemporaryPath( null );

		lastStartPointSet = false;
		setPathUnfinished( false );

		resultsDialog.changeState( NeuriteTracerResultsDialog.WAITING_TO_START_PATH );

		repaintAllPanes();
	}

	synchronized public void finishedPath( ) {

		// Is there an unconfirmed path?  If so, warn people about it...

		if( temporaryPath != null ) {
			IJ.error( "There's an unconfirmed path, need to confirm or cancel it before finishing the path." );
			return;
		}

		if( currentPath == null ) {
			IJ.error("You can't complete a path with only a start point in it.");
			return;
		}

		lastStartPointSet = false;
		setPathUnfinished( false );

		Path savedCurrentPath = currentPath;
		setCurrentPath(null);

		pathAndFillManager.addPath( savedCurrentPath, true );

		unsavedPaths = true;

		// ... and change the state of the UI
		resultsDialog.changeState( NeuriteTracerResultsDialog.WAITING_TO_START_PATH );

		repaintAllPanes( );
	}

	synchronized public void clickForTrace( int x_in_pane, int y_in_pane, int plane, boolean join ) {

		PointInImage joinPoint = null;

		if( join ) {
			int [] p = new int[3];
			findPointInStack( x_in_pane, y_in_pane, plane, p );
			joinPoint = pathAndFillManager.nearestJoinPointOnSelectedPaths( p[0], p[1], p[2] );
		}

		if( resultsDialog == null )
			return;

		// FIXME: in some of the states this doesn't make sense; check for them:

		if( currentSearchThread != null )
			return;

		if( temporaryPath != null )
			return;

		if( filler != null ) {
			setFillThresholdFrom( x_in_pane, y_in_pane, plane );
			return;
		}

		if( pathUnfinished ) {
			/* Then this is a succeeding point, and we
			   should start a search. */
			testPathTo( x_in_pane, y_in_pane, plane, joinPoint );
			resultsDialog.changeState( NeuriteTracerResultsDialog.SEARCHING );
		} else {
			/* This is an initial point. */
			startPath( x_in_pane, y_in_pane, plane, joinPoint );
			resultsDialog.changeState( NeuriteTracerResultsDialog.PARTIAL_PATH );
		}

	}

	public void setFillThresholdFrom( int x_in_pane, int y_in_pane, int plane ) {

		int [] p = new int[3];

		findPointInStack( x_in_pane, y_in_pane, plane, p );

		int x = p[0];
		int y = p[1];
		int z = p[2];

		float distance = filler.getDistanceAtPoint(x,y,z);

		setFillThreshold( distance );
	}

	public void setFillThreshold( double distance ) {

		if( distance > 0 ) {

			if (verbose) System.out.println("Setting new threshold of: "+distance);

			resultsDialog.thresholdChanged(distance);

			filler.setThreshold(distance);
		}

	}

	synchronized void startPath( int x_in_pane, int y_in_pane, int plane, PointInImage joinPoint ) {

		endJoin = null;
		endJoinPoint = null;

		if( lastStartPointSet ) {
			IJ.showStatus( "The start point has already been set; to finish a path press 'F'" );
			return;
		}

		int [] p = new int[3];
		findPointInStack( x_in_pane, y_in_pane, plane, p );

		setPathUnfinished( true );
		lastStartPointSet = true;

		Path path = new Path(x_spacing,y_spacing,z_spacing,spacing_units);
		path.setName("New Path");

		if( joinPoint == null ) {
			last_start_point_x = p[0];
			last_start_point_y = p[1];
			last_start_point_z = p[2];
		} else {
			last_start_point_x = (int)Math.round( joinPoint.x / x_spacing );
			last_start_point_y = (int)Math.round( joinPoint.y / y_spacing );
			last_start_point_z = (int)Math.round( joinPoint.z / z_spacing );
			path.setStartJoin( joinPoint.onPath, joinPoint );
		}

		setCurrentPath( path );
	}

	/* Return true if we have just started a new path, but have
	   not yet added any connections to it, otherwise return
	   false. */

	public boolean justFirstPoint() {
		return pathUnfinished && (currentPath.size() == 0);
	}

	public static String getStackTrace( ) {
		StringWriter sw = new StringWriter();
		new Exception("Dummy Exception for Stack Trace").printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}

	double x_spacing = 1;
	double y_spacing = 1;
	double z_spacing = 1;

	String spacing_units = "";

	public void viewFillIn3D( boolean asMask ) {
		ImagePlus imagePlus = filler.fillAsImagePlus( asMask );
		imagePlus.show();
	}

	public void setPositionAllPanes( int x, int y, int z ) {

		xy.setSlice( z + 1 );
		zy.setSlice( x );
		xz.setSlice( y );

	}

	int imageType = -1;

	byte [][] slices_data_b;
	short [][] slices_data_s;
	float [][] slices_data_f;

	NeuriteTracerResultsDialog resultsDialog;

	boolean cancelled = false;

	TextWindow helpTextWindow;

	boolean singleSlice;

	ArchiveClient archiveClient;

	float stackMax = Float.MIN_VALUE;
	float stackMin = Float.MAX_VALUE;

	public void run( String ignoredArguments ) {

		/* The useful macro options are:

		     imagefilename=<FILENAME>
		     tracesfilename=<FILENAME>
		     use_3d
		     use_three_pane
		*/

		String macroOptions = Macro.getOptions();

		String macroImageFilename = null;
		String macroTracesFilename = null;

		if( macroOptions != null ) {
			macroImageFilename = Macro.getValue(
				macroOptions, "imagefilename", null );
			macroTracesFilename = Macro.getValue(
				macroOptions, "tracesfilename", null );
		}

		Applet applet = IJ.getApplet();
		if( applet != null ) {
			archiveClient = new ArchiveClient( applet, macroOptions );
		}

		if( archiveClient != null )
			archiveClient.closeChannelsWithTag("nc82");

		try {

			ImagePlus currentImage = null;
			if( macroImageFilename == null ) {
				currentImage = IJ.getImage();
			} else {
				currentImage = BatchOpener.openFirstChannel( macroImageFilename );
				if( currentImage == null ) {
					IJ.error("Opening the image file specified in the macro parameters ("+macroImageFilename+") failed.");
					return;
				}
				currentImage.show();
			}

			if( currentImage == null ) {
				IJ.error( "There's no current image to trace." );
				return;
			}

			imageType = currentImage.getType();

			if( imageType == ImagePlus.COLOR_RGB ) {
				YesNoCancelDialog queryRGB = new YesNoCancelDialog( IJ.getInstance(),
										    "Convert RGB image",
										    "Convert this RGB image to an 8 bit luminance image first?\n" +
										    "(If you want to trace a particular channel instead, cancel and \"Split Channels\" first.)" );

				if( ! queryRGB.yesPressed() ) {
					return;
				}

				currentImage = RGB_to_Luminance.convertToLuminance(currentImage);
				currentImage.show();
				imageType = currentImage.getType();
			}

			if( currentImage.getStackSize() == 1 )
				singleSlice = true;

			width = currentImage.getWidth();
			height = currentImage.getHeight();
			depth = currentImage.getStackSize();

			Calibration calibration = currentImage.getCalibration();
			if( calibration != null ) {
				x_spacing = calibration.pixelWidth;
				y_spacing = calibration.pixelHeight;
				z_spacing = calibration.pixelDepth;
				spacing_units = calibration.getUnits();
				if( spacing_units == null || spacing_units.length() == 0 )
					spacing_units = "" + calibration.getUnit();
			}

			pathAndFillManager = new PathAndFillManager(this);

			file_info = currentImage.getOriginalFileInfo();

			// Turn it grey, since I find that helpful:
			{
				ImageProcessor imageProcessor = currentImage.getProcessor();
				byte [] reds = new byte[256];
				byte [] greens = new byte[256];
				byte [] blues = new byte[256];
				for( int i = 0; i < 256; ++i ) {
					reds[i] = (byte)i;
					greens[i] = (byte)i;
					blues[i] = (byte)i;
				}
				IndexColorModel cm = new IndexColorModel(8, 256, reds, greens, blues);
				imageProcessor.setColorModel( cm );
				if( currentImage.getStackSize() > 1 )
					currentImage.getStack().setColorModel( cm );
				currentImage.updateAndRepaintWindow();
			}

			if( file_info != null ) {
				String originalFileName=file_info.fileName;
				if (verbose) System.out.println("originalFileName was: "+originalFileName);
				if( originalFileName != null ) {
					int lastDot=originalFileName.lastIndexOf(".");
					if( lastDot > 0 ) {
						String beforeExtension=originalFileName.substring(0, lastDot);
						String tubesFileName=beforeExtension+".tubes.tif";
						ImagePlus tubenessImage = null;
						File tubesFile=new File(file_info.directory,tubesFileName);
						if (verbose) System.out.println("Testing for the existence of "+tubesFile.getAbsolutePath());
						if( tubesFile.exists() ) {
							long megaBytesExtra = ( ((long)width) * height * depth * 4 ) / (1024 * 1024);
							String extraMemoryNeeded = megaBytesExtra + "MiB";
							YesNoCancelDialog d = new YesNoCancelDialog( IJ.getInstance(),
												     "Confirm",
												     "A tubeness file ("+tubesFile.getName()+") exists.  Load this file?\n"+
												     "(This would use an extra "+extraMemoryNeeded+" of memory.)");
							if( d.cancelPressed() )
								return;
							else if( d.yesPressed() ) {
								IJ.showStatus("Loading tubes file.");
								tubenessImage=BatchOpener.openFirstChannel(tubesFile.getAbsolutePath());
								if (verbose) System.out.println("Loaded the tubeness file");
								if( tubenessImage == null ) {
									IJ.error("Failed to load tubes image from "+tubesFile.getAbsolutePath()+" although it existed");
									return;
								}
								if( tubenessImage.getType() != ImagePlus.GRAY32 ) {
									IJ.error("The tubeness file must be a 32 bit float image - "+tubesFile.getAbsolutePath()+" was not.");
									return;
								}
								int width = tubenessImage.getWidth();
								int height = tubenessImage.getHeight();
								int depth = tubenessImage.getStackSize();
								ImageStack tubenessStack = tubenessImage.getStack();
								tubeness = new float[depth][];
								for( int z = 0; z < depth; ++z ) {
									FloatProcessor fp = (FloatProcessor)tubenessStack.getProcessor( z + 1 );
									tubeness[z] = (float[])fp.getPixels();
								}
							}
						}
					}
				}
			}

			single_pane = true;
			Image3DUniverse universeToUse = null;
			String [] choices3DViewer = null;;

			if( ! singleSlice ) {
				boolean java3DAvailable = haveJava3D();
				boolean showed3DViewerOption = false;

				GenericDialog gd = new GenericDialog("Simple Neurite Tracer (v" +
								     PLUGIN_VERSION + ")");
				gd.addMessage("Tracing the image: "+currentImage.getTitle());
				String extraMemoryNeeded = " (will use an extra: ";
				int bitDepth = currentImage.getBitDepth();
				int byteDepth = bitDepth == 24 ? 4 : bitDepth / 8;
				long megaBytesExtra = ( ((long)width) * height * depth * byteDepth * 2 ) / (1024 * 1024);
				extraMemoryNeeded += megaBytesExtra + "MiB of memory)";

				gd.addCheckbox("Use_three_pane view?"+extraMemoryNeeded, false);

				if( ! java3DAvailable ) {
					String message = "(Java3D classes don't seem to be available, so no 3D viewer option is available.)";
					System.out.println(message);
					gd.addMessage(message);
				} else if( currentImage.getBitDepth() != 8 ) {
					String message = "(3D viewer option is only currently available for 8 bit images)";
					System.out.println(message);
					gd.addMessage(message);
				} else {
					showed3DViewerOption = true;
					choices3DViewer = new String[Image3DUniverse.universes.size()+2];
					String no3DViewerString = "No 3D view";
					String useNewString = "Create New 3D Viewer";
					choices3DViewer[choices3DViewer.length-2] = useNewString;
					choices3DViewer[choices3DViewer.length-1] = no3DViewerString;
					for( int i = 0; i < choices3DViewer.length - 2; ++i ) {
						String contentsString = Image3DUniverse.universes.get(i).allContentsString();
						String shortContentsString;
						if( contentsString.length() == 0 )
							shortContentsString = "[Empty]";
						else
							shortContentsString = contentsString.substring(0,Math.min(40,contentsString.length()-1));
						choices3DViewer[i] = "Use 3D viewer ["+i+"] containing " + shortContentsString;
					}
					gd.addChoice( "Choice of 3D Viewer:", choices3DViewer, useNewString );
				}

				gd.showDialog();
				if (gd.wasCanceled())
					return;

				single_pane = ! gd.getNextBoolean();
				if( showed3DViewerOption ) {
					String chosenViewer = gd.getNextChoice();
					int chosenIndex;
					for( chosenIndex = 0; chosenIndex < choices3DViewer.length; ++chosenIndex )
						if( choices3DViewer[chosenIndex].equals(chosenViewer) )
							break;
					if( chosenIndex == choices3DViewer.length - 2 ) {
						use3DViewer = true;
						universeToUse = null;
					} else if( chosenIndex == choices3DViewer.length - 1 ) {
						use3DViewer = false;
						universeToUse = null;
					} else {
						use3DViewer = true;
						universeToUse = Image3DUniverse.universes.get(chosenIndex);;
					}
				}
			}

			initialize(currentImage);

			xy_tracer_canvas = (InteractiveTracerCanvas)xy_canvas;
			xz_tracer_canvas = (InteractiveTracerCanvas)xz_canvas;
			zy_tracer_canvas = (InteractiveTracerCanvas)zy_canvas;

			setupTrace = true;
			resultsDialog = new NeuriteTracerResultsDialog( "Tracing for: " + xy.getShortTitle(),
									this,
									applet != null );

			/* FIXME: the first could be changed to add
			   'this', and move the small implementation
			   out of NeuriteTracerResultsDialog into this
			   class. */
			pathAndFillManager.addPathAndFillListener(resultsDialog);
			pathAndFillManager.addPathAndFillListener(resultsDialog.pw);
			pathAndFillManager.addPathAndFillListener(resultsDialog.fw);

			if( (x_spacing == 0.0) ||
			    (y_spacing == 0.0) ||
			    (z_spacing == 0.0) ) {

				IJ.error( "One dimension of the calibration information was zero: (" +
					  x_spacing + "," + y_spacing + "," + z_spacing + ")" );
				return;

			}

			{
				ImageStack s = xy.getStack();
				switch(imageType) {
				case ImagePlus.GRAY8:
				case ImagePlus.COLOR_256:
					slices_data_b = new byte[depth][];
					for( int z = 0; z < depth; ++z )
						slices_data_b[z] = (byte []) s.getPixels( z + 1 );
					stackMin = 0;
					stackMax = 255;
					break;
				case ImagePlus.GRAY16:
					slices_data_s = new short[depth][];
					for( int z = 0; z < depth; ++z )
						slices_data_s[z] = (short []) s.getPixels( z + 1 );
					IJ.showStatus("Finding stack minimum / maximum");
					for( int z = 0; z < depth; ++z ) {
						for( int y = 0; y < height; ++y )
							for( int x = 0; x < width; ++x ) {
								short v = slices_data_s[z][y*width+x];
								if( v < stackMin )
									stackMin = v;
								if( v > stackMax )
									stackMax = v;
							}
						IJ.showProgress( z / (float)depth );
					}
					IJ.showProgress(1.0);
					break;
				case ImagePlus.GRAY32:
					slices_data_f = new float[depth][];
					for( int z = 0; z < depth; ++z )
						slices_data_f[z] = (float []) s.getPixels( z + 1 );
					IJ.showStatus("Finding stack minimum / maximum");
					for( int z = 0; z < depth; ++z ) {
						for( int y = 0; y < height; ++y )
							for( int x = 0; x < width; ++x ) {
								float v = slices_data_f[z][y*width+x];
								if( v < stackMin )
									stackMin = v;
								if( v > stackMax )
									stackMax = v;
							}
						IJ.showProgress( z / (float)depth );
					}
					IJ.showProgress(1.0);
					break;
				}
			}

			xy_tracer_canvas.addKeyListener( xy_tracer_canvas );
			xy_window.addKeyListener( xy_tracer_canvas );

			if( ! single_pane ) {

				xz_tracer_canvas.addKeyListener( xz_tracer_canvas );
				xz_window.addKeyListener( xz_tracer_canvas );

				zy_tracer_canvas.addKeyListener( zy_tracer_canvas );
				zy_window.addKeyListener( zy_tracer_canvas );

			}

			if( use3DViewer ) {

				boolean reusing;
				if( universeToUse == null ) {
					reusing = false;
					univ = new Image3DUniverse(512, 512);
				} else {
					reusing = true;
					univ = universeToUse;
				}
				univ.setUseToFront(false);
				univ.addUniverseListener(pathAndFillManager);
				if( ! reusing ) {
					univ.show();
					GUI.center(univ.getWindow());
				}
				boolean [] channels = { true, true, true };

				String title = "Image for tracing ["+currentImage.getTitle()+"]";
				String contentName = univ.getSafeContentName( title );
				univ.resetView();
				Content c = univ.addContent(xy,
							    new Color3f(Color.white),
							    contentName,
							    10, // threshold
							    channels,
							    2, // resampling factor
							    Content.VOLUME);
				c.setLocked(true);
				c.setTransparency(0.5f);
				univ.resetView();
			}

			File tracesFileToLoad = null;
			if( macroTracesFilename != null ) {
				tracesFileToLoad = new File( macroTracesFilename );
				if( tracesFileToLoad.exists() )
					pathAndFillManager.load( tracesFileToLoad.getAbsolutePath() );
				else
					IJ.error("The traces file suggested by the macro parameters ("+macroTracesFilename+") does not exist");
			}

			resultsDialog.displayOnStarting();

		} finally {
			IJ.getInstance().addKeyListener( IJ.getInstance() );
		}
	}

	public boolean isReady() {
		if( resultsDialog == null )
			return false;
		return resultsDialog.isVisible();
	}

	public void launchPaletteAround( int x, int y, int z ) {

		int either_side = 40;

		int x_min = x - either_side;
		int x_max = x + either_side;
		int y_min = y - either_side;
		int y_max = y + either_side;
		int z_min = z - either_side;
		int z_max = z + either_side;

		int originalWidth = xy.getWidth();
		int originalHeight = xy.getHeight();
		int originalDepth = xy.getStackSize();

		if( x_min < 0 )
			x_min = 0;
		if( y_min < 0 )
			y_min = 0;
		if( z_min < 0 )
			z_min = 0;
		if( x_max >= originalWidth )
			x_max = originalWidth - 1;
		if( y_max >= originalHeight )
			y_max = originalHeight - 1;
		if( z_max >= originalDepth )
			z_max = originalDepth - 1;

		double [] sigmas = new double[9];
		for( int i = 0; i < sigmas.length; ++i ) {
			sigmas[i] = ((i + 1) * getMinimumSeparation()) / 2;
		}

		resultsDialog.changeState( NeuriteTracerResultsDialog.WAITING_FOR_SIGMA_CHOICE );

		Sigma_Palette sp = new Sigma_Palette();
		sp.setListener( resultsDialog );
		sp.makePalette( xy, x_min, x_max, y_min, y_max, z_min, z_max, new TubenessProcessor(true), sigmas, 256 / resultsDialog.getMultiplier(), 3, 3, z );
	}

	public void startFillerThread( FillerThread filler ) {

		this.filler = filler;

		filler.addProgressListener(this);
		filler.addProgressListener(resultsDialog.fw);

		addThreadToDraw(filler);

		filler.start();

		resultsDialog.changeState(NeuriteTracerResultsDialog.FILLING_PATHS);

	}

	// This should only be assigned to when synchronized on this object
	// (FIXME: check that that is true)
	FillerThread filler = null;

	synchronized public void startFillingPaths( Set<Path> fromPaths ) {

		// currentlyFilling = true;
		resultsDialog.fw.pauseOrRestartFilling.setLabel("Pause");

		filler = new FillerThread( xy,
					   stackMin,
					   stackMax,
					   false, // startPaused
					   true, // reciprocal
					   0.03f, // Initial threshold to display
					   5000 ); // reportEveryMilliseconds

		addThreadToDraw(filler);

		filler.addProgressListener( this );
		filler.addProgressListener( resultsDialog.fw );

		filler.setSourcePaths( fromPaths );

		resultsDialog.setFillListVisible(true);

		filler.start();

		resultsDialog.changeState(NeuriteTracerResultsDialog.FILLING_PATHS);

	}

	public void setFillTransparent( boolean transparent ) {
		xy_tracer_canvas.setFillTransparent(transparent);
		if( ! single_pane ) {
			xz_tracer_canvas.setFillTransparent(transparent);
			zy_tracer_canvas.setFillTransparent(transparent);
		}
	}

	public void maximumDistanceCompletelyExplored( SearchThread source, float f ) {
		// Unused
	}

	public byte [] squareNormalToVector( int side,        // The number of samples in x and y in the plane, separated by step
					     double step,     // step is in the same units as the _spacing, etc. variables.
					     double ox,      /* These are scaled now */
					     double oy,
					     double oz,
					     double nx,
					     double ny,
					     double nz,
					     double [] x_basis_vector,    /* The basis vectors are returned here  */
					     double [] y_basis_vector ) { /* they *are* scaled by _spacing        */

		byte [] result = new byte[side*side];

		double epsilon = 0.000001;

		/* To find an arbitrary vector in the normal plane, do
		   the cross product with (0,0,1), unless the normal
		   is parallel to that, in which case we cross it with
		   (0,1,0) instead... */

		double ax, ay, az;

		if( Math.abs(nx) < epsilon && Math.abs(ny) < epsilon ) {
			// Cross with (0,1,0):
			ax = nz;
			ay = 0;
			az = -nx;
		} else {
			// Cross with (0,0,1):
			ax = -ny;
			ay = nx;
			az = 0;
		}

		/* Now to find the other vector in that plane, do the
		 * cross product of (ax,ay,az) with (nx,ny,nz) */

		double bx = ay * nz - az * ny;
		double by = az * nx - ax * nz;
		double bz = ax * ny - ay * nx;

		/* Normalize a and b */

		double a_size = Math.sqrt( ax*ax + ay*ay + az*az );
		ax = ax / a_size; ay = ay / a_size; az = az / a_size;

		double b_size = Math.sqrt( bx*bx + by*by + bz*bz );
		bx = bx / b_size; by = by / b_size; bz = bz / b_size;

		/* Scale them with spacing... */

		double ax_s = ax * step;
		double ay_s = ay * step;
		double az_s = az * step;

		double bx_s = bx * step;
		double by_s = by * step;
		double bz_s = bz * step;

		if( verbose ) {
			System.out.println( "a (in normal plane) is "+ax+","+ay+","+az);
			System.out.println( "b (in normal plane) is "+bx+","+by+","+bz);
		}

		if( true  ) {

			// a and b must be perpendicular:
			double a_dot_b = ax * bx + ay * by + az * bz;

			// ... and each must be perpendicular to the normal
			double a_dot_n = ax * nx + ay * ny + az * nz;
			double b_dot_n = bx * nx + by * ny + bz * nz;

			if( verbose ) {
				System.out.println("a_dot_b: "+a_dot_b);
				System.out.println("a_dot_n: "+a_dot_n);
				System.out.println("b_dot_n: "+b_dot_n);
			}

		}

		// FIXME: do other image types too...
		byte [][] v = slices_data_b;

		for( int grid_i = 0; grid_i < side; ++grid_i ) {
			for( int grid_j = 0; grid_j < side; ++grid_j ) {

				double midside_grid = ((side - 1) / 2.0f);

				double gi = midside_grid - grid_i;
				double gj = midside_grid - grid_j;

				double vx = ox + gi * ax_s + gj * bx_s;
				double vy = oy + gi * ay_s + gj * by_s;
				double vz = oz + gi * az_s + gj * bz_s;

				// So now denormalize to pixel co-ordinates:

				double image_x = vx / x_spacing;
				double image_y = vy / y_spacing;
				double image_z = vz / z_spacing;

				/* And do a trilinear interpolation to
				   find the value there: */

				double x_d = image_x - Math.floor(image_x);
				double y_d = image_y - Math.floor(image_y);
				double z_d = image_z - Math.floor(image_z);

				int x_f = (int)Math.floor(image_x); int x_c = (int)Math.ceil(image_x);
				int y_f = (int)Math.floor(image_y); int y_c = (int)Math.ceil(image_y);
				int z_f = (int)Math.floor(image_z); int z_c = (int)Math.ceil(image_z);

				/* Check that these values aren't
				   poking off the edge of the screen -
				   if so then make them zero. */

				double fff;
				double cff;
				double fcf;
				double ccf;

				double ffc;
				double cfc;
				double fcc;
				double ccc;

				if( (x_f < 0) || (x_c < 0) || (y_f < 0) || (y_c < 0) || (z_f < 0) || (z_c < 0) ||
				    (x_f >= width) || (x_c >= width) || (y_f >= height) || (y_c >= height) || (z_f >= depth) || (z_c >= depth) ) {

					fff = 0;
					cff = 0;
					fcf = 0;
					ccf = 0;
					ffc = 0;
					cfc = 0;
					fcc = 0;
					ccc = 0;

				} else {

					fff = v[z_f][width*y_f+x_f]&0xFF;
					cff = v[z_c][width*y_f+x_f]&0xFF;

					fcf = v[z_f][width*y_c+x_f]&0xFF;
					ccf = v[z_c][width*y_c+x_f]&0xFF;

					ffc = v[z_f][width*y_f+x_c]&0xFF;
					cfc = v[z_c][width*y_f+x_c]&0xFF;

					fcc = v[z_f][width*y_c+x_c]&0xFF;
					ccc = v[z_c][width*y_c+x_c]&0xFF;

				}

				// Now we should be OK to do the interpolation for real:

				double i1 = (1 - z_d) * (fff) + (cff) * z_d;
				double i2 = (1 - z_d) * (fcf) + (ccf) * z_d;

				double j1 = (1 - z_d) * (ffc) + (cfc) * z_d;
				double j2 = (1 - z_d) * (fcc) + (ccc) * z_d;

				double w1 = i1 * (1 - y_d) + i2 * y_d;
				double w2 = j1 * (1 - y_d) + j2 * y_d;

				double value_f = w1 * (1 - x_d) + w2 * x_d;

				int value = (int)value_f;
				if( (value < 0) || (value > 255) ) {
					System.out.println("BUG: Out of range value!");
				}

				result[grid_j*side+grid_i] = (byte)value;
			}
		}

		x_basis_vector[0] = ax_s;
		x_basis_vector[1] = ay_s;
		x_basis_vector[2] = az_s;

		y_basis_vector[0] = bx_s;
		y_basis_vector[1] = by_s;
		y_basis_vector[2] = bz_s;

		return result;
	}

	public double getMinimumSeparation() {
		return Math.min(Math.abs(x_spacing),Math.min(Math.abs(y_spacing),Math.abs(z_spacing)));
	}

	boolean hessianEnabled = false;
	ComputeCurvatures hessian = null;
	/* This variable just stores the sigma which the current
	   'hessian' ComputeCurvatures was / is being calculated
	   (or -1 if 'hessian' is null) ... */
	double hessianSigma = -1;

	public void startHessian() {
		if( hessian == null ) {
			resultsDialog.changeState(NeuriteTracerResultsDialog.CALCULATING_GAUSSIAN);
			hessianSigma = resultsDialog.getSigma();
			hessian = new ComputeCurvatures( xy, hessianSigma, this, true );
			new Thread(hessian).start();
		} else {
			double newSigma = resultsDialog.getSigma();
			if( newSigma != hessianSigma ) {
				resultsDialog.changeState(NeuriteTracerResultsDialog.CALCULATING_GAUSSIAN);
				hessianSigma = newSigma;
				hessian = new ComputeCurvatures( xy, hessianSigma, this, true );
				new Thread(hessian).start();
			}
		}
	}

	// Even better, we might have a "tubeness" file already there.
	// If this is non-null then we found the "tubeness" file
	// (called foo.tubes.tif) on startup and loaded it
	// successfully.

	float [][] tubeness;

	public synchronized void enableHessian( boolean enable ) {
		hessianEnabled = enable;
		if( enable ) {
			startHessian();
			resultsDialog.editSigma.setEnabled(false);
			resultsDialog.sigmaWizard.setEnabled(false);
		} else {
			resultsDialog.editSigma.setEnabled(true);
			resultsDialog.sigmaWizard.setEnabled(true);
		}
	}

	public synchronized void cancelGaussian( ) {
		if( hessian != null ) {
			hessian.cancelGaussianGeneration();
		}
	}

	// This is the implementation of GaussianGenerationCallback

	public void proportionDone( double proportion ) {
		if( proportion < 0 ) {
			hessianEnabled = false;
			hessian = null;
			hessianSigma = -1;
			resultsDialog.gaussianCalculated(false);
			IJ.showProgress(1.0);
			return;
		} else if( proportion >= 1.0 ) {
			hessianEnabled = true;
			resultsDialog.gaussianCalculated(true);
		}
		IJ.showProgress(proportion);
	}

/*
	public void getTracings( boolean mineOnly ) {
		boolean result = pathAndFillManager.getTracings( mineOnly, archiveClient );
		if( result )
			unsavedPaths = false;
	}
*/

/*
	public void uploadTracings( ) {
		boolean result = pathAndFillManager.uploadTracings( archiveClient );
		if( result )
			unsavedPaths = false;
	}
*/

	public static boolean haveJava3D() {
		ClassLoader loader = IJ.getClassLoader();
		if (loader == null)
			throw new RuntimeException("IJ.getClassLoader() failed (!)");
		try {
			Class<?> c = loader.loadClass("ij3d.ImageWindow3D");
			/* In fact the documentation says that this
			   should throw an exception and not return
			   null, but just in case: */
			return c != null;
		} catch( Exception e ) {
			return false;
		}
	}

	public void addLineTo3DViewer( double x1, double y1, double z1,
				       double x2, double y2, double z2,
				       double radius,
				       Color c,
				       String name ) {

		if( ! use3DViewer )
			return;

		int points = 8;

		double [] x_points_d = new double[points];
		double [] y_points_d = new double[points];
		double [] z_points_d = new double[points];
		double [] radiuses = new double[points];

		for( int i = 0; i < points; ++i ) {
			x_points_d[i] = ( i * (x2 - x1) ) / points + x1;
			y_points_d[i] = ( i * (y2 - y1) ) / points + y1;
			z_points_d[i] = ( i * (z2 - z1) ) / points + z1;
			radiuses[i] = radius;
		}

		double [][][] allPoints = Pipe.makeTube(x_points_d,
							y_points_d,
							z_points_d,
							radiuses,
							1,       // resample - 1 means just "use mean distance between points", 3 is three times that, etc.
							8);     // "parallels" (12 means cross-sections are dodecagons)

		java.util.List triangles = Pipe.generateTriangles(allPoints,
								  1); // scale

		String nameWhenAddedToViewer = univ.getSafeContentName( name );
		univ.resetView();
		univ.addMesh(triangles,
			     c == null ? new Color3f(Color.magenta) : new Color3f(c),
			     nameWhenAddedToViewer,
			     1); // threshold
		Content content = univ.getContent(nameWhenAddedToViewer);
		content.setLocked(true);
		univ.resetView();
	}

	public void showCorrespondencesTo( File tracesFile, Color c, double maxDistance ) {

		PathAndFillManager pafmTraces = new PathAndFillManager(
			width, height, depth,
			(float)x_spacing, (float)y_spacing, (float)z_spacing,
			spacing_units );

		if( ! pafmTraces.load( tracesFile.getAbsolutePath() ) ) {
			IJ.error("Failed to load traces from: "+tracesFile.getAbsolutePath());
			return;
		}

		// Now find corresponding points from the first one, and draw lines to them:
		ArrayList< NearPoint > cp = pathAndFillManager.getCorrespondences( pafmTraces, 2.5 );
		Iterator< NearPoint > i = cp.iterator();
		int done = 0;
		while( i.hasNext() ) {
			NearPoint np = i.next();
			if( np != null ) {
				// System.out.println("Drawing:");
				// System.out.println(np.toString());
				addLineTo3DViewer(
					np.nearX, np.nearY, np.nearZ,
					np.pathPointX, np.pathPointY, np.pathPointZ,
					Math.abs(x_spacing),
					c,
					tracesFile.getName()+"-"+done);
			}
			++done;
		}
	}

	private boolean showOnlySelectedPaths;

	public void setShowOnlySelectedPaths(boolean showOnlySelectedPaths) {
		this.showOnlySelectedPaths = showOnlySelectedPaths;
		update3DViewerContents();
		repaintAllPanes();
	}

	public boolean getShowOnlySelectedPaths() {
		return showOnlySelectedPaths;
	}

	/* Whatever the state of the paths, update the 3D viewer to
	   make sure that they're the right colour, the right version
	   (fitted or unfitted) is being used and whether the path
	   should be displayed at all - it shouldn't if the "Show only
	   selected paths" option is set. */

	public void update3DViewerContents() {
		pathAndFillManager.update3DViewerContents();
	}

	public Image3DUniverse get3DUniverse() {
		return univ;
	}

	public Color3f selectedColor3f = new Color3f( Color.green );
	public Color3f deselectedColor3f = new Color3f( Color.magenta );
	public Color selectedColor = Color.GREEN;
	public Color deselectedColor = Color.MAGENTA;

	public void setSelectedColor( Color newColor ) {
		selectedColor = newColor;
		selectedColor3f = new Color3f( newColor );
		repaintAllPanes();
		update3DViewerContents();
	}

	public void setDeselectedColor( Color newColor ) {
		deselectedColor = newColor;
		deselectedColor3f = new Color3f( newColor );
		repaintAllPanes();
		update3DViewerContents();
	}

}

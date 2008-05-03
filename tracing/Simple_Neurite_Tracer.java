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

/* FIXME: This plugin is a mess now, needs a lot of tidying up. */

import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.text.*;
import ij.measure.Calibration;
import ij.io.*;

import java.applet.Applet;

import java.awt.*;
import java.awt.event.*;

import java.io.*;

import client.ArchiveClient;

import stacks.ThreePanes;

import util.Arrow;
import util.ArrowDisplayer;
import util.BatchOpener;

import features.GaussianGenerationCallback;
import features.ComputeCurvatures;

import amira.AmiraMeshDecoder;
import amira.AmiraParameters;

/* Note On Confusing Terminology: traces and paths are the same thing;
   they're made up of connections.  Traces, paths and connections are
   all non-branching sequences of adjacent points in the image. */

public class Simple_Neurite_Tracer extends ThreePanes
        implements PlugIn, SearchProgressCallback, ArrowDisplayer, FillerProgressCallback, GaussianGenerationCallback {
	
	public static final String PLUGIN_VERSION = "1.1.3";
        static final boolean verbose = false;
	
        PathAndFillManager pathAndFillManager;
	
        boolean unsavedPaths = false;
	
        public boolean pathsUnsaved() {
                return unsavedPaths;
        }
	
        public PathAndFillManager getPathAndFillManager() {
                return pathAndFillManager;
        }
	
        /* Just for convenience, keep casted references to the
           superclass's TracerCanvas objects */
	
        TracerCanvas xy_tracer_canvas;
        TracerCanvas xz_tracer_canvas;
        TracerCanvas zy_tracer_canvas;
	
        public ImagePlus getImagePlus() {
                return xy;
        }
	
        public Simple_Neurite_Tracer( ) {
                // Everything's set up in the run method...
        }
	
        /* This override the method in ThreePanes... */
	
	@Override
        public TracerCanvas createCanvas( ImagePlus imagePlus, int plane ) {
                return new TracerCanvas( imagePlus, this, plane );
        }
	
        public void cancelSearch( ) {
                if( currentSearchThread != null )
                        currentSearchThread.requestStop();
        }
	
	public void threadStatus( SearchThread source, int status ) {
		
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
		
                // This is called by both filler and currentSearchThread,
                // so distinguish these:
		
                if( source == currentSearchThread ) {
			
			if( success ) {
				Path result = currentSearchThread.getResult();
				if( result == null ) {
					IJ.error("Bug! Succeeded, but null result.");
					return;
				}
				// if (verbose) System.out.println( "finished, with endJoin: "+endJoin+" and "+endJoinIndex );
				result.setJoin( Path.PATH_END, endJoin, endJoinIndex );
				setTemporaryPath( result );
				
				resultsDialog.changeState(NeuriteTracerResultsDialog.QUERY_KEEP);
				
			} else {
				
				resultsDialog.changeState(NeuriteTracerResultsDialog.PARTIAL_PATH);
			}
			
			
			// Indicate in the dialog that we've finished...
			
			currentSearchThread = null;
			
			repaintAllPanes();
			
                }
		
        }
	
	public void pointsInSearch( SearchThread source, int inOpen, int inClosed ) {
		// IJ.error("FIXME: implement");
                // Just use this signal to repaint the canvas, in case there's
                // been no mouse movement.
                repaintAllPanes();
	}
	
        String nonsense = "unused"; // FIXME, just for synchronization...
	
        /* These member variables control what we're actually doing -
           whether that's tracing, logging points or displaying values
           of the Hessian at particular points. */
	
        boolean setupLog = false;
        boolean setupEv = false;
        boolean setupTrace = false;
        boolean setupPreprocess = false;
	
        /* If we're timing out the searches (probably not any longer...) */
	
        boolean setupTimeout = false;
        float   setupTimeoutValue = 0.0f;
	
        /* For the original file info - needed for loading the
           corresponding labels file and checking if a "tubes.tif"
           file was already there... */
	
        public FileInfo file_info;
	
        protected int width, height, depth;
	
        int last_x, last_y, last_z;
	
        String logFilename;
	
        public void setNewArrow( Arrow a ) {
		
                xy_tracer_canvas.unsetArrows( );
		xy_tracer_canvas.setArrow( 0, a );
		
		if( ! single_pane ) {
			zy_tracer_canvas.unsetArrows( );
			zy_tracer_canvas.setArrow( 0, a );
			
			xz_tracer_canvas.unsetArrows( );
			xz_tracer_canvas.setArrow( 0, a );
			
			zy_tracer_canvas.repaint();
			xz_tracer_canvas.repaint();
		}
                xy_tracer_canvas.repaint();
		
        }
	
        public void logPosition( int x, int y, int z, double ev1, double ev2, double ev3 ) {
		
                try {
                        BufferedWriter out = new BufferedWriter(new FileWriter(logFilename, true));
                        out.write(xy.getShortTitle()+"\t"+x+"\t"+y+"\t"+z+"\t"+ev1+"\t"+ev2+"\t"+ev2+"\n");
                        out.close();
                } catch (IOException e) {
                        IJ.error( "Writing to log file '" + logFilename + "' failed" );
                }
		
        }
	
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
	
        public void setArrow( int i, Arrow a ) {
		
                xy_tracer_canvas.setArrow( i, a );
		if( ! single_pane ) {
			zy_tracer_canvas.setArrow( i, a );
			xz_tracer_canvas.setArrow( i, a );
		}
		
        }
	
        public void setCrosshair( int new_x, int new_y, int new_z ) {
		
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
		
		/*
		  System.out.println("loaded materialList was of size: "+materialList.length);
		  for( int i = 0; i < materialList.length; ++i ) {
		  System.out.println("materialList["+i+"]: "+materialList[i]);
		  }
		*/
		
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
	
        public void mouseMovedTo( int x_in_pane, int y_in_pane, int in_plane, boolean shift_key_down, boolean join_modifier_down ) {
		
                int x, y, z;
		
                int [] p = new int[3];
                findPointInStack( x_in_pane, y_in_pane, in_plane, p );
                x = p[0];
                y = p[1];
                z = p[2];
		
                if( join_modifier_down ) {
			
                        PointInImage pointInImage = pathAndFillManager.nearestJoinPointOnSelectedPaths( x, y, z );
                        if( pointInImage != null ) {
                                x = pointInImage.x;
                                y = pointInImage.y;
                                z = pointInImage.z;
                        }
                }
		
                if( shift_key_down )
                        setSlicesAllPanes( x, y, z );
		
                if( (xy_tracer_canvas != null) &&
                    ((xz_tracer_canvas != null) || single_pane) &&
                    ((zy_tracer_canvas != null) || single_pane) ) {
			
                        setCrosshair( x, y, z );
			if( labelData != null ) {
				
				byte b = labelData[z][y*width+x];
				int m = b & 0xFF;
				
				String material = materialList[m];
				IJ.showStatus( "Material at crosshairs is: "+material);
			}
			
                        repaintAllPanes( ); // Or the crosshair isn't updated....
                }
		
                if( filler != null ) {
			synchronized (filler) {
				float distance = filler.getDistanceAtPoint(x,y,z);
				resultsDialog.showMouseThreshold(distance);
			}
                }
		
                last_x = x;
                last_y = y;
                last_z = z;
		
        }
	
        boolean lastStartPointSet = false;
	
        int last_start_point_x;
        int last_start_point_y;
        int last_start_point_z;
	
        Path endJoin;
        int endJoinIndex;
	
        /* If we've finished searching for a path, but the user hasn't
         * confirmed that they want to keep it yet, temporaryPath is
         * non-null and holds the Path we just searched out. */
	
        // Any method that deals with these two fields should be synchronized.
	
        Path temporaryPath = null;
        Path currentPath = null;
	
        // When we set temporaryPath, we also want to update the display
	
        synchronized public void setTemporaryPath( Path path ) {
		
                xy_tracer_canvas.setTemporaryPath( path );
		if( ! single_pane ) {
			zy_tracer_canvas.setTemporaryPath( path );
			xz_tracer_canvas.setTemporaryPath( path );
		}
		
                temporaryPath = path;
        }
	
        synchronized public void setCurrentPath( Path path ) {
		
                xy_tracer_canvas.setCurrentPath( path );
		if( ! single_pane ) {
			zy_tracer_canvas.setCurrentPath( path );
			xz_tracer_canvas.setCurrentPath( path );
		}
		
                currentPath = path;
        }
	
        synchronized public Path getCurrentPath( ) {
                return currentPath;
        }
	
        /* pathUnfinished indicates that we have started to create a
           path, but not yet finished it (in the sense of moving on to
           a new path with a differen starting point.)  FIXME: this
           may be redundant..
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
		
                ImagePlus ip = new ImagePlus( "Paths endered in a Stack", newStack );
                ip.show( );
        }
	
        /* If non-null, holds a reference to the currently searching thread */
	
        TracerThread currentSearchThread;
	
        /* Start a search thread looking for the goal in the arguments... */
	
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
			
                        // if (verbose) System.out.println("not setting endJoin");
                } else {
                        x_end = joinPoint.x;
                        y_end = joinPoint.y;
                        z_end = joinPoint.z;
                        endJoin = joinPoint.onPath;
                        endJoinIndex = joinPoint.onPathIndex;
			
                        // if (verbose) System.out.println("set endJoin");
                }
		
		currentSearchThread = new TracerThread(
			xy,		       
			0, // timeoutSeconds
			1000, // reportEveryMilliseconds
			last_start_point_x,
			last_start_point_y,
			last_start_point_z,
			x_end,
			y_end,
			z_end,
			true, // reciprocal
			(hessianEnabled ? hessian : null) );
		
		currentSearchThread.addProgressListener( this );
		
                currentSearchThread.start();
		
                repaintAllPanes();
        }
	
        synchronized public void confirmTemporary( ) {
		
                currentPath.add( temporaryPath );
		
                PointInImage last = temporaryPath.lastPoint();
                last_start_point_x = last.x;
                last_start_point_y = last.y;
                last_start_point_z = last.z;
		
                // if (verbose) System.out.println("confirming path; have "+allPaths.size()+" afterwards");
		
                if( temporaryPath.endJoins == null ) {
                        setTemporaryPath( null );
                        resultsDialog.changeState( NeuriteTracerResultsDialog.PARTIAL_PATH );
                        repaintAllPanes( );
                } else {
                        // if (verbose) System.out.println("confirming, but with an endJoin");
                        setTemporaryPath( null );
                        finishedPath( ); // Since joining onto another path for the end must finish the path.
                }
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
		
                pathAndFillManager.addPath( currentPath );
                setCurrentPath( null );
		
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
		
                // FIXME: in some of the states this doesn't make sense; check for them...
		
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
                endJoinIndex = -1;
		
                if( lastStartPointSet ) {
                        IJ.showStatus( "The start point has already been set; to finish a path press 'F'" );
                        return;
                }
		
                int [] p = new int[3];
                findPointInStack( x_in_pane, y_in_pane, plane, p );
		
                setPathUnfinished( true );
                lastStartPointSet = true;
		
                Path path = new Path();
		
                if( joinPoint == null ) {
                        last_start_point_x = p[0];
                        last_start_point_y = p[1];
                        last_start_point_z = p[2];
                } else {
                        last_start_point_x = joinPoint.x;
                        last_start_point_y = joinPoint.y;
                        last_start_point_z = joinPoint.z;
                        path.setJoin( Path.PATH_START, joinPoint.onPath, joinPoint.onPathIndex );
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
	
        double x_spacing;
        double y_spacing;
        double z_spacing;
	
        String spacing_units;
	
	public void viewFillIn3D( ) {
		ImagePlus imagePlus = filler.fillAsImagePlus( ! resultsDialog.createMask() );
                imagePlus.show();
	}
	
        public void setPositionAllPanes( int x, int y, int z ) {
		
                xy.setSlice( z + 1 );
                zy.setSlice( x );
                xz.setSlice( y );
		
        }
	
        byte [][] slices_data;
	
        NeuriteTracerResultsDialog resultsDialog;
	
        boolean cancelled = false;
	
        TextWindow helpTextWindow;
	
        // HessianAnalyzer hessianAnalyzer;
        ArchiveClient archiveClient;
	
        public void run( String ignoredArguments ) {
		
                // if (verbose) System.out.println("Macro options are: "+Macro.getOptions());
		
                // if (verbose) System.err.println("client running with arguments: "+arguments);
		
                Applet applet = IJ.getApplet();
                if( applet != null ) {
                        archiveClient = new ArchiveClient( applet, Macro.getOptions() );
                }
		
                if( archiveClient != null )
                        archiveClient.closeChannelsWithTag("nc82");
		
                try {
			
                        // toastKeyListeners( IJ.getInstance(), "IJ.getInstance()" );
			
                        ImagePlus currentImage = WindowManager.getCurrentImage();
			
                        if( currentImage == null ) {
                                IJ.error( "There's no current image to trace." );
                                return;
                        }

			if( (currentImage.getWidth() < 2) ||
                            (currentImage.getHeight() < 2) ||
                            (currentImage.getStackSize() < 2) ) {
                                IJ.error( "There must be at least two sample points in each dimension" );
                                return;
                        }
			
                        if( currentImage.getType() != ImagePlus.GRAY8 ) {
                                IJ.error("This plugin only works on 8 bit images at the moment.");
                                return;
                        }
					
                        width = currentImage.getWidth();
                        height = currentImage.getHeight();
                        depth = currentImage.getStackSize();
			
                        Calibration calibration = currentImage.getCalibration();
			
                        x_spacing = calibration.pixelWidth;
                        y_spacing = calibration.pixelHeight;
                        z_spacing = calibration.pixelDepth;
			
                        spacing_units = calibration.getUnit();
			
                        // if (verbose) System.out.println( "calibration was: " + x_spacing + ", " + y_spacing + ", " + z_spacing );

			pathAndFillManager = new PathAndFillManager(this);
					
                        file_info = currentImage.getOriginalFileInfo();
			
                        // Turn it grey, since I find that helpful...
			
                        IJ.runMacro("run(\"Grays\");");

			{
				String originalFileName=file_info.fileName;
				int lastDot=originalFileName.lastIndexOf(".");
				String beforeExtension=originalFileName.substring(0, lastDot);
				String tubesFileName=beforeExtension+".tubes.tif";
				ImagePlus tubenessImage = null;
				File tubesFile=new File(file_info.directory,tubesFileName);
				if( tubesFile.exists() ) {
					IJ.showStatus("Loading tubes file.");
					tubenessImage=BatchOpener.openFirstChannel(tubesFile.getAbsolutePath());
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
			
			single_pane = false;
			
			{
				GenericDialog gd = new GenericDialog("Simple Neurite Tracer (v" +
								     PLUGIN_VERSION + ")");
				gd.addMessage("Tracing: "+currentImage.getTitle());
				gd.addCheckbox("Use three pane view?", true);
				
				gd.showDialog();
				if (gd.wasCanceled())
					return;
				
				single_pane = ! gd.getNextBoolean();
			}
			
                        initialize(currentImage);
			
                        xy_tracer_canvas = (TracerCanvas)xy_canvas;
                        xz_tracer_canvas = (TracerCanvas)xz_canvas;
                        zy_tracer_canvas = (TracerCanvas)zy_canvas;
			
                        // toastKeyListeners( IJ.getInstance(), "IJ.getInstance()" );
			
                        setupTrace = true; // can be changed with the "just log points" or "show eigenvalues"
                        resultsDialog = new NeuriteTracerResultsDialog( "Tracing for: " + xy.getShortTitle(),
                                                                        this,
                                                                        applet != null );
			
                        pathAndFillManager.addPathAndFillListener(resultsDialog);
			
                        if( (x_spacing == 0.0) ||
                            (y_spacing == 0.0) ||
                            (z_spacing == 0.0) ) {
				
                                IJ.error( "One dimension of the calibration information was zero: (" +
                                          x_spacing + "," + y_spacing + "," + z_spacing + ")" );
                                return;
				
                        }
			
                        {
                                ImageStack s = xy.getStack();
                                slices_data = new byte[depth][];
                                for( int z = 0; z < depth; ++z ) {
                                        slices_data[z] = (byte []) s.getPixels( z + 1 );
                                }
                        }
			
                        /*
			  hessianAnalyzer = new HessianAnalyzer( slices_data,
			  width,
			  height,
			  depth,
			  x_spacing,
			  y_spacing,
			  z_spacing );
			  
                        */
			
                        // toastKeyListeners( xy_tracer_canvas, "xy_tracer_canvas" );
                        // toastKeyListeners( xy_window, "xy_window" );
			
                        xy_tracer_canvas.addKeyListener( xy_tracer_canvas );
                        xy_window.addKeyListener( xy_tracer_canvas );
			
			if( ! single_pane ) {
				
				// toastKeyListeners( xz_tracer_canvas, "xz_tracer_canvas" );
				// toastKeyListeners( xz_window, "xz_window" );
				
				xz_tracer_canvas.addKeyListener( xz_tracer_canvas );
				xz_window.addKeyListener( xz_tracer_canvas );
				
				// toastKeyListeners( zy_tracer_canvas, "zy_tracer_canvas" );
				// toastKeyListeners( zy_window, "zy_window" );
				
				zy_tracer_canvas.addKeyListener( zy_tracer_canvas );
				zy_window.addKeyListener( zy_tracer_canvas );
				
			}
			
			
                } finally {
			
                        IJ.getInstance().addKeyListener( IJ.getInstance() );
			
                }
        }
	
	public void startFillerThread( FillerThread filler ) {
		
		this.filler = filler;
		
		filler.addProgressListener(this);
		filler.addProgressListener(resultsDialog);
		
		filler.start();
		
                resultsDialog.changeState(NeuriteTracerResultsDialog.FILLING_PATHS);
		
	}
	
	// This should only be assigned to when synchronized on this object (FIXME: check)
        FillerThread filler = null;
	
        synchronized public void startFillingPaths( ) {
		
                // FIXME: check if one is running already, etc.
		
                filler = new FillerThread( xy,
					   false, // startPaused
                                           true, // reciprocal
                                           0.03f, // Initial threshold to display
                                           5000 ); // reportEveryMilliseconds
		
		filler.addProgressListener( this );
		filler.addProgressListener( resultsDialog );
		
		filler.getSourcePathsFromPlugin(this);
		
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
		// IJ.error("FIXME: implement");
        }
	
        public byte [] squareNormalToVector( int side,     // The number of samples in x and y in the plane, separated by step
                                             double step,   // step is in the same units as the _spacing, etc. variables.
                                             int original_x,     // These are are *not* yet scaled in z
                                             int original_y,     // They're just sample point differences
                                             int original_z,
                                             int normal_x,
                                             int normal_y,
                                             int normal_z,
                                             double [] x_basis_vector,    // The basis vectors are returned here
                                             double [] y_basis_vector ) { // they *are* scaled by _spacing
		
                double ox = original_x * x_spacing;
                double oy = original_y * y_spacing;
                double oz = original_z * z_spacing;
		
                if (verbose) System.out.println( "scaled start point is "+ox+","+oy+","+oz);
		
                double nx = normal_x * x_spacing;
                double ny = normal_y * y_spacing;
                double nz = normal_z * z_spacing;
		
                if (verbose) System.out.println( "scaled normal is "+nx+","+ny+","+nz);
		
                byte [] result = new byte[side*side];
		
                /* To find an arbitrary vector in the normal plane, do
                   the cross product with (0,0,1) */
		
                double ax = -ny;
                double ay = nx;
                double az = 0;
		
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
		
                byte [][] v = slices_data;
		
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
	
	
        synchronized public void fitCircles( int index, boolean display, int withinSide ) {
		
                Path s = pathAndFillManager.getPath(index);
                Path fitted = s.fitCircles( withinSide, this, display );
                pathAndFillManager.addPath( fitted );
        }
	
        boolean hessianEnabled = false;
        ComputeCurvatures hessian = null;
	
	// Even better, we might have a "tubeness" file already there:

	float [][] tubeness;

        public synchronized void enableHessian( boolean enable ) {
                if( enable ) {
                        if( hessian == null ) {
                                hessian = new ComputeCurvatures( xy, 1.0, this );
                                new Thread(hessian).start();
                                hessianEnabled = false;
                        } else {
                                hessianEnabled = true;
                        }
                } else {
                        hessianEnabled = false;
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
			resultsDialog.gaussianCalculated(false);
			IJ.showProgress(1.0);
			return;
		} else if( proportion >= 1.0 ) {
                        hessianEnabled = true;
                        resultsDialog.gaussianCalculated(true);
                }
                IJ.showProgress(proportion);
        }
	
        public void getTracings( boolean mineOnly ) {
                boolean result = pathAndFillManager.getTracings( mineOnly, archiveClient );
                if( result )
                        unsavedPaths = false;
        }
	
        public void uploadTracings( ) {
                boolean result = pathAndFillManager.uploadTracings( archiveClient );
                if( result )
                        unsavedPaths = false;
		
        }
	
	
}

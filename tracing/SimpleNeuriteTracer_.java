/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package tracing;

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

import java.util.PriorityQueue;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Date;
import java.util.ArrayList;

import java.text.SimpleDateFormat;

import java.io.*;

import client.ArchiveClient;

import java.util.StringTokenizer;

import stacks.ThreePanes;

import util.Arrow;
import util.ArrowDisplayer;

/* Note On Confusing Terminology: traces and paths are the same
   thing; they're made up of connections.  Traces, paths and
   connections are all non-branching sequences of adjacent points in
   the image. */

public class SimpleNeuriteTracer_ extends ThreePanes
	implements PlugIn, AStarProgressCallback, ArrowDisplayer, FillerProgressCallback {

	boolean unsavedPaths = false;

	public boolean pathsUnsaved() {
		return unsavedPaths;
	}

	/* Just for convenience, keep casted references to the
	   superclass's TracerCanvas objects */

	TracerCanvas xy_tracer_canvas;
	TracerCanvas xz_tracer_canvas;
	TracerCanvas zy_tracer_canvas;

	public ImagePlus getImagePlus() {
		return xy;
	}
	
	public SimpleNeuriteTracer_( ) {
		// Everything's set up in the run method...
	}
	
	/* This override the method in ThreePanes... */

	public TracerCanvas createCanvas( ImagePlus imagePlus, int plane ) {
		return new TracerCanvas( imagePlus, this, plane );
	}

	public void cancelSearch( ) {
		if( currentSearchThread != null )
			currentSearchThread.requestStop();
	}

	public void cancelFilling( ) {
		if( filler != null )
			filler.requestStop();
	}

	/* Now a couple of callback methods, which get information
	   about the progress of the search. */

	public void finished( boolean success ) {

		if( success ) {
			Connection result = currentSearchThread.getResult();
			if( result == null )
				IJ.error("Bug! Succeeded, but null result.");
			else
				setTemporaryConnection( result );
		}
		synchronized(nonsense) {
			currentOpenBoundaryPoints = null;
		}
		
		// Indicate in the dialog that we've finished...

		if( success )
			resultsDialog.changeState(NeuriteTracerResultsDialog.QUERY_KEEP);
		else
			resultsDialog.changeState(NeuriteTracerResultsDialog.PARTIAL_PATH);

		currentSearchThread = null;

		repaintAllPanes();

	}

	short[] currentSubthresholdFillerPoints;
	short[] currentOpenBoundaryPoints;
	String nonsense = "unused"; // FIXME, just for synchronization...

	public void currentOpenBoundary( short[] points ) {

		// FIXME: complete this

		synchronized(nonsense) {
			this.currentOpenBoundaryPoints = points;
			resultsDialog.updateSearchingStatistics(points.length/3);
		}
		
		repaintAllPanes();
		
	}

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
	   corresponding labels file. */

	public FileInfo file_info;
	
	protected int width, height, depth;
	
	/* (FIXME: check this is right) The connection that we've just found... */
	private Connection currentConnection;
	
	int last_x, last_y, last_z;
	
	String logFilename;

	public void setNewArrow( Arrow a ) {

		xy_tracer_canvas.unsetArrows( );
		xy_tracer_canvas.setArrow( 0, a );
			
		zy_tracer_canvas.unsetArrows( );
		zy_tracer_canvas.setArrow( 0, a );
			
		xz_tracer_canvas.unsetArrows( );
		xz_tracer_canvas.setArrow( 0, a );
			
		zy_tracer_canvas.repaint();
		xz_tracer_canvas.repaint();
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

	public void justDisplayNearSlices( boolean value ) {

		xy_tracer_canvas.just_near_slices = value;
		xz_tracer_canvas.just_near_slices = value;
		zy_tracer_canvas.just_near_slices = value;

		repaintAllPanes();

	}
	
	public void setArrow( int i, Arrow a ) {
		
		xy_tracer_canvas.setArrow( i, a );
		zy_tracer_canvas.setArrow( i, a );
		xz_tracer_canvas.setArrow( i, a );
		
	}
	
	public void setCrosshair( int new_x, int new_y, int new_z ) {

		xy_tracer_canvas.setCrosshairs( new_x, new_y, new_z, true );
		xz_tracer_canvas.setCrosshairs( new_x, new_y, new_z, true );
		zy_tracer_canvas.setCrosshairs( new_x, new_y, new_z, true );
		
	}

	public void getTracings( boolean mineOnly ) {

		Hashtable<String,String> parameters = new Hashtable<String,String>();

		parameters.put("method","most-recent-annotation");
		parameters.put("type","traces");
		parameters.put("variant","basic");
		parameters.put("md5sum",archiveClient.getValue("md5sum"));
		if( mineOnly )
			parameters.put("for_user",archiveClient.getValue("user"));
		else
			parameters.put("for_user","");

		ArrayList< String [] > tsv_results = archiveClient.synchronousRequest( parameters, null );

		String [] first_line = (String [])tsv_results.get(0);
		int urls_found;
		String bestUrl = null;
		if( first_line[0].equals("success") ) {
			urls_found = Integer.parseInt(first_line[1]);
			if( urls_found == 0 )
				IJ.error( "No anntation files by " + (mineOnly ? archiveClient.getValue("user") : "any user") + " found." );
			else {
				bestUrl = ((String [])tsv_results.get(1))[1];
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

		// IJ.error( "got fileContents: " +fileContents);

		if( fileContents != null ) {

			ArrayList< SegmentedConnection > all_paths = loadFromString(fileContents);
			// IJ.error("got new all_paths: " + all_paths);
			if( all_paths != null )
				setAllPaths( all_paths );
		}

	}

	static public ArrayList< SegmentedConnection > loadFromString( String fileContents ) {

		ArrayList< SegmentedConnection > all_paths = new ArrayList< SegmentedConnection >();

		StringTokenizer tokenizer = new StringTokenizer( fileContents, "\n" );

		try {

			int last_path_index = -1;
			
			String line;
			Connection currentConnection = new Connection();
			
			while( tokenizer.hasMoreTokens() ) {

				line = (String)tokenizer.nextToken();

				int nextTabIndex = line.indexOf('\t');
				if( nextTabIndex < 0 )
					throw new Exception("No tabs found in the line");
				int path_index = Integer.parseInt( line.substring(0,nextTabIndex) );
				line = line.substring(nextTabIndex+1);
				
				nextTabIndex = line.indexOf('\t');
				if( nextTabIndex < 0 )
					throw new Exception("Not enough fields in the line");
				int x = Integer.parseInt( line.substring(0,nextTabIndex) );
				line = line.substring(nextTabIndex+1);
				
				nextTabIndex = line.indexOf('\t');
				if( nextTabIndex < 0 )
					throw new Exception("Not enough fields in the line");
				int y = Integer.parseInt( line.substring(0,nextTabIndex) );
				line = line.substring(nextTabIndex+1);
				
				nextTabIndex = line.indexOf('\t');
				if( nextTabIndex < 0 )
					throw new Exception("No tabs found in the first line");
				int z = Integer.parseInt( line.substring(0,nextTabIndex) );
				line = line.substring(nextTabIndex+1);
				
				boolean join = new Boolean(line).booleanValue();

				// System.out.println( "got point " + path_index + ", (" + x + ", " + y + ", " + z + ") " + join );

				// System.out.println( "(last path_index " + last_path_index + ", current: " + path_index + ")" );

				if( ((last_path_index >= 0) && (last_path_index != path_index)) || ! tokenizer.hasMoreTokens() ) {
					
					// IJ.error( "adding that path" );

					// System.out.println("adding that path");

					SegmentedConnection s = new SegmentedConnection();
					s.addConnection( currentConnection );
					s.startsAtJoin( join );

					all_paths.add( s );
					
					currentConnection = new Connection();

				}

				currentConnection.addPoint( x, y, z );
				
				last_path_index = path_index;

			}

		} catch( Exception e ) {
			
			IJ.error( "Exception while parsing the data" );
			return null;
		}
		
		return all_paths;
	}

	static public ArrayList< SegmentedConnection > loadTracingsFromFile( String filename ) {

		ArrayList< SegmentedConnection > all_paths = new ArrayList< SegmentedConnection >();

		try {

			BufferedReader in = new BufferedReader( new FileReader(filename) );
			
			int last_path_index = -1;
			
			String line;
			Connection currentConnection = new Connection();
			boolean join = false;
			
			while( null != (line = in.readLine()) ) {
				
				int nextTabIndex = line.indexOf('\t');
				if( nextTabIndex < 0 )
					throw new Exception("No tabs found in the line");
				int path_index = Integer.parseInt( line.substring(0,nextTabIndex) );
				line = line.substring(nextTabIndex+1);
				
				nextTabIndex = line.indexOf('\t');
				if( nextTabIndex < 0 )
					throw new Exception("Not enough fields in the line");
				int x = Integer.parseInt( line.substring(0,nextTabIndex) );
				line = line.substring(nextTabIndex+1);
				
				nextTabIndex = line.indexOf('\t');
				if( nextTabIndex < 0 )
					throw new Exception("Not enough fields in the line");
				int y = Integer.parseInt( line.substring(0,nextTabIndex) );
				line = line.substring(nextTabIndex+1);
				
				nextTabIndex = line.indexOf('\t');
				if( nextTabIndex < 0 )
					throw new Exception("No tabs found in the first line");
				int z = Integer.parseInt( line.substring(0,nextTabIndex) );
				line = line.substring(nextTabIndex+1);
				
				join = new Boolean(line).booleanValue();

				// System.out.println( "got point " + path_index + ", (" + x + ", " + y + ", " + z + ") " + join );

				// System.out.println( "(last path_index " + last_path_index + ", current: " + path_index + ")" );

				if( (last_path_index >= 0) && (last_path_index != path_index) ) {
					
					// System.out.println("adding that path");

					SegmentedConnection s = new SegmentedConnection();
					s.addConnection( currentConnection );
					s.startsAtJoin( join );

					all_paths.add( s );
					
					currentConnection = new Connection();

				}

				currentConnection.addPoint( x, y, z );
				
				last_path_index = path_index;

			}

			// Now just add the last path.

			SegmentedConnection s = new SegmentedConnection();
			s.addConnection( currentConnection );
			s.startsAtJoin( join );

			all_paths.add( s );


		} catch( FileNotFoundException e ) {
			
			IJ.error( "Couldn't find the file: " + filename );
			return null;

		} catch( IOException e ) {
			
			IJ.error( "There was an error while reading the file: " + filename );
			return null;

		} catch( Exception e ) {
			
			IJ.error( "Exception while reading the file: " + filename );
			return null;
		}

		return all_paths;
	}

	public static byte [] tracesAsBytes( ArrayList< SegmentedConnection > all_paths ) {
		String s = tracesAsString( all_paths );
		try {
			byte [] bytes = s.getBytes("UTF-8");
			return bytes;
		} catch( UnsupportedEncodingException e ) {
			return null;
		}
	}

	public static String tracesAsString( ArrayList< SegmentedConnection > all_paths ) {
		
		StringBuffer sb = new StringBuffer();

		if( all_paths != null ) {
			// System.out.println("Have some all_paths paths to draw.");
			int paths = all_paths.size();
			// System.out.println("Paths to draw: "+paths);
			for( int i = 0; i < paths; ++i ) {

				int last_x = -1;
				int last_y = -1;
				int last_z = -1;

				SegmentedConnection s = (SegmentedConnection)all_paths.get(i);
				int segments_in_path = s.connections.size();
				for( int j = 0; j < segments_in_path; ++j ) {
					Connection connection = (Connection)s.connections.get(j);
					for( int k = 0; k < connection.points; ++k ) {
						int x = connection.x_positions[k];
						int y = connection.y_positions[k];
						int z = connection.z_positions[k];
						if( (last_x == x) && (last_y == y) && (last_z == z) ) {
							// Skip this, it's just the same.
						} else {
							String toWrite = "" + i + "\t" +
								x + "\t" +
								y + "\t" + 
								z + "\t" +
								s.join_at_start + "\n";
							// System.out.println( "Writing line: " + toWrite );
							sb.append( toWrite );							
						}
						last_x = x;
						last_y = y;
						last_z = z;
					}
				}
				
			}
		}

		return sb.toString();
                
	}

	public void uploadTracings( ) {

		Hashtable<String,String> parameters = new Hashtable<String,String>();

		parameters.put("method","upload-annotation");
		parameters.put("type","traces");
		parameters.put("variant","basic");
		parameters.put("md5sum",archiveClient.getValue("md5sum"));

		// Need to included data too....
		
		byte [] fileAsBytes;

		synchronized(this) {
			fileAsBytes = tracesAsBytes( allPaths );
		}

		ArrayList< String [] > tsv_results = archiveClient.synchronousRequest( parameters, fileAsBytes );

		String [] first_line = (String [])tsv_results.get(0);
		if( first_line[0].equals("success") ) {
			IJ.error("Annotations uploaded successfully!");
			unsavedPaths = false;
		} else if( first_line[0].equals("error") ) {
			IJ.error("There was an error while uploading the annotation file: "+first_line[1]);
		} else {
			IJ.error("There was an unknown response to the annotation file upload request: " + first_line[0]);
		}
		
	}
	
	synchronized public void loadTracings( ) {
			
		String fileName;
		String directory;

		if( file_info != null ) {

			fileName = file_info.fileName;
			directory = file_info.directory;
			
			File possibleLoadFile = new File(directory,fileName+".traces");
			
			String path = possibleLoadFile.getPath();
			
			if(possibleLoadFile.exists()) {
						
				YesNoCancelDialog d = new YesNoCancelDialog( IJ.getInstance(),
									     "Confirm",
									     "Load the default traces file? ("+path+")" );
				
				if( d.yesPressed() ) {
					
					ArrayList< SegmentedConnection > all_paths = loadTracingsFromFile(path);
					if( all_paths != null )
						setAllPaths( all_paths );
					
					unsavedPaths = false;
					
					return;
					
				} else if( d.cancelPressed() ) {

					return;
					
				}
			}
		}

		//  Presumably "No" was pressed...

		OpenDialog od;
		
		od = new OpenDialog("Select traces file...",
				    null,
				    null );
		
		fileName = od.getFileName();
		directory = od.getDirectory();

		if( fileName != null ) {				

			ArrayList< SegmentedConnection > all_paths = loadTracingsFromFile(directory+File.separator+fileName);
			if( all_paths != null )
				setAllPaths( all_paths );
			
			unsavedPaths = false;
			
			return;
		}

	}

	public void writeTracesToFile( String filename ) {
		
                try {

                        BufferedWriter out = new BufferedWriter(new FileWriter(filename,false));

			if( allPaths != null ) {
				// System.out.println("Have some allPaths paths to draw.");
				int paths = allPaths.size();
				// System.out.println("Paths to draw: "+paths);
				for( int i = 0; i < paths; ++i ) {

					int last_x = -1;
					int last_y = -1;
					int last_z = -1;

					SegmentedConnection s = (SegmentedConnection)allPaths.get(i);
					int segments_in_path = s.connections.size();
					for( int j = 0; j < segments_in_path; ++j ) {
						Connection connection = (Connection)s.connections.get(j);
						for( int k = 0; k < connection.points; ++k ) {
							int x = connection.x_positions[k];
							int y = connection.y_positions[k];
							int z = connection.z_positions[k];
							if( (last_x == x) && (last_y == y) && (last_z == z) ) {
								// Skip this, it's just the same.
							} else {
								String toWrite = "" + i + "\t" +
									x + "\t" +
									y + "\t" + 
									z + "\t" +
									s.join_at_start + "\n";
								// System.out.println( "Writing line: " + toWrite );
								out.write( toWrite );

							}
							last_x = x;
							last_y = y;
							last_z = z;
						}
					}

				}
			}

                        out.close();

                } catch (IOException e) {
                        IJ.error( "Writing traces to file '" + filename + "' failed" );
                }

	}
	
	public void mouseMovedTo( int x_in_pane, int y_in_pane, int in_plane, boolean shift_key_down ) {

		int [] p = new int[3];

		findPointInStack( x_in_pane, y_in_pane, in_plane, p );

		int x = p[0];
		int y = p[1];
		int z = p[2];

		if( shift_key_down )
			setSlicesAllPanes( x, y, z );
		
		if( (xy_tracer_canvas != null) &&
		    (xz_tracer_canvas != null) &&
		    (zy_tracer_canvas != null) ) {
			
			if( currentConnection != null ) {
				
				xy_tracer_canvas.setConnection( currentConnection );
				xz_tracer_canvas.setConnection( currentConnection );
				zy_tracer_canvas.setConnection( currentConnection );
				
			}
		
			setCrosshair( x, y, z );

			repaintAllPanes( ); // Or the crosshair isn't updated....
		}

		if( filler != null ) {
			float distance = filler.getDistanceAtPoint(x,y,z);
			resultsDialog.showMouseThreshold(distance);
		}

		last_x = x;
		last_y = y;
		last_z = z;
		
	}
	
	boolean lastStartPointSet = false;

	int last_start_point_x;
	int last_start_point_y;
	int last_start_point_z;

	ArrayList< SegmentedConnection > allPaths;

	/* If we've finished searching for a path, but the user hasn't
	 * confirmed that they want to keep it yet, temporaryConnection is
	 * non-null and holds the Connection we just searched out. */

	Connection temporaryConnection = null;

	// When we set temporaryConnection, we also want to update the display

	public void setTemporaryConnection( Connection connection ) {

		xy_tracer_canvas.setTemporaryConnection( connection );
		zy_tracer_canvas.setTemporaryConnection( connection );
		xz_tracer_canvas.setTemporaryConnection( connection );

		temporaryConnection = connection;
	}

	/* pathUnfinished indicates that we have started to create a
	   path, but not yet finished it (in the sense of moving on
	   the a new disconnected starting.  FIXME: this may be
	   redundant..
         */

	boolean pathUnfinished = false;

	public void setPathUnfinished( boolean unfinished ) {

		this.pathUnfinished = unfinished;
		xy_tracer_canvas.setPathUnfinished( unfinished );
		zy_tracer_canvas.setPathUnfinished( unfinished );
		xz_tracer_canvas.setPathUnfinished( unfinished );
	}

	synchronized public void setAllPaths( ArrayList< SegmentedConnection > allPaths ) {

		// System.out.println("Setting completed in each canvas to: " +allPaths );

		xy_tracer_canvas.setCompleted( allPaths );
		zy_tracer_canvas.setCompleted( allPaths );
		xz_tracer_canvas.setCompleted( allPaths );

		this.allPaths = allPaths;

		int paths = allPaths.size();
		// System.out.println("Paths to draw: "+paths);
		for( int i = 0; i < paths; ++i ) {
						
			SegmentedConnection s = (SegmentedConnection)allPaths.get(i);
			resultsDialog.addPathToList("Path with index: " +(i+1));
		}

		repaintAllPanes();
	}

	int [] selectedPaths = null;

	public synchronized void showPaths(int [] selectedIndices) {

		int length = selectedIndices.length;
		selectedPaths = new int[length];
		System.arraycopy(selectedIndices,0,selectedPaths,0,length);

		repaintAllPanes();
	}

	synchronized boolean pathSelected( int index ) {
		if( selectedPaths != null ) {
			for( int i = 0; i < selectedPaths.length; ++i ) {
				if( selectedPaths[i] == index )
					return true;
			}
		}
		return false;
	}

	/* Create a new 8 bit ImagePlus of the same dimensions as this
	   image, but with values set to either 255 (if there's a point
	   on a path there) or 0 */

	synchronized public void makePathVolume( ) {


                byte [][] snapshot_data = new byte[depth][];

                for( int i = 0; i < depth; ++i )
                        snapshot_data[i] = new byte[width*height];
		
		// ------------------------------------------------------------
			
		if( allPaths != null ) {
			// System.out.println("Have some allPaths paths to draw.");
			int paths = allPaths.size();
			// System.out.println("Paths to draw: "+paths);
			for( int i = 0; i < paths; ++i ) {
				SegmentedConnection s = (SegmentedConnection)allPaths.get(i);
				int segments_in_path = s.connections.size();
				for( int j = 0; j < segments_in_path; ++j ) {
					Connection connection = (Connection)s.connections.get(j);
					for( int k = 0; k < connection.points; ++k ) {
						int x = connection.x_positions[k];
						int y = connection.y_positions[k];
						int z = connection.z_positions[k];
						snapshot_data[z][y*width+x] = (byte)255;
					}
				}
			}
		}

		// ------------------------------------------------------------
		
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

	AStarThread currentSearchThread;

	/* Start a search thread looking for the goal in the arguments... */

	synchronized public void testPathTo( int x_in_pane, int y_in_pane, int plane ) {

		if( ! lastStartPointSet ) {
			IJ.showStatus( "No initial start point has been set.  Do that with a mouse click." +
				      " (Or a shift-click if the start of the path should join another neurite." );
			return;
		}

		if( temporaryConnection != null ) {
			IJ.showStatus( "There's already a temporary path; use 'N' to cancel it or 'Y' to keep it." );
			return;
		}

		int [] p = new int[3];
		findPointInStack( x_in_pane, y_in_pane, plane, p );

		currentSearchThread = new AStarThread( slices_data, 
						       last_start_point_x,
						       last_start_point_y,
						       last_start_point_z,
						       p[0],
						       p[1],
						       p[2],
						       this,
						       true, // reciprocal
						       false, // preprocess
						       0, // timeoutSeconds
						       1000, // reportEveryMilliseconds 
						       this );

		currentSearchThread.start();

		repaintAllPanes();

	}

	synchronized public void confirmTemporary( ) {

		int pathsSoFar = allPaths.size();
		// System.out.println("confirming path; have "+pathsSoFar+" already");
		SegmentedConnection currentPath = (SegmentedConnection)allPaths.get(pathsSoFar-1);
		currentPath.addConnection( temporaryConnection );
		PointInImage last = temporaryConnection.lastPoint();
		last_start_point_x = last.x;
		last_start_point_y = last.y;
		last_start_point_z = last.z;
		setTemporaryConnection( null );

		// System.out.println("confirming path; have "+allPaths.size()+" afterwards");
		
		resultsDialog.changeState( NeuriteTracerResultsDialog.PARTIAL_PATH );
			
		repaintAllPanes();
	}

	synchronized public void cancelTemporary( ) {
		
		if( ! lastStartPointSet ) {
			IJ.error( "No initial start point has been set yet.  Do that with a mouse click." +
				  " (Or a shift-click if the start of the path should join another neurite." );
			return;
		}

		if( temporaryConnection == null ) {
			IJ.error( "There's no temporary path to cancel!" );
			return;
		}

		// Remove that last temporary path...
		setTemporaryConnection( null );

		resultsDialog.changeState( NeuriteTracerResultsDialog.PARTIAL_PATH );

		repaintAllPanes( );
	}

	synchronized public void cancelPath( ) {

		allPaths.remove(allPaths.size()-1);

		lastStartPointSet = false;
		setPathUnfinished( false );
		
		resultsDialog.changeState( NeuriteTracerResultsDialog.WAITING_TO_START_PATH );		
		
		repaintAllPanes();
	}
	
	synchronized public void removePath( int index ) {

		if( (index < 0) || (index >= allPaths.size()) ) {
			IJ.error( "BUG: NeuriteTracer_ asked to remove an out-of-range index" );
		}

		allPaths.remove(index);

		repaintAllPanes();

	}

	synchronized public void finishedPath( ) {

		// Is there an unconfirmed path?  If so, warn people about it...
		
		if( temporaryConnection != null ) {
			IJ.error( "There's an unconfirmed path, need to confirm or cancel it before finishing the path." );
			return;
		}

		lastStartPointSet = false;
		setPathUnfinished( false );
	       
		// Now tell the UI that it can add that path...

		{
			String nameForList = "Path with index: " + allPaths.size();
			resultsDialog.addPathToList( nameForList );

		}

		unsavedPaths = true;

		// ... and change the state of the UI
		resultsDialog.changeState( NeuriteTracerResultsDialog.WAITING_TO_START_PATH );

		repaintAllPanes( );
	}

	synchronized public void clickForTrace( int x_in_pane, int y_in_pane, int plane, boolean join ) {
		
		// FIXME: in some of the states this doesn't make sense; check for them...

		if( currentSearchThread != null )
			return;

		if( temporaryConnection != null )
			return;

		if( filler != null ) {
			setFillThresholdFrom( x_in_pane, y_in_pane, plane );
			return;
		}

		if( pathUnfinished ) {
			/* Then this is a succeeding point, and we
			   should start a search. */
			testPathTo( x_in_pane, y_in_pane, plane );
			resultsDialog.changeState( NeuriteTracerResultsDialog.SEARCHING );
		} else {
			/* This is an initial point. */
			startPath( x_in_pane, y_in_pane, plane, join );
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

	public void setFillThreshold( float distance ) {

		if( distance > 0 ) {

			System.out.println("Setting new threshold of: "+distance);
		
			resultsDialog.thresholdChanged(distance);
			
			filler.setThreshold(distance);
			filler.displayUpdate();

		}
		
	}

	synchronized public void startPath( int x_in_pane, int y_in_pane, int plane, boolean join ) {

		if( lastStartPointSet ) {
			IJ.showStatus( "The start point has already been set; to finish a path press 'F'" );
			return;
		}

		if( allPaths == null ) {
			setAllPaths( new ArrayList< SegmentedConnection >( ) );
		}

		int [] p = new int[3];
		findPointInStack( x_in_pane, y_in_pane, plane, p );

		setPathUnfinished( true );
		lastStartPointSet = true;
		last_start_point_x = p[0];
		last_start_point_y = p[1];
		last_start_point_z = p[2];
		SegmentedConnection s = new SegmentedConnection();
		s.startsAtJoin( join );
		allPaths.add( s );

	}	

	/* Return true if we have just started a new path, but have
	   not yet added any connections to it, otherwise return
	   false. */

	public boolean justFirstPoint() {
		
		if( pathUnfinished ) {
			SegmentedConnection currentPath = (SegmentedConnection)allPaths.get(allPaths.size()-1);
			if( currentPath.size() == 0 )
				return true;
		}
		
		return false;
	}

	String getStackTrace( ) {
		StringWriter sw = new StringWriter();
		new Exception("Dummy Exception for Stack Trace").printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}
	
	double x_spacing;
	double y_spacing;
	double z_spacing;
	
	public void setPositionAllPanes( int x, int y, int z ) {
		
		xy.setSlice( z + 1 );
		zy.setSlice( x );
		xz.setSlice( y );
		
	}
	
	byte [][] slices_data;
	
	NeuriteTracerResultsDialog resultsDialog;

	boolean cancelled = false;
	
	TextWindow helpTextWindow;

	HessianAnalyzer hessianAnalyzer;
	ArchiveClient archiveClient;

	public void run( String ignoredArguments ) {
		
		// System.err.println("Macro options are: "+Macro.getOptions());

		// System.err.println("client running with arguments: "+arguments);

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

			file_info = currentImage.getOriginalFileInfo();

			// Turn it grey, since I find that helpful...

			IJ.runMacro("run(\"Grays\");");

			initialize(currentImage);

			xy_tracer_canvas = (TracerCanvas)xy_canvas;
			xz_tracer_canvas = (TracerCanvas)xz_canvas;
			zy_tracer_canvas = (TracerCanvas)zy_canvas;

			// toastKeyListeners( IJ.getInstance(), "IJ.getInstance()" );

			setupTrace = true; // can be changed with the "just log points" or "show eigenvalues"
			resultsDialog = new NeuriteTracerResultsDialog( "Tracing for: " + xy.getShortTitle(),
									this,
									applet != null );

			width = xy.getWidth();
			height = xy.getHeight();
			depth = xy.getStackSize();
			
			Calibration calibration = xy.getCalibration();
			
			x_spacing = calibration.pixelWidth;
			y_spacing = calibration.pixelHeight;
			z_spacing = calibration.pixelDepth;
			
			// System.out.println( "calibration was: " + x_spacing + ", " + y_spacing + ", " + z_spacing );
			
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

			hessianAnalyzer = new HessianAnalyzer( slices_data,
							       width,
							       height,
							       depth,
							       x_spacing,
							       y_spacing,
							       z_spacing );

			/* We may want the analyzeAtPoint method to
			 * draw back into the plugin, so set this... */
			
			


			// toastKeyListeners( xy_tracer_canvas, "xy_tracer_canvas" );
			// toastKeyListeners( xy_window, "xy_window" );
			
			xy_tracer_canvas.addKeyListener( xy_tracer_canvas );
			xy_window.addKeyListener( xy_tracer_canvas );

			// toastKeyListeners( xz_tracer_canvas, "xz_tracer_canvas" );
			// toastKeyListeners( xz_window, "xz_window" );

			xz_tracer_canvas.addKeyListener( xz_tracer_canvas );
			xz_window.addKeyListener( xz_tracer_canvas );

			// toastKeyListeners( zy_tracer_canvas, "zy_tracer_canvas" );
			// toastKeyListeners( zy_window, "zy_window" );

			zy_tracer_canvas.addKeyListener( zy_tracer_canvas );
			zy_window.addKeyListener( zy_tracer_canvas );

			
		} finally {
				
			IJ.getInstance().addKeyListener( IJ.getInstance() );
			
		}
	}	

	FillerThread filler = null;

	synchronized public void startFillingPaths( ) {
		
		// FIXME: check if one is running already, etc.

		filler = new FillerThread( this,
					   true, // reciprocal
					   false, // preprocess
					   0.03f, // Initial threshold to display
					   5000, // reportEveryMilliseconds
					   this ); // callback


		filler.start();

		resultsDialog.changeState(NeuriteTracerResultsDialog.FILLING_PATHS);
		
	}

	public void pointsWithinThreshold( short [] points ) {
		
		synchronized(nonsense) {
			this.currentSubthresholdFillerPoints = points;
		}
		
		repaintAllPanes();

	}

	synchronized public void stopped() {

		synchronized(nonsense) {
			this.currentSubthresholdFillerPoints = null;
		}
		
		repaintAllPanes();
	}

	public void maximumDistanceCompletelyExplored( float f ) {

		resultsDialog.setMaxDistanceExplored(f);

	}

	public void viewFillIn3D() {
		
		byte [][] new_slice_data = new byte[depth][];
		for( int z = 0; z < depth; ++z ) {
			new_slice_data[z] = new byte[width * height];
		}

		boolean realData = true;

		synchronized(nonsense) {
			int n = currentSubthresholdFillerPoints.length / 3;
			for( int i = 0; i < n; ++i ) {

				int x = currentSubthresholdFillerPoints[3*i];
				int y = currentSubthresholdFillerPoints[3*i+1];
				int z = currentSubthresholdFillerPoints[3*i+2];

				if( realData ) {

					
					new_slice_data[z][y*width+x] = slices_data[z][y*width+x];
					
				} else {

					new_slice_data[z][y*width+x] = (byte)255;
				}
			}
		}

		ImageStack stack = new ImageStack(width,height);

		for( int z = 0; z < depth; ++z ) {
			ByteProcessor bp = new ByteProcessor(width,height);
			bp.setPixels( new_slice_data[z] );
			stack.addSlice(null,bp);
		}

		ImagePlus imp=new ImagePlus("filled neuron",stack);

		imp.setCalibration(xy.getCalibration());

		imp.show();

		/*
		ImageJ_3D_Viewer viewer = new ImageJ_3D_Viewer();		
		viewer.setup("",imp);
		viewer.run(imp.getProcessor());
		*/

	}
}

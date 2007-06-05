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

import vib.transforms.OrderedTransformations;

import math3d.Point3d;
import math3d.JacobiFloat;

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

import amira.AmiraParameters;
import amira.AmiraMeshEncoder;
import amira.AmiraMeshDecoder;
import vib.SegmentationViewerCanvas;
import vib.FastMatrix;
import client.ArchiveClient;

import java.util.StringTokenizer;

import vib.oldregistration.Bookstein_FromMarkers;
import util.BatchOpener;
import util.FileAndChannel;

import stacks.ThreePanes;

class EigenResultsFloat {
	
	public float[] sortedValues;
	public float[][] sortedVectors;
	
}

class EigenResultsDouble {
	
	public double[] sortedValues;
	public double[][] sortedVectors;
	
}

public class NeuriteTracer_ extends ThreePanes implements PlugIn, AStarProgressCallback {
	
	OrderedTransformations transformation;
	byte[][] label_data;
	String [] materialNames;       
	int materials;

	int label_stack_width;
	int label_stack_height;
	int label_stack_depth;

	/* Just for convenience, keep casted references to the
	   superclass's TracerCanvas objects */

	TracerCanvas xy_tracer_canvas;
	TracerCanvas xz_tracer_canvas;
	TracerCanvas zy_tracer_canvas;

	public ImagePlus getImagePlus() {
		return xy;
	}
	
	public NeuriteTracer_( ) {
		
	}
	
	public TracerCanvas createCanvas( ImagePlus imagePlus, int plane ) {
		return new TracerCanvas( imagePlus, this, plane );
	}

	public void setSlicesAllPanes( int new_x, int new_y, int new_z ) {

		zy.setSlice( new_x + 1 );
		xz.setSlice( new_y + 1 );
		xy.setSlice( new_z + 1 );
	}

	public void repaintAllPanes( ) {

		xy_canvas.repaint();
		xz_canvas.repaint();
		zy_canvas.repaint();
	}

	public PointInImage findPointInStack( int x_in_pane, int y_in_pane, int plane ) {

		int new_x, new_y, new_z;
		
		new_x = -1;
		new_y = -1;
		new_z = -1;        
		
		switch( plane ) {
			
		case TracerCanvas.XY_PLANE:
		{
			new_x = x_in_pane;
			new_y = y_in_pane;
			new_z = xy.getCurrentSlice( ) - 1;
		}
		break;
		
		case TracerCanvas.XZ_PLANE:
		{
			new_x = x_in_pane;
			new_y = xz.getCurrentSlice( ) - 1;
			new_z = y_in_pane;
		}
		break;
		
		case TracerCanvas.YZ_PLANE:
		{
			new_x = zy.getCurrentSlice( ) - 1;
			new_y = y_in_pane;
			new_z = x_in_pane;
		}
		break;
		
		}

		return new PointInImage( new_x, new_y, new_z );
	}

	boolean setupLog = false;
	boolean setupEv = false;
	boolean setupTrace = false;
	boolean setupPreprocess = false;
	boolean setupTimeout = false;
	float   setupTimeoutValue = 0.0f;
	
	public FileInfo file_info;
	
	private int width, height, depth;
	
	private Connection currentPath;
	
	int last_x, last_y, last_z;
	
	String logFilename;
	
	public void logPosition( int x, int y, int z, double ev1, double ev2, double ev3 ) {
		
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(logFilename, true));
			out.write(xy.getShortTitle()+"\t"+x+"\t"+y+"\t"+z+"\t"+ev1+"\t"+ev2+"\t"+ev2+"\n");
			out.close();
		} catch (IOException e) {
			IJ.error( "Writing to log file '" + logFilename + "' failed" );
		}
		
	}
	
	public void setArrow( int i, Arrow a ) {
		
		xy_tracer_canvas.setArrow( i, a );
		zy_tracer_canvas.setArrow( i, a );
		xz_tracer_canvas.setArrow( i, a );
		
	}
	
	public void setCrosshair( int new_x, int new_y, int new_z ) {

		xy_tracer_canvas.setDestinationCrosshair( new_x, new_y, new_z );
		xz_tracer_canvas.setDestinationCrosshair( new_x, new_y, new_z );
		zy_tracer_canvas.setDestinationCrosshair( new_x, new_y, new_z );
		
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
		
		byte [] fileAsBytes = tracesAsBytes( allPaths );

		ArrayList< String [] > tsv_results = archiveClient.synchronousRequest( parameters, fileAsBytes );

		String [] first_line = (String [])tsv_results.get(0);
		if( first_line[0].equals("success") ) {
			IJ.error("Annotations uploaded successfully!");
		} else if( first_line[0].equals("error") ) {
			IJ.error("There was an error while uploading the annotation file: "+first_line[1]);
		} else {
			IJ.error("There was an unknown response to the annotation file upload request: " + first_line[0]);
		}
		
	}

	public void loadTracings( ) {
			
		if( file_info == null ) {
			IJ.error("No original file info for this image, so can't load corresponding traces file.");
			return;
		}

		String fileName = file_info.fileName;
		String directory = file_info.directory;

		File possibleLoadFile = new File(directory,fileName+".traces");
	
		String path = possibleLoadFile.getPath();

		if(possibleLoadFile.exists()) {
						
			ArrayList< SegmentedConnection > all_paths = loadTracingsFromFile(path);
			if( all_paths != null )
				setAllPaths( all_paths );

		} else {
			IJ.error("The corresponding traces file (" + path + ") doesn't exist.");
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
	
	public void mouseMovedTo( int x_in_pane, int y_in_pane, int in_plane ) {

		PointInImage p = findPointInStack( x_in_pane, y_in_pane, in_plane );

		setSlicesAllPanes( p.x, p.y, p.z );
		
		if( (xy_canvas != null) &&
		    (xz_canvas != null) &&
		    (zy_canvas != null) ) {
			
			
			if( currentPath != null ) {
				
				xy_tracer_canvas.setConnection( currentPath );
				xz_tracer_canvas.setConnection( currentPath );
				zy_tracer_canvas.setConnection( currentPath );
				
			}
		
			setCrosshair( p.x, p.y, p.z );

			repaintAllPanes( );
		}

		if( transformation != null ) {

			double [] transformedPoint = new double[3];
			
			transformation.apply(p.x,p.y,p.z,transformedPoint);
			
			int target_x = (int)transformedPoint[0];
			int target_y = (int)transformedPoint[1];
			int target_z = (int)transformedPoint[2];
		
			String message;

			if( target_x >= 0 && target_x < label_stack_width &&
			    target_y >= 0 && target_x < label_stack_height &&
			    target_z >= 0 && target_x < label_stack_depth ) {

				int material_index = label_data[target_z][label_stack_width*target_y+target_x]&0xFF;
				
				String material_name = materialNames[material_index];

				message = "point maps to: " + material_name;

			} else {

				message = "point maps to out-of-range position";


			}

			IJ.showStatus( message );

		}

		last_x = p.x;
		last_y = p.y;
		last_z = p.z;
		
	}
	
	boolean lastStartPointSet = false;

	int last_start_point_x;
	int last_start_point_y;
	int last_start_point_z;

	ArrayList< SegmentedConnection > allPaths;
	Connection temporaryPath = null;
	boolean pathUnfinished = false;

	public void setPathUnfinished( boolean unfinished ) {

		this.pathUnfinished = unfinished;
		xy_tracer_canvas.setPathUnfinished( unfinished );
		zy_tracer_canvas.setPathUnfinished( unfinished );
		xz_tracer_canvas.setPathUnfinished( unfinished );
	}

	public void setTemporaryPath( Connection path ) {

		xy_tracer_canvas.setTemporaryPath( path );
		zy_tracer_canvas.setTemporaryPath( path );
		xz_tracer_canvas.setTemporaryPath( path );

		temporaryPath = path;
	}

	public void setAllPaths( ArrayList< SegmentedConnection > allPaths ) {

		System.out.println("Setting completed in each canvas to: " +allPaths );

		xy_tracer_canvas.setCompleted( allPaths );
		zy_tracer_canvas.setCompleted( allPaths );
		xz_tracer_canvas.setCompleted( allPaths );

		this.allPaths = allPaths;
	}

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

	synchronized public void testPathTo( int x_in_pane, int y_in_pane, int plane ) {

		if( ! lastStartPointSet ) {
			IJ.showStatus( "No initial start point has been set.  Do that with a mouse click." +
				      " (Or a shift-click if the start of the path should join another neurite." );
			return;
		}

		if( temporaryPath != null ) {
			IJ.showStatus( "There's already a temporary path; use 'N' to cancel it or 'Y' to keep it." );
			return;
		}

		PointInImage p = findPointInStack( x_in_pane, y_in_pane, plane );
		
		/* Now moved to AStarThread

		Connection tmpPath = bestAStarBetween( last_start_point_x,
						       last_start_point_y,
						       last_start_point_z,
						       p.x,
						       p.y,
						       p.z,
						       false,
						       setupTimeoutValue );
		
		*/

		setTemporaryPath( tmpPath );

		repaintAllPanes();

	}

	synchronized public void confirmPath( ) {

		int pathsSoFar = allPaths.size();
		System.out.println("confirming path; have "+pathsSoFar+" already");
		SegmentedConnection currentPath = (SegmentedConnection)allPaths.get(pathsSoFar-1);
		currentPath.addConnection( temporaryPath );
		PointInImage last = temporaryPath.lastPoint();
		last_start_point_x = last.x;
		last_start_point_y = last.y;
		last_start_point_z = last.z;
		setTemporaryPath( null );

		System.out.println("confirming path; have "+allPaths.size()+" afterwards");
		
		repaintAllPanes();
	}

	synchronized public void cancelPath( ) {
		
		if( ! lastStartPointSet ) {
			IJ.error( "No initial start point has been set yet.  Do that with a mouse click." +
				  " (Or a shift-click if the start of the path should join another neurite." );
			return;
		}

		if( temporaryPath == null ) {
			IJ.error( "There's not temporary path to cancel!" );
			return;
		}

		// Remove that last temporary path...
		setTemporaryPath( null );

		repaintAllPanes( );
	}

	synchronized public void finishedPath( ) {

		// Is there an unconfirmed path?  If so, warn people about it...
		
		if( temporaryPath != null ) {
			IJ.error( "There's an unconfirmed path, need to confirm or cancel it before finishing the path." );
			return;
		}

		lastStartPointSet = false;
		setPathUnfinished( false );

		repaintAllPanes( );
	}

	synchronized public void startPath( int x_in_pane, int y_in_pane, int plane, boolean join ) {

		if( lastStartPointSet ) {
			IJ.showStatus( "The start point has already been set; to finish a path press 'F'" );
			return;
		}

		if( allPaths == null ) {
			setAllPaths( new ArrayList< SegmentedConnection >( ) );
		}

		PointInImage p = findPointInStack( x_in_pane, y_in_pane, plane );

		setPathUnfinished( true );
		lastStartPointSet = true;
		last_start_point_x = p.x;
		last_start_point_y = p.y;
		last_start_point_z = p.z;
		SegmentedConnection s = new SegmentedConnection();
		s.startsAtJoin( join );
		allPaths.add( s );

	}	


	// -----------------------------------------------------------------

/*
	boolean startPointSet = false;
	
	int start_x;
	int start_y;
	int start_z;

	public void addPoint( int x_in_pane, int y_in_pane, int plane ) {
				
		int new_x, new_y, new_z;
		
		new_x = -1;
		new_y = -1;
		new_z = -1;        
		
		int currentSlice = xy.getCurrentSlice( ) - 1;
		
		switch( plane ) {
			
		case TracerCanvas.XY_PLANE:
		{
			new_x = x_in_pane;
			new_y = y_in_pane;
			new_z = currentSlice;
		}
		break;
		
		case TracerCanvas.XZ_PLANE:
		{
			new_x = x_in_pane;
			new_y = last_y;
			new_z = currentSlice;
		}
		break;
		
		case TracerCanvas.YZ_PLANE:
		{
			new_x = last_x;
			new_y = y_in_pane;
			new_z = x_in_pane;
		}
		break;
		
		}
		
		if( startPointSet ) {
			currentPath = bestAStarBetween( start_x, start_y, start_z,
							new_x,
							new_y,
							new_z,
							false,
							setupTimeoutValue );
		} else {
			start_x = new_x;
			start_y = new_y;
			start_z = new_z;
			startPointSet = true;
		}
		

//        if( currentPath == null ) {
//            currentPath = new Connection( );
//        } else {
//            currentPath.addPoint( new_x, new_y, new_z );
//        }

		
		xy_canvas.repaint();
		zy_canvas.repaint();
		xz_canvas.repaint();        
		
	}
*/	
	double reciprocal_fudge = 0.5;
	
	float estimateCostToGoal( int current_x, int current_y, int current_z,
				  int goal_x, int goal_y, int goal_z,
				  double minimum_cost_per_unit_distance ) {
		
		double xdiff = (goal_x - current_x) * x_spacing;
		double ydiff = (goal_y - current_y) * y_spacing;
		double zdiff = (goal_z - current_z) * z_spacing;
		
		double distance = Math.sqrt( xdiff * xdiff + ydiff * ydiff + zdiff * zdiff );
		
		return (float) ( minimum_cost_per_unit_distance * distance );
		
	}
	
	String getStackTrace( ) {
		StringWriter sw = new StringWriter();
		new Exception("Dummy Exception for Stack Trace").printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}
	
	boolean assertions = true;
	
	void add_node( PriorityQueue queue, Hashtable hash, AStarNode a ) {
		
		// We should never add a node if it's already in one of these:
		if( assertions ) {
			
			if( null != found_in( queue, hash, a ) ) {
				System.out.println( "E: trying to add a node ("+a+") that's already in queue.");
				System.out.println( getStackTrace() );
			}
			
			float f_to_remove = a.f;
			
			int x_to_remove = a.x;
			int y_to_remove = a.y;
			int z_to_remove = a.z;
			
			Iterator<AStarNode> oi = queue.iterator();
			while( oi.hasNext() ) {
				AStarNode i = oi.next();
				if( (x_to_remove == i.x) &&
				    (y_to_remove == i.y) &&
				    (z_to_remove == i.z) ) {
					System.out.println("E: trying to add a node ("+a+") that's already in queue by position ("+i+")");
					System.out.println("E: (when number in queue was: "+queue.size()+")");
				}
                /*
                if( f_to_remove == i.f ) {
                    System.out.println("E: trying to add a node ("+a+") that's already in the queue by priority ("+i+")");
                    System.out.println("E: (when number in queue was: "+queue.size()+")");
                }
                */
			}
			
		}
		int in_queue_before_adding = queue.size();
		// FIXME: does this have a return value?
		queue.add( a );
		int in_queue_after_adding = queue.size();
		if( (in_queue_after_adding - in_queue_before_adding) != 1 ) {
			System.out.println( "E: while adding ("+a+") to queue:");
			System.out.println( "E: there should be one more element in the queue after adding" );
			System.out.println( "E: in fact, "+in_queue_before_adding+" -> "+in_queue_after_adding );
			System.out.println( getStackTrace() );            
		}
		int in_hash_before_adding = hash.size();
		hash.put( a, a );
		int in_hash_after_adding = hash.size();
		if( (in_hash_after_adding - in_hash_before_adding) != 1 ) {
			System.out.println( "E: while adding ("+a+") to hash:");
			System.out.println( "E: there should be one more element in the hash after adding" );
			System.out.println( "E: in fact, "+in_hash_before_adding+" -> "+in_hash_after_adding );
			System.out.println( getStackTrace() );            
		}
	}
	
	void remove( PriorityQueue queue, Hashtable hash, AStarNode a ) {
		
        /*
        if( assertions ) {
        
            float f_to_remove = a.f;

            int x_to_remove = a.x;
            int y_to_remove = a.y;
            int z_to_remove = a.z;

            boolean in_queue_iteration_right_position = false;

            Iterator<AStarNode> oi = queue.iterator();
            while( oi.hasNext() ) {
                AStarNode i = oi.next();
                if( (x_to_remove == i.x) &&
                    (y_to_remove == i.y) &&
                    (z_to_remove == i.z) ) {

                    if( in_queue_iteration_right_position ) {
                        System.out.println("E: found node at right point more than once.");
                        System.out.println("E: the extra one was: "+i);
                        System.out.println( getStackTrace() );
                    } else {
                        in_queue_iteration_right_position = true;
                        foundInQueue = i;
                    }
                }
                if( f_to_remove == i.f ) {
                    

                }
            }
            
        }
        */

		int in_queue_before = queue.size();
		boolean removed = queue.remove( a ); 
		int in_queue_after = queue.size();
		
		if( (in_queue_before - in_queue_after) != 1 ) {
			System.out.println( "E: size mismatch removing: " + a );
			System.out.println( "E: " + in_queue_before + " -> " +in_queue_after );
			System.out.println( getStackTrace() );
		}
		
		if( ! removed ) {
			System.out.println( "E: didn't succeed in removing the node from the queue" );
			System.out.println( "E: tried to remove: " + a );
		}
		
		remove_from_hash( hash, a );
		
	}
	
	void remove_from_hash( Hashtable hash, AStarNode a ) {
		
		if( assertions ) {
			// Check that that node was actually in there...
			AStarNode in_hash = (AStarNode)hash.get( a );
			if( in_hash == null ) { 
				System.out.println( "E: trying to remove from the hash something that isn't there" );
				System.out.println( "E: node to remove was: "+a );
				System.out.println( getStackTrace() );
			} else {
				if( in_hash != a ) {
					System.out.println( "E: while removing from hash, reference of found object mismatched" );
					System.out.println( "E:   node to remove was: "+a );
					System.out.println( "E:   node in there was: "+a );
					System.out.println( getStackTrace() );
				}
			}            
		}
		
		int in_hash_before_removing = hash.size();
		hash.remove( a );
		int in_hash_after_removing = hash.size();        
		if( (in_hash_before_removing - in_hash_after_removing) != 1 ) {
			System.out.println( "E: while removing ("+a+") from the hash:");
			System.out.println( "E: there should be one fewer element in the hash after removing" );
			System.out.println( "E: in fact, "+in_hash_before_removing+" -> "+in_hash_after_removing );
			System.out.println( getStackTrace() );                        
		}
		
	}
	
	AStarNode get_highest_priority( PriorityQueue queue, Hashtable hash ) {
		
		int before_removing_with_poll = queue.size();
		AStarNode fromQueue = (AStarNode)queue.poll();
		int after_removing_with_poll = queue.size();
		if( (before_removing_with_poll - after_removing_with_poll) != 1 ) {
			System.out.println( "E: while removing with poll ("+fromQueue+") from the queue");
			System.out.println( "E: there should be one fewer element in the queue after removing" );
			System.out.println( "E: in fact, "+before_removing_with_poll+" -> "+after_removing_with_poll );
			System.out.println( getStackTrace() );
		}        
		remove_from_hash( hash, fromQueue );
		
		return fromQueue;
		
	}
	
	AStarNode found_in( PriorityQueue queue, Hashtable hash, AStarNode a ) {
		boolean in_hash;
		boolean in_queue;
		boolean in_queue_iteration;
		AStarNode foundInHash = (AStarNode)hash.get( a );
		AStarNode foundInQueue = null;
		in_hash = (foundInHash != null);
		in_queue = queue.contains( a );
		in_queue_iteration = false;
		Iterator<AStarNode> oi = queue.iterator();
		while( oi.hasNext() ) {
			AStarNode i = oi.next();
			if( (a.x == i.x) && (a.y == i.y) && (a.z == i.z) ) {
				if( in_queue_iteration ) {
					System.out.println("E: found node at right point more than once.");
					System.out.println("E: the extra one was: "+i);
					System.out.println( getStackTrace() );
				} else {
					in_queue_iteration = true;
					foundInQueue = i;
				}
			}
		}
		if( (! in_hash) && (! in_queue) && (! in_queue_iteration) ) {
			// Then that's fine:
			return null;
		} else if( in_hash && in_queue && in_queue_iteration ) {
			// That's also fine.
			if( foundInHash != foundInQueue ) {
				// i.e. we've two, but their references are different:
				System.out.println("E: found the node in both the hash and the queue,");
				System.out.println("E: but their references were not equal");
				System.out.println("E: in hash: "+foundInHash);
				System.out.println("E: in queue: "+foundInQueue);
				System.out.println( getStackTrace() );
			}
			return foundInHash;
		} else {
			System.out.println( "E: something was inconsistent: " );
			System.out.println( "E:    in_hash "+in_hash);
			System.out.println( "E:    in_queue_iteration "+in_queue_iteration);
			System.out.println( "E:    in_queue "+in_queue);
			System.out.println( "E:    foundInHash "+foundInHash);
			System.out.println( "E:    foundInQueue "+foundInQueue);
			System.out.println( "E: (when number in queue was: "+queue.size()+")");
			System.out.println( getStackTrace() );
		}
		
		return foundInHash;
	}
	
	
	void snapshot( PriorityQueue open_from_start, PriorityQueue closed_from_start,
		       PriorityQueue open_from_goal, PriorityQueue closed_from_goal,
		       String title ) {
		
		byte [][] snapshot_data = new byte[depth][];
		
		for( int i = 0; i < depth; ++i )
			snapshot_data[i] = new byte[width*height];
		
		Iterator<AStarNode> ai;        
		
		ai = closed_from_start.iterator();
		while( ai.hasNext() ) {
			AStarNode i = ai.next();
			snapshot_data[i.z][i.y*width+i.x] = (byte)127;
		}
		
		ai = open_from_start.iterator();
		while( ai.hasNext() ) {
			AStarNode i = ai.next();
			snapshot_data[i.z][i.y*width+i.x] = (byte)255;
		}
		
                
		ai = closed_from_goal.iterator();
		while( ai.hasNext() ) {
			AStarNode i = ai.next();
			snapshot_data[i.z][i.y*width+i.x] = (byte)192;
		}
		
		ai = open_from_goal.iterator();
		while( ai.hasNext() ) {
			AStarNode i = ai.next();
			snapshot_data[i.z][i.y*width+i.x] = (byte)64;
		}
		
		ImageStack newStack = new ImageStack( width, height );
		
		for( int i = 0; i < depth; ++i ) {
			ByteProcessor thisSlice = new ByteProcessor( width, height );
			thisSlice.setPixels( snapshot_data[i] );
			newStack.addSlice( null, thisSlice );
		}
		
		ImagePlus ip = new ImagePlus( title, newStack );
		ip.show( );
	}

	/* Calculate the hessian at a point in the image... */
	
	float [][] hessian( int [] values,
			    int either_side,
			    double sx,     // sigma_x
			    double sy,     // sigma_y
			    double sz,     // sigma_z
			    double x_spacing,
			    double y_spacing,
			    double z_spacing ) {
		
		int diameter = either_side * 2 + 1;
		int points = diameter * diameter * diameter;
		
		/* We assume (for the purposes of calculating the second
		   derivatives) that the continuous function at the centre
		   point is actually made up from the sum of Gaussians centred
		   at that and the surrounding points.
		*/
		
		double gxx = 0;
		double gyy = 0;
		double gzz = 0;
		
		double gxy = 0;
		double gxz = 0;
		double gyz = 0;
		
		if( points == values.length ) {

/*            
            for( int i = 0; i < points; ++i ) {
                
                int x_grid = (i % diameter) - either_side;
                int y_grid = ((i % (diameter*diameter)) / diameter) - either_side;
                int z_grid = (i / (diameter*diameter)) - either_side;

                int A = values[i];

                double x = - x_grid * x_spacing;
                double y = - y_grid * y_spacing;
                double z = - z_grid * z_spacing;

                double sx2 = sx*sx;
                double sy2 = sy*sy;
                double sz2 = sz*sz;

            }
*/

			for( int i = 0; i < points; ++i ) {
				
				int x_grid = (i % diameter) - either_side;
				int y_grid = ((i % (diameter*diameter)) / diameter) - either_side;
				int z_grid = (i / (diameter*diameter)) - either_side;
				
				int A = values[i];
				
				double x = - x_grid * x_spacing;
				double y = - y_grid * y_spacing;
				double z = - z_grid * z_spacing;
				
				double sx2 = sx*sx;
				double sy2 = sy*sy;
				double sz2 = sz*sz;
				
				double squares_product = sx2*sy2*sz2;
				
				double in_exp = (- sx2*sy2*z*z - sx2*sz2*y*y - sy2*sz2*x*x) / squares_product;
				
				gxx +=
					(4*A*x*x - 2*A*sx2) * Math.exp( in_exp ) / ( sx2*sx2 );
				
				gxy +=
					4*A*x*y * Math.exp( in_exp ) / ( sx2*sy2 );
				
				gxz +=
					4*A*x*z * Math.exp( in_exp ) / ( sx2*sz2 );
				
				gyy +=
					(4*A*y*y - 2*A*sy2) * Math.exp( in_exp ) / ( sy2*sy2 );
				
				gyz +=
					4*A*y*z * Math.exp( in_exp ) / ( sy2*sz2 );
				
				gzz +=
					(4*A*z*z - 2*A*sz2) * Math.exp( in_exp ) / ( sz2*sz2 );
				
			}
			
			float [][] hessian = new float[3][3];
			
			hessian[0][0] = (float)gxx;
			hessian[0][1] = (float)gxy;
			hessian[0][2] = (float)gxz;
			
			hessian[1][0] = (float)gxy;
			hessian[1][1] = (float)gyy;
			hessian[1][2] = (float)gyz;
			
			hessian[2][0] = (float)gxz;
			hessian[2][1] = (float)gyz;
			hessian[2][2] = (float)gzz;
			
			return hessian;
			
		} else
			return null;
		
	}
	
	public EigenResultsDouble analyzeAtPoint( int x, int y, int z, int either_side, boolean interactive ) {
		
		int i;
		
		if( x < either_side || x >= (width - either_side) )
			return null;
		if( y < either_side || y >= (height - either_side) )
			return null;
		if( z < either_side || z >= (depth - either_side) )
			return null;
		
		int diameter = either_side * 2 + 1;
		int points = diameter * diameter * diameter;
		
		int [] values_around_point = new int[points];
		
		byte [][] relevant_pixels = new byte[diameter][];
		
		for( i = 0; i < diameter; ++i ) {        
			relevant_pixels[i] = slices_data[ z + (i - either_side) ];
			// relevant_pixels[i] = (byte []) original_stack.getPixels( z + (i-1) );
		}
		
		for( i = 0; i < points; ++i ) {
			
			int x_grid = (i % diameter) - either_side;
			int y_grid = ((i % (diameter * diameter)) / diameter) - either_side;
			int z_grid = (i / (diameter * diameter)) - either_side;
			
			byte [] pixels;
			
			values_around_point[i] = (int)( relevant_pixels[z_grid + either_side][ (y + y_grid) * width + (x + x_grid) ] & 0xFF );
			
		}
		
		float [][] hessian = hessian( values_around_point,
					      either_side,
					      x_spacing / 2,
					      y_spacing / 2,
					      z_spacing / 2,
					      x_spacing,
					      y_spacing,
					      z_spacing );
		
		JacobiFloat jc = new JacobiFloat( hessian );
		
		float[] eigenValuesFloat=jc.getEigenValues();
		float[][] eigenVectorMatrixFloat=jc.getEigenVectors();
		
		// Remarkably, I think that the bit down to // --- is quicker
		// than just sorting them.
		
		int smallestEValueIndex = 0;
		for( i = 1; i < 3; ++i ) {
			if( Math.abs(eigenValuesFloat[i]) < Math.abs(eigenValuesFloat[smallestEValueIndex]) ) {
				smallestEValueIndex = i;
			}
		}
		
		int largestEValueIndex = 2;
		for( i = 0; i < 2; ++i ) {
			if( Math.abs(eigenValuesFloat[i]) > Math.abs(eigenValuesFloat[largestEValueIndex]) ) {
				largestEValueIndex = i;
			}
		}
		int middleEValueIndex = 1;
		for( i = 0; i < 3; ++i ) {
			if( (i != smallestEValueIndex) && (i != largestEValueIndex) ) {
				middleEValueIndex = i;
			}
		}
		
		if( interactive ) {
			
			for( i = 0; i < 3; ++i )
				System.out.println( "evalue [" + i + "]: " + eigenValuesFloat[i] );
			
			System.out.println( "small " + smallestEValueIndex +
					    ", middle " + middleEValueIndex +
					    ", large " + largestEValueIndex );
			
		}
		
		// -------------------------------------------------------------
		
		double [] valuesSorted=new double[3];
		
		valuesSorted[0] = eigenValuesFloat[smallestEValueIndex];
		valuesSorted[1] = eigenValuesFloat[middleEValueIndex];
		valuesSorted[2] = eigenValuesFloat[largestEValueIndex];
		
		double[][] vectorsSorted=new double[3][3];
		
		vectorsSorted[0][0] = eigenVectorMatrixFloat[0][smallestEValueIndex];
		vectorsSorted[0][1] = eigenVectorMatrixFloat[1][smallestEValueIndex];
		vectorsSorted[0][2] = eigenVectorMatrixFloat[2][smallestEValueIndex];
		
		vectorsSorted[1][0] = eigenVectorMatrixFloat[0][middleEValueIndex];
		vectorsSorted[1][1] = eigenVectorMatrixFloat[1][middleEValueIndex];
		vectorsSorted[1][2] = eigenVectorMatrixFloat[2][middleEValueIndex];
		
		vectorsSorted[2][0] = eigenVectorMatrixFloat[0][largestEValueIndex];
		vectorsSorted[2][1] = eigenVectorMatrixFloat[1][largestEValueIndex];
		vectorsSorted[2][2] = eigenVectorMatrixFloat[2][largestEValueIndex];
		
		// -------------------------------------------------------------
		
		if( interactive ) {
			
			System.out.println( "smallest evalue: " + valuesSorted[0] );
			
			System.out.println( "   v_x: " + vectorsSorted[0][0] );
			System.out.println( "   v_y: " + vectorsSorted[0][1] );
			System.out.println( "   v_z: " + vectorsSorted[0][2] );
			
			System.out.println( "middle evalue: " + valuesSorted[1] );
			
			System.out.println( "   v_x: " + vectorsSorted[1][0] );
			System.out.println( "   v_y: " + vectorsSorted[1][1] );
			System.out.println( "   v_z: " + vectorsSorted[1][2] );
			
			System.out.println( "largest evalue: " + valuesSorted[2] );
			
			System.out.println( "   v_x: " + vectorsSorted[2][0] );
			System.out.println( "   v_y: " + vectorsSorted[2][1] );
			System.out.println( "   v_z: " + vectorsSorted[2][2] );
			
			Arrow a = new Arrow( Color.RED, x, y, z,
					     vectorsSorted[0][0],
					     vectorsSorted[0][1],
					     vectorsSorted[0][2],
					     100  );
			
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
		
		EigenResultsDouble er = new EigenResultsDouble();
		er.sortedValues = valuesSorted;
		er.sortedVectors = vectorsSorted;
		
		return er;
		
		/*

        float[][] second_derivatives =
            new float[3][3];
    
        double delta_x;
        double delta_y;
        double delta_z;

        double delta_x_plus_e_x;
        double delta_y_plus_e_x;
        double delta_z_plus_e_x;

        double delta_x_plus_e_y;
        double delta_y_plus_e_y;
        double delta_z_plus_e_y;

        double delta_x_plus_e_z;
        double delta_y_plus_e_z;
        double delta_z_plus_e_z;

        if( x == width - 1 )
            x = width - 2;

        if( y == height - 1 )
            y = height - 2;

        if( z == depth - 1 )
            z = depth - 2;

        delta_x = div[ z ][ y * (width * 3) + x ] / x_spacing;

        delta_x_plus_e_x = div[ z ][ y * (width * 3) + (x + 3) ] / x_spacing;
        delta_x_plus_e_y = div[ z ][ (y + 1) * (width * 3) + x ] / y_spacing;
        delta_x_plus_e_z = div[ z + 1 ][ y * (width * 3) + x ] / z_spacing;

        delta_y = div[ z ][ y * (width * 3) + x + 1 ] / y_spacing;
        delta_y_plus_e_x = div[ z ][ y * (width * 3) + (x + 3) + 1 ] / y_spacing;
        delta_y_plus_e_y = div[ z ][ (y + 1) * (width * 3) + x + 1 ] / y_spacing;
        delta_y_plus_e_z = div[ z + 1 ][ y * (width * 3) + x + 1 ] / y_spacing;

        delta_z = div[ z - 1 ][ y * (width * 3) + x + 2 ] / z_spacing;
        delta_z_plus_e_x = div[ z ][ y * (width * 3) + (x + 3) + 2 ] / z_spacing;
        delta_z_plus_e_y = div[ z ][ (y + 1) * (width * 3) + x + 2 ] / z_spacing;
        delta_z_plus_e_z = div[ z + 1 ][ y * (width * 3) + x + 2 ] / z_spacing;

        double change_in_delta_x_wrt_x = (delta_x_plus_e_x - delta_x) / x_spacing;
        double change_in_delta_y_wrt_x = (delta_y_plus_e_x - delta_y) / x_spacing;
        double change_in_delta_z_wrt_x = (delta_z_plus_e_x - delta_z) / x_spacing;
        
        double change_in_delta_x_wrt_y = (delta_x_plus_e_y - delta_x) / y_spacing;
        double change_in_delta_y_wrt_y = (delta_y_plus_e_y - delta_y) / y_spacing;
        double change_in_delta_z_wrt_y = (delta_z_plus_e_y - delta_z) / y_spacing;
        
        double change_in_delta_x_wrt_z = (delta_x_plus_e_z - delta_x) / z_spacing;
        double change_in_delta_y_wrt_z = (delta_y_plus_e_z - delta_y) / z_spacing;
        double change_in_delta_z_wrt_z = (delta_z_plus_e_z - delta_z) / z_spacing;

        System.out.println( "dxx: " + change_in_delta_x_wrt_x );
        System.out.println( "dyx: " + change_in_delta_y_wrt_x );
        System.out.println( "dzx: " + change_in_delta_z_wrt_x );

        System.out.println( "dxy: " + change_in_delta_x_wrt_y );
        System.out.println( "dyy: " + change_in_delta_y_wrt_y );
        System.out.println( "dzy: " + change_in_delta_z_wrt_y );

        System.out.println( "dxz: " + change_in_delta_x_wrt_z );
        System.out.println( "dyz: " + change_in_delta_y_wrt_z );
        System.out.println( "dzz: " + change_in_delta_z_wrt_z );

        second_derivatives[0][0] = (float)change_in_delta_x_wrt_x;
        second_derivatives[0][1] = (float)change_in_delta_y_wrt_x;
        second_derivatives[0][2] = (float)change_in_delta_z_wrt_x;

        second_derivatives[1][0] = (float)change_in_delta_x_wrt_y;
        second_derivatives[1][1] = (float)change_in_delta_y_wrt_y;
        second_derivatives[1][2] = (float)change_in_delta_z_wrt_y;

        second_derivatives[2][0] = (float)change_in_delta_x_wrt_z;
        second_derivatives[2][1] = (float)change_in_delta_y_wrt_z;
        second_derivatives[2][2] = (float)change_in_delta_z_wrt_z;

        */
	}
	
	short div[][];    
	
	double x_spacing;
	double y_spacing;
	double z_spacing;
	
	public void setPositionAllPanes( int x, int y, int z ) {
		
		xy.setSlice( z + 1 );
		zy.setSlice( x );
		xz.setSlice( y );
		
	}
	
	byte [][] slices_data;
	
	NeuriteTracerSetupDialog dialog;
	NeuriteTracerResultsDialog resultsDialog;

	boolean cancelled = false;
	
	TextWindow helpTextWindow;
	
	static public void toastKeyListeners( Component c, String nameForDebugging ) {
		KeyListener [] listeners = c.getKeyListeners();
		for( int i = 0; i < listeners.length; ++i ) {
			KeyListener l = listeners[i];
			System.out.println( "Removing KeyListener (" + (Object)l + ") from " +
					    nameForDebugging + "(" + (Object)c + ")" );
			c.removeKeyListener( l );
		}
	}
	
	ArchiveClient archiveClient;

	public void run( String ignoredArguments ) {
		
		System.err.println("Macro options are: "+Macro.getOptions());

		// System.err.println("client running with arguments: "+arguments);

		Applet applet = IJ.getApplet();
		if( applet != null ) {
			archiveClient = new ArchiveClient( applet, Macro.getOptions() );
		}

		if( archiveClient != null ) {
			
			// We close any channel that's marked nc82

			Hashtable<String,String> parameters = new Hashtable<String,String>();
			parameters.put("method","channel-tags");
			parameters.put("md5sum",archiveClient.getValue("md5sum"));
			
			ArrayList< String [] > tsv_results = archiveClient.synchronousRequest(parameters,null);
			int tags = Integer.parseInt(((String [])tsv_results.get(0))[1]); // FIXME error checking
			int nc82_channel = -1;
			for( int i = 0; i < tags; ++i ) {
				String [] row = (String [])tsv_results.get(i);
				if( "nc82".equals(row[1]) ) {
					nc82_channel = Integer.parseInt(row[0]);
					break;
				}
			}
			if( nc82_channel >= 0 ) {
				
				String lookFor = "Ch"+(nc82_channel+1);

				int[] wList = WindowManager.getIDList();
				if (wList==null) {
					IJ.error("Name_Points: no images have been loaded");
					return;
				}
				
				for (int i=0; i<wList.length; i++) {
					ImagePlus imp = WindowManager.getImage(wList[i]);
					String title = imp!=null?imp.getTitle():"";
					int indexOfChannel = title.indexOf(lookFor);
					if( indexOfChannel >= 0 ) {
						imp.close();
						break;
					}
				}
				
			}
			
		}

		try {
			
        /*

        float [][] transform = new float[3][3];
        transform[0][0] = 1; transform[0][1] = 2; transform[0][2] = 0;
        transform[1][0] = 2; transform[1][1] = 1; transform[1][2] = 0;
        transform[2][0] = 0; transform[2][1] = 0; transform[2][2] = 1;

        JacobiFloat jc = new JacobiFloat(transform,200);

        float [] evals = jc.getEigenValues();
        float [][] evecs = jc.getEigenVectors();

        System.out.println( "eval0: " + evals[0] );
        System.out.println( "eval1: " + evals[1] );
        System.out.println( "eval2: " + evals[2] );

        System.out.println( "evec0: " + evecs[0][0] + ", " + evecs[0][1] + ", " + evecs[0][2] );
        System.out.println( "evec1: " + evecs[1][0] + ", " + evecs[1][1] + ", " + evecs[1][2] );
        System.out.println( "evec2: " + evecs[2][0] + ", " + evecs[2][1] + ", " + evecs[2][2] );

        // The other way round (below) seems to be correct:

        System.out.println( "other evec0: " + evecs[0][0] + ", " + evecs[1][0] + ", " + evecs[2][0] );
        System.out.println( "other evec1: " + evecs[0][1] + ", " + evecs[1][1] + ", " + evecs[2][1] );
        System.out.println( "other evec2: " + evecs[0][2] + ", " + evecs[1][2] + ", " + evecs[2][2] );

        */

        /*

        int side = 16;
        int mid = side / 2;

        ImageStack stack = new ImageStack( side, side );

        for( int i = 0; i < side; ++i ) {
            byte [] b = new byte[side*side];
            if( i == mid )
                for( int j = 0; j < side; ++j )
                    b[ mid * side + j ] = (byte)255;
            ByteProcessor bp = new ByteProcessor(side,side);
            bp.setPixels( b );
            stack.addSlice( null, bp );
        }

        ImagePlus newImage = new ImagePlus( "Test X Axis", stack );
        newImage.show();

        ImageStack stack2 = new ImageStack( side, side );

        for( int i = 0; i < side; ++i ) {
            byte [] b = new byte[side*side];
            if( i == mid )
                for( int j = 0; j < side; ++j )
                    b[ j * side + mid ] = (byte)255;
            ByteProcessor bp = new ByteProcessor(side,side);
            bp.setPixels( b );
            stack2.addSlice( null, bp );
        }

        ImagePlus newImage2 = new ImagePlus( "Test Y Axis", stack2 );
        newImage2.show();

        ImageStack stack3 = new ImageStack( side, side );

        for( int i = 0; i < side; ++i ) {
            byte [] b = new byte[side*side];
            b[ mid * side + mid ] = (byte)255;
            ByteProcessor bp = new ByteProcessor(side,side);
            bp.setPixels( b );
            stack3.addSlice( null, bp );
        }

        ImagePlus newImage3 = new ImagePlus( "Test Z Axis", stack3 );
        newImage3.show();

        if( true )
            return;
	    
        */

			toastKeyListeners( IJ.getInstance(), "IJ.getInstance()" );			

			ImagePlus currentImage = WindowManager.getCurrentImage();

			if( currentImage == null ) {
				IJ.error( "There's no current image to crop." );
				return;
			}

			if( (currentImage.getWidth() < 2) ||
			    (currentImage.getHeight() < 2) ||
			    (currentImage.getStackSize() < 2) ) {
				IJ.error( "There must be at least two sample points in each dimension" );
				return;
			}
			
			initialize(currentImage);

			xy_tracer_canvas = (TracerCanvas)xy_canvas;
			xz_tracer_canvas = (TracerCanvas)xz_canvas;
			zy_tracer_canvas = (TracerCanvas)zy_canvas;

			toastKeyListeners( IJ.getInstance(), "IJ.getInstance()" );
			if( applet == null ) {

				file_info = xy.getOriginalFileInfo();
				System.out.println( "file_info is: " + file_info );

				if( file_info != null ) {
					
					// Is there a points file?
					
					File pointsFile = new File( file_info.directory, file_info.fileName + ".points" );
					System.out.println( "Looking for pointsFile: " + pointsFile );
					if( pointsFile.exists() ) {
						
						String realPath = file_info.directory+file_info.fileName;
						System.out.println( "real path is: " +realPath );
						
						String standardBrainFileName="/media/WD USB 2/standard-brain/data/vib-drosophila/CantonM43c.grey";
						String standardBrainLabelsFileName="/media/WD USB 2/standard-brain/data/vib-drosophila/CantonM43c.labels";
						
						FileAndChannel standardBrainFC=new FileAndChannel(standardBrainFileName,0);
						
						if( standardBrainFC.exists() ) {

							FileAndChannel actualImageFC=new FileAndChannel(realPath,0);
							
							Bookstein_FromMarkers matcher=new Bookstein_FromMarkers();
							matcher.loadImages(standardBrainFC,actualImageFC);
							transformation=matcher.register();
							
							ImagePlus labels;
							{
								ImagePlus[] tmp=BatchOpener.openFromFile(standardBrainLabelsFileName);
								labels=tmp[0];
							}
							
							AmiraParameters parameters = new AmiraParameters(labels);
						
							materials = parameters.getMaterialCount();
							
							ImageStack labelStack=labels.getStack();
							
							label_stack_width   = labelStack.getWidth();
							label_stack_height  = labelStack.getHeight();
							label_stack_depth   = labelStack.getSize();
							
							materialNames = new String[256];
							for( int i = 0; i < materials; ++i ) {
								materialNames[i] = parameters.getMaterialName(i);
							}
							
							label_data=new byte[label_stack_depth][];
							for( int z = 0; z < label_stack_depth; ++z )
								label_data[z] = (byte[])labelStack.getPixels( z + 1 );
						
						}
					}
					
					
				}
			}

			dialog = new NeuriteTracerSetupDialog( "Tracing Options", this );
			
			if( cancelled )
				return;
			
			if( setupTrace ) {
				
				String title = "Help For Neurite Tracer\n";
				String message =
					"Start a path with a mouse click.  Then move along the neurite\n"+
					"some way and press 'T' to calculate a temporary path to that\n"+
					"point.  (The temporary path will appear in blue after a\n"+
					"couple of seconds.)  If that path looks right, press 'Y'\n"+
					"to confirm it and move along the neurite to create the\n"+
					"next temporary path segment.  If the path looks wrong,\n"+
					"cancel it with 'N' and try pressing 'T' closer to the\n"+
					"start point.  To finish a path, press 'F'.  (You can\n"+
					"then start another path by clicking with the mouse again.)\n"+
					"To produce an image stack from the paths (e.g. for viewing\n"+
					"with MipJ) press 'V'.";
				
				helpTextWindow = new TextWindow( title, message, 450, 300 );

				resultsDialog = new NeuriteTracerResultsDialog( "Tracing for: " + xy.getShortTitle(),
										this,
										applet != null );
			}
			
			width = xy.getWidth();
			height = xy.getHeight();
			depth = xy.getStackSize();
			
			Calibration calibration = xy.getCalibration();
			
			x_spacing = calibration.pixelWidth;
			y_spacing = calibration.pixelHeight;
			z_spacing = calibration.pixelDepth;
			
			System.out.println( "calibration was: " + x_spacing + ", " + y_spacing + ", " + z_spacing );
			
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

			toastKeyListeners( xy_tracer_canvas, "xy_tracer_canvas" );
			toastKeyListeners( xy_window, "xy_window" );
			
			xy_tracer_canvas.addKeyListener( xy_tracer_canvas );
			xy_window.addKeyListener( xy_tracer_canvas );

			toastKeyListeners( xz_tracer_canvas, "xz_tracer_canvas" );
			toastKeyListeners( xz_window, "xz_window" );

			xz_tracer_canvas.addKeyListener( xz_tracer_canvas );
			xz_window.addKeyListener( xz_tracer_canvas );

			toastKeyListeners( zy_tracer_canvas, "zy_tracer_canvas" );
			toastKeyListeners( zy_window, "zy_window" );

			zy_tracer_canvas.addKeyListener( zy_tracer_canvas );
			zy_window.addKeyListener( zy_tracer_canvas );

			if( true )
				return;
			
			// ------------------------------------------------------------------------
			
			IJ.showStatus( "Now going through the image..." );
			
			IJ.showProgress( 0.0 );
			
			int either_side = 2;
			
			ImageStack eigenStack = new ImageStack( width - 2 * either_side, height - 2 * either_side );
			
			for( int z = 0; z < depth; ++z ) {
				
				byte [] small = new byte[ (width - 2 * either_side) * (height - 2 * either_side) ];
				byte [] ratio = new byte[ (width - 2 * either_side) * (height - 2 * either_side) ];
				byte [] simil = new byte[ (width - 2 * either_side) * (height - 2 * either_side) ];
				
				ColorProcessor cp = new ColorProcessor( width - 2 * either_side, height - 2 * either_side );
				
				for( int y = 0; y < height; ++y ) {
					for( int x = 0; x < width; ++x ) {
						
						if( x < either_side || x >= (width - either_side) )
							continue;
						if( y < either_side || y >= (height - either_side) )
							continue;
						if( z < either_side || z >= (depth - either_side) )
							continue;
						
						EigenResultsDouble er = analyzeAtPoint( x, y, z, either_side, false );
						
						int v = (int) ( 255.0 - Math.abs(er.sortedValues[0]) );
						if( v < 0 )
							v = 0;
						if( v > 255 )
							v = 255;
						
						small[ (y - either_side) * (width - 2 * either_side) + (x - either_side) ] = (byte) v;
						
						int r = 0;
						
						if( Math.abs(er.sortedValues[0]) >= 0.00001 ) {
							
							r = (int) ( 100.0 * (Math.abs(er.sortedValues[1]) / Math.abs( er.sortedValues[0])) );
							if( r < 0 )
								r = 0;
							if( r > 255 )
								r = 255;
							
						}
						
						ratio[ (y - either_side) * (width - 2 * either_side) + (x - either_side) ] = (byte) r;
						
						int s = 0;
						
						if( Math.abs(er.sortedValues[2]) >= 0.00001 ) {
							
							double diff = Math.abs( Math.abs(er.sortedValues[1]) - Math.abs(er.sortedValues[2]) );
							
							double proportion = 1 - (diff / Math.abs(er.sortedValues[2]));
							
							s = (int)( proportion * 255 );
							
						}
						
						simil[ (y - either_side) * (width - 2 * either_side) + (x - either_side) ] = (byte) s;
						
					}
				}
				
				cp.setRGB( ratio, small, simil );
				eigenStack.addSlice( null, cp );
				
				System.out.println( "slice: " + z + " (" + depth + ")" );
				
				IJ.showProgress( z / depth );
			}
			
			ImagePlus eigenImagePlus = new ImagePlus( "smallness of smallest eigenvalue", eigenStack );
			eigenImagePlus.show();
			
			IJ.showProgress( 100.0 );
			
/*
        for( int z = 0; z < depth; ++z ) {

            System.gc();

            byte [] changes_minus_1 = null;
            byte [] changes_current = null;
            byte [] changes_plus_1 = null;

            for( int y = 0; y < height; ++y ) {

                for( int x = 0; x < width; ++x ) {

                    double

                    // So at this point, calculate the matrix in
                    // doubles and eigenvalues.




                    // delta x
                    if( x == width - 1 ) {
                        // FIXME: the boundary case; assume it's the
                        // same as the last one...
                        change_in_change_x =
                            (short) ( (int) (pixels_current[ y * width + x ] & 0xFF) -
                                      (int) (pixels_current[ y * width + (x - 1) ] & 0xFF) );
                    } else {
                        change_in_change_x =
                            (short) ( (int) (pixels_current[ y * width + (x + 1) ] & 0xFF) -
                                      (int) (pixels_current[ y * width + x ] & 0xFF) );
                    }

                    // delta y
                    if( y == height - 1 ) {
                        // FIXME: the boundary case; assume it's the
                        // same as the last one...
                        change_in_change_y =
                            (short) ( (int) (pixels_current[ y * width + x ] & 0xFF) -
                                      (int) (pixels_current[ (y - 1) * width + x ] & 0xFF) );
                    } else {
                        change_in_change_y =
                            (short) ( (int) (pixels_current[ (y + 1) * width + x ] & 0xFF) -
                                      (int) (pixels_current[ y * width + x ] & 0xFF) );
                    }

                    // delta z
                    if( z == depth - 1 ) {
                        // FIXME: the boundary case; assume it's the
                        // same as the last one...
                        change_in_change_z =
                            (short) ( (int) (pixels_current[ y * width + x ] & 0xFF) -
                                      (int) (pixels_minus_1[ y * width + x ] & 0xFF ) );
                    } else {
                        change_in_change_z =
                            (short) ( (int) (pixels_plus_1[ y * width + x ] & 0xFF) -
                                      (int) (pixels_current[ y * width + x ] & 0xFF) );
                    }





                    short change_in_value_x;
                    short change_in_value_y;
                    short change_in_value_z;





                    int delta_x_index_in_div_slice_index = y * (width * 3) + x * 3;
                    int delta_y_index_in_div_slice_index = y * (width * 3) + x * 3 + 1;
                    int delta_z_index_in_div_slice_index = y * (width * 3) + x * 3 + 2;

                    div[z][delta_x_index_in_div_slice_index] = change_in_value_x;
                    div[z][delta_y_index_in_div_slice_index] = change_in_value_y;
                    div[z][delta_z_index_in_div_slice_index] = change_in_value_z;

                }

            }

        }
*/
			
			} finally {
			
				IJ.getInstance().addKeyListener( IJ.getInstance() );
			
		}
	}	
}
	

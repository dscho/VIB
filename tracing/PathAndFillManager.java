/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package tracing;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Arrays;

import java.io.*;

import ij.*;

import client.ArchiveClient;

public class PathAndFillManager {
	
	public PathAndFillManager( ) {
		allPaths = new ArrayList< Path >();
		allFills = new ArrayList< FillerThread >();
		listeners = new ArrayList< PathListener >();
		selectedPaths = new boolean[0];
	}

	ArrayList< Path > allPaths;
	ArrayList< FillerThread > allFills;
	
	ArrayList< PathListener > listeners;

	boolean [] selectedPaths;

	public int size() {
		return allPaths.size();
	}

	/* This is used by the interface to have changes in the path
	   manager reported so that they can be reflected in the UI. */
	
	public synchronized void addPathListener( PathListener listener ) {
		listeners.add(listener);
	}
	
	public synchronized Path getPath( int i ) {
		return allPaths.get(i);
	}

	public synchronized void setSelected( int [] selectedIndices ) {
		selectedPaths = new boolean[allPaths.size()];
		for( int i = 0; i < selectedPaths.length; ++i ) {
			selectedPaths[i] = false;
		}
		for( int i = 0; i < selectedIndices.length; ++i ) {
			selectedPaths[selectedIndices[i]] = true;
		}		
	}

	public synchronized boolean isSelected( int pathIndex ) {
		return selectedPaths[pathIndex];
	}

	public boolean anySelected( ) {
		for( int i = 0; i < selectedPaths.length; ++i )
			if( selectedPaths[i] )
				return true;
		return false;
	}

	public synchronized void resetListeners( ) {

		int paths = allPaths.size();

		String [] listEntries = new String[paths];
				
		for( int i = 0; i < paths; ++i ) {
			Path p = allPaths.get(i);			
			if( p == null ) {
				System.out.println("path was null with i "+i+" out of "+paths );
			}
			String name = "Path (" + i + ")";
			if( p.startJoins != null ) {
				name += ", starts on (" + pathToIndex(p.startJoins) + ")";
			}
			if( p.endJoins != null ) {
				name += ", ends on (" + pathToIndex(p.endJoins) + ")";
			}
			listEntries[i] = name;
		}

		for( Iterator i = listeners.iterator(); i.hasNext(); ) {
			((PathListener)(i.next())).setPathList( listEntries );
		}

	}
	
	private int pathToIndex( Path p ) {
		for( int i = 0; i < allPaths.size(); ++i ) {
			Path toCompare = allPaths.get(i);
			if( p == toCompare )
						return i;
		}
		return -1;
	}

	public synchronized void addPath( Path p ) {
		allPaths.add(p);
		boolean [] newSelectedPaths = new boolean[allPaths.size()];
		System.arraycopy(selectedPaths,0,newSelectedPaths,0,selectedPaths.length);
		selectedPaths = newSelectedPaths;
		resetListeners();
	}
	
	public synchronized void deletePath( int index ) {
		deletePath( index, true );
	}

	private synchronized void deletePath( int index, boolean updateInterface ) {
		
		System.out.println("About to remove index: "+index+", leaving: "+allPaths.size()+" items.");
		Path deleted = allPaths.remove(index);
		System.out.println("After, left "+allPaths.size()+" items.");

		// We don't just delete; have to fix up the references
		// in other paths (for start and end joins).

		for( Iterator i = allPaths.iterator(); i.hasNext(); ) {
			Path p = (Path)i.next();
			if( p.startJoins == p ) {
				p.startJoins = null;
				p.startJoinsIndex = -1;
			}
			if( p.endJoins == p ) {	
				p.endJoins = null;
				p.endJoinsIndex = -1;
			}
		}

		int paths = allPaths.size();
		boolean [] newSelectedPaths = new boolean[paths];

		System.arraycopy(selectedPaths, 0, newSelectedPaths, 0, index );
		if( index < (paths - 1) )
			System.arraycopy(selectedPaths, index + 1, newSelectedPaths, index, paths - index - 1 );

		selectedPaths = newSelectedPaths;

		if( updateInterface )
			resetListeners();
	}
	
	public void deletePaths( int [] indices ) {
		
		Arrays.sort( indices );
		
		for( int i = indices.length - 1; i >= 0; --i ) {
			deletePath( indices[i], false );
		}
		
		resetListeners();
	}

	public void addFill( FillerThread fillerThread ) {

		allFills.add(fillerThread);

		resetListeners();
	}

	public void deleteFills( int [] indices ) {

		Arrays.sort( indices );
		
		for( int i = indices.length - 1; i >= 0; --i ) {
			deleteFill( indices[i], false );
		}	

		resetListeners();
	}

	public void deleteFill( int index ) {
		deleteFill( index, true );
	}

	private synchronized void deleteFill( int index, boolean updateInterface ) {
		
		allFills.remove( index );
		
		if( updateInterface )
			resetListeners();
	}

	// FIXME: replace this with something that writes out an XML format.
	
	synchronized public void writeTracesToFile( String filename ) {
		
		try {
			
			BufferedWriter out = new BufferedWriter(new FileWriter(filename,false));
			
			int paths = allPaths.size();
			// System.out.println("Paths to draw: "+paths);
			for( int i = 0; i < paths; ++i ) {
					
				int last_x = -1;
				int last_y = -1;
				int last_z = -1;
					
				Path path = (Path)allPaths.get(i);
				for( int k = 0; k < path.size(); ++k ) {
					int x = path.x_positions[k];
					int y = path.y_positions[k];
					int z = path.z_positions[k];
					if( (last_x == x) && (last_y == y) && (last_z == z) ) {
						// Skip this, it's just the same.
					} else {
						String toWrite = "" + i + "\t" +
							x + "\t" +
							y + "\t" + 
							z + "\t" +
							(path.startJoins != null) + "\n";
						// System.out.println( "Writing line: " + toWrite );
						out.write( toWrite );
						
					}
					last_x = x;
					last_y = y;
					last_z = z;
				}
			}
			
                        out.close();
			
                } catch (IOException e) {
                        IJ.error( "Writing traces to file '" + filename + "' failed" );
                }
		
	}
	
	public boolean load( String filename ) {

		ArrayList< Path > all_paths = loadTracingsFromFile( filename );

		if( all_paths == null ) {
			return false;
		} else {
			this.allPaths = all_paths;
			return true;
		}

	}
	
	static public ArrayList< Path > loadTracingsFromFile( String filename ) {
		
		ArrayList< Path > all_paths = new ArrayList< Path >();
		
		try {
			
			BufferedReader in = new BufferedReader( new FileReader(filename) );
			
			int last_path_index = -1;
			
			String line;
			Path currentPath = new Path();
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
				
				// join = new Boolean(line).booleanValue();
				
				if( (last_path_index >= 0) && (last_path_index != path_index) ) {

					all_paths.add( currentPath );
					
					currentPath = new Path();
					
				}
				
				currentPath.addPoint( x, y, z );
				
				last_path_index = path_index;
				
			}
			
			// Now just add the last path.
			
			all_paths.add( currentPath );
						
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

	public boolean getTracings( boolean mineOnly, ArchiveClient archiveClient ) {

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
			if( urls_found == 0 ) {
				IJ.error( "No anntation files by " + (mineOnly ? archiveClient.getValue("user") : "any user") + " found." );
				return false;
			} else {
				bestUrl = ((String [])tsv_results.get(1))[1];
				// IJ.error( "Got the URL: " + bestUrl );
			}
		} else if( first_line[0].equals("error") ) {
			IJ.error("There was an error while getting the most recent annotation: "+first_line[1]);
			return false;
		} else {
			IJ.error("There was an unknown response to request for an annotation file: " + first_line[0]);
			return false;
		}

		// Now fetch that file:

		// FIXME:

		if( bestUrl == null )
			return false;

		String fileContents =  ArchiveClient.justGetFileAsString( bestUrl );

		// IJ.error( "got fileContents: " +fileContents);

		if( fileContents == null ) {
			return false;
		} else {
			allPaths = loadFromString(fileContents);
			return true;
		}

	}

	static public ArrayList< Path > loadFromString( String fileContents ) {

		ArrayList< Path > all_paths = new ArrayList< Path >();

		StringTokenizer tokenizer = new StringTokenizer( fileContents, "\n" );

		try {

			int last_path_index = -1;
			
			String line;
			Path currentPath = new Path();
			
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

					all_paths.add( currentPath );

					currentPath = new Path();

				}

				currentPath.addPoint( x, y, z );
				
				last_path_index = path_index;

			}

		} catch( Exception e ) {
			
			IJ.error( "Exception while parsing the data" );
			return null;
		}
		
		return all_paths;
	}

	public static byte [] tracesAsBytes( ArrayList< Path > all_paths ) {
		String s = tracesAsString( all_paths );
		try {
			byte [] bytes = s.getBytes("UTF-8");
			return bytes;
		} catch( UnsupportedEncodingException e ) {
			return null;
		}
	}

	public static String tracesAsString( ArrayList< Path > all_paths ) {
		
		StringBuffer sb = new StringBuffer();

		// System.out.println("Have some all_paths paths to draw.");
		int paths = all_paths.size();
		// System.out.println("Paths to draw: "+paths);
		for( int i = 0; i < paths; ++i ) {

			int last_x = -1;
			int last_y = -1;
			int last_z = -1;
			
			Path path = all_paths.get(i);
			
			for( int k = 0; k < path.size(); ++k ) {
				int x = path.x_positions[k];
				int y = path.y_positions[k];
				int z = path.z_positions[k];
				if( (last_x == x) && (last_y == y) && (last_z == z) ) {
					// Skip this, it's just the same.
				} else {
					String toWrite = "" + i + "\t" +
						x + "\t" +
						y + "\t" + 
						z + "\t" +
						(path.startJoins != null) + "\n";
					// System.out.println( "Writing line: " + toWrite );
					sb.append( toWrite );							
				}
				last_x = x;
				last_y = y;
				last_z = z;
			}
		}

		return sb.toString();
                
	}

	public boolean uploadTracings( ArchiveClient archiveClient ) {

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
			return true;
		} else if( first_line[0].equals("error") ) {
			IJ.error("There was an error while uploading the annotation file: "+first_line[1]);
			return false;
		} else {
			IJ.error("There was an unknown response to the annotation file upload request: " + first_line[0]);
			return false;
		}
		
	}

	/* This method will set all the points in array that
	 * correspond to points on one of the paths to 255, leaving
	 * everything else as it is. */

	synchronized void setPathPointsInVolume( byte [][] slices, int width, int height, int depth ) {
		for( Iterator j = allPaths.iterator(); j.hasNext(); ) {
			Path p = (Path)j.next();
			for( int i = 0; i < p.size(); ++i ) {
				slices[p.z_positions[i]][p.y_positions[i] * width + p.x_positions[i]] = 
					(byte)255;
			}
		}
	}

	synchronized PointInImage nearestJoinPointOnSelectedPaths( int x, int y, int z ) {

		PointInImage result = null;

		if( ! anySelected() ) {
			IJ.error( "There are no paths selected, so you can't join (you're holding 'Control')" );
			return null;
		}

		int minimumDistanceSquared = Integer.MAX_VALUE;

		int paths = allPaths.size();

		for( int s = 0; s < paths; ++s ) {
			
			if( ! selectedPaths[s] )
				continue;

			Path p = (Path)allPaths.get(s);

			for( int i = 0; i < p.size(); ++i ) {

				int this_x = p.x_positions[i];
				int this_y = p.y_positions[i];
				int this_z = p.z_positions[i];

				int diff_x = x - this_x;
				int diff_y = y - this_y;
				int diff_z = z - this_z;

				int thisDistanceSquared = diff_x * diff_x + diff_y * diff_y + diff_z * diff_z;

				if( thisDistanceSquared < minimumDistanceSquared ) {
					result = new PointInImage( this_x, this_y, this_z );
					result.onPath = p;
					result.onPathIndex = i;
					minimumDistanceSquared = thisDistanceSquared;
				}
			}
		}

		return result;
	}
      	
}

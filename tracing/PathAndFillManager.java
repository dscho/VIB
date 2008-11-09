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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Arrays;
import java.util.HashSet;
import java.util.TreeSet;

import java.io.*;

import ij.*;

import client.ArchiveClient;
import ij.measure.Calibration;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.SAXException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap;

import java.awt.Color;

import javax.media.j3d.View;
import ij3d.Content;
import ij3d.UniverseListener;

class TracesFileFormatException extends SAXException {
	public TracesFileFormatException(String message) {
		super(message);
	}
}

public class PathAndFillManager extends DefaultHandler implements UniverseListener {

	static final boolean verbose = Simple_Neurite_Tracer.verbose;

	Simple_Neurite_Tracer plugin;
	ImagePlus imagePlus;

	int maxUsedID = -1;

	public PathAndFillManager( ) {
		allPaths = new ArrayList< Path >();
		allFills = new ArrayList< Fill >();
		listeners = new ArrayList< PathAndFillListener >();
		selectedPathsSet = new HashSet();
	}

	public PathAndFillManager( ImagePlus imagePlus ) {
		this();
		this.imagePlus = imagePlus;
		Calibration c = imagePlus.getCalibration();
		this.x_spacing = Math.abs(c.pixelWidth);
		this.y_spacing = Math.abs(c.pixelHeight);
		this.z_spacing = Math.abs(c.pixelDepth);
		this.spacing_units = c.getUnit();
		if( this.spacing_units == null || this.spacing_units.length() == 0 )
			this.spacing_units = "" + c.getUnit();
		this.width = imagePlus.getWidth();
		this.height = imagePlus.getHeight();
		this.depth = imagePlus.getStackSize();
	}

	public PathAndFillManager( Simple_Neurite_Tracer plugin ) {
		this();
		this.plugin = plugin;
		this.x_spacing = plugin.x_spacing;
		this.y_spacing = plugin.y_spacing;
		this.z_spacing = plugin.z_spacing;
		this.spacing_units = plugin.spacing_units;
		this.width = plugin.width;
		this.height = plugin.height;
		this.depth = plugin.depth;
	}

	public PathAndFillManager( int width, int height, int depth, float x_spacing, float y_spacing, float z_spacing, String spacing_units ) {
		this();
		this.x_spacing = x_spacing;
		this.y_spacing = y_spacing;
		this.z_spacing = z_spacing;
		this.width = width;
		this.height = height;
		this.depth = depth;
		if( spacing_units == null )
			this.spacing_units = "unknown";
	}

	int width;
	int height;
	int depth;

	double x_spacing;
	double y_spacing;
	double z_spacing;
	String spacing_units;

	ArrayList< Path > allPaths;
	ArrayList< Fill > allFills;

	ArrayList< PathAndFillListener > listeners;

	HashSet< Path > selectedPathsSet;

	public int size() {
		return allPaths.size();
	}

	/* This is used by the interface to have changes in the path
	   manager reported so that they can be reflected in the UI. */

	public synchronized void addPathAndFillListener( PathAndFillListener listener ) {
		listeners.add(listener);
	}

	public synchronized Path getPath( int i ) {
		return allPaths.get(i);
	}

	public synchronized Path getPathFromID( int id ) {
		Iterator<Path> pi = allPaths.iterator();
		while( pi.hasNext() ) {
			Path p = pi.next();
			System.out.println("        comparing with ID "+id+" ("+p+")");
			if( id == p.getID() ) {
				System.out.println("          Found it!");
				return p;
			}
		}
		return null;
	}

	/* This is called to update the PathAndFillManager's idea of
	   which paths are currently selected.  This is also
	   propagated to:

               (a) Each Path object (so that the 3D viewer can reflect
               the change, for instance.)

               (b) All the registered PathAndFillListener objects.
	*/
	public synchronized void setSelected( Path [] selectedPaths, Object sourceOfMessage ) {
		selectedPathsSet.clear();
		for( int i = 0; i < selectedPaths.length; ++i )
			selectedPathsSet.add( selectedPaths[i] );
		for( Iterator<PathAndFillListener> i = listeners.iterator(); i.hasNext(); ) {
			PathAndFillListener pafl = i.next();
			if( pafl != sourceOfMessage )
				// The source of the message already knows the states:
				pafl.setSelectedPaths( selectedPathsSet, this );
		}
	}

	public synchronized boolean isSelected( Path path ) {
		return selectedPathsSet.contains(path);
	}

	public boolean anySelected( ) {
		return selectedPathsSet.size() > 0;
	}

	/* This method returns an array of the "primary paths", which
	   should be displayed at the top of a tree-like hierarchy.

           The paths actually form a graph, of course, but most UIs
           will want to display the graph as a tree. */

	public synchronized Path [] getPathsStructured() {

		ArrayList<Path> primaryPaths=new ArrayList<Path>();

		/* Some paths may be explicitly marked as primary, so
		   extract those and everything connected to them
		   first.  If you encounter another path marked as
		   primary when exploring from these then that's an
		   error... */

		TreeSet<Path> pathsLeft = new TreeSet<Path>();

		for( int i = 0; i < allPaths.size(); ++i ) {
			pathsLeft.add(allPaths.get(i));
		}

		int markedAsPrimary = 0;

		/* This is horrendously inefficent but with the number
		   of paths that anyone might reasonably add by hand
		   (I hope!) it's acceptable. */

		Iterator<Path> pi = pathsLeft.iterator();
		Path primaryPath = null;
		while( pi.hasNext() ) {
			Path p = pi.next();
			if( p.getPrimary() ) {
				pi.remove();
				primaryPaths.add(p);
				++ markedAsPrimary;
			}
		}

		for( int i = 0; i < primaryPaths.size(); ++i ) {
		        primaryPath = primaryPaths.get(i);
			System.out.println("Calling setChildren on primaryPath");
			primaryPath.setChildren(pathsLeft);
		}

		System.out.println("Now looking at each path left that has no start join");

		// Start with each one left that doesn't start on another:
		boolean foundOne = true;
		while( foundOne ) {
			foundOne = false;
			System.out.println("pathsLeft.size() is: "+pathsLeft.size());
			pi = pathsLeft.iterator();
			while( pi.hasNext() ) {
				Path p = pi.next();
				if( p.startJoins == null ) {
					foundOne = true;
					pi.remove();
					primaryPaths.add(p);
					p.setChildren(pathsLeft);
					break;
				}
			}
		}

		System.out.println("Now looking at each path that's left");

		// If there's anything left, start with that:
		while( pathsLeft.size() > 0 ) {
			pi = pathsLeft.iterator();
			Path p = pi.next();
			pi.remove();
			primaryPaths.add(p);
			p.setChildren(pathsLeft);
		}

		return primaryPaths.toArray(new Path[]{});
	}

	public synchronized void resetListeners( Path justAdded ) {
		resetListeners( justAdded, false );
	}

	public synchronized void resetListeners( Path justAdded, boolean expandAll ) {

		Hashtable< Path, Integer > pathToID = new Hashtable< Path, Integer >();

		ArrayList<String> pathListEntries = new ArrayList<String>();

		Iterator<Path> pi = allPaths.iterator();
		while( pi.hasNext() ) {
			Path p = pi.next();
			int pathID = p.getID();
			// if (verbose) System.out.println("path " + i + " is " + (Object)p );
			pathToID.put(p,new Integer(pathID));
			if( p == null ) {
				throw new RuntimeException("BUG: A path in allPaths was null!");
			}
			String pathName;
			String name = p.getName();
			if( name == null )
				name = "Path [" + pathID + "]";
			if( p.startJoins != null ) {
				name += ", starts on " + p.startJoins.getName();
			}
			if( p.endJoins != null ) {
				name += ", ends on " + p.endJoins.getName();
			}
			name += " [" + p.getRealLengthString() + " " + spacing_units + "]";
			pathListEntries.add( name );
		}

		for( Iterator i = listeners.iterator(); i.hasNext(); ) {
			PathAndFillListener listener = (PathAndFillListener)(i.next());
			listener.setPathList( pathListEntries.toArray( new String[]{} ), justAdded, expandAll );
		}

		int fills = allFills.size();

		String [] fillListEntries = new String[fills];

		for( int i = 0; i < fills; ++i ) {

			Fill f = allFills.get(i);
			if( f == null ) {
				if (verbose) System.out.println("fill was null with i "+i+" out of "+fills );
				continue;
			}

			String name = "Fill (" + i + ")";

			if( (f.sourcePaths != null) && (f.sourcePaths.size() > 0) ) {

				name += " from paths: ";

				Path [] sortedSourcePaths =f.sourcePaths.toArray( new Path[]{} );
				Arrays.sort( sortedSourcePaths );

				for( int j = 0; j < sortedSourcePaths.length; ++j ) {
					Path p = sortedSourcePaths[j];
					if( j != 0 )
						name += ", ";
					// if (verbose) System.out.println("source path " + j + " is " + (Object)p );
					Integer fromPath = pathToID.get( p );
					if( fromPath == null )
						name += "(unknown)";
					else
						name += "(" + fromPath.intValue() + ")";
				}
			}
			fillListEntries[i] = name;
		}

		for( Iterator i = listeners.iterator(); i.hasNext(); ) {
			((PathAndFillListener)(i.next())).setFillList( fillListEntries );
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

	public void addPath( Path p ) {
		addPath(p,false);
	}

	public synchronized void addPath( Path p, boolean forceNewName ) {
		if( p.getID() < 0 ) {
			p.setID(++maxUsedID);
		}
		String suggestedName = getDefaultName(p);
		if(p.getName() == null || forceNewName) {
			p.setName(suggestedName);
		}
		allPaths.add(p);
		resetListeners( p );
	}

	/* Find the default name for a new path, making sure it
	   doesn't collide with any of the existing names: */

	protected String getDefaultName(Path p) {
		if( p.getID() < 0 )
			throw new RuntimeException("A path's ID should never be negative");
		return "Path ("+p.getID()+")";
	}

	public synchronized void deletePath( int index ) {
		deletePath( index, true );
	}

	public synchronized void deletePath( Path p ) {
		int i = getPathIndex( p );
		if( i < 0 )
			throw new RuntimeException("Trying to delete a non-existent path");
		deletePath( i );
	}

	public synchronized int getPathIndex( Path p ) {
		int i = 0;
		for( i = 0; i < allPaths.size(); ++i ) {
			if( p == allPaths.get( i ) )
				return i;
		}
		return -1;
	}

	private synchronized void deletePath( int index, boolean updateInterface ) {

		// if (verbose) System.out.println("About to remove index: "+index+", leaving: "+allPaths.size()+" items.");
		Path deleted = allPaths.remove(index);
		// if (verbose) System.out.println("After, left "+allPaths.size()+" items.");

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

		selectedPathsSet.remove(deleted);

		if( updateInterface )
			resetListeners( null );
	}

	public void deletePaths( int [] indices ) {

		Arrays.sort( indices );

		for( int i = indices.length - 1; i >= 0; --i ) {
			deletePath( indices[i], false );
		}

		resetListeners( null );
	}

	public void addFill( Fill fill ) {

		allFills.add(fill);
		resetListeners( null );
	}

	public void deleteFills( int [] indices ) {

		Arrays.sort( indices );

		for( int i = indices.length - 1; i >= 0; --i ) {
			deleteFill( indices[i], false );
		}

		resetListeners( null );
	}

	public void deleteFill( int index ) {
		deleteFill( index, true );
	}

	private synchronized void deleteFill( int index, boolean updateInterface ) {

		allFills.remove( index );

		if( updateInterface )
			resetListeners( null );
	}

	public void reloadFill( int index ) {

		Fill toReload = allFills.get(index);

		plugin.startFillerThread( FillerThread.fromFill( plugin.getImagePlus(),
								 plugin.stackMin,
								 plugin.stackMax,
								 true,
								 toReload ) );

	}

	synchronized public void writeXML( String fileName,
					   Simple_Neurite_Tracer plugin,
					   boolean compress ) throws IOException {

		PrintWriter pw = null;

		try {
			if( compress ) {
				System.out.println("Creating, with compression...");
				pw = new PrintWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(fileName)),"UTF-8"));
				System.out.println("Created, pw is: "+pw);
			} else {
				System.out.println("Creating, without compression...");
				pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fileName),"UTF-8"));
				System.out.println("Created, pw is: "+pw);
			}

			pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			pw.println("<!DOCTYPE tracings [");
			pw.println("  <!ELEMENT tracings       (samplespacing,imagesize,path*,fill*)>");
			pw.println("  <!ELEMENT imagesize      EMPTY>");
			pw.println("  <!ELEMENT samplespacing  EMPTY>");
			pw.println("  <!ELEMENT path           (point+)>");
			pw.println("  <!ELEMENT point          EMPTY>");
			pw.println("  <!ELEMENT fill           (node*)>");
			pw.println("  <!ELEMENT node           EMPTY>");
			pw.println("  <!ATTLIST samplespacing  x             CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST samplespacing  y             CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST samplespacing  z             CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST samplespacing  units         CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST imagesize      width         CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST imagesize      height        CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST imagesize      depth         CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST path           id            CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST path           primary       CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST path           startson      CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST path           startsindex   CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST path           endson        CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST path           endsindex     CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST path           reallength    CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST point          x             CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST point          y             CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST point          z             CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST fill           id            CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST fill           frompaths     CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST fill           metric        CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST fill           threshold     CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST node           id            CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST node           x             CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST node           y             CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST node           z             CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST node           previousid    CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST node           distance      CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST node           status        (open|closed)   #REQUIRED>");
			pw.println("]>");
			pw.println("");

			pw.println("<tracings>");

			pw.println("  <samplespacing x=\"" + x_spacing + "\" " +
				   "y=\"" + y_spacing + "\" " +
				   "z=\"" + z_spacing + "\" " +
				   "units=\"" + spacing_units + "\"/>" );

			pw.println("  <imagesize width=\"" + width + "\" height=\"" + height + "\" depth=\"" + depth + "\"/>" );

			Hashtable< Path, Integer > pathToID =
				new Hashtable< Path, Integer >();

			for( Iterator j = allPaths.iterator(); j.hasNext(); ) {
				Path p = (Path)j.next();
				int id = p.getID();
				if( id < 0 )
					throw new RuntimeException("In writeXML() there was a path with a negative ID (BUG)");
				pathToID.put( p, id );
			}

			for( Iterator j = allPaths.iterator(); j.hasNext(); ) {
				Path p = (Path)j.next();
				// This probably should be a String returning
				// method of Path.
				pw.print("  <path id=\"" + p.getID() + "\"" );
				String startsString = "";
				String endsString = "";
				if( p.startJoins != null ) {
					int startPathIndex = ((pathToID.get(p.startJoins))).intValue();
					startsString = " startson=\"" + startPathIndex + "\"" +
						" startsindex=\"" + p.startJoinsIndex + "\"";
				}
				if( p.endJoins != null ) {
					int endPathIndex = ((pathToID.get(p.endJoins))).intValue();
					endsString = " endson=\"" + endPathIndex + "\"" +
						" endsindex=\"" + p.endJoinsIndex + "\"";
				}
				if( p.getPrimary() )
					pw.print(" primary=\"true\"");
				pw.print(startsString);
				pw.print(endsString);
				if( p.name != null ) {
					pw.print( " name=\""+p.name+"\"" );
				}
				pw.print(" reallength=\"" + p.getRealLength( ) + "\"");
				pw.println( ">" );
				for( int i = 0; i < p.size(); ++i ) {
					pw.println("    <point x=\"" +
						   p.x_positions[i] + "\" " +
						   "y=\"" + p.y_positions[i] + "\" z=\"" +
						   p.z_positions[i] + "\"/>");
				}
				pw.println( "  </path>" );
			}
			// Now output the fills:
			int fillIndex = 0;
			for( Iterator j = allFills.iterator(); j.hasNext(); ) {
				Fill f = (Fill) j.next();
				f.writeXML( pw, fillIndex, pathToID );
			}
			pw.println("</tracings>");
		} finally {
			pw.close();
		}
	}

	double parsed_x_spacing;
	double parsed_y_spacing;
	double parsed_z_spacing;

	String parsed_units;

	int parsed_width;
	int parsed_height;
	int parsed_depth;

	Fill current_fill;
	Path current_path;

	HashMap< Integer, Integer > startJoins;
	HashMap< Integer, Integer > startJoinsIndices;
	HashMap< Integer, Integer > endJoins;
	HashMap< Integer, Integer > endJoinsIndices;

	ArrayList< int [] > sourcePathIDForFills;

	int last_fill_node_id;

	int last_fill_id;

	HashSet< Integer > foundIDs;

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws TracesFileFormatException {

		if( qName.equals("tracings") ) {

			startJoins        = new HashMap< Integer, Integer >();
			startJoinsIndices = new HashMap< Integer, Integer >();
			endJoins          = new HashMap< Integer, Integer >();
			endJoinsIndices   = new HashMap< Integer, Integer >();

			sourcePathIDForFills = new ArrayList< int [] >();
			foundIDs = new HashSet< Integer >();

			last_fill_id = -1;

			/* We need to remove the old paths and fills
			 * before loading the ones: */

			if (verbose) System.out.println("Clearing old paths and fills...");

			clearPathsAndFills();

			if (verbose) System.out.println("Now "+allPaths.size()+" paths and "+allFills.size()+" fills");

		} else if( qName.equals("imagesize") ) {

			try {

				String widthString = attributes.getValue("width");
				String heightString = attributes.getValue("height");
				String depthString = attributes.getValue("depth");

				parsed_width = Integer.parseInt(widthString);
				parsed_height = Integer.parseInt(heightString);
				parsed_depth = Integer.parseInt(depthString);

				if( ! ((parsed_width == width) &&
				       (parsed_height == height) &&
				       (parsed_depth == depth)) ) {
					throw new TracesFileFormatException("The image size in the traces file didn't match - it's probably for another image");
				}

			} catch( NumberFormatException e ) {
				throw new TracesFileFormatException("There was an invalid attribute to <imagesize/>: "+e);
			}

		} else if( qName.equals("samplespacing") ) {

			try {

				String xString = attributes.getValue("x");
				String yString = attributes.getValue("y");
				String zString = attributes.getValue("z");
				parsed_units = attributes.getValue("units");

				parsed_x_spacing = Double.parseDouble(xString);
				parsed_y_spacing = Double.parseDouble(yString);
				parsed_z_spacing = Double.parseDouble(zString);

			} catch( NumberFormatException e ) {
				throw new TracesFileFormatException("There was an invalid attribute to <samplespacing/>: "+e);
			}

		} else if( qName.equals("path") ) {

			String idString = attributes.getValue("id");
			String d = attributes.getValue("d");

			String startsonString =  attributes.getValue("startson");
			String startsindexString =  attributes.getValue("startsindex");
			String endsonString =  attributes.getValue("endson");
			String endsindexString =  attributes.getValue("endsindex");

			String nameString = attributes.getValue("name");

			String primaryString = attributes.getValue("primary");

			if( (startsonString == null && startsindexString != null) ||
			    (startsonString != null && startsindexString == null) ) {
				throw new TracesFileFormatException("If startson is specified for a path, then startsindex must also be specified.");
			}

			if( (endsonString == null && endsindexString != null) &&
			    (endsonString != null && endsindexString == null) ) {
				throw new TracesFileFormatException("If endson is specified for a path, then endsindex must also be specified.");
			}

			int startson, startsindex, endson, endsindex;

			current_path = new Path( x_spacing, y_spacing, z_spacing, spacing_units );

			Integer startsOnInteger = null;
			Integer startsIndexInteger = null;
			Integer endsOnInteger = null;
			Integer endsIndexInteger = null;

			if( primaryString != null && primaryString.equals("true") )
				current_path.setPrimary(true);

			int id = -1;

			try {

				id = Integer.parseInt(idString);
				if( foundIDs.contains(id) ) {
					throw new TracesFileFormatException("There is more than one path with ID "+id);
				}
				current_path.setID(id);
				
				if( startsonString == null )
					startson = startsindex = -1;
				else {
					startson = Integer.parseInt(startsonString);
					startsindex = Integer.parseInt(startsindexString);

					startsOnInteger = new Integer( startson );
					startsIndexInteger = new Integer( startsindexString );
				}

				if( endsonString == null )
					endson = endsindex = -1;
				else {
					endson = Integer.parseInt(endsonString);
					endsindex = Integer.parseInt(endsindexString);

					endsOnInteger = new Integer( endson );
					endsIndexInteger = new Integer( endsindex );
				}

			} catch( NumberFormatException e ) {
				throw new TracesFileFormatException("There was an invalid attribute in <path/>: "+e);
			}

			if( nameString == null )
				current_path.setDefaultName();
			else
				current_path.setName(nameString);

			if( startsOnInteger != null )
				startJoins.put( id, startsOnInteger );
			if( endsOnInteger != null )
				endJoins.put( id, endsOnInteger );

			if( startsIndexInteger != null )
				startJoinsIndices.put( id, startsIndexInteger );
			if( endsIndexInteger != null )
				endJoinsIndices.put( id, endsIndexInteger );

		} else if( qName.equals("point") ) {

			try {

				String xString = attributes.getValue("x");
				String yString = attributes.getValue("y");
				String zString = attributes.getValue("z");

				int parsed_x = Integer.parseInt(xString);
				int parsed_y = Integer.parseInt(yString);
				int parsed_z = Integer.parseInt(zString);

				current_path.addPoint(parsed_x,parsed_y,parsed_z);

			} catch( NumberFormatException e ) {
				throw new TracesFileFormatException("There was an invalid attribute to <imagesize/>");
			}

		} else if( qName.equals("fill") ) {

			try {

				String [] sourcePaths = { };
				String fromPathsString = attributes.getValue("frompaths");
				if( fromPathsString != null )
					sourcePaths = fromPathsString.split(", *");

				current_fill = new Fill();

				String metric = attributes.getValue("metric");
				current_fill.setMetric(metric);

				last_fill_node_id = -1;

				String fill_id_string = attributes.getValue("id");

				int fill_id = Integer.parseInt(fill_id_string);

				if( fill_id < 0 ) {
					throw new TracesFileFormatException("Can't have a negative id in <fill>");
				}

				if( fill_id != (last_fill_id + 1) ) {
				       throw new TracesFileFormatException( "Out of order id in <fill> (" + fill_id +
									    " when we were expecting " + (last_fill_id + 1) + ")" );
				}

				int [] sourcePathIndices = new int[ sourcePaths.length ];

				for( int i = 0; i < sourcePaths.length; ++i )
					sourcePathIndices[i] = Integer.parseInt(sourcePaths[i]);

				sourcePathIDForFills.add( sourcePathIndices );

				last_fill_id = fill_id;

				String thresholdString = attributes.getValue("threshold");
				double fillThreshold = Double.parseDouble(thresholdString);

				current_fill.setThreshold(fillThreshold);

			} catch( NumberFormatException e ) {
				throw new TracesFileFormatException("There was an invalid attribute to <fill>");
			}


		} else if( qName.equals("node") ) {

			try {

				String xString = attributes.getValue("x");
				String yString = attributes.getValue("y");
				String zString = attributes.getValue("z");
				String idString = attributes.getValue("id");
				String distanceString = attributes.getValue("distance");
				String previousString = attributes.getValue("previousid");

				int parsed_x = Integer.parseInt(xString);
				int parsed_y = Integer.parseInt(yString);
				int parsed_z = Integer.parseInt(zString);
				int parsed_id = Integer.parseInt(idString);
				double parsed_distance = Double.parseDouble(distanceString);
				int parsed_previous;
				if( previousString == null )
					parsed_previous = -1;
				else
					parsed_previous = Integer.parseInt(previousString);

				if( parsed_id != (last_fill_node_id + 1) ) {
					throw new TracesFileFormatException("Fill node IDs weren't consecutive integers");
				}

				String openString = attributes.getValue("status");

				current_fill.add( parsed_x,
						  parsed_y,
						  parsed_z,
						  parsed_distance,
						  parsed_previous,
						  openString.equals("open") );

				last_fill_node_id = parsed_id;

			} catch( NumberFormatException e ) {
				throw new TracesFileFormatException("There was an invalid attribute to <node/>: "+e);
			}

		} else {
			throw new TracesFileFormatException("Unknown element: '"+qName+"'");
		}

	}

	@Override
	public void endElement(String uri, String localName, String qName) {

		if( qName.equals("path") ) {

			allPaths.add( current_path );
			if( plugin.use3DViewer ) {
				current_path.addTo3DViewer(plugin.univ,Color.MAGENTA);
			}

		} else if( qName.equals("fill") ) {

			allFills.add( current_fill );

		} else if( qName.equals("tracings") ) {

			// Then we've finished...

			for( int i = 0; i < allPaths.size(); ++i ) {
				Path p = allPaths.get(i);
				System.out.println("Going path "+i+", which is: "+p);

				Integer startID = startJoins.get(p.getID());
				Integer startIndexInteger = startJoinsIndices.get(p.getID());
				Integer endID = endJoins.get(p.getID());
				Integer endIndexInteger = endJoinsIndices.get(p.getID());

				System.out.println("  startID: "+startID);
				System.out.println("  startIndexInteger: "+startIndexInteger);
				System.out.println("  endID: "+endID);
				System.out.println("  endIndexInteger: "+endIndexInteger);
				if( startID != null ) {
					Path startPath = getPathFromID(startID);
					System.out.println("    Got startPath: "+startPath);
					p.setStartJoin( startPath, startIndexInteger );
				}
				if( endID != null ) {
					Path endPath = getPathFromID(endID);
					System.out.println("    Got endPath: "+endPath);
					p.setEndJoin( endPath, endIndexInteger );
				}
				System.out.println("=== path is now: "+p);
			}

			// Now turn the source paths into real paths...

			for( int i = 0; i < allFills.size(); ++i ) {
				Fill f = allFills.get(i);
				int [] sourcePathIDs = sourcePathIDForFills.get(i);
				Path [] realSourcePaths = new Path[sourcePathIDs.length];
				for( int j = 0; j < sourcePathIDs.length; ++j ) {
					realSourcePaths[j] = getPathFromID(sourcePathIDs[j]);
				}
				f.setSourcePaths( realSourcePaths );
			}

			setSelected( new Path[0], this );
			resetListeners( null, true );
			plugin.repaintAllPanes();
		}

	}

	public boolean loadFromString( String tracesFileAsString ) {

		StringReader reader=new StringReader(tracesFileAsString);
		boolean result = load(null,reader);
		reader.close();
		return result;

	}

	public boolean load( InputStream is, Reader reader ) {

		try {

			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setValidating(true);
			SAXParser parser = factory.newSAXParser();

			if( is != null )
				parser.parse( is, this );
			else if( reader != null ) {
				InputSource inputSource=new InputSource(reader);
				parser.parse( inputSource, this );
			}

		} catch( javax.xml.parsers.ParserConfigurationException e ) {

			clearPathsAndFills();
			IJ.error("There was a ParserConfigurationException: "+e);
			return false;

		} catch( SAXException e ) {

			clearPathsAndFills();
			IJ.error(e.toString());
			return false;

		} catch( FileNotFoundException e ) {

			clearPathsAndFills();
			IJ.error("File not found: "+e);
			return false;

		} catch( IOException e ) {

			clearPathsAndFills();
			IJ.error("There was an IO exception while reading the file: "+e);
			return false;

		}

		return true;

	}

	void clearPathsAndFills( ) {
			maxUsedID = -1;
			allPaths.clear();
			allFills.clear();
			resetListeners( null );
	}

	public boolean load( String filename ) {

		File f = new File(filename);
		if( ! f.exists() ) {
			IJ.error("The traces file '"+filename+"' does not exist.");
			return false;
		}

		boolean gzipped = false;

		try {
			InputStream is;
			byte[] buf = new byte[2];
			is = new FileInputStream(filename);
			is.read(buf, 0, 2);
			is.close();
			if (verbose) System.out.println("buf[0]: "+buf[0]+", buf[1]: "+buf[1]);
			if( ((buf[0]&0xFF) == 0x1F) && ((buf[1]&0xFF) == 0x8B) )
				gzipped = true;

		} catch (IOException e) {
			IJ.error("Couldn't read from file: "+filename);
			return false;
		}

		InputStream is = null;
		boolean result = false;

		try {

			if( gzipped ) {
				if (verbose) System.out.println("Loading gzipped file...");
				is = new GZIPInputStream(new BufferedInputStream(new FileInputStream(filename)));
			} else {
				if (verbose) System.out.println("Loading uncompressed file...");
				is = new BufferedInputStream(new FileInputStream(filename));
			}

			result = load(is,null);

			if( is != null )
				is.close();

		} catch( IOException ioe ) {
			IJ.error("Couldn't open file '"+filename+"' for reading.");
			return false;
		}

		return result;
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

		String [] first_line = tsv_results.get(0);
		int urls_found;
		String bestUrl = null;
		if( first_line[0].equals("success") ) {
			urls_found = Integer.parseInt(first_line[1]);
			if( urls_found == 0 ) {
				IJ.error( "No anntation files by " + (mineOnly ? archiveClient.getValue("user") : "any user") + " found." );
				return false;
			} else {
				bestUrl = (tsv_results.get(1))[1];
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

		// FIXME: This doesn't make much sense now that these files
		// might be gzipped and very large.  Save as a local file
		// instead use that (asuuming the security model allows that.)

		String fileContents =  ArchiveClient.justGetFileAsString( bestUrl );

		// IJ.error( "got fileContents: " +fileContents);

		if( fileContents == null ) {
			return false;
		} else {
			// FIXME: new format
			//  allPaths = loadFromString(fileContents);

			// return true;
			IJ.error("BUG: not implemented yet...");
			return false;
		}

	}

	// This method outputs the wrong format now.  FIXME

	@Deprecated
	public static byte [] tracesAsBytes( ArrayList< Path > all_paths ) {
		String s = tracesAsString( all_paths );
		try {
			byte [] bytes = s.getBytes("UTF-8");
			return bytes;
		} catch( UnsupportedEncodingException e ) {
			return null;
		}
	}

	// This method outputs the wrong format now.  FIXME

	@Deprecated
	public static String tracesAsString( ArrayList< Path > all_paths ) {

		StringBuffer sb = new StringBuffer();

		// if (verbose) System.out.println("Have some all_paths paths to draw.");
		int paths = all_paths.size();
		// if (verbose) System.out.println("Paths to draw: "+paths);
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
					// if (verbose) System.out.println( "Writing line: " + toWrite );
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

		if( archiveClient == null ) {
			IJ.error("This version of Simple Neurite Tracer doesn't seem to have been launched from an online archive, so you can't upload.");
			return false;
		}

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

		String [] first_line = tsv_results.get(0);
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

		int minimumDistanceSquared = Integer.MAX_VALUE;

		int paths = allPaths.size();

		for( int s = 0; s < paths; ++s ) {

			Path p = allPaths.get(s);

			if( ! selectedPathsSet.contains(p) )
				continue;

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

	@Deprecated
	ArrayList<Path> getAllPaths() {
		return allPaths;
	}

	// Methods we need to implement for UniverseListener:

	public void transformationStarted(View view) { }
	public void transformationUpdated(View view) { }
	public void transformationFinished(View view) { }

	public void contentAdded(Content c) { }
	public void contentRemoved(Content c) { }
	public void contentChanged(Content c) { }
	/* If someone selects a path in the 3D viewer, it would be
	   good to update the path list's selections with that: */
	public void contentSelected(Content c) {
		Path selectedPath = null;
		for( Iterator<Path> j = allPaths.iterator(); j.hasNext(); ) {
			Path p = j.next();
			if( p.content3D == c )
				selectedPath = p;
		}
		if( selectedPath == null ) {
			// This is probably someone accidentally
			// selecting the original image...
		} else {
			Path [] newSelectedPaths = new Path[1];
			newSelectedPaths[0] = selectedPath;
			setSelected(newSelectedPaths, this);
		}
	}
	public void canvasResized() { }
	public void universeClosed() { }

}

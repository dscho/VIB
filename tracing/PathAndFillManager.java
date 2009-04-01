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
import java.util.PriorityQueue;
import java.util.LinkedList;

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
import javax.vecmath.Color3f;
import ij3d.Content;
import ij3d.UniverseListener;

import util.CMTK_Transformation;

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
		this.x_spacing = c.pixelWidth;
		this.y_spacing = c.pixelHeight;
		this.z_spacing = c.pixelDepth;
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

	public synchronized Path getPathFromName( String name ) {
		return getPathFromName( name, true );
	}
	public synchronized Path getPathFromName( String name, boolean caseSensitive ) {
		Iterator<Path> pi = allPaths.iterator();
		while( pi.hasNext() ) {
			Path p = pi.next();
			if( caseSensitive ) {
				if( name.equals(p.getName()) )
					return p;
			} else {
				if( name.equalsIgnoreCase(p.getName()) )
					return p;
			}
		}
		return null;
	}

	public synchronized Path getPathFromID( int id ) {
		Iterator<Path> pi = allPaths.iterator();
		while( pi.hasNext() ) {
			Path p = pi.next();
			if( id == p.getID() ) {
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
		if( plugin != null ) {
			plugin.repaintAllPanes();
			plugin.update3DViewerContents();
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
			primaryPath.setChildren(pathsLeft);
		}

		// Start with each one left that doesn't start on another:
		boolean foundOne = true;
		while( foundOne ) {
			foundOne = false;
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
		if( getPathFromID( p.getID() ) != null )
			throw new RuntimeException("Attempted to add a path with an ID that was already added");
		if( p.getID() < 0 ) {
			p.setID(++maxUsedID);
		}
		if(p.name == null || forceNewName) {
			String suggestedName = getDefaultName(p);
			p.setName(suggestedName);
		}
		// Now check if there's already a path with this name.
		// If so, try adding numbered suffixes:
		String originalName = p.getName();
		String candidateName = originalName;
		int numberSuffix = 2;
		while( getPathFromName( candidateName ) != null ) {
			candidateName = originalName + " (" + numberSuffix + ")";
			++ numberSuffix;
		}
		p.setName( candidateName );
		/* Generate a new content3D, since it matters that the
		   path is added with the right name via
		   update3DViewerContents: */
		if( plugin != null && plugin.use3DViewer ) {
			p.removeFrom3DViewer( plugin.univ );
			p.addTo3DViewer( plugin.univ );
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
			throw new RuntimeException("Trying to delete a non-existent path: "+p);
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

		Path originalPathToDelete = allPaths.get(index);

		Path unfittedPathToDelete = null;
		Path fittedPathToDelete = null;

		if( originalPathToDelete.fittedVersionOf == null ) {
			unfittedPathToDelete = originalPathToDelete;
			fittedPathToDelete = originalPathToDelete.fitted;
		} else {
			unfittedPathToDelete = originalPathToDelete.fittedVersionOf;
			fittedPathToDelete = originalPathToDelete;
		}

		allPaths.remove(unfittedPathToDelete);
		if( fittedPathToDelete != null )
			allPaths.remove(fittedPathToDelete);

		// We don't just delete; have to fix up the references
		// in other paths (for start and end joins):

		for( Iterator i = allPaths.iterator(); i.hasNext(); ) {
			Path p = (Path)i.next();
			if( p.startJoins == unfittedPathToDelete ) {
				p.startJoins = null;
				p.startJoinsPoint = null;
			}
			if( p.endJoins == unfittedPathToDelete ) {
				p.endJoins = null;
				p.endJoinsPoint = null;
			}
		}

		selectedPathsSet.remove(fittedPathToDelete);
		selectedPathsSet.remove(unfittedPathToDelete);

		if( plugin != null && plugin.use3DViewer ) {
			if( fittedPathToDelete != null && fittedPathToDelete.content3D != null )
				fittedPathToDelete.removeFrom3DViewer(plugin.univ);
			if( unfittedPathToDelete.content3D != null )
				unfittedPathToDelete.removeFrom3DViewer(plugin.univ);
		}

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

	// FIXME: should probably use XMLStreamWriter instead of this ad-hoc approach:
	synchronized public void writeXML( String fileName,
					   boolean compress ) throws IOException {

		PrintWriter pw = null;

		try {
			if( compress )
				pw = new PrintWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(fileName)),"UTF-8"));
			else
				pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fileName),"UTF-8"));

			pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			pw.println("<!DOCTYPE tracings [");
			pw.println("  <!ELEMENT tracings       (samplespacing,imagesize,path*,fill*)>");
			pw.println("  <!ELEMENT imagesize      EMPTY>");
			pw.println("  <!ELEMENT samplespacing  EMPTY>");
			pw.println("  <!ELEMENT path           (point+)>");
			pw.println("  <!ELEMENT point          EMPTY>");
			pw.println("  <!ELEMENT fill           (node*)>");
			pw.println("  <!ELEMENT node           EMPTY>");
			pw.println("  <!ATTLIST samplespacing  x                 CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST samplespacing  y                 CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST samplespacing  z                 CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST samplespacing  units             CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST imagesize      width             CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST imagesize      height            CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST imagesize      depth             CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST path           id                CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST path           primary           CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST path           name              CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST path           startson          CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST path           startsindex       CDATA           #IMPLIED>"); // deprecated
			pw.println("  <!ATTLIST path           startsx           CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST path           startsy           CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST path           startsz           CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST path           endson            CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST path           endsindex         CDATA           #IMPLIED>"); // deprecated
			pw.println("  <!ATTLIST path           endsx             CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST path           endsy             CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST path           endsz             CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST path           reallength        CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST path           usefitted         (true|false)    #IMPLIED>");
			pw.println("  <!ATTLIST path           fitted            CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST path           fittedversionof   CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST point          x                 CDATA           #REQUIRED>"); // deprecated
			pw.println("  <!ATTLIST point          y                 CDATA           #REQUIRED>"); // deprecated
			pw.println("  <!ATTLIST point          z                 CDATA           #REQUIRED>"); // deprecated
			pw.println("  <!ATTLIST point          xd                CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST point          yd                CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST point          zd                CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST point          tx                CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST point          ty                CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST point          tz                CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST point          r                 CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST fill           id                CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST fill           frompaths         CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST fill           metric            CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST fill           threshold         CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST node           id                CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST node           x                 CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST node           y                 CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST node           z                 CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST node           previousid        CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST node           distance          CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST node           status            (open|closed)   #REQUIRED>");
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
					int startPathID = ((pathToID.get(p.startJoins))).intValue();
					// Find the nearest index for backward compatability:
					int nearestIndexOnStartPath = p.startJoins.indexNearestTo(
						p.startJoinsPoint.x,
						p.startJoinsPoint.y,
						p.startJoinsPoint.z );
					startsString = " startson=\"" + startPathID + "\"" +
						" startsindex=\"" + nearestIndexOnStartPath + "\"" +
						" startx=\"" + p.startJoinsPoint.x + "\"" +
						" starty=\"" + p.startJoinsPoint.y + "\"" +
						" startz=\"" + p.startJoinsPoint.z + "\"";
				}
				if( p.endJoins != null ) {
					int endPathID = ((pathToID.get(p.endJoins))).intValue();
					// Find the nearest index for backward compatability:
					int nearestIndexOnEndPath = p.endJoins.indexNearestTo(
						p.endJoinsPoint.x,
						p.endJoinsPoint.y,
						p.endJoinsPoint.z );
					endsString = " endson=\"" + endPathID + "\"" +
						" endsindex=\"" + nearestIndexOnEndPath + "\""+
						" endsx=\"" + p.endJoinsPoint.x + "\"" +
						" endsy=\"" + p.endJoinsPoint.y + "\"" +
						" endsz=\"" + p.endJoinsPoint.z + "\"";
				}
				if( p.getPrimary() )
					pw.print(" primary=\"true\"");
				pw.print(" usefitted=\""+p.getUseFitted()+"\"");
				if( p.fitted != null ) {
					pw.print(" fitted=\""+p.fitted.getID()+"\"");
				}
				if( p.fittedVersionOf != null ) {
					pw.print(" fittedversionof=\""+p.fittedVersionOf.getID()+"\"");
				}
				pw.print(startsString);
				pw.print(endsString);
				if( p.name != null ) {
					pw.print( " name=\""+escapeForXMLAttributeValue(p.name)+"\"" );
				}
				pw.print(" reallength=\"" + p.getRealLength( ) + "\"");
				pw.println( ">" );

				for( int i = 0; i < p.size(); ++i ) {
					int px = p.getXUnscaled(i);
					int py = p.getYUnscaled(i);
					int pz = p.getZUnscaled(i);
					double pxd = p.precise_x_positions[i];
					double pyd = p.precise_y_positions[i];
					double pzd = p.precise_z_positions[i];
					String attributes = "x=\"" + px + "\" " + "y=\"" + py + "\" z=\"" + pz + "\" "+
						"xd=\"" + pxd + "\" yd=\"" + pyd + "\" zd=\"" + pzd + "\"";
					if( p.hasCircles() ) {
						attributes += " tx=\""+p.tangents_x[i]+"\"";
						attributes += " ty=\""+p.tangents_y[i]+"\"";
						attributes += " tz=\""+p.tangents_z[i]+"\"";
						attributes += " r=\""+p.radiuses[i]+"\"";
					}
					pw.println("    <point "+attributes+"/>");
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
			if( pw != null )
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
	HashMap< Integer, PointInImage > startJoinsPoints;
	HashMap< Integer, Integer > endJoins;
	HashMap< Integer, Integer > endJoinsIndices;
	HashMap< Integer, PointInImage > endJoinsPoints;
	HashMap< Integer, Boolean > useFittedFields;
	HashMap< Integer, Integer > fittedFields;
	HashMap< Integer, Integer > fittedVersionOfFields;

	ArrayList< int [] > sourcePathIDForFills;

	int last_fill_node_id;

	int last_fill_id;

	HashSet< Integer > foundIDs;

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws TracesFileFormatException {

		if( qName.equals("tracings") ) {

			startJoins            = new HashMap< Integer, Integer >();
			startJoinsIndices     = new HashMap< Integer, Integer >();
			startJoinsPoints      = new HashMap< Integer, PointInImage >();
			endJoins              = new HashMap< Integer, Integer >();
			endJoinsIndices       = new HashMap< Integer, Integer >();
			endJoinsPoints        = new HashMap< Integer, PointInImage >();
			useFittedFields       = new HashMap< Integer, Boolean >();
			fittedFields          = new HashMap< Integer, Integer >();
			fittedVersionOfFields = new HashMap< Integer, Integer >();

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

			String useFittedString = attributes.getValue("usefitted");
			String fittedIDString = attributes.getValue("fitted");
			String fittedVersionOfIDString = attributes.getValue("fittedversionof");

			String startsonString =  attributes.getValue("startson");
			String startsindexString =  attributes.getValue("startsindex");
			String startsxString = attributes.getValue("startsx");
			String startsyString = attributes.getValue("startsy");
			String startszString = attributes.getValue("startsz");
			String endsonString =  attributes.getValue("endson");
			String endsindexString =  attributes.getValue("endsindex");
			String endsxString = attributes.getValue("endsx");
			String endsyString = attributes.getValue("endsy");
			String endszString = attributes.getValue("endsz");

			String nameString = attributes.getValue("name");

			String primaryString = attributes.getValue("primary");

			if( startsxString == null && startsyString == null && startszString == null ) { }
			else if( startsxString != null && startsyString != null && startszString != null ) { }
			else {
				throw new TracesFileFormatException("If one of starts[xyz] is specified, all of them must be.");
			}

			if( endsxString == null && endsyString == null && endszString == null ) { }
			else if( endsxString != null && endsyString != null && endszString != null ) { }
			else {
				throw new TracesFileFormatException("If one of ends[xyz] is specified, all of them must be.");
			}

			boolean accurateStartProvided = startsxString != null;
			boolean accurateEndProvided = endsxString != null;

			if( startsonString != null && (startsindexString == null && ! accurateStartProvided)  ) {
				throw new TracesFileFormatException("If startson is specified for a path, then startsindex or starts[xyz] must also be specified.");
			}

			if( endsonString != null && (endsindexString == null && ! accurateStartProvided)  ) {
				throw new TracesFileFormatException("If endson is specified for a path, then endsindex or ends[xyz] must also be specified.");
			}

			int startson, startsindex, endson, endsindex;
			double startsx, startsy, startsz;
			double endsx, endsy, endsz;

			current_path = new Path( x_spacing, y_spacing, z_spacing, spacing_units );

			Integer startsOnInteger = null;
			Integer startsIndexInteger = null;
			PointInImage startJoinPoint = null;
			Integer endsOnInteger = null;
			Integer endsIndexInteger = null;
			PointInImage endJoinPoint = null;

			Integer fittedIDInteger = null;
			Integer fittedVersionOfIDInteger = null;

			if( primaryString != null && primaryString.equals("true") )
				current_path.setPrimary(true);

			int id = -1;

			try {

				id = Integer.parseInt(idString);
				if( foundIDs.contains(id) ) {
					throw new TracesFileFormatException("There is more than one path with ID "+id);
				}
				current_path.setID(id);
				if( id > maxUsedID )
					maxUsedID = id;

				if( startsonString == null )
					startson = startsindex = -1;
				else {
					startson = Integer.parseInt(startsonString);
					startsOnInteger = new Integer( startson );

					if( startsxString != null ) {
						startJoinPoint = new PointInImage( Double.parseDouble( startsxString ),
										   Double.parseDouble( startsyString ),
										   Double.parseDouble( startszString ) );
					} else {
						// The index (older file format) was supplied:
						startsindex = Integer.parseInt(startsindexString);
						startsIndexInteger = new Integer( startsindexString );
					}
				}

				if( endsonString == null )
					endson = endsindex = -1;
				else {
					endson = Integer.parseInt(endsonString);
					endsOnInteger = new Integer( endson );

					if( endsxString != null ) {
						endJoinPoint = new PointInImage( Double.parseDouble( endsxString ),
										 Double.parseDouble( endsyString ),
										 Double.parseDouble( endszString ) );
					} else {
						// The index (older file format) was supplied:
						endsindex = Integer.parseInt(endsindexString);
						endsIndexInteger = new Integer( endsindex );
					}
				}

				if( fittedVersionOfIDString != null )
					fittedVersionOfIDInteger = new Integer( Integer.parseInt(fittedVersionOfIDString) );
				if( fittedIDString != null )
					fittedIDInteger = new Integer( Integer.parseInt(fittedIDString) );

			} catch( NumberFormatException e ) {
				e.printStackTrace();
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

			if( startJoinPoint != null )
				startJoinsPoints.put( id, startJoinPoint );
			if( endJoinPoint != null )
				endJoinsPoints.put( id, endJoinPoint );

			if( startsIndexInteger != null )
				startJoinsIndices.put( id, startsIndexInteger );
			if( endsIndexInteger != null )
				endJoinsIndices.put( id, endsIndexInteger );

			if( useFittedString == null )
				useFittedFields.put( id, false );
			else {
				if( useFittedString.equals("true") )
					useFittedFields.put( id, true );
				else if( useFittedString.equals("false") )
				        useFittedFields.put( id, false );
				else
					throw new TracesFileFormatException("Unknown value for 'fitted' attribute: '"+useFittedString+"'");
			}

			if( fittedIDInteger != null )
				fittedFields.put( id, fittedIDInteger );
			if( fittedVersionOfIDInteger != null )
				fittedVersionOfFields.put( id, fittedVersionOfIDInteger );

		} else if( qName.equals("point") ) {

			try {

				double parsed_xd, parsed_yd, parsed_zd;

				String xdString = attributes.getValue("xd");
				String ydString = attributes.getValue("yd");
				String zdString = attributes.getValue("zd");

				String xString = attributes.getValue("x");
				String yString = attributes.getValue("y");
				String zString = attributes.getValue("z");

				if( xdString != null &&
				    ydString != null &&
				    zdString != null ) {
					parsed_xd = Double.parseDouble(xdString);
					parsed_yd = Double.parseDouble(ydString);
					parsed_zd = Double.parseDouble(zdString);
				} else if( xdString != null ||
					   ydString != null ||
					   zdString != null ) {
					throw new TracesFileFormatException("If one of the attributes xd, yd or zd to the point element is specified, they all must be.");
				} else if( xString != null &&
					   yString != null &&
					   zString != null ) {
					parsed_xd = parsed_x_spacing * Integer.parseInt(xString);
					parsed_yd = parsed_y_spacing * Integer.parseInt(yString);
					parsed_zd = parsed_z_spacing * Integer.parseInt(zString);
				} else if( xString != null ||
					   yString != null ||
					   zString != null ) {
					throw new TracesFileFormatException("If one of the attributes x, y or z to the point element is specified, they all must be.");
				} else {
					throw new TracesFileFormatException("Each point element must have at least the attributes (x, y and z) or (xd, yd, zd)");
				}

				current_path.addPointDouble(parsed_xd,parsed_yd,parsed_zd);

				int lastIndex = current_path.size() - 1;
				String radiusString = attributes.getValue("r");
				String tXString = attributes.getValue("tx");
				String tYString = attributes.getValue("ty");
				String tZString = attributes.getValue("tz");

				if( radiusString != null &&
				    tXString != null &&
				    tYString != null &&
				    tZString != null ) {
					if( lastIndex == 0 )
						// Then we've just started, create the arrays in Path:
						current_path.createCircles();
					else if( ! current_path.hasCircles() )
						throw new TracesFileFormatException("The point at index " + lastIndex + " had a fitted circle, but none previously did");
					current_path.tangents_x[lastIndex] = Double.parseDouble( tXString );
					current_path.tangents_y[lastIndex] = Double.parseDouble( tYString );
					current_path.tangents_z[lastIndex] = Double.parseDouble( tZString );
					current_path.radiuses[lastIndex] = Double.parseDouble( radiusString );
				} else if( radiusString != null ||
					   tXString != null ||
					   tYString != null ||
					   tZString != null )
					throw new TracesFileFormatException("If one of the r, tx, ty or tz attributes to the point element is specified, they all must be");
				else {
					// All circle attributes are null:
					if( current_path.hasCircles() )
						throw new TracesFileFormatException("The point at index " + lastIndex + " had no fitted circle, but all previously did");
				}

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

	public void addTo3DViewer( Path p ) {
		if( plugin != null && plugin.use3DViewer && p.fittedVersionOf == null && p.size() > 1 ) {
			Path pathToAdd;
			if( p.getUseFitted() )
				pathToAdd = p.fitted;
			else
				pathToAdd = p;
			pathToAdd.addTo3DViewer(plugin.univ,plugin.deselectedColor);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) {

		if( qName.equals("path") ) {

			allPaths.add( current_path );

		} else if( qName.equals("fill") ) {

			allFills.add( current_fill );

		} else if( qName.equals("tracings") ) {

			// Then we've finished...

			for( int i = 0; i < allPaths.size(); ++i ) {
				Path p = allPaths.get(i);

				Integer startID = startJoins.get(p.getID());
				Integer startIndexInteger = startJoinsIndices.get(p.getID());
				PointInImage startJoinPoint = startJoinsPoints.get(p.getID());
				Integer endID = endJoins.get(p.getID());
				Integer endIndexInteger = endJoinsIndices.get(p.getID());
				PointInImage endJoinPoint = endJoinsPoints.get(p.getID());
				Integer fittedID = fittedFields.get(p.getID());
				Integer fittedVersionOfID = fittedVersionOfFields.get(p.getID());
				Boolean useFitted = useFittedFields.get(p.getID());

				if( startID != null ) {
					Path startPath = getPathFromID(startID);
					if( startJoinPoint == null ) {
						// Then we have to get it from startIndexInteger:
						startJoinPoint = startPath.getPointInImage(startIndexInteger.intValue());
					}
					p.setStartJoin( startPath, startJoinPoint );
				}
				if( endID != null ) {
					Path endPath = getPathFromID(endID);
					if( endJoinPoint == null ) {
						// Then we have to get it from endIndexInteger:
						endJoinPoint = endPath.getPointInImage(endIndexInteger.intValue());
					}
					p.setEndJoin( endPath, endJoinPoint );
				}
				if( fittedID != null ) {
					Path fitted = getPathFromID(fittedID);
					p.fitted = fitted;
					p.setUseFitted(useFitted.booleanValue());
				}
				if( fittedVersionOfID != null ) {
					Path fittedVersionOf = getPathFromID(fittedVersionOfID);
					p.fittedVersionOf = fittedVersionOf;
				}

				addTo3DViewer( p );
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
			if( plugin != null )
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
		if( plugin != null && plugin.use3DViewer ) {
			for( Iterator< Path > i = allPaths.iterator();
			     i.hasNext(); ) {
				Path p = i.next();
				p.removeFrom3DViewer( plugin.univ );
			}
		}
		allPaths.clear();
		allFills.clear();
		resetListeners( null );
	}

	private static class SWCPoint implements Comparable {
		ArrayList<SWCPoint> nextPoints;
		SWCPoint previousPoint;
		int id, type, previous;
		double x, y, z, radius;
		public SWCPoint( int id, int type, double x, double y, double z, double radius, int previous ) {
			nextPoints = new ArrayList<SWCPoint>();
			this.id = id;
			this.type = type;
			this.x = x;
			this.y = y;
			this.z = z;
			this.radius = radius;
			this.previous = previous;
		}
		public PointInImage getPointInImage() {
			return new PointInImage( x, y, z );
		}
		public void addNextPoint( SWCPoint p ) {
			if( ! nextPoints.contains( p ) )
				nextPoints.add( p );
		}
		public void setPreviousPoint( SWCPoint p ) {
			previousPoint = p;
		}
		public String toString( ) {
			return "SWCPoint ["+id+"] "+Path.swcTypeNames[type]+" "+
				"("+x+","+y+","+z+") "+
				"radius: "+radius+", "+
				"[previous: "+ previous+"]";
		}
		public int compareTo( Object o ) {
			int oid = ((SWCPoint)o).id;
			return (id < oid) ? -1 : ((id > oid) ? 1 : 0);
		}
	}

	public boolean importSWC( BufferedReader br ) throws IOException {

		clearPathsAndFills( );

		Pattern pEmpty = Pattern.compile("^\\s*$");
		Pattern pComment = Pattern.compile("^([^#]*)#.*$");

		HashSet< Integer > alreadySeen = new HashSet< Integer >();
		HashMap< Integer, SWCPoint > idToSWCPoint = new HashMap< Integer, SWCPoint >();

		ArrayList<SWCPoint> primaryPoints = new ArrayList<SWCPoint>();

		/* The SWC files I've tried use world co-ordinates
		   (good) but some seem to have the sign wrong, so
		   calculate the minimum and maximum in each axis to
		   test for this: */

		double minX = Math.min( 0 * x_spacing, width * x_spacing );
		double minY = Math.min( 0 * y_spacing, width * y_spacing );
		double minZ = Math.min( 0 * z_spacing, width * z_spacing );

		double maxX = Math.max( 0 * x_spacing, width * x_spacing );
		double maxY = Math.max( 0 * y_spacing, width * y_spacing );
		double maxZ = Math.max( 0 * z_spacing, width * z_spacing );

		String line;
		while( (line = br.readLine()) != null ) {
			Matcher mComment = pComment.matcher(line);
			line = mComment.replaceAll("$1");
			Matcher mEmpty = pEmpty.matcher(line);
			if( mEmpty.matches() )
				continue;
			String [] fields = line.split("\\s+");
			if( fields.length != 7 ) {
				IJ.error("Wrong number of fields ("+fields.length+") in line: "+line);
				return false;
			}
			try {
				int id = Integer.parseInt(fields[0]);
				int type = Integer.parseInt(fields[1]);
				double x = Double.parseDouble(fields[2]);
				double y = Double.parseDouble(fields[3]);
				double z = Double.parseDouble(fields[4]);
				double radius = Double.parseDouble(fields[5]);
				int previous = Integer.parseInt(fields[6]);
				if( alreadySeen.contains(id) ) {
					IJ.error("Point with ID "+id+" found more than once");
					return false;
				}
				alreadySeen.add( id );

				if( (x < 0) && ! (x >= minX && x <= maxX) )
					x = Math.abs( x );
				if( (y < 0) && ! (y >= minY && y <= maxY) )
					y = Math.abs( y );
				if( (z < 0) && ! (z >= minZ && z <= maxZ) )
					z = Math.abs( z );

				SWCPoint p = new SWCPoint( id, type, x, y, z, radius, previous );
				idToSWCPoint.put( id, p );
				if( previous == -1 )
					primaryPoints.add( p );
				else {
					SWCPoint previousPoint = idToSWCPoint.get( previous );
					p.previousPoint = previousPoint;
					previousPoint.addNextPoint( p );
				}
			} catch( NumberFormatException nfe ) {
				IJ.error( "There was a malformed number in line: "+line );
				return false;
			}
		}

		HashMap< SWCPoint, Path > pointToPath =
			new HashMap< SWCPoint, Path >();

		PriorityQueue< SWCPoint > backtrackTo =
			new PriorityQueue< SWCPoint >();

		for( Iterator< SWCPoint > pi = primaryPoints.iterator();
		     pi.hasNext(); ) {
			SWCPoint start = pi.next();
			backtrackTo.add( start );
		}

		HashMap< Path, SWCPoint > pathStartsOnSWCPoint =
			new HashMap< Path, SWCPoint >();
		HashMap< Path, PointInImage > pathStartsAtPointInImage =
			new HashMap< Path, PointInImage >();

		SWCPoint start;
		Path currentPath;
		while( (start = backtrackTo.poll()) != null ) {
			currentPath = new Path( x_spacing, y_spacing, z_spacing, spacing_units );
			currentPath.createCircles();
			int added = 0;
			if( start.previousPoint != null ) {
				SWCPoint beforeStart = start.previousPoint;
				pathStartsOnSWCPoint.put( currentPath, beforeStart );
				pathStartsAtPointInImage.put( currentPath, beforeStart.getPointInImage() );
				currentPath.addPointDouble( beforeStart.x,
							    beforeStart.y,
							    beforeStart.z );
				currentPath.radiuses[added] = beforeStart.radius;
				++ added;

			}
			// Now we can start adding points to the path:
			SWCPoint currentPoint = start;
			while( currentPoint != null ) {
				currentPath.addPointDouble( currentPoint.x,
							    currentPoint.y,
							    currentPoint.z );
				currentPath.radiuses[added] = currentPoint.radius;
				++ added;
				pointToPath.put( currentPoint, currentPath );
				/* Remove each one from "alreadySeen"
				   when we add it to a path, just to
				   check that nothing's left at the
				   end, which indicates that the file
				   is malformed. */
				alreadySeen.remove( currentPoint.id );
				if( currentPoint.nextPoints.size() > 0 ) {
					SWCPoint newCurrentPoint = currentPoint.nextPoints.get(0);
					currentPoint.nextPoints.remove(0);
					for( int i = 0; i < currentPoint.nextPoints.size(); ++i ) {
						SWCPoint pointToQueue = currentPoint.nextPoints.get(i);
						backtrackTo.add( pointToQueue );
					}
					currentPoint = newCurrentPoint;
				} else
					currentPoint = null;
			}
			currentPath.setGuessedTangents( 2 );
			addPath( currentPath );
		}

		if( alreadySeen.size() > 0 ) {
			IJ.error( "Malformed file: there are some misconnected points" );
			for( Iterator<Integer> i = alreadySeen.iterator();
			     i.hasNext(); ) {
				SWCPoint p = idToSWCPoint.get( i.next() );
				System.out.println( "  Misconnected: " + p);
			}
			return false;
		}

		// Set the start joins:
		for( Iterator<Path> i = allPaths.iterator();
		     i.hasNext(); ) {
			Path p = i.next();
			SWCPoint swcPoint = pathStartsOnSWCPoint.get( p );
			if( swcPoint == null )
				continue;
			Path previousPath = pointToPath.get(swcPoint);
			PointInImage pointInImage = pathStartsAtPointInImage.get( p );
			p.setStartJoin( previousPath, pointInImage );
		}

		for( Iterator<Path> i = allPaths.iterator();
		     i.hasNext(); ) {
			Path p = i.next();
			addTo3DViewer( p );
		}

		resetListeners( null, true );
		return true;
	}

	public boolean importSWC( String filename ) {

		File f = new File(filename);
		if( ! f.exists() ) {
			IJ.error("The traces file '"+filename+"' does not exist.");
			return false;
		}

		InputStream is = null;
		boolean result = false;

		try {

			is = new BufferedInputStream(new FileInputStream(filename));
			BufferedReader br = new BufferedReader(new InputStreamReader(is,"UTF-8"));

			result = importSWC(br);

			if( is != null )
				is.close();

		} catch( IOException ioe ) {
			IJ.error("Couldn't open file '"+filename+"' for reading.");
			return false;
		}

		return result;


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

/* FIXME: new format now... */
/*
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
*/

	// This method outputs the wrong format now.  FIXME
/*
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
*/

	// This method outputs the wrong format now.  FIXME
/*
	@Deprecated
	public static String tracesAsString( ArrayList< Path > all_paths ) {

		StringBuffer sb = new StringBuffer();

		// if (verbose) System.out.println("Have some all_paths paths to draw.");
		int paths = all_paths.size();
		// if (verbose) System.out.println("Paths to draw: "+paths);
		for( int i = 0; i < paths; ++i ) {

			double last_x = Double.MIN_VALUE;
			double last_y = Double.MIN_VALUE;
			double last_z = Double.MIN_VALUE;

			Path path = all_paths.get(i);

			for( int k = 0; k < path.size(); ++k ) {
				double xd = path.precise_x_positions[k];
				double yd = path.precise_y_positions[k];
				double zd = path.precise_z_positions[k];
				if( (last_x == xd) && (last_y == yd) && (last_z == zd) ) {
					// Skip this, it's just the same.
				} else {
					String toWrite = "" + i + "\t" +
						xd + "\t" +
						yd + "\t" +
						zd + "\t" +
						(path.startJoins != null) + "\n";
					// if (verbose) System.out.println( "Writing line: " + toWrite );
					sb.append( toWrite );
				}
				last_x = xd;
				last_y = yd;
				last_z = zd;
			}
		}

		return sb.toString();

	}
*/

/* FIXME: should change this to upload XML instead */
/*
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
*/

	/* This method will set all the points in array that
	 * correspond to points on one of the paths to 255, leaving
	 * everything else as it is. */

	synchronized void setPathPointsInVolume( byte [][] slices, int width, int height, int depth ) {
		for( Iterator j = allPaths.iterator(); j.hasNext(); ) {
			Path p = (Path)j.next();
			for( int i = 0; i < p.size(); ++i ) {
				slices[p.getZUnscaled(i)][p.getYUnscaled(i) * width + p.getXUnscaled(i)] =
					(byte)255;
			}
		}
	}

	synchronized PointInImage nearestJoinPointOnSelectedPaths( double x, double y, double z ) {

		PointInImage result = null;

		double minimumDistanceSquared = Double.MAX_VALUE;

		int paths = allPaths.size();

		for( int s = 0; s < paths; ++s ) {

			Path p = allPaths.get(s);

			if( ! selectedPathsSet.contains(p) )
				continue;

			if( 0 == p.size() )
				continue;

			int i = p.indexNearestTo( x * x_spacing,
						  y * y_spacing,
						  z * z_spacing );

			PointInImage nearestOnPath = p.getPointInImage( i );

			double distanceSquared = nearestOnPath.distanceSquaredTo(
				x * x_spacing,
				y * y_spacing,
				z * z_spacing );

			if( distanceSquared < minimumDistanceSquared ) {
				result = nearestOnPath;
				minimumDistanceSquared = distanceSquared;
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
	public void contentSelected(Content c) { }
	public void canvasResized() { }
	public void universeClosed() {
		plugin.use3DViewer = false;
	}
	// ... end of methods for UniverseListener

	private static void replaceAll( StringBuffer s, String substring, String replacement ) {
		int fromIndex = 0;
		while (true) {
			int foundIndex = s.indexOf(substring,fromIndex);
			if( foundIndex >= 0 ) {
				int afterEnd = foundIndex + substring.length();
				s.replace(foundIndex,afterEnd,replacement);
				fromIndex = afterEnd;
			} else
				break;
		}
	}

	// This is quite ineffficient, but not expected to be a serious problem:
	public static String escapeForXMLAttributeValue( String s ) {
		StringBuffer sb = new StringBuffer(s);
		replaceAll( sb, "&", "&amp;" );
		replaceAll( sb, "<", "&lt;" );
		replaceAll( sb, ">", "&gt;" );
		replaceAll( sb, "'", "&apos;" );
		replaceAll( sb, "\"", "&quot;" );
		return sb.toString();
	}

	public NearPoint nearestPointOnAnyPath( double x, double y, double z, double distanceLimit ) {

		/* Order all points in all paths by their euclidean
		   distance to (x,y,z): */

		PriorityQueue< NearPoint > pq = new PriorityQueue< NearPoint >();

		for( Iterator< Path > i = allPaths.iterator();
		     i.hasNext(); ) {
			Path path = i.next();
			if( path.useFitted )
				continue;
			if( path.fittedVersionOf != null && ! path.fittedVersionOf.useFitted )
				continue;
			for( int j = 0; j < path.size(); ++j ) {
				pq.add( new NearPoint( x, y, z, path, j ) );
			}
		}

		while( true ) {

			NearPoint np = pq.poll();
			if( np == null )
				return null;

			/* Don't bother looking at points that are
			   more than distanceLimit away.  Since we get
			   them in the order closest to furthest away,
			   if we exceed this limit returned: */

			if( np.distanceToPathPointSquared() > (distanceLimit * distanceLimit) )
				return null;

			double distanceToPath = np.distanceToPathNearPoint();
			if( distanceToPath >= 0 )
				return np;
		}
	}

	public AllPointsIterator allPointsIterator() {
		return new AllPointsIterator();
	}

	public int pointsInAllPaths( ) {
		AllPointsIterator a = allPointsIterator();
		int points = 0;
		while( a.hasNext() ) {
			a.next();
			++ points;
		}
		return points;
	}

	public class AllPointsIterator implements java.util.Iterator {

		public AllPointsIterator() {
			numberOfPaths = allPaths.size();
			currentPath = null;
			currentPathIndex = -1;
			currentPointIndex = -1;
		}

		int numberOfPaths;
		// These should all be set to be appropriate to the
		// last point that was returned:
		Path currentPath;
		int currentPathIndex;
		int currentPointIndex;

		public boolean hasNext() {
			if( currentPath == null || currentPointIndex == currentPath.points - 1 ) {
				/* Find out if there is a non-empty
				   path after this: */
				int tmpPathIndex = currentPathIndex + 1;
				while( tmpPathIndex < numberOfPaths ) {
					Path p = allPaths.get( tmpPathIndex );
					if( p.size() > 0 )
						return true;
				}
				return false;
			}
			/* So we know that there's a current path and
			   we're not at the end of it, so there must
			   be another point: */
			return true;
		}

		public PointInImage next() {
			if( currentPath == null || currentPointIndex == currentPath.points - 1 ) {
				currentPointIndex = 0;
				/* Move to the next non-empty path: */
				while( true ) {
					++ currentPathIndex;
					if( currentPathIndex == numberOfPaths )
						throw new java.util.NoSuchElementException();
					currentPath = allPaths.get( currentPathIndex );
					if( currentPath.size() > 0 )
						break;
				}
			} else
				++ currentPointIndex;
			return currentPath.getPointInImage(currentPointIndex);
		}

		public void remove() {
			throw new UnsupportedOperationException("AllPointsIterator does not allow the removal of points");
		}

	}

	/* For each point in *this* PathAndFillManager, find the
	   corresponding point on the other one.  If there's no
	   corresponding one, include a null instead. */

	public ArrayList< NearPoint > getCorrespondences( PathAndFillManager other, double maxDistance ) {

		ArrayList< NearPoint > result = new ArrayList< NearPoint >();

		AllPointsIterator i = allPointsIterator();
		int numberFromIterator = 0;
		while( i.hasNext() ) {
			PointInImage p = i.next();
			NearPoint np = other.nearestPointOnAnyPath(
				p.x,
				p.y,
				p.z,
				maxDistance );
			result.add(np);
		}
		return result;
	}

	public static String stringForCSV( String s ) {
		boolean quote = false;
		String result = s;
		if( s.indexOf(',') >= 0 )
			quote = true;
		if( s.indexOf('"') >= 0 ) {
			System.out.println("ooh, got a quote..");
			quote = true;
			result = s.replaceAll("\"","\"\"");
			System.out.println("result is now: "+result);
		}
		if( quote )
			return "\"" + result + "\"";
		else
			return result;
	}

	/* Output some potentially useful information about the paths
	   as a CSV (comma separated values) file. */

	public void exportToCSV( File outputFile ) throws IOException {
		// FIXME: also add statistics on volumes of fills and
		// reconstructions...
		String [] headers = { "PathID",
				      "PathName",
				      "PrimaryPath",
				      "PathLength",
				      "PathLengthUnits",
				      "StartsOnPath",
				      "EndsOnPath",
				      "ConnectedPathIDs",
				      "ChildPathIDs" };

		Path [] primaryPaths = getPathsStructured();
		HashSet<Path> h = new HashSet<Path>();
		for( int i = 0; i < primaryPaths.length; ++i )
			h.add(primaryPaths[i]);

		PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outputFile.getAbsolutePath()),"UTF-8"));
		int columns = headers.length;
		for( int c = 0; c < columns; ++c ) {
			pw.print(stringForCSV(headers[c]));
			if( c < (columns - 1) )
				pw.print(",");
		}
		pw.print("\r\n");
		Iterator<Path> pi = allPaths.iterator();
		while( pi.hasNext() ) {
			Path p = pi.next();
			Path pForLengthAndName = p;
			if( p.getUseFitted() ) {
				pForLengthAndName = p.fitted;
			}
			if( p.fittedVersionOf != null )
				continue;
			pw.print(stringForCSV(""+p.getID()));
			pw.print(",");
			pw.print(stringForCSV(""+pForLengthAndName.getName()));
			pw.print(",");
			boolean primary = h.contains(p);
			pw.print(stringForCSV(""+primary));
			pw.print(",");
			pw.print(stringForCSV(""+pForLengthAndName.getRealLength()));
			pw.print(",");
			pw.print(stringForCSV(""+p.spacing_units));
			pw.print(",");
			if( p.startJoins != null )
				pw.print(""+p.startJoins.getID());
			pw.print(",");
			if( p.endJoins != null )
				pw.print(""+p.endJoins.getID());
			pw.print(",");
			pw.print(stringForCSV(p.somehowJoinsAsString()));
			pw.print(",");
			pw.print(stringForCSV(p.childrenAsString()));
			pw.print("\r\n");
			pw.flush();
		}
		pw.close();
	}

	/* Whatever the state of the paths, update the 3D viewer to
	   make sure that they're the right colour, the right version
	   (fitted or unfitted) is being used and whether the path
	   should be displayed at all - it shouldn't if the "Show only
	   selected paths" option is set. */

	public void update3DViewerContents() {
		if( plugin != null && ! plugin.use3DViewer )
			return;
		boolean showOnlySelectedPaths = plugin.getShowOnlySelectedPaths();
		// Now iterate over all the paths:
		Iterator<Path> pi = allPaths.iterator();
		while( pi.hasNext() ) {

			Path p = pi.next();

			if( p.fittedVersionOf != null )
				continue;

			Content c = null;
			Content cWrong = null;

			if( p.getUseFitted() ) {
				c = p.fitted.content3D;
				cWrong = p.content3D;
			} else {
				c = p.content3D;
				if( p.fitted != null )
					cWrong = p.fitted.content3D;
			}
			boolean selected = p.getSelected();

			if( c == null )
				throw new RuntimeException( "content3D should never be null for any path if use3DViewer is true");

			String contentName = c.getName();
			Color3f contentColor = c.getColor();

			String wrongContentName = null;
			if( cWrong != null )
				wrongContentName = cWrong.getName();

			// Check that the color is right:
			if( selected && ! contentColor.equals(plugin.selectedColor3f) )
				c.setColor( plugin.selectedColor3f );
			if( ! selected && ! contentColor.equals(plugin.deselectedColor3f ) )
				c.setColor( plugin.deselectedColor3f );

			boolean in3DViewer = plugin.univ.contains(contentName);

			if( selected || ! showOnlySelectedPaths ) {
				// Then this path should be in the
				if( ! in3DViewer ) {
					plugin.univ.resetView();
					plugin.univ.addContent( c );
				}
			} else {
				if( in3DViewer )
					plugin.univ.removeContent(contentName);
			}

			// If the wrong content is in the viewer, remove that:
			if( wrongContentName != null && plugin.univ.contains(wrongContentName) )
				plugin.univ.removeContent(wrongContentName);

		}
	}

	/** A base class for all the methods we might want to use to
	    transform paths. */

	public PathAndFillManager transformPaths( PathTransformer transformation, ImagePlus templateImage ) {

		double pixelWidth = 1;
		double pixelHeight = 1;
		double pixelDepth = 1;
		String units = "pixels";

		Calibration templateCalibration = templateImage.getCalibration();
		if( templateCalibration != null ) {
			pixelWidth = templateCalibration.pixelWidth;
			pixelHeight = templateCalibration.pixelHeight;
			pixelDepth = templateCalibration.pixelDepth;
			units = templateCalibration.getUnits();
		}

		PathAndFillManager pafmResult = new PathAndFillManager( templateImage.getWidth(),
									templateImage.getHeight(),
									templateImage.getStackSize(),
									(float)pixelWidth,
									(float)pixelHeight,
									(float)pixelDepth,
									units );

		int [] startJoinsIndices = new int[size()];
		int [] endJoinsIndices = new int[size()];

		PointInImage [] startJoinsPoints = new PointInImage[size()];
		PointInImage [] endJoinsPoints = new PointInImage[size()];

		Path [] addedPaths = new Path[size()];

		int i = 0;
		for( Path p : allPaths ) {

			Path startJoin = p.getStartJoins();
			if( startJoin == null ) {
				startJoinsIndices[i] = -1;
				endJoinsPoints[i] = null;
			} else {
				startJoinsIndices[i] = allPaths.indexOf(startJoin);
				startJoinsPoints[i] =
					p.getStartJoinsPoint().transform( transformation );
			}

			Path endJoin = p.getEndJoins();
			if( endJoin == null ) {
				endJoinsIndices[i] = -1;
				endJoinsPoints[i] = null;
			} else {
				endJoinsIndices[i] = allPaths.indexOf(endJoin);
				endJoinsPoints[i] =
					p.getEndJoinsPoint().transform( transformation );
			}

			Path transformedPath = p.transform( transformation, templateImage, imagePlus );
			addedPaths[i] = transformedPath;
			pafmResult.addPath( transformedPath );

			++i;
		}

		for( i = 0; i < size(); ++i ) {
			if( startJoinsIndices[i] >= 0 )
				addedPaths[i].setStartJoin( addedPaths[startJoinsIndices[i]], startJoinsPoints[i] );
			if( endJoinsIndices[i] >= 0 )
				addedPaths[i].setEndJoin( addedPaths[endJoinsIndices[i]], endJoinsPoints[i] );
		}

		return pafmResult;
	}
}

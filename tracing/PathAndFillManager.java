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

public class PathAndFillManager extends DefaultHandler {
	
	static final boolean verbose = Simple_Neurite_Tracer.verbose;
	
	Simple_Neurite_Tracer plugin;
        ImagePlus imagePlus;
	
	public PathAndFillManager( ) {
                allPaths = new ArrayList< Path >();
                allFills = new ArrayList< Fill >();
                listeners = new ArrayList< PathAndFillListener >();
                selectedPaths = new boolean[0];
	}
	
	public PathAndFillManager( ImagePlus imagePlus ) {
		this();
		this.imagePlus = imagePlus;
		Calibration c = imagePlus.getCalibration();
		this.x_spacing = c.pixelWidth;
		this.y_spacing = c.pixelHeight;
		this.z_spacing = c.pixelDepth;
		this.spacing_units = c.getUnit();
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
	
        boolean [] selectedPaths;
	
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
		
                Hashtable< Path, Integer > pathIndicesHash = new Hashtable< Path, Integer >();
		
                int paths = allPaths.size();
		
                String [] pathListEntries = new String[paths];
		
                for( int i = 0; i < paths; ++i ) {
                        Path p = allPaths.get(i);
                        // if (verbose) System.out.println("path " + i + " is " + (Object)p );
                        pathIndicesHash.put(p,new Integer(i));
                        if( p == null ) {
                                if (verbose) System.out.println("path was null with i "+i+" out of "+paths );
                        }
                        String name = "Path (" + i + ")";
                        if( p.startJoins != null ) {
                                name += ", starts on (" + pathToIndex(p.startJoins) + ")";
                        }
                        if( p.endJoins != null ) {
                                name += ", ends on (" + pathToIndex(p.endJoins) + ")";
                        }
			name += " [" + p.getRealLengthString( x_spacing,
							      y_spacing,
							      z_spacing ) +
				" " + spacing_units + "]";
                        pathListEntries[i] = name;
                }
		
                for( Iterator i = listeners.iterator(); i.hasNext(); ) {
                        ((PathAndFillListener)(i.next())).setPathList( pathListEntries );
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
				
                                for( int j = 0; j < f.sourcePaths.size(); ++j ) {
					
                                        Path p = f.sourcePaths.get(j);
					
                                        if( j != 0 ) {
                                                name += ", ";
                                        }
					
                                        // if (verbose) System.out.println("source path " + j + " is " + (Object)p );
					
                                        Integer fromPath = pathIndicesHash.get( p );
					
                                        if( fromPath == null ) {
                                                // if (verbose) System.out.println("from unknown path");
                                                name += "(unknown)";
                                        } else {
                                                name += "(" + fromPath.intValue() + ")";
                                        }
					
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
	
        public void addFill( Fill fill ) {
		
                allFills.add(fill);
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
	
	public void reloadFill( int index ) {
		
		Fill toReload = allFills.get(index);
		
		plugin.startFillerThread( FillerThread.fromFill( plugin.getImagePlus(),
								 true,
								 toReload ) );
		
	}
	
        synchronized public void writeXML( String fileName,
                                           Simple_Neurite_Tracer plugin,
                                           boolean compress ) throws IOException {
		
                PrintWriter pw = null;
		
                if( compress ) {
			pw = new PrintWriter(new GZIPOutputStream(new FileOutputStream(fileName)));
                } else {
			pw = new PrintWriter(new BufferedWriter(new FileWriter(fileName,false)));
                }
		
                pw.println("<?xml version=\"1.0\"?>");
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
		
                Hashtable< Path, Integer > h =
                        new Hashtable< Path, Integer >();
		
                int pathIndex = 0;
		
                for( Iterator j = allPaths.iterator(); j.hasNext(); ) {
                        Path p = (Path)j.next();
                        h.put( p, new Integer(pathIndex) );
			
			++ pathIndex;
		}
		
		pathIndex = 0;
		
                for( Iterator j = allPaths.iterator(); j.hasNext(); ) {
                        Path p = (Path)j.next();
			
                        // This probably should be a String returning
                        // method of Path.
			
                        pw.print("  <path id=\"" + pathIndex + "\"" );
			
			String startsString = "";
			String endsString = "";
			
			if( p.startJoins != null ) {
				int startPathIndex = ((h.get(p.startJoins))).intValue();
				startsString = " startson=\"" + startPathIndex + "\"" +
					" startsindex=\"" + p.startJoinsIndex + "\"";
			}
			if( p.endJoins != null ) {
				int endPathIndex = ((h.get(p.endJoins))).intValue();
				endsString = " endson=\"" + endPathIndex + "\"" +
					" endsindex=\"" + p.endJoinsIndex + "\"";
			}
			
			pw.print(startsString);
			pw.print(endsString);
			
			pw.print(" reallength=\"" +
				 p.getRealLength(
					 x_spacing,
					 y_spacing,
					 z_spacing ) + "\"");
			
                        pw.println( ">" );
			
                        for( int i = 0; i < p.size(); ++i ) {
				
                                pw.println("    <point x=\"" +
					   p.x_positions[i] + "\" " +
					   "y=\"" + p.y_positions[i] + "\" z=\"" +
					   p.z_positions[i] + "\"/>");
				
                        }
			
                        pw.println( "  </path>" );
			
                        ++ pathIndex;
                }
		
                // Now output the fills:
		
                int fillIndex = 0;
		
                for( Iterator j = allFills.iterator(); j.hasNext(); ) {
			
                        Fill f = (Fill) j.next();
			
                        // This should probably be a method of Fill...
			
                        pw.print( "  <fill id=\"" + fillIndex + "\""  );
			
                        if( (f.sourcePaths != null) && (f.sourcePaths.size() > 0) ) {
				
                                pw.print( " frompaths=\"" );
				
				
                                for( int k = 0; k < f.sourcePaths.size(); ++k ) {
					
                                        Path p = f.sourcePaths.get(k);
					
                                        if( k != 0 ) {
                                                pw.print( ", " );
                                        }
					
                                        Integer fromPath = h.get( p );
					
                                        if( fromPath == null ) {
                                                pw.print( "-1" );
                                        } else {
                                                pw.print( "" + fromPath.intValue() );
                                        }
					
                                }
				
                                pw.print( "\"" );
				
				
				
                        }
			
                        pw.println( " metric=\"" + f.getMetric() + "\" threshold=\"" + f.getThreshold() + "\">" );
			
                        f.writeNodesXML( pw );
			
                        pw.println( "  </fill>" );
			
                }
		
                pw.println("</tracings>");
		
                pw.close();
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
	
        ArrayList< Integer > startJoins;
        ArrayList< Integer > endJoins;
	
	ArrayList< int [] > sourcePathIndicesForFills;
	
	int last_fill_node_id;
	
	int last_fill_id;
	
        @Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		
                int width, height, depth;
		
		if( qName.equals("tracings") ) {
			
                        startJoins = new ArrayList< Integer >();
                        endJoins = new ArrayList< Integer >();
			
			sourcePathIndicesForFills = new ArrayList< int [] >();
			
			last_fill_id = -1;
			
		} else if( qName.equals("imagesize") ) {
			
                        try {
				
                                String widthString = attributes.getValue("width");
                                String heightString = attributes.getValue("height");
                                String depthString = attributes.getValue("depth");
				
                                parsed_width = Integer.parseInt(widthString);
                                parsed_height = Integer.parseInt(heightString);
                                parsed_depth = Integer.parseInt(depthString);
				
                        } catch( NumberFormatException e ) {
				
                                IJ.error("There was an invalid attribute to <imagesize/>: "+e);
                                // FIXME: do something else to abandon this...
				
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
				
                                IJ.error("There was an invalid attribute to <samplespacing/>: "+e);
                                // FIXME: do something else to abandon this...
                        }
			
                } else if( qName.equals("path") ) {
			
                        String idString = attributes.getValue("id");
                        String d = attributes.getValue("d");
			
                        String startsonString =  attributes.getValue("startson");
                        String startsindexString =  attributes.getValue("startsindex");
                        String endsonString =  attributes.getValue("endson");
                        String endsindexString =  attributes.getValue("endsindex");
			
                        if( (startsonString == null && startsindexString != null) ||
                            (startsonString != null && startsindexString == null) ) {
                                IJ.error("If startson is specified for a path, then startsindex must also be specified.");
                                // FIXME: error
                        }
			
                        if( (endsonString == null && endsindexString != null) &&
                            (endsonString != null && endsindexString == null) ) {
                                IJ.error("If endson is specified for a path, then endsindex must also be specified.");
                                // FIXME: error
                        }
			
                        int startson, startsindex, endson, endsindex;
			
                        current_path = new Path();
			
			Integer startsOnInteger = null;
			Integer endsOnInteger = null;
			
                        try {
				
                                int id = Integer.parseInt(idString);
				
                                if( startsonString == null )
                                        startson = startsindex = -1;
                                else {
                                        startson = Integer.parseInt(startsonString);
                                        startsindex = Integer.parseInt(startsindexString);
					
					startsOnInteger = new Integer( startson );
                                        current_path.startJoinsIndex = startsindex;
                                }
				
                                if( endsonString == null )
                                        endson = endsindex = -1;
                                else {
                                        endson = Integer.parseInt(endsonString);
                                        endsindex = Integer.parseInt(endsindexString);
					
					endsOnInteger = new Integer( endson );
                                        current_path.endJoinsIndex = endsindex;
                                }
				
                        } catch( NumberFormatException e ) {
				
                                IJ.error("There was an invalid attribute in <path/>");
                                // FIXME: do something else to abandon this...
                        }			
			
			startJoins.add( startsOnInteger );
			endJoins.add( endsOnInteger );
			
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
				
                                IJ.error("There was an invalid attribute to <imagesize/>");
                                // FIXME: do something else to abandon this...
                        }
			
		} else if( qName.equals("fill") ) {
			
			try {
				
				String fromPathsString = attributes.getValue("frompaths");
				String [] sourcePaths = fromPathsString.split(", *");
				
				current_fill = new Fill();
				
				String metric = attributes.getValue("metric");
				current_fill.setMetric(metric);
				
				last_fill_node_id = -1;
				
				String fill_id_string = attributes.getValue("id");
				
				int fill_id = Integer.parseInt(fill_id_string);
				
				if( fill_id < 0 ) {
					IJ.error("Can't have a negative id in <fill>");
					// FIXME: do something else to abandon this....
				}
				
				if( fill_id != (last_fill_id + 1) ) {
					IJ.error( "Out of order id in <fill> (" + fill_id +
						  " when we were expecting " + (last_fill_id + 1) + ")" );
					// FIXME: do something else to abandon this....
				}
				
				int [] sourcePathIndices = new int[ sourcePaths.length ];
				
				for( int i = 0; i < sourcePaths.length; ++i )
					sourcePathIndices[i] = Integer.parseInt(sourcePaths[i]);
				
				sourcePathIndicesForFills.add( sourcePathIndices );
				
				last_fill_id = fill_id;
				
				String thresholdString = attributes.getValue("threshold");
				double fillThreshold = Double.parseDouble(thresholdString);
				
				current_fill.setThreshold(fillThreshold);
				
                        } catch( NumberFormatException e ) {
				
                                IJ.error("There was an invalid attribute to <fill>");
                                // FIXME: do something else to abandon this...
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
					IJ.error("Fill node IDs weren't consecutive integers");
					// FIXME: 
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
				
                                IJ.error("There was an invalid attribute to <node/>: "+e);
                                // FIXME: do something else to abandon this...
                        }
			
		} else if( qName.equals("tracings") ) {
			
                } else {
			
			IJ.error("Unknown element: '"+qName+"'");
			// FIXME: 
			
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
				Integer startInteger = startJoins.get(i);
				Integer endInteger = endJoins.get(i);
				if( startInteger != null ) {
					int start = startInteger.intValue();
					Path startPath = allPaths.get(start);
					p.startJoins = startPath;
				}
				if( endInteger != null ) {
					int end = endInteger.intValue();	
					Path endPath = allPaths.get(end);	
					p.endJoins = endPath;
				}
			}
			
			// Now turn the source paths into real paths...
			
			for( int i = 0; i < allFills.size(); ++i ) {
				Fill f = allFills.get(i);
				int [] sourcePathIndices = sourcePathIndicesForFills.get(i);
				Path [] realSourcePaths = new Path[sourcePathIndices.length];
				for( int j = 0; j < sourcePathIndices.length; ++j ) {
					realSourcePaths[j] = allPaths.get(sourcePathIndices[j]);
				}
				f.setSourcePaths( realSourcePaths );
			}
			
			setSelected( new int[0] );
			resetListeners();
			plugin.repaintAllPanes();
		}
		
	}
	
	
	public boolean load( Reader reader ) {
		
		try {
			
			InputSource inputSource=new InputSource(reader);
			
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setValidating(true);
			SAXParser parser = factory.newSAXParser();
			
			parser.parse( inputSource, this );	    
			
		} catch( javax.xml.parsers.ParserConfigurationException e ) {
			
                        IJ.error("There was an exception: "+e);
                        return false;
			
                } catch( org.xml.sax.SAXException e ) {
			
                        IJ.error("There was an exception: "+e);
                        return false;
			
                } catch( java.io.FileNotFoundException e ) {
			
                        IJ.error("There was an exception: "+e);
                        return false;
			
                } catch( java.io.IOException e ) {
			
                        IJ.error("There was an exception: "+e);
                        return false;
			
                }
		
                return true;
		
        }
	
	public boolean loadFromString( String tracesFileAsString ) {
		
		StringReader reader=new StringReader(tracesFileAsString);
		return load(reader);
		
	}
	
        public boolean load( InputStream is ) {
		
                try {
			
                        SAXParserFactory factory = SAXParserFactory.newInstance();
                        factory.setValidating(true);
                        SAXParser parser = factory.newSAXParser();
			
                        parser.parse( is, this );
			
                        // FIXME: somehow wait for parsing to end...
			
                } catch( javax.xml.parsers.ParserConfigurationException e ) {
			
                        IJ.error("There was an exception: "+e);
                        return false;
			
                } catch( org.xml.sax.SAXException e ) {
			
                        IJ.error("There was an exception: "+e);
                        return false;
			
                } catch( java.io.FileNotFoundException e ) {
			
                        IJ.error("There was an exception: "+e);
                        return false;
			
                } catch( java.io.IOException e ) {
			
                        IJ.error("There was an exception: "+e);
                        return false;
			
                }
		
                return true;
		
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
		
                InputStream is;
		
                try {
			
			if( gzipped ) {
				if (verbose) System.out.println("Loading gzipped file...");
				is = new GZIPInputStream(new BufferedInputStream(new FileInputStream(filename)));
			} else {
				if (verbose) System.out.println("Loading uncompressed file...");
				is = new BufferedInputStream(new FileInputStream(filename));
			}
			
                } catch( IOException ioe ) {
			IJ.error("Couldn't open file '"+filename+"' for reading.");
			return false;
                }
		
                return load(is);
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
		
                if( ! anySelected() ) {
                        IJ.error( "There are no paths selected, so you can't join (you're holding 'Control')" );
                        return null;
                }
		
                int minimumDistanceSquared = Integer.MAX_VALUE;
		
                int paths = allPaths.size();
		
                for( int s = 0; s < paths; ++s ) {
			
                        if( ! selectedPaths[s] )
                                continue;
			
                        Path p = allPaths.get(s);
			
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
	
}

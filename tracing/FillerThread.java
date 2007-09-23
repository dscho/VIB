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

import java.util.*;

public class FillerThread extends SearchThread {
	
	/* You should synchronize on this object if you want to rely
	 * on the pause status not changing.  (The run() method is not
	 * synchronized itself, for possibly dubious performance
	 * reasons.) */
	
        boolean reciprocal;
	
        float threshold;
	
        double reciprocal_fudge = 0.5;
	
        public void setThreshold( double threshold ) {
                this.threshold = (float)threshold;
        }

	public float getThreshold( ) {
		return threshold;
	}
	
        public float getDistanceAtPoint( int x, int y, int z ) {
		
                SearchNode [] slice = nodes_as_image[z];
                if( slice == null )
                    return -1.0f;
                
		SearchNode n = slice[y*width+x];
		if( n == null )
			return -1.0f;
		else
			return n.g;
		
        }	

        // FIXME: may be buggy, synchronization issues
	
        Fill getFill( ) {
		
                Hashtable< SearchNode, Integer > h =
                        new Hashtable< SearchNode, Integer >();
		
                ArrayList< SearchNode > a =
                        new ArrayList< SearchNode >();
		
                // The tricky bit here is that we want to create a
                // Fill object with index
		
		int openAtOrAbove;

                int i = 0;
			
                for( Iterator<SearchNode> j = closed_from_start.iterator();
                     j.hasNext(); ) {
                        SearchNode current = j.next();
                        /* if( current.g <= threshold ) { */
			h.put( current, new Integer(i) );
			a.add( current );
			++ i;
			/* } */
                }
		
		openAtOrAbove = i;

		System.out.println("openAtOrAbove is: "+openAtOrAbove);
	
                for( Iterator<SearchNode> j = open_from_start.iterator();
                     j.hasNext(); ) {
                        SearchNode current = j.next();
                        /* if( current.g <= threshold ) { */
			h.put( current, new Integer(i) );
			a.add( current );
			++ i;
			/* } */
                }
		
                Fill fill = new Fill();

                fill.setThreshold( threshold );
                if( reciprocal )
                        fill.setMetric( "reciprocal-intensity-scaled" );
                else
                        fill.setMetric( "256-minus-intensity-scaled" );

                fill.setSpacing( x_spacing,
                                 y_spacing,
                                 z_spacing,
                                 spacing_units );

		System.out.println("... out of a.size() "+a.size()+" entries");

                for( i = 0; i < a.size(); ++i ) {
                        SearchNode f = a.get(i);
                        int previousIndex = -1;
                        SearchNode previous = f.getPredecessor();
                        if( previous != null ) {
                                Integer p = h.get(previous);
                                if( p != null ) {
                                        previousIndex = p.intValue();
                                }
                        }
                        fill.add( f.x, f.y, f.z, f.g, previousIndex, i >= openAtOrAbove );
                }

                if( sourcePaths != null ) {
                        Path [] dummy = { };
                        fill.setSourcePaths( sourcePaths.toArray(dummy) );
                }

                return fill;
        }

        ArrayList< Path > sourcePaths;

	public static FillerThread fromFill( ImagePlus imagePlus,
					     boolean startPaused,
					     Fill fill ) {
		
		boolean reciprocal;
		float initialThreshold;
		String metric = fill.getMetric();

		if( metric.equals("reciprocal-intensity-scaled") ) {
			
			reciprocal = true;

		} else if( metric.equals("256-minus-intensity-scaled") ) {

			reciprocal = false;

		} else {

			IJ.error("Trying to load a fill with an unknown metric ('" + metric + "')");
			return null;

		}

		System.out.println("loading a fill with threshold: " + fill.getThreshold() );

		FillerThread result = new FillerThread( imagePlus,
							startPaused,
							reciprocal,
							fill.getThreshold(),
							5000 );

		ArrayList< SearchNode > tempNodes = new ArrayList< SearchNode >();

                for( Iterator it = fill.nodeList.iterator(); it.hasNext(); ) {
                        Fill.Node n = (Fill.Node)it.next();

			SearchNode s = new SearchNode( n.x,
						       n.y,
						       n.z,
						       (float)n.distance,
						       0,						       
						       null,
						       SearchThread.FREE );
			tempNodes.add(s);
		}

		for( int i = 0; i < tempNodes.size(); ++i ) {

			Fill.Node n = fill.nodeList.get(i);
			SearchNode s = tempNodes.get(i);
			if( n.previous >= 0 ) {
				s.setPredecessor( tempNodes.get(n.previous) );
			}

			if( n.open ) {
				s.searchStatus = OPEN_FROM_START;
				result.addNode( s );
			} else {
				s.searchStatus = CLOSED_FROM_START;
				result.addNode( s );
			}

		}
		
		return result;

	}

        /* If you specify 0 for timeoutSeconds then there is no timeout. */

        public FillerThread( ImagePlus imagePlus,
			     boolean startPaused,
                             boolean reciprocal,
                             double initialThreshold,
			     long reportEveryMilliseconds ) {
		
		super( imagePlus,
		       false, // bidirectional
		       false, // definedGoal
		       startPaused,
		       0,
		       reportEveryMilliseconds );

                this.reciprocal = reciprocal;
                setThreshold( initialThreshold );

                long lastThresholdChange = 0;

		setPriority( MIN_PRIORITY );
        }

	public void getSourcePathsFromPlugin( Simple_Neurite_Tracer plugin ) {
		
                PathAndFillManager pathAndFillManager = plugin.getPathAndFillManager();

                // Just get these from the plugin; this thread should be
                // created synchronized...

                sourcePaths = new ArrayList< Path >();

                for( int i = 0; i < pathAndFillManager.size(); ++i ) {

                        if( ! pathAndFillManager.selectedPaths[i] )
                                continue;

                        Path p = pathAndFillManager.getPath(i);

                        sourcePaths.add(p);

                        for( int k = 0; k < p.size(); ++k ) {

                                SearchNode f = new SearchNode( p.x_positions[k],
                                                               p.y_positions[k],
                                                               p.z_positions[k],
                                                               0,
							       0,
                                                               null,
							       OPEN_FROM_START );
				
				addNode(f);
                        }
                }

	}

        public ImagePlus fillAsImagePlus( boolean realData ) {

                byte [][] new_slice_data = new byte[depth][];
                for( int z = 0; z < depth; ++z ) {
                        new_slice_data[z] = new byte[width * height];
                }

                ImageStack stack = new ImageStack(width,height);

                for( int z = 0; z < depth; ++z ) {

			for( int y = 0; y < height; ++y ) {
				for( int x = 0; x < width; ++x ) {
					SearchNode s = nodes_as_image[z][y*width+x];
					if( (s != null) && (s.g <= threshold) ) {
						new_slice_data[z][y*width+x] = realData ? slices_data[z][y*width+x] : (byte)255;
					}
				}
			}

                        ByteProcessor bp = new ByteProcessor(width,height);
                        bp.setPixels( new_slice_data[z] );
                        stack.addSlice(null,bp);
                }

                ImagePlus imp=new ImagePlus("filled neuron",stack);

                imp.setCalibration(imagePlus.getCalibration());

		return imp;
        }

	@Override
	protected void reportPointsInSearch() {
		
		super.reportPointsInSearch();
		
		// Find the minimum distance in the open list.
		SearchNode p = open_from_start.peek();
		if( p == null )
			return;
		
		float minimumDistanceInOpen = p.g;
		
		for (Iterator<SearchProgressCallback> j = progressListeners.iterator(); j.hasNext();) {
			SearchProgressCallback progress = j.next();
			if( progress instanceof FillerProgressCallback ) {
				FillerProgressCallback fillerProgress = (FillerProgressCallback)progress;
				fillerProgress.maximumDistanceCompletelyExplored( this, minimumDistanceInOpen );
			}
            }

	}
	
	

}
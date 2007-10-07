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

import java.util.*;
import java.io.*;

public class Fill {
	
        public double distanceThreshold;
	
        public class Node {
                public int x;
                public int y;
                public int z;
                public double distance;
                public int previous;
		public boolean open;
        }
	
        ArrayList< Node > nodeList;
	
        public Fill( ) {
                nodeList = new ArrayList< Node >();
        }
	
        public void add( int x, int y, int z, double distance, int previous, boolean open ) {
                Node n = new Node();
                n.x = x;
                n.y = y;
                n.z = z;
                n.distance = distance;
                n.previous = previous;
		n.open = open;
                nodeList.add(n);
        }
	
        ArrayList< Path > sourcePaths;
	
        public void setSourcePaths( Path [] sourcePaths ) {
                this.sourcePaths = new ArrayList< Path >();
                for( int i = 0; i < sourcePaths.length; ++i ) {
                        this.sourcePaths.add( sourcePaths[i] );
                }
        }
	
        public String metric;
	
        public void setMetric( String metric ) {
                this.metric = metric;
        }
	
        public String getMetric( ) {
                return metric;
        }
	
        public double x_spacing, y_spacing, z_spacing;
        public String spacing_units;
	
        public void setSpacing( double x_spacing, double y_spacing, double z_spacing, String units ) {
                this.x_spacing = x_spacing;
                this.y_spacing = y_spacing;
                this.z_spacing = z_spacing;
                this.spacing_units = units;
        }
	
        public void setThreshold( double threshold ) {
                this.distanceThreshold = threshold;
        }
	
        public double getThreshold( ) {
                return distanceThreshold;
        }
	
        public void writeNodesXML( PrintWriter pw ) {
		
                int i = 0;
                for( Iterator it = nodeList.iterator(); it.hasNext(); ++i ) {
                        Node n = (Node)it.next();
                        pw.println( "    <node id=\"" + i + "\" " +
                                    "x=\"" + n.x + "\" " +
                                    "y=\"" + n.y + "\" " +
                                    "z=\"" + n.z + "\" " +
                                    ((n.previous >= 0) ? "previousid=\"" + n.previous + "\" " : "") +
                                    "distance=\"" + n.distance + "\" status=\"" + (n.open ? "open" : "closed") + "\"/>" );
                }
        }
}

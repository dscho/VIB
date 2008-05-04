/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007 Mark Longair */

/*
  This file is part of the ImageJ plugin "Auto Tracer".
  
  The ImageJ plugin "Auto Tracer" is free software; you can
  redistribute it and/or modify it under the terms of the GNU General
  Public License as published by the Free Software Foundation; either
  version 3 of the License, or (at your option) any later version.
  
  The ImageJ plugin "Auto Tracer" is distributed in the hope that it
  will be useful, but WITHOUT ANY WARRANTY; without even the implied
  warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  See the GNU General Public License for more details.
  
  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package tracing;

import java.util.Hashtable;

public class SinglePathsGraph {
	
	int width, height, depth;
	
	public SinglePathsGraph( int width, int height, int depth ) {
		this.width = width;
		this.height = height;
		this.depth = depth;
	}
	
	// For fast lookup from positions:
	Hashtable<Integer,AutoPoint> fromPosition=new Hashtable<Integer,AutoPoint>();
	
	public AutoPoint get( int x, int y, int z ) {
		int k = x + y * width + z * width * height;
		return fromPosition.get(k);
	}
	
	public void addPoint( AutoPoint p ) {
		int k = p.x + p.y * width + p.z * width * height;
		AutoPoint existingPoint=fromPosition.get( k );
		if( existingPoint == null ) {
			fromPosition.put(k,p);
		} else {
			// "merge" this point with the exisiting one -
			// i.e. just add the predecessors:
			existingPoint.addPredecessors(p.predecessors);
		}
	}	
}

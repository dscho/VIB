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

public class AutoPoint {
	public int x;
	public int y;
	public int z;
	public boolean overThreshold = false;
	public AutoPoint [] predecessors;
	public AutoPoint(int x,int y,int z) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.predecessors = null;
	}
	@Override
	public String toString() {
		return "("+x+","+y+","+z+")";
	}
	@Override
	public boolean equals(Object o) {
		AutoPoint op=(AutoPoint)o;
		// System.out.println("Testing equality between "+this+" and "+op);
		boolean result = (this.x == op.x) && (this.y == op.y) && (this.z == op.z);
		return result;
	}
	public void addPredecessor(AutoPoint p) {
		if( predecessors == null ) {
			predecessors = new AutoPoint[1];
			predecessors[0] = p;
		} else {
			for( int i = 0; i < predecessors.length; ++i )
				if( p.equals( predecessors[i] ) )
					return;
			AutoPoint [] n = new AutoPoint[predecessors.length+1];
			System.arraycopy(predecessors,0,n,0,predecessors.length);
			n[predecessors.length] = p;
			predecessors = n;
		}
	}
	public void addPredecessors(AutoPoint [] newPredecessors) {
		if( newPredecessors == null )
			return;
		if( predecessors == null ) {
			predecessors = new AutoPoint[newPredecessors.length];
			System.arraycopy(newPredecessors,0,predecessors,0,newPredecessors.length);
		} else {
			AutoPoint [] n = new AutoPoint[predecessors.length+newPredecessors.length];
			System.arraycopy(predecessors,0,n,0,predecessors.length);
			System.arraycopy(newPredecessors,0,n,predecessors.length,newPredecessors.length);
			predecessors = n;
		}
	}
}

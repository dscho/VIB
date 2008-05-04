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
import java.awt.*;

import ij.ImagePlus;

import stacks.ThreePanesCanvas;
import stacks.PaneOwner;
import stacks.ThreePanes;

import util.Arrow;

public class TracerCanvas extends ThreePanesCanvas {

	private int maxArrows = 4;
	private Arrow[] arrows = new Arrow[maxArrows];

	public void setArrow( int i, Arrow a ) {
		arrows[i] = a;
	}
	
	public Arrow getArrow( int i ) {
		return arrows[i];
	}
	
	public void unsetArrows( ) {
		for( int i = 0; i < maxArrows; ++i )
			arrows[i] = null;
	}	
	
	private PathAndFillManager pathAndFillManager;

	public TracerCanvas( ImagePlus imagePlus,
			     PaneOwner owner,
			     int plane,
			     PathAndFillManager pathAndFillManager ) {

		super( imagePlus, owner, plane );
		this.pathAndFillManager = pathAndFillManager;
	}

	ArrayList<SearchThread> searchThreads = new ArrayList<SearchThread>();

	void addSearchThread( SearchThread s ) {
		synchronized (searchThreads) {
			searchThreads.add( s );
		}
	}

	void removeSearchThread( SearchThread s ) {
		synchronized (searchThreads) {
			int index = -1;
			for( int i = 0; i < searchThreads.size(); ++i ) {
				SearchThread inList = searchThreads.get(i);
				if( s == inList )
					index = i;
			}
			if( index >= 0 )
				searchThreads.remove( index );
		}
	}

	boolean just_near_slices = false;
	int eitherSide;

	@Override
	protected void drawOverlay(Graphics g) {

		for( int i = maxArrows - 1; i >= 0; --i ) {
			// for( int i = 0; i < maxArrows; ++i ) {
			
			Arrow a = arrows[i];
			if( a == null )
				continue;
			
			g.setColor(a.c);
			
			if( plane == ThreePanes.XY_PLANE ) {
				g.drawLine( (int)( a.start_x ),
					    (int)( a.start_y ),
					    (int)( a.start_x + a.length * a.vx ),
					    (int)( a.start_y + a.length * a.vy ) );
			} else if( plane == ThreePanes.XZ_PLANE ) {
				g.drawLine( (int)( a.start_x ),
					    (int)( a.start_z ),
					    (int)( a.start_x + a.length * a.vx ),
					    (int)( a.start_z + a.length * a.vz ) );
			} else if( plane == ThreePanes.ZY_PLANE ) {
				g.drawLine( (int)( a.start_z ),
					    (int)( a.start_y ),
					    (int)( a.start_z + a.length * a.vz ),
					    (int)( a.start_y + a.length * a.vy ) );
			}
		}

		/*
		int current_z = -1;
		
		if( plane == ThreePanes.XY_PLANE ) {
			current_z = imp.getCurrentSlice() - 1;
		}
		*/

		int current_z = imp.getCurrentSlice() - 1;
	
		synchronized (searchThreads) {
			for( Iterator<SearchThread> i = searchThreads.iterator(); i.hasNext(); )
				i.next().drawProgressOnSlice( plane, current_z, this, g );
		}

		if( pathAndFillManager != null ) {
			for( int i = 0; i < pathAndFillManager.size(); ++i ) {
			
				Path p = pathAndFillManager.getPath(i);
				if( p == null )
					continue;
			
				Color color = Color.MAGENTA;
				if( pathAndFillManager.isSelected(i) ) {
					color = Color.GREEN;
				}
			
				if( just_near_slices ) {
					p.drawPathAsPoints( this, g, color, plane, current_z, eitherSide );
				} else
					p.drawPathAsPoints( this, g, color, plane );
			
			}
		}

		super.drawOverlay(g);

	}


}

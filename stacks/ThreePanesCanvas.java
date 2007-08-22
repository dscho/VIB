/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007 Mark Longair */

/*
    This file is part of the ImageJ plugins "Simple Neurite Tracer"
    and "Three Pane Crop".

    The ImageJ plugins "Three Pane Crop" and "Simple Neurite Tracer"
    are free software; you can redistribute them and/or modify them
    under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 3 of the License, or
    (at your option) any later version.

    The ImageJ plugins "Simple Neurite Tracer" and "Three Pane Crop"
    are distributed in the hope that they will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
    or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public
    License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package stacks;

import ij.*;
import ij.gui.*;

import java.awt.*;
import java.awt.event.*;

public class ThreePanesCanvas extends ImageCanvas {
	
	protected PaneOwner owner;
	protected int plane;
	
	protected ThreePanesCanvas( ImagePlus imagePlus, PaneOwner owner, int plane ) {
		super(imagePlus);
		this.owner = owner;
		this.plane = plane;
	}
	
	static public Object newThreePanesCanvas( ImagePlus imagePlus, PaneOwner owner, int plane ) {
		return new ThreePanesCanvas( imagePlus, owner, plane );
	}
	
	protected void drawOverlay( Graphics g ) {
		
		if( draw_crosshairs ) {
			
			if( plane == ThreePanes.XY_PLANE ) {
				int x = screenX(current_x);
				int y = screenY(current_y);
				int x_pixel_width = screenX(current_x+1) - x;
				int y_pixel_width = screenY(current_y+1) - y;
				drawCrosshairs( g, Color.red, x + (x_pixel_width / 2), y + (y_pixel_width / 2) );
			} else if( plane == ThreePanes.XZ_PLANE ) {
				int x = screenX(current_x);
				int y = screenY(current_z);
				int x_pixel_width = screenX(current_x+1) - screenX(current_x);
				int y_pixel_width = screenY(current_z+1) - screenY(current_z);
				drawCrosshairs( g, Color.red, x + (x_pixel_width / 2), y + (y_pixel_width / 2) );
			} else if( plane == ThreePanes.ZY_PLANE ) {
				int x = screenX(current_z);
				int y = screenY(current_y);
				int x_pixel_width = screenX(current_z+1) - screenX(current_z);
				int y_pixel_width = screenY(current_y+1) - screenY(current_y);
				drawCrosshairs( g, Color.red, x + (x_pixel_width / 2), y + (y_pixel_width / 2)  );
			}
			
		}
		
	}
	
	public void paint(Graphics g) {
		super.paint(g);
		drawOverlay(g);
	}
	
	public void mouseClicked( MouseEvent e ) {
		
	}
	
	protected void drawCrosshairs( Graphics g, Color c, int x_on_screen, int y_on_screen ) {
		g.setColor( c );
		int hairLength = 8;
		g.drawLine( x_on_screen, y_on_screen + 1, x_on_screen, y_on_screen + (hairLength - 1) );
		g.drawLine( x_on_screen, y_on_screen - 1, x_on_screen, y_on_screen - (hairLength - 1) );
		g.drawLine( x_on_screen + 1, y_on_screen, x_on_screen + (hairLength - 1), y_on_screen );
		g.drawLine( x_on_screen - 1, y_on_screen, x_on_screen - (hairLength - 1), y_on_screen );
	}
	
	public void setCrosshairs( int x, int y, int z, boolean display ) {
		current_x = x;
		current_y = y;
		current_z = z;
		draw_crosshairs = display;
	}
	
	private int current_x, current_y, current_z;
	boolean draw_crosshairs;
	
   	public void mouseMoved(MouseEvent e) {
		
		int off_screen_x = offScreenX(e.getX());
		int off_screen_y = offScreenY(e.getY());

		boolean shift_key_down = (e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0;
		
		owner.mouseMovedTo( off_screen_x, off_screen_y, plane, shift_key_down );
		
	}
	
}

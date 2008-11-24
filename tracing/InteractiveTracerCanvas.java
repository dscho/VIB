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
import ij.gui.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import stacks.ThreePanesCanvas;
import stacks.ThreePanes;

public class InteractiveTracerCanvas extends TracerCanvas implements KeyListener {

	static final boolean verbose = Simple_Neurite_Tracer.verbose;

	boolean fillTransparent = false;

	Color transparentGreen = new Color( 0, 128, 0, 128 );

	public void setFillTransparent( boolean transparent ) {
		this.fillTransparent = transparent;
	}

	// -------------------------------------------------------------

	private Simple_Neurite_Tracer tracerPlugin;

	public Simple_Neurite_Tracer getTracerPlugin() {
		return tracerPlugin;
	}

	InteractiveTracerCanvas( ImagePlus imp, Simple_Neurite_Tracer plugin, int plane, PathAndFillManager pathAndFillManager ) {
		super(imp,plugin,plane,pathAndFillManager);
		tracerPlugin = plugin;
		// Simple_Neurite_Tracer.toastKeyListeners( IJ.getInstance(), "InteractiveTracerCanvas constructor" );
		// addKeyListener( this );
	}

	private Path unconfirmedSegment;
	private Path currentPath;
	private boolean lastPathUnfinished;

	public void setPathUnfinished( boolean unfinished ) {
		this.lastPathUnfinished = unfinished;
	}

	public void setTemporaryPath( Path path ) {
		this.unconfirmedSegment = path;
	}

	public void setCurrentPath( Path path ) {
		this.currentPath = path;
	}

	public void keyPressed(KeyEvent e) {

		int keyCode = e.getKeyCode();
		char keyChar = e.getKeyChar();

		boolean mac = IJ.isMacintosh();

		boolean shift_down = (keyCode == KeyEvent.VK_SHIFT);
		boolean join_modifier_down = mac ? keyCode == KeyEvent.VK_ALT : keyCode == KeyEvent.VK_CONTROL;

		if (verbose) System.out.println("keyCode=" + keyCode + " (" + KeyEvent.getKeyText(keyCode)
						+ ") keyChar=\"" + keyChar + "\" (" + (int)keyChar + ") "
						+ KeyEvent.getKeyModifiersText(flags));

		if( keyChar == 'y' || keyChar == 'Y' ) {

			// if (verbose) System.out.println( "Yes, running confirmPath" );
			tracerPlugin.confirmTemporary( );

		} else if( keyCode == KeyEvent.VK_ESCAPE ) {

			// if (verbose) System.out.println( "Yes, running cancelPath+" );
			tracerPlugin.cancelTemporary( );

		} else if( keyChar == 'f' || keyChar == 'F' ) {

			// if (verbose) System.out.println( "Finalizing that path" );
			tracerPlugin.finishedPath( );

		} else if( keyChar == 'v' || keyChar == 'V' ) {

			// if (verbose) System.out.println( "View paths as a stack" );
			tracerPlugin.makePathVolume( );

		} else if( keyChar == '5' ) {

			just_near_slices = ! just_near_slices;

		} else if( shift_down || join_modifier_down ) {

			tracerPlugin.mouseMovedTo( last_x_in_pane, last_y_in_pane, plane, shift_down, join_modifier_down );

		}

		e.consume();
	}

	public void keyReleased(KeyEvent e) {}

	public void keyTyped(KeyEvent e) {}

	@Override
	public void mouseMoved( MouseEvent e ) {

		last_x_in_pane = offScreenX(e.getX());
		last_y_in_pane = offScreenY(e.getY());

		boolean mac = IJ.isMacintosh();

		boolean shift_key_down = (e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0;
		boolean joiner_modifier_down = mac ? ((e.getModifiersEx() & InputEvent.ALT_DOWN_MASK) != 0) : ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0);

		tracerPlugin.mouseMovedTo( last_x_in_pane, last_y_in_pane, plane, shift_key_down, joiner_modifier_down );
	}

	int last_x_in_pane;
	int last_y_in_pane;

	@Override
	public void mouseClicked( MouseEvent e ) {

		int currentState = tracerPlugin.resultsDialog.getState();

		if( currentState == NeuriteTracerResultsDialog.LOADING ||
		    currentState == NeuriteTracerResultsDialog.SAVING ) {

			// Do nothing

		} else if( currentState == NeuriteTracerResultsDialog.WAITING_FOR_SIGMA_POINT ) {

			tracerPlugin.launchPaletteAround(
				offScreenX(e.getX()),
				offScreenY(e.getY()),
				imp.getCurrentSlice() - 1 );

		} else if( currentState == NeuriteTracerResultsDialog.WAITING_FOR_SIGMA_CHOICE	) {

			IJ.error( "You must close the sigma palette to continue" );

		} else if( tracerPlugin.setupTrace ) {
			boolean join = IJ.isMacintosh() ? e.isAltDown() : e.isControlDown();
			tracerPlugin.clickForTrace( offScreenX(e.getX()), offScreenY(e.getY()), plane, join );
		} else
			IJ.error( "BUG: No operation chosen" );
	}

	@Override
	protected void drawOverlay(Graphics g) {

		if( tracerPlugin.loading )
		    return;

		FillerThread filler = tracerPlugin.filler;
		if( filler != null ) {
			filler.setDrawingColors( fillTransparent ? transparentGreen : Color.GREEN,
						 fillTransparent ? transparentGreen : Color.GREEN );
			filler.setDrawingThreshold( filler.getThreshold() );
		}

		super.drawOverlay(g);

		int pixel_size = (int)getMagnification();
		if( pixel_size < 1 )
			pixel_size = 1;

		int spotExtra = 2 * pixel_size;
		int spotDiameter = 5 * pixel_size;

		if( unconfirmedSegment != null ) {
			unconfirmedSegment.drawPathAsPoints( this, g, Color.BLUE, plane );

			if( unconfirmedSegment.endJoins != null ) {

				int n = unconfirmedSegment.size();

				int x = screenX(unconfirmedSegment.getXUnscaled(n-1));
				int y = screenY(unconfirmedSegment.getYUnscaled(n-1));

				int rectX = x - spotExtra;
				int rectY = y - spotExtra;

				g.setColor(Color.BLUE);
				g.fillRect( rectX, rectY, spotDiameter, spotDiameter );

				g.setColor(Color.GREEN);
				g.drawRect( rectX, rectY, spotDiameter, spotDiameter );
			}
		}

		Path currentPathFromTracer = tracerPlugin.getCurrentPath();

		if( currentPathFromTracer != null ) {
			if( just_near_slices )
				currentPathFromTracer.drawPathAsPoints( this, g, Color.RED, plane, imp.getCurrentSlice() - 1, eitherSide );
			else
				currentPathFromTracer.drawPathAsPoints( this, g, Color.RED, plane );

			if( lastPathUnfinished && currentPath.size() == 0 ) {

				int x = screenX(tracerPlugin.last_start_point_x);
				int y = screenY(tracerPlugin.last_start_point_y);

				int rectX = x - spotExtra;
				int rectY = y - spotExtra;

				g.setColor(Color.BLUE);
				g.fillRect( rectX, rectY, spotDiameter, spotDiameter );

				if( currentPathFromTracer.startJoins != null ) {
					g.setColor(Color.GREEN);
					g.drawRect( rectX, rectY, spotDiameter, spotDiameter );
				}

			}
		}

	}

}

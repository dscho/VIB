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
	
	/* Keep another Graphics for double-buffering... */
	
	private int backBufferWidth;
	private int backBufferHeight;
	
	private Graphics backBufferGraphics;
	private Image backBufferImage;
	
	private void resetBackBuffer() {
		
		if(backBufferGraphics!=null){
			backBufferGraphics.dispose();
			backBufferGraphics=null;
		}
		
		if(backBufferImage!=null){
			backBufferImage.flush();
			backBufferImage=null;
		}
		
		backBufferWidth=getSize().width;
		backBufferHeight=getSize().height;
		
		backBufferImage=createImage(backBufferWidth,backBufferHeight);
	        backBufferGraphics=backBufferImage.getGraphics();
	}
	
	@Override
	public void paint(Graphics g) {
		
		if(backBufferWidth!=getSize().width ||
		   backBufferHeight!=getSize().height ||
		   backBufferImage==null ||
		   backBufferGraphics==null)
			resetBackBuffer();
		
		super.paint(backBufferGraphics);
		drawOverlay(backBufferGraphics);
		g.drawImage(backBufferImage,0,0,this);
	}
	
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
		
		// IJ.showStatus( "click at " + System.currentTimeMillis() + ": " + e );
		// IJ.showStatus( "click at (" + e.getX() + "," + e.getY() + ")" );
		
		if( tracerPlugin.setupLog ) {
			
			int x = offScreenX(e.getX());
			int y = offScreenY(e.getY());
			
			int z = imp.getCurrentSlice() - 1;
			
			tracerPlugin.setPositionAllPanes( x, y, z );
			
			/* FIXME: put this back at some point
			   
			   EigenResultsDouble er;
			   try {
			   er = tracerPlugin.hessianAnalyzer.analyzeAtPoint( x, y, z, 2, 1.0f, false );
			   } catch( Exception exception ) {
			   IJ.error("Caught an exception while calculating the Hessian: "+exception);
			   return;
			   }
			   
			   tracerPlugin.logPosition( x, y, z, er.sortedValues[0], er.sortedValues[1], er.sortedValues[2] );
			   
			*/
			
		} else if( tracerPlugin.setupEv ) {
			
			/* FIXME: put this back at some point
			   
			   int x = offScreenX(e.getX());
			   int y = offScreenX(e.getY());
			   int z = imp.getCurrentSlice() - 1;
			   
			   EigenResultsDouble er_2_around;
			   EigenResultsDouble er_1_around;
			   
			   try {
			   er_2_around = tracerPlugin.hessianAnalyzer.analyzeAtPoint( x, y, z, 2, 1.0f, false );
			   er_1_around = tracerPlugin.hessianAnalyzer.analyzeAtPoint( x, y, z, 1, 1.0f, false );
			   } catch( Exception exception ) {
			   IJ.error("Caught an exception while calculating the Hessian: "+exception);
			   return;
			   }			
			   
			   Arrow arrow_1_around = new Arrow( Color.ORANGE, x, y, z,
			   er_1_around.sortedVectors[0][0],
			   er_1_around.sortedVectors[0][1],
			   er_1_around.sortedVectors[0][2],
			   100 );
			   
			   Arrow arrow_2_around = new Arrow( Color.RED, x, y, z,
			   er_2_around.sortedVectors[0][0],
			   er_2_around.sortedVectors[0][1],
			   er_2_around.sortedVectors[0][2],
			   100 );
			   
			   tracerPlugin.setArrow( 0, arrow_2_around );
			   tracerPlugin.setArrow( 1, arrow_1_around );
			   
			*/
			
		} else if( tracerPlugin.setupTrace ) {
			                    
			boolean join = IJ.isMacintosh() ? e.isAltDown() : e.isControlDown();
			
			tracerPlugin.clickForTrace( offScreenX(e.getX()), offScreenY(e.getY()), plane, join );
			
		} else {
			
			IJ.error( "BUG: No operation chosen" );
			
		}
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

		if( unconfirmedSegment != null ) {
			unconfirmedSegment.drawPathAsPoints( this, g, Color.BLUE, plane );
		}
		
		Path currentPathFromTracer = tracerPlugin.getCurrentPath();
		
		if( currentPathFromTracer != null ) {
			if( just_near_slices )
				currentPathFromTracer.drawPathAsPoints( this, g, Color.RED, plane, imp.getCurrentSlice() - 1, eitherSide );
			else
				currentPathFromTracer.drawPathAsPoints( this, g, Color.RED, plane );
		}
		
	}
	
}

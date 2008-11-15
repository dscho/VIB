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

class NormalPlaneCanvas extends ImageCanvas {

	public NormalPlaneCanvas( ImagePlus imp,
				  Simple_Neurite_Tracer plugin,
				  double [] centre_x_positions,
				  double [] centre_y_positions,
				  double [] radiuses,
				  double [] scores,
				  Path fittedPath ) {
		super(imp);
		tracerPlugin = plugin;
		this.centre_x_positions = centre_x_positions;
		this.centre_y_positions = centre_y_positions;
		this.radiuses = radiuses;
		this.scores = scores;
		this.fittedPath = fittedPath;
		System.out.println("Created NormalPlaneCanvas");
		int slices = imp.getStackSize();
		System.out.println("slices in ImagePlus: "+slices);
		System.out.println("centre_x_positions.length: "+centre_x_positions.length);
		System.out.println("centre_y_positions.length: "+centre_y_positions.length);
		System.out.println("radiuses.length: "+radiuses.length);
		for( int i = 0; i < scores.length; ++i )
			if( scores[i] > maxScore )
				maxScore = scores[i];
	}

	double maxScore = -1;

	double [] centre_x_positions;
	double [] centre_y_positions;
	double [] radiuses;
	double [] scores;

	Path fittedPath;

	Simple_Neurite_Tracer tracerPlugin;

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

	int last_slice = -1;

	protected void drawOverlay(Graphics g) {

		int z = imp.getCurrentSlice() - 1;

		if( z != last_slice ) {
			int [] point = new int[3];
			fittedPath.getPoint( z, point );
			tracerPlugin.setSlicesAllPanes( point[0], point[1], point[2] );
			tracerPlugin.setCrosshair( point[0], point[1], point[2] );
			last_slice = z;
		}

		g.setColor(Color.RED);

		int x_top_left = screenXD( centre_x_positions[z] - radiuses[z] );
		int y_top_left = screenYD( centre_y_positions[z] - radiuses[z] );

		g.fillRect( screenXD(centre_x_positions[z])-2,
			    screenYD(centre_y_positions[z])-2,
			    5,
			    5 );

		int diameter = screenXD(centre_x_positions[z] + radiuses[z]) - screenXD(centre_x_positions[z] - radiuses[z]);

		g.drawOval( x_top_left, y_top_left, diameter, diameter );

		double proportion = scores[z] / maxScore;
		int drawToX = (int)( proportion * ( imp.getWidth() - 1 ) );
		g.setColor(Color.GREEN);
		g.fillRect( screenX(0),
			    screenY(0),
			    screenX(drawToX) - screenX(0),
			    screenY(2) - screenY(0) );

	}

}

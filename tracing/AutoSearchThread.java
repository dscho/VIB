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

import ij.ImagePlus;

import java.util.PriorityQueue;

public class AutoSearchThread extends SearchThread {

	float [][] tub

	public AutoSearchThread(ImagePlus image,
				ImagePlus tubeness,
				Auto_Tracer.Point startPoint,
				PriorityQueue<Auto_Tracer.Point> mostTubelikePoints ) {
		super(
			image,  // Image to trace
			false,  // bidirectional
			false,  // definedGoal
			false,  // startPaused
			0,      // timeoutSeconds
			1000 ); // reportEveryMilliseconds
		
		
		
	}
	
	
	
	
	
	
	
}

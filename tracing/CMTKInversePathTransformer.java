/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007, 2008, 2009 Mark Longair */

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

import util.CMTK_Transformation;

/* And now some helpful implementations: */

public class CMTKInversePathTransformer implements PathTransformer {

	private CMTK_Transformation.Inverse t;

	public CMTKInversePathTransformer( CMTK_Transformation.Inverse t ) {
		this.t = t;
	}

	public void transformPoint( double modelX, double modelY, double modelZ, double [] transformed ) {
		t.transformPoint( modelX, modelY, modelZ, transformed );
	}

	public void transformPoint( double modelX, double modelY, double modelZ, int [] transformed ) {
		t.transformPoint( modelX, modelY, modelZ, transformed );
	}

	public void transformPoint( int modelX, int modelY, int modelZ, int [] transformed ) {
		t.transformPoint( modelX, modelY, modelZ, transformed );
	}

	public void transformPoint( int modelX, int modelY, int modelZ, double [] transformed ) {
		t.transformPoint( modelX, modelY, modelZ, transformed );
	}

}

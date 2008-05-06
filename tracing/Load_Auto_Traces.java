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

import ij.*;
import ij.plugin.*;
import ij.gui.*;
import ij.io.*;
import ij.process.*;

import java.io.*;
import java.awt.*;
import java.util.StringTokenizer;

public class Load_Auto_Traces implements PlugIn {

	public int getNonNegativeIntAfterPrefix( String line, String prefix ) {
		try {
			String s=line.substring(prefix.length());
			return Integer.parseInt(s);
		} catch( NumberFormatException e ) {
			return -1;
		}
	}

	public float getNonNegativeFloatAfterPrefix( String line, String prefix ) {
		try {
			String s=line.substring(prefix.length());
			return Float.parseFloat(s);
		} catch( NumberFormatException e ) {
			return -1;
		}
	}

	public void run( String ignored ) {

                OpenDialog od;
		
                od = new OpenDialog("Select traces.obj file...",
                                    null,
                                    null );
		
                String fileName = od.getFileName();
                String directory = od.getDirectory();
		
                if( fileName == null )
			return;
		
		System.out.println("Got "+fileName);

		int width = -1, height = -1, depth = -1;
		float spacing_x = Float.MIN_VALUE;
		float spacing_y = Float.MIN_VALUE;
		float spacing_z = Float.MIN_VALUE;

		try {
			String widthPrefix = "# width: ";
			String heightPrefix = "# height: ";
			String depthPrefix = "# depth: ";
			String spacingXPrefix = "# spacing_x: ";
			String spacingYPrefix = "# spacing_y: ";
			String spacingZPrefix = "# spacing_z: ";

			String vertexPrefix = "v ";
			String linePrefix = "l ";
			String groupPrefix = "g ";

			byte [][] values = null;

			BufferedReader br = new BufferedReader(new FileReader(directory + fileName));
			String lastLine;
			while( null != (lastLine = br.readLine()) ) {
				
				if( lastLine.startsWith(widthPrefix) )
					width = getNonNegativeIntAfterPrefix(lastLine, widthPrefix);
				if( lastLine.startsWith(heightPrefix) )
					height = getNonNegativeIntAfterPrefix(lastLine, heightPrefix);
				if( lastLine.startsWith(depthPrefix) )
					depth = getNonNegativeIntAfterPrefix(lastLine, depthPrefix);
				if( lastLine.startsWith(spacingXPrefix) )
					spacing_x = getNonNegativeFloatAfterPrefix(lastLine, spacingXPrefix);
				if( lastLine.startsWith(spacingYPrefix) )
					spacing_y = getNonNegativeFloatAfterPrefix(lastLine, spacingYPrefix);
				if( lastLine.startsWith(spacingZPrefix) )
					spacing_z = getNonNegativeFloatAfterPrefix(lastLine, spacingZPrefix);

				if( lastLine.startsWith(vertexPrefix) ) {

					if( values == null ) {
						if( width < 0 ||
						    height < 0 ||
						    depth < 0 ||
						    spacing_x == Float.MIN_VALUE ||
						    spacing_y == Float.MIN_VALUE ||
						    spacing_z == Float.MIN_VALUE ) {
				
							IJ.error("Some metadata was missing from the comments before the first vertex.");
							return;
						}
						values = new byte[depth][];
						for( int z = 0; z < depth; ++z )
							values[z] = new byte[width*height];
					}
						
					StringTokenizer tokenizer=new StringTokenizer(lastLine.substring(vertexPrefix.length())," ");
					
					float [] vertex = new float[3];

					int i = 0;
					while( tokenizer.hasMoreTokens() ) {
						vertex[i] = Float.parseFloat( tokenizer.nextToken() );
						++i;
					}

					int x = (int)Math.round(vertex[0]);
					int y = (int)Math.round(vertex[1]);
					int z = (int)Math.round(vertex[2]);

					values[z][y*width+x] = (byte)255;

				}

				if( lastLine.startsWith(linePrefix) ) {
					// Do nothing - in fact we
					// expect the lines to only
					// jump from vertex to an
					// adjacent one, so we don't
					// really care...
				}

				if( lastLine.startsWith(groupPrefix) )
					; // Do nothing
			}
			
			ImageStack stack = new ImageStack(width,height);

			for( int z = 0; z < depth; ++z ) {
				ByteProcessor bp = new ByteProcessor(width,height);
				bp.setPixels(values[z]);
				stack.addSlice("",bp);
			}

			ImagePlus imagePlus=new ImagePlus(fileName,stack);
			imagePlus.show();


		} catch( IOException e ) {
			IJ.error("IOException loading "+directory+fileName+": "+e);
			return;
		}

		
	}



}


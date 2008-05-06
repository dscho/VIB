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
import java.util.Set;
import java.util.Iterator;
import java.io.*;
import java.util.zip.GZIPOutputStream;

public class SinglePathsGraph {
	
	int width, height, depth;
	float spacing_x, spacing_y, spacing_z;

	public SinglePathsGraph( int width, int height, int depth, double spacing_x, double spacing_y, double spacing_z ) {
		this.width = width;
		this.height = height;
		this.depth = depth;
		this.spacing_x = (float)spacing_x;
		this.spacing_y = (float)spacing_y;
		this.spacing_z = (float)spacing_z;
		this.directedLinksCapacity = 256;
		this.directedLinksInArray = 0;
		this.directedLinks = new int[directedLinksCapacity*2];
	}

	// For fast lookup from positions:
	Hashtable<Integer,AutoPoint> fromPosition=new Hashtable<Integer,AutoPoint>();
	
	public AutoPoint get( int x, int y, int z ) {
		int k = x + y * width + z * width * height;
		return fromPosition.get(k);
	}
	
	public void addPoint( AutoPoint p, AutoPoint previous ) {
		int k = (int)p.x + (int)p.y * width + (int)p.z * width * height;
		AutoPoint existingPoint=fromPosition.get( k );
		if( existingPoint == null ) {
			fromPosition.put(k,p);
		}
		if( previous != null ) {
			int k_previous = (int)previous.x + (int)previous.y * width + (int)previous.z * width * height;
			addLink( k_previous, k );
		}
	}

	// This all turns out to be very memory hungry, so keep track
	// of the links between points here:

	int directedLinksInArray;
	int directedLinksCapacity;

	int [] directedLinks;

	void addLink( int from_k, int to_k ) {
		if( directedLinksInArray == directedLinksCapacity ) {
			int newCapacity = (directedLinksCapacity * 5) / 4;
			int [] n = new int[newCapacity*2];
			System.arraycopy(directedLinks,0,n,0,directedLinksInArray*2);
			directedLinksCapacity = newCapacity;
			directedLinks = n;
		}
		directedLinks[ directedLinksInArray * 2 ] = from_k;
		directedLinks[ directedLinksInArray * 2 + 1 ] = to_k;
		++ directedLinksInArray;
	}

	void addLink( int from_x, int from_y, int from_z, int to_x, int to_y, int to_z ) {
		int from_k = from_x + from_y * width + from_z * width * height;
		int to_k = to_x + to_y * width + to_z * width * height;
		addLink( from_k, to_k );
	}

	void writeWavefrontObj( String outputFilename ) throws IOException {

		PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outputFilename),"UTF-8"));

		// It's a bit ugly encoding this data in the comments,
		// but it's a useful enough stop gap measure:
		pw.println("# width: "+width);
		pw.println("# height: "+height);
		pw.println("# depth: "+depth);
		pw.println("# spacing_x: "+spacing_x);
		pw.println("# spacing_y: "+spacing_y);
		pw.println("# spacing_z: "+spacing_z);
		

		/* First build a map from keys to wavefront indices: */

		Hashtable<Integer,Integer> wavefrontToKey=new Hashtable<Integer,Integer>();

		Set<Integer> keySet=fromPosition.keySet();

		int counter = 1;
		for(Iterator<Integer> i=keySet.iterator();i.hasNext();) {
			int k=i.next();
			wavefrontToKey.put(k,counter);
			int x = k % width;
			int y = ( k / width ) % height;
			int z = ( k / ( width * height ) ) % depth;
			pw.println("v "+(x * spacing_x)+" "+(y * spacing_y)+" "+(z * spacing_z));
			++counter;
		}
		
		for( int i = 0; i < directedLinksInArray; ++i ) {
			int from_k = directedLinks[i*2];
			int to_k = directedLinks[i*2+1];
			int from_vertex = wavefrontToKey.get(from_k);
			int to_vertex = wavefrontToKey.get(to_k);
			pw.println("l "+from_vertex+" "+to_vertex);
		}

		pw.print("g");

		pw.close();
	}


	void writeXML( String outputFilename ) throws IOException {

		PrintWriter pw = null;

		boolean compress = false;
		
		try {
			if( compress ) {
				System.out.println("Creating, with compression...");
				pw = new PrintWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(outputFilename)),"UTF-8"));
				System.out.println("Created, pw is: "+pw);
			} else {
				System.out.println("Creating, without compression...");
				pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outputFilename),"UTF-8"));
				System.out.println("Created, pw is: "+pw);
			}
			
			pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			pw.println("<!DOCTYPE connections [");
			pw.println("  <!ELEMENT tracings       (samplespacing,imagesize,path*,fill*)>");
			pw.println("  <!ELEMENT imagesize      EMPTY>");
			pw.println("  <!ELEMENT samplespacing  EMPTY>");
			pw.println("  <!ELEMENT path           (point+)>");
			pw.println("  <!ELEMENT point          EMPTY>");
			pw.println("  <!ELEMENT fill           (node*)>");
			pw.println("  <!ELEMENT node           EMPTY>");
			pw.println("  <!ATTLIST samplespacing  x             CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST samplespacing  y             CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST samplespacing  z             CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST samplespacing  units         CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST imagesize      width         CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST imagesize      height        CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST imagesize      depth         CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST path           id            CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST path           startson      CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST path           startsindex   CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST path           endson        CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST path           endsindex     CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST path           reallength    CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST point          x             CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST point          y             CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST point          z             CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST fill           id            CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST fill           frompaths     CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST fill           metric        CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST fill           threshold     CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST node           id            CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST node           x             CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST node           y             CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST node           z             CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST node           previousid    CDATA           #IMPLIED>");
			pw.println("  <!ATTLIST node           distance      CDATA           #REQUIRED>");
			pw.println("  <!ATTLIST node           status        (open|closed)   #REQUIRED>");
			pw.println("]>");
			pw.println("");
			
			pw.println("<tracings>");
			
			// FIXME: implement

			pw.println("</tracings>");

		} finally {
			pw.close();
		}
		}
	}
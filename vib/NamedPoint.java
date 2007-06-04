/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package vib;

import ij.*;
import ij.process.*;
import ij.io.*;
import ij.gui.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.text.*;

import ij.measure.Calibration;

import java.awt.Color;
import java.io.*;

import math3d.Point3d;

import vib.transforms.Transform;
import vib.transforms.FastMatrixTransform;
import vib.transforms.OrderedTransformations;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.StringTokenizer;

import java.awt.*;
import java.awt.event.*;

import java.io.*;
import java.util.regex.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;

import vib.FastMatrix;

public class NamedPoint {
	
        public double x,y,z;
        public boolean set;
	
        String name;
	
        public NamedPoint(String name,
                          double x,
                          double y,
                          double z) {
                this.x = x;
                this.y = y;
                this.z = z;
                this.name = name;
                this.set = true;
        }
	
        public NamedPoint(String name) {
                this.name = name;
                this.set = false;
        }
	
        public static void correctWithCalibration( ArrayList<NamedPoint> namedPoints, Calibration c ) {
		
                FastMatrixTransform fm=FastMatrixTransform.fromCalibrationWithoutOrigin(c);
		
                Iterator i0;
                for(i0=namedPoints.listIterator();i0.hasNext();) {
                        NamedPoint p=(NamedPoint)i0.next();
                        p.transformWith(fm);
                }
		
        }
	
        public void transformWith(FastMatrix m) {
                m.apply(x,y,z);
                x=m.x;
                y=m.y;
                z=m.z;
        }
	
        public NamedPoint transformWith(OrderedTransformations o) {
                double[] result=new double[3];
                o.apply(x,y,z,result);
                return new NamedPoint(name,result[0],result[1],result[2]);
        }
	
        public static ArrayList<NamedPoint> transformPointsWith( ArrayList<NamedPoint> namedPoints, OrderedTransformations o ) {
		
                ArrayList<NamedPoint> result=new ArrayList<NamedPoint>();
		
                Iterator i0;
                for(i0=namedPoints.listIterator();i0.hasNext();) {
                        NamedPoint p=(NamedPoint)i0.next();
                        result.add(p.transformWith(o));
                }
		
                return result;
        }
	
	static byte [] pointsDataAsBytes( ArrayList<NamedPoint> points ) {
		
		int total_bytes = 0;
		
		ArrayList< byte [] > linesOfBytes = new ArrayList< byte [] >();
		Iterator i;
		for(i=points.listIterator();i.hasNext();) {
			NamedPoint p = (NamedPoint)i.next();
			if(p.set) {
				String line = p.toYAML() + "\n";
				byte [] line_bytes;
				try {
					line_bytes = line.getBytes("UTF-8");
				} catch( UnsupportedEncodingException e ) {
					IJ.error( "Got an UnsupportedEncodingException - this should never happen: " + e );
					return null;
				}
				linesOfBytes.add( line_bytes );
				total_bytes += line_bytes.length;
			}
		}
		
		byte [] result = new byte[total_bytes];
		int add_at = 0;
		
		for( int j = 0; j < linesOfBytes.size(); ++j ) {
			byte [] line_of_bytes = (byte [])linesOfBytes.get(j);
			System.arraycopy( line_of_bytes, 0, result, add_at, line_of_bytes.length );
			add_at += line_of_bytes.length;
		}
		
		return result;
	}
	
        public static boolean savePointsFile( ArrayList<NamedPoint> points, String savePath ) {
		
                try {
                        FileOutputStream fos = new FileOutputStream(savePath);
			byte [] asBytes = NamedPoint.pointsDataAsBytes( points );
			fos.write(asBytes);
                        fos.close();
                        return true;
                } catch( IOException e ) {
                        return false;
                }
        }
	
	public static ArrayList<NamedPoint> pointsFromString( String fileContents ) {
		
		StringTokenizer tokenizer=new StringTokenizer(fileContents,"\n");
		
		ArrayList<NamedPoint> newNamedPoints = new ArrayList<NamedPoint>();
		Pattern p_data =
			Pattern.compile("^\"(.*)\": *"+
					"\\[ *([eE0-9\\.\\-]+) *, *"+
					"([eE0-9\\.\\-]+) *, *"+
					"([eE0-9\\.\\-]+) *\\] *$");
		Pattern p_empty = Pattern.compile("^ *$");
		
		while( tokenizer.hasMoreTokens() ) {
			
			String line = tokenizer.nextToken();
			
			System.err.println("parsing line: "+line);
			
			Matcher m_data = p_data.matcher(line);
			Matcher m_empty = p_empty.matcher(line);
			
			if (m_data.matches()) {
				newNamedPoints.add(
					new NamedPoint(m_data.group(1),
						       Double.parseDouble(m_data.group(2)),
						       Double.parseDouble(m_data.group(3)),
						       Double.parseDouble(m_data.group(4)))
					);
			} else if (m_empty.matches()) {
				// Ignore empty lines...
			} else {
				IJ.error("There was a malformed line: "+line);
				break;
			}
		}
		
		return newNamedPoints;
	}
	
        public static ArrayList<NamedPoint> pointsForImage( String fullFileName ) {
		
                // System.out.println("Trying to find points for image: "+fullFileName);
		
                String defaultFilename=fullFileName+".points";
                // System.out.println("Looking for points file at: "+fullFileName);
		
		try {
			
                        ArrayList<NamedPoint> newNamedPoints = new ArrayList<NamedPoint>();
                        Pattern p_data =
                                Pattern.compile("^\"(.*)\": *"+
                                                "\\[ *([eE0-9\\.\\-]+) *, *"+
                                                "([eE0-9\\.\\-]+) *, *"+
                                                "([eE0-9\\.\\-]+) *\\] *$");
                        Pattern p_empty = Pattern.compile("^ *$");
                        BufferedReader f = new BufferedReader(
                                new FileReader(defaultFilename));
                        String line;
                        while ((line=f.readLine())!=null) {
				
                                Matcher m_data = p_data.matcher(line);
                                Matcher m_empty = p_empty.matcher(line);
				
                                if (m_data.matches()) {
                                        newNamedPoints.add(
                                                new NamedPoint(m_data.group(1),
                                                               Double.parseDouble(m_data.group(2)),
                                                               Double.parseDouble(m_data.group(3)),
                                                               Double.parseDouble(m_data.group(4)))
                                                );
                                } else if (m_empty.matches()) {
                                        // Ignore empty lines...
                                } else {
                                        IJ.error("There was a points file ("+
                                                 defaultFilename+") but this line was malformed:\n"+
                                                 line);
                                        break;
                                }
                        }
			
                        return newNamedPoints;
			
		} catch( IOException e ) {
                        return null;
                }
		
        }
	
        static public ArrayList<NamedPoint> pointsForImage( ImagePlus imp ) {
		
                FileInfo info = imp.getOriginalFileInfo();
                if( info == null ) {
                        return null;
                }
                String fileName = info.fileName;
                String url = info.url;
                String directory = info.directory;
		
                return pointsForImage( directory+File.separator+fileName );
        }
	
        public static ArrayList<String> pointsInBoth(ArrayList<NamedPoint> points0,
                                                     ArrayList<NamedPoint> points1) {
		
                ArrayList<String> common = new ArrayList<String>();
                Iterator i0;
                for(i0=points0.listIterator();i0.hasNext();) {
                        String pointName = ((NamedPoint)i0.next()).name;
                        for(Iterator i1=points1.listIterator();i1.hasNext();) {
                                if (pointName.equals(((NamedPoint)i1.next()).name)) {
                                        common.add(new String(pointName));
                                        break;
                                }
                        }
                }
                return common;
        }
	
        public static String escape(String s) {
                String result = s.replaceAll("\\\\","\\\\\\\\");
                result = result.replaceAll("\\\"","\\\\\"");
                return result;
        }
	
        public static String unescape(String s) {
                // FIXME: actually write the unescaping code...
                return s;
        }
	
        public Point3d toPoint3d() {
                return new Point3d(x,y,z);
        }
	
        public String getName() {
                return name;
        }
	
        public String toYAML() {
                String line = "\""+
                        escape(name)+
                        "\": [ "+
                        x+", "+
                        y+", "+
                        z+" ]";
                return line;
        }
	
}

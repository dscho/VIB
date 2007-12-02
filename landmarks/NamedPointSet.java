/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/*
 * NamedPointSet.java
 * 
 * Created on 28-Sep-2007, 11:49:37
 */

package landmarks;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.io.FileInfo;
import ij.measure.Calibration;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import math3d.Point3d;
import vib.transforms.FastMatrixTransform;
import vib.transforms.OrderedTransformations;

public class NamedPointSet {
	
	ArrayList<NamedPoint> points;
	
	public int size() {
		return points.size();
	}
	
	public NamedPointSet() {
		points = new ArrayList<NamedPoint>();
	}
	
	public ListIterator listIterator() {
		return points.listIterator();
	}
	
	/* FIXME: these next two methods should return new transformed
	   point sets instead. */
	
        public void correctWithCalibration( Calibration c ) {
		
                FastMatrixTransform fm=FastMatrixTransform.fromCalibrationWithoutOrigin(c);
		
                Iterator i0;
                for(i0=points.listIterator();i0.hasNext();) {
                        NamedPoint p=(NamedPoint)i0.next();
                        p.transformWith(fm);
                }
		
        }	

        public void transformPointsWith( OrderedTransformations o ) {
		
                Iterator i0;
                for(i0=points.listIterator();i0.hasNext();) {
                        NamedPoint p=(NamedPoint)i0.next();
                        p.transformWith(o);
                }
	}
	
	public NamedPoint getPoint( String name ) {
		Iterator i;
		for (i = listIterator(); i.hasNext();) {
			NamedPoint p = (NamedPoint)i.next();
			if( p.name.equals(name) ) {
				return p;
			}
		}
		return null;
	}
	
	public NamedPoint get(int i) { 
		return points.get(i);
	}
	
	public NamedPoint get(String name) {
                Iterator i0;
                for(i0=points.listIterator();i0.hasNext();) {
                        NamedPoint p=(NamedPoint)i0.next();
                        if( p.getName().equals(name) )
				return p;
                }
		return null;
	}
	
	void showAsROI(int i, ImagePlus imp) {
		NamedPoint p = points.get(i);
		assert p.set;
		int slice = (int)p.z;
		if(slice < 0)
			slice = 0;
		if(slice > imp.getStackSize())
			slice = imp.getStackSize()-1;
		imp.setSlice(slice+1);
		ImageCanvas canvas = imp.getCanvas();
		Roi roi = new PointRoi(canvas.screenX((int)p.x),
				       canvas.screenY((int)p.y),
				       imp);
		imp.setRoi(roi);
	}
	
	public boolean savePointsFile( String savePath ) {
		
		try {
			FileOutputStream fos = new FileOutputStream(savePath);
			byte [] asBytes = dataAsBytes( );
			fos.write(asBytes);
			fos.close();
			return true;
		} catch( IOException e ) {
			return false;
		}
	}	
	
	public byte [] dataAsBytes( ) {
		
		int total_bytes = 0;
		
		ArrayList< byte [] > linesOfBytes = new ArrayList< byte [] >();
		Iterator i;
		for(i=listIterator();i.hasNext();) {
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
			byte [] line_of_bytes = linesOfBytes.get(j);
			System.arraycopy( line_of_bytes, 0, result, add_at, line_of_bytes.length );
			add_at += line_of_bytes.length;
		}
		
		return result;
	}
	
	public static NamedPointSet fromString( String fileContents ) {
		
		StringTokenizer tokenizer=new StringTokenizer(fileContents,"\n");
		
		NamedPointSet result = new NamedPointSet();
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
				result.add(
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
		
		return result;
	}
	
	public void add(NamedPoint namedPoint) {
		points.add(namedPoint);
	}
	
        public static NamedPointSet forImage( String fullFileName ) {
		
                // System.out.println("Trying to find points for image: "+fullFileName);
		
                String defaultFilename=fullFileName+".points";
                // System.out.println("Looking for points file at: "+fullFileName);
		

		System.out.println("Trying to load: "+defaultFilename);
		try {
			
                        NamedPointSet newNamedPoints = new NamedPointSet();
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
			/*
			IJ.error("Error opening the points file "+defaultFilename+": "+e);
			System.out.println("Got an IOException while loading the points file: "+e);
			e.printStackTrace();
			*/
			return null;
                }
		
        }
	
        public static NamedPointSet forImage( ImagePlus imp ) {
		
                FileInfo info = imp.getOriginalFileInfo();
                if( info == null ) {
                        return null;
                }
                String fileName = info.fileName;
                String url = info.url;
                String directory = info.directory;
		
                return NamedPointSet.forImage( directory+File.separator+fileName );
        }		
	
        public ArrayList<String> namesSharedWith( NamedPointSet other) {
		
                ArrayList<String> common = new ArrayList<String>();
                Iterator i0;
                for(i0=listIterator();i0.hasNext();) {
                        String pointName = ((NamedPoint)i0.next()).name;
                        for(Iterator i1=other.listIterator();i1.hasNext();) {
                                if (pointName.equals(((NamedPoint)i1.next()).name)) {
                                        common.add(new String(pointName));
                                        break;
                                }
                        }
                }
                return common;
        }

	public Point3d[] getPoint3DArrayForNames( String [] names ) {
		Point3d [] result = new Point3d[names.length];
		for( int i = 0; i < names.length; ++i ) {
			NamedPoint np = get(names[i]);
			if( np == null )
				return null;
			else
				result[i] = new Point3d( np.x, np.y, np.z );
		}
		return result;
	}
}

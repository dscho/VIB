/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package landmarks;

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

	public String toString() {
		return ""+name+
			" at "+x+
			", "+y+
			", "+z;
	}
}

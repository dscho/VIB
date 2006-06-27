
import ij.gui.ShapeRoi;

import java.awt.geom.PathIterator;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.*;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import math3d.Triangle;
import math3d.Point3d;

/**
 * User: Tom Larkworthy
 * Date: 19-Jun-2006
 * Time: 23:16:32
 */
public class Utils {
    public static final ShapeRoi toShapeRoi(PathIterator iter){
            
            ArrayList<Float> pathData = new ArrayList<Float>();

            while(!iter.isDone()){
                float[] values = new float[6];
                int type = iter.currentSegment(values);

                pathData.add((float)type);
                if(type == PathIterator.SEG_MOVETO || type == PathIterator.SEG_LINETO){
                    pathData.add(values[0]);
                    pathData.add(values[1]);
                }else if(type == PathIterator.SEG_CLOSE){
                    //no coords for this type
                }
                else{
                    throw new UnsupportedOperationException("upgrade ShapeBuilder " + type);
                }
                iter.next();
            }
            float [] floatRepresentation = new float[pathData.size()];
            for (int i = 0; i < floatRepresentation.length; i++) {
                floatRepresentation[i] = pathData.get(i);
            }
            return new ShapeRoi(floatRepresentation);

    }           

    public static class Spiral implements Iterator<Point>{

        private Point current;

        int depth=0;

        boolean applyX = false;


        public Spiral(Point start) {
            this.current = start;
        }

        public boolean hasNext() {
            return true;
        }

        //returns point sin an outward spiral from and including the start point
        public Point next() {

            //the decision to add or subracts depending whether depth is divisable by 2 or not
            int modAmount = depth%2==0?depth:-depth;

            if(applyX){
                applyX = false;
                current.x = current.x + modAmount;
            }
            else{
                applyX = true;
                current.y = current.y + modAmount;
                depth++; //increpent depth every other
            }

            return current;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public static void main(String[] args) {
        Spiral s = new Spiral(new Point(5,5));
        for(int i=0;i<5; i++){
            System.out.println(s.next());
        }
    }

}

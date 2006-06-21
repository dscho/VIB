
import ij.gui.ShapeRoi;

import java.awt.geom.PathIterator;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;

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
}

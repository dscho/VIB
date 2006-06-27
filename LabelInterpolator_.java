
import ij.ImagePlus;
import ij.IJ;
import ij.plugin.PlugIn;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.LinkedList;
import java.awt.geom.GeneralPath;
import java.awt.geom.AffineTransform;
import java.awt.*;

import math3d.*;

/**
 * Generates label data for unlabelled slices based on the data in the nearest slices
 * User: Tom Larkworthy
 * Date: 23-Jun-2006
 * Time: 19:11:01
 */
public class LabelInterpolator_ implements PlugIn {

    public void run(String arg) {
        interpolate(new SegmentatorModel(IJ.getImage()));
    }


    public static void interpolate(SegmentatorModel model) {
        if (model.getCurrentMaterial() == null) {
            IJ.showMessage("please select a label first");
            return;
        }

        StackData data = new StackData(model, model.getCurrentMaterial().id);

        System.out.println("stack data = " + data);
        IJ.showProgress(.5);
        for (Interpolation interpolation : data.getInterpolations()) {
            interpolation.interpolate();
            for (int i = interpolation.firstIndex; i <= interpolation.secondIndex; i++) {
                model.updateSliceNoRedraw(i);
            }
        }

        IJ.showProgress(1);

        model.data.updateAndDraw();
    }


    private static class StackData {
        ImagePlus labelsData;
        ArrayList<Integer> labelledSlices = new ArrayList<Integer>();

        SegmentatorModel model;
        int label;

        public StackData(SegmentatorModel model, int label) {
            this.model = model;
            this.labelsData = model.getLabelImagePlus();
            this.label = label;

            findLabelledSlices();


        }

        private void findLabelledSlices() {
            //we go through every slice looking for a pixel with the same label
            //this will fill labelledSlices with the indexes of all labelled slices
            for (int i = 1; i <= labelsData.getStackSize(); i++) {
                byte[] pixels = (byte[]) labelsData.getStack().getProcessor(i).getPixels();

                for (int j = 0; j < pixels.length; j++) {
                    byte pixel = pixels[j];
                    if (pixel == label) {
                        labelledSlices.add(i);
                        break;
                    }
                }
            }
        }

        public Iterable<Interpolation> getInterpolations() {
            LinkedList<Interpolation> results = new LinkedList<Interpolation>();

            for (int i = 0; i < labelledSlices.size() - 1; i++) {
                Interpolation inter = new Interpolation();
                int firstIndex = labelledSlices.get(i);
                int secondIndex = labelledSlices.get(i + 1);

                inter.width = model.getLabelImagePlus().getWidth();
                inter.color = label;

                inter.firstIndex = firstIndex;
                inter.secondIndex = secondIndex;

                inter.labelledPixels1 = (byte[]) labelsData.getStack().getProcessor(firstIndex).getPixels();
                inter.labelledPixels2 = (byte[]) labelsData.getStack().getProcessor(secondIndex).getPixels();

                inter.outline1 = model.getLabelCanvas().getOutline(firstIndex, label);
                inter.outline2 = model.getLabelCanvas().getOutline(secondIndex, label);


                for (int j = firstIndex + 1; j < secondIndex; j++) {
                    inter.interpolatedPixels.add((byte[]) labelsData.getStack().getProcessor(j).getPixels());
                }
                results.add(inter);
            }

            return results;
        }

        public String toString() {
            return "labelledSlices: " + labelledSlices;
        }
    }


    private static class Interpolation {
        byte[] labelledPixels1;
        byte[] labelledPixels2;
        int firstIndex;
        int secondIndex;

        int width;
        int color;

        GeneralPath outline1;
        GeneralPath outline2;

        ArrayList<byte[]> interpolatedPixels = new ArrayList<byte[]>();


        /**
         * gets the distance between the two labelled pixel slices
         *
         * @return
         */
        int getDistance() {
            return interpolatedPixels.size();
        }

        int getPixel(int x, int y, byte[] data) {
            return data[x + y * width];
        }

        void setPixel(int x, int y, int color, byte[] data) {
            data[x + y * width] = (byte) color;
        }

        /**
         * runs the interpolation algorithm
         */
        void interpolate() {
            Rectangle bounds1 = outline1.getBounds();
            Rectangle bounds2 = outline2.getBounds();

            Rectangle totalBounds = new Rectangle(bounds1);
            totalBounds.add(bounds2);

            //Polygon3d tightBounds = Utils.buildPolygon(outline1, firstIndex, outline2, secondIndex);

            for (int x = totalBounds.x; x < totalBounds.x + totalBounds.width; x++) {
                for (int y = totalBounds.y; y < totalBounds.y + totalBounds.height; y++) {
                    //if both end don't have a pixel at the location
                    //then we know that the interpolated image won't weith
                    if (getPixel(x, y, labelledPixels1) != color && getPixel(x, y, labelledPixels2) != color) {

                    } else if (getPixel(x, y, labelledPixels1) == color && getPixel(x, y, labelledPixels2) == color) {
                        
                        //if both ends do have the pixel then we know the interpolated will also
                        for (byte[] pixels : interpolatedPixels) {
                            setPixel(x, y, color, pixels);
                        }

                    } else {
                        //one slice at x,y is labelled and the other is not
                        //we need to do some math to work out what is labelled and what is not
                        Line fillLine;
                        //a unmatch pixel in one img,
                        if (getPixel(x, y, labelledPixels1) == color) {
                            //unmatched in lowest slice
                            Point oppPoint = getNearest(new Point(x, y), labelledPixels2, width, color,100);

                            if(oppPoint == null) continue;

                            //draw a line between the nearest neighbours
                            fillLine = new Line(new Point3d(x, y, firstIndex), new Point3d(oppPoint.x, oppPoint.y, secondIndex));
                        } else {
                            //unmatched in highest slice
                            Point oppPoint = getNearest(new Point(x, y), labelledPixels1, width, color,100);
                            if(oppPoint == null) continue;
                            //draw a line between the nearest neighbours
                            fillLine = new Line(new Point3d(x, y, secondIndex), new Point3d(oppPoint.x, oppPoint.y, firstIndex));
                        }

                        System.out.println("fillLine = " + fillLine);

                        //now we need to see where this line intesects out unlabelled slices
                        //need to check each slice
                        int index = firstIndex;
                        for (byte[] pixels : interpolatedPixels) {
                            int sliceDepth = ++index;

                            //create a plane that replesents the slice
                            //(probably quicker to jump straight in to plane equation) todo
                            Plane slice = new Plane(new Point3d(0, 0, sliceDepth),
                                    new Point3d(0, 1, sliceDepth),
                                    new Point3d(1, 0, sliceDepth), new Point3d(1, 0, -sliceDepth));

                            Point3d intersect = slice.intersection(fillLine);
                            //System.out.println("intersect = " + intersect);

                            setPixel(Math.round((float) intersect.x), Math.round((float) intersect.y), color, pixels);

                        }
                        System.out.println("filled");
                    }
                }
            }

            System.out.println("one interpolation done");
        }

        /**
         * finds the nearest point to the one supplied with the specified colour in the supplied pixel data
         *
         * @param point
         * @param pixels
         * @param color
         * @return
         */
        private Point getNearest(Point point, byte[] pixels, int width, int color, int max) {
            //should search in an ever increasing spiral circles to find the neairbour
            Utils.Spiral spiral = new Utils.Spiral(point);

            int count = 0;
            while (true) {
                Point tstPoint = spiral.next();
                if(count++ > max) {
                    System.out.println("spiral search failed");
                    return null;
                }

                if (tstPoint.x < 0 || tstPoint.x >= width) continue;
                if (tstPoint.y < 0 || tstPoint.y >= pixels.length / width) continue;

                //System.out.println("tstPoint = " + tstPoint);
                if (pixels[tstPoint.x + tstPoint.y * width] == color) return tstPoint;


            }


        }

    }


}

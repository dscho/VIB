/*
 *
 *
 *
 */

import ij.IJ;
import ij.ImageStack;
import ij.process.ColorProcessor;
import ij.ImagePlus;

import math3d.Point3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;

import vib.FastMatrix;

/* This class contains a sequence of transformations (currently just
 * FastMatrix applications, but in the future possibly others.) */

public class OrderedTransformations {

    private ArrayList<FastMatrix> listOfTransforms;

    public OrderedTransformations() {
        listOfTransforms = new ArrayList<FastMatrix>();
    }

    public void add( FastMatrix f ) {
        listOfTransforms.add(f);
    }

    public int number( ) {
        return listOfTransforms.size();
    }

    public String toString( ) {

        String result = "";

        int j = 0;
        for( Iterator i = listOfTransforms.iterator(); i.hasNext(); ++j ) {
            FastMatrix f = (FastMatrix)i.next();
            result += "Transformation " + j + " is:\n";
            result += f.toStringIndented( "   " );
        }

        return result;
    }

    public void reduce( ) {

        Iterator i;

        // Remove any identities...

        for( i = listOfTransforms.iterator(); i.hasNext(); ) {
            FastMatrix f = (FastMatrix)i.next();
            if( f.isIdentity() )
                i.remove();
        }

        // Try to compose each transformation in the list:

        ArrayList<FastMatrix> newList = new ArrayList<FastMatrix> ();

        FastMatrix last = null;

        for( i = listOfTransforms.iterator(); i.hasNext(); ) {

            /* Each time we enter this loop:

                - If last is null, then we should get the next matrix
                  and start with that.  (This happens the first time
                  through only.)

                - If last is not null, then we should try to compose
                  the succeeding matrices with it.  (This happens if
                  we've tried a composition and it failed.)
            */

            if( last == null )
                last = (FastMatrix)i.next();

            while( i.hasNext() ) {

                FastMatrix next = (FastMatrix)i.next();

                FastMatrix compositionResult = last.composeWith( next );

                if( compositionResult == null ) {

                    // Then fix up and break...

                    newList.add( last );
                    last = next;
                    break;

                } else {

                    // The move the result to last, and try composing
                    // the next one.

                    last = compositionResult;

                }

            }

            // We can get here by running out of elements, or a
            // composition failing.  (In the latter case, we've
            // already added the newly composed bit.)

            // If we get to here because there's nothing left in the
            // list, then we should just add last.

            if( ! i.hasNext() ) {
                if( last != null )
                    newList.add( last );
            }

        }

        listOfTransforms = newList;

    }

    // FIXME: untested as yet...

    public OrderedTransformations invert( ) {

        ArrayList newList = new ArrayList<FastMatrix>();

        ListIterator i;

        // Move to the end of the list...

        for( i = listOfTransforms.listIterator(); i.hasNext(); i.next() )
            ;

        // Step back through the list, inverting them and adding to
        // the new list.

        while( i.hasPrevious() ) {

            FastMatrix f = (FastMatrix)i.previous();

            FastMatrix f_inverted = f.inverse();

            newList.add( f_inverted );

        }

        OrderedTransformations result = new OrderedTransformations();

        result.listOfTransforms = newList;

        return result;
    }

    public void apply( double x, double y, double z, double [] result ) {

        for( FastMatrix f : listOfTransforms ) {
            f.apply( x, y, z );
            x = f.x;
            y = f.y;
            z = f.z;
        }

        result[0] = x;
        result[1] = y;
        result[2] = z;
    }

    public double [] apply( double x, double y, double z ) {

        for( FastMatrix f : listOfTransforms ) {
            f.apply( x, y, z );
            x = f.x;
            y = f.y;
            z = f.z;
        }

        double [] result = new double[3];
        result[0] = x;
        result[1] = y;
        result[2] = z;
        return result;
    }

    public Point3d apply( Point3d p ) {

        double [] result = apply( p.x, p.y, p.z );
        return new Point3d( result[0], result[1], result[2] );
    }

    public double scoreTransformation( ImagePlus image0,
                                       ImagePlus image1,
                                       Threshold threshold0,
                                       Threshold threshold1 ) {

        return scoreTransformation( image0, image1, threshold0, threshold1, 0 );

    }

    public double scoreTransformation( ImagePlus image0,
                                       ImagePlus image1,
                                       Threshold threshold0,
                                       Threshold threshold1,
                                       int skipPixelsInTemplate ) {

        OrderedTransformations invertedTransform = invert();

        ImageStack stack0 = image0.getStack();
        ImageStack stack1 = image1.getStack();

        int d0 = stack0.getSize();
        int h0 = stack0.getHeight();
        int w0 = stack0.getWidth();

        int d1 = stack1.getSize();
        int h1 = stack1.getHeight();
        int w1 = stack1.getWidth();

        double [] transformedPoint = new double[3];

        long numberOfPixelsConsidered = 0;
        long sumSquaredDifferences = 0;

        int x_in_domain;
        int y_in_domain;
        int z_in_domain;

        for( int z = 0;
             z < d0;
             z += (1 + skipPixelsInTemplate) ) {

            byte [] templatePixels = (byte []) stack0.getPixels( z + 1 );

            for( int y = 0;
                 y < h0;
                 y += (1 + skipPixelsInTemplate) ) {

                for( int x = 0;
                     x < w0;
                     x += (1 + skipPixelsInTemplate) ) {

                    invertedTransform.apply( x, y, z, transformedPoint );

                    x_in_domain = ((int)transformedPoint[0]);
                    y_in_domain = ((int)transformedPoint[1]);
                    z_in_domain = ((int)transformedPoint[2]);

                    int value_in_template = (int)( 0xFF & templatePixels[ x + y * w0 ] );
                    if( value_in_template < threshold0.value )
                        value_in_template = 0;

                    int value_in_domain;

                    if( ( x_in_domain >= 0 ) && ( x_in_domain < w1 ) &&
                        ( y_in_domain >= 0 ) && ( y_in_domain < h1 ) &&
                        ( z_in_domain >= 0 ) && ( z_in_domain < d1 ) ) {

                        byte [] domainPixels = (byte[])stack1.getPixels( z_in_domain + 1 );

                        value_in_domain = (int)( 0xFF & domainPixels[ x_in_domain + y_in_domain * w1 ] );

                        if( value_in_domain < threshold1.value )
                            value_in_domain = 0;

                    } else {

                        value_in_domain = 0;

                    }

                    int difference = value_in_domain - value_in_template;
                    sumSquaredDifferences += (long)( difference * difference );
                    numberOfPixelsConsidered += 1;

                }
            }
        }

        // System.out.println( "Number of pixels considered was: " + numberOfPixelsConsidered );

        return Math.sqrt( sumSquaredDifferences / (double) numberOfPixelsConsidered );

    }

    public void createNewImage( ImageStack image0, ImageStack image1, boolean cropToTemplate ) {

        // FIXME: check image depths of image0 and image1.  (This only
        // works on 8bit stacks at the moment.)

        int width0 = image0.getWidth();
        int width1 = image1.getWidth();
        int height0 = image0.getHeight();
        int height1 = image1.getHeight();
        int depth0 = image0.getSize();
        int depth1 = image1.getSize();

        int widthNew;
        int heightNew;
        int depthNew;

        ImageStack stack = null;

        OrderedTransformations invertedTransform = invert();

        if( cropToTemplate ) {

            widthNew = width0;
            heightNew = height0;
            depthNew = depth0;

            stack = new ImageStack( widthNew, heightNew );

            int x, y, z;

            int x_in_domain;
            int y_in_domain;
            int z_in_domain;

            byte [][] image1_data = new byte[depth1][];

            for( z = 0; z < depth1; ++z ) {
                image1_data[z] = (byte[])image1.getPixels( z + 1 );
            }

            for( z = 0; z < depthNew; ++z ) {

                byte [] magentaPixels = (byte []) image0.getPixels( z + 1 );
                byte [] greenPixels = new byte[ widthNew * heightNew ];

                double [] transformedPoint = new double[3];

                for( y = 0; y < height0; ++y )
                    for( x = 0; x < width0; ++x ) {

                        invertedTransform.apply( x, y, z, transformedPoint );

                        x_in_domain = ((int)transformedPoint[0]);
                        y_in_domain = ((int)transformedPoint[1]);
                        z_in_domain = ((int)transformedPoint[2]);

                        if( ( x_in_domain >= 0 ) && ( x_in_domain < width1 ) &&
                            ( y_in_domain >= 0 ) && ( y_in_domain < height1 ) &&
                            ( z_in_domain >= 0 ) && ( z_in_domain < depth1 ) ) {

                            byte [] pixels = image1_data[z_in_domain];
                            // byte [] pixels = (byte[])image1.getPixels( z_in_domain + 1 );

                            greenPixels[ x + y * width0 ] =
                                pixels[ x_in_domain + y_in_domain * width1 ];

                        } /* else {

                            greenPixels[ x + y * width0 ] = 127;

                        } */


                    }

                ColorProcessor cp = new ColorProcessor( widthNew, heightNew );

                cp.setRGB( magentaPixels, greenPixels, magentaPixels );

                stack.addSlice( null, cp );

                IJ.showProgress( (double) (z + 1) / depthNew );

            }

        } else {

            // ------ Transform the corners of the domain image -----------------------

            // FIXME: Obviously with some transformations this won't
            // give us a good bounding box for the transformed image,
            // but for the moment it's good enough.

            int new_min_x_1, new_min_y_1, new_min_z_1;
            int new_max_x_1, new_max_y_1, new_max_z_1;

            {

                Point3d corner0 = new Point3d( 0,           0,            0 );
                Point3d corner1 = new Point3d( 0,           0,            depth1 - 1);
                Point3d corner2 = new Point3d( 0,           height1 - 1,  0 );
                Point3d corner3 = new Point3d( 0,           height1 - 1,  depth1 - 1);
                Point3d corner4 = new Point3d( width1 - 1,  0,            0 );
                Point3d corner5 = new Point3d( width1 - 1,  0,            depth1 - 1);
                Point3d corner6 = new Point3d( width1 - 1,  height1 - 1,  0 );
                Point3d corner7 = new Point3d( width1 - 1,  height1 - 1,  depth1 - 1);

                Point3d corner0_transformed = apply( corner0 );
                Point3d corner1_transformed = apply( corner1 );
                Point3d corner2_transformed = apply( corner2 );
                Point3d corner3_transformed = apply( corner3 );
                Point3d corner4_transformed = apply( corner4 );
                Point3d corner5_transformed = apply( corner5 );
                Point3d corner6_transformed = apply( corner6 );
                Point3d corner7_transformed = apply( corner7 );

                /*
                System.out.println( "corner0 now at: " + corner0_transformed );
                System.out.println( "corner1 now at: " + corner1_transformed );
                System.out.println( "corner2 now at: " + corner2_transformed );
                System.out.println( "corner3 now at: " + corner3_transformed );
                System.out.println( "corner4 now at: " + corner4_transformed );
                System.out.println( "corner5 now at: " + corner5_transformed );
                System.out.println( "corner6 now at: " + corner6_transformed );
                System.out.println( "corner7 now at: " + corner7_transformed );
                */

                double [] corner_xs = { corner0_transformed.x,
                                        corner1_transformed.x,
                                        corner2_transformed.x,
                                        corner3_transformed.x,
                                        corner4_transformed.x,
                                        corner5_transformed.x,
                                        corner6_transformed.x,
                                        corner7_transformed.x };

                double [] corner_ys = { corner0_transformed.y,
                                        corner1_transformed.y,
                                        corner2_transformed.y,
                                        corner3_transformed.y,
                                        corner4_transformed.y,
                                        corner5_transformed.y,
                                        corner6_transformed.y,
                                        corner7_transformed.y };

                double [] corner_zs = { corner0_transformed.z,
                                        corner1_transformed.z,
                                        corner2_transformed.z,
                                        corner3_transformed.z,
                                        corner4_transformed.z,
                                        corner5_transformed.z,
                                        corner6_transformed.z,
                                        corner7_transformed.z };

                java.util.Arrays.sort( corner_xs );
                java.util.Arrays.sort( corner_ys );
                java.util.Arrays.sort( corner_zs );

                double min_trans_corner_x = corner_xs[0];
                double min_trans_corner_y = corner_ys[0];
                double min_trans_corner_z = corner_zs[0];

                double max_trans_corner_x = corner_xs[7];
                double max_trans_corner_y = corner_ys[7];
                double max_trans_corner_z = corner_zs[7];

                new_min_x_1 = (int)Math.floor(min_trans_corner_x);
                new_min_y_1 = (int)Math.floor(min_trans_corner_y);
                new_min_z_1 = (int)Math.floor(min_trans_corner_z);

                new_max_x_1 = (int)Math.ceil(max_trans_corner_x);
                new_max_y_1 = (int)Math.ceil(max_trans_corner_y);
                new_max_z_1 = (int)Math.ceil(max_trans_corner_z);

                System.out.println( "min corner: " + new_min_x_1 + ", " + new_min_y_1 + ", " + new_min_z_1 );
                System.out.println( "max corner: " + new_max_x_1 + ", " + new_max_y_1 + ", " + new_max_z_1 );

            }
            // ---- done with the corner stuff ----------------------------------------

            // These are the dimensions of the domain stack when mapped...

            int width_mapped2 =  (new_max_x_1 - new_min_x_1) + 1;
            int height_mapped2 = (new_max_y_1 - new_min_y_1) + 1;
            int depth_mapped2 =  (new_max_z_1 - new_min_z_1) + 1;

            // Offsets for the transformed and template images within
            // the new larger image...

            int transformed_offset_x = 0;
            int transformed_offset_y = 0;
            int transformed_offset_z = 0;

            int target_offset_x = 0;
            int target_offset_y = 0;
            int target_offset_z = 0;

            if( new_min_x_1 < 0 )
                target_offset_x = - new_min_x_1;
            else if( new_min_x_1 > 0 )
                transformed_offset_x = new_min_x_1;

            if( new_min_y_1 < 0 )
                target_offset_y = - new_min_y_1;
            else if( new_min_y_1 > 0 )
                transformed_offset_y = new_min_y_1;

            if( new_min_z_1 < 0 )
                target_offset_z = - new_min_z_1;
            else if( new_min_z_1 > 0 )
                transformed_offset_z = new_min_z_1;

            System.out.println( "target offsets: " + target_offset_x +
                                ", " + target_offset_y + ", " +
                                target_offset_z );

            System.out.println( "transformed offsets: " + transformed_offset_x +
                                ", " + transformed_offset_y + ", " +
                                transformed_offset_z );

            // ------------------------------------------------------------------------

            widthNew = Math.max( width0, new_max_x_1 + 1 ) - Math.min( 0, new_min_x_1 );
            heightNew = Math.max( height0, new_max_y_1 + 1 ) - Math.min( 0, new_min_y_1 );
            depthNew = Math.max( depth0, new_max_z_1 + 1 ) - Math.min( 0, new_min_z_1 );

            System.out.println( "New image dimensions: " + widthNew + "x" + heightNew + "x" + depthNew );

            stack = new ImageStack( widthNew, heightNew );

            int x_in_template;
            int y_in_template;
            int z_in_template;

            int x_in_domain;
            int y_in_domain;
            int z_in_domain;

            byte [] magentaPixels;

            double [] transformed;

            int x, y, z;

            byte [][] image1_data = new byte[depth1][];

            for( z = 0; z < depth1; ++z ) {
                image1_data[z] = (byte[])image1.getPixels( z + 1 );
            }

            for( z = 0; z < depthNew; ++z ) {

                System.out.println( "  Doing slice: " + z );

                z_in_template = z - target_offset_z;

                if( (z_in_template >= 0) && (z_in_template < depth0 ) )
                    magentaPixels = (byte [])image0.getPixels( (z - target_offset_z) + 1 );
                else
                    magentaPixels = null; // Just to stop the initialization warning...

                byte [] magentaPixelsExpanded = new byte[widthNew * heightNew];
                byte [] greenPixelsExpanded = new byte[widthNew * heightNew];

                for( y = 0; y < heightNew; ++y ) {
                    for( x = 0; x < widthNew; ++x ) {

                        x_in_template = x - target_offset_x;
                        y_in_template = y - target_offset_y;

                        transformed = invertedTransform.apply(
                            x - target_offset_x,
                            y - target_offset_y,
                            z - target_offset_z );

                        x_in_domain = ((int)transformed[0]);
                        y_in_domain = ((int)transformed[1]);
                        z_in_domain = ((int)transformed[2]);

                        if( ( z_in_domain >= 0 ) && ( z_in_domain < depth1 ) &&
                            ( y_in_domain >= 0 ) && ( y_in_domain < height1 ) &&
                            ( x_in_domain >= 0 ) && ( x_in_domain < width1 ) ) {

                            greenPixelsExpanded[ x + y * widthNew ] =
                                image1_data[z_in_domain][ x_in_domain + y_in_domain * width1 ];

                        }

                        if( ( z_in_template >= 0 ) && ( z_in_template < depth0 ) &&
                            ( x_in_template >= 0 ) && ( x_in_template < width0 ) &&
                            ( y_in_template >= 0 ) && ( y_in_template < height0 ) ) {

                            magentaPixelsExpanded[ x + y * widthNew ] =
                                magentaPixels[ x_in_template + y_in_template * width0 ];

                        }
                    }
                }

                // System.out.println( "    Creating ColorProcessor and adding slice: " + z );

                ColorProcessor cp = new ColorProcessor( widthNew, heightNew );

                cp.setRGB( magentaPixelsExpanded, greenPixelsExpanded, magentaPixelsExpanded );

                stack.addSlice( null, cp );

                IJ.showProgress( (double) (z + 1) / depthNew );

            }

        }

        IJ.showProgress( 1.0 );

        ImagePlus impNew = new ImagePlus( "overlayed", stack );

        /* FIXME; why? read about calibration...
            if (image[0]!=null)
                imp2.setCalibration(image[0].getCalibration());
        */

        impNew.show();

    }

}

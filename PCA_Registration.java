import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;
import ij.plugin.filter.*;

import ij.measure.Calibration;

import java.awt.Color;
import java.io.*;


import math3d.Point3d;

import java.util.ArrayList;
import java.util.Comparator;

/* ------------------------------------------------------------------------

    Terminology note:

       I refer to the image that we transform as the "domain" image
       and the one which is kept in its original orientation as the
       "template" image.

     BUGS / FIXME:

       - sample spacing (aspect ratio) information is ignored at the
         moment.

   ------------------------------------------------------------------------ */

// A convenience class for returning Threshold information.

class Threshold {

    public int value;
    public long belowThreshold;

    public Threshold( int value, long belowThreshold ) {
        this.value = value;
        this.belowThreshold = belowThreshold;
    }

}

// The intensity mapping defined by this technique is rather crude,
// but seems to work well enough for this application.  (Note that it
// won't actually work as a standalone PlugInFilter at the moment.)

class IntensityMap implements PlugInFilter {

    public static int [] histogramToCumulative( int [] valueHistogram ) {

        int [] cumulative = new int[valueHistogram.length];

        // Each entry in cumulative gives us the number of points with
        // that value or lower.  (So,
        // cumulative[valueHistogram.length-1] should be the total
        // number of points referred to in the image.

        int acc = 0;

        for( int i = 0; i < valueHistogram.length; ++i ) {
            acc += valueHistogram[i];
            cumulative[i] = acc;
        }

        return cumulative;

    }

    private ImagePlus image;
    private String arg;

    public int setup( String arg, ImagePlus image ) {

        this.image = image;
        return DOES_8G;

    }

    public void run( ImageProcessor ip ) {

        ImageStack stack = image.getStack();

        for( int z = 0; z < image.getStackSize(); ++z ) {

            byte [] pixels = ( byte [] ) stack.getPixels( z + 1 );

            for( int i = 0; i < pixels.length; ++i ) {
                int v = domainToTemplate[ (int)( pixels[i] & 0xFF ) ];
                pixels[i] = (byte)v;
            }

            stack.setPixels( pixels, z + 1 );

        }

    }

    private int [] domainToTemplate;
    private int [] templateToDomain;

    public static IntensityMap fromHistograms( int [] valueHistogramTemplate,
                                               int [] valueHistogramDomain ) {
        return fromHistograms( valueHistogramTemplate,
                               valueHistogramDomain,
                               new Threshold( 0, 0 ),
                               new Threshold( 0, 0 ) );
    }

    public static IntensityMap fromHistograms(
        int [] valueHistogramTemplateOriginal,
        int [] valueHistogramDomainOriginal,
        Threshold thresholdTemplate,
        Threshold thresholdDomain ) {

        IntensityMap result = new IntensityMap();

        int [] valueHistogramTemplate = valueHistogramTemplateOriginal.clone();
        int [] valueHistogramDomain = valueHistogramDomainOriginal.clone();

        // Clear all the below-threshold buckets in the histogram.

        for( int i = 0; i < thresholdTemplate.value; ++i )
            valueHistogramTemplate[i] = 0;

        for( int i = 0; i < thresholdDomain.value; ++i )
            valueHistogramDomain[i] = 0;

        // Work out the cumulative distribution from those.

        int [] cumulativeTemplate = histogramToCumulative( valueHistogramTemplate );
        int [] cumulativeDomain = histogramToCumulative( valueHistogramDomain );

        // How many super-threshold points are in each?

        long pointsInTemplate = cumulativeTemplate[cumulativeTemplate.length-1];
        long pointsInDomain = cumulativeDomain[cumulativeDomain.length-1];

        // Convert those to proportions:

        float [] cumulativeProportionsTemplate = new float[cumulativeTemplate.length];
        float [] cumulativeProportionsDomain = new float[cumulativeDomain.length];

        for( int i = 0; i < cumulativeTemplate.length; ++i )
            cumulativeProportionsTemplate[i] = cumulativeTemplate[i] / (float)pointsInTemplate;

        /*
        System.out.println( "Proportions for template:" );
        for( int i = 0; i < cumulativeProportionsTemplate.length; ++i ) {
            System.out.println( "i[" + i + "]: " + cumulativeProportionsTemplate[i] );
        }
        */

        for( int i = 0; i < cumulativeDomain.length; ++i )
            cumulativeProportionsDomain[i] = cumulativeDomain[i] / (float)pointsInDomain;

        /*
        System.out.println( "Proportions for domain:" );
        for( int i = 0; i < cumulativeProportionsDomain.length; ++i ) {
            System.out.println( "i[" + i + "]: " + cumulativeProportionsDomain[i] );
        }
        */

        // ------------------------------------------------------------------------

        // Now build the map; whether we start with the domain or template.

        int [] domainToTemplate = new int[valueHistogramDomain.length];
        int [] templateToDomain = new int[valueHistogramTemplate.length];

        {
            int j = thresholdTemplate.value;

            for( int i = thresholdDomain.value; i < valueHistogramDomain.length; ++i ) {
                float propGEinDomain = cumulativeProportionsDomain[i];
                while( propGEinDomain > cumulativeProportionsTemplate[j] ) {
                    ++j;
                }
                domainToTemplate[i] = j;
            }

            /*
            System.out.println( "Found intensity mapping:" );
            for( int i = 0; i < valueHistogramDomain.length; ++i ) {
                System.out.println( "" + i + " -> " + domainToTemplate[i] );
            }
            */

        }

        {
            int j = 0;

            for( int i = 1; i < valueHistogramTemplate.length; ++i ) {
                float propGEinTemplate = cumulativeProportionsTemplate[i];
                while( propGEinTemplate > cumulativeProportionsDomain[j] ) {
                    ++j;
                }
                templateToDomain[i] = j;
            }

            /*
            System.out.println( "Found intensity mapping:" );
            for( int i = 0; i < valueHistogramTemplate.length; ++i ) {
                System.out.println( "" + i + " -> " + templateToDomain[i] );
            }
            */

        }

        result.domainToTemplate = domainToTemplate;
        result.templateToDomain = templateToDomain;

        return result;

    }


}

// A convenience class for returning the results of a Principal Components Analysis:

class PrincipalComponents {

    public double vectors[][];
    public double values[];
    public double meanXYZ[];

    public String toString( ) {

        String result = "Means in each dimension: ( " + meanXYZ[0] +
            ", " + meanXYZ[1] + ", " + meanXYZ[2] + ")\n";

        for( int i = 0; i < 3; ++ i ) {
            result += "  [ " + vectors[i][0] + ",   (eigenvalue: " + values[i] + ")\n";
            result += "    " + vectors[i][1] + ",\n";
            result += "    " + vectors[i][2] + " ]\n";
        }
        return result;
    }

    class MagnitudeComparator implements Comparator {

        public int compare( Object a, Object b ) {
            double x = (Double)a;
            double y = (Double)b;
            return Double.compare( Math.abs(x), Math.abs(y) );
        }

    }

    public FastMatrix correctAspect;

    public PrincipalComponents( double [] values,
                                double [][] vectors,
                                double meanXYZ[],
                                double relativeSpacingX,
                                double relativeSpacingY,
                                double relativeSpacingZ ) {

        correctAspect = (new FastMatrix(1.0)).scale( relativeSpacingX,
                                                     relativeSpacingY,
                                                     relativeSpacingZ );
        // The only subtlety here is that we sort the passed-in
        // eigevectors and eigenvalues based on the absolute size of
        // the eigenvalues.

        if( values.length != 3 )
            throw new IllegalArgumentException( "There must be 3 eigenvalues (not " + values.length + ")" );

        // Sort based on the magnitude, but the Arrays.sort method
        // with a comparator only works on objects, so...

        Double [] boxedEigenValues = new Double[3];
        for( int i = 0; i < 3; ++ i ) boxedEigenValues[i] = values[i];

        java.util.Arrays.sort( boxedEigenValues, new MagnitudeComparator() );

        this.values = new double[3];
        for( int i = 0; i < 3; ++ i ) this.values[i] = boxedEigenValues[i];

        if( (vectors.length != 3) || (vectors[0].length != 3) ||
            (vectors[1].length != 3) || (vectors[2].length != 3) ) {
            throw new IllegalArgumentException( "The eigenvecctors must be passed as double[3][3] array" );
        }

        boolean vectorsFilled[] = new boolean[3];
        vectorsFilled[0] = vectorsFilled[1] = vectorsFilled[2] = false;

        this.vectors = new double[3][];

        for( int i = 0; i < 3; ++i ) {
            int j;
            for( j = 0;
                 (vectorsFilled[j]) || (this.values[j] != values[i]);
                 ++j )
                ;
            this.vectors[j] = vectors[i].clone();
            vectorsFilled[j] = true;
        }

        if( meanXYZ.length != 3 )
            throw new IllegalArgumentException( "There must be 3 mean values (not " + meanXYZ.length + ")" );

        this.meanXYZ = meanXYZ.clone();

        assert (this.values != null);

        assert (this.vectors[0] != null);
        assert (this.vectors[1] != null);
        assert (this.vectors[2] != null);

        assert (this.meanXYZ != null);

    }

}


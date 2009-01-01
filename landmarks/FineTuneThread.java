/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package landmarks;

import ij.IJ;
import ij.ImagePlus;
import pal.math.ConjugateDirectionSearch;
import vib.FastMatrix;

/* This can all get very confusing, so to make my convention clear:

     green channel == fixed image == current image
     
     magenta channel == transformed image == cropped template image

   So we're transforming the template onto the current image.

       templatePoint is in the template
       guessedPoint is in the current image

*/

// FIXME: NamedPoint is now NamedPointWorld (check templatePoint and guessedPoint)

public class FineTuneThread extends Thread {
	
	boolean keepResults = true;
    
	int threadIndex;
	int totalThreads;
        int method;
        double cubeSide;
        ImagePlus croppedTemplate;
        ImagePlus template;
        NamedPointWorld templatePoint;
        ImagePlus newImage;
        NamedPointWorld guessedPoint;
        double [] guessedRotation;
        ProgressWindow progressWindow;
	Name_Points plugin;
	
        public FineTuneThread( 
		int threadIndex,
		int totalThreads,
		int method,
		double cubeSide,
                ImagePlus croppedTemplate, // The cropped template image.
                ImagePlus template, // The full template image.
                NamedPointWorld templatePoint,
                ImagePlus newImage, // The full current image.
                NamedPointWorld guessedPoint,
                double [] guessedRotation,
		ProgressWindow progressWindow,
                Name_Points plugin ) {
		
                this.threadIndex = threadIndex;
		this.totalThreads = totalThreads;
                this.method = method;
                this.cubeSide = cubeSide;
                this.croppedTemplate = croppedTemplate;
                this.template = template;
                this.templatePoint = templatePoint;
                this.newImage = newImage;
                this.guessedPoint = guessedPoint;
                this.guessedRotation = guessedRotation;
                this.progressWindow = progressWindow;
		this.plugin = plugin;
		
        }
	
	ConjugateDirectionSearch optimizer;
	
	@Override
	public void run() {
		
		FastMatrix scalePointInTemplate=FastMatrix.fromCalibration(croppedTemplate);
		FastMatrix scalePointInNewImage=FastMatrix.fromCalibration(newImage);
		FastMatrix inverseScalePointInNewImage=scalePointInNewImage.inverse();
		
		scalePointInTemplate.apply( templatePoint.x, templatePoint.y, templatePoint.z );
		scalePointInNewImage.apply( guessedPoint.x, guessedPoint.y, guessedPoint.z );
		
		double initial_trans_x = scalePointInNewImage.x;
		double initial_trans_y = scalePointInNewImage.y;
		double initial_trans_z = scalePointInNewImage.z;
		
		IJ.showProgress(0.01);
		
		/* We want to generate all possible rigid rotations
		   of one axis onto another.  So, the x axis can be
		   mapped on to one of 6 axes.  Then the y axis can
		   be mapped on to one of 4 axes.  The z axis can
		   then only be mapped onto 1 axis if the handedness
		   is to be preserved.
		   
		   As a special case, if guessedRotation is supplied then
		   we try that first.
		*/
		
		int firstRotation = (guessedRotation == null) ? 0 : -1;
		
		int rotation;
		for( rotation = firstRotation; rotation < 24; ++rotation ) {

			if( ((rotation - firstRotation) % totalThreads) != threadIndex ) {
				System.out.println("### Thread "+threadIndex+" skipping rotation "+rotation+" ###");
				continue;
			}

			System.out.println("### Thread "+threadIndex+" doing rotation "+rotation+" ###");
		
			if( guessedRotation == null )
				progressWindow.updateTriedSoFar( rotation, 24 );
			else
				progressWindow.updateTriedSoFar( rotation + 1, 25 );
			
			double [] startValues = new double[6];
			
			if( rotation < 0 ) {
				
				startValues[0] = guessedRotation[0];
				startValues[1] = guessedRotation[1];
				startValues[2] = guessedRotation[2];
				startValues[3] = initial_trans_x;
				startValues[4] = initial_trans_y;
				startValues[5] = initial_trans_z;
				
			} else {
				
				int firstAxis = rotation / 8;
				int firstAxisParity = 2 * ((rotation / 4) % 2) - 1;
				int secondAxisInformation = rotation % 4;
				int secondAxisIncrement = 1 + (secondAxisInformation / 2);
				int secondAxisParity = 2 * (secondAxisInformation % 2) - 1;
				int secondAxis = (firstAxis + secondAxisIncrement) % 3;
				
				double [] xAxisMappedTo = new double[3];
				double [] yAxisMappedTo = new double[3];
				
                                xAxisMappedTo[firstAxis] = firstAxisParity;
                                yAxisMappedTo[secondAxis] = secondAxisParity;
				
                                double [] zAxisMappedTo = FastMatrix.crossProduct( xAxisMappedTo, yAxisMappedTo );
				
                                System.out.println("x axis mapped to: "+xAxisMappedTo[0]+","+xAxisMappedTo[1]+","+xAxisMappedTo[2]);
                                System.out.println("y axis mapped to: "+yAxisMappedTo[0]+","+yAxisMappedTo[1]+","+yAxisMappedTo[2]);
                                System.out.println("z axis mapped to: "+zAxisMappedTo[0]+","+zAxisMappedTo[1]+","+zAxisMappedTo[2]);
				
                                double [][] m = new double[3][4];
				
                                m[0][0] = xAxisMappedTo[0];
                                m[1][0] = xAxisMappedTo[1];
                                m[2][0] = xAxisMappedTo[2];
				
                                m[0][1] = yAxisMappedTo[0];
                                m[1][1] = yAxisMappedTo[1];
                                m[2][1] = yAxisMappedTo[2];
				
                                m[0][2] = zAxisMappedTo[0];
                                m[1][2] = zAxisMappedTo[1];
                                m[2][2] = zAxisMappedTo[2];
				
                                FastMatrix rotationMatrix = new FastMatrix(m);
                                double [] eulerParameters = new double[6];
                                rotationMatrix.guessEulerParameters(eulerParameters);
				
				double z1 = eulerParameters[0];
				double x1 = eulerParameters[1];
				double z2 = eulerParameters[2];
				
				startValues[0] = z1;
				startValues[1] = x1;
				startValues[2] = z2;
				startValues[3] = initial_trans_x;
				startValues[4] = initial_trans_y;
				startValues[5] = initial_trans_z;
			}
			
			// Now create the optimizer, etc.
			
			optimizer = new ConjugateDirectionSearch();
			
			optimizer.step = 1;
			optimizer.scbd = 10.0;
			optimizer.illc = true;
			
			TransformationAttempt attempt = new TransformationAttempt(
				cubeSide,
				croppedTemplate,
				templatePoint,
				newImage,
				guessedPoint,
				method,
				progressWindow );
			
			optimizer.optimize(attempt, startValues, 2, 2);		
			
			if( pleaseStop ) {
				// FIXME: this will have to change....
				return;
			}
			
			// Now it should be optimized such that our result
			// is in startValues.
			
			System.out.println("startValues now: ");
			Name_Points.printParameters(startValues);
			
			if( pleaseStop ) {
				return;
			}
			
			// Now reproduce those results; they might be good...
			
			RegistrationResult r = Name_Points.mapImageWith(
				croppedTemplate,
				newImage,
				templatePoint,
				guessedPoint,
				startValues,
				cubeSide,
				method,
				false,
				"score: ");
			
			if( pleaseStop ) {
				return;
			}
			
			IJ.showProgress( (rotation + 1) / 24.0 );
			
		}
		
	}
	
        boolean pleaseStop = false;
	
        public void askToFinish() {
		pleaseStop = true;
		if( optimizer != null )
			optimizer.interrupt = true;
        }
	
}

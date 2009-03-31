/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package util;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.plugin.PlugIn;
import ij.process.FloatProcessor;
import java.awt.Font;
import java.awt.FontMetrics;
import vib.TransformedImage;

public class Histogram_2D implements PlugIn {

	// These values are set when calling start2DHistogram:
	int bins;
	long totalValues;
	long[][] counts = new long[bins][bins];
	float minValue;
	float maxValue;
	float rangeWidth;

	// Use these to keep statistics on the image:
	boolean keepStatistics = false;
	boolean correlationCalculated = false;
	float statsMinValue;
	float statsMaxValue;
	float sumX;
	float sumY;
	float sumXY;
	float sumXX;
	long statsValues;
	float fittedGradient;
	float fittedYIntercept;
	
	public void collectStatisticsFor(float statsMinValue, float statsMaxValue) {
		this.statsMinValue = statsMinValue;
		this.statsMaxValue = statsMaxValue;
		sumX = 0;
		sumY = 0;
		sumXX = 0;
		sumXY = 0;
		statsValues = 0;
		keepStatistics = true;
		fittedGradient = 0;
		fittedYIntercept = 0;
	}
	       
	public void start2DHistogram(
	    float minValue,
	    float maxValue,
	    int bins ) {
		
		this.bins = bins;
		this.totalValues = 0;
		this.counts = new long[bins][bins];
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.rangeWidth = maxValue - minValue;
		
		keepStatistics = false;
		correlationCalculated = false;
	}

	public void addImagePlusPair(
	    ImagePlus imageA,
	    ImagePlus imageB ) {
		
		ImageStack stackA = imageA.getStack();
		ImageStack stackB = imageB.getStack();
	
		int depth = imageA.getStackSize();
		int width = imageA.getWidth();
		int height = imageA.getHeight();
		
		int depthB = imageB.getStackSize();
		int widthB = imageB.getWidth();
		int heightB = imageB.getHeight();
		
		int typeA=imageA.getType();
		int typeB=imageB.getType();
		
		int bitDepthA=imageA.getBitDepth();
		int bitDepthB=imageB.getBitDepth();
		
		if( depth != depthB ||
		    width != widthB ||
		    height != heightB ) {
			String as=""+width+"x"+height+"x"+depth;
			String bs=""+widthB+"x"+heightB+"x"+depthB;			
			String error="Every image pair must have identical dimensions.\n";
			error += "("+imageA.getTitle()+" ("+as+") and ";
			error += imageB.getTitle()+" ("+bs+") do not.)";
			IJ.error(error);
			return;
		}
		
		if( bitDepthA != bitDepthB ) {
			IJ.error("Each image pair must be of the same bit depth.");
			return;
		}

		if( ! (bitDepthA == 8 || bitDepthA == 16) ) {
			IJ.error("Image pairs must be 8 bit or 16 bit images at the moment.");
			return;
		}

		IJ.showProgress(0);
		
		for (int z = 0; z < depth; ++z) {

			byte[] pixelsABytes = null;
			byte[] pixelsBBytes = null;
			
			short[] pixelsAShorts = null;
			short[] pixelsBShorts = null;
			
			if( bitDepthA == 8 ) {
				pixelsABytes = (byte[]) stackA.getPixels(z + 1);
				pixelsBBytes = (byte[]) stackB.getPixels(z + 1);
			} else if( bitDepthA == 16 ) {
				pixelsAShorts = (short[]) stackA.getPixels(z + 1);
				pixelsBShorts = (short[]) stackB.getPixels(z + 1);
			}

			for (int y = 0; y < height; ++y) {
				for (int x = 0; x < width; ++x) {

					float valueA = -1;
					float valueB = -1;
					
					if( bitDepthA == 8 ) {
						valueA = pixelsABytes[y * width + x] & 0xFF;
						valueB = pixelsBBytes[y * width + x] & 0xFF;
					} else if( bitDepthA == 16 ) {
						valueA = pixelsAShorts[y * width + x];
						valueB = pixelsBShorts[y * width + x];
					}

					if( keepStatistics &&
					    (valueA >= statsMinValue) && 
					    (valueB >= statsMinValue) &&
					    (valueA <= statsMaxValue) && 
					    (valueB <= statsMaxValue) ) {
						sumX += valueA;
						sumY += valueB;
						sumXX += valueA * valueA;
						sumXY += valueA * valueB;
						++ statsValues;
					}
					
					int i1 = (int)Math.floor((valueA - minValue) * bins / rangeWidth);
					int i2 = (int)Math.floor((valueB - minValue) * bins / rangeWidth);
					if( i1 >= bins )
						i1 = bins - 1;
					if( i2 >= bins )
						i2 = bins - 1;
					
					++counts[i1][i2];
					++totalValues;					
				}
			}
			IJ.showProgress(z/depth);
		}
		IJ.showProgress(1);
	}

	public void calculateCorrelation( ) {
		if( ! keepStatistics ) {
			throw new RuntimeException("calculateCorrelation() was called without collectStatisticsFor() having been called.");
		}
		float a = (statsValues * sumXY - sumX * sumY) / (statsValues * sumXX - sumX * sumX);
		float b = (sumY - a * sumX) / statsValues;
		fittedGradient = a;
		fittedYIntercept = b;
		correlationCalculated = true;
	}
	
	public ImagePlus [] getHistograms( ) {
		
		System.out.println("totalValues is: "+totalValues);
		
		double [][] p = new double[bins][bins];
		double [][] selfInformation = new double[bins][bins];		
		
		for( int avalue = 0; avalue < bins; ++avalue )
			for( int bvalue = 0; bvalue < bins; ++bvalue ) {
				
				
				p[avalue][bvalue] = (double)counts[avalue][bvalue] / totalValues;
				selfInformation[avalue][bvalue] = - Math.log(p[avalue][bvalue]) / Math.log(2);
			}

		ImagePlus newImagePlus;		
		{
		
			float floatValues [] = new float[bins*bins];

			for( int avalue = 0; avalue < bins; ++avalue )
				for( int bvalue = 0; bvalue < bins; ++bvalue ) {
					floatValues[((bins-1)-bvalue)*bins+avalue] = (float)Math.log(p[avalue][bvalue]);
				}

			FloatProcessor fp = new FloatProcessor(bins,bins);
			fp.setPixels(floatValues);			
			ImageStack newStack=new ImageStack(bins,bins);
			newStack.addSlice("", fp);
			newImagePlus=new ImagePlus("2D Histogram Probabilities",newStack);

		}
		
		ImagePlus selfNewImagePlus;
		{

			float selfValues [] = new float[bins*bins];

			for( int avalue = 0; avalue < bins; ++avalue )
				for( int bvalue = 0; bvalue < bins; ++bvalue ) {

					selfValues[((bins-1)-bvalue)*bins+avalue] = (float)selfInformation[avalue][bvalue];
				}

			FloatProcessor selfFP = new FloatProcessor(bins,bins);
			selfFP.setPixels(selfValues);
			ImageStack selfNewStack=new ImageStack(bins,bins);
			selfNewStack.addSlice("", selfFP);
			selfNewImagePlus=new ImagePlus("Self Information",selfNewStack);

		}
		
		
		ImagePlus [] result = new ImagePlus[2];
		result[PROBABILITIES] = newImagePlus;
		result[SELF_INFORMATION] = selfNewImagePlus;

		return result;
	}

	public ImagePlus frame2DHistogram(
	    String title,
	    ImagePlus histogram,
	    String xLabel, float xmin, float xmax,
	    String yLabel, float ymin, float ymax,
	    int method ) {

		int tickSize = 5;
		int tickMargin = 10;
		boolean serifFont = false;
		int fontSize = 10;
		int titleSize = 12;
		
		int leftBorder = 100;
		int rightBorder = 180;
		int topBorder = 60;
		int bottomBorder = 100;

		if( histogram.getType()	!= ImagePlus.GRAY32 ) {
			IJ.error("frame2DHistogram only works on GRAY32 (Float) 2D histogram images");
			return null;
		}
		
		if( histogram.getStackSize() != 1 ) {
			IJ.error("The histogram must not be a stack.");
			return null;
		}
		
		int oldWidth=histogram.getWidth();
		int oldHeight=histogram.getHeight();
		FloatProcessor oldFP=(FloatProcessor)histogram.getProcessor();
		float oldMin=(float) oldFP.getMin();
		float oldMax=(float) oldFP.getMax();
		float [] oldFloats=(float[])oldFP.getPixels();
		
		int newWidth=oldWidth+leftBorder+rightBorder;
		int newHeight=oldHeight+topBorder+bottomBorder;		
		float[] newFloats=new float[newWidth*newHeight];
		for(int i=0;i<newFloats.length;++i)
			newFloats[i]=oldMax;
		
		for(int y=0;y<oldHeight;++y) {
			for(int x=0;x<oldWidth;++x) {
				newFloats[(y+topBorder)*newWidth+(x+leftBorder)] =
				    oldFloats[y*oldWidth+x];
			}
		}
		
		FloatProcessor newFP=new FloatProcessor(newWidth,newHeight);
		newFP.setPixels(newFloats);
		newFP.setMinAndMax(oldMin, oldMax);

		newFP.setValue(oldMin);

		// Draw ticks:
		newFP.drawLine(
		    leftBorder,
		    topBorder+oldHeight,
		    leftBorder,
		    topBorder+oldHeight+tickSize);	
		newFP.drawLine(
		    leftBorder+oldWidth-1,
		    topBorder+oldHeight,
		    leftBorder+oldWidth-1,
		    topBorder+oldHeight+tickSize);
		newFP.drawLine(
		    leftBorder-1,
		    topBorder,
		    (leftBorder-1)-tickSize,
		    topBorder);
		newFP.drawLine(
		    leftBorder-1,
		    topBorder+oldHeight-1,
		    (leftBorder-1)-tickSize,
		    topBorder+oldHeight-1);		
		
		ImagePlus newImagePlus=new ImagePlus(
		    "Framed Histogram",
		    newFP );

		String fontName = serifFont ? "Serif" : "SanSerif";
		int fontType = false ? Font.BOLD : Font.PLAIN;
		Font font=new Font(fontName, fontType, fontSize);
		
		newImagePlus.show();		
		ImageCanvas ic=newImagePlus.getCanvas();
		FontMetrics fm=ic.getFontMetrics(font);	
		
		newFP.setFont(font);
		newFP.setAntialiasedText(true);		
	
		String sXmin=""+xmin;
		String sXmax=""+xmax;
		String sYmin=""+ymin;
		String sYmax=""+ymax;
		
		newFP.drawString(
		    sXmin,
		    leftBorder - (fm.stringWidth(sXmin) / 2),
		    topBorder + oldHeight + tickMargin + fm.getHeight() );
		newFP.drawString(
		    sXmax,
		    leftBorder+oldWidth - (fm.stringWidth(sXmax) / 2),
		    topBorder + oldHeight + tickSize + tickMargin + fm.getHeight() );
		newFP.drawString(
		    sYmin,
		    leftBorder - tickMargin - fm.stringWidth(sYmin) - tickSize,
		    topBorder + oldHeight + fm.getHeight() / 2 );
		newFP.drawString(
		    sYmax,
		    leftBorder - tickMargin - fm.stringWidth(sYmax) - tickSize,
		    topBorder + fm.getHeight() / 2 );		
		
		newFP.drawString(
		    xLabel,
		    leftBorder + oldWidth / 2 - fm.stringWidth(xLabel) / 2,
		    topBorder + oldHeight + tickSize + 2 * tickMargin + 2 * fm.getHeight() );
		
		/* Draw a similar label in a new FloatProcessor and copy
		 * it over. */
		
		int labelWidth=fm.stringWidth(yLabel);
		int labelHeight=fm.getHeight();
		
		FloatProcessor fpToRotate=new FloatProcessor(labelWidth,labelHeight);
		float [] labelFloats=new float[labelWidth*labelHeight];
		for( int i = 0; i < labelFloats.length; ++i )
			labelFloats[i] = oldMax;
		fpToRotate.setFont(font);
		fpToRotate.setPixels(labelFloats);
		fpToRotate.setValue(oldMin);
		fpToRotate.setMinAndMax(oldMin,oldMax);
		fpToRotate.drawString(yLabel,0,labelHeight);		
		
		int yLabelTopLeftX = leftBorder - tickSize - tickMargin - labelHeight * 2;
		int yLabelTopLeftY = topBorder + (oldHeight / 2) - (labelWidth / 2);
		
		for(int y=0;y<labelHeight;++y)
			for(int x=0;x<labelWidth;++x) {
				int newX= yLabelTopLeftX + y;
				int newY= yLabelTopLeftY + labelWidth - x;
				newFloats[newY*newWidth+newX]=labelFloats[y*labelWidth+x];
			}
				
		/* Now draw a bar at the side showing the value range. */
		
		int barWidth = 30;
		int barHeight = (oldHeight * 2) / 3;
		
		int barTopLeftX = leftBorder + oldWidth + 40;
		int barTopLeftY = topBorder + (oldHeight - barHeight) / 2;
		
		newFP.drawRect(barTopLeftX, barTopLeftY, barWidth+2, barHeight+2);
		
		for(int barOffset=0;barOffset<barHeight;++barOffset) {
			int barLineX1=barTopLeftX+1;
			int barLineX2=barTopLeftX+barWidth;
			int barLineY=barTopLeftY+1+(barHeight-(barOffset+1));
			float value=((float)barOffset*(oldMax-oldMin))/(barHeight-1)+oldMin;
			newFP.setValue(value);
			newFP.drawLine(barLineX1,barLineY,barLineX2,barLineY);
		}
		
		/* Now add some tick marks to the bar */
		newFP.setValue(oldMin);
		newFP.drawLine(
		    barTopLeftX+barWidth+2,
		    barTopLeftY,
		    barTopLeftX+barWidth+2+tickSize,
		    barTopLeftY);
		newFP.drawString(
		    ""+oldMax,
		    barTopLeftX+barWidth+2+tickSize+tickMargin,
		    barTopLeftY+fm.getHeight()/2
		    );
		newFP.drawLine(
		    barTopLeftX+barWidth+2,
		    barTopLeftY+barHeight+1,
		    barTopLeftX+barWidth+2+tickSize,
		    barTopLeftY+barHeight+1);
		newFP.drawString(
		    ""+oldMin,
		    barTopLeftX+barWidth+2+tickSize+tickMargin,
		    barTopLeftY+barHeight+fm.getHeight()/2
		    );
		
		/* Now just draw the title */
		
		fontType = Font.BOLD;
		Font titleFont=new Font(fontName, fontType, titleSize);

		FontMetrics titleFM=ic.getFontMetrics(font);	
		
		newFP.setFont(titleFont);
		newFP.drawString(
		    title,
		    newWidth / 2 - titleFM.stringWidth(title) / 2,
		    topBorder / 2 + titleFM.getHeight() / 2 );
		
		/* If a line fit has been calculated, draw that over
		 * the image... */

		if( correlationCalculated ) {
			
			// Draw the fitted line onto the histogram (as
			// a dotted line)...

			newFP.drawPixel( 10, 10 );

			if( fittedGradient <= 1 ) {

				for( int xBin=0; xBin<bins; ++xBin ) {
					float realX = minValue + ( (xBin+0.5f) / bins ) * rangeWidth;
					System.out.println("xBin "+xBin+" mapped to "+realX);
					float realY = fittedGradient * realX + fittedYIntercept;
					int yBin = (int)Math.floor((realY - minValue) * bins / rangeWidth);
					System.out.println("bin: ("+xBin+","+yBin+")");
					if( yBin >= 0 && yBin < bins ) {
						newFP.setValue( (xBin % 2) == 0 ? minValue : maxValue );
						newFP.drawPixel( leftBorder+xBin, topBorder+oldHeight-yBin );
					}
				}

			} else {

				for( int yBin=0; yBin<bins; ++yBin ) {
					float realY = minValue + ( (yBin+0.5f) / bins ) * rangeWidth;
					System.out.println("yBin "+yBin+" mapped to "+realY);
					float realX = (realY - fittedYIntercept) / fittedGradient;
					int xBin = (int)Math.floor((realX - minValue) * bins / rangeWidth);
					System.out.println("bin: ("+xBin+","+yBin+")");
					if( xBin >= 0 && xBin < bins ) {
						newFP.setValue( (yBin % 2) == 0 ? minValue : maxValue );
						newFP.drawPixel( leftBorder+xBin, topBorder+oldHeight-yBin );
					}
				}

			}

		}

		newImagePlus.updateAndRepaintWindow();
		
		return newImagePlus;
	}      	

	public final static int PROBABILITIES = 0;
	public final static int SELF_INFORMATION = 1;
		
	public void run(String ignored) {
                
		String titleSubstring = "";
		
		int[] wList = WindowManager.getIDList();
		if (wList == null) {
			IJ.error("No images are open.");
			return;
		}

                String [] matchingTitles=new String[wList.length];
                ImagePlus [] matchingImagePlus=new ImagePlus[wList.length];
                ImagePlus [] allImages=new ImagePlus[wList.length];
                
                int totalMatchingTitles = 0;
		for (int i = 0; i < wList.length; i++) {
			ImagePlus imp = WindowManager.getImage(wList[i]);
                        String title = (imp == null) ? "" : imp.getTitle();
                        if(title.indexOf(titleSubstring) >= 0) {
                            matchingTitles[totalMatchingTitles] = title;
                            matchingImagePlus[totalMatchingTitles] = imp;
                            ++totalMatchingTitles;
                        }
                        allImages[i] = imp;
		}
                
                if( totalMatchingTitles < 2 ) {
                    IJ.error("There are only "+totalMatchingTitles+" matching images; need at least 2.");
                    return;
                }
                
                String [] onlyMatchingTitles = new String[totalMatchingTitles];
                System.arraycopy(matchingTitles,0,onlyMatchingTitles,0,totalMatchingTitles);
                ImagePlus [] onlyMatchingImagePlus = new ImagePlus[totalMatchingTitles];
                System.arraycopy(matchingImagePlus, 0, onlyMatchingImagePlus, 0, totalMatchingTitles);

		String [] methods = { "p (Probability)", "-log\u2082(p) (Self-information)" };

		GenericDialog gd = new GenericDialog("2D Histogram");
		gd.addChoice("A:", onlyMatchingTitles, onlyMatchingTitles[0]);
		gd.addChoice("B:", onlyMatchingTitles, onlyMatchingTitles[1]);
		gd.addChoice("Values to plot: ", methods, methods[0]);
		gd.showDialog();
		if (gd.wasCanceled()) {
			return;
		}

		int[] index = new int[2];
		index[0] = gd.getNextChoiceIndex();
		index[1] = gd.getNextChoiceIndex();

		int method = gd.getNextChoiceIndex();

		ImagePlus [] sourceImages = new ImagePlus[2];
		
		sourceImages[0] = onlyMatchingImagePlus[index[0]];
		sourceImages[1] = onlyMatchingImagePlus[index[1]];
		
		IJ.showStatus( "Calculating values range..." );
                float[] valueRange;
                {
                    TransformedImage ti = new TransformedImage(
                        sourceImages[0],
                        sourceImages[1]);

                    valueRange = ti.getValuesRange();
                }
                
                sourceImages[0].getProcessor().setMinAndMax(valueRange[0],valueRange[1]);
                sourceImages[1].getProcessor().setMinAndMax(valueRange[0],valueRange[1]);
                
		int width = sourceImages[0].getWidth();
		int height = sourceImages[0].getHeight();
		int depth = sourceImages[0].getStackSize();
		
		if ( ! (width == sourceImages[1].getWidth() &&
			height == sourceImages[1].getHeight() &&
			depth == sourceImages[1].getStackSize())) {
			IJ.error("The dimensions of image stack " +
				 sourceImages[1].getTitle() +
				 " do not match those of " + sourceImages[0].getTitle());
			return;
		}
		
		start2DHistogram(
			valueRange[0],
			valueRange[1],
			256 );

		IJ.showStatus("Binning values from the images...");
		addImagePlusPair(sourceImages[0],sourceImages[1]);
		
		ImagePlus[] results = getHistograms();
		
		frame2DHistogram(
		    methods[method] + " for Pairs of Values",
		    results[method],
		    sourceImages[0].getTitle(),
		    valueRange[0], valueRange[1],
		    sourceImages[1].getTitle(),
		    valueRange[0], valueRange[1],
		    method );
	}
}

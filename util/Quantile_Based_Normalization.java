/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007 Mark Longair */

/*
    This file is part of the ImageJ plugin "Quantile Based Normalization".

    The ImageJ plugin "Quantile Based Normalization" is free software;
    you can redistribute it and/or modify it under the terms of the
    GNU General Public License as published by the Free Software
    Foundation; either version 3 of the License, or (at your option)
    any later version.

    The ImageJ plugin "Quantile Based Normalization" is distributed in
    the hope that it will be useful, but WITHOUT ANY WARRANTY; without
    even the implied warranty of MERCHANTABILITY or FITNESS FOR A
    PARTICULAR PURPOSE.  See the GNU General Public License for more
    details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package util;

import ij.*;
import ij.gui.GenericDialog;
import ij.process.*;
import ij.plugin.*;
import ij.io.*;
import java.util.Random;

public class Quantile_Based_Normalization implements PlugIn {

    /* The idea of this normalization is to rank all of the values in
       an image, divide those up into a number of quantiles and replace
       each value in each quantile with the mean in that rank.  If you
       replace with the rank instead of the mean then you get histogram
       equalization.

       FIXME: in practice this isn't very useful with just a single
       image - my main Thing To Do is to get this to work across
       multiple images.
       */

    /* The only subtlety with the former method is that the mean is not
       going to be integral, so we randomly replace the values in a quatile
       with a mix of the two bytes around the mean in such a proportion that
       the mean will be a close to correct as we can get it. */

    class Replacements {

            long [] replacements;
            long totalReplacements;
            int minReplacement = Integer.MAX_VALUE;
            int maxReplacement = Integer.MIN_VALUE;
            Random rng;
            int quantile;

            public Replacements(int possibleValues) {
                replacements = new long[possibleValues];
                rng=new Random();
            }

            public void addSomeReplacements( long howManyToReplace, int replacement ) {
                if( replacement < minReplacement )
                    minReplacement = replacement;
                if( replacement > maxReplacement )
                    maxReplacement = replacement;
                replacements[replacement] += howManyToReplace;
                totalReplacements += howManyToReplace;
            }

            public int getRandomReplacement() {
                if( totalReplacements == 0 ) {
                    return -1;
                }

                long index=Math.abs(rng.nextLong()) % totalReplacements;

                long replacementsSkipped = 0;

                for( int r = minReplacement; r <= maxReplacement; ++r ) {

                    long indexInThisSlot = index - replacementsSkipped;

                    if( indexInThisSlot < replacements[r] ) {
                        // Then we remove one of these and return
                        // the value of r.
                        -- replacements[r];
                        -- totalReplacements;
                        return r;
                    } else {
                        replacementsSkipped += replacements[r];
                    }
                }
                return -1;
            }

            @Override
            public String toString() {
                if( totalReplacements == 0 )
                    return "No replacements left.";
                else {
                    String result = "" + totalReplacements + " replacements left (in";
                    for( int i = minReplacement; i <= maxReplacement; ++i ) {
                        if( replacements[i] > 0 )
                            result += " " + i + " (" + replacements[i] + ")";
                    }
                    return result;
                }

            }

    }

	public void run(String ignored) {
		
                ImagePlus imagePlus = WindowManager.getCurrentImage();
                if( imagePlus == null ) {
                    IJ.error("No current image to operate on.");
                    return;
                }

                int type=imagePlus.getType();
                if( ! ((type == ImagePlus.GRAY8) || (type == ImagePlus.COLOR_256)) ) {
                    IJ.error("This plugin only works on 8bit (GRAY8 or COLOR_256) images.");
                    return;
                }

                GenericDialog gd=new GenericDialog("Quantile Normalization");
                gd.addNumericField("Quantiles", 256, 0);
                String [] choices={ "mean", "rank"};
                gd.addChoice("Replace each quantile with", choices, "rank");
                gd.addCheckbox("Rescale (if replacing with ranks)", true);

                gd.showDialog();

                if(gd.wasCanceled())
                    return;

                int numberOfQuantiles = (int)gd.getNextNumber();
                if( numberOfQuantiles < 1 || numberOfQuantiles > 256 ) {
                    IJ.error("Number of quantiles must be between 1 and 256 inclusive.");
                    return;
                }

                boolean replaceWithRankInstead=false;
                String choice=gd.getNextChoice();
                replaceWithRankInstead = choice.equals("rank");

                boolean rescaleRanks=gd.getNextBoolean();

                int width=imagePlus.getWidth();
                int height=imagePlus.getHeight();
                int depth=imagePlus.getStackSize();

                Replacements [] meanReplacements = new Replacements[256];
                for( int b = 0; b < 256; ++b )
                    meanReplacements[b] = new Replacements(256);

                Replacements [] rankReplacements = new Replacements[256];
                for( int b = 0; b < 256; ++b )
                    rankReplacements[b] = new Replacements(numberOfQuantiles);

                long frequencies[] = new long[256];

                ImageStack stack=imagePlus.getStack();

                IJ.showStatus("Calculating frequencies...");

                for( int z=0; z<depth; ++z ) {
                    byte [] pixels=(byte[])stack.getPixels(z+1);
                    for( int y=0; y<height; ++y )
                        for( int x=0; x<width; ++x ) {
                            int value=pixels[y*width+x]&0xFF;
                            ++frequencies[value];
                        }
                }

                long pointsInImage = width*height*depth;

                IJ.showStatus("Dividing into quantiles...");

 		for (int q = 0; q < numberOfQuantiles; ++q) {

                        long [] replacementsInThisQuartile=new long[256];

			long indexStartThisQuantile = (int) (q * pointsInImage / numberOfQuantiles);
			long indexStartNextQuantile = (int) (((q + 1) * pointsInImage) / numberOfQuantiles);

			long pointsInQuartile = indexStartNextQuantile - indexStartThisQuantile;

			// If this is the last quantile, make sure we actually
			// include everything...
			if (q == numberOfQuantiles - 1) {
				indexStartNextQuantile = pointsInImage;
			}

			// Calculate mean in this quantile...
			long cumulativeIncluding = 0;
			long cumulativeBefore = 0;

			long sumValuesInQuartile = 0;
			long numberOfValuesInQuartile = 0;

			for (int value = 0; value < frequencies.length; ++value) {

				cumulativeIncluding += frequencies[value];

				if ((cumulativeIncluding < indexStartThisQuantile) || (cumulativeBefore >= indexStartNextQuantile)) {

					// Then there's no overlap...

				} else {

					long startInValues = 0;

					if (indexStartThisQuantile > cumulativeBefore) {
						startInValues = indexStartThisQuantile - cumulativeBefore;
					}

                                        // This is the end inclusive...
					long endInValues = frequencies[value] - 1;

					if (indexStartNextQuantile < cumulativeIncluding) {
						endInValues = (indexStartNextQuantile - cumulativeBefore) - 1;
					}
					long pointsInOverlap = (endInValues - startInValues) + 1;
					numberOfValuesInQuartile += pointsInOverlap;
					sumValuesInQuartile += value * pointsInOverlap;
                                        replacementsInThisQuartile[value] = pointsInOverlap;
				}

				cumulativeBefore += frequencies[value];
			}

			double mean = sumValuesInQuartile / (double) numberOfValuesInQuartile;

			int byteLowerThanMean = (int) Math.floor(mean);
			int byteHigherThanMean = (int) Math.ceil(mean);

			double proportionLower = Math.ceil(mean) - mean;
			int lowerBytes = (int) Math.round(proportionLower*(indexStartNextQuantile-indexStartThisQuantile));
			int higherBytes = (int) (numberOfValuesInQuartile - lowerBytes);
        
                        long replacementsAddedAlready = 0;

                        for( int i = 0; i < 256; ++i ) {

                            long r = replacementsInThisQuartile[i];

                            if( r == 0 )
                                continue;

                            long howManyLowerToAdd = 0;
                            long howManyHigherToAdd = 0;

                            if( replacementsAddedAlready >= lowerBytes ) {
                                howManyHigherToAdd = r;
                            } else if( replacementsAddedAlready + r >= lowerBytes ) {
                                howManyLowerToAdd = lowerBytes - replacementsAddedAlready;
                                howManyHigherToAdd = r - howManyLowerToAdd;
                            } else {
                                howManyLowerToAdd = r;
                            }

                            
                            meanReplacements[i].addSomeReplacements(howManyLowerToAdd, byteLowerThanMean);
                            meanReplacements[i].addSomeReplacements(howManyHigherToAdd, byteHigherThanMean);

                            rankReplacements[i].addSomeReplacements(r, q);

                            replacementsAddedAlready += r;

                        }
		}

                IJ.showStatus("Replacing values....");

                IJ.showProgress(0);

                ImageStack newStack = new ImageStack(width,height);
                for( int z = 0; z < depth; ++z ) {
                    byte [] oldPixels = (byte[])stack.getPixels(z+1);
                    byte [] newPixels = new byte[width*height];
                    for( int y = 0; y < height; ++y )
                        for( int x = 0; x < width; ++x ) {
                            int oldValue = oldPixels[y*width+x]&0xFF;
                            int replacement;
                            if( replaceWithRankInstead ) {
                                replacement = rankReplacements[oldValue].getRandomReplacement();
                                if(rescaleRanks)
                                    replacement = (255*replacement) / (numberOfQuantiles - 1);
                            } else {                              
                                replacement = meanReplacements[oldValue].getRandomReplacement();
                            }
                            if( replacement < 0 ) {
                                System.out.println("BUG: ran out of replacements for "+oldValue);
                                newPixels[y*width+x] = (byte)oldValue;
                            } else {
                                newPixels[y*width+x] = (byte)replacement;
                            }
                    }
                    ByteProcessor bp=new ByteProcessor(width,height);
                    bp.setPixels(newPixels);
                    newStack.addSlice("",bp);

                    IJ.showProgress( z / (double)depth );

                }

                IJ.showProgress(1.0);

                ImagePlus newImage = new ImagePlus( "normalized "+imagePlus.getTitle(), newStack );
                newImage.setCalibration(imagePlus.getCalibration());

                newImage.show();
	}
}

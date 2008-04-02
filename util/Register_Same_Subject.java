/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package util;

import distance.MutualInformation;
import distance.PixelPairs;
import ij.IJ;
import ij.ImagePlus;
import ij.ImagePlus;
import ij.Macro;
import ij.io.FileSaver;
import ij.plugin.PlugIn;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import vib.FastMatrix;
import vib.RigidRegistration_;
import vib.TransformedImage;

public class Register_Same_Subject implements PlugIn {
	
	public String findCommonPrefix(String[] a) {
		assert a.length > 0;
		String lastPrefix = "";
		for (int afterPrefix = 1; afterPrefix <= a[0].length(); ++afterPrefix) {
			String candidatePrefix = a[0].substring(0, afterPrefix);
			boolean prefixOfAll = true;
			for (int string = 0; string < a.length; ++string) {
				if (!a[string].startsWith(candidatePrefix)) {
					prefixOfAll = false;
					break;
				}
			}
			if (prefixOfAll) {
				lastPrefix = candidatePrefix;
			} else {
				break;
			}
		}
		return lastPrefix;
	}
	
		
	int channelForRegistration = 0;
	
	public void processSubjectImages( String [] filenamesForSubject, String baseDirectory ) throws FileNotFoundException, UnsupportedEncodingException, IOException {
						
		String prefix = findCommonPrefix(filenamesForSubject);
		System.out.println("Got common prefix: " + prefix);

		// Register against the middle image:
		int templateIndex = filenamesForSubject.length / 2;

		ImagePlus templateImagePlus = null;
		File templateFile=null;
		// Just load the template image in this block:
		{
			String templateFilename = baseDirectory + filenamesForSubject[templateIndex];
			templateFile = new File(templateFilename);
			try {
				templateImagePlus = BatchOpener.openParticularChannel(
				    templateFilename,
				    channelForRegistration);
				if (templateImagePlus == null) {
					IJ.error("File not found: " + templateFilename);
					return;
				}
			} catch (NoSuchChannelException e) {
				IJ.error("Zero-indexed channel " + templateIndex +
				    " not found in: " + templateFilename);
				return;
			}
		}

		// Make sure that the output directory exists:
		
		String outputDirectory = baseDirectory + "registered" + File.separator;
		File outputDirectoryFile = new File(outputDirectory);
		outputDirectoryFile.mkdir();
		if (!(outputDirectoryFile.exists() && outputDirectoryFile.isDirectory())) {
			IJ.error("'" + baseDirectory + "' wasn't created properly");
			return;
		}

		// Now register them one-by-one, or just write out the template:
		
		String registeredFormatString = outputDirectory + "%simage%d-channel%d.tif";
		String transformationFormatString = outputDirectory + "%s%d-TO-%d.txt";
		String histogramFormatString = outputDirectory + "%schannel%d-%d-VS-%d-histogram.png";
		String detectorFormatString = outputDirectory + "%schannel%d-%d-VS-%d-detectors.csv";
		
		for (int filenameIndex = 0;
		    filenameIndex < filenamesForSubject.length;
		    ++filenameIndex) {

			String filename = baseDirectory + filenamesForSubject[filenameIndex];
			File file = new File(filename);
			
			if (filenameIndex == templateIndex) {
				// Just write them out again:
				ImagePlus[] channels = BatchOpener.open(filename);
				for (int c = 0; c < channels.length; ++c) {

					String outputFilename = String.format(
					    registeredFormatString,
					    prefix, filenameIndex, c);

					System.out.println("Saving to: " + outputFilename);
					if (new File(outputFilename).exists()) {
						channels[c].close();
						continue;
					}

					boolean saved = new FileSaver(channels[c]).saveAsTiffStack(outputFilename);
					if (!saved) {
						IJ.error("Failed to save: '" + outputFilename + "'");
						return;
					}

					channels[c].close();
				}

			} else {

				// We actually have to do some registration:
				ImagePlus[] channels = BatchOpener.open(filename);

				// Does the transformation already exist?
				String transformationFilename = String.format(
				    transformationFormatString,
				    prefix,
				    filenameIndex,
				    templateIndex);

				File transformationFile = new File(transformationFilename);
				FastMatrix matrix = null;

				if (transformationFile.exists()) {

					// Load it...
					BufferedReader br = null;
					try {
						br = new BufferedReader(new FileReader(transformationFilename));
						String firstLine = br.readLine();
						matrix = FastMatrix.parseMatrix(firstLine);
						System.out.println("Parsed: " + firstLine);
						System.out.println("    to: " + matrix.toString());
					} catch (FileNotFoundException e) {
						IJ.error("BUG: File not found for: " + transformationFilename);
						return;
					} catch (IOException e) {
						IJ.error("IOException when reading from: " + transformationFilename);
						return;
					}

				} else {

					// Generate the transformation anew and write it to disk:

					System.out.println("templateImagePlus is: " + templateImagePlus);
					System.out.println("toTransform is: " + channels[channelForRegistration]);

					TransformedImage ti = new TransformedImage(
					    templateImagePlus,
					    channels[channelForRegistration]);

					float[] valueRange = ti.getValuesRange();

					ti.measure = new distance.MutualInformation(valueRange[0], valueRange[1], 256);

					RigidRegistration_ registrar = new RigidRegistration_();
					registrar.setup("", channels[channelForRegistration]);

					int level = RigidRegistration_.guessLevelFromWidth(templateImagePlus.getWidth());

					System.out.println("Registering...");

					matrix = registrar.rigidRegistration(
					    ti,
					    "", // material b box
					    "", // initial
					    -1, // material 1
					    -1, // material 2
					    false, // no optimization
					    level, // level
					    level > 2 ? 2 : level, // stop level
					    0.1, // tolerance
					    1, // number of initial positions
					    false, // show transformed
					    false, // show difference image
					    false, // fast but inaccurate
					    null);

					/*                                                
					double [][] matrixArrays = 
					{ {  1,  0,  0,   0 },
					{  0,  1,  1,   0 },
					{  0,  0, -1,  20 } };
					matrix = new FastMatrix(matrixArrays);
					 */
					// We've just calculated the transformation now, haven't
					// applied it.  Write it to disk first:

					try {
						PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(transformationFilename), "UTF-8"));
						pw.print(matrix.toString());
						pw.close();
					} catch (FileNotFoundException e) {
						IJ.error("Can't find file " + transformationFilename);
					} catch (IOException e) {
						IJ.error("Can't write to file " + transformationFilename);
					}

				}
				
				// Transform each channel:
				
				for (int c = 0; c < channels.length; ++c) {

					String outputFilename = String.format(
					    registeredFormatString,
					    prefix, filenameIndex, c);

					if (new File(outputFilename).exists()) {
						channels[c].close();
						continue;
					}

					// So we do have to map it...

					PixelPairs measure = new MutualInformation(0, 4095, 256);

					ImagePlus toTransform = channels[c];
					TransformedImage transOther = new TransformedImage(
					    templateImagePlus,
					    toTransform);
					transOther.measure = measure;
					transOther.setTransformation(matrix);

					ImagePlus result = transOther.getTransformed();

					boolean saved = new FileSaver(result).saveAsTiffStack(outputFilename);
					if (!saved) {
						IJ.error("Failed to save: '" + outputFilename + "'");
						return;
					}

					result.close();
					channels[c].close();
				}

				/* Now some analysis.  These two images
				   are different and we construct a histogram
				   between the two. */

				File fileForDarkerRegions;
				int indexForDarkerRegions;
				File fileForBrighterRegions;
				int indexForBrighterRegions;
				
				// The indices are ordered such that lower indices
				// have better detail for dark regions:
				if( templateIndex < filenameIndex ) {
					fileForDarkerRegions = templateFile;
					indexForDarkerRegions = templateIndex;
					fileForBrighterRegions = file;
					indexForBrighterRegions = filenameIndex;
				} else {
					fileForDarkerRegions = file;
					indexForDarkerRegions = filenameIndex;
					fileForBrighterRegions = templateFile;
					indexForBrighterRegions = templateIndex;
				}
				
				ChannelDataLSM darkDetailChannelData [] =
				    ChannelDataLSM.getChannelsData(fileForDarkerRegions);
				ChannelDataLSM brightDetailChannelData [] =
				    ChannelDataLSM.getChannelsData(fileForBrighterRegions);
				
				for (int c = 0; c < channels.length; ++c) {

					String dChannelFilename = String.format(
					    registeredFormatString,
					    prefix, indexForDarkerRegions, c);
				
					String bChannelFilename = String.format(
					    registeredFormatString,
					    prefix, indexForBrighterRegions, c);
					
					ImagePlus dChannel =
					    BatchOpener.openFirstChannel(dChannelFilename);
					ImagePlus bChannel =
					    BatchOpener.openFirstChannel(bChannelFilename);
					
					TransformedImage ti = new TransformedImage(
					    dChannel,
					    bChannel);

					float [] valueRange = ti.getValuesRange();

					// The maximum value that a voxel might have
					float vMax = -1;
					if( valueRange[1] < 256 ) {
						// Probably 8 bit:
						vMax = 255;
					} else if( valueRange[1] < 4096 ) {
						// Probably 12 bit:
						vMax = 4095;
					} else {
						IJ.error("Can't handle this image type; only 8 bit or 12 bit images so far.");
						return;
					}
					
					Histogram_2D histogram=new Histogram_2D();
					
					histogram.start2DHistogram(
					    valueRange[0],
					    valueRange[1],
					    256);

					float valueRangeWidth = valueRange[1] - valueRange[0];
					
					histogram.collectStatisticsFor(
					    valueRange[0] + (valueRangeWidth / 128),
					    valueRange[1] - (valueRangeWidth / 128));

					histogram.addImagePlusPair(dChannel, bChannel);

					histogram.calculateCorrelation();
					System.out.println("##### Got correlation gradient: "+histogram.fittedGradient);
					System.out.println("##### Got correlation Y intercept: "+histogram.fittedYIntercept);
					
					ImagePlus[] results = histogram.getHistograms();

					float a = histogram.fittedGradient;
					float b = histogram.fittedYIntercept;
					
					float m = -b / (a * vMax);
					float n = (vMax - b) / (a * vMax);
					
					// These values we've calculated suggest
					// gradients and y intercepts for each image:
					float dYintercept = 0;
					float dGradient = vMax;
					
					float bYintercept = (m * vMax) / (m - n);
					float bGradient = vMax / (n - m);
					
					ImagePlus framed=histogram.frame2DHistogram(
					    "2D Histogram of Values",
					    results[1],
					    dChannel.getTitle(),
					    valueRange[0], valueRange[1],
					    bChannel.getTitle(),
					    valueRange[0], valueRange[1] );
			
					String histogramFilename =
					    String.format(
						histogramFormatString,
						prefix,
						c,
						indexForDarkerRegions,
						indexForBrighterRegions );

					boolean saved = new FileSaver(framed).saveAsPng(histogramFilename);
					if( ! saved ) {
						throw new IOException("Failed to save PNG file to: "+histogramFilename);
					}
						
					String detectorFilename =
					    String.format(
					    detectorFormatString,
					    prefix,
					    c,
					    indexForDarkerRegions,
					    indexForBrighterRegions );
										
					PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(detectorFilename),"UTF-8"));
					
					pw.println("YIntercept\tGradient\tDetectorGain\tAmplifierOffset");
					pw.print(""+dYintercept);
					pw.print("\t"+dGradient);
					pw.print("\t"+darkDetailChannelData[c].detectorGain);
					pw.println("\t"+darkDetailChannelData[c].amplifierOffset);
					pw.print(""+bYintercept);
					pw.print("\t"+bGradient);
					pw.print("\t"+brightDetailChannelData[c].detectorGain);					
					pw.println("\t"+brightDetailChannelData[c].amplifierOffset);
					pw.close();
					
					for( int r = 0; r < results.length; ++r ) {
						results[r].close();
					}
					framed.close();
					dChannel.close();
					bChannel.close();
				}

				System.gc();
			}

		}
		templateImagePlus.close();		
		
	}
	
	public void run(String arg0) {
		
		String baseDirectory = null;
		String macroOptions = Macro.getOptions();
		if (macroOptions != null) {
			baseDirectory = Macro.getValue(macroOptions, "directory", null);
		}
		
		if (baseDirectory == null) {
			baseDirectory = "/Volumes/WD Passport/hdr-lacz-dscam/";
		}
		
		if (!baseDirectory.endsWith(File.separator)) {
			baseDirectory += File.separator;
		}
		
		File directoryFile = new File(baseDirectory);
		if (!(directoryFile.exists() && directoryFile.isDirectory())) {
			IJ.error("'" + baseDirectory + "' must exist and be a directory.");
			return;
		}
		
		String[][] filenames = {
                        {"181y-12bit-aaarrg-dark-detail.lsm", // in register!
			 "181y-12bit-aaarrg-mid-detail.lsm",  // misregistered (slight xy) with:
			 "181y-12bit-aaarrg.lsm"
			},
			{"210yxdscam-lacz-FM-dark-info.lsm",  // misregistered (big z) with:
			 "210yxdscam-lacz-FM-bright-info.lsm"
			}, // fixed by registration
			{"227yxdscam-lacz-FH-dark-info.lsm",  // misregistered (slight xy) with:
			 "227yxdscam-lacz-FH-bright-info.lsm"
			}, // fixed by registration
			{"227yxdscam-lacz-FI-dark-info.lsm",  // misregistered (slight xy) with:
			 "227yxdscam-lacz-FI-bright-info.lsm"
			}, // fixed by registration
			{"23yxdscam-lacz-FN-dark-info.lsm",   // misregistered (slight xy) with:
			 "23yxdscam-lacz-FN-bright-info.lsm"
			}, // fixed by registration
			{"52yxdscam-lacz-FK-dark-info.lsm",   // misregistered (slight xy) and big z:
			 "52yxdscam-lacz-FK-light-info.lsm"
			}, // a couple of pixels off after registration
			{"71y-lacZ-dscam-FB-dark-info.lsm",   // misregistered (slight xy) with:
			 "71y-lacZ-dscam-FB-mid-info.lsm",    // misregistered (slight xy) with:
			 "71y-lacZ-dscam-FB-bright-info.lsm"
			}, // a couple of pixels off after registration
                           // off by a pixel after registration
			{"71y-lacZ-dscam-FC-dark-info.lsm",   // in register already!
			 "71y-lacZ-dscam-FC-mid-info.lsm",    // in register already!
			 "71y-lacZ-dscam-FC-bright-info.lsm"
			}, // still in register
			{"C61xdscam-lacz-FF-dark-info.lsm",   // misregistered (slight x,y,z)
			 "C61xdscam-lacz-FF-light-info.lsm"
			}, // fixed by registration (i think, just off at the very top)
			{"C61xdscam-lacz-FG-dark-info.lsm",   // in register already!
			 "C61xdscam-lacz-FG-mid-info.lsm",    // in register already!
			 "C61xdscam-lacz-FG-bright-info.lsm"
			},
			{"c159bxdscam-lacz-FD-dark-info.lsm",  // misregistered (slight xy) witH:
			 "c159bxdscam-lacz-FD-bright-info.lsm"
			}, // mostly fixed by registration?
			{"c159bxdscam-lacz-FE-dark-info.lsm",  // misregistered (slight xy) with:
			 "c159bxdscam-lacz-FE-light-info.lsm"
			}, // fixed by registration
                        {"c255xdscam-lacz-FJ-dark-info.lsm",   // misregistered (slight x,y,z) with:
			 "c255xdscam-lacz-FJ-light-info.lsm"
			}, // fixed by registration
			{"c259xdscam-lacz-FL-dark-info.lsm",   // misregistered (slight x,y,z) with:
			 "c259xdscam-lacz-FL-bright-info.lsm"
			} // fixed by registration
		};
		
		for (int subjectIndex = 0; subjectIndex < filenames.length; ++subjectIndex) {
			
			String[] filenamesForSubject = filenames[subjectIndex];
			try {
				processSubjectImages( filenamesForSubject, baseDirectory );
			} catch( IOException e ) {
				IJ.error("Got an IOException while processing images: "+e);
				System.out.println("IOException: "+e);
				e.printStackTrace();
			}

		}
	}
}

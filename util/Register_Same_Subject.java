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
import java.util.logging.Level;
import java.util.logging.Logger;
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
	
	public void run(String arg0) {
		
		int channelForRegistration = 0;
		
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
			{"181y-12bit-aaarrg-dark-detail.lsm",
			 "181y-12bit-aaarrg-mid-detail.lsm",
			 "181y-12bit-aaarrg.lsm"
			},
			{"210yxdscam-lacz-FM-dark-info.lsm",
			 "210yxdscam-lacz-FM-bright-info.lsm"
			},
			{"227yxdscam-lacz-FH-dark-info.lsm",
			 "227yxdscam-lacz-FH-bright-info.lsm"
			},
			{"227yxdscam-lacz-FI-dark-info.lsm",
			 "227yxdscam-lacz-FI-bright-info.lsm"
			},
			{"23yxdscam-lacz-FN-dark-info.lsm",
			 "23yxdscam-lacz-FN-bright-info.lsm"
			},
			{"52yxdscam-lacz-FK-dark-info.lsm",
			 "52yxdscam-lacz-FK-light-info.lsm"
			},
			{"71y-lacZ-dscam-FB-dark-info.lsm",
			 "71y-lacZ-dscam-FB-mid-info.lsm",
			 "71y-lacZ-dscam-FB-bright-info.lsm"
			},
			{"71y-lacZ-dscam-FC-dark-info.lsm",
			 "71y-lacZ-dscam-FC-mid-info.lsm",
			 "71y-lacZ-dscam-FC-bright-info.lsm"
			},
			{"C61xdscam-lacz-FF-dark-info.lsm",
			 "C61xdscam-lacz-FF-light-info.lsm"
			},
			{"C61xdscam-lacz-FG-dark-info.lsm",
			 "C61xdscam-lacz-FG-mid-info.lsm",
			 "C61xdscam-lacz-FG-bright-info.lsm"
			},
			{"c159bxdscam-lacz-FD-dark-info.lsm",
			 "c159bxdscam-lacz-FD-bright-info.lsm"
			},
			{"c159bxdscam-lacz-FE-dark-info.lsm",
			 "c159bxdscam-lacz-FE-light-info.lsm"
			},
			{"c255xdscam-lacz-FJ-dark-info.lsm",
			 "c255xdscam-lacz-FJ-light-info.lsm"
			},
			{"c259xdscam-lacz-FL-dark-info.lsm",
			 "c259xdscam-lacz-FL-bright-info.lsm"
			}
		};
		
		for (int subjectIndex = 0; subjectIndex < filenames.length; ++subjectIndex) {
			String[] filenamesForSubject = filenames[subjectIndex];
			String prefix = findCommonPrefix(filenamesForSubject);
			System.out.println("Got common prefix: " + prefix);
			
			// Register against the middle image:
			int templateIndex = filenamesForSubject.length / 2;
			
			ImagePlus templateImagePlus = null;
			{
				String templateFilename = baseDirectory + filenamesForSubject[templateIndex];
				try {
					templateImagePlus = BatchOpener.openParticularChannel(
						templateFilename,
						channelForRegistration);
					if( templateImagePlus == null ) {
						IJ.error("File not found: "+templateFilename);
						return;
					}
				} catch (NoSuchChannelException e) {
					IJ.error("Zero-indexed channel " + templateIndex +
						 " not found in: " + templateFilename);
					return;
				}
			}
			
			// Now register them one-by-one, or just write out the template:
			String outputDirectory = baseDirectory + "registered" + File.separator;
			File outputDirectoryFile = new File(outputDirectory);
			outputDirectoryFile.mkdir();
			if (!(outputDirectoryFile.exists() && outputDirectoryFile.isDirectory())) {
				IJ.error("'" + baseDirectory + "' wasn't created properly");
				return;
			}
			
			String registeredFormatString = outputDirectory + "%simage%d-channel%d.tif";
			String transformationFormatString = outputDirectory + "%s%d-TO-%d.txt";
			
			for (int filenameIndex = 0;
			     filenameIndex < filenamesForSubject.length;
			     ++filenameIndex) {
				
				String filename = baseDirectory + filenamesForSubject[filenameIndex];
				
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
						
						System.out.println("templateImagePlus is: "+templateImagePlus);
						System.out.println("toTransform is: "+channels[channelForRegistration]);
						
						TransformedImage ti = new TransformedImage(
							templateImagePlus,
							channels[channelForRegistration]);
						
						ti.measure = new distance.MutualInformation(4096);
						
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
							1.0, // tolerance
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
					
					for (int c = 0; c < channels.length; ++c) {
						
						String outputFilename = String.format(
							registeredFormatString,
							prefix, filenameIndex, c);
						
						if (new File(outputFilename).exists()) {
							channels[c].close();
							continue;
						}
						
						// So we do have to map it...
						
						PixelPairs measure = new MutualInformation(4096);
						
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
					
                                        System.gc();
                                }
				
			}
                        templateImagePlus.close();
		}
	}
}

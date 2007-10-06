/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package util;

import ij.*;
import ij.process.*;
import ij.plugin.*;
import ij.io.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/* This class contains methods I would like to see incorporated into
   HandleExtraFileTypes.  The main features are:

     * An open method that returns an array of ImagePlus objects,
       one per channel, without calling show() on any of them.

     * Files are identified as particular types by their content,
       (magic numbers, etc.) never by their file extension.

     * The method doesn't rely on plugins being present, instead it
       uses reflection to check whether the required classes are
       available.  This is a bit ugly, but means that this could be
       incorporated into the main ImageJ source code without the
       plugins also needing to be included at compile time.

 */

public class BatchOpener {
	
	public static ImagePlus[] open(String path) {
		
		/* Read a few bytes from the beginning of the file into a
		   buffer to look for magic numbers and so on: */
		
		InputStream is;
		byte[] buf = new byte[132];
		try {
			is = new FileInputStream(path);
			is.read(buf, 0, 132);
			is.close();
		} catch (IOException e) {
			// Couldn't open the file for reading
			return null;
		}
		
		// FIXME: deal with gzipped files sensibly...
		byte[] gzipped_magic = {(byte) 0x1f, (byte) 0x8b};
		
		File file = new File(path);
		String name = file.getName();
		String nameLowerCase = name.toLowerCase();
		String directory = file.getParent();
		
		// Test if this is a TIFF-based file of some kind:
		byte[] tiffMagicIntel = {73, 73, 42, 0};
		byte[] tiffMagicMotorola = {77, 77, 0, 42};
		
		byte[] firstFour = new byte[4];
		System.arraycopy(buf, 0, firstFour, 0, 4);
		
		boolean tiffLittleEndian = Arrays.equals(tiffMagicIntel, firstFour);
		boolean tiffBigEndian = Arrays.equals(tiffMagicMotorola, firstFour);
		
		if (tiffLittleEndian || tiffBigEndian) {
			
			RandomAccessFile in = null;
			
			try {
				in = new RandomAccessFile(path, "r");
			} catch (IOException e) {
				return null;
			}
			
			if (in == null) {
				IJ.error("Can (no longer!) open the file '" + path + "'");
				return null;
			}
			
			boolean isLSM;
			try {
				isLSM = findLSMTag(in, tiffLittleEndian);
			} catch (IOException e) {
				return null;
			}
			
			if (isLSM) {
				
				try {
					in.close();
				} catch( IOException e ) {
					IJ.error("Couldn't close the LSM file.");
					return null;
				}
				
				// Zeiss Confocal LSM 510 image file (.lsm) handler
				// Insist on LSM_Toolbox for this rather than LSM_Reader,
				// which doesn't have an appropriate open method.
				// http://imagejdocu.tudor.lu/Members/ppirrotte/lsmtoolbox
				ClassLoader loader = IJ.getClassLoader();
				if (loader == null) {
					IJ.error("IJ.getClassLoader() failed (!)");
					return null;
				}
				
				try {
					
					/* This unfortunate ugliness is because at
					   compile time we can't be sure that the
					   LSM_Toolbox jar is in the classpath. */
					
					Class<?> c = loader.loadClass("org.imagearchive.lsm.toolbox.Reader");
					Object newInstance = c.newInstance();
					
					/* This version of open doesn't show() them... */
					Class [] parameterTypes = { String.class,
								    String.class,
								    Boolean.TYPE,
								    Boolean.TYPE,
								    Boolean.TYPE };
					
					Method m = c.getMethod( "open", parameterTypes );
					Object [] parameters = new Object[5];
					parameters[0] = file.getParent();
					parameters[1] = file.getName();
					parameters[2] = false;
					parameters[3] = false;
					parameters[4] = false;
					
					ImagePlus [] result = (ImagePlus [])m.invoke(newInstance,parameters);
					return result;
					
				} catch (IllegalArgumentException e) {
					IJ.error("There was an illegal argument when trying to invoke the LSM_Toolbox reader: " + e);
				} catch (InvocationTargetException e) {
					Throwable realException = e.getTargetException();
					IJ.error("There was an exception thrown by the LSM_Toolbox plugin: " + realException);
				} catch (ClassNotFoundException e) {
					IJ.error("The LSM_Toolbox plugin was not found: " + e);
				} catch (InstantiationException e) {
					IJ.error("Failed to instantiate the LSM toolbox reader: " + e);
				} catch ( IllegalAccessException e ) {
					IJ.error("IllegalAccessException when trying to create an instance of the reader: "+e);
				} catch (NoSuchMethodException e) {
					IJ.error("There was a NoSuchMethodException when trying to invoke the LSM_Toolbox reader: " + e);
				} catch (SecurityException e) {
					IJ.error("There was a SecurityException when trying to invoke the LSM_Toolbox reader: " + e);
				}
				
				return null;
			}
			
			/* Now test to see if this is a Leica TIFF, which
			   unfortunately seems to involve seeking to near the
			   end of the file.  This code is copied from
			   HandleExtraFileTypes */
			
			byte[] leicaBytes = new byte[44];
			long seekTo = -1;
			
			try {
				seekTo = in.length() - 1658;
				in.seek(seekTo);
				in.readFully(leicaBytes);
			} catch( IOException e ) {
				IJ.error("Couldn't seek to "+seekTo+" in "+path);
				return null;
			}
			
			String leicaString = new String(leicaBytes);
			
			if (leicaString.equals("Leica Lasertechnik GmbH, " + 
					       "Heidelberg, Germany")) {
				
				try {
					in.close();
				} catch( IOException e ) {
					IJ.error("Couldn't close the Leica TIFF file.");
					return null;
				}
				
				/* Then this is a Leica TIFF file.  Look for the VIB
				   Leica_SP_Reader plugin, which allows us to get an
				   ImagePlus for each channel. */
				
				ClassLoader loader = IJ.getClassLoader();
				if (loader == null) {
					IJ.error("IJ.getClassLoader() failed (!)");
					return null;
				}
				
				try {
					
					/* This unfortunate ugliness is because at
					   compile time we can't be sure that
					   zeiss.Leica_SP_Reader is in the classpath. */
					
					Class<?> c = loader.loadClass("leica.Leica_SP_Reader");
					Object newInstance = c.newInstance();
					
					Class [] parameterTypes = { String.class };
					Object [] parameters = new Object[1];
					parameters[0] = path;
					Method m = c.getMethod( "run", parameterTypes );
					m.invoke(newInstance,parameters);
                    
					/* That should have loaded the file or
					   thrown an IOException. */
					
					parameterTypes = new Class[0];
					parameters = new Object[0];
					m = c.getMethod("getNumberOfChannels", parameterTypes);
					Integer n=(Integer)m.invoke(newInstance,parameters);
					
					if( n < 1 ) {
						IJ.error("Error: got "+n+" channels from "+path+" with the Leica SP Reader");
						return null;
					}
					
					ImagePlus [] result = new ImagePlus[n];
					
					for( int i = 0; i < n; ++i ) {
						parameterTypes = new Class[1];
						parameterTypes[0] = Integer.TYPE;
						parameters = new Object[1];
						parameters[0] =  new Integer(i);
						m = c.getMethod("getImage", parameterTypes);
						result[i] = (ImagePlus)m.invoke(newInstance,parameters);
					}
					
					return result;
					
				} catch (IllegalArgumentException e) {
					IJ.error("There was an illegal argument when trying to invoke a method on the Leica SP Reader plugin: " + e);
				} catch (InvocationTargetException e) {
					Throwable realException = e.getTargetException();
					IJ.error("There was an exception thrown by the Leica SP Reader plugin: " + realException);
				} catch (ClassNotFoundException e) {
					IJ.error("The Leica SP Reader plugin was not found: " + e);
				} catch (InstantiationException e) {
					IJ.error("Failed to instantiate the Leica SP Reader plugin: " + e);
				} catch ( IllegalAccessException e ) {
					IJ.error("IllegalAccessException when trying the Leica SP Reader plugin: "+e);
				} catch (NoSuchMethodException e) {
					IJ.error("Couldn't find a method in the Leica SP Reader plugin: " + e);
				} catch (SecurityException e) {
					IJ.error("There was a SecurityException when trying to invoke a method of the Leica SP Reader plugin: " + e);
				}
				
				return null;
			}

			try {
				in.close();
			} catch( IOException e ) {
				IJ.error("Couldn't close the file.");
				return null;
			}
			
			// Use the default opener:
			ImagePlus[] i = new ImagePlus[1];
			i[0] = IJ.openImage(path);
			return i;
			
		}
        
		ImagePlus imp;
        
		// MHL: the code below is essentially the same as in
		// HandleExtraFileTypes.  I've just dropped those types
		// that open and show the images themselves, since they're
		// probably not useful for non-GUI use...
        
		// GJ: added Biorad PIC confocal file handler
		// Note that the Biorad_Reader plugin extends the ImagePlus class,
		// which is why the IJ.runPlugIn() call below returns an ImagePlus object.
		// ------------------------------------------
		// These make 12345 if you read them as the right kind of short
		// and should have this value in every Biorad PIC file
		if (buf[54] == 57 && buf[55] == 48) {
			// Ok we've identified the file type
			// Now load it using the relevant plugin
			imp = (ImagePlus) IJ.runPlugIn("Biorad_Reader", path);
			if (imp == null) {
				return null;
			}
			if (imp != null && imp.getWidth() == 0) {
				return null;
			}
			ImagePlus[] i = new ImagePlus[1];
			i[0] = IJ.openImage(path);
			return i;
		}
        
		// GJ: added Gatan Digital Micrograph DM3 handler
		// Note that the DM3_Reader plugin extends the ImagePlus class,
		// which is why the IJ.runPlugIn() call below returns an ImagePlus object.
		// ----------------------------------------------
		// Check if the file ends in .DM3 or .dm3
		if (name.endsWith(".dm3")) {
			// These make an int value of 3 which is the DM3 version number
			if (buf[0] == 0 && buf[1] == 0 && buf[2] == 0 && buf[3] == 3) {
				// Ok we've identified the file type - now load it
				imp = (ImagePlus) IJ.runPlugIn("DM3_Reader", path);
				if (imp == null) {
					return null;
				}
				if (imp != null && imp.getWidth() == 0) {
					return null;
				}
				ImagePlus[] i = new ImagePlus[1];
				i[0] = IJ.openImage(path);
				return i;
			}
		}
        
		// IPLab file handler
		// Note that the IPLab_Reader plugin extends the ImagePlus class.
		// Little-endian IPLab files start with "iiii" or "mmmm".
		if ((buf[0] == 105 && buf[1] == 105 && buf[2] == 105 && buf[3] == 105) || (buf[0] == 109 && buf[1] == 109 && buf[2] == 109 && buf[3] == 109)) {
			imp = (ImagePlus) IJ.runPlugIn("IPLab_Reader", path);
			if (imp == null) {
				return null;
			}
			if (imp != null && imp.getWidth() == 0) {
				return null;
			}
			ImagePlus[] i = new ImagePlus[1];
			i[0] = IJ.openImage(path);
			return i;
		}
        
		// Packard InstantImager format (.img) handler -> check HERE before Analyze check below!
		// Note that the InstantImager_Reader plugin extends the ImagePlus class.
		// Check extension and signature bytes KAJ_
		if (name.endsWith(".img") && buf[0] == 75 && buf[1] == 65 && buf[2] == 74 && buf[3] == 0) {
			imp = (ImagePlus) IJ.runPlugIn("InstantImager_Reader", path);
			if (imp == null) {
				return null;
			}
			if (imp != null && imp.getWidth() == 0) {
				return null;
			}
			ImagePlus[] i = new ImagePlus[1];
			i[0] = IJ.openImage(path);
			return i;
		}
        
		// Amira file handler
		if (buf[0] == 0x23 && buf[1] == 0x20 && buf[2] == 0x41 &&
		    buf[3] == 0x6d && buf[4] == 0x69 && buf[5] == 0x72 &&
		    buf[6] == 0x61 && buf[7] == 0x4d && buf[8] == 0x65 &&
		    buf[9] == 0x73 && buf[10] == 0x68 && buf[11] == 0x20) {
			ImagePlus[] i = new ImagePlus[1];
			imp = (ImagePlus) IJ.runPlugIn("AmiraMeshReader_", path);
			if (imp == null) {
				return null;
			}
			i[0] = imp;
			return i;
		}

		return null;
	}
    
    static boolean findLSMTag(RandomAccessFile in, boolean littleEndian) throws IOException {
        return findTag(34412,in,littleEndian);
    }
    
	// This is a stripped down version of TiffDecoder.getTiffInfo() that just
	// looks for an particular TIFF tag...
	
	static boolean findTag(long tagToLookFor, RandomAccessFile in, boolean littleEndian) throws IOException {
		
		int byteOrder = in.readShort();
		int magicNumber = getShort(in, littleEndian); // 42
		int offset = getInt(in, littleEndian);
		if (magicNumber != 42) {
			IJ.error("Not really a TIFF file (BUG: should have been detected earlier.)");
			// FIXME: throw an exception...
		}
		
		if (offset < 0) {
			IJ.error("TIFF file probably corrupted: offset is negative");
			return false;
		}
		
		while (offset > 0) {
			
			in.seek(offset);
			
			// Get Image File Directory data
			int tag;
			int fieldType;
			int count;
			int value;
			int nEntries = getShort(in, littleEndian);
			if (nEntries < 1 || nEntries > 1000) {
				return false;
			}
			for (int i = 0; i < nEntries; i++) {
				tag = getShort(in, littleEndian);
				fieldType = getShort(in, littleEndian);
				count = getInt(in, littleEndian);
				value = getValue(in, littleEndian, fieldType, count);
				if (tag == tagToLookFor) {
					return true;
				}
			}
			offset = getInt(in, littleEndian);
		}
		return false;
	}
	
	static int getInt(RandomAccessFile in, boolean littleEndian) throws IOException {
		int b1 = in.read();
		int b2 = in.read();
		int b3 = in.read();
		int b4 = in.read();
		if (littleEndian) {
			return (b4 << 24) + (b3 << 16) + (b2 << 8) + (b1 << 0);
		} else {
			return (b1 << 24) + (b2 << 16) + (b3 << 8) + b4;
		}
	}
	
	static int getShort(RandomAccessFile in, boolean littleEndian) throws IOException {
		int b1 = in.read();
		int b2 = in.read();
		if (littleEndian) {
			return (b2 << 8) + b1;
		} else {
			return (b1 << 8) + b2;
		}
	}
	private static final int TIFF_FIELD_TYPE_SHORT = 3;
	private static final int TIFF_FIELD_TYPE_LONG = 4;
	
	static int getValue(RandomAccessFile in, boolean littleEndian, int fieldType, int count) throws IOException {
		int value = 0;
		int unused;
		if (fieldType == TIFF_FIELD_TYPE_SHORT && count == 1) {
			value = getShort(in, littleEndian);
			unused = getShort(in, littleEndian);
		} else {
			value = getInt(in, littleEndian);
		}
		return value;
	}
}

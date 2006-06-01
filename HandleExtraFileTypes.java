import ij.*;
import ij.process.*;
import ij.plugin.*;
import ij.io.*;
import java.io.*;

// Plugin to handle file types which are not implemented 
// directly in ImageJ through io.Opener
// nb since there is no _ in the name it will not appear in Plugins menu
// -----
// Can be user modified so that your own specialised file types
// can be opened through File ... Open
// OR by drag and drop onto the ImageJ main panel
// OR by double clicking in the MacOS 9/X Finder
// -----
// Go to the point marked MODIFY HERE and modify to 
// recognise and load your own file type
// I have implemented two file types as examples:
// 	Biorad PIC and Gatan DM3
// -----
// Gregory Jefferis - 030629
// jefferis@stanford.edu

public class HandleExtraFileTypes extends ImagePlus implements PlugIn {
	static final int IMAGE_OPENED=-1;
	static final int PLUGIN_NOT_FOUND=-1;
	
	// Called from io/Opener.java
	public void run(String path) {
		if (IJ.versionLessThan("1.30u")) return;
		if (path.equals("")) return;
		File theFile=new File(path);
		String directory = theFile.getParent();
		String fileName = theFile.getName();
		if (directory==null) directory = "";
		
		// Try and recognise file type and load the file if recognised
		ImagePlus imp=openImage(directory,fileName);
		if (imp==null) {			
			// failed to load file or plugin has opened and displayed it
			IJ.showStatus("");
			return;  // failed to load file or plugin has opened and displayed it
		}
		ImageStack stack=imp.getStack();
		// Set the stack of this HandleExtraFileTypes object
		// to that attached to the ImagePlus object returned by openImage()
		setStack(fileName,stack);
		// Copy over the calibration info since it doesn't come with the ImageProcessor
		setCalibration(imp.getCalibration());
		// Also copy the Show Info field over if it exists
		if (imp.getProperty("Info")!=null)
			setProperty("Info",imp.getProperty("Info"));
		// Copy over the FileInfo
		setFileInfo(imp.getOriginalFileInfo());
	}  

	private ImagePlus openImage(String directory, String name) {
		ImagePlus imp;

		// Set out file name and path
		if (directory.length()>0 && !directory.endsWith(Prefs.separator))
			directory += Prefs.separator;
		String path = directory+name;

		// set up a stream to read in 132 bytes from the file header
		// These can be checked for "magic" values which are diagnostic
		// of some image types
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
		name = name.toLowerCase();

		// OK now we get to the interesting bit 
			
		// GJ: added Biorad PIC confocal file handler
		// Note that the Biorad_Reader plugin extends the ImagePlus class,
		// which is why the IJ.runPlugIn() call below returns an ImagePlus object.
		// ------------------------------------------
		// These make 12345 if you read them as the right kind of short
		// and should have this value in every Biorad PIC file
		if(buf[54]==57 && buf[55]==48) {
			// Ok we've identified the file type 
			// Now load it using the relevant plugin
			imp = (ImagePlus)IJ.runPlugIn("Biorad_Reader", path);
			if (imp==null) width = PLUGIN_NOT_FOUND;
			if (imp!=null&&imp.getWidth()==0) imp = null;
			return imp;
		}
		
		// GJ: added Gatan Digital Micrograph DM3 handler
		// Note that the DM3_Reader plugin extends the ImagePlus class,
		// which is why the IJ.runPlugIn() call below returns an ImagePlus object.
		// ----------------------------------------------
		// Check if the file ends in .DM3 or .dm3
		if (name.endsWith(".dm3")) {
			// These make an int value of 3 which is the DM3 version number
			if(buf[0]==0 && buf[1]==0 && buf[2]==0 && buf[3]==3) {
				// Ok we've identified the file type - now load it
				imp = (ImagePlus)IJ.runPlugIn("DM3_Reader", path);
				if (imp==null) width = PLUGIN_NOT_FOUND;
				if (imp!=null&&imp.getWidth()==0) imp = null;
				return imp;
			}
		}

		// Analyze format (.img/.hdr) handler
		// Note that the Analyze_Reader plugin opens and displays the
		// image and does not implement the ImagePlus class.
		if (name.endsWith(".img") || name.endsWith(".hdr")) {
			// Open Analyze image and display it
			IJ.runPlugIn("Analyze_Reader", path);
			// Set flag so Opener.openImage() does not display error
			width = IMAGE_OPENED; 
			return null; 
		}

		// IPLab file handler
		// Note that the IPLab_Reader plugin extends the ImagePlus class.
		// Little-endian IPLab files start with "iiii" or "mmmm".
		if ((buf[0]==105&& buf[1]==105&&buf[2]==105&&buf[3]==105)
		|| (buf[0]==109&& buf[1]==109&&buf[2]==109&&buf[3]==109)) {
			imp = (ImagePlus)IJ.runPlugIn("IPLab_Reader", path);
			if (imp==null) width = PLUGIN_NOT_FOUND;
			if (imp!=null&&imp.getWidth()==0) imp = null;
			return imp;
		}

		// Analyze format (.img/.hdr) handler
		// Note that the Analyze_Reader plugin opens and displays the
		// image and does not implement the ImagePlus class.
		if (name.endsWith(".img") || name.endsWith(".hdr")) {
			// Open Analyze image and display it
			IJ.runPlugIn("Analyze_Reader", path);
			// Set flag so Opener.openImage() does not display error
			width = IMAGE_OPENED; 
			return null; 
		}

		// Image Cytometry Standard (.ics) handler
		// http://valelab.ucsf.edu/~nico/IJplugins/Ics_Opener.html		
		if (name.endsWith(".ics")) {
			// Open ICS image and display it
			IJ.runPlugIn("Ics_Opener", path);
			// Set flag so Opener.openImage() does not display error
			width = IMAGE_OPENED; 
			return null; 
		}

		//  Princeton Instruments SPE image file (.spe) handler
		//  http://rsb.info.nih.gov/ij/plugins/spe.html 	
		if (name.endsWith(".spe")) {
			// Open SPEimage and display it
			IJ.runPlugIn("OpenSPE_", path);
			// Set flag so Opener.openImage() does not display error
			width = IMAGE_OPENED; 
			return null; 
		}

		//  Zeiss Confocal LSM 510 image file (.lsm) handler
		//  http://rsb.info.nih.gov/ij/plugins/lsm-reader.html
		if (name.endsWith(".lsm")) {
			IJ.runPlugIn("LSM_Reader", path);
			width = IMAGE_OPENED;
			return null;
		}

		// BM: added Bruker  file handler 29.07.04
		if (name.equals("ser") || name.equals("fid") || name.equals("2rr") || name.equals("2ii") || name.equals("3rrr") 
		|| name.equals("3iii") || name.equals("2dseq")) {
			ij.IJ.showStatus("Opening Bruker " + name + " File");
			IJ.runPlugIn("BrukerOpener", name + "|" + path);
			width = IMAGE_OPENED;
			return null;
		}

		// AVI: open AVI files using AVI_Reader plugin
		if (name.endsWith(".avi")) {
			IJ.runPlugIn("AVI_Reader", path);
			width = IMAGE_OPENED;
			return null;
		}

		// QuickTime: open .mov files using QT_Movie_Opener plugin
		if (name.endsWith(".mov")) {
			IJ.runPlugIn("QT_Movie_Opener", path);
			width = IMAGE_OPENED;
			return null;
		}

		// ZVI file handler
		// Little-endian ZVI files start with d0 cf 11 e0.
		if (name.endsWith(".zvi") || (buf[0]==-48&& buf[1]==-49&&buf[2]==17&&buf[3]==-32)) {
			IJ.runPlugIn("ZVI_Reader", path);
			width = IMAGE_OPENED;
			return null;
		}

		// Amira file handler
		if (//name.endsWith(".am") || name.endsWith(".labels")  ||
			(buf[0]==0x23&& buf[1]==0x20&&buf[2]==0x41
			 &&buf[3]==0x6d&&buf[4]==0x69&&buf[5]==0x72
			 &&buf[6]==0x61&&buf[7]==0x4d&&buf[8]==0x65
			 &&buf[9]==0x73&&buf[10]==0x68&&buf[11]==0x20)) {
			Object o = IJ.runPlugIn("AmiraMeshReader_", path);
			width = IMAGE_OPENED;
			if (o instanceof ImagePlus)
				return (ImagePlus)o;
			return null;
		}
		// ****************** MODIFY HERE ******************
		// Do what ever you have to do to recognise your own file type
		// and then call appropriate plugin 
		// using the above as models 
		// eg:
		
		/* 
		// A. Dent: Added XYZ handler
		// ----------------------------------------------
		// Check if the file ends in .XYZ or .xyz
		if (name.toUpperCase().endsWith(".XYZ")) {
			// Bytes 0 and 1 must equal 42 for this file type
			if(buf[0]==42 && buf[1]==42) {
				// Ok we've identified the file type - now load it
				imp = (ImagePlus)IJ.runPlugIn("XYZ_Reader", path);
				if (imp==null) width = PLUGIN_NOT_FOUND;
				if (imp!=null&&imp.getWidth()==0) imp = null;
				return imp;
			}
		}
		*/
		
		// If we got this far we didn't recognise the file type
		return null;
	}

}

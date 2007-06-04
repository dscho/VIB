/*
 *
 */

package util;

import java.io.*;

import ij.*;
import ij.process.*;

import zeiss.LSM_Reader;

public class BatchOpener {

    /* It would be very useful to me to be able to open all the images
       I habitually deal with as an array of ImagePlus files without
       displaying the images. */

    public static ImagePlus[] openFromFile( String path ) {

        // System.out.println("  BatchOpener.openFromFile(): path="+path);

        InputStream is;
        byte[] buf = new byte[132];
        try {
            is = new FileInputStream(path);
            is.read(buf, 0, 132);
            is.close();
        } catch (IOException e) {
            // Couldn't open the file for reading                                                                               return null;
        }

        File file=new File(path);
        String name=file.getName();
        String nameLowerCase=name.toLowerCase();
        String directory=file.getParent();
        
        /* Test if this is an Amira file, then an LSM, then anything else. */

        // Amira file handler
        if (buf[0]==0x23&& buf[1]==0x20&&buf[2]==0x41
            &&buf[3]==0x6d&&buf[4]==0x69&&buf[5]==0x72
            &&buf[6]==0x61&&buf[7]==0x4d&&buf[8]==0x65
            &&buf[9]==0x73&&buf[10]==0x68&&buf[11]==0x20) {
            // System.out.println("  BatchOpener.openFromFile(): it's an AmiraMesh file");
            ImagePlus[] i=new ImagePlus[1];
            i[0] = (ImagePlus)IJ.runPlugIn("AmiraMeshReader_", path);
            return i;
        }
        
		//  Zeiss Confocal LSM 510 image file (.lsm) handler
		//  http://rsb.info.nih.gov/ij/plugins/lsm-reader.html
		else if (name.endsWith(".lsm")) {
            // System.out.println("  BatchOpener.openFromFile(): it's an LSM file");
            LSM_Reader reader=new LSM_Reader();
			return reader.OpenLSM2(directory,name);
		}

        else {
            // System.out.println("  BatchOpener.openFromFile(): it's some other file");
            ImagePlus[] i=new ImagePlus[1];
            i[0] = IJ.openImage(path);
            return i;
        }

    }
    
    public static void closeAllWithoutConfirmation() {

        int[] wList = WindowManager.getIDList();
        if (wList==null) {
            // IJ.error("BatchOpener.closeAllWithoutConfirmation(): No images are open.");
            return;
        }
        
        for (int i=0; i<wList.length; i++) {
            ImagePlus imp = WindowManager.getImage(wList[i]);
            imp.changes=false;
            imp.getWindow().close();
        }

    }

//    public static void drawCrosshair(ImagePlus i,int x,int y,int z) {
//        drawCrosshair(i,x,y,z,"");
//    }
//    
//    public static void drawCrosshair(ImagePlus i,int x,int y,int z,String text) {
//
//        ImageStack stack=i.getStack();
//        ImageProcessor processor=stack.getProcessor(z+1);
//        processor.setValue(processor.getMax());
//                /*
//                  i.setSlice(z+1);
//                  ImageProcessor processor=i.getProcessor();
//                  processor.setColor(Toolbar.getForegroundColor());
//                */
//        processor.setLineWidth(1);
//        processor.moveTo(x+1,y);
//        processor.lineTo(x+5,y);
//        processor.moveTo(x-1,y);
//        processor.lineTo(x-5,y);
//        processor.moveTo(x,y+1);
//        processor.lineTo(x,y+5);
//        processor.moveTo(x,y-1);
//        processor.lineTo(x,y-5);
//        processor.drawString(text,x+10,y+10);
//    }
    
}


package org.imagearchive.lsm.toolbox;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.io.FileSaver;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.MedianCut;
import ij.process.ShortProcessor;

import java.awt.Image;
import java.io.File;

/*********************************************************************************
 * Batch Converter Class - Adapted from Wayne Rasband's Batch Converter plug-in. *
 ********************************************************************************/

public class BatchConverter_{
    
    private String[] choices = {"Tiff", "8-bit Tiff", "Jpeg", "Zip", "Raw"};
    private String format = "Tiff";
    private MasterModel masterModel;
    public BatchConverter_(MasterModel masterModel) {
		super();
		this.masterModel = masterModel;
	}
    
    /***************************************************************
     * method : run(), opens a dialog box for the batch conversion *
     **************************************************************/
    
    public void run(String arg) {
        OpenDialog od = new OpenDialog("Select a file in source folder...", "");
        if (od.getFileName()==null) return;
        String dir1 = od.getDirectory();
        GenericDialog gd = new GenericDialog("LSM Batch Converter", IJ.getInstance());
        gd.addChoice("Convert to: ", choices, format);
        gd.showDialog();
        if (gd.wasCanceled())
            return;
        format = gd.getNextChoice();
        SaveDialog sd = new SaveDialog("Open destination folder...", "dummy name (required)", "");
        if (sd.getFileName()==null) return;
        String dir2 = sd.getDirectory();
        convert(dir1, dir2, format);
    }
    
    /************************************************
     * method : convert, does the actual conversion *
     ***********************************************/
    
    public void convert(String dir1, String dir2, String format) {
        if (!dir2.endsWith(File.separator))
            dir2 += File.separator;
        String[] list = new File(dir1).list();
        if (list==null)
            return;
        for (int i=0; i<list.length; i++) {
            IJ.showStatus(i+"/"+list.length);
            File f = new File(dir1+list[i]);
            if (!f.isDirectory()) {
                Reader reader = new Reader(masterModel);
                ImagePlus [] impTab = reader.open(dir1, list[i],false,false,false);
                for (int k=0; k<impTab.length; k++){
                    ImagePlus img = impTab[k];
                    if (img!=null) {
                        img = process(img);
                        if (img!=null){
                            if (img.getStackSize() != 1) {
                                for (int slice=0; slice<img.getStackSize(); slice++){
                                    img.setSlice(slice);
                                    ImageProcessor ip = img.getProcessor();
                                    ImageProcessor newip = ip.createProcessor(ip.getWidth(),ip.getHeight());
                                    newip.setPixels(ip.getPixelsCopy());
                                    String slicename = img.getTitle();
                                    slicename +=("_slice");
                                    String numb = IJ.d2s(slice,0);
                                    slicename += "_"+numb;
                                    ImagePlus img2 = new ImagePlus(slicename, newip);
                                    save(img2, dir2, format, k);
                                    slicename ="";
                                    numb ="";
                                }
                            } else save(img, dir2, format, k);
                        }
                    }
                }
            }
        }
        IJ.showProgress(1.0);
        IJ.showStatus("");
        IJ.showMessage("Conversion done");
    }
    
    /************************************************************************************
     * method : process, optional method to add some image processing before conversion *
     ***********************************************************************************/
    
    /** This is the place to add code to process each image. The image
     * is not written if this method returns null. */
    public ImagePlus process(ImagePlus img) {
        /* No processing defined for this plugin */
        return img;
    }
    
    /****************************************************************
     * method : save, saves the image with an appropriate file name *
     ****************************************************************/
    
    public void save(ImagePlus img, String dir, String format, int index) {
        String name = img.getTitle();
        int dotIndex = name.lastIndexOf(".");
        int doubledotIndex = name.lastIndexOf(":");
        String Chan = name.substring(doubledotIndex+1, name.length());
        if (dotIndex>=0)
            name = name.substring(0, dotIndex);
        if (doubledotIndex >= 0) name = name.concat(Chan);
        String path = dir + name;
        if (format.equals("Tiff"))
            new FileSaver(img).saveAsTiff(path+".tif");
        else if (format.equals("8-bit Tiff"))
            saveAs8bitTiff(img, path+".tif");
        else if (format.equals("Zip"))
            new FileSaver(img).saveAsZip(path+".zip");
        else if (format.equals("Raw"))
            new FileSaver(img).saveAsRaw(path+".raw");
        else if (format.equals("Jpeg"))
            new FileSaver(img).saveAsJpeg(path+".jpg");
    }
    
    /*******************************************************************
     * method : saveAs8bitTiff, image processing for 8-bit Tiff saving *
     ******************************************************************/
    
    void saveAs8bitTiff(ImagePlus img, String path) {
        ImageProcessor ip = img.getProcessor();
        if (ip instanceof ColorProcessor) {
            ip = reduceColors(ip); img.setProcessor(null, ip);} else if ((ip instanceof ShortProcessor) || (ip instanceof FloatProcessor)) {
            ip = ip.convertToByte(true); img.setProcessor(null, ip);}
        new FileSaver(img).saveAsTiff(path);
    }
    
    /*****************************************************************************
     * method : reduceColors, reduces the color range for the appropriate format *
     ****************************************************************************/
    
    ImageProcessor reduceColors(ImageProcessor ip) {
        MedianCut mc = new MedianCut((int[])ip.getPixels(), ip.getWidth(), ip.getHeight());
        Image img = mc.convert(256);
        return(new ByteProcessor(img));
    }

	
}



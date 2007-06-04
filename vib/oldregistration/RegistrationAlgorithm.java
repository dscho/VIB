/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package vib.oldregistration;

import ij.ImagePlus;
import ij.IJ;
import ij.WindowManager;

import vib.transforms.OrderedTransformations;
import util.BatchOpener;
import util.FileAndChannel;

public abstract class RegistrationAlgorithm {
	
	boolean keepSourceImages;
	ImagePlus[] sourceImages;
	OrderedTransformations transformation;
	
	public ImagePlus getTemplate() {
		return sourceImages[0];
	}
	
	public ImagePlus getDomain() {
		return sourceImages[1];
	}
	
	public void loadImages( FileAndChannel f0, FileAndChannel f1 ) {
		
		ImagePlus[] f0imps=BatchOpener.openFromFile(f0.getPath());
		ImagePlus[] f1imps=BatchOpener.openFromFile(f1.getPath());
		
		sourceImages=new ImagePlus[2];
		
		sourceImages[0]=f0imps[f0.getChannelZeroIndexed()];
		sourceImages[1]=f1imps[f1.getChannelZeroIndexed()];
	}
	
	public void loadImagesOld( FileAndChannel f0, FileAndChannel f1 ) {
		
		transformation=null;
		
		String fileTemplate=f0.getPath();
		String fileDomain=f1.getPath();
		
		sourceImages=new ImagePlus[2];
		keepSourceImages=true;
		
		ImagePlus[] toClose=new ImagePlus[256];
		int imagesToClose=0;
		
		IJ.openImage(fileTemplate);
		
		int[] wList = WindowManager.getIDList();
		if (wList==null) {
			IJ.error("Bookstein_FromMarkers.produceOverlayed(): No images are open after loading template");
			return;
		}
		
		for (int i=0; i<wList.length; i++) {
			ImagePlus imp = WindowManager.getImage(wList[i]);
			if(f0.correctFileName(imp)) {
				if(f0.correctChannel(imp))
					sourceImages[0]=imp;
				else
					toClose[imagesToClose++]=imp;
			}
		}
		
		for(int i=0;i<imagesToClose;++i)
			toClose[i].close();
		
		imagesToClose=0;
		
		System.gc();
		
		IJ.openImage(fileDomain);
		
		wList = WindowManager.getIDList();
		if (wList==null) {
			IJ.error("Bookstein_FromMarkers.produceOverlayed(): No images are open after loading domain");
			return;
		}
		
		for (int i=0; i<wList.length; i++) {
			ImagePlus imp = WindowManager.getImage(wList[i]);
			if(f1.correctFileName(imp)) {
				if(f1.correctChannel(imp))
					sourceImages[1]=imp;
				else
					toClose[imagesToClose++]=imp;
			}
		}
		
		for(int i=0;i<imagesToClose;++i)
			toClose[i].close();
		
		System.gc();
		
	}
	
}

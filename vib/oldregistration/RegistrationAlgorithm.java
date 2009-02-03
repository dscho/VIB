/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package vib.oldregistration;

import ij.ImagePlus;
import ij.IJ;
import ij.WindowManager;

import util.BatchOpener;
import util.FileAndChannel;

public abstract class RegistrationAlgorithm {
	
	public boolean keepSourceImages;
	public ImagePlus[] sourceImages;
	
	public ImagePlus getTemplate() {
		return sourceImages[0];
	}
	
	public ImagePlus getDomain() {
		return sourceImages[1];
	}

	// The same functionality according to different terminology:

	public ImagePlus getModel() {
		return sourceImages[0];
	}

	public ImagePlus getFloating() {
		return sourceImages[1];
	}
	
	public void loadImages( FileAndChannel f0, FileAndChannel f1 ) {
		
		ImagePlus[] f0imps=BatchOpener.open(f0.getPath());
		ImagePlus[] f1imps=BatchOpener.open(f1.getPath());
		
		sourceImages=new ImagePlus[2];
		
		sourceImages[0]=f0imps[f0.getChannelZeroIndexed()];
		sourceImages[1]=f1imps[f1.getChannelZeroIndexed()];
	}

}

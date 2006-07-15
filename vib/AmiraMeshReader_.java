/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package vib;

import ij.IJ;
import ij.ImagePlus;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;
import ij.text.TextWindow;
import java.awt.*;

public class AmiraMeshReader_ extends ImagePlus implements PlugIn {

	public void run(String arg) {
		boolean showIt = (IJ.getInstance() != null && arg.equals(""));
		String dir="";
		if(arg==null || arg.equals("")) {
			OpenDialog od = new OpenDialog("AmiraFile", null);
			dir=od.getDirectory();
			arg=od.getFileName();
		}
		if(arg==null)
			return;
		AmiraMeshDecoder d=new AmiraMeshDecoder();
		if(d.open(dir+arg)) {
			if (d.isTable()) {
				TextWindow table = d.getTable();
			} else {
				setStack(arg,d.getStack());
				d.parameters.setParameters(this);
				if (showIt)
					show();
			}
		}
	}

}



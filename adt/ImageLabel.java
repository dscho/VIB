/*
 * Created on 30-May-2006
 */
package adt;

import java.util.HashMap;
import java.awt.*;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;

public class ImageLabel {
	final ImagePlus data;
	final String name;
	final double [] color;
	final HashMap<Integer, Roi> sliceLables = new HashMap<Integer, Roi>();
	
	public ImageLabel(String name, ImagePlus data, double [] color) {
		this.data = data;
		this.name = name;
        this.color = color;
	}
	
	public void setLabelForSlice(int sliceNumber, Roi roi){
		sliceLables.put(sliceNumber, roi);		
	}
	
	public void setLabelForSlice(){
		setLabelForSlice(data.getCurrentSlice(), data.getRoi());		
	}
	
	public Roi getLabelForSlice(int sliceNumber){
		return sliceLables.get(sliceNumber);		
	}
	
	public Roi getLabelForSlice(){
		return getLabelForSlice(data.getCurrentSlice());		
	}
	
	public void showLabel(){
		data.setRoi(getLabelForSlice());
	}
	
	public String toString(){
		return name;
	}

	public String getName() {		
		return name;
	}

    public double[] getColor() {
        return color;
    }

}

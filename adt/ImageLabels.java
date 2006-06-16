/*
 * Created on 30-May-2006
 */
package adt;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.awt.*;

import ij.ImagePlus;

/**
 * adapter pattern, instanciate around a ImagePlus object to then get at the objects 
 * accociated with that ImagePlus instance
 * @author s0570397
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ImageLabels {
	final ImagePlus data;
	
	public static final String IMAGE_LABELS = "Segmentator.imageLabels";
	
	
	public ImageLabels(ImagePlus data){
		this.data = data;
		
		
		//ensure the data structures we are expecting are there
		if(data.getProperty(IMAGE_LABELS)==null){
			data.setProperty(IMAGE_LABELS, new HashMap<String, ImageLabel>());
		}
	}

	private HashMap<String, ImageLabel> getLabelsMap(){
		return (HashMap<String, ImageLabel>) data.getProperty(IMAGE_LABELS);
	}
	
	public Set<String> getLabelNames(){
		return getLabelsMap().keySet();
	}
	
	public ImageLabel getLabel(String name){
		return getLabelsMap().get(name);
	}
	
	public Iterable<ImageLabel> getLabels(){
		return getLabelsMap().values();
	}
	
	public ImageLabel newLabel(String name, double [] color){
		if(getLabelsMap().get(name) != null) throw new RuntimeException("name in use allready");
		
		ImageLabel newLabel = new ImageLabel(name, data, color);
		getLabelsMap().put(name, newLabel);
		return newLabel;
	}

    public ImageLabel newLabel(String name){
		return newLabel(name, new double[]{1f,1f,1f});
	}

	public void removeLabel(String name) {
		getLabelsMap().put(name,null);
	}
	
	
}

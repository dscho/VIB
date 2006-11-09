package vib.app;

import vib.AmiraMeshDecoder;
import vib.AmiraMeshEncoder;
import vib.AmiraTable;
import vib.AmiraTableEncoder;

import java.io.File;

import ij.io.Opener;
import ij.io.FileSaver;
import ij.ImagePlus;

/**
 * Class to represent all components and outputs of an image which 
 * belong to the VIB algorithm. This is: <br>
 * * Path to the image containing the reference channel<br>
 * * Path to the image containing the label fields, located in wd/labels/<br>
 * * A reference to the ImagePlus, corresponding to the ref channel. This may
 * be null, but can be loaded.<br>
 * * A reference to the ImagePlus containing the label fields. This may also be
 * null, but can be loaded.<br>
 * * Additionally, methods are available for retrieving the ImagePlus to a
 * given channel number n, usually located in wd/images-n/
 */
public class VIBImage {

	private String name;
	private String wd;
	private int refChannel;
	private int resamplingF;
	
	private ImagePlus ref = null;
	private ImagePlus labels = null;
	private ImagePlus ref_r = null;
	private ImagePlus labels_r = null;
	
	public VIBImage(String wd, String name, int refChannel, int res) {
		this.wd = wd;
		this.name = name;
		this.refChannel = refChannel;
		this.resamplingF = res;
	}

	public VIBImage(String wd, 
			String name, int refChannel, int res, ImagePlus ref) {
		this(wd, name, refChannel, res);
		this.ref = ref;
	}

	public VIBImage(String wd, String name, int refChannel, int res,
										ImagePlus ref, ImagePlus labels) {
		this(wd, name, refChannel, res, ref);
		this.labels = labels;
	}

	// setter
	public void setReferenceChannel(ImagePlus ref) {
		this.ref = ref;
	}

	public void setLabels(ImagePlus labels) {
		this.labels = labels;
	}

	public void setResampledLabels(ImagePlus labels) {
		this.labels_r = labels;
	}

	public void setResampledReferenceChannel(ImagePlus image) {
		this.ref_r = image;
	}

	public void releaseReferenceChannel() {
		this.ref = null;
	}

	public void releaseLabels() {
		this.labels = null;
	}

	public void releaseResampledReferenceChannel() {
		this.ref_r = null;
	}

	public void releaseResampledLabels() {
		this.labels_r = null;
	}

	// mutators
	public boolean saveReferenceChannel() {
		if(ref == null)
			return false;
		FileSaver fs = new FileSaver(ref);
		return fs.saveAsTiffStack(getReferencePath());
	}

	public boolean saveResampledReferenceChannel() {
		if(ref_r == null)
			return false;
		FileSaver fs = new FileSaver(ref_r);
		return fs.saveAsTiffStack(getResampledReferencePath());
	}

	public boolean saveLabels() {
	    String path = getLabelsPath(); 
		File dir = new File(getLabelsDir());
		if(!dir.exists())
			dir.mkdir();
		else if(!dir.isDirectory())
			return false;
		AmiraMeshEncoder e=new AmiraMeshEncoder(path);
	    if(!e.open()) 
			return false;
		if(!e.write((ImagePlus)labels))
			return false;
		return true;
	}
	
	public boolean saveResampledLabels() {
	    String path = getResampledLabelsPath(); 
		File dir = new File(getResampledLabelsDir());
		if(!dir.exists())
			dir.mkdir();
		else if(!dir.isDirectory())
			return false;
		AmiraMeshEncoder e=new AmiraMeshEncoder(path);
	    if(!e.open()) 
			return false;
		if(!e.write((ImagePlus)labels_r))
			return false;
		return true;
	}

	public boolean saveChannel(int channel, ImagePlus imp) {
		String channelDir = getChannelDir(channel);
		File dir = new File(channelDir);
		if(!dir.exists())
			dir.mkdir();
		else if(!dir.isDirectory())
			return false;
		FileSaver fs = new FileSaver(imp);
		return fs.saveAsTiffStack(getChannelPath(channel));
	}

	public boolean saveResampledChannel(int channel, ImagePlus imp) {
		String channelDir_r = getResampledChannelDir(channel);
		File dir = new File(channelDir_r);
		if(!dir.exists())
			dir.mkdir();
		else if(!dir.isDirectory())
			return false;
		FileSaver fs = new FileSaver(imp);
		return fs.saveAsTiffStack(getResampledChannelPath(channel));
	}

	public boolean saveStatistics(AmiraTable t) {
		File dir = new File(getStatisticsDir());
		if(!dir.exists())
			dir.mkdir();
		else if(!dir.isDirectory())
			return false;
		AmiraTableEncoder e = new AmiraTableEncoder(t);
		if(!e.write(getStatisticsPath()))
			return false;
		return true;
	}
	
	// getter - images
	public ImagePlus getReferenceChannel() {
		if(ref == null) {
			loadRef();
		}
		return ref;
	}

	public ImagePlus getLabels() {
		if(labels == null) {
			loadLabels();
		}
		return labels;
	}

	public ImagePlus getChannel(int channel) {
		return loadChannel(channel);
	}

	public ImagePlus getResampledChannel(int channel) {
		return loadResampledChannel(channel);
	}

	public ImagePlus getResampledReferenceChannel() {
		if(ref_r == null)
			loadResampledRef();
		return ref_r;
	}

	public ImagePlus getResampledLabels() {
		if(labels_r == null)
			loadResampledLabels();
		return labels_r;
	}

	public AmiraTable getStatistics() {
		return loadStatistics();
	}
	
	// getter - paths
	public String getLabelsDir() {
		return wd + File.separator + "labels" + File.separator;
	}

	public String getLabelsName() {
		return name.substring(0, name.lastIndexOf('.')) + ".labels";
	}

	public String getName() {
		return name;
	}

	public String getLabelsPath() {
		return getLabelsDir() + getLabelsName();
	}

	public String getReferenceDir() {
		return wd + File.separator + "images-" + refChannel + File.separator;
	}

	public String getReferencePath() {
		return getReferenceDir() + name;
	}

	public String getChannelDir(int channel) {
		return wd + File.separator + "images-" + channel + File.separator;
	}

	public String getChannelPath(int channel) {
		return getChannelDir(channel) + name;
	}

	public String getResampledChannelDir(int channel) {
		return wd + File.separator + "resampled" + resamplingF
					+ "_" + channel + File.separator;
	}

	public String getResampledChannelPath(int channel) {
		return getResampledChannelDir(channel) + name;
	}

	public String getResampledReferenceDir() {
		return getResampledChannelDir(refChannel);
	}

	public String getResampledReferencePath() {
		return getResampledChannelPath(refChannel);
	}

	public String getResampledLabelsDir() {
		return wd + File.separator + "resampled" + resamplingF
					+ "_labels" + File.separator;
	}

	public String getResampledLabelsPath() {
		return getResampledLabelsDir() + getLabelsName();
	}

	public String getStatisticsDir() {
		return wd + File.separator + "statistics" + File.separator;
	}

	public String getStatisticsName() {
		return name.substring(0, name.lastIndexOf('.')) + ".statistics";
	}

	public String getStatisticsPath() {
		return getStatisticsDir() + getStatisticsName();
	}
	
	// load methods
	private void loadRef() {
		this.ref = new Opener().openImage(getReferencePath());
	}

	private void loadLabels() {
		this.labels = new Opener().openImage(getLabelsPath());
	}

	private void loadResampledRef() {
		this.ref_r = new Opener().openImage(getResampledReferencePath());
	}

	private void loadResampledLabels() {
		this.labels_r = new Opener().openImage(getResampledLabelsPath());
	}

	private ImagePlus loadChannel(int channel) {
		return new Opener().openImage(getChannelPath(channel));
	}

	private ImagePlus loadResampledChannel(int channel) {
		return new Opener().openImage(getResampledChannelPath(channel));
	}

	private AmiraTable loadStatistics() {
		AmiraMeshDecoder d=new AmiraMeshDecoder();
		if(d.open(getStatisticsPath())) {
			if (d.isTable()) {
				AmiraTable table = d.getTable();
				return table;
			} 
		}
		return null;
	}

	public void print() {
		System.out.println("ref = " + ref);
		System.out.println("label = " + labels);
	}
}

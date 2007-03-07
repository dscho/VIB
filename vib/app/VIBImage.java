package vib.app;

import amira.AmiraMeshDecoder;
import amira.AmiraMeshEncoder;
import amira.AmiraTable;
import amira.AmiraTableEncoder;

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

	private Options options;

	private String wd;
	private int refChannel;
	private int resamplingF;

	private VIBImage template = null;
	private ImagePlus ref = null;
	private ImagePlus labels = null;
	private ImagePlus ref_r = null;
	private ImagePlus labels_r = null;

	public String originalPath = null;
	public String name = null;
	public String basename = null;
	public String labelsDir = null;
	public String labelsName = null;
	public String labelsPath = null;
	public String referenceDir = null;
	public String referencePath = null;
	public String resampledReferenceDir = null;
	public String resampledReferencePath = null;
	public String resampledLabelsDir = null;
	public String resampledLabelsPath = null;
	public String statisticsDir = null;
	public String statisticsName = null;
	public String statisticsPath = null;
	public String warpedLabelsPath = null;
	public String warpedLabelsDir = null;
	public String averageDir = null;
	public String averageLabelsName = null;
	public String averageLabelsPath = null;

	// Constructor
	public VIBImage(File file, Options options) {
		this.options = options;
		this.wd = options.getWorkingDirectory().getAbsolutePath();
		this.name = file.getName();
		this.originalPath = file.getAbsolutePath();
		this.refChannel = options.getRefChannel();
		this.resamplingF = options.getResamplingFactor();
		initPaths();
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
		File dir = new File(referenceDir);
		if(!dir.exists())
			dir.mkdir();
		FileSaver fs = new FileSaver(ref);
		return fs.saveAsTiffStack(referencePath);
	}

	public boolean saveResampledReferenceChannel() {
		if(ref_r == null)
			return false;
		File dir = new File(resampledReferenceDir);
		if(!dir.exists())
			dir.mkdir();
		FileSaver fs = new FileSaver(ref_r);
		return fs.saveAsTiffStack(resampledReferencePath);
	}

	public boolean saveLabels() {
	    String path = labelsPath; 
		File dir = new File(labelsDir);
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
	    String path = resampledLabelsPath; 
		File dir = new File(resampledLabelsDir);
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
		File dir = new File(statisticsDir);
		if(!dir.exists())
			dir.mkdir();
		else if(!dir.isDirectory())
			return false;
		AmiraTableEncoder e = new AmiraTableEncoder(t);
		if(!e.write(statisticsPath))
			return false;
		return true;
	}
	
	public boolean saveWarped(int channel, ImagePlus imp) {
		String warpedDir = getWarpedDir(channel);
		File dir = new File(warpedDir);
		if(!dir.exists())
			dir.mkdir();
		else if(!dir.isDirectory())
			return false;
		FileSaver fs = new FileSaver(imp);
		return fs.saveAsTiffStack(getWarpedPath(channel));
	}	

	public boolean saveWarpedLabels(ImagePlus imp) {
		File dir = new File(warpedLabelsDir);
		if(!dir.exists())
			dir.mkdir();
		else if(!dir.isDirectory())
			return false;
		FileSaver fs = new FileSaver(imp);
		return fs.saveAsTiffStack(warpedLabelsPath);
	}

	public boolean saveAverageChannel(ImagePlus imp, int channel) {
		File dir = new File(averageDir);
		if(!dir.exists())
			dir.mkdir();
		else if(!dir.isDirectory())
			return false;
		FileSaver fs = new FileSaver(imp);
		return fs.saveAsTiffStack(getAverageChannelPath(channel));
	}

	public boolean saveAverageLabels(ImagePlus imp) {
		File dir = new File(averageDir);
		if(!dir.exists())
			dir.mkdir();
		else if(!dir.isDirectory())
			return false;
		FileSaver fs = new FileSaver(imp);
		return fs.saveAsTiffStack(averageLabelsPath);
	}
	

	// getter - paths
	public String getChannelDir(int channel) {
		return wd + File.separator + "images_" + channel + File.separator;
	}

	public String getChannelPath(int channel) {
		return getChannelDir(channel) + name;
	}

	public String getResampledChannelDir(int channel) {
		String dir = wd + File.separator;
		dir += resamplingF == 1 ? "images" : "resampled" + resamplingF;
		return dir + "_" + channel + File.separator;
	}

	public String getResampledChannelPath(int channel) {
		return getResampledChannelDir(channel) + name;
	}	

	public String getResampledLabelsDir() {
		String dir = wd + File.separator;
		dir += resamplingF == 1 ? "labels" 
								: "resampled" + resamplingF + "_labels";
		return dir + File.separator;
	}

	public String getWarpedDir(int channel) {
		return wd + File.separator + "warped_" + channel + File.separator;
	}

	public String getWarpedPath(int channel) {
		return getWarpedDir(channel) + basename + ".warped";
	}

	public String getAverageChannelPath(int channel) {
		return averageDir + File.separator + getAverageChannelName(channel);
	}

	public String getAverageChannelName(int channel) {
		return "channel" + channel + ".average";
	}
	
	// getter - images
	public VIBImage getTemplate() {
		if(template == null) {
			template = new VIBImage(options.getTemplate(), options);
		}
		return template;
	}
	
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

	public ImagePlus getWarped(int channel) {
		return loadWarped(channel);
	}

	public ImagePlus getWarpedLabels() {
		return loadWarpedLabels();
	}

	public ImagePlus getAverageChannel(int channel) {
		return loadAverageChannel(channel);
	}

	public ImagePlus getAverageLabels() {
		return loadAverageLabels();
	}

	public void print() {
		System.out.println("ref = " + ref);
		System.out.println("label = " + labels);
	}
	
	// load methods
	private void loadRef() {
		this.ref = new Opener().openImage(referencePath);
	}

	private void loadLabels() {
		this.labels = new Opener().openImage(labelsPath);
	}

	private void loadResampledRef() {
		this.ref_r = new Opener().openImage(resampledReferencePath);
	}

	private void loadResampledLabels() {
		this.labels_r = new Opener().openImage(resampledLabelsPath);
	}

	private ImagePlus loadChannel(int channel) {
		return new Opener().openImage(getChannelPath(channel));
	}

	private ImagePlus loadResampledChannel(int channel) {
		return new Opener().openImage(getResampledChannelPath(channel));
	}

	private AmiraTable loadStatistics() {
		AmiraMeshDecoder d=new AmiraMeshDecoder();
		if(d.open(statisticsPath)) {
			if (d.isTable()) {
				AmiraTable table = d.getTable();
				return table;
			} 
		}
		return null;
	}

	private ImagePlus loadWarped(int channel) {
		return new Opener().openImage(getWarpedPath(channel));
	}

	private ImagePlus loadWarpedLabels() {
		return new Opener().openImage(warpedLabelsPath);
	}

	private ImagePlus loadAverageChannel(int channel) {
		return new Opener().openImage(getAverageChannelPath(channel));
	}

	private ImagePlus loadAverageLabels() {
		return new Opener().openImage(averageLabelsPath);
	}

	private void initPaths() {
		basename		= name.substring(0, name.lastIndexOf('.'));
		// labels
		labelsDir 		= wd + File.separator + "labels" + File.separator;
		labelsName 		= name.substring(0, name.lastIndexOf('.')) + ".labels";
		labelsPath 		= labelsDir + labelsName;

		// refChannel
		referenceDir	= getChannelDir(refChannel);
		referencePath 	= getChannelPath(refChannel);

		// resampledRef
		resampledReferenceDir 	= getResampledChannelDir(refChannel);
		resampledReferencePath	= getResampledChannelPath(refChannel);

		// resampledLabels
		resampledLabelsDir		= getResampledLabelsDir();
		resampledLabelsPath 	= resampledLabelsDir + labelsName;

		// statistics
		statisticsDir	= wd + File.separator + "statistics" + File.separator;
		statisticsName  = name.substring(0, name.lastIndexOf('.')) 
												+ ".statistics";
		statisticsPath 	= statisticsDir + statisticsName;

		// warped labels
		warpedLabelsDir = wd+File.separator+"warped_labels"+File.separator;
		warpedLabelsPath = warpedLabelsDir + basename + ".warped";

		// average brains
		averageDir 			= wd+File.separator+"average"+File.separator;
		averageLabelsName 	= "labels.average";
		averageLabelsPath 	= averageDir + averageLabelsName;
	}
}

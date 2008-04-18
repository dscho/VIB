package voltex;

import java.awt.Polygon;
import java.awt.image.IndexColorModel;

import java.util.List;
import java.util.Vector;
import javax.vecmath.Color3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;
import javax.vecmath.Point2d;
import javax.vecmath.Point3f;
import javax.vecmath.Point3d;
import javax.media.j3d.View;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.BranchGroup;
import com.sun.j3d.utils.behaviors.picking.PickingCallback;

import ij.ImagePlus;
import ij.IJ;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.measure.Calibration;

import ij3d.Content;
import ij3d.ImageCanvas3D;
import ij3d.Image3DUniverse;
import ij3d.ColorTable;

import vib.Resample_;

public class VoltexGroup extends Content {

	private Renderer renderer;
	private TransformGroup tg;
	private float volume;

	public VoltexGroup(String name, Color3f color, ImagePlus image, 
		boolean[] channels, int resamplingF) {
		
		super(name, color, image, channels, resamplingF);
		float scale = image.getWidth() * 
				(float)image.getCalibration().pixelWidth;

		IndexColorModel cmodel = color == null ? 
			ColorTable.getIndexedColorModel(image, channels) :
			ColorTable.getAverageGrayColorModel(image, channels);
		ImagePlus imp = resamplingF == 1 ? image 
				: Resample_.resample(image, resamplingF);
		renderer = new VolumeRenderer(imp, cmodel, 
					color, getTransparency());
		renderer.fullReload();

		getLocalRotate().addChild(renderer.getVolumeNode());

		compile();
	}

	public void thresholdUpdated(int d) {
		renderer.setThreshold(d);
	}

	public void calculateMinMaxCenterPoint() {
		ImagePlus imp = getImage();
		int w = imp.getWidth(), h = imp.getHeight();
		int d = imp.getStackSize();
		Calibration c = imp.getCalibration();
		minPoint = new Point3f();
		maxPoint = new Point3f();
		centerPoint = new Point3f();
		minPoint.x = w * (float)c.pixelHeight;
		minPoint.y = h * (float)c.pixelHeight;
		minPoint.z = d * (float)c.pixelDepth;
		maxPoint.x = 0;
		maxPoint.y = 0;
		maxPoint.z = 0;

		long vol = 0;
		for(int zi = 0; zi < d; zi++) {
			float z = zi * (float)c.pixelDepth;
			byte[] p = (byte[])imp.getStack().getPixels(zi+1);
			for(int i = 0; i < p.length; i++) {
				if(p[i] == 0) continue;
				vol += (p[i] & 0xff);
				float x = (i % w) * (float)c.pixelWidth;
				float y = (i / w) * (float)c.pixelHeight;
				if(x < minPoint.x) minPoint.x = x;
				if(y < minPoint.y) minPoint.y = y;
				if(z < minPoint.z) minPoint.z = z;
				if(x > maxPoint.x) maxPoint.x = x;
				if(y > maxPoint.y) maxPoint.y = y;
				if(z > maxPoint.z) maxPoint.z = z;
				centerPoint.x += (p[i] & 0xff) * x;
				centerPoint.y += (p[i] & 0xff) * y;
				centerPoint.z += (p[i] & 0xff) * z;
			}
		}
		centerPoint.x /= vol;
		centerPoint.y /= vol;
		centerPoint.z /= vol;

		volume = (float)(vol * c.pixelWidth
				* c.pixelHeight
				* c.pixelDepth);
		
		createBoundingBox();
		showBoundingBox(false);
	}

	public float getVolume() {
		return volume;
	}

// 	public void calculateMinMaxCenterPoint() {
// 		ImagePlus imp = getImage();
// 		Calibration c = imp.getCalibration();
// 		minPoint = new Point3f();
// 		maxPoint = new Point3f((float)(imp.getWidth()*c.pixelWidth),
// 				(float)(imp.getHeight()*c.pixelHeight),
// 				(float)(imp.getStackSize()*c.pixelDepth));
// 		centerPoint = new Point3f(maxPoint.x/2, maxPoint.y/2, 
// 				maxPoint.z/2);
// 		createBoundingBox();
// 		showBoundingBox(false);
// 	}
		
	public static VoltexGroup addContent(Image3DUniverse univ, 
							ImagePlus grey) {
		GenericDialog gd = new GenericDialog("Add grey");
		int img_count = WindowManager.getImageCount();
		Vector greyV = new Vector();
		String[] greys;
		if(grey == null) {
			for(int i=1; i<=img_count; i++) {
				int id = WindowManager.getNthImageID(i);
				ImagePlus imp = WindowManager.getImage(id);
				if(imp != null){
					 greyV.add(imp.getTitle());
				}
			}
			if(greyV.size() == 0)
				IJ.error("No images open");
			greys = (String[])greyV.toArray(new String[]{});
			gd.addChoice("Image", greys, greys[0]);
		}
		String tmp = grey != null ? grey.getTitle() : "";
		gd.addStringField("Name", tmp, 10);
		gd.addChoice("Color", ColorTable.colorNames, 
						ColorTable.colorNames[0]);
		gd.addNumericField("Resampling factor", 2, 0);
		gd.addMessage("Channels");
		gd.addCheckboxGroup(1, 3, 
					new String[] {"red", "green", "blue"}, 
					new boolean[]{true, true, true});
		gd.showDialog();
		if(gd.wasCanceled())
			return null;
			
		if(grey == null)
			grey = WindowManager.getImage(gd.getNextChoice());
		String name = gd.getNextString();
		Color3f color = ColorTable.getColor(gd.getNextChoice());
		int factor = (int)gd.getNextNumber();
		boolean[] channels = new boolean[]{gd.getNextBoolean(), 
						gd.getNextBoolean(), 
						gd.getNextBoolean()};
		Vector3f tr = new Vector3f();
		if(grey != null) {
			Calibration c = grey.getCalibration();
			tr.x = (float)(-grey.getWidth() * c.pixelWidth/2);
			tr.y = (float)(-grey.getHeight() * c.pixelHeight/2);
			tr.z = (float)(-grey.getStackSize() * c.pixelDepth/2);
		}

		if(univ.contains(name)) {
			IJ.error("Could not add new content. A content with " +
				"name \"" + name + "\" exists already.");
			return null;
		}
		
		return univ.addVoltex(grey, color, name, channels, factor);
	}

	public void eyePtChanged(View view) {
		renderer.eyePtChanged(view);
	}

	public void channelsUpdated(boolean[] channels) {
		IndexColorModel cmodel = getColor() == null ?
			ColorTable.getIndexedColorModel(
				getImage(), channels) :
			ColorTable.getAverageGrayColorModel(
				getImage(), channels);
		renderer.setColorModel(cmodel);
	}

	public void colorUpdated(Color3f oldColor, Color3f newColor) {
		// color model only needs update if there is a switch
		// between null and non-null color
		if(oldColor == null && newColor != null || 
			oldColor != null && newColor == null) {

			IndexColorModel cmodel = newColor == null ?
				ColorTable.getIndexedColorModel(
					getImage(), getChannels()) :
				ColorTable.getAverageGrayColorModel(
					getImage(), getChannels());
			renderer.setColorModel(cmodel);
		}
		renderer.setColor(newColor);
	}

	public void transparencyUpdated(float transparency) {
		renderer.setTransparency(transparency);
	}

	public void volumeToImagePlate(Transform3D toImagePlate) {
		Transform3D toVWorld = new Transform3D();
		renderer.getVolumeNode().getLocalToVworld(toVWorld);
		toImagePlate.mul(toVWorld);
	}

	public Point2d volumePointInCanvas(Canvas3D canvas, Transform3D volToIP,
							int x, int y, int z) {
		
		Volume volume = renderer.volume;
		double px = x * volume.xSpace;
		double py = y * volume.ySpace;
		double pz = z * volume.zSpace;
		Point3d locInImagePlate = new Point3d(px, py, pz);
		
		volToIP.transform(locInImagePlate);

		Point2d onCanvas = new Point2d();
		canvas.getPixelLocationFromImagePlate(locInImagePlate, onCanvas);

		return onCanvas;
	}

	public void fillRoiBlack(Image3DUniverse universe, byte fillValue) {
		ImageCanvas3D canvas = (ImageCanvas3D)universe.getCanvas();
		Polygon p = canvas.getRoi().getPolygon();
		
		Transform3D volToIP = new Transform3D();
		canvas.getImagePlateToVworld(volToIP);
		volToIP.invert();
		volumeToImagePlate(volToIP);

		ImagePlus image = renderer.image;
		int w = image.getWidth(), h = image.getHeight();
		int d = image.getStackSize();
		for(int z = 0; z < d; z++) {
			byte[] data =(byte[])image.getStack().getPixels(z+1);
			for(int y = 0; y < h; y++) {
				for(int x = 0; x < w; x++) {
					int index = y * w + x;
					Point2d onCanvas = volumePointInCanvas(
							canvas, volToIP,x,y,z);
					if(p.contains(onCanvas.x, onCanvas.y)) {
						data[index] = fillValue;
					}
				}
			}
			IJ.showStatus("Filling...");
			IJ.showProgress(z, d);
		}
		renderer.fullReload();
		// also fill the original image
		image = super.image;
		w = image.getWidth(); 
		h = image.getHeight();
		d = image.getStackSize();
		int factor = getResamplingFactor();
		for(int z = 0; z < d; z++) {
			byte[] data =(byte[])image.getStack().getPixels(z+1);
			for(int y = 0; y < h; y++) {
				for(int x = 0; x < w; x++) {
					int index = y * w + x;
					Point2d onCanvas = volumePointInCanvas(
						canvas, 
						volToIP,
						x/factor,
						y/factor,
						z/factor);
					if(p.contains(onCanvas.x, onCanvas.y)) {
						data[index] = fillValue;
					}
				}
			}
			IJ.showStatus("Filling...");
			IJ.showProgress(z, d);
		}
	}

	public void flush() {
		System.out.println("Set renderer = null");
		renderer = null;
		image = null;
	}
}


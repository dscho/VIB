package voltex;

import java.awt.Polygon;
import java.awt.image.IndexColorModel;

import java.util.List;
import java.util.Vector;
import javax.vecmath.Color3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Point2d;
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

import ij3d.Content;
import ij3d.ImageCanvas3D;
import ij3d.Image3DUniverse;
import ij3d.ColorTable;

import vib.Resample_;

public class VoltexGroup extends Content {

	private Renderer renderer;
	private TransformGroup tg;

	public VoltexGroup(String name, String color, 
			ImagePlus image, boolean[] channels, int resamplingF) {
		super(name, color, image, channels, resamplingF);
		
		float scale = image.getWidth() * 
					(float)image.getCalibration().pixelWidth;

		IndexColorModel cmodel = ColorTable.adjustColorModel(
				(IndexColorModel)image.getProcessor().getColorModel(), 
				color, channels);
		ImagePlus imp = resamplingF == 1 ? image 
						: Resample_.resample(image, resamplingF);
		renderer = new Axis2DRenderer(image, cmodel);
		renderer.fullReload();

		Point3d maxCoord = renderer.volume.maxCoord;
		Point3d minCoord = renderer.volume.minCoord;
		
		Transform3D translate = new Transform3D();
		translate.setTranslation(new Vector3d(
					-(maxCoord.x-minCoord.x)/2, 
					-(maxCoord.y-minCoord.y)/2, 
					-(maxCoord.z-minCoord.z)/2));
		tg = new TransformGroup(translate);
		tg.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		tg.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		
		pickTr.addChild(tg);
		tg.addChild(renderer.getVolumeNode());

		compile();
	}
		
	public static void addContent(Image3DUniverse univ, ImagePlus grey) {
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
		gd.addChoice("Color", 
				ColorTable.colorNames, ColorTable.colorNames[0]);
		gd.addNumericField("Resampling factor", 2, 0);
		gd.addMessage("Channels");
		gd.addCheckboxGroup(1, 3, new String[] {"red", "green", "blue"}, 
						new boolean[]{true, true, true});
		gd.showDialog();
		if(gd.wasCanceled())
			return;
			
		if(grey == null)
			grey = WindowManager.getImage(gd.getNextChoice());
		String name = gd.getNextString();
		String color = gd.getNextChoice();
		int factor = (int)gd.getNextNumber();
		boolean[] channels = new boolean[]{gd.getNextBoolean(), 
								gd.getNextBoolean(), 
								gd.getNextBoolean()};
		univ.addVoltex(grey, color, name, channels, factor);
	}

	public void eyePtChanged(View view) {
		renderer.eyePtChanged(view);
	}

	public void colorUpdated(String color, boolean[] channels) {
		IndexColorModel cmodel = ColorTable.adjustColorModel(
			(IndexColorModel)getImage().getProcessor().getColorModel(), 
			color, channels);
		ImagePlus imp = getResamplingFactor() == 1 ? getImage()
				: Resample_.resample(getImage(), getResamplingFactor());
		renderer = new Axis2DRenderer(getImage(), cmodel);
		renderer.fullReload();
		tg.removeChild(0);
		tg.addChild(renderer.getVolumeNode());
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
			byte[] data =(byte[])image.getStack().
								getProcessor(z+1).getPixels();
			for(int y = 0; y < h; y++) {
				for(int x = 0; x < w; x++) {
					int index = y * w + x;
					Point2d onCanvas = 
						volumePointInCanvas(canvas, volToIP,x,y,z);
					if(p.contains(onCanvas.x, onCanvas.y)) {
						data[index] = fillValue;
					}
				}
			}
			IJ.showStatus("Filling...");
			IJ.showProgress(z, d);
		}
		renderer.fullReload();
	}
}


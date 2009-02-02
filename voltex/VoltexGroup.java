package voltex;

import java.awt.Polygon;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.media.j3d.View;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.Transform3D;

import ij.ImagePlus;
import ij.IJ;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.process.ImageProcessor;

import ij3d.Content;
import ij3d.ContentNode;

import javax.vecmath.Tuple3d;
import vib.Resample_;

/**
 * This class extends ContentNode to display a Content as a
 * Volume Rendering.
 * 
 * @author Benjamin Schmid
 */
public class VoltexGroup extends ContentNode {

	/** The VolumeRenderer behind this VoltexGroup */
	protected VolumeRenderer renderer;

	/** Reference to the Content which holds this VoltexGroup */
	protected Content c;

	/** The volume of this VoltexGroup */
	private float volume;

	/** The minimum coordinate of this VoltexGroup */
	private Point3d min;

	/** The maximum coordinate of this VoltexGroup */
	private Point3d max;

	/** The center point of this VoltexGroup */
	private Point3d center;

	/**
	 * This constructor only exists to allow supclasses to access the super
	 * constructor of BranchGroup.
	 */
	protected VoltexGroup() {
		super();
	}

	/**
	 * Initialize this VoltexGroup with the specified Content.
	 * @param c
	 * @throws IllegalArgumentException if the specified Content has no image.
	 */
	public VoltexGroup(Content c) {
		super();
		if(c.getImage() == null)
			throw new IllegalArgumentException("VoltexGroup can only" +
				"be initialized from a Content that holds an image.");
		this.c = c;
		ImagePlus imp = c.getResamplingFactor() == 1 
			? c.getImage() 
			: Resample_.resample(c.getImage(), c.getResamplingFactor());
		renderer = new VolumeRenderer(imp, c.getColor(),
				c.getTransparency(), c.getChannels());
		renderer.fullReload();
		calculateMinMaxCenterPoint();
		addChild(renderer.getVolumeNode());
	}

	/**
	 * @see ContentNode#getMin(Tupe3d) getMin
	 */
	public void getMin(Tuple3d min) {
		min.set(this.min);
	}

	/**
	 * @see ContentNode#getMax (Tupe3d) getMax
	 */
	public void getMax(Tuple3d max) {
		max.set(this.max);
	}

	/**
	 * @see ContentNode#getCenter(Tupe3d) getCenter
	 */
	public void getCenter(Tuple3d center) {
		center.set(this.center);
	}

	/**
	 * @see ContentNode#thresholdUpdated() thresholdUpdated
	 */
	public void thresholdUpdated() {
		renderer.setThreshold(c.getThreshold());
	}

	/**
	 * @see ContentNode#getVolume() getVolume
	 */
	public float getVolume() {
		return volume;
	}

	/**
	 * @see ContentNode#eyePtChanged(View view) eyePtChanged
	 */
	public void eyePtChanged(View view) {
		renderer.eyePtChanged(view);
	}

	/**
	 * @see ContentNode#channelsUpdated() channelsUpdated
	 */
	public void channelsUpdated() {
		renderer.setChannels(c.getChannels());
	}

	/**
	 * @see ContentNode#shadeUpdated() shadeUpdated
	 */
	public void shadeUpdated() {
		// do nothing
	}

	/**
	 * @see ContentNode#colorUpdated() colorUpdated
	 */
	public void colorUpdated() {
		renderer.setColor(c.getColor());
	}

	/**
	 * @see ContentNode#transparencyUpdated() transparencyUpdated
	 */
	public void transparencyUpdated() {
		renderer.setTransparency(c.getTransparency());
	}

	/**
	 * Stores the matrix which transforms this VoltexGroup to the
	 * image plate in the specified Transform3D.
	 * @param toImagePlate
	 */
	public void volumeToImagePlate(Transform3D toImagePlate) {
		Transform3D toVWorld = new Transform3D();
		renderer.getVolumeNode().getLocalToVworld(toVWorld);
		toImagePlate.mul(toVWorld);
	}

	/**
	 * Fills the projection of the specified ROI with the given fillValue.
	 * Does nothing if the given ROI is null.
	 * Works not only on the internally created image (the resampled one),
	 * but also on the original image.
	 * @param universe
	 * @param fillValue
	 */
	public void fillRoiBlack(Canvas3D canvas, Roi roi, byte fillValue) {
		if(roi == null)
			return;

		Polygon p = roi.getPolygon();
		Transform3D volToIP = new Transform3D();
		canvas.getImagePlateToVworld(volToIP);
		volToIP.invert();
		volumeToImagePlate(volToIP);

		Volume vol = renderer.getVolume();
		for(int z = 0; z < vol.zDim; z++) {
			for(int y = 0; y < vol.yDim; y++) {
				for(int x = 0; x < vol.xDim; x++) {
					Point2d onCanvas = volumePointInCanvas(
							canvas, volToIP, x, y, z);
					if(p.contains(onCanvas.x, onCanvas.y)) {
						vol.set(x, y, z, fillValue);
					}
				}
			}
			IJ.showStatus("Filling...");
			IJ.showProgress(z, vol.zDim);
		}
		renderer.fullReload();
		// also fill the original image
		ImagePlus image = c.getImage();
		int factor = c.getResamplingFactor();
		if(image == null || factor == 1)
			return;

		vol = new Volume(image);
		for(int z = 0; z < vol.zDim; z++) {
			for(int y = 0; y < vol.yDim; y++) {
				for(int x = 0; x < vol.xDim; x++) {
					Point2d onCanvas = volumePointInCanvas( canvas, 
						volToIP, x/factor, y/factor, z/factor);
					if(p.contains(onCanvas.x, onCanvas.y)) {
						vol.set(x, y, z, fillValue);
					}
				}
			}
			IJ.showStatus("Filling...");
			IJ.showProgress(z, vol.zDim);
		}
	}

	/**
	 * Returns the 3D coordinates of the given x, y, z position on the
	 * 3D canvas.
	 * @param canvas
	 * @param volToIP
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	private Point2d volumePointInCanvas(Canvas3D canvas, Transform3D volToIP,
							int x, int y, int z) {
		
		Volume vol = renderer.volume;
		double px = x * vol.pw;
		double py = y * vol.ph;
		double pz = z * vol.pd;
		Point3d locInImagePlate = new Point3d(px, py, pz);
		
		volToIP.transform(locInImagePlate);

		Point2d onCanvas = new Point2d();
		canvas.getPixelLocationFromImagePlate(locInImagePlate, onCanvas);

		return onCanvas;
	}

	/**
	 * Calculate the minimum, maximum and center coordinate, together with
	 * the volume.
	 */
	protected void calculateMinMaxCenterPoint() {
		ImagePlus imp = c.getImage();
		int w = imp.getWidth(), h = imp.getHeight();
		int d = imp.getStackSize();
		Calibration cal = imp.getCalibration();
		min = new Point3d();
		max = new Point3d();
		center = new Point3d();
		min.x = w * (float)cal.pixelHeight;
		min.y = h * (float)cal.pixelHeight;
		min.z = d * (float)cal.pixelDepth;
		max.x = 0;
		max.y = 0;
		max.z = 0;

		long vol = 0;
		for(int zi = 0; zi < d; zi++) {
			float z = zi * (float)cal.pixelDepth;
			ImageProcessor ip = imp.getStack().getProcessor(zi+1);
			int wh = w * h;
			for(int i = 0; i < wh; i++) {
				int v = ip.get(i);
				if(v == 0) continue;
				vol += v;
				float x = (i % w) * (float)cal.pixelWidth;
				float y = (i / w) * (float)cal.pixelHeight;
				if(x < min.x) min.x = x;
				if(y < min.y) min.y = y;
				if(z < min.z) min.z = z;
				if(x > max.x) max.x = x;
				if(y > max.y) max.y = y;
				if(z > max.z) max.z = z;
				center.x += v * x;
				center.y += v * y;
				center.z += v * z;
			}
		}
		center.x /= vol;
		center.y /= vol;
		center.z /= vol;

		volume = (float)(vol * cal.pixelWidth
				* cal.pixelHeight
				* cal.pixelDepth);
		
	}
}

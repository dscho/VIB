package surfaceplot;

import ij.ImagePlus;
import ij.gui.StackWindow;
import ij3d.Content;
import ij3d.ContentNode;
import ij3d.Volume;

import java.awt.Component;
import java.awt.Scrollbar;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import javax.media.j3d.View;
import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;

import vib.Resample_;

/**
 * This class extends ContentNode to render a Content as a surface plot.
 * By default, the current slice of the image stack is rendered. When
 * the slider in the original image window is dragged, the surface plot
 * is automatically updated.
 * 
 * @author Benjamin Schmid
 */
public class SurfacePlotGroup extends ContentNode implements AdjustmentListener{

	/** The actual surface plot object */
	private SurfacePlot surfacep;

	/** The content which is displayed */
	private Content c;

	/** The min coordinate */
	private Point3d min = new Point3d();
	/** The max coordinate */
	private Point3d max = new Point3d();
	/** The center coordinate */
	private Point3d center = new Point3d();

	/**
	 * Constructs a surface plot for the given Content.
	 * @param c
	 */
	public SurfacePlotGroup (Content c) {
		super();
		this.c = c;
		int res = c.getResamplingFactor();
		ImagePlus imp = res == 1 ? c.getImage() 
			: Resample_.resample(c.getImage(), res, res, 1);
		Volume volume = new Volume(imp);
		volume.setAverage(true);
		volume.setChannels(c.getChannels());
		surfacep = new SurfacePlot(volume, c.getColor(),
				c.getTransparency(), c.getImage().getSlice());

		surfacep.calculateMinMaxCenterPoint(min, max, center);
		addChild(surfacep);
		if(c.getImage().getStackSize() == 1)
			return;
		StackWindow win = (StackWindow)c.getImage().getWindow();
		if(win == null)
			return;
		Component[] co = win.getComponents();
		for(int i = 0; i < co.length; i++) {
			if(co[i] instanceof Scrollbar) {
				((Scrollbar)co[i]).addAdjustmentListener(this);
			}
		}
	}

	/**
	 * Implements AdjustmentListener interface to automatically update the
	 * surface plot when the slice slider in the stack window is dragged.
	 * @param e
	 */
	public void adjustmentValueChanged(AdjustmentEvent e) {
		surfacep.setSlice(((Scrollbar)e.getSource()).getValue());
	}

	/**
	 * @see ContentNode#getMax(Tupe3d) getMax
	 */
	public void getMax(Tuple3d max) {
		max.set(this.max);
	}

	/**
	 * @see ContentNode#getMin(Tupe3d) getMin
	 */
	public void getMin(Tuple3d min) {
		min.set(this.min);
	}

	/**
	 * @see ContentNode#getCenter(Tupe3d) getCenter
	 */
	public void getCenter(Tuple3d center) {
		center.set(this.center);
	}

	/**
	 * @see ContentNode#eyePtChanged(View) eyePtChanged
	 */
	public void eyePtChanged(View view) {
		// do nothing
	}

	/**
	 * @see ContentNode#thresholdUpdated(Tupe3d) thresholdUpdated
	 */
	public void thresholdUpdated() {
		// TODO
	}

	/**
	 * @see ContentNode#channelsUpdated(Tupe3d) channelsUpdated
	 */
	public void channelsUpdated() {
		surfacep.setChannels(c.getChannels());
	}

	/**
	 * @see ContentNode#getVolume() getVolume
	 */
	public float getVolume() {
		if(surfacep == null)
			return -1;
		// TODO
		return 0f;
	}

	/**
	 * @see ContentNode#colorUpdated() colorUpdated
	 */
	public void colorUpdated() {
		surfacep.setColor(c.getColor());
	}

	/**
	 * @see ContentNode#transparencyUpdated() transparencyUpdated
	 */
	public void transparencyUpdated() {
		surfacep.setTransparency(c.getTransparency());
	}

	/**
	 * @see ContentNode#shadeUpdated() shadeUpdated
	 */
	public void shadeUpdated() {
		surfacep.setShaded(c.isShaded());
	}
}

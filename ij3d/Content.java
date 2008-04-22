package ij3d;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;

import java.awt.image.IndexColorModel;

import java.util.BitSet;

import isosurface.IsoShape;
import javax.media.j3d.*;
import javax.vecmath.Color3f;
import com.sun.j3d.utils.behaviors.mouse.MouseRotate;
import com.sun.j3d.utils.behaviors.mouse.MouseBehaviorCallback;

import javax.vecmath.Vector3f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Point3f;

public abstract class Content extends BranchGroup {

	String name;
	Color3f color;
	protected ImagePlus image;
	boolean[] channels = new boolean[]{true, true, true};
	float transparency = 0f;
	int resamplingF = 1;
	protected int threshold = 0;
	private boolean locked = false;
	private boolean visible = true;

	private Switch bbSwitch;
	private BitSet whichChild = new BitSet(2);
	
	protected boolean selected;
	protected Point3f centerPoint, minPoint, maxPoint;
	
	protected TransformGroup localRotate;
	protected TransformGroup localTranslate;

	public static final int BB = 0;
	public static final int CS = 1;
	public static final int CO = 2;

	public Content() {
		// create BranchGroup for this image
		this.name = name;
		setCapability(BranchGroup.ALLOW_DETACH);
		setCapability(BranchGroup.ENABLE_PICK_REPORTING);

		// create transformation for pickeing
		localTranslate = new TransformGroup();
		localTranslate.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		localTranslate.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		addChild(localTranslate);
		localRotate = new TransformGroup();
		localRotate.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		localRotate.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		localTranslate.addChild(localRotate);

		bbSwitch = new Switch();
		bbSwitch.setWhichChild(Switch.CHILD_MASK);
		bbSwitch.setCapability(Switch.ALLOW_SWITCH_READ);
		bbSwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
		localRotate.addChild(bbSwitch);
	}

	public Content(String name, Color3f color) {
		this();
		this.name = name;
		this.color = color;
	}

	public Content(String name, Color3f color, ImagePlus image, boolean[] 
		channels, int resamplingF) {
		
		this(name, color);
		this.image = image;
		this.channels = channels;
		this.resamplingF = resamplingF;
		calculateMinMaxCenterPoint();
	}

	public void addContentChild(BranchGroup bg) {
		bbSwitch.addChild(bg);
		whichChild.set(BB, false);
		whichChild.set(CS, true);
		whichChild.set(CO, true);
		bbSwitch.setChildMask(whichChild);
	}
	
	public void createBoundingBox() {
		while(bbSwitch.numChildren() > 0)
			bbSwitch.removeChild(0);
			
		BoundingBox b = new BoundingBox(minPoint, maxPoint);
		bbSwitch.addChild(b);
		float cl = (float)Math.abs(maxPoint.x - minPoint.x) / 5f;
		CoordinateSystem cs = new CoordinateSystem(cl, new Color3f(0, 1, 0));
		bbSwitch.addChild(cs);
		// initially show the bounding box, but not the coordinate system
		whichChild.set(BB, false);
		whichChild.set(CS, true);
		bbSwitch.setChildMask(whichChild);
	}

	public void setVisible(boolean b) {
		visible = b;
		whichChild.set(CO, b);
		bbSwitch.setChildMask(whichChild);
	}

	public void showBoundingBox(boolean b) {
		whichChild.set(BB, b);
		bbSwitch.setChildMask(whichChild);
	}

	public void showCoordinateSystem(boolean b) {
		whichChild.set(CS, b);
		bbSwitch.setChildMask(whichChild);
	}
	
	public void setSelected(boolean selected) {
		this.selected = selected;
		showBoundingBox(selected);
	}

	public void toggleLock() {
		locked = !locked;
	}

	public void setLocked(boolean b) {
		locked = b;
	}

	public void applyTransform(Transform3D transform) {
		Transform3D t1 = new Transform3D();
		localTranslate.getTransform(t1);
		Transform3D t2 = new Transform3D();
		localRotate.getTransform(t2);
		t1.mul(t2);

		transform.mul(t1);
		setTransform(transform);
	}

	public void setTransform(Transform3D transform) {
		Transform3D t = new Transform3D();
		Point3f c = centerPoint;
		
		Matrix3f m = new Matrix3f();
		transform.getRotationScale(m);
		t.setRotationScale(m);
		// One might thing a rotation matrix has no translational 
		// component, however, if the rotation is composed of 
		// translation - rotation - backtranslation, it has indeed.
		Vector3f v = new Vector3f();
		v.x = -m.m00*c.x - m.m01*c.y - m.m02*c.z + c.x;
		v.y = -m.m10*c.x - m.m11*c.y - m.m12*c.z + c.y;
		v.z = -m.m20*c.x - m.m21*c.y - m.m22*c.z + c.z;
		t.setTranslation(v);
		localRotate.setTransform(t);
		
		Vector3f v2 = new Vector3f();
		transform.get(v2);
		v2.sub(v);
		t.set(v2);
		localTranslate.setTransform(t);
	}

	public void setChannels(boolean[] channels) {
		boolean channelsChanged = channels[0] != this.channels[0] || 
				channels[1] != this.channels[1] || 
				channels[2] != this.channels[2];
		if(!channelsChanged)
			return;
		this.channels = channels;
		channelsUpdated(channels);
	}

	public void setThreshold(int th) {
		if(th != threshold) {
			this.threshold = th;
			thresholdUpdated(threshold);
		}
	}

	public void setColor(Color3f color) {
		boolean colorChanged = !(this.color == null && color == null)
			|| (this.color == null && color != null)
			|| (color == null && this.color != null) 
			|| !(this.color.equals(color));
		if(!colorChanged)
			return;
		Color3f oldColor = this.color;
		this.color = color;
		colorUpdated(oldColor, color);
	}

	public void setTransparency(float transparency) {
		transparency = transparency < 0 ? 0 : transparency;
		transparency = transparency > 1 ? 1 : transparency;
		if(Math.abs(transparency - this.transparency) < 0.01)
			return;
		this.transparency = transparency;
		transparencyUpdated(transparency);
	}

	public String getName() {
		return name;
	}

	public ImagePlus getImage() {
		return image;
	}

	public boolean[] getChannels() {
		return channels;
	}

	public Color3f getColor() {
		return color;
	}

	public int getThreshold() {
		return threshold;
	}

	public float getTransparency() {
		return transparency;
	}

	public int getResamplingFactor() {
		return resamplingF;
	}

	public TransformGroup getLocalRotate() {
		return localRotate;
	}

	public TransformGroup getLocalTranslate() {
		return localTranslate;
	}

	public boolean isLocked() {
		return locked;
	}

	public boolean isVisible() {
		return visible;
	}

	public boolean hasCoord() {
		return bbSwitch.getChildMask().get(CS);
	}

	public abstract void eyePtChanged(View view);
	public abstract void calculateMinMaxCenterPoint();
	public abstract float getVolume();
	public abstract void colorUpdated(Color3f oldColor, Color3f newColor);
	public abstract void channelsUpdated(boolean[] channels);
	public abstract void transparencyUpdated(float transparency);
	public abstract void thresholdUpdated(int t);
	public abstract void flush();
}



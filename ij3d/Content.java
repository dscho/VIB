package ij3d;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.io.FileInfo;
import ij.io.OpenDialog;

import vib.BenesNamedPoint;

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
import javax.vecmath.Point3d;

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
	private boolean coordVisible = true;
	protected boolean selected = false;
	private boolean showPL = true;

	private Switch bbSwitch;
	private BitSet whichChild = new BitSet(2);
	
	protected Point3f centerPoint, minPoint, maxPoint;
	private PointListShape pointlist = null;
	
	protected TransformGroup localRotate;
	protected TransformGroup localTranslate;

	public static final int BB = 0;
	public static final int CS = 1;
	public static final int PL = 2;
	public static final int CO = 3;

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
		whichChild.set(BB, selected);
		whichChild.set(CS, coordVisible);
		whichChild.set(CO, visible);
		whichChild.set(PL, showPL);
		bbSwitch.setChildMask(whichChild);
	}
	
	public void createBoundingBox() {
		while(bbSwitch.numChildren() > 0)
			bbSwitch.removeChild(0);
			
		BoundingBox b = new BoundingBox(minPoint, maxPoint);
		b.setPickable(false);
		bbSwitch.addChild(b);
		float cl = (float)Math.abs(maxPoint.x - minPoint.x) / 5f;
		CoordinateSystem cs = new CoordinateSystem(cl, new Color3f(0, 1, 0));
		cs.setPickable(false);
		bbSwitch.addChild(cs);
		pointlist = new PointListShape();
		pointlist.setPickable(false);
		bbSwitch.addChild(pointlist);
		// initially show the bounding box, but not the coordinate system
		whichChild.set(BB, selected);
		whichChild.set(CS, coordVisible);
		whichChild.set(CO, visible);
		whichChild.set(PL, showPL);
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

	public void showPointList(boolean b) {
		if(pointlist != null) {
			whichChild.set(PL, b);
			showPL = b;
		}
		bbSwitch.setChildMask(whichChild);
	}

	public void loadPointList() {
		pointlist.setColor(color);
		pointlist.load(image);
	}

	public void savePointList() {
		String dir = OpenDialog.getDefaultDirectory();
		String name = this.name;
		if(image != null) {
			FileInfo fi = image.getFileInfo();
			dir = fi.directory;
			name = fi.fileName;
		}
		name += ".points";
		pointlist.save(dir, name);
	}

	public void addPointListPoint(Point3d p) {
		String name = IJ.getString(
				"Name for point", "point" + pointlist.size());
		if(!name.equals(""))
			pointlist.addPoint(name, p.x, p.y, p.z);
	}
	
	public void setListPointPos(int i, Point3d pos) {
		pointlist.setPos(i, pos);
	}

	public BenesNamedPoint getPointListPointAt(Point3d p) {
		return pointlist.getPoint(p);
	}

	public int getPointListPointIndexAt(Point3d p) {
		return pointlist.getIndex(p);
	}

	public void deletePointListPoint(int i) {
		pointlist.delete(i);
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
 		pointlist.setColor(color);
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
		return coordVisible;
	}

	public boolean isPLVisible() {
		return showPL;
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



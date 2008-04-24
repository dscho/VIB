package ij3d;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.io.FileInfo;
import ij.io.OpenDialog;

import vib.PointList;
import vib.BenesNamedPoint;
import isosurface.IsoShape;
import isosurface.MeshGroup;
import voltex.VoltexGroup;
import orthoslice.OrthoGroup;

import java.awt.image.IndexColorModel;
import java.util.BitSet;
import java.util.List;

import com.sun.j3d.utils.behaviors.mouse.MouseRotate;
import com.sun.j3d.utils.behaviors.mouse.MouseBehaviorCallback;

import javax.media.j3d.*;
import javax.vecmath.Color3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Point3f;
import javax.vecmath.Point3d;

public class Content extends BranchGroup {

	// attributes
	protected String name;
	protected Color3f color = null;
	protected ImagePlus image;
	protected boolean[] channels = new boolean[] {true, true, true};
	protected float transparency = 0f;
	protected int resamplingF = 1;
	protected int threshold = 0;
	protected int type = VOLUME;

	// visibility flags
	private boolean locked = false;
	private boolean visible = true;
	private boolean coordVisible = true;
	protected boolean selected = false;
	private boolean showPL = true;

	// entries
	private ContentNode contentNode = null;
	private BoundingBox bb = null;
	private CoordinateSystem cs = null; 
	private PointListShape pointlist = null;

	// scene graph entries
	private Switch bbSwitch;
	private BitSet whichChild = new BitSet(2);

	protected TransformGroup localRotate;
	protected TransformGroup localTranslate;
	

	// global constants
	public static final int CO = 0;
	public static final int BB = 1;
	public static final int CS = 2;
	public static final int PL = 3;

	public static final int VOLUME = 0;
	public static final int ORTHO = 1;
	public static final int SURFACE = 2;
	
	public Content(String name) {
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
		bbSwitch.setCapability(Switch.ALLOW_CHILDREN_WRITE);
		bbSwitch.setCapability(Switch.ALLOW_CHILDREN_EXTEND);
		localRotate.addChild(bbSwitch);
	}

	public void displayAs(int type) {
		if(image == null)
			return;
		// remove everything if possible
		bbSwitch.removeAllChildren();

		// create content node and add it to the switch
		switch(type) {
			case VOLUME: contentNode = new VoltexGroup(this); break;
			case ORTHO: contentNode = new OrthoGroup(this); break;
			case SURFACE: contentNode = new MeshGroup(this); break;
		}
		bbSwitch.addChild(contentNode);

		// create the bounding box and add it to the switch
		BoundingBox b = new BoundingBox(
				contentNode.min, contentNode.max);
		b.setPickable(false);
		bbSwitch.addChild(b);

		// create coordinate system and add it to the switch
		float cl = (float)Math.abs(contentNode.max.x 
					- contentNode.min.x) / 5f;
		CoordinateSystem cs = new CoordinateSystem(
						cl, new Color3f(0, 1, 0));
		cs.setPickable(false);
		bbSwitch.addChild(cs);

		// create point list and add it to the switch
		pointlist = new PointListShape();
		pointlist.setPickable(false);
		bbSwitch.addChild(pointlist);


		// initialize child mask of the switch
		whichChild.set(BB, selected);
		whichChild.set(CS, coordVisible);
		whichChild.set(CO, visible);
		whichChild.set(PL, showPL);
		bbSwitch.setChildMask(whichChild);

		// update type
		this.type = type;
	}
	
	public void displayMesh(List mesh) {
		// remove everything if possible
		bbSwitch.removeAllChildren();

		// create content node and add it to the switch
		contentNode = new MeshGroup(this, mesh);
		bbSwitch.addChild(contentNode);

		// create the bounding box and add it to the switch
		BoundingBox b = new BoundingBox(
				contentNode.min, contentNode.max);
		b.setPickable(false);
		bbSwitch.addChild(b);

		// create coordinate system and add it to the switch
		float cl = (float)Math.abs(contentNode.max.x 
					- contentNode.min.x) / 5f;
		CoordinateSystem cs = new CoordinateSystem(
						cl, new Color3f(0, 1, 0));
		cs.setPickable(false);
		bbSwitch.addChild(cs);

		// create point list and add it to the switch
		pointlist = new PointListShape();
		pointlist.setPickable(false);
		bbSwitch.addChild(pointlist);


		// initialize child mask of the switch
		whichChild.set(BB, selected);
		whichChild.set(CS, coordVisible);
		whichChild.set(CO, visible);
		whichChild.set(PL, showPL);
		bbSwitch.setChildMask(whichChild);

		// update type
		this.type = SURFACE;
	}
	


	/* ************************************************************
	 * setters - visibility flags
	 *
	 * ***********************************************************/

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

	/* ************************************************************
	 * point list
	 * 
	 * ***********************************************************/

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

	public PointList getPointList() {
		return pointlist.getPointList();
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

	/* ************************************************************
	 * setters - transform
	 *
	 **************************************************************/
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
		Point3f c = contentNode.center;
		
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

	/* ************************************************************
	 * setters - attributes
	 *
	 * ***********************************************************/

	public void setChannels(boolean[] channels) {
		boolean channelsChanged = channels[0] != this.channels[0] || 
				channels[1] != this.channels[1] || 
				channels[2] != this.channels[2];
		if(!channelsChanged)
			return;
		this.channels = channels;
		contentNode.channelsUpdated();
	}

	public void setThreshold(int th) {
		if(th != threshold) {
			this.threshold = th;
			contentNode.thresholdUpdated();
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
		contentNode.colorUpdated();
	}

	public void setTransparency(float transparency) {
		transparency = transparency < 0 ? 0 : transparency;
		transparency = transparency > 1 ? 1 : transparency;
		if(Math.abs(transparency - this.transparency) < 0.01)
			return;
		this.transparency = transparency;
		contentNode.transparencyUpdated();
	}

	public void eyePtChanged(View view) {
		contentNode.eyePtChanged(view);
	}

	/* *************************************************************
	 * getters
	 *
	 **************************************************************/
	public String getName() {
		return name;
	}

	public int getType() {
		return type;
	}

	public ContentNode getContent() {
		return contentNode;
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

}



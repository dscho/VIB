package ij3d;

import ij3d.shapes.CoordinateSystem;
import ij3d.shapes.BoundingBox;
import ij3d.shapes.BoundingSphere;
import ij3d.pointlist.PointListShape;
import ij3d.pointlist.PointListDialog;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.FileInfo;
import ij.io.OpenDialog;

import vib.PointList;
import vib.BenesNamedPoint;
import isosurface.MeshGroup;
import voltex.VoltexGroup;
import orthoslice.OrthoGroup;
import surfaceplot.SurfacePlotGroup;

import java.util.BitSet;
import java.util.List;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.Switch;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.View;

import javax.vecmath.Color3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Point3f;
import javax.vecmath.Point3d;

public class Content extends BranchGroup implements UniverseListener {

	// attributes
	protected String name;
	protected Color3f color = null;
	protected ImagePlus image;
	protected boolean[] channels = new boolean[] {true, true, true};
	protected float transparency = 0f;
	protected int resamplingF = 1;
	protected int threshold = 0;
	protected boolean shaded = true;
	protected int type = VOLUME;

	// visibility flags
	private boolean locked = false;
	private boolean visible = true;
	private boolean coordVisible = UniverseSettings.showLocalCoordinateSystemsByDefault;
	private boolean showPL = false;
	private boolean boundingSphereVisible = false;
	protected boolean selected = false;

	// entries
	private ContentNode contentNode = null;
	private PointListShape pointlist = null;
	private BoundingSphere boundingSphere = null;

	// scene graph entries
	private Switch bbSwitch;
	private BitSet whichChild = new BitSet(5);

	protected TransformGroup localRotate;
	protected TransformGroup localTranslate;

	// reference to the point list dialog
	private PointListDialog plw;


	// global constants
	public static final int CO = 0;
	public static final int BB = 1;
	public static final int BS = 2;
	public static final int CS = 3;
	public static final int PL = 4;

	public static final int VOLUME = 0;
	public static final int ORTHO = 1;
	public static final int SURFACE = 2;
	public static final int SURFACE_PLOT2D = 3;
	
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
			case SURFACE_PLOT2D: contentNode =
				new SurfacePlotGroup(this); break;
		}
		bbSwitch.addChild(contentNode);

		// create the bounding box and add it to the switch
		BoundingBox bb = new BoundingBox(
				contentNode.min, contentNode.max);
		bb.setPickable(false);
		bbSwitch.addChild(bb);
		boundingSphere = new BoundingSphere(contentNode.center,
				contentNode.center.distance(contentNode.min));
		boundingSphere.setPickable(false);
		bbSwitch.addChild(boundingSphere);

		// create coordinate system and add it to the switch
		float cl = (float)Math.abs(contentNode.max.x 
					- contentNode.min.x) / 5f;
		CoordinateSystem cs = new CoordinateSystem(
						cl, new Color3f(0, 1, 0));
		cs.setPickable(false);
		bbSwitch.addChild(cs);

		// create point list and add it to the switch
		// only create the point list when it does not exist already
		if(pointlist == null)
			pointlist = new PointListShape(name);
// 		pointlist.setPickable(false);
		bbSwitch.addChild(pointlist);


		// initialize child mask of the switch
		whichChild.set(BB, selected);
		whichChild.set(BS, boundingSphereVisible);
		whichChild.set(CS, coordVisible);
		whichChild.set(CO, visible);
		whichChild.set(PL, showPL);
		bbSwitch.setChildMask(whichChild);

		// update type
		this.type = type;
	}

	public static int getDefaultThreshold(ImagePlus imp, int type) {
		if(type != SURFACE)
			return 0;
		ImageStack stack = imp.getStack();
		int d = imp.getStackSize();
		// compute stack histogram
		int[] h = stack.getProcessor(1).getHistogram();
		for(int z = 1; z < d; z++) {
			int[] tmp = stack.getProcessor(z+1).getHistogram();
			for(int i = 0; i < h.length; i++)
				h[i] += tmp[i];

		}
		return imp.getProcessor().getAutoThreshold(h);
	}

	public static int getDefaultResamplingFactor(ImagePlus imp, int type) {
		int w = imp.getWidth(), h = imp.getHeight();
		int d = imp.getStackSize();
		int max = Math.max(w, Math.max(h, d));
		switch(type) {
			case SURFACE: return (int)Math.ceil(max / 128f);
			case VOLUME:  return (int)Math.ceil(max / 256f);
			case ORTHO:   return (int)Math.ceil(max / 256f);
			case SURFACE_PLOT2D: return (int)Math.ceil(max / 128f);
		}
		return 1;
	}

	public void displayMesh(List mesh) {
		displayMesh(mesh, MeshGroup.TRIANGLES);
	}

	public void displayMesh(List mesh, int mode) {
		displayMesh(mesh, mode, new LineAttributes());
	}

	public void displayMesh(List mesh, int mode, LineAttributes attrs) {
		// remove everything if possible
		bbSwitch.removeAllChildren();

		// create content node and add it to the switch
		contentNode = new MeshGroup(this, mesh, mode, attrs);
		bbSwitch.addChild(contentNode);

		// create the bounding box and add it to the switch
		BoundingBox bb = new BoundingBox(
				contentNode.min, contentNode.max);
		bb.setPickable(false);
		bbSwitch.addChild(bb);
		boundingSphere = new BoundingSphere(contentNode.center,
				contentNode.center.distance(contentNode.min));
		boundingSphere.setPickable(false);
		bbSwitch.addChild(boundingSphere);

		// create coordinate system and add it to the switch
		float cl = (float)Math.abs(contentNode.max.x
					- contentNode.min.x) / 5f;
		CoordinateSystem cs = new CoordinateSystem(
						cl, new Color3f(0, 1, 0));
		cs.setPickable(false);
		bbSwitch.addChild(cs);

		// create point list and add it to the switch
		pointlist = new PointListShape(name);
		pointlist.setPickable(false);
		bbSwitch.addChild(pointlist);


		// initialize child mask of the switch
		whichChild.set(BB, selected);
		whichChild.set(BS, boundingSphereVisible);
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
		whichChild.set(CS, b);
		// only if hiding, hide the point list
		if(!b) {
			showPointList(false);
			whichChild.set(BS, b);
		}
		bbSwitch.setChildMask(whichChild);
	}

	public void showBoundingSphere(boolean b) {
		whichChild.set(BS, b);
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

	public void setPointListDialog(PointListDialog p) {
		this.plw = p;
	}

	public void showPointList(boolean b) {
		if(pointlist == null)
			return;

		whichChild.set(PL, b);
		showPL = b;
		bbSwitch.setChildMask(whichChild);
		if(b && plw != null)
			plw.addPointList(name, pointlist.getPanel());
		else if(!b && plw != null)
			plw.removePointList(pointlist.getPanel());
	}

	public void loadPointList() {
		pointlist.setColor(color);
		pointlist.load(image);
	}

	public void savePointList() {
		String dir = OpenDialog.getDefaultDirectory();
		String n = this.name;
		if(image != null) {
			FileInfo fi = image.getFileInfo();
			dir = fi.directory;
			n = fi.fileName;
		}
		pointlist.save(dir, n);
	}

	public void addPointListPoint(Point3d p) {
		int size = pointlist.size();
		int point = 1;
		if(size != 0) {
			String lastp = pointlist.getPointList().
						get(size-1).getName();
			try {
				point = Integer.parseInt(lastp.substring(
					5, lastp.length())) + 1;
			} catch(Exception e) {
				point = size;
			}
		}
		String n = "point" + point;
		pointlist.addPoint(n, p.x, p.y, p.z);
		plw.update();
	}

	public void setListPointPos(int i, Point3d pos) {
		pointlist.setPos(i, pos);
	}

	public float getLandmarkPointSize() {
		return pointlist.getRadius();
	}

	public void setLandmarkPointSize(float r) {
		pointlist.setRadius(r);
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
		plw.update();
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

	public void setShaded(boolean b) {
		this.shaded = b;
		contentNode.shadeUpdated();
	}

	public boolean isShaded() {
		return shaded;
	}

	public void setColor(Color3f color) {
		boolean colorChanged = !(this.color == null && color == null)
			|| (this.color == null && color != null)
			|| (color == null && this.color != null)
			|| !(this.color.equals(color));
		if(!colorChanged)
			return;
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

	/* ************************************************************
	 * Universe Listener interface
	 *
	 *************************************************************/
	public void transformationStarted(View view) {}
	public void contentAdded(Content c) {}
	public void contentRemoved(Content c) {
		if(plw != null && this == c)
			plw.removePointList(pointlist.getPanel());
	}
	public void canvasResized() {}
	public void contentSelected(Content c) {}
	public void contentChanged(Content c) {}

	public void universeClosed() {
		if(plw != null)
			plw.removePointList(pointlist.getPanel());
	}

	public void transformationUpdated(View view) {
		eyePtChanged(view);
	}
	public void transformationFinished(View view) {
		eyePtChanged(view);
	}

	public void eyePtChanged(View view) {
		contentNode.eyePtChanged(view);
	}

	/* *************************************************************
	 * getters
	 *
	 **************************************************************/
	@Override
	public String getName() {
		return name;
	}

	public int getType() {
		return type;
	}

	public ContentNode getContent() {
		return contentNode;
	}

	public BoundingSphere getBoundingSphere() {
		return boundingSphere;
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


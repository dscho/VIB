package ij3d;

import ij.IJ;
import ij.measure.Calibration;
import ij.ImagePlus;
import vib.PointList;
import vib.BenesNamedPoint;
import com.sun.j3d.utils.geometry.ColorCube;
import com.sun.j3d.utils.universe.*;
import com.sun.j3d.utils.geometry.*;
import com.sun.j3d.utils.behaviors.mouse.*;
import javax.media.j3d.*;
import javax.vecmath.*;

public class PointListShape extends BranchGroup 
			implements PointList.PointListListener{

	private PointListPanel pld;
	private PointList points;
	private Color3f color = new Color3f(1, 1, 0);
	private Color3f highlightColor = new Color3f(0, 1, 0);
	private Appearance appearance;
	private float radius = 10;
	private String name = "";

	public PointListShape(String name) {
		this(name, new PointList());
	}
	
	public PointListShape(String name, ImagePlus image) {
		this(name, PointList.load(image));
	}

	public PointListShape(String name, PointList points) {
		this.name = name;
		setCapability(ALLOW_CHILDREN_EXTEND);
		setCapability(ALLOW_CHILDREN_WRITE);
		setCapability(ALLOW_DETACH);
		this.points = points;
		points.addPointListListener(this);
		pld = new PointListPanel(name, points);
		initAppearance(color);
		initGeom();
	}

	public PointListPanel getPanel() {
		return pld;
	}

	public void load(ImagePlus image) {
		while(points.size() > 0)
			points.remove(0);
		points = PointList.load(image);
		points.addPointListListener(this);
		pld = new PointListPanel(name, points);
		initGeom();
	}

	public int size() {
		return points.size();
	}

	public float getRadius() {
		return radius;
	}

	public void save(String dir, String name) {
		points.save(dir, name);
	}

	public int getIndex(BenesNamedPoint p) {
		return points.indexOf(p);
	}

	public int getIndex(Point3d p) {
		Point3d existing = new Point3d();
		float r_sq = radius * radius;
		for(int i = 0; i < points.size(); i++) {
			BenesNamedPoint bnp = (BenesNamedPoint)points.get(i);
			existing.x = bnp.x;
			existing.y = bnp.y;
			existing.z = bnp.z;
			if(p.distanceSquared(existing) < 2*r_sq)
				return i;
		}
		return -1;
	}

	public BenesNamedPoint getPoint(Point3d p) {
		int ind = getIndex(p);
		if(ind == -1)
			return null;
		return points.get(ind);
	}

	public PointList getPointList() {
		return points;
	}

	public void addPoint(String name, double x, double y, double z) {
		points.add(new BenesNamedPoint(name, x, y, z));
		// the listener gets informed about that and updates the
		// geometry automatically - see added() below
	}

	public void delete(int i) {
		BenesNamedPoint bnp = points.get(i);
		points.remove(bnp);
		// the listener gets informed about that and updates the
		// geometry automatically - see removed() below
	}

	public void delete(BenesNamedPoint bnp) {
		points.remove(bnp);
		// the listener gets informed about that and updates the
		// geometry automatically - see removed() below
	}

	public void setPos(int i, Point3d pos) {
		BenesNamedPoint bnp = points.get(i);
		points.placePoint(bnp, pos.x, pos.y, pos.z);
		// the listener gets informed about that and updates the
		// geometry automatically - see moved() below
	}

	public void setColor(Color3f c) {
		color = c == null ? new Color3f(1, 1, 0) : c;
		initAppearance(color);
		for(int i = 0; i < numChildren(); i++) {
			BranchGroup bg = (BranchGroup)getChild(i);
			TransformGroup tg = (TransformGroup)bg.getChild(0);
			Sphere s = (Sphere)tg.getChild(0);
			s.setAppearance(appearance);
		}
	}


	// listener interface
	public void added(BenesNamedPoint p) {
		Point3f p3f = new Point3f((float)p.x, (float)p.y, (float)p.z);
		addPointToGeometry(p3f, p.getName());
	}

	public void removed(BenesNamedPoint p) {
		deletePointFromGeometry(p.getName());
	}

	public void renamed(BenesNamedPoint p) {
		int i = points.indexOf(p);
		getChild(i).setName(points.get(i).getName());
	}

	public void moved(BenesNamedPoint p) {
		int i = points.indexOf(p);
		if(i >= 0 && i < points.size())
			updatePositionInGeometry(i, new Point3d(p.x, p.y, p.z));
	}

	public void reordered() {
		initGeom();
	}

	public void highlighted(final BenesNamedPoint p) {
		final int i = points.indexOf(p);
		BranchGroup bg = (BranchGroup)getChild(i);
		TransformGroup tg = (TransformGroup)bg.getChild(0);
		ScaleInterpolator si = (ScaleInterpolator)tg.getChild(1);
		final Alpha a = si.getAlpha();
		a.resume();
		try {
			Thread.currentThread().sleep(600);
		} catch(Exception e) { }
		a.pause();
	}

	// private methods responsible for updating the universe
	private Transform3D t3d = new Transform3D();
	private Vector3f v3f = new Vector3f();

	private void deletePointFromGeometry(String name) {
		for(int i = 0; i < numChildren(); i++) {
			BranchGroup bg = (BranchGroup)getChild(i);
			if(bg.getName().equals(name)) {
				bg.detach();
				return;
			}
		}
	}

	private void addPointToGeometry(Point3f p, String name) {
		v3f.x = p.x; v3f.y = p.y; v3f.z = p.z;

		BranchGroup bg = new BranchGroup();
		bg.setName(name);
		bg.setCapability(BranchGroup.ALLOW_DETACH);

		t3d.set(v3f);
		TransformGroup tg = new TransformGroup(t3d);
		tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		bg.addChild(tg);

		TransformGroup sig = new TransformGroup();
		sig.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		tg.addChild(sig);

		Alpha alpha = new Alpha();
		alpha.setStartTime(System.currentTimeMillis());
		alpha.setMode(Alpha.DECREASING_ENABLE|Alpha.INCREASING_ENABLE);
		alpha.setIncreasingAlphaDuration(300);
		alpha.setDecreasingAlphaDuration(300);
		alpha.pause();
		ScaleInterpolator si = new ScaleInterpolator(alpha, sig);
		si.setMaximumScale(5);
		si.setMinimumScale(0.5f);
		si.setSchedulingBounds(new BoundingSphere());
		tg.addChild(si);

		Sphere sphere = new Sphere(radius);
		sphere.getShape().setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
		sphere.setCapability(Sphere.ENABLE_APPEARANCE_MODIFY);
		sphere.setAppearance(appearance);
		sig.addChild(sphere);

		addChild(bg);
	}

	private void updatePositionInGeometry(int i, Point3d pos) {
		BranchGroup bg = (BranchGroup)getChild(i);
		TransformGroup tg = (TransformGroup)bg.getChild(0);
		v3f.x = (float)pos.x;
		v3f.y = (float)pos.y;
		v3f.z = (float)pos.z;
		t3d.set(v3f);
		tg.setTransform(t3d);
	}

	private void initGeom() {
		removeAllChildren();
		for(int i = 0; i < points.size(); i++) {
			BenesNamedPoint po = points.get(i);
			Point3f p3f = new Point3f(
				(float)po.x,(float)po.y,(float)po.z);
			addPointToGeometry(p3f, po.getName());
		}
	}

	private void initAppearance(Color3f color) {
		appearance = new Appearance();
		ColoringAttributes colorAttrib = new ColoringAttributes();
		colorAttrib.setShadeModel(ColoringAttributes.SHADE_GOURAUD);
		colorAttrib.setColor(color);
		appearance.setColoringAttributes(colorAttrib);

		Material material = new Material();
 		material.setDiffuseColor(color);
		appearance.setMaterial(material);

	}

	public String toString() {
		return points.toString();
	}
} 


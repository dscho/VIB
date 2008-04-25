package ij3d;

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

	private PointListDialog pld;
	private PointList points;
	private Color3f color = new Color3f(1, 1, 0);
	private Color3f highlightColor = new Color3f(0, 1, 0);
	private Appearance appearance;
	private float radius = 10;

	public PointListShape() {
		setCapability(ALLOW_CHILDREN_EXTEND);
		setCapability(ALLOW_CHILDREN_WRITE);
		setCapability(ALLOW_DETACH);
		points = new PointList();
		points.addPointListListener(this);
		pld = new PointListDialog(points);
		initGeom();
		initAppearance(color);
	}
	
	public PointListShape(PointList points) {
		setCapability(ALLOW_CHILDREN_EXTEND);
		setCapability(ALLOW_CHILDREN_WRITE);
		setCapability(ALLOW_DETACH);
		this.points = points;
		points.addPointListListener(this);
		pld = new PointListDialog(points);
		initAppearance(color);
	}

	public PointListShape(ImagePlus image) {
		setCapability(ALLOW_CHILDREN_EXTEND);
		setCapability(ALLOW_CHILDREN_WRITE);
		setCapability(ALLOW_DETACH);
		points = PointList.load(image);
		points.addPointListListener(this);
		pld = new PointListDialog(points);
		initGeom();
		initAppearance(color);
	}

	public PointListShape(String dir, String file, boolean showDialog) {
		setCapability(ALLOW_CHILDREN_EXTEND);
		setCapability(ALLOW_CHILDREN_WRITE);
		setCapability(ALLOW_DETACH);
		points = PointList.load(dir, file, showDialog);
		points.addPointListListener(this);
		pld = new PointListDialog(points);
		initGeom();
		initAppearance(color);
	}

	public void showDialog(boolean b) {
		pld.setVisible(b);
	}

	public void load(ImagePlus image) {
		while(points.size() > 0)
			points.remove(0);
		points = PointList.load(image);
		points.addPointListListener(this);
		pld = new PointListDialog(points);
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
		for(int i = 0; i < points.size(); i++) {
			if(points.get(i).getName().equals(p.getName()))
				return i;
		}
		return -1;
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
	public void added(int i) {
		BenesNamedPoint p = points.get(i);
		Point3f p3f = new Point3f((float)p.x, (float)p.y, (float)p.z);
		addPointToGeometry(p3f, p.getName());
	}

	public void removed(int i) {
		if(i >= 0)
			deletePointFromGeometry(i);
	}

	public void renamed(int i) {
		// do nothing
	}

	public void moved(int i) {
		BenesNamedPoint p = points.get(i);
		if(i >= 0 && i < points.size())
			updatePositionInGeometry(i, new Point3d(p.x, p.y, p.z));
	}

	public void highlighted(final int i) {
		new Thread(new Runnable() {
			public void run() {
				BranchGroup bg = (BranchGroup)getChild(i);
				TransformGroup tg = (TransformGroup)bg.getChild(0);
				Sphere s = (Sphere)tg.getChild(0);
				initAppearance(highlightColor);
				s.setAppearance(appearance);
				try {
					Thread.currentThread().sleep(2000);
				} catch(Exception e) {}
				initAppearance(color);
				s.setAppearance(appearance);
			}
		}).start();
	}

	// private methods responsible for updating the universe
	private Transform3D t3d = new Transform3D();
	private Vector3f v3f = new Vector3f();

	private void deletePointFromGeometry(int i) {
		((BranchGroup)getChild(i)).detach();
	}

	private void addPointToGeometry(Point3f p, String name) {
		v3f.x = p.x; v3f.y = p.y; v3f.z = p.z;
		t3d.set(v3f);
		TransformGroup tg = new TransformGroup(t3d);
		tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

		Sphere sphere = new Sphere(radius);
		sphere.getShape().setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
		sphere.setCapability(Sphere.ENABLE_APPEARANCE_MODIFY);
		sphere.setAppearance(appearance);
		tg.addChild(sphere);

		BranchGroup bg = new BranchGroup();
		bg.setName(name);
		bg.setCapability(BranchGroup.ALLOW_DETACH);
		bg.addChild(tg);
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


package vib;

import Quick3dApplet.*;
import ij.plugin.*;
import ij.*;
import ij.gui.*;
import ij.process.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

public class Image3dCanvas extends ImageCanvas implements MouseWheelListener {

	int width = 400, height = 300;
	public Pixstore ps;
	public Render r;
	public Vector objects;
	public Map objectTitles;

	public Image3dCanvas(ImagePlus imp) {
		super(imp);
		if (!(imp.getProcessor() instanceof ColorProcessor))
			throw new RuntimeException("Need a color image");
		width = imp.getWidth();
		height = imp.getHeight();
		ps = new Pixstore(width, height);
		imp.getProcessor().setPixels(ps.pix);

		r = new Render();
		r.setViewOffset(new Vec(0, 0, -8f));
		r.setAntiAlias( false );

		objects = new Vector();
		objectTitles = new HashMap();

		addMouseWheelListener(this);
	}

	public Image3dCanvas(String title, int width, int height) {
		this(new ImagePlus(title, new ColorProcessor(width, height)));
		new ImageWindow(imp, this);
	}

	private Vec center;

	public Vec getCenter() {
		Vec result = new Vec();
		long count = 0;
		Iterator iter = objects.iterator();
		while (iter.hasNext()) {
			RenderObject ro = (RenderObject)iter.next();
			result.addMe(result, ro.getCentre());
			count++;
		}
		if (count > 0)
			result.mulMe(result, 1.0f / count);
		return result;
	}

	public void rotate(Matrix rot) {
		if (center == null)
			center = getCenter();
		Vec viewPoint = r.getViewOffset();
		Vec toCenter = Vec.sub(center, viewPoint);
		float distance = toCenter.magnitude();

		Matrix current = r.getViewRot();
		Vec viewDir = Matrix.mul(current, new Vec(0, 0, 1));
		Vec center = Vec.add(viewPoint, Vec.mul(viewDir, distance));
		current = Matrix.mul(Matrix.inverseOf(rot), current);
		r.setViewRot(current);
		viewDir = Matrix.mul(current, new Vec(0, 0, 1));
		center.addMe(center, Vec.mul(viewDir, -distance));
		r.setViewOffset(center);
	}

	public void translate(Vec v) {
		v = Matrix.mul(r.getViewRot(), v);
		r.setViewOffset(Vec.add(r.getViewOffset(), v));
	}

	public Vec getBestViewOffset() {
		Vec center = null;
		float radius = 0;
		Iterator iter = objects.iterator();
		while (iter.hasNext()) {
			RenderObject ro = (RenderObject)iter.next();
			Vec center2 = ro.getCentre();
			float radius2 = ro.getRadious();

			if (center == null) {
				center = center2;
				radius = radius2;
			} else {
				Vec diff = Vec.sub(center2, center);
				float distance = diff.magnitude();
				if (distance + radius2 < radius)
					continue;
				if (distance == 0) {
					if (radius < radius2)
						radius = radius2;
					continue;
				}
				radius2 = (radius + distance + radius2) / 2;
				float factor = (radius2 - radius) / distance;
				center.addMe(center, Vec.mul(diff, factor));
				radius = radius2;
			}
		}
		if (center == null)
			return new Vec();
		Vec viewDir = Matrix.mul(r.getViewRot(), new Vec(0, 0, 1));
		radius *= r.getZdist() * 2;
		return Vec.sub(center, Vec.mul(viewDir, radius));
	}

	public void fitToWindow() {
		r.setViewOffset(getBestViewOffset());
	}

	public synchronized void repaint() {
		r.draw(objects, ps);
		super.repaint();
	}

	public synchronized void paint(Graphics g) {
		setImageUpdated();

		super.paint(g);
	}

	public void mouseWheelMoved(MouseWheelEvent e) {
		translate(new Vec(0, 0, e.getWheelRotation() * 25));
		repaint();
	}

	int dragX, dragY, button;

	public void mousePressed(MouseEvent e) {
		int screenX = e.getX(), screenY = e.getY();
		dragX = offScreenX(screenX);
		dragY = offScreenY(screenY);

		button = e.getButton();
		if (button == 1) {
			if (IJ.altKeyDown())
				button = 2;
			else if (IJ.shiftKeyDown())
				button = 3;
		}
	}

	public void mouseDragged(MouseEvent e) {
		int screenX = e.getX(), screenY = e.getY();
		int nx = offScreenX(screenX), ny = offScreenY(screenY);

		float rx = (float)(nx * 2 - width) / width;
		float ry = (float)(ny * 2 - height) / height;
		nx -= dragX;
		ny -= dragY;
		if (nx != 0 || ny != 0) {
			dragX += nx;
			dragY += ny;
			if (button == e.BUTTON1) {
				float zr = nx * ry - ny * rx;
				Matrix delta = new Matrix();
				delta.setRotationXyzProgressive(
						-ny * (1 - Math.abs(rx)) * .03f,
						nx * (1 - Math.abs(ry)) * .03f,
						zr * .03f);
				rotate(delta);
			} else if (button == e.BUTTON2)
				translate(new Vec(0, 0, nx + ny));
			else if (button == e.BUTTON3)
				translate(new Vec(-nx, -ny, 0));

			repaint();
		}
	}

	public void mouseMoved(MouseEvent e) {
		int sx = e.getX();
		int sy = e.getY();
		int ox = offScreenX(sx);
		int oy = offScreenY(sy);
		Render.PointedAt p = r.getObjectPointedAt(objects, ox, oy);
		String status = "";
		if (p != null) {
			final DecimalFormat format =
				new DecimalFormat("#,###.##");
			String title = (String)objectTitles.get(p.ro);
			if (title != null)
				status = title + ": ";
			status += "(" + format.format(p.pos.x) + ", "
				+ format.format(p.pos.y) + ", "
				+ format.format(p.pos.z) + ")";
		}
		IJ.showStatus(status);
	}
}


package ij3d;

import vib.BenesNamedPoint;

import com.sun.j3d.utils.pickfast.PickCanvas;
import javax.media.j3d.PickInfo;

import java.awt.event.*;
import java.awt.*;

import com.sun.j3d.utils.behaviors.mouse.MouseBehaviorCallback;
import javax.media.j3d.*;
import javax.vecmath.*;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Toolbar;
import ij.measure.Calibration;
import java.util.Enumeration;

import voltex.Renderer;
import orthoslice.OrthoGroup;

public class MouseBehavior extends Behavior {

	private DefaultUniverse univ;
	private ImageCanvas3D ic3d;

	private WakeupOnAWTEvent[] mouseEvents;
	private WakeupCondition wakeupCriterion;

	private int toolID;
	private int x_last = 0, y_last = 0;
	private MouseBehaviorCallback callback;

	public static final int B1 = MouseEvent.BUTTON1_DOWN_MASK;
	public static final int B2 = MouseEvent.BUTTON2_DOWN_MASK;
	public static final int B3 = MouseEvent.BUTTON3_DOWN_MASK;

	public static final int SHIFT = InputEvent.SHIFT_DOWN_MASK;
	public static final int CTRL  = InputEvent.CTRL_DOWN_MASK;

	public static final int PICK_POINT_MASK = MouseEvent.BUTTON1_DOWN_MASK;
	public static final int DELETE_POINT_MASK = InputEvent.SHIFT_DOWN_MASK |
						MouseEvent.BUTTON1_DOWN_MASK;


	private Transform3D currentXform = new Transform3D();
	private Transform3D transformX = new Transform3D();
	private Transform3D transformY = new Transform3D();
	private Vector3f transl = new Vector3f();

	public MouseBehavior(DefaultUniverse univ) {
		this.univ = univ;
		this.ic3d = (ImageCanvas3D)univ.getCanvas();
		mouseEvents = new WakeupOnAWTEvent[5];
	}

	private boolean shouldRotate(int mask, int toolID) {
		int onmask = B2, onmask2 = B1;
		int offmask = SHIFT | CTRL;
		boolean b0 = (mask & (onmask | offmask)) == onmask;
		boolean b1 = (toolID == Toolbar.HAND
				&& (mask & (onmask2|offmask)) == onmask2);
		return b0 || b1;
	}

	private boolean shouldTranslate(int mask, int toolID) {
		int onmask = B2 | SHIFT, onmask2 = B1 | SHIFT;
		int offmask = CTRL;
		return (mask & (onmask | offmask)) == onmask ||
			(toolID == Toolbar.HAND
				&& (mask & (onmask2|offmask)) == onmask2);
	}

	private boolean shouldZoom(int mask, int toolID) {
		if(toolID != Toolbar.MAGNIFIER)
			return false;
		int onmask = B1;
		int offmask = SHIFT | CTRL;
		return (mask & (onmask | offmask)) == onmask;
	}

	private boolean shouldMovePoint(int mask, int toolID) {
		if(toolID != Toolbar.POINT)
			return false;
		int onmask = B1;
		int offmask = SHIFT | CTRL;
		return (mask & (onmask | offmask)) == onmask;
	}

	public void initialize() {
		mouseEvents[0]= new WakeupOnAWTEvent(MouseEvent.MOUSE_DRAGGED);
		mouseEvents[1]= new WakeupOnAWTEvent(MouseEvent.MOUSE_PRESSED);
		mouseEvents[2]= new WakeupOnAWTEvent(MouseEvent.MOUSE_RELEASED);
		mouseEvents[3]= new WakeupOnAWTEvent(MouseEvent.MOUSE_WHEEL);
		mouseEvents[4]= new WakeupOnAWTEvent(AWTEvent.KEY_EVENT_MASK);
		wakeupCriterion = new WakeupOr(mouseEvents);
		this.wakeupOn(wakeupCriterion);
	}

	public void setupCallback(MouseBehaviorCallback callback) {
		this.callback = callback;
	}

	private void transformChanged(int type, Transform3D t) {
		if(callback != null)
			callback.transformChanged(type, t);
	}

	public void processStimulus(Enumeration criteria) {
		toolID = Toolbar.getToolId();
		if(toolID != Toolbar.HAND && toolID != Toolbar.MAGNIFIER &&
				toolID != Toolbar.POINT) {
			wakeupOn (wakeupCriterion);
			return;
		}
		WakeupOnAWTEvent wakeup;
		AWTEvent[] events;
		AWTEvent evt;
		while(criteria.hasMoreElements()) {
			wakeup = (WakeupOnAWTEvent)criteria.nextElement();
			events = (AWTEvent[])wakeup.getAWTEvent();
			if(events.length > 0) {
				evt = events[events.length -1];
				if(evt instanceof MouseEvent)
					doProcess((MouseEvent)evt);
				if(evt instanceof KeyEvent)
					doProcess((KeyEvent)evt);
			}
		}
		wakeupOn(wakeupCriterion);
	}

	public void doProcess(KeyEvent e) {
		int id = e.getID();

		if(id == KeyEvent.KEY_RELEASED || id == KeyEvent.KEY_TYPED)
			return;

		Content c = univ.getSelected();
		int code = e.getKeyCode();
		int axis = -1;
		if(ic3d.isKeyDown(KeyEvent.VK_X))
			axis = Renderer.X_AXIS;
		else if(ic3d.isKeyDown(KeyEvent.VK_Y))
			axis = Renderer.Y_AXIS;
		else if(ic3d.isKeyDown(KeyEvent.VK_Z))
			axis = Renderer.Z_AXIS;
		// Consume events if used, to avoid other listeners from reusing the event
		boolean consumed = true;
		try {
		if(e.isShiftDown()) {
			switch(code) {
				case KeyEvent.VK_RIGHT:translate(c,5,0);return;
				case KeyEvent.VK_LEFT:translate(c,-5,0);return;
				case KeyEvent.VK_UP: translate(c,0,-5);return;
				case KeyEvent.VK_DOWN: translate(c,0,5);return;
			}
		} else if(e.isAltDown()) {
			switch(code) {
				case KeyEvent.VK_UP: zoom(c, 1); return;
				case KeyEvent.VK_DOWN: zoom(c, -1); return;
			}
		} else if(c != null && c.getType() == Content.ORTHO && axis != -1) {
			OrthoGroup og = (OrthoGroup)c.getContent();
			switch(code) {
				case KeyEvent.VK_RIGHT:
				case KeyEvent.VK_UP: og.increase(axis); return;
				case KeyEvent.VK_LEFT:
				case KeyEvent.VK_DOWN:og.decrease(axis); return;
				case KeyEvent.VK_SPACE: og.setVisible(axis,
					!og.isVisible(axis)); return;
			}
		} else {
			switch(code) {
				case KeyEvent.VK_RIGHT: rotate(c, 5, 0); return;
				case KeyEvent.VK_LEFT: rotate(c, -5, 0); return;
				case KeyEvent.VK_UP: rotate(c, 0, -5); return;
				case KeyEvent.VK_DOWN: rotate(c, 0, 5); return;
			}
		}
		// must be last line in try/catch block
		consumed = false;
		} finally {
			// executed when returning anywhere above,
			// since then consumed is not set to false
			if (consumed) e.consume();
		}
	}

	public void doProcess(MouseEvent e) {
		int id = e.getID();
		int mask = e.getModifiersEx();
		Content c = univ.getSelected();
		if(id == MouseEvent.MOUSE_PRESSED) {
			x_last = e.getX();
			y_last = e.getY();
			if(toolID == Toolbar.POINT) {
				if(c != null)
					c.showPointList(true);
				if(mask == PICK_POINT_MASK) {
					pickPoint(c, e);
				} else if(mask == DELETE_POINT_MASK) {
					deletePoint(c, e);
				}
				((ImageCanvas3D)univ.getCanvas()).killRoi();
			}
		} else if(id == MouseEvent.MOUSE_DRAGGED) {
			if(shouldTranslate(mask, toolID))
				translate(c, e);
			else if(shouldRotate(mask, toolID))
				rotate(c, e);
			else if(shouldZoom(mask, toolID))
				zoom(c, e);
			else if(shouldMovePoint(mask, toolID))
				movePoint(c, e);
		} else if(id == MouseEvent.MOUSE_RELEASED) {
			if(toolID == Toolbar.POINT) {
				movingIndex = -1;
			}
		}
		if(id == MouseEvent.MOUSE_WHEEL) {
			int axis = -1;
			if(ic3d.isKeyDown(KeyEvent.VK_X))
				axis = Renderer.X_AXIS;
			else if(ic3d.isKeyDown(KeyEvent.VK_Y))
				axis = Renderer.Y_AXIS;
			else if(ic3d.isKeyDown(KeyEvent.VK_Z))
				axis = Renderer.Z_AXIS;
			if(c != null && c.getType() == Content.ORTHO
								&& axis != -1) {
				OrthoGroup og = (OrthoGroup)c.getContent();
				MouseWheelEvent we = (MouseWheelEvent)e;
				int units = 0;
				if(we.getScrollType() ==
					MouseWheelEvent.WHEEL_UNIT_SCROLL)
					units = we.getUnitsToScroll();
				if(units > 0) og.increase(axis);
				else if(units < 0) og.decrease(axis);

			} else {
				wheel_zoom(c, e);
			}
		}
	}

	Transform3D translate = new Transform3D();
	Transform3D globalRotate = new Transform3D();
	Transform3D globalTranslate = new Transform3D();
	Transform3D globalRotInverse = new Transform3D();
	Transform3D globalTransInverse = new Transform3D();

	public void rotate(Content c, MouseEvent e) {
		int x = e.getX(), y = e.getY();
		int dx = x - x_last, dy = y - y_last;
		rotate(c, dx, dy);
		x_last = x;
		y_last = y;
	}

	public void rotate(Content c, int dx, int dy) {
		float x_angle = 0.03f * dy;
		float y_angle = 0.03f * dx;
		transformX.rotX(x_angle);
		transformY.rotY(y_angle);

		TransformGroup tg = (c == null || c.isLocked()) ?
				univ.getGlobalRotate() : c.getLocalRotate();
		Point3f center = (c==null || c.isLocked()) ?
				((Image3DUniverse)univ).getGlobalCenterPoint() :
				c.getContent().center;
		tg.getTransform(currentXform);

		univ.getGlobalRotate().getTransform(globalRotate);
		univ.getGlobalTranslate().getTransform(globalTranslate);

		globalRotInverse.invert(globalRotate);
		globalTransInverse.invert(globalTranslate);

		if(c != null && !c.isLocked()) {
			transl.x = -center.x;
			transl.y = -center.y;
			transl.z = -center.z;
			translate.set(transl);
			currentXform.mul(translate, currentXform);
			currentXform.mul(globalRotate, currentXform);
		}

		currentXform.mul(transformX, currentXform);
		currentXform.mul(transformY, currentXform);

		if(c != null && !c.isLocked()) {
			currentXform.mul(globalRotInverse, currentXform);
			transl.x = -transl.x;
			transl.y = -transl.y;
			transl.z = -transl.z;
			translate.set(transl);
			currentXform.mul(translate, currentXform);
		}

		tg.setTransform(currentXform);
		transformChanged(MouseBehaviorCallback.ROTATE, currentXform);
	}

	public void translate(Content c, MouseEvent e) {
		int x = e.getX(), y = e.getY();
		int dx = x - x_last, dy = y - y_last;
		translate(c, dx, dy);
		x_last = x;
		y_last = y;
	}

	public void translate(Content c, int dx, int dy) {
		transl.x = dx * 1f;
		transl.y = -dy * 1f;
		transl.z = 0;
		transformX.set(transl);

		TransformGroup tg = (c == null || c.isLocked()) ?
			univ.getGlobalTranslate() : c.getLocalTranslate();

		tg.getTransform(currentXform);

		univ.getGlobalRotate().getTransform(globalRotate);
		globalRotInverse.invert(globalRotate);

		currentXform.mul(globalRotate, currentXform);
		currentXform.mul(transformX, currentXform);
		currentXform.mul(globalRotInverse, currentXform);

		tg.setTransform(currentXform);
		transformChanged(MouseBehaviorCallback.TRANSLATE, currentXform);
	}

	public void wheel_zoom(Content c, MouseEvent e) {
		MouseWheelEvent we = (MouseWheelEvent)e;
		int units = 0;
		if(we.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL)
			units = we.getUnitsToScroll();
		wheel_zoom(c, units);
	}

	public void wheel_zoom(Content c, int units) {
		double factor = 0.9;
		if(units != 0) {

			transformX.setIdentity();

			double scale = units > 0 ? 1f/Math.abs(factor)
						: Math.abs(factor);

			transformX.setScale(scale);
			TransformGroup tg = univ.getGlobalScale();
			tg.getTransform(currentXform);
			currentXform.mul(transformX, currentXform);

			tg.setTransform(currentXform);
			transformChanged(
				MouseBehaviorCallback.TRANSLATE, currentXform);
		}
	}

	public void zoom(Content c, MouseEvent e) {
		int y = e.getY();
		int dy = y - y_last;
		zoom(c, dy);

		x_last = e.getX();
		y_last = y;
	}

	public void zoom(Content c, int dy) {
		double factor = 0.9f;
		double ddy = dy < 0 ? -1d : 1d;
		ddy *= factor;
		if(ddy != 0) {
			transformX.setIdentity();
			double scale = ddy > 0 ? 1f/Math.abs(ddy)
						: Math.abs(ddy);

			transformX.setScale(scale);
			TransformGroup tg = univ.getGlobalScale();
			tg.getTransform(currentXform);
			currentXform.mul(transformX, currentXform);

			tg.setTransform(currentXform);
			transformChanged(
				MouseBehaviorCallback.TRANSLATE, currentXform);
		}
	}

	public void zoom_old(Content c, MouseEvent e) {
		int y = e.getY();
		int dy = y - y_last;

		transl.x = 0f;
		transl.y = 0f;
		transl.z = 0.5f * dy;
		transformX.set(transl);

		//TransformGroup tg = univ.getGlobalRotate();
		TransformGroup tg = univ.getViewingPlatform().getViewPlatformTransform();
		tg.getTransform(currentXform);
		currentXform.mul(transformX, currentXform);

		tg.setTransform(currentXform);
		transformChanged(MouseBehaviorCallback.TRANSLATE, currentXform);

		x_last = e.getX();
		y_last = y;
	}

	public void deletePoint(Content c, MouseEvent e) {
		if(c == null) {
			IJ.error("Selection required");
			return;
		}
		Point3d p3d = getPickPoint(c, e);
		if(p3d == null)
			return;
		int ind = c.getPointListPointIndexAt(p3d);
		if(ind != -1) {
			c.deletePointListPoint(ind);
		}
	}

	int movingIndex = -1;
	public void movePoint(Content c, MouseEvent e) {
		if(c == null) {
			IJ.error("Selection required");
			return;
		}
		Point3d p3d = getPickPoint(c, e);
		if(p3d == null)
			return;
		if(movingIndex == -1)
			movingIndex = c.getPointListPointIndexAt(p3d);
		if(movingIndex != -1) {
			c.setListPointPos(movingIndex, p3d);
		}
	}

	public void pickPoint(Content c, MouseEvent e) {
		if(c == null) {
			IJ.error("Selection required");
			return;
		}
		Point3d p3d = getPickPoint(c, e);
		if(p3d == null)
			return;
		BenesNamedPoint bnp = c.getPointListPointAt(p3d);
		if(bnp == null) {
			c.addPointListPoint(p3d);
		}
	}

	private Point3d getPickPoint(Content c, MouseEvent e) {
		int x = e.getX(), y = e.getY();
		PickCanvas pickCanvas = new PickCanvas(
					univ.getCanvas(), univ.getScene());
		pickCanvas.setMode(PickInfo.PICK_GEOMETRY);
		pickCanvas.setFlags(PickInfo.CLOSEST_INTERSECTION_POINT);
		pickCanvas.setTolerance(3.0f);
		pickCanvas.setShapeLocation(x, y);
		try {
			PickInfo[] result = pickCanvas.pickAllSorted();
			if(result == null || result.length == 0)
				return null;

			for(int i = 0; i < result.length; i++) {
				Point3d intersection = result[i].getClosestIntersectionPoint();
				float v = getVolumePoint(c, intersection);
				if(v > 20)
					return intersection;
			}
			return null;
		} catch(Exception ex) {
			return null;
		}
	}

	private float getVolumePoint(Content c, Point3d p) {

		ImagePlus img = c.image;
		Calibration cal = img.getCalibration();
		double pw = cal.pixelWidth;
		double ph = cal.pixelHeight;
		double pd = cal.pixelDepth;
		int ix = (int)Math.round(p.x / pw);
		int iy = (int)Math.round(p.y / ph);
		int iz = (int)Math.round(p.z / pd);
		if(iz < 0 || iz >= img.getStackSize() ||
				iy < 0 || iy >= img.getHeight() ||
				ix < 0 || ix >= img.getWidth())
			return 0;
		else
			return img.getStack().getProcessor(iz + 1).getf(ix, iy);

	}
}


package ij3d;

import vib.BenesNamedPoint;

import com.sun.j3d.utils.pickfast.PickCanvas;
import javax.media.j3d.PickInfo;
import com.sun.j3d.utils.pickfast.PickIntersection;

import java.awt.event.*;
import java.awt.*;

import com.sun.j3d.utils.behaviors.mouse.MouseBehaviorCallback;
import javax.media.j3d.*;
import javax.vecmath.*;

import ij.IJ;
import ij.gui.Toolbar;
import java.util.Enumeration;
import java.util.Iterator;

public class MouseBehavior extends Behavior {

	private DefaultUniverse univ;

	private WakeupOnAWTEvent[] mouseEvents;
	private WakeupCondition wakeupCriterion;

	private int toolID;
	private int x_last = 0, y_last = 0;
	private MouseBehaviorCallback callback;

	public static final int ROTATE_MASK = MouseEvent.BUTTON1_DOWN_MASK;
	public static final int TRANSLATE_MASK = MouseEvent.BUTTON1_DOWN_MASK |
						InputEvent.SHIFT_DOWN_MASK;
	public static final int ZOOM_MASK = MouseEvent.BUTTON1_DOWN_MASK;
	public static final int PICK_POINT_MASK = MouseEvent.BUTTON1_DOWN_MASK;
	public static final int DELETE_POINT_MASK = InputEvent.SHIFT_DOWN_MASK |
						MouseEvent.BUTTON1_DOWN_MASK;

	private Transform3D currentXform = new Transform3D();
	private Transform3D transformX = new Transform3D(); 
	private Transform3D transformY = new Transform3D();
	private Vector3f transl = new Vector3f();

	public MouseBehavior(DefaultUniverse univ) {
		this.univ = univ;
		mouseEvents = new WakeupOnAWTEvent[5];
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
		if(id != KeyEvent.KEY_RELEASED)
			return;
		Content c = univ.getSelected();
		int code = e.getKeyCode();
		int mast = e.getModifiersEx();
		if(e.isShiftDown()) {
			switch(code) {
				case KeyEvent.VK_RIGHT:translate(c, 5, 0);break;
				case KeyEvent.VK_LEFT:translate(c, -5, 0);break;
				case KeyEvent.VK_UP: translate(c, 0, -5);break;
				case KeyEvent.VK_DOWN: translate(c, 0, 5);break;
			}
		} else if(e.isAltDown()) {
			switch(code) {
				case KeyEvent.VK_UP: zoom(c, 1); break;
				case KeyEvent.VK_DOWN: zoom(c, -1); break;
			}
		} else {
			switch(code) {
				case KeyEvent.VK_RIGHT: rotate(c, 5, 0); break;
				case KeyEvent.VK_LEFT: rotate(c, -5, 0); break;
				case KeyEvent.VK_UP: rotate(c, 0, -5); break;
				case KeyEvent.VK_DOWN: rotate(c, 0, 5); break;
			}
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
				if(mask == PICK_POINT_MASK) {
					pickPoint(c, e);
				} else if(mask == DELETE_POINT_MASK) {
					deletePoint(c, e);
				}
				((ImageCanvas3D)univ.getCanvas()).killRoi();
			}
		} else if(id == MouseEvent.MOUSE_DRAGGED) {
			if(toolID == Toolbar.MAGNIFIER && mask == ZOOM_MASK)
				zoom(c, e);
			else if(toolID == Toolbar.HAND) {
				switch(mask) {
					case ROTATE_MASK: rotate(c, e); 
					break;
					case TRANSLATE_MASK: translate(c, e); 
					break;
				}
			} else if(toolID == Toolbar.POINT) {
				if(mask == PICK_POINT_MASK)
					movePoint(c, e);
			}
		} else if(id == MouseEvent.MOUSE_RELEASED) {
			if(toolID == Toolbar.POINT) {
				movingIndex = -1;
			}
		}
		if(id == MouseEvent.MOUSE_WHEEL) {
			wheel_zoom(c, e);
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
				c.centerPoint;
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
		pickCanvas.setFlags(
			PickInfo.NODE | PickInfo.CLOSEST_INTERSECTION_POINT);
		pickCanvas.setTolerance(3.0f); 
		pickCanvas.setShapeLocation(x, y); 
		PickInfo result = null;
		try {
			result = pickCanvas.pickClosest();
			if(result == null) 
				return null;
			Point3d p3d = result.getClosestIntersectionPoint();
			return p3d;
		} catch(Exception ex) {
			return null;
		}
	}
}


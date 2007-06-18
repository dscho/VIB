package ij3d;

import java.awt.event.*;
import java.awt.*;

import com.sun.j3d.utils.behaviors.mouse.MouseBehaviorCallback;
import javax.media.j3d.*;
import javax.vecmath.*;

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

	private Transform3D currentXform = new Transform3D();
	private Transform3D transformX = new Transform3D(); 
	private Transform3D transformY = new Transform3D();
	private Vector3f transl = new Vector3f();

	public MouseBehavior(DefaultUniverse univ) {
		this.univ = univ;
		mouseEvents = new WakeupOnAWTEvent[4];
	}

	public void initialize() {
		mouseEvents[0]= new WakeupOnAWTEvent(MouseEvent.MOUSE_DRAGGED);
		mouseEvents[1]= new WakeupOnAWTEvent(MouseEvent.MOUSE_PRESSED);
		mouseEvents[2]= new WakeupOnAWTEvent(MouseEvent.MOUSE_RELEASED);
		mouseEvents[3]= new WakeupOnAWTEvent(MouseEvent.MOUSE_WHEEL);
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
		if(toolID != Toolbar.HAND && toolID != Toolbar.MAGNIFIER) {
			wakeupOn (wakeupCriterion);
			return;
		}
		WakeupOnAWTEvent wakeup;
		AWTEvent[] events;
		MouseEvent evt;
		while(criteria.hasMoreElements()) {
			wakeup = (WakeupOnAWTEvent)criteria.nextElement();
			events = wakeup.getAWTEvent();
			if(events.length > 0) {
				evt = (MouseEvent) events[events.length -1];
				doProcess(evt);
			}
		}
		wakeupOn(wakeupCriterion);
	}

	public void doProcess(MouseEvent e) {
		int id = e.getID();
		int mask = e.getModifiersEx();
		Content c = univ.getSelected();
		if(id == MouseEvent.MOUSE_PRESSED) {
			x_last = e.getX();
			y_last = e.getY();
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
		
		x_last = x;
		y_last = y;
	}

	public void translate(Content c, MouseEvent e) {
		int x = e.getX(), y = e.getY();
		int dx = x - x_last, dy = y - y_last;

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
		
		x_last = x;
		y_last = y;
	}

	public void wheel_zoom(Content c, MouseEvent e) {
		MouseWheelEvent we = (MouseWheelEvent)e;
		int units = 0;
		if(we.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL)
			units = we.getUnitsToScroll();
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
		double factor = 0.9f;
		double dy = (double)(y - y_last);
		dy = dy < 0 ? -1d : 1d;
		dy *= factor;
		if(dy != 0) {
			transformX.setIdentity();
			double scale = dy > 0 ? 1f/Math.abs(dy) : Math.abs(dy);

			transformX.setScale(scale);
			TransformGroup tg = univ.getGlobalScale();
			tg.getTransform(currentXform);
			currentXform.mul(transformX, currentXform);

			tg.setTransform(currentXform);
			transformChanged(
				MouseBehaviorCallback.TRANSLATE, currentXform);
		}	
		x_last = e.getX();
		y_last = y;
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
}
	


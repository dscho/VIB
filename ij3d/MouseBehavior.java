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

	private int x_last = 0, y_last = 0;
	private MouseBehaviorCallback callback;

	//public static final int ROTATE_BUTTON = MouseEvent.BUTTON1;
	//public static final int TRANSLATE_BUTTON = MouseEvent.BUTTON1;
	//public static final int ZOOM_BUTTON = MouseEvent.BUTTON1;

	public static final int ROTATE_MASK = MouseEvent.BUTTON1_DOWN_MASK;
	public static final int TRANSLATE_MASK = MouseEvent.BUTTON1_DOWN_MASK |
						InputEvent.SHIFT_DOWN_MASK;
	public static final int ZOOM_MASK = MouseEvent.BUTTON1_DOWN_MASK |
						InputEvent.ALT_DOWN_MASK;

	private static final Vector3f ORIGIN = new Vector3f(0f, 0f, 0f);

	private Transform3D globalRotation = new Transform3D();
	private Transform3D oldXform = new Transform3D();
	private Transform3D currentXform = new Transform3D();
	private Transform3D transformX = new Transform3D(); 
	private Transform3D transformY = new Transform3D();
	private Vector3f transl = new Vector3f();
	private boolean transformed = false;
	

	public MouseBehavior(DefaultUniverse univ) {
		this.univ = univ;
		mouseEvents = new WakeupOnAWTEvent[3];
	}

	public void initialize() {
		mouseEvents[0]= new WakeupOnAWTEvent(MouseEvent.MOUSE_DRAGGED);
		mouseEvents[1]= new WakeupOnAWTEvent(MouseEvent.MOUSE_PRESSED);
		mouseEvents[2]= new WakeupOnAWTEvent(MouseEvent.MOUSE_RELEASED);
		wakeupCriterion = new WakeupOr(mouseEvents);
		this.wakeupOn(wakeupCriterion);
	}

	public void setupCallback(MouseBehaviorCallback callback) {
		this.callback = callback;
	}

	private void transformChanged(int type, Transform3D t) {
		transformed = true;
		if(callback != null)
			callback.transformChanged(type, t);
	}

	public void processStimulus(Enumeration criteria) {
		if(Toolbar.getToolId() != Toolbar.HAND) {
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
		} else if(id == MouseEvent.MOUSE_RELEASED) {
			if(c != null || !transformed)
				return;
			for(Iterator it = univ.contents(); it.hasNext();) {
				univ.getGlobalRotate().
					getTransform(globalRotation);
				Content content = (Content)it.next();
				content.getTG().getTransform(currentXform);
				globalRotation.mul(currentXform);
				content.getTG().setTransform(globalRotation);
			}
			globalRotation.setIdentity();
			univ.getGlobalRotate().setTransform(globalRotation);
			transformed = false;
		} else if(id == MouseEvent.MOUSE_DRAGGED) {
			switch(mask) {
				case ROTATE_MASK: rotate(c, e); break;
				case TRANSLATE_MASK: translate(c, e); break;
				case ZOOM_MASK: zoom(c, e); break;
			}
		}
	}


	public void rotate(Content c, MouseEvent e) {
		int x = e.getX(), y = e.getY();
		int dx = x - x_last, dy = y - y_last;
		float x_angle = 0.03f * dy;
		float y_angle = 0.03f * dx;
		transformX.rotX(x_angle);
		transformY.rotY(y_angle);

		TransformGroup tg = (c == null) ? 
					univ.getGlobalRotate() : c.getTG();
		tg.getTransform(currentXform);
		tg.getTransform(oldXform);

		currentXform.setTranslation(ORIGIN);
		currentXform.mul(transformX, currentXform);
		currentXform.mul(transformY, currentXform);
		oldXform.get(transl);
		currentXform.setTranslation(transl);

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
		
		TransformGroup tg = (c == null) ? 
					univ.getGlobalRotate() : c.getTG();
		tg.getTransform(currentXform);
		currentXform.mul(transformX, currentXform);

		tg.setTransform(currentXform);
		transformChanged(MouseBehaviorCallback.TRANSLATE, currentXform);
		
		x_last = x;
		y_last = y;
	}

	public void zoom(Content c, MouseEvent e) {
		int y = e.getY();
		int dy = y - y_last;

		transl.x = 0f;
		transl.y = 0f;
		transl.z = 10f * dy;
		transformX.set(transl);
		
		TransformGroup tg = univ.getGlobalRotate();
		tg.getTransform(currentXform);
		currentXform.mul(transformX, currentXform);

		tg.setTransform(currentXform);
		transformChanged(MouseBehaviorCallback.TRANSLATE, currentXform);
		
		x_last = e.getX();
		y_last = y;
	}
}
	


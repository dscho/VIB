package ij3d;

import java.awt.event.*;
import java.awt.*;

import com.sun.j3d.utils.behaviors.mouse.MouseBehaviorCallback;
import javax.media.j3d.*;
import javax.vecmath.*;

import ij.gui.Toolbar;
import java.util.Enumeration;
import java.util.Iterator;

public class RotateBehavior extends Behavior {

	private DefaultUniverse univ;

	private WakeupOnAWTEvent[] mouseEvents;
	private WakeupCondition wakeupCriterion;

	private int x_last = 0, y_last = 0;
	private MouseBehaviorCallback callback;

	public static final int ROTATE_BUTTON = MouseEvent.BUTTON1;
	public static final int TRANSLATE_BUTTON = MouseEvent.BUTTON2;
	public static final int ZOOM_BUTTON = MouseEvent.BUTTON3;

	private static final Vector3f ORIGIN = new Vector3f(0f, 0f, 0f);

	private Transform3D globalRotation = new Transform3D();
	private Transform3D oldXform = new Transform3D();
	private Transform3D currentXform = new Transform3D();
	private Transform3D transformX = new Transform3D(); 
	private Transform3D transformY = new Transform3D();
	private Vector3f transl = new Vector3f();
	private boolean transformed = false;
	

	public RotateBehavior(DefaultUniverse univ) {
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
		int button = e.getButton();
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
			switch(button) {
				case ROTATE_BUTTON: rotate(c, e); break;
				case TRANSLATE_BUTTON: translate(e); break;
				case ZOOM_BUTTON: zoom(e); break;
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

	public void translate(MouseEvent e) {
		System.out.println("translate");
	}

	public void zoom(MouseEvent e) {
		System.out.println("zoom");
	}
}
	


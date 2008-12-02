package ij3d.behaviors;

import ij3d.*;

import java.awt.event.*;
import java.awt.*;

import javax.media.j3d.*;
import javax.vecmath.*;

import ij.gui.Toolbar;
import java.util.Enumeration;

import voltex.Renderer;
import orthoslice.OrthoGroup;

public class MouseBehavior extends Behavior {

	private DefaultUniverse univ;
	private ImageCanvas3D ic3d;

	private WakeupOnAWTEvent[] mouseEvents;
	private WakeupCondition wakeupCriterion;

	private int toolID;

	private int x_last, y_last;

	private Translator translator;
	private Rotator rotator;
	private Zoomer zoomer;
	private Picker picker;

	public static final int B1 = MouseEvent.BUTTON1_DOWN_MASK;
	public static final int B2 = MouseEvent.BUTTON2_DOWN_MASK;
	public static final int B3 = MouseEvent.BUTTON3_DOWN_MASK;

	public static final int SHIFT = InputEvent.SHIFT_DOWN_MASK;
	public static final int CTRL  = InputEvent.CTRL_DOWN_MASK;

	public static final int PICK_POINT_MASK = MouseEvent.BUTTON1_DOWN_MASK;
	public static final int DELETE_POINT_MASK = InputEvent.SHIFT_DOWN_MASK |
						MouseEvent.BUTTON1_DOWN_MASK;


	public MouseBehavior(DefaultUniverse univ) {
		this.univ = univ;
		this.ic3d = (ImageCanvas3D)univ.getCanvas();
		this.translator = univ.getTranslator();
		this.rotator = univ.getRotator();
		this.zoomer = univ.getZoomer();
		this.picker = univ.getPicker();
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
//				case KeyEvent.VK_RIGHT:translator.translate(c,5,0); return;
//				case KeyEvent.VK_LEFT: translator.translate(c,-5,0);return;
//				case KeyEvent.VK_UP:   translator.translate(c,0,-5);return;
//				case KeyEvent.VK_DOWN: translator.translate(c,0,5); return;
			}
		} else if(e.isAltDown()) {
			switch(code) {
				case KeyEvent.VK_UP: zoomer.zoom(c, 1); return;
				case KeyEvent.VK_DOWN: zoomer.zoom(c, -1); return;
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
//				case KeyEvent.VK_RIGHT: rotate(c, 5, 0); return;
//				case KeyEvent.VK_LEFT: rotate(c, -5, 0); return;
//				case KeyEvent.VK_UP: rotate(c, 0, -5); return;
//				case KeyEvent.VK_DOWN: rotate(c, 0, 5); return;
				case KeyEvent.VK_UP: zoomer.zoom_old(c, 5); return;
				case KeyEvent.VK_DOWN: zoomer.zoom_old(c, -5); return;

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
			translator.init(c, e.getX(), e.getY());
			x_last = e.getX();
			y_last = e.getY();
			if(toolID == Toolbar.POINT) {
				if(c != null)
					c.showPointList(true);
				if(mask == PICK_POINT_MASK) {
					picker.pickPoint(c, e);
				} else if(mask == DELETE_POINT_MASK) {
					picker.deletePoint(c, e);
				}
				((ImageCanvas3D)univ.getCanvas()).killRoi();
			}
		} else if(id == MouseEvent.MOUSE_DRAGGED) {
			if(shouldTranslate(mask, toolID))
				translator.translate(e);
			else if(shouldRotate(mask, toolID))
				rotator.rotate(e);
			else if(shouldZoom(mask, toolID))
				zoomer.zoom(c, e);
			else if(shouldMovePoint(mask, toolID))
				picker.movePoint(c, e);
		} else if(id == MouseEvent.MOUSE_RELEASED) {
			if(toolID == Toolbar.POINT) {
				picker.stopMoving();
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
				zoomer.wheel_zoom(c, e);
			}
		}
	}
}


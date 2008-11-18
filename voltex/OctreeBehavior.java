package voltex;

import ij3d.*;

import java.awt.event.*;
import java.awt.*;

import javax.media.j3d.*;
import javax.vecmath.*;

import ij.gui.Toolbar;
import java.util.Enumeration;

public class OctreeBehavior extends Behavior {

	private DefaultUniverse univ;

	private WakeupOnAWTEvent[] mouseEvents;
	private WakeupCondition wakeupCriterion;

	private int toolID;

	public OctreeBehavior(DefaultUniverse univ) {
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
					;//doProcess((MouseEvent)evt);
				if(evt instanceof KeyEvent)
					;//doProcess((KeyEvent)evt);
			}
		}
		wakeupOn(wakeupCriterion);
	}
}


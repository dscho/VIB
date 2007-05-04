package ij3d;

import com.sun.j3d.utils.behaviors.mouse.MouseRotate;
import javax.media.j3d.*;
import ij.gui.Toolbar;
import java.util.Enumeration;

public class RotateBehavior extends MouseRotate {

	public RotateBehavior(Content content) {
		super();
	}

	public void processStimulus(Enumeration criteria) {
		if(Toolbar.getToolId() == Toolbar.HAND) {
			super.processStimulus(criteria);
		} else 
			wakeupOn (getWakeupCondition());
	}
}
	


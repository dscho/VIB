package voltex;

import java.awt.*;
import java.awt.image.*;
import java.awt.color.ColorSpace;
import javax.media.j3d.*;
import javax.vecmath.*;
import java.io.*;
import java.text.NumberFormat;
import com.sun.j3d.utils.behaviors.mouse.*;

abstract public class Renderer {

    View	view;
    Volume 	volume;

    NumberFormat numFormatter = null;

    public Renderer(View vw, Volume vol) {
		view = vw;
		volume = vol;
    }

    /** 
     * Attach the branchgroups for this renderer to the display
     */
    abstract public void attach(Group dynamicGroup, Group staticGroup);

    /**
     * Called to make changes to the renderer state
     */
    abstract void update();

    /**
     * Called when the view position relative to the renderer changes
     */
    public void eyePtChanged() {}; 


    /** 
     * return the eye's position in <node>'s coordinate space
     */
    Point3d getViewPosInLocal(Node node) {

		Point3d viewPosition = new Point3d();
		Vector3d translate = new Vector3d();
		double angle = 0.0;
		double mag,sign;
		double tx,ty,tz;


		if (node == null ){
			System.out.println("called getViewPosInLocal() with null node");
			return null;
		}
		if (!node.isLive()) {
			System.out.println("called getViewPosInLocal() with non-live node");
			return null;
		}

		//  get viewplatforms's location in virutal world
		Canvas3D canvas = (Canvas3D)view.getCanvas3D(0);
		canvas.getCenterEyeInImagePlate(viewPosition);
		Transform3D t = new Transform3D();
		canvas.getImagePlateToVworld(t);
		t.transform(viewPosition);
		//System.out.println("vworld view position is " + viewPosition);

		// get parent transform
		Transform3D parentInv = new Transform3D();
		node.getLocalToVworld(parentInv);
		//System.out.println("node xform is \n" + parentInv);
		parentInv.invert();

		// transform the eye position into the parent's coordinate system
		parentInv.transform(viewPosition);

		return viewPosition;
    }

    // format a number to two digits past the decimal
    String numFormat(double value) {
		return numFormat(value, 2);
    }

    // format a number to numDigits past the decimal
    String numFormat(double value, int numDigits) {
		if (numFormatter == null) {
			numFormatter = NumberFormat.getInstance();
		}
		numFormatter.setMaximumFractionDigits(numDigits);
		return numFormatter.format(value);
    }
}

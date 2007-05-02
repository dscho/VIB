package ij3d;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.ImageStack;
import ij.IJ;

import ij3d.ImageWindow3D;
import ij3d.ImageCanvas3D;

import isosurface.Triangulator;
import isosurface.MeshGroup;

import voltex.VoltexGroup;

import marchingcubes.MCTriangulator;

import java.awt.Panel;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.*;
import java.awt.GraphicsConfiguration;

import java.util.List;
import java.util.Hashtable;
import java.util.Iterator;

import com.sun.j3d.utils.picking.behaviors.PickingCallback;
import com.sun.j3d.utils.behaviors.keyboard.*;
import com.sun.j3d.utils.applet.MainFrame; 
import com.sun.j3d.utils.universe.*;
import com.sun.j3d.utils.geometry.ColorCube;
import com.sun.j3d.utils.geometry.Sphere;
import javax.media.j3d.*;
import javax.media.j3d.Alpha;
import javax.media.j3d.RotationInterpolator;
import javax.vecmath.*;

import com.sun.j3d.utils.picking.PickCanvas;
import com.sun.j3d.utils.picking.PickResult;

public abstract class DefaultAnimatableUniverse extends DefaultUniverse {

	private Alpha animation;

	public DefaultAnimatableUniverse(int width, int height) {
		super(width, height);

		// Animation
		animation = new Alpha(-1, 4000);
		animation.pause();
		animation.setStartTime(
			java.util.Calendar.getInstance().getTimeInMillis());
		RotationInterpolator rotpol = 
			new RotationInterpolator(animation, rotationsTG) {
			public void processStimulus(java.util.Enumeration e) {
				super.processStimulus(e);
				if(animated)
					transformChanged(0, null);
			}
		};
		rotpol.setSchedulingBounds(bounds);
		rotationsTG.addChild(rotpol);

		root.compile();
		addBranchGraph(root);
	}

	private ImageStack stack;
	private boolean recording = false;
	private boolean animated = false;
	private float animationValue = 0;
	private boolean doRecord = false;
	private float oldValue = 0f;

	public void transformChanged(int type, TransformGroup tg) {
		super.transformChanged(type, tg);
		if(recording && animated) {
			boolean newLoop = animation.value() < oldValue;
			oldValue = animation.value();
			if(!doRecord && newLoop) {
				doRecord = true;
			} else if(doRecord && newLoop) {
				doRecord = false;
				oldValue = 0f;
				ImagePlus mov = stopRecording();
				if(mov != null) mov.show();
			}
		} else  {
			doRecord = recording;
		}
		
		// add actual image to recording stack
		if(!doRecord) 
			return;
		ImageWindow3D win = (ImageWindow3D)getCanvas().getParent();
		win.updateImagePlus();
		ImageProcessor ip = win.getImagePlus().getProcessor();
		int w = ip.getWidth(), h = ip.getHeight();
		if(stack == null) 
			stack = new ImageStack(w, h);
		stack.addSlice("", ip);
	}

	public void startRecording() {
		recording = true;
		if(animated) {
			animationValue = animation.value();
		}
	}

	public ImagePlus stopRecording() {
		recording = false;
		if(stack == null)
			return null;
		ImagePlus imp = new ImagePlus("Movie", stack);
		stack = null;
		return imp;
	}

	public void startAnimation() {
		animation.resume();
		animated = true;
		fireTransformationStarted();
	}

	public void pauseAnimation() {
		animation.pause();
		animated = false;
		fireTransformationFinished();
	}
} 

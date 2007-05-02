package ij3d;

import ij.gui.Toolbar;
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

import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import com.sun.j3d.utils.picking.behaviors.PickingCallback;
import com.sun.j3d.utils.picking.behaviors.PickRotateBehavior;
import com.sun.j3d.utils.picking.behaviors.PickTranslateBehavior;
import com.sun.j3d.utils.picking.PickCanvas;
import com.sun.j3d.utils.picking.PickResult;

import com.sun.j3d.utils.behaviors.keyboard.*;
import com.sun.j3d.utils.behaviors.mouse.*;
import com.sun.j3d.utils.universe.*;
import com.sun.j3d.utils.geometry.ColorCube;
import com.sun.j3d.utils.geometry.Sphere;

import javax.media.j3d.*;
import javax.vecmath.*;

public abstract class DefaultUniverse extends SimpleUniverse implements 
						PickingCallback {

	protected BranchGroup root;
	protected BranchGroup scene;
	protected TransformGroup rotationsTG;
	protected TransformGroup scaleTG;
	protected BoundingSphere bounds;
	protected ImageWindow3D win;

	private List listeners = new ArrayList();

	public DefaultUniverse(int width, int height) {
		super(new ImageCanvas3D(width, height));
		getViewingPlatform().setNominalViewingTransform();

		root = new BranchGroup();
		
		scaleTG = new TransformGroup();
		scaleTG.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		scaleTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		root.addChild(scaleTG);

		rotationsTG = new TransformGroup();
		rotationsTG.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		rotationsTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		scaleTG.addChild(rotationsTG);

		scene = new BranchGroup();
		scene.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		scene.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
		scene.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		rotationsTG.addChild(scene);

		bounds = new BoundingSphere();
		bounds.setRadius(10.0);

		// Lightening
		AmbientLight lightA = new AmbientLight();
		lightA.setInfluencingBounds(bounds);
		lightA.setEnable(false);
		root.addChild(lightA);
		DirectionalLight lightD1 = new DirectionalLight();
		lightD1.setInfluencingBounds(bounds);
		root.addChild(lightD1);

		SpotLight lightS = new SpotLight();
		lightS.setInfluencingBounds(bounds);
		root.addChild(lightS);

		// Mouse behaviors
		RotateBehavior rotate = new RotateBehavior();
		rotate.setupCallback(this);
		scene.addChild(rotate);

		TranslateBehavior translate = new TranslateBehavior();
		translate.setupCallback(this);
		scene.addChild(translate);

		ZoomBehavior zoom = new ZoomBehavior();
		scene.addChild(zoom);

	}

	public void transformChanged(int type, TransformGroup tg) {
		fireTransformationUpdated();
	}

	public void show() {
		win = new ImageWindow3D("ImageJ 3D Viewer", this);
	}

	public void addUniverseListener(UniverseListener l) {
		listeners.add(l);
	}

	public void removeUniverseListener(UniverseListener l) {
		listeners.remove(l);
	}

	protected void fireTransformationUpdated() {
		for(int i = 0; i < listeners.size(); i++) {
			UniverseListener l = (UniverseListener)listeners.get(i);
			l.transformationUpdated();
		}
	}

	protected void fireTransformationFinished() {
		for(int i = 0; i < listeners.size(); i++) {
			UniverseListener l = (UniverseListener)listeners.get(i);
			l.transformationFinished();
		}
	}

	protected void fireContentAdded(Content c) {
		for(int i = 0; i < listeners.size(); i++) {
			UniverseListener l = (UniverseListener)listeners.get(i);
			l.contentAdded(c);
		}
	}

	protected void fireContentChanged(Content c) {
		for(int i = 0; i < listeners.size(); i++) {
			UniverseListener l = (UniverseListener)listeners.get(i);
			l.contentChanged(c);
		}
	}

	protected void fireContentRemoved(Content c) {
		for(int i = 0; i < listeners.size(); i++) {
			UniverseListener l = (UniverseListener)listeners.get(i);
			l.contentRemoved(c);;
		}
	}

	private class RotateBehavior extends PickRotateBehavior {
		int mode = PickCanvas.GEOMETRY;
		
		public RotateBehavior() {
			super(scene, getCanvas(), bounds);
			setMode(mode);
		}

		public void processStimulus(Enumeration criteria) {
			if(Toolbar.getToolId() == Toolbar.HAND) {
				super.processStimulus(criteria);
			} else 
				wakeupOn (wakeupCondition);
		}
	}
		
	private class TranslateBehavior extends PickTranslateBehavior {
		int mode = PickCanvas.GEOMETRY;
		public TranslateBehavior() {
			super(scene, getCanvas(), bounds);
			setMode(mode);
		}

		public void processStimulus(Enumeration criteria) {
			if(Toolbar.getToolId() == Toolbar.HAND) {
				super.processStimulus(criteria);
			} else 
				wakeupOn (wakeupCondition);
		}
	}

	private class ZoomBehavior extends MouseZoom {
		public ZoomBehavior() {
			super(scaleTG);
			setSchedulingBounds(bounds);
		}
	}
} 

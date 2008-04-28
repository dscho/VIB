package ij3d;

import ij.gui.GenericDialog;
import ij.gui.MultiLineLabel;
import ij.IJ;
import ij.WindowManager;
import ij.ImagePlus;
import ij.text.TextWindow;
import ij.gui.Toolbar;

import math3d.Transform_IO;

import java.text.DecimalFormat;

import java.awt.event.*;
import java.awt.*;
import java.util.Vector;
import java.util.Iterator;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

import vib.PointList;
import vib.BenesNamedPoint;
import vib.FastMatrix;

import orthoslice.OrthoGroup;
import voltex.VoltexGroup;
import voltex.Renderer;
import isosurface.MeshGroup;
import isosurface.MeshExporter;
import isosurface.MeshEditor;

import javax.vecmath.Color3f;
import javax.media.j3d.View;
import javax.media.j3d.Transform3D;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.Collection;

public class RegistrationMenubar extends MenuBar implements ActionListener, 
					 		ItemListener,
							UniverseListener {

	private Image3DUniverse univ;
	
	private MenuItem exit;

	private Menu register;

	private List openDialogs = new ArrayList();


	public RegistrationMenubar(Image3DUniverse univ) {
		super();
		this.univ = univ;

		univ.addUniverseListener(this);

		register = createRegisterMenu();
		this.add(register);

	}

	public Menu createRegisterMenu() {
		Menu reg = new Menu("Register");

		exit = new MenuItem("Exit registration");
		exit.addActionListener(this);
		reg.add(exit);

		return reg;
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == exit) {
			exitRegistration();
		}
	}

	public void itemStateChanged(ItemEvent e) {
	}

	// Universe Listener interface
	public void transformationStarted(View view) {}
	public void transformationFinished(View view) {}
	public void canvasResized() {}
	public void transformationUpdated(View view) {}
	public void contentChanged(Content c) {}
	public void universeClosed() {}
	public void contentAdded(Content c) {}

	public void contentRemoved(Content c) {} 
	public void contentSelected(Content c) {}

	public void register() {
		new Thread(new Runnable() {
			public void run() {
				regist();
			}
		}).start();
	}

	public void exitRegistration() {
		MenuBar mb = univ.getMenuBar();
		univ.setMenubar(mb);
	}

	public void regist() {
		// Select the contents used for registration
		DecimalFormat df = new DecimalFormat("00.000");
		Collection contents = univ.getContents();
		if(contents.size() < 2) {
			IJ.error("At least two bodies are required for " +
				" registration");
			return;
		}
		String[] conts = new String[contents.size()];
		int i = 0;
		for(Iterator it = contents.iterator(); it.hasNext();)
			conts[i++] = ((Content)it.next()).getName();
		GenericDialog gd = new GenericDialog("Registration");
		gd.addChoice("template", conts, conts[0]);
		gd.addChoice("model", conts, conts[1]);
		gd.addCheckbox("allow scaling", true);
		openDialogs.add(gd);
		gd.showDialog();
		openDialogs.remove(gd);
		if(gd.wasCanceled())
			return;
		final Content templ = univ.getContent(gd.getNextChoice());
		final Content model = univ.getContent(gd.getNextChoice());
		boolean scaling = gd.getNextBoolean();

		// Select the landmarks of the template
		model.setVisible(false);
		templ.displayAs(Content.ORTHO);
		templ.setColor(new Color3f(1, 0, 0));
		Toolbar.getInstance().setTool(Toolbar.POINT);
		univ.select(templ);

		univ.setStatus("Select landmarks in " + templ.getName() +
				" and click OK");

		Panel p = new Panel(new FlowLayout());
		Button b = new Button("OK");
		p.add(b);
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				synchronized(templ) {
					templ.notify();
				}
			}
		});
		univ.pld.addPanel(p);
		synchronized(templ) {
			try {
				templ.wait();
			} catch(Exception e) {}
		}
		templ.setVisible(false);


		// select the landmarks of the model
		model.setVisible(true);
		model.displayAs(Content.ORTHO);
		model.setColor(new Color3f(0, 1, 0));
		Toolbar.getInstance().setTool(Toolbar.POINT);
		univ.select(model);

		univ.setStatus("Select landmarks in " + model.getName() +
				" and click OK");

		synchronized(templ) {
			try {
				templ.wait();
			} catch(Exception e) {}
		}
		model.setVisible(false);
		univ.setStatus("");


		// select the landmarks common to template and model
		PointList tpoints = templ.getPointList();
		PointList mpoints = model.getPointList();
		if(tpoints.size() < 2 || mpoints.size() < 2) {
			IJ.error("At least two points are required in each "
				+ "of the point lists");
		}
		List sett = new ArrayList();
		List setm = new ArrayList();
		for(i = 0; i < tpoints.size(); i++) {
			BenesNamedPoint pt = tpoints.get(i);
			BenesNamedPoint pm = mpoints.get(pt.getName());
			if(pm != null) {
				sett.add(pt);
				setm.add(pm);
			}
		}
		if(sett.size() < 2) {
			IJ.error("At least two points with the same name "
				+ "must exist in both bodies");
			univ.setStatus("");
			return;
		}

		// Display common landmarks
		String message = "Points used for registration\n \n";
		for(i = 0; i < sett.size(); i++) {
			BenesNamedPoint bnp = (BenesNamedPoint)sett.get(i);
			message += (bnp.getName() + "    "
				+ df.format(bnp.x) + "    "
				+ df.format(bnp.y) + "    "
				+ df.format(bnp.z) + "\n");
		}
		boolean cont = IJ.showMessageWithCancel(
			"Points used for registration", message);
		if(!cont) return;

		// calculate best rigid
		BenesNamedPoint[] sm = new BenesNamedPoint[setm.size()];
		BenesNamedPoint[] st = new BenesNamedPoint[sett.size()];
		FastMatrix fm = FastMatrix.bestRigid(
			(BenesNamedPoint[])setm.toArray(sm),
			(BenesNamedPoint[])sett.toArray(st));

		// reset the transformation of the template
		// and set the transformation of the model.
		Transform3D t3d = new Transform3D(fm.rowwise16());
		templ.setTransform(new Transform3D());
		model.setTransform(t3d);

		templ.setVisible(true);
		model.setVisible(true);

		univ.clearSelection();
		Toolbar.getInstance().setTool(Toolbar.HAND);
	}

	public void closeAllDialogs() {
		while(openDialogs.size() > 0) {
			GenericDialog gd = (GenericDialog)openDialogs.get(0);
			gd.dispose();
			openDialogs.remove(gd);
		}
	}
}


/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package vib;

/*
 * TODO:
 * - make translateMax/angleMax be dependent on the data
 * - make Choice auto-select when chosing the other neuropil
 *   (refactor center determination)
 */

import amira.AmiraParameters;

import distance.*;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.YesNoCancelDialog;
import ij.macro.Interpreter;
import ij.measure.Calibration;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.text.TextWindow;
import java.awt.Choice;
import java.util.StringTokenizer;
import math3d.Point3d;
import pal.math.*;

public class RigidRegistration_ implements PlugInFilter {
	ImagePlus image;

	GenericDialog gd;
	String[] materials1, materials2;
	private boolean verbose = false;

	public void run(ImageProcessor ip) {
		verbose = true;
		gd = new GenericDialog("Registration Parameters");
		gd.addStringField("initialTransform", "", 30);
		gd.addNumericField("n initial positions to try", 1, 0);

		int level = 0;
		while ((image.getWidth() >> level) > 20)
			level++;
		gd.addNumericField("tolerance", 1.0, 3);
		gd.addNumericField("level", level, 0);
		gd.addNumericField("stopLevel", (level > 2 ? 2 : level), 0);
		gd.addStringField("materialCenterAndBBox", "", 30);
		gd.addCheckbox("noOptimization", false);
		gd.addCheckbox("showTransformed", false);
		gd.addCheckbox("showDifferenceImage", false);
		gd.addCheckbox("Fast but inaccurate", !true);
		boolean isLabels = AmiraParameters.isAmiraLabelfield(image);

		if (isLabels) {
			AmiraParameters params = new AmiraParameters(image);
			materials1 = params.getMaterialList();
			gd.addChoice("material", materials1, materials1[0]);
			// cannot possibly fail!
			AmiraParameters.addAmiraLabelsList(gd, "Template");
			gd.addChoice("templateMaterial", 
					materials1, materials1[0]);
			getMaterials2();
		} else {
			int[] wIDs = WindowManager.getIDList();
			if(wIDs == null){
				IJ.error("No images open");
				return;
			}
			String[] titles = new String[wIDs.length];
			for(int i=0;i<wIDs.length;i++){
				titles[i] = WindowManager.
						getImage(wIDs[i]).getTitle();
			}
			
			gd.addChoice("Template", titles,
				WindowManager.getCurrentImage().getTitle());
			String[] methods = {
				"Euclidean", "MutualInfo", "Threshold55",
				"Threshold155" };
			gd.addChoice("measure", methods, "Euclidean");
		}

		gd.showDialog();
		if (gd.wasCanceled())
			return;

		String initial = gd.getNextString();
		int nInitialPositions = (int) gd.getNextNumber();
		double tolerance = gd.getNextNumber();
		level = (int)gd.getNextNumber();
		int stopLevel = (int)gd.getNextNumber();
		String materialBBox = gd.getNextString();
		boolean noOptimization = gd.getNextBoolean();
		boolean showTransformed = gd.getNextBoolean();
		boolean showDifferenceImage = gd.getNextBoolean();
		boolean fastButInaccurate = gd.getNextBoolean();
		int mat1 = (isLabels ? gd.getNextChoiceIndex() : -1);
		ImagePlus templ = WindowManager.getImage(gd.getNextChoice());
		int mat2 = (isLabels ? gd.getNextChoiceIndex() : -1);
		TransformedImage trans = new TransformedImage(templ, image);
		if (isLabels) {
			trans.measure = new distance.TwoValues(mat1, mat2);
			if(verbose)
				VIB.println("working on materials " + mat1 + " "
					+ mat2);
		} else {
			int measureIndex = gd.getNextChoiceIndex();
			if (measureIndex == 1)
				trans.measure =
					new distance.MutualInformation();
			else if (measureIndex == 2)
				trans.measure =
					new distance.Thresholded(55);
			else if (measureIndex == 3)
				trans.measure =
					new distance.Thresholded(155);
			else
				trans.measure =
					new distance.Euclidean();
		}

		FastMatrix matrix = rigidRegistration(trans, materialBBox, 
					initial, mat1, mat2, noOptimization, 
					level, stopLevel, tolerance, 
					nInitialPositions, showTransformed, 
					showDifferenceImage, fastButInaccurate);

		if (!Interpreter.isBatchMode() && verbose)
			WindowManager.setWindow(new TextWindow("Matrix",
						matrix.toStringForAmira(),
						550, 150));

		lastResult = matrix;
	}

	/**
	 *
	 *
	 * @param trans TransformedImage, needs the .measure setup correctly 
	 * along with the two images setup
	 * @param materialBBox  can be empty
	 * @param mat1  used in material distance measures
	 * @param mat2  used in material distance measures
	 * @param initial  can be empty, then 24 orientations are tried
	 * @param noOptimization
	 * @param level start level
	 * @param stopLevel end level
	 * @param tolerance
	 * @param nInitialPositions number of promising initial positions 
	 * to optimize further
	 * @param showTransformed  boolean whether to show the resultant 
	 * transofrmed image or not
	 * @param showDifferenceImage boolean whether to show the resultant 
	 * transofrmed image or not
	 * @return The best FastMatrix
	 */
	public FastMatrix rigidRegistration(
			TransformedImage trans,
			String materialBBox,
			String initial,
			int mat1,
			int mat2,
			boolean noOptimization,
			int level,
			int stopLevel,
			double tolerance,
			int nInitialPositions,
			boolean showTransformed,
			boolean showDifferenceImage, 
			boolean fastButInaccurate) {
		if (mat1 >= 0)
			trans.narrowSearchToMaterial(mat1, 10);

		Point3d center = null;
		if (materialBBox!=null && !materialBBox.equals(""))
			center = parseMaterialBBox(trans, materialBBox);

		double[] params = null;
		if (initial!=null && ! initial.equals("")) {
			params = new double[9];
			try {
				if (center == null)
					center = getCenter(trans.transform,
							 mat1);

				FastMatrix m = FastMatrix.parseMatrix(
						initial);
				if (mat2 >= 0) {
					/*
					 * If registering labelfields, make
					 * sure that the center of gravity
					 * is transformed onto orig's
					 * center of gravity.
					 */
					m.apply(center);
					Point3d p = getCenter(trans.orig, mat2);
					p = p.minus(m.getResult());
					m = FastMatrix.translate(p.x, p.y, p.z)
						.times(m);
				}

				m.guessEulerParameters(params, center);
			} catch(Exception e) {
				StringTokenizer t =
					new StringTokenizer(initial);
				for (int i = 0; i < 9; i++)
					params[i] = Double.parseDouble(
							t.nextToken());
			}
		}

		FastMatrix matrix;
		if (!noOptimization) {
			Optimizer opt = fastButInaccurate 
			? new FastOptimizer(trans, level, stopLevel, tolerance)
			: new Optimizer(trans, level, stopLevel, tolerance);
			opt.eulerParameters = params;

			if(opt.eulerParameters == null){
				FastMatrix [] results = 
					new FastMatrix[nInitialPositions];
				double badnees[] = 
					new double[nInitialPositions];

				for(int i = 0; i < nInitialPositions; i++){
					opt.eulerParameters = null;
					results[i] = opt.doRegister(
							level - stopLevel, i);
					//todo probably recalculated wastefully	
					badnees[i] = opt.calculateBadness(
							results[i]);   
				}

				//now select the best
				double best = Double.MAX_VALUE;
				int bestIndex = -1;
				for (int i = 0; i < badnees.length; i++) {

					if(badnees[i] < best){
						best = badnees[i];
						bestIndex = i;
					}
				}


				matrix = results[bestIndex];
				if(verbose)
					System.out.println("winner was " + 
							(bestIndex+1) + 
							" with " + matrix);

			}else{
				matrix = opt.doRegister(level - stopLevel);
			}


			opt = null;
		} else {
			try {
				matrix = FastMatrix.parseMatrix(initial);
			} catch(Exception e) {
				StringTokenizer t =
					new StringTokenizer(initial);
				for (int i = 0; i < 9; i++)
					params[i] = Double.parseDouble(
							t.nextToken());
				matrix = RegistrationOptimizer.getEulerMatrix(
					params);
			}
		}

		trans.setTransformation(matrix);
		if(verbose) 
			VIB.println(matrix.toString());
		if (showTransformed)
			trans.getTransformed().show();
		if (showDifferenceImage)
			trans.getDifferenceImage().show();

		// give the garbage collector a chance:
		trans = null;
		System.gc();
		System.gc();
		return matrix;
	}

	Point3d parseMaterialBBox(TransformedImage trans, String bbox) {
		StringTokenizer t = new StringTokenizer(bbox);
		try {
			Point3d center = new Point3d();
			center.x = Double.parseDouble(t.nextToken());
			center.y = Double.parseDouble(t.nextToken());
			center.z = Double.parseDouble(t.nextToken());
			double x0, x1, y0, y1, z0, z1;
			x0 = Double.parseDouble(t.nextToken());
			x1 = Double.parseDouble(t.nextToken());
			y0 = Double.parseDouble(t.nextToken());
			y1 = Double.parseDouble(t.nextToken());
			z0 = Double.parseDouble(t.nextToken());
			z1 = Double.parseDouble(t.nextToken());
			FastMatrix toOrig = trans.fromOrig.inverse();
			toOrig.apply(x0, y0, z0);
			Point3d llf = toOrig.getResult();
			toOrig.apply(x1, y1, z1);
			Point3d urb = toOrig.getResult();
			trans.narrowBBox((int)llf.x - 10,
					(int)Math.ceil(urb.x) + 10,
					(int)llf.y - 10,
					(int)Math.ceil(urb.y) + 10,
					(int)llf.z - 10,
					(int)Math.ceil(urb.z) + 10);
			return center;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	Point3d getCenter(InterpolatedImage ii, int mat1) {
		if (mat1 >= 0)
			return ii.getCenterOfGravity(mat1);
		else
			return ii.getCenterOfGravity();
	}

	static FastMatrix lastResult;

	public static String getLastResult() {
		return lastResult.toStringForAmira();
	}

	void getMaterials2() {
		Choice templateChoice = (Choice)gd.getChoices().get(1);
		int index = templateChoice.getSelectedIndex();
		if (index < 0)
			index = 0;
		String template = templateChoice.getItem(index);
		ImagePlus t = WindowManager.getImage(template);
		AmiraParameters params = new AmiraParameters(t);
		materials2 = params.getMaterialList();

		Choice mat2 = (Choice)gd.getChoices().get(2);
		String chosen = (mat2 == null ? null :
				mat2.getItem(mat2.getSelectedIndex()));

		mat2.removeAll();
		int selectedIndex = 0;
		for (int i = 0; i < materials2.length; i++) {
			mat2.addItem(materials2[i]);
			if (chosen != null && materials2[i].equals(chosen))
				selectedIndex = i;
		}

		if (chosen == null)
			adjustMaterial(false);
		else
			mat2.select(selectedIndex);
	}

	void adjustMaterial(boolean fromTemplate) {
		Choice c1 = (Choice)gd.getChoices().get(0);
		Choice c2 = (Choice)gd.getChoices().get(2);
		if (fromTemplate) {
			Choice c3 = c1; c1 = c2; c2 = c3;
		}

		int index = c1.getSelectedIndex();
		String chosen = c1.getItem(index);
		for (int i = 0; i < c2.getItemCount(); i++)
			if (chosen.equals(c2.getItem(i))) {
				c2.select(i);
				return;
			}

		if (c2.getSelectedIndex() < 0)
			c2.select(0);
	}

	public int setup(String arg, ImagePlus imp) {
		image = imp;
		return DOES_8G | DOES_8C | NO_CHANGES;
	}

	static class Optimizer extends RegistrationOptimizer {
		TransformedImage t;
		int start, stop;
		double tolerance;

		public Optimizer(TransformedImage trans,
				int startLevel, int stopLevel,
				double tol) {
			if (stopLevel < 2)
				t = trans;
			else
				t = trans.resample(1 << (stopLevel - 1));
			start = startLevel;
			stop = stopLevel;
			tolerance = tol;
		}

		public FastMatrix doRegister(int level) {
			return doRegister(level, 0);
		}

		/**
		 *
		 * @param level
		 * @param initialGuessPlace 0 uses the best distance measure from the initial set of guesses if EulerParams were not supplied
		 * @return
		 */

		public FastMatrix doRegister(int level, int initialGuessPlace) {
			if (level > 0) {
				TransformedImage backup = t;
				t = t.resample(2);
				t.setTransformation(doRegister(level - 1));
				//t.getTransformed().show();
				//t.getDifferenceImage().show();
				t = backup;
				System.gc();
				System.gc();
			}
			if(verbose)
				VIB.println("level is " + (start - level));

			double factor = (1 << (start - level));
			int minFactor = (1 << start);
			angleMax = Math.PI / 4 * factor / minFactor;
			translateMax = 20.0 * factor / minFactor;
			if(eulerParameters == null){
				eulerParameters = 
				searchInitialEulerParams()[initialGuessPlace];
			}
			return doRegister(tolerance / factor);
		}

		public void getInitialCenters() {
			if (t.measure instanceof distance.TwoValues) {
				distance.TwoValues d = (distance.TwoValues)
					t.measure;
				int m1 = d.getMaterial(0);
				int m2 = d.getMaterial(1);
				transC = t.transform.getCenterOfGravity(m1);
				origC = t.orig.getCenterOfGravity(m2);
			} else {
				transC = t.transform.getCenterOfGravity();
				origC = t.orig.getCenterOfGravity();
			}
		}

		public double calculateBadness(FastMatrix matrix) {
			t.setTransformation(matrix);
			return t.getDistance();
		}
	}
	
	static class FastOptimizer extends Optimizer {
		private int centerX, centerY, centerZ;
		
		public FastOptimizer(TransformedImage trans,
				int startLevel, int stopLevel,
				double tol) {
			super(trans, startLevel, stopLevel, tol);
			current = new Point3d();
		}

		@Override
		public void getInitialCenters(){
			super.getInitialCenters();
			Calibration calib = t.orig.getImage().getCalibration();
			centerX = (int)Math.round((origC.x - calib.xOrigin) 
							/ calib.pixelWidth);
			centerY = (int)Math.round((origC.y - calib.yOrigin) 
							/ calib.pixelHeight);
			centerZ = (int)Math.round((origC.z - calib.zOrigin) 
							/ calib.pixelDepth);
		}


		private Point3d start, stop, current;
		
		public void initStartStop(int i0, int j0, int k0, 
				int i1, int j1, int k1) {
			t.matrix.apply(i0, j0, k0);
			start = t.matrix.getResult();
			t.matrix.apply(i1, j1, k1);
			stop = t.matrix.getResult().minus(start);
		}

		public void calculateCurrent(int i, int total) {
			current.x = start.x + i * stop.x / total;
			current.y = start.y + i * stop.y / total;
			current.z = start.z + i * stop.z / total;
		}
			
		@Override
		public double calculateBadness(FastMatrix matrix) {
			t.setTransformation(matrix);
			t.measure.reset();
			for (int i = 0; i < t.orig.w; i++) {
				initStartStop(i, 0, centerZ, 
							i, t.orig.h, centerZ);
				for (int j = 0; j < t.orig.h; j++) {
					calculateCurrent(j, t.orig.h);
					float vOrig = t.orig.
						getNoInterpol(i, j, centerZ);
					float  vTrans = (float)t.transform.
						interpol.get(current.x, 
						current.y, current.z);
					t.measure.add(vOrig, vTrans);
				}
			}
			for (int i = 0; i < t.orig.d; i++) {
				initStartStop(0,centerY,i,t.orig.w,centerY,i);
				for (int j = 0; j < t.orig.w; j++) {
					calculateCurrent(j, t.orig.w);
					float vOrig = (float)t.orig.
						getNoInterpol(j, centerY, i);
					float vTrans = (float) t.transform.
						interpol.get(current.x, 
						current.y, current.z);
					t.measure.add(vOrig, vTrans);
				}
			}
			for (int i = 0; i < t.orig.d; i++) {
				initStartStop(centerX,0,i,centerX,t.orig.h,i);
				for (int j = 0; j < t.orig.h; j++) {
					calculateCurrent(j, t.orig.h);
					float vOrig = (float)t.orig.
						getNoInterpol(centerX, j, i);
					float vTrans = (float)t.transform.
						interpol.get(current.x, 
						current.y, current.z);
					t.measure.add(vOrig, vTrans);
				}
			}
			return t.measure.distance();
		}
	}
}

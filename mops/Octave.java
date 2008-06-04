package mops;

import java.util.List;
import java.util.ArrayList;

import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;

import vib.InterpolatedImage;

public class Octave {

	private InterpolatedImage first, last;
	private float[] sigma_diff;

	private InterpolatedImage[] dog;

	public Octave(InterpolatedImage image, float[] sigma_diff) {
		this.first = image;
		this.sigma_diff = sigma_diff;
		this.last = Filter.gauss(
			first, sigma_diff[sigma_diff.length - 1]);
	}

	public InterpolatedImage resample() {
		ImagePlus tmp = last.getImage();
		int w = tmp.getWidth(), h = tmp.getHeight();
		int d = tmp.getStackSize();
		int w_2 = w/2, h_2 = h/2, d_2 = d/2;
		ImageStack stack = new ImageStack(w_2, h_2);
		for(int z = 0; z < d_2; z++) {
			float[] p = new float[w_2 * h_2];
			for(int y = 0; y < h_2; y++) {
				for(int x = 0; x < w_2; x++) {
					p[y * w_2 + x] = last.getNoCheckFloat(
						x*2, y*2, z*2);
				}
			}
			stack.addSlice("", p);
		}
		ImagePlus retImage = new ImagePlus("", stack);
		Calibration c = tmp.getCalibration().copy();
		c.pixelWidth *= 2;
		c.pixelHeight *= 2;
		c.pixelDepth *= 2;
		retImage.setCalibration(c);
		return new InterpolatedImage(retImage);
	}

	public void dog() {
		int steps = sigma_diff.length;
		dog = new InterpolatedImage[steps - 1];
		InterpolatedImage prev = first.cloneImage(), next;
		for(int i = 0; i < steps - 1; i++) {
			next = Filter.gauss(prev, sigma_diff[i+1]);
			Filter.sub(next, prev);
			dog[i] = prev;
			prev = next;
		}
	}

	/*
	 * float indices of return list are
	 * 0 -> x, 1 -> y, 2 -> z, 3 -> scale index
	 */
	public List<float[]> getCandidates() {
		if(dog == null)
			dog();

		List<float[]> l = new ArrayList<float[]> ();
		InterpolatedImage.Iterator it = null;
		for(int i = 0; i < dog.length; i++) {
			Filter.suppNonExtremum(dog[i]);
			it = dog[i].iterator();
			float v;
			while(it.next() != null) {
				v = dog[i].getNoCheckFloat(it.i, it.j, it.k);
				if(v != 0)
					l.add(new float[] {it.i,it.j,it.k,i});
			}
		}
		return l;
	}
}


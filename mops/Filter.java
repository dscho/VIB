package mops;

import vib.InterpolatedImage;
import process3d.Smooth_;

public class Filter {

	public static InterpolatedImage gauss(InterpolatedImage ii, float sig) {
		return new InterpolatedImage(Smooth_.smooth(
					ii.getImage(), true, sig, false));
	}

	/*
	 * subtract i2 from i1 and save the result in _i2_
	 * The images must have the same size; this is not checked.
	 */
	public static void sub(InterpolatedImage i1, InterpolatedImage i2) {
		InterpolatedImage.Iterator it = i1.iterator();
		while(it.next() != null) {
			i2.setFloat(it.i, it.j, it.k,
				i1.getNoCheckFloat(it.i, it.j, it.k) -
				i2.getNoCheckFloat(it.i, it.j, it.k));
		}
	}

	public static InterpolatedImage gradX(InterpolatedImage ii) {
		InterpolatedImage ret = ii.cloneDimensionsOnly();
		InterpolatedImage.Iterator it = ret.iterator();
		while(it.next() != null) {
			ret.setFloat(it.i, it.j, it.k,
				(ii.getNoInterpolFloat(it.i+1, it.j, it.k) - 
				ii.getNoInterpolFloat(it.i-1, it.j, it.k) / 2));
		}
		return ret;
	}

	public static InterpolatedImage gradY(InterpolatedImage ii) {
		InterpolatedImage ret = ii.cloneDimensionsOnly();
		InterpolatedImage.Iterator it = ret.iterator();
		while(it.next() != null) {
			ret.setFloat(it.i, it.j, it.k,
				(ii.getNoInterpolFloat(it.i, it.j+1, it.k) - 
				ii.getNoInterpolFloat(it.i, it.j-1, it.k) / 2));
		}
		return ret;
	}

	public static InterpolatedImage gradZ(InterpolatedImage ii) {
		InterpolatedImage ret = ii.cloneDimensionsOnly();
		InterpolatedImage.Iterator it = ret.iterator();
		while(it.next() != null) {
			ret.setFloat(it.i, it.j, it.k,
				(ii.getNoInterpolFloat(it.i, it.j, it.k+1) - 
				ii.getNoInterpolFloat(it.i, it.j, it.k-1) / 2));
		}
		return ret;
	}

	public static void suppNonExtremum(InterpolatedImage ii) {
		InterpolatedImage.Iterator it = ii.iterator();
		while(it.next() != null) {
			float v1 = ii.getNoCheckFloat(it.i, it.j, it.k);
			boolean isMax = true, isMin = true;
			for(int i = 0; i < 27; i++) {
				if(i == 27/2)
					continue;
				int mz = i / 9 - 1; // 1 = d/2
				int my = (i % 9) / 3 - 1; // 1 = h/2
				int mx = (i % 9) % 3 - 1; // 1 = w/2
				float v2 = ii.getNoInterpolFloat(
					it.i + mx, it.j + my, it.k + mz);
				if(v2 > v1) isMax = false;
				if(v2 < v1) isMin = false;
				if(!isMin && !isMax)
					ii.setFloat(it.i, it.j, it.k, 0);
			}
		}
	}
}


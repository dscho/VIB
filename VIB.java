/*
 * convenience class
 */

import ij.IJ;

public class VIB {
	public static void showStatus(String message) {
		if (IJ.getInstance() == null)
			println(message);
		else
			IJ.showStatus(message);
	}

	public static void showProgress(int step, int count) {
		if (IJ.getInstance() == null)
			;
		else
			IJ.showProgress(step, count);
	}

	public static void println(String message) {
		BatchLog_.appendText(message + "\n");
	}
}


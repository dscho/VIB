import ij.IJ;
import ij.ImagePlus;
import ij.Menus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.io.SaveDialog;
import ij.plugin.PlugIn;
import ij.text.TextWindow;

import java.awt.Menu;
import java.awt.MenuBar;
import java.util.Vector;

import amira.AmiraMeshEncoder;
import amira.AmiraParameters;
import amira.AmiraTableEncoder;

public class AmiraMeshWriter_ implements PlugIn {

	public void run(String arg) {
		GenericDialog gd = new GenericDialog("Choose Window");
		int c = AmiraParameters.addWindowList(gd, "window", false);
		if (c == 0) {
			IJ.error("No window?");
			return;
		}
		if (c > 1) {
			gd.showDialog();
			if (gd.wasCanceled())
				return;
		}

		String title = gd.getNextChoice();
		Object frame = WindowManager.getImage(title);
		if (frame == null)
			frame = WindowManager.getFrame(title);
		else {
			int type = ((ImagePlus)frame).getType();
			if (type != ImagePlus.GRAY8 &&
					type != ImagePlus.COLOR_256) {
				IJ.error("Invalid image type");
				return;
			}
		}
		if (frame == null) {
			IJ.error("No window?");
			return;
		}

		writeImage(frame);

	}

	public static void writeImage(Object frame) {
		SaveDialog od = new SaveDialog("AmiraFile", null, ".am");
		String dir=od.getDirectory();
		String name=od.getFileName();
		if(name==null)
			return;

		if (frame instanceof TextWindow) {
			TextWindow t = (TextWindow)frame;
			AmiraTableEncoder e = new AmiraTableEncoder(t);
			if (!e.write(dir + name))
				IJ.error("Could not write to " + dir + name);
			return;
		}

		AmiraMeshEncoder e=new AmiraMeshEncoder(dir+name);

		if(!e.open()) {
			IJ.error("Could not write "+dir+name);
			return;
		}

		if(!e.write((ImagePlus)frame))
			IJ.error("Error writing "+dir+name);
	}

	public static String[] getWindowList() {
		Vector v = new Vector();
		MenuBar mbar = Menus.getMenuBar();
		Menu menu = null;
		for (int i = 0; i < mbar.getMenuCount(); i++)
			if (mbar.getMenu(i).getLabel().equals("Window")) {
				menu = mbar.getMenu(i);
				break;
			}
		if (menu == null)
			throw new RuntimeException("no Window menu?");
		for (int i = 0; i < WindowManager.getWindowCount(); i++) {
			ImagePlus img = WindowManager.getImage(i + 1);
			v.add(img.getTitle());
		}
		for (int i = 0; i < menu.getItemCount(); i++) {
			String title = menu.getItem(i).getLabel();
			if (WindowManager.getFrame(title) != null)
				v.add(title);
		}

		String[] result = new String[v.size()];
		for (int i = 0; i < result.length; i++)
			result[i] = (String)v.get(i);
		return result;
	}
};



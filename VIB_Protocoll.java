import java.awt.*;
import java.awt.event.*;

import java.io.File;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

import vib.app.gui.Console;
import vib.app.gui.FileGroupDialog;
import vib.app.FileGroup;
import vib.app.Options;
import vib.app.module.EndModule;
import vib.app.module.Module;

public class VIB_Protocoll implements PlugIn, ActionListener {
	private Button fg;
	private Options options;
	
	public void run(String arg) {
		options = new Options();
		GenericDialog gd = new GenericDialog("VIB Protocoll");
		
		Panel filegroupedit = new Panel(new FlowLayout());
		fg = new Button("Edit file group");
		fg.addActionListener(this);
		filegroupedit.add(fg);
		FileGroup fgroup = options.getFileGroup();
		fgroup.add("/home/bene/phd/VIB_git/Bilder/VIB05_m7_78y_mcd8GFP_AJ.tif");
		fgroup.add("/home/bene/phd/VIB_git/Bilder/VIB05_m6_78y_mcd8GFP_AJ.tif");

		gd.addStringField("Working directory","/home/bene/gitakt/results/", 12);
		gd.addPanel(filegroupedit, 
				GridBagConstraints.CENTER, new Insets(5,0,0,0));
		gd.addStringField("Template", "/home/bene/phd/VIB_git/Bilder/VIB05_m6_78y_mcd8GFP_AJ.tif", 12);

		gd.addNumericField("No of channels", 2, 0);
		gd.addNumericField("No of the reference channel", 2, 0);

		gd.addNumericField("Resampling factor", 2, 0);
		String[] algorithms = new String[]{"LabelDiffusionInterpolation"};
		gd.addChoice("Transformation", algorithms, algorithms[0]);

		gd.showDialog();
		if(gd.wasCanceled())
			return;

		options.setWorkingDirectory(new File(gd.getNextString()));
		options.setTemplate(new File(gd.getNextString()));
		options.setNumChannels((int)Math.round(gd.getNextNumber()));
		options.setRefChannel((int)Math.round(gd.getNextNumber()));
		options.setResamplingFactor((int)Math.round(gd.getNextNumber()));
		options.setTransformationMethod(gd.getNextChoice());

		Console console = Console.instance();
		final Frame f = new Frame();
		f.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				f.dispose();
			}
		});
		f.add(console);
		f.setSize(200,200);
		f.setVisible(true);
		Module m = null;
		try {
			m = new EndModule(null, options);
			m.run();
		} catch(OutOfMemoryError e) {
			System.out.println("out or memory...exiting");
			System.exit(0);
		}
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == fg) {
			FileGroupDialog fgd = new FileGroupDialog(
					IJ.getInstance(), options.getFileGroup());
			fg.setLabel(options.getFileGroup().getName());
		}
	}
}

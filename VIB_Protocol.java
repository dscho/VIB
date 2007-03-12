import java.awt.*;
import java.awt.event.*;

import java.util.Vector;
import java.io.File;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.io.OpenDialog;
import ij.io.SaveDialog;

import vib.app.gui.Console;
import vib.app.gui.FileGroupDialog;
import vib.app.FileGroup;
import vib.app.Options;
import vib.app.module.EndModule;
import vib.app.module.State;

public class VIB_Protocol implements PlugIn, ActionListener {

	// indices in the getStringFields() / getNumericFields()
	static final int WD = 0;
	static final int TEMPL = 1;
	static final int NO_CHANNEL = 0;
	static final int REF_CHANNEL = 1;
	static final int RES_F = 2;
	static final int TRANSF = 0;

	private Button fg, load, save;
	private Options options;
	private GenericDialog gd;
	
	public void run(String arg) {
		options = new Options();
// TODO
//		String configfile = ij.Macro.getValue(ij.Macro.getOptions(), 
//				"configfile", "");
//		if(!configfile.equals(""))
//			options.loadFrom("/home/bene/gitakt/results/images_small/options.config");

		gd = new GenericDialog("VIB Protocol");
		
		Panel loadsave = new Panel(new FlowLayout());
		load = new Button("Load config");
		load.addActionListener(this);
		loadsave.add(load);
		save = new Button("Save config");
		save.addActionListener(this);
		loadsave.add(save);
		gd.addPanel(loadsave, GridBagConstraints.CENTER, new Insets(5,0,0,0));

		Panel filegroupedit = new Panel(new FlowLayout());
		fg = new Button("Edit file group");
		fg.addActionListener(this);
		filegroupedit.add(fg);

		gd.addStringField("Working directory","", 12);
		gd.addPanel(filegroupedit, 
				GridBagConstraints.CENTER, new Insets(5,0,0,0));
		gd.addStringField("Template", "", 12);

		gd.addNumericField("No of channels", 2, 0);
		gd.addNumericField("No of the reference channel", 2, 0);

		gd.addNumericField("Resampling factor", 2, 0);
		String[] algorithms = new String[]{"LabelDiffusionInterpolation"};
		gd.addChoice("Transformation", algorithms, algorithms[0]);

		options.loadFrom(
				"/home/bene/gitakt/results/images_small/options.config");
		initTextFields();

		gd.showDialog();
		if(gd.wasCanceled())
			return;

		initOptions();

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
		State state = new State(options);
		EndModule.runOnAllImages(state);
	}

	public void initTextFields() {
		setString(WD, options.workingDirectory);
		setString(TEMPL, options.templatePath);
		setNumber(NO_CHANNEL, options.numChannels);
		setNumber(REF_CHANNEL, options.refChannel);
		setNumber(RES_F, options.resamplingFactor);
		int method = options.transformationMethod;
		setChoice(TRANSF, Options.TRANSFORMS[method]);
	}

	public void initOptions() {
		options.workingDirectory = getString(WD);
		options.templatePath = getString(TEMPL);
		options.numChannels = getNumber(NO_CHANNEL);
		options.refChannel = getNumber(REF_CHANNEL);
		options.resamplingFactor = getNumber(RES_F);
		options.setTransformationMethod(getChoice(TRANSF));
	}

	private String getChoice(int i) {
		Choice c = (Choice)gd.getChoices().get(i);
		return c.getSelectedItem();
	}

	private void setChoice(int i, String val) {
		((Choice)gd.getChoices().get(i)).select(val);
	}

	private int getNumber(int i) {
		TextField tf = (TextField)gd.getNumericFields().get(i);
		double d = 0;
		try {
			d = Double.parseDouble(tf.getText());
		} catch (NumberFormatException e) {
			IJ.error(tf.getText() + " is not a number");
		}
		return (int)Math.round(d);
	}

	private String getString(int i) {
		TextField tf = (TextField)gd.getStringFields().get(i);
		return tf.getText();
	}

	private void setNumber(int i, int num) {
		((TextField)gd.getNumericFields().get(i)).
			setText(Integer.toString(num));
	}

	private void setString(int i, String st) {
		((TextField)gd.getStringFields().get(i)).setText(st);
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == fg) {
			FileGroupDialog fgd = new FileGroupDialog(
					IJ.getInstance(), options.fileGroup);
			fg.setLabel(options.fileGroup.name);
		} else if(e.getSource() == load) {
			OpenDialog op = new OpenDialog("Open config",null);
			options.loadFrom(op.getDirectory() + op.getFileName());
			initTextFields();
		} else if(e.getSource() == save) {
			initOptions();
			SaveDialog sa = new SaveDialog("Save config", "options", ".config");
			options.saveTo(sa.getDirectory() + sa.getFileName());
		}
	}
}

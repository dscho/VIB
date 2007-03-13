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

	private Button fg, load, save, templateButton;
	private Options options;
	private GenericDialog gd;
	private FileGroupDialog fgd;
	private File template;
	
	public void run(String arg) {
		options = new Options();
		System.out.println(System.getProperty("user.dir"));

		gd = new GenericDialog("VIB Protocol");
		fgd = new FileGroupDialog(options.fileGroup);
		templateButton = fgd.getTemplateButton();
		templateButton.addActionListener(this);
		
		gd.addPanel(fgd);

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = c.gridy = 1;
		c.fill = GridBagConstraints.NONE;
		c.weightx = c.weighty = 0.0;
		load = new Button("Select working directory");
		load.addActionListener(this);
		// work around not being able to access gd.y
		Panel panel = new Panel();
		gd.addPanel(panel);
		gd.remove(panel);
		gd.add(load, c);


		gd.addStringField("Working directory","", 25);
		gd.addStringField("Template", "", 25);
		gd.addNumericField("No of channels", 2, 0);
		gd.addNumericField("No of the reference channel", 2, 0);
		gd.addNumericField("Resampling factor", 2, 0);

		final TextField wdtf = (TextField)gd.getStringFields().get(WD);
		wdtf.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				loadFrom(wdtf.getText());
			}
		});

		// make the template textfield ineditable
		TextField templateField =
			(TextField)gd.getStringFields().get(TEMPL);
		templateField.setEditable(false);

		gd.showDialog();
		if(gd.wasCanceled())
			return;

		initOptions();
		options.saveTo(options.workingDirectory + File.separator
				+ Options.CONFIG_FILE);

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
		new EndModule().runOnAllImages(state);
	}

	public void loadFrom(String workingDirectory) {
		setString(WD, workingDirectory);
		File f = new File(workingDirectory + File.separator +
				Options.CONFIG_FILE);
		if(f.exists()) {
			options.loadFrom(f.getAbsolutePath());
			initTextFields();
		}
	}

	public void initTextFields() {
		template = new File(options.templatePath);
		setString(WD, options.workingDirectory);
		setString(TEMPL, options.templatePath);
		setNumber(NO_CHANNEL, options.numChannels);
		setNumber(REF_CHANNEL, options.refChannel);
		setNumber(RES_F, options.resamplingFactor);
		//int method = options.transformationMethod;
		//setChoice(TRANSF, Options.TRANSFORMS[method]);
		fgd.update();
	}

	public void initOptions() {
		options.workingDirectory = getString(WD);
		options.templatePath = getString(TEMPL);
		options.numChannels = getNumber(NO_CHANNEL);
		options.refChannel = getNumber(REF_CHANNEL);
		options.resamplingFactor = getNumber(RES_F);
		//options.setTransformationMethod(getChoice(TRANSF));
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
		if(e.getSource() == templateButton) {
			File selected = fgd.getSelected();
			if(selected != null) {
				template = selected;
				setString(TEMPL, selected.getName());
			}
		} else if (e.getSource() == load) {
			FileDialog dialog =
				new FileDialog(gd, "Working Directory");
			dialog.setVisible(true);
			String dir = dialog.getDirectory();
			if (dir != null)
				loadFrom(dir);
		}
	}
}

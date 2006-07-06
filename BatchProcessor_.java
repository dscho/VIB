
import ij.plugin.PlugIn;
import ij.plugin.ImageCalculator;
import ij.IJ;
import ij.ImagePlus;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import gui.GuiBuilder;
import imagescience.transforms.Affine;
import imagescience.images.ByteImage;

/**
 * User: Tom Larkworthy
 * Date: 05-Jul-2006
 * Time: 12:16:26
 */
public class BatchProcessor_ extends JFrame implements PlugIn {


	JList fileList;

	public static final String RUN_BATCH = "run";

	JCheckBox amiraConvert, scale, rigidRegistration, averageIntensity;

	JTextField amiraDirectory; //to put the amira files in

	JSpinner width, height; //scaling spinners

	JSpinner initialPos, startLevel, stopLevel; //Rigid Reg params
	JTextField templateFileLocation;

	JTextField averageIntensityLocation;

	public BatchProcessor_() {
		Controllor controllor = new Controllor();

		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

		fileList = addFileList(this);

		amiraConvert = GuiBuilder.addCheckBox(this, "convert to Amira format");
		//amiraDirectory = GuiBuilder.addDirectoryField(this, "destination dir");   NOT USED YET

		scale = GuiBuilder.addCheckBox(this, "scale");
		JPanel sp = new JPanel(new GridLayout(1, 2));
		width = GuiBuilder.addLabeledNumericSpinner(sp, "width", 512, 0, Integer.MAX_VALUE, null);
		height = GuiBuilder.addLabeledNumericSpinner(sp, "height", 512, 0, Integer.MAX_VALUE, null);
		add(sp);

		rigidRegistration = GuiBuilder.addCheckBox(this, "MI rigid registration");
		JPanel rp = new JPanel(new GridLayout(1, 3));
		initialPos = GuiBuilder.addLabeledNumericSpinner(rp, "n start locs", 10, 1, 24, null);
		stopLevel = GuiBuilder.addLabeledNumericSpinner(rp, "stop level", 2, 1, 10, null);
		startLevel = GuiBuilder.addLabeledNumericSpinner(rp, "start level", 4, 1, 10, null);
		add(rp);
		templateFileLocation = GuiBuilder.addFileField(this, "template file");


		averageIntensity = GuiBuilder.addCheckBox(this, "calc average intensity");
		averageIntensityLocation = GuiBuilder.addFileSaveField(this, "save location");

		GuiBuilder.addCommand(this, RUN_BATCH, RUN_BATCH, controllor);
	}


	public static JList addFileList(Container c) {
		final DefaultListModel model = new DefaultListModel();
		final JList list = new JList(model);

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(new JLabel("files..."), BorderLayout.NORTH);

		JPanel controlPanel = new JPanel(new GridLayout(1, 2));
		panel.add(controlPanel, BorderLayout.SOUTH);
		panel.add(new JScrollPane(list));
		JButton add = new JButton("add");
		add.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser(IJ.getDirectory("current"));
				chooser.setMultiSelectionEnabled(true);
				chooser.showOpenDialog(null);

				File[] files = chooser.getSelectedFiles();
				if (files != null) {
					for (int i = 0; i < files.length; i++) {
						model.addElement(files[i]);
					}
				}
			}
		});

		JButton remove = new JButton("remove");
		remove.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				int index = list.getSelectedIndex();  //just currently working for the first of the selections
				if (index == -1) return;

				model.removeElementAt(index);
			}
		});

		controlPanel.add(add);
		controlPanel.add(remove);


		c.add(panel);

		return list;
	}

	public ArrayList<File> getFiles() {
		ArrayList<File> ret = new ArrayList<File>();
		for (int i = 0; i < fileList.getModel().getSize(); i++) {
			ret.add((File) fileList.getModel().getElementAt(i));
		}
		return ret;
	}

	public void setFiles(ArrayList<File> files) {
		((DefaultListModel) fileList.getModel()).removeAllElements();
		for (File file : files) {
			((DefaultListModel) fileList.getModel()).addElement(file);
		}
	}

	public void run(String arg0) {
		setVisible(!isVisible());
	}


	/**
	 * converts the files into amira file format
	 *
	 * @param files
	 * @return
	 */
	private static ArrayList<File> amiraConvert(ArrayList<File> files) {
		ArrayList<File> ret = new ArrayList<File>();
		for (File file : files) {
			try {
				ret.add(amiraConvert(file));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return ret;
	}

	private static File amiraConvert(File file) {

		ImagePlus ip = IJ.openImage(file.getAbsolutePath());


		File saveFile = new File(file.getParent(), "A" + file.getName() + ".grey");

		saveAndClose(saveFile, ip);

		return saveFile;
	}


	private ArrayList<File> scale(ArrayList<File> files, int width, int height) {
		ArrayList<File> ret = new ArrayList<File>();
		for (File file : files) {
			try {
				ret.add(scale(file, width, height));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return ret;
	}

	private File scale(File file, int width, int height) {
		Affine trans = new Affine();

		ImagePlus ip = open(file);

		double sxFactor = (double) width / (double) ip.getWidth();
		double syFactor = (double) height / (double) ip.getHeight();
		double szFactor = (sxFactor + syFactor) / 2;  //should normally be the same

		double[][] matrix = new double[][]{{sxFactor, 0, 0, 0},
										   {0, syFactor, 0, 0},
										   {0, 0, szFactor, 0},
										   {0, 0, 0, 1}};

		ImagePlus result = trans.run(new ByteImage(ip), matrix, Affine.LINEAR, true, false).imageplus();


		File saveFile = new File(file.getParent(), "W" + width + "H" + height + file.getName());

		saveAndClose(saveFile, result);
		ip.close();

		return saveFile;
	}


	private ArrayList<File> miRigidRegress(ArrayList<File> files, File templateLoc, int intialPositions, int stopLevel, int startLevel, double tolerance) {
		ArrayList<File> ret = new ArrayList<File>();

		ImagePlus template = open(templateLoc);

		for (File file : files) {
			try {
				ret.add(miRigidRegress(file, template, intialPositions, stopLevel, startLevel, tolerance));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		template.close();
		return ret;
	}

	private File miRigidRegress(File file, ImagePlus template, int intialPositions, int stopLevel, int startLevel, double tolerance) {
		ImagePlus image = open(file);

		RigidRegistration_ register = new RigidRegistration_();

		TransformedImage trans = new TransformedImage(template, image);
		trans.measure = new distance.MutualInformation();


		FastMatrix matrix = register.rigidRegistration(trans, null, null, -1, -1, false, startLevel, stopLevel, tolerance, intialPositions, false, false);

		trans.setTransformation(matrix);

		File saveFile = new File(file.getParent(), "R" + intialPositions + "L" + startLevel + "-" + stopLevel + file.getName());

		saveAndClose(saveFile, trans.getTransformed());

		return saveFile;

	}


	private void averageIntensity(ArrayList<File> files, File saveLocation) {
		ImagePlus result = null;

		int[][] pixelTotals = null;

		int stackSize = 0;
		int pixelSize = 0;


		int n = 0;


		for (File file : files) {
			ImagePlus current = open(file);
			n++;

			if (result == null) {
				result = current; //reuse the first image (which is setup) as our final data store


				stackSize = current.getStackSize();
				pixelSize = current.getWidth() * current.getHeight();
				pixelTotals = new int[pixelSize][stackSize];
			}

			//copy data across and do math

			for (int stack = 1; stack <= stackSize; stack++) {
				byte[] pixels = (byte[]) current.getStack().getProcessor(stack).getPixels();

				for (int i = 0; i < pixelSize; i++) {
					pixelTotals[i][stack - 1] += pixels[i] & 0xFF;
				}
			}


			if (current != result) {
				current.close();//close current after reading it (except if it is our results object)
			}
		}

		//now we need to transfer the info  into the results image

		for (int stack = 1; stack <= stackSize; stack++) {
			byte[] pixels = (byte[]) result.getStack().getProcessor(stack).getPixels();
			for (int i = 0; i < pixelSize; i++) {
				pixels[i] = (byte) (pixelTotals[i][stack - 1] / n);
			}
		}

		saveAndClose(saveLocation, result);
	}

	private static ImagePlus open(File file) {
		AmiraMeshReader_ reader = new AmiraMeshReader_();
		reader.run(file.getPath());
		return reader;
	}


	private static void saveAndClose(File saveFile, ImagePlus ip) {
		AmiraMeshEncoder e = new AmiraMeshEncoder(saveFile.getAbsolutePath());

		if (!e.open()) {
			IJ.error("Could not write " + saveFile.getAbsolutePath());
			ip.close();
		}

		if (!e.write(ip)) {
			IJ.error("Error writing " + saveFile.getAbsolutePath());
			ip.close();
		}

		ip.close();
	}


	private class Controllor implements ActionListener, Runnable {

		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals(RUN_BATCH)) {

				Thread thread = new Thread(this);
				thread.setPriority(Thread.NORM_PRIORITY-2); //run in a lower priority so it doesn't lock the machine up so much
                thread.start();
			} else {
				System.err.println("unkown action command sent to BatchProcessor's controllor");
			}
		}

		public void run() {
			if (amiraConvert.isSelected())
				setFiles(amiraConvert(getFiles()));
			if (scale.isSelected())
				setFiles(scale(getFiles(),
						((SpinnerNumberModel) width.getModel()).getNumber().intValue(),
						((SpinnerNumberModel) height.getModel()).getNumber().intValue()));
			if (rigidRegistration.isSelected()) {
				setFiles(miRigidRegress(getFiles(),
						new File(templateFileLocation.getText()),
						((SpinnerNumberModel) initialPos.getModel()).getNumber().intValue(),
						((SpinnerNumberModel) stopLevel.getModel()).getNumber().intValue(),
						((SpinnerNumberModel) startLevel.getModel()).getNumber().intValue(), 1));

			}

			if (averageIntensity.isSelected())
				averageIntensity(getFiles(), new File(averageIntensityLocation.getText()));
		}


	}
}

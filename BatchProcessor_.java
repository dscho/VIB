
import ij.plugin.PlugIn;
import ij.plugin.ImageCalculator;
import ij.IJ;
import ij.ImagePlus;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

import gui.GuiBuilder;
import imagescience.transforms.Affine;
import imagescience.images.ByteImage;
import adt.RunningStatistics;

/**
 * User: Tom Larkworthy
 * Date: 05-Jul-2006
 * Time: 12:16:26
 */
public class BatchProcessor_ extends JFrame implements PlugIn {


	JList fileList;

	public static final String RUN_BATCH = "run";

	JCheckBox amiraConvert, scale, rigidRegistration, summeryImages;

	JTextField amiraDirectory; //to put the amira files in

	JSpinner width, height; //scaling spinners

	JSpinner initialPos, startLevel, stopLevel; //Rigid Reg params
	JTextField templateFileLocation;

	JTextField summeryImagesName;

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


		summeryImages = GuiBuilder.addCheckBox(this, "calc average intensity & label distributions");
		summeryImagesName = GuiBuilder.addFileSaveField(this, "save location");

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


	private void generateSummeryImages(ArrayList<File> files, File saveLocation) {
		ImagePlus averageImage = null;
		HashMap<Byte, RunningStatistics> materialStats = new HashMap<Byte, RunningStatistics>();
		HashMap<Byte, String> materialNames = null;


		int[][] averagePixels = null;
		HashMap<Byte, byte[][]> materialPixels = new HashMap<Byte, byte[][]>();

		int stackSize = 0;
		int pixelSize = 0;

		int width = -1;
		int height = -1;


		int n = 0;

		for (File file : files) {

			n++;

			//process average image
			ImagePlus current = open(file);
			if (averageImage == null) {
				averageImage = current; //reuse the first image (which is setup) as our final data store for the average intensity values


				stackSize = current.getStackSize();
				pixelSize = current.getWidth() * current.getHeight();

				width = current.getWidth();
				height = current.getHeight();

				averagePixels = new int[pixelSize][stackSize];
			}

			//copy data across and do math
			for (int stack = 1; stack <= stackSize; stack++) {
				byte[] pixels = (byte[]) current.getStack().getProcessor(stack).getPixels();

				for (int i = 0; i < pixelSize; i++) {
					averagePixels[i][stack - 1] += pixels[i] & 0xFF;
				}
			}


			//process associate labels (if exists)

			ImagePlus currentLabels = open(getLabelFileFor(file));
			if (currentLabels != null) {

				System.out.println("found labels for " + file);

				//retreive names of the bytes while we have a file open, and if we havn't allready
				//an initialization routine onl called once
				if (materialNames == null) {
					materialNames = new HashMap<Byte, String>();
					AmiraParameters params = new AmiraParameters(currentLabels);
					String[] names = params.getMaterialList();
					for (int i = 0; i < names.length; i++) {
						materialNames.put((byte) i, names[i]);
					}
					System.out.println("materialNames = " + materialNames);
				}

				for (int stack = 1; stack <= stackSize; stack++) {
					IJ.showProgress(stack,  stackSize);
					byte[] currentLabelPixels = (byte[]) currentLabels.getStack().getProcessor(stack).getPixels();

					for (int i = 0; i < pixelSize; i++) {
						byte pixel = currentLabelPixels[i];

						if (pixel == -1) pixel = 0; //255 -> 1 Dunno where that comes from (boundary??)

						RunningStatistics stats = getRunningStats(pixel, materialStats);
						//record the intensity value of the coresponding pixel in the stastitics for that material
						stats.addData(((byte[]) current.getStack().getProcessor(stack).getPixels())[i] & 0xFF);
						byte[][] labelPixels = getLabelPixels(pixel, materialPixels, pixelSize, stackSize);
						labelPixels[i][stack - 1]++; //record the occurence

					}


				}
				for (RunningStatistics statistics : materialStats.values()) {
					statistics.endOfSequence();
				}

				currentLabels.close();
			} else {
				System.err.println("could not find labels file for " + file);
			}


			//close image data after use
			if (current != averageImage) {
				current.close();//close current after reading it (except if it is our results object)
			}


		}

		//now we need to transfer the average pixel data info  into the results image

		for (int stack = 1; stack <= stackSize; stack++) {
			byte[] pixels = (byte[]) averageImage.getStack().getProcessor(stack).getPixels();
			for (int i = 0; i < pixelSize; i++) {
				pixels[i] = (byte) (averagePixels[i][stack - 1] / n);
			}
		}

		saveAndClose(new File(saveLocation.getPath() + ".grey"), averageImage);

		//we also need to generate all the images for the labels
		for (Byte materialId : materialPixels.keySet()) {

			String materialName = materialNames.get(materialId);
			byte[][] labelPixels = materialPixels.get(materialId);

			System.out.println(IJ.freeMemory());
			ImagePlus saveImage = IJ.createImage(materialName, "8-bit", width, height, stackSize);

			for (int stack = 1; stack <= stackSize; stack++) {
				byte[] pixels = (byte[]) saveImage.getStack().getProcessor(stack).getPixels();
				for (int i = 0; i < pixelSize; i++) {
					pixels[i] = (byte) (255 * (double) labelPixels[i][stack - 1] / n);
				}
			}

			saveAndClose(new File(saveLocation.getPath() + "_" + materialName + ".grey"), saveImage);
		}

		//save a summery file
		saveSummery(saveLocation.getPath(), materialStats, materialNames, materialPixels);

	}

	private void saveSummery(String basePath, HashMap<Byte, RunningStatistics> materialStats, HashMap<Byte, String> materialNames, HashMap<Byte, byte[][]> pixels) {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(basePath + ".txt"));

			for (Byte materialId : materialStats.keySet()) {
				out.append(materialId.toString());
				out.append("\t");
				out.append(materialNames.get(materialId));
				out.append("\t");
				out.append(String.valueOf(materialStats.get(materialId).getMean()));
				out.append("\t");
				out.append(String.valueOf(materialStats.get(materialId).getVariance()));
				out.append("\t");
				out.append(String.valueOf(materialStats.get(materialId).getMeanSequenceLength()));
				out.append("\t");
				out.append(String.valueOf(materialStats.get(materialId).getVarianceSequenceLength()));

				if (pixels.get(materialId) != null) {
					out.append("\t");
					out.append(basePath + "_" + materialNames.get(materialId) + ".grey");
				}

				out.append("\n");
			}

			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * retrieves the pixel array for the specified material, creating one if one does not exist allready
	 *
	 * @param material
	 * @param materialPixelStore
	 * @param pixelSize
	 * @param stackSize
	 * @return
	 */
	private byte[][] getLabelPixels(byte material, HashMap<Byte, byte[][]> materialPixelStore, int pixelSize, int stackSize) {
		byte[][] pixels = materialPixelStore.get(new Byte(material));
		if (pixels == null) {
			pixels = new byte[pixelSize][stackSize];
			materialPixelStore.put(new Byte(material), pixels);
		}
		return pixels;
	}

	/**
	 * @return
	 */
	private RunningStatistics getRunningStats(byte material, HashMap<Byte, RunningStatistics> statsStore) {
		RunningStatistics stats = statsStore.get(new Byte(material));
		if (stats == null) {
			stats = new RunningStatistics();
			statsStore.put(new Byte(material), stats);
		}
		return stats;
	}

	/**
	 * returns the file
	 *
	 * @param file
	 * @return
	 */

	private File getLabelFileFor(File file) {
		int suffixIndex = file.getName().lastIndexOf(".grey");
		if (suffixIndex == -1) return null;

		String trimmed = file.getName().substring(0, suffixIndex);
		String labelName = trimmed + ".labels";

		File labelsFileLocation = new File(file.getParentFile(), labelName);
		System.out.println("labelsFileLocation = " + labelsFileLocation);
		return labelsFileLocation;
	}


	private static ImagePlus open(File file) {
		if (!file.exists()) return null;
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
		JComponent source;

		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals(RUN_BATCH)) {
				source = ((JComponent) e.getSource());
				source.setEnabled(false);
				Thread thread = new Thread(this);
				thread.setPriority(Thread.MIN_PRIORITY); //run in a lower priority so it doesn't lock the machine up so much
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

			if (summeryImages.isSelected())
				generateSummeryImages(getFiles(), new File(summeryImagesName.getText()));

			if (source != null) {
				source.setEnabled(true);
				source = null;
			}
		}


	}
}

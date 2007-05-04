import ij.IJ;
import ij.plugin.PlugIn;
import ij.gui.GenericDialog;
import ij.util.Tools;

import java.awt.*;
import java.awt.event.*;

import java.io.*;
import java.util.zip.*;
import java.util.Enumeration;
import java.util.Vector;

public class Install_Java3D implements PlugIn, ActionListener {
	
	private Button zipfile;
	private GenericDialog gd;
	private TextField tf;

	public void run(String arg) {
		File java_home = new File(System.getProperty("java.home"));
		
		gd = new GenericDialog("Install Java3D");

		gd.addMessage("Found a Java JRE in " + java_home + 
				"\nJava3D will be installed there" + 
				"\nPlease specify the downloaded Java3D zip " + 
				"file below");

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.insets = new Insets(20, 20, 0, 0);
		c.fill = GridBagConstraints.NONE;
		c.weightx = c.weighty = 0.0;
		zipfile = new Button("Select zip file");
		zipfile.addActionListener(this);
		// work around not being able to access gd.y
		Panel panel = new Panel();
		gd.addPanel(panel);
		gd.remove(panel);
		gd.add(zipfile, c);
		tf = new TextField(25);
		c.gridx++;
		c.fill = GridBagConstraints.HORIZONTAL;
		gd.add(tf, c);


		gd.showDialog();
		if(gd.wasCanceled())
			return;
		if(!checkDir(java_home) || !checkWrite(java_home))
			return;
		File file = new File(tf.getText());
		if(!file.isFile()) {
			IJ.error(file + " is not a regular file");
			return;
		}
		
		try {
			install(file, java_home);
			IJ.showMessage("Installation successfull");
		} catch(Exception e) {
			IJ.error("Error: " + e.getMessage());
			e.printStackTrace();
		}

	}

	public void install(File file, File java_home) throws Exception {
		File tempdir = new File(java_home, "tmp");
		tempdir.mkdir();
		
		Vector files = unzip(file, tempdir, "j3d-jre.zip");
		if(files.size() == 0) {
			IJ.error("Couldn't find any j3d-jre.zip in "
				+ "the archive");
			throw new Exception(
				"Could not find j3d-jre.zip");
		}
		File j3djre = (File)files.get(0);
		unzip(j3djre, java_home, null);

		tempdir.delete();
	}

	public Vector unzip(File zipfile, File dir, String fileToExtract) 
							throws Exception {
		ZipFile zfile = new ZipFile(zipfile);
		Enumeration en = zfile.entries();
		Vector extracted = new Vector();
		while(en.hasMoreElements()) {
			ZipEntry ze = (ZipEntry)en.nextElement();
			if(ze.isDirectory()) {
				File newDir = new File(dir, ze.getName());
				if(!newDir.exists()) {
					IJ.showStatus("Creating directory "
 							+ ze.getName());
					newDir.mkdir();
				} else if(!newDir.isDirectory()) {
					IJ.error("Cannot create " + newDir);
					throw new Exception();
				}
			} else if(fileToExtract == null || 
					ze.getName().endsWith(fileToExtract)) {
				IJ.showStatus("Extracting "+ze.getName());
				InputStream is = zfile.getInputStream(ze);
				File ext = new File(dir, ze.getName());
				FileOutputStream out = 
					new FileOutputStream(ext);
				int c = 0;
				int counter = 0;
				while((c = is.read()) != -1) {
					out.write(c);
					IJ.showProgress(counter++, 
						(int)ze.getSize());
				}
				out.flush();
				is.close();
				out.close();
				extracted.add(ext);
			}
		}
		return extracted;
	}

	private boolean checkDir(File f) {
		if(f.isDirectory())
			return true;
		IJ.error(f.getName() + "is not a valid directory");
		return false;
	}

	private boolean checkWrite(File f) {
		if(f.canWrite())
			return true;
		IJ.error("Can't write to " + f.getName() + 
			"\nMaybe you should be Administrator");
		return false;
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == zipfile) {
			FileDialog dialog =
				new FileDialog(gd, "Zip File");
			dialog.setVisible(true);
			String dir = dialog.getDirectory();
			String file = dialog.getFile();
			if (dir != null && file != null)
				tf.setText(dir + file);
		}
	}

	public static String[] getPaths(String env) {
		String ps = System.getProperty("path.separator");
		return Tools.split(System.getProperty(env), ps); 
	}
}

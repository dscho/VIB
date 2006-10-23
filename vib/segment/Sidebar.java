package vib.segment;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;
import ij.*;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import vib.AmiraParameters;
import vib.InterpolatedImage;

/**
 * Sidebar: 
 * This class build all the interface of the SegmentationEditor. It handle the
 * listeners too.
 * 
 * @author Francois KUSZTOS
 * @version 5
 */
public class Sidebar extends Panel implements CustomCanvas.CanvasListener {

	private CustomCanvas cc;
	
	private Font font = new Font("Helvetica", Font.PLAIN, 10);

	private GridBagConstraints constr;
	private Label lZoomLevel;
	private ImageButton bZoomPlus, bZoomMinus;
	private ImageButton bPlus, bMinus;
	private Checkbox check3d;
	private Choice labelImagesChoice;
	private Vector labelImages;


	private ActionListener al;
	private MaterialList materials;
	private InfoPanel pInfos;
	
	
	public Sidebar(CustomCanvas cc, ActionListener al) {
		this.cc = cc;
		this.al = al;

		cc.addCanvasListener(this);
			
		setLayout(new GridBagLayout());

		constr = new GridBagConstraints();
		constr.fill = GridBagConstraints.BOTH;
		constr.anchor = GridBagConstraints.WEST;
		constr.gridwidth = GridBagConstraints.REMAINDER;
		constr.insets = new Insets(0, 5, 0, 5);
		
		addLabel("Labels:");
		add(addLabelImageChoice(), constr);
		
		addLabel("Materials:");
		materials = new MaterialList(cc.getLabels());
		add(materials, constr);

		addZoom();
		addSelection();

		pInfos = new InfoPanel(font);
		constr.insets = new Insets(20, 5, 0, 5);
		add(pInfos, constr);
	}

	public void updateLZoomLevel(double magnification) {
		lZoomLevel.setText(String.valueOf(magnification));
	}

	public boolean is3d() {
		return check3d.getState();
	}
	
	public int currentMaterialID() {
		return materials.currentMaterialID();
	}

	public MaterialList getMaterials() {
		return materials;
	}

	public void magnificationChanged(double d) {
		updateLZoomLevel(d);
	}

	public void updateInfoPanel() {
		pInfos.updateLabels();
	}

	public void updateInfoPanel(int x, int y, int z, 
								double posX, double posY, double posZ,
								int voxelValue, String material) {
		pInfos.updateLabels(x, y, z, posX, posY, posZ, voxelValue, material);
	}

	public void setLabelImage(ImagePlus image) {
		if (image == null) {
			image = InterpolatedImage
						.cloneDimensionsOnly(cc.getImage(),ImagePlus.COLOR_256)
						.getImage();
			image.show();
			// TODO: get initial parameters
		}
		cc.setLabels(image);
		cc.repaint();
		if (materials != null) {
			materials.initFrom(image);
			materials.repaint();
		}
	}
	
	private ImageButton addImageButton(String path, ActionListener l) {
		URL url;
		Image img = null;
		try {
			url = getClass().getResource("icons/" + path);
			img = createImage((ImageProducer)url.getContent());
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch(Exception e) { 
			e.printStackTrace(); }
		if (img == null)
			throw new RuntimeException("Image not found: " + path);
		ImageButton button = new ImageButton(img);
		button.addActionListener(l);
		add(button, constr);
		return button;
	}

	private Label addLabel(String txt) {
		constr.insets = new Insets(10, 5, 0, 5);
		Label label = new Label(txt);
		label.setFont(font);
		add(label, constr);
		constr.insets = new Insets(0, 5, 0, 5);
		return label;
	}
	
	private Choice addLabelImageChoice() {
		labelImagesChoice = new Choice();
		labelImages = new Vector();
		int count = WindowManager.getWindowCount();

		// TODO: add image listener
		for (int i = 0; i < count; i++) {
			ImagePlus image = WindowManager.getImage(i + 1);
			if (!AmiraParameters.isAmiraLabelfield(image) ||
					image == cc.getImage() ||
					image.getWidth() != 
					cc.getImage().getWidth() ||
					image.getHeight() != 
					cc.getImage().getHeight() ||
					image.getStack().getSize() != 
					cc.getImage().getStack().getSize())
				continue;
			labelImagesChoice.add(image.getTitle());
			labelImages.add(image);
		}
		labelImagesChoice.add("<new>");
		labelImages.add(null);
		setLabelImage((ImagePlus)labelImages.get(0));
		
		labelImagesChoice.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				int selected =
				labelImagesChoice.getSelectedIndex();
				setLabelImage((ImagePlus)labelImages.get(selected));
			}
		});

		return labelImagesChoice;
	}
	

	private void addZoom() {
		addLabel("Zoom:");
		
		constr.gridwidth = 1;
		bZoomPlus = addImageButton("iconZoomPlus.png", al);
		bZoomPlus.setActionCommand("zoomin");
		bZoomMinus = addImageButton("iconZoomMinus.png", al);
		bZoomMinus.setActionCommand("zoomout");
		
		constr.gridwidth = GridBagConstraints.REMAINDER;
		constr.fill = GridBagConstraints.NONE;
		lZoomLevel = addLabel(String.valueOf(cc.getMagnification()));
		constr.fill = GridBagConstraints.BOTH;
	}
	
	private void addSelection() {
		constr.gridwidth = GridBagConstraints.REMAINDER;
		addLabel("Selection:");
		
		constr.gridwidth = 1;
		bPlus = addImageButton("iconPlus.png", al);
		bPlus.setActionCommand("plus");
		bMinus = addImageButton("iconMinus.png", al);
		bMinus.setActionCommand("minus");
		
		constr.gridwidth = GridBagConstraints.REMAINDER;
		constr.fill = GridBagConstraints.NONE;
		check3d = new Checkbox("3d", false);
		add(check3d, constr);
		constr.fill = GridBagConstraints.BOTH;
	}
}


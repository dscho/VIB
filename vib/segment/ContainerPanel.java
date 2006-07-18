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
import vib.InterpolatedImage;

/**
 * ContainerPanel: 
 * This class build all the interface of the Segmentation_Editor. It handle the
 * listeners too.
 * 
 * @author Francois KUSZTOS
 * @version 5
 */
public class ContainerPanel extends Panel {
	Window window;
	Roi[] savedRois;
	Segmentation_Editor.CustomCanvas cc;

	Choice labelImagesChoice;
	Vector labelImages;

	MainPanel pMain;
	InfosPanel pInfos;
	Font font = new Font("Helvetica", Font.PLAIN, 12);
	
	public ContainerPanel(Segmentation_Editor.CustomCanvas cc, Window window, Roi[] savedRois) {
		this.cc = cc;
		this.window = window;
		this.savedRois = savedRois;
		
		setLayout(new BorderLayout());

		pMain = new MainPanel();
		add(pMain, BorderLayout.NORTH);
		
		/*
		pTools = new ToolsPanel();
		add(pTools, BorderLayout.CENTER);
		*/
		
		pInfos = new InfosPanel();
		add(pInfos, BorderLayout.SOUTH);
	}

	void setLabelImage(ImagePlus image) {
		if (image == null) {
			image = InterpolatedImage.cloneDimensionsOnly(
					cc.getImage(),
					ImagePlus.COLOR_256).getImage();
			image.show();
			// TODO: get initial parameters
		}
		cc.setLabels(image);
		cc.repaint();
		if (pMain != null) {
			pMain.materials.initFrom(image);
			pMain.materials.repaint();
		}
	}

	//Build methods
	private Button makeButton(String name, Object constr, Panel pan, ActionListener listener) {
		Button button = new Button(name);
		button.addActionListener(listener);
		pan.add(button, constr);
		return button;
	}
	
	private ImageButton makeImageButton(String path, Object constr, Panel pan, ActionListener listener) {
		URL url;
		Image img = null;
		try {
			url = getClass().getResource("icons/" + path);
			img = createImage((ImageProducer)url.getContent());
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch(Exception e) { e.printStackTrace(); }
		if (img == null)
			throw new RuntimeException("image not found: " + path);
		ImageButton button = new ImageButton(img);
		button.addActionListener(listener);
		pan.add(button, constr);
		return button;
	}
	
	Label makeLabel(String txt, Object constr, Panel pan) {
		Label label = new Label(txt);
		label.setFont(font);
		pan.add(label, constr);
		return label;
	}

	private Scrollbar makeScrollbar(Object constr, Panel pan, int orientation, int value, int visible, int minimum, int maximum, AdjustmentListener listener) {
		Scrollbar scroll = new Scrollbar(orientation, value, visible, minimum, maximum);
		pan.add(scroll, constr);
		scroll.addAdjustmentListener(listener);
		return scroll;
	}
	
	public class MainPanel extends Panel implements ActionListener {
		GridBagConstraints constr;
		MaterialList materials;
		
		private Label lZoomLevel;
		private ImageButton bZoomPlus, bZoomMinus;
		private ImageButton bPlus, bMinus;
		private Checkbox check3d;
	
		public MainPanel() {
			setLayout(new GridBagLayout());

			constr = new GridBagConstraints();
			constr.fill = GridBagConstraints.BOTH;
			constr.gridwidth = GridBagConstraints.REMAINDER;
			
			addLabelImagesChoice();
			addMaterialAndZoom();
			addSelection();
		}
		
		void addLabelImagesChoice() {
			makeLabel(" ", constr, this);
			makeLabel("Labels:", constr, this);
			labelImagesChoice = new Choice();
			labelImages = new Vector();
			int count = WindowManager.getWindowCount();

			// TODO: add image listener
			for (int i = 0; i < count; i++) {
				ImagePlus image = WindowManager.getImage(i + 1);
				if (image == cc.getImage() ||
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
			add(labelImagesChoice, constr);
			labelImagesChoice.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent e) {
					int selected =
					labelImagesChoice.getSelectedIndex();
					setLabelImage((ImagePlus)labelImages.get(selected));
					}
					});
		}

	private void addMaterialAndZoom() {
			makeLabel(" ", constr, this);
			makeLabel("Materials:", constr, this);
			
			materials = new MaterialList(cc.getLabels());
			add(materials, constr);

			makeLabel(" ", constr, this);
			makeLabel("Zoom:", constr, this);
				
			constr.gridwidth = 1;
			constr.weightx = 1.0;
			bZoomPlus = makeImageButton("iconZoomPlus.png", constr, this, this);
			bZoomMinus = makeImageButton("iconZoomMinus.png", constr, this, this);
			makeLabel(" ", constr, this);	
			lZoomLevel = makeLabel(String.valueOf(cc.getMagnification()), constr, this);
			//bSave = makeImageButton("iconSaveAs.png", constr, this, listenerZoom);
			
			constr.gridwidth = GridBagConstraints.REMAINDER;
			makeLabel(" ", constr, this);
		}
		
		public void addSelection() {
			constr.weightx = 0.0;
			makeLabel("Selection:", constr, this);
				
			constr.gridwidth = 1;
			constr.weightx = 1.0;
			//bArrow = makeImageButton("iconArrow.png", constr, this, listenerSelection);
			//bLetterC = makeImageButton("iconLetterC.png", constr, this, listenerSelection);
			//bLetterR = makeImageButton("iconLetterR.png", constr, this, listenerSelection);
			bPlus = makeImageButton("iconPlus.png", constr, this, this);
			bMinus = makeImageButton("iconMinus.png", constr, this, this);
			check3d = new Checkbox("3d", false);
			add(check3d, constr);
				
			constr.gridwidth = GridBagConstraints.REMAINDER;
			makeLabel(" ", constr, this);
				
		}
		

		public void updateLZoomLevel(double magnification) {
			lZoomLevel.setText(String.valueOf(magnification));
		}

		public void actionPerformed(ActionEvent e) {
			Object b = e.getSource();
			if (b == bZoomPlus) {
				cc.zoomIn(cc.getWidth()/2, cc.getHeight()/2);
			} else if ( b == bZoomMinus) {
				cc.zoomOut(cc.getWidth()/2, cc.getHeight()/2);
			} else if ( b == bPlus) {
				int currentSlice = cc.getImage().getCurrentSlice();
				Roi roi = cc.getImage().getRoi();
				assignSliceTo(currentSlice,roi,materials.currentMaterialID());	
				cc.getImage().killRoi();
				if(check3d.getState()){
					for(int i=0;i<savedRois.length;i++){
						roi = savedRois[i];
						if(roi != null){
							assignSliceTo(i,roi,materials.currentMaterialID());
							savedRois[i] = null;
						}
					}
				}
				cc.getImage().setSlice(currentSlice);
				cc.getLabels().setSlice(currentSlice);
				cc.getImage().updateAndDraw();
				cc.getLabels().updateAndDraw();
			} else if (b == bMinus) {
				int currentSlice = cc.getImage().getCurrentSlice();
				Roi roi = cc.getImage().getRoi();
				releaseSliceFrom(currentSlice, roi, materials.currentMaterialID());
				cc.getImage().killRoi();
				if(check3d.getState()){
					for(int i=0;i<savedRois.length;i++){
						roi = savedRois[i];
						if(roi != null){
							releaseSliceFrom(i,roi,materials.currentMaterialID());
							savedRois[i] = null;
						}
					}
				}
				cc.getImage().setSlice(currentSlice);
				cc.getLabels().setSlice(currentSlice);
				cc.getImage().updateAndDraw();
				cc.getLabels().updateAndDraw();
			}
		}
		
		public void assignSliceTo(int slice, Roi roi, int materialID){
			ImagePlus grey = cc.getImage();
			ImagePlus labels = cc.getLabels();
			if (grey == null || labels == null)
				return;			
			if (roi == null)
				return;
			ImageProcessor labP = labels.getStack().getProcessor(slice);
			labP.setRoi(roi);
			Rectangle bounds = roi.getBoundingRect();
	        for(int i=bounds.x;i<=bounds.x+bounds.width;i++){
	            for(int j=bounds.y;j<=bounds.y+bounds.height;j++){
	                if(roi.contains(i,j)) labP.set(i,j,materialID);
	            }
	        }
	        cc.updateSlice(slice);
	    }
		
		public void releaseSliceFrom(int slice, Roi roi, int materialID){
			ImagePlus grey = cc.getImage();
			ImagePlus labels = cc.getLabels();
			if (grey == null || labels == null)
				return;			
			if (roi == null)
				return;
			ImageProcessor labP = labels.getStack().getProcessor(slice);
			labP.setRoi(roi);
			Rectangle bounds = roi.getBoundingRect();
	        for(int i=bounds.x;i<=bounds.x+bounds.width;i++){
	            for(int j=bounds.y;j<=bounds.y+bounds.height;j++){
	                if(roi.contains(i,j) && labP.get(i,j)==materialID){ 
	                	labP.set(i,j,materials.getDefaultMaterialID());
	                }
	            }
	        }
	        cc.updateSlice(slice);
		}
	}

	//Infos Panel
	public class InfosPanel extends Panel {
		Label lPosX, lPosY, lPosZ;
		Label lIndexX, lIndexY, lIndexZ;
		Label lMaterialName, lVoxelValueNum;
		
		public InfosPanel() {
			super();
			GridBagConstraints constr = new GridBagConstraints();
			constr.weightx = 1.0;
			
			setLayout(new GridBagLayout());
			
			makeLabel("Pos:", constr, this);
			lPosX = makeLabel("____X____", constr, this);
			lPosY = makeLabel("____Y____", constr, this);
			
			constr.gridwidth = GridBagConstraints.REMAINDER;
			lPosZ = makeLabel("____Z____", constr, this);

			constr.gridwidth = 1;
			makeLabel("Index:", constr, this);
			lIndexX = makeLabel(" X  ", constr, this);
			lIndexY = makeLabel(" Y  ", constr, this);
			
			constr.gridwidth = GridBagConstraints.REMAINDER;
			lIndexZ = makeLabel(" Z  ", constr, this);
			
			constr.gridwidth = 1;
			makeLabel("Material:", constr, this);
			
			constr.gridwidth = GridBagConstraints.REMAINDER;
			lMaterialName = makeLabel("  name   ", constr, this);
			
			constr.gridwidth = 1;
			makeLabel("Voxel Value:", constr, this);
			
			constr.gridwidth = GridBagConstraints.REMAINDER;
			lVoxelValueNum = makeLabel("______ ~ ______", constr, this);
		}

		public void updateLabels() {
			lIndexX.setText("    -    ");
			lIndexY.setText("    -    ");
			lIndexZ.setText("    -    ");
			lPosX.setText("    -    ");
			lPosX.setText("    -    ");
			lPosX.setText("    -    ");
			lMaterialName.setText("    -    ");
			lVoxelValueNum.setText("    -    ");
		}

		public void updateLabels(int x, int y, int z,
				double posX, double posY, double posZ,
				int value, String name) {
			lIndexX.setText("" + x);
			lIndexY.setText("" + y);
			lIndexZ.setText("" + z);
			lPosX.setText("" + posX);
			lPosY.setText("" + posY);
			lPosZ.setText("" + posZ);
			lMaterialName.setText(name);
			lVoxelValueNum.setText("" + value);
		}
	}
}

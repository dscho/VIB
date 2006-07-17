package vib.segment;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;
import ij.*;
import ij.measure.Calibration;
import ij.gui.GenericDialog;
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
	Segmentation_Editor.CustomCanvas cc;

	Choice labelImagesChoice;
	Vector labelImages;

	MainPanel pMain;
	ToolsPanel pTools;
	InfosPanel pInfos;
	Font font = new Font("Helvetica", Font.PLAIN, 12);
	
	public ContainerPanel(Segmentation_Editor.CustomCanvas cc, Window window) {
		this.cc = cc;
		this.window = window;
		
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
		} catch(Exception e) {}
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
	
	private TextField makeTextField (String init, Object constr, Panel pan) {
		TextField text = new TextField(init);
		text.setEditable(true);
		text.setColumns(3);
		pan.add(text, constr);
		return text;
	}

	private Checkbox makeCheckbox (String text, boolean selected, Object constr, Panel pan) {
		Checkbox check = new Checkbox(text, selected);
		pan.add(check, constr);
		return check;
	}


	public class MainPanel extends Panel {
		Color color = Color.GRAY;
		GridBagConstraints constr;
		MaterialList materials;
		
		ListenerZoom listenerZoom;
		ListenerSelection listenerSelection;
		ListenerTools listenerTools;
		
		private Label lMaterials, lZoom, lSelection, lTools, lZoomLevel;
		
		private ImageButton bZoomPlus, bZoomMinus, bSave, bArrow, bLetterC, bLetterR;
		private ImageButton bPlus, bMinus, bBrush, bLasso, bMagicWand;
		private ImageButton bPropagatingContour, bBlowTool, bCrossHair;
		private Checkbox check3d;
	
		public MainPanel() {
			setLayout(new GridBagLayout());

			constr = new GridBagConstraints();
			constr.fill = GridBagConstraints.BOTH;
			constr.gridwidth = GridBagConstraints.REMAINDER;
			
			addLabelImagesChoice();
			addMaterialAndZoom();
			addSelection();
			//addTools();
		}
		
		void addLabelImagesChoice() {
			makeLabel(" ", constr, this);
			lMaterials = makeLabel("Labels:", constr, this);
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
			listenerZoom = new ListenerZoom();
			makeLabel(" ", constr, this);
			lMaterials = makeLabel("Materials:", constr, this);
			
			materials = new MaterialList();
			materials.initFrom(cc.getLabels());
			add(materials, constr);

			makeLabel(" ", constr, this);
			lZoom = makeLabel("Zoom:", constr, this);
				
			constr.gridwidth = 1;
			constr.weightx = 1.0;
			bZoomPlus = makeImageButton("iconZoomPlus.png", constr, this, listenerZoom);
			bZoomMinus = makeImageButton("iconZoomMinus.png", constr, this, listenerZoom);
			makeLabel(" ", constr, this);	
			lZoomLevel = makeLabel(String.valueOf(cc.getMagnification()), constr, this);
			//bSave = makeImageButton("iconSaveAs.png", constr, this, listenerZoom);
			
			constr.gridwidth = GridBagConstraints.REMAINDER;
			makeLabel(" ", constr, this);
		}
		
		public void addSelection() {
			listenerSelection = new ListenerSelection();
			
			constr.weightx = 0.0;
			lSelection = makeLabel("Selection:", constr, this);
				
			constr.gridwidth = 1;
			constr.weightx = 1.0;
			//bArrow = makeImageButton("iconArrow.png", constr, this, listenerSelection);
			//bLetterC = makeImageButton("iconLetterC.png", constr, this, listenerSelection);
			//bLetterR = makeImageButton("iconLetterR.png", constr, this, listenerSelection);
			bPlus = makeImageButton("iconPlus.png", constr, this, listenerSelection);
			bMinus = makeImageButton("iconMinus.png", constr, this, listenerSelection);
			Checkbox check3d = new Checkbox("3d", false);
			add(check3d, constr);
				
			constr.gridwidth = GridBagConstraints.REMAINDER;
			makeLabel(" ", constr, this);
				
		}
		
		public void addTools() {
			listenerTools = new ListenerTools();
			
			constr.weightx = 0.0;
			lTools = makeLabel("Tools:", constr, this);
				
			constr.gridwidth = 1;
			constr.weightx = 1.0;
			bBrush = makeImageButton("iconBrush.png", constr, this, listenerTools);
			bLasso = makeImageButton("iconLasso.png", constr, this, listenerTools);
			bMagicWand = makeImageButton("iconMagicWand.png", constr, this, listenerTools);
			bPropagatingContour = makeImageButton("iconPropagatingContour.png", constr, this, listenerTools);
			bBlowTool = makeImageButton("iconBlowTool.png", constr, this, listenerTools);
			
			constr.gridwidth = GridBagConstraints.REMAINDER;
			bCrossHair = makeImageButton("iconCrossHair.png", constr, this, listenerTools);
		}
		
		public Color getColor() {
			return color;
		}
		
		public void updateLZoomLevel(double magnification) {
			lZoomLevel.setText(String.valueOf(magnification));
		}
		
		
		class ListenerZoom implements ActionListener {
			
			public ListenerZoom() {}
			
			public void actionPerformed(ActionEvent e) {
				Object b = e.getSource();
				if( b == bZoomPlus ) {
					cc.zoomIn(cc.getWidth()/2, cc.getHeight()/2); //zoom from the center of the image
				} else if( b == bZoomMinus ) {
					cc.zoomOut(cc.getWidth()/2, cc.getHeight()/2);
				} else {
					// b == bSave
					
				}
			}
		} //SegementationEditorMainPanel.ListenerZoom inner class
		
		
		class ListenerSelection implements ActionListener {
			
			public ListenerSelection() {}
			
			public void actionPerformed(ActionEvent e) {
				Object b = e.getSource();
				if( b == bArrow ) {
					
				} else if( b == bLetterC ) {
						
				} else if( b == bLetterR ) {
							
				} else if( b == bPlus ) {
								
				} else {
						// b == bMinus
					
				}			
			}
		} //SegementationEditorMainPanel.ListenerSelection inner class
		
		
		class ListenerTools implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				Object button = e.getSource();
				if (button == bBrush)
					pTools.card.show(pTools, "brush");
				else if (button == bLasso)
					pTools.card.show(pTools, "lasso");
				else if (button == bMagicWand)
					pTools.card.show(pTools, "magicWand");
				else if (button == bPropagatingContour)
					pTools.card.show(pTools, "propagatingContour");
				else if (button == bBlowTool)
					pTools.card.show(pTools, "blowTool");
				else //b == bCrossHair
					pTools.card.show(pTools, "crossHair");
			}
		}
		
	}
	
	//Tools Panel
	public class ToolsPanel extends Panel {

		Color color = Color.WHITE;
		
		CardLayout card;
		
		GridBagConstraints constrBrush, constrLasso, constrMagicWand, constrPropagatingContour, constrBlowTool;
		
		ToolListener listener = new ToolListener();
		
		AdjustmentListener4Scroll adjustmentListener;
		
		
		Panel pContainer, pBrush, pLasso, pMagicWand, pPropagatingContour, pBlowTool, pCrossHair;
		
		Scrollbar sBrush, sPropagatingContour;
		
		Label lBrush, lLasso, lMagicWand, lPropagatingContour, lBlowTool, lCrossHair, lMagicWandBlank,
				lMagicWandDrawLimitLine, lPropagatingContourTime, lBlowToolTolerance, lBlowToolGaussWidth;
		
		Button bPropagatingContourMenu, bPropagatingContourClear, bPropagatingContourDolt;
		
		ImageButton bBrush1px, bBrush3px, bBrush5px, bBrush7px, bBrush10px, bBrush15px;
		
		TextField tBrush, tMagicWand1, tMagicWand2, tPropagatingContour, tBlowToolTolerance, tBlowToolGaussWidth;
		
		Checkbox cLassoAutoTrace, cLassoTraceEdges, cMagicWandAbsoluteValues, cMagicWandSameMaterialOnly,
				cMagicWandFillInterior;

		public ToolsPanel() {
			super();
			card = new CardLayout();
			setLayout(card);
			//setBackground(color);
			//setSize(dimension);
			
			addBrushPanel();
			addLassoPanel();
			addMagicWandPanel();
			addPropagatingContourPanel();
			addBlowToolPanel();
			addCrossHairPanel();
			
			UpdateScroll us = new UpdateScroll();
			us.start();
			
			card.show(this, "brush");
		}
		
		public void addBrushPanel() {
			adjustmentListener = new AdjustmentListener4Scroll();
			
			pBrush = new Panel();
			//pBrush.setBackground(color);
			
			constrBrush = new GridBagConstraints();
			constrBrush.fill = GridBagConstraints.BOTH;
			pBrush.setLayout(new GridBagLayout());

			constrBrush.gridwidth = GridBagConstraints.REMAINDER;
			lBrush = makeLabel("Brush", constrBrush, pBrush);
			
			constrBrush.gridwidth = GridBagConstraints.RELATIVE;
			constrBrush.weightx = 4.0;
			sBrush = makeScrollbar(constrBrush, pBrush, Scrollbar.HORIZONTAL, 10, 5, 0, 250, adjustmentListener);
			
			constrBrush.gridwidth = GridBagConstraints.REMAINDER;
			constrBrush.weightx = 2.0;
			tBrush = makeTextField(String.valueOf(sBrush.getValue()), constrBrush, pBrush);

			constrBrush.gridwidth = 1;
			constrBrush.weightx = 1.0;
			bBrush1px = makeImageButton("iconBrush-1px.png", constrBrush, pBrush, listener);
			bBrush3px = makeImageButton("iconBrush-3px.png", constrBrush, pBrush, listener);
			bBrush5px = makeImageButton("iconBrush-5px.png", constrBrush, pBrush, listener);
			bBrush7px = makeImageButton("iconBrush-7px.png", constrBrush, pBrush, listener);
			bBrush10px = makeImageButton("iconBrush-10px.png", constrBrush, pBrush, listener);
			
			constrBrush.gridwidth = GridBagConstraints.REMAINDER;
			bBrush15px = makeImageButton("iconBrush-15px.png", constrBrush, pBrush, listener);
			
			add(pBrush, "brush");
		}
		
		public void addLassoPanel() {
			pLasso = new Panel();
			//pLasso.setBackground(color);
			
			constrLasso = new GridBagConstraints();
			constrLasso.fill = GridBagConstraints.BOTH;
			pLasso.setLayout(new GridBagLayout());

			constrLasso.gridwidth = GridBagConstraints.REMAINDER;
			lLasso = makeLabel("Lasso", constrLasso, pLasso);
			
			constrLasso.gridwidth = GridBagConstraints.REMAINDER;
			cLassoAutoTrace = makeCheckbox("auto trace", true, constrLasso, pLasso);
			cLassoTraceEdges = makeCheckbox("trace edges", true, constrLasso, pLasso);
			
			add(pLasso, "lasso");
		}
		
		public void addMagicWandPanel() {
			pMagicWand = new Panel();
			//pMagicWand.setBackground(color);
			
			constrMagicWand = new GridBagConstraints();
			constrMagicWand.fill = GridBagConstraints.BOTH;
			pMagicWand.setLayout(new GridBagLayout());

			constrMagicWand.gridwidth = GridBagConstraints.REMAINDER;
			lMagicWand = makeLabel("Magic Wand", constrMagicWand, pMagicWand);
			
			constrMagicWand.gridwidth = 1;
			constrMagicWand.weightx = 1.0;
			tMagicWand1 = makeTextField("-2", constrMagicWand, pMagicWand);
			
			tMagicWand2 = makeTextField("10", constrMagicWand, pMagicWand);
			
			constrMagicWand.gridwidth = GridBagConstraints.REMAINDER;
			lMagicWandBlank = makeLabel(" ", constrMagicWand, pMagicWand);
			
			cMagicWandAbsoluteValues = makeCheckbox("absolute values", false, constrMagicWand, pMagicWand);
			cMagicWandSameMaterialOnly = makeCheckbox("same material only", false, constrMagicWand, pMagicWand);
			cMagicWandFillInterior = makeCheckbox("fill interior", false, constrMagicWand, pMagicWand);
			
			lMagicWandDrawLimitLine = makeLabel("Draw limit line", constrMagicWand, pMagicWand);
			
			add(pMagicWand, "magicWand");
		}
		
		public void addPropagatingContourPanel() {
			pPropagatingContour = new Panel();
			//pPropagatingContour.setBackground(color);
			
			constrPropagatingContour = new GridBagConstraints();
			constrPropagatingContour.fill = GridBagConstraints.BOTH;
			pPropagatingContour.setLayout(new GridBagLayout());

			constrPropagatingContour.gridwidth = GridBagConstraints.REMAINDER;
			lPropagatingContour = makeLabel("Propagating Contour", constrPropagatingContour, pPropagatingContour);
			
			constrPropagatingContour.gridwidth = 1;
			constrPropagatingContour.weightx = 1.0;
			lPropagatingContourTime = makeLabel("Time", constrPropagatingContour, pPropagatingContour);
			
			constrPropagatingContour.weightx = 2.0;
			sPropagatingContour = makeScrollbar(constrPropagatingContour, pPropagatingContour, Scrollbar.HORIZONTAL, 5, 5, 0, 100, adjustmentListener);
			
			constrPropagatingContour.weightx = 1.0;
			constrPropagatingContour.gridwidth = GridBagConstraints.REMAINDER;
			tPropagatingContour = makeTextField("5", constrPropagatingContour, pPropagatingContour);
			
			constrPropagatingContour.weightx = 0.0;
			bPropagatingContourMenu = makeButton("Menu", constrPropagatingContour, pPropagatingContour, listener);
			bPropagatingContourClear = makeButton("Clear", constrPropagatingContour, pPropagatingContour, listener);
			bPropagatingContourDolt = makeButton("Dolt", constrPropagatingContour, pPropagatingContour, listener);
			
			add(pPropagatingContour, "propagatingContour");
		}
		
		public void addBlowToolPanel() {
			pBlowTool = new Panel();
			//pBlowTool.setBackground(color);

			constrBlowTool = new GridBagConstraints();
			pBlowTool.setLayout(new GridBagLayout());

			constrBlowTool.gridwidth = GridBagConstraints.REMAINDER;
			lBlowTool = makeLabel("Blow Tool", constrBlowTool, pBlowTool);
			
			constrBlowTool.gridwidth = 1;
			constrBlowTool.weightx = 1.0;
			lBlowToolTolerance = makeLabel("Tolerance:", constrBlowTool, pBlowTool);
			
			constrBlowTool.gridwidth = GridBagConstraints.REMAINDER;
			tBlowToolTolerance = makeTextField("35", constrBlowTool, pBlowTool);
			
			constrBlowTool.gridwidth = 1;
			lBlowToolGaussWidth = makeLabel("Gauss Width:", constrBlowTool, pBlowTool);
			
			constrBlowTool.gridwidth = GridBagConstraints.REMAINDER;
			tBlowToolGaussWidth = makeTextField("4", constrBlowTool, pBlowTool);
			
			add(pBlowTool, "blowTool");
		} //  ContainerPanel.ToolsPanel construtor
		
		public void addCrossHairPanel() {
			pCrossHair = new Panel();
			//pCrossHair.setBackground(color);

			lCrossHair = new Label("Cross Hair");
			//lCrossHair.setBackground(color);
			pCrossHair.add(lCrossHair);
			
			add(pCrossHair, "crossHair");
		}
		
		public Color getColor() {
			return color;
		}
		
//		sBrush, bBrush1px, bBrush3px, bBrush5px, bBrush7px, bBrush10px, bBrush15px, tBrush		
		class ToolListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				Object b = e.getSource();
				if(b == bBrush1px) {
					tBrush.setText("1");
				} else if(b == bBrush3px) {
					tBrush.setText("3");
				} else if(b == bBrush5px) {
					tBrush.setText("5");
				} else if(b == bBrush7px) {
					tBrush.setText("7");
				} else if(b == bBrush10px) {
					tBrush.setText("10");
				} else if(b == bBrush15px) {
					tBrush.setText("15");
				}
			}
		}
		
		/**
		 * This thread updates the scrollbars values in case the TextArea content linked with
		 * is changed.
		 */
		class UpdateScroll extends Thread {
			public void run() {
				while(true) {
					try {
						sleep(500);
					} catch(Exception e) {
						IJ.write("Error : the updating of the scollbars is crashed");
						break;
					}
					try { // Necessary precaution in case the TextArea is empty
						sBrush.setValue(Integer.valueOf(tBrush.getText()).intValue());
						sPropagatingContour.setValue(Integer.valueOf(tPropagatingContour.getText()).intValue());
					} catch(Exception e) {}
				}
			}
			
		}
		
		class AdjustmentListener4Scroll implements AdjustmentListener {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				Object b = e.getSource();
				if(b == sBrush) {
					tBrush.setText(String.valueOf(sBrush.getValue()));
				} else { // b == sPropagationContour
					tPropagatingContour.setText(String.valueOf(sPropagatingContour.getValue()));
				}
			}
		}
	} // ToolsPanel inner class

	//Infos Panel
	public class InfosPanel extends Panel {

		Color color = Color.LIGHT_GRAY;
		
		Label lPos, lPosX, lPosY, lPosZ, lIndex, lIndexX, lIndexY, lIndexZ, lMaterial,
				lMaterialName, lVoxelValue, lVoxelValueNum;
		
		public InfosPanel() {
			super();
			//setBackground(color);
			
			GridBagConstraints constr = new GridBagConstraints();
			
			setLayout(new GridBagLayout());
			
			constr.weightx = 1.0;
			lPos = makeLabel("Pos:", constr, this);
			lPosX = makeLabel("____X____", constr, this);
			lPosY = makeLabel("____Y____", constr, this);
			
			constr.gridwidth = GridBagConstraints.REMAINDER;
			lPosZ = makeLabel("____Z____", constr, this);

			constr.gridwidth = 1;
			lIndex = makeLabel("Index:", constr, this);
			lIndexX = makeLabel(" X  ", constr, this);
			lIndexY = makeLabel(" Y  ", constr, this);
			
			constr.gridwidth = GridBagConstraints.REMAINDER;
			lIndexZ = makeLabel(" Z  ", constr, this);
			
			constr.gridwidth = 1;
			lMaterial = makeLabel("Material:", constr, this);
			
			constr.gridwidth = GridBagConstraints.REMAINDER;
			lMaterialName = makeLabel("  name   ", constr, this);
			
			constr.gridwidth = 1;
			lVoxelValue = makeLabel("Voxel Value:", constr, this);
			
			constr.gridwidth = GridBagConstraints.REMAINDER;
			lVoxelValueNum = makeLabel("______ ~ ______", constr, this);
		}
		
		public Color getColor() {
			return color;
		}
		
		/**
		 * This method updates the labels of the InfosPanel with the values spent in
		 * parameter.
		 * @param posX		the horizontal position of the mouse - double
		 * @param posY		the vertical position of the mouse - double
		 * @param posZ		the depth of the photo (in touch with the slices) - double
		 * @param value		the voxel value - String
		 */
		public void updateLabels(int x, int y, int z, double posX, double posY, double posZ, String value) {
			lIndexX.setText(String.valueOf(x));
			lIndexY.setText(String.valueOf(y));
			lIndexZ.setText(String.valueOf(z));
			lPosX.setText(String.valueOf(posX));
			lPosY.setText(String.valueOf(posY));
			lPosZ.setText(String.valueOf(posZ));
			lVoxelValueNum.setText(value);
		}
		
		/**
		 * This method updates the labels of the InfosPanel with the values spent in
		 * parameter.
		 * @param posX		the horizontal position of the mouse - String
		 * @param posY		the vertical position of the mouse - String
		 * @param posZ		the depth of the photo (in touch with the slices) - String
		 * @param value		the voxel value - String
		 */
		public void updateLabels(String posX, String posY, String posZ, String value) {
			lIndexX.setText(String.valueOf("   -   "));
			lIndexY.setText(String.valueOf("   -   "));
			lIndexZ.setText(String.valueOf("   -   "));
			lPosX.setText(posX);
			lPosY.setText(posY);
			lPosZ.setText(posZ);
			lVoxelValueNum.setText(value);
		}
		
	} // ContainerPanel.InfosPanel inner class

} // ContainerPanel

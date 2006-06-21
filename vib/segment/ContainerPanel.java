package vib.segment;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;
import ij.*;
import ij.measure.Calibration;

/**
 * ContainerPanel: 
 * This class build all the interface of the Segmentation_Editor. It handle the
 * listeners too.
 * 
 * @author Francois KUSZTOS
 * @version 3.0
 */
public class ContainerPanel extends Panel {
	
	Segmentation_Editor.CustomCanvas cc;
	MainPanel pMain;
	ToolsPanel pTools;
	InfosPanel pInfos;
	Font font = new Font("Helvetica", Font.PLAIN, 12);
	
	public ContainerPanel(Segmentation_Editor.CustomCanvas cc) {
		this.cc = cc;
		
		setLayout(new BorderLayout());
		setBackground(Color.LIGHT_GRAY);
		
		pMain = new MainPanel();
		add(pMain, BorderLayout.NORTH);
		
		pTools = new ToolsPanel();
		add(pTools, BorderLayout.CENTER);
		
		pInfos = new InfosPanel();
		add(pInfos, BorderLayout.SOUTH);
	}

/*
	public Dimension getMinimumSize() {
		return getSize();
	}
*/

	//Build methods
	public Button makeButton(String name, Object constr, Panel pan, ActionListener listener, Color bkgColor) {
		Button button = new Button(name);
		button.addActionListener(listener);
		button.setBackground(bkgColor);
		pan.add(button, constr);
		return button;
	}
	
	public ImageButton makeImageButton(String path, Object constr, Panel pan, ActionListener listener, Color bkgColor) {
		URL url;
		Image img = null;
		try {
			url = getClass().getResource("icons/" + path);
			img = createImage((ImageProducer)url.getContent());
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch(Exception e) {}
		ImageButton button = new ImageButton(img);
		button.setBackground(bkgColor);
		button.addActionListener(listener);
		pan.add(button, constr);
		return button;
	}
	
	public Label makeLabel(String txt, Object constr, Panel pan, Color bkgColor) {
		Label label = new Label(txt);
		label.setBackground(bkgColor);
		label.setFont(font);
		pan.add(label, constr);
		return label;
	}

	public Scrollbar makeScrollbar(Object constr, Panel pan, int orientation, int value, int visible, int minimum, int maximum, AdjustmentListener listener, Color bkgColor) {
		Scrollbar scroll = new Scrollbar(orientation, value, visible, minimum, maximum);
		scroll.setBackground(bkgColor);
		pan.add(scroll, constr);
		scroll.addAdjustmentListener(listener);
		return scroll;
	}
	
	public TextField makeTextField (String init, Object constr, Panel pan, Color bkgColor) {
		TextField text = new TextField(init);
		text.setBackground(bkgColor);
		text.setEditable(true);
		text.setColumns(3);
		pan.add(text, constr);
		return text;
	}

	public Checkbox makeCheckbox (String text, boolean selected, Object constr, Panel pan, Color bkgColor) {
		Checkbox check = new Checkbox(text, selected);
		check.setBackground(bkgColor);
		pan.add(check, constr);
		return check;
	}


	//Main Panel
	public class MainPanel extends Panel {

		Color color = Color.LIGHT_GRAY;
		GridBagConstraints constr;
		Labelizer iLabelizer;
		MatList2 data;
		
		ListenerZoom listenerZoom;
		ListenerSelection listenerSelection;
		ListenerTools listenerTools;
		
		Label lBlank, lBlank2, lBlank3, lBlank4, lBlank5;
		Label lMaterials, lZoom, lSelection, lTools, lZoomLevel;
		
		ImageButton bZoomPlus, bZoomMinus, bArrow, bLetterC, bLetterR;
		ImageButton bPlus, bMinus, bBrush, bLasso, bMagicWand;
		ImageButton bPropagatingContour, bBlowTool, bCrossHair;
		
	
		public MainPanel() {
			setLayout(new GridBagLayout());
			setBackground(color);

			constr = new GridBagConstraints();
			constr.fill = GridBagConstraints.BOTH;
			constr.gridwidth = GridBagConstraints.REMAINDER;
			
			iLabelizer = new Labelizer(this);
			addMaterialAndZoom();
			addSelection();
			addTools();
		}
		
		public void addMaterialAndZoom() {
			listenerZoom = new ListenerZoom();
			
			lMaterials = makeLabel("Materials:", constr, this, color);
			
			data = new MatList2();
			add(data, constr);

			lBlank = makeLabel(" ", constr, this, color);
			lZoom = makeLabel("Zoom:", constr, this, color);
				
			constr.gridwidth = 1;
			constr.weightx = 1.0;
			bZoomPlus = makeImageButton("iconZoomPlus.png", constr, this, listenerZoom, color);
			bZoomMinus = makeImageButton("iconZoomMinus.png", constr, this, listenerZoom, color);
			lBlank2 = makeLabel(" ", constr, this, color);	
			
			constr.weightx = 2.0;
			lZoomLevel = makeLabel(String.valueOf(cc.getMagnification()), constr, this, color);
			
			constr.gridwidth = GridBagConstraints.REMAINDER;
			constr.weightx = 1.0;
			lBlank3 = makeLabel(" ", constr, this, color);
		}
		
		public void addSelection() {
			listenerSelection = new ListenerSelection();
			
			constr.weightx = 0.0;
			lSelection = makeLabel("Selection:", constr, this, color);
				
			constr.gridwidth = 1;
			constr.weightx = 1.0;
			bArrow = makeImageButton("iconArrow.png", constr, this, listenerSelection, color);
			bLetterC = makeImageButton("iconLetterC.png", constr, this, listenerSelection, color);
			bLetterR = makeImageButton("iconLetterR.png", constr, this, listenerSelection, color);
			bPlus = makeImageButton("iconPlus.png", constr, this, listenerSelection, color);
			bMinus = makeImageButton("iconMinus.png", constr, this, listenerSelection, color);
				
			constr.gridwidth = GridBagConstraints.REMAINDER;
			lBlank4 = makeLabel(" ", constr, this, color);
				
		}
		
		public void addTools() {
			listenerTools = new ListenerTools();
			
			constr.weightx = 0.0;
			lTools = makeLabel("Tools:", constr, this, color);
				
			constr.gridwidth = 1;
			constr.weightx = 1.0;
			bBrush = makeImageButton("iconBrush.png", constr, this, listenerTools, color);
			bLasso = makeImageButton("iconLasso.png", constr, this, listenerTools, color);
			bMagicWand = makeImageButton("iconMagicWand.png", constr, this, listenerTools, color);
			bPropagatingContour = makeImageButton("iconPropagatingContour.png", constr, this, listenerTools, color);
			bBlowTool = makeImageButton("iconBlowTool.png", constr, this, listenerTools, color);
			
			constr.gridwidth = GridBagConstraints.REMAINDER;
			bCrossHair = makeImageButton("iconCrossHair.png", constr, this, listenerTools, color);
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
				} else {
					// b == bZoomMinus
					cc.zoomOut(cc.getWidth()/2, cc.getHeight()/2);
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
		
		
/*		class MatList extends Panel {
			
			Object materials[][];
			int indice;
			
			public MatList() {
				super();
				setSize(new Dimension(180, 100));
				setBackground(Color.WHITE);
				setVisible(true);
				setLayout(new GridLayout(9,1));
				addMaterial("Exterior", Color.GRAY);
				addMaterial("Internal", Color.ORANGE);
				addMaterial("Other", Color.RED);
				addMaterial("?", Color.BLUE);
				addMaterial("azeokbn", Color.GREEN);
				addMaterial("adg", Color.CYAN);
				addMaterial("dfgqsf", Color.MAGENTA);
				addMaterial("qfq", Color.PINK);
				addMaterial("dqfqdfqqdjkl", Color.YELLOW);
				doLayout();
			}
			
			public void addMaterial(String name, Color selectionColor) {
				add(new Box(name, selectionColor));
			}
			
			class Box extends Panel {
				
				private Rect rectangle;
				private Color selectionColor;
				private Label name;
				private long id;
				
				public Box(String str, Color selectionColor) {
					super();
					//setSize(new Dimension(180, 26));
					setBackground(Color.WHITE);
					
					id = System.currentTimeMillis();
					name = new Label(str);
					name.setFont(font);

					this.selectionColor = selectionColor;
					rectangle = new Rect(selectionColor);
					
					GridBagConstraints constr = new GridBagConstraints();
					
					setLayout(new GridBagLayout());
					constr.fill = GridBagConstraints.BOTH;
					
					constr.weightx = 1.0;
					add(rectangle, constr);
					
					constr.weightx = 4.0;
					constr.gridwidth = GridBagConstraints.REMAINDER;
					add(name, constr);
				}
				
				public void setSelectionColor(Color c) {
					selectionColor = c;
					repaint();
				}
				
				public void setLayerName(String str) {
					name.setText(str);
				}
				
				class Rect extends Panel {
					
					Color color;
					
					public Rect(Color c) {
						super();
						color = c;
						setBackground(Color.WHITE);
						setSize(new Dimension(35, 26));
						setVisible(true);
						repaint();
					}
					
					public void paint(Graphics g) {
						g.setColor(color);
						g.fillRect(5, 4, 28, 18);
					}
					
				} // ContainerPanel.MainPanel.MatList.Box.Rect inner class
				
			} // ContainerPanel.MainPanel.MatList.Box inner class
			
		} // ContainerPanel.MainPanel.MatList inner class
*/		
		
		class MatList2 extends List implements MouseListener, ItemListener, ActionListener {
			
			PopupMenu popup;
			
			public MatList2() {
				super(6, false);
				add("Exterior");
				add("Inside");
				addMaterial();
				addMaterial();
				delMaterial();
				addMaterial();
				addMaterial();
				addMaterial();
				addMaterial();
				addMaterial();
				addMaterial();
				addMaterial();
				addMaterial();
				addMouseListener(this);
				createPopup();
			}
			
			public void createPopup() { // create the PopupMenu which appear when a right-click is done
				PopupMenu underPopup = new PopupMenu();
		   		CheckboxMenuItem ci;
		   		MenuItem mi;
				String uItemList[] = {
					"invisible", "contour", "hatched",
					"dotted", "light dotted", "-", "3D view"
				};
				// 0=false, 1=true, 2=null
				int uItemListChecked[] = {0, 1, 0, 0, 0, 2, 0};
		   		for (int i = 0; i < uItemList.length; i++) {
		   			if (uItemList[i].equals("-"))
		   				underPopup.addSeparator();
		   			else if (!uItemList[i].equals("")) {
		   				boolean checked;
		   				if(uItemListChecked[i] == 0)
		   					checked = false;
		   				else checked = true;
		   				ci = new CheckboxMenuItem(uItemList[i], checked);
		   				ci.addItemListener(this);
		   				underPopup.add(ci);
		   			}
		   		}
		   		underPopup.setLabel("Draw Style");

				String itemList[] = {
					"Locate", "Delete Material",
					"Rename Material", "Edit Color",
					"Lock Material", "-", "New Material",
					"Lock All"
				};
				popup = new PopupMenu();
				add(popup);
				popup.add(underPopup);
		   		for(int i = 0; i < itemList.length; i++) {
					if (itemList[i].equals("-"))
						popup.addSeparator();
					else if (!itemList[i].equals("")) {
						mi = new MenuItem(itemList[i]);
						mi.addActionListener(this);
						popup.add(mi);
					}
		   		}
			}
			/**
			 * Add a new material in the materials list and then select it.
			 * The name of the new material is made by the number of items 
			 * in the list.
			 */
			public void addMaterial() {
				int num = getItemCount();
				num++;
				add("Material"+num);
				select(num);
			}

			/**
			 * Delete the selected material in the list.
			 */
			public void delMaterial() {
				int selected = getSelectedIndex()-1;
				remove(selected);
			}
			
			public void mouseClicked(MouseEvent e) {}
			public void mouseReleased(MouseEvent e) {}
			public void mouseEntered(MouseEvent e) {}
			public void mouseExited(MouseEvent e) {}
			public void mousePressed(MouseEvent e) {
				if(e.isPopupTrigger() && popup != null)
					popup.show(this, e.getX(), e.getY());
			}

			boolean altDown = false;
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ALT)
					altDown = true;
			}
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ALT)
					altDown = false;
			}
			public void keyTyped(KeyEvent e) { }

			public void actionPerformed(ActionEvent e) {
				IJ.write("actionPerformed... you see?");
				
			}

			public void itemStateChanged(ItemEvent e) {
				IJ.write("itemStateChanged... you see?");
			}
			
			
		} // ContainerPanel.MainPanel.Matlist2 inner class
		
	} // ContainerPanel.MainPanel inner class

	
	
	//Tools Panel
	public class ToolsPanel extends Panel {

		Color color = Color.WHITE;
		Dimension dimension = new Dimension(200, 170);
		
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
			setBackground(color);
			setSize(dimension);
			
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
			pBrush.setBackground(color);
			
			constrBrush = new GridBagConstraints();
			constrBrush.fill = GridBagConstraints.BOTH;
			pBrush.setLayout(new GridBagLayout());

			constrBrush.gridwidth = GridBagConstraints.REMAINDER;
			lBrush = makeLabel("Brush", constrBrush, pBrush, color);
			
			constrBrush.gridwidth = GridBagConstraints.RELATIVE;
			constrBrush.weightx = 4.0;
			sBrush = makeScrollbar(constrBrush, pBrush, Scrollbar.HORIZONTAL, 10, 5, 0, 250, adjustmentListener, color);
			
			constrBrush.gridwidth = GridBagConstraints.REMAINDER;
			constrBrush.weightx = 2.0;
			tBrush = makeTextField(String.valueOf(sBrush.getValue()), constrBrush, pBrush, color);

			constrBrush.gridwidth = 1;
			constrBrush.weightx = 1.0;
			bBrush1px = makeImageButton("iconBrush-1px.png", constrBrush, pBrush, listener, color);
			bBrush3px = makeImageButton("iconBrush-3px.png", constrBrush, pBrush, listener, color);
			bBrush5px = makeImageButton("iconBrush-5px.png", constrBrush, pBrush, listener, color);
			bBrush7px = makeImageButton("iconBrush-7px.png", constrBrush, pBrush, listener, color);
			bBrush10px = makeImageButton("iconBrush-10px.png", constrBrush, pBrush, listener, color);
			
			constrBrush.gridwidth = GridBagConstraints.REMAINDER;
			bBrush15px = makeImageButton("iconBrush-15px.png", constrBrush, pBrush, listener, color);
			
			add(pBrush, "brush");
		}
		
		public void addLassoPanel() {
			pLasso = new Panel();
			pLasso.setBackground(color);
			
			constrLasso = new GridBagConstraints();
			constrLasso.fill = GridBagConstraints.BOTH;
			pLasso.setLayout(new GridBagLayout());

			constrLasso.gridwidth = GridBagConstraints.REMAINDER;
			lLasso = makeLabel("Lasso", constrLasso, pLasso, color);
			
			constrLasso.gridwidth = GridBagConstraints.REMAINDER;
			cLassoAutoTrace = makeCheckbox("auto trace", true, constrLasso, pLasso, color);
			cLassoTraceEdges = makeCheckbox("trace edges", true, constrLasso, pLasso, color);
			
			add(pLasso, "lasso");
		}
		
		public void addMagicWandPanel() {
			pMagicWand = new Panel();
			pMagicWand.setBackground(color);
			
			constrMagicWand = new GridBagConstraints();
			constrMagicWand.fill = GridBagConstraints.BOTH;
			pMagicWand.setLayout(new GridBagLayout());

			constrMagicWand.gridwidth = GridBagConstraints.REMAINDER;
			lMagicWand = makeLabel("Magic Wand", constrMagicWand, pMagicWand, color);
			
			constrMagicWand.gridwidth = 1;
			constrMagicWand.weightx = 1.0;
			tMagicWand1 = makeTextField("-2", constrMagicWand, pMagicWand, color);
			
			tMagicWand2 = makeTextField("10", constrMagicWand, pMagicWand, color);
			
			constrMagicWand.gridwidth = GridBagConstraints.REMAINDER;
			lMagicWandBlank = makeLabel(" ", constrMagicWand, pMagicWand, color);
			
			cMagicWandAbsoluteValues = makeCheckbox("absolute values", false, constrMagicWand, pMagicWand, color);
			cMagicWandSameMaterialOnly = makeCheckbox("same material only", false, constrMagicWand, pMagicWand, color);
			cMagicWandFillInterior = makeCheckbox("fill interior", false, constrMagicWand, pMagicWand, color);
			
			lMagicWandDrawLimitLine = makeLabel("Draw limit line", constrMagicWand, pMagicWand, color);
			
			add(pMagicWand, "magicWand");
		}
		
		public void addPropagatingContourPanel() {
			pPropagatingContour = new Panel();
			pPropagatingContour.setBackground(color);
			
			constrPropagatingContour = new GridBagConstraints();
			constrPropagatingContour.fill = GridBagConstraints.BOTH;
			pPropagatingContour.setLayout(new GridBagLayout());

			constrPropagatingContour.gridwidth = GridBagConstraints.REMAINDER;
			lPropagatingContour = makeLabel("Propagating Contour", constrPropagatingContour, pPropagatingContour, color);
			
			constrPropagatingContour.gridwidth = 1;
			constrPropagatingContour.weightx = 1.0;
			lPropagatingContourTime = makeLabel("Time", constrPropagatingContour, pPropagatingContour, color);
			
			constrPropagatingContour.weightx = 2.0;
			sPropagatingContour = makeScrollbar(constrPropagatingContour, pPropagatingContour, Scrollbar.HORIZONTAL, 5, 5, 0, 100, adjustmentListener, color);
			
			constrPropagatingContour.weightx = 1.0;
			constrPropagatingContour.gridwidth = GridBagConstraints.REMAINDER;
			tPropagatingContour = makeTextField("5", constrPropagatingContour, pPropagatingContour, color);
			
			constrPropagatingContour.weightx = 0.0;
			bPropagatingContourMenu = makeButton("Menu", constrPropagatingContour, pPropagatingContour, listener, color);
			bPropagatingContourClear = makeButton("Clear", constrPropagatingContour, pPropagatingContour, listener, color);
			bPropagatingContourDolt = makeButton("Dolt", constrPropagatingContour, pPropagatingContour, listener, color);
			
			add(pPropagatingContour, "propagatingContour");
		}
		
		public void addBlowToolPanel() {
			pBlowTool = new Panel();
			pBlowTool.setBackground(color);

			constrBlowTool = new GridBagConstraints();
			pBlowTool.setLayout(new GridBagLayout());

			constrBlowTool.gridwidth = GridBagConstraints.REMAINDER;
			lBlowTool = makeLabel("Blow Tool", constrBlowTool, pBlowTool, color);
			
			constrBlowTool.gridwidth = 1;
			constrBlowTool.weightx = 1.0;
			lBlowToolTolerance = makeLabel("Tolerance:", constrBlowTool, pBlowTool, color);
			
			constrBlowTool.gridwidth = GridBagConstraints.REMAINDER;
			tBlowToolTolerance = makeTextField("35", constrBlowTool, pBlowTool, color);
			
			constrBlowTool.gridwidth = 1;
			lBlowToolGaussWidth = makeLabel("Gauss Width:", constrBlowTool, pBlowTool, color);
			
			constrBlowTool.gridwidth = GridBagConstraints.REMAINDER;
			tBlowToolGaussWidth = makeTextField("4", constrBlowTool, pBlowTool, color);
			
			add(pBlowTool, "blowTool");
		} //  ContainerPanel.ToolsPanel construtor
		
		public void addCrossHairPanel() {
			pCrossHair = new Panel();
			pCrossHair.setBackground(color);

			lCrossHair = new Label("Cross Hair");
			lCrossHair.setBackground(color);
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
			setSize(new Dimension(200, 90));
			setBackground(color);
			
			GridBagConstraints constr = new GridBagConstraints();
			
			setLayout(new GridBagLayout());
			
			constr.weightx = 1.0;
			lPos = makeLabel("Pos:", constr, this, color);
			lPosX = makeLabel("____X____", constr, this, color);
			lPosY = makeLabel("____Y____", constr, this, color);
			
			constr.gridwidth = GridBagConstraints.REMAINDER;
			lPosZ = makeLabel("____Z____", constr, this, color);

			constr.gridwidth = 1;
			lIndex = makeLabel("Index:", constr, this, color);
			lIndexX = makeLabel(" X  ", constr, this, color);
			lIndexY = makeLabel(" Y  ", constr, this, color);
			
			constr.gridwidth = GridBagConstraints.REMAINDER;
			lIndexZ = makeLabel(" Z  ", constr, this, color);
			
			constr.gridwidth = 1;
			lMaterial = makeLabel("Material:", constr, this, color);
			
			constr.gridwidth = GridBagConstraints.REMAINDER;
			lMaterialName = makeLabel("  name   ", constr, this, color);
			
			constr.gridwidth = 1;
			lVoxelValue = makeLabel("Voxel Value:", constr, this, color);
			
			constr.gridwidth = GridBagConstraints.REMAINDER;
			lVoxelValueNum = makeLabel("______ ~ ______", constr, this, color);
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
		public void updateLabels(double posX, double posY, double posZ, String value) {
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
			lPosX.setText(posX);
			lPosY.setText(posY);
			lPosZ.setText(posZ);
			lVoxelValueNum.setText(value);
		}
		
	} // ContainerPanel.InfosPanel inner class

} // ContainerPanel

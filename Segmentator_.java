
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.HashMap;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

import events.RoiEvent;
import events.RoiListener;
import events.RoiWatcher;
import events.SliceEvent;
import events.SliceListener;
import events.SliceWatcher;
import adt.Connectivity2D;
import adt.Points;

import gui.GuiBuilder;
import ij.*;
import ij.gui.*;
import ij.plugin.PlugIn;

/*
 * Created on 29-May-2006
 */

public class Segmentator_ extends JFrame implements PlugIn {

    private static final String SEGMENT = "segment";
    private static final String REMOVE_ISLANDS = "remove islands";
    private static final String SET_LABEL = "set label";

    private static final String LOAD_IMAGE = "load image";
    private static final String SAVE_IMAGE = "save image";
    private static final String LOAD_LABELS = "load labels";
    private static final String SAVE_LABELS = "save labels";
    private static final String LOAD_MATERIALS = "load materials";

    SpinnerNumberModel minArea;

    JList labelList;
    DefaultListModel labelListModel;

    public Segmentator_() {
        super("segmentator");

        IJ.runPlugIn("LabelBrush_", ""); //load our drawing tool

//int toolId = Toolbar.getInstance().addTool("brush");


        Controllor controllor = new Controllor();


        //new ij.plugin.MacroInstaller().install("Roi Brush Tool");

        ImagePlus.addImageListener(controllor);


        //the stup method may be called AFTER images have been opened,
        //we need listeners attached to all image windows though, to track focus
        for (Frame frame : ImageWindow.getFrames()) {
            if (frame instanceof ImageWindow) {
                controllor.imageOpened(((ImageWindow) frame).getImagePlus());
            }
        }


        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        labelList = addLabelList(this);
        labelListModel = (DefaultListModel) labelList.getModel();

        labelList.addListSelectionListener(controllor);

        GuiBuilder.add2Command(this, LOAD_IMAGE, LOAD_IMAGE, SAVE_IMAGE, SAVE_IMAGE, controllor);
        GuiBuilder.add2Command(this, LOAD_LABELS, LOAD_LABELS, SAVE_LABELS, SAVE_LABELS, controllor);
        GuiBuilder.addCommand(this, LOAD_MATERIALS, LOAD_MATERIALS, controllor);

        pack();
    }

    public void run(String arg0) {
        setVisible(!isVisible());
    }


    public void clearLabelsList() {
        labelListModel.clear();
    }


    public void populateLabelList(AmiraParameters params) {
        clearLabelsList();
        for (int id = 0; id < params.getMaterialCount(); id++) {
            labelListModel.addElement(params.getMaterial(id));
        }
    }


    public AmiraParameters.Material getCurrentMaterial(){
        int selectedIndex = labelList.getSelectedIndex();
        if(selectedIndex == -1) return null;
        else{
            return (AmiraParameters.Material) labelListModel.get(selectedIndex);
        }
    }



    public static JList addLabelList(Container c) {
        final DefaultListModel model = new DefaultListModel();
        final JList list = new JList(model);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("labels..."), BorderLayout.NORTH);

        JPanel controlPanel = new JPanel(new GridLayout(1, 2));
        //panel.add(controlPanel, BorderLayout.SOUTH);
        panel.add(new JScrollPane(list));

        /*  REMOVED ADD AND REMOVE, thinkin might be better if the user is always forced to use a ready made .labels file
        JButton add = new JButton("add");
        add.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String name = IJ.getString("name?", "");
                ImagePlus imagePlus = IJ.getImage();

                //ImageLabels labels = new ImageLabels(imagePlus);
                //model.addElement(labels.newLabel(name));
            }
        });

        JButton remove = new JButton("remove");
        remove.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                int index = list.getSelectedIndex();  //just currently working for the first of the selections
                System.out.println(index);
                if (index == -1) return;

                ImagePlus imagePlus = IJ.getImage();

                //ImageLabels labels = new ImageLabels(imagePlus);
                //ImageLabel selected = (ImageLabel) model.getElementAt(index);

                //remove from the list of labels and the list model...
                //labels.removeLabel(selected.getName());
                //model.removeElementAt(index);
            }
        });

        JButton deselect = new JButton("deselect");
        deselect.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                list.clearSelection();
                IJ.getImage().killRoi();
            }
        });
        controlPanel.add(add);
        controlPanel.add(remove);
        controlPanel.add(deselect);
        */
        c.add(panel);

        return list;
    }

    private class Controllor implements ActionListener, ImageListener, WindowFocusListener, SliceListener, RoiListener, ListSelectionListener {

        ImagePlus currentImage;


        public void actionPerformed(ActionEvent e) {
            //IJ.showMessage(e.getActionCommand());

            if (e.getActionCommand().equals(LOAD_IMAGE)) {
                IJ.runPlugIn("AmiraMeshReader_", "");

                if (AmiraParameters.isAmiraLabelfield(IJ.getImage())) {
                    IJ.showMessage("file was not an image file");
                } else {
                    updateCurrent(IJ.getImage());
                }


            } else if (e.getActionCommand().equals(SAVE_IMAGE)) {
                //todo
                IJ.showMessage("greyscale edits not implemented yet, you can  save your labels though...");
            }else if(e.getActionCommand().equals(LOAD_LABELS)){
                IJ.runPlugIn("AmiraMeshReader_", "");
                if (AmiraParameters.isAmiraLabelfield(IJ.getImage())) {
                    //load label data
                    loadLabels(IJ.getImage());
                } else {
                    IJ.showMessage("file was not a labels file");
                }
            }else if(e.getActionCommand().equals(SAVE_LABELS)){
                AmiraMeshWriter_.writeImage(new SegmentatorModel(currentImage).getLabelImagePlus());
            }else if(e.getActionCommand().equals(LOAD_MATERIALS)){
                IJ.runPlugIn("AmiraMeshReader_", "");
                if (AmiraParameters.isAmiraLabelfield(IJ.getImage())) {
                    //load label data
                    loadMaterials(IJ.getImage());
                } else {
                    IJ.showMessage("file was not a labels file");
                }
            }

            currentImage.updateAndDraw();
        }

        //reads the parameters to get what the labels should be
        //but does not use any of the pixel data
        private void loadMaterials(ImagePlus materialImage) {


            AmiraParameters params = new AmiraParameters(materialImage);

            //create a blank image to work with
            System.out.println("currentImage.getStackSize() = " + currentImage.getStackSize());

            //todo does not work...
            IJ.newImage("new", "8-bit", currentImage.getWidth(), currentImage.getHeight(), currentImage.getStackSize());

            ImagePlus newImage = IJ.getImage();

            newImage.setProperty(AmiraParameters.INFO, materialImage.getProperty(AmiraParameters.INFO));

            //copy the parameter information across
            AmiraParameters newParams = new AmiraParameters(newImage);

            //close the initial image
            materialImage.close();

            //and load the new blank one as normal
            loadLabels(newImage);
        }

        private void loadLabels(ImagePlus labelImage) {

            labelImage.hide();//don't want the extra one visible to the user

            new SegmentatorModel(currentImage).setLabelImagePlus(labelImage);

            AmiraParameters params = new AmiraParameters(labelImage);

            populateLabelList(params);

            SegmentationViewerCanvas canvas = new SegmentationViewerCanvas(currentImage, labelImage);

            new SegmentatorModel(currentImage).setLabelCanvas(canvas);


            if (currentImage.getStackSize() > 1)
                new StackWindow(currentImage, canvas);
            else
                new ImageWindow(currentImage, canvas);

            //after a new window is constructed. the old one is
            //cloased and the listener tidied up
            //so we need to make sure we add a new one
            //we do not need to do this for ROIs becuase
            //they work by polling
            new SliceWatcher(currentImage).addSliceListener(this);
            //new RoiWatcher(currentImage).addRoiListener(this);
        }


        public void imageOpened(ImagePlus ip) {
            //when a new image is opened in the environement we need to listen to it gaining foces
            ip.getWindow().addWindowFocusListener(this);
        }

        public void imageClosed(ImagePlus ip) {
//			when a new image is closed we need to tidy up the listeners
            ip.getWindow().removeWindowFocusListener(this);
        }

        public void imageUpdated(ImagePlus ip) {
            //System.out.println("image Updated");
        }

        public void windowGainedFocus(WindowEvent e) {
            updateCurrent(IJ.getImage());
        }


        public void windowLostFocus(WindowEvent e) {
            //clearLabelsList(); removes labels when toolbar is highlighted
        }

        private void updateCurrent(ImagePlus newCurrent) {

            if (newCurrent == currentImage)
                return;
            else {
                if (currentImage != null) {
                    new SliceWatcher(currentImage).removeSliceListener(this);
                    //new RoiWatcher(currentImage).removeRoiListener(this);
                }

                if (newCurrent != null) {
                    new SliceWatcher(newCurrent).addSliceListener(this);
                    //new RoiWatcher(newCurrent).addRoiListener(this);

                    //populateLabelList(newCurrent);
                }
                currentImage = newCurrent;
            }

        }

        public void sliceNumberChanged(SliceEvent e) {
            System.out.println(e.getSource().getCurrentSlice());
            //drawLabels(currentImage);
            //drawLabels(currentImage, currentLabels, canvas);
        }

        public void roiChanged(RoiEvent e) {
            //todo
            //setLabel(currentImage);
        }

        public void valueChanged(ListSelectionEvent e) {
            new SegmentatorModel(currentImage).setCurrentMaterial(getCurrentMaterial());
        }
    }

}


import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.HashMap;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.ListModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

import events.RoiEvent;
import events.RoiListener;
import events.RoiWatcher;
import events.SliceEvent;
import events.SliceListener;
import events.SliceWatcher;
import adt.ImageLabel;
import adt.ImageLabels;
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

    private static final String LOAD_IMAGE = "load mesh";
    private static final String SAVE_IMAGE = "save mesh";


    SpinnerNumberModel minArea;

    JList labelList;
    DefaultListModel labelListModel;

    public Segmentator_() {
        super("segmentator");

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

        GuiBuilder.addCommand(this, LOAD_IMAGE, LOAD_IMAGE, controllor);
        GuiBuilder.addCommand(this, SAVE_IMAGE, SAVE_IMAGE, controllor);

        pack();
    }

    public void run(String arg0) {
        setVisible(!isVisible());
    }


    public void clearLabelsList() {
        labelListModel.clear();
    }

    public void populateLabelList(ImagePlus master) {
        clearLabelsList();


        ImageLabels labels = new ImageLabels(master);

        for (ImageLabel label : labels.getLabels()) {
            labelListModel.addElement(label);//todo add alphabetically
        }

    }

    //draws the currentImage ROI boxes for the labels
    public void drawLabels(ImagePlus ip) {

        int selectedIndex = labelList.getSelectedIndex();

        //todo expand to allow multiple selection rather than first that is highlighted
        if (selectedIndex != -1) {
            ImageLabel selected = (ImageLabel) labelListModel.getElementAt(selectedIndex);

            Roi label = selected.getLabelForSlice();
            System.out.println(label);
            if (label != null) {
                ip.setRoi(label);
            } else {
                if (ip.getRoi() != null) ip.killRoi();
            }
        }
    }

    public void setLabel(ImagePlus ip) {
        int selectedIndex = labelList.getSelectedIndex();
//		todo expand to allow multiple selection rather than first that is highlighted
        if (selectedIndex != -1) {
            ImageLabel selected = (ImageLabel) labelListModel.getElementAt(selectedIndex);

            selected.setLabelForSlice();
        }
    }


    public static JList addLabelList(Container c) {
        final DefaultListModel model = new DefaultListModel();
        final JList list = new JList(model);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("labels..."), BorderLayout.NORTH);

        JPanel controlPanel = new JPanel(new GridLayout(1, 2));
        panel.add(controlPanel, BorderLayout.SOUTH);
        panel.add(list);

        JButton add = new JButton("add");
        add.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String name = IJ.getString("name?", "");
                ImagePlus imagePlus = IJ.getImage();

                ImageLabels labels = new ImageLabels(imagePlus);
                model.addElement(labels.newLabel(name));
            }
        });

        JButton remove = new JButton("remove");
        remove.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                int index = list.getSelectedIndex();  //just currently working for the first of the selections
                System.out.println(index);
                if (index == -1) return;

                ImagePlus imagePlus = IJ.getImage();

                ImageLabels labels = new ImageLabels(imagePlus);
                ImageLabel selected = (ImageLabel) model.getElementAt(index);

                //remove from the list of labels and the list model...
                labels.removeLabel(selected.getName());
                model.removeElementAt(index);
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

        c.add(panel);

        return list;
    }

    public void loadAmiraParams(AmiraParameters params, ImagePlus target, ImagePlus labelImage) {
        //load the materials as labels
        for (String material : params.getMaterialList()) {
            int id = params.getMaterialID(material);

            //insert the material object
            ImageLabel newLabel = new ImageLabels(target).newLabel(material, params.getMaterialColor(id));

        }

        //now we need to go through all the pixel data and assign values to the ROIs

        //first we draw out the pixel data into a useful form
        System.out.println("loading voxels");
        ImageStack stack = labelImage.getStack();
        for (int z = 1; z < stack.getSize(); z++) {
            IJ.showProgress(z, stack.getSize());

            //we load a slice into a useful lookup form
            HashMap<Integer, Connectivity2D> materialPoints = new HashMap<Integer, Connectivity2D>();

            byte[] pixels = (byte[]) stack.getPixels(z);
            int width = stack.getWidth();
            int offset, i;
            for (int y = 0; y < stack.getHeight(); y++) {
                offset = y * width;
                for (int x = 0; x < width; x++) {
                    i = offset + x;



                    int pixelValue = pixels[i];

                    Connectivity2D connectivityGraph = materialPoints.get(pixelValue);
                    if(connectivityGraph==null){
                        connectivityGraph = new Connectivity2D();
                        materialPoints.put(pixelValue, connectivityGraph);
                    }

                    connectivityGraph.addLowerRightPoint(new Point(x,y));
                }
            }
            //now we need to convert the data for each material into a shape

            for (Integer materialId : materialPoints.keySet()) {

                Connectivity2D connectivityGraph = materialPoints.get(materialId);
                ShapeRoi roi = null;
                for (Points points : connectivityGraph.getIslands()) {
                    Polygon outline = points.getOutline();
                    if(roi == null){
                        roi = new ShapeRoi(new PolygonRoi(outline, PolygonRoi.POLYGON));
                    }else{
                        roi.or(new ShapeRoi(new PolygonRoi(outline, PolygonRoi.POLYGON)));
                    }
                }
                new ImageLabels(target).getLabel(params.getMaterialName(materialId)).setLabelForSlice(z, roi);
            }
        }



    }

    private class Controllor implements ActionListener, ImageListener, WindowFocusListener, SliceListener, RoiListener, ListSelectionListener {

        ImagePlus currentImage;
        ImagePlus currentLabels;

        public void actionPerformed(ActionEvent e) {
            //IJ.showMessage(e.getActionCommand());

            if (e.getActionCommand().equals(LOAD_IMAGE)) {
                IJ.runPlugIn("AmiraMeshReader_", "");

                updateCurrent(IJ.getImage());

            } else if (e.getActionCommand().equals(SAVE_IMAGE)) {
                IJ.runPlugIn("AmiraMeshWriter_", "");
            }

            currentImage.updateAndDraw();
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
            System.out.println("image Updated");
        }

        public void windowGainedFocus(WindowEvent e) {
            updateCurrent(IJ.getImage());
        }


        public void windowLostFocus(WindowEvent e) {
            //clearLabelsList(); removes labels when toolbar is highlighted
        }

        private void updateCurrent(ImagePlus newCurrent) {
            if (AmiraParameters.isAmiraLabelfield(newCurrent)) {
                if (newCurrent == currentLabels)
                    return;
                else {
                    currentLabels = newCurrent;


                    if (currentLabels != null) {
                        AmiraParameters params = new AmiraParameters(currentLabels);

                        loadAmiraParams(params, currentImage, currentLabels);

                        currentLabels.close();

                        populateLabelList(currentImage);

                    }
                }
            } else {
                if (newCurrent == currentImage)
                    return;
                else {
                    if (currentImage != null) {
                        new SliceWatcher(currentImage).removeSliceListener(this);
                        new RoiWatcher(currentImage).removeRoiListener(this);
                    }

                    if (newCurrent != null) {
                        new SliceWatcher(newCurrent).addSliceListener(this);
                        new RoiWatcher(newCurrent).addRoiListener(this);
                        populateLabelList(newCurrent);
                    }
                    currentImage = newCurrent;
                }
            }
        }

        public void sliceNumberChanged(SliceEvent e) {
            System.out.println(e.getSource().getCurrentSlice());
            drawLabels(currentImage);
        }

        public void roiChanged(RoiEvent e) {
            setLabel(currentImage);
        }

        public void valueChanged(ListSelectionEvent e) {
            drawLabels(currentImage);
        }
    }

}

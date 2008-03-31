package org.imagearchive.lsm.toolbox.gui;

import ij.ImagePlus;
import ij.io.RandomAccessStream;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;

import org.imagearchive.lsm.toolbox.MasterModel;
import org.imagearchive.lsm.toolbox.Reader;

public class ImagePreview extends JComponent implements PropertyChangeListener {
    ImageIcon thumbnail = null;

    File file = null;

    Reader reader;

    public ImagePreview(MasterModel masterModel, JFileChooser fc) {
        setPreferredSize(new Dimension(138, 50));
        fc.addPropertyChangeListener(this);
        reader = new Reader(masterModel);
    }

    public void loadImage() {
        if (file == null) {
            thumbnail = null;
            return;
        }
        ImageIcon tmpIcon = null;
        try {

            RandomAccessStream stream = new RandomAccessStream(
                    new RandomAccessFile(file, "r"));
            if (reader.isLSMfile(stream)) {
                ImagePlus imp[] = reader.open(file.getParent(), file.getName(), false, false, true);
                if (imp != null && imp.length>0)
                    tmpIcon = new ImageIcon(imp[0].getImage());
                else {
                    thumbnail = null;
                    return;
                }
            }
        } catch (IOException e) {
            thumbnail = null;
            return;
        }

        if (tmpIcon != null) {
            if (tmpIcon.getIconWidth() > 128) {
                thumbnail = new ImageIcon(tmpIcon.getImage().getScaledInstance(
                        128, -1, Image.SCALE_DEFAULT));
            } else {
                thumbnail = tmpIcon;
            }
        }
    }

    public void propertyChange(PropertyChangeEvent e) {
        boolean update = false;
        String prop = e.getPropertyName();

        // If the directory changed, don't show an image.
        if (JFileChooser.DIRECTORY_CHANGED_PROPERTY.equals(prop)) {
            file = null;
            update = true;

            // If a file became selected, find out which one.
        } else if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(prop)) {
            file = (File) e.getNewValue();
            update = true;
        }

        // Update the preview accordingly.
        if (update) {
            thumbnail = null;
            if (isShowing()) {
                loadImage();
                repaint();
            }
        }
    }

    protected void paintComponent(Graphics g) {
        if (thumbnail == null) {
            loadImage();
        }
        if (thumbnail != null) {
            int x = getWidth() / 2 - thumbnail.getIconWidth() / 2;
            int y = getHeight() / 2 - thumbnail.getIconHeight() / 2;

            if (y < 0) {
                y = 0;
            }

            if (x < 5) {
                x = 5;
            }
            thumbnail.paintIcon(this, g, x, y);
        }
    }
}
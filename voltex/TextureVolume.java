package voltex;

import java.awt.*;
import java.awt.image.*;
import java.awt.color.ColorSpace;
import javax.media.j3d.*;
import javax.vecmath.*;
import java.io.*;
import java.text.NumberFormat;

public abstract class TextureVolume implements VolRendConstants {

    protected Volume volume;

    protected static final int RELOAD_NONE = 0;
    protected static final int RELOAD_VOLUME = 1;

    private int			volEditId = -1;
    private boolean		volumeReloadNeeded = true;

    
	public TextureVolume(Volume volume) {
		this.volume = volume;
    }

    int update() {
		int newVolEditId = -1;
		if ((newVolEditId = volume.update()) != volEditId) {
			volEditId = newVolEditId;
			volumeReloadNeeded = true;
		}
		if (volumeReloadNeeded) {
			volumeReload();
			return RELOAD_VOLUME;
		} else {
			return RELOAD_NONE;
		}
    }

    void volumeReload() {
		if (volume.hasData()) {
			loadTexture();
		}
		volumeReloadNeeded = false;
    }

    int byteToInt(int byteValue) {
		return (int)((byteValue / 255.0f) * Integer.MAX_VALUE);
    }

    abstract void loadTexture();
}

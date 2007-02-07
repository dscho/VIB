package voltex;

import java.awt.*;
import java.awt.image.*;
import java.awt.color.ColorSpace;
import javax.media.j3d.*;
import javax.vecmath.*;
import java.io.*;
import java.text.NumberFormat;

public class Texture2DVolume extends TextureVolume {

    // sets of textures, one for each axis, sizes of the arrays are set
    // by the dimensions of the Volume
    protected Texture2D[]		xTextures;	
    protected Texture2D[]		yTextures;	
    protected Texture2D[]		zTextures;	

    protected TexCoordGeneration 	xTg = new TexCoordGeneration();
    protected TexCoordGeneration 	yTg = new TexCoordGeneration();
    protected TexCoordGeneration 	zTg = new TexCoordGeneration();

    private ColorModel          colorModel;
    private WritableRaster      raster;

    public Texture2DVolume(Volume volume) {
		super(volume);
    }

    void loadTexture() {
	    ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
	    int[] nBits = {8};
	    colorModel = new ComponentColorModel(cs, nBits, false, false, 
		Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);

		System.out.print("Loading Z axis texture maps");
		loadAxis(Z_AXIS);
		System.out.println("done");
		System.out.print("Loading Y axis texture maps");
		loadAxis(Y_AXIS);
		System.out.println("done");
		System.out.print("Loading X axis texture maps");
		loadAxis(X_AXIS);
		System.out.println("done");
    }

    private void loadAxis(int axis) {
		int	rSize = 0;    // number of tex maps to create
		int	sSize = 0; 	  // s,t = size of texture map to create
		int tSize = 0; 
		Texture2D[] textures = null;

		switch (axis) {
		  case Z_AXIS:
			rSize = volume.zDim;
			sSize = volume.xTexSize;
			tSize = volume.yTexSize;
			textures = zTextures = new Texture2D[rSize];
			zTg = new TexCoordGeneration();
			zTg.setPlaneS(new Vector4f(volume.xTexGenScale, 0.0f, 0.0f, 0.0f));
			zTg.setPlaneT(new Vector4f(0.0f, volume.yTexGenScale, 0.0f, 0.0f));
			break;
		  case Y_AXIS:
			rSize = volume.yDim;
			sSize = volume.xTexSize;
			tSize = volume.zTexSize;
			textures = yTextures = new Texture2D[rSize];
			yTg = new TexCoordGeneration();
			yTg.setPlaneS(new Vector4f(volume.xTexGenScale, 0.0f, 0.0f, 0.0f));
			yTg.setPlaneT(new Vector4f(0.0f, 0.0f, volume.zTexGenScale, 0.0f));
			break;
		  case X_AXIS:
			rSize = volume.xDim;
			sSize = volume.yTexSize;
			tSize = volume.zTexSize;
			textures = xTextures = new Texture2D[rSize];
			xTg = new TexCoordGeneration();
			xTg.setPlaneS(new Vector4f(0.0f, volume.yTexGenScale, 0.0f, 0.0f));
			xTg.setPlaneT(new Vector4f(0.0f, 0.0f, volume.zTexGenScale, 0.0f));
			break;
		}

		raster = colorModel.createCompatibleWritableRaster(sSize, tSize); 

		BufferedImage bImage = 
			new BufferedImage(colorModel, raster, false, null); 

		byte[] byteData = null;
		byteData = ((DataBufferByte)raster.getDataBuffer()).getData(); 

		for (int i=0; i < rSize; i ++) { 
			switch (axis) {
			  case Z_AXIS: volume.loadZIntensity(i, byteData); break;
			  case Y_AXIS: volume.loadYIntensity(i, byteData); break;
			  case X_AXIS: volume.loadXIntensity(i, byteData); break;
			}

			Texture2D tex;
			ImageComponent2D pArray;
			tex = new Texture2D(Texture.BASE_LEVEL, 
				Texture.INTENSITY, sSize, tSize);
			pArray = new ImageComponent2D(
				ImageComponent.FORMAT_CHANNEL8, sSize, tSize);
			pArray.set(bImage);
		
			tex.setImage(0, pArray);
			tex.setEnable(true);
			tex.setMinFilter(Texture.BASE_LEVEL_LINEAR);
			tex.setMagFilter(Texture.BASE_LEVEL_LINEAR);
			
			tex.setBoundaryModeS(Texture.CLAMP);
			tex.setBoundaryModeT(Texture.CLAMP);

			textures[i] = tex;
			System.out.print(".");
		} 
    } 
}

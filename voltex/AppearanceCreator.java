package voltex;

import ij.IJ;

import java.awt.*;
import java.awt.image.*;
import java.awt.color.ColorSpace;
import javax.media.j3d.*;
import javax.vecmath.*;
import java.io.*;
import java.text.NumberFormat;

public class AppearanceCreator implements VolRendConstants {

	public TexCoordGeneration xTg = new TexCoordGeneration();
	public TexCoordGeneration yTg = new TexCoordGeneration();
	public TexCoordGeneration zTg = new TexCoordGeneration();
	public Texture2D[] xTextures;	
	public Texture2D[] yTextures;	
	public Texture2D[] zTextures;	

	private Volume volume;
	private IndexColorModel cmodel;

	public AppearanceCreator(Volume volume, IndexColorModel cmodel) {
		this.volume = volume;
		this.cmodel = cmodel;
	}

	public void setColorModel(IndexColorModel cmodel) {
		this.cmodel = cmodel;
	}

	public Appearance getAppearance(int direction, 
			int index, Color3f color, float transparency) {
		Texture tex = null;
		TexCoordGeneration tg = null;
		return createAppearance(
			getTexture(direction, index),
			getTg(direction, index),
			color,
			transparency);
	}

	public Texture2D getTexture(int direction, int index) {
		switch(direction) {
			case X_AXIS: return xTextures[index];
			case Y_AXIS: return yTextures[index];
			case Z_AXIS: return zTextures[index];
		} 
		return null;
	}

	public TexCoordGeneration getTg(int direction, int index) {
		switch(direction) {
			case X_AXIS: return xTg;
			case Y_AXIS: return yTg;
			case Z_AXIS: return zTg;
		} 
		return null;
	}

	public void loadTexture() {
		volume.update();
		IJ.showStatus("Loading Z axis texture maps");
		loadAxis(Z_AXIS);
		IJ.showStatus("Loading Y axis texture maps");
		loadAxis(Y_AXIS);
		IJ.showStatus("Loading X axis texture maps");
		loadAxis(X_AXIS);
	}

	private Appearance createAppearance(Texture tex, TexCoordGeneration tg, 
			Color3f color, float transparency) {
		Appearance a = new Appearance();
		a.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_READ);
		a.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_READ);
		a.setCapability(Appearance.ALLOW_RENDERING_ATTRIBUTES_READ);

		TextureAttributes texAttr = new TextureAttributes();
		texAttr.setTextureMode(TextureAttributes.COMBINE);
		texAttr.setCombineRgbMode(TextureAttributes.COMBINE_MODULATE);
		//texAttr.setCombineRgbMode(TextureAttributes.COMBINE_REPLACE);

		TransparencyAttributes t = new TransparencyAttributes();
		t.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
		t.setTransparencyMode(TransparencyAttributes.BLENDED);
		t.setTransparency(transparency);

		PolygonAttributes p = new PolygonAttributes();
		p.setCullFace(PolygonAttributes.CULL_NONE);

		Material m = new Material();
		m.setLightingEnable(false);
		
		ColoringAttributes c = new ColoringAttributes();
		c.setCapability(ColoringAttributes.ALLOW_COLOR_WRITE);
		c.setShadeModel(ColoringAttributes.FASTEST);
		if(color == null) {
			c.setColor(1f, 1f, 1f);
		} else {
			c.setColor(color);
		}

		// Avoid rendering of voxels having an alpha value of zero
		RenderingAttributes ra = new RenderingAttributes();
		ra.setCapability(RenderingAttributes.ALLOW_ALPHA_TEST_VALUE_WRITE);
		ra.setAlphaTestValue(0.1f);
		ra.setAlphaTestFunction(RenderingAttributes.GREATER);
		
		a.setMaterial(m);
		a.setTransparencyAttributes(t);
		a.setTexture(tex);
		a.setTextureAttributes(texAttr);
		a.setTexCoordGeneration(tg);
		a.setPolygonAttributes(p);
		a.setColoringAttributes(c);
		a.setRenderingAttributes(ra);
		return a;
	}

	private void loadAxis(int axis) {
		int rSize = 0;
		int sSize = 0;
		int tSize = 0;
		Texture2D[] textures = null;

		switch (axis) {
		  case Z_AXIS:
			rSize = volume.zDim;
			sSize = volume.xTexSize;
			tSize = volume.yTexSize;
			textures = zTextures = new Texture2D[rSize];
			zTg = new TexCoordGeneration();
			zTg.setPlaneS(new Vector4f(volume.xTexGenScale, 0f, 0f, 0f));
			zTg.setPlaneT(new Vector4f(0f, volume.yTexGenScale, 0f, 0f));
			break;
		  case Y_AXIS:
			rSize = volume.yDim;
			sSize = volume.xTexSize;
			tSize = volume.zTexSize;
			textures = yTextures = new Texture2D[rSize];
			yTg = new TexCoordGeneration();
			yTg.setPlaneS(new Vector4f(volume.xTexGenScale, 0f, 0f, 0f));
			yTg.setPlaneT(new Vector4f(0f, 0f, volume.zTexGenScale, 0f));
			break;
		  case X_AXIS:
			rSize = volume.xDim;
			sSize = volume.yTexSize;
			tSize = volume.zTexSize;
			textures = xTextures = new Texture2D[rSize];
			xTg = new TexCoordGeneration();
			xTg.setPlaneS(new Vector4f(0f, volume.yTexGenScale, 0f, 0f));
			xTg.setPlaneT(new Vector4f(0f, 0f, volume.zTexGenScale, 0f));
			break;
		}

		int textureMode, componentType; 
		textureMode = Texture.RGBA;
		componentType = ImageComponent.FORMAT_RGBA;

		WritableRaster raster = cmodel.
					createCompatibleWritableRaster(sSize, tSize); 
		byte[] data = ((DataBufferByte)raster.getDataBuffer()).getData();

		BufferedImage bImage = 
			new BufferedImage(cmodel, raster, false, null); 


		for (int i=0; i < rSize; i ++) { 
			switch (axis) {
				case Z_AXIS: volume.loadZ(i, data); break;
				case Y_AXIS: volume.loadY(i, data); break;
				case X_AXIS: volume.loadX(i, data); break;
			}
			IJ.showProgress(i, rSize);

			Texture2D tex;
			ImageComponent2D pArray;
			boolean byRef = false;
			boolean yUp = true;
			tex = new Texture2D(Texture.BASE_LEVEL, 
						textureMode, sSize, tSize);
			pArray = new ImageComponent2D(
						componentType, sSize, tSize, byRef, yUp);
			pArray.set(bImage);
		
			tex.setImage(0, pArray);
			tex.setEnable(true);
			tex.setMinFilter(Texture.BASE_LEVEL_LINEAR);
			tex.setMagFilter(Texture.BASE_LEVEL_LINEAR);
			
			tex.setBoundaryModeS(Texture.CLAMP);
			tex.setBoundaryModeT(Texture.CLAMP);

			textures[i] = tex;
		} 
	} 
}

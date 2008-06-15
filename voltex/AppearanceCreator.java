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

	public AppearanceCreator(Volume volume, 
			Color3f color, float transparency, boolean[] ch) {
		this.volume = volume;
		this.volume.setChannels(ch);
		initAttributes(color, transparency);
	}

	public Appearance getAppearance(int direction, 
			int index, Color3f color, float transparency) {
		Texture tex = null;
		TexCoordGeneration tg = null;
		Appearance a = new Appearance();
		a.setCapability(Appearance.ALLOW_TEXTURE_WRITE);
		a.setCapability(Appearance.ALLOW_TEXGEN_WRITE);
		a.setMaterial(material);
		a.setTransparencyAttributes(transAttr);
		a.setPolygonAttributes(polyAttr);
		a.setColoringAttributes(colAttr);
		a.setRenderingAttributes(rendAttr);

		a.setTexture(getTexture(direction, index));
		a.setTexCoordGeneration(getTg(direction, index));
		a.setTextureAttributes(texAttr);
		return a;
	}

	public void setTransparency(float f) {
		transAttr.setTransparency(f);
	}

	public void setThreshold(float f) {
		rendAttr.setAlphaTestValue(f);
	}

	public void setColor(Color3f c) {
		colAttr.setColor(c);
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
		IJ.showStatus("Loading Z axis texture maps");
		loadAxis(Z_AXIS);
		IJ.showStatus("Loading Y axis texture maps");
		loadAxis(Y_AXIS);
		IJ.showStatus("Loading X axis texture maps");
		loadAxis(X_AXIS);
	}

	private TextureAttributes texAttr;
	private TransparencyAttributes transAttr;
	private PolygonAttributes polyAttr;
	private Material material;
	private ColoringAttributes colAttr;
	private RenderingAttributes rendAttr;

	private void initAttributes(Color3f color, float transparency) {
		texAttr = new TextureAttributes();
		texAttr.setTextureMode(TextureAttributes.COMBINE);
		texAttr.setCombineRgbMode(TextureAttributes.COMBINE_MODULATE);
		//texAttr.setCombineRgbMode(TextureAttributes.COMBINE_REPLACE);

		transAttr = new TransparencyAttributes();
		transAttr.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
		transAttr.setTransparencyMode(TransparencyAttributes.BLENDED);
		transAttr.setTransparency(transparency);

		polyAttr = new PolygonAttributes();
		polyAttr.setCullFace(PolygonAttributes.CULL_NONE);

		material = new Material();
		material.setLightingEnable(false);
		
		colAttr = new ColoringAttributes();
		colAttr.setCapability(ColoringAttributes.ALLOW_COLOR_WRITE);
		colAttr.setShadeModel(ColoringAttributes.FASTEST);
		if(color == null) {
			colAttr.setColor(1f, 1f, 1f);
		} else {
			colAttr.setColor(color);
		}

		// Avoid rendering of voxels having an alpha value of zero
		rendAttr = new RenderingAttributes();
		rendAttr.setCapability(
			RenderingAttributes.ALLOW_ALPHA_TEST_VALUE_WRITE);
		rendAttr.setAlphaTestValue(0.1f);
		rendAttr.setAlphaTestFunction(RenderingAttributes.GREATER);
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
			zTg.setPlaneS(new Vector4f(
					volume.xTexGenScale, 0f, 0f, 0f));
			zTg.setPlaneT(new Vector4f(
					0f, volume.yTexGenScale, 0f, 0f));
			break;
		  case Y_AXIS:
			rSize = volume.yDim;
			sSize = volume.xTexSize;
			tSize = volume.zTexSize;
			textures = yTextures = new Texture2D[rSize];
			yTg = new TexCoordGeneration();
			yTg.setPlaneS(new Vector4f(
					volume.xTexGenScale, 0f, 0f, 0f));
			yTg.setPlaneT(new Vector4f(
					0f, 0f, volume.zTexGenScale, 0f));
			break;
		  case X_AXIS:
			rSize = volume.xDim;
			sSize = volume.yTexSize;
			tSize = volume.zTexSize;
			textures = xTextures = new Texture2D[rSize];
			xTg = new TexCoordGeneration();
			xTg.setPlaneS(new Vector4f(
					0f, volume.yTexGenScale, 0f, 0f));
			xTg.setPlaneT(new Vector4f(
					0f, 0f, volume.zTexGenScale, 0f));
			break;
		}

		boolean rgb = volume.getDataType() == Volume.INT_DATA;
		boolean opaque = volume.getTransparenyType() == Volume.OPAQUE;
		// otherwise, let's assume we're dealing with intensity data
		int textureMode, componentType;
		BufferedImage bImage = null;
		Object data = null;

		if(rgb) {
			bImage = new BufferedImage(sSize, tSize, 
				BufferedImage.TYPE_INT_ARGB);
			DataBuffer db = bImage.getRaster().getDataBuffer();
			data = ((DataBufferInt)db).getData();
			textureMode = opaque ? Texture.RGB : Texture.RGBA;
			componentType = ImageComponent.FORMAT_RGBA;
		} else {
			bImage = new BufferedImage(sSize, tSize,
				BufferedImage.TYPE_BYTE_GRAY);
			DataBuffer db = bImage.getRaster().getDataBuffer();
			data = ((DataBufferByte)db).getData();
			textureMode = opaque ? Texture.LUMINANCE 
						: Texture.INTENSITY;
			componentType = ImageComponent.FORMAT_CHANNEL8;
		}

		for (int i = 0; i < rSize; i ++) { 
			switch (axis) {
				case Z_AXIS: volume.loadZ(i, data); break;
				case Y_AXIS: volume.loadY(i, data); break;
				case X_AXIS: volume.loadX(i, data); break;
			}
			IJ.showProgress(i, rSize);

			boolean byRef = false;
			boolean yUp = true;
			Texture2D tex = new Texture2D(Texture.BASE_LEVEL, 
						textureMode, sSize, tSize);
			ImageComponent2D pArray = new ImageComponent2D(
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

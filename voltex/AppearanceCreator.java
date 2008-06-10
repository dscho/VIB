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
	public Texture2D[][] xTextures;
	public Texture2D[][] yTextures;
	public Texture2D[][] zTextures;

	private Volume volume;
	private ColorModel cmodel;

	public AppearanceCreator(Volume volume, ColorModel cmodel,
				Color3f color, float transparency) {
		this.volume = volume;
		this.cmodel = cmodel;
		initAttributes(color, transparency);
	}

	public void setColorModel(ColorModel cmodel) {
		this.cmodel = cmodel;
	}

	public Appearance getAppearance(int direction, 
			int index, Color3f color, float transparency) {
		Texture tex = null;
		TexCoordGeneration tg = null;
		Appearance a = new Appearance();
		a.setMaterial(material);
		a.setTransparencyAttributes(transAttr);
		a.setPolygonAttributes(polyAttr);
		a.setColoringAttributes(colAttr);
		a.setRenderingAttributes(rendAttr);

		TextureUnitState[] tus = new TextureUnitState[3];
		tus[0] = new TextureUnitState(getTexture(direction, index, 0),
			texAttr, getTg(direction, index));
		tus[1] = new TextureUnitState(getTexture(direction, index, 1),
			texAttr, getTg(direction, index));
		tus[2] = new TextureUnitState(getTexture(direction, index, 2),
			texAttr, getTg(direction, index));
		a.setTextureUnitState(tus);
// 		a.setTexture(getTexture(direction, index));
// 		a.setTexCoordGeneration(getTg(direction, index));
// 		a.setTextureAttributes(texAttr);
		return a;
	}

	public void setTransparency(float f) {
		transAttr.setTransparency(f);
	}

	public Texture2D getTexture(int direction, int index, int channel) {
		switch(direction) {
			case X_AXIS: return xTextures[channel][index];
			case Y_AXIS: return yTextures[channel][index];
			case Z_AXIS: return zTextures[channel][index];
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

	TextureAttributes texAttr;
	TransparencyAttributes transAttr;
	PolygonAttributes polyAttr;
	Material material;
	ColoringAttributes colAttr;
	RenderingAttributes rendAttr;


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
		rendAttr.setCapability(RenderingAttributes.ALLOW_ALPHA_TEST_VALUE_WRITE);
		rendAttr.setAlphaTestValue(0.1f);
		rendAttr.setAlphaTestFunction(RenderingAttributes.GREATER);
	}

// 	private Appearance createAppearance(Texture tex, TexCoordGeneration tg, 
// 			Color3f color, float transparency) {
// 		Appearance a = new Appearance();
// 		a.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_READ);
// 		a.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_READ);
// 		a.setCapability(Appearance.ALLOW_RENDERING_ATTRIBUTES_READ);
// 		a.setCapability(Appearance.ALLOW_TEXTURE_WRITE);
// 		a.setCapability(Appearance.ALLOW_TEXGEN_WRITE);
// 
// 		TextureAttributes texAttr = new TextureAttributes();
// 		texAttr.setTextureMode(TextureAttributes.COMBINE);
// 		texAttr.setCombineRgbMode(TextureAttributes.COMBINE_MODULATE);
// 		//texAttr.setCombineRgbMode(TextureAttributes.COMBINE_REPLACE);
// 
// 		TransparencyAttributes t = new TransparencyAttributes();
// 		t.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
// 		t.setTransparencyMode(TransparencyAttributes.BLENDED);
// 		t.setTransparency(transparency);
// 
// 		PolygonAttributes p = new PolygonAttributes();
// 		p.setCullFace(PolygonAttributes.CULL_NONE);
// 
// 		Material m = new Material();
// 		m.setLightingEnable(false);
// 		
// 		ColoringAttributes c = new ColoringAttributes();
// 		c.setCapability(ColoringAttributes.ALLOW_COLOR_WRITE);
// 		c.setShadeModel(ColoringAttributes.FASTEST);
// 		if(color == null) {
// 			c.setColor(1f, 1f, 1f);
// 		} else {
// 			c.setColor(color);
// 		}
// 
// 		// Avoid rendering of voxels having an alpha value of zero
// 		RenderingAttributes ra = new RenderingAttributes();
// 		ra.setCapability(RenderingAttributes.ALLOW_ALPHA_TEST_VALUE_WRITE);
// 		ra.setAlphaTestValue(0.1f);
// 		ra.setAlphaTestFunction(RenderingAttributes.GREATER);
// 		
// 		a.setMaterial(m);
// 		a.setTransparencyAttributes(t);
// 		a.setTexture(tex);
// 		a.setTextureAttributes(texAttr);
// 		a.setTexCoordGeneration(tg);
// 		a.setPolygonAttributes(p);
// 		a.setColoringAttributes(c);
// 		a.setRenderingAttributes(ra);
// 		return a;
// 	}
// 
	private void loadAxis(int axis) {
		int rSize = 0;
		int sSize = 0;
		int tSize = 0;
		Texture2D[][] textures = null;

		switch (axis) {
		  case Z_AXIS:
			rSize = volume.zDim;
			sSize = volume.xTexSize;
			tSize = volume.yTexSize;
			textures = zTextures = new Texture2D[3][rSize];
			zTg = new TexCoordGeneration();
			zTg.setPlaneS(new Vector4f(volume.xTexGenScale, 0f, 0f, 0f));
			zTg.setPlaneT(new Vector4f(0f, volume.yTexGenScale, 0f, 0f));
			break;
		  case Y_AXIS:
			rSize = volume.yDim;
			sSize = volume.xTexSize;
			tSize = volume.zTexSize;
			textures = yTextures = new Texture2D[3][rSize];
			yTg = new TexCoordGeneration();
			yTg.setPlaneS(new Vector4f(volume.xTexGenScale, 0f, 0f, 0f));
			yTg.setPlaneT(new Vector4f(0f, 0f, volume.zTexGenScale, 0f));
			break;
		  case X_AXIS:
			rSize = volume.xDim;
			sSize = volume.yTexSize;
			tSize = volume.zTexSize;
			textures = xTextures = new Texture2D[3][rSize];
			xTg = new TexCoordGeneration();
			xTg.setPlaneS(new Vector4f(0f, volume.yTexGenScale, 0f, 0f));
			xTg.setPlaneT(new Vector4f(0f, 0f, volume.zTexGenScale, 0f));
			break;
		}

		int textureMode, componentType; 


		byte[][] data = new byte[3][];
		BufferedImage[] bIm = new BufferedImage[3];
		for(int i = 0; i < 3; ++i) {
			bIm[i] = new BufferedImage(sSize, tSize,
						BufferedImage.TYPE_BYTE_GRAY);
			WritableRaster raster = bIm[i].getRaster();
			DataBufferByte db = (DataBufferByte)raster.getDataBuffer();
			data[i] = db.getData();
		}


// 		WritableRaster raster = cmodel.
// 			createCompatibleWritableRaster(sSize, tSize); 
// 		DataBuffer db = raster.getDataBuffer();

// 		boolean rgb = db.getDataType() == DataBuffer.TYPE_INT;
		// otherwise, let's assume we're dealing with intensity data
		
// 		byte[][] data = ((DataBufferByte)db).getBankData();
		textureMode = Texture.INTENSITY;
		componentType = ImageComponent.FORMAT_CHANNEL8;
// 		Object data = null;
// 		if(rgb) {
// 			data = ((DataBufferInt)db).getData();
// 			textureMode = Texture.RGBA;
// 			componentType = ImageComponent.FORMAT_RGBA;
// 		} else {
// 			data = ((DataBufferByte)db).getData();
// 			textureMode = Texture.INTENSITY;
// 			componentType = ImageComponent.FORMAT_CHANNEL8;
// 		}

// 		BufferedImage bImage = 
// 			new BufferedImage(cmodel, raster, true, null); 

		for (int i = 0; i < rSize; i ++) { 
// 		for (int i = rSize / 2; i < rSize / 2 + 1; i ++) { 
			switch (axis) {
				case Z_AXIS: volume.loadZ(i, data); break;
				case Y_AXIS: volume.loadY(i, data); break;
				case X_AXIS: volume.loadX(i, data); break;
			}
			IJ.showProgress(i, rSize);

			for(int b = 0; b < data.length; b++) {
				boolean byRef = false;
				boolean yUp = true;
				Texture2D tex = new Texture2D(Texture.BASE_LEVEL, 
							textureMode, sSize, tSize);
				ImageComponent2D pArray = new ImageComponent2D(
					componentType, sSize, tSize, byRef, yUp);
if(i == rSize/2) {
BufferedImage tmp = new BufferedImage(sSize, tSize, BufferedImage.TYPE_INT_ARGB);
Graphics g = tmp.createGraphics();
g.drawImage(bIm[b], 0, 0, null);
g.dispose();
new ij.ImagePlus("channel " + b, tmp).show();
}
				pArray.set(bIm[b]);
// 				pArray.set(bImage);
			
				tex.setImage(0, pArray);
				tex.setEnable(true);
				tex.setMinFilter(Texture.BASE_LEVEL_LINEAR);
				tex.setMagFilter(Texture.BASE_LEVEL_LINEAR);
				
				tex.setBoundaryModeS(Texture.CLAMP);
				tex.setBoundaryModeT(Texture.CLAMP);

				textures[b][i] = tex;
			}
		} 
	} 
}

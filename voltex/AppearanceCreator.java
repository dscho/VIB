package voltex;

import java.awt.image.*;
import javax.media.j3d.*;
import javax.vecmath.*;

public class AppearanceCreator implements VolRendConstants {

	private int textureMode, componentType;

	private TexCoordGeneration xTg = new TexCoordGeneration();
	private TexCoordGeneration yTg = new TexCoordGeneration();
	private TexCoordGeneration zTg = new TexCoordGeneration();

	private BufferedImage xImage, yImage, zImage;
	private Object xData, yData, zData;

	private Volume volume;
	private static boolean[] defaultChannels = new boolean[]{true, true, true};

	public AppearanceCreator() {
		initAttributes(null, 0.1f);
	}

	public AppearanceCreator(Volume volume) {
		this(volume, null, 0.1f, defaultChannels);
	}

	public AppearanceCreator(Volume volume,
			Color3f color, float transparency, boolean[] ch) {
		initAttributes(color, transparency);
		setVolume(volume, ch);
	}

	public void release() {
		xTg = null; yTg = null; zTg = null;
		volume = null;
		xImage = null; yImage = null; zImage = null;
		xData = null; yData = null; zData = null;
	}

	public void setVolume(Volume v) {
		setVolume(v, defaultChannels);
	}

	public void setVolume(Volume v, boolean[] ch) {
		this.volume = v;
		this.volume.setChannels(ch);
		zTg = new TexCoordGeneration();
		zTg.setPlaneS(new Vector4f(v.xTexGenScale, 0f, 0f,
				-(float)(v.xTexGenScale * v.minCoord.x)));
		zTg.setPlaneT(new Vector4f(0f, v.yTexGenScale, 0f,
				-(float)(v.yTexGenScale * v.minCoord.y)));
		yTg = new TexCoordGeneration();
		yTg.setPlaneS(new Vector4f(v.xTexGenScale, 0f, 0f,
				-(float)(v.xTexGenScale * v.minCoord.x)));
		yTg.setPlaneT(new Vector4f(0f, 0f, v.zTexGenScale,
				-(float)(v.zTexGenScale * v.minCoord.z)));
		xTg = new TexCoordGeneration();
		xTg.setPlaneS(new Vector4f(0f, v.yTexGenScale, 0f,
				-(float)(v.yTexGenScale * v.minCoord.y)));
		xTg.setPlaneT(new Vector4f(0f, 0f, v.zTexGenScale,
				-(float)(v.zTexGenScale * v.minCoord.z)));
		boolean rgb = v.getDataType() == Volume.INT_DATA;
		boolean opaque = v.getTransparenyType() == Volume.OPAQUE;

		int bImgType = rgb ? BufferedImage.TYPE_INT_ARGB
					 : BufferedImage.TYPE_BYTE_GRAY;
		xImage = new BufferedImage(v.yTexSize, v.zTexSize, bImgType);
		yImage = new BufferedImage(v.xTexSize, v.zTexSize, bImgType);
		zImage = new BufferedImage(v.xTexSize, v.yTexSize, bImgType);

		DataBuffer dbx = xImage.getRaster().getDataBuffer();
		DataBuffer dby = yImage.getRaster().getDataBuffer();
		DataBuffer dbz = zImage.getRaster().getDataBuffer();
		if(rgb) {
			textureMode = opaque ? Texture.RGB : Texture.RGBA;
			componentType = ImageComponent.FORMAT_RGBA;
			xData = ((DataBufferInt)dbx).getData();
			yData = ((DataBufferInt)dby).getData();
			zData = ((DataBufferInt)dbz).getData();
		} else {
			textureMode = opaque ? Texture.LUMINANCE : Texture.INTENSITY;
			componentType = ImageComponent.FORMAT_CHANNEL8;
			xData = ((DataBufferByte)dbx).getData();
			yData = ((DataBufferByte)dby).getData();
			zData = ((DataBufferByte)dbz).getData();
		}
	}

	public Appearance getAppearance(int direction, int index) {
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

	public Texture2D getTexture(int axis, int index) {
		boolean byRef = false;
//		boolean byRef = true;
		boolean yUp = true;
		int sSize = 0, tSize = 0;
		BufferedImage bImage = null;
		switch (axis) {
			case Z_AXIS:
				volume.loadZ(index, zData);
				sSize = volume.xTexSize;
				tSize = volume.yTexSize;
				bImage = zImage;
				break;
			case Y_AXIS:
				volume.loadY(index, yData);
				sSize = volume.xTexSize;
				tSize = volume.zTexSize;
				bImage = yImage;
				break;
			case X_AXIS:
				volume.loadX(index, xData);
				sSize = volume.yTexSize;
				tSize = volume.zTexSize;
				bImage = xImage;
				break;
		}
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
		return tex;
	}

	public TexCoordGeneration getTg(int direction, int index) {
		switch(direction) {
			case X_AXIS: return xTg;
			case Y_AXIS: return yTg;
			case Z_AXIS: return zTg;
		}
		return null;
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
		texAttr.setPerspectiveCorrectionMode(TextureAttributes.NICEST);
		//texAttr.setCombineRgbMode(TextureAttributes.COMBINE_REPLACE);

		transAttr = new TransparencyAttributes();
		transAttr.setTransparency(0.1f);
		transAttr.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
		transAttr.setTransparencyMode(TransparencyAttributes.BLENDED);
		transAttr.setTransparency(transparency);

		polyAttr = new PolygonAttributes();
		polyAttr.setCullFace(PolygonAttributes.CULL_NONE);

		material = new Material();
// 		material.setLightingEnable(true);
		material.setLightingEnable(false);

		colAttr = new ColoringAttributes();
		colAttr.setCapability(ColoringAttributes.ALLOW_COLOR_WRITE);
		colAttr.setShadeModel(ColoringAttributes.NICEST);
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
}

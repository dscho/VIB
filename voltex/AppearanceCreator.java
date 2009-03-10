package voltex;

import ij3d.AxisConstants;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.ImageComponent;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.Material;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.RenderingAttributes;
import javax.media.j3d.TexCoordGeneration;
import javax.media.j3d.Texture;
import javax.media.j3d.Texture2D;
import javax.media.j3d.TextureAttributes;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Color3f;
import javax.vecmath.Vector4f;


/**
 * This class is a helper class whose main task is to create Appearance
 * objects for a specified axis and direction.
 * Each time when ask for an Appearance object, a new Appearance is created.
 * This is necessary, since each slice has another texture. However, all
 * the Appearance objects created by one instance of this class share their
 * Appearance attributes. In this way, it is easy and fast to change color,
 * transparency, etc.
 * 
 * @author Benjamin Schmid
 */
public class AppearanceCreator implements AxisConstants {

	/** The volume from which the textures are created */
	private Volume volume;

	/** Texture mode, e.g. Texture.RGB or so */
	private int textureMode;
	/** Component type, e.g. ImageComponent.FORMAT_RGBA or so */
	private int componentType;

	/** Indicates if transparent or opaque texture modes should be used */
	private boolean opaque = false;

	/** TexCoordGeneration object for x direction */
	private TexCoordGeneration xTg;
	/** TexCoordGeneration object for y direction */
	private TexCoordGeneration yTg;
	/** TexCoordGeneration object for z direction */
	private TexCoordGeneration zTg;

	/** Temporary BufferedImage in x direction */
	private BufferedImage xImage;
	/** Temporary BufferedImage in y direction */
	private BufferedImage yImage;
	/** Temporary BufferedImage in z direction */
	private BufferedImage zImage;

	/** Temporary DataBuffer Object in x direction */
	private Object xData;
	/** Temporary DataBuffer Object in y direction */
	private Object yData;
	/** Temporary DataBuffer Object in z direction */
	private Object zData;

	/** texture attributes */
	private TextureAttributes texAttr;
	/** transparency attributes */
	private TransparencyAttributes transAttr;
	/** polygon attributes */
	private PolygonAttributes polyAttr;
	/** material */
	private Material material;
	/** color attributes */
	private ColoringAttributes colAttr;
	/** rendering attributes */
	private RenderingAttributes rendAttr;

	/**
	 * Constructor.
	 * Initializes this AppearanceCreator with the given image data.
	 * @param volume
	 */
	public AppearanceCreator(Volume volume) {
		this(volume, null, 0.1f);
	}

	/**
	 * Initializes this AppearanceCreator with the given image data,
	 * color and transparency values.
	 * @param volume
	 * @param color
	 * @param transparency
	 */
	public AppearanceCreator(Volume volume,
			Color3f color, float transparency) {
		initAttributes(color, transparency);
		setVolume(volume);
	}

	/**
	 * Release all stored data.
	 */
	public void release() {
		xTg = null; yTg = null; zTg = null;
		volume = null;
		xImage = null; yImage = null; zImage = null;
		xData = null; yData = null; zData = null;
	}

	/**
	 * Change the image data of this AppearanceCreator
	 * @param v
	 */
	public void setVolume(Volume v) {
		this.volume = v;
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

	/**
	 * Returns the Appearance object for the specified direction and index.
	 * This is composed of the shared Appearance attributes plus the
	 * individual textures.
	 * @param direction
	 * @param index
	 * @return
	 */
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
		a.setTexCoordGeneration(getTg(direction));
		a.setTextureAttributes(texAttr);
		return a;
	}

	/**
	 * Set the transparency for all the textures that were loaded by this
	 * AppearanceCreator.
	 * @param f
	 */
	public void setTransparency(float f) {
		transAttr.setTransparency(f);
	}

	/**
	 * Set the threshold for all the textures that were loaded by this
	 * AppearanceCreator.
	 * Pixel values below the threshold are not rendered.
	 * @param f
	 */
	public void setThreshold(float f) {
		rendAttr.setAlphaTestValue(f);
	}

	/**
	 * Set the color for all the textures that were loaded by this
	 * AppearanceCreator.
	 * Pixel values below the threshold are not rendered.
	 * @param f
	 */
	public void setColor(Color3f c) {
		colAttr.setColor(c);
	}

	/**
	 * Returns the texture for the specified axis and slice
	 * @param axis
	 * @param index
	 * @return
	 */
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

	/**
	 * Get the TextureGeneration of the specified direction
	 * @param direction
	 * @param index
	 * @return
	 */
	public TexCoordGeneration getTg(int direction) {
		switch(direction) {
			case X_AXIS: return xTg;
			case Y_AXIS: return yTg;
			case Z_AXIS: return zTg;
		}
		return null;
	}

	/**
	 * Initialize the Appearance attributes.
	 * @param color
	 * @param transparency
	 */
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

package voltex;

import java.awt.Font;

import javax.vecmath.Point3f;
import javax.vecmath.Color3f;
import javax.vecmath.Vector3f;
import javax.media.j3d.LineArray;
import javax.media.j3d.Geometry;
import javax.media.j3d.Shape3D;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.Text3D;
import javax.media.j3d.Font3D;
import javax.media.j3d.FontExtrusion;
import javax.media.j3d.Appearance;
import javax.media.j3d.Material;
import javax.media.j3d.ColoringAttributes;


public class CoordinateSystem extends BranchGroup {
	
	public CoordinateSystem() {
		Shape3D lines = new Shape3D();
		lines.setGeometry(createGeometry());
		addChild(lines);
		
		// the appearance for all texts
		Appearance textAppear = new Appearance();
		ColoringAttributes textColor = new ColoringAttributes();
		textColor.setColor(1.0f, 0.0f, 0.0f);
		textAppear.setColoringAttributes(textColor);
 
		Transform3D translate = new Transform3D();

		translate.setTranslation(new Vector3f(0.1f, -0.05f, 0.0f));
		addText("x", translate, textAppear);
		translate.setTranslation(new Vector3f(-0.05f, 0.1f, 0.0f));
		addText("y", translate, textAppear);
		translate.setTranslation(new Vector3f(-0.05f, -0.05f, 0.1f));
		addText("z", translate, textAppear);
	}

	public void addText(String s,Transform3D translate,Appearance textAppear) {

		Transform3D scale = new Transform3D();
		// translation
		TransformGroup tg = new TransformGroup(translate);
		addChild(tg);
		// scale
		scale.setScale(0.05f);
		TransformGroup scaleTG = new TransformGroup(scale);
		tg.addChild(scaleTG);
		// text
		Font3D font3D = new Font3D(new Font("Helvetica", Font.PLAIN, 1),
                                    new FontExtrusion());
		Text3D textGeom = new Text3D(font3D, s);
		textGeom.setAlignment(Text3D.ALIGN_CENTER);
		Shape3D textShape = new Shape3D();
		textShape.setGeometry(textGeom);
		textShape.setAppearance(textAppear);
		scaleTG.addChild(textShape);
	}	

	public static Geometry createGeometry() {
		Point3f origin = new Point3f();
		Point3f onX = new Point3f(0.2f, 0, 0);
		Point3f onY = new Point3f(0, 0.2f, 0);
		Point3f onZ = new Point3f(0, 0, 0.2f);

		Point3f[] coords = {origin, onX, origin, onY, origin, onZ};
		int N = coords.length;
		
		Color3f colors[] = new Color3f[N];
		Color3f red = new Color3f(1.0f, 0.0f, 0.0f);
		for(int i=0; i<N; i++){
			colors[i] = red;
		}
		
		LineArray ta = new LineArray (N, 
					LineArray.COORDINATES | 
					LineArray.COLOR_3);
		ta.setCoordinates(0, coords);
		ta.setColors(0, colors);
		// initialize the geometry info here
		
		return ta;
	}
}

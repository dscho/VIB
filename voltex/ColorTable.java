package voltex;

import javax.vecmath.Color3f;
import java.util.Hashtable;
import java.util.Enumeration;

public class ColorTable extends Hashtable{
	
	public static final String BLUE = "BLUE";
	public static final String RED = "RED";
	public static final String GREEN = "GREEN";

	private static ColorTable instance = new ColorTable();

	private ColorTable() {
		super();
		put(BLUE, new Color3f(0.0f, 0.0f, 1.0f));
		put(RED, new Color3f(1.0f, 0.0f, 0.0f));
		put(GREEN, new Color3f(0.0f, 1.0f, 0.0f));
	}

	public static ColorTable instance() {
		return instance();
	}

	public static Color3f getColor(String name) {
		return (Color3f)instance.get(name);
	}

	public static String[] colors() {
		String[] ret = new String[instance.size()];
		Enumeration keyEn = instance.keys();
		int index = 0;
		while(keyEn.hasMoreElements()){
			ret[index++] = (String)keyEn.nextElement();
		}
		return ret;
	}
}

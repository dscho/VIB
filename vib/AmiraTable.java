package vib;

import ij.macro.Interpreter;
import ij.text.TextPanel;
import ij.text.TextWindow;
import java.text.DecimalFormat;
import java.util.Properties;

public class AmiraTable extends TextWindow {
	Properties properties;

	public AmiraTable(String title, String headings, String data) {
		this(title, headings, data, false);
	}

	public AmiraTable(String title, String headings, String data,
			boolean initParameters) {
		super(title, headings, data, 500, 400);
		properties = new Properties();
		if (initParameters) {
			int rowCount = getTextPanel().getLineCount();
			String p = "Parameters { "
				+ getParameterString(rowCount,
						headings.split("\t")) + " }";
			AmiraParameters parameters = new AmiraParameters(p);
			parameters.setParameters(properties);
		}
	}

	public static String getParameterString(int rows, String[] headings) {
		String p = "\tContentType \"HxSpreadSheet\",\n";

		DecimalFormat format = new DecimalFormat("0000");
		for (int i = 0; i < headings.length; i++)
			p += "\t__ColumnName" + format.format(i)
				+ "\"" + headings[i] + "\",\n";

		p += "\tnumRows " + rows + ",\n";

		return p;
	}

	public void show() {
		if (!Interpreter.isBatchMode())
			super.show();
	}
}

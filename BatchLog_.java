/*
 * Call this plugin to display a (short) message.
 *
 * When called in batch mode, it opens a window which will be reused in
 * subsequent calls.
 *
 * When not run in batch mode, it just calls IJ.showStatus().
 *
 */

import ij.IJ;
import ij.Macro;
import ij.macro.Interpreter;
import ij.plugin.PlugIn;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.TextArea;
import java.io.FileWriter;
import java.net.InetAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/*
 * TODO:
 * add progress bar
 * add stop button
 */
public class BatchLog_ implements PlugIn {
	static TextArea area;
	public static boolean alwaysShowWindow = true;

	// log file
	static String logFile;
	static DateFormat dateFormat;
	static String hostName;

	public void run(String message) {
		if (message.equals(""))
			message = Macro.getOptions();
		if (message.startsWith("logfile:")) {
			logFile = message.substring(8).trim();
			dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
			try {
				InetAddress l = InetAddress.getLocalHost();
				// work around "localhost"
				l = InetAddress.getByName(l.getHostAddress());
				hostName = l.getHostName();
			} catch (Exception e) {
				hostName = "<unnamed>";
			}
			return;
		}

		message += "\n";
		appendText(message);
		if (logFile != null) {
			try {
				FileWriter out = new FileWriter(logFile, true);
				message = dateFormat.format(new Date())
					+ " (" + hostName + "): " + message;
				out.write(message);
				out.close();
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	}

	final public static void appendText(String message) {
		if (alwaysShowWindow || Interpreter.isBatchMode()) {
			if (area == null) {
				area = new TextArea(25, 80);
				area.setText(message);
				area.setEditable(false);
				area.setVisible(true);
				Frame frame = new Frame("Log");
				frame.add(area);
				frame.setSize(new Dimension(400, 300));
				frame.doLayout();
				frame.show();
				area.setCaretPosition(Integer.MAX_VALUE);
			} else
				area.append(message);
		} else
			IJ.showStatus(message);
	}
}


package util;

import ij.IJ;
import java.io.File;
import java.util.ArrayList;
import java.util.Set;
import org.imagearchive.lsm.toolbox.Reader;
import org.imagearchive.lsm.toolbox.info.CZ_LSMInfo;
import org.imagearchive.lsm.toolbox.info.scaninfo.Track;
import org.imagearchive.lsm.toolbox.info.scaninfo.Recording;
import org.imagearchive.lsm.toolbox.info.scaninfo.DetectionChannel;
import org.imagearchive.lsm.toolbox.info.scaninfo.IlluminationChannel;

public class ChannelDataLSM {

	public static class LASER {

		public double power;
		public double wavelength;
	}
	double amplifierGain;
	double amplifierOffset;
	double detectorGain;
	double pinholeDiameter;
	String detectorName;
	String detectionChannelName;

	void setFromDetectionChannel(DetectionChannel dc) {
		amplifierGain = (Double) dc.records.get("AMPLIFIER_GAIN");
		amplifierOffset = (Double) dc.records.get("AMPLIFIER_OFFSET");
		detectorGain = (Double) dc.records.get("DETECTOR_GAIN");
		pinholeDiameter = (Double) dc.records.get("PINHOLE_DIAMETER");
		detectorName = (String) dc.records.get("DETECTOR_NAME");
		detectionChannelName = (String) dc.records.get("DETECTION_CHANNEL_NAME");
	}
	LASER[] lasers;

	static ChannelDataLSM[] getChannelsData(File f) {

		ArrayList<ChannelDataLSM> channels = new ArrayList<ChannelDataLSM>();

		Reader reader = new Reader();
		CZ_LSMInfo cz = reader.readCz(f);

		if (cz.scanInfo.recordings.size() == 0) {
			IJ.error("No recordings found in file.\n(File was '" + f + "')");
			return null;
		}
		if (cz.scanInfo.recordings.size() > 1) {
			IJ.error("Don't support multiple recordings in a file.\n(File was '" + f + "')");
			return null;
		}

		// Go through the scan info:
		Recording r = (Recording) cz.scanInfo.recordings.get(0);

		Track[] tracks = r.tracks;
		for (int ti = 0; ti < tracks.length; ++ti) {
			System.out.println("Track " + ti);
			Track t = tracks[ti];

			long trackAcquire = ((Long) t.records.get("ACQUIRE")).longValue();
			System.out.println("ACQUIRE for track " + ti + " is: " + trackAcquire);
			if (trackAcquire == 0) {
				continue;
			}

			ArrayList<LASER> lasers = new ArrayList<LASER>();

			/* If we want to know about the LASERs that are on at
			 * the time, look at the illumination channels. */

			for (int ii = 0; ii < t.illuminationChannels.length; ++ii) {
				IlluminationChannel ic = t.illuminationChannels[ii];
				System.out.println("  IlluminationChannel " + ii);
				Long acquireIllumination = (Long) ic.records.get("ACQUIRE");
				if (acquireIllumination == 0) {
					continue;
				}
				LASER laser = new LASER();
				laser.wavelength = (Double) ic.records.get("WAVELENGTH");
				laser.power = (Double) ic.records.get("POWER");
				lasers.add(laser);
				System.out.println("    WAVELENGTH: " + laser.wavelength);
				System.out.println("    POWER: " + laser.power);
			}

			/* Now consider any detection channel where acquire is
			 * not 0 (always -1)? */

			for (int ci = 0; ci < t.detectionChannels.length; ++ci) {
				System.out.println("  Detection Channel " + ci);
				DetectionChannel c = t.detectionChannels[ci];
				long detectorAcquire = ((Long) c.records.get("ACQUIRE")).longValue();
				System.out.println("  ACQUIRE for channel " + ci + " is: " + detectorAcquire);
				if (detectorAcquire == 0) {
					continue;
				}
				ChannelDataLSM cd = new ChannelDataLSM();
				cd.setFromDetectionChannel(c);
				cd.lasers = lasers.toArray(new LASER[0]);
				channels.add(cd);
				Set<String> set = c.records.keySet();
				/*
				for( Iterator<String> i = c.records.keySet().iterator(); i.hasNext(); ) {
				String key = i.next();
				System.out.println("      Key: "+key);
				}
				 */
				String[] keys = {
					"AMPLIFIER_GAIN",
					"AMPLIFIER_OFFSET",
					"DETECTOR_GAIN",
					"PINHOLE_DIAMETER",
					"DETECTOR_NAME",
					"DETECTION_CHANNEL_NAME"
				};
				for (int k = 0; k < keys.length; ++k) {
					Object value = c.records.get(keys[k]);
					if (value instanceof Double) {
						Double d = (Double) value;
						System.out.println("      " + keys[k] + " (Double) => " + value);
					} else if (value instanceof String) {
						String s = (String) value;
						System.out.println("      " + keys[k] + " (String) => " + value);
					}
				}
			}
		}

		return channels.toArray(new ChannelDataLSM[0]);
	}
}

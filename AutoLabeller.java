
import adt.Sparse3DByteArray;
import adt.Byte3DArray;
import adt.Unsparse3DByteArray;
import adt.ByteProbability;

import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.LinkedHashMap;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * User: Tom Larkworthy
 * Date: 12-Jul-2006
 * Time: 19:10:47
 */
public abstract class AutoLabeller {
	HashMap<Byte, MaterialStats> stats = new LinkedHashMap<Byte, MaterialStats>();

	//HashMap<Byte, HashMap<Byte, Double>> mixtureProbabilityCache = new HashMap<Byte, HashMap<Byte, Double>>();

	HashMap<Byte, Double>[] mixtureProbabilityCache;


	HashMap<Byte, Byte>[] mixtureFastProbabilityCache;

	final int numberOfMaterials;
	byte [] materialIds;

	//bounds of labels that are not external!
	int xMin = Integer.MAX_VALUE, xMax = Integer.MIN_VALUE;
	int yMin = Integer.MAX_VALUE, yMax = Integer.MIN_VALUE;
	int zMin = Integer.MAX_VALUE, zMax = Integer.MIN_VALUE;




	public AutoLabeller(String summeryLocation) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(summeryLocation));
		String line = in.readLine();


		TreeSet<MaterialStats> orderedSet = new TreeSet<MaterialStats>();
		while (line != null) {
			MaterialStats materialStats = readStats(line);
			orderedSet.add(materialStats);

			System.out.println("loaded " + materialStats.toString());


			line = in.readLine();
		}
		materialIds = new byte[orderedSet.size()];
		numberOfMaterials = materialIds.length;

		int index =0;
		for (MaterialStats materialStats : orderedSet) {
			System.out.println("adding " + materialStats);
			stats.put(materialStats.id, materialStats);
			materialIds[index++] = materialStats.id;
		}

		System.out.println("summary file read");

		mixtureProbabilityCache = new LinkedHashMap[256];
		mixtureFastProbabilityCache = new LinkedHashMap[256];

		for(int i=0; i<256; i++) {
			byte t = ByteProbability.toByte(i);

			System.out.println("intensity = " + i);

			LinkedHashMap<Byte, Double> mp = calcMixtureProb(t);
			mixtureProbabilityCache[t&0xFF] = mp;
            LinkedHashMap<Byte, Byte> fmp = new LinkedHashMap<Byte, Byte>();
			for (Byte materialId : stats.keySet()) {
				fmp.put(materialId, ByteProbability.toByte(mp.get(materialId)));
			}
			mixtureFastProbabilityCache[t&0xFF] = fmp;

			System.out.println("mp = " + mp);
			System.out.println("fmp = " + fmp);
		}

		System.out.flush();
	}



	/**
	 * calculates the probability of the pixel intensity occuring given the material
	 *
	 * @param pixel
	 */
	private LinkedHashMap<Byte, Double> calcMixtureProb(byte pixel) {
		LinkedHashMap<Byte, Double> ret = new LinkedHashMap<Byte, Double>();

		double total = 0;

		double totalVolume = 0;
		for (Byte materialId : stats.keySet()) {
			totalVolume += stats.get(materialId).volumeMean;
		}

		for (Byte materialId : stats.keySet()) {
			double prob = stats.get(materialId).getProProb(pixel);
			total += prob;
			ret.put(materialId, prob);
		}
		//normalize
		for (Byte materialId : stats.keySet()) {
			ret.put(materialId, ret.get(materialId) / total);
		}
		total=0;
		System.out.println("before volume = " + ret);
		//mix
		for (Byte materialId : stats.keySet()) {
			ret.put(materialId, ret.get(materialId) * stats.get(materialId).volumeMean / totalVolume);
			total += ret.get(materialId);
		}
		//normalize
		for (Byte materialId : stats.keySet()) {
			ret.put(materialId, ret.get(materialId) / total);
		}
        System.out.println("after volume = " + ret);


		return ret;
	}

	public HashMap<Byte, Double> getMixtureProbs(byte pixel){
		return mixtureProbabilityCache[pixel&0xFF];
	}

	public HashMap<Byte, Byte> getFastMixtureProbs(byte pixel){
		return mixtureFastProbabilityCache[pixel&0xFF];
	}

	private MaterialStats readStats(String line) {
		StringTokenizer tokenizer = new StringTokenizer(line, "\t", false);

		MaterialStats ret = new MaterialStats();

		ret.id = Byte.parseByte(tokenizer.nextToken());
		ret.name = tokenizer.nextToken();

		ret.intensityMean = Double.parseDouble(tokenizer.nextToken());
		ret.intensityVarience = Double.parseDouble(tokenizer.nextToken());

		ret.volumeMean = Double.parseDouble(tokenizer.nextToken());
		ret.volumeVariance = Double.parseDouble(tokenizer.nextToken());

		if (tokenizer.hasMoreTokens()) {
			String file = tokenizer.nextToken();

			AmiraMeshReader_ reader = new AmiraMeshReader_();
			reader.run(file);

			Byte3DArray pd;

			if (ret.id != 0) {
				pd = new Sparse3DByteArray();
			} else {
				pd = new Unsparse3DByteArray(reader.getWidth(), reader.getHeight(), reader.getStackSize());
			}

			int width = reader.getWidth();

			for (int z = 1; z <= reader.getStackSize(); z++) {
				byte[] pixels = (byte[]) reader.getStack().getProcessor(z).getPixels();
				for (int i = 0; i < pixels.length; i++) {
					byte pixel = pixels[i];
					if (pixel != 0)
						pd.put(i % width, i / width, z, pixel);
				}
			}

			ret.spatialDistribution = pd;

			if (ret.id != 0) {
				//if it is not an external spatial distribution then we should add the bounds to the bounding box
				xMin = Math.min(pd.getxMin(), xMin);
				xMax = Math.max(pd.getxMax(), xMax);
				yMin = Math.min(pd.getyMin(), yMin);
				yMax = Math.max(pd.getyMax(), yMax);
				zMin = Math.min(pd.getzMin(), zMin);
				zMax = Math.max(pd.getzMax(), zMax);
			}
			reader.close();
		}

		return ret;
	}

	public abstract void segment(SegmentatorModel model);

	public class MaterialStats implements Comparable{
		byte id;
		String name;

		double intensityMean;
		double intensityVarience;

		double volumeMean;
		double volumeVariance;

		Byte3DArray spatialDistribution;

		//returns a proportional probability of this intensity value being generated by this material
		public double getProProb(byte pixel) {
			int val = pixel & 0xFF;
			//return a guassian based estimate of likelyness
			return (Math.pow(Math.E, -(Math.pow(val - intensityMean, 2) / (2 * intensityVarience)))) / (Math.sqrt(intensityVarience) * Math.sqrt(2 * Math.PI));
		}

		public String toString() {
			StringBuffer buf = new StringBuffer();
			buf.append(name).append(" ");
			buf.append(id & 0xFF).append(" ");
			buf.append("[");
			buf.append(spatialDistribution.getxMin()).append(", ");
			buf.append(spatialDistribution.getxMax()).append(", ");
			buf.append(spatialDistribution.getyMin()).append(", ");
			buf.append(spatialDistribution.getyMax()).append(", ");
			buf.append(spatialDistribution.getzMin()).append(", ");
			buf.append(spatialDistribution.getzMax()).append("]");
			return buf.toString();
		}

		public int compareTo(Object o) {
			return ((MaterialStats)o).id < id ? 1:-1;
		}
	}
}

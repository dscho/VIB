
import adt.RunningStatistics;

import java.io.IOException;
import java.util.HashMap;

import ij.IJ;

/**
 * User: Tom Larkworthy
 * Date: 12-Jul-2006
 * Time: 19:12:08
 */
public class AutoLabellerNaive extends AutoLabeller {
	public AutoLabellerNaive(String summeryLocation) throws IOException {
		super(summeryLocation);
	}

	public void segment(SegmentatorModel model) {
        if(true) return;
		
        System.out.println("starting naive segmentation");
		int width = model.data.getWidth();
		int height = model.data.getHeight();
		int volume = model.data.getStackSize() * model.data.getWidth() * model.data.getHeight();



		//p(label|data) is proportioanl to
		//p(data|label)p(label)
		//where p(label) is what proportion on average is that label present
		HashMap<Byte, Double> pLabels = new HashMap<Byte, Double>();

		for (Byte materialId : stats.keySet()) {
			MaterialStats stat = stats.get(materialId);
			pLabels.put(materialId, stat.volumeMean / volume);

			//System.out.println("p(label= " + (materialId&0xFF)+") = " + stat.volumeMean / volume);
		}




		//we can assume the labels are blank allready
		//so only within the labels bounding box should we label
		for (int z = zMin; z <= zMax; z++) {
    	//for (int z = 65; z <= 70; z++) {
			IJ.showProgress(z, zMax);
            System.out.println("z = " + z);

			byte[] pixels = (byte[]) model.data.getStack().getProcessor(z).getPixels();
			for(int x=xMin; x<xMax; x++)
			for(int y=yMin; y<yMax; y++)
			{
				int i= y*width + x;

				//probabilities based on pixel intensities
				HashMap<Byte, Double> mixtureProbs = getMixtureProbs(pixels[i]);

				//now find which label is ML
				double ML = Double.MIN_VALUE;

				//System.out.printf("(%d, %d)", x, y);
				byte MLid = 0;

				for(int materialIndex=0; materialIndex < numberOfMaterials; materialIndex++) {
					byte materialId = materialIds[materialIndex];
                    Byte materialIdObj =  materialId;

					MaterialStats stat = stats.get(materialIdObj);


					//where p(data|label) depends on the spatial sposition, and intensity vals
					double spatialProb = stat.spatialDistribution.getDouble(x,y,z);
					//spatialProb = Math.min(spatialProb, .95);
					double intensityProb = mixtureProbs.get(materialIdObj);
					double pDataGLabel = spatialProb * intensityProb ;
				    double p = pLabels.get(materialIdObj) *  pDataGLabel;

					//System.out.printf("%d %f %f %f", materialId, spatialProb, intensityProb, p);
					if(p > ML){
						ML = p;
						MLid = materialId;
					}

				}
				((byte [] )model.getLabelImagePlus().getStack().getProcessor(z).getPixels())[i] = MLid;
			}
			model.updateSlice(z);
		}
	}
}

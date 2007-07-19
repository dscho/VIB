package vib.app.module;

import amira.AmiraTable;
import amira.AmiraTableEncoder;

import ij.ImagePlus;

import vib.TissueStatistics_;

import vib.app.ImageMetaData;

public class TissueStatistics extends Module {
	public String getName() { return "TissueStatistics"; }
	protected String getMessage() { return "Calculating tissue statistics"; }

	protected void run(State state, int index) {
		new ResampleLabels().runOnOneImage(state, index);

		prereqsDone(state, index);

		String statisticsPath = state.getStatisticsPath(index);
		String labelsPath = state.getImagePath(-1, index);
		if (state.upToDate(labelsPath, statisticsPath))
			return;

		ImagePlus labelField = state.getImage(labelsPath);
		TissueStatistics_.Statistics stats =
			TissueStatistics_.getStatistics(labelField);
		ImageMetaData metaData = new ImageMetaData();
		for (int i = 0; i < stats.materials.length; i++)
			metaData.setMaterial(stats.materials[i],
					(int)stats.count[i], stats.count[i] *
					stats.voxelVolume(),
					stats.centerX(i), stats.centerY(i),
					stats.centerZ(i));
		metaData.saveTo(statisticsPath);
	}
}

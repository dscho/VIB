package vib.app.module;

import amira.AmiraTable;
import amira.AmiraTableEncoder;

import ij.ImagePlus;

import vib.TissueStatistics_;

public class TissueStatistics extends Module {
	protected String getName() { return "TissueStatistics"; }
	protected String getMessage() { return "Calculating tissue statistics"; }

	protected void run(State state, int index) {
		new ResampleLabels().runOnOneImage(state, index);

		prereqsDone(state, index);

		String statisticsPath = state.getStatisticsPath(index);
		String labelsPath = state.getImagePath(-1, index);
		if (state.upToDate(labelsPath, statisticsPath))
			return;

		ImagePlus labelField = state.getImage(labelsPath);
		TissueStatistics_ t = new TissueStatistics_();
		AmiraTable statistics = t.calculateStatistics(labelField);
		statistics.hide();

		// TODO: move away from AmiraTable
		AmiraTableEncoder encoder = new AmiraTableEncoder(statistics);
		if(!encoder.write(statisticsPath))
			throw new RuntimeException("could not write "
					+ statisticsPath);
	}
}

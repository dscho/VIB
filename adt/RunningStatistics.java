package adt;

/**
 * Class used to track statistics with low memory footprint where large number data sets are expected
 * <p/>
 * User: Tom Larkworthy
 * Date: 12-Jul-2006
 * Time: 15:25:56
 */
public class RunningStatistics {
	double sum = 0;
	double sumSquared = 0;

	int n = 0;



	int seqLength = 0;
	double seqSum = 0;
	double seqSumSquared = 0;

	int seqN = 0;

	public void addData(double val) {
		sum += val;
		sumSquared += val * val;
		n++;
		seqLength++;
	}

	public void endOfSequence(){
    	seqSum += seqLength;
		seqSumSquared += seqLength * seqLength;
		seqLength = 0;
		seqN++;
	}

	public double getMean() {
		return sum / (n );
	}

	public double getVariance() {
		return sumSquared / n - getMean() * getMean();
	}
	public int getN(){
		return n;
	}

	public double getMeanSequenceLength(){
		return seqSum / seqN;
	}

	public double getVarianceSequenceLength(){
		return seqSumSquared / seqN - getMeanSequenceLength() * getMeanSequenceLength();
	}

	public static void main(String[] args) {
		RunningStatistics stats = new RunningStatistics();

		for (int i = 0; i < 1000; i++) {
			double number = Math.random();
			stats.addData(number);

			System.out.printf("%f %f %f\n", number, stats.getMean(), stats.getVariance());

		}

		stats = new RunningStatistics();

		for (int i = 0; i < 1000; i++) {
			double number = Math.random();
			stats.addData(number);

			System.out.printf("%f %f %f\n", number, stats.getMean(), stats.getVariance());

		}
	}
}

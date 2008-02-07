package distance;

public class MutualInformation implements PixelPairs {
	private int maximum;
	private int realMaximum = 0;
	private long joint[];
	private int count;

    public MutualInformation(int maximumValue) {
        maximum = maximumValue;
    }

    public MutualInformation() {
        this(256);
    }

	public void reset() {
		joint = new long[maximum * maximum];
		count = realMaximum = 0;
	}

	public void add(float v1, float v2) {
		int i1 = (int)v1;
		int i2 = (int)v2;
		//System.err.println("add " + i1 + ", " + i2);
		if(i2<0 || i2>=maximum)
			throw new RuntimeException("out of bounds");
		joint[i1 * maximum + i2]++;
		count++;
		if (realMaximum < i1) realMaximum = i1;
		if (realMaximum < i2) realMaximum = i2;
	}

	public float distance() {
		float h12 = 0, h1 = 0, h2 = 0;
		for(int i = 0; i < realMaximum; i++) {
			float p = joint[i] / (float)count;
			if(p > 0)
				h12 -= p * Math.log(p);
			
			long p1 = 0, p2 = 0;
			for(int j = 0; j < realMaximum; j++) {
				//System.err.print(" " + joint[i * maximum + j]);
				p1 += joint[i * maximum + j];
				p2 += joint[j * maximum + i];
			}
			//System.err.println("");
			if(p1 > 0)
				h1 -= p1 / (float)count
					* Math.log(p1 / (float)count);
			if(p2 > 0)
				h2 -= p2 / (float)count
					* Math.log(p2 / (float)count);
		}
		//System.err.println("mutinf: " + (h12-h1-h2));
		return h12-h1-h2;
	}
}


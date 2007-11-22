/*  -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package landmarks;

public class RegistrationResult implements Comparable {

	int overlay_width;
	int overlay_height;
	int overlay_depth;
	
	byte [][] transformed_bytes;
	byte [][] fixed_bytes;
	
	double score;
	double[] parameters;
	
	public int compareTo(Object otherRegistrationResult) {
		RegistrationResult other = (RegistrationResult) otherRegistrationResult;
		return Double.compare(score, other.score);
	}
	
	@Override
	public String toString() {
		return "score: " + score + " for parameters: " + parameters;
	}
}

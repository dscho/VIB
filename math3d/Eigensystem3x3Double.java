/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package math3d;

public class Eigensystem3x3Double {

	double [][] m;

	double [] eigenVectors;
	double [] eigenValues;

	public Eigensystem3x3Double(double [][] symmetricMatrix) {
		this.m = symmetricMatrix;
		if( m[0][1] != m[1][0] || m[0][2] != m[2][0] || m[1][2] != m[2][1] ) {
			throw new RuntimeException("Eigensystem3x3Double only works with symmetric matrices");
		}
	}

	public void getEvalues(double [] eigenValues) {
		eigenValues[0] = this.eigenValues[0];
		eigenValues[1] = this.eigenValues[1];
		eigenValues[2] = this.eigenValues[2];
	}

	public double [] getEvaluesCopy() {
		return eigenValues.clone();
	}

	public double [] getEvalues() {
		return eigenValues;
	}

	public boolean findEvalues() {

		eigenValues = new double[3];

		// Find the coefficients of the characteristic polynomial:
		// http://en.wikipedia.org/wiki/Eigenvalue_algorithm

		// The commented entries refer to:
		/*
                        A  B  C
                        B  D  E
                        C  E  F
		*/

		double a = -1;
		double b = 
			+ m[0][0]  // A
			+ m[1][1]  // D
			+ m[2][2]; // F
		double c =
		        + m[0][1] * m[0][1]  // + BB
			+ m[0][2] * m[0][2]  // + CC
			+ m[1][2] * m[1][2]  // + EE
			- m[0][0] * m[1][1]  // - AD
			- m[0][0] * m[2][2]  // - AF
			- m[1][1] * m[2][2]; // - DF

		// ADF - AEE - BBF + 2BCE - CCD

		double d = 
			+ m[0][0] * m[1][1] * m[2][2]     //  ADF
			- m[0][0] * m[1][2] * m[1][2]     // -AEE
			- m[0][1] * m[0][1] * m[2][2]     // -BBF
                        + 2 * m[0][1] * m[0][2] * m[1][2] // 2BCE
                        - m[0][2] * m[0][2] * m[1][1];    // -CCD

		/*
		System.out.println("a: "+a);
		System.out.println("b: "+b);
		System.out.println("c: "+c);
		System.out.println("d: "+d);
		*/

		// The odd casts here are because we automatically generate a
		// float version using sed 's/double/float/' :)

		final double third = (double) 0.333333333333333;

		// Now use the root-finding formula described here:
		// http://en.wikipedia.org/wiki/Cubic_equation#oot-finding_formula

		double q = (3*a*c - b*b) / (9*a*a);
		double r = (9*a*b*c - 27*a*a*d - 2*b*b*b) / (54*a*a*a);

		/*
		System.out.println("q is: "+q);
		System.out.println("r is: "+r);
		*/
		
		double discriminant = q*q*q + r*r;

		if( discriminant > 0 ) {

			/* Some of the roots are complex.  This should
			   never happen, since this is a real
			   symmetric matrix... */

			String problemMatrix = 
				"[" + m[0][0] + ", " + m[0][1] + ", " + m[0][2] +"]\n" +
				"[" + m[1][0] + ", " + m[1][1] + ", " + m[1][2] +"]\n" +
				"[" + m[2][0] + ", " + m[2][1] + ", " + m[2][2] +"]\n";

			throw new RuntimeException( "(BUG) Some complex roots found for matrix:\n" + problemMatrix );

		} else if( discriminant < 0 ) {

			double rootThree = (double)1.73205080756888;

			double innerSize = (double)Math.sqrt( r*r - discriminant );
			double innerAngle;

			if( r > 0 )
				innerAngle = (double)Math.atan( Math.sqrt(-discriminant) / r );
			else
				innerAngle = (double)( Math.PI - Math.atan( Math.sqrt(-discriminant) / -r ) );

			// So now s is the cube root of innerSize * e ^ (   innerAngle * i )
			//    and t is the cube root of innerSize * e ^ ( - innerAngle * i )

			double stSize = (double)Math.pow(innerSize,third);

			double sAngle = innerAngle / 3;
			double tAngle = - innerAngle / 3;

			double sPlusT = 2 * stSize * (double)Math.cos(sAngle);

			eigenValues[0] = sPlusT - (b / (3*a));

			double firstPart = - (sPlusT / 2) - (b / 3*a);

			double lastPart = - rootThree * stSize * (double)Math.sin(sAngle);

			eigenValues[1] = firstPart + lastPart;
			eigenValues[2] = firstPart - lastPart;

			return true;

		} else {

			// It's zero, so the second two solutions are
			// the same:

			double cubeRoot = (double)Math.pow(r,third);
			eigenValues[0] = 2 * cubeRoot - (b / (3*a));
			eigenValues[1] = - cubeRoot;
			eigenValues[2] = - cubeRoot;

			return true;

		}
	}
}

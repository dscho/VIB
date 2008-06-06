/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Some basic unit tests for the various Eigenvalue decomposition classes. */

package math3d;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import Jama.Matrix;
import Jama.EigenvalueDecomposition;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Random;

public class TestEigenvalueDecompositions {

	boolean printTimings = false;

	ArrayList<TestMatrixAndResult> realSymmetricTestCases;

	static class TestMatrixAndResult {

		TestMatrixAndResult( double [] matrixEntries,
				 double [] evaluesResult ) {
			this.m = new double[3][3];
			this.evaluesResult = new double[3];
			this.evectorsResult = new double[3][3];
			for(int i=0; i<9; ++i ) {
				m[i/3][i%3] = matrixEntries[i];
			}
			System.arraycopy(evaluesResult,0,this.evaluesResult,0,3);
		}
		double [][] m;
		double [] evaluesResult;
		double [][] evectorsResult;
		void checkEvalues(double [] calculatedEvalues) {
			assertTrue(calculatedEvalues.length == 3);
			double [] copiedCalculatedEvalues = new double[3];
			double [] copiedGroundEvalues = new double[3];
			System.arraycopy(calculatedEvalues,0,copiedCalculatedEvalues,0,3);
			System.arraycopy(evaluesResult,0,copiedGroundEvalues,0,3);
			Arrays.sort(copiedCalculatedEvalues);
			Arrays.sort(copiedGroundEvalues);
			for( int i = 0; i < 3; ++i ) {
				assertEquals(copiedGroundEvalues[i],copiedCalculatedEvalues[i],0.000001);
			}
		}
		float [][] getMatrixFloats() {
			float [][] result = new float[3][3];
			for(int i=0; i<9; ++i ) {
				result[i/3][i%3] = (float)m[i/3][i%3];
			}
			return result;
		}
		float [] getEvaluesFloats() {
			float [] result = new float[3];
			for(int i=0; i<3; ++i ) {
				result[i] = (float)evaluesResult[i];
			}
			return result;
		}
	}

	@Before public void setUp() {
		realSymmetricTestCases = new ArrayList<TestMatrixAndResult>();

		// An example I solved in axiom:
		double [] m2 =
			{ 1, 2, 3,
			  2, 4, 5,
			  3, 5, 6 };
		double [] evalues2 =
			{ -0.51572947158925714026, 11.344814282762077688, 0.17091518882717945217 };
		realSymmetricTestCases.add( new TestMatrixAndResult(m2,evalues2) );

		// An example from http://en.wikipedia.org/wiki/Eigenvalue_algorithm
		double [] m1 =
			{ 0,  1, -1,
			  1,  1,  0,
			  -1, 0,  1 };
		double [] evalues1 =
			{ 2, 1, -1 };
		realSymmetricTestCases.add( new TestMatrixAndResult(m1,evalues1) );
		
		// Now generate lots of random real symmetric matrices
		// (and assume that the Jama version is correct!)
		
		Random r = new Random(123456789);
		for( int i = 0; i < 200000; ++i ) {
			double [] m = new double[9];
			m[0] = r.nextDouble();
			m[1] = m[3] = r.nextDouble();
			m[2] = m[6] = r.nextDouble();
			m[4] = r.nextDouble();
			m[5] = m[7] = r.nextDouble();
			m[8] = r.nextDouble();
			
			double [][] mm = new double[3][3];
			for( int d = 0; d < 9; ++d )
				mm[d/3][d%3] = m[d];

			// Find the eigenvalues with Jama:
			Matrix M = new Matrix(mm);
			EigenvalueDecomposition E = new EigenvalueDecomposition(M);
			double [] eigenValues = E.getRealEigenvalues();

			realSymmetricTestCases.add( new TestMatrixAndResult(m,eigenValues) );
		}
	}

	@Test
	public void testJama() {
	       
		long startTime = System.currentTimeMillis();

		for( Iterator<TestMatrixAndResult> i = realSymmetricTestCases.iterator();
		     i.hasNext(); ) {

			TestMatrixAndResult mr = i.next();

			Matrix M = new Matrix(mr.m);
			EigenvalueDecomposition E = new EigenvalueDecomposition(M);

			double[] result = E.getImagEigenvalues();
			boolean foundImaginaryEigenvalues = false;
			for (int e = 0; e < result.length; ++e) {
				// There should be no complex eigenvalues:
				assertTrue(result[e] == 0);
			}
			
			result = E.getRealEigenvalues();
			mr.checkEvalues(result);
		}

		if (printTimings) System.out.println("testJama: "+(System.currentTimeMillis()-startTime)/1000.0);
	}

	@Test
	public void testJacobiDouble() {
	       
		long startTime = System.currentTimeMillis();

		for( Iterator<TestMatrixAndResult> i = realSymmetricTestCases.iterator();
		     i.hasNext(); ) {

			TestMatrixAndResult mr = i.next();

			JacobiDouble jc=new JacobiDouble(mr.m,50);
			double [] result = jc.getEigenValues();
			mr.checkEvalues(result);
		}

		if (printTimings) System.out.println("testJacobiDouble: "+(System.currentTimeMillis()-startTime)/1000.0);
	}

	@Test
	public void testJacobiFloat() {

		long startTime = System.currentTimeMillis();
	       
		for( Iterator<TestMatrixAndResult> i = realSymmetricTestCases.iterator();
		     i.hasNext(); ) {

			TestMatrixAndResult mr = i.next();

			float [][] mfloat=new float[3][3];
			for( int e=0; e<9; ++e )
				mfloat[e/3][e%3] = (float)mr.m[e/3][e%3];

			JacobiFloat jc=new JacobiFloat(mfloat,50);
			float [] result = jc.getEigenValues();
			double [] resultDoubles = new double[3];
			for(int e=0; e<3; ++e)
				resultDoubles[e] = (double)result[e];
			mr.checkEvalues(resultDoubles);
		}

		if (printTimings) System.out.println("testJacobiFloat: "+(System.currentTimeMillis()-startTime)/1000.0);
	}
 
	@Test
	public void testRootFinding() {

		long startTime = System.currentTimeMillis();

		for( Iterator<TestMatrixAndResult> i = realSymmetricTestCases.iterator();
		     i.hasNext(); ) {

			TestMatrixAndResult mr = i.next();

			Eigensystem3x3Double e = new Eigensystem3x3Double(mr.m);
			boolean result = e.findEvalues();
			assert(result);
			if(result)
				mr.checkEvalues(e.eigenValues);
		}

		if (printTimings) System.out.println("testRootFinding: "+(System.currentTimeMillis()-startTime)/1000.0);
	}
}

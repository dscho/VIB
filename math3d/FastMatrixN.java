package math3d;

import ij.IJ;

public class FastMatrixN {
	public static void invert(double[][] matrix) {
		invert(matrix, false);
	}

	public static void invert(double[][] matrix, boolean showStatus) {
		int M = matrix.length;

		if (M != matrix[0].length)
			throw new RuntimeException("invert: no square matrix");

		double[][] other = new double[M][M];
		for (int i = 0; i < M; i++)
			other[i][i] = 1;

		// empty lower left triangle
		for (int i = 0; i < M; i++) {
			if (showStatus)
				IJ.showStatus("invert matrix: "
						+ i + "/" + (2 * M));
			// find pivot
			int p = i;
			for (int j = i + 1; j < M; j++)
				if (Math.abs(matrix[j][i]) >
						Math.abs(matrix[p][i]))
					p = j;

			if (p != i) {
				double[] d = matrix[p];
				matrix[p] = matrix[i];
				matrix[i] = d;
				d = other[p];
				other[p] = other[i];
				other[i] = d;
			}

			// normalize
			if (matrix[i][i] != 1.0) {
				double f = matrix[i][i];
				for (int j = i; j < M; j++)
					matrix[i][j] /= f;
				for (int j = 0; j < M; j++)
					other[i][j] /= f;
			}

			// empty rest of column
			for (int j = i + 1; j < M; j++) {
				double f = matrix[j][i];
				for (int k = i; k < M; k++)
					matrix[j][k] -= f * matrix[i][k];
				for (int k = 0; k < M; k++)
					other[j][k] -= f * other[i][k];
			}
		}

		// empty upper right triangle
		for (int i = M - 1; i > 0; i--) {
			if (showStatus)
				IJ.showStatus("invert matrix: "
						+ (2 * M - i) + "/" + (2 * M));
			for (int j = i - 1; j >= 0; j--) {
				double f = matrix[j][i] / matrix[i][i];
				for (int k = i; k < M; k++)
					matrix[j][k] -= f * matrix[i][k];
				for (int k = 0; k < M; k++)
					other[j][k] -= f * other[i][k];
			}
		}

		// exchange
		for (int i = 0; i < M; i++)
			matrix[i] = other[i];
	}

	public static double[][] clone(double[][] matrix) {
		int M = matrix.length, N = matrix[0].length;
		double[][] result = new double[M][N];
		for (int i = 0; i < M; i++)
			System.arraycopy(matrix[i], 0, result[i], 0, N);
		return result;
	}

	public static double[][] times(double[][] m1, double[][] m2) {
		int K = m2.length;
		if (m1[0].length != m2.length)
			throw new RuntimeException("rank mismatch");
		int M = m1.length, N = m2[0].length;
		double[][] result = new double[M][N];
		for (int i = 0; i < M; i++)
			for (int j = 0; j < N; j++)
				for (int k = 0; k < K; k++)
					result[i][j] += m1[i][k] * m2[k][j];
		return result;
	}
	
	/**
	 * @return The upper triangular form resulting from the 
	 * Cholensky decomposition
	 * @see http://en.wikipedia.org/wiki/Cholesky_decomposition
	 */
	static double[][] choleskyDecomposition(double[][] m){
		
		if(m.length != m[0].length){
			throw new RuntimeException("matrix must be symmetric and positive definite");
		}
		int N = m.length;
		double[][] l = new double[N][N];
		for(int i=0;i<N;i++)
			for(int j=0;j<N;j++)
				l[i][j] = 0.0;
		double sum = 0.0;
		for(int i=0;i<N;i++){
			// l[i][i]
			sum = 0.0;
			for(int k = 0;k<i;k++){
				sum += l[k][i] * l[k][i];
			}
			if(m[i][i] - sum < 0){
				throw new RuntimeException("matrix must be symmetric and positive definite");
			}
			l[i][i] = Math.sqrt(m[i][i] - sum);
			// l[i][j]
			for(int j=i+1;j<N;j++){
				sum = 0.0;
				for(int k=0;k<i;k++){
					sum += l[k][j] * l[k][i];					
				}
				l[i][j] = (m[i][j] - sum)/l[i][i];
			}
		}		
		return l;
	}

	public static double[][] transpose(double[][]m){
		double[][] ret = new double[m[0].length][m.length];
		for(int i=0;i<ret.length;i++){
			for(int j=0;j<ret[i].length;j++){
				ret[i][j] = m[j][i];
			}
		}
		return ret;
	}
	
	/**
	 * Solve Ax = b
	 * @param A matrix to be applied
	 * @param b result
	 * @return x
	 * @see http://planetmath.org/?op=getobj&from=objects&id=1287
	 */
	public static double[] solve(double[][]A, double[] b){
		
		// get the cholesky decomposition of A which is in upper triangle form
		double [][] U = choleskyDecomposition(A);
		double [][] L = transpose(U);
		// first solve Ly = b for y
		double[] y = new double[b.length];
		for(int i=0;i<y.length;i++){
			double sum = 0.0;
			for(int j=0;j<i;j++){
				sum += L[i][j]*y[j];
			}
			y[i] = (b[i] - sum)/L[i][i];
		}
		// then solve Ux = y for x
		double[] x = new double[b.length];
		for(int i=x.length-1;i>=0;i--){
			double sum = 0.0;
			for(int j=i+1;j<x.length;j++){
				sum += U[i][j]*x[j];
			}
			x[i] = (y[i] - sum)/U[i][i];
		}
		return x;
	}
	
	/**
	 * Calculates b in Ax = b
	 * @param A matrix to apply
	 * @param x vector
	 * @return b
	 */
	public static double[] apply(double[][]A, double x[]){
		int m = A.length;
		int n = A[0].length;
		double[] b = new double[x.length];
		for(int i=0;i<m;i++){
			b[i] = 0.0;
			for(int j=0;j<n;j++){
				b[i] += A[i][j] * x[j];
			}
		}		
		return b;
	}

	static void print(double[][] m) {
		for (int i = 0; i < m.length; i++) {
			for (int j = 0; j < m[i].length; j++)
				System.out.print(" " + (float)m[i][j]);
			System.out.println("");
		}
		System.out.println();
	}
	
	public static void main(String[] args) {
		double[][] m = {
			{1, 2, 3, 2},
			{-1, 0, 2, -3},
			{-2, 1, 1, 1},
			{0, -2, 3, 0}};

		double[][] m1 = clone(m);
		invert(m1);

		double[][] m2 = times(m, m1);
		print(m2);
		
		double[][] k = {
				{2,1,0},
				{1,2,0},
				{0,0,3}
		};
		System.out.println("Original matrix ");
		print(k);
		double[][] l = choleskyDecomposition(k);
		System.out.println("Upper triangular form u of cholesky decomposition ");
		print(l);
		double[][] l_t = transpose(l);
		System.out.println("Transposed form u^T of u ");
		print(l_t);
		double[][] prod = times(l_t,l);
		System.out.println("Finally the product of the u^T and u, which should give the original matrix ");
		print(prod);
		
		double[] x = new double[]{1.0,2.0,3.0};
		System.out.println("A vector x: x = [1.0 2.0 3.0]^T\n");
		double[] b = apply(k, x);
		System.out.println("Applying the original matrix to x gives b: ");
		System.out.println("[" + b[0] + " " + b[1] + " " + b[2] + "]\n");
		double[] x_n = solve(k,b);
		System.out.println("Now solve Ax = b for x and see if it is the original x");
		System.out.println("x_new = [" + x_n[0] + " " + x_n[1] + " " + x_n[2] + "]\n");
		
	}
}


package mops;

import ij.measure.Calibration;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import vib.InterpolatedImage;

public class DoGDetector3D
{
	private float minContrast; 
	private final float maxCurvatureRatio = 10;
	
	Octave octave;
	
	/**
	 * detected candidates as float triples 0=>x, 1=>y, 2=>scale index
	 */
	final ArrayList< Feature > candidates = new ArrayList< Feature >();
	public ArrayList< Feature > getCandidates()
	{
		return candidates;
	}
	
	/**
	 * Constructor
	 */
	public DoGDetector3D()
	{
		octave = null;
	}
	
	public void run( Octave octave )
	{
		// scale the minimal contrast threshold proposed in Lowe (2004, p. 11) with respect
		// to the step size of the scale octave (see Lowe 2004, p. 6) 
		//minContrast = 0.03f * ( ( float )Math.pow( 2.0, 1.0 / ( sigma.length - 3 ) ) - 1.0f );
		
		// less restrictive contrast filter
		minContrast = 0.025f * ( octave.k - 1.0f );
		
		this.octave = octave;
		octave.dog();
	}
	
	public void detectCandidates( int di )
	{
		InterpolatedImage d = octave.dog[ di ];
		InterpolatedImage.Iterator it = d.iterator( false, 1, 1, 1, d.getWidth() - 2, d.getHeight() - 2, d.getDepth() - 2 );
		Calibration c = d.getImage().getCalibration();
I:		while(it.next() != null) {
			boolean isMax = true, isMin = true;
			float v = d.getNoCheckFloat(it.i, it.j, it.k);
			for(int i = 0; i < 40; i++)
			{
				int ms = i / 27 - 1;
				int mz = ( i % 27 ) / 9 - 1;
				int my = ( i % 9 ) / 3 - 1;
				int mx = ( i % 3 ) - 1;
				float v2 = octave.dog[ di + ms ].getNoInterpolFloat(
					it.i + mx, it.j + my, it.k + mz);
				if(v2 > v) isMax = false;
				if(v2 < v) isMin = false;
				if(!( isMin || isMax) )
					continue I;
			}
			for(int i = 41; i < 81; i++)
			{
				int ms = i / 27 - 1;
				int mz = ( i % 27 ) / 9 - 1;
				int my = ( i % 9 ) / 3 - 1;
				int mx = ( i % 3 ) - 1;
				float v2 = octave.dog[ di + ms ].getNoInterpolFloat(
					it.i + mx, it.j + my, it.k + mz);
				if(v2 > v) isMax = false;
				if(v2 < v) isMin = false;
				if(!( isMin || isMax) )
					continue I;
			}
			// TODO interpolate subpixel location
			// TODO reject low contrast candidates
			// TODO implement filter Curvatures
			// TODO implement orientation estimation
			
			if ( filterCurvature( di, it.i, it.j, it.k ) )
				candidates.add( new Feature( it.i, it.j, it.k, octave.sigma[ di ] ) );
		}
	}
	
	/*
	 * float indices of return list are
	 * 0 -> x, 1 -> y, 2 -> z, 3 -> scale index
	 */
	public void detectCandidates() {
		candidates.clear();
		InterpolatedImage.Iterator it = null;
		for( int i = 1; i < dog.length - 1; i++ )
		{
			Filter.suppNonExtremum( dog[i] );
			it = dog[i].iterator();
			float v;
			while(it.next() != null) {
				v = dog[i].getNoCheckFloat(it.i, it.j, it.k);
				if(v != 0)
					l.add(new float[] {it.i,it.j,it.k,i});
			}
		}
		return l;
	}
	

}

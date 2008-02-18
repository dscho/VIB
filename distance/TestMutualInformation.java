/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Some very basic unit tests for the distance.MutualInformation class */

package distance;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class TestMutualInformation extends BaseOfTests {
	
	MutualInformation exampleMeasure;

	@Test
	public void testMutualInformationValue() {
	    
		// Try an example slightly adapted from David MacKay's book:

		exampleMeasure = new MutualInformation( 1, 4, 4 );

		exampleMeasure.reset();

		addMacKayExample(exampleMeasure);
	    
		float mi = exampleMeasure.mutualInformation();
		assertEquals( mi, 0.375 );

		exampleMeasure.reset();
		addMacKayExample(exampleMeasure);
		float miAfterReset = exampleMeasure.mutualInformation();
		assertEquals( miAfterReset, 0.375 );
	}

	@Test
	public void testEntropies() {

		exampleMeasure = new MutualInformation( 1, 4, 4 );
		exampleMeasure.reset();

		addMacKayExample(exampleMeasure);

		float mi = exampleMeasure.mutualInformation();

		float h1 = exampleMeasure.getEntropy1();
		assertEquals( h1, 1.75 );

		float h2 = exampleMeasure.getEntropy2();
		assertEquals( h2, 2 );

		float h12 = exampleMeasure.getJointEntropy();
		assertEquals( h12, 3.375 );
	}

	@Test
	public void testFillIn8Bit() {
		
		exampleMeasure = new MutualInformation();
		exampleMeasure.reset();

		addUniform8Bit(exampleMeasure);

		/* Strictly speaking we shouldn't be examining the
		   internal state, but it's important that binning in
		   the 8 bit case still works properly... */

		for( int i = 0; i < 256 * 256; ++i ) {
			assertEquals( exampleMeasure.joint[i], 1 );
		}

		float mi = exampleMeasure.mutualInformation();

		assertEquals( 8,  exampleMeasure.getEntropy1() );
		assertEquals( 8,  exampleMeasure.getEntropy2() );
		assertEquals( 16, exampleMeasure.getJointEntropy() );

		assertEquals( 0, mi );
	}
}

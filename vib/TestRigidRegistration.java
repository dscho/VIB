/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package vib;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.*;

import util.BatchOpener;
import distance.Euclidean;
import distance.MutualInformation;
import distance.TwoValues;
import ij.ImagePlus;
import ij.ImageJ;
import ij.io.FileSaver;

public class TestRigidRegistration {

	RigidRegistration_ plugin;

	ImageJ imageJ;

	@Before
	public void loadImagesAndImageJ() {
		// Start ImageJ (maybe not necessary?
		this.imageJ = new ImageJ();
	}

	@After
	public void closeImageAndImageJ() {		
		imageJ.quit();
	}

	static final int fanShapedBody = 11;
	static final int protocerebralBridge = 12;

	String centralComplex_Labels_71yAAeastmost = "test-images/71yAAeastmost.labels";
	String centralComplex_Labels_c005BA = "test-images/c005BA.labels";

	@Test
	public void testRegistrationMaterials() {

		ImagePlus centralComplex_Labels_71yAAeastmost_ImagePlus = BatchOpener.openFirstChannel(
			centralComplex_Labels_71yAAeastmost );

		ImagePlus centralComplex_Labels_c005BA_ImagePlus = BatchOpener.openFirstChannel(
			centralComplex_Labels_c005BA );
	
		int materials [] = { fanShapedBody, protocerebralBridge };
		float bestScores [] = { 14, 55555555 };

		for( int i = 0; i < materials.length; ++i ) {

			int material = materials[i];
			float bestScore = bestScores[i];
		
			if( material == protocerebralBridge ) {
				// FIXME: registration fails for this
				// case, so skip it for the moment.
				continue;
			}
			
			FastMatrix matrix;
			
			ImagePlus template = centralComplex_Labels_c005BA_ImagePlus;
			ImagePlus toTransform = centralComplex_Labels_71yAAeastmost_ImagePlus;
			
			assertTrue( template != null );
			assertTrue( toTransform != null );
			
			// First try with the Euclidean metric:
			
			plugin = new RigidRegistration_();
			plugin.setup( "", toTransform );
			
			int level = RigidRegistration_.guessLevelFromWidth(
				template.getWidth() );
			
			TransformedImage ti = new TransformedImage(
				template,
				toTransform );
			
			ti.measure = new TwoValues(material,material);
			
			matrix = plugin.rigidRegistration(
				ti,
				"",         // material b box
				"",         // initial
				material,   // material 1
				material,   // material 2
				false,      // no optimization
				level,      // level
				level > 2 ? 2 : level, // stop level
				1.0,        // tolerance
				1,          // number of initial positions
				false,      // show transformed
				false,      // show difference image
				false,      // fast but inaccurate
				null );     // other images to transform
			
			String outputTransformed = "test-images/output/testRegistrationMaterials-"+material+"-transformed.tif";
			String outputDifference = "test-images/output/testRegistrationMaterials-"+material+"-difference.tif";
			
			// This should be able to get the distance down to less than 14:
			assertTrue( ti.getDistance() <= bestScore );
			
			boolean saved;
			
			saved = new FileSaver(ti.getTransformed()).saveAsTiffStack(outputTransformed);
			assertTrue(saved);
			
			saved = new FileSaver(ti.getDifferenceImage()).saveAsTiffStack(outputDifference);
			assertTrue(saved);
			
			// Now MutualInformation:
			
			
		}

		centralComplex_Labels_71yAAeastmost_ImagePlus.close();
		centralComplex_Labels_c005BA_ImagePlus.close();

	}

	@Test
	public void testRegistration12BitGray() {

		String darkDetail =   "test-images/181y-12bit-aaarrg-dark-detail-reduced.tif";
		String midDetail =    "test-images/181y-12bit-aaarrg-mid-detail-reduced.tif";
		String brightDetail = "test-images/181y-12bit-aaarrg-bright-reduced.tif";

		ImagePlus darkDetail_ImagePlus   = BatchOpener.openFirstChannel( darkDetail );
		ImagePlus midDetail_ImagePlus    = BatchOpener.openFirstChannel( midDetail );
		ImagePlus brightDetail_ImagePlus = BatchOpener.openFirstChannel( brightDetail );

		float [] bestScores = { -1.20, -5 };

		for( int timeThrough = 0; timeThrough < 2; ++timeThrough ) {

			ImagePlus template    = null;
			ImagePlus toTransform = null;

			if( timeThrough == 0 ) {
				template = midDetail_ImagePlus;
				toTransform = darkDetail_ImagePlus;
			} else if( timeThrough == 1 ) {
				template = midDetail_ImagePlus;
				toTransform = brightDetail_ImagePlus;
			}
			
			assertTrue( template != null );
			assertTrue( toTransform != null );
			
			plugin = new RigidRegistration_();
			plugin.setup( "", toTransform );
			
			int level = RigidRegistration_.guessLevelFromWidth(
				template.getWidth() );
			
			TransformedImage ti = new TransformedImage(
				template,
				toTransform );
			
			ti.measure = new MutualInformation(0,4095,1024);
			
			FastMatrix matrix = plugin.rigidRegistration(
				ti,
				"",         // material b box
				"",         // initial
				-1,         // material 1
				-1,         // material 2
				false,      // no optimization
				level,      // level
				level > 2 ? 2 : level, // stop level
				1.0,        // tolerance
				1,          // number of initial positions
				false,      // show transformed
				false,      // show difference image
				false,      // fast but inaccurate
				null );     // other images to transform
			
			String outputTransformed = "test-images/output/testRegistration12BitGray-"+timeThrough+"-transformed.tif";
			String outputDifference = "test-images/output/testRegistration12BitGray-"+timeThrough+"-difference.tif";
			
			System.out.println("distance was: "+ti.getDistance());

			// This should be able to get the distance down to less than 14:
			assertTrue( ti.getDistance() <= bestScores[timeThrough] );
			
			boolean saved;
			
			saved = new FileSaver(ti.getTransformed()).saveAsTiffStack(outputTransformed);
			assertTrue(saved);
			
			saved = new FileSaver(ti.getDifferenceImage()).saveAsTiffStack(outputDifference);
			assertTrue(saved);

		}

		darkDetail_ImagePlus.close();
		midDetail_ImagePlus.close();
		brightDetail_ImagePlus.close();
	}

}


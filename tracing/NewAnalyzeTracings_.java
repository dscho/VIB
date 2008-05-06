/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package tracing;

import java.awt.*;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.*;
import java.util.Enumeration;

import amira.AmiraParameters;

import ij.*;
import ij.io.*;
import ij.gui.*;
import ij.plugin.PlugIn;

import vib.transforms.OrderedTransformations;

import util.FileAndChannel;
import vib.oldregistration.Bookstein_FromMarkers;

import util.BatchOpener;

class ImagesFromLine {
	
	String lineName;
	ArrayList<String> baseNames = new ArrayList<String>();
	
}

public class NewAnalyzeTracings_ implements PlugIn {

		
	String [] labelNames = { "Ellipsoid Body",
				 "Fan-Shaped Body",
				 "Noduli",
				 "Protocerebral Bridge" };
		
	int [] labelIndices =  { 9,
				 11,
				 10,
				 12 };
	
      	public Connectivity buildGraph( String tracesObjFileName, String labelsFileName ) {
		
		Connectivity result = new Connectivity();		
		int x, y, z;

		/* First load the traces file:




		 */


		
		/* This nonsense is all about loading the label files: */

		ImagePlus labels = BatchOpener.openFirstChannel(labelsFileName);
		if( labels == null )
			throw new RuntimeException("Couldn't open labels file "+labelsFileName);
		ImageStack labelStack=labels.getStack();

		byte[][] label_data=new byte[templateDepth][];
		for( z = 0; z < templateDepth; ++z )
			label_data[z] = (byte[])labelStack.getPixels( z + 1 );
		
		AmiraParameters parameters = new AmiraParameters(labels);
		int materials = parameters.getMaterialCount();
		result.materialNames = new String[256];
		result.materialNameToIndex = new Hashtable< String, Integer >();
		for( int i = 0; i < materials; ++i ) {
			result.materialNames[i] = parameters.getMaterialName(i);
			result.materialNameToIndex.put(result.materialNames[i],new Integer(i));
			System.out.println("Material: "+i+" is "+result.materialNames[i]);
		}
		
		result.redValues = new int[materials];
		result.greenValues = new int[materials];
		result.blueValues = new int[materials];
		
		for( int i=0; i < materials; i++ ) {
			
			double[] c = parameters.getMaterialColor(i);
			
			result.redValues[i] = (int)(255*c[0]);
			result.greenValues[i] = (int)(255*c[1]);
			result.blueValues[i] = (int)(255*c[2]);
		}


	
		
		
		
		
	}
	
	public void run( String argument ) {
		
		// String baseDirectory = "/media/WD USB 2/corpus/central-complex/";
		String baseDirectory = "/home/mark/tmp-corpus/";
		ArrayList<ImagesFromLine> lines = new ArrayList<ImagesFromLine>();
		
		ImagesFromLine linec5 = new ImagesFromLine();
		linec5.lineName = "c005";
		linec5.baseNames.add("c005BA");
		linec5.baseNames.add("c005BB");
		linec5.baseNames.add("c005BC");
		linec5.baseNames.add("c005BE");
		linec5.baseNames.add("c005BF");
		linec5.baseNames.add("c5xUAS-CD8GFP-40x-central-complex-BF");
		linec5.baseNames.add("c5xUAS-lacZ-40x-cc-BB");
		linec5.baseNames.add("c5xUAS-lacZ-40x-cc-BC");		
		lines.add(linec5);
		
		ImagesFromLine line210y = new ImagesFromLine();
		line210y.lineName = "210y";
		line210y.baseNames.add("210y-40x-central-complex-CA");
		line210y.baseNames.add("210y-40x-central-complex-CB");
		line210y.baseNames.add("210y-40x-central-complex-CD");
		line210y.baseNames.add("210y-40x-central-complex-CE");
		line210y.baseNames.add("210yAC");
		line210y.baseNames.add("210yAD");
		line210y.baseNames.add("210yAE");
		line210y.baseNames.add("210yAO");
		line210y.baseNames.add("210yAP");
		lines.add(line210y);
		
		ImagesFromLine line71y = new ImagesFromLine();
		line71y.lineName = "71y";
		line71y.baseNames.add("71yAAeastmost");
		line71y.baseNames.add("71yABwestmost");
		line71y.baseNames.add("71yAF");
		line71y.baseNames.add("71yAM");
		line71y.baseNames.add("71yAN");
		line71y.baseNames.add("71yAQ");
		line71y.baseNames.add("71yAR");
		lines.add(line71y);
		
		ImagesFromLine linec61 = new ImagesFromLine();
		linec61.lineName = "c61";
		linec61.baseNames.add("c061AG");
		linec61.baseNames.add("c061AH");
		linec61.baseNames.add("c061AI()");
		linec61.baseNames.add("c061AK");
		linec61.baseNames.add("c061AL");
		linec61.baseNames.add("c061AU");
		lines.add(linec61);
		
		for( Iterator<ImagesFromLine> lineIterator = lines.iterator();
		     lineIterator.hasNext(); ) {
			
			ImagesFromLine line = lineIterator.next();
			
			System.out.println("Looking at line: "+line.lineName);
			
			for( Iterator<String> baseNameIterator = line.baseNames.iterator();
			     baseNameIterator.hasNext(); ) {
				
				String baseName = baseNameIterator.next();
				
				System.out.println("  Image basename: "+baseName );
				
				File lsmFile = new File( baseDirectory, baseName + ".lsm" );
				if( ! lsmFile.exists() )
					continue;
				
				System.out.println("!!!");
				
				// Load labels and traces.obj ...
				
				
				
				
				
			}
		}
	}
}

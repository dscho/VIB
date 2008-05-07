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

public class NewAnalyzeTracings_ implements PlugIn, TraceLoaderListener {

	String [] labelNames = { "Ellipsoid Body",
				 "Fan-Shaped Body",
				 "Noduli",
				 "Protocerebral Bridge" };
		
	int [] labelIndices =  { 9,
				 11,
				 10,
				 12 };
	
	class NewGraphNode {
		public NewGraphNode() { }
		public NewGraphNode(int x, int y, int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
		int x = -1;
		int y = -1;
		int z = -1;
		NewGraphNode linkedTo [] = null;
		public boolean equals(Object other) {
			NewGraphNode o=(NewGraphNode)other;
			return x == o.x && y == o.y && z == o.z;
		}
	}

	public int positionToKey( int x, int y, int z ) {
		return x + y * width + z * width * height;
	}

	int width = -1, height = -1, depth = -1;
	float spacing_x = Float.MIN_VALUE;
	float spacing_y = Float.MIN_VALUE;
	float spacing_z = Float.MIN_VALUE;

	ArrayList<NewGraphNode> verticesInObjOrder;
	Hashtable<Integer,NewGraphNode> positionToNode = new Hashtable<Integer,NewGraphNode>();
	int numberOfVertices = -1;
	ArrayList<ArrayList<NewGraphNode>> links; 
	
	public void gotVertex( int vertexIndex,
			       float x_scaled, float y_scaled, float z_scaled,
			       int x_image, int y_image, int z_image ) {
		
		if( width < 0 ||
		    height < 0 ||
		    depth < 0 ||
		    spacing_x == Float.MIN_VALUE ||
		    spacing_y == Float.MIN_VALUE ||
		    spacing_z == Float.MIN_VALUE ) {

			throw new RuntimeException("Some metadata was missing from the comments before the first vertex.");
		}

		verticesInObjOrder.add( new NewGraphNode( x_image, y_image, z_image ) );
	}

	public void gotLine( int fromVertexIndex, int toVertexIndex ) {
		if( links == null ) {
			numberOfVertices = verticesInObjOrder.size() - 1;
			links = new ArrayList<ArrayList<NewGraphNode>>(numberOfVertices);
			for( int i = 0; i <= numberOfVertices; ++i )
				links.add(new ArrayList<NewGraphNode>());
		}

		ArrayList<NewGraphNode> fromLinks = links.get(fromVertexIndex);
		ArrayList<NewGraphNode> toLinks = links.get(toVertexIndex);

		NewGraphNode toVertex=verticesInObjOrder.get(toVertexIndex);
		NewGraphNode fromVertex=verticesInObjOrder.get(fromVertexIndex);

		if( ! fromLinks.contains( toVertex ) )
			fromLinks.add( toVertex );

		if( ! toLinks.contains( fromVertex ) )
			toLinks.add( fromVertex );
	}

	public void gotWidth( int width ) {
		this.width = width;
	}

	public void gotHeight( int height ) {
		this.height = height;
	}

	public void gotDepth( int depth ) {
		this.depth = depth;
	}

	public void gotSpacingX( float spacing_x ) {
		this.spacing_x = spacing_x;
	}

	public void gotSpacingY( float spacing_y ) {
		this.spacing_y = spacing_y;
	}

	public void gotSpacingZ( float spacing_z ) {
		this.spacing_z = spacing_z;
	}	

      	public Connectivity buildGraph( File tracesObjFile, File labelsFile ) {

		String tracesObjFileName = tracesObjFile.getAbsolutePath();
		String labelsFileName = labelsFile.getAbsolutePath();

		/* First load the traces file: */

		// The indices in the .obj begin at 1, so put in a dummy node at 0:
		
		verticesInObjOrder = new ArrayList<NewGraphNode>();
		verticesInObjOrder.add( new NewGraphNode() );

		System.out.println("Loading traces file: "+tracesObjFileName);
			
		boolean success = SinglePathsGraph.loadWithListener( tracesObjFileName, this );

		if( ! success ) {
			throw new RuntimeException("Failed to load traces");
		}
		
		System.out.println("Finished loading: "+(verticesInObjOrder.size()-1)+" vertices found");

		long linksBothWays = 0;
		for( int i = 1; i < verticesInObjOrder.size(); ++i ) {
			NewGraphNode v = verticesInObjOrder.get(i);
			ArrayList<NewGraphNode> linkedTo = links.get(i);
			int l = linkedTo.size();
			linksBothWays += l;			
			NewGraphNode [] a=new NewGraphNode[l];
			v.linkedTo = linkedTo.toArray(a);
		}

		links = null;

		System.out.println("And set the links in the NewGraphNodes: "+linksBothWays);
		

		// Now we want to index by position rather than vertex index:
		boolean first = true;
		for( Iterator<NewGraphNode> nodeIterator = verticesInObjOrder.iterator();
		     nodeIterator.hasNext(); ) {
			NewGraphNode n = nodeIterator.next();
			if( first ) { first = false; continue; }
			int k = positionToKey( n.x, n.y, n.z );
			positionToNode.put( k, n );
		}

		verticesInObjOrder = null;

		System.out.println("Added vertices to the hash, now has: "+positionToNode.size()+" entries");
		
		/* And now the labels file: */

		ImagePlus labels = BatchOpener.openFirstChannel(labelsFileName);
		if( labels == null )
			throw new RuntimeException("Couldn't open labels file "+labelsFileName);
		ImageStack labelStack=labels.getStack();

		byte[][] label_data=new byte[depth][];
		for( int z = 0; z < depth; ++z )
			label_data[z] = (byte[])labelStack.getPixels( z + 1 );

		AmiraParameters parameters = new AmiraParameters(labels);
		int materials = parameters.getMaterialCount();
		String [] materialNames = new String[256];
		Hashtable<String,Integer> materialNameToIndex = new Hashtable< String, Integer >();
		for( int i = 0; i < materials; ++i ) {
			materialNames[i] = parameters.getMaterialName(i);
			materialNameToIndex.put(materialNames[i],new Integer(i));
			System.out.println("Material: "+i+" is "+materialNames[i]);
		}
		
		int [] redValues = new int[materials];
		int [] greenValues = new int[materials];
		int [] blueValues = new int[materials];

		for( int i=0; i < materials; i++ ) {
			
			double[] c = parameters.getMaterialColor(i);
			
			redValues[i] = (int)(255*c[0]);
			greenValues[i] = (int)(255*c[1]);
			blueValues[i] = (int)(255*c[2]);
		}

		for( int a = 0; a < labelIndices.length; ++a ) {
			int labelIndex = labelIndices[a];
			String labelPrettyString = labelNames[a];

			System.out.println("   Dealing with label index "+labelIndex+", name: "+labelPrettyString);			

			ArrayList<NewGraphNode> neuropilEdgePoints = new ArrayList<NewGraphNode>();

			for( int z = 0; z < depth; ++z )
				for( int y = 0; y < height; ++y )
					for( int x = 0; x < width; ++x ) {
						if( label_data[z][y*width+x] != labelIndex )
							continue;
						int k = positionToKey(x,y,z);
						NewGraphNode n = positionToNode.get(k);
						if( n == null )
							continue;
						/* So now we have a traced point in the right neuropil region.
						   We only care about edge points, though, so check that it has
						   a neighbour that's in the exterior. */
						NewGraphNode [] linkedNodes = n.linkedTo;
						for( int i = 0; i < linkedNodes.length; ++i ) {
							NewGraphNode l = linkedNodes[i];
							if( label_data[l.z][l.y*width+l.x] == 0 ) {
								neuropilEdgePoints.add(l);
								break;
							}
						}
					}
			System.out.println("   Found "+neuropilEdgePoints.size()+" points on the edge of the "+labelPrettyString);
		}

		return null;
		
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
				/*
				if( ! lsmFile.exists() )
					continue;
				*/
				
				File tracesObjFile = new File( baseDirectory, baseName + ".traces.obj" );
				if( ! lsmFile.exists() )
					continue;

				File labelsFile = new File( baseDirectory, baseName + ".labels" );
				if( ! lsmFile.exists() )
					continue;

				System.out.println("!!!");
				
				// Load labels and traces.obj ...
				
				buildGraph( tracesObjFile, labelsFile );
				
				
				
			}
		}
	}
}

/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Some very basic unit tests for the distance.MutualInformation class */

package distance;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class BaseOfTests {

        /* This example corresponds to Exercise 8.6 on p140, with the
           following probabilities:

             P(x,y) |               x
                    |    1      2      3      4    |
             ---------------------------------------
                 1  |   1/8   1/16   1/32   1/32   |
              y  2  |  1/16    1/8   1/32   1/32   |
                 3  |  1/16   1/16   1/16   1/16   |
                 4  |   1/4      0      0      0   |
             ---------------------------------------
             
             This gives us the entropies:
     
                hx = 1.75
                hy = 2
                
                hxy = 3.375
     
                hxgiveny = 1.375
     
             So I(X;Y) is 0.375

        */

	public void addMacKayExample(PixelPairs measure) {		
		measure.add( 1, 1 );
		measure.add( 1, 1 );
		measure.add( 1, 1 );
		measure.add( 1, 1 );

		measure.add( 1, 2 );
		measure.add( 1, 2 );
        
		measure.add( 1, 3 );
		measure.add( 1, 3 );

		measure.add( 1, 4 );
		measure.add( 1, 4 );
		measure.add( 1, 4 );
		measure.add( 1, 4 );
		measure.add( 1, 4 );
		measure.add( 1, 4 );
		measure.add( 1, 4 );
		measure.add( 1, 4 );

		measure.add( 2, 1 );
		measure.add( 2, 1 );

		measure.add( 2, 2 );
		measure.add( 2, 2 );
		measure.add( 2, 2 );
		measure.add( 2, 2 );
		
		measure.add( 2, 3 );
		measure.add( 2, 3 );
		
		measure.add( 3, 1 );

		measure.add( 3, 2 );

		measure.add( 3, 3 );
		measure.add( 3, 3 );

		measure.add( 4, 1 );

		measure.add( 4, 2 );

		measure.add( 4, 3 );
		measure.add( 4, 3 );
	}

	/* Add uniform grid of values to the measure */

	public void addUniform8Bit(PixelPairs measure) {
		for(int i=0;i<256;++i)
			for(int j=0;j<256;++j)
				measure.add(i,j);
	}
}

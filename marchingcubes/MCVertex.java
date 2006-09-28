/**
 * class representing a vertex (position & wiehgt) used for marching cubes
 * @author GERVAISE Raphael & RICHARD Karen
 */

package marchingcubes;

import javax.vecmath.Point3f;

public class MCVertex {
    public static int DEFAULT_WEIGHT = 0;
    
    // an objet is used instead of an int to keep "pointer" relations 
	// betweeen vertexes
    protected MCInt weight;
    
    protected Point3f position;    
    
    /**
     * contructor of a MCVertex object
     * @param position position of the vertex in the 3D space
     */
    public MCVertex(Point3f position) {
        this(position, DEFAULT_WEIGHT);
    }
    
    /**
     * contructor of MCVertex object
     * @param position position of the vertex in the 3D space
     * @param weight weight of the vertex (value of the function at this point)
     */
    public MCVertex(Point3f position, int weight) {        
        this.position = position;
        this.weight = new MCInt(weight);        
    }    
    
    /**
     * returns the weight of the vertex
     * @return the weight of the vertex
     */
    public int weight() {
        return this.weight.value;
    }
}

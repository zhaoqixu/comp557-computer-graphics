package comp557.a4;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

/**
 * Class for a plane at y=0.
 * 
 * This surface can have two materials.  If both are defined, a 1x1 tile checker 
 * board pattern should be generated on the plane using the two materials.
 */
public class Plane extends Intersectable {
    
	/** The second material, if non-null is used to produce a checker board pattern. */
	Material material2;
	
	/** The plane normal is the y direction */
	public static final Vector3d n = new Vector3d( 0, 1, 0 );
    
    /**
     * Default constructor
     */
    public Plane() {
    	super();
    }

    private boolean quadrants(Point3d p) {
    	Point3d tmp = new Point3d(p);
    	tmp.x = (tmp.x % 2 + 2) %2;
    	tmp.z = (tmp.z % 2 + 2) %2;
    	return (tmp.x >= 1 && tmp.z >=1) || (tmp.x <= 1 && tmp.z <= 1);
    }
        
    @Override
    public void intersect( Ray ray, IntersectResult result ) {
    
        // TODO: Objective 4: intersection of ray with plane
    	if (ray.viewDirection.y !=0 ) {
    		double t = -ray.eyePoint.y / ray.viewDirection.y;
    		if (t > 1e-6 && t < result.t) {
    			result.t = t;
    			result.p.scaleAdd(t, ray.viewDirection, ray.eyePoint);
    			result.n.set(Plane.n);
    			if (quadrants(result.p)) result.material = material;
    			else result.material = material2 != null? material2 : material;
    		}
    	}
    }
    
}

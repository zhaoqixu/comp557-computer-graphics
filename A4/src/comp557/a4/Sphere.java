package comp557.a4;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

/**
 * A simple sphere class.
 */
public class Sphere extends Intersectable {
    
	/** Radius of the sphere. */
	public double radius = 1;
    
	/** Location of the sphere center. */
	public Point3d center = new Point3d( 0, 0, 0 );
    
    /**
     * Default constructor
     */
    public Sphere() {
    	super();
    }
    
    /**
     * Creates a sphere with the request radius and center. 
     * 
     * @param radius
     * @param center
     * @param material
     */
    public Sphere( double radius, Point3d center, Material material ) {
    	super();
    	this.radius = radius;
    	this.center = center;
    	this.material = material;
    }
    
    @Override
    public void intersect( Ray ray, IntersectResult result ) {
    
        // TODO: Objective 2: intersection of ray with sphere
    	Vector3d ecDir = new Vector3d();
    	ecDir.sub(center, ray.eyePoint);
    	if (ecDir.dot(ray.viewDirection) <= 0) return;
    	Vector3d ceDir = new Vector3d();
    	ceDir.scale(-1, ecDir);
    	double a = ray.viewDirection.dot(ray.viewDirection);
    	double b = ray.viewDirection.dot(ceDir);
    	double c = ceDir.dot(ceDir) - radius * radius;
    	double d = b*b - a*c;
    	if (d >= 0) {
    		double t = (-b - Math.sqrt(d))/a;
    		if (t > 1e-6 && t < result.t) {
    			result.t = t;
    			result.p.scaleAdd(t, ray.viewDirection, ray.eyePoint);
    			result.n.sub(result.p, center);
    			result.n.normalize();
    			result.material = material;
    		}
    	}
    }
    
}

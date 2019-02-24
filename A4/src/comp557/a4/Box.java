package comp557.a4;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

/**
 * A simple box class. A box is defined by it's lower (@see min) and upper (@see max) corner. 
 */
public class Box extends Intersectable {

	public Point3d max;
	public Point3d min;
	
    /**
     * Default constructor. Creates a 2x2x2 box centered at (0,0,0)
     */
    public Box() {
    	super();
    	this.max = new Point3d( 1, 1, 1 );
    	this.min = new Point3d( -1, -1, -1 );
    }	

	@Override
	public void intersect(Ray ray, IntersectResult result) {
		// TODO: Objective 6: intersection of Ray with axis aligned box
		
		double txmin, txmax, tymin, tymax, tzmin, tzmax, tmp, tmin, tmax;
		Point3d e = new Point3d(ray.eyePoint);
		Vector3d v = new Vector3d(ray.viewDirection);
		txmin = (min.x - e.x)/v.x;
		txmax = (max.x - e.x)/v.x;
		tymin = (min.y - e.y)/v.y;
		tymax = (max.y - e.y)/v.y;
		tzmin = (min.z - e.z)/v.z;
		tzmax = (max.z - e.z)/v.z;
		if (txmin > txmax) {
			tmp = txmin;
			txmin = txmax;
			txmax = tmp;
		}
		if (tymin > tymax) {
			tmp = tymin;
			tymin = tymax;
			tymax = tmp;
		}
		if (tzmin > tzmax) {
			tmp = tzmin;
			tzmin = tzmax;
			tzmax = tmp;
		}
		tmin = Math.max(txmin, Math.max(tymin, tzmin));
		tmax = Math.min(txmax, Math.min(tymax, tzmax));
		if (tmax < tmin || tmin < 1e-6 || tmin >= result.t) return;
		else {
			result.t = tmin;
			result.p.scaleAdd(result.t, ray.viewDirection, ray.eyePoint);
			if (txmin > tymin && txmin > tzmin) {
				result.n.set(1,0,0);
				if (e.x < min.x) result.n.negate();
			} else if (tymin > txmin && tymin > tzmin) {
				result.n.set(0,1,0);
				if (e.y < min.y) result.n.negate();
			} else {
				result.n.set(0,0,1);
				if (e.z < min.z) result.n.negate();
			}
			result.material = material;
		}
	}

}

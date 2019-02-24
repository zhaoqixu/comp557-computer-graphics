package comp557.a4;

import java.util.HashMap;
import java.util.Map;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

public class Mesh extends Intersectable {
	
	/** Static map storing all meshes by name */
	public static Map<String,Mesh> meshMap = new HashMap<String,Mesh>();
	
	/**  Name for this mesh, to allow re-use of a polygon soup across Mesh objects */
	public String name = "";
	
	/**
	 * The polygon soup.
	 */
	public PolygonSoup soup;

	public Mesh() {
		super();
		this.soup = null;
	}			
		
	@Override
	public void intersect(Ray ray, IntersectResult result) {
		
		// TODO: Objective 9: ray triangle intersection for meshes
		for(int[] face : soup.faceList){
			Point3d a = soup.vertexList.get(face[0]).p;
			Point3d b = soup.vertexList.get(face[1]).p;
			Point3d c = soup.vertexList.get(face[2]).p;	
			
			Vector3d ab = new Vector3d();
			ab.sub(b, a);
			Vector3d bc = new Vector3d();
			bc.sub(c, b);
			Vector3d ca = new Vector3d();
			ca.sub(a, c);
			Vector3d ac = new Vector3d();
			ac.sub(c, a);
			Vector3d n = new Vector3d();
			n.cross(ab, ac);
			n.normalize();
			Vector3d ea = new Vector3d();
			ea.sub(a,ray.eyePoint);
			
			double t = ea.dot(n)/ray.viewDirection.dot(n);
			if (t > 1e-6 && t < result.t) {
				Point3d x = new Point3d();
				x.scaleAdd(t, ray.viewDirection, ray.eyePoint);
				Vector3d ax = new Vector3d();
				Vector3d bx = new Vector3d();
				Vector3d cx = new Vector3d();
				ax.sub(x, a);
				bx.sub(x, b);
				cx.sub(x, c);
				ax.cross(ab, ax);
				bx.cross(bc, bx);
				cx.cross(ca, cx);
				if (ax.dot(n) > 1e-6 && bx.dot(n) > 1e-6 && cx.dot(n) > 1e-6) {
					result.t = t;
	    			result.p.scaleAdd(t, ray.viewDirection, ray.eyePoint);
	    			result.n.set(n);
	    			result.material = material;
				}
			}
		}
	}

}

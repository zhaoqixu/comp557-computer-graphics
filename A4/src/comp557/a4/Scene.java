package comp557.a4;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.vecmath.Color3f;
import javax.vecmath.Color4f;
import javax.vecmath.Vector3d;

/**
 * Simple scene loader based on XML file format.
 */
public class Scene {
    
    /** List of surfaces in the scene */
    public List<Intersectable> surfaceList = new ArrayList<Intersectable>();
	
	/** All scene lights */
	public Map<String,Light> lights = new HashMap<String,Light>();

    /** Contains information about how to render the scene */
    public Render render;
    
    /** The ambient light colour */
    public Color3f ambient = new Color3f();
    
    private double u,v,l,b,r_l,t_b;
    
//  private final Color3f WHITE = new Color3f(1,1,1);
  private final Color3f BLACK = new Color3f(0,0,0);
  
  /** The unit vectors at e */
  Vector3d unitU = new Vector3d();
  Vector3d unitV = new Vector3d();
  Vector3d unitW = new Vector3d();;

    /** 
     * Default constructor.
     */
    public Scene() {
    	this.render = new Render();
    }
    
    /**
     * renders the scene
     */
    public void render(boolean showPanel) {
 
        Camera cam = render.camera; 
        int w = cam.imageSize.width;
        int h = cam.imageSize.height;
        
        render.init(w, h, showPanel);
        
        precompute(cam, w, h);
        Color3f c = new Color3f();
        Color3f tmpc = new Color3f();
        Color3f magnitude = new Color3f(0.5f, 0.5f, 0.5f);
        Color3f reflectc = new Color3f();
        Ray ray = new Ray();
        Ray reflectRay = new Ray();
        IntersectResult result = new IntersectResult();
        IntersectResult reflectResult = new IntersectResult();
        double[] offset = new double[2];
        int grid = (int)Math.sqrt(render.samples);
        double jitter = 0.5;
        
        for ( int i = 0; i < h && !render.isDone(); i++ ) {
            for ( int j = 0; j < w && !render.isDone(); j++ ) {
            	                
            	// TODO: Objective 8: do antialiasing by sampling more than one ray per pixel
            	c.set(BLACK);
            	for (int m = 0; m < grid; m++) {
            		jitter = render.jitter? Math.random() * 0.5 : 0.5;
            		offset[0] = (double)(m + jitter)/grid; 
            		for (int n = 0; n < grid; n++) {
            			jitter = render.jitter? Math.random() * 0.5 : 0.5;
                		offset[1] = (double)(n + jitter)/grid; 
		                // TODO: Objective 1: generate a ray (use the generateRay method)
		            	generateRay(i, j, offset, cam, ray);
		                // TODO: Objective 2: test for intersection with scene surfaces
		            	result = new IntersectResult();
		            	for (Intersectable surface : surfaceList) surface.intersect(ray, result);
		
		                // TODO: Objective 3: compute the shaded result for the intersection point (perhaps requiring shadow rays)
		            	tmpc.set(render.bgcolor);
		            	
		            	if(result.material != null){
		            		tmpc.set(result.material.diffuse.x*ambient.x, result.material.diffuse.y*ambient.y, result.material.diffuse.z*ambient.z);
		            		Vector3d l = new Vector3d();
		            		Vector3d v = new Vector3d();
		            		Vector3d half = new Vector3d();
	            			v.sub(cam.from, result.p);
	            			v.normalize();
		            		for(Light light : lights.values()){
		            			Ray shadowRay = new Ray();
		            			Vector3d viewDirection = new Vector3d();
		            			viewDirection.sub(light.from, result.p);
		            			shadowRay.set(result.p, viewDirection);
		            			IntersectResult shadowResult = new IntersectResult();
		            			if (!inShadow(result, light, shadowResult, shadowRay)) {
				            		l.sub(light.from, result.p);
			            			l.normalize();
			            			half.add(v, l);
			            			half.normalize();
			            			Color3f tmp = new Color3f(result.material.diffuse.x*light.color.x, result.material.diffuse.y*light.color.y, result.material.diffuse.z*light.color.z);
			            			tmp.scale((float) Math.max(0, result.n.dot(l)));
			            			tmpc.add(tmp);
			            			tmp = new Color3f(result.material.specular.x*light.color.x, result.material.specular.y*light.color.y, result.material.specular.z*light.color.z);
			            			tmp.scale((float)Math.pow(Math.max(0, result.n.dot(half)), result.material.shinyness));
			            			tmpc.add(tmp);
		            			}
		            		}
		            		// Reflection
		            		// render.reflect = true;
		            		if (render.reflect && !result.material.specular.epsilonEquals(new Color4f(0,0,0,1), 1e-9f)) {
		            			for (int k = 0; k < 10; k++) {
		            				reflectc.set(BLACK);
		            				if (result.material == null) break;
		            				if (result.material.specular.epsilonEquals(new Color4f(0,0,0,1), 1e-6f)) break;
	 		            			generateReflectRay(ray, reflectRay, result);
			            			reflectResult = new IntersectResult();
			            			for (Intersectable surface : surfaceList) surface.intersect(reflectRay, reflectResult);
			            			
			            			if(reflectResult.material != null){
			            				reflectc.set(reflectResult.material.diffuse.x*ambient.x, reflectResult.material.diffuse.y*ambient.y, reflectResult.material.diffuse.z*ambient.z);
					            		Vector3d l2 = new Vector3d();
					            		Vector3d v2 = new Vector3d();
					            		Vector3d half2 = new Vector3d();
				            			v2.sub(result.p, reflectResult.p);
				            			v2.normalize();
					            		for(Light light : lights.values()){
					            			Ray shadowRay2 = new Ray();
					            			Vector3d viewDirection = new Vector3d();
					            			viewDirection.sub(light.from, reflectResult.p);
					            			shadowRay2.set(reflectResult.p, viewDirection);
					            			IntersectResult shadowResult = new IntersectResult();
					            			if (!inShadow(reflectResult, light, shadowResult, shadowRay2)) {
							            		l2.sub(light.from, reflectResult.p);
						            			l2.normalize();
						            			half2.add(v2, l2);
						            			half2.normalize();
						            			Color3f tmp = new Color3f(reflectResult.material.diffuse.x*light.color.x, reflectResult.material.diffuse.y*light.color.y, reflectResult.material.diffuse.z*light.color.z);
						            			tmp.scale((float) Math.max(0, reflectResult.n.dot(l2)));
						            			reflectc.add(tmp);
						            			tmp = new Color3f(reflectResult.material.specular.x*light.color.x, reflectResult.material.specular.y*light.color.y, reflectResult.material.specular.z*light.color.z);
						            			tmp.scale((float) Math.pow(Math.max(0, reflectResult.n.dot(half2)), reflectResult.material.shinyness));
						            			reflectc.add(tmp);
					            			}
					            		}
					            		reflectc.set(reflectc.x*result.material.specular.x, reflectc.y*result.material.specular.y, reflectc.z*result.material.specular.z);
			            			}
			            			tmpc.add(reflectc);
			            			ray.set(reflectRay.eyePoint,reflectRay.viewDirection);
			            			result = new IntersectResult(reflectResult);
		            			}
		            		}
		            	}
		            	c.add(tmpc);
            		}
	            }
            	c.scale((float)1/(grid*grid));
	            c.clamp(0, 1);
	            
            	// Here is an example of how to calculate the pixel value.
            	int r = (int)(255*c.x);
                int g = (int)(255*c.y);
                int b = (int)(255*c.z);
                int a = 255;
                int argb = (a<<24 | r<<16 | g<<8 | b);    
                
                // update the render image
                render.setPixel(j, i, argb);
            }
        }
        
        // save the final render image
        render.save();
        
        // wait for render viewer to close
        render.waitDone();
        
    }
    
    public void precompute(Camera cam, int w, int h) {
		// compute unit vectors at eye position
    	unitW.sub(cam.from, cam.to);
    	unitW.normalize();
    	unitU.cross(cam.up, unitW);
    	unitU.normalize();
    	unitV.cross(unitW, unitU);
    	unitV.normalize();
    	
    	// compute bottom, left, top - bottom, right - left
    	b = Math.atan(Math.toRadians(cam.fovy/2));
    	t_b = -2 * b;
    	l = - w * b / h;
    	r_l = -2 * l;
}
    
    /**
     * Generate a ray through pixel (i,j).
     * 
     * @param i The pixel row.
     * @param j The pixel column.
     * @param offset The offset from the center of the pixel, in the range [-0.5,+0.5] for each coordinate. 
     * @param cam The camera.
     * @param ray Contains the generated ray.
     */
	public void generateRay(final int i, final int j, final double[] offset, final Camera cam, Ray ray) {
		
		// TODO: Objective 1: generate rays given the provided parmeters
		u = l + r_l * (j + offset[0])/cam.imageSize.width;
		v = b + t_b * (i + offset[1])/cam.imageSize.height;
		Vector3d viewDirection = new Vector3d();
		viewDirection.scale(-1, unitW);
		viewDirection.scaleAdd(u, unitU, viewDirection);
		viewDirection.scaleAdd(v, unitV, viewDirection);
		viewDirection.normalize();
		ray.set(cam.from, viewDirection);
	}
	
	private void generateReflectRay(Ray ray, Ray reflectRay, IntersectResult result) {
		Vector3d viewDirection = new Vector3d();
		Vector3d v = new Vector3d();
		v.set(ray.viewDirection);
		v.negate();
		viewDirection.scale( 2 * (v.dot(result.n)) , result.n);
		viewDirection.sub(v);
		reflectRay.set(result.p, viewDirection);
	}

	/**
	 * Shoot a shadow ray in the scene and get the result.
	 * 
	 * @param result Intersection result from raytracing. 
	 * @param light The light to check for visibility.
	 * @param root The scene node.
	 * @param shadowResult Contains the result of a shadow ray test.
	 * @param shadowRay Contains the shadow ray used to test for visibility.
	 * 
	 * @return True if a point is in shadow, false otherwise. 
	 */
	public boolean inShadow(final IntersectResult result, final Light light, IntersectResult shadowResult, Ray shadowRay) {
		
		// TODO: Objective 5: check for shdows and use it in your lighting computation
		for (Intersectable surface : surfaceList) {
			surface.intersect(shadowRay, shadowResult);
			if (shadowResult.material != null && shadowResult.t < 1) return true;
		}
		return false;
	}    
}

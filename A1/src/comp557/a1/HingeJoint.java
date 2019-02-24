package comp557.a1;
//Zhaoqi Xu - 260563752

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;

import mintools.parameters.DoubleParameter;
import javax.vecmath.Tuple3d;


public class HingeJoint extends DAGNode {

	
	DoubleParameter r;
	Tuple3d translation;
	String axis;
		
	public HingeJoint( String name, Tuple3d translation, String axis, double minValue, double maxValue  ) {
		super(name);
		this.axis = axis;
		this.translation = translation;
		dofs.add( r = new DoubleParameter( name+" r", 0, minValue, maxValue ) );		
	}
	
	@Override
	public void display(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPushMatrix();
                
        gl.glTranslated(translation.x, translation.y, translation.z);
        switch(this.axis) {
        	case "x": gl.glRotated(r.getValue(), 1, 0, 0);
        			  break;
        	case "y": gl.glRotated(r.getValue(), 0, 1, 0);
        			  break;
        	case "z": gl.glRotated(r.getValue(), 0, 0, 1);
			  		  break;
			default:  break;
        }
        
        super.display(drawable);
        
        gl.glPopMatrix();
	}
}

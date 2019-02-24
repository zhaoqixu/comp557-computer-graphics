package comp557.a1;
//Zhaoqi Xu - 260563752

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;

import mintools.parameters.DoubleParameter;
import javax.vecmath.Tuple3d;


public class BallJoint extends DAGNode {

	DoubleParameter rx;
	DoubleParameter ry;
	DoubleParameter rz;
	Tuple3d translation;
		
	public BallJoint( String name, Tuple3d translation, double minValue, double maxValue ) {
		super(name);
		this.translation = translation;
		dofs.add( rx = new DoubleParameter( name+" rx", 0, minValue, maxValue ) );		
		dofs.add( ry = new DoubleParameter( name+" ry", 0, minValue, maxValue ) );
		dofs.add( rz = new DoubleParameter( name+" rz", 0, minValue, maxValue ) );
	}
	
	@Override
	public void display(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPushMatrix();
        
        gl.glTranslated(translation.x, translation.y, translation.z);

        gl.glRotated(rx.getValue(), 1, 0, 0);
        gl.glRotated(ry.getValue(), 0, 1, 0);
        gl.glRotated(rz.getValue(), 0, 0, 1);
        
        super.display(drawable);
        
        gl.glPopMatrix();
	}
}

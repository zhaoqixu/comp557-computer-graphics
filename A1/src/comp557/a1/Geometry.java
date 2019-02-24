package comp557.a1;
//Zhaoqi Xu - 260563752

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;

import javax.vecmath.Tuple3d;

public class Geometry extends DAGNode {
	
	Tuple3d rgb;
	Tuple3d translation;
	Tuple3d scale;

	String shape;
	
	public Geometry ( String name, String shape, Tuple3d rgb, Tuple3d translation, Tuple3d scale) {
		super(name);
		this.shape = shape;
		this.rgb = rgb;
		this.translation = translation;
		this.scale = scale;
	}
	
	@Override
	public void display(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();

		// TODO: Objective 1: implement the FreeJoint display method
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        
        gl.glPushMatrix();
        
        gl.glColor3d(rgb.x, rgb.y, rgb.z);
        gl.glTranslated(translation.x, translation.y, translation.z);
        gl.glScaled(scale.x, scale.y, scale.z);
        
        if (shape.equals("box")) {
        	glut.glutSolidCube(1);
        } else if (shape.equals("sphere")) {
        	glut.glutSolidSphere(1, 50, 50);
        }
        
        super.display(drawable);
        
        gl.glPopMatrix();
	}
}


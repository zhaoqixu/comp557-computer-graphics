package comp557.a2;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.FloatBuffer;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import com.jogamp.opengl.DebugGL2;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.gl2.GLUT;

import mintools.parameters.BooleanParameter;
import mintools.parameters.DoubleParameter;
import mintools.parameters.IntParameter;
import mintools.swing.ControlFrame;
import mintools.swing.VerticalFlowPanel;
import mintools.viewer.FlatMatrix4d;
import mintools.viewer.FlatMatrix4f;
import mintools.viewer.Interactor;
import mintools.viewer.TrackBallCamera;

import javax.vecmath.Point2d;

/**
 * Assignment 2 - depth of field blur, and anaglyphys
 * 
 * For additional information, see the following paper, which covers
 * more on quality rendering, but does not cover anaglyphs.
 * 
 * The Accumulation Buffer: Hardware Support for High-Quality Rendering
 * Paul Haeberli and Kurt Akeley
 * SIGGRAPH 1990
 * 
 * http://http.developer.nvidia.com/GPUGems/gpugems_ch23.html
 * GPU Gems [2007] has a slightly more recent survey of techniques.
 *
 * @author Zhaoqi Xu 260563752
 */
public class A2App implements GLEventListener, Interactor {

	/** TODO: Put your name in the window title */
	private String name = "Comp 557 Assignment 2 - Zhaoqi Xu - 260563752";
	
    /** Viewing mode as specified in the assignment */
    int viewingMode = 1;
        
    /** eye Z position in world coordinates */
    private DoubleParameter eyeZPosition = new DoubleParameter( "eye z", 0.5, 0.25, 3 ); 
    /** near plane Z position in world coordinates */
    private DoubleParameter nearZPosition = new DoubleParameter( "near z", 0.25, -0.2, 0.5 ); 
    /** far plane Z position in world coordinates */
    private DoubleParameter farZPosition  = new DoubleParameter( "far z", -0.5, -2, -0.25 ); 
    /** focal plane Z position in world coordinates */
    private DoubleParameter focalPlaneZPosition = new DoubleParameter( "focal z", 0, -1.5, 0.4 );     

    /** Samples for drawing depth of field blur */    
    private IntParameter samples = new IntParameter( "samples", 5, 1, 100 );   
    
    /** 
     * Aperture size for drawing depth of field blur
     * In the human eye, pupil diameter ranges between approximately 2 and 8 mm
     */
    private DoubleParameter aperture = new DoubleParameter( "aperture size", 0.003, 0, 0.01 );
    
    /** x eye offsets for testing (see objective 4) */         
    private DoubleParameter eyeXOffset = new DoubleParameter("eye offset in x", 0.0, -0.3, 0.3);
    /** y eye offsets for testing (see objective 4) */
    private DoubleParameter eyeYOffset = new DoubleParameter("eye offset in y", 0.0, -0.3, 0.3);
    
    private BooleanParameter drawCenterEyeFrustum = new BooleanParameter( "draw center eye frustum", true );    
    
    private BooleanParameter drawEyeFrustums = new BooleanParameter( "draw left and right eye frustums", true );
    
	/**
	 * The eye disparity should be constant, but can be adjusted to test the
	 * creation of left and right eye frustums or likewise, can be adjusted for
	 * your own eyes!! Note that 63 mm is a good inter occular distance for the
	 * average human, but you may likewise want to lower this to reduce the
	 * depth effect (images may be hard to fuse with cheap 3D colour filter
	 * glasses). Setting the disparity negative should help you check if you
	 * have your left and right eyes reversed!
	 */
    private DoubleParameter eyeDisparity = new DoubleParameter("eye disparity", 0.063, -0.1, 0.1 );

    private GLUT glut = new GLUT();
    
    private Scene scene = new Scene();
    
	final FastPoissonDisk fpd = new FastPoissonDisk();

    /**
     * Launches the application
     * @param args
     */
    public static void main(String[] args) {
        new A2App();
    }
    
    GLCanvas glCanvas;
    
    /** Main trackball for viewing the world and the two eye frustums */
    TrackBallCamera tbc = new TrackBallCamera();
    /** Second trackball for rotating the scene */
    TrackBallCamera tbc2 = new TrackBallCamera();
    
    /**
     * Creates the application
     */
    public A2App() {      
        Dimension controlSize = new Dimension(640, 640);
        Dimension size = new Dimension(640, 480);
        ControlFrame controlFrame = new ControlFrame("Controls");
        controlFrame.add("Camera", tbc.getControls());
        controlFrame.add("Scene TrackBall", tbc2.getControls());
        controlFrame.add("Scene", getControls());
        controlFrame.setSelectedTab("Scene");
        controlFrame.setSize(controlSize.width, controlSize.height);
        controlFrame.setLocation(size.width + 20, 0);
        controlFrame.setVisible(true);    
        GLProfile glp = GLProfile.getDefault();
        GLCapabilities glc = new GLCapabilities(glp);
        glCanvas = new GLCanvas( glc );
        glCanvas.setSize( size.width, size.height );
        glCanvas.setIgnoreRepaint( true );
        glCanvas.addGLEventListener( this );
        glCanvas.requestFocus();
        FPSAnimator animator = new FPSAnimator( glCanvas, 60 );
        animator.start();        
        tbc.attach( glCanvas );
        tbc2.attach( glCanvas );
        // initially disable second trackball, and improve default parameters given our intended use
        tbc2.enable(false);
        tbc2.setFocalDistance( 0 );
        tbc2.panRate.setValue(5e-5);
        tbc2.advanceRate.setValue(0.005);
        this.attach( glCanvas );        
        JFrame frame = new JFrame( name );
        frame.getContentPane().setLayout( new BorderLayout() );
        frame.getContentPane().add( glCanvas, BorderLayout.CENTER );
        frame.setLocation(0,0);        
        frame.addWindowListener( new WindowAdapter() {
            @Override
            public void windowClosing( WindowEvent e ) {
                System.exit(0);
            }
        });
        frame.pack();
        frame.setVisible( true );        
    }
    
    @Override
    public void dispose(GLAutoDrawable drawable) {
    	// nothing to do
    }
        
    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        // do nothing
    }
    
    @Override
    public void attach(Component component) {
        component.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() >= KeyEvent.VK_1 && e.getKeyCode() <= KeyEvent.VK_7) {
                    viewingMode = e.getKeyCode() - KeyEvent.VK_1 + 1;
                }
                // only use the tbc trackball camera when in view mode 1 to see the world from
                // first person view, while leave it disabled and use tbc2 ONLY FOR ROTATION when
                // viewing in all other modes
                if ( viewingMode == 1 ) {
                	tbc.enable(true);
                	tbc2.enable(false);
	            } else {
                	tbc.enable(false);
                	tbc2.enable(true);
	            }
            }
        });
    }
    
    /**
     * @return a control panel
     */
    public JPanel getControls() {     
        VerticalFlowPanel vfp = new VerticalFlowPanel();
        
        VerticalFlowPanel vfp2 = new VerticalFlowPanel();
        vfp2.setBorder(new TitledBorder("Z Positions in WORLD") );
        vfp2.add( eyeZPosition.getSliderControls(false));        
        vfp2.add( nearZPosition.getSliderControls(false));
        vfp2.add( farZPosition.getSliderControls(false));        
        vfp2.add( focalPlaneZPosition.getSliderControls(false));     
        vfp.add( vfp2.getPanel() );
        
        vfp.add ( drawCenterEyeFrustum.getControls() );
        vfp.add ( drawEyeFrustums.getControls() );        
        vfp.add( eyeXOffset.getSliderControls(false ) );
        vfp.add( eyeYOffset.getSliderControls(false ) );        
        vfp.add ( aperture.getSliderControls(false) );
        vfp.add ( samples.getSliderControls() );        
        vfp.add( eyeDisparity.getSliderControls(false) );
        VerticalFlowPanel vfp3 = new VerticalFlowPanel();
        vfp3.setBorder( new TitledBorder("Scene size and position" ));
        vfp3.add( scene.getControls() );
        vfp.add( vfp3.getPanel() );        
        return vfp.getPanel();
    }
             
    public void init( GLAutoDrawable drawable ) {
    	drawable.setGL( new DebugGL2( drawable.getGL().getGL2() ) );
        GL2 gl = drawable.getGL().getGL2();
        gl.glShadeModel(GL2.GL_SMOOTH);             // Enable Smooth Shading
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.5f);    // Black Background
        gl.glClearDepth(1.0f);                      // Depth Buffer Setup
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        gl.glEnable(GL.GL_LINE_SMOOTH);
        gl.glEnable(GL2.GL_POINT_SMOOTH);
        gl.glEnable(GL2.GL_NORMALIZE );
        gl.glEnable(GL.GL_DEPTH_TEST);              // Enables Depth Testing
        gl.glDepthFunc(GL.GL_LEQUAL);               // The Type Of Depth Testing To Do 
        gl.glLineWidth( 2 );                        // slightly fatter lines by default!
    }   

	// TODO: Objective 1 - adjust for your screen resolution and dimension to something reasonable.
	double screenWidthPixels = 1920;
	double screenWidthMeters = 0.50;
	double metersPerPixel = screenWidthMeters / screenWidthPixels;
    
    @Override
    public void display(GLAutoDrawable drawable) {        
        GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);            

        double w = drawable.getSurfaceWidth() * metersPerPixel;
        double h = drawable.getSurfaceHeight() * metersPerPixel;

        if ( viewingMode == 1 ) {
        	// We will use a trackball camera, but also apply an 
        	// arbitrary scale to make the scene and frustums a bit easier to see
        	// (note the extra scale could have been part of the initializaiton of
        	// the tbc track ball camera, but this is eaiser)
            tbc.prepareForDisplay(drawable);
            gl.glScaled(15,15,15);        
            
            gl.glPushMatrix();
            tbc2.applyViewTransformation(drawable); // only the view transformation
            scene.display( drawable );
            gl.glPopMatrix();
            
            gl.glLineWidth(0.5f);
            
            // Objective 1: Draw Screen Window Rectangle and Eye Point
            gl.glDisable( GL2.GL_LIGHTING );
            gl.glColor3d(1.0, 1.0, 0);

            gl.glPushMatrix();
            gl.glBegin(GL.GL_LINE_LOOP);
            gl.glVertex3d( -w/2,-h/2, 0 );
            gl.glVertex3d( -w/2, h/2, 0 );
            gl.glVertex3d(  w/2, h/2, 0 );
            gl.glVertex3d(  w/2,-h/2, 0 );
            gl.glEnd();
            gl.glPopMatrix();
            
            // draw eye position
            gl.glColor3d(1.0, 1.0, 1.0);
            gl.glPushMatrix();
            gl.glTranslated(0, 0, eyeZPosition.getValue());
            glut.glutSolidSphere(0.0125, 20, 20);
            gl.glPopMatrix();
            
            // TODO: Objective 2 - draw camera frustum if drawCenterEyeFrustum is true
            if (drawCenterEyeFrustum.getValue()) {
            	
                double eyePos =  eyeZPosition.getValue();
                double near =  -nearZPosition.getValue() + eyePos;
                double far =  -farZPosition.getValue() + eyePos;
                
            	double left = -w/2;
                double right = w/2;
                double bottom = -h/2;
                double top = h/2;

                left = left * near / eyePos;
                right = right * near / eyePos;
                bottom = bottom * near / eyePos;
                top = top * near / eyePos;
                
                gl.glPushMatrix();
                gl.glLoadIdentity();
                gl.glFrustum(left, right, bottom, top, near, far);
                FlatMatrix4d frustumMatrix = new FlatMatrix4d();
                gl.glGetDoublev( GL2.GL_MODELVIEW_MATRIX, frustumMatrix.asArray(), 0);
                frustumMatrix.reconstitute();
                frustumMatrix.getBackingMatrix().invert();
                gl.glPopMatrix();
                
                gl.glPushMatrix();
                gl.glTranslated(0, 0, eyePos);

                gl.glPushMatrix();
                gl.glMultMatrixd(frustumMatrix.asArray(), 0);   
                glut.glutWireCube(2);      
                gl.glPopMatrix();
                gl.glPopMatrix();
                
                // Objective 3: Focal Plane Rectangle
                gl.glColor3d(0.5, 0.5, 0.5);
                double focal = focalPlaneZPosition.getValue();
                double focalLeft = -w/2 * (eyePos - focal) / eyePos;
                double focalRight = w/2 * (eyePos - focal) / eyePos;
                double focalBottom = -h/2 * (eyePos - focal) / eyePos;
                double focalTop = h/2 * (eyePos - focal) / eyePos;
                
                gl.glPushMatrix();
                gl.glTranslated(0, 0, focal);
                gl.glBegin(GL.GL_LINE_LOOP);
                gl.glVertex3d(focalLeft, focalBottom, 0);
                gl.glVertex3d(focalLeft, focalTop, 0);
                gl.glVertex3d(focalRight, focalTop, 0);
                gl.glVertex3d(focalRight, focalBottom, 0);
                gl.glEnd();
                gl.glPopMatrix();
                
                // Objective 4: Eye Position Offset
                gl.glColor3d(1, 1, 1);
                double eyeOffsetX = eyeXOffset.getValue();
                double eyeOffsetY = eyeYOffset.getValue();
                left = (focalLeft - eyeOffsetX) * near / (eyePos - focal);
                right = (focalRight - eyeOffsetX) * near / (eyePos - focal);
                bottom = (focalBottom - eyeOffsetY) * near / (eyePos - focal);
                top = (focalTop - eyeOffsetY) * near / (eyePos - focal);

                gl.glPushMatrix();
                gl.glLoadIdentity();
                gl.glFrustum(left, right, bottom, top, near, far);
                FlatMatrix4d frustumOffsetMatrix = new FlatMatrix4d();
                gl.glGetDoublev( GL2.GL_MODELVIEW_MATRIX, frustumOffsetMatrix.asArray(), 0);
                frustumOffsetMatrix.reconstitute();
                frustumOffsetMatrix.getBackingMatrix().invert();
                gl.glPopMatrix();
                
                gl.glPushMatrix();
                gl.glTranslated(eyeOffsetX, eyeOffsetY, eyePos);

                gl.glPushMatrix();
                gl.glMultMatrixd(frustumOffsetMatrix.asArray(), 0);   
                glut.glutWireCube(2);      
                gl.glPopMatrix();
                gl.glPopMatrix();

            }
            
            // TODO: Objective 6 - draw left and right eye frustums if drawEyeFrustums is true
            if (drawEyeFrustums.getValue() == true) {
            	
            	double disparity = eyeDisparity.getValue();
            	double eyePos =  eyeZPosition.getValue();
                double near =  -nearZPosition.getValue() + eyePos;
                double far =  -farZPosition.getValue() + eyePos;
                double focal = focalPlaneZPosition.getValue();

            	double left = -w/2;
                double right = w/2;
                double bottom = -h/2;
                double top = h/2;

                double focalLeftre = (left-disparity/2) * (eyePos - focal) / eyePos;
                double focalRightre = (right-disparity/2) * (eyePos - focal) / eyePos;
                double focalLeftle = (left+disparity/2) * (eyePos - focal) / eyePos;
                double focalRightle = (right+disparity/2) * (eyePos - focal) / eyePos;
                double focalBottom = bottom * (eyePos - focal) / eyePos;
                double focalTop = top * (eyePos - focal) / eyePos;
                left = (focalLeftre - eyeXOffset.getValue()) * near / (eyePos - focal);
                right = (focalRightre - eyeXOffset.getValue()) * near / (eyePos - focal);
                bottom = (focalBottom - eyeYOffset.getValue()) * near / (eyePos - focal);
                top = (focalTop - eyeYOffset.getValue()) * near / (eyePos - focal);
                
                // right eye, cyan
                gl.glDisable(GL2.GL_LIGHTING);
                gl.glColor3d(0, 1, 1);
                
                gl.glPushMatrix();
                gl.glLoadIdentity();
                gl.glFrustum(left, right, bottom, top, near, far);
                FlatMatrix4d frustumMatrixre = new FlatMatrix4d();
                gl.glGetDoublev( GL2.GL_MODELVIEW_MATRIX, frustumMatrixre.asArray(), 0);
                frustumMatrixre.reconstitute();
                frustumMatrixre.getBackingMatrix().invert();
                gl.glPopMatrix();
                
                gl.glPushMatrix();
                gl.glTranslated(eyeXOffset.getValue()+disparity/2, eyeYOffset.getValue(), eyePos);
                glut.glutSolidSphere(0.0125, 20, 20);

                gl.glPushMatrix();
                gl.glMultMatrixd(frustumMatrixre.asArray(), 0);   
                glut.glutWireCube(2);      
                gl.glPopMatrix();
                gl.glPopMatrix();
                
                // left eye, red
                left = (focalLeftle - eyeXOffset.getValue()) * near / (eyePos - focal);
                right = (focalRightle - eyeXOffset.getValue()) * near / (eyePos - focal);
                gl.glColor3d(1, 0, 0);
                
                gl.glPushMatrix();
                gl.glLoadIdentity();
                gl.glFrustum(left, right, bottom, top, near, far);
                FlatMatrix4d frustumMatrixle = new FlatMatrix4d();
                gl.glGetDoublev( GL2.GL_MODELVIEW_MATRIX, frustumMatrixle.asArray(), 0);
                frustumMatrixle.reconstitute();
                frustumMatrixle.getBackingMatrix().invert();
                gl.glPopMatrix();
                
                gl.glPushMatrix();
                gl.glTranslated(eyeXOffset.getValue()-disparity/2, eyeYOffset.getValue(), eyePos);
                glut.glutSolidSphere(0.0125, 20, 20);

                gl.glPushMatrix();
                gl.glMultMatrixd(frustumMatrixle.asArray(), 0);   
                glut.glutWireCube(2);      
                gl.glPopMatrix();
                gl.glPopMatrix();
                
                gl.glEnable(GL2.GL_LIGHTING);
            }
        } else if ( viewingMode == 2 ) {
        	
        	// TODO: Objective 2 - draw the center eye camera view

        	double eyePos =  eyeZPosition.getValue();
            double near =  -nearZPosition.getValue() + eyePos;
            double far =  -farZPosition.getValue() + eyePos;
            
        	double left = -w/2;
            double right = w/2;
            double bottom = -h/2;
            double top = h/2;

            left = left * near / eyePos;
            right = right * near / eyePos;
            bottom = bottom * near / eyePos;
            top = top * near / eyePos;
            
            gl.glMatrixMode(GL2.GL_PROJECTION);
            gl.glLoadIdentity();
            gl.glFrustum(left, right, bottom, top, near, far);
            
            gl.glMatrixMode( GL2.GL_MODELVIEW );
            gl.glLoadIdentity();
            gl.glTranslated(0, 0,-eyePos);

            tbc2.applyViewTransformation(drawable);
            scene.display(drawable);
        	 
        } else if ( viewingMode == 3 ) {            
        	
        	// TODO: Objective 5 - draw center eye with depth of field blur
        	int n = samples.getValue();
        	gl.glAccum(GL2.GL_LOAD, 1.0f/n);
        	
        	double eyePos =  eyeZPosition.getValue();
            double near =  -nearZPosition.getValue() + eyePos;
            double far =  -farZPosition.getValue() + eyePos;
            double focal = focalPlaneZPosition.getValue();

         	double left = -w/2;
            double right = w/2;
            double bottom = -h/2;
            double top = h/2;
            
            double focalLeft = left * (eyePos - focal) / eyePos;
            double focalRight = right * (eyePos - focal) / eyePos;
            double focalBottom = bottom * (eyePos - focal) / eyePos;
            double focalTop = top * (eyePos - focal) / eyePos;
        	
        	for (int i = 0; i < n; i++) {
        		Point2d p = new Point2d();
        		fpd.get(p, i, n);
        		double eyeOffsetX = aperture.getValue() * p.x;
        		double eyeOffsetY = aperture.getValue() * p.y;
        		
        		left = (focalLeft - eyeOffsetX) * near / (eyePos - focal);
                right = (focalRight - eyeOffsetX) * near / (eyePos - focal);
                bottom = (focalBottom - eyeOffsetY) * near / (eyePos - focal);
                top = (focalTop - eyeOffsetY) * near / (eyePos - focal);

                gl.glMatrixMode(GL2.GL_PROJECTION);
                gl.glLoadIdentity();
                gl.glFrustum(left, right, bottom, top, near, far);
                
                gl.glMatrixMode( GL2.GL_MODELVIEW );
                gl.glLoadIdentity();
                gl.glTranslated(-eyeOffsetX, -eyeOffsetY, -eyePos);
                
                gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT );
                tbc2.applyViewTransformation(drawable);
                scene.display(drawable);
                gl.glAccum(GL2.GL_ACCUM, 1.0f/n);            	 	
        	}
            gl.glAccum(GL2.GL_RETURN, 1);     
            
        } else if ( viewingMode == 4 ) {
        	
            // TODO: Objective 6 - draw the left eye view
        	double disparity = eyeDisparity.getValue();
        	double eyePos =  eyeZPosition.getValue();
            double near =  -nearZPosition.getValue() + eyePos;
            double far =  -farZPosition.getValue() + eyePos;
            double focal = focalPlaneZPosition.getValue();

        	double left = -w/2;
            double right = w/2;
            double bottom = -h/2;
            double top = h/2;

            double focalLeftle = (left+disparity/2) * (eyePos - focal) / eyePos;
            double focalRightle = (right+disparity/2) * (eyePos - focal) / eyePos;
            double focalBottom = bottom * (eyePos - focal) / eyePos;
            double focalTop = top * (eyePos - focal) / eyePos;
            left = focalLeftle * near / (eyePos - focal);
            right = focalRightle * near / (eyePos - focal);
            bottom = focalBottom * near / (eyePos - focal);
            top = focalTop * near / (eyePos - focal);
        	
            gl.glMatrixMode(GL2.GL_PROJECTION);
            gl.glLoadIdentity();
            gl.glFrustum(left, right, bottom, top, near, far);
            
            gl.glMatrixMode( GL2.GL_MODELVIEW );
            gl.glLoadIdentity();
            gl.glTranslated(disparity/2, 0, -eyePos);

            tbc2.applyViewTransformation(drawable);
            scene.display(drawable);
            
        } else if ( viewingMode == 5 ) {  
        	
        	// TODO: Objective 6 - draw the right eye view
        	double disparity = eyeDisparity.getValue();
        	double eyePos =  eyeZPosition.getValue();
            double near =  -nearZPosition.getValue() + eyePos;
            double far =  -farZPosition.getValue() + eyePos;
            double focal = focalPlaneZPosition.getValue();

        	double left = -w/2;
            double right = w/2;
            double bottom = -h/2;
            double top = h/2;

            double focalLeftre = (left-disparity/2) * (eyePos - focal) / eyePos;
            double focalRightre = (right-disparity/2) * (eyePos - focal) / eyePos;
            double focalBottom = bottom * (eyePos - focal) / eyePos;
            double focalTop = top * (eyePos - focal) / eyePos;
            left = focalLeftre * near / (eyePos - focal);
            right = focalRightre * near / (eyePos - focal);
            bottom = focalBottom * near / (eyePos - focal);
            top = focalTop * near / (eyePos - focal);
        	
            gl.glMatrixMode(GL2.GL_PROJECTION);
            gl.glLoadIdentity();
            gl.glFrustum(left, right, bottom, top, near, far);
            
            gl.glMatrixMode( GL2.GL_MODELVIEW );
            gl.glLoadIdentity();
            gl.glTranslated(-disparity/2, 0, -eyePos);

            tbc2.applyViewTransformation(drawable);
            scene.display(drawable);
        	                               
        } else if ( viewingMode == 6 ) {            
        	
        	// TODO: Objective 7 - draw the anaglyph view using glColouMask
        	
        	double disparity = eyeDisparity.getValue();
        	// reduce eyeDisparity by 0.04 will get a better 3D effect
//        	double disparity = eyeDisparity.getValue()-0.04;
        	double eyePos =  eyeZPosition.getValue();
            double near =  -nearZPosition.getValue() + eyePos;
            double far =  -farZPosition.getValue() + eyePos;
            double focal = focalPlaneZPosition.getValue();

        	double left = -w/2;
            double right = w/2;
            double bottom = -h/2;
            double top = h/2;

            double focalLeftle = (left+disparity/2) * (eyePos - focal) / eyePos;
            double focalRightle = (right+disparity/2) * (eyePos - focal) / eyePos;
            double focalLeftre = (left-disparity/2) * (eyePos - focal) / eyePos;
            double focalRightre = (right-disparity/2) * (eyePos - focal) / eyePos;
            double focalBottom = bottom * (eyePos - focal) / eyePos;
            double focalTop = top * (eyePos - focal) / eyePos;
            left = focalLeftle * near / (eyePos - focal);
            right = focalRightle * near / (eyePos - focal);
            bottom = focalBottom * near / (eyePos - focal);
            top = focalTop * near / (eyePos - focal);
            
        	// draw for left eye
        	gl.glColorMask( true, false, false, true );
        	gl.glMatrixMode(GL2.GL_PROJECTION);
            gl.glLoadIdentity();
            gl.glFrustum(left, right, bottom, top, near, far);
            
            gl.glMatrixMode( GL2.GL_MODELVIEW );
            gl.glLoadIdentity();
            gl.glTranslated(disparity/2, 0, -eyePos);

            gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT );

            tbc2.applyViewTransformation(drawable);
            scene.display(drawable);
        	 
        	// draw for right eye
            
            left = focalLeftre * near / (eyePos - focal);
            right = focalRightre * near / (eyePos - focal);
            
        	gl.glColorMask( false, true, true, true );
        	gl.glMatrixMode(GL2.GL_PROJECTION);
            gl.glLoadIdentity();
            gl.glFrustum(left, right, bottom, top, near, far);
             
            gl.glMatrixMode( GL2.GL_MODELVIEW );
            gl.glLoadIdentity();
            gl.glTranslated(-disparity/2, 0, -eyePos);
            
            gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT );

            tbc2.applyViewTransformation(drawable);
            scene.display(drawable);
            
            // reset color mask to make sure that other view mode is not affected
            gl.glColorMask(true,true,true,true);
    	
        } else if ( viewingMode == 7 ) {            
        	
        	// TODO: Bonus Ojbective 8 - draw the anaglyph view with depth of field blur
        	int n = samples.getValue();
        	gl.glAccum(GL2.GL_LOAD, 1.0f/n);
        	
        	double disparity = eyeDisparity.getValue();
        	
        	double eyePos =  eyeZPosition.getValue();
            double near =  -nearZPosition.getValue() + eyePos;
            double far =  -farZPosition.getValue() + eyePos;
            double focal = focalPlaneZPosition.getValue();

         	double left = -w/2;
            double right = w/2;
            double bottom = -h/2;
            double top = h/2;
            
            double focalLeftle = (left+disparity/2) * (eyePos - focal) / eyePos;
            double focalRightle = (right+disparity/2) * (eyePos - focal) / eyePos;
            double focalLeftre = (left-disparity/2) * (eyePos - focal) / eyePos;
            double focalRightre = (right-disparity/2) * (eyePos - focal) / eyePos;
            double focalBottom = bottom * (eyePos - focal) / eyePos;
            double focalTop = top * (eyePos - focal) / eyePos;
 	
        	for (int i = 0; i < n; i++) {
        		Point2d p = new Point2d();
        		fpd.get(p, i, n);
        		double eyeOffsetX = aperture.getValue() * p.x;
        		double eyeOffsetY = aperture.getValue() * p.y;
        		
        		left = (focalLeftle - eyeOffsetX) * near / (eyePos - focal);
                right = (focalRightle - eyeOffsetX) * near / (eyePos - focal);
                bottom = (focalBottom - eyeOffsetY) * near / (eyePos - focal);
                top = (focalTop - eyeOffsetY) * near / (eyePos - focal);
                
                // left eye frustum
                gl.glColorMask( true, false, false, true );
            	gl.glMatrixMode(GL2.GL_PROJECTION);
                gl.glLoadIdentity();
                gl.glFrustum(left, right, bottom, top, near, far);
                
                gl.glMatrixMode( GL2.GL_MODELVIEW );
                gl.glLoadIdentity();
                gl.glTranslated(disparity/2, 0, -eyePos);

                gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT );

                tbc2.applyViewTransformation(drawable);
                scene.display(drawable);
                
                // right eye frustum
                left = focalLeftre * near / (eyePos - focal);
                right = focalRightre * near / (eyePos - focal);
                
            	gl.glColorMask( false, true, true, true );
            	gl.glMatrixMode(GL2.GL_PROJECTION);
                gl.glLoadIdentity();
                gl.glFrustum(left, right, bottom, top, near, far);
                 
                gl.glMatrixMode( GL2.GL_MODELVIEW );
                gl.glLoadIdentity();
                gl.glTranslated(-disparity/2, 0, -eyePos);
                
                gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT );

                tbc2.applyViewTransformation(drawable);
                scene.display(drawable);
                
                gl.glColorMask(true,true,true,true);
                
                gl.glAccum(GL2.GL_ACCUM, 1.0f/n);            	 	
        	}
            gl.glAccum(GL2.GL_RETURN, 1);     
        }
    }
}

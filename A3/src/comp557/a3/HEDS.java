package comp557.a3;

// Zhaoqi Xu, 260563752

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;

import javax.vecmath.Matrix3d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector4d;

/**
 * Half edge data structure.
 * Maintains a list of faces (i.e., one half edge of each) to allow
 * for easy display of geometry.
 */
public class HEDS {

    /**
     * List of faces 
     */
    Set<Face> faces = new HashSet<Face>();
    
    // priority queue
    int check = 0;
    PriorityQueue<Edge> pq = new PriorityQueue<Edge>();
    
    /**
     * Constructs an empty mesh (used when building a mesh with subdivision)
     */
    public HEDS() {
        // do nothing
    }
        
    /**
     * Builds a half edge data structure from the polygon soup   
     * @param soup
     */
    public HEDS( PolygonSoup soup, double regularizationWeight) {
        halfEdges.clear();
        faces.clear();
        
        // TODO: Objective 1: create the half edge data structure from the polygon soup    

        for (int[] f : soup.faceList) {
        	int i = f[f.length - 1];
        	int j = f[0];
        	HalfEdge he = new HalfEdge();
        	halfEdges.put(i+","+j, he);
        	HalfEdge firstEdge = he;
        	he.head = soup.vertexList.get(j);
        	he.twin = halfEdges.get(j+","+i);
        	if (he.twin != null) {
        		he.twin.twin = he;
        	}
        	for (int k = 1; k < f.length; k++) {
        		 i = j;
        		 j = f[k];
        		 HalfEdge next = new HalfEdge();
             	 halfEdges.put(i+","+j, next);
        		 next.head = soup.vertexList.get(j);
        		 next.twin = halfEdges.get(j+","+i);
              	 if (next.twin != null) {
             	 	 next.twin.twin = next;
             	 }
        		 he.next = next;
        		 he = next;
        	}
        	he.next = firstEdge;
        	faces.add(new Face(firstEdge));        	
        }
        
        // TODO: Objective 5: fill your priority queue on load
        buildQueue(regularizationWeight);
    }
    
    /**
     * You might want to use this to match up half edges... 
     */
    Map<String,HalfEdge> halfEdges = new TreeMap<String,HalfEdge>();
    
    // TODO: Objective 2, 3, 4, 5: write methods to help with collapse, and for checking topological problems

    private HalfEdge edgeCollapse( HalfEdge he, double regularizationWeight ) {
        
    	if ( ! redoListHalfEdge.isEmpty() ) {
    		
    		redoCollapse();
    		
    		if ( redoListHalfEdge.isEmpty() ) {
    			return pq.peek().he;
    		}
    		
    		return redoListHalfEdge.peek();
    	}

    	// add to undolist before remove
    	undoList.add(he);
    	
    	Edge e = he.e;
    	
    	Vertex v = new Vertex();
    	
        v.Q.set( e.Q );

        v.p.set( he.e.v.x, he.e.v.y, he.e.v.z );

    	HalfEdge A = he.next.twin;
        HalfEdge B = he.prev().twin;
        HalfEdge C = he.twin.next.twin;
        HalfEdge D = he.twin.prev().twin;        
        
        // collapse the two faces
        A.twin = B; 
        B.twin = A;
        C.twin = D; 
        D.twin = C;

        // remove the two faces
        faces.remove( he.leftFace );
        faces.remove( he.twin.leftFace );
        
        // update edge information
        pq.remove( C.e );
        pq.remove( A.e );
        C.e = D.e;
        A.e = B.e;
        A.e.he = A;
        B.e.he = B;
        C.e.he = C;
        D.e.he = D;
        
        // loop around vertex v and recompute the normal and K matrix
        HalfEdge loopHE = A;        
        do {
        	loopHE.head = v;
        	loopHE.leftFace.recomputeNormal();
        	loopHE = loopHE.next.twin;        	
        } while ( loopHE != A );
        
        // update the quadric error for the edge
        do {
        	pq.remove( loopHE.e );
        	Edge edge = loopHE.e;
        	edge.error = computeQuadricError( loopHE, edge.v,  edge.Q , regularizationWeight);        	
        	pq.add( loopHE.e );        	
        	loopHE = loopHE.next.twin;        	
        } while ( loopHE != A );
    	        
        return pq.peek().he;
    }
    
    // avoid Topological problems
    public HalfEdge checkTopo( double regularizationWeight) {
    	
    	// no more edge candidate to collapse, return null
    	if ( pq.isEmpty() ) {
    		
    		return (HalfEdge) null;
    		
    	}
    	
    	// otherwise, check the edge with the highest priority
    	Edge e = pq.remove();
    	HalfEdge he = e.he;
    	   	
    	HalfEdge A = he.next.twin;
        HalfEdge B = he.prev().twin;
        HalfEdge C = he.twin.next.twin;
        HalfEdge D = he.twin.prev().twin;        
                
        // check that if the 1-rings of the edge vertices have more than 2 vertices in common
    	HashSet<Vertex> vertexSet = new HashSet<Vertex>();
    	HalfEdge loopHE = A;
    	
    	do {
    		vertexSet.add( loopHE.next.head );
    		loopHE = loopHE.next.twin;
    	} while ( loopHE.twin != D );
    	
    	loopHE = C;
    	do {
    		if ( vertexSet.contains( loopHE.next.head) ) {
    			System.out.println("1-rings of the edge vertices have more than 2 vertices in common");
    			return he;
    		}
    		loopHE = loopHE.next.twin;
    	} while (loopHE.twin != B);
        
        // check for non-manifold problems
        if ( B.valence() <= 3 || D.valence() <= 3 ) {
        	System.out.println("This operation will result in a non-manifold shape!");
        	return he;
        }
    	
        return edgeCollapse( he, regularizationWeight );
    }
    
    public double computeQuadricError( HalfEdge he, Vector4d v,  Matrix4d Q , double regularizationWeight ) {
    	double error = 0;
    	
    	Point3d m = new Point3d();
    	Point3d vi = he.head.p;
    	Point3d vj = he.prev().head.p;
    	m.x = (vi.x+vj.x)/2;
    	m.y = (vi.y+vj.y)/2;
    	m.z = (vi.z+vj.z)/2;
    	
    	Matrix4d R = new Matrix4d();
    	R.setIdentity();
    	R.m03 = -m.x;
    	R.m13 = -m.y;
    	R.m23 = -m.z;
    	R.m30 = -m.x;
    	R.m31 = -m.y;
    	R.m32 = -m.z;
    	R.m33 = m.x*m.x + m.y*m.y + m.z*m.z;
    	R.mul( regularizationWeight );

    	Q.setZero();
    	Q.add(he.head.Q);
    	Q.add(he.prev().head.Q);
    	R.add(Q);
    	
    	Matrix3d A = new Matrix3d();
    	R.getRotationScale(A);
    	
    	Vector3d b = new Vector3d();
    	b.x = R.m03;
    	b.y = R.m13;
    	b.z = R.m23;
    	
    	b.scale(-1);
    	A.invert();
    	
    	A.transform(b);
    	v.set(b);
    	v.w = 1;
    	
    	Vector4d qv = new Vector4d();
    	R.transform(v, qv);
    	error = qv.dot(v);
    	return error;
    }
        
    public void buildQueue( double regularizationWeight) {
    	check++;
    	pq.clear();
    	for ( Face f : faces ) {
    		HalfEdge loopHE = f.he;
    		do {
    			// check if halfedge has been visited
    			if ( check != loopHE.check ) {
    				loopHE.head.Q.setZero();
    				loopHE.check = check;
    				
    				if ( loopHE.twin != null ) {
    					loopHE.twin.head.Q.setZero();
    					loopHE.twin.check = check;
    				}
    				
    				// add Q matrices to vertices
    				HalfEdge loopHETemp = loopHE;
    				do {
    		    		loopHETemp.head.Q.add( loopHETemp.leftFace.K );
    		    		loopHETemp = loopHETemp.next.twin;
    		    	} while ( loopHETemp != loopHE && loopHETemp != null );
    				
    				// handle the case if loopHETemp is null
    				if ( loopHETemp == null ) {
    					loopHETemp = loopHE.twin;
        				while (loopHETemp != null ) {
        		    		loopHETemp.prev().head.Q.add( loopHETemp.leftFace.K );
        		    		loopHETemp = loopHETemp.prev().twin;
        		    	} 
    				}
    		    	
    				// add Q matrices to other end vertices
    				if ( loopHE.twin !=  null) {
	    				loopHETemp = loopHE.twin;
	    		    	do {
	    		    		loopHETemp.head.Q.add( loopHETemp.leftFace.K );
	    		    		loopHETemp = loopHETemp.next.twin;
	    		    	} while ( loopHETemp != loopHE.twin && loopHETemp != null );
	    		    	
	    		    	if ( loopHETemp == null ) {
	    					loopHETemp = loopHE.twin.twin;
	        				while (loopHETemp != null ) {
	        		    		loopHETemp.prev().head.Q.add( loopHETemp.leftFace.K );
	        		    		loopHETemp = loopHETemp.prev().twin;
	        		    	}
	    				}
    				}			
    				
    				
    				Edge edge = new Edge();
    				
    				// compute quadricError and assign to edge
    				edge.error = computeQuadricError( loopHE, edge.v, edge.Q, regularizationWeight );
    				
    				// assign edge to half edge
    				if ( loopHE.twin != null ) loopHE.twin.e = edge;
    				loopHE.e = edge;

    				// assign half edge to edge
    				edge.he = loopHE;
	    			
	    			// add edge to priority queue
	    			pq.add(edge);
    			}
	    		loopHE = loopHE.next;
    		} while ( f.he != loopHE );
    	}
    }
    
    /**
	 * Need to know both verts before the collapse, but this information is actually 
	 * already stored within the excized portion of the half edge data structure.
	 * Thus, we only need to have a half edge (the collapsed half edge) to undo
	 */
	LinkedList<HalfEdge> undoList = new LinkedList<>();
	/**
	 * To redo an undone collapse, we must know which edge to collapse.  We should
	 * likewise reuse the Vertex that was created for the collapse.
	 */
	LinkedList<HalfEdge> redoListHalfEdge = new LinkedList<>();
	LinkedList<Vertex> redoListVertex = new LinkedList<>();

    void undoCollapse() {
    	if ( undoList.isEmpty() ) return;
    
    	HalfEdge he = undoList.removeLast(); // ignore the request
    	
    	// TODO: Objective 6: undo the last collapse
    	// be sure to put the information on the redo list so you can redo the collapse too!
    	faces.add(he.leftFace);
    	Vertex vertex = he.next.twin.head;    	

    	if ( he.twin != null ) {
    		HalfEdge C = he.twin.next.twin;
    		C.twin = he.twin.next;
    		HalfEdge D = he.twin.next.next.twin;
    		D.twin = he.twin.next.next;
        	faces.add(he.twin.leftFace);
    	}
    	
    	HalfEdge A = he.next.twin;
    	A.twin = he.next;
    	HalfEdge B = he.next.next.twin;
    	B.twin = he.next.next;
    	
    	// update head vertices
    	updateHead( he.prev().head, he.prev() );
    	updateHead( he.head, he );
    	
    	// store the edge and the collapsed vertex position
    	redoListVertex.add( vertex );
    	redoListHalfEdge.add( he );
    }
    
    void redoCollapse() {
    	if ( redoListHalfEdge.isEmpty() ) return; // ignore the request
    	
    	HalfEdge he = redoListHalfEdge.removeLast();
    	Vertex vertex = redoListVertex.removeLast();
    	
    	undoList.add(he); // put this on the undo list so we can undo this collapse again

    	
    	// TODO: Objective 7: undo the edge collapse!
    	HalfEdge A = he.next.twin;
        HalfEdge B = he.prev().twin;
        HalfEdge C = he.twin.next.twin;
        HalfEdge D = he.twin.prev().twin;        
            
        A.twin = B; 
        B.twin = A;
        C.twin = D; 
        D.twin = C;

        // remove the faces
        faces.remove( he.twin.leftFace );
        faces.remove( he.leftFace );
            
        // update head vertices
        updateHead( vertex, A );
    }
    
    // Sets a new  head for the given halfedge and loops all adjacent halfedges
    // and recomputes the face normals
    private void updateHead(Vertex v,  HalfEdge he ) {
        
    	HalfEdge loopHE = he;    
        while ( null != loopHE.twin ) {
        	loopHE = loopHE.twin.prev();
        	if ( loopHE == he ) break;
        }
        do {
        	loopHE.head = v;
        	loopHE.leftFace.recomputeNormal();
        	loopHE = loopHE.next.twin;        	
        } while ( loopHE != he  && loopHE != null );
    }
    
    /**
     * Draws the half edge data structure by drawing each of its faces.
     * Per vertex normals are used to draw the smooth surface when available,
     * otherwise a face normal is computed. 
     * @param drawable
     */
    public void display(GLAutoDrawable drawable) {
    	 GL2 gl = drawable.getGL().getGL2();

         // we do not assume triangular faces here        
         Point3d p;
         Vector3d n;        
         for ( Face face : faces ) {
             HalfEdge he = face.he;
             gl.glBegin( GL2.GL_POLYGON );
             n = he.leftFace.n;
             gl.glNormal3d( n.x, n.y, n.z );
             HalfEdge e = he;
             do {
                 p = e.head.p;
                 gl.glVertex3d( p.x, p.y, p.z );
                 e = e.next;
             } while ( e != he );
             gl.glEnd();
         }
    }
    
}

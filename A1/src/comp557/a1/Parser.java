package comp557.a1;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Scanner;

import javax.vecmath.Tuple3d;
import javax.vecmath.Vector3d;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A factory class to generate a DAG from XML definition. 
 *  Zhaoqi Xu - 260563752
 */
public class Parser {

	public static DAGNode load( String filename ) {
		try {
			InputStream inputStream = new FileInputStream(new File(filename));
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(inputStream);
			return createScene( null, document.getDocumentElement() ); // we don't check the name of the document elemnet
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to load simulation input file.", e);
		}
	}
	
	/**
	 * Load a DAG subtree from a XML node.
	 * Returns the root on the call where the parent is null, but otherwise
	 * all children are added as they are created and all other deeper recursive
	 * calls will return null.
	 */
	public static DAGNode createScene( DAGNode parent, Node dataNode ) {
        NodeList nodeList = dataNode.getChildNodes();
        for ( int i = 0; i < nodeList.getLength(); i++ ) {
            Node n = nodeList.item(i);
            // skip all text, just process the ELEMENT_NODEs
            if ( n.getNodeType() != Node.ELEMENT_NODE ) continue;
            String nodeName = n.getNodeName();
            DAGNode dagNode = null;
            if ( nodeName.equalsIgnoreCase( "node" ) ) {
            	dagNode = Parser.createJoint( n );
            } else if ( nodeName.equalsIgnoreCase( "geom" ) ) {        		
        		dagNode = Parser.createGeom( n );
            }
            // recurse to load any children of this node
            createScene( dagNode, n );
            if ( parent == null ) {
            	// if no parent, we can only have one root... ignore other nodes at root level
            	return dagNode;
            } else {
            	parent.add( dagNode );
            }
        }
        return null;
	}
	
	/**
	 * Create a joint
	 * 
	 * TODO: Objective 5: Adapt commented code in createJoint() to create your joint nodes when loading from xml
	 */
	public static DAGNode createJoint( Node dataNode ) {
		String type = dataNode.getAttributes().getNamedItem("type").getNodeValue();
		String name = dataNode.getAttributes().getNamedItem("name").getNodeValue();
		if ( type.equals("freejoint") ) {
			FreeJoint joint = new FreeJoint( name );
			return joint;
		} else if ( type.equals("ballxyz") ) {
			// position is optional (ignored if missing) but should probably be a required attribute!  
			// Could add optional attributes for limits (to all joints)
			Tuple3d p;
//			BallXYZ joint = new BallXYZ( name );
//			if ( (t=getTuple3dAttr(dataNode,"position")) != null ) joint.setPosition( t );			
//			return joint;
			if ( (p=getTuple3dAttr(dataNode,"position")) != null ) {
				BallJoint joint = new BallJoint(name, p, -70, 70);
				return joint;
			} else {
				System.out.println("Data is missing");
				return null;
			}
			
		} else if ( type.equals("hinge") ) {
			// position and axis are required... passing null to set methods
			// likely to cause an expection (perhaps OK)
			
//			Hinge joint = new Hinge( name );
//			joint.setPosition( getTuple3dAttr(dataNode,"position") );
//			joint.setAxis( getTuple3dAttr(dataNode,"axis") );
//			return joint;
			Tuple3d p;
			
			String a = dataNode.getAttributes().getNamedItem("axis").getNodeValue();
			double min = Double.valueOf(dataNode.getAttributes().getNamedItem("min").getNodeValue());
			double max = Double.valueOf(dataNode.getAttributes().getNamedItem("max").getNodeValue());
			
			if ( (p=getTuple3dAttr(dataNode,"position")) != null && a != null && min != 0 && max != 0) {
				HingeJoint joint = new HingeJoint(name, p, a, min, max);
				return joint;
			} else {
				System.out.println("Data is missing");
				return null;
			}
		}
		return null;
	}

	/**
	 * Creates a geometry DAG node 
	 * 
	 * TODO: Objective 5: Adapt commented code in greatGeom to create your geometry nodes when loading from xml
	 */
	public static DAGNode createGeom( Node dataNode ) {
		String type = dataNode.getAttributes().getNamedItem("type").getNodeValue();
		String name = dataNode.getAttributes().getNamedItem("name").getNodeValue();
		Tuple3d t;
//		if ( type.equals("box" ) ) {
//			BodyBox geom = new BodyBox( name );
//			if ( (t=getTuple3dAttr(dataNode,"center")) != null ) geom.setCentre( t );
//			if ( (t=getTuple3dAttr(dataNode,"scale")) != null ) geom.setScale( t );
//			if ( (t=getTuple3dAttr(dataNode,"color")) != null ) geom.setColor( t );
//			return geom;
			Tuple3d p, c, s;
			p = getTuple3dAttr(dataNode, "position");
			c = getTuple3dAttr(dataNode, "color");
			s = getTuple3dAttr(dataNode, "scale");
			Geometry geom = new Geometry(name, type, c, p, s);
			return geom;

			
			
//		} else if ( type.equals( "sphere" )) {
////			BodySphere geom = new BodySphere( name );				
////			if ( (t=getTuple3dAttr(dataNode,"center")) != null ) geom.setCentre( t );
////			if ( (t=getTuple3dAttr(dataNode,"scale")) != null ) geom.setScale( t );
////			if ( (t=getTuple3dAttr(dataNode,"color")) != null ) geom.setColor( t );
////			return geom;	
//		}
//		return null;		
	}
	
	/**
	 * Loads tuple3d attributes of the given name from the given node.
	 * @param dataNode
	 * @param attrName
	 * @return null if attribute not present
	 */
	public static Tuple3d getTuple3dAttr( Node dataNode, String attrName ) {
		Node attr = dataNode.getAttributes().getNamedItem( attrName);
		Vector3d tuple = null;
		if ( attr != null ) {
			Scanner s = new Scanner( attr.getNodeValue() );
			tuple = new Vector3d( s.nextDouble(), s.nextDouble(), s.nextDouble() );			
			s.close();
		}
		return tuple;
	}

}
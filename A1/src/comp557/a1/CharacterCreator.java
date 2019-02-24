package comp557.a1;

import javax.swing.JTextField;

import comp557.a1.DAGNode;
import comp557.a1.Parser;
import mintools.parameters.BooleanParameter;

public class CharacterCreator {

	static public String name = "Me - Zhaoqi Xu - 260563752";
	
	// TODO: Objective 6: change default of load from file to true once you start working with xml
	static BooleanParameter loadFromFile = new BooleanParameter( "Load from file (otherwise by procedure)", false );
	static JTextField baseFileName = new JTextField("a1data/character");
	static { baseFileName.setName("what is this?"); }
	
	/**
	 * Creates a character, either procedurally, or by loading from an xml file
	 * @return root node
	 */
	static public DAGNode create() {
		
		if ( loadFromFile.getValue() ) {
			// TODO: Objectives 6: create your character in the character.xml file 
			return Parser.load( baseFileName.getText() + ".xml");
		} else {
			// TODO: Objective 1,2,3,4: test DAG nodes by creating a small DAG in the CharacterCreator.create() method 
						
			// Use this for testing, but ultimately it will be more interesting
			// to create your character with an xml description (see example).
			
			// Here we just return null, which will not be very interesting, so write
			// some code to create a test or partial charcter and return the root node.
//			FreeJoint torso = new FreeJoint("torso");
//			BallJoint shoulder = new BallJoint("shoulder", -130, 130);
//			HingeJoint elbow = new HingeJoint("elbow", "x", 0, 135);
//			Geometry cube = new Geometry("hand", "sphere", 
//										(1, 0, 0),
//										[1, 1, 1],
//										[1, 2, 1]);
//			torso.add(shoulder);
//			shoulder.add(elbow);
//			elbow.add(cube);
			return null;
		}
	}
}

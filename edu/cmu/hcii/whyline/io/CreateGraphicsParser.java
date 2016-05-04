package edu.cmu.hcii.whyline.io;

import edu.cmu.hcii.whyline.bytecode.*;
import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public class CreateGraphicsParser extends ExecutionEventParser {

	public CreateGraphicsParser(Trace trace) {
		super(trace);
	}

	public static boolean handles(Instruction inst) {

		if(!(inst instanceof INVOKEVIRTUAL)) return false;
		
		return ((INVOKEVIRTUAL)inst).getMethodInvoked().matchesClassNameAndDescriptor(QualifiedClassName.get("java/awt/Graphics"), "create", "()Ljava/awt/Graphics;");

	}
	
	public boolean handle(int eventID) {

		if(trace.getKind(eventID) == EventKind.CREATEGRAPHICS) {

			trace.getGraphicsHistory().add(new CreateGraphicsOutputEvent(trace, eventID));
			return true;
		
		}
		else return false;

	}

}
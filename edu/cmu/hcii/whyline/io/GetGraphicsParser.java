package edu.cmu.hcii.whyline.io;

import edu.cmu.hcii.whyline.bytecode.*;
import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public class GetGraphicsParser extends ExecutionEventParser {

	public GetGraphicsParser(Trace trace) {
		super(trace);
	}

	public static boolean handles(Instruction inst) {
		
		if(!(inst instanceof INVOKEVIRTUAL)) return false;

		MethodrefInfo methodref = ((INVOKEVIRTUAL)inst).getMethodInvoked();
		if(methodref.getMethodName().equals("getGraphics") && methodref.getMethodDescriptor().equals("()Ljava/awt/Graphics;"))
			return true;
		else if(methodref.getMethodName().equals("createGraphics") && methodref.getMethodDescriptor().equals("()Ljava/awt/Graphics2D;"))
			return true;
		
		return false;

	}
	
	public boolean handle(int eventID) {

		if(trace.getKind(eventID) != EventKind.GETGRAPHICS) return false;
		
		trace.getGraphicsHistory().add(new GetGraphicsOutputEvent(trace, eventID));

		return true;

	}

}

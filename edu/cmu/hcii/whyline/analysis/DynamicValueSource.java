package edu.cmu.hcii.whyline.analysis;

import static edu.cmu.hcii.whyline.trace.EventKind.PUTSTATIC;
import static edu.cmu.hcii.whyline.trace.EventKind.RETURN;
import static edu.cmu.hcii.whyline.trace.EventKind.SETLOCAL;
import edu.cmu.hcii.whyline.bytecode.GETFIELD;
import edu.cmu.hcii.whyline.bytecode.GETSTATIC;
import edu.cmu.hcii.whyline.bytecode.GetLocal;
import edu.cmu.hcii.whyline.bytecode.Instruction;
import edu.cmu.hcii.whyline.bytecode.Invoke;
import edu.cmu.hcii.whyline.trace.EventKind;
import edu.cmu.hcii.whyline.trace.Trace;
import edu.cmu.hcii.whyline.util.IntegerVector;

/**
 * @author Andrew J. Ko
 *
 */
public class DynamicValueSource {

	public static IntegerVector getPathToSource(Trace trace, int eventID) {

		IntegerVector path = new IntegerVector(10);
		
		int e = eventID;

		while(e >= 0) {

			
			EventKind kind = trace.getKind(e);

			if(!kind.isArgument) path.append(e);
			
			if(kind.isArgument)
				e = trace.getHeapDependency(e);

			else if(kind == EventKind.PUTFIELD)
				e = trace.getOperandStackValue(e, 1).getEventID();
			
			else if(kind == PUTSTATIC)
				e = trace.getOperandStackValue(e, 0).getEventID();
			
			else if(kind == SETLOCAL)
				e = trace.getOperandStackValue(e, 0).getEventID();

			else if(kind == RETURN)
				e = trace.getOperandStackValue(e, 0).getEventID();
			
			else if(kind.isValueProduced) {
				
				Instruction i = trace.getInstruction(e);
				
				if(i instanceof GETFIELD)
					e = trace.getHeapDependency(e);

				else if(i instanceof GETSTATIC)
					e = trace.getHeapDependency(e);
	
				else if(i instanceof GetLocal)
					e = trace.getHeapDependency(e);					

				else if(i instanceof Invoke)
					e = trace.getHeapDependency(e);

				else
					break;
				
			}
			else
				break;
			
		}
		
		return path;
			
	}
	
}

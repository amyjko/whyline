package edu.cmu.hcii.whyline.trace;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import edu.cmu.hcii.whyline.bytecode.*;
import edu.cmu.hcii.whyline.util.IntegerVector;
import edu.cmu.hcii.whyline.util.Saveable;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public final class ExceptionHistory implements Saveable {

	private final Trace trace;

	private IntegerVector exceptionIDs = new IntegerVector(100);
	
	public ExceptionHistory(Trace trace) {

		this.trace = trace;
	
	}

	public void addExceptionTime(int time) {
		
		exceptionIDs.append(time);
		
	}

	public IntegerVector getTimes() { return exceptionIDs; }

	public int getExceptionThrownBefore(int eventID) {

		int threadID = trace.getThreadID(eventID);
		
		int index = exceptionIDs.getIndexOfLargestValueLessThanOrEqualTo(eventID);
		while(index >= 0) {
			
			int exceptionEventID = exceptionIDs.get(index);
			if(trace.getKind(exceptionEventID) == EventKind.EXCEPTION_THROWN && trace.getThreadID(exceptionEventID) == threadID)
				return exceptionEventID;
			index--;
			
		}
		return -1;
		
	}

	public int getThrowEventIDForCatchID(int catchID) {

		Instruction catchInstruction = trace.getInstruction(catchID);
		ExceptionHandler handler = catchInstruction.getCode().getHandlerStartingWith(catchInstruction);

		assert handler != null;

		ClassInfo catchType = handler.getCatchType();
		QualifiedClassName catchTypeName = catchType == null ? QualifiedClassName.JAVA_LANG_THROWABLE : catchType.getName();
		
		int index = exceptionIDs.getIndexOfLargestValueLessThanOrEqualTo(catchID);
		while(index >= 0) {
			
			int exceptionEventID = exceptionIDs.get(index);

			if(trace.getKind(exceptionEventID) == EventKind.EXCEPTION_THROWN)
				if(trace.getThreadID(exceptionEventID) == trace.getThreadID(catchID)) {
				
					Value exceptionValue = trace.getOperandStackValue(exceptionEventID, 0);
					int exceptionValueID = exceptionValue.getEventID();
					
					if(trace.getKind(exceptionValueID).isValueProduced) {
							
						if(catchTypeName == QualifiedClassName.JAVA_LANG_THROWABLE)
							return exceptionEventID;
						else if(catchTypeName == QualifiedClassName.JAVA_LANG_EXCEPTION)
							return exceptionEventID;
						else {

							long id = trace.getObjectIDProduced(exceptionValueID);
							if(id < 0) {
								QualifiedClassName typeOfException = trace.getClassnameOfObjectID(id);
								Classfile exceptionClass = trace.getClassfileByName(typeOfException);
								if(typeOfException == catchTypeName || (exceptionClass != null && exceptionClass.isSubclassOf(catchTypeName)))
									return exceptionEventID;
							}
							
						}
						
					}
										
				}
			index--;
			
		}
		return -1;
		
	}

	public IntegerVector getExceptionsCaughtBetween(int startID, int endID, int threadID) {

		IntegerVector catches = new IntegerVector(10);
		
		int index = exceptionIDs.getIndexOfLargestValueLessThanOrEqualTo(endID);
		while(index >= 0) {

			int exceptionEventID = exceptionIDs.get(index);

			if(exceptionEventID <= startID)
				return catches;
			
			if(trace.getKind(exceptionEventID) == EventKind.EXCEPTION_CAUGHT && trace.getThreadID(exceptionEventID) == threadID)
				catches.append(exceptionEventID);
			index--;
			
		}
		return catches;
		
	}

	public void write(DataOutputStream out) throws IOException {

		exceptionIDs.write(out);
		
	}

	public void read(DataInputStream in) throws IOException {

		exceptionIDs.read(in);

	}

}

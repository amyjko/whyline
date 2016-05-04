package edu.cmu.hcii.whyline.io;

import edu.cmu.hcii.whyline.bytecode.*;
import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public final class TextualOutputEvent extends OutputEvent {

	private final String stringPrinted;
	private boolean isResultOfStringBuilder;
	private boolean isArgumentOfPrint;
	private boolean appendsNewline;
	
	public TextualOutputEvent(Trace trace, int eventID) {

		super(trace, eventID);

		isArgumentOfPrint = false;
		appendsNewline = false;
		isResultOfStringBuilder = false;

		boolean parameterToWriter = false;

		Instruction producerInstruction = trace.getInstruction(eventID);
		
		StackDependencies.Consumers consumers = producerInstruction.getConsumers();
		if(consumers.getFirstConsumer() instanceof Invoke) {
			
			Invoke invoke = (Invoke)consumers.getFirstConsumer();
			isArgumentOfPrint = 
				invoke.getMethodInvoked().matchesClassAndName(QualifiedClassName.PRINT_STREAM, "println") ||
				invoke.getMethodInvoked().matchesClassAndName(QualifiedClassName.PRINT_STREAM, "print");

			appendsNewline = invoke.getMethodInvoked().getMethodName().equals("println");

			parameterToWriter = trace.getClassIDs().isOrIsSubclassOf(invoke.getMethodInvoked().getClassName(), QualifiedClassName.WRITER);
			
		}
		
		// If it is the argument to a println, was it produced as the result of a toString()
		if(isArgumentOfPrint) {

			if(producerInstruction instanceof Invoke) {

				Invoke invoke = (Invoke)producerInstruction;
				isResultOfStringBuilder = invoke.getMethodInvoked().matchesClassAndName(QualifiedClassName.STRING_BUILDER, "toString");				
				
			}

		}
		
		boolean producerIsObjectProduced = trace.getKind(eventID) == EventKind.OBJECT_PRODUCED;
		Object regularValue = trace.getDescription(eventID);
		Object objectValue = producerIsObjectProduced ? trace.getObjectProduced(eventID) : null;
		
		Object value = producerIsObjectProduced ? objectValue : regularValue;
		
		if(isResultOfStringBuilder) stringPrinted = "\n";
		else if(isArgumentOfPrint) stringPrinted = value + "\n";
		else {
			
			if(parameterToWriter) { 
				if(value instanceof Integer)
					value = (char)((Integer)value).intValue();
				else if(producerIsObjectProduced && regularValue instanceof Long) {

					long arrayID = (Long)regularValue;
					QualifiedClassName classname = trace.getClassnameOfObjectID(arrayID);
					if(classname.isArray() && classname.getText().equals("[C")) {

//						ExecutionEvent<?> newArrayEvent = producer.getTrace().getInstantiationOf(arrayID);
//						if(newArrayEvent != null) {
//							int arraylength = newArrayEvent.getArgumentProducer(0).getInteger();
//							for(int i = 0; i < arraylength; i++) {
//								
//								ExecutionEvent<?> valueProducedForIndex = producer.getTrace().getDefinitionOfArrayIndexPriorTo(arrayID, i, producer.getEventID());
//								
//							}
//						}

					}
					
				}
				
			}
			else if(producerIsObjectProduced && regularValue instanceof Long) {
				
				long id = (Long)regularValue;
				QualifiedClassName type = trace.getClassnameOfObjectID(id);

				StringBuilder builder = new StringBuilder();
				builder.append(type.getSimpleName());
				builder.append(" #");
				builder.append(id);
				value = builder.toString();
				
			}
			
			stringPrinted = "" + value + (appendsNewline ? "\n" : "");
		}
		
	}

	public boolean segmentsOutput() { return !isResultOfStringBuilder; }
	
	public String getStringPrinted() {
		
		return stringPrinted;
		
	}

	public String getHTMLDescription() { return "\"" + stringPrinted + "\"" + " printed"; }

	public String toString() {
		
		return "println \"" + getStringPrinted().replace("\n", "\\n") + "\"";
		
	}
	
}
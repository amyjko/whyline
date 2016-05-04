package edu.cmu.hcii.whyline.io;

import edu.cmu.hcii.whyline.bytecode.*;
import edu.cmu.hcii.whyline.trace.*;
import edu.cmu.hcii.whyline.tracing.ClassIDs;

/**
 * @author Andrew J. Ko
 *
 */
public class TextualOutputParser extends ExecutionEventParser {

	public TextualOutputParser(Trace trace) {
		super(trace);
	}

	
	/*
	 * What counts as textual output? Any argument of any type to 
	 */
	public static boolean handles(ClassIDs classIDs, Instruction output) {
		
		StackDependencies.Consumers consumers = output.getConsumers();
		
		// If this has no consumers, then it doesn't contribute to output.
		if(consumers.getNumberOfConsumers() == 0) return false;
		
		// Consumer of this value produced must be an invocation
		Instruction consumer = consumers.getFirstConsumer();

		// Textual output produces values for invocations.
		if(!(consumer instanceof Invoke)) return false;

		Invoke invocation = (Invoke)consumer;
		MethodrefInfo methodInvoked = invocation.getMethodInvoked();
		QualifiedClassName classInvokedOn = methodInvoked.getClassName();
		
		// It must produce a value for an instance of one of these classes.
		if(!(classIDs.isOrIsSubclassOfTextualOutputProducer(classInvokedOn)))
			return false;

		// This instruction must NOT produce a value for this invocation's instances. It must be a parameter.
		StackDependencies.Producers producers = invocation.getInstanceProducers(); 
		for(int i = 0; i < producers.getNumberOfProducers(); i++) 
			if(producers.getProducer(i) == output) 
				return false;

		// If this is an argument to a StringBuilder constructor, then we want to find the instruction that loaded the instance used to construct the StringBuilder.
		// We'll later search if that value was ultimately and indirectly consumed by a println.
		if(methodInvoked.matchesClassAndName(QualifiedClassName.STRING_BUILDER, "<init>")) {
			// If its a dup, then get the producer of the dup's argument
			if(invocation.getInstanceProducers().getFirstProducer() instanceof Duplication) {
				Duplication dup = (Duplication)invocation.getInstanceProducers().getFirstProducer();
				consumer = dup.getProducersOfArgument(0).getFirstProducer();
			}
			// Otherwise, this isn't output.
			else 
				return false;
		}
		// Otherwise, we start searching from the instruction itself.
		else 
			consumer = output;
		
		// Does the instruction of interest ultimately get consumed by a print or println call?
		while(consumer != null) {
			
			MethodrefInfo method = consumer instanceof Invoke ? ((Invoke)consumer).getMethodInvoked() : null;

			if(method != null) {
				if(classIDs.isOrIsSubclassOf(method.getClassName(), QualifiedClassName.WRITER) && 
						   (method.getMethodName().equals("write") ||
							method.getMethodName().equals("append") ||
							method.getMethodName().startsWith("print"))) {
					return true;
				}
				if(classIDs.isOrIsSubclassOf(method.getClassName(), QualifiedClassName.PRINT_STREAM) && method.getMethodName().startsWith("print")) return true;
			}

			StackDependencies.Consumers consumersConsumers = consumer.getConsumers(); 
			consumer = consumersConsumers.getNumberOfConsumers() > 0 ? consumersConsumers.getFirstConsumer() : null;
		
		}
		
		return false;
		
	}
	
	public boolean handle(int eventID) {
		
		// Has to be a value produced for the println, append, or StringBuilder() methods.
		if(!trace.getKind(eventID).isValueProduced) return false;
		
		if(!handles(trace.getClassIDs(), trace.getInstruction(eventID))) return false;

		trace.getPrintHistory().add(new TextualOutputEvent(trace, eventID));
		
		return true;

	}

}

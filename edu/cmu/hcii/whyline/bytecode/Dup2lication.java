package edu.cmu.hcii.whyline.bytecode;


import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public abstract class Dup2lication extends Duplication {

	public Dup2lication(CodeAttribute method) {
		super(method);
	}
	
	public final EventKind getSecondTypeProduced() {

		// It's okay that we're only looking at one of the potential producers (the one at index 0)
		// because whoever the producer is, all producers should produce the same type.
		return getProducersOfArgument(0).getFirstProducer().getTypeProduced();
		
	}

	public String getReadableDescription() {
		
		return getProducersOfArgument(0).getFirstProducer().getReadableDescription();
		
	}

	public boolean duplicatesMultipleOperands() { return true; }

}
package edu.cmu.hcii.whyline.bytecode;


import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public abstract class Duplication extends StackManipulation {

	public Duplication(CodeAttribute method) {
		super(method);
	}
	
	public abstract int getOpcode();

	public final EventKind getTypeProduced() {

		// It's okay that we're only looking at one of the potential producers (the one at index 0)
		// because whoever the producer is, all producers should produce the same type.
		return getProducersOfArgument(0).getFirstProducer().getTypeProduced();
		
	}
	
	public String getReadableDescription() {
		
		return getProducersOfArgument(0).getFirstProducer().getReadableDescription();
		
	}
	
	public String getTypeDescriptorOfArgument(int argIndex) { return null; }

}
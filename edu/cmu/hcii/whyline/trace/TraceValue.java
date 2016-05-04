package edu.cmu.hcii.whyline.trace;

import edu.cmu.hcii.whyline.bytecode.Instruction;
import edu.cmu.hcii.whyline.qa.Answer;
import edu.cmu.hcii.whyline.qa.CauseAnswer;
import edu.cmu.hcii.whyline.qa.Question;
import edu.cmu.hcii.whyline.util.Util;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public final class TraceValue extends Value {

	private final int eventID;
	private final Instruction producer;
	
	public TraceValue(Trace trace, int eventID, Instruction producer) {
		
		super(trace);
		
		this.eventID = eventID;
		this.producer = producer;
		
	}
	
	public int getEventID() { return eventID; }
	
	public Instruction getProducer() { return producer; }

	public boolean getBoolean() throws NoValueException { 
		
		if(trace.getKind(eventID).isNonArgumentDefinition) return trace.getDefinitionValueSet(eventID).getBoolean(); 
		else return trace.getBooleanProduced(eventID);
		
	}

	public int getInteger() throws NoValueException {
		
		if(trace.getKind(eventID).isNonArgumentDefinition) return trace.getDefinitionValueSet(eventID).getInteger(); 
		else return trace.getIntegerProduced(eventID); 
	
	}
	
	public long getLong()  { 
		
		if(trace.getKind(eventID).isNonArgumentDefinition) {
			Value value = trace.getDefinitionValueSet(eventID);
			// Why would this be null? One reason is that we couldn't find the source of an exception value pushed onto the operand stack.
			return value == null ? 0 : value.getLong();
		}
		return trace.getLongProduced(eventID); 
		
	}

	public float getFloat()  throws NoValueException { 

		if(trace.getKind(eventID).isNonArgumentDefinition) return trace.getDefinitionValueSet(eventID).getFloat(); 
		return trace.getFloatProduced(eventID); 
		
	}

	public double getDouble()  throws NoValueException { 

		if(trace.getKind(eventID).isNonArgumentDefinition) return trace.getDefinitionValueSet(eventID).getDouble(); 
		return trace.getDoubleProduced(eventID); 
		
	}
	
	public Object getValue() throws NoValueException { 
	
		EventKind kind = trace.getKind(eventID);

		if(kind.isValueProduced || kind.isArgument)  {
			switch(kind) {
			case INTEGER_PRODUCED :
			case INTEGER_ARG :
				return getInteger();

			case SHORT_PRODUCED :
			case SHORT_ARG :
				return trace.getShortProduced(eventID);

			case BYTE_PRODUCED :
			case BYTE_ARG :
				return trace.getByteProduced(eventID);

			case FLOAT_PRODUCED :
			case FLOAT_ARG :
				return trace.getFloatProduced(eventID);

			case BOOLEAN_PRODUCED :
			case BOOLEAN_ARG :
				return trace.getBooleanProduced(eventID);

			case CHARACTER_PRODUCED :
			case CHARACTER_ARG :
				return trace.getCharacterProduced(eventID);

			case DOUBLE_PRODUCED :
			case DOUBLE_ARG :
				return trace.getDoubleProduced(eventID);

			case LONG_PRODUCED :
			case LONG_ARG :
				return trace.getLongProduced(eventID);

			case NEW_OBJECT :
			case NEW_ARRAY :
			case OBJECT_PRODUCED :
			case OBJECT_ARG :
				return trace.getObjectIDProduced(eventID);

			default :
				throw new RuntimeException("\n\n" + trace.eventToString(eventID) + " \n\nis an unknown type of trace stack value.");			
			}
		}
		else if(kind.isDefinition) return trace.getDefinitionValueSet(eventID).getValue();
		else throw new RuntimeException("Don't know how to get a trace stack value for  " + kind);
		
	}
	
	public Object getImmutable() throws NoValueException { 

		EventKind kind = trace.getKind(eventID);

		if(kind.isObjectProduced || kind.isArgument) {
			long objectID = trace.getObjectIDProduced(eventID);
			return trace.getImmutableObject(objectID);
		}
		else if(kind.isDefinition)
			return trace.getDefinitionValueSet(eventID).getImmutable();
		else
			return null;
		
	}

	public boolean isObject() { 

		EventKind kind = trace.getKind(eventID);
		
		if(kind.isObjectProduced || kind == EventKind.OBJECT_ARG)
			return true;
		
		if(kind.isDefinition) {
			Value val = trace.getDefinitionValueSet(eventID);
			return val == null ? false : val.isObject();
		}
		else return false;
		
	}
	
	public String getVerbalExplanation() { return trace.getHTMLDescription(eventID); }
	
	public String getDisplayName(boolean html) { 

		Object value = null;
		try {
			value = getValue();
		} catch(NoValueException e) {
			return "[unknown]";
		}
		
		if(isObject()) {

			if(value == null) return "null";
			
			Object immutable = null;
			try {
				immutable = getImmutable();
			} catch(NoValueException e) {}
			if(immutable != null) return Util.format(immutable, html);

			long val = ((Long)value).longValue();
			return trace.getDescriptionOfObjectID(val);

		}
		else return Util.format(value, html);
	
	}
	
	public boolean hasEventID() { return true; }

	public String toString() { return "TraceStackValue:" + trace.eventToString(eventID); }

	public Answer getAnswer(Question<?> q) {
		
		return new CauseAnswer(q, eventID, "These events were responsible.");

	}
	
}

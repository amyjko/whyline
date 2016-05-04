package edu.cmu.hcii.whyline.trace;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import edu.cmu.hcii.whyline.Whyline;
import edu.cmu.hcii.whyline.bytecode.*;

/**
 * All of the different kinds of Whyline recording events.
 * @author Andrew J. Ko
 *
 */ 
public enum EventKind {

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Definitions
	//
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	PUTFIELD("(J)V", null, false, false, false, false, false, false, true, false, false, false) {
		public String getDescription(Trace trace, int eventID) {
			return ((edu.cmu.hcii.whyline.bytecode.PUTFIELD)trace.getInstruction(eventID)).getLocalIDName() + " = " + trace.getOperandStackValue(eventID, 1).getDisplayName(false);
		}
		public String getHTMLDescription(Trace trace, int eventID) {
			return getAssignmentHTML(
					((edu.cmu.hcii.whyline.bytecode.PUTFIELD)trace.getInstruction(eventID)).getLocalIDName(),
					trace.getOperandStackValue(eventID, 1).getDisplayName(true));
		}
	},

	PUTSTATIC("(J)V", null, false, false, false, false, false, false, true, false, false, false) { 
		public String getDescription(Trace trace, int eventID) {
			return 
				((edu.cmu.hcii.whyline.bytecode.PUTSTATIC)trace.getInstruction(eventID)).getLocalIDName() + 
				" = " + 
				trace.getOperandStackValue(eventID, 0).getDisplayName(false);
		}

		public String getHTMLDescription(Trace trace, int eventID) {

			edu.cmu.hcii.whyline.bytecode.PUTSTATIC put = ((edu.cmu.hcii.whyline.bytecode.PUTSTATIC)trace.getInstruction(eventID));
			return getAssignmentHTML(
				put.getFieldref().getClassname().getSimpleName() + "." + put.getLocalIDName(),
				trace.getOperandStackValue(eventID, 0).getDisplayName(true));
		}
	},

	SETARRAY("(J)V", null, false, false, false, false, false, false, true, false, false, false) { 
		public String getDescription(Trace trace, int eventID) {
			long arrayID = trace.getOperandStackValue(eventID, 0).getLong();
			if(arrayID < 0) return "[unknown array at unknown index] =" + "?";
			else return trace.getDescriptionOfObjectID(arrayID) + "[" + trace.getOperandStackValue(eventID, 1).getDisplayName(false) + "] = " + trace.getOperandStackValue(eventID, 2).getDisplayName(false);
		}
		public String getHTMLDescription(Trace trace, int eventID) { 

			long arrayID = trace.getOperandStackValue(eventID, 0).getLong();
			String array = arrayID < 0 ? "(unknown array)" : trace.getDescriptionOfObjectID(arrayID);
			String index = trace.getOperandStackValue(eventID, 1).getDisplayName(true);
			String value = trace.getOperandStackValue(eventID, 2).getDisplayName(true);
			return getAssignmentHTML(array + "[" + index + "]", value);
			
		}

	},
	
	SETLOCAL("(J)V", null, false, false, false, false, false, false, true, false, false, false) { 
		public String getDescription(Trace trace, int eventID) {
			Value value = trace.getOperandStackValue(eventID, 0);
			String argumentString = value == null ? "?" : value.getDisplayName(false);
			return ((SetLocal)trace.getInstruction(eventID)).getLocalIDName() + " = " + argumentString;
		}

		public String getHTMLDescription(Trace trace, int eventID) {
			Value valueSet = trace.getOperandStackValue(eventID, 0);
			String value = valueSet == null ? "<b>(unknown value)</b>" : valueSet.getDisplayName(true);
			return getAssignmentHTML(((SetLocal)trace.getInstruction(eventID)).getLocalIDName(), value);
		}
	},

	IINC("(IJ)V", null, false, false, false, false, false, false, true, false, false, false) {
		public String getDescription(Trace trace, int eventID) {
			IINC set = ((IINC)trace.getInstruction(eventID));
			
			try {
				int val = trace.getIncrementValue(eventID);
				return set.getLocalIDName() + " = " + val;
			} catch (NoValueException e) {
				return set.getLocalIDName() + " = unknown";
			}
		}

		public String getHTMLDescription(Trace trace, int eventID) {
			IINC set = ((IINC)trace.getInstruction(eventID));
			return "<b>" + set.getLocalIDName() + "</b> was incremented by <b>" + set.getIncrement() + "</b>";
		}
	},
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Comparisons and branches
	//
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	COMPINTS("(J)V", null, false, false, false, false, false, true, false, false, false, false)	{ 
		public String getDescription(Trace trace, int eventID) {
			try {
				int one = trace.getOperandStackValue(eventID, 0).getInteger();
				int two = trace.getOperandStackValue(eventID, 1).getInteger();
				Instruction inst = trace.getInstruction(eventID);
				if(inst instanceof IF_ICMPEQ) return one == two ? "" + one + " == " + two : "" + one + " != " + two;
				else if(inst instanceof IF_ICMPGE) return one >= two ? "" + one + " >= " + two : "" + one + " < " + two;
				else if(inst instanceof IF_ICMPGT) return one > two ? "" + one + " > " + two : "" + one + " <= " + two;
				else if(inst instanceof IF_ICMPLE) return one <= two ? "" + one + " <= " + two : "" + one + " > " + two;
				else if(inst instanceof IF_ICMPNE) return one != two ? "" + one + " != " + two : "" + one + " == " + two;
				else if(inst instanceof IF_ICMPLT) return one < two ? "" + one + " < " + two : "" + one + " >= " + two;
				else return "";
			} catch(NoValueException e) {
				return UnknownValueExplanations.UNKNOWN_VALUES_FOR_COMPARISON;
			}
				
		}

		public String getHTMLDescription(Trace trace, int eventID) {

			Value one = trace.getOperandStackValue(eventID, 0);
			Value two = trace.getOperandStackValue(eventID, 1);

			return "Compared <b>" + one.getDisplayName(true) + "</b> and <b>" + two.getDisplayName(true) + "</b>";
			
		}
	},
	
	COMPZERO("(J)V", null, false, false, false, false, false, true, false, false, false, false) { 
		public String getDescription(Trace trace, int eventID) {
			try {
				int val = trace.getOperandStackValue(eventID, 0).getInteger();
				Instruction inst = trace.getInstruction(eventID);
				if(inst instanceof IFEQ) return val == 0 ? "" + val + " == 0" : "" + val + " != 0";
				else if(inst instanceof IFGE) return val >= 0 ? "" + val + " >= 0" : "" + val + " < 0";
				else if(inst instanceof IFGT) return val > 0 ? "" + val + " > 0" : "" + val + " <= 0";
				else if(inst instanceof IFLE) return val <= 0 ? "" + val + " <= 0" : "" + val + " > 0";
				else if(inst instanceof IFNE) return val != 0 ? "" + val + " != 0" : "" + val + " == 0";
				else if(inst instanceof IFLT) return val < 0 ? "" + val + " < 0" : "" + " >= 0";
				else return "";
			} catch(NoValueException e) {
				return UnknownValueExplanations.UNKNOWN_VALUES_FOR_COMPARISON;
			}
		}

		public String getHTMLDescription(Trace trace, int eventID) {
			Value val = trace.getOperandStackValue(eventID, 0);
			return "Compared <b>" + val.getDisplayName(true) + "</b> to <b>0</b>";
		}
	},
	
	COMPREFS("(J)V", null, false, false, false, false, false, true, false, false, false, false)	{ 
		public String getDescription(Trace trace, int eventID) {
			long one = trace.getOperandStackValue(eventID, 0).getLong();
			long two = trace.getOperandStackValue(eventID, 1).getLong();
			if(one < 0 || two < 0)
				return UnknownValueExplanations.UNKNOWN_VALUES_FOR_COMPARISON;
			if(one == two) return "=";
			else return "!=";
		}
		public String getHTMLDescription(Trace trace, int eventID) {
			long one = trace.getOperandStackValue(eventID, 0).getLong();
			long two = trace.getOperandStackValue(eventID, 1).getLong();
			return "Compared <b>" + trace.getDescriptionOfObjectID(one) + "</b> to <b>" + trace.getDescriptionOfObjectID(two);			
		}
	},

	COMPNULL("(J)V", null, false, false, false, false, false, true, false, false, false, false) { 
		public String getDescription(Trace trace, int eventID) {
			long id = trace.getOperandStackValue(eventID, 0).getLong();
			if(id == 0) return "= null";
			else if(id < 0) return UnknownValueExplanations.UNKNOWN_VALUES_FOR_COMPARISON;
			else return "!= null";
		}

		public String getHTMLDescription(Trace trace, int eventID) {
			Value value = trace.getOperandStackValue(eventID, 0);
			long id = value.getLong();
			return "Compared <b>" + trace.getDescriptionOfObjectID(id) + "</b> to <b>null</b>";			
		}
	},
	
	TABLEBRANCH	("(J)V", null, false, false, false, false, false, true, false, false, false, false) { 
		public String getDescription(Trace trace, int eventID) {
			return "case " + trace.getOperandStackValue(eventID, 0).getDisplayName(false);
		}

		public String getHTMLDescription(Trace trace, int eventID) {
			
			return "Switched on <b>" + trace.getOperandStackValue(eventID, 0).getDisplayName(true) + "</b>";
			
		}

	},
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Invocations
	//
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	INVOKE_VIRTUAL("(J)V", null, false, false, false, true, false, false, false, false, true, false) { 
		public String getHTMLDescription(Trace trace, int eventID) {
			return getInvocationHTML(trace, eventID);
		}
		public String getDescription(Trace trace, int eventID) {
			return getInvocationHTML(trace, eventID);
		}
	},
	
	INVOKE_SPECIAL("(J)V", null, false, false, false, true, false, false, false, false, true, false) {
		public String getDescription(Trace trace, int eventID) {
			return getInvocationHTML(trace, eventID);
		}
		public String getHTMLDescription(Trace trace, int eventID) {
			return getInvocationHTML(trace, eventID);
		}
	},
	
	INVOKE_STATIC("(J)V", null, false, false, false, true, false, false, false, false, true, false)  { 
		public String getDescription(Trace trace, int eventID) {
			return getInvocationHTML(trace, eventID);
		}
		public String getHTMLDescription(Trace trace, int eventID) {
			return getInvocationHTML(trace, eventID);
		}
	},
	
	INVOKE_INTERFACE("(J)V", null, false, false, false, true, false, false, false, false, true, false) { 
		public String getDescription(Trace trace, int eventID) {
			return getInvocationHTML(trace, eventID);
		}
		public String getHTMLDescription(Trace trace, int eventID) {
			return getInvocationHTML(trace, eventID);
		}
	},
	
	START_METHOD("(J)V", null, false, false, false, false, false, false, false, false, true, true) {
		public String getDescription(Trace trace, int eventID) {

			return trace.getInstruction(eventID).getMethod().getInternalName() + "()";
			
		}
		
		public String getHTMLDescription(Trace trace, int eventID) {

			Instruction inst = trace.getInstruction(eventID);
			return "<b>" + inst.getMethod().getJavaName() + "</b> began.";
			
		}
	},
	
	RETURN("(J)V", null, false, false, false, false, false, false, false, false, true, false) { 
		public String getDescription(Trace trace, int eventID) {

			if(trace.getInstruction(eventID) instanceof RETURN) return "return";
			else return "return " + trace.getOperandStackValue(eventID, 0).getDisplayName(false); 
			
		}

		public String getHTMLDescription(Trace trace, int eventID) {
		
			AbstractReturn ret = (AbstractReturn)trace.getInstruction(eventID);
			String prefix = "<b>" + ret.getMethod().getJavaName() + "()</b> returned";
			if(ret instanceof RETURN)
				return prefix;
			else 
				return prefix + " <b>" + trace.getOperandStackValue(eventID, 0).getDisplayName(true) + "</b>";
			
		}
	},
	
	EXCEPTION_THROWN("(J)V", null, false, false, false, false, false, false, false, false, true, false) {
		public String getDescription(Trace trace, int eventID) {
			try {
				Value exThrown = trace.getOperandStackValue(eventID, 0);
				return exThrown == null ? "unknown exception" : "" + exThrown.getImmutable();
			} catch (NoValueException e) {
				return UnknownValueExplanations.UNKNOWN_EXCEPTION_TYPE;
			}
		}

		public String getHTMLDescription(Trace trace, int eventID) {
			Value exThrown = trace.getOperandStackValue(eventID, 0);
			long exID = exThrown.getLong();
			if(exID < 0) return UnknownValueExplanations.UNKNOWN_EXCEPTION_TYPE;
			else return "A <b>" + (exThrown == null ? "unknown type of exception" : trace.getClassnameOfObjectID(exID)) + "</b> was thrown.";
		}

	},

	EXCEPTION_CAUGHT("(J)V", null, false, false, false, false, false, false, false, false, true, false) {
		public String getDescription(Trace trace, int eventID) {
			Instruction inst = trace.getInstruction(eventID);
			return "exception caught in " + inst.getMethod().getJavaName() + "()";
		}

		public String getHTMLDescription(Trace trace, int eventID) {
			String kind = "Exception";
			Instruction inst = trace.getInstruction(eventID);
			for(ExceptionHandler handler : inst.getCode().getExceptionTable()) {
				if(handler.getHandlerPC() == inst) {
					ClassInfo exceptionType = handler.getCatchType();
					if(exceptionType == null) kind = "Exception";
					else kind = exceptionType.getSimpleName();
					break;
				}
			}
			return "<b>" + kind + "</b> caught in <b>" + inst.getMethod().getJavaName() + "()</b>";
		}
	},

	MONITOR("(J)V", null, false, false, false, false, false, false, false, false, true, false) {
		public String getDescription(Trace trace, int eventID) {
			Instruction inst = trace.getInstruction(eventID);
			Value value = trace.getOperandStackValue(eventID, 0);
			return value.getDisplayName(true) + (inst instanceof MONITORENTER ? " acquired lock" : " released lock");
		}

		public String getHTMLDescription(Trace trace, int eventID) {
			Instruction inst = trace.getInstruction(eventID);
			Value value = trace.getOperandStackValue(eventID, 0);
			return "<b>" + value.getDisplayName(true) + "</b> " + (inst instanceof MONITORENTER ? " acquired lock" : " released lock");
		}
	},

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// These represent executions of subclasses of PushConstant, for which we don't need to record values, since they're constant.
	//
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	CONSTANT_INTEGER_PRODUCED("(J)V", OperandStackType.INT, true, true, false, false, false, false, false, false, false, false) { 
		public String getDescription(Trace trace, int eventID) { return getDescriptionOfConstantProduced(trace, eventID); }
		public String getHTMLDescription(Trace trace, int eventID) { return getExplanationOfValueProduced(trace, eventID); }
	},

	CONSTANT_SHORT_PRODUCED("(J)V", OperandStackType.INT, true, true, false, false, false, false, false, false, false, false) { 
		public String getDescription(Trace trace, int eventID) { return getDescriptionOfConstantProduced(trace, eventID); }
		public String getHTMLDescription(Trace trace, int eventID) { return getExplanationOfValueProduced(trace, eventID); }
	},

	CONSTANT_BYTE_PRODUCED("(J)V", OperandStackType.INT, true, true, false, false, false, false, false, false, false, false) { 
		public String getDescription(Trace trace, int eventID) { return getDescriptionOfConstantProduced(trace, eventID); }
		public String getHTMLDescription(Trace trace, int eventID) { return getExplanationOfValueProduced(trace, eventID); }
	},

	CONSTANT_FLOAT_PRODUCED("(J)V", OperandStackType.FLOAT, true, true, false, false, false, false, false, false, false, false) { 
		public String getDescription(Trace trace, int eventID) { return getDescriptionOfConstantProduced(trace, eventID); }
		public String getHTMLDescription(Trace trace, int eventID) { return getExplanationOfValueProduced(trace, eventID); }
	},

	CONSTANT_BOOLEAN_PRODUCED("(J)V", OperandStackType.INT, true, true, false, false, false, false, false, false, false, false)	 { 
		public String getDescription(Trace trace, int eventID) { return getDescriptionOfConstantProduced(trace, eventID); }
		public String getHTMLDescription(Trace trace, int eventID) { return getExplanationOfValueProduced(trace, eventID); }
	},

	CONSTANT_CHARACTER_PRODUCED("(J)V", OperandStackType.INT, true, true, false, false, false, false, false, false, false, false) {
		public String getDescription(Trace trace, int eventID) { return getDescriptionOfConstantProduced(trace, eventID); }
		public String getHTMLDescription(Trace trace, int eventID) { return getExplanationOfValueProduced(trace, eventID); }
	},
	
	CONSTANT_DOUBLE_PRODUCED("(J)V", OperandStackType.DOUBLE, true, true, false, false, false, false, false, false, false, false) { 
		public String getDescription(Trace trace, int eventID) { return getDescriptionOfConstantProduced(trace, eventID); }
		public String getHTMLDescription(Trace trace, int eventID) { return getExplanationOfValueProduced(trace, eventID); }
	},

	CONSTANT_LONG_PRODUCED("(J)V", OperandStackType.LONG, true, true, false, false, false, false, false, false, false, false) { 
		public String getDescription(Trace trace, int eventID) { return getDescriptionOfConstantProduced(trace, eventID); }
		public String getHTMLDescription(Trace trace, int eventID) { return getExplanationOfValueProduced(trace, eventID); }
	},

	CONSTANT_OBJECT_PRODUCED("(J)V", OperandStackType.REFERENCE, true, true, true, false, false, false, false, false, false, false) {
		public String getDescription(Trace trace, int eventID) { return getDescriptionOfConstantProduced(trace, eventID); }
		public String getHTMLDescription(Trace trace, int eventID) { return getExplanationOfValueProduced(trace, eventID); }
	},

	THIS_PRODUCED("(J)V", OperandStackType.REFERENCE, false, true, true, false, false, false, false, false, false, false) {
		public String getDescription(Trace trace, int eventID) { return getDescriptionOfObjectProduced(trace, eventID); }
		public String getHTMLDescription(Trace trace, int eventID) { return getExplanationOfValueProduced(trace, eventID); }
	},

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// These represent executions of instructions that compute numbers that aren't constant.
	//
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	INTEGER_PRODUCED("(IJ)V", OperandStackType.INT, false, true, false, false, false, false, false, false, true, false) { 
		public String getDescription(Trace trace, int eventID) { 
			try { return Integer.toString(trace.getIntegerProduced(eventID)); }
			catch (NoValueException e) { return "?"; } 
		}
		public String getHTMLDescription(Trace trace, int eventID) { return getExplanationOfValueProduced(trace, eventID); }
	},

	SHORT_PRODUCED("(SJ)V", OperandStackType.INT, false, true, false, false, false, false, false, false, true, false) { 
		public String getDescription(Trace trace, int eventID) { 
			try { return Short.toString(trace.getShortProduced(eventID)); }
			catch (NoValueException e) { return "?"; } 
		}
		public String getHTMLDescription(Trace trace, int eventID) { return getExplanationOfValueProduced(trace, eventID); }
	},

	BYTE_PRODUCED("(BJ)V", OperandStackType.INT, false, true, false, false, false, false, false, false, true, false) { 
		public String getDescription(Trace trace, int eventID) { 
			try { return Byte.toString(trace.getByteProduced(eventID)); }
			catch (NoValueException e) { return "?"; } 
		}
		public String getHTMLDescription(Trace trace, int eventID) { return getExplanationOfValueProduced(trace, eventID); }
	},

	FLOAT_PRODUCED("(FJ)V", OperandStackType.FLOAT, false, true, false, false, false, false, false, false, true, false) { 
		public String getDescription(Trace trace, int eventID) { 
			try { return Float.toString(trace.getFloatProduced(eventID)); }
			catch (NoValueException e) { return "?"; } 
		}
		public String getHTMLDescription(Trace trace, int eventID) { return getExplanationOfValueProduced(trace, eventID); }
	},

	BOOLEAN_PRODUCED("(ZJ)V", OperandStackType.INT, false, true, false, false, false, false, false, false, true, false)	 { 
		public String getDescription(Trace trace, int eventID) { 
			try { return Boolean.toString(trace.getBooleanProduced(eventID)); }
			catch (NoValueException e) { return "?"; } 
		}
		public String getHTMLDescription(Trace trace, int eventID) { return getExplanationOfValueProduced(trace, eventID); }
	},

	CHARACTER_PRODUCED("(CJ)V", OperandStackType.INT, false, true, false, false, false, false, false, false, true, false) {
		public String getDescription(Trace trace, int eventID) { 
			try { return Character.toString(trace.getCharacterProduced(eventID)); }
			catch (NoValueException e) { return "?"; } 
		}
		public String getHTMLDescription(Trace trace, int eventID) { return getExplanationOfValueProduced(trace, eventID); }
	},
	
	DOUBLE_PRODUCED("(DJ)V", OperandStackType.DOUBLE, false, true, false, false, false, false, false, false, true, false) { 
		public String getDescription(Trace trace, int eventID) { 
			try { return Double.toString(trace.getDoubleProduced(eventID)); 	}
			catch (NoValueException e) { return "?"; } 
		}
		public String getHTMLDescription(Trace trace, int eventID) { return getExplanationOfValueProduced(trace, eventID); }
	},

	LONG_PRODUCED("(JJ)V", OperandStackType.LONG, false, true, false, false, false, false, false, false, true, false) { 
		public String getDescription(Trace trace, int eventID) { 
			return Long.toString(trace.getLongProduced(eventID));
		}
		public String getHTMLDescription(Trace trace, int eventID) { return getExplanationOfValueProduced(trace, eventID); }
	},

	OBJECT_PRODUCED("(Ljava/lang/Object;ZJ)V", OperandStackType.REFERENCE, false, true, true, false, false, false, false, false, true, false) { 
		public String getDescription(Trace trace, int eventID) { return getDescriptionOfObjectProduced(trace, eventID); }
		public String getHTMLDescription(Trace trace, int eventID) { return getExplanationOfValueProduced(trace, eventID); }
	},

	NEW_OBJECT("(Ljava/lang/Object;ZJ)V", OperandStackType.REFERENCE, false, true, true, false, false, false, false, true, true, false) {
		public String getDescription(Trace trace, int eventID) { 
			long id = trace.getObjectIDProduced(eventID);
			return "new " + (id < 0 ? "?" : trace.getDescriptionOfObjectID(id)); 
		}
		public String getHTMLDescription(Trace trace, int eventID) { return getExplanationOfValueProduced(trace, eventID); }
	},

	NEW_ARRAY("(Ljava/lang/Object;J)V", OperandStackType.REFERENCE, false, true, true, false, false, false, false, true, true, false) { 
		public String getDescription(Trace trace, int eventID) { 
			long id = trace.getObjectIDProduced(eventID);
			return "new " + (id < 0 ? "[?]" : trace.getDescriptionOfObjectID(id));
		}
		public String getHTMLDescription(Trace trace, int eventID) { return getExplanationOfValueProduced(trace, eventID); }
	},
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// These represent setting of arguments on a method call, which doesn't correspond to a particular instruction.
	//
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	INTEGER_ARG("(IJ)V", OperandStackType.INT, false, false, false, false, true, false, true, false, false, true) { 
		public String getDescription(Trace trace, int eventID) { return getDescriptionOfSetArgumentEvent(trace, eventID); }
		public String getHTMLDescription(Trace trace, int eventID) { return getExplanationOfSetMethodArgument(trace, eventID); }
	},

	SHORT_ARG("(SJ)V", OperandStackType.INT, false, false, false, false, true, false, true, false, false, true) { 
		public String getDescription(Trace trace, int eventID) { return getDescriptionOfSetArgumentEvent(trace, eventID); }
		public String getHTMLDescription(Trace trace, int eventID) { return getExplanationOfSetMethodArgument(trace, eventID); }
	},

	BYTE_ARG("(BJ)V", OperandStackType.INT, false, false, false, false, true, false, true, false, false, true) { 
		public String getDescription(Trace trace, int eventID) { return getDescriptionOfSetArgumentEvent(trace, eventID); }
		public String getHTMLDescription(Trace trace, int eventID) { return getExplanationOfSetMethodArgument(trace, eventID); }
	},

	FLOAT_ARG("(FJ)V", OperandStackType.FLOAT, false, false, false, false, true, false, true, false, false, true) {
		public String getDescription(Trace trace, int eventID) { return getDescriptionOfSetArgumentEvent(trace, eventID); }
		public String getHTMLDescription(Trace trace, int eventID) { return getExplanationOfSetMethodArgument(trace, eventID); }
	},

	BOOLEAN_ARG("(ZJ)V", OperandStackType.INT, false, false, false, false, true, false, true, false, false, true) { 
		public String getDescription(Trace trace, int eventID) { return getDescriptionOfSetArgumentEvent(trace, eventID); }
		public String getHTMLDescription(Trace trace, int eventID) { return getExplanationOfSetMethodArgument(trace, eventID); }
	},

	CHARACTER_ARG("(CJ)V", OperandStackType.INT, false, false, false, false, true, false, true, false, false, true) { 
		public String getDescription(Trace trace, int eventID) { return getDescriptionOfSetArgumentEvent(trace, eventID); }
		public String getHTMLDescription(Trace trace, int eventID) { return getExplanationOfSetMethodArgument(trace, eventID); }
	},
	
	DOUBLE_ARG("(DJ)V", OperandStackType.DOUBLE, false, false, false, false, true, false, true, false, false, true) { 
		public String getDescription(Trace trace, int eventID) { return getDescriptionOfSetArgumentEvent(trace, eventID); }
		public String getHTMLDescription(Trace trace, int eventID) { return getExplanationOfSetMethodArgument(trace, eventID); }
	},
	
	LONG_ARG ("(JJ)V", OperandStackType.LONG, false, false, false, false, true, false, true, false, false, true) { 
		public String getDescription(Trace trace, int eventID) { return getDescriptionOfSetArgumentEvent(trace, eventID); }
		public String getHTMLDescription(Trace trace, int eventID) { return getExplanationOfSetMethodArgument(trace, eventID); }
	},
	
	OBJECT_ARG("(Ljava/lang/Object;J)V", OperandStackType.REFERENCE, false, false, false, false, true, false, true, false, false, true) { 
		public String getDescription(Trace trace, int eventID) { return getDescriptionOfSetArgumentEvent(trace, eventID); }
		public String getHTMLDescription(Trace trace, int eventID) { return getExplanationOfSetMethodArgument(trace, eventID); }
	},
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// These are special events that we record to parse graphical output.
	//
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	GETGRAPHICS("(Ljava/lang/Object;J)Ljava/awt/Graphics2D;", null, false, false, false, false, false, false, false, false, false, true) { 
		public String getDescription(Trace trace, int eventID) { return "repaint"; }
		public String getHTMLDescription(Trace trace, int eventID) { return "repaint"; }
	},
	
	CREATEGRAPHICS("(Ljava/lang/Object;J)Ljava/awt/Graphics2D;", null, false, false, false, false, false, false, false, false, false, true) { 
		public String getDescription(Trace trace, int eventID) { return "create graphics context"; }
		public String getHTMLDescription(Trace trace, int eventID) { return "create graphics context"; }
	},
		
	MOUSE_EVENT("(Ljava/lang/Object;IIIIJ)V", null, false, false, false, false, false, false, false, false, false, true) {
		public String getDescription(Trace trace, int eventID) { 
			
			String description = null;
			
			MouseArguments args = trace.getMouseArguments(eventID);
			int type = args.type;
			int button = args.button;
			
			if(type == MouseEvent.MOUSE_CLICKED)
				description = "button " + button + " click";
			else if(type == MouseEvent.MOUSE_DRAGGED)
				description = "mouse drag";
			else if(type == MouseEvent.MOUSE_PRESSED)
				description = "button " + button + " press";
			else if(type == MouseEvent.MOUSE_RELEASED)
				description = "button " + button + " release";
			else if(type == MouseEvent.MOUSE_MOVED || type == MouseEvent.MOUSE_ENTERED || type == MouseEvent.MOUSE_EXITED)
				description = "mouse move";
			else if(type == MouseEvent.MOUSE_WHEEL)
				description = "mouse wheel move";
			else
				description = "unknown type of mouse event";

			return description;
			
		}
		public String getHTMLDescription(Trace trace, int eventID) { return getDescription(trace, eventID); }
	},

	KEY_EVENT("(Ljava/lang/Object;IIICIJ)V", null, false, false, false, false, false, false, false, false, false, true) {
		public String getDescription(Trace trace, int eventID) { 
			
			KeyArguments args = trace.getKeyArguments(eventID);

			int type = args.type;
			
			StringBuilder builder = new StringBuilder();
			
			String modifiersText = KeyEvent.getModifiersExText(args.modifiers);
			builder.append(modifiersText);
			if(modifiersText.length() > 0) builder.append(" ");
			
			if(type == KeyEvent.KEY_PRESSED) {
				builder.append(KeyEvent.getKeyText(args.keyCode));
				builder.append(" was pressed");
			}
			else if(type == KeyEvent.KEY_RELEASED) {
				builder.append(KeyEvent.getKeyText(args.keyCode));
				builder.append(" was released");
			}
			else if(type == KeyEvent.KEY_TYPED) {
				builder.append('\'');
				builder.append((char)args.keyChar);
				builder.append('\'');
				builder.append(" was typed");
			}
			else
				builder.append("unknown type of mouse event ");

			return builder.toString();
			
		}
		public String getHTMLDescription(Trace trace, int eventID) { return getDescription(trace, eventID); }
	},

	WINDOW("(Ljava/lang/Object;J)V", null, false, false, false, false, false, false, false, false, false, true) {
		public String getDescription(Trace trace, int eventID) { 
			return "window was " + (isHide(trace, eventID) ? "hidden" : "shown");
		}
		public String getHTMLDescription(Trace trace, int eventID) { return getDescription(trace, eventID); }
		public boolean isHide(Trace trace, int eventID) { return trace.getInstruction(eventID).getMethod().getInternalName().equals("hide"); }
	},

	IMAGE_SIZE("(Ljava/lang/Object;J)V", null, false, false, false, false, false, false, false, false, false, true) {
		public String getDescription(Trace trace, int eventID) { 
			return "image size";
		}
		public String getHTMLDescription(Trace trace, int eventID) { return getDescription(trace, eventID); }
	}
	
	;
	
	public final String name, descriptor;

	public final int id = ordinal();
	public final boolean isConstantProduced;
	private final OperandStackType stackType;
	
	public final boolean isValueProduced;
	public final boolean isObjectProduced;
	public final boolean isInvocation;
	public final boolean isArgument;
	public final boolean isBranch;
	public final boolean isDefinition;
	public final boolean isInstantiation;
	public final boolean isNonArgumentDefinition;

	public final boolean loadImmediately;
	public final boolean isArtificial;

	private static EventKind[] values = values();
	
	static {
		
		assert values.length <= 64 : "Can't have more than 64 kinds of events because we only use 6 bits of a byte to store the kind.";
		
	}

	EventKind(
			String descriptor, 
			OperandStackType stackType, 
			boolean isConstantProduced, 
			boolean isValueProduced, 
			boolean isObjectProduced, 
			boolean isInvocation, 
			boolean isArgument, 
			boolean isBranch, 
			boolean isDefinition, 
			boolean isInstantiation, 
			boolean loadImmediately, 
			boolean isArtificial) { 

		this.stackType = stackType;

		this.isConstantProduced = isConstantProduced;
		this.isValueProduced = isValueProduced;
		this.isObjectProduced = isObjectProduced;
		this.isInvocation = isInvocation;
		this.isArgument = isArgument;
		this.isBranch = isBranch;
		this.isDefinition = isDefinition;
		this.isInstantiation = isInstantiation;
		
		this.isNonArgumentDefinition = isDefinition && !isArgument;
		
		this.loadImmediately = loadImmediately;
		this.isArtificial = isArtificial;

		name = name();
		
		this.descriptor = descriptor;
		
	}

	private static String classToInternalTypeName(Class<?> t) {

		String type = t.getName();
		if(type.startsWith("[")) return type;
		else if(type.equals("boolean")) return "Z";
		else if(type.equals("byte")) return "B";
		else if(type.equals("float")) return "F";
		else if(type.equals("double")) return "D";
		else if(type.equals("char")) return "C";
		else if(type.equals("short")) return "S";
		else if(type.equals("int")) return "I";
		else if(type.equals("long")) return "J";
		else if(type.equals("void")) return "V";
		else return "L" + type.replace('.', '/') + ";";		
		
	}
	
	public String getName() { return name; }
	public String getDescriptor() { return descriptor; }
	public OperandStackType getStackType() { return stackType; }
	public boolean isDoubleOrLong() { return stackType.isDoubleOrLong(); }

	public abstract String getDescription(Trace trace, int eventID);
	public abstract String getHTMLDescription(Trace trace, int eventID);
	
	public static EventKind intToEvent(int event) { return values[event]; }

	public static int getNumberOfKinds() { return values.length; }
	
	protected String getDescriptionOfSetArgumentEvent(Trace trace, int eventID) {

		return getNameOfArgumentSet(trace, eventID) + " = " + getDescriptionOfSetArgumentValue(trace, eventID);
		
	}

	protected String getExplanationOfSetMethodArgument(Trace trace, int eventID) { 
		
		return 
			"<b>" + 
			getNameOfArgumentSet(trace, eventID) + 
			"</b> was passed <b>" + 
			getDescriptionOfSetArgumentValue(trace, eventID) + 
			"</b>"; 
		
	}

	protected String getNameOfArgumentSet(Trace trace, int eventID) {
		
		Instruction inst = trace.getInstruction(eventID);
		return inst.getCode().getLocalIDNameRelativeToInstruction(trace.getArgumentLocalIDSet(eventID), inst);
		
	}

	public String getDescriptionOfSetArgumentValue(Trace trace, int eventID) { 
		
		String text = null;

		try {
		
			switch(trace.getKind(eventID)) {
				case OBJECT_ARG :
					
					long val = trace.getObjectIDProduced(eventID);
					
					if(val == 0) return "null";
					
					Object immutable = trace.getImmutableObject(val);
					if(immutable instanceof String) return "\"" + (String)immutable + "\"";
		
					return trace.getDescriptionOfObjectID(val);
		
				case INTEGER_ARG : return Integer.toString(trace.getIntegerProduced(eventID));
				case SHORT_ARG : return Short.toString(trace.getShortProduced(eventID));
				case FLOAT_ARG : return Float.toString(trace.getFloatProduced(eventID));
				case BOOLEAN_ARG : return Boolean.toString(trace.getBooleanProduced(eventID));
				case CHARACTER_ARG : return Character.toString(trace.getCharacterProduced(eventID));
				case DOUBLE_ARG : return Double.toString(trace.getDoubleProduced(eventID));
				case LONG_ARG : return Long.toString(trace.getLongProduced(eventID));
				default : return "unknown type of set argument type: " + trace.getKind(eventID);
					
			}
			
		} catch(NoValueException e) {
			return "?";
		}
		
	}
	
	protected String getDescriptionOfObjectProduced(Trace trace, int eventID) {
		
		Object objectProduced = trace.getObjectProduced(eventID);
		// Could a constant value be found?
		if(objectProduced instanceof String) return "\"" + objectProduced + "\"";
		// Otherwise, generate a more suitable name.
		else {
			long objectID = trace.getObjectIDProduced(eventID);
			return objectID < 0 ? "?" : trace.getDescriptionOfObjectID(objectID);
		}

	}

	protected String getExplanationOfValueProduced(Trace trace, int eventID) {
		
		Instruction inst = trace.getInstruction(eventID);
		
		String valueText = trace.getKind(eventID).getDescription(trace, eventID);
		valueText = valueText.replace("<", "&lt;");
		valueText = valueText.replace(">", "&gt;");
		valueText = "<b>" + valueText + "</b>";
		
		if(inst instanceof GetLocal) {
			
			return "<b>" + ((GetLocal)inst).getLocalIDName() + "</b> was " + valueText; 
			
		}
		else if(inst instanceof GetArrayValue) {
			
			return trace.getOperandStackValue(eventID, 0).getVerbalExplanation() + ", and the value at index <b>" + trace.getOperandStackValue(eventID, 1).getDisplayName(true) + "</b> was " + valueText; 			
			
		}
		else if(inst instanceof PushConstant) {

			return valueText +" was a constant";
			
		}
		else if(inst instanceof NEW) {
			
			return "Instantiated a new <b>" + ((NEW)inst).getClassInstantiated().getName().getSimpleName() + "</b>";
			
		}
		else if(inst instanceof Instantiation) {
			
			return "a new array was created";
			
		}
		else if(inst instanceof Duplication) {
			
			return trace.getOperandStackValue(eventID, 0).getVerbalExplanation(); 
			
		}
		else if(inst instanceof ARRAYLENGTH) {
			
			return "" + trace.getOperandStackValue(eventID, 0).getVerbalExplanation() + " had " + valueText + " elements";
			
		}
		else if(inst instanceof Invoke) {
			
			return "<b>" + ((Invoke)inst).getMethodInvoked().getMethodName() + "()</b> returned " + valueText;
			
		}
		else if(inst instanceof CHECKCAST) {
			
			return "";
			
		}
		else if(inst instanceof UnaryComputation) {
			
			return ((UnaryComputation)inst).getPastTenseVerb() + " " + trace.getOperandStackValue(eventID, 0).getDisplayName(true) + " and got " + valueText;
			
		}
		else if(inst instanceof BinaryComputation) {

			return ((BinaryComputation)inst).getPastTenseVerb() + " " + trace.getOperandStackValue(eventID, 0).getDisplayName(true) + " and " + trace.getOperandStackValue(eventID, 1).getDisplayName(true) + " and got " + valueText; 

		}
		else if(inst instanceof GETFIELD) {

			return "<b>" + trace.getOperandStackValue(eventID, 0).getDisplayName(true) + "</b>'s field <b>" + ((GETFIELD)inst).getFieldref().getName() + "</b> was " + valueText;
			
		}
		else if(inst instanceof GETSTATIC) {
			
			return "<b>" + ((GETSTATIC)inst).getFieldref().getClassname().getSimpleName() + "." + ((GETSTATIC)inst).getFieldref().getName() + "</b> was " + valueText;
			
		}
		else if(inst instanceof INSTANCEOF) {
			
			boolean wasInstanceOf = valueText.equals("true");
			return "<b>" + trace.getOperandStackValue(eventID, 0).getDisplayName(true) + "</b> was " + (wasInstanceOf ? "" : "<b>not</b>") + " an instance of <b>" + ((INSTANCEOF)inst).getClassInfo().getName().getSimpleName() + "</b>.";
			
		}
		else {
			
			return "";
			
		}
		
	}

	protected String getDescriptionOfConstantProduced(Trace trace, int eventID) {
		
		Instruction inst = trace.getInstruction(eventID);

		Object value = null;
		
		int memoryDependencyID = trace.getHeapDependency(eventID);
		
		// If a get local produced a value, find its definition.
		if(inst instanceof GetLocal) { 
			
			trace.getOperandStackDependencies(eventID);
			assert memoryDependencyID >= 0 : "Why don't we have a definition for " + inst + "?";

			EventKind memoryKind = trace.getKind(memoryDependencyID);
			if(memoryKind.isArgument)
				return memoryKind.getDescriptionOfSetArgumentValue(trace, eventID);
			
			else {
			
				Value valueSet = trace.getDefinitionValueSet(memoryDependencyID);
				assert valueSet != null : "Why don't we have a value set for " + trace.getInstruction(memoryDependencyID) + "?";
				
				try {
					value = valueSet.getValue();
				} catch (NoValueException e) {}

			}

		}
		// If a push constant generated the value, return the instruction's constant.
		else if(inst instanceof PushConstant) value = ((PushConstant<?>)inst).getConstant();
		
		// If a single dup generated the the value, regardless of where it inserted it in the stack, return the producer's constant
		else if(inst instanceof DUP || inst instanceof DUP_X1 || inst instanceof DUP_X2) {
			
			try {
				value = trace.getOperandStackValue(eventID, 0).getValue();
			} catch (NoValueException e) {}
		
		}
		
		// Otherwise, we need to determine which constant value produced by a DUP2 to return.
		else if(inst instanceof DUP2 || inst instanceof DUP2_X1 || inst instanceof DUP2_X2) {

			System.err.println("" + inst + " used value produced by ");
			for(Value producer : trace.getOperandStackDependencies(eventID))
				System.err.println("" + producer);
			
			Whyline.debug("Still don't know how to find the constant values produced by a DUP2 instruction.");
			
		}
		else assert false : "Don't know how to determine a constant value produced by " + inst.getClass();
	
		return "" + value;
		
	}
	
	public static String getAssignmentHTML(String name, String value) {

		return "<b>" + name  + "</b> was assigned <b>" + value + "</b>";
		
	}

	public static String getInvocationHTML(Trace trace, int eventID) {

		Invoke invoke = (Invoke)trace.getInstruction(eventID);

		
		String prefix = "Called <b>" + invoke.getJavaMethodName() + "()</b>";
		if(invoke instanceof INVOKESTATIC)
			return prefix;
		else
			return prefix + " on <b>" + trace.getOperandStackValue(eventID, 0).getDisplayName(true)+ "</b>";
		
	}

	public String toString() { 

		return name;
	
	}
	
}
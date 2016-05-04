/**
 * 
 */
package edu.cmu.hcii.whyline.analysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.cmu.hcii.whyline.bytecode.*;
import edu.cmu.hcii.whyline.trace.Trace;

/**
 * @author Andrew J. Ko
 *
 */
public class ValueSource {

	public final Instruction instruction;
	public final ValueSource typeProducedIn;
	public QualifiedClassName type;
	public final ArrayList<Instruction> path;
	
	private int isEncapsulatedbyProducingClass = -1;
	
	public ValueSource(QualifiedClassName type) {
		
		if(type != null) this.type = type;
		this.instruction = null;
		this.typeProducedIn = null;
		path = new ArrayList<Instruction>(0);
		
	}
	
	public ValueSource(Instruction inst, ValueSource typeProducedIn, Iterable<Instruction> path) {
		
		this.instruction = inst;
		this.typeProducedIn = typeProducedIn;
		
		this.path = new ArrayList<Instruction>();
		for(Instruction i : path)
			this.path.add(i);
		this.path.trimToSize();
		
		if(instruction instanceof PushConstant) {
			
			Object constant = ((PushConstant<?>)instruction).getConstant();
			if(constant == null) type = QualifiedClassName.NULL;
			else if(constant instanceof QualifiedClassName) type = QualifiedClassName.JAVA_LANG_CLASS;
			else if(constant instanceof String) type = QualifiedClassName.JAVA_LANG_STRING;

		}
		
		else if(instruction instanceof Instantiation) 
			type = ((Instantiation)instruction).getClassnameOfTypeProduced();

		else if(instruction instanceof GETSTATIC) {
			
			String typeDescriptor = ((GETSTATIC)instruction).getFieldref().getTypeDescriptor();
			if(typeDescriptor.charAt(0) == 'L') typeDescriptor = typeDescriptor.substring(1, typeDescriptor.length() - 1);
			type = QualifiedClassName.get(typeDescriptor);
			
		}
		
		// If its a "this" we use the instruction's class. 
		else if(instruction instanceof GetLocal) {
			
			if(instruction instanceof ALOAD_0 && !instruction.getMethod().isStatic())
				type = instruction.getClassfile().getInternalName();
			
			// Is it a method parameter?
			else if(((GetLocal)instruction).getLocalID() < instruction.getMethod().getLocalIDOfFirstNonArgument()) {
			
				MethodInfo methodOfValue = instruction.getMethod();
				int argumentNumber = methodOfValue.getParsedDescriptor().getArgumentNumberFromLocalID(((GetLocal)instruction).getLocalID());
				QualifiedClassName typeName = methodOfValue.getParsedDescriptor().getTypeOfArgumentNumber(methodOfValue.isStatic() ? argumentNumber : argumentNumber - 1);
				type = typeName;

			}
			// Otherwise, its probably a throwable in a catch.
			else {
				
				List<ExceptionHandler> handlers = instruction.getCode().getExceptionHandlersThatExecute(instruction);
				for(ExceptionHandler handler : handlers)
					type = handler.getCatchType() == null ? QualifiedClassName.JAVA_LANG_THROWABLE : handler.getCatchType().getName();
				
			}
			
		}
		else if(instruction instanceof Computation) {
			
			
		}
		else if(instruction instanceof Invoke) {
			
			type = ((Invoke)instruction).getMethodInvoked().getParsedDescriptor().getReturnType();

		}
		else assert false : "How did " + instruction + " end up producing a potential value for something?";

	}

	public boolean isEncapsulatedByProducingClass(Trace trace) {

		if(instruction == null) return false;
		
		if(instruction instanceof ALOAD_0) return false;
		
		if(isEncapsulatedbyProducingClass == -1)
			isEncapsulatedbyProducingClass = isEncapsulatedHelper(new HashSet<Instruction>(), null, instruction, trace) ? 1 : 0;
		return isEncapsulatedbyProducingClass == 1;
		
	}
	
	private boolean isEncapsulatedHelper(Set<Instruction> visited, Instruction producer, Instruction consumer, Trace trace) {

		if(visited.contains(consumer)) return false;
		visited.add(consumer);

		//	if we hit a put field, check consumers of all get fields
		if(consumer instanceof PUTFIELD) {

			FieldInfo field = trace.resolveFieldReference(((PUTFIELD)consumer).getFieldref());
			if(field != null)
				for(Use fieldUser : field.getUses())
					for(Instruction fieldUserConsumer : fieldUser.getConsumers())
						if(!isEncapsulatedHelper(visited, fieldUser, fieldUserConsumer, trace))
							return false;
			
		}
		//	if we hit a set local, check consumers of all uses
		else if(consumer instanceof SetLocal) {
			
			for(Use use : consumer.getCode().getLocalDependencies().getPotentialUsesOfLocalIDAtOrAfter(consumer, ((SetLocal)consumer).getLocalID()))
				for(Instruction useConsumer : use.getConsumers())
					if(!isEncapsulatedHelper(visited, use, useConsumer, trace))
						return false;
		
		}
		// if we hit a method call and pass it as an argument other than the instance, say no
		else if(consumer instanceof Invoke) { 
			
			int arg = consumer.getArgumentNumberOfProducer(producer);
			if(((Invoke)consumer).getMethodInvoked().isStatic()) return false;
			else if(arg != 0) return false;
			
		}
		//	if we hit a return, say no
		else if(consumer instanceof AbstractReturn) { return false; }
		//	if we hit a set array, say no (we could be more precise)
		else if(consumer instanceof SetArrayValue) { return false; }
		//	if we hit a put static, say no unless private
		else if(consumer instanceof PUTSTATIC)  { return false; }
		//	if we hit something else (like computation or dups), follow its consumers 
		else {
			
			for(Instruction consumerConsumer : consumer.getConsumers())
				if(!isEncapsulatedHelper(visited, consumer, consumerConsumer, trace))
					return false;
			
		}
		
		return true;
		
	}
	
	public boolean equals(Object o) {
		
		if(!(o instanceof ValueSource)) return false;
		ValueSource other = (ValueSource)o;
		if(other.instruction != this.instruction) return false;
		if(other.type != type) return false;
		if(other.typeProducedIn == null && typeProducedIn == null) return true;
		if(other.typeProducedIn != null && typeProducedIn == null) return false;
		if(other.typeProducedIn == null && typeProducedIn != null) return false;
		return other.typeProducedIn.equals(typeProducedIn);
		
	}

	public String toString() { return "" + type + (typeProducedIn == null ? "" : " produced in " + typeProducedIn) + " by " + instruction; }
	
}
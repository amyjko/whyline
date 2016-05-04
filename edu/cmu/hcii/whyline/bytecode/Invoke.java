package edu.cmu.hcii.whyline.bytecode;

import java.util.*;

import edu.cmu.hcii.whyline.analysis.Cancelable;
import edu.cmu.hcii.whyline.analysis.ValueSource;
import edu.cmu.hcii.whyline.analysis.ValueSourceAnalyzer;

import edu.cmu.hcii.whyline.trace.*;
import edu.cmu.hcii.whyline.tracing.ClassIDs;

/**
 * @author Andrew J. Ko
 *
 */ 
public abstract class Invoke extends Instruction {

	private int numberOfOperandsConsumed = -1;

	protected final MethodrefInfo methodInfo;

	private Set<ValueSource> potentialValuesOfInstance;
	private MethodInfo[] preciseMethodsCalled;
	private HashMap<MethodInfo, ValueSource> valuesByMethod;
	
	public Invoke(CodeAttribute method, MethodrefInfo ref) {
		
		super(method);
		
		this.methodInfo = ref;
		
	}

	public abstract int getOpcode();

    /**
     * Converts internal, invokable method names like <init> to their java counterparts. 
     */
    public String getJavaMethodName() { return methodInfo.getMethodName(); }

	public MethodrefInfo getMethodInvoked() { return methodInfo; }
	
	public final int getNumberOfOperandsProduced() { return getMethodInvoked().returnsVoid() ? 0 : 1; }
	
	public int getNumberOfOperandsConsumed() { 
		
		// Optimization to avoid creating a parsed method descriptor.
		if(numberOfOperandsConsumed == -1) numberOfOperandsConsumed = getMethodInvoked().getNumberOfParameters() + 1; 
		return numberOfOperandsConsumed;
		
	}

	public final int getNumberOfOperandsPeekedAt() { return 0; }
	
	public EventKind getTypeProduced() { 

		QualifiedClassName returnType = getMethodInvoked().getReturnType(); 
		if(returnType.isPrimitive())
			return NameAndTypeInfo.typeCharacterToClass(returnType.getText().charAt(0));
		else
			return EventKind.OBJECT_PRODUCED;
		
	}

	public StackDependencies.Producers getInstanceProducers() {

		if(this instanceof INVOKESTATIC) return new StackDependencies.Producers();
		else return getProducersOfArgument(0);
		
	}

	public boolean callsOnThis() {
		
		Instruction instanceProducer = getProducersOfArgument(0).getFirstProducer();
		return instanceProducer instanceof GetLocal && ((GetLocal)instanceProducer).getLocalID() == 0;
		
	}
				
	public MethodInfo[] getPreciseMethodsCalled(Trace trace, Cancelable cancelable) {

		if(cancelable != null && cancelable.wasCanceled()) return null;
		
		if(preciseMethodsCalled != null) return preciseMethodsCalled;
		
		// Assign an empty array in case the call graph is recursive. This way, we won't run this method through infinite recursion.
		preciseMethodsCalled = new MethodInfo[0];
		
		Set<MethodInfo> preciseMethodsCalledSet = new HashSet<MethodInfo>();
		
		preciseMethodsCalledSet = new HashSet<MethodInfo>();
		potentialValuesOfInstance = ValueSourceAnalyzer.getPotentialValues(trace, this, 0, cancelable);
		if(cancelable != null && cancelable.wasCanceled()) {
			preciseMethodsCalled = null;
			potentialValuesOfInstance = null;
			return null;
		}

		Map<MethodInfo, ValueSource> values = new HashMap<MethodInfo, ValueSource>(potentialValuesOfInstance.size() / 3);
		for(ValueSource potentialValue : potentialValuesOfInstance) {

			Instruction value = potentialValue.instruction;
			
			if(potentialValue.type != null) {
				MethodInfo method = trace.resolveMethodReference(potentialValue.type, this);
				if(method != null) {
					preciseMethodsCalledSet.add(method);
					values.put(method, potentialValue);
				}
				
			}
			
		}
		
		// If we didn't find any methods, we have to be imprecise.
		if(preciseMethodsCalledSet.isEmpty()) {

			// We'll be imprecise for virtual methods, but not for interface calls. They're too unpredictable.
			if(this instanceof INVOKEVIRTUAL) {
				for(MethodInfo method : trace.getMethodsFromReference(this))
					preciseMethodsCalledSet.add(method);
			}
			
		}
		
		preciseMethodsCalled = new MethodInfo[preciseMethodsCalledSet.size()];
		preciseMethodsCalledSet.toArray(preciseMethodsCalled);
		return preciseMethodsCalled;
		
	}

	@Deprecated
	public final String getTypeDescriptorOfArgument(int argIndex) { 
		
		// If we want the first argument of a virtual method, return the class.
		if(this instanceof INVOKESTATIC)
			return getMethodInvoked().getParsedDescriptor().getTypeOfArgumentNumber(argIndex).getText();
		else {
			if(argIndex == 0) {
				StringBuilder builder = new StringBuilder("L");
				builder.append(getMethodInvoked().getClassName().getText());
				builder.append(";");
				return builder.toString();
			}
			// Otherwise, return the argument type of the descriptor.
			else 
				return getMethodInvoked().getParsedDescriptor().getTypeOfArgumentNumber(argIndex - 1).getText();
		}
		
	}
	
	public ValueSource getPotentialValueFor(MethodInfo method) { 
		
		if(valuesByMethod == null) return null;	
		else return valuesByMethod.get(method); 
		
	}

	public Set<ValueSource> getPotentialValuesOfInstance(Trace trace) { return potentialValuesOfInstance; }
	
	public String getReadableDescription() { return getMethodInvoked().getMethodName() + "()"; }

	public String getAssociatedName() { return getJavaMethodName(); }

	/**
	 * This may have arguments representing enclosing instances, if this is a constructor call on an inner class.
	 * If so, we may return 2 or 3, depending on the type of inner class. Otherwise, we return 1.
	 */
	public int getFirstArgumentAppearingInSource() {
		
		if(methodInfo.callsInstanceInitializer() && methodInfo.getClassName().isInner() && !getClassfile().isStatic()) {

			if(getClassfile().isInnerClass())
				return 3;
			else
				return 2;
			
		}
		else 
			return 1;

	}

	public boolean couldCallOn(ValueSource typeOfThis, Trace trace) {
	
		getPreciseMethodsCalled(trace, null);
		
		assert potentialValuesOfInstance != null;

		ClassIDs classids = trace.getClassIDs();

		boolean thisIsEncapsulated = typeOfThis.isEncapsulatedByProducingClass(trace);
		
		for(ValueSource value : potentialValuesOfInstance) {

			boolean instanceTypeIsEncapsulated = value.isEncapsulatedByProducingClass(trace);
			
			if(value.type != null && typeOfThis != null) {

				Classfile valueClass = trace.getClassfileByName(value.type);

				// If the value is null or the value is compatible with the type of "this"...
				if(valueClass == null || valueClass.isExtendsOrImplements(typeOfThis.type)) {
					
					// If the value of this doesn't escape its creator...
					if(thisIsEncapsulated) {
						// And the value doesn't either
						if(value.isEncapsulatedByProducingClass(trace)) {
							
							// Only allow the call if the creators are compatible 
							if(typeOfThis.typeProducedIn.type != null && value.typeProducedIn.type != null) {

								Classfile valueProducerClass = trace.getClassfileByName(value.typeProducedIn.type);
								if(valueProducerClass == null || valueProducerClass.isExtendsOrImplements(typeOfThis.typeProducedIn.type))
									return true;

							}
							
						}
					}
					// If the value of "this" is not encapsulated, and it escapes its creator, then allow this call.
					else return true;
				}
			}
			
		}
		return false;
	
	}

	public String toString() { return super.toString() + " " + getMethodInvoked().getShortQualifiedNameAndDescriptor(); }

}
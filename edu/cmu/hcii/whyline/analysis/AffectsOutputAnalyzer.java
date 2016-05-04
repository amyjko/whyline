package edu.cmu.hcii.whyline.analysis;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import edu.cmu.hcii.whyline.bytecode.*;
import edu.cmu.hcii.whyline.io.GraphicalOutputParser;
import edu.cmu.hcii.whyline.trace.Trace;
import edu.cmu.hcii.whyline.util.Util;

/**
 * @author Andrew J. Ko
 *
 */
public final class AffectsOutputAnalyzer {

	private final Set<Instruction> dataDependenciesVisited = new HashSet<Instruction>();
	private final Set<Instruction> instructionsCheckedForInvokingOutput = new HashSet<Instruction>();

	private final Trace trace;

	private final Set<FieldInfo> fieldsPotentiallyAffectingOutput = new HashSet<FieldInfo>();
	private final Set<MethodInfo> methodsPotentiallyAffectingOutput = new HashSet<MethodInfo>();
	private final Set<MethodInfo> methodsPotentiallyInvokingOutput = new HashSet<MethodInfo>();

	private List<Instruction> graphical;
	private Collection<Instruction> textual;

	private double remainingOfCurrentPrimitive = 0.0;

	private final Util.ProgressListener listener;
	
	public AffectsOutputAnalyzer(Trace trace, Util.ProgressListener listener) {

		this.trace = trace;
		this.listener = listener;
				
		this.graphical = trace.getGraphicalOutputInstructions();
		this.textual = trace.getTextualOutputInvokingInstructions();
		
		int total = graphical.size() + textual.size();
		int remaining = total;
		for(Instruction primitive : graphical) {
			
			markDataDependenciesOf(primitive);

			// If this is graphical output, we only mark it as invoking if it actually affects the screen.
			// Other things, like setColor(), setFont() only affect what's drawn.
			if(GraphicalOutputParser.invokesOutput(primitive))
				markInvokersAsInvokingOutput(primitive);
			
		}

		for(Instruction primitive : textual) {

			markDataDependenciesOf(primitive);
			markInvokersAsInvokingOutput(primitive);
			
		}
		
	}
	
	private QualifiedClassName getTypeOfThis(Instruction i) {
		
		return 
			i.getMethod().isStatic() ? null :
			i.getClassfile().getInternalName();
		
	}

	// Mark the method that the given instruction occurs, and all of its potential callers,
	// as "invoking" output.
	private void markInvokersAsInvokingOutput(Instruction invoker) {
		
		// If we've already checked this one, we don't need to again.
		if(instructionsCheckedForInvokingOutput.contains(invoker)) return;
		instructionsCheckedForInvokingOutput.add(invoker);

		MethodInfo method = invoker.getMethod();
		if(method != null) {
			
			methodsPotentiallyInvokingOutput.add(method);

			for(Invoke invoke : method.getPotentialCallers())
				markInvokersAsInvokingOutput(invoker);
			
		}
		
	}
	
	/**
	 *  Given an instruction that represents some sort of output, marks methods and fields in the program that are a affect it
	 * (in a static slice sorta way, via control and data dependenciesIf we reach an instruction, method,
	 * or field that we've already checked, we don't need to recheck it. Must do this iteratively to avoid stack overflows.
	 */
	private void markDataDependenciesOf(Instruction primitive) {
		
		Set<Instruction> visiting = new LinkedHashSet<Instruction>();
		Set<Instruction> toVisit = new LinkedHashSet<Instruction>();
		
		toVisit.add(primitive);
		
		while(toVisit.size() > 0) {

			listener.notice("Finding output affecting code (" + Util.commas(toVisit.size()) + " remaining)...");

			// How much of the whole set of classes have we visited?
			listener.progress(((double)dataDependenciesVisited.size()) / trace.getNumberOfInstructions() / 2);
			
			// Put all of the instructions to visit in the visiting set.
			Set<Instruction> temp = visiting;
			visiting = toVisit;
			
			// Clear and use the old visiting set to gather new instructions to visit. 
			toVisit = temp;
			toVisit.clear();
			
			for(Instruction inst : visiting) {
				
				if(inst != null && !dataDependenciesVisited.contains(inst)) {
					
					dataDependenciesVisited.add(inst);
					
					// If it depends on a field, then check all of the fields definitions
					if(inst instanceof GETFIELD) {
						
						FieldInfo field = trace.resolveFieldReference(((GETFIELD)inst).getFieldref());
						if(field != null) {
							fieldsPotentiallyAffectingOutput.add(field);
							for(Definition definition : field.getDefinitions())
								toVisit.add(definition);
						}
						
					}
					// If it depends on a method call, check the return statements
					else if(inst instanceof Invoke) {

						// For each method this invocation might refer to, go through each of its returns and mark a dependency on it.
						for(MethodInfo method : trace.getMethodsFromReference((Invoke)inst)) {
							methodsPotentiallyAffectingOutput.add(method);
							for(AbstractReturn ret : method.getReturns())
								toVisit.add(ret);
						}
						
					}
					// If this uses a local variable, where can its values come from?
					else if(inst instanceof GetLocal) {
						
						GetLocal get = (GetLocal)inst;

						// Visit potential definitions of the local within the method.
						for(SetLocal set : get.getCode().getLocalDependencies().getPotentialDefinitionsOfGetLocal(get))
							toVisit.add(set);

						// Visit all potential definitions of the method argument. 
						if(get.getsMethodArgument()) {
							int argumentNumber = get.getMethod().getArgumentNumberOfLocalID(get.getLocalID());
							for(Invoke potentialCaller : get.getMethod().getPotentialCallers()) {
								StackDependencies.Producers producers = potentialCaller.getProducersOfArgument(argumentNumber);
								for(int i = 0; i < producers.getNumberOfProducers(); i++)
									toVisit.add(producers.getProducer(i));
							}
						}

					}

					// Visit control dependencies
					for(Instruction branch : inst.getBranchDependencies()) 
						toVisit.add(branch);

					// Visit arguments used by this instruction.
					for(int arg = 0; arg < inst.getNumberOfArgumentProducers(); arg++) {
						StackDependencies.Producers producers = inst.getProducersOfArgument(arg);
						for(int i = 0; i < producers.getNumberOfProducers(); i++)
							toVisit.add(producers.getProducer(i));
					}
					
				}
				
			}
			
		}
			
	}
	
	public Set<FieldInfo> getFieldsAffectingOutput() { return fieldsPotentiallyAffectingOutput; }
	public Set<MethodInfo> getMethodsAffectingOutput() { return methodsPotentiallyAffectingOutput; }
	public Set<MethodInfo> getMethodsInvokingOutput() { return methodsPotentiallyInvokingOutput; }

}

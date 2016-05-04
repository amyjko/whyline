package edu.cmu.hcii.whyline.analysis;

import java.util.*;

import edu.cmu.hcii.whyline.bytecode.*;
import edu.cmu.hcii.whyline.trace.Trace;

/**
 * @author Andrew J. Ko
 *
 */
public class BlackBoxDependencies {

	private final Set<Use> dependencies;
	private final Set<Instruction> visited;
	private final Invoke invoke;
	private final Trace trace;

	private BlackBoxDependencies(Trace trace, Invoke invoke) {
		
		this.trace = trace;
		this.invoke = invoke;
		this.dependencies = new HashSet<Use>();
		this.visited = new HashSet<Instruction>();
		
		analyze();
		
	}
	
	/**
	 * Given an invocation of some method, returns a set of instructions that represent
	 * argument values that affect the invocations return value. If there are no known 
	 * methods that this invocation could resolve to, returns an empty set.
	 */
	public static Set<Use> get(Trace trace, Invoke invoke) { return (new BlackBoxDependencies(trace, invoke)).dependencies; }

	private void analyze() {
	
		// Do we have code for any of the methods that this invocation might resolve to?
		MethodInfo[] methods = trace.getMethodsFromReference(invoke);
		
		for(MethodInfo method : methods)
			process(method);
		
	}

	private void process(MethodInfo method) {
		
		for(AbstractReturn ret : method.getReturns())
			process(ret);
		
	}

	private void process(Instruction producer) {

		if(visited.contains(producer)) return;
		visited.add(producer);
		
		if(producer instanceof GETSTATIC) {

			dependencies.add((GETSTATIC)producer);
			
		}
		else if(producer instanceof GetLocal) {
			
			int localID = ((GetLocal)producer).getLocalID();
			int arg = producer.getMethod().getArgumentNumberOfLocalID(localID);
			// Base case.
			if(arg < producer.getMethod().getNumberOfArguments())
				dependencies.add((GetLocal)producer);
			else
				for(SetLocal set : producer.getCode().getLocalDependencies().getPotentialDefinitionsOfLocalIDBefore(producer, localID))
					process(set);
			
		}
		else if(producer instanceof GETFIELD) {

			FieldInfo field = producer.getClassfile().getFieldByName(((GETFIELD)producer).getFieldref().getName());
			for(Definition def : field.getDefinitions())
				process(def);
			
		}
		// Just iterate through potential dependencies.
		else {

			for(int arg = 0; arg < producer.getNumberOfArgumentProducers(); arg++) {
				StackDependencies.Producers producers = producer.getProducersOfArgument(arg);
				for(Instruction p : producers.getProducers())
					process(p);
			}

		}
		
	}
	
}

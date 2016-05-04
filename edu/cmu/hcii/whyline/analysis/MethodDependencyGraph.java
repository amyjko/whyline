package edu.cmu.hcii.whyline.analysis;

import java.util.*;

import edu.cmu.hcii.whyline.bytecode.*;
import edu.cmu.hcii.whyline.trace.Trace;
import gnu.trove.TObjectIntHashMap;

/**
 * @author Andrew J. Ko
 *
 */
public class MethodDependencyGraph {

	private final Trace trace;
	
	private Map<MethodInfo,Set<MethodInfo>> methodToMethods = new HashMap<MethodInfo,Set<MethodInfo>>();
	
	public MethodDependencyGraph(Trace trace) {

		this.trace = trace;
		
		List<Classfile> classes = trace.getClassfiles();
		
		for(Classfile c : classes)
			for(MethodInfo m : c.getDeclaredMethods())
				analyze(m);
				
	}

	/**
	 * Returns a map of all known methods in trace and their distances from the given target
	 * in the method dependence graph. If the method is not in the map, the distance is undefined
	 * (there is no path).
	 */
	public TObjectIntHashMap<String> getMethodDistancesToMethod(MethodInfo target) {
		
		if(target == null) {
			System.out.println("Received null method target.");
			return null;
		}
		
		TObjectIntHashMap<String> distancesByMethod = new TObjectIntHashMap<String>(); 
		System.out.println("Analyzing distance from " + methodToMethods.size() + " methods to " + target.getQualifiedNameAndDescriptor());

		Set<MethodInfo> visited = new HashSet<MethodInfo>();
		LinkedList<MethodInfo> queue = new LinkedList<MethodInfo>();
		distancesByMethod.put(target.getQualifiedNameAndDescriptor(), 0);
		visited.add(target);
		queue.offer(target);
		while(!queue.isEmpty()) {
			
			MethodInfo method = queue.poll();
			assert distancesByMethod.containsKey(method.getQualifiedNameAndDescriptor());
			int distance = distancesByMethod.get(method.getQualifiedNameAndDescriptor()) + 1;
			for(MethodInfo relatedMethod : methodToMethods.get(method)) {

				// Visit this method.
				if(!visited.contains(relatedMethod)) {
					visited.add(relatedMethod);
					distancesByMethod.put(relatedMethod.getQualifiedNameAndDescriptor(), distance);
					queue.offer(relatedMethod);
				}
				
			}
			
			
		}
		
		return distancesByMethod;
		
	}
	
	private void associate(MethodInfo one, MethodInfo two) {

		Set<MethodInfo> methods = methodToMethods.get(one);
		if(methods == null) {
			methods = new HashSet<MethodInfo>();
			methodToMethods.put(one, methods);
		}
		methods.add(two);

	}
	
	private void analyze(MethodInfo m) {
		
		CodeAttribute code = m.getCode();
		
		if(code == null) return;
		
		// Map methods to methods called and methods called to methods.
		// Map field references to methods of field assignments
		for(Instruction inst : code.getInstructions()) {
			if(inst instanceof Invoke) {
				for(MethodInfo methodCalled : trace.getMethodsFromReference((Invoke)inst)) {
					if(m != methodCalled) {
						associate(m, methodCalled);
						associate(methodCalled, m);
					}
				}
			}
			else if(inst instanceof GETFIELD) {
				FieldInfo field = trace.resolveFieldReference(((GETFIELD)inst).getFieldref());
				if(field != null) {
					for(Definition def : field.getDefinitions()) {
						if(def.getMethod() != m) {
							associate(m, def.getMethod());
							associate(def.getMethod(), m);
						}
					}
				}
				// Otherwise, couldn't find the field in any class, probably because we don't have the class.
			}
		}
		
	}
	
}
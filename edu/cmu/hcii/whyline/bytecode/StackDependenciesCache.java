package edu.cmu.hcii.whyline.bytecode;

import edu.cmu.hcii.whyline.analysis.AnalysisException;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public interface StackDependenciesCache {

	public StackDependencies getStackDependenciesFor(MethodInfo method) throws AnalysisException;
	
}

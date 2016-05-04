package edu.cmu.hcii.whyline.analysis;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.cmu.hcii.whyline.bytecode.Classfile;
import edu.cmu.hcii.whyline.bytecode.FieldInfo;
import edu.cmu.hcii.whyline.bytecode.INVOKESTATIC;
import edu.cmu.hcii.whyline.bytecode.Instantiation;
import edu.cmu.hcii.whyline.bytecode.Invoke;
import edu.cmu.hcii.whyline.bytecode.MethodInfo;
import edu.cmu.hcii.whyline.source.JavaSourceFile;
import edu.cmu.hcii.whyline.source.Line;
import edu.cmu.hcii.whyline.source.Token;
import edu.cmu.hcii.whyline.source.TokenRange;
import edu.cmu.hcii.whyline.trace.Trace;
import edu.cmu.hcii.whyline.ui.WhylineUI;

/**
 * @author Andrew J. Ko
 *
 */
public class ClassUsesSearch implements SearchResultsInterface {

	private final WhylineUI whylineUI;
	private final Classfile classfile;
	
	public ClassUsesSearch(WhylineUI whylineUI, Classfile classfile) {
		
		this.whylineUI = whylineUI;
		this.classfile = classfile;
		
	}

	public String getCurrentStatus() { return "Done."; }

	public SortedSet<Token> getResults() {
		
		SortedSet<Token> tokens = new TreeSet<Token>();
		Trace trace = whylineUI.getTrace();
		
		// Find all instantiations of the class
		List<Instantiation> instantiations = trace.getInstantiationsOf(classfile.getInternalName());
		for(Instantiation inst : instantiations) {
			tokens.addAll(inst.getLine().getTokens());
		}
		
		// Find all method references
		for(Invoke invoke : trace.getInvocations()) {
			if(invoke instanceof INVOKESTATIC) {
				if(invoke.getMethodInvoked().getClassName() == classfile.getInternalName()) {
					Line line = invoke.getLine();
					if(line != null)
						tokens.addAll(line.getTokens());
				}
			}
		}
		
		//  Find all return values referencing the class
		for(Classfile c : trace.getClasses()) {

			JavaSourceFile source = c.getSourceFile();
			if(source != null) {
				for(MethodInfo m : c.getDeclaredMethods()) {
					if(m.getParsedDescriptor().getReturnType() == classfile.getInternalName()) {
						TokenRange returnTokens = source.getTokenRangeForReturnType(m);
						if(returnTokens != null)
							tokens.add(returnTokens.first);
					}
				}
				for(FieldInfo f : c.getDeclaredFields()) {
					if(f.getTypeName() == classfile.getInternalName()) {
						TokenRange fieldTypeRange = source.getTokenRangeForField(f);
						if(fieldTypeRange != null)
							tokens.add(fieldTypeRange.first);
					}
				}
			}
		}
				
		return tokens;
		
	}

	public String getResultsDescription() { return  "uses of " + classfile.getSimpleName(); }

	public boolean isDone() { return true; }

}

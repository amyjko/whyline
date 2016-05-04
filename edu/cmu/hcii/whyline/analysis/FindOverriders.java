package edu.cmu.hcii.whyline.analysis;

import java.util.SortedSet;
import java.util.TreeSet;

import edu.cmu.hcii.whyline.bytecode.MethodInfo;
import edu.cmu.hcii.whyline.source.JavaSourceFile;
import edu.cmu.hcii.whyline.source.Line;
import edu.cmu.hcii.whyline.source.Token;
import edu.cmu.hcii.whyline.ui.WhylineUI;

/**
 * @author Andrew J. Ko
 *
 */
public class FindOverriders implements SearchResultsInterface {

	private final MethodInfo method;
	private final WhylineUI whylineUI;
	
	private final SortedSet<Token> overriders = new TreeSet<Token>();

	public FindOverriders(WhylineUI whylineUI, MethodInfo method) {
		
		this.whylineUI = whylineUI;
		this.method = method;
	
		for(MethodInfo m : method.getOverriders()) {
			
			JavaSourceFile source = m.getClassfile().getSourceFile();
			if(source != null) {
			
				Line line = source.getTokenForMethodName(m).getLine();
				overriders.addAll(line.getTokensAfterFirstNonWhitespaceToken());
				
			}
			
		}
	
	}
	
	public String getResultsDescription() { return "overriders of " + method.getInternalName(); }
	
	public String getCurrentStatus() { return "Done."; }
	
	public SortedSet<Token> getResults() { return overriders; }
	
	public boolean isDone() { return true; }

}

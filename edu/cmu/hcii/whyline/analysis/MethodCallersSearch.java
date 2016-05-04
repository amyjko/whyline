package edu.cmu.hcii.whyline.analysis;

import java.util.SortedSet;
import java.util.TreeSet;

import edu.cmu.hcii.whyline.bytecode.Invoke;
import edu.cmu.hcii.whyline.bytecode.MethodInfo;
import edu.cmu.hcii.whyline.source.Token;
import edu.cmu.hcii.whyline.ui.WhylineUI;

/**
 * @author Andrew J. Ko
 *
 */
public class MethodCallersSearch implements SearchResultsInterface {

	private final WhylineUI whylineUI;
	private final MethodInfo method;
	
	public MethodCallersSearch(WhylineUI whylineUI, MethodInfo method) {
		
		this.whylineUI = whylineUI;
		this.method = method;
		
	}

	public String getCurrentStatus() { return "Done."; }

	public SortedSet<Token> getResults() {
		
		SortedSet<Token> lines = new TreeSet<Token>();
		for(Invoke caller : method.getPotentialCallers())
			lines.addAll(caller.getLine().getTokensAfterFirstNonWhitespaceToken());

		return lines;
		
	}

	public String getResultsDescription() { return  "callers of " + method.getJavaName() + "()"; }

	public boolean isDone() { return true; }

}

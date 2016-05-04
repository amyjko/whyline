package edu.cmu.hcii.whyline.analysis;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.cmu.hcii.whyline.bytecode.*;
import edu.cmu.hcii.whyline.source.Token;
import edu.cmu.hcii.whyline.ui.WhylineUI;

/**
 * @author Andrew J. Ko
 *
 */
public class FieldUsesSearch implements SearchResultsInterface {

	private final WhylineUI whylineUI;
	private final FieldInfo field;
	
	public FieldUsesSearch(WhylineUI whylineUI, FieldInfo field) {
		
		this.whylineUI = whylineUI;
		this.field = field;
		
	}

	public String getCurrentStatus() { return "Done."; }

	public SortedSet<Token> getResults() {

		SortedSet<Token> lines = new TreeSet<Token>();

		List<Use> uses = field.getUses();
		for(Use use : uses) 
			if(use.getLine() != null)
				lines.addAll(use.getLine().getTokensAfterFirstNonWhitespaceToken());

		List<Definition> defs = field.getDefinitions();
		for(Definition def : defs) lines.addAll(def.getLine().getTokensAfterFirstNonWhitespaceToken());

		return lines;
		
	}

	public String getResultsDescription() { return  "uses of " + field.getDisplayName(true, -1); }

	public boolean isDone() { return true; }

}

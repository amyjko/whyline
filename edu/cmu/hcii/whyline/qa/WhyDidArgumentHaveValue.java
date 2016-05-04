package edu.cmu.hcii.whyline.qa;

import edu.cmu.hcii.whyline.analysis.AnalysisException;
import edu.cmu.hcii.whyline.trace.Value;
import edu.cmu.hcii.whyline.util.Named;

public class WhyDidArgumentHaveValue extends WhyDidQuestion<Named> {

	private final Value value;
	
	public WhyDidArgumentHaveValue(Asker asker, Value value, String argumentName, String descriptionOfEvent) {

		super(asker, new Argument(argumentName), descriptionOfEvent);

		this.value = value;
		
	}

	protected Answer answer() throws AnalysisException {

		return value.getAnswer(this);

	}

	public String getQuestionExplanation() {
		
		return "explain why <b>" + getDescriptionOfSubject() + "</b> " + getDescriptionOfEvent();
		
	}

	private static class Argument implements Named {
		private final String name;
		public Argument(String name) { this.name = name; }
		public String getDisplayName(boolean html, int lengthLimit) { return name; }
	}
	
}

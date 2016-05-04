package edu.cmu.hcii.whyline.qa;

import edu.cmu.hcii.whyline.analysis.AnalysisException;
import edu.cmu.hcii.whyline.trace.Value;

public class WhyDidntArgumentChange extends WhyDidntQuestion<Value> {

	public WhyDidntArgumentChange(Asker asker, Value subject, String event) {
	
		super(asker, subject, event);
		
	}
	
	protected Answer answer() throws AnalysisException {
		
//		Map<String, Set<ValueSource>> sources = ValueSourceAnalyzer.getSourcesByValue(trace, paint.getInstruction(), 1, this);
//		if(wasCanceled()) return null;
//		QuestionMenu values = new QuestionMenu("Questions about this %", "%", paint);
//		for(String value : getOrderedValues(sources.keySet()))
//			values.addQuestion(new WhyDidntArgumentHaveValue(asker, paint.getEventID(), 1, entityIDs, sources.get(value), "color" + "= <b>" + (value.equals("") ? "something else" : value) + "</b>"));
//		return values;

		return new MessageAnswer(this, "Haven't implemented this answering algorithm yet.");
		
	}

	public String getQuestionExplanation() {
		return "explain why this argument didn't change after " + scope.getDescription();
	}

}

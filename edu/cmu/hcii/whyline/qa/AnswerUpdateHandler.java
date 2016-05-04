package edu.cmu.hcii.whyline.qa;

import edu.cmu.hcii.whyline.analysis.AnalysisException;

public abstract class AnswerUpdateHandler {
	
	private final int millisecondsBetweenUpdates;
	
	public AnswerUpdateHandler(int millisecondsBetweenUpdates) {
		
		this.millisecondsBetweenUpdates = millisecondsBetweenUpdates;
		
	}

	public long getMillisecondsBetweenUpdates() { return millisecondsBetweenUpdates; }
	
	public abstract void update(float percentComplete, String update);
	public abstract void error(AnalysisException e);
	public abstract void done(Answer answer);
	
}

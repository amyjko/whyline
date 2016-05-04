package edu.cmu.hcii.whyline.qa;

import edu.cmu.hcii.whyline.analysis.AnalysisException;
import edu.cmu.hcii.whyline.trace.Trace;

public interface Asker {

	public void answer(Question<?> question);
	public Trace getTrace();
	public void doneAnswering();
	public void updateAnsweringStatus(Question<?> question, String status, double percentComplete);
	public void problemAnswering(Question<?> question, AnalysisException e);
	public Scope getCurrentScope();
	public void processing(boolean b);
	
}
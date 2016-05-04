package edu.cmu.hcii.whyline.qa;

import edu.cmu.hcii.whyline.analysis.Cancelable;
import edu.cmu.hcii.whyline.trace.Trace;
import edu.cmu.hcii.whyline.util.Named;

public abstract class QuestionMenuMaker implements Comparable<QuestionMenuMaker>, Cancelable {

	private final Asker asker;
	private final Trace trace;
	protected boolean canceled = false;

	public QuestionMenuMaker(Asker asker) { 

		this.asker = asker;
		this.trace = asker.getTrace();
		
	}
	
	public Trace getTrace() { return trace; }
	
	public abstract QuestionMenu make();
	public abstract String getMenuLabel();
	public abstract Named getSubject();
	
	public int compareTo(QuestionMenuMaker other) { return getMenuLabel().compareTo(other.getMenuLabel()); }
	
	public boolean wasCanceled() { return canceled;  }

	public void uncancel() { canceled = false; }
	public void cancel() {  canceled = true; }
	
}

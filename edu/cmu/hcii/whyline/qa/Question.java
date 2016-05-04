package edu.cmu.hcii.whyline.qa;

import java.util.HashMap;
import java.util.Map;

import edu.cmu.hcii.whyline.analysis.AnalysisException;
import edu.cmu.hcii.whyline.bytecode.*;
import edu.cmu.hcii.whyline.io.IOEvent;
import edu.cmu.hcii.whyline.trace.Trace;

import edu.cmu.hcii.whyline.util.Named;
import edu.cmu.hcii.whyline.util.Util;

public abstract class Question<T extends Named> implements Comparable<Question<?>> {

	protected final Asker asker;
	protected final Trace trace;
	protected final T subject;
	private final String descriptionOfEvent;
	protected final Scope scope;
	private boolean isAnswered = false;

	private Answer answer;
	
	// A table of unexecuted instructions, to help us ensure that there's only one per instruction in an answer.
	private final Map<Instruction, UnexecutedInstruction> unexecutedInstructions = new HashMap<Instruction, UnexecutedInstruction>();

	public Question(Asker asker, T subject, String descriptionOfEvent) {

		super();

		this.asker = asker;
		this.trace = asker.getTrace();
		this.subject = subject;
		this.scope = asker.getCurrentScope();
		this.descriptionOfEvent = descriptionOfEvent;
				
	}

	public final String getDescriptionOfSubject() { return getDescriptionOfSubject(-1); }

	public final String getDescriptionOfSubject(int limit) { return subject.getDisplayName(false, limit); }

	public final String getDescriptionOfEvent() { return getDescriptionOfEvent(-1); }

	public final String getDescriptionOfEvent(int limit) { 

		// THIS SHOULDN'T TRUNCATE THE HTML! It won't format properly.
		String truncatedEvent = Util.elide(descriptionOfEvent, limit);
		
		return truncatedEvent;
		
	}

	public final Trace getTrace() { return asker.getTrace(); }

	public final int getInputEventID() { return scope.getInputEventID(); }

	public final int getOutputEventID() { return scope.getOutputEventID(); }

	public final IOEvent getInputEvent() { return trace.getIOHistory().getEventAtTime(getInputEventID()); }
	
	public final T getSubject() { return subject; }
	
	public final void computeAnswer() {
		
		if(isAnswered) return;
		isAnswered = true;
		try {
			answer = answer();
			asker.doneAnswering();
		} catch (AnalysisException e) {
			asker.problemAnswering(this, e);
		}
		
	}
	
	/**
	 * Only returns the answer. Null if the naswer hasn't been computed yet.
	 */
	public Answer getAnswer() { return answer; }

	protected abstract Answer answer() throws AnalysisException;
	
	public UnexecutedInstruction getUnexecutedInstruction(Instruction inst, ExpectedObject object) {
		
		UnexecutedInstruction ine = unexecutedInstructions.get(inst);
		if(ine == null) {
			
			ine = new UnexecutedInstruction(this, inst, object);
			unexecutedInstructions.put(inst, ine);
			
		}
		
		return ine;
		
	}
	
	public Asker getAsker() { return asker; }
	
	public int compareTo(Question<?> question) { return getDescriptionOfEvent(-1).compareTo(question.getDescriptionOfEvent(-1)); }

	public abstract String getQuestionExplanation();
	
	public abstract boolean isPhrasedNegatively(); 
	
	public final String toString() { return getQuestionText(); }
	
	public final String getQuestionText() {
		
		String subject = "<b>" + this.subject.getDisplayName(true, -1) + "</b>";
		String event = "<b>" + getDescriptionOfEvent() + "</b>";
		
		if(isPhrasedNegatively())
			return "why didn't " + subject + " " + event +"?";
		else
			return "why did " + subject + " " + event + "?";
		
	}
	
	protected static class EventID implements Named {
		
		private final int eventID; 
		
		public EventID(int eventID) { this.eventID = eventID; }
		
		public String getDisplayName(boolean html, int limit) {
			
			return "";
			
		}

		public int getEventID() { return eventID; }
		
	}
	
}

package edu.cmu.hcii.whyline.qa;

import edu.cmu.hcii.whyline.io.IOEvent;
import edu.cmu.hcii.whyline.trace.EventKind;
import edu.cmu.hcii.whyline.trace.Trace;

/**
 * @author Andrew J. Ko
 *
 */
public final class WhyDidntInputAffectOutput extends WhyDidntQuestion<Scope> {
	
	private final IOEvent io;
	
	public WhyDidntInputAffectOutput(Asker asker) {

		super(asker, asker.getCurrentScope(), "<b>anything happen</b>");

		this.io = subject.getInputEvent();
		
	}
	
	protected Answer answer() {

		asker.processing(true);
		
		Answer answer = new MessageAnswer(this, "Haven't implemeted WhyDidntInputAffectOutput yet");

		// Where's the deepest call stack after this input event, before the end of the method this input event occurred in?

		int startID = trace.getStartID(subject.getInputEventID());
		int returnID = startID < 0 ? -1 : trace.getStartIDsReturnOrCatchID(startID);
		
		Trace.ThreadIterator events = trace.getThreadIteratorAt(subject.getInputEventID());
		int deepestStartID = -1;
		int deepestDepth = -1;
		int depth = 0;
		while(events.hasNextInThread()) {
			
			int eventID = events.nextInThread();
			if(eventID == returnID)
				break;
			if(trace.getKind(eventID) == EventKind.START_METHOD) {
				depth++;
				if(deepestStartID < 0  || depth > deepestDepth) {
					deepestDepth = depth;
					deepestStartID = eventID;
				}
			}
			else if(!events.hasNextInMethod())
				depth--;
		}
		
		answer.getExplanationFor(subject.getInputEventID());
		
		asker.processing(false);
		
		return answer;
		
	}
	
	public String getQuestionExplanation() {
		
		return "explain why <b>" + getDescriptionOfSubject() + "</b> didn't execute";
		
	}

}
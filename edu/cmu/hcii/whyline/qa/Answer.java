package edu.cmu.hcii.whyline.qa;

import java.util.*;

import edu.cmu.hcii.whyline.bytecode.Instruction;
import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public abstract class Answer implements Comparable<Answer> {

	private static int answerID = 0;
	
	private final int id  = answerID++;
	
	protected final Trace trace;
	protected final Question<?>  question;

	private final UnexecutedInstruction[] instructionNotExecuted;

	private final gnu.trove.TIntObjectHashMap<Explanation> eventExplanations = new gnu.trove.TIntObjectHashMap<Explanation>();
	private final gnu.trove.TIntObjectHashMap<ExplanationBlock> blocksRepresentingEvents = new gnu.trove.TIntObjectHashMap<ExplanationBlock>();
	private final SortedMap<Integer,ThreadBlock> threadBlocks = new TreeMap<Integer,ThreadBlock>();
	private final gnu.trove.TIntObjectHashMap<LoopBlock> loopBlocksByFirstBranch = new gnu.trove.TIntObjectHashMap<LoopBlock>();
	
	private final ArrayList<AnswerChangeListener> listeners = new ArrayList<AnswerChangeListener>();
	
	private int latestEventID = -1;
	
	private String explanation;
		
	public Answer(Question<?> question) {
		
		this(question, null);
		
	}
	
	public Answer(Question<?> question, UnexecutedInstruction[] unexecuted) {

		this.question = question;
		this.trace = question.getTrace();
		this.instructionNotExecuted = unexecuted == null ? new UnexecutedInstruction[0] : unexecuted;
		
	}

	public Trace getTrace() { return trace; }
	
	public UnexecutedInstruction[] getUnexecutedInstructions() { return instructionNotExecuted; }

	public boolean hasVisualizationContent() { return latestEventID >= 0 || instructionNotExecuted != null; }  
	
	public abstract String getAnswerText();

	public abstract String getKind();

	protected abstract int getPriority();

	public final void addChangeListener(AnswerChangeListener listener) { listeners.add(listener); }
	
	public Collection<ThreadBlock> getThreadBlocks() { return Collections.<ThreadBlock>unmodifiableCollection(threadBlocks.values()); }

	private ThreadBlock getThreadBlockFor(int threadID) {
		
		ThreadBlock threadBlock = threadBlocks.get(threadID);
		if(threadBlock == null) {
			
			threadBlock = new ThreadBlock(this, threadID);
			threadBlocks.put(threadID, threadBlock);

			for(AnswerChangeListener listener : listeners)
				listener.threadBlockAdded(threadBlock);
			
		}
		return threadBlock;
		
	}
	
	public final boolean hasExplanationFor(int eventID) {

		return eventExplanations.containsKey(eventID);
		
	}
		
	public final Explanation getExplanationFor(int eventID) {

		if(eventID < 0) return null;
		
		Explanation explanation = eventExplanations.get(eventID);

		if(explanation == null) {

			EventKind kind = trace.getKind(eventID);
			
			if(kind.isInvocation) explanation = new InvocationBlock(this, eventID);
			else if(kind.isBranch) explanation = new BranchBlock(this, eventID);
			else if(kind == EventKind.EXCEPTION_THROWN) explanation = new ExceptionBlock(this, eventID);
			else if(kind == EventKind.START_METHOD) explanation = new StartMethodBlock(this, eventID);
			else explanation = new Explanation(this, eventID);
			
			eventExplanations.put(eventID, explanation);

			if(latestEventID < eventID) 
				latestEventID = eventID;
			
		}
		
		return explanation;
		
	}

	public int getLatestEventID() { return latestEventID; }
	
	/**
	 * Given some event, returns the block that represents its control dependency.
	 * 
	 * @param explanation
	 * @return Returns the event block (representing an invocation or branch) that caused this event to occur.
	 */
	public ExplanationBlock getBlockRepresentingControlDependencyOf(Explanation explanation) {
		
		if(explanation.getEventID() < 0) 
			return null;

		if(explanation instanceof ThreadBlock)
			return null;
		
		int threadID = trace.getThreadID(explanation.getEventID());
		int controlID = trace.getControlID(explanation.getEventID());
		
		// If this event has no control dependency, then we add it to the appropriate thread block.
		if(controlID < 0) {

			ThreadBlock block = getThreadBlockFor(threadID); 
			assert block.getThreadID() == threadID : "threadID = " + threadID + " but found threadID = " + block.getThreadID();
			return block;
		
		}
		else {

			Instruction inst = trace.getInstruction(explanation.getEventID());
			Instruction controlInst = trace.getInstruction(controlID);
			
			// If the control dependency occurred in the same method, but its instruction index was greater than
			// the explanation's event's instruction index, then it was part of a loop.
			if(	trace.getKind(controlID).isBranch && 
				controlInst.getMethod() == inst.getMethod() &&
				controlInst.getIndex() > inst.getIndex()) {
				
				int firstBranchEventID = trace.getBranchFirstExecutionInMethod(controlID);
				
				LoopBlock loopBlock = loopBlocksByFirstBranch.get(firstBranchEventID);
				if(loopBlock == null) {
					loopBlock = new LoopBlock(this, firstBranchEventID);
					loopBlocksByFirstBranch.put(firstBranchEventID, loopBlock);
				}
				return loopBlock;
				
			}
			else {
				
				ExplanationBlock block = blocksRepresentingEvents.get(controlID);
				
				// If we didn't find one, create one.
				if(block == null) {
					
					Explanation controlExplanation = getExplanationFor(controlID);

					assert controlExplanation instanceof ExplanationBlock : "Why was the control dependency explanation of \n\n\t" + explanation + "\n\nequal to \n\n\t" + controlExplanation + "\n\n";
					
					block = (ExplanationBlock)controlExplanation;
					blocksRepresentingEvents.put(controlID, block);
					
				}

				// If the block we found is in a different thread than the event that needs
				// a control dependency block, then we just return the thread for the requesting event. This happens mainly
				// as a result of one thread causing control events in another (e.g., Thread.start() calls cause
				// Thread.run() to be invoked.
				if(trace.getThreadID(controlID) != threadID) {
				
					return getThreadBlockFor(threadID);			
					
				}
				else return block;
				
			}
			
		}
		
	}
	
	public Question<?> getQuestion() { return question; }

	private HashSet<ExplanationBlock> blocksThatChanged = new HashSet<ExplanationBlock>();
	
	public void eventBlockChanged(ExplanationBlock block) {

		blocksThatChanged.add(block);
		
	}	
	
	/**
	 * Tells any registered listeners about blocks whose elements have changed because of events being explained.
	 */
	public void broadcastChanges() {
		
		if(blocksThatChanged.isEmpty()) return;
		
		for(AnswerChangeListener listener : listeners)
			listener.eventBlocksChanged(Collections.<ExplanationBlock>unmodifiableSet(new HashSet<ExplanationBlock>(blocksThatChanged)));
		
		blocksThatChanged.clear();
		
	}

	/**
	 * 
	 * Given an event explanation, skips over intermediate computation that caused it, finding the root causes.
	 * 
	 * Specifically, given an explanation of an event, returns a map in which the key represents "terminal" dependencies of the given explanation.
	 * These are generally events with no causes, such as constants, or with multiple causes, and thus the user must express interest 
	 * in one before jumping further back in the execution history.
	 * 
	 * The values in the map represent the most immediate "effect" of the "cause" keys; in fact, the causes are derived from the effect's causes.
	 *  For example, if a cause was that a local variable had a particular value, the effect was that it was used somewhere.
	 *  
	 */
	public final SortedMap<Explanation, Explanation> getTerminalDataDependencies(Explanation explanation) {
		
		getQuestion().getAsker().processing(true);
		
		SortedMap<Explanation,Explanation> dependencies = new TreeMap<Explanation,Explanation>();
		determineTerminalDataDependencies(explanation, dependencies);

		getQuestion().getAsker().processing(false);
		
		return dependencies;
		
	}

	public final Explanation getSourceOfExplanationsValue(Explanation desiredEvent) {
		
		// Find the source, get the corresponding upstream dependencies, notify of changes.
		int sourceOfValueID = trace.getSourceOfValueID(desiredEvent.getEventID());
		getTerminalDataDependencies(desiredEvent);
		Explanation source = getExplanationFor(sourceOfValueID);
		broadcastChanges();
		return source;
		
	}
	
	/**
	 * Returns false if any of the dependencies searched were not available yet.
	 */
	private  boolean determineTerminalDataDependencies(Explanation effect, SortedMap<Explanation,Explanation> dependencies) {
		
		Explanation[] causes = effect.getCauses();
		if(causes == null)
			return false;
		else if(causes.length == 0)
			return false;

		for(Explanation cause : causes) {

			if(cause != null) {

				assert effect != cause : "How can " + trace.eventToString(effect.getEventID()) + " explain itself?";

				// First, explain the cause.
				cause.explain();
				
				//  If this cause is a value produced, does the value produced depend on anything upstream? If not, add this as a cause.
				// This is here because originally we excluded all values produced.
				if(trace.getKind(cause.getEventID()).isValueProduced) {
					
					boolean hasCauses = determineTerminalDataDependencies(cause, dependencies);
					if(!hasCauses)
						dependencies.put(cause, effect);
					
				}
				else if(cause.isTerminalDataDependency()) {
					dependencies.put(cause, effect);
				}
				
			}
			
		}
		return true;
		
	}

	public int compareTo(Answer otherAnswer) {

		int priorityDelta = getPriority() - otherAnswer.getPriority(); 
		if(priorityDelta == 0)
			return id - otherAnswer.id;
		else 
			return priorityDelta;
		
	}
	
	public String toString() {
		
		return getAnswerText();
		
	}

}
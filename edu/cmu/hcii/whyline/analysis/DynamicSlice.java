package edu.cmu.hcii.whyline.analysis;

import java.util.*;


import edu.cmu.hcii.whyline.bytecode.Instruction;
import edu.cmu.hcii.whyline.source.Line;
import edu.cmu.hcii.whyline.source.Token;
import edu.cmu.hcii.whyline.trace.*;
import edu.cmu.hcii.whyline.ui.*;

/**
 * @author Andrew J. Ko
 *
 */
public final class DynamicSlice implements SearchResultsInterface {

	private final Trace trace;
	private final WhylineUI whylineUI;
		
	private final Line criterion;
	private final boolean onlyMostRecent;
	
	private final Slicer slicer;
	
	private final int earliestEventIDSliced;
	private final gnu.trove.TIntHashSet eventsSliced;
	
	public DynamicSlice(WhylineUI whylineUI, Line line, boolean onlyMostRecent) {
		
		this.whylineUI = whylineUI;
		this.trace = whylineUI.getTrace();
		this.criterion = line;
		this.onlyMostRecent = onlyMostRecent;

		
		// Find the execution(s) of these instructions.
		eventsSliced = new gnu.trove.TIntHashSet();
		int smallestID = Integer.MAX_VALUE;
		for(Instruction inst : criterion.getInstructions()) {

			int recentExecution = trace.getNumberOfEvents();
			
			do {
			
				recentExecution = whylineUI.getTrace().findExecutionOfInstructionPriorTo(inst, recentExecution);
				if(recentExecution >= 0) {
					eventsSliced.add(recentExecution);
					if(recentExecution < smallestID) smallestID = recentExecution;
				}
			
			} while(recentExecution >= 0);
		
		}

		earliestEventIDSliced = smallestID;
		
		slicer = new Slicer(eventsSliced);
		slicer.start();

	}
	
	public String getResultsDescription() { return "slice on " + criterion.getFile().getShortFileName() + ":" + criterion.getLineNumber().getNumber(); }

	public String getCurrentStatus() {
		
		if(slicer.done) 
			return slicer.slice.isEmpty() ? "Slice is empty." : "Done.";
		else 
			return "" + slicer.eventsToExplain.size() + " events left to explain...";
		
	}
	
	public SortedSet<Token> getResults() {
	
		return slicer.slice;
		
	}
	
	public boolean isDone() { return slicer.done; }

	public int getEventIDSliced() { return earliestEventIDSliced; }
	
	private class Slicer extends Thread {

		public final SortedSet<Token> slice = new TreeSet<Token>();
		public final gnu.trove.TIntHashSet eventsToExplain;
		public boolean done = false;
		
		private Slicer(gnu.trove.TIntHashSet eventsToExplain) {

			this.eventsToExplain = new gnu.trove.TIntHashSet(eventsToExplain.toArray());			
			
		}
		
		public void run() {
			
			gnu.trove.TIntHashSet newEvents = new gnu.trove.TIntHashSet();
			
			while(eventsToExplain.size() > 0) {

				synchronized(eventsToExplain) {
				
					for(int event : eventsToExplain.toArray()) {
	
						slice.addAll(trace.getInstruction(event).getLine().getTokensAfterFirstNonWhitespaceToken());
						
						List<Value> sd = trace.getOperandStackDependencies(event);
						int md = trace.getHeapDependency(event);
	
						if(md >= 0) newEvents.add(md);
						for(Value value : sd)
							if(value instanceof TraceValue)
								newEvents.add(((TraceValue)value).getEventID());
						
					}
	
					eventsToExplain.clear();
					eventsToExplain.addAll(newEvents.toArray());
					newEvents.clear();
					
				}
				
			}
			
			done = true;
						
		}
		
	}

}


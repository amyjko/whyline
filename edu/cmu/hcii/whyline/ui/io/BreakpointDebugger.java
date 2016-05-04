package edu.cmu.hcii.whyline.ui.io;

import java.util.*;

import edu.cmu.hcii.whyline.bytecode.*;
import edu.cmu.hcii.whyline.source.*;
import edu.cmu.hcii.whyline.trace.EventKind;
import edu.cmu.hcii.whyline.trace.Trace;
import edu.cmu.hcii.whyline.ui.UI;
import edu.cmu.hcii.whyline.ui.WhylineUI;
import edu.cmu.hcii.whyline.util.IntegerVector;

import gnu.trove.TIntObjectHashMap;

/**
 * An emulated breakpoint debugger, using the trace data.
 * 
 * @author Andrew J. Ko
 *
 */
public final class BreakpointDebugger {
	
	private final Trace trace;

	private final WhylineUI whylineUI;
	
	private int currentEventID = -1; 
	private Line currentLine;
	private boolean inPauseMode = false;

	private final Map<Line, Instruction> breakpoints = new HashMap<Line, Instruction>();
	private final Map<Token, Print> prints = new HashMap<Token, Print>();

	private final TIntObjectHashMap<Set<Print>> printsByEventID = new TIntObjectHashMap<Set<Print>>();	

	public final List<Output> printStatementOutput = new Vector<Output>(); 
	
	public BreakpointDebugger(WhylineUI whylineUI) {

		this.whylineUI = whylineUI;
		this.trace = whylineUI.getTrace();
		
	}
	
	public Line getCurrentLine() { return currentLine; }
	
	public int getCurrentEventID() { return currentEventID;  }

	public boolean isRunning() { return currentEventID >= 0; }

	public List<Output> getPrintStatementOutput() { return printStatementOutput; }
	
	public void setCurrentEventID(int newEventID) {

		// Any prints to print? Print 'em! If we're about to stop the thread, then print everything to the end of the trace.
		addPrintsBetween(currentEventID, currentEventID >= 0 && newEventID < 0 ? trace.getNumberOfEvents() - 1 : newEventID);
		
		this.currentEventID = newEventID;
		if(currentEventID < 0) {

			stop();
			
			javax.swing.JOptionPane.showMessageDialog(whylineUI, "The program is done executing.");
			
		}
		else {
			Instruction inst = trace.getInstruction(currentEventID); 
			currentLine = inst == null ? null : inst.getLine();
			
			whylineUI.getActions().runToBreakpoint.setEnabled(true);
			setBreakpointButtonsEnabled(true);
			whylineUI.arrangeForAsking();

		}
		BreakpointDebugger.this.whylineUI.setInputTime(currentEventID);
		BreakpointDebugger.this.whylineUI.selectEvent(currentEventID, true, UI.STEP_INTO_UI); 
		
	}
	
	public void stop() {

		currentEventID = -1;
		
		whylineUI.getActions().runToBreakpoint.setEnabled(true);
		setBreakpointButtonsEnabled(false);
		whylineUI.arrangeForAsking();
		BreakpointDebugger.this.whylineUI.setInputTime(whylineUI.getTrace().getNumberOfEvents() - 1);

		whylineUI.getObjectsUI().removeAllObjects();
		
		whylineUI.setBreakpointDebuggerState("stopped", false);
		
	}

	public void setBreakpointButtonsEnabled(boolean enabled) {
		
		whylineUI.getActions().stepOut.setEnabled(enabled);
		whylineUI.getActions().stepInto.setEnabled(enabled);
		whylineUI.getActions().stepOver.setEnabled(enabled);
		whylineUI.getActions().stop.setEnabled(enabled);

	}

	public void runToBreakpoint() {
		
		if(inPauseMode) {
			
			inPauseMode = false;
			setPauseMode(false);
			setCurrentEventID(whylineUI.getInputEventID());
			
		}
		else {
		
			whylineUI.setBreakpointDebuggerState("running", true);
	
			if(!isRunning()) {
				
				whylineUI.setInputTime(0);
				setCurrentEventID(0);
				printStatementOutput.clear();
				
			}
			
			whylineUI.arrangeForPlayback();
					
			setBreakpointButtonsEnabled(false);
			
			int nextEventID = -1;
			
			// Find the next eventID with a breakpoint set on its instruction.
			for(Instruction inst : breakpoints.values()) {

				int eventID = currentEventID;
				do {
					eventID = trace.findExecutionOfInstructionAfter(inst, eventID);
					// Continue if it's not an an invoke or it is but the event is a value produced.
				} while(eventID >= 0 && inst instanceof Invoke && (trace.getKind(eventID).isArtificial || trace.getKind(eventID).isValueProduced));
				
				if(eventID >= 0 && (nextEventID < 0 || eventID < nextEventID))
					nextEventID = eventID;
				
			}

			setCurrentEventID(nextEventID);
			
		}
		
		if(currentEventID > 0)
			whylineUI.setBreakpointDebuggerState("paused", true);

	}
	
	private void addPrintsBetween(int currentID, int nextID) {
		
		IntegerVector eventIDsToPrint = new IntegerVector(10);
		
		for(Print print : prints.values()) {
		
			int beginID = currentID;
			
			while(beginID >= 0 && beginID <= nextID) {
			
				Instruction inst = print.instruction;
				
				if(inst == null) continue;
				
				int eventID = trace.findExecutionOfInstructionAfter(inst, beginID);
							
				if(eventID >= 0 && eventID <= nextID) {
					beginID = eventID;
					eventIDsToPrint.append(eventID);
				}
				else beginID = -1;
				
			}
			
		}
		
		eventIDsToPrint.sortInAscendingOrder();
		
		for(int i = 0; i < eventIDsToPrint.size(); i++) {
			int eventID = eventIDsToPrint.get(i);
			printStatementOutput.add(new Output(eventID));
		}
		
	}
		
	public Line getNearestBreakpointLine(Line line) {
		
		Instruction inst = gettInstrumentedInstructionAfter(line);
		return inst == null ? null : inst.getLine();
		
	}
	
	/**
	 * Returns true if the breakpoint was set.
	 */
	public boolean toggleBreakpoint(Line line) {

		line = getNearestBreakpointLine(line);
		
		boolean on = breakpoints.containsKey(line);
		if(on) breakpoints.remove(line);
		else {
			Instruction inst = gettInstrumentedInstructionAfter(line);
			if(inst != null) {
				breakpoints.put(line, inst); 
			}
		}
		whylineUI.getLinesUI().updateBreakpointLines(line);
		return !on;
		
	}
		
	public List<Line> getLinesWithBreakpointsOrPrints() { 
	
		Set<Line> lines = new HashSet<Line>();
		lines.addAll(breakpoints.keySet());
		for(Token t : prints.keySet())
			lines.add(t.getLine());
		return new ArrayList<Line>(lines); 
		
	}
	
	public boolean hasPrint(Token t) { return prints.containsKey(t); }
	
	public boolean hasBreakpoint(Line i) { return breakpoints.containsKey(i); }

	public void addPrint(Token token, String message) {
		
		prints.put(token, new Print(token, message));

		whylineUI.getLinesUI().updateBreakpointLines(token.getLine());
		
	}

	public void removePrint(Token token) {
		
		prints.remove(token);

	}
	
	public void clearBreakpointsAndPrints() {
		
		breakpoints.clear();
		prints.clear();
		
	}	
	
	/**
	 * Search for the next method invocation that we've traced, stopping when we reach an event with an instruction on a different line.
	 */
	public void stepInto() {
		
		if(!isRunning()) return;
		
		Line previousLine = currentLine;
		do {
			setCurrentEventID(trace.getNextEventIDInThread(currentEventID));
		} while(isRunning() && currentLine != null && currentLine == previousLine && trace.getKind(currentEventID) != EventKind.START_METHOD);
		
	}

	/**
	 * Find the next event in this method on a different line than we started.
	 */
	public void stepOver() {
		
		if(!isRunning()) return;

		Line previousLine = currentLine;
		do {
			int nextMethodID = trace.getNextEventIDInMethod(currentEventID);
			if(nextMethodID < 0) nextMethodID = trace.getNextEventIDInThread(currentEventID);
			setCurrentEventID(nextMethodID);
		} while(isRunning() && currentLine != null && currentLine == previousLine);
		
	}
	
	public void stepOut() {
		
		if(!isRunning()) return;
		
		Line previousLine = currentLine;

		int startID = trace.getStartID(currentEventID);
		int returnID = startID >= 0 ? trace.getStartIDsReturnOrCatchID(startID) : -1;
		int nextID = returnID >= 0 ? trace.getNextEventIDInThread(returnID) : -1;
		
		setCurrentEventID(nextID);

		while(currentLine != null && isRunning() && currentLine == previousLine)
			setCurrentEventID(trace.getNextEventIDInThread(currentEventID));			
		
	}
	
	private Instruction gettInstrumentedInstructionAfter(Line line) {

		try {
			while(line != null && line.getFirstInstruction() == null) 
				line = line.getLineAfter();
		} catch(Exception e) {
			return null; 
		}
		
		if(line == null) return null;

		Instruction inst = line.getFirstInstruction();

		// Originally, we allowed instantiations here, but when we record NEWs, we record AFTER the object is initialized, so that would be wrong here.
		while(inst != null && !(inst instanceof Definition || inst instanceof AbstractReturn || inst instanceof Invoke || inst instanceof Branch))
			inst = inst.getNext();
						
		return inst;
		
	}
	
	public boolean canPrint(Token token ) {

		Instruction code = token.getFile().getInstructionFor(token);
		
		if(code == null)
			return false;
		else
			return code instanceof GetLocal || code instanceof SetLocal || code instanceof GETFIELD || code instanceof PUTFIELD || code instanceof Invoke;
		
	}
	
	public class Output implements Comparable<Output> {
		
		public final String output;
		public final int eventID;
		
		public Output(int eventID) {
			
			this.eventID = eventID;

			Instruction code = trace.getInstruction(eventID);
			
			if(code instanceof Use) {

				output = ((Use)code).getAssociatedName() + " = " + trace.getDescription(eventID);
			
			}
			else if(code instanceof Definition) {

				output = ((Definition)code).getAssociatedName() + " = " + trace.getDefinitionValueSet(eventID).getDisplayName(false);

			}
			else if(code instanceof Invoke) {

				if(trace.getKind(eventID).isValueProduced) {

					output = ((Invoke)code).getJavaMethodName() + "() returned " + trace.getDescription(eventID);

				}
				else
					this.output = null;
				
			}
			else output = null;
				
		}

		public int compareTo(Output o) { return eventID - o.eventID; }
	
	}

	private class Print  {

		private final Token token;
		private final Instruction instruction;
		private final String print;
		
		public Print(Token token, String print) {

			this.token = token;
			this.print = print;
			
			this.instruction = token.getFile().getInstructionFor(token);
			
			assert instruction != null;
			
		}
		
	}

	public void setPauseMode(boolean inPauseMode) { 
		
		this.inPauseMode = inPauseMode; 
		whylineUI.setBreakpointPauseMode(inPauseMode);
		
	}

}
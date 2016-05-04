package edu.cmu.hcii.whyline.analysis;

import java.util.Vector;

import edu.cmu.hcii.whyline.bytecode.Branch;
import edu.cmu.hcii.whyline.bytecode.Instruction;
import edu.cmu.hcii.whyline.trace.Trace;

/**
 * @author Andrew J. Ko
 *
 */
public final class LoopPath {
	
	private final Vector<Decision> decisions = new Vector<Decision>(); 
	
	public LoopPath() {}
	
	public LoopPath(LoopPath path, Branch branch, Instruction target) {
		
		for(Decision dec : path.decisions)
			decisions.add(dec);

		addDecision(branch, target);
		
	}
	
	
	public void addDecision(Branch branch, Instruction target) {
		
		decisions.add(new Decision(branch, target));
		
	}
	
	public boolean matches(Trace trace, int eventID) {
		
		int nextEvent = eventID;
		
		for(Decision dec : decisions) {

			final Instruction target = dec.target;
			
			// Must find the target for this decision to match.
			int targetEvent = trace.findEventInMethodInThreadAfter(nextEvent, target);
			
			if(targetEvent < 0) return false;
			else nextEvent = targetEvent;
			
		}
		
		return true;
		
	}
	
	public String toString() {
		
		String s = "";
		for(Decision dec : decisions) {
			
			s = s + dec + "\n";
			
		}
		return s;
		
	}
	
}

final class Decision {
	
	public final Branch branch;
	public final Instruction target;

	public Decision(Branch branch, Instruction target) {
		
		this.branch = branch;
		this.target = target;
		
	}
	
	public String toString() { 
		
		return branch + "\t\t" + target.toString();
		
	}
	
}

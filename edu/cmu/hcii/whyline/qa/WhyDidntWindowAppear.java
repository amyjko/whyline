package edu.cmu.hcii.whyline.qa;

import java.util.List;

import edu.cmu.hcii.whyline.analysis.UnexecutedInstructionAnalyzer;
import edu.cmu.hcii.whyline.bytecode.Instantiation;
import edu.cmu.hcii.whyline.bytecode.Classfile;
import edu.cmu.hcii.whyline.util.IntegerVector;

/**
 *  
 * 
 * @author Andrew J. Ko
 *
 */
public final class WhyDidntWindowAppear extends WhyDidntQuestion<Classfile> {
	
	public WhyDidntWindowAppear(Asker asker, Classfile windowType, String descriptionOfEvent) {

		super(asker, windowType, descriptionOfEvent);
		
	}
	
	protected Answer answer() {

		if(scope.isEndOfProgram())
			return new MessageAnswer(this, "<i>Nothing</i> changed after the end of the program. Perhaps you forgot to select a specific time?");

		// Did this instantiate?
		IntegerVector instantationIDs = trace.getInstantiationHistory().getInstantiationsOf(subject);
		boolean instantiationsExecuted = instantationIDs.size() > 0;
		for(int i = 0; i < instantationIDs.size(); i++) {
			
			int instantiationID = instantationIDs.get(i);

			// If we made it this far, the class was instantiated. Were there any calls on setVisible?
			if(scope.includesInclusive(instantiationID)) {

				return new CauseAnswer(this, instantiationID, "An instance of <b>" + subject.getDisplayName(false, -1) + "</b> was created. Maybe it <b>setVisible</b>() wasn't called?");
				
			}
			
		}

		// If there were no instantiations in scope, why were they not reached?
		if(instantiationsExecuted) {
			return new CauseAnswer(this, instantationIDs.lastValue(), "There were instantiations of <b>" + subject.getDisplayName(false, -1) + "</b>, but not after the input you selected. Here is the most recent.");
		}
		else {
			List<Instantiation> instantiations = trace.getInstantiationsOf(subject.getInternalName());
			if(instantiations.isEmpty()) {
				return new MessageAnswer(this, "There are no known instantiations of <b>" + subject.getDisplayName(false, -1) + "</b> in this program.");
			}
			else {
				// Explain why none of the instantiations were reached.
				return UnexecutedInstructionAnalyzer.explain(this, instantiations.toArray(new Instantiation[instantiations.size()]), null);
			}
		}
		
	}
	
	public String getQuestionExplanation() {
		
		return "explain why windows of type <b>" + getDescriptionOfSubject() + "</b> didn't appear";
		
	}

}
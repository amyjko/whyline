package edu.cmu.hcii.whyline.analysis;

import java.util.ArrayList;

import edu.cmu.hcii.whyline.bytecode.Instruction;
import edu.cmu.hcii.whyline.qa.*;
import edu.cmu.hcii.whyline.trace.Trace;
import edu.cmu.hcii.whyline.util.IntegerVector;

/**
 * @author Andrew J. Ko
 *
 */
public class WhyNotValueAnalyzer {

	public static Answer compare(Question<?> question, ValueSource source, Trace trace, ExpectedObject objectExpectation, int eventID) {
		
		IntegerVector actualPathEvents = DynamicValueSource.getPathToSource(trace, eventID);

		ArrayList<Instruction> actualPath = new ArrayList<Instruction>(actualPathEvents.size());
		for(int i = 0; i < actualPathEvents.size(); i++) actualPath.add(trace.getInstruction(actualPathEvents.get(i)));
				
		ArrayList<Instruction> expectedPath = new ArrayList<Instruction>(source.path);

		// The dynamic path will have a different start than the expected path. First we need to find the first common instruction.
		Instruction firstCommonInstruction = null;
		for(Instruction actual : actualPath) {
			
			int index = expectedPath.indexOf(actual);
			if(index >= 0) {
				firstCommonInstruction = actual;
				break;
			}
			
		}

		// If we couldn't find a common path, just explain why the expected instruction wasn't reached.
		// But instead of starting from the first expected instruction, start from it's consumer. Probably something like a putfield or invocation.
		if(firstCommonInstruction == null) {
			
//			System.err.println("Couldn't find a common instruction, so explaining " + expectedPath.get(0).getConsumers()[0]);
			
			return UnexecutedInstructionAnalyzer.explain(question, new Instruction[] {expectedPath.get(0).getConsumers().getFirstConsumer()}, objectExpectation);
			
		}
		
//		System.out.println("They both have " + firstCommonInstruction + " in common");

		int expectedIndex = expectedPath.indexOf(firstCommonInstruction);
		int actualIndex = actualPath.indexOf(firstCommonInstruction);

		Instruction expectedDeviation = null;
		Instruction actualDeviation = null;
		
		while(true) {
			
			if(expectedIndex >= expectedPath.size()) break;
			else if(actualIndex >= actualPath.size()) break;
			else if(expectedPath.get(expectedIndex) != actualPath.get(actualIndex)) {
				expectedDeviation = expectedPath.get(expectedIndex);
				actualDeviation = actualPath.get(actualIndex);
				break;
			}
			else {
				actualIndex++;
				expectedIndex++;
			}
			
		}

		if(expectedDeviation == null) {
			
//			System.err.println("Returning \"this code did execute\" for " + trace.getInstruction(eventID) + " " + trace.getKind(eventID));
			IntegerVector executions = new IntegerVector(1);
			executions.append(eventID);
			return new ThisCodeDidExecuteAnswer(question, executions);
			
		}
		else {
			
//			System.out.println("Expected went to " + expectedDeviation);
//			System.out.println("Actual went to " + actualDeviation);

//			System.err.println("Did " + expectedDeviation + " execute within scope?");

			IntegerVector executions = trace.findExecutionsOfInstructionAfter(expectedDeviation, 0, question.getInputEventID());

//			System.err.println("Found " + executions.size() + " executions after " + question.getInputTime());			
			
			if(executions.isEmpty()) {
				
				return UnexecutedInstructionAnalyzer.explain(question, new Instruction[] {expectedDeviation}, objectExpectation);
				
			}
			else {
				
				int actualDeviationEventID = actualPathEvents.get(actualIndex);
				
				for(int i = 0; i < executions.size(); i++)
					if(executions.get(i) > actualDeviationEventID)
						return new ThisCodeDidExecuteAnswer(question, executions);
				
				return new ValueOverriddenAnswer(question, executions.lastValue(), actualDeviationEventID);
					
			}
			
		}

	}
	
}
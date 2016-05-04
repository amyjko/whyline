package edu.cmu.hcii.whyline.qa;

import java.util.Collection;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import edu.cmu.hcii.whyline.bytecode.*;
import edu.cmu.hcii.whyline.io.TextualOutputEvent;

public class TextMenuFactory {

	public static QuestionMenu getTextualOutputMenu(Asker asker, TextualOutputEvent event) {
	
		QuestionMenu questions = new QuestionMenu(asker, "Questions about text", "");
		
		QuestionMenu whyDidntMenu = new QuestionMenu(asker, "Questions about text that <i>didn't</i> print", "why didn't");
	
		// Add a question for this one event.
		if(event != null)
			questions.addQuestion(new WhyDidEventOccur(asker, event.getEventID(), "<em>" + event.getStringPrinted() + "</em> print"));
		
		Collection<Instruction> outputByFinalConsumer = asker.getTrace().getTextualOutputInvokingInstructions();
	
		// Sort the instructions by method
		SortedMap<MethodInfo,SortedSet<Instruction>> textOutputByMethods = new TreeMap<MethodInfo,SortedSet<Instruction>>();
		for(Instruction textOutput : outputByFinalConsumer) {

			if(asker.getTrace().classIsReferencedInFamiliarSourceFile(textOutput.getClassfile().getInternalName())) {

				SortedSet<Instruction> outputInMethod = textOutputByMethods.get(textOutput.getMethod());
				if(outputInMethod == null) {
					
					outputInMethod = new TreeSet<Instruction>();
					textOutputByMethods.put(textOutput.getMethod(), outputInMethod);
					
				}
				outputInMethod.add(textOutput);
				
			}
			
		}
		
		// Generate a menu for each method.
		SortedMap<Classfile, QuestionMenu> classMenus = new TreeMap<Classfile, QuestionMenu>();
		
		for(MethodInfo method : textOutputByMethods.keySet()) {
	
			QuestionMenu methodMenu = 
				new QuestionMenu(asker, "Questions about text that % didn't print.", "%", method); 
			
			SortedSet<Instruction> outputInMethod = textOutputByMethods.get(method);
			for(Instruction textOutput : outputInMethod) {
	
				String string = "this string";
				if(textOutput instanceof PushConstant)
					string = "\"" + ((PushConstant<?>)textOutput).getConstant() + "\"";
				else {
					string = concatenateConstantStringsFromToString(textOutput, "");
				}
				
				methodMenu.addQuestion(new WhyDidntInstructionExecute(asker, textOutput, "print " + string + ""));
				
			}
	
			QuestionMenu classMenu = classMenus.get(method.getClassfile());
			if(classMenu == null) {
				
				classMenu = new QuestionMenu(asker, "Questions about text that % didn't print", "%", method.getClassfile());
				classMenus.put(method.getClassfile(), classMenu);
				
			}
			classMenu.addMenu(methodMenu);
			
		}		
	
		// Add all of the class menus to the why didn't menu
		for(QuestionMenu classMenu : classMenus.values())
			whyDidntMenu.addMenu(classMenu);
			
		questions.addMenu(whyDidntMenu);

		return questions;
		
	}

	private static String concatenateConstantStringsFromToString(Instruction inst, String text) {
		
		for(int arg = 0; arg < inst.getNumberOfArgumentProducers(); arg++)
			text = text + concatenateConstantStringsFromToString(inst.getProducersOfArgument(arg).getFirstProducer(), text);

		if(inst instanceof PushConstant) text = text + ((PushConstant<?>)inst).getConstant();
		else if(inst instanceof GetLocal) text = text + ((GetLocal)inst).getLocalIDName();
		
		return text;
		
	}

}

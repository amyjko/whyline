package edu.cmu.hcii.whyline.qa;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;

import edu.cmu.hcii.whyline.analysis.*;
import edu.cmu.hcii.whyline.bytecode.*;
import edu.cmu.hcii.whyline.bytecode.StackDependencies.Producers;
import edu.cmu.hcii.whyline.source.*;
import edu.cmu.hcii.whyline.trace.Value;
import edu.cmu.hcii.whyline.trace.EventKind;
import edu.cmu.hcii.whyline.trace.Serializer;
import edu.cmu.hcii.whyline.ui.*;
import edu.cmu.hcii.whyline.ui.components.WhylineMenu;
import edu.cmu.hcii.whyline.ui.components.WhylineMenuItem;
import edu.cmu.hcii.whyline.ui.components.WhylinePopup;
import edu.cmu.hcii.whyline.ui.events.AbstractUIEvent;
import edu.cmu.hcii.whyline.ui.events.LoggedAction;
import edu.cmu.hcii.whyline.ui.events.Note;
import edu.cmu.hcii.whyline.ui.io.BreakpointDebugger;

public class SourceFileMenuFactory {
	
	public static void addQuestionsForToken(final WhylineUI whylineUI, final WhylinePopup popup, final Token token) {
		
		addPrintQuestions(whylineUI, popup, token);

		addClassQuestions(whylineUI, popup, token);

		addParameterQuestions(whylineUI, popup, token);
		
		addInstructionQuestions(whylineUI, popup, token);
		
	}
	
	private static void addClassQuestions(final WhylineUI whylineUI, WhylinePopup popup, Token token) {
				
		QualifiedClassName classname = token.getFile().getClassnameFor(token);
		if(classname != null) {

			WhylineMenu menu = new WhylineMenu("class " + classname.getSimpleName()); 

			if(classname.isArray())
				classname = classname.getArrayElementClassname();
			
			final Classfile classfile = whylineUI.getTrace().getClassfileByName(classname);
			if(classfile != null) {
				String name = "<b>" + classfile.getSimpleName() + "</b>.class";
				menu.add(new WhylineMenuItem("<html>show <b>declaration</b>", 
					new LoggedAction(whylineUI) {
						protected AbstractUIEvent<?> act() { return whylineUI.selectClass(classfile, true, UI.POPUP_UI); }
					}));
			}
			
			popup.add(menu);
			
		}

	}

	private static void addParameterQuestions(final WhylineUI whylineUI, WhylinePopup popup, Token token) {
		
		final Parameter parameter = token.getFile().getMethodParameterFor(token);

		if(parameter != null && parameter.getMethod().getCode() != null) {
			
			CodeAttribute instructions = parameter.getMethod().getCode();
			int localID = instructions.getMethod().getLocalIDFromArgumentNumber(parameter.getNumber());
			Question<?> q = getLocalIDQuestion(whylineUI, localID, instructions.getLocalIDNameRelativeToInstruction(localID, instructions.getFirstInstruction()));
			if(q != null) {
				popup.add(new QuestionMenu.QuestionItem(q, null));
			}
			
		}

	}

	public static void addPrintQuestions(final WhylineUI whylineUI, WhylinePopup popup, final Token token) {
		
		if(whylineUI.getMode() != WhylineUI.Mode.BREAKPOINT)
			return;
		
		final Instruction code = token.getFile().getInstructionFor(token);
		
		if(code == null)
			return;

		if(whylineUI.getBreakpointDebugger().canPrint(token)) {

			BreakpointDebugger debugger = whylineUI.getBreakpointDebugger();
			boolean printSet = debugger.hasPrint(token);
			
			if(printSet)
				popup.add(new WhylineMenuItem("<html><b>remove</b> print statement",
					new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							BreakpointDebugger debugger = whylineUI.getBreakpointDebugger();
							debugger.removePrint(token);
							whylineUI.getLinesUI().updateBreakpointLines(null);
						}
				}));
			else 
			popup.add(new WhylineMenuItem(code instanceof Invoke ? "<html>print  <b>return value</b> after executing" : "<html>print <b>value</b> after executing", new ActionListener() {
				public void actionPerformed(ActionEvent e) {
	
					BreakpointDebugger debugger = whylineUI.getBreakpointDebugger();
						
					debugger.addPrint(token, "");
								
				}
			}));
			
		}

	}
	
	private static void addInstructionQuestions(final WhylineUI whylineUI, final WhylinePopup popup, final Token token) {
		
		final Instruction code = token.getFile().getInstructionFor(token);

		if(code == null) return;
		
		// Base on the instruction above, fill in these blanks:
		final MethodInfo method;
		final FieldInfo field;
		final Classfile classfile;
		final int localID;
		
		final Question<?> q;
		
		if(code instanceof Invoke) {
			
			field = null;
			method = whylineUI.getTrace().resolveMethodReference(((Invoke)code).getMethodInvoked().getClassName(), (Invoke)code);
			classfile = method == null ? null : method.getClassfile();
			localID = -1;
			q = null;
			
		}
		else if(code instanceof FieldrefContainer) {
			
			FieldrefInfo ref = ((FieldrefContainer)code).getFieldref();
			field = whylineUI.getTrace().resolveFieldReference(ref);

			method = null;
			classfile = field == null ? null : field.getClassfile();
			localID = -1;

			q = getFieldQuestion(whylineUI, field);
			
		}
		else if(code instanceof GetLocal) {
			
			field = null;
			method = null;
			classfile = null;
			localID = ((GetLocal)code).getLocalID();

			q = getLocalIDQuestion(whylineUI, localID, code.getCode().getLocalIDNameRelativeToInstruction(localID, code.getNext()));

		}
		else if(code instanceof SetLocal) {
			
			field = null;
			method = null;
			localID = ((SetLocal)code).getLocalID();

			// What type is being assigned here?
			Producers producers = code.getProducersOfArgument(0);
			Instruction producer = producers.getFirstProducer();
			if(producer instanceof Invoke) {
				QualifiedClassName returnType = ((Invoke)producer).getMethodInvoked().getReturnType();
				if(!returnType.isPrimitive()) {
					classfile = whylineUI.getTrace().getClassfileByName(returnType);
				}
				else classfile = null;
			}
			else if(producer instanceof NEW) {
				QualifiedClassName type = ((NEW)producer).getClassnameOfTypeProduced();
				classfile = whylineUI.getTrace().getClassfileByName(type);
			}
			else
				classfile = null;

			q = getLocalIDQuestion(whylineUI, localID, code.getCode().getLocalIDNameRelativeToInstruction(localID, code.getNext()));

		}
		else {
			
			field = null;
			method = null;
			classfile = null;
			localID = -1;
			q = null;

		}
		
		if(q != null) {
			
			popup.add(new QuestionMenu.QuestionItem(q, null));
			
		}
		
		if(localID >= 0) {

			String name = "<b>" + code.getCode().getLocalIDNameRelativeToInstruction(localID, code.getNext()) + "</b>";

			WhylineMenu menu = new WhylineMenu("<html>local " + name);

			menu.add(new WhylineMenuItem("<html>show <b>uses</b>", new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					whylineUI.getLinesUI().addResults(new LocalUsesSearch(whylineUI, code, localID));
				}}));
			
			popup.add(menu);
			
		}
		
		if(field != null) {
			
			String name = "<b>" + field.getName() + "</b>";

			WhylineMenu menu = new WhylineMenu("<html>field " + name);

			menu.add(new WhylineMenuItem("<html>show <b>uses</b>", new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					whylineUI.getLinesUI().addResults(new FieldUsesSearch(whylineUI, field));
				}}));
			
			popup.add(menu);
			
		}
		
		if(method != null) {

			Invoke invoke = (Invoke)code;
					
			String methodNameHTML = "<b>" + invoke.getJavaMethodName() + "</b>()";

			WhylineMenu menu = new WhylineMenu("<html>method " + methodNameHTML);
			
			menu.add(new WhylineMenuItem("<html>show <b>declaration</b>", 
				new LoggedAction(whylineUI) {
					protected AbstractUIEvent<?> act() { return whylineUI.selectMethod(method, true, UI.DECLARATION_UI); }
					public Instruction getInstruction() { 
						return token.getFile().getInstructionFor(token);
					}}));
			
			menu.add(new WhylineMenuItem("<html>show <b>callers</b>", 
					new LoggedAction(whylineUI) {
						protected AbstractUIEvent<?> act() { 
							whylineUI.getLinesUI().addResults(new MethodCallersSearch(whylineUI, method));
							return new Note(Serializer.listToString("callers", Serializer.methodToString(method)));
						}
						public Instruction getInstruction() { 
							return token.getFile().getInstructionFor(token);
						}
					}));

			popup.add(menu);
			
		}

		if(classfile != null) {
		
			String name = "<b>" + classfile.getSimpleName() + "</b>";
			
			WhylineMenu menu = new WhylineMenu("<html>class " + name); 

			menu.add(new WhylineMenuItem("<html>show <b>declaration</b>", 
				new LoggedAction(whylineUI) {
					protected AbstractUIEvent<?> act() { return whylineUI.selectClass(classfile, true, UI.POPUP_UI); }
				}));
			menu.add(new WhylineMenuItem("<html>show <b>uses</b>", 
					new LoggedAction(whylineUI) {
						protected AbstractUIEvent<?> act() { 
							whylineUI.getLinesUI().addResults(new ClassUsesSearch(whylineUI, classfile));
							return null;
						}
					}));
					
			popup.add(menu);
			
		}
		
	}
	
	public static void addQuestionsForLine(final WhylineUI whylineUI, final WhylinePopup popup, final Line line) {

		WhylineMenu menu = new WhylineMenu("<html>line <b>" + line.getLineNumber().getNumber() + "</b>"); 
		
		if(whylineUI.getMode() == WhylineUI.Mode.SLICER) {

			menu.add(new WhylineMenuItem("<html>add <i>most recent</i> execution of this statement to slicing criterion", 
				new LoggedAction(whylineUI) {
					protected AbstractUIEvent<?> act() {
						whylineUI.addDynamicSlice(line, true);
						return new Note(Serializer.listToString("slicerecent", Serializer.lineToString(line)));
					}
				}));

			menu.add(new WhylineMenuItem("<html> add <i>all executions</i> of this statement to slicing criterion",
				new LoggedAction(whylineUI) {
					protected AbstractUIEvent<?> act() {
						whylineUI.addDynamicSlice(line, false);
						return new Note(Serializer.listToString("sliceall", Serializer.lineToString(line)));
					}
				}));
			
		}
		else if(whylineUI.getMode() == WhylineUI.Mode.BREAKPOINT) {
			
			final Line nearestLine = whylineUI.getBreakpointDebugger().getNearestBreakpointLine(line);
			if(nearestLine != null) {
			
				final BreakpointDebugger debugger = whylineUI.getBreakpointDebugger();
				boolean breakpointSet = debugger.hasBreakpoint(nearestLine);

				menu.add(new WhylineMenuItem("<html><b>" + (breakpointSet ? "remove" : "set") + "</b> breakpoint", new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						debugger.toggleBreakpoint(nearestLine);
					}
				}));

				menu.add(new WhylineMenuItem("<html><b>run</b> to", new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						debugger.toggleBreakpoint(nearestLine);
						debugger.runToBreakpoint();
						debugger.toggleBreakpoint(nearestLine);
					}
				}));
				
			}
				
		}

		menu.add(new WhylineMenuItem("<html>bookmark", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				whylineUI.getPersistentState().addRelevantLine(line);
			}}));

		menu.add(new WhylineMenuItem("<html>copy to <b>clipboard</b>", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard(); 
				systemClipboard.setContents(new StringSelection(line.getLineNumber().getNumber() + "\t" + line.getLineText()), null);
			}}));

		popup.add(menu);
		
	}

	public static void addQuestionsForMethod(final WhylineUI whylineUI, final WhylinePopup popup, final Line line) {

		if(line == null) return;
		if(!(line.getFile() instanceof JavaSourceFile)) return;
		
		final MethodInfo method = ((JavaSourceFile)line.getFile()).getMethodOfLine(line);
		
		if(method == null) return;
		
		String htmlName = "<b>" + method.getJavaName() + "()</b>";
		
		WhylineMenu menu = new WhylineMenu("<html>method " + htmlName);
		
		if(method.getPotentialCallers().isEmpty()) {
			WhylineMenuItem item = new WhylineMenuItem("<html><i>no callers</i>");
			item.setEnabled(false);
			menu.add(item);
		}
		else
			menu.add(new WhylineMenuItem("<html>show <b>callers</b>", 
			new LoggedAction(whylineUI) {
				protected AbstractUIEvent<?> act() {
					whylineUI.getLinesUI().addResults(new MethodCallersSearch(whylineUI, method));
					return new Note(Serializer.listToString("callers", Serializer.methodToString(method)));
				}
			}));

		if(!method.getOverriders().isEmpty()) {
			menu.add(new WhylineMenuItem("<html>show <b>implementors/overriders</b>", 
				new LoggedAction(whylineUI) {
					protected AbstractUIEvent<?> act() {
						whylineUI.getLinesUI().addResults(new FindOverriders(whylineUI, method));
						return new Note(Serializer.listToString("overriders", Serializer.methodToString(method)));
					}
				}));
		}

		if(method.getMethodOverriden() != null) {
			menu.add(new WhylineMenuItem("<html>show <b>overriden method</b>", 
				new LoggedAction(whylineUI) {
					protected AbstractUIEvent<?> act() { return whylineUI.selectMethod(method.getMethodOverriden(), true, UI.OVERRIDE_UI); }
				}));
		}

		menu.add(new WhylineMenuItem("<html>copy  to <b>clipboard</b>", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				
				try {
				
					Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard(); 
					
					JavaSourceFile source = method.getClassfile().getSourceFile();
					if(source != null) {
						LineNumber first = source.getFirstTokenOfMethodHeader(method).getLineNumber();
						LineNumber last = method.getCode().getLastLineNumber();
						if(first != null && last != null) {
							StringBuilder builder = new StringBuilder();
							for(int i = first.getNumber(); i <= last.getNumber(); i++) {
								Line line = source.getLine(i);
								builder.append(line.getLineNumber().getNumber() + "\t" + line.getLineText() + "\n");
							}
							systemClipboard.setContents(new StringSelection(builder.toString()), null);
						}
						else javax.swing.JOptionPane.showMessageDialog(whylineUI, "Couldn't copy method text because couldn't find its beginnging and end.");
					}
					else javax.swing.JOptionPane.showMessageDialog(whylineUI, "Couldn't find source.");
					
				} catch(ParseException ex) {
					ex.printStackTrace();
				}

			}}));

		menu.add(new WhylineMenuItem("<html>show <b>bytecode</b>", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				
				whylineUI.selectFile(method.getClassfile(), true, "menu");				
			
			}
		}));
		
		popup.add(menu);
		
	}
	
	private static Question<?> getLocalIDQuestion(WhylineUI whylineUI, int localID, String name) {
	
		if(localID < 0) return null;
		
		if(whylineUI.isWhyline()) {
			
			int beforeID = whylineUI.getSelectedEventID();
			
			if(beforeID < 0) return null;
			
			int eventID = whylineUI.getTrace().findLocalIDAssignmentBefore(localID, beforeID);
			if(eventID >= 0) {

				String value = null;
				EventKind kind = whylineUI.getTrace().getKind(eventID);
				if(kind.isDefinition) {
					if(kind.isArgument) value = whylineUI.getTrace().getArgumentValueDescription(eventID);
					else {
						Value v = whylineUI.getTrace().getDefinitionValueSet(eventID);
						value = v == null ? "(unknown value" : v.getDisplayName(true);
					}
				}
				return new WhyDidEventOccur(whylineUI, eventID, "why did <b>" + name + "</b> = <i>" + value + "<i>");
			}

		}
		return null;
		
	}
		
	private static Question<?> getFieldQuestion(WhylineUI whylineUI, FieldInfo field) {
			
		if(field != null && whylineUI.isWhyline()) {
			if(field.isStatic()) {
				int beforeID = whylineUI.getSelectedEventID();
				if(beforeID >= 0) {
					int definitionID = whylineUI.getTrace().findGlobalAssignmentBefore(field.getQualifiedName(), beforeID);
					if(definitionID >= 0) {
						Value value = whylineUI.getTrace().getDefinitionValueSet(definitionID);
						return new WhyDidEventOccur(whylineUI, definitionID, "<b>" + field.getName() + "<b> = " + (value == null ? "its current value" : value.getDisplayName(true)));
					}
				}
			}
			else {
			
//					whylineUI.getTrace().findFieldAssignmentBefore(field, objectID, eventID)
				
			}					
		}
		return null;
				
	}

}

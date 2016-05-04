package edu.cmu.hcii.whyline.ui.events;

import java.util.HashMap;

import edu.cmu.hcii.whyline.trace.Trace;

/**
 * @author Andrew J. Ko
 *
 */
public enum UIEventKind {

	NOTE("note") { public AbstractUIEvent<?> create(Trace trace, String[] args) { return new Note(trace, args); } },
	MODE("mode") { public AbstractUIEvent<?> create(Trace trace, String[] args) { return new ModeSet(trace, args); } },
	
	CLASS_NAVIGATION("class") { public AbstractUIEvent<?> create(Trace trace, String[] args) { return new ClassNavigation(trace, args); } },
	EVENT_NAVIGATION("event") { public AbstractUIEvent<?> create(Trace trace, String[] args) { return new EventNavigation(trace, args); } },
	EXPLANATION_NAVIGATION("explanation") { public AbstractUIEvent<?> create(Trace trace, String[] args) { return new ExplanationNavigation(trace, args); } },
	FILE_NAVIGATION("file") { public AbstractUIEvent<?> create(Trace trace, String[] args) { return new FileNavigation(trace, args); } },
	INSTRUCTION_NAVIGATION("instruction") { public AbstractUIEvent<?> create(Trace trace, String[] args) { return new InstructionNavigation(trace, args); } },
	UNEXECUTED_NAVIGATION("unexecuted") { public AbstractUIEvent<?> create(Trace trace, String[] args) { return new UnexecutedInstructionNavigation(trace, args); } },
	LINE_NAVIGATION("line") { public AbstractUIEvent<?> create(Trace trace, String[] args) { return new LineNavigation(trace, args); } },
	METHOD_NAVIGATION("method") { public AbstractUIEvent<?> create(Trace trace, String[] args) { return new MethodNavigation(trace, args); } }, 
	
	LINE_HOVER("linehover")  { public AbstractUIEvent<?> create(Trace trace, String[] args) { return new LineHover(trace, args); } }, 
	NO_LINE_HOVER("nolinehover")  { public AbstractUIEvent<?> create(Trace trace, String[] args) { return new NoLineHover(trace, args); } }, 
	METHOD_HOVER("methodhover") { public AbstractUIEvent<?> create(Trace trace, String[] args) { return new MethodHover(trace, args); } }, 
	QUESTION_SELECTED("questionselected") { public AbstractUIEvent<?> create(Trace trace, String[] args) { return new QuestionSelected(trace, args); } },
	
	;
	
	private final String name;

	private static HashMap<String,UIEventKind> kindsByShortName = null;
	
	private UIEventKind(String name) {
		
		this.name = name;
		
	}

	public String getShortName() { return name; }
	
	public abstract AbstractUIEvent<?> create(Trace trace, String[] args);

	public static UIEventKind fromType(String type) { 

		if(kindsByShortName == null) {
			
			kindsByShortName = new HashMap<String,UIEventKind>();
			for(UIEventKind kind : values())
				kindsByShortName.put(kind.getShortName(), kind);
			
		}
		
		return kindsByShortName.get(type);
		
	}
	
}
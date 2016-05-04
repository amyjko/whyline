package edu.cmu.hcii.whyline.ui;

/**
 * @author Andrew J. Ko
 *
 */
public class Tooltips {

	private static String getHeader(String name) {
		
		String header = 
			"<html><h2>" + name + "</h2>" +
			"<p>";

		return header;
		
	}
	
	private static String getActionsHeader() {
		
		return "<h3>Actions</h3>";
		
	}
	
	private static String action(String action, String response) {
		
		return "<p><b>" + action + "</b>: " + response;
		
	}
	
	private static String tooltip(String name, String explanation, String ... actions) {
		
		StringBuilder builder = new StringBuilder();
		
		builder.append(getHeader(name));
		builder.append(explanation);
		builder.append(getActionsHeader());
		for(String action : actions) 
			builder.append(action);

		builder.append("<br>");
		
		return builder.toString();
		
	}
	
	public static final String GRAPHICS_UI_NAME = "<b>Graphical Input/Output</b>";
	public static final String CONSOLE_UI_NAME = "<b>Console Input/Output</b>";
	public static final String EXCEPTIONS_UI_NAME = "<b>Exceptions</b>";
	public static final String IO_TABS_NAME = "<b>Input/Output Tabs</b>";
	public static final String TIME_CONTROLLER_UI_NAME = "<b>Time Controller</b>";
	public static final String SOURCE_FILES_UI_NAME = "<b>Source Files</b>";
	public static final String CALL_STACK_UI_NAME = "<b>Call Stack</b>";
	public static final String FILE_OUTLINE_UI_NAME = "<b>File Outline</b>";
	public static final String RESULTS_UI_NAME = "<b>Search Results</b>";
	public static final String SEARCH_FIELD_UI_NAME = "<b>Search Field</b>";
	public static final String QUESTION_TABS_UI_NAME = "<b>Question Tabs</b>";
	public static final String SITUATIONS_UI_NAME = "<b>Situations</b>";
	public static final String VISUALIZATION_UI_NAME = "<b>Answer Visualization</b>";
		

	public static final String GRAPHICS_UI = 
		tooltip(
			GRAPHICS_UI_NAME,
			"This shows all of the <b>graphical output</b> that your program produced." +
			"<br>What it shows depends on what time is selected in the " + TIME_CONTROLLER_UI_NAME,
			action("Hover over output ", "see where it was rendered."),
			action("Click on output", "ask a question about it."),
			action("Type the escape key", "make a question menu go away.")
		);

	public static final String CONSOLE_UI = 
		tooltip(
			CONSOLE_UI_NAME,
			"This shows all of the <b>textual output</b> that your program produced.",
			action("Hover over output ", "see where it was printed.") +
			action("Click on output", "ask a question about it.") +
			action("Type the escape key", "make a question menu go away.")
		);
	
	public static final String EXCEPTIONS_UI =
		tooltip(
			EXCEPTIONS_UI_NAME,
			"This shows all of the <b>exceptions</b> that your program threw or caught.",
			action("Click on an exception", "ask a question about it."),
			action("Type the escape key", "make a question menu go away.")
		);
	
	public static final String IO_TABS = 
		tooltip(
			IO_TABS_NAME,	
			"These tabs allow you to view the <b>different types of output</b> your program produced."
		);
	
	public static final String TIME_CONTROLLER_UI = 
		tooltip(
			TIME_CONTROLLER_UI_NAME,
			"This allows you to see the <b>history</b> of your program's output." +
			"Additionally, the time you have selected affects:" +
			"<ul>" +
			"<li>which <b>why did</b> questions appear in the question menus" +
			"<li>what answers appear for <b>why didn't</b> questions." +
			"<p>When you're <b>viewing an answer</b>, it is used only for reviewing the output history." +
			"</ul>",
			action("Drag the time cursor", "change the time"),
			action("Use the left and right arrow keys", "change the time.")
		);
	
	public static final String SOURCE_FILES_UI = 
		tooltip(
			SOURCE_FILES_UI_NAME, 
			"This shows all of the <b>source files</b> that are relevant to your program's output" + 
			"<br>and any questions you've asked.",
			action("Hover over a file name", "see the full file name"),
			action("Click on a file name", "show a file"),
			action("Click on a method", "ask questions about a method"),
			action("Click on a line", "ask questions about a line"),
			action("Click on a name", "ask questions about a name")
		);
	
	public static final String CALL_STACK_UI = 
		tooltip(
			CALL_STACK_UI_NAME, 
			"This shows all of the methods that have been called, as well as the values for" +
			"<br>local variables in each method and the fields of objects.",
			action("Click on a header", "Collapse a stack frame or object definition"),
			action("Click on an object", "Show the object's fields")
		);
	
	public static final String FILE_OUTLINE_UI = 
		tooltip(
			FILE_OUTLINE_UI_NAME, 
			"This view shows the <b>methods</b> in the file shown below.",
			action("Click on a method name", "Show the method")
		);
	
	public static final String RESULTS_UI = 
		tooltip(
			RESULTS_UI_NAME, 
			"This shows the <b>results</b> of text searches and other operations.",
			action("Click on a result line", "Show the source file line referenced.")
		);
	
	public static final String SEARCH_FIELD_UI = 
		tooltip(
			SEARCH_FIELD_UI_NAME, 
			"This field enables you to search for text in source files." +
			"<br>The results will appear below after you press enter.",
			action("Type enter", "Search for the text you've typed in the field")
		);
	
	public static final String QUESTION_TABS_UI = 
		tooltip(
			QUESTION_TABS_UI_NAME,
			"This shows the <b>questions</b> you've asked.",
			action("Click the 'ask' tab", "Ask another question"),
			action("Click on a question tab", "See the answer to a question"),
			action("Click on the 'x'", "Close a question tab")
		);

	public static final String SITUATIONS_UI = 
		tooltip(
			SITUATIONS_UI_NAME, 
			"This shows all of the possible explanations for the question you asked." +
			"<br>Usually a <b>why did</b> question has a single situation, whereas " +
			"<br>a <b>why didn't</b> question may have multiple situations.",
			action("Click a situation", "See the visualization explaining the situation."),
			action("Click on a situation type header", "Collapse or expand the group of situations.")
		);

	public static final String VISUALIZATION_UI = 
		tooltip(
			VISUALIZATION_UI_NAME,
			"This is a visualization of the things that happened in the situation you've selected." +
			"<br>It shows all of the method calls, assignments to variables, and other events" +
			"<br>that are relevant to the question you asked, but omits other irrelevant events." +
			"<br>The visualization has a selection, which controls what information is visible in all " +
			"<br>of the other Whyline views.",
			action("Click on an event", "Select the event and see its data dependencies."),
			action("Type left or right", "Go back and forth between events."),
			action("Type up", "Go to the previous control event (a method call or conditional)"),
			action("Type down", "Go to the next control event (a method call or conditional)"),
			action("Type a number", "Go to the event that produced the value for the selected event"),
			action("Type enter", "Add the current selection to a list of relevant events.")
		);

	public static final String IDENTIFIER = "Click to show commands for this <b>identifier</b>.";
	public static final String LINE = "Click to show commands for this <b>line</b>.";
	public static final String METHOD = "Click to show commands for this <b>method</b>.";
	public static final String CONSOLE = "Click to ask about <b>text</b> that didn't print";
	public static final String CONSOLE_TEXT = "Click to ask about this <b>text</b>";
	public static final String TIME_CURSOR = "Drag or use " + UI.LEFT_ARROW + " and " + UI.RIGHT_ARROW + " to explore <b>I/O events</b>";
	public static final String IO_FILTER_ON = "Click to turn off this filter and see all <b>I/O events</b>";
	public static final String IO_FILTER_OFF = "Click to only see <b>I/O events</b> of this type";
	public static final String SOURCE_FILE = "Click to show this <b>source file</b>";
	public static final String METHOD_NAME = "Click to show this <b>method</b>";
	public static final String CONTROL_ARROW = "Click to show what caused this to execute";
	public static final String DATA_ARROW = "Click to show where this data came from";
	public static final String WINDOWS = "Click to ask about <b>paint</b> that didn't appear";
	public static final String RENDER_EVENT = "Click to ask about this <b>paint</b> and what it represents";
	public static final String EVENT = "Click to select <b>event</b>";

}
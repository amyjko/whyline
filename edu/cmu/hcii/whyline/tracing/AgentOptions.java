package edu.cmu.hcii.whyline.tracing;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * When passing a Java agent to a JVM, it allows a string of options to be passed.
 * 
 * -javaagent:<jarpath>[=<options>]
 * 
 * This class provides a programatic interface for storing and parsing these options and generating a suitiable string for passing these options.  
 *
 * @author Andrew J. Ko
 */
public final class AgentOptions {

	public static String REPLACEMENT_FOR_SPACES_IN_AGENT_OPTIONS = "#space#";
	public static String REPLACEMENT_FOR_COMMAS_IN_AGENT_OPTIONS = "#comma#";
	public static String REPLACEMENT_FOR_NEWLINES_IN_AGENT_OPTIONS = "#newline#";
	
	public static enum Option {

		PROJECT(String.class, "The root folder for the project being recorded, containing the classes and source. It's used to help specify other paths in the agent options relatively rather than absolutely."),
		CLASSES(String.class, "The path to the root of the recorded program's classes (or jar file)"),
		SOURCE(String.class, "The path to the root of the recorded program's source"),
		SKIP(String.class, "A list of#newline# separated package and class name prefixes (since newlines aren't allowed in the command line string)"),
		PRINT_INSTRUMENTATION_EVENTS(Boolean.class, "optional flag to print debug info for instrumentation events"),
		PRINT_INSTRUMENTATION_SUMMARY(Boolean.class, "optional flag to print summaries for each instrumented class"),
		PRINT_METHODS_BEFORE_AND_AFTER(Boolean.class, "optional flag to print before and after events for each instrument");
		
		public final Class<?> type;
		public final String purpose;
		private Option(Class<?> type, String purpose) { this.type = type; this.purpose = purpose;  }

		// Can't override this because its used to print stuff.
		public final String toString() { return super.toString(); }
		
	}

	private final Map<Option,Object> options = new HashMap<Option,Object>();
	
	public AgentOptions(String projectPath, String classPaths, String sourcePaths) {

		setOption(Option.PROJECT, projectPath);
		setOption(Option.CLASSES, classPaths);
		setOption(Option.SOURCE, sourcePaths);
		
	}
	
	public AgentOptions(String optionsString) {
		
		if(optionsString != null && optionsString.length() > 0) {
		
			 // Split the string by commas
			 String[] options = optionsString.split(",");

			 for(String optionString : options) {
				 
				 String[] propertyValuePair = optionString.split("=");
				 
				 assert propertyValuePair.length == 2 : "This agent options string is corruped; this text, " + optionString + ", has more than two = signs.";
				 
				 Option option = Option.valueOf(propertyValuePair[0]);
				 
				 assert option != null : "Don't know of an option named " + propertyValuePair[0];
	
				 String valueString = propertyValuePair.length > 1 ? propertyValuePair[1] : "";
	
				 valueString = valueString.replace(REPLACEMENT_FOR_SPACES_IN_AGENT_OPTIONS, " ");
				 valueString = valueString.replace(REPLACEMENT_FOR_COMMAS_IN_AGENT_OPTIONS, ",");
				 valueString = valueString.replace(REPLACEMENT_FOR_NEWLINES_IN_AGENT_OPTIONS, "\n");
	
				 Object value = null;
				 if(option.type == String.class) value = valueString;
				 else if(option.type == Boolean.class) value = Boolean.parseBoolean(valueString);
				 else throw new RuntimeException("Only support Booleans and Strings for property values, but you gave me " + value);
	
				 setOption(option, value);
				 
			 }	
			 
		}
		
	}
	
	public void setOption(Option option, Object value) {

		if(option.type != Object.class)
			if(!option.type.isAssignableFrom(value.getClass()))
				throw new RuntimeException("Illegal value for option named " + option + "; it expects a " + option.type);

		options.put(option, value);
		
	}
	
	public Object getOption(Option option) { 
	
		if(!options.containsKey(option)) 
			throw new RuntimeException("The agent option " + option + " hasn't been passed in. It's purpose is " + option.purpose);
		return options.get(option); 
		
	}
	
	public boolean declaresOption(Option option) { return options.containsKey(option); }
	
	public String getOptionsAsValidCommandLineArgument() {

		StringBuilder optionsString = new StringBuilder();

		Iterator<Option> optionIterator = options.keySet().iterator();
		while(optionIterator.hasNext()) {

			Option option = optionIterator.next();
			optionsString.append(option.toString());
			optionsString.append("=");
			
			Object value = options.get(option);
			String valueString = "" + value;
			
			// The value string may not spaces or commas. The command line args are parsed using spaces
			// and we use commas to separate the agent options.
			valueString = valueString.replace(" ", REPLACEMENT_FOR_SPACES_IN_AGENT_OPTIONS);
			valueString = valueString.replace(",", REPLACEMENT_FOR_COMMAS_IN_AGENT_OPTIONS);
			valueString = valueString.replace("\n", REPLACEMENT_FOR_NEWLINES_IN_AGENT_OPTIONS);
			
			optionsString.append(valueString);
			if(optionIterator.hasNext()) 
				optionsString.append(',');
			
		}

		return optionsString.toString();
		
	}
	
}

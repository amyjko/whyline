package edu.cmu.hcii.whyline.tracing;

import java.io.*;
import java.util.*;
import java.util.jar.*;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.cmu.hcii.whyline.Whyline;
import edu.cmu.hcii.whyline.bytecode.Classfile;
import edu.cmu.hcii.whyline.util.Util;

/**
 * Contains knowledge necessary for executing a Java program.
 * 
 * @author Andrew J. Ko
 *
 */ 
public class ExecutionConfiguration {

	private static final String CONFIG_TAG = "config";
	private static final String NAME_TAG = "name";
	private static final String PROJECT_TAG = "location";
	private static final String CLASSES_TAG = "classes";
	private static final String SOURCE_TAG = "source";
	private static final String MAIN_TAG = "main";
	private static final String ARGS_TAG = "arguments";
	private static final String STARTMEM_TAG = "startmemory";
	private static final String MAXMEM_TAG = "maxmemory";
	private static final String CLASSPATH_TAG = "classpath";
	private static final String SKIP_TAG = "classestoskip";

	private String name = "";
	private String projectPath = "";
	private String classpaths = "";
	private String sourcepaths = "";
	private String arguments = "";
	private String mainClass = "";
	private String locationLastCheckedForMains;
	private String startMemory = "64";
	private String maxMemory = "128";
	private String classesToSkip = "";

	private SortedSet<String> classfilesWithMain = new TreeSet<String>();

	public ExecutionConfiguration(String location, String main, String arguments, String start, String max, String classpath) {

		this.name = (new File(location)).getName();
		this.projectPath = location;
		this.arguments = arguments;
		this.startMemory = start;
		this.maxMemory = max;
		this.classpaths = classpath;
		
		this.mainClass = main;
		
	}

	public ExecutionConfiguration(Node config) {

		this.name = "unnamed";

		NodeList children = config.getChildNodes();
		for(int j = 0; j < children.getLength(); j++) {

			Node child = children.item(j);
            if(child.getNodeType() == Node.ELEMENT_NODE) {

            	String tag = child.getNodeName();
            	NodeList tagChildren = child.getChildNodes(); 
            	if(tagChildren.getLength() == 1 && tagChildren.item(0).getNodeType() == Node.TEXT_NODE) {

            		String value =  tagChildren.item(0).getNodeValue();

            		if(value.startsWith("\"") && value.endsWith("\""))
            			value = value.substring(1, value.length() - 1);
            		
	    	    	if(tag.startsWith(PROJECT_TAG)) projectPath = value;
	    	    	else if(tag.equals(NAME_TAG)) name = value;
	    	    	else if(tag.equals(SOURCE_TAG)) sourcepaths = value;
	    	    	else if(tag.equals(MAIN_TAG)) mainClass = value;
	    	    	else if(tag.equals(ARGS_TAG)) arguments = value;
	    	    	else if(tag.equals(STARTMEM_TAG)) startMemory = value;
	    	    	else if(tag.equals(MAXMEM_TAG)) maxMemory = value;
	    	    	else if(tag.equals(CLASSPATH_TAG)) classpaths = value;
	    	    	else if(tag.equals(SKIP_TAG)) classesToSkip = value.replace(",", "\n");

            	}
            	
            }
			
		}

	}

	private void tag(FileWriter writer, String tag, String value) throws IOException {
		
		writer.write("<");
		writer.write(tag);
		writer.write(">");
		writer.write("\"" + value + "\"");
		writer.write("</");
		writer.write(tag);
		writer.write(">");
		writer.write("\n");
		
	}
	
	public void write(FileWriter writer) throws IOException {

		writer.write("<" + CONFIG_TAG + ">\n");

		tag(writer, NAME_TAG, name);
		tag(writer, PROJECT_TAG, projectPath);
		tag(writer, CLASSPATH_TAG, classpaths);
		tag(writer, SOURCE_TAG, sourcepaths);
		tag(writer, MAIN_TAG, mainClass);
		tag(writer, ARGS_TAG, arguments);
		tag(writer, STARTMEM_TAG, startMemory);
		tag(writer, MAXMEM_TAG, maxMemory);
		tag(writer, SKIP_TAG, classesToSkip.replace("\n", ","));

		writer.write("</" + CONFIG_TAG + ">\n");
		
	}

	public String getName() { return this.name; }
	public void setName(String name) { this.name = name; }
	
	public void setProjectPath(String location) {

		this.projectPath = location;

	}
	
	private static void findClassAndJARFiles(File directory, List<File> classfiles) {

		File[] files = directory.listFiles();
		for(File file : files) {
			
			if(file.isDirectory()) findClassAndJARFiles(file, classfiles);
			else if(file.getName().endsWith(".class")) classfiles.add(file);
			else if(file.getName().endsWith(".jar")) classfiles.add(file);
			
		}
		
	}
	
	public void determineClassesWithMain() { 
		
		if(projectPath.equals(locationLastCheckedForMains)) return;
		
		locationLastCheckedForMains = projectPath;
		
		classfilesWithMain.clear();

		File project = new File(projectPath);
		if(!project.exists()) return;
		
    	String[] paths = classpaths.equals("") ? new String[] { projectPath } : classpaths.split(File.pathSeparator);

    	for(String path : paths) {
    	
	    	File classes = new File(path);
	    	if(!classes.isAbsolute())
	    		classes = new File(projectPath, path);

			if(classes.isDirectory()) {
	
				Vector<File> files = new Vector<File>();
				findClassAndJARFiles(classes, files);
				
				for(File file : files) {
	
					if(file.getName().endsWith(".class")) {
						try {
							String name = Classfile.hasMain(Util.getReaderFor(file));
							if(name != null)
								classfilesWithMain.add(name);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					else if(file.getName().endsWith(".jar"))
						processJAR(file);
					
				}
	
			}
			else if(classes.getName().endsWith(".jar"))
				processJAR(classes);
				
    	}

	}

	private void processJAR(File jarFile) {
		
		try {
			// Write all of the source files
			JarFile jar = new JarFile(jarFile);

			Enumeration<JarEntry> entries = jar.entries();
			while(entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				if(entry.getName().endsWith(".class")) {
					String name = Classfile.hasMain(new DataInputStream(jar.getInputStream(entry))); 
					if(name != null)
						classfilesWithMain.add(name);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public SortedSet<String> getClassfilesWithMain() { return classfilesWithMain; }

	public String getSourcePaths() { return sourcepaths; }

	public void setSourcePaths(String pathToSource) { this.sourcepaths = pathToSource; }

	public String getProjectPath() { return projectPath; }

	private String convertPathToClassesToPathList() {

		String pathList = "";
		
		File classes = new File(projectPath);
		
		if(classes.isDirectory()) {

			pathList = classes.getAbsolutePath();
			
			List<File> files = new ArrayList<File>();
			findClassAndJARFiles(classes, files);
			
			for(File file : files) {

				if(file.getName().endsWith(".jar"))
					pathList = pathList + File.pathSeparator + file.getAbsolutePath();
				
			}

		}
		else if(classes.getName().endsWith(".jar"))
			pathList = classes.getAbsolutePath();

		return pathList;
		
	}
	
	public String getMainClass() { return mainClass; }
	public void setMainClass(String mainClass) { this.mainClass = mainClass; }

	public String getArguments() { return arguments; }
	public void setArguments(String args) { this.arguments = args; }

	public String getStartMemory() { return startMemory; }
	public void setStartMemory(String startMemory) { this.startMemory = startMemory; }

	public String getMaxMemory() { return maxMemory; }
	public void setMaxMemory(String maxMemory) { this.maxMemory = maxMemory; }

	public String getClassPaths() { return classpaths; }

	public void setClassPaths(String classpathAdditions) {
		
		this.classpaths = classpathAdditions; 
	
	}
	
	public String getAbsoluteClasspathAdditions() {
		
		String[] paths = classpaths.split(File.pathSeparator);
		String result = "";
		for(String path : paths)
			result = result + projectPath + (projectPath.endsWith(File.separator) ? "" : File.separatorChar) + path + File.pathSeparatorChar;
		return result;
		
	}

	public String getClassesToSkip() { return classesToSkip; }
	public void setClassesToSkip(String classesToSkip) { this.classesToSkip = classesToSkip; }

	public String[] getExecutionCommand(File pathToWhylineJAR, AgentOptions options, boolean quoteClasspath) { 

		String programArguments = getArguments().trim();
		String[] programArgs = programArguments.equals("") ? new String[0] : programArguments.split(" ");

		int numberOfArguments = 9;
		
		String[] args = new String[numberOfArguments + programArgs.length];
		args[0] = "java";
		args[1] = "-Dswing.defaultlaf=javax.swing.plaf.metal.MetalLookAndFeel"; 			// We need to force a non-native look and feel in order to trace output.
		args[2] = "-cp";
		args[3] = (quoteClasspath ? "\"" : "") +   
			pathToWhylineJAR.getAbsolutePath() + File.pathSeparatorChar + 
			convertPathToClassesToPathList() + 
			(getClassPaths().equals("") ? "" : File.pathSeparatorChar + getAbsoluteClasspathAdditions()) +
			(quoteClasspath ? "\"" : "");
		
		// We don't use a path separator after the "p" because it's a colon on every platform.
		args[4] = "-Xbootclasspath/p:" + pathToWhylineJAR.getPath();
		args[5] = "-Xms" + getStartMemory() + "m";
		args[6] = "-Xmx" + getMaxMemory() + "m";
		args[7] = "-javaagent:" + pathToWhylineJAR.getPath() + "=" + (options == null ? "" : options.getOptionsAsValidCommandLineArgument());
		args[8] = getMainClass();
		for(int i = numberOfArguments; i < args.length; i++) args[i] = programArgs[i - numberOfArguments];

		return args;
		
	}
		
	public boolean isValid() {

		return  
			isValidProjectLocation(projectPath) == null &&
			isValidMemory(getStartMemory()) == null  && 
			isValidMemory(getMaxMemory())  == null && 
			isValidPathList(projectPath, classpaths) == null &&
			isValidPathList(projectPath, sourcepaths) == null &&
			isValidClassWithMain(projectPath, classpaths, mainClass) == null;

	}
		
	public String getJavaCommand(AgentOptions options, boolean quoteClasspath) {
		
		String[] args = getExecutionCommand(Whyline.WHYLINE_JAR_PATH, options, quoteClasspath);
		
		String command = "";
		for(String s : args) command = command + s + " ";

		return command;
		
	}
	
	public String toString() {

		return name;
		
	}

	public static String isValidMemory(String mem) {
		
		try { 
			int mb = Integer.parseInt(mem); 
			if(mb < 1) return "Must be greater than or equal to 1";
			return null;
		} catch(NumberFormatException e) { 
			return "Must be an integer";
		}

	}

	public static String isValidProjectLocation(String text) {
		File folder = new File(text);
		if(!folder.exists()) return "Couldn't find folder...";
		else if(!folder.isDirectory()) return "The project path must be a folder, not a file"; 
		else return null;
	}

	/**
	 * 
	 * @param paths
	 * @return Returns null if there are no problems.
	 */
	public  static String isValidPathList(String project, String pathsString) {

		String[] paths = pathsString.split(File.pathSeparator);
		for(String path : paths) {
			File file = new File(path);
			if(!file.isAbsolute())
				file = new File(project, path);
			if(!file.exists())
				return "Couldn't find file \"" + path + "\"";
			else if(!(file.isDirectory() || file.getName().endsWith(".jar")))
				return "File must be a directory or .jar file";
		}
		
		return null;
			
	}

	public static String isValidClassWithMain(String project, String classPaths, String classname) {
		
		if(classname == null || classname.equals(""))
			return "Must specify a class name to execute";
		
		// Convert main class into file.
		classname = classname.replace(".", File.separator) + ".class";
		String[] paths = classPaths.split(File.pathSeparator);
		File classfile = null;
		for(String path : paths) {
			File file = new File(path);
			if(!file.isAbsolute())
				file = new File(project, path);
			classfile = new File(file, classname);
			// Is there a classfile with this name?
			if(classfile.exists())
				break;
			else {
				// Is there a jar file with this name.
				classfile = null;
			}
		}
		if(classfile == null)
			return null;//"Couldn't find this classfile in any of the specified class paths";
		else
			return null;

	}

}
package edu.cmu.hcii.whyline.tracing;

import java.io.*;
import java.lang.instrument.*;
import java.util.*;
import java.util.jar.*;

import edu.cmu.hcii.whyline.Whyline;
import edu.cmu.hcii.whyline.analysis.AnalysisException;
import edu.cmu.hcii.whyline.bytecode.Classfile;
import edu.cmu.hcii.whyline.bytecode.JavaSpecificationViolation;
import edu.cmu.hcii.whyline.bytecode.QualifiedClassName;
import edu.cmu.hcii.whyline.trace.EventKind;
import edu.cmu.hcii.whyline.util.Util;

import static edu.cmu.hcii.whyline.tracing.AgentOptions.Option;

/**
 * 
 * The instrumentation package works as follows. To execute the premain() function below,
 * you run the following command line statement:
 * 
 * 		java -javaagent:myagentjarfile.jar [plus whatever other command line options, such as the program you're executing]
 * 
 * To make the agent jar file, one needs this compiled class and a manifest file.
 *  
 * 		Manifest-Version: 1.0
 * 		Premain-Class: edu.cmu.hcii.whyline.Tracer
 * 		Boot-Class-Path: /Library/Java/Extensions/whyline.jar
 * 		Can-Redefine-Classes: true
 * 
 * Agent contains our premain() definition. The boot class path contains any libraries needed during boot loading (in particular, the whyline jarfile).
 * We also tell the JVM to allow us to redefine classes that have already been loaded, so we can even get at Object.class. 
 * There's a bug in the current version of the Apple JVM that makes the JVM crash on relative paths, so instead of hard coding 
 * the boot class path right now, I'm puttng both the javassist.jar and the tracer.jar file in Library/Java/Extensions so that it finds both of them.
 * 
 * To make the jar file, we do
 * 
 * 		jar cmf tracer-manifest tracer.jar Tracer.class
 * 
 * Then we put the compiled jar file in Library/Java/Extensions. Then, to run the agent with some Java 
 * application called Paint.jar, we do
 * 
 * 		java -javaagent:tracer.jar -jar Paint.jar
 * 
 * 
 * @author Andrew J. Ko
 *
 */
public final class Agent {

	private static File sourcefileFile;

	static DataOutputStream classes;
	
	private static String projectPath = null;
	
	public static ClassIDs classIDs;
	static {
		try {
		
			File classIDsFile = new File(Whyline.getWorkingTraceFolder(), Whyline.CLASSIDS_PATH);
			// If there's no  global class ids file yet, make one.
			if(!Whyline.getWorkingClassIDsFile().exists())
				Whyline.getWorkingClassIDsFile().createNewFile();

			// Copy the global ids file to the working trace.
			Util.copyFile(Whyline.getWorkingClassIDsFile(), classIDsFile);

			classIDs = new ClassIDs(classIDsFile);
		
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	private static void fail(String message) {
		
		Whyline.debugBreak();
		Whyline.debug(message);
		Whyline.debugBreak();
		System.exit(Whyline.WHYLINE_FAILURE_EXIT_CODE);
		
	}
	
    /**
     * This is a special method, recognized by the JVM, that will allow us to attach an instrumentation tool to the execution of this program.
     * 
     * @param optionsString 		A string containing no spaces. Agent parses it to get at the options. 
     * @param instrumentation	The instrumentation instance passed by the JVM.
     */
    public static void premain(String optionsString, final Instrumentation instrumentation) { 
    	
		try {

			// Clear out the previous trace folder, if any.
			Util.deleteFolder(Whyline.getWorkingTraceFolder());

			// Create a new folder.
			(new File(Whyline.getWorkingTraceFolder(), Whyline.STATIC_FOLDER_PATH)).mkdirs();
			(new File(Whyline.getWorkingTraceFolder(), Whyline.DYNAMIC_FOLDER_PATH)).mkdir();
			(new File(Whyline.getWorkingTraceFolder(), Whyline.HISTORY_PATH)).mkdir();
			
			classes = Util.getWriterFor(Whyline.getWorkingClassnamesFile());

			// Process any agent strings passed in by the invoker
			processAgentOptions(optionsString);

			// If we don't do this, and ALL of the classes we load are pre-instrumented, then the KindOfEvent.class
			// gets initialized during instrumentation, which causes Class.getEnumConstants() to return null for some reason.
			EventKind.class.getEnumConstants();
			
			// Tell the JVM who to notify about loaded classes...
	    	instrumentation.addTransformer(new ClassInstrumenter()); 
	    	
	    	// Make sure we include all of the classes already loaded.
			for(Class<?> c : instrumentation.getAllLoadedClasses()) {
				QualifiedClassName name = QualifiedClassName.get(c.getName());
				if(name != null) {
					classIDs.includeClassName(name);
					Class<?> superclass = c.getSuperclass();
					if(superclass != null) classIDs.markSuperclass(name, QualifiedClassName.get(superclass.getName()));
					// If this was loaded by the bootstrap class loader, and its not an array or primitive, than include it in the trace. 
					if(c.getClassLoader() == null && !name.isArray() && !name.isPrimitive())
						addClassToTrace(name, null, 0, ClassInstrumenter.bootClassPath);
				}
			}

		} catch (Exception e) {
			
			e.printStackTrace();
			fail("" + e.toString());

		}

    	Whyline.debug("Invoking main()...");

    }
    
    /**
     * Parses the options string, setting various global debugging flags and paths.
     * 
     * @param optionsString
     * @throws IOException 
     */
    private static void processAgentOptions(String optionsString) throws IOException {
    	
    	AgentOptions options = new AgentOptions(optionsString);
    	
    	projectPath = (String)options.getOption(Option.PROJECT);
    	
    	String classPaths = (String)options.getOption(Option.CLASSES);
    	
    	Whyline.debug("Writing user's classes...");
    	
    	processUserClasses(classPaths);

    	Whyline.debug("Writing user's source...");

    	String sourcePaths = (String)options.getOption(Option.SOURCE);
		
    	cacheUserSourceFiles(sourcePaths);

    	if(options.declaresOption(Option.PRINT_INSTRUMENTATION_SUMMARY))
    		ClassInstrumenter.DEBUG_INSTRUMENTATION = (Boolean)options.getOption(Option.PRINT_INSTRUMENTATION_SUMMARY);
		
    	if(options.declaresOption(Option.PRINT_INSTRUMENTATION_SUMMARY))
    		Tracer.DEBUG_CLASSES_INSTRUMENTED = (Boolean)options.getOption(Option.PRINT_INSTRUMENTATION_SUMMARY);
		
    	if(options.declaresOption(Option.PRINT_METHODS_BEFORE_AND_AFTER))
    		ClassInstrumenter.DEBUG_BEFORE_AND_AFTER = (Boolean)options.getOption(Option.PRINT_METHODS_BEFORE_AND_AFTER);
    	
    	String prefixesToSkip = (String)options.getOption(AgentOptions.Option.SKIP);

    	// Send internal qualified names, rather than "."'s, since that's what the class transformer receives
    	for(String prefix : prefixesToSkip.split("\n"))
    		ClassInstrumenter.addPrefixToSkip(prefix.replace(".", "/"));
    	
    }
    
    private static void processUserClasses(String classPaths) throws IOException {

    	String[] paths = classPaths.equals("") ? new String[] { projectPath } : classPaths.split(File.pathSeparator);

    	for(String path : paths) {
    	
	    	File classes = new File(path);
	    	if(!classes.isAbsolute())
	    		classes = new File(projectPath, path);
	
	    	if(!classes.exists()) fail("Couldn't find a file or directory with the name \"" + path + "\"");
	    	else if(classes.isDirectory()) recursivelyProcessUserClassesInFolder(classes, "");
	    	else if(!classes.getName().endsWith(".jar")) {
	    		
	    		long lastModified = classes.lastModified();
	    		
				JarFile jar = new JarFile(classes);
	
				Enumeration<JarEntry> e = jar.entries();
				while(e.hasMoreElements()) {
					JarEntry entry = e.nextElement();
					if(entry.getName().endsWith(".class")) {
						
						// Mark the modification date to help instrumentation avoid unnecessarily instrumenting.
						QualifiedClassName qualifiedClassName = QualifiedClassName.get(entry.getName().substring(0, entry.getName().lastIndexOf(".")));
						cacheUserClassfile(qualifiedClassName, jar.getInputStream(entry), entry.getSize(), lastModified, classes.getAbsolutePath());
	
					}
				}
	
	    	}
	    	
    	}

    }
    
    private static void recursivelyProcessUserClassesInFolder(File directory, String qualifier) throws IOException {
    	
		File[] files = directory.listFiles();

		for(File file : files) {

			if(file.isDirectory()) recursivelyProcessUserClassesInFolder(file, qualifier + file.getName() + "/");
			else if(file.getName().endsWith(".class")) {

				// Mark the modification date to help instrumentation avoid unnecessarily instrumenting.
				QualifiedClassName qualifiedClassName = QualifiedClassName.get(qualifier + file.getName().substring(0, file.getName().lastIndexOf(".")));
				cacheUserClassfile(qualifiedClassName, new FileInputStream(file), file.length(), file.lastModified(), file.getAbsolutePath());

			}
			else if(file.getName().endsWith(".jar")) {
				
				long lastModified = file.lastModified();
				
				JarFile jar = new JarFile(file);

				Enumeration<JarEntry> e = jar.entries();
				while(e.hasMoreElements()) {
					JarEntry entry = e.nextElement();
					if(entry.getName().endsWith(".class")) {
						
						// Mark the modification date to help instrumentation avoid unnecessarily instrumenting.
						QualifiedClassName qualifiedClassName = QualifiedClassName.get(entry.getName().substring(0, entry.getName().lastIndexOf(".")));
						cacheUserClassfile(qualifiedClassName, jar.getInputStream(entry), entry.getSize(), file.lastModified(), file.getAbsolutePath());
						
					}
				}
				
			}
			
		}
    	
    }

    private static void cacheUserClassfile(QualifiedClassName classname, InputStream stream, long numberOfBytes, long modificationDate, String classpaths) {

		Agent.classIDs.markClassnameModificationDate(classname, modificationDate);

		byte[] bytes = new byte[(int)numberOfBytes];
		try {

	    	BufferedInputStream reader = new BufferedInputStream(stream);
			reader.read(bytes);
			
			addClassToTrace(
					classname, 
					classpaths == null ? 
							(new Classfile(bytes)).toByteArray() : 
							null, 
					modificationDate, 
					classpaths);

		} catch (IOException e) {
			e.printStackTrace();
			fail("There was writing " + classname);
		} catch (JavaSpecificationViolation e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (AnalysisException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		    	
    }
    
    /**
     * Searches the user specified path for java source files and writes them all to a file in the following format:
     * 
     * <ul>
     * 		<li>UTF (the source file name)
     * 		<li>long (the length of the source file byte array)
     * 		<li>byte[] (the source file, encoded as bytes)
     * </ul>
     * @param sourcePaths 
     * 
     * @throws IOException
     */
    private static void cacheUserSourceFiles(String sourcePaths) throws IOException {

    	String[] paths = sourcePaths.equals("") ? new String[] { projectPath } : sourcePaths.split(File.pathSeparator);

    	Whyline.getWorkingSourceFolder().mkdir();

    	for(String path : paths) {
						
	    	File source = new File(path);
	    	if(!source.isAbsolute())
	    		source = new File(projectPath, path);
	    	
	    	if(!source.exists()) fail("I couldn't find a file or directory with the name \"" + path + "\"");
	    	else if(source.isDirectory()) findJavaSourceFilesInApplicationPath(source, "");
	    	else if(source.getName().endsWith(".jar")) {
	    		
				// Write all of the source files
				JarFile jar = new JarFile(source);
				Enumeration<JarEntry> e = jar.entries();
				while(e.hasMoreElements()) {
					JarEntry entry = e.nextElement();
					if(entry.getName().endsWith(".java")) {
						
						DataInputStream stream = new DataInputStream(jar.getInputStream(entry));
						byte[] bytes = new byte[(int)entry.getSize()];
						stream.readFully(bytes);
						FileOutputStream out= new FileOutputStream(new File(entry.getName().replace('/', File.separatorChar)));
						out.write(bytes);
						
					}
				}
	    	}
    	}
    	
    }
   
    private static void findJavaSourceFilesInApplicationPath(File directory, String qualifier) throws IOException {
    	
		File[] files = directory.listFiles();

		for(File file : files) {

			if(file.isDirectory()) 
				findJavaSourceFilesInApplicationPath(file, qualifier + file.getName() + "/");
			
			else if(file.getName().endsWith(".java")) {

				File newFolder = new File(Whyline.getWorkingSourceFolder(), qualifier.replace('/', File.separatorChar));
				newFolder.mkdirs();
				Util.copyFile(file, new File(newFolder, file.getName()));
				
			}
			
		}
    	
    }
    
    public static void addClassToTrace(QualifiedClassName name, byte[] bytes, long modificationDate, String paths) {

    	if(name.getText().startsWith("edu/cmu/hcii/whyline")) return;
    	
    	synchronized(classes) {
		
	    	try {
	
	    		String classname = name.getText();
	    		File classfile = null;

	    		// If we didn't receive a path, then we'll write the class file to disk and use the cache path.
	    		if(paths == null) {
		    		String filename = classname.replace('/', File.separatorChar) + ".class";
	    			classfile = new File(Whyline.getUninstrumentedClassCacheFolder(), filename);	    			
	    			paths = classfile.getAbsolutePath();
	    		}

	    		// Remember the name of the class and its path.
	    		classes.writeUTF(classname);
	    		classes.writeUTF(paths);
	    		Tracer.numberOfClassfiles++;

	        	// If we have bytes, write them!
	        	if(bytes != null) {
	    			classfile.delete();
	    			classfile.getParentFile().mkdirs();
	    			OutputStream out = new BufferedOutputStream(new FileOutputStream(classfile, false));
	    			out.write(bytes);
	    			out.flush();
	    			out.close();
	    		}
	    		
	    	} catch (IOException e) {
	    		e.printStackTrace();
	    	}
	    	
		}
    	
    }
    
}

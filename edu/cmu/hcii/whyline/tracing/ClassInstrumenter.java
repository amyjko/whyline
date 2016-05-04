package edu.cmu.hcii.whyline.tracing;

import java.io.*;
import java.lang.instrument.*;
import java.security.ProtectionDomain;
import java.util.*;

import edu.cmu.hcii.whyline.Whyline;
import edu.cmu.hcii.whyline.bytecode.*;
import edu.cmu.hcii.whyline.trace.EventKind;
import edu.cmu.hcii.whyline.util.Util;

/**
 * Instruments a JVM classfile for Whyline recording.
 * 
 * @author Andrew J. Ko
 *
 */ 
public class ClassInstrumenter implements ClassFileTransformer {

	public static boolean DEBUG_INSTRUMENTATION = false;
	public static boolean DEBUG_BEFORE_AND_AFTER = false;
	public static String DEBUG_PRINT_INSTRUMENTED_BYTECODE = "fjsdlfkjdkf";

	/**
	 * Contains prefixes of packages to skip.
	 */	
	private static Vector<String> classnamePrefixesToSkip = new Vector<String>(20);
	static {
		classnamePrefixesToSkip.add("sun/misc/");
		classnamePrefixesToSkip.add("sun/reflect/");
		classnamePrefixesToSkip.add("sun/nio/");
		classnamePrefixesToSkip.add("sun/net/");
		classnamePrefixesToSkip.add("sun/security/");
		classnamePrefixesToSkip.add("java/lang/");
		classnamePrefixesToSkip.add("java/io/");
		classnamePrefixesToSkip.add("java/nio/");
		classnamePrefixesToSkip.add("java/util/");
		classnamePrefixesToSkip.add("java/net/");
		classnamePrefixesToSkip.add("java/text/");
		classnamePrefixesToSkip.add("java/security/");		
		classnamePrefixesToSkip.add("gnu/trove/");
		classnamePrefixesToSkip.add("edu/cmu/hcii/whyline/");
		// We aren't instrumenting this class because Toolkit$SelectiveAWTEventListener.eventDispatched() seems to hang if we do.
		classnamePrefixesToSkip.add("java/awt/Toolkit$SelectiveAWTEventListener");
		classnamePrefixesToSkip.add("apple/awt/CRenderer");
		classnamePrefixesToSkip.add("apple/awt/CTextPipe");
		classnamePrefixesToSkip.add("apple/awt/CPeerSurfaceData");
		classnamePrefixesToSkip.add("sun/awt/AWTAutoShutdown");
		classnamePrefixesToSkip.add("sun/awt/FontConfiguration");
		classnamePrefixesToSkip.add("apple/awt/OSXSurfaceData");
		classnamePrefixesToSkip.add("sun/java2d/loops/");
		classnamePrefixesToSkip.add("sun/java2d/pipe/");
	}
	
	public ClassInstrumenter() { 

		// Make the cache directories if necessary
		Whyline.getClassCacheFolder().mkdir();
		Whyline.getInstrumentedClassCacheFolder().mkdir();
		Whyline.getUninstrumentedClassCacheFolder().mkdir();
		
	}

	public static void addPrefixToSkip(String prefix) {
		
		if(prefix.length() > 0)
			classnamePrefixesToSkip.add(prefix);
		
	}
	
	private static void debug(String message) {
		
		Whyline.debug(message);
		
	}
	
	/**
	 * Returns the pre-instrumented version of the class by the given name.
	 * 
	 * @param classname In fully qualified internal format (with /'s not dots).
	 * @return The byte array that represents the class file.
	 * @throws IOException 
	 */
	public static byte[] getCachedVersionOf(QualifiedClassName classname) throws IOException {

		String pathString = getPathToInstrumentedClass(classname);
		
		File file = new File(pathString);
		
		if(!file.exists()) return null;

		FileInputStream reader = new FileInputStream(file);
		byte[] bytes = new byte[(int)file.length()];
		reader.read(bytes);
		return bytes;
		
	}

	private static String getPathToInstrumentedClass(QualifiedClassName classname) {

		// Use the class name to generate a platform specific path to the class.
		return Whyline.getInstrumentedClassCacheFolder().getAbsolutePath() + File.separatorChar + classname.getCorrespondingClassfileName();

	}

	private static void cache(QualifiedClassName classname, byte[] classfile) {
		
		String pathString = getPathToInstrumentedClass(classname);
		
		String directory = pathString.substring(0, pathString.lastIndexOf(File.separatorChar) + 1);
		
		(new File(directory)).mkdirs();

		try {

			File path = new File(pathString);
			if(path.exists()) path.delete();
			FileOutputStream writer = new FileOutputStream(path);
			writer.write(classfile);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * 
	 * @param className
	 * @return True if the class by the given name should be instrumented, false otherwise.
	 */
	private boolean shouldTrace(String className) {

		for(String prefix : classnamePrefixesToSkip)
			if(className.startsWith(prefix)) {
//				debug("Skipping " + className + "; starts with " + prefix);
				return false;
			}

		return true;
		
	}

	public class ClassInstrumentationInfo {
		
		private final ConstantPool pool;
		public final MethodrefInfo tracerDebugIn, tracerDebugOut;
		public final ClassInfo tracerClassInfo;
		private final EnumMap<EventKind,MethodrefInfo> methodRefsAdded = new EnumMap<EventKind,MethodrefInfo>(EventKind.class);
		
		public ClassInstrumentationInfo(ConstantPool pool) throws JavaSpecificationViolation {

			this.pool = pool;
			tracerClassInfo = pool.addClassInfo(Tracer.class);
			tracerDebugIn = pool.addMethodrefInfo(tracerClassInfo, "debugIn", "()V");
			tracerDebugOut = pool.addMethodrefInfo(tracerClassInfo, "debugOut", "()V");

		}
		
		public MethodrefInfo getMethodFor(EventKind event) throws JavaSpecificationViolation {
			
			MethodrefInfo methodref = methodRefsAdded.get(event);
			if(methodref == null) {
				methodref = pool.addMethodrefInfo(tracerClassInfo, event.name, event.descriptor);
				methodRefsAdded.put(event, methodref);
			}
			return methodref;
			
		}
		
	}
	
	public static final String bootClassPath = System.getProperty("sun.boot.class.path");
	
    // The interface method required by ClassFileTransformer. This allows us to take a class, defined by the bytes given, and transform it
    // before its officially loaded (actually, since we have the Can-Redefine-Classes flag set to true in the agent's manifest file, we can also
	// redefine classes that have already been loaded. This is where we do all of the instrumentation.
	public byte[] transform(ClassLoader loader, String classnameString, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
				
		// If we're done tracing, we do nothing..
		if(Tracer.isShutdown()) return null;

		QualifiedClassName classname = QualifiedClassName.get(classnameString);
		
		ClassInfo superclassInfo = null;

		try {
			
			Agent.classIDs.includeClassName(classname);

			long beforeTime = System.currentTimeMillis();
							
			// Load a structured representation of the classfile
	        Classfile classfile = new Classfile(classfileBuffer);

	        // Find out where this class came from, if possible.
			final String path;

			// If its the bootstrap loader, use the bootstrap class path.
			if(loader == null) path = bootClassPath;
			// If its a URL class loader with a path, use the path.
			else if(loader instanceof java.net.URLClassLoader){
				java.net.URL url = ((java.net.URLClassLoader)loader).getResource(classnameString);
				path = url == null ? null : url.getPath();
			}
			// Otherwise, we don't know.
			else path = null;

			// Save a pre-analyzed version if we can, so we don't have to waste time analyzing it over again.
			// We only pass the classfiles bytes if we don't have a path.
			Agent.addClassToTrace(classname, path == null ? classfile.toByteArray() : null, 0, path);

			boolean shouldTrace = shouldTrace(classname.getText());
			if(Tracer.isShuttingDown()) shouldTrace = false;

			if(!shouldTrace)
				return null;

	        Tracer.addClassesToCacheOnExit(classfile.getConstantPool().getClassNamesReferenced(), loader);

	        // Mark the classes superclass.
	        superclassInfo = classfile.getSuperclassInfo();
	        if(superclassInfo != null) Agent.classIDs.markSuperclass(classname, superclassInfo.getName());

			// If we decided to instrument this, mark it as instrumented in the ids
			Agent.classIDs.markClassnameAsInstrumented(classname);

			// If the class has not been updated, we used the preinstrumented version, if there is one.
			if(!Agent.classIDs.classHasBeenUpdated(classname)) {
				byte[] preinstrumentedClassfile = getCachedVersionOf(classname);
				if(preinstrumentedClassfile != null) {
					debug("Recording " + classname);
					return preinstrumentedClassfile;
				}
			}

			System.err.print("whyline >\tInstrumenting " + classname + "...");
	        		        
	        long beforeWriting = System.currentTimeMillis();

			if(DEBUG_INSTRUMENTATION)
				debug("Done parsing " + classname);
	        		        
	        int nextInstructionID = 0;
	        int instructionsInstrumented = 0;
	        int totalInstructions = 0;
	        int methodCount = 0;

	        long beforeMethods = System.currentTimeMillis();

	        ClassInstrumentationInfo instrumentationData = new ClassInstrumentationInfo(classfile.getConstantPool());
		        
	        // Instrument each method in the classfile
	        for(MethodInfo method : classfile.getDeclaredMethods()) {
	        	
	        	if(DEBUG_BEFORE_AND_AFTER) debug("Before\n" + method.getCode());
	        	
	    		if(DEBUG_INSTRUMENTATION) {
	    			debug("\tAnalyzing " + method.getClassfile().getInternalName() + " " + method.getInternalName() + method.getDescriptor());
	    		}

	    		// If this method has no code, skip it. Otherwise, instrument it.
	    		if(method.getCode() != null) {

	    			int numberOfInstructionsBeforeInstrumentation = method.getCode().getNumberOfInstructions();
	    			
		    		MethodInstrumenter methodRefs = new MethodInstrumenter(instrumentationData, method, nextInstructionID);

		    		try {
		    			methodRefs.instrument();
		        	} 
		    		// If there's a specification violation, then we don't 
		    		catch(JavaSpecificationViolation e) {
	
		    			e.printStackTrace();
	
		    			// We weren't able to instrument it! Mark the method as not instrumented.
		    			Agent.classIDs.markMethodAsNotInstrumented(classname, method.getMethodNameAndDescriptor());
		    			
		    		}

	    			// Keep track of instrumentation statistics...
		    		instructionsInstrumented += methodRefs.getNumberOfInstructionsInstrumented();
		        	methodCount++;
		        	totalInstructions += method.getCode().getNumberOfInstructions();

		        	nextInstructionID += numberOfInstructionsBeforeInstrumentation;
		        	
	    		}

	    		if(DEBUG_BEFORE_AND_AFTER) debug("After\n" + method.getCode());

	        }
	        
	        long afterMethods = System.currentTimeMillis();

	        long beforeToArray = System.currentTimeMillis();
	        
			// Finally, we convert the structured representation of the classfile back to a byte array, 
	        // which the JVM will then use to define (or redefine) the class.
        	byte[] code = classfile.toByteArray();

    		cache(classname, code);

    		System.err.println("cached");
    		
	        long afterToArray = System.currentTimeMillis();
	        
        	long afterTime = System.currentTimeMillis();

        	profiles.add(
        		new InstrumentationProfile(classname, 
        				(afterTime - beforeTime) / 1000.0, 
        				(beforeWriting - beforeTime) / 1000.0,
        				(beforeMethods - beforeWriting) / 1000.0,
        				(afterMethods - beforeMethods) / 1000.0, 
        				(afterToArray - beforeToArray) / 1000.0, 
        				totalInstructions, instructionsInstrumented, methodCount));

        	return code;

		} 
		catch (Exception e) {

			debug("Can't finish instrumenting " + classname + " because an exception was raised during instrumentation:\n\n");
			e.printStackTrace();
			Tracer.shutdown();
			System.exit(Whyline.WHYLINE_FAILURE_EXIT_CODE);
			
		}
		
		// If we failed for some reason, return no change.
		return null;

	}	
	
	public static void printInstrumentationStatistics() {
		
		debug("Instrumentation statistics, in the order that classes were loaded . . .");

		String header = InstrumentationProfile.getHeader();
		debug(header);
		
		debug(Util.fillString('-', header.length()));
		
		double totalTime = 0;
		int totalInstructionsInstrumented = 0, totalMethods = 0, totalInstructions = 0;
		
		for(InstrumentationProfile profile : profiles) {
			
			debug("" + profile);
			if(!profile.skipped()) {
				totalTime += profile.totalSeconds;
				totalInstructionsInstrumented += profile.numberOfInstructionsInstrumented;
				totalMethods += profile.numberOfMethodsInstrumented;
				totalInstructions += profile.totalInstructions;
			}
			
		}

		debug(Util.fillString('-', header.length()));

		debug(
				Util.fillOrTruncateString("Instrumented " + totalNumberOfInstrumentedClasses + " classes", 40) + "   " +
				Util.fillOrTruncateString(""+ totalTime, 10) + "   " +
				Util.fillOrTruncateString("", 10) + "   " +
				Util.fillOrTruncateString("", 10) + "   " +
				Util.fillOrTruncateString("" + totalMethods, 10) + "   " +
				Util.fillOrTruncateString("" + totalInstructionsInstrumented, 10) + "   " +
				Util.fillOrTruncateString("" + (int)(100.0 * totalInstructionsInstrumented / totalInstructions) + "% instrumented", 10));

		debug(Util.fillString('-', header.length()));

		debug("Instructions per second = " + (totalInstructionsInstrumented / totalTime));
		debug("Reading time = " + totalReadingTime);
		debug("Instrumentation time = " + totalInstrumentationTime);
		debug("Writing time = " + totalWritingTime);
		debug("To array time = " + totalConversionTime);
		debug("");
		
	}
	
	public static final List<InstrumentationProfile> profiles = new ArrayList<InstrumentationProfile>(100);
	public static double totalInstrumentationTime = 0, totalReadingTime = 0, totalWritingTime = 0, totalConversionTime = 0, totalNumberOfInstrumentedClasses = 0;

}


/**
 * @author Andrew J. Ko
 *
 * Stores a bit of empirical data about the classes instrumented.
 */
class InstrumentationProfile {
	
	public final QualifiedClassName className;
	public final boolean instrumented;
	public final double totalSeconds, readingTime, writingTime, instrumentationTime, conversionTime;
	public final int numberOfInstructionsInstrumented;
	public final int totalInstructions;
	public final int numberOfMethodsInstrumented;
	
	public InstrumentationProfile(QualifiedClassName className, double totalTime, double readingTime, double writingTime, double instrumentingTime, double toArrayTime, int totalInstructions, int instructionInstrumented, int methodCount) {

		this.instrumented = true;
		this.className = className;
		this.totalSeconds = totalTime;
		this.readingTime = readingTime;
		this.writingTime = writingTime;
		this.instrumentationTime = instrumentingTime;
		this.conversionTime = toArrayTime;
		this.totalInstructions = totalInstructions;
		this.numberOfInstructionsInstrumented = instructionInstrumented;
		this.numberOfMethodsInstrumented = methodCount;

		if(instrumented) ClassInstrumenter.totalNumberOfInstrumentedClasses++;
		ClassInstrumenter.totalInstrumentationTime += instrumentingTime;
		ClassInstrumenter.totalReadingTime += readingTime;
		ClassInstrumenter.totalWritingTime += writingTime;
		ClassInstrumenter.totalConversionTime += conversionTime;
		
	}
	
	public InstrumentationProfile(QualifiedClassName className) {
		
		this.instrumented = false;
		this.className = className;
		this.totalSeconds = 0;
		this.instrumentationTime = 0;
		this.readingTime = 0;
		this.writingTime = 0;
		this.conversionTime = 0;
		this.totalInstructions = 0;
		this.numberOfInstructionsInstrumented = 0;
		this.numberOfMethodsInstrumented = 0;
		
	}
	
	public boolean skipped() { return !instrumented; }
	
	public String toString() {
		
		return 
			Util.fillOrTruncateString(className.getText(), 40) + "   " + 
			Util.fillOrTruncateString(instrumented ? "" + totalSeconds : "-", 10) + "   " +
			Util.fillOrTruncateString(instrumented ? "" + (readingTime + " (" + (readingTime / totalSeconds) + ")") : "-", 15) + "   " +
			Util.fillOrTruncateString(instrumented ? "" + (writingTime + " (" + (writingTime / totalSeconds) + ")") : "-", 15) + "   " +
			Util.fillOrTruncateString(instrumented ? "" + (instrumentationTime + " (" + (instrumentationTime / totalSeconds) + ")") : "-", 15) + "   " +
			Util.fillOrTruncateString(instrumented ? "" + conversionTime : "-", 10) + "   " +
			Util.fillOrTruncateString(instrumented ? "" + numberOfMethodsInstrumented : "-", 10) + "   " + 
			Util.fillOrTruncateString(instrumented ? "" + totalInstructions : "-", 10) + "   " +
			Util.fillOrTruncateString(instrumented ? "" + numberOfInstructionsInstrumented : "-", 10);
		
	}
	
	public static String getHeader() {
		
		return 
		Util.fillOrTruncateString("Class", 40) + "   " +
		Util.fillOrTruncateString("Total time", 10) + "   " +
		Util.fillOrTruncateString("Reading time", 15) + "   " +
		Util.fillOrTruncateString("Writing time", 15) + "   " +
		Util.fillOrTruncateString("Instrument time", 15) + "   " +
		Util.fillOrTruncateString("Conversion time", 10) + "   " +
		Util.fillOrTruncateString("Method count", 10) + "   " +
		Util.fillOrTruncateString("# instructions", 10) + "   " +
		Util.fillOrTruncateString("# instrumented", 10);
			

	}
	
}
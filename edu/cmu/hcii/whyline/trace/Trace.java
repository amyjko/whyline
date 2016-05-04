package edu.cmu.hcii.whyline.trace;

import java.io.*;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import edu.cmu.hcii.whyline.*;
import edu.cmu.hcii.whyline.analysis.*;
import edu.cmu.hcii.whyline.bytecode.*;
import edu.cmu.hcii.whyline.bytecode.StackDependencies.Consumers;
import edu.cmu.hcii.whyline.io.*;
import edu.cmu.hcii.whyline.qa.Explanation;
import edu.cmu.hcii.whyline.source.*;
import edu.cmu.hcii.whyline.trace.nodes.*;
import edu.cmu.hcii.whyline.tracing.*;
import edu.cmu.hcii.whyline.util.*;
import edu.cmu.hcii.whyline.util.Util.ProgressListener;
import gnu.trove.*;

import static edu.cmu.hcii.whyline.trace.EventKind.*;

/**
 * Represents a Whyline recording.
 *  
 * @author Andrew J. Ko
 *
 */ 
public final class Trace {

	//////////////////////////////////////////////////////////////////////////////////////////
	// GLOBAL PROPERTIES
	//////////////////////////////////////////////////////////////////////////////////////////
	
	private static final boolean PRINT_PERF = false;
	
	// Performance parameters to tweak
	private static final int EVENTS_PER_BLOCK = 4096; 
	private static final int STACK_DEPENDENCIES_CACHE_SIZE = 8192;
	private static final int BYTES_PER_EVENT = 7;
	private static final double FRACTION_OF_MEMORY_FOR_BLOCKS = .25;
	private static final int THREAD_ID_CACHE_SIZE = Short.MAX_VALUE;

	//////////////////////////////////////////////////////////////////////////////////////////
	// LOCATIONS
	//////////////////////////////////////////////////////////////////////////////////////////
	
	private final File TRACE_FOLDER;

	private final File IDS_FOLDER;
	private final File CALLS_FOLDER;
	private final File VALUES_FOLDER;

	private final File SERIAL_THREAD_TRACES_FOLDER;
	private final File SOURCE_FOLDER;
	private final File CLASSNAMES, IMMUTABLES, META, CLASSIDS, OBJECTTYPES;
	
	private final File CALL_GRAPH;
	private final boolean callGraphIsCached;

	private final File OUTPUT;
	private final boolean outputIsCached;
	
	//////////////////////////////////////////////////////////////////////////////////////////
	// LOADING
	//////////////////////////////////////////////////////////////////////////////////////////

	private boolean doneLoading = false;
	private boolean isLoadingSerial = true;
	private boolean canceled = false;

	//////////////////////////////////////////////////////////////////////////////////////////
	// MISCELLANEOUS TRACE DATA
	//////////////////////////////////////////////////////////////////////////////////////////

	private TraceMetaData metadata;

	//////////////////////////////////////////////////////////////////////////////////////////
	// REPRESENTATIONS OF STATIC TRACE DATA
	//////////////////////////////////////////////////////////////////////////////////////////
	
	private final ClassIDs classIDs;

	private final Map<QualifiedClassName,Classfile> classesByName = new HashMap<QualifiedClassName,Classfile>();
	private TIntObjectHashMap<Classfile> classfilesByID;
	
	private final HashSet<String> familiarMethods = new HashSet<String>();
	private final HashSet<String> familiarFields = new HashSet<String>();
	private final HashSet<QualifiedClassName> familiarClasses = new HashSet<QualifiedClassName>();
	private final SortedSet<String> userSourceFiles = new TreeSet<String>();

	private final Map<String, MethodInfo[]> methodsByQualifiedSignature = new HashMap<String,MethodInfo[]>();
	private final Map<String,JavaSourceFile> sourceByQualifiedName = new TreeMap<String,JavaSourceFile>();
	private final Map<String,Set<Classfile>> classesWaitingForSourceByQualifiedSourceFileName = new HashMap<String,Set<Classfile>>();

	//////////////////////////////////////////////////////////////////////////////////////////
	// REPRESENTATIONS OF DYNAMIC TRACE DATA
	//////////////////////////////////////////////////////////////////////////////////////////

	private ThreadTrace[] threads;
	
	private Blocks<IDBlock> idBlocks;
	private Blocks<ValueBlock> valueBlocks;
	private Blocks<CallsBlock> callBlocks;
	
	private final TLongObjectHashMap<ImageData> imageData = new TLongObjectHashMap<ImageData>();
	private final TIntObjectHashMap<KeyArguments> keyArguments = new TIntObjectHashMap<KeyArguments>();
	private final TIntObjectHashMap<MouseArguments> mouseArguments = new TIntObjectHashMap<MouseArguments>();
	private final TIntObjectHashMap<RepaintArguments> repaintArguments = new TIntObjectHashMap<RepaintArguments>();
	private final TIntObjectHashMap<CreateGraphicsArguments> createGraphicsArguments = new TIntObjectHashMap<CreateGraphicsArguments>();

	private final TLongObjectHashMap<Object> immutablesByID = new TLongObjectHashMap<Object>(10000);
	private final TObjectLongHashMap<Object> idsOfImmutables = new TObjectLongHashMap<Object>(10000);
	private TLongIntHashMap objectTypes;

	private final ExceptionHistory exceptionHistory = new ExceptionHistory(this);
	private final StaticVariableAssignmentHistory staticAssignmentHistory = new StaticVariableAssignmentHistory(this);
	private final FieldAssignmentHistory fieldAssignmentHistory = new FieldAssignmentHistory(this);
	private final ArrayHistory arrayHistory = new ArrayHistory(this);
	private InstantiationHistory instantiationHistory;
	private final ClassInitializationHistory initializationHistory = new ClassInitializationHistory(this); 
	private final ThreadStartHistory runHistory = new ThreadStartHistory(this);
	private InvocationHistory invocationHistory;
	
	//////////////////////////////////////////////////////////////////////////////////////////
	// DERIVED DATA
	//////////////////////////////////////////////////////////////////////////////////////////

	private final TLongObjectHashMap<ObjectState> objects = new TLongObjectHashMap<ObjectState>(); 
	private final TLongObjectHashMap<Map<String,FieldState>> fields = new TLongObjectHashMap<Map<String,FieldState>>(); 

	private final ArrayList<Instruction> textualOutput = new ArrayList<Instruction>(1000);
	private final ArrayList<Instruction> graphicalOutput = new ArrayList<Instruction>(1000);
	private SortedSet<Classfile> windowClasses;
	
	private Map<Classfile,Map<MethodInfo,Set<Instruction>>> textualOutputByMethodByClass = new HashMap<Classfile,Map<MethodInfo,Set<Instruction>>>();
	private Map<Classfile,Map<MethodInfo,Set<Instruction>>> graphicalOutputByMethodByClass = new HashMap<Classfile,Map<MethodInfo,Set<Instruction>>>();
	
	private Collection<FieldInfo> outputAffectingFields;
	private Collection<MethodInfo> outputAffectingMethods;
	private Collection<MethodInfo> outputInvokingMethods;
	private Set<Classfile> outputInvokingClasses = new HashSet<Classfile>(10);
	
	private Map<QualifiedClassName, List<Instantiation>> allocationsByClass = new HashMap<QualifiedClassName, List<Instantiation>>(1000);
	private ArrayList<Invoke> invocations = new ArrayList<Invoke>(10000);

	private int numberOfMethods = 0, numberOfFields = 0, numberOfInvocationInstructions = 0, numberOfInstructions = 0;

	private IOHistory<IOEvent> ioHistory = new IOHistory<IOEvent>(null, this);
	private IOHistory<InputEvent> inputHistory = new IOHistory<InputEvent>(ioHistory, this);
	private IOHistory<OutputEvent> outputHistory = new IOHistory<OutputEvent>(ioHistory, this);
	private IOHistory<GraphicalOutputEvent> graphicsHistory = new IOHistory<GraphicalOutputEvent>(outputHistory, this);
	private IOHistory<RenderEvent> renderHistory = new IOHistory<RenderEvent>(graphicsHistory, this);
	private IOHistory<TextualOutputEvent> printsHistory = new IOHistory<TextualOutputEvent>(outputHistory, this);
	private IOHistory<MouseStateInputEvent> mouseHistory = new IOHistory<MouseStateInputEvent>(inputHistory, this);
	private IOHistory<KeyStateInputEvent> keyHistory = new IOHistory<KeyStateInputEvent>(inputHistory, this);
	private IOHistory<WindowVisibilityOutputEvent> windowHistory = new IOHistory<WindowVisibilityOutputEvent>(outputHistory, this);
	private List<WindowState> windows = new ArrayList<WindowState>(3);

	//////////////////////////////////////////////////////////////////////////////////////////
	// ANALYSIS CACHES
	//////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Control dependencies. A hash of eventIDs by eventIDs. Give an eventID and get the eventID representing its dynamic control dependency.
	 * This is cached and determined on demand. 
	 */
	private final TIntIntHashMap controlIDs = new TIntIntHashMap();

	/**
	 * This is a list of arrays of argument producers for each event in the trace.
	 * The argument producers are ordered by the order in which they appear if the stack we're 
	 * written left to write. Argument 0 is the last item popped off the stack, the last argument
	 * in the list is the first popped off the stack. This mimics the order of the list of argument producers 
	 * noted in by the corresponding Instruction.
	 */
	private final TIntObjectHashMap<Value[]> arguments = new TIntObjectHashMap<Value[]>(1000);
	
	private IntegerVector[] previousLocalAssignmentCache = null;
	private int previousLocalAssignmentCacheStartID = -1;
	private int lastEventIDCachedForLocalAssignment = -1;

	private TIntIntHashMap threadIDCache = new TIntIntHashMap(1024);

	/**
	 * Ensures that only a fixed number of stack dependencies are held in memory. 
	 */
	private final StackDependenciesCache stackDependenciesCache = new StackDependenciesCache() {
		private final LinkedList<MethodInfo> recent = new LinkedList<MethodInfo>();
		private final HashMap<MethodInfo,StackDependencies> cache = new HashMap<MethodInfo,StackDependencies>(STACK_DEPENDENCIES_CACHE_SIZE); 
		public synchronized StackDependencies getStackDependenciesFor(MethodInfo method) throws AnalysisException {
			if(recent.size() > STACK_DEPENDENCIES_CACHE_SIZE) {
				MethodInfo first = recent.getFirst();
				if(first != method) cache.remove(recent.removeFirst());
			}
			recent.add(method);
			StackDependencies dependencies = cache.get(method);
			if(dependencies == null && method.getCode() != null) {
				dependencies = new StackDependencies(method.getCode());
				cache.put(method, dependencies);
				dependencies.analyze();
			}
			return dependencies;
		}
	};

	//////////////////////////////////////////////////////////////////////////////////////////
	// MISCELLANEOUS
	//////////////////////////////////////////////////////////////////////////////////////////

	private final TraceListener listener;
	private final SortedSet<Explanation> narrative = new TreeSet<Explanation>();	

		
	/**
	 * The trace listener is notified as the trace loads. It should provide proper feedback to the user as loading occurs.
	 * 
	 * @param listener The object to tell trace loading events about.
	 * 
	 * @param traceDirectory
	 * @throws Exception 
	 */
	public Trace(TraceListener listener, File traceDirectory) throws IOException {
		
		this.listener = listener;
		
		TRACE_FOLDER = traceDirectory;

		IDS_FOLDER = new File(TRACE_FOLDER, Whyline.IDS_PATH);
		IDS_FOLDER.mkdirs();
		CALLS_FOLDER = new File(TRACE_FOLDER, Whyline.CALLS_PATH);
		CALLS_FOLDER.mkdirs();
		VALUES_FOLDER = new File(TRACE_FOLDER, Whyline.VALUES_PATH);
		VALUES_FOLDER.mkdirs();
		
		SERIAL_THREAD_TRACES_FOLDER = new File(traceDirectory, Whyline.SERIAL_PATH);

		SOURCE_FOLDER = new File(TRACE_FOLDER, Whyline.SOURCE_PATH);
		
		CLASSNAMES = new File(TRACE_FOLDER, Whyline.CLASSNAMES_PATH);
		IMMUTABLES = new File(TRACE_FOLDER, Whyline.IMMUTABLES_PATH);
		META = new File(TRACE_FOLDER, Whyline.META_PATH);
		CLASSIDS = new File(TRACE_FOLDER, Whyline.CLASSIDS_PATH);
		OBJECTTYPES = new File(TRACE_FOLDER, Whyline.OBJECT_TYPES_PATH);
		
		CALL_GRAPH = new File(getPath(), Whyline.CALL_GRAPH_PATH);
		callGraphIsCached = CALL_GRAPH.exists();

		OUTPUT = new File(getPath(), Whyline.OUTPUT_PATH);
		outputIsCached = OUTPUT.exists();

		File classids = new File(TRACE_FOLDER, Whyline.CLASSIDS_PATH);
		
		if(classids.exists())
			classIDs = new ClassIDs(classids);
		else
			throw new IOException("Couldn't find " + classids.getAbsolutePath());
		
		classfilesByID = new TIntObjectHashMap<Classfile>(classIDs.getNumberOfClasses() * 2);
		
	}

	public void addWindow(WindowState window) {
		
		windows.add(window);
		if(listener != null) listener.windowParsed(window);
		
	}
	
	public ImageData getImageData(long imageID) { return imageData.get(imageID); }
	
	public File getPath() { return TRACE_FOLDER; }
	
	public boolean isSaved() { return !TRACE_FOLDER.getAbsolutePath().equals(Whyline.getWorkingTraceFolder().getAbsolutePath()); }
	
	public int getNumberOfEvents() { return metadata == null ? 0 : metadata.getNumberOfEvents(); }

	/////////////////////////////////////////////////////
	// CLASS, method and field info
	
	public int getNumberOfClasses() { return metadata == null ? 0 : metadata.getNumberOfClasses(); }
	public Iterable<Classfile> getClasses() { return classesByName.values(); }
	public Classfile getClassfileByName(QualifiedClassName name) { return classesByName.get(name); }
	public Classfile getClassfileByID(int id) { return classfilesByID.get(id); }
	public ClassIDs getClassIDs() { return classIDs; }
	public QualifiedClassName getNameOfClassID(int id) { return classIDs.getNameOfClassID(id); }

	public List<Classfile> getClassfiles() {
		
		Classfile[] classes = new Classfile[classfilesByID.size()];
		classfilesByID.getValues(classes);
		return Collections.unmodifiableList(Arrays.<Classfile>asList(classes));
		
	}
	
	public int getNumberOfMethods() { return numberOfMethods; }
	public int getNumberOfInstructions() { return numberOfInstructions; }
	public int getNumberOfFields() { return numberOfFields; }
	public int getNumberOfInvocationInstructions() { return numberOfInvocationInstructions; }

	public boolean classOrSubclassIsReferencedInFamiliarSourceFile(QualifiedClassName classname) {
	
		Classfile cf = getClassfileByName(classname);
		assert cf != null : "Why couldn't we find the classfile for " + classname;

		if(classIsReferencedInFamiliarSourceFile(cf.getInternalName())) return true;

		for(Classfile subclass : cf.getAllSubclasses())
			if(classIsReferencedInFamiliarSourceFile(subclass.getInternalName())) 
				return true;
		
		return false;
		
	}
	
	public boolean classIsReferencedInFamiliarSourceFile(QualifiedClassName classname) { return classname.referencedInFamiliarClass(); }
	public boolean methodIsReferencedInFamiliarSourceFile(String fullyQualifiedMethodName) { return familiarMethods.contains(fullyQualifiedMethodName); }
	public boolean fieldIsReferencedInFamiliarSourceFile(String fullyQualifiedFieldName) { return familiarFields.contains(fullyQualifiedFieldName); }

	public MethodInfo resolveMethodReference(QualifiedClassName classname, Invoke invoke) {
		
		Classfile classfile = getClassfileByName(invoke.getMethodInvoked().getClassName());

		MethodInfo method = null;
		MethodrefInfo methodref = invoke.getMethodInvoked();
		String nameAndDescriptor = methodref.getMethodNameAndDescriptor();

		// Method resolution depends on the type of invocation.
		if(classfile == null) {

			
		}
		else if(invoke instanceof INVOKEVIRTUAL) {
			
			Classfile classfileOfType = getClassfileByName(classname);

			if(classfileOfType != null) {

				// Are these types even compatible?
				if(!(classfileOfType == classfile || classfileOfType.isSubclassOf(classfile.getInternalName())))
					return null;
				
				MethodInfo methodOfType = classfileOfType.getDeclaredMethodByNameAndDescriptor(nameAndDescriptor);
				Classfile type = classfileOfType;
				while(type.getSuperclass() != null && (methodOfType == null || methodOfType.isAbstract() || !methodOfType.isAccessibleFrom(classfileOfType))) {

					type = type.getSuperclass();
					methodOfType = type.getDeclaredMethodByNameAndDescriptor(nameAndDescriptor);
					
				}

				// If we found a non abstract method and its accessible from the given type, then we found it.
				if(methodOfType != null && !methodOfType.isAbstract() && methodOfType.isAccessibleFrom(classfileOfType))
					method = methodOfType;
				
			}

		}
		else if(invoke instanceof INVOKESPECIAL) {
			
			method = classfile.getDeclaredOrInheritedMethodByNameAndDescriptor(nameAndDescriptor);
			
			// if the resolved method is protected (¤4.6),  
			if(method != null && method.isProtected()) {
			
				Classfile currentClass = invoke.getClassfile();
				Classfile superClass = invoke.getClassfile().getSuperclass();
				Classfile methodClass = method.getClassfile();
				Classfile classfileOfType = getClassfileByName(classname);

				// ... and it is either a member of the current class or a member of a superclass of the current class,
				if(methodClass == currentClass || methodClass == superClass)
					// ... then the class of object must be either the current class or a subclass of the current class.
					if(!(classfileOfType == currentClass || classfileOfType == superClass))
						method = null;

			}			
			
		}
		else if(invoke instanceof INVOKEINTERFACE) {
			
			MethodInfo methodOfClassReferenced = classfile.getDeclaredOrInheritedMethodByNameAndDescriptor(nameAndDescriptor);

			Classfile classfileOfType = getClassfileByName(classname);

			if(classfileOfType != null) {

				MethodInfo methodOfType = classfileOfType.getDeclaredMethodByNameAndDescriptor(nameAndDescriptor);
				Classfile type = classfileOfType;
				while(type.getSuperclass() != null && (methodOfType == null || methodOfType.isAbstract())) {

					type = type.getSuperclass();
					methodOfType = type.getDeclaredMethodByNameAndDescriptor(nameAndDescriptor);
					
				}

				// If we found a non abstract method and its accessible from the given type, then we found it.
				if(methodOfType != null && !methodOfType.isAbstract())
					method = methodOfType;
				
			}
			
		}
		else if(invoke instanceof INVOKESTATIC)
			method = classfile.getDeclaredMethodByNameAndDescriptor(nameAndDescriptor);
		
		if(method != null) assert !method.isAbstract() : "Should not be returning abstract method\n\n" + method.getQualifiedNameAndDescriptor() + "\n\nwhen resolving " + invoke + "\n\non " + classname;
		
		return method;
		
	}
		
	/**
	 * Returns methods, in order of overriding, from superclasses to subclasses
	 * @param methodref
	 * 
	 * @return
	 */
	public MethodInfo[] getMethodsFromReference(Invoke invoke) {

		String qualifedSignature = invoke.getMethodInvoked().getQualfiedNameAndDescriptor();
		MethodInfo[] cachedMethods = methodsByQualifiedSignature.get(qualifedSignature);
		if(cachedMethods != null) return cachedMethods;
		
		MethodrefInfo methodref = invoke.getMethodInvoked();

		Set<MethodInfo> methods; 
		
		if(invoke instanceof INVOKESTATIC) {

			methods = new HashSet<MethodInfo>(1);

			MethodInfo method = resolveMethodReference(invoke.getMethodInvoked().getClassName(), invoke);
			if(method != null)
				methods.add(method);
			
		}
		else {

			methods = new HashSet<MethodInfo>(3);

			// What name is it called on?
			QualifiedClassName classNameInvokedOn = invoke.getMethodInvoked().getClassName();

			StackDependencies.Producers producersOfInstance = invoke.getProducersOfArgument(0);

			// If the invoke's instance has no producers, its probably getting the Throwable from a catch block.
			// let's find the specific type of exception types and resolve on them.
			if(producersOfInstance.getNumberOfProducers() == 0) {

				List<ExceptionHandler> handlers = invoke.getCode().getExceptionHandlersThatExecute(invoke);

				for(ExceptionHandler handler : handlers) {
				
					MethodInfo m = resolveMethodReference(handler.getCatchType().getName(), invoke);
					if(m != null) methods.add(m);
					
				}
				
				classNameInvokedOn = null;
				
			}
			// If this invoke is called on "this", then we can be more specific about the type.
			else if(producersOfInstance.getFirstProducer() instanceof GetLocal && ((GetLocal)producersOfInstance.getFirstProducer()).getLocalID() == 0)
				classNameInvokedOn = invoke.getClassfile().getInternalName();

			Classfile classInvokedOn = classNameInvokedOn == null ? null : getClassfileByName(classNameInvokedOn);

			if(classInvokedOn != null) {

				// We'll be conservative on interface calls, because it could be anything.
				if(invoke instanceof INVOKEINTERFACE) {
					
					// This might invoke on any class that implements the specified interface.						
					for(Classfile classfile : classInvokedOn.getImplementors()) {

						// This class, or any of its subclasses may define the interface method.
						MethodInfo m = resolveMethodReference(classfile.getInternalName(), invoke);
						if(m != null) methods.add(m);

						for(Classfile subclass : classfile.getAllSubclasses()) {
							
							m = resolveMethodReference(subclass.getInternalName(), invoke);
							if(m != null) methods.add(m);
							
						}
						
					}
					
				}
				// Otherwise, for other types, we can just resolve them reliably.
				else if(invoke instanceof INVOKESPECIAL) {
					
					// The only legal classes that the class name of the invocation might be referring to are the class of the invocation, or its superclass.

					MethodInfo m = resolveMethodReference(classNameInvokedOn, invoke);
					if(m != null) methods.add(m);

					Classfile superclass = classInvokedOn.getSuperclass();
					if(superclass != null) {
						
						m = resolveMethodReference(superclass.getInternalName(), invoke);
						if(m != null) methods.add(m);
							
					}						
					
				}
				else if(invoke instanceof INVOKEVIRTUAL) {
					
					// Does this invoke invoke on "this"?
					StackDependencies.Producers producers = invoke.getProducersOfArgument(0);
					MethodInfo methodInvokedIn = invoke.getMethod();

					boolean invokedOnTHIS =
						methodInvokedIn.isVirtual() &&
						producers.getNumberOfProducers() == 1 && 
						producers.getFirstProducer() instanceof GetLocal && 
						((GetLocal)producers.getFirstProducer()).getLocalID() == 0;
					
					// What classes might this be? It could be the named class or any of its subclasses.
					MethodInfo m = resolveMethodReference(classNameInvokedOn, invoke);
					if(m != null) methods.add(m);
					
					for(Classfile subclass : classInvokedOn.getAllSubclasses()) {

						m = resolveMethodReference(subclass.getInternalName(), invoke);

						// If this invoke invokes on this, does this subclass override the method invoked in without calling super on it?
						if(invokedOnTHIS) {
							
							MethodInfo overridingMethod = subclass.getDeclaredOrInheritedMethodByNameAndDescriptor(methodInvokedIn.getMethodNameAndDescriptor());
							if(overridingMethod != null && overridingMethod != methodInvokedIn && !overridingMethod.callsSuper())
								m = null;
							
						}
						
						if(m != null)
							methods.add(m);
						
					}
					
				}
			
			}
			
		}
		
		cachedMethods = new MethodInfo[methods.size()];
		methods.toArray(cachedMethods);
		methodsByQualifiedSignature.put(qualifedSignature, cachedMethods);
		
		return cachedMethods;
		
	}
	
	private void gatherOverridenMethods(Classfile classToCheck, Vector<MethodInfo> methods, String methodNameAndDescriptor) {
		
		// Look in all subclass and implementors
		for(Classfile subclass : classToCheck.getDirectSubclasses()) {
		
			MethodInfo match = subclass.getDeclaredMethodByNameAndDescriptor(methodNameAndDescriptor);
			if(match != null) methods.add(match);
			gatherOverridenMethods(subclass, methods, methodNameAndDescriptor);
			
		}

		for(Classfile implementor : classToCheck.getImplementors()) {
			
			MethodInfo match = implementor.getDeclaredMethodByNameAndDescriptor(methodNameAndDescriptor);
			if(match != null) methods.add(match);
			gatherOverridenMethods(implementor, methods, methodNameAndDescriptor);
						
		}
		
	}
	
	public FieldInfo resolveFieldReference(FieldrefInfo fieldref) {

		Classfile classfile = getClassfileByName(fieldref.getClassname());
		if(classfile == null) return null;
		
		FieldInfo field = classfile.getFieldByName(fieldref.getName());
		return field;
		
	}
	
	/////////////////////////////////////////////////////
	// THREADS 

	public MethodInfo getMain() { return metadata.getMain(this); }
	public Iterable<String> getMainArguments() { return metadata.getMainArguments(); }
	
	public int getMainStartEventID() { 
	
		IntegerVector mainIDs = invocationHistory.getStartIDsAfterEventID(getMain(), -1);
		return mainIDs.isEmpty() ? -1 : mainIDs.get(0);
		
	}

	public int getNumberOfThreads() { return metadata.getNumberOfThreads(); }

	public int getNumberOfEventsInThread(int threadID) { return threads[threadID].metadata.numberOfEventsInThread; }
		
	public String getThreadName(int threadID) {

		if(threadID < 0 || threadID >= threads.length)
			throw new RuntimeException("Invalid threadID " + threadID + "; should be >= 0 and < " + threads.length);
		
		return threads[threadID].getName();

	}
	
	private ThreadTrace getThreadLoaderFor(int eventID) { return threads[getThreadID(eventID)]; }
	
	public CallStack getCallStack(int eventID) { return new CallStack(this, eventID); }
	
	/////////////////////////////////////////////////////
	// SOURCEFILES

	/**
	 * Returns all user source files and all other loaded library source files.
	 */
	public Iterable<JavaSourceFile> getAllSourceFiles() { 

		// Coerce the loading of all known source files.
		for(String name : userSourceFiles)
			getSourceByQualifiedName(name);
		
		return sourceByQualifiedName.values(); 
		
	}

	public JavaSourceFile getSourceFor(Classfile c) { return c.hasSourceFileAttribute() ? getSourceByQualifiedName(c.getQualifiedSourceFileName()) : null; }
	
	public JavaSourceFile getFamiliarSourceByQualifiedName(String name) { 
		
		return getSourceByQualifiedName(name, false);
		
	}

	public JavaSourceFile getSourceByQualifiedName(String name) { 

		return getSourceByQualifiedName(name, true);
		
	}

	public boolean hasNonJDKSourceFor(Classfile c) {
		
		return userSourceFiles.contains(c.getQualifiedSourceFileName());
		
	}
	
	/**
	 * Searches the program source and JDK
	 */
	 private JavaSourceFile getSourceByQualifiedName(String name, boolean searchJDK) { 
		
		if(name == null) return null;
		
		JavaSourceFile source = sourceByQualifiedName.get(name);
		
		if(source == null) {

			File sourceFile = new File(SOURCE_FOLDER.getAbsolutePath(), name.replace('/', File.separatorChar));
			if(sourceFile.exists()) {

				try {

					DataInputStream data = Util.getReaderFor(sourceFile);
					byte[] bytes = new byte[(int)sourceFile.length()];
					data.readFully(bytes);
					data.close();
					source = new JavaSourceFile(name, bytes, true);
				
				} catch (IOException e) {}

			}

			// If we didn't find it in the trace, try the JDK
			if(source == null && searchJDK)
				source = JDKSource.getSourceForQualifiedName(name);

			if(source != null) {

				sourceByQualifiedName.put(name, source);

				// Associate all of the class files that refer to this source.
				Set<Classfile> unassociatedClasses = classesWaitingForSourceByQualifiedSourceFileName.get(name);
				if(unassociatedClasses != null) {
					for(Classfile c : unassociatedClasses)
						source.linkClassfile(c);
					classesWaitingForSourceByQualifiedSourceFileName.remove(name);
				}				

				listener.additionalSourceLoaded(source);
				
			}

		}
		return source;
		
	}

	public boolean hasUserSourceFileFor(Classfile cf) { return cf.hasSourceFileAttribute() && userSourceFiles.contains(cf.getQualifiedSourceFileName()); }

	public int getNumberOfUserSourceFiles() { return userSourceFiles.size(); }

	/////////////////////////////////////////////////////
	// IO

	public IOHistory<GraphicalOutputEvent> getGraphicsHistory() { return graphicsHistory; }
	public IOHistory<RenderEvent> getRenderHistory() { return renderHistory; }
	public IOHistory<TextualOutputEvent> getPrintHistory() { return printsHistory; }
	public IOHistory<MouseStateInputEvent> getMouseHistory() { return mouseHistory; }
	public IOHistory<KeyStateInputEvent> getKeyHistory() { return keyHistory; }
	public IOHistory<IOEvent> getIOHistory() { return ioHistory; }
	public IOHistory<InputEvent> getInputHistory() { return inputHistory; }
	public IOHistory<OutputEvent> getOutputHistory() { return outputHistory; }

	public boolean hasTextualOutputEvents() { return getPrintHistory().getNumberOfEvents() > 0; }
	public boolean hasGraphicalOutputEvents() { return getGraphicsHistory().getNumberOfEvents() > 0; }

	public boolean hasTextualOutputInstructions() { return textualOutput.size() > 0; }
	public boolean hasGraphicalOutputInstructions() { return graphicalOutput.size() > 0; }

	public Map<MethodInfo,Set<Instruction>> getTextualOutputByMethodForClass(Classfile classfile) { return textualOutputByMethodByClass.get(classfile); }
	public Map<MethodInfo,Set<Instruction>> getGraphicalOutputByMethodForClass(Classfile classfile) { return graphicalOutputByMethodByClass.get(classfile); }

	public List<Instruction> getTextualOutputInstructions() { return textualOutput; }
	public List<Instruction> getGraphicalOutputInstructions() { return graphicalOutput; }

	public Collection<FieldInfo> getOutputAffectingFields() { return outputAffectingFields; }	
	public Collection<MethodInfo> getOutputAffectingMethods() { return outputAffectingMethods; }
	public Collection<MethodInfo> getOutputInvokingMethods() { return outputInvokingMethods; }
	public Collection<Classfile> getOutputInvokingClasses() { return outputInvokingClasses; }
	
	public Collection<Instruction> getTextualOutputInvokingInstructions() {
		
		Map<Instruction,Instruction> outputByFinalConsumer = new HashMap<Instruction,Instruction>(textualOutput.size());
		
		// Sort the text output by final consumer, so we only have one question per statement.
		for(Instruction textOutput : getTextualOutputInstructions()) {
	
			Instruction finalConsumer = textOutput.getFinalConsumer();
			if(finalConsumer != null && finalConsumer instanceof INVOKEVIRTUAL) {

				Instruction firstTextConsumed = outputByFinalConsumer.get(finalConsumer);
				if(firstTextConsumed == null || firstTextConsumed.getIndex() < textOutput.getIndex())
					outputByFinalConsumer.put(finalConsumer, textOutput);
					
			}
			
		}

		return outputByFinalConsumer.values();
		
	}
	
	public SortedSet<FieldInfo> getPublicOutputAffectingFieldsOf(Classfile classfile) {
		
		TreeSet<FieldInfo> publicOutputAffectingFields = new TreeSet<FieldInfo>();
		
		if(classfile != null)
			for(FieldInfo field : classfile.getAllInstanceFields())
				if(outputAffectingFields.contains(field) && (field.isPublic() || !field.getSetters().isEmpty()))
					publicOutputAffectingFields.add(field);
	
		return publicOutputAffectingFields;
	
	}

	public SortedSet<MethodInfo> getPublicOutputInvokingMethodsOf(Classfile classfile) {
		
		TreeSet<MethodInfo> methods = new TreeSet<MethodInfo>();
		
		// All public methods, except those that are setters for fields we've already added.
		for(MethodInfo method : classfile.getAllMethods())  {
			
			boolean isPublic = method.isPublic();

			if(methodInvokesOutput(method) && isPublic)
				methods.add(method);
			
		}
		
		return methods;

	}

	public SortedSet<Classfile> getConcreteWindowClasses() {
		
		if(windowClasses == null) {

			windowClasses = new TreeSet<Classfile>();
			QualifiedClassName frame = QualifiedClassName.get("java/awt/Window");
	
			for(Classfile c : getClassfiles()) {
				if(c.isSubclassOf(frame) && !c.isAbstract())
					windowClasses.add(c);
			}
			
		}
		return Collections.<Classfile>unmodifiableSortedSet(windowClasses);
		
	}
	
	public boolean classInvokesOutput(Classfile classfile) { 
		
		return outputInvokingClasses.contains(classfile); 
		
	}
	
	private boolean methodInvokesOutput(MethodInfo method) {
		
		return 
			outputInvokingMethods.contains(method) || 
			(method.getMethodOverriden() == null ? false : methodInvokesOutput(method.getMethodOverriden()));
		
	}
		
	/////////////////////////////////////////////////////
	// OBJECTS

	public Object getImmutableObject(long objectID) { return immutablesByID.get(objectID); }	
	
	/////////////////////////////////////////////////////
	// TRACE

	
	
	/////////////////////////////////////////////////////
	// LOADING
	/////////////////////////////////////////////////////
	
	public boolean isDoneLoading() { return doneLoading; }
	
	public void cancelLoading() { canceled = true; }
	
	public void load(int msPerNotification) throws IOException {
		
		(new Loader(msPerNotification)).start();

	}
				
	public List<Invoke> getInvocations() {
		
		return invocations;
		
	}
	
	public List<Instantiation> getInstantiationsOf(QualifiedClassName classname) { 
		
		List<Instantiation> allocations = allocationsByClass.get(classname);
		if(allocations == null) {
			allocations = new ArrayList<Instantiation>(0);
			allocationsByClass.put(classname, allocations);
		}
		return allocations;
		
	}
	
	// Returns true if the trace contains classes with instantiations of the given class, one of its subclasses, or one of its implementors.
	public boolean userCodeContainsInstantiationsOf(QualifiedClassName classname) {
		
		List<Instantiation> allocations = getInstantiationsOf(classname);
		
		for(Instantiation a : allocations) {
		
			if(a.getClassfile().getInternalName().referencedInFamiliarClass())
				return true;
			
		}
			
		Classfile c = getClassfileByName(classname);
		
		if(c == null) return false;
		
		for(Classfile sub : c.getDirectSubclasses())
			if(userCodeContainsInstantiationsOf(sub.getInternalName()))
				return true;

		for(Classfile implementor : c.getImplementors())
			if(userCodeContainsInstantiationsOf(implementor.getInternalName()))
				return true;

		return false;
		
	}		

	private IDBlock getIDBlock(int eventID) { return idBlocks.getBlockContaining(eventID); }
	private ValueBlock getValueBlock(int eventID) { return valueBlocks.getBlockContaining(eventID); }
	private CallsBlock getCallsBlock(int eventID) { return callBlocks.getBlockContaining(eventID); }

	/**
	 * Returns a global ID for this instruction, comprised of the instruction's global class ID and its instruction ID (index in the class, as opposed to index in the method).
	 */
	public int getInstructionIDFor(Instruction inst) {
		
		int classID = classIDs.getIDOfClassname(inst.getClassfile().getInternalName());
		int instructionID = inst.getMethod().getFirstInstructionID() + inst.getIndex();
		return (classID << MethodInstrumenter.INSTRUCTION_ID_BIT_SIZE) | instructionID;
		
	}

	public Instruction getInstruction(int eventID) {
		
		return eventID < 0 ? null : getInstructionWithID(getInstructionID(eventID));
		
	}
	
	public Instruction getInstructionWithID(int classAndInstructionID) { 

		int classID = classAndInstructionID >>> MethodInstrumenter.INSTRUCTION_ID_BIT_SIZE;
		int instructionID = (classAndInstructionID << MethodInstrumenter.CLASS_ID_BIT_SIZE) >>> MethodInstrumenter.CLASS_ID_BIT_SIZE;
		Classfile classfile = getClassfileByID(classID);
		return classfile == null ? null : classfile.getInstructionByID(instructionID);
		
	}
	
	private int getDefinitionOfLocalIDBefore(int eventID, int localID) {
		
		int startID = getStartID(eventID);
		
		assert startID >= 0 : "Why couldn't we find a start ID for " + eventToString(eventID);

		boolean cached = startID == previousLocalAssignmentCacheStartID;
				
		if(cached && eventID < lastEventIDCachedForLocalAssignment) {
	
			IntegerVector assignmentIDs = previousLocalAssignmentCache[localID];
			return assignmentIDs.getLargestValueLessThanOrEqualTo(eventID);
			
		}
		else {
			
			previousLocalAssignmentCacheStartID = startID;
			
			Instruction inst = getInstruction(eventID);
			MethodInfo method = inst.getMethod();
			
			// If we're continuing to fill a cache
			IntegerVector[] assignmentIDsByLocalID = 
				cached ?
					previousLocalAssignmentCache :
					new IntegerVector[method.getCode().getMaxLocals()];

			if(lastEventIDCachedForLocalAssignment < 0) lastEventIDCachedForLocalAssignment = startID; 
			// Build the local assignment history for this method.
			ThreadIterator iterator = new ThreadIterator(cached ? lastEventIDCachedForLocalAssignment : startID);
			while(iterator.hasNextInMethod()) {
				int nextID = iterator.nextInMethod();
				lastEventIDCachedForLocalAssignment = nextID;
				EventKind kind = getKind(nextID);
				int localIDSet = -1;
				if(kind == SETLOCAL || kind == IINC) localIDSet = ((SetLocal)getInstruction(nextID)).getLocalID();
				else if(kind.isArgument) localIDSet = getArgumentLocalIDSet(nextID);
				if(localIDSet >= 0) {
					
					IntegerVector assignmentIDs = assignmentIDsByLocalID[localIDSet];
					if(assignmentIDs == null) {
						assignmentIDs = new IntegerVector(2);
						assignmentIDsByLocalID[localIDSet] = assignmentIDs;
					}
					assignmentIDs.append(nextID);
					
				}
			}
			
			previousLocalAssignmentCache = assignmentIDsByLocalID;
			return assignmentIDsByLocalID[localID].getLargestValueLessThanOrEqualTo(eventID);
			
		}
		
	}
	
	/**
	 * Searches the trace for stack value produced for the given event/argument pair. For example, if the event was the execution of a
	 * method invocation instruction, getStacValue(--, 0) would return the stack value produced for argument 0 of the invocation.
	 */
	public Value getOperandStackValue(int eventID, int argument) { 
		
		Value[] productionEvents = arguments.get(eventID);
		// Create a container to hold these results for future callers.
		if(productionEvents == null) {
			productionEvents = new Value[getInstruction(eventID).getNumberOfArgumentProducers()];
			arguments.put(eventID, productionEvents);
		}
		assert argument < productionEvents.length : "Sent illegal argument " + argument + " to  instruction with " + getInstruction(eventID).getNumberOfArgumentProducers() + " arguments:\n\n" + getInstruction(eventID);
		if(productionEvents[argument] == null) {
			Value value = getOperandStackValueHelper(eventID, argument);
			productionEvents[argument] = value;
		}
		
		return productionEvents[argument];
		
	}

	/**
	 * Given a producer and consumer, determines the distance between the two instruction's events in the event stream, 
	 * excluding intermediate method call's events (and static initializers).
	 */
	private Value getOperandStackValueHelper(int consumerID, int argument) {
		
		Value value = null;
		
		EventKind kind = getKind(consumerID);
		Instruction consumer = getInstruction(consumerID);
		MethodInfo method = consumer.getMethod();
		boolean isVirtual = method.isVirtual();
		int numberOfArguments = consumer.getNumberOfArgumentProducers();

		// Some kinds of events have no producers.
		if(kind.isArtificial || numberOfArguments == 0 || (kind.isValueProduced && consumer instanceof Invoke))
			return null;

		StackDependencies.Producers producers = consumer.getProducersOfArgument(argument);

		if(consumer instanceof IADD)
			System.err.println();
		
		// If there are no producers for this argument, but the argument exists nonetheless, it must be the consumer of an exception local.
		if(producers.getNumberOfProducers() == 0)
			return null;

		// Copy the producers
		Instruction[] prods = new Instruction[producers.getNumberOfProducers()];
		for(int i = 0; i < producers.getNumberOfProducers(); i++) {

			Instruction potentialProducer = producers.getProducer(i);
			
			Instruction priorProducer = consumer;
			while(potentialProducer instanceof Duplication) {
				Instruction newProducer = getSourceOfDuplicationsValue((Duplication)potentialProducer, priorProducer, argument);
				priorProducer = potentialProducer;
				potentialProducer = newProducer;
			}
			
			prods[i] = potentialProducer;
			
		}
		
		boolean singleProducer = prods.length == 1;
		Instruction firstProducer = prods[0];

		// Now that we've resolved the duplications, let's handle optimizations.
		boolean referencesUninitializedObject = firstProducer.referencesUninitializedObject();

		// OPTIMIZATION: if we're accessing this method's "this" reference, instead of looking for a value produced by an ALOAD_0,
		// look for its definition instead, so we don't have to record these.
		if(isVirtual && singleProducer && firstProducer instanceof ALOAD_0 && !referencesUninitializedObject) {

			int startID = getStartID(consumerID);
			// We don't record the set argument event of the instance initializer, so this trick won't work for these.
			if(method.isInstanceInitializer()) {
				// Instead, we'll find the caller of this initializer and ask it for its 'this'.
				int callID = getStartIDsInvocationID(startID); 
				if(callID >= 0) value = getOperandStackValue(callID, 0);
				// If there is no known caller, then we don't have the value.
				if(value == null) 
					value = new UnknownValue(this, consumerID, UnknownValue.Reason.THIS_NOT_RECORDED);
			}
			else {
				ThreadIterator afterStart = new ThreadIterator(startID);
				int nextID = afterStart.nextInThread();
				EventKind nextKind = getKind(nextID);
				value = new TraceValue(this, nextID, firstProducer);
			}
			
		}
		// OPTIMIZATION: if there's a single producer and it's a GetLocal (a LOAD instruction) that doesn't reference an uninitialized object, 
		// instead of looking for a value produced by the LOAD, look for its definition instead, so we don't have to record these.
		else if(singleProducer && firstProducer instanceof GetLocal && !referencesUninitializedObject) {

			int localIDUsed = ((GetLocal)firstProducer).getLocalID();
			if(value == null) {
				int definitionID = getDefinitionOfLocalIDBefore(consumerID, localIDUsed);
				if(definitionID >= 0) value = new TraceValue(this, definitionID, firstProducer);
				else assert false : "It shouldn't be possible to not find a definition for local " + localIDUsed;
			}
			
		}
		else if(singleProducer && producerIsUnrecordedConstant(firstProducer)) {
			
			Instruction priorProducer = firstProducer;
			Instruction constantProducer = firstProducer;
			while(constantProducer instanceof Duplication) {
				Instruction newProducer = getSourceOfDuplicationsValue((Duplication)constantProducer, priorProducer, argument);
				priorProducer = constantProducer;
				constantProducer = newProducer;
			}

			value = new ConstantValue(this, (PushConstant<?>)constantProducer);
			
		}
		else if(firstProducer instanceof JSR || firstProducer instanceof JSR_W) {
			value = new UnknownValue(this, consumerID, UnknownValue.Reason.JSR_ARGUMENT);
		}
		// If this instruction's value comes from a NEW_OBJECT event, find the new object event.
		else if(singleProducer && firstProducer instanceof NEW) {

			// We special case <init> consumers, since they come BEFORE the NEW_OBJECT event.
			if(kind == EventKind.INVOKE_SPECIAL) {

				int newID = getInstantiationFollowingInitialization(consumerID);
				if(newID >= 0)
					value = new TraceValue(this, newID, firstProducer);
				else {
					value = new UnknownValue(this, consumerID, UnknownValue.Reason.NO_PLACEHOLDER);
				}
				
			}
			else {
							
				int placeholderID = -1;
				Trace.ThreadIterator iterator = getThreadIteratorAt(consumerID);
				while(value == null && iterator.hasPreviousInMethod()) {
					
					int previousID = iterator.previousInMethod();
					EventKind previousKind = getKind(previousID);
					if(previousKind == NEW_OBJECT && getInstruction(previousID) == firstProducer) {
						placeholderID = previousID;
						break;
					}
				}
				if(placeholderID >= 0) {
					long objectID = getLongProduced(placeholderID);
					if(objectID < 0)
						value = new UnknownValue(this, consumerID, UnknownValue.Reason.PLACEHOLDER_WITH_NO_VALUE);
					else {
						int newID = getInstantiationHistory().getInstantiationIDOf(objectID);
						if(newID >= 0)
							value = new TraceValue(this, newID, firstProducer);
						else
							value = new UnknownValue(this, consumerID, UnknownValue.Reason.PLACEHOLDER_WITH_NO_CORRESPONDING);
					}
				}
				else
					value = new UnknownValue(this, consumerID, UnknownValue.Reason.NO_PLACEHOLDER);
				
			}
			
		}
		// If the potential producer references the uninitialized instance BEFORE the super() call, then we look for the producer 
		// of the 1st argument of the invocation of this <init> call.
		else if(referencesUninitializedObject && firstProducer.referencesInstanceInInitializerBeforeSuperInitializer()) {
			
			int startID = getStartID(consumerID);
			int callID = getStartIDsInvocationID(startID); 
			if(callID >= 0)
				value = getOperandStackValue(callID, 0);
			if(value == null)
				value = new UnknownValue(this, consumerID, UnknownValue.Reason.NO_INVOCATION_INSTANCE);
			
		}
		// If the producers don't match any of the instrumentation optimizations, 
		// then search for the value backwards through the thread trace
		else if(prods.length > 0) {

			// Arguments are 0-indexed, and pushed onto the stack in order, so we'll traverse them backwards.
			int argumentsFound = 0;
			
			// Start an iterator at the production event
			Trace.ThreadIterator iterator = getThreadIteratorAt(consumerID);
	
			// We continue searching backwards in the thread until we've passed over the number of arguments that the producer takes.
			// If after all that, we find nothing, its an unknown value.
			while(value == null && iterator.hasPreviousInMethod() && argumentsFound < numberOfArguments) {
	
				int previousID = iterator.previousInMethod();
				EventKind previousKind = getKind(previousID);

				if(previousKind.isValueProduced) {
					Instruction previousInstruction = getInstruction(previousID);
					
					boolean candidateIsProducer = false;
					for(Instruction prod : prods)
						if(prod == previousInstruction)
							candidateIsProducer = true;
					
					if(candidateIsProducer) {

						argumentsFound++;
						if(previousInstruction instanceof GetLocal) {
							value = new TraceValue(this, getDefinitionOfLocalIDBefore(previousID, ((GetLocal)previousInstruction).getLocalID()), previousInstruction);
						}
						else if(previousKind.isConstantProduced)
							value = new ConstantValue(this, (PushConstant<?>)previousInstruction);
						else
							value = new TraceValue(this, previousID, previousInstruction);
					}
					
				}
				
			}

			if(value == null)
				value = new UnknownValue(this, consumerID, UnknownValue.Reason.UNKNOWN);

		}
		// If this instruction has NO producers, but nonetheless expects some values, its probably the first line of an exception handler
		else if(consumer.isExceptionHandlerStart() && consumer instanceof SetLocal) {
			
			value = new UnknownValue(this, consumerID, UnknownValue.Reason.NO_EXCEPTION_SOURCE);
			
		}
		else
			assert false : "Why doesn't " + consumer + " have any producers, despite claiming to?";

		return value;
		
	}
	
	private int getInstantiationFollowingInitialization(int initializationID) {

		QualifiedClassName classCreated = ((INVOKESPECIAL)getInstruction(initializationID)).getMethodInvoked().getClassName();
		Trace.ThreadIterator iterator = getThreadIteratorAt(initializationID);
		while(iterator.hasNextInMethod()) {
			int nextID = iterator.nextInMethod();
			EventKind nextKind = getKind(nextID);
			if(nextKind == NEW_OBJECT && ((NEW)getInstruction(nextID)).getClassInstantiated().getName() == classCreated) {
				return nextID;
			}
		}
		return -1;
		
	}
	
	private static boolean producerIsUnrecordedConstant(Instruction potentialProducer) {

		if(!potentialProducer.getTypeProduced().isConstantProduced) return false;
		else if(potentialProducer.isIO()) return false;
		else if(potentialProducer instanceof GetLocal) return false;
		else return true;

	}

	// By passing the consumer, this method can figure out which value of the dup's two values to return.
	private static Instruction getSourceOfDuplicationsValue(Duplication dup, Instruction consumerOfDuplicationsValue, int argumentNumberOfDuplicationsValue) {
		
		StackDependencies.Consumers consumers = dup.getConsumers();
		
		// If the duplication only has a single consumer, then it only produces a single value.
		// This is the case for DUP, DUP_X1, DUP_X2, and DUP2, DUP2_X1, and DUP2_X2 on longs and doubles.
		// In this case, we simply return the producer of the dup's value. 
		if(consumers.getNumberOfConsumers() == 1) {
			return dup.getProducersOfArgument(0).getFirstProducer();
		}
		// Otherwise, the duplication is a DUP2, DUP_X1, or DUP_X2 that produces two values. This is more complicated.
		else {

			Instruction producerOfFirstValue = dup.getProducersOfArgument(0).getFirstProducer();
			Instruction producerOfSecondValue = dup.getProducersOfArgument(1).getFirstProducer();
		
			// Are they the same instruction? If so, we need to know which argument the consumer is asking about.
			// That's the only way to know if we should return the first or second value produced.
			if(consumers.getFirstConsumer() == consumers.getSecondConsumer()) {
				
				// If its asking for its first, its definitely the first.
				if(argumentNumberOfDuplicationsValue == 0)
					return producerOfFirstValue;
				// Otherwise, is this also the producer for the argument before the one currently being sought? If so, its the second.
				else if(consumerOfDuplicationsValue.getProducersOfArgument(argumentNumberOfDuplicationsValue - 1).getFirstProducer() == dup)
					return producerOfSecondValue;
				// Otherwise, its asking for the first.
				else
					return producerOfFirstValue;
				
			}
			// Are they different instructions? If the consumer of the first value is asking, return the producer of the first value.
			// Otherwise, return the producer of the second value.
			else {
				
				if(consumers.getFirstConsumer() == consumerOfDuplicationsValue)
					return producerOfFirstValue;
				else
					return producerOfSecondValue;
				
			}
			
		}
		
	}
	
	public List<Value> getOperandStackDependencies(int eventID) { 

		int numberOfArguments = getInstruction(eventID).getNumberOfArgumentProducers();
		for(int i = 0; i < numberOfArguments; i++)
			getOperandStackValue(eventID, i);
		
		Value[] producers = arguments.get(eventID);
		
		if(producers == null) return Collections.<Value>emptyList();
		else return Collections.<Value>unmodifiableList(Arrays.<Value>asList(producers));
		
	}
		
	public String getDescription(int eventID) { return getKind(eventID).getDescription(this, eventID); }

	public String getHTMLDescription(int eventID) { return getKind(eventID).getHTMLDescription(this, eventID); }
	
	/**
	 * We have this special support for getting an associated name since method arguments have no associated instructions.
	 */
	public String getNameAssociatedWithEvent(int eventID) { 
	
		if(getKind(eventID).isArgument) {
			int localID = getArgumentLocalIDSet(eventID);
			Instruction instruction = getInstruction(eventID);
			MethodInfo method = instruction.getMethod();
			return method.getCode().getLocalIDNameRelativeToInstruction(localID, instruction);
		}
		else return getInstruction(eventID).getAssociatedName(); 
		
	}

	public Value getMethodArgumentValue(int eventID, int argumentNumber) {	

		// There won't be an invocation to main.
		if(getInstruction(eventID).getMethod().isImplicitlyInvoked()) return null;

		int startID = getStartID(eventID);
		
		// We won't find one if the invocation of the method wasn't recorded.
		if(!getInvocationByStartTable(startID).containsKey(startID)) return null;
		
		return getOperandStackValue(getInvocationByStartTable(startID).get(startID), argumentNumber);
		
	}

	public int getNextEventIDInMethod(int eventID) {
		
		ThreadIterator iterator = new ThreadIterator(eventID);
		return iterator.hasNextInMethod() ? iterator.nextInMethod() : -1;
		
	}
	
	public int getPreviousEventIDInMethod(int eventID) {
		
		ThreadIterator iterator = new ThreadIterator(eventID);
		return iterator.hasPreviousInMethod() ? iterator.previousInMethod() : -1;
		
	}
	
	public int getNextEventIDInThread(int eventID) { 
		
		Trace.ThreadIterator iterator = getThreadIteratorAt(eventID);
		if(iterator.hasNextInThread()) return iterator.nextInThread();
		else return -1;
		
	}

	public int getPreviousEventInThread(int eventID) {

		Trace.ThreadIterator iterator = getThreadIteratorAt(eventID);
		if(iterator.hasPreviousInThread()) return iterator.previousInThread();
		else return -1;
		
	}

	public int getSourceOfValueID(int eventID) { 
		
		int e = eventID;
		int mostRecentNonNullNonValueProducedEvent = e;

		while(e >= 0) {

			EventKind kind = getKind(e);
			
			if(!kind.isValueProduced || kind.isInstantiation)
				mostRecentNonNullNonValueProducedEvent = e;
			
			if(kind.isArgument)
				e = getHeapDependency(e);
			else if(kind == EventKind.PUTFIELD)
				e = getOperandStackValue(e, 1).getEventID();
			else if(kind == PUTSTATIC)
				e = getOperandStackValue(e, 0).getEventID();
			else if(kind == SETLOCAL)
				e = getOperandStackValue(e, 0).getEventID();
			else if(kind == SETARRAY)
				e = getOperandStackValue(e, 2).getEventID();
			else if(kind == RETURN)
				e = getOperandStackValue(e, 0).getEventID();
			else if(kind.isValueProduced) {
				Instruction i = getInstruction(e);
				if(i instanceof Use || i instanceof Invoke || i instanceof NEW) {
					int heapID = getHeapDependency(e);
					if(heapID >= 0)
						e = heapID;
					else
						break;
				}
				else
					break;
			}
			else {
				break;
			}
			
		}
		
		return e;
		
	}
	
	public int getStartID(int eventID) { 
		
		return eventID < 0 ? -1 : getInvocationHistory().determineStartMethodIDOf(eventID); 
		
	}

	/**
	 * Finds the heap or stack frame dependency for this instruction, if any. Operand stack dependencies are dealt with separately.
	 */
	public int getHeapDependency(int eventID) { 

		EventKind kind = getKind(eventID);

		// If the event corresponds to a method argument value, find the value passed to the invocation of this method (if the call was traced)
		if(kind.isArgument) {
			
			Instruction inst = getInstruction(eventID);
			MethodInfo method = inst.getMethod();
			int argumentNumber = method.getArgumentNumberOfLocalID(getArgumentLocalIDSet(eventID));
			Value stackvalue = getMethodArgumentValue(eventID, argumentNumber);
			if(stackvalue == null ) return -1;
			else if(!(stackvalue instanceof TraceValue)) return -1;
			else return stackvalue.getEventID();			
			
		}
			
		// If the event is a new instruction, point back to the constructor call.
		else if(kind == NEW_OBJECT)
			return getInitializationByInstantiationTable(eventID).get(eventID);
			
		else {
			
			Instruction inst = getInstruction(eventID);

			// Find the assignment to the array index...
			if(inst instanceof GetArrayValue) {
				
				try {
					
					long objectAddress = getOperandStackValue(eventID, 0).getLong();
					int arrayIndex = getOperandStackValue(eventID, 1).getInteger();
					return getArrayAssignmentBefore(objectAddress, arrayIndex, eventID);
					
				} catch(NoValueException e) { return -1; }

			}
			// Find the assignment to the field...
			else if(inst instanceof GETFIELD) {
				
				long referenceID = getOperandStackValue(eventID, 0).getLong();
				return referenceID < 0 ? -1 : findFieldAssignmentBefore(((FieldrefContainer)inst).getFieldref(), referenceID, eventID);

			}
			// Find the assignment to the local...
			else if(inst instanceof GetLocal) {
				
				int localID = ((GetLocal)inst).getLocalID();

				int dependencyID = findLocalIDAssignmentBefore(localID, eventID);

				// If there's no definition and this is a method argument, then return the definition from the invocation that led to this instruction.
				if(((GetLocal)inst).getsMethodArgument()) {
					if(dependencyID < 0) {
						
						int argNumber = inst.getMethod().getArgumentNumberOfLocalID(localID);
						Value value = getMethodArgumentValue(eventID, argNumber);
						if(value == null)
							return -1;
						else 
							dependencyID = value.getEventID();
						
					}
					else if(getInstruction(eventID).getMethod().isSynthetic()) {
						return getHeapDependency(dependencyID);
					}
				}
				
				return dependencyID;

			}
			// Find the assignment to the global...
			else if(inst instanceof GETSTATIC) {
				
				return findGlobalAssignmentBefore(((GETSTATIC)inst).getFieldref().getQualifiedName(), eventID);

			}
			// Special support for increment instructions...
			else if(inst instanceof IINC) {
				
				// If it's a parameter, there won't be an event in the history. Instead, let's find the
				// value produced for this method. First we have to find the invocation that led to this
				// method and then find the event that produced the value that executed it.
				final int localID = ((SetLocal)inst).getLocalID();
				
				int mostRecentLocalDefinition = findEventInMethodInThreadBefore(eventID, new Trace.SearchCriteria() {
					public boolean matches(int eventID) {
						Instruction candidate = getInstruction(eventID);
						if(candidate instanceof SetLocal)
							return ((SetLocal)candidate).getLocalID() == localID;
						else
							return false;
					}
				});
				
				// If we found the definition, explain it.
				if(mostRecentLocalDefinition >= -1) return mostRecentLocalDefinition;
				else return -1;

			}
			// Either this represents the value produced by a method's return, or it represents the invocation itself.
			// Only value produced events have dependencies.
			else if(inst instanceof Invoke)
				return kind.isValueProduced ? getInvocationReturnDependencyID(eventID) : -1;

			else return -1;
			
		}
	
	}
	
	private int getInvocationReturnDependencyID(int invocationID) {
		
		Invoke invoke = (Invoke)getInstruction(invocationID);
		
		assert invoke.getMethodInvoked().getReturnType() != QualifiedClassName.VOID : 
			" Why do we have a value produced event from a method that doesn't return a value?";

		// The value produced is recorded immediately after the method return, so it should be the previous method in thread.
		int returnID = getPreviousEventInThread(invocationID);
		
		// Did it return from the method we invoked? If so, we'll use it's value produced. 
		if(getKind(returnID) == EventKind.RETURN) {

			MethodInfo methodOfReturn = getInstruction(returnID).getMethod(); 
			if(methodOfReturn.getMethodNameAndDescriptor().equals(invoke.getMethodInvoked().getMethodNameAndDescriptor())) {
				
				// If the method doesn't appear in source, return the value produced by the return.
				if(methodOfReturn.isSynthetic())
					return getOperandStackValue(returnID, 0).getEventID();
				// Otherwise, just return the return event.
				else 
					return returnID;
			}
			// If the event before didn't match the expected event, then we probably didn't record the method called and found a different return.
			else {
				return -1;
			}
			
		}
		// If we didn't find a return event that matches, then we must not have instrumented the method.
		// We don't return anything here because we want to find all of the potential object state dependencies on this call.
		else return -1;
		
	}
	
	/**
	 * Finds the invocationID of the call that produced the value presented by the given event ID.
	 */
	public int getInvocationProducedInvocationID(int invocationProducedID) {
		
		Invoke call = (Invoke)getInstruction(invocationProducedID);

		// Walk backwards through this method until we find a call to the same method. It should only be a few steps away.
		ThreadIterator iterator = new ThreadIterator(invocationProducedID);
		while(iterator.hasPreviousInMethod()) {
			int eventID = iterator.previousInMethod();
			EventKind kind = getKind(eventID);
			Instruction inst = getInstruction(eventID);
			if(getKind(eventID).isInvocation && getInstruction(eventID) == call)
				return eventID;
		}
		return -1;
		
	}
	
	/**
	 * Given the eventID of an invocation, find all prior invocations on the object that could have affected 
	 * the value returned by the given invocation. This is used when we haven't recorded the internals of a 
	 * method call, but still want some low-precision information about what could have affected the return value. 
	 */
	public IntegerVector getUnrecordedInvocationDependencyIDs(int invocationID) {
		
		EventKind kind = getKind(invocationID);
		// This only applies to invocations with return values.
		if(!kind.isValueProduced) return null;

		Instruction inst = getInstruction(invocationID);
		if(!(inst instanceof Invoke)) return null;
				
		Invoke invoke = (Invoke)inst;
		
		// If we're asking about a method that returns nothing, there are no dependencies to be asking about. 
		if(invoke.getMethodInvoked().returnsVoid()) return null;

		// Find the invocation event corresponding to this value produced event.
		int callID = getInvocationProducedInvocationID(invocationID);
		int startID = getInvocationStartID(callID);
		int returnID = getStartIDsReturnOrCatchID(startID);
		
		// If we recorded this call, skip it. This method is only for unrecorded call dependencies.
		if(returnID > 0) return null;

		// If we have the return (the event passed to this method), we should have the call. 
		assert callID >= 0;
		
		// If this is a static method, add the arguments of the call as dependencies.
		if(invoke.getMethodInvoked().isStatic()) {

			IntegerVector callIDs = new IntegerVector(1); 
			callIDs.append(callID);
			return callIDs;
		
		}
		// If this is an instance method, find all other invocations on this instance and add them as dependencies.
		// Also include the call itself, to include its parameters.
		else {
					
			// If we found the corresponding return, also find all of the arguments passed to methods
			// called on the corresponding instance, if there were any.
			// Find the instance called on...
			Value instanceValue = getOperandStackValue(callID, 0);
			// Find all other invocations on the given object.
			if(instanceValue != null) {
				long instanceID = instanceValue.getLong();
				IntegerVector callIDs = invocationHistory.findInvocationsOfPublicStateAffectingMethodsWithParametersOnObjectIDBefore(instanceID, callID);
				callIDs.append(callID);
				return callIDs;
			}
			
		}
		return null;
		
	}
	
	public int getControlID(int eventID) { 
	
		int controlID = controlIDs.get(eventID);
		if(controlID == 0) {
			controlID = determineControlID(eventID);
			controlIDs.put(eventID, controlID);
		}
		return controlID;
		
	}

	/**
	 * Helper for getControlID, which caches the results of this method.
	 */
	private int determineControlID(int eventID) {

		int controlID = -1;
		
		Instruction inst = getInstruction(eventID);
		
		final MethodInfo method = inst.getMethod();
		
		// Is this in a catch?
		List<ExceptionHandler> handlers = method.getCode().getExceptionHandlersThatExecute(inst);
		boolean inCatch = handlers.size() > 0;
		
		final Set<Instruction> controlDependencies = inst.getBranchDependencies();

		boolean controlDependenciesInCatch = true;
		for(Instruction control : controlDependencies) if(method.getCode().getExceptionHandlersThatExecute(control).size() == 0) controlDependenciesInCatch = false;

		if(inCatch && !controlDependenciesInCatch) {

			ExceptionHistory exceptions = getExceptionHistory();
			int throwEvent = exceptions.getExceptionThrownBefore(eventID);
			if(throwEvent >= 0) {
				controlID = throwEvent;
				return controlID;
			}
			
		}
		
		// Find the most recently executed control dependency
		int branchEvent = findEventInMethodInThreadBefore(eventID, new Trace.SearchCriteria() { 
			public boolean matches(int eventID) { 
				return 
					controlDependencies.contains(getInstruction(eventID)) &&
					(getKind(eventID).isBranch || getKind(eventID).isInvocation); 
			}
		});
		
		if(branchEvent >= 0) {
			controlID = branchEvent;
			return controlID;
		}
		
		// Otherwise, the control dependency is the execution of the method that executed this event.
		int threadID = getThreadID(eventID);

		// Was this in a Thread.run() method? If so, find the Thread.start() call.
		if(method.isRun()) {
			
			for(Invoke invoke : method.getPotentialCallers()) {

				int threadStartEvent = findThreadStartBefore(invoke, eventID);

				if(threadStartEvent >= 0) controlID = threadStartEvent;
				
				// Returning this would cause an infinite loop.
				if(threadStartEvent == eventID) controlID = -1;

				return controlID;
			
			}
			
		}
		else if(method.isClassInitializer()) {
			
			int clinitStartEvent = getClassInitializationHistory().getClassInitializationEventFor(method.getClassfile().getInternalName());

			// If this is the class initializer start, then we need to find the method that caused this to be invoked.
			if(clinitStartEvent == eventID) {

				Trace.ThreadIterator iterator = getThreadIteratorAt(eventID);
				if(iterator.hasPreviousInThread()) controlID = getControlID(iterator.previousInThread());
				else controlID = -1;
				
			}
			else controlID = clinitStartEvent;

			return controlID;
			
		}
		else if(method == getMain()) {
		
			controlID = getMainStartEventID();

			// Returning the given eventID  would cause an infinite loop!
			if(controlID == eventID) controlID = -1;
			return controlID;
			
		}
		
		// Otherwise, find the call to this event's method.
		int invocationID = getStartIDsInvocationID(getStartID(eventID));
		if(invocationID >= 0) {
			controlID = invocationID;
			if(method.isSynthetic())
				controlID = getControlID(invocationID);
		}

		// If we didn't find anything (and the code above is correct!) then the invocation was probably native.
		return controlID;
		
	}
	
	public int getNumberOfBlocks() { return getNumberOfEvents() / EVENTS_PER_BLOCK + 1; }
		
	// I tried caching this, but it really didn't help. It misses too much to be worth the cost of keeping the cache.
	private int getInstructionID(int eventID) { 

		IDBlock block = idBlocks.blocks[eventID / EVENTS_PER_BLOCK];
		if(block == null) block = getIDBlock(eventID);
		return block.instructionIDs[eventID - block.firstEventID];
		
	}

	public EventKind getKind(int eventID) { 

		IDBlock block = idBlocks.blocks[eventID / EVENTS_PER_BLOCK];

		if(block == null) block = getIDBlock(eventID);
		byte kindID = block.kindIDs[eventID - block.firstEventID]; 
		return EventKind.intToEvent(kindID);
		
	}
	
	public int getThreadID(int eventID) { 

		if(threadIDCache.containsKey(eventID))
			return threadIDCache.get(eventID);

		int threadID = findThreadID(eventID); 
		if(threadID < 0) throw new RuntimeException("Couldn't find threadID for event " + eventID);
		if(threadIDCache.size() > THREAD_ID_CACHE_SIZE) threadIDCache.clear();
		threadIDCache.put(eventID, threadID);
		return threadID;
		
	}
	
	private int findThreadID(int eventID) {
		
		// Go through each thread, searching for the trace that contains the given ID
		for(ThreadTrace trace : threads)
			if(eventID >= trace.getFirstEventID() && eventID <= trace.getLastEventID() && trace.eventIDs.contains(eventID))
					return trace.getThreadID();
		return -1;
		
	}
	
	// BranchEvent facade
	public int getBranchFirstExecutionInMethod(int branchID) { 
		
		int startEventID = getStartID(branchID);
		return findEventInMethodInThreadAfter(startEventID, getInstruction(branchID));
		
	}
	
	public Object getObjectProduced(int eventID) {
	
		if(getKind(eventID).isConstantProduced) {
			if(getInstruction(eventID) instanceof PushConstant) return ((PushConstant<?>)getInstruction(eventID)).getConstant();
			else return null;
		}
		else {
			long id = getObjectIDProduced(eventID);
			Object o = id < 0 ? null : getImmutableObject(id);
			return o;
		}
		
	}
	
	public boolean getBooleanProduced(int booleanID) throws NoValueException { 
		
		if(getBooleansProduced(booleanID).containsKey(booleanID)) return getBooleansProduced(booleanID).get(booleanID) > 0;
		else if(getKind(booleanID).isConstantProduced) return (Boolean)((PushConstant<?>)getInstruction(booleanID)).getConstant();
		else throw new NoValueException(this, booleanID, "didn't produce an boolean."); 
		
	}
		
	public int getIntegerProduced(int eventID) throws NoValueException { 
		
		if(getShortsProduced(eventID).containsKey(eventID)) return getShortsProduced(eventID).get(eventID);
		else if(getIntegersProduced(eventID).containsKey(eventID)) return getIntegersProduced(eventID).get(eventID);
		else if(getBooleansProduced(eventID).containsKey(eventID)) return getBooleansProduced(eventID).get(eventID);
		else if(getKind(eventID).isConstantProduced) return (Integer)((PushConstant<?>)getInstruction(eventID)).getConstant();
		else throw new NoValueException(this, eventID, "didn't produce an integer."); 
			
	}
	
	public long getObjectIDProduced(int eventID) { return getLongProduced(eventID); }

	public long getLongProduced(int eventID) { 

		if(getShortsProduced(eventID).containsKey(eventID)) return getShortsProduced(eventID).get(eventID);
		else if(getIntegersProduced(eventID).containsKey(eventID)) return getIntegersProduced(eventID).get(eventID);
		else if(getLongsProduced(eventID).containsKey(eventID)) return getLongsProduced(eventID).get(eventID);
		else if(getKind(eventID).isConstantProduced) {
			Object val = ((PushConstant<?>)getInstruction(eventID)).getConstant();
			if(val == null) return 0;
			else if(val instanceof String) return getIDOfImmutable(val);
			else throw new RuntimeException("Couldn't find a long produced by " + eventID);
		}
		else return -1; 
		
	} 

	public float getFloatProduced(int eventID) throws NoValueException { 
		
		if(getFloatsProduced(eventID).containsKey(eventID)) return getFloatsProduced(eventID).get(eventID);
		else if(getKind(eventID).isConstantProduced) return (Float)((PushConstant<?>)getInstruction(eventID)).getConstant();
		else throw new NoValueException(this, eventID, "didn't produce a float."); 
		
	}
	
	public double getDoubleProduced(int eventID) throws NoValueException { 
		
		if(getDoublesProduced(eventID).containsKey(eventID)) return getDoublesProduced(eventID).get(eventID);
		else if(getKind(eventID).isConstantProduced) return (Double)((PushConstant<?>)getInstruction(eventID)).getConstant();
		else throw new NoValueException(this, eventID, "didn't produce a double."); 
		
	}

	public char getCharacterProduced(int eventID) throws NoValueException { 
		
		if(getCharactersProduced(eventID).containsKey(eventID)) return (char)getCharactersProduced(eventID).get(eventID); 
		else if(getKind(eventID).isConstantProduced) return (Character)((PushConstant<?>)getInstruction(eventID)).getConstant();
		else throw new NoValueException(this, eventID, "didn't produce a character."); 

	}
	
	public byte getByteProduced(int eventID) throws NoValueException {
		
		if(getBytesProduced(eventID).containsKey(eventID)) return getBytesProduced(eventID).get(eventID); 
		else if(getKind(eventID).isConstantProduced) return (Byte)((PushConstant<?>)getInstruction(eventID)).getConstant();
		else throw new NoValueException(this, eventID, "didn't produce a byte."); 

	}
	
	public short getShortProduced(int eventID) throws NoValueException { 
		
		if(getShortsProduced(eventID).containsKey(eventID)) return getShortsProduced(eventID).get(eventID); 
		else if(getKind(eventID).isConstantProduced) return (Short)((PushConstant<?>)getInstruction(eventID)).getConstant();
		else throw new NoValueException(this, eventID, "didn't produce a short."); 

	}

	// ReturnValueEvent facade
	public Value getReturnValueReturned(int returnEventID) { 
	
		if(getInstruction(returnEventID) instanceof RETURN) return null;
		else return getOperandStackValue(returnEventID, 0); 
		
	}
	
	public int getReturnStartID(int returnID) {
		
		if(getStartByReturnTable(returnID).containsKey(returnID)) return getStartByReturnTable(returnID).get(returnID);
		else throw new RuntimeException("" + eventToString(returnID) + " didn't have a return.");

	}
	
	// DefinitionEvent facade
	public Value getDefinitionValueSet(int definitionID) { 
		
		EventKind kind = getKind(definitionID);
		switch(kind) {
			case IINC : {
				try { return new IncrementValue(this, definitionID, getIncrementValue(definitionID)); }
				catch (NoValueException e) { return new UnknownValue(this, definitionID, UnknownValue.Reason.NO_INCREMENT); }
			}
			case PUTFIELD : return getOperandStackValue(definitionID, 1);
			case PUTSTATIC : return getOperandStackValue(definitionID, 0);
			case SETARRAY : return getOperandStackValue(definitionID, 2);
			case SETLOCAL : return getOperandStackValue(definitionID, 0);
		}
		
		return null;		
	}
		
	public boolean eventDefinesLocalID(int definitionID, int localID) { 

		EventKind kind = getKind(definitionID);
		
		if(kind == SETLOCAL) return ((SetLocal)getInstruction(definitionID)).getLocalID() == localID;
		else if(kind.isArgument) return getArgumentLocalIDSet(definitionID) == localID;
		else if(kind == IINC) return ((IINC)getInstruction(definitionID)).getLocalID() == localID;
		else return false;
	
	}

	// PutFieldEvent facade
	public long getPutFieldObjectIDAssigned(int putEventID) throws NoValueException { 
		
		Value value = getOperandStackValue(putEventID, 0);
		if(value instanceof UnknownValue || value == null) return 0;
		else return value.getLong();
	
	}
	
	private final TIntLongHashMap invocationInstanceIDByInvocation = new TIntLongHashMap(100000);

	public long getInvocationInstanceID(int invocationID) { 

		Instruction inst = getInstruction(invocationID); 
		if(inst instanceof INVOKESTATIC) return 0;

		// First check the table...
		long instanceID = invocationInstanceIDByInvocation.get(invocationID);
		if(instanceID <= 0) {

			// If we instrumented this call, it will be much faster to look for the argument event for the instance.
			int startID = getInvocationStartID(invocationID);
			if(startID >= 0) {
				int argumentID = getNextEventIDInMethod(startID);
				if(argumentID >= 0 && getKind(argumentID).isArgument && getArgumentLocalIDSet(argumentID) == 0)
					instanceID = getObjectIDProduced(argumentID);
			}

			// If we couldn't find the start of the method, find the stack value (which is slower).
			if(instanceID < 0) {
				instanceID = getOperandStackValue(invocationID, 0).getLong();
				invocationInstanceIDByInvocation.put(invocationID, instanceID);
			}
			
			invocationInstanceIDByInvocation.put(invocationID, instanceID);
			
		}
		return instanceID;		
	
	}
	
	public QualifiedClassName getInvocationClassInvokedOn(int invocationID) { 

		Invoke inst = (Invoke) getInstruction(invocationID);
		if(inst instanceof INVOKESTATIC) return inst.getMethodInvoked().getClassName();
		else {
			long id = getInvocationInstanceID(invocationID);
			if(id < 0) return QualifiedClassName.JAVA_LANG_OBJECT;
			else return getClassnameOfObjectID(id);
		}
		
	}
	
	public int getInvocationStartID(int invocationID) { 
	
		if(getStartByInvocationTable(invocationID).containsKey(invocationID)) return getStartByInvocationTable(invocationID).get(invocationID);
		else return -1;
		
	}

	// SetMethodArgumentEvent facade.
	public String getArgumentDescription(int setMethodArgumentID) { 

		return getKind(setMethodArgumentID).getDescriptionOfSetArgumentEvent(this, setMethodArgumentID);
		
	}

	public String getArgumentValueDescription(int setMethodArgumentID) { 

		return getKind(setMethodArgumentID).getDescriptionOfSetArgumentValue(this, setMethodArgumentID);
		
	}

	public String getArgumentNameSet(int setMethodArgumentID) { 

		Instruction inst = getInstruction(setMethodArgumentID);
		return inst.getCode().getLocalIDNameRelativeToInstruction(getArgumentLocalIDSet(setMethodArgumentID), inst);
		
	}
	
	public int getArgumentLocalIDSet(int setMethodArgumentID) { 

		assert getKind(setMethodArgumentID).isArgument : "Can only send set argument event IDs to this, but received \n\t" + eventToString(setMethodArgumentID);
		
		MethodInfo method = getInstruction(setMethodArgumentID).getMethod();
		
		// How far away is this set method arg's start method event?
		ThreadIterator i = getThreadIteratorAt(setMethodArgumentID);
		int argNumber = 0;
		while(i.hasPreviousInMethod()) {
			int nextID = i.previousInMethod();
			EventKind kind = getKind(nextID);
			if(kind == EventKind.START_METHOD) break;
			else if(kind.isArgument) argNumber++;
		}

		// We don't instrument 'this' in <init> methods, so we advance the arg by one.
		if(method.isInstanceInitializer()) argNumber++;

		int predictedID = method.getLocalIDFromArgumentNumber(argNumber);
		
		return predictedID;
	
	}
	
	public KeyArguments getKeyArguments(int keyEventID) { return keyArguments.get(keyEventID); }
	public MouseArguments getMouseArguments(int mouseEventID) { return mouseArguments.get(mouseEventID); }
	public RepaintArguments getRepaintArguments(int repaintEventID) { return repaintArguments.get(repaintEventID); }
	public CreateGraphicsArguments getCreateGraphicsArguments(int createGraphicsEventID) { return createGraphicsArguments.get(createGraphicsEventID); }
	
	public int getInitializationOfObjectID(long objectID) {

		int newID = getInstantiationOf(objectID);
		if(newID < 0) return -1;
		TIntIntHashMap initIDs = getInitializationByInstantiationTable(newID);
		return 
			initIDs.containsKey(newID) ? 
			initIDs.get(newID) : 
			-1;

	}
	
	public int getStartIDsReturnOrCatchID(int startID) { 
		
		TIntIntHashMap returnIDs = getReturnByStartTable(startID);
		return 
			returnIDs.containsKey(startID) ? 
			returnIDs.get(startID) : 
			-1;
	
	}

	public int getStartIDsInvocationID(int startID) { 
	
		if(getInvocationByStartTable(startID).containsKey(startID)) return getInvocationByStartTable(startID).get(startID);
		else return -1;
		
	}	

	public Value getStartMethodObjectIDInvokedOn(int startID) { 
		int invocationID = getStartIDsInvocationID(startID);
		return invocationID < 0 ? null : getOperandStackValue(invocationID, 0); 
	}
		
	public int getIncrementValue(int incrementID) throws NoValueException { 
	
		if(getValuesByIncrementID(incrementID).containsKey(incrementID)) return getValuesByIncrementID(incrementID).get(incrementID);
		else throw new NoValueException(this, incrementID, "not an increment.");
		
	}
	
	// SetArrayEvent facade
	public Value getSetArrayArraySet(int setArrayEventID) { return getOperandStackValue(setArrayEventID, 0); }
	public Value getSetArrayIndexSet(int setArrayEventID) { return getOperandStackValue(setArrayEventID, 1); }
	public Value getSetArrayValueSet(int setArrayEventID) { return getOperandStackValue(setArrayEventID, 2); }

	// Exception facades
	public int getCatchesThrowID(int catchID) { return getExceptionHistory().getThrowEventIDForCatchID(catchID); }
	
	public long getIDOfImmutable(Object obj) { return idsOfImmutables.get(obj); }

	public QualifiedClassName getClassnameOfObjectID(long objectID) {
		
		if(objectID == 0) return QualifiedClassName.NULL;
		else if(objectID < 0) return null;

		int classID = objectTypes.get(objectID);
		assert classID != 0 : "Object ID " + objectID + " is of an unknown type.";
		return getNameOfClassID(classID);
		
	}

	public String getAssociatedNameOfObjectID(long objectID) {

		String associatedName = null;
		
		// Can we find an local identifier name for the object?
		int instantiationID = getInstantiationOf(objectID);
		if(instantiationID >= 0) {
			Instruction instantiation = getInstruction(instantiationID);
			while(instantiation != null) {
				if(instantiation instanceof SetLocal || instantiation instanceof PUTFIELD || instantiation instanceof PUTSTATIC) {
					associatedName = instantiation.getAssociatedName();
					break;
				}
				Consumers consumers = instantiation.getConsumers();
				if(consumers.getNumberOfConsumers() > 1) 
					break;
				else
					instantiation = consumers.getFirstConsumer();
			}
		}

		return associatedName;
		
	}

	public Classfile getClassfileOfObjectID(long objectID) {
		
		if(objectID == 0) return null;
		int classID = objectTypes.get(objectID);
		if(classID == 0) return null;
		else return getClassfileByID(classID);		
		
	}

	public String getDescriptionOfObjectID(long objectID) {
		
		if(objectID < 0) return "unknown";
		else if(objectID == 0) return "null";
		QualifiedClassName name = getClassnameOfObjectID(objectID);
		String number = Util.commas(objectID);
		return name.getSimpleName() + " #" + number;
		
	}
	
	/////////////////////////////////////////////////////
	// EXECUTION EVENTS and convenience getters.
	
	// The after time is inclusive.
	public int findEventBetween(int afterTime, int beforeTime, SearchCriteria criteria) {
		
		int indexToCheck = beforeTime - 1;

		if(afterTime < 0) afterTime = 0;
		
		while(indexToCheck >= afterTime) {
			
			if(criteria.matches(indexToCheck)) return indexToCheck; 
			
			indexToCheck--;
			
		}
		return -1;
		
	}

	public @Deprecated int findEventInThreadBefore(int eventID, SearchCriteria criteria) {
		
		ThreadIterator iterator = getThreadIteratorAt(eventID);
		while(iterator.hasPreviousInThread()) {
			int id = iterator.previousInThread();
			if(criteria.matches(id))
				return id;
		}
		
		return -1;
		
	}	
	
	public int findEventInMethodInThreadBefore(int eventID, SearchCriteria criteria) {
				
		ThreadIterator iterator = getThreadIteratorAt(eventID);
		
		EventKind kind = getKind(eventID);
		
		// If we start at the start method, we're already done.
		if(kind == START_METHOD) return -1;
		
		while(iterator.hasPreviousInMethod()) {

			int e = iterator.previousInMethod();
			if(criteria.matches(e)) return e; 
			
		}
		
		return -1;
		
	}

	public int findEventInMethodInThreadAfter(int eventID, Instruction instructionToFindExecutionOf) {
		
		ThreadIterator iterator = getThreadIteratorAt(eventID);
		
		while(iterator.hasNextInMethod()) {

			int e = iterator.nextInMethod();
			if(getInstruction(e) == instructionToFindExecutionOf) return e;

		}
		
		return -1;
		
	}
			
	public int getInstantiationOf(long objectID) { return instantiationHistory.getInstantiationIDOf(objectID); }

	public InstantiationHistory getInstantiationHistory() { return instantiationHistory; }
	
	public int getArrayAssignmentBefore(final long arrayID, final int arrayIndex, int beforeEventID) {
		
		int mostRecentArrayDefinition = arrayHistory.getIndexAssignmentBefore(arrayID, arrayIndex, beforeEventID);

		if(mostRecentArrayDefinition >= 0)
			return mostRecentArrayDefinition;

		// If we don't find it, we use the instantiation event to represent the default array element value.
		return getInstantiationOf(arrayID);

	}

	public int getArrayLength(long arrayID) {
		
		int initID = getInstantiationOf(arrayID);
		
		if(initID > 0) {
		
			Instruction newArray = getInstruction(initID);
			Value value = getOperandStackValue(initID, 0);
			try {
				return value.getInteger();
			} catch (NoValueException e) {
				return -1;
			}
			
		}
		else return -1;
		
	}
	
	/**
	 * 
	 * @param getstatic
	 * @param event
	 * @return May return null, which indicates that the static started with a default value for its type.
	 */
	public int findGlobalAssignmentBefore(String qualifedName, int eventID) {

		return staticAssignmentHistory.getLastDefinitionOfBefore(qualifedName, eventID);

	}

	public int findFieldAssignmentBefore(FieldInfo field, long objectID, int eventID) { return findFieldAssignmentBefore(field.getDisplayName(true, -1), objectID, eventID); }
	public int findFieldAssignmentBefore(FieldrefInfo field, long objectID, int eventID) { return findFieldAssignmentBefore(field.getName(), objectID, eventID); }
	public int findFieldAssignmentBefore(String fieldname, long objectID, int eventID) {

		int lastDefinition = fieldAssignmentHistory.getDefinitionOfFieldBefore(objectID, fieldname, eventID);
				
		if(lastDefinition >= 0)
			return lastDefinition;
			
		// If we didn't find a recent definition, then the field had a default value. We'll point to the new event.
		return getInstantiationOf(objectID);

	}
	
	public int findFieldAssignmentAfter(FieldInfo field, long objectID, int eventID) { 
		
		return fieldAssignmentHistory.getDefinitionOfFieldAfter(objectID, field.getName(), eventID);

	}
	
	/**
	 * Finds the call responsible for the given eventID and looks for the local ID assignment before it.
	 */
	public int findLocalIDAssignmentBefore(final int localID, int eventID) {

		assert localID >= 0 : "Can't find a local with local ID " + localID;

		int definition = findEventInMethodInThreadBefore(eventID, new Trace.SearchCriteria() {
			public boolean matches(int eventID) {
				if(getKind(eventID).isDefinition)
					return eventDefinesLocalID(eventID, localID);
				else
					return false;
			}
		});

		return definition;
		
	}
		
	public int findExecutionOfInstructionPriorTo(final Instruction inst, int eventID) {
		
		return findEventBetween(0, eventID, new SearchCriteria() {
			public boolean matches(int eventID) {
				return getInstruction(eventID) == inst;
			}
		});
		
	}

	public int findExecutionOfInstructionInThreadAfter(final Instruction inst, int eventID) {

		int threadID = getThreadID(eventID);
		eventID++;
		
		while(eventID < getNumberOfEvents()) {

			if(getThreadID(eventID) != threadID)
				continue;
			
			if(getInstruction(eventID) == inst && !(inst instanceof Invoke && getKind(eventID).isInvocation)) return eventID; 
			
			eventID++;
			
		}
		return -1;
		
	}
	
	public int findExecutionOfInstructionAfter(Instruction inst, int afterID) {
		
		MethodInfo method = inst.getMethod();
		
		int currentStartID = getStartID(afterID);

		// We look ahead by one in case we're already on the nearest match.
		IntegerVector startIDs = invocationHistory.getStartIDsAfterEventID(method, currentStartID - 1);
	
		int eventID = -1;
		for(int i = 0; i < startIDs.size(); i++) {
			int startID = startIDs.get(i);
			eventID = findExecutionOfInstructionInCallAfter(startID, inst, afterID);
			if(eventID >= 0)
				break;
		}
		
		return eventID;
		
	}
	
	public int findExecutionOfInstructionInCallAfter(int startID, Instruction inst, int afterID) {
		
		Trace.ThreadIterator events = getThreadIteratorAt(startID);
		
		while(events.hasNextInMethod()) {
			
			int eventID = events.nextInMethod();
			
			if(eventID > afterID && getInstruction(eventID) == inst)
				return eventID;
			
		}

		return -1;		
		
	}

	public IntegerVector findExecutionsOfInstructionAfter(Instruction instruction, long objectID, int afterID) {
		
		StackDependencies.Consumers consumers = instruction.getConsumers();
		Instruction consumer1 = consumers.getFirstConsumer();
		Instruction consumer2 = consumers.getSecondConsumer();
		
		if(instruction instanceof PUTFIELD || consumer1 instanceof PUTFIELD || consumer2 instanceof PUTFIELD) {
			
			PUTFIELD instructionToAnalyze = (PUTFIELD)(
				instruction instanceof PUTFIELD ? instruction :
				consumer1 instanceof PUTFIELD ? consumer1 :
				consumer2 instanceof PUTFIELD ? consumer2 : 
				null
			);

			return fieldAssignmentHistory.getDefinitionsOfObjectFieldAfter(objectID, instructionToAnalyze.getFieldref().getName(), afterID);
			
		}
		else if(instruction instanceof Invoke) {

			return invocationHistory.findInvocationsOnObjectIDAfterEventID((Invoke)instruction, objectID, afterID);
			
		}
		// If its the first instruction, see if the method executed at all.
		else if(instruction == instruction.getCode().getFirstInstruction()) {

			return invocationHistory.getStartIDsOnObjectIDAfterEventID(instruction.getMethod(), objectID, afterID);

		}
		// If its not one of the kinds above, we'll have to do it a bit less efficiently.
		else {

			IntegerVector starts = invocationHistory.getStartIDsOnObjectIDAfterEventID(instruction.getMethod(), objectID, afterID);

			IntegerVector executions = new IntegerVector(10);

			Instruction inst = instruction;
			while(inst != null && !(inst instanceof Invoke || inst instanceof Branch || inst instanceof Definition)) inst = inst.getNext();
			if(inst == null) {
				inst = instruction;
				while(inst != null && !(inst instanceof Invoke || inst instanceof Branch || inst instanceof Definition)) inst = inst.getPrevious();
				if(inst == null) inst = instruction.getCode().getFirstInstruction();
			}
			final Instruction instructionToSearchFor = inst;
			
			// Find executions of the instruction in each method execution.
			for(int i = 0; i < starts.size(); i++) {

				int startID = starts.get(i);
				int returnID = getStartIDsReturnOrCatchID(startID);

				int executionID = 
					findEventBetween(
						startID, 
						returnID < 0 ? getNumberOfEvents() - 1 : returnID, 
						new Trace.SearchCriteria() {
							public boolean matches(int eventID) {
								return getInstruction(eventID) == instructionToSearchFor;
							}
						}
					);
				
				if(executionID >= 0) executions.append(executionID);
				
			}
			
			return executions;

		}
		
	}

	public int findThreadStartBefore(Invoke invoke, int eventID) { return runHistory.getMostRecentExecutionOfBefore(invoke, eventID); }
		
	public FieldAssignmentHistory getFieldAssignmentHistory() { return fieldAssignmentHistory; } 
	public ExceptionHistory getExceptionHistory() { return exceptionHistory; }
	public ArrayHistory getArrayAssignmentHistory() { return arrayHistory; }
	public ClassInitializationHistory getClassInitializationHistory() { return initializationHistory; }
	public InvocationHistory getInvocationHistory() { return invocationHistory; }
	
	/////////////////////////////////////////////////////
	// Narrative management

	/**
	 * Returns true if it didn't already contain this explanation.
	 */
	public boolean addNarrativeExplanation(Explanation explanation) {
		
		return narrative.add(explanation);
		
	}

	public void removeNarrativeExplanation(Explanation explanation) {
		
		narrative.remove(explanation);
		
	}

	public SortedSet<Explanation> getNarrativeExplanations() { 
		
		return Collections.<Explanation>unmodifiableSortedSet(narrative); 
		
	}
	
	public ThreadIterator getThreadIteratorAt(int eventID) { return new ThreadIterator(eventID); }

	public int getThreadFirstEventID(int threadID) { return threads[threadID].getFirstEventID(); }
	public int getThreadLastEventID(int threadID) { return threads[threadID].getLastEventID(); }

	public int getThreadEventIDNearest(int threadID, int eventID) { 
		
		IntegerRange ranges = threads[threadID].eventIDs;
		if(ranges.contains(eventID)) return eventID;
		
		int rangeIndex = ranges.getRangeIndexWithValueLessThanOrEqualTo(eventID);
		return 
			rangeIndex < 0 ? -1 :
			ranges.getUpperBoundOfRange(rangeIndex);
		
	}
			
	
	public String eventToString(int eventID) {
		
		if(eventID < 0) return "invalid eventID";
		
		StringBuffer buf = new StringBuffer();
		buf.append(Util.fillOrTruncateString(getThreadName(getThreadID(eventID)), 10));
		buf.append(Util.fillOrTruncateString(Integer.toString(eventID), 10));
		buf.append(Util.fillOrTruncateString(getKind(eventID).name, 15));
		EventKind kind = getKind(eventID);
		if(kind.isArgument) {
			Instruction inst = getInstruction(eventID);
			buf.append(Instruction.getMethodString(inst.getMethod()));
			int arg = getArgumentLocalIDSet(eventID);
			String name = inst.getCode().getLocalIDNameRelativeToInstruction(arg, inst);
			buf.append(name);
			buf.append(" (argument ");
			buf.append(arg);
			buf.append(") = " + getArgumentValueDescription(eventID));
		}
		else if(!kind.isArtificial)
			buf.append(getInstruction(eventID).toString());
		return buf.toString();
		
	}

	public void printContextAroundEventAtIndex(int eventIndex, int context) {
		
		System.err.println(getContextAroundEventAtIndex(eventIndex, context));

	}
	
	public String getContextAroundEventAtIndex(int eventIndex, int context) {
		
		try {
			StringBuilder builder = new StringBuilder(context * 40 * 2);
			
			builder.append("" + context + " events before and after...\n");
			
			// Look for the NewObjectEvent that occurred after the execution of this invoke special.
			int startIndex = Math.max(0, eventIndex - context);
			int endIndex = Math.min(getNumberOfEvents(), eventIndex + context);
			for(int i = startIndex; i < endIndex; i++) {
				
				builder.append((i == eventIndex ? "*\t" : "\t") + eventToString(i) + "\n");
				
			}
			
			return builder.toString();
			
		}
		catch(Exception e) {
		
			return "[Couldn't create context because of exception during string generation.]";
		
		}

	}

	public String toString() {
	
		StringBuffer buf = new StringBuffer();
		
		buf.append("Execution of " + getMain().getQualifiedNameAndDescriptor());
		buf.append("\n");
		buf.append(metadata.getNumberOfEvents());
		buf.append(" events\n");

		return buf.toString();
		
	}

	private TIntIntHashMap getIntegersProduced(int eventID) { return getValueBlock(eventID).integersProduced; }
	private TIntLongHashMap getLongsProduced(int eventID) { return getValueBlock(eventID).longsProduced; }
	private TIntFloatHashMap getFloatsProduced(int eventID) { return getValueBlock(eventID).floatsProduced; }
	private TIntDoubleHashMap getDoublesProduced(int eventID) {return getValueBlock(eventID).doublesProduced; }
	private TIntIntHashMap getCharactersProduced(int eventID) { return getValueBlock(eventID).charactersProduced; }
	private TIntByteHashMap getBytesProduced(int eventID) { return getValueBlock(eventID).bytesProduced; }
	private TIntShortHashMap getShortsProduced(int eventID) { return getValueBlock(eventID).shortsProduced; }
	private TIntByteHashMap getBooleansProduced(int eventID) { return getValueBlock(eventID).booleansProduced; }
	private TIntIntHashMap getValuesByIncrementID(int eventID) { return getValueBlock(eventID).valuesByIncrementID; }
	
	private TIntIntHashMap getStartByReturnTable(int eventID) { return getCallsBlock(eventID).startIDByReturnID; }
	private TIntIntHashMap getStartByInvocationTable(int eventID) { return getCallsBlock(eventID).startIDByInvocationID; }
	private TIntIntHashMap getInvocationByStartTable(int eventID) { return getCallsBlock(eventID).invocationIDByStartID; }
	private TIntIntHashMap getReturnByStartTable(int eventID) { return getCallsBlock(eventID).returnIDByStartID; }
	private TIntIntHashMap getInitializationByInstantiationTable(int newID) { return getCallsBlock(newID).initIDByNewID; }
	
	public static TraceMetaData getMetaDataFrom(File traceDirectory) {

		try {
			return new TraceMetaData(new File(traceDirectory, Whyline.META_PATH));
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

	}	

	public FieldState getFieldNode(long objectID, FieldInfo field) {
		
		assert objectID > 0;
		Map<String,FieldState> map = fields.get(objectID);
		if(map == null) {
			map = new HashMap<String,FieldState>();
			fields.put(objectID, map);
		}
		FieldState node = map.get(field.getDisplayName(true, -1));
		if(node == null) {
			node = new FieldState(this, objectID, field);
			map.put(field.getDisplayName(true, -1), node);
		}
		return node;
		
	}

	public ObjectState getObjectNode(long objectID) {

		ObjectState obj = objects.get(objectID);
		if(obj == null) {
			
			obj = new ObjectState(this, objectID);
			objects.put(objectID, obj);

		}
		return obj;
		
	}
	
	public File getSaveLocation(String name) {
		
		return new File(Whyline.getSavedTracesFolder(), name);
		
	}
	
	/**
	 * Returns false if the folder already exists.
	 */
	public boolean save(String name, Util.ProgressListener listener) throws IOException {

		Saver saver = new Saver(name, listener);
		return saver.save();
		
	}
		
	private class Saver {

		private static final double FOLDER_COPYING_PROGRESS_WEIGHT = .35;
		private static final double BLOCK_WRITING_WEIGHT = .35;
		private static final double CLASS_COPYING_WEIGHT = .2;
		private static final double OTHER_WEIGHT = .1;

		private final String name;
		private final ProgressListener listener;
		private final File source, destination;

		private long lastUpdate = 0;
		private double percentCopied = 0.0;
		private final int totalBlocks;
		private int blocksWritten = 0;
		private final int totalClasses;
		private int classesWritten = 0;
		private double percentOfOtherDataWritten = 0;
		
		public Saver(String name, Util.ProgressListener listener) {
			
			this.name = name;
			this.listener = listener;
			this.source = getPath();
			this.destination = getSaveLocation(name);
			
			totalBlocks = callBlocks.getNumberOfBlocks() + valueBlocks.getNumberOfBlocks() + idBlocks.getNumberOfBlocks();
			totalClasses = classfilesByID.size();

		}

		private void updateProgress(boolean updateNow) {

			long now = System.currentTimeMillis();
			if(updateNow || now - lastUpdate > 50) {
				lastUpdate = now;
				listener.notice(
						percentCopied < 1.0 ? "Copying trace data..." :
						blocksWritten < totalBlocks ? "Saving events" :
						classesWritten < totalClasses ? "Writing classes (" + (totalClasses - classesWritten) + " remaining)" :
						"Finishing...");
				double progress = 
					FOLDER_COPYING_PROGRESS_WEIGHT * percentCopied + 
					BLOCK_WRITING_WEIGHT * ((double)blocksWritten / totalBlocks) +
					CLASS_COPYING_WEIGHT * ((double)classesWritten / totalClasses) +
					OTHER_WEIGHT * percentOfOtherDataWritten;
				listener.progress(progress);
			}
			
		}
		
		public boolean save() throws IOException {

			if(destination.exists()) return false;
			destination.mkdir();
			
			if(!Whyline.getSavedTracesFolder().exists()) 
				Whyline.getSavedTracesFolder().mkdir();

			listener.notice("Copying folder...");
			
			// Copy all of the files in the recent trace folder to the new trace folder.
			Util.copyFolder(source, destination, new Util.ProgressListener() {
				public void progress(double percent) { 
					percentCopied = percent;
					updateProgress(true);
				}
				public void notice(String notice) { }
			});

			// Delete the serial trace to save space.
			try {
				Util.deleteFolder(new File(destination, Whyline.SERIAL_PATH));
			} catch(IOException e) {}

			File ids = new File(destination, Whyline.IDS_PATH);
			File calls = new File(destination, Whyline.CALLS_PATH);
			File values = new File(destination, Whyline.VALUES_PATH);
			
			// Save all of the blocks for later use (in the recent trace directory)
			ids.mkdirs();
			for(int i = 0; i < idBlocks.getNumberOfBlocks(); i++) {
				idBlocks.getBlock(i).writeToDisk(ids);
				blocksWritten++;
				updateProgress(false);
			}
			calls.mkdirs();
			for(int i = 0; i < callBlocks.getNumberOfBlocks(); i++) {
				callBlocks.getBlock(i).writeToDisk(calls);
				blocksWritten++;
				updateProgress(false);
			}
			values.mkdirs();
			for(int i = 0; i < valueBlocks.getNumberOfBlocks(); i++) {
				valueBlocks.getBlock(i).writeToDisk(values);
				blocksWritten++;
				updateProgress(false);
			}

			listener.notice("Writing classes...");

			// Copy all of the classes loaded into a single file.
			File classesFile = new File(destination, Whyline.CLASSES_PATH);
			DataOutputStream out = Util.getWriterFor(classesFile);
			out.writeInt(classfilesByID.size());
			for(Object o : classfilesByID.getValues()) {
				((Classfile)o).writeToStream(out);
				classesWritten++;
				updateProgress(false);
			}
			out.close();

			// Write all of the threads
			File rangesFolder = new File(destination, Whyline.RANGES_PATH);
			rangesFolder.mkdir();
			for(ThreadTrace t : threads)
				t.writeToDisk(rangesFolder);

			percentOfOtherDataWritten = .2;
			updateProgress(true);

			// Save all of the other histories
			listener.notice("Saving exceptions...");
			Util.save(exceptionHistory, new File(destination, Whyline.EXCEPTIONS_PATH));

			listener.notice("Saving global assignments...");
			Util.save(staticAssignmentHistory, new File(destination, Whyline.STATIC_ASSIGNMENTS_PATH));
			
			listener.notice("Saving field assignments...");
			Util.save(fieldAssignmentHistory, new File(destination, Whyline.FIELD_ASSIGNMENTS_PATH));
			
			listener.notice("Saving array assignments...");
			Util.save(arrayHistory, new File(destination, Whyline.ARRAY_ASSIGNMENTS_PATH));

			listener.notice("Saving instantiations...");
			Util.save(instantiationHistory, new File(destination, Whyline.INSTANTIATIONS_PATH));

			listener.notice("Saving initializations...");
			Util.save(initializationHistory, new File(destination, Whyline.INITIALIZATIONS_PATH));

			listener.notice("Saving runs...");
			Util.save(runHistory, new File(destination, Whyline.RUNS_PATH));

			percentOfOtherDataWritten = .4;
			updateProgress(true);

			listener.notice("Saving invocations...");
			Util.save(invocationHistory, new File(destination, Whyline.INVOCATIONS_PATH));

			percentOfOtherDataWritten = .7;
			updateProgress(true);

			// Save all of the I/O eventIDs for later use. We'll parse them in on the next read.
			listener.notice("Saving I/O...");

			Util.save(ioHistory, new File(destination, Whyline.IO_PATH));

			percentOfOtherDataWritten = .8;
			updateProgress(true);

			(new File(destination, Whyline.ARGUMENTS_PATH)).mkdirs();
			
			{
				DataOutputStream io = Util.getWriterFor(new File(destination, Whyline.IMAGE_PATH));
				io.writeInt(imageData.size());
				TLongObjectIterator<ImageData> images = imageData.iterator();
				while(images.hasNext()) {
					images.advance();
					images.value().write(io);
				}
				io.close();
			}

			{
				DataOutputStream io = Util.getWriterFor(new File(destination, Whyline.KEY_PATH));
				io.writeInt(keyArguments.size());
				TIntObjectIterator<KeyArguments> keys = keyArguments.iterator();
				while(keys.hasNext()) {
					keys.advance();
					io.writeInt(keys.key());
					keys.value().write(io);
				}
				io.close();
			}

			{
				DataOutputStream io = Util.getWriterFor(new File(destination, Whyline.MOUSE_PATH));
				io.writeInt(mouseArguments.size());
				TIntObjectIterator<MouseArguments> mouses = mouseArguments.iterator();
				while(mouses.hasNext()) {
					mouses.advance();
					io.writeInt(mouses.key());
					mouses.value().write(io);
				}
				io.close();
			}
			
			{
				DataOutputStream io = Util.getWriterFor(new File(destination, Whyline.REPAINT_PATH));
				io.writeInt(repaintArguments.size());
				TIntObjectIterator<RepaintArguments> mouses = repaintArguments.iterator();
				while(mouses.hasNext()) {
					mouses.advance();
					io.writeInt(mouses.key());
					mouses.value().write(io);
				}
				io.close();
			}

			{
				DataOutputStream io = Util.getWriterFor(new File(destination, Whyline.CREATE_PATH));
				io.writeInt(createGraphicsArguments.size());
				TIntObjectIterator<CreateGraphicsArguments> mouses = createGraphicsArguments.iterator();
				while(mouses.hasNext()) {
					mouses.advance();
					io.writeInt(mouses.key());
					mouses.value().write(io);
				}
				io.close();
			}

			percentOfOtherDataWritten = 1.0;
			updateProgress(true);
			
			return true;

		}
		
	}
	
	public static abstract class SearchCriteria {
		
		public abstract boolean matches(int eventID);		
		
	}

	private static class ReferencesWaitingForReferentToLoad<ReferenceType> {
		
		private final Hashtable<String,Vector<ReferenceType>> waitersByReferentName = new Hashtable<String,Vector<ReferenceType>>();

		public ReferencesWaitingForReferentToLoad() {}

		public void addReference(String nameOfReferent, ReferenceType reference) {
			
			Vector<ReferenceType> waiters = waitersByReferentName.get(nameOfReferent);
			if(waiters == null) {
				waiters = new Vector<ReferenceType>();
				waitersByReferentName.put(nameOfReferent, waiters);
			}
			waiters.add(reference);				
			
		}

		public Iterable<ReferenceType> removeAndReturnReferencesWaitingFor(String referentName) {
			
			Iterable<ReferenceType> waiters = waitersByReferentName.get(referentName);
			waitersByReferentName.remove(referentName);
			return waiters;
			
		}
		
		public int size() { return waitersByReferentName.size(); }
		
		public String toString() { return waitersByReferentName.keySet().toString(); }
		
	}	
	
	public final class ThreadIterator {
		
		private final ThreadTrace thread;
		
		private final int firstID;
		private final IntegerRange eventIDs;
		private int range;
		private int lower, upper;
		private int last;
		private int value;

		private int startID = -1;
		private int cachedNextInMethodID = -1;
		
		public ThreadIterator(int eventID) {

			this.firstID = eventID;
			thread = threads[getThreadID(eventID)];
			eventIDs = thread.eventIDs;
			this.last = thread.getLastEventID();
			jumpTo(eventID);
			
		}
			
		private int getStartID() {
			if(startID == -1) startID = Trace.this.getStartID(firstID);
			return startID;
		}

		public int current() { return value; }

		public boolean hasNextInThread() { 
			
			return (value + 1 <= upper) || eventIDs.hasRange(range + 1); 
			
		}
		
		public boolean hasPreviousInThread() { return (value - 1 >= lower) || eventIDs.hasRange(range - 1); }

		// Returns true if this iterator has yet to reach this methods return, the end of the trace, or a caught exception thrown during this call.
		public boolean hasNextInMethod() {  
			
			if(!hasNextInThread()) return false;
			if(getKind(value) == RETURN) return false;
			
			int current = value;
			
			int next = nextInThread();
			boolean hasNext = true;
			while(getKind(value) == START_METHOD) {
				MethodInfo method = getInstruction(value).getMethod();
				int returnID = getStartIDsReturnOrCatchID(value);
				if(returnID >= 0) {
					EventKind returnKind = getKind(returnID);
					// If we caught an exception in the method we started in, then jump to 
					if(returnKind == EventKind.RETURN) { 
						jumpTo(returnID);
						if(hasNextInThread())
							nextInThread();
						else {
							hasNext = false;
							break;
						}
					}
					else if(returnKind == EventKind.EXCEPTION_CAUGHT && Trace.this.getStartID(returnID) == getStartID()) jumpTo(returnID);
					else {
						hasNext = false;
						break;
					}
				}
				// If there is no corresponding return, we're done.
				else {
					hasNext = false;
					break;
				}
			}
			// Remember our work.
			if(hasNext) cachedNextInMethodID = value;
			else cachedNextInMethodID = -1;
			// Restore to the current value.
			jumpTo(current);
			
			return hasNext;
			
		}
			
		// Returns true if this iterator has yet to reach this methods start event or the beginning of the trace.
		public boolean hasPreviousInMethod() { return hasPreviousInThread() && value > getStartID(); }

		public int nextInMethod() {
			
			if(cachedNextInMethodID >= 0)
				jumpTo(cachedNextInMethodID);
			else {
			
				nextInThread();
				// If we reach a start method, jump over it.
				while(getKind(value) == START_METHOD) {
					MethodInfo method = getInstruction(value).getMethod();
					int returnID = getStartIDsReturnOrCatchID(value);
					if(returnID >= 0) {
						EventKind returnKind = getKind(returnID);
						// If we caught an exception in the method we started in, then jump to 
						if(returnKind == EventKind.RETURN) { jumpTo(returnID); nextInThread(); }
						else if(returnKind == EventKind.EXCEPTION_CAUGHT && Trace.this.getStartID(returnID) == getStartID()) jumpTo(returnID);
						else throw new NoSuchElementException("There is no next event in this method.");
					} 
					else {
						throw new NoSuchElementException("There is no next event in this method.");
					}
				}
			}
			return value;
		
		}
				
		public int previousInMethod() { 

			if(getKind(value) == EXCEPTION_CAUGHT) {
				MethodInfo method = getInstruction(firstID).getMethod();
				int previous;
				do {
					previous = previousInThread();
				} while(getInstruction(previous).getMethod()  != method);
				return value;
			}

			int previous = previousInThread();
			// While the previous event in the thread is a return and its not the return of this method,
			// jump to the corresponding start of the method and go to the event before it.
			while(getKind(previous) == RETURN) {
				int startID = Trace.this.getReturnStartID(previous);
				if(startID < 0)
					throw new RuntimeException("How can we not know the start ID of the return " + previous);
				else if(startID != getStartID()) {
					jumpTo(startID);
					if(hasPreviousInThread())
						previous = previousInThread();
					else
						break;
				}
				else 
					break;
			}
			return value;
			
		}

		public int nextInThread() {
			
			if(value + 1 <= upper) value++;
			else {
				range++;
				lower = eventIDs.getLowerBoundOfRange(range);
				upper = eventIDs.getUpperBoundOfRange(range);
				value = lower;
			}
			return value;
			
		}
		
		public int previousInThread() {

			if(value - 1 >= lower) value--;
			else {
				range--;
				lower = eventIDs.getLowerBoundOfRange(range);
				upper = eventIDs.getUpperBoundOfRange(range);
				value = upper;
			}
			return value;
			
		}
		
		public void jumpTo(int eventID) {
			
			range = eventIDs.getRangeIndexContaining(eventID);
			
			assert range >= 0 : "" + eventID + " does not occur in the given thread. There must be a bug.";
	
			lower = eventIDs.getLowerBoundOfRange(range);
			upper = eventIDs.getUpperBoundOfRange(range);
			value = eventID;

		}
		
	}
	
	private final class Loader extends Thread {

		private int millisecondsBetweenNotification = 200;

		private Map<String,File[]> cachedPaths = new HashMap<String,File[]>();
		private Map<File,JarFile> jars = new HashMap<File,JarFile>();

		private String status = "";

		// Data structures used for storing and tracking data while loading serial trace
		private final ReferencesWaitingForReferentToLoad<Classfile> classesWaitingForSuperclasses = new ReferencesWaitingForReferentToLoad<Classfile>();
		private final ReferencesWaitingForReferentToLoad<Classfile> classesWaitingForInterfaces = new ReferencesWaitingForReferentToLoad<Classfile>();
		private ArrayList<FieldrefContainer> fieldReferencesToResolve = new ArrayList<FieldrefContainer>(10000);
		private final ArrayList<Invoke> potentialThreadStarts = new ArrayList<Invoke>(10);
		private IDBlock currentIDBlock;
		private ValueBlock currentValueBlock;

		// A list of handlers which will process the execution events as they are read, possibly creating output events or other things with the stream. 
		private final GetGraphicsParser repaintParser = new GetGraphicsParser(Trace.this);
		private final CreateGraphicsParser createGraphicsParser = new CreateGraphicsParser(Trace.this);
		private final GraphicalOutputParser graphicsParser = new GraphicalOutputParser(Trace.this);
		private final TextualOutputParser textParser = new TextualOutputParser(Trace.this);
		private final MouseInputParser mouseParser = new MouseInputParser(Trace.this);
		private final KeyInputParser keyParser = new KeyInputParser(Trace.this);
		private final WindowParser windowParser = new WindowParser(Trace.this);

		// Progress data.
		private int numberOfEventsRead = 0;
		private int numberOfClassesRead = 0;
		private double percentOfIOAnalyzed = 0;
		private double percentOfCallsAnalyzed = 0;
		private double percentOfGraphicsParsed = 0;

		public Loader(int msPerNotification) throws IOException { 
			
			super("Whyline trace loader");
			
			millisecondsBetweenNotification = msPerNotification;
			
			setPriority(Thread.MAX_PRIORITY); 

		}

		private double getPercentOfEventsLoaded() { return (double)numberOfEventsRead / getNumberOfEvents(); }
		
		private double getPercentOfClassesLoaded() { return (double)numberOfClassesRead / getNumberOfClasses(); }
		
		public double getPercentLoaded() {

			// The weights differ depending on the form of data being loaded.
			if(isSaved())
				return
					.6 * getPercentOfClassesLoaded() +
					.1 * percentOfCallsAnalyzed +
					.1 * getPercentOfEventsLoaded() +
					.1 * percentOfIOAnalyzed +
					.2 * percentOfGraphicsParsed;
			else
				return
				.2 * getPercentOfClassesLoaded() +
				.1 * percentOfCallsAnalyzed +
				.4 * getPercentOfEventsLoaded() +
				.2 * percentOfIOAnalyzed +
				.1 * percentOfGraphicsParsed;

		}
		
		private void printTime(long beforeTime, long count, String kind) {
			
			if(!PRINT_PERF) return;
			
			long ms = (System.nanoTime()- beforeTime) / 1000000;
			
			System.err.println(
					Util.fillOrTruncateString(kind, 16) + 
					Util.fillOrTruncateString(Util.commas((int)ms) + " ms", 16) + 
					(count < 0 ? 
						"" : 
						(Util.commas((int)((1000 * count) / ms)) + " per second")));

		}
		
		public void run() { load(); }
		
		private void load() {
		
			Timer timer = new Timer("Trace loading progress notifier", true);
			timer.scheduleAtFixedRate(new TimerTask() {
				public void run() {
					listener.loadingProgress(status, getPercentLoaded());
				}
			}, 0, millisecondsBetweenNotification);

			try {
				
				long beforeLoading = System.nanoTime();

				// Load statistics about the trace.
				status = "Loading trace meta data...";
				listener.loadingMetadata();
				loadMeta();

				long timeBeforeEventReading = System.currentTimeMillis();

				loadSource();

				if(canceled) return;
				
				numberOfClassesRead = 0;
				
				listener.loadingClassFiles();

				if(canceled) return;

				long beforeLoadingClasses = System.nanoTime();

				loadClassfiles();

				if(canceled) return;

				printTime(beforeLoadingClasses, numberOfClassesRead, "classes");
				
				long beforeCreatingCallGraph = System.nanoTime();
				
				associatedThreadStartsWithRunMethods();
				
				int callCount = invocations.size();
				
				createCallGraph();

				if(canceled) return;

				printTime(beforeCreatingCallGraph, callCount, "calls");

				status = "Marking I/O...";

				markOutput();

				if(canceled) return;

				status = "Cleaning up...";

				for(Classfile cf : classesByName.values())
					cf.trim();
				
				listener.doneLoadingClassFiles();

				status = "Preparing to load events...";

				// Must load meta before immutables so we have the right trace length.
				loadObjectTypes();
				loadImmutables();
			
				if(canceled) return;

				listener.doneLoadingMetadata();

				///////////////////////////////////////////////////////
				// Now that we're done loading the static stuff, prepare the dynamic stuff.

				int numberOfBlocks = getNumberOfBlocks(); 
				
				long memoryInUse = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
				long maxMemory = Runtime.getRuntime().maxMemory();
				long memoryFreeForBlocksEtc = maxMemory - memoryInUse;
				
				int bytesPerBlock = 16 * EVENTS_PER_BLOCK;
				int maxBlocksInMemory = (int) ((memoryFreeForBlocksEtc / bytesPerBlock) * FRACTION_OF_MEMORY_FOR_BLOCKS);
				
				idBlocks = new IDBlocks(Trace.this, IDS_FOLDER, numberOfBlocks, maxBlocksInMemory);
				valueBlocks = new ValueBlocks(Trace.this, VALUES_FOLDER, numberOfBlocks, maxBlocksInMemory);
				callBlocks = new CallBlocks(Trace.this, CALLS_FOLDER, numberOfBlocks, maxBlocksInMemory);

				long beforeLoadingEvents = System.nanoTime();

				File serialHistory = new File(getPath(), Whyline.SERIAL_PATH);
				if(serialHistory.exists())
					loadSerialAccessHistory();
				else
					loadRandomAccessHistory();
				
				if(canceled) return;

				printTime(beforeLoadingEvents, getNumberOfEvents(), "events");
				
				long timeAfterEventReading = System.currentTimeMillis();

				// Load the window history
				windowParser.parse(new Util.ProgressListener() {
					public void notice(String notice) { status = notice; }
					public void progress(double percent) { 
						percentOfGraphicsParsed = percent;
						if(listener != null) listener.ioEventsParsed(windowParser.getLastEventIDParsed());
					}
				});
				
				listener.doneLoading(timeAfterEventReading - timeBeforeEventReading);
				
				timer.cancel();

				doneLoading = true;

				printTime(beforeLoading, -1, "total");
				
			} catch (Exception e) {
				e.printStackTrace();
				listener.exceptionDuringLoading(e);
			}
			
			timer.cancel();

			System.gc();

		}
		
		/**
		 * There is no explicit call from java/lang/Thread.start() to a thread's run() method, so we have to add callers manually.
		 */
		private void associatedThreadStartsWithRunMethods() {
			
			for(Invoke invoke : potentialThreadStarts) {

				MethodrefInfo methodref = invoke.getMethodInvoked();
				Classfile classfile = getClassfileByName(methodref.getClassName());

				if(classfile != null) {
					
					if(classfile.isSubclassOf(QualifiedClassName.JAVA_LANG_THREAD)) {
					
						MethodInfo method = classfile.getDeclaredMethodByNameAndDescriptor("run()V");
						
						if(method != null) {
							
							method.addPotentialCaller(invoke);
							
						}

					}
					
				}
				
			}
			
		}

		private File[] resolvePaths(String paths) {

			File[] resolvedPaths = cachedPaths.get(paths);
			if(resolvedPaths == null) {

				String[] pathStrings = paths.split(File.pathSeparator);
				resolvedPaths = new File[pathStrings.length];
				for(int i = 0; i < pathStrings.length; i++)
					resolvedPaths[i] = new File(pathStrings[i]);

				// Remember those with more than one path
				if(pathStrings.length > 1)
					cachedPaths.put(paths, resolvedPaths);
				
			}
			return resolvedPaths;			
			
		}
				
		private DataInputStream getStreamForBundledClass(String classFileName, File file) throws IOException {
			
			JarFile jar = jars.get(file);
			if(jar == null) {
				jar = new JarFile(file);
				jars.put(file, jar);
			}

			ZipEntry entry = jar.getEntry(classFileName);
			if(entry != null)
				return new DataInputStream(jar.getInputStream(entry));
			else
				return null;
			
		}
		
		private boolean resolveClass(String internallyQualifiedClassname, String paths) throws IOException, AnalysisException, JavaSpecificationViolation {

			File[] resolvedPaths = resolvePaths(paths);

			String classFileName = internallyQualifiedClassname.replace('/', File.separatorChar) + ".class";
			String jarFileName = internallyQualifiedClassname + ".class";

			String[] pathStrings = paths.split(File.pathSeparator);
			for(File file : resolvedPaths) {

				// Check for a class in a class hierarchy at the given folder.
				if(file.isDirectory()) {

					File classFilePath = new File(file, classFileName);
					if(classFilePath.exists()) {
						DataInputStream data = Util.getReaderFor(classFilePath);
						loadClassfile(data);
						data.close();
						return true;
					}

				}
				// Check for the given file.
				else if(file.isFile()) {

					String filename = file.getName();
					if(filename.endsWith(".class")) {
						DataInputStream data = Util.getReaderFor(file);
						loadClassfile(data);
						data.close();						
						return true;
					}
					else if(filename.endsWith(".jar")) {
						DataInputStream data = getStreamForBundledClass(jarFileName, file);
						if(data != null) {
							loadClassfile(data);
							data.close();
							return true;
						}
					}
					
				}
				
			}

			return false;
			
		}
		
		private void loadMeta() throws AnalysisException {
			
			try {

				metadata = new TraceMetaData(META);
								
				if(metadata.getNumberOfObjects() > Integer.MAX_VALUE)
					throw new AnalysisException("" + metadata.getNumberOfObjects() + " objects is too many objects for the Whyline to handle with a Java array.");

				instantiationHistory = new InstantiationHistory(Trace.this, metadata.getNumberOfObjects());
				
				threads = new ThreadTrace[getNumberOfThreads()];
				
				// Read how many events were in each thread and create the threads.
				for(int i = 0; i < getNumberOfThreads(); i++) {

					ThreadTrace thread = new ThreadTrace(metadata.getThreadMetaData(i));
					assert threads[thread.getThreadID()] == null;
					threads[thread.getThreadID()] = thread;
					
				}

			} catch(IOException e) {
				e.printStackTrace();
				throw new AnalysisException("The recording's meta data was corrupted, probably because the program crashed or hung before the Whyline could record it properly.");
			}
			
		}
		
		private void createCallGraph() throws IOException {

			// Resolve all of the field references.
			for(FieldrefContainer fieldReferencer : fieldReferencesToResolve) {
			
				Classfile classfile = getClassfileByName(fieldReferencer.getFieldref().getClassname());
				if(classfile != null) {
					
					FieldInfo field = classfile.getFieldByName(fieldReferencer.getFieldref().getName());
					if(field != null) {
						
						if(fieldReferencer instanceof Use)
							field.addUse((Use)fieldReferencer);
						else if(fieldReferencer instanceof Definition)
							field.addDefinition((Definition)fieldReferencer);
						
					}
					
				}
				
			}
			
			if(callGraphIsCached) {

				DataInputStream in = Util.getReaderFor(CALL_GRAPH);
				int numberOfInvocations = in.readInt();
				for(int i = 0; i < numberOfInvocations; i++) {
					Invoke invoke = (Invoke) getInstructionWithID(in.readInt());
					int numberOfMethods = in.readInt();
					for(int j = 0; j < numberOfMethods; j++) {
						Classfile c = getClassfileByID(in.readInt());
						short id = in.readShort();
						if(c != null)
							c.getMethodNumber(id).addPotentialCaller(invoke);
						else
							throw new IOException("While trying to read cached call graph, couldn't find classfile with ID " + id);
					}
					if(i % 1024 == 0) {
						percentOfCallsAnalyzed = (double)i / numberOfInvocations;
						status = "Associating calls (" + Util.commas(numberOfInvocations - i) + " remaining)";
					}
				}
				in.close();
				
			}
			else {
			
				DataOutputStream out = Util.getWriterFor(CALL_GRAPH);
				
				numberOfInvocationInstructions = invocations.size();
				
				int invocationsRemaining = invocations.size();
	
				// Cache these so that we don't have to recreate them.
				HashMap<String, MethodInfo[]> methodsByQualifiedSignature = new HashMap<String,MethodInfo[]>(1000);
				
				out.writeInt(invocations.size());
				
				int count = 0;
				// Then, using this knowledge, associate methods with callers. 
				for(Invoke invoke : invocations) {
	
					String qualifiedSignature = invoke.getMethodInvoked().getQualfiedNameAndDescriptor();
					MethodInfo[] methods = methodsByQualifiedSignature.get(qualifiedSignature);
					if(methods == null) {
						methods = getMethodsFromReference(invoke);
						methodsByQualifiedSignature.put(qualifiedSignature, methods);
					}

					// Cache the calls to disk, writing the invoke's ID, the number of resolved methods, and each method's index.
					out.writeInt(getInstructionIDFor(invoke));
					out.writeInt(methods.length);
					for(MethodInfo method : methods) {
						out.writeInt(classIDs.getIDOfClassname(method.getClassfile().getInternalName()));
						out.writeShort(method.getDeclarationIndex());
						method.addPotentialCaller(invoke);
					}
					
					invocationsRemaining--;
					count++;
					if(count > 1024) {
						count = 0;
						status = "Associating calls (" + Util.commas(invocationsRemaining) + " remaining)";
						percentOfCallsAnalyzed = 1.0 - ((double)invocationsRemaining) / invocations.size();
					}				
	
				}
				
				out.close();
				
			}			
			
		}
		
		private void markOutput() throws IOException { 
			
			if(outputIsCached) {

				DataInputStream in = Util.getReaderFor(OUTPUT);
				
				// Read output instructions
				int numberOfGraphical = in.readInt();
				for(int i = 0; i < numberOfGraphical; i++)
					graphicalOutput.add(getInstructionWithID(in.readInt()));

				int numberOfTextual = in.readInt();
				for(int i = 0; i < numberOfTextual; i++)
					textualOutput.add(getInstructionWithID(in.readInt()));

				// Read output affecting fields and methods
				int numberOfOutputAffectingFields = in.readInt();
				outputAffectingFields = new HashSet<FieldInfo>(numberOfOutputAffectingFields);
				for(int i = 0; i < numberOfOutputAffectingFields; i++) 
					outputAffectingFields.add(getClassfileByID(in.readInt()).getFieldNumber(in.readShort()));

				int numberOfOutputAffectingMethods = in.readInt();
				outputAffectingMethods = new HashSet<MethodInfo>(numberOfOutputAffectingMethods);
				for(int i = 0; i < numberOfOutputAffectingMethods; i++)
					outputAffectingMethods.add(getClassfileByID(in.readInt()).getMethodNumber(in.readShort()));

				int numberOfOutputInvokingMethods = in.readInt();
				outputInvokingMethods = new HashSet<MethodInfo>(numberOfOutputInvokingMethods);
				for(int i = 0; i < numberOfOutputInvokingMethods; i++)
					outputInvokingMethods.add(getClassfileByID(in.readInt()).getMethodNumber(in.readShort()));

			}
			else {

				DataOutputStream out = Util.getWriterFor(OUTPUT);
				
				// Now mark all output instructions and their dependencies as affecting output.
				AffectsOutputAnalyzer outputAnalyzer = new AffectsOutputAnalyzer(Trace.this, new ProgressListener() {
					public void notice(String notice) {status = notice;  }
					public void progress(double percent) { percentOfIOAnalyzed = percent; }
				});

				outputAffectingFields = outputAnalyzer.getFieldsAffectingOutput();
				outputAffectingMethods = outputAnalyzer.getMethodsAffectingOutput();
				outputInvokingMethods = outputAnalyzer.getMethodsInvokingOutput();

				// Write all of the output instruction indices
				out.writeInt(graphicalOutput.size());
				for(Instruction inst : graphicalOutput)
					out.writeInt(getInstructionIDFor(inst));				

				out.writeInt(textualOutput.size());
				for(Instruction inst : textualOutput)
					out.writeInt(getInstructionIDFor(inst));	

				// Write all of the output affecting fields and methods.
				out.writeInt(outputAffectingFields.size());
				for(FieldInfo field : outputAffectingFields) {
					out.writeInt(classIDs.getIDOfClassname(field.getClassfile().getInternalName()));
					out.writeShort(field.getDeclarationIndex());
				}

				out.writeInt(outputAffectingMethods.size());
				for(MethodInfo method : outputAffectingMethods) {
					out.writeInt(classIDs.getIDOfClassname(method.getClassfile().getInternalName()));
					out.writeShort(method.getDeclarationIndex());
				}
				
				out.writeInt(outputInvokingMethods.size());
				for(MethodInfo method : outputInvokingMethods) {
					out.writeInt(classIDs.getIDOfClassname(method.getClassfile().getInternalName()));
					out.writeShort(method.getDeclarationIndex());
				}
				
				out.close();
				
			}
			
			// Mark all output invoking classes
			for(MethodInfo method : outputInvokingMethods)
				outputInvokingClasses.add(method.getClassfile());
			
			// Organize all output instructions by class and method
			for(Instruction textual : textualOutput)
				addToHash(textualOutputByMethodByClass, textual);

			for(Instruction graphical : graphicalOutput)
				addToHash(graphicalOutputByMethodByClass, graphical);
			
		}
		
		private void addToHash(Map<Classfile,Map<MethodInfo,Set<Instruction>>> classes, Instruction inst) {
			
			MethodInfo method = inst.getMethod();
			Classfile classfile = method.getClassfile();
			
			Map<MethodInfo,Set<Instruction>> byMethod = classes.get(classfile);
			if(byMethod == null) {
				byMethod = new HashMap<MethodInfo,Set<Instruction>>();
				classes.put(classfile, byMethod);
			}
			
			Set<Instruction> set = byMethod.get(method);
			if(set == null) {
				set = new HashSet<Instruction>();
				byMethod.put(method, set);
			}
			
			set.add(inst);			
			
		}
		
		private void loadObjectTypes() throws IOException {

			DataInputStream objectTypesData = Util.getReaderFor(OBJECTTYPES);

			objectTypes = new TLongIntHashMap((int)OBJECTTYPES.length() / 12);
			
			while(objectTypesData.available() > 0) {

				long id = objectTypesData.readLong();
				int classID = objectTypesData.readInt();
				objectTypes.put(id, classID);
				
			}

			objectTypesData.close();
			
		}
		
		private void loadImmutables() throws IOException, AnalysisException {
			
			DataInputStream immutableData = Util.getReaderFor(IMMUTABLES);
			while(immutableData.available() > 0) {

				ImmutableKind type = ImmutableKind.intToType(immutableData.readUnsignedByte());

				long id = immutableData.readLong();
				Object object = type.createObject(Trace.this, immutableData);
				
				assert !immutablesByID.containsKey(id) : "We've already read a definition for immutable " + id + ": not replacing " + immutablesByID.get(id) + " with " + object + " of type " + object.getClass(); 

				immutablesByID.put(id, object);
				idsOfImmutables.put(object, id);
				
			}

			immutableData.close();
		
			// Now we know how many threads we have.
			invocationHistory = new InvocationHistory(Trace.this);
			
		}

		private boolean loadSource() {

			ArrayList<File> files = new ArrayList<File>();
			gatherSource(SOURCE_FOLDER, files);

			for(File file : files) {
			
				String qualifiedName = file.getAbsolutePath().substring(SOURCE_FOLDER.getAbsolutePath().length() + 1).replace(File.separatorChar, '/');
				userSourceFiles.add(qualifiedName);
				
			}
			
			return true;
			
		}
		
		private void gatherSource(File folder, ArrayList<File> files) {
			
			for(File file : folder.listFiles()) {
				
				if(file.getName().endsWith(".java"))
					files.add(file);
				else if(file.isDirectory())
					gatherSource(file, files);
				
			}
			
		}

		private void loadClassfiles() throws IOException {
			
			DataInputStream classesData = Util.getReaderFor(CLASSNAMES);

			Map<String,String> pathsByClassnames = new HashMap<String,String>(getNumberOfClasses());
			
			// These are read in the order of being loaded, so classes that were loaded at runtime 
			// can override those that we collected before executing the program.
			for(int i = 0; i < getNumberOfClasses(); i++) {
				String classname = classesData.readUTF();
				String paths = classesData.readUTF();
				pathsByClassnames.put(classname, paths);
			}
			
			classesData.close();
			
			// Now actually find all of the class files.				
			File classesFile = new File(TRACE_FOLDER, Whyline.CLASSES_PATH);
			if(classesFile.exists()) {
				DataInputStream in = Util.getReaderFor(classesFile);
				int classCount = in.readInt();
				int count = 0;
				for(int i = 0; i < classCount; i++) {
					try {
						loadClassfile(in);
					} catch (AnalysisException e) {
						e.printStackTrace();
					} catch (JavaSpecificationViolation e) {
						e.printStackTrace();
					}
					numberOfClassesRead++;
					count++;
					if(count > Byte.MAX_VALUE) {
						count = 0;
						status = "Loading classes (" + Util.commas(getNumberOfClasses() - numberOfClassesRead) + " remaining)";
					}
				}
				in.close();
			}
			else {
				int count = 0;
				for(String classname : pathsByClassnames.keySet()) {
					String paths = pathsByClassnames.get(classname);
					try {
						boolean success = resolveClass(classname, paths);
						if(!success) {
							Whyline.debug("Couldn't resolve " + classname + " from " + paths);
						}
					} catch (AnalysisException e) {
						e.printStackTrace();
					} catch (JavaSpecificationViolation e) {
						e.printStackTrace();
					}
					numberOfClassesRead++;
					count++;
					if(count > Byte.MAX_VALUE) {
						count = 0;
						status = "Resolving classes (" + Util.commas(getNumberOfClasses() - numberOfClassesRead) + " remaining)";
					}
				}				
			}

		}
		
		private Classfile loadClassfile(DataInputStream stream) throws IOException, AnalysisException, JavaSpecificationViolation {

			Classfile classfile = new Classfile(stream, Trace.this);
			
			classfile.setStackDependenciesCache(stackDependenciesCache);
			
			// Add the source file if we have it.
	        if(classfile.hasSourceFileAttribute()) {
	        	
	        	Set<Classfile> unassociatedClasses = classesWaitingForSourceByQualifiedSourceFileName.get(classfile.getQualifiedSourceFileName());
	        	if(unassociatedClasses == null) {
	        		unassociatedClasses = new HashSet<Classfile>();
	        		classesWaitingForSourceByQualifiedSourceFileName.put(classfile.getQualifiedSourceFileName(), unassociatedClasses);
	        	}
	        	unassociatedClasses.add(classfile);
	        	
	        	// If we do have the source, add the classes referenced by the source as classes the user is "familiar" with.
	        	if(classfile.hasSourceFileAttribute() && userSourceFiles.contains(classfile.getQualifiedSourceFileName())) {

	    	        for(ConstantPoolInfo info : classfile.getConstantPool().getItems()) {
	    	        	
	            		if(info instanceof ClassInfo) {
	            			QualifiedClassName classname = ((ClassInfo)info).getName();
	            			familiarClasses.add(classname);
	            			classname.markAsReferencedInFamiliarClass();
	            		}
	            		else if(info instanceof MethodrefInfo) familiarMethods.add(((MethodrefInfo)info).getQualfiedNameAndDescriptor()); 
	            		else if(info instanceof FieldrefInfo) familiarFields.add(((FieldrefInfo)info).getQualifiedName());

	    	        }

	        	}
	        
	        }

	        // Now go through the instructions in search for Invocations, PUTFIELDs, and PUTSTATICs, so that
	        // we can add these references to each of the MethodInfos and FieldInfos. That way we'll have a 
	        // call graph and data definition graph for use in question generation.
	        for(MethodInfo method : classfile.getDeclaredMethods()) {
	        	
	        	numberOfMethods++;
	        	
	        	CodeAttribute code = method.getCode();
	        	if(code != null) {

		        	for(Instruction inst : code.getInstructions()) {

		        		numberOfInstructions++;
		        		
		        		if(inst instanceof Invoke) {

		        			invocations.add((Invoke)inst);
		        			
			    			// Have we loaded the method yet? If so, add this reference as a caller. Otherwise, it needs to wait.
		        			Invoke invoke = (Invoke)inst;
		        			MethodrefInfo methodref = invoke.getMethodInvoked();		        			
			    			if(methodref.getMethodNameAndDescriptor().equals("start()V")) 
			    				potentialThreadStarts.add(invoke);
			    			
		        		}
		        		else if(inst instanceof FieldrefContainer) {

		        			fieldReferencesToResolve.add((FieldrefContainer) inst);

		        		}
		        		else if(inst instanceof Instantiation) {
		        			
		        			QualifiedClassName classname = ((Instantiation)inst).getClassnameOfTypeProduced(); 
		        			
		        			List<Instantiation> allocations = allocationsByClass.get(classname);
		        			if(allocations == null) {
		        				allocations = new ArrayList<Instantiation>(5);
		        				allocationsByClass.put(classname, allocations);
		        			}
		        			allocations.add((Instantiation)inst);	        			

		        		}

		        		// If the instruction is output, add it to the appropriate list.
		        		if(!outputIsCached && inst.isIO()) {
			        		if(TextualOutputParser.handles(classIDs, inst)) textualOutput.add(inst);
			        		else if(GraphicalOutputParser.handles(inst)) graphicalOutput.add(inst);
		        		}
		        		
		        	}

	        	}
	        		        	
	        }
			
	        // Now go through the fields and see if any PUTFIELDs are waiting for a definition
	        numberOfFields += classfile.getDeclaredFields().size();
	        
	        Classfile existingClass = classesByName.put(classfile.getInternalName(), classfile);
	        
	        assert existingClass == null : "But we've already loaded a class named " + classfile.getInternalName() + ". There must be two classfiles with the same name in this trace!";

	        int classID = classIDs.getIDOfClassname(classfile.getInternalName());
			classfilesByID.put(classID, classfile);

			// Handle superclass
			ClassInfo superclassInfo = classfile.getSuperclassInfo();
			if(superclassInfo != null) {
				QualifiedClassName superclassName = superclassInfo.getName();
				Classfile superclass = getClassfileByName(superclassName);
				if(superclass == null) classesWaitingForSuperclasses.addReference(superclassName.getText(), classfile);
				// Otherwise, no need to wait.
				else {
					classfile.setSuperclass(superclass);
					superclass.addSubclass(classfile);
				}
			}

			// Handle subclasses if they're waiting for this one we just loaded.
			Iterable<Classfile> classfilesWaitingForThis = classesWaitingForSuperclasses.removeAndReturnReferencesWaitingFor(classfile.getInternalName().getText());
			if(classfilesWaitingForThis != null) {
				for(Classfile classfileWaiting : classfilesWaitingForThis) {
					classfile.addSubclass(classfileWaiting);
					classfileWaiting.setSuperclass(classfile);
				}
			}
			
			// Handle implementors
			for(ClassInfo interfaze : classfile.getInterfacesImplemented()) {
				
				// FInd the classfile
				Classfile interfaceClassfile = getClassfileByName(interfaze.getName());
				if(interfaceClassfile == null) classesWaitingForInterfaces.addReference(interfaze.getName().getText(), classfile);
				else interfaceClassfile.addImplementor(classfile);
				
			}

			// Handle classfiles waiting for an interface to be loaded
			if(classfile.isInterface()) {
				Iterable<Classfile> classfilesWaitingForThisInterface = classesWaitingForInterfaces.removeAndReturnReferencesWaitingFor(classfile.getInternalName().getText());
				if(classfilesWaitingForThisInterface != null)
					for(Classfile classfileWaiting : classfilesWaitingForThisInterface)
						classfile.addImplementor(classfileWaiting);
			}
			
			return classfile;
			
		}

		private void loadSerialAccessHistory() throws IOException {

			isLoadingSerial = true;
			
			// Make some data structures to hold thread loading state. This will represent the active
			// threads; we'll recreate the array when a thread finishes in order to stop checking it.
			ThreadLoader[] loaders = new ThreadLoader[threads.length];
			for(int i =0; i < threads.length; i++)
				loaders[i] = new ThreadLoader(Trace.this, threads[i]);
			
			// Find the thread that starts with eventID 0.
			ThreadLoader thread = null;
			for(ThreadLoader loader : loaders)
				if(loader.getNextEventID() == 0) {
					thread = loader;
					break;
				}
			
			assert thread != null : "No thread with eventID 0? What's the deal?";
			
			// Used to update progress every so many events 
			int count = 0;
			
			// To avoid the call to getNumberOfEvents() every loop
			int numberOfEvents = getNumberOfEvents();
			
			for(int eventID = 0; eventID < numberOfEvents; eventID++) {
	
				// If the current thread's next event ID is not the ID we expect, find the thread whose next ID is the expected ID.
				if(thread.getNextEventID() != eventID) {

					thread = null;
					for(ThreadLoader loader : loaders)
						if(loader.getNextEventID() == eventID) {
							thread = loader;
							break;
						}

					assert thread != null : "Looked for thread that contained event " + eventID + " but couldn't find it. Number of events = " + numberOfEvents;

				}				

				if(eventID % EVENTS_PER_BLOCK == 0) {
					int blockID = eventID / EVENTS_PER_BLOCK;
					//  Unlock the previous block so it can be cached, and lock the current block.
					if(blockID > 0) idBlocks.unlock(blockID - 1);
					idBlocks.lock(blockID);
					currentIDBlock = getIDBlock(eventID);
					currentValueBlock = getValueBlock(eventID);
				}
				
				// Tell the thread trace that it contains this event.
				thread.thread.eventIDs.include(eventID);

				////////////////////////////////////////////////////////////////////////////////////////////
				// Parse the event, updating all relevant data structures based on its kind.
				
				DataInputStream traceData = thread.data;
				int nextKindFlags = thread.nextKindFlags;
				int threadID = thread.threadID;
				
				// Read the event ID
				assert eventID == thread.nextEventID;
				
				// Shift away the isIO and thread switch flags.
				int kindID = nextKindFlags >>> 2;
							
				// Erase the 6 kind bits, then erase the bottom bit. 
				boolean isIO = (nextKindFlags << 30) >>> 31 == 1;
				
				// Then read the 32 bits representing the class and instruction identifiers
				int instructionID = traceData.readInt();

				int index = eventID - currentIDBlock.firstEventID;
				currentIDBlock.instructionIDs[index] = instructionID;
				currentIDBlock.kindIDs[index] = (byte)kindID;
				
				// First read the event type
				EventKind kind = EventKind.intToEvent(kindID);
				
				// If its output, we read it.
				// If we need it to cache the call stacks, we read it.
				boolean loadImmediately = kind.loadImmediately;
				
				// Handle event specific behaviors, such as inclusion into histories and event arguments.
				switch(kind) {

					// If its a start method event for a class initialization, remember it.
					case START_METHOD :
						
						Instruction start = getInstruction(eventID);
						assert start != null : "Couldn't find instruction for event " + eventID;
						MethodInfo method = start.getMethod(); 
						if(method.isClassInitializer())
							initializationHistory.addClassInitializationEvent(start.getClassfile().getInternalName(), eventID);
						
						invocationHistory.addStartID(eventID, threadID);
						
						break;

					case INVOKE_VIRTUAL :
						invocationHistory.addInvocationID(eventID);
						Invoke virtual = (Invoke)getInstruction(eventID);
						MethodrefInfo methodref = virtual.getMethodInvoked();
						
						if(methodref.getMethodName().equals("start")) {
							QualifiedClassName classname = methodref.getClassName();
							if(classIDs.isOrIsSubclassOf(classname, QualifiedClassName.JAVA_LANG_THREAD))
								runHistory.addThreadStartTime(eventID);
						}
											
						break;

					case INVOKE_STATIC :
						invocationHistory.addInvocationID(eventID);
						MethodrefInfo ref = ((Invoke)getInstruction(eventID)).getMethodInvoked(); 
						if(ref.getMethodName().equals("arraycopy") && ref.getMethodDescriptor().equals("(Ljava/lang/Object;ILjava/lang/Object;II)V"))
							arrayHistory.addArrayCopyID(eventID);

						break;

					case INVOKE_INTERFACE :
						invocationHistory.addInvocationID(eventID);
						break;
						
					case INVOKE_SPECIAL :
						invocationHistory.addInvocationID(eventID);
						
						INVOKESPECIAL invoke = ((INVOKESPECIAL)getInstruction(eventID));
						
						// Track <init>s that consume NEWs. Check this by finding the producer of the <init>'s instance, passing through duplication instuctions.
						if(invoke.isInstanceInitializer()) {
							StackDependencies.Producers instanceProducers  = invoke.getProducersOfArgument(0);
							while(instanceProducers.getNumberOfProducers() == 1 && instanceProducers.getFirstProducer() instanceof Duplication)
								instanceProducers = instanceProducers.getFirstProducer().getProducersOfArgument(0);
							if(instanceProducers.getNumberOfProducers() == 1 && instanceProducers.getFirstProducer() instanceof NEW)
								thread.initIDsWaitingForNewIDs.push(eventID);
						}
						
						break;

					// Remember the time of these exception events.
					case EXCEPTION_CAUGHT : 
					case EXCEPTION_THROWN : exceptionHistory.addExceptionTime(eventID); break;
					
					// Remember the times of these assignment types.
					case PUTSTATIC :  staticAssignmentHistory.addStaticAssignmentID(eventID); break;
					case PUTFIELD : fieldAssignmentHistory.addFieldAssignmentID(eventID); break;
					case SETARRAY : arrayHistory.addArrayAssignmentID(eventID); break;

					case NEW_OBJECT :
						// Read and store the captured ID
						long objectID = traceData.readLong();
						instantiationHistory.addObjectInstantiationID(eventID, objectID); 
						getValueBlock(eventID).longsProduced.put(eventID, objectID);

						QualifiedClassName classInstantiated = null;
						QualifiedClassName classInitialized = null;
						int initID = -1;
						
						// Associate this instantiation with the prior initialization. We loop 
						// because <init> calls can fail and throw exceptions, and never result in a NEW event.
						// This way, we pop <init> calls that didn't succeed because of exceptions.
						do {
							initID = thread.initIDsWaitingForNewIDs.pop();
							classInstantiated = ((NEW)getInstruction(eventID)).getClassInstantiated().getName();
							classInitialized = ((INVOKESPECIAL)getInstruction(initID)).getMethodInvoked().getClassName();
						} while(classInitialized != classInstantiated);
						
						// Remember the association.
						getInitializationByInstantiationTable(eventID).put(eventID, initID);
						break;

					case NEW_ARRAY :
						long arrayID = traceData.readLong();
						currentValueBlock.longsProduced.put(eventID, arrayID);
						instantiationHistory.addArrayInstantiationID(eventID, arrayID); 
						break;
					
					// Read the value argument and store it in the table.
					case IINC : 
						getValuesByIncrementID(eventID).put(eventID, traceData.readInt()); 
						break;
					
					case GETGRAPHICS :
						repaintArguments.put(eventID, 
								new RepaintArguments(
										traceData.readBoolean(), // represents window 
										traceData.readLong(), // object ID 
										traceData.readLong(), // graphicsID 
										traceData.readShort(), traceData.readShort(), // width, height 
										traceData.readShort(), traceData.readShort(), // translateX, translateY
										traceData.readLong(), // windowX, windowY
										traceData.readShort(), traceData.readShort() // windowX, windowY
								));
						break;
						
					case CREATEGRAPHICS : 
						createGraphicsArguments.put(eventID,
								new CreateGraphicsArguments(traceData.readLong(), traceData.readLong())); 
						break;
						
					case MOUSE_EVENT :
						mouseArguments.put(eventID, 
								new MouseArguments(traceData.readLong(), traceData.readInt(), traceData.readInt(), traceData.readInt(), traceData.readInt())); 
						break;
						
					case KEY_EVENT :
						keyArguments.put(eventID, 
								new KeyArguments(traceData.readLong(), traceData.readInt(), traceData.readInt(), traceData.readInt(), traceData.readChar(), traceData.readInt())); 
						break;
						
					case WINDOW :
						traceData.readLong(); 
						break;
					
					case IMAGE_SIZE :
						long imageID = traceData.readLong();
						ImageData data = imageData.get(imageID);
						if(data == null) {
							data = new ImageData(imageID);
							imageData.put(imageID, data);
						}
						data.addSize(eventID, traceData.readInt(), traceData.readInt());
						break;
						
					case INTEGER_PRODUCED :
					case INTEGER_ARG :
						int integerValue = traceData.readInt();
						if(integerValue <= Short.MAX_VALUE && integerValue >= Short.MIN_VALUE) currentValueBlock.shortsProduced.put(eventID, (short)integerValue);
						else currentValueBlock.integersProduced.put(eventID, integerValue);
						break;
						
					case SHORT_PRODUCED :
					case SHORT_ARG :
						currentValueBlock.shortsProduced.put(eventID, traceData.readShort());
						break;

					case BYTE_PRODUCED :
					case BYTE_ARG :
						currentValueBlock.bytesProduced.put(eventID, traceData.readByte());
						break;

					case FLOAT_PRODUCED :
					case FLOAT_ARG :
						currentValueBlock.floatsProduced.put(eventID, traceData.readFloat());
						break;

					case BOOLEAN_PRODUCED :
					case BOOLEAN_ARG :
						currentValueBlock.booleansProduced.put(eventID, (byte)(traceData.readBoolean() ? 1 : 0));
						break;

					case CHARACTER_PRODUCED :
					case CHARACTER_ARG :
						currentValueBlock.charactersProduced.put(eventID, traceData.readChar());
						break;

					case DOUBLE_PRODUCED :
					case DOUBLE_ARG :
						currentValueBlock.doublesProduced.put(eventID, traceData.readDouble());
						break;

					case LONG_PRODUCED :
					case LONG_ARG :
					case OBJECT_PRODUCED :
					case OBJECT_ARG :
						long longValue = traceData.readLong();
						if(longValue <= Short.MAX_VALUE && longValue  >= Short.MIN_VALUE) currentValueBlock.shortsProduced.put(eventID, (short)longValue);
						else if(longValue <= Integer.MAX_VALUE && longValue >= Integer.MIN_VALUE) currentValueBlock.integersProduced.put(eventID, (int)longValue);
						else currentValueBlock.longsProduced.put(eventID, longValue);
						
						if(kind == OBJECT_PRODUCED) {
							Instruction inst = getInstruction(eventID);
							if(inst instanceof INVOKEVIRTUAL) {
								MethodrefInfo mr = ((INVOKEVIRTUAL)inst).getMethodInvoked();
								if(mr.getMethodName().equals("toCharArray") && mr.getClassName() == QualifiedClassName.JAVA_LANG_STRING)
								arrayHistory.addToCharArrayID(eventID);
							}
						}
						
						break;

					default : 

				}
				
				// If this is the kind of event we need to load immediately, we handle the call stacks for it.
				if(loadImmediately) {

					if(thread.callStack == null) 
						// Create a call stack with a listener that updates associated events.
						thread.callStack = new CallStack(Trace.this, threadID, eventID, new CallStack.Listener() {
							public void foundInvocationStartPair(int invocationID, int startID) {
								getInvocationByStartTable(startID).put(startID, invocationID);
								getStartByInvocationTable(invocationID).put(invocationID, startID);
							}
							public void foundStartReturnOrCatchPair(int startID, int returnOrCatchID) {
								getReturnByStartTable(startID).put(startID, returnOrCatchID);
								getStartByReturnTable(returnOrCatchID).put(returnOrCatchID, startID);
							}
						});
					else thread.callStack.handleNextEventID(eventID);
					
				}
				
				if(isIO)
					handleIOEvent(eventID);

				numberOfEventsRead++;
				
				if(eventID == thread.lastEventID)
					thread.nextEventID = -1;
				else
					thread.nextEventID();

				
				//////////////////////////////////////////////////////////////////////////////////////////////
				
				
				// Is the thread now done? Remove it from the active thread list.
				if(thread.isDone()) {
					
					// Release the file.
					thread.data.close();

					// Make a new array without the finished thread loading state.
					ThreadLoader[] newLoaders = new ThreadLoader[loaders.length - 1];
					int newIndexI = 0;
					for(ThreadLoader loader : loaders)
						if(loader != thread)
							newLoaders[newIndexI++] = loader;
					loaders = newLoaders;
					
				}
				
				// Update the loading status if the count has reached the limit
				if(++count == Short.MAX_VALUE) {
					count = 0;
					double fraction = getPercentLoaded();
					int percent = (int)(100 * fraction);
					status = "Reading events (" + Util.commas(getNumberOfEvents() - numberOfEventsRead) + " remaining)";
				}
				
			}

			//  Unlock the last block so it can be cached.
			idBlocks.unlock((getNumberOfEvents() - 1) / EVENTS_PER_BLOCK);
			
			// Save some space by trimming the io histories.
			ioHistory.trimToSize();
			inputHistory.trimToSize();
			outputHistory.trimToSize();
			graphicsHistory.trimToSize();
			renderHistory.trimToSize();
			printsHistory.trimToSize();
			mouseHistory.trimToSize();
			
			// Trim the histories.
			fieldAssignmentHistory.trimToSize();
			arrayHistory.trimToSize();
			instantiationHistory.trimToSize();
			invocationHistory.trimToSize();				

			for(ThreadTrace t : threads)
				t.trimToSize();

			isLoadingSerial = false;
			
		}

		private void loadRandomAccessHistory() throws IOException {
			
			isLoadingSerial = false;
			
			// Mark all of the blocks as created so that the caching mechanism knows to load them.
			idBlocks.markAllBlocksWritten();
			callBlocks.markAllBlocksWritten();
			valueBlocks.markAllBlocksWritten();
			
			File path = getPath();

			// Read the thread histories.
			File ranges = new File(path, Whyline.RANGES_PATH);
			for(ThreadTrace t : threads) {
				status = "Reading " + t.getName() + "...";
				t.readFromDisk(ranges);
			}

			numberOfEventsRead = (int) (.1 * getNumberOfEvents());
			status = "Reading exceptions...";

			Util.load(exceptionHistory, new File(path, Whyline.EXCEPTIONS_PATH));

			status = "Reading global assignments...";

			Util.load(staticAssignmentHistory, new File(path, Whyline.STATIC_ASSIGNMENTS_PATH));

			status = "Reading field assignments...";
			
			Util.load(fieldAssignmentHistory, new File(path, Whyline.FIELD_ASSIGNMENTS_PATH));

			status = "Reading array assignments...";

			Util.load(arrayHistory, new File(path, Whyline.ARRAY_ASSIGNMENTS_PATH));

			status = "Reading instantiations...";

			Util.load(instantiationHistory, new File(path, Whyline.INSTANTIATIONS_PATH));

			status = "Reading initializations...";

			Util.load(initializationHistory, new File(path, Whyline.INITIALIZATIONS_PATH));

			status = "Reading runs...";

			Util.load(runHistory, new File(path, Whyline.RUNS_PATH));
			
			status = "Reading invocations...";
			Util.load(invocationHistory, new File(path, Whyline.INVOCATIONS_PATH));

			numberOfEventsRead = (int) (.3 * getNumberOfEvents());

			{
				DataInputStream io = Util.getReaderFor(new File(path, Whyline.IMAGE_PATH));
				int size = io.readInt();
				imageData.ensureCapacity(size);
				for(int i = 0; i < size; i++) {
					ImageData data = new ImageData(io);
					imageData.put(data.getImageID(), data);
				}
				io.close();
			}

			{
				DataInputStream io = Util.getReaderFor(new File(path, Whyline.KEY_PATH));
				int size = io.readInt();
				keyArguments.ensureCapacity(size);
				for(int i = 0; i < size; i++)
					keyArguments.put(io.readInt(), new KeyArguments(io));
				io.close();
			}

			{
				DataInputStream io = Util.getReaderFor(new File(path, Whyline.MOUSE_PATH));
				int size = io.readInt();
				mouseArguments.ensureCapacity(size);
				for(int i = 0; i < size; i++)
					mouseArguments.put(io.readInt(), new MouseArguments(io));
				io.close();
			}
			
			{
				DataInputStream io = Util.getReaderFor(new File(path, Whyline.REPAINT_PATH));
				int size = io.readInt();
				repaintArguments.ensureCapacity(size);
				for(int i = 0; i < size; i++)
					repaintArguments.put(io.readInt(), new RepaintArguments(io));
				io.close();
			}

			{
				DataInputStream io = Util.getReaderFor(new File(path, Whyline.CREATE_PATH));
				int size = io.readInt();
				createGraphicsArguments.ensureCapacity(size);
				for(int i = 0; i < size; i++)
					createGraphicsArguments.put(io.readInt(), new CreateGraphicsArguments(io));
				io.close();
			}

			numberOfEventsRead = (int) (.4 * getNumberOfEvents());

			status = "Loading I/O events ...";
			DataInputStream io = Util.getReaderFor(new File(path, Whyline.IO_PATH));
			int count = io.readInt();
			for(int i = 0; i < count; i++) {
				handleIOEvent(io.readInt());
				if(i % 128 == 0) {
					double percentDone = (double)i / count;
					numberOfEventsRead = (int) ((percentDone + .4) * getNumberOfEvents());
				}
			}
			io.close();

			numberOfEventsRead = getNumberOfEvents();
			
		}

		private void handleIOEvent(int eventID) {
			
			// Mark the instruction as I/O (as opposed to the event, which would waste space).
			getInstruction(eventID).setIsIO();
			
			switch(getKind(eventID)) {
				case MOUSE_EVENT : 
					mouseParser.handle(eventID); break;
				case KEY_EVENT :
					keyParser.handle(eventID); break;
				case GETGRAPHICS :
					repaintParser.handle(eventID); break;
				case CREATEGRAPHICS :
					createGraphicsParser.handle(eventID); break;
				case WINDOW : 
					windowHistory.add(new WindowVisibilityOutputEvent(Trace.this, eventID)); break;
				default :
					if(graphicsParser.handle(eventID)) {}
					else if(textParser.handle(eventID)) {}
			}

		}

	}

	private static class ThreadLoader {

		private final Trace trace;
		private final ThreadTrace thread;
		private final int threadID;
		private final String name;
		
		private final DataInputStream data;

		// These can't really be interpreted without the help of loadSerialHistory() because we're compressing eventIDs.
		// We only write a full eventID every 256 events.
		private int nextKindFlags = -1;
		private int nextEventID = -1;
		private boolean threadSwitch;
		public final int lastEventID;

		private CallStack callStack;
		
		private IntegerVector newIDsWaitingForObjectIDs = new IntegerVector(20);
		private IntegerVector initIDsWaitingForNewIDs = new IntegerVector(20);

		public ThreadLoader(Trace trace, ThreadTrace thread) throws IOException {

			this.trace = trace;
			this.thread = thread;
			threadID = thread.getThreadID();
			lastEventID = thread.getLastEventID();
			name = thread.getName();
			
			data = 
				new DataInputStream(
					new BufferedInputStream(
							new FileInputStream(thread.getSerialFile()), 65536));

			// Start on the first event, unless the last event is before the first, which means there were no events.
			// If there's data in the serial trace file (it could be empty), read the first event.
			if(data.available() > 0) nextEventID();			
			if(thread.metadata.lastEventID < thread.metadata.firstEventID) nextEventID = -1;
			
			if(threadID >= Short.MAX_VALUE) throw new RuntimeException("Reached maximum number of threads " + Short.MAX_VALUE);				

		}
		
		public boolean isDone() { return nextEventID < 0 || nextEventID > lastEventID; }
		
		public int getNextEventID() { return nextEventID; }
		
		private void nextEventID() throws IOException {

			nextKindFlags = data.readUnsignedByte();
			
			threadSwitch = switched(nextKindFlags);
			if(threadSwitch)
				nextEventID = data.readInt();
			else
				nextEventID++;
			
		}

		// Read the thread switch bit.
		private boolean switched(int kindFlags) { return (kindFlags << 31) >>> 31 == 1; }

		private int getPlaceholderIDFor(int classAndInstructionID) {
			
			int placeholder = -1;
			int index = newIDsWaitingForObjectIDs.size() - 1;
			for(; index >= 0; index--) {
				int candidate = newIDsWaitingForObjectIDs.get(index);
				if(trace.getInstructionID(candidate) == classAndInstructionID) {
					placeholder = candidate;
					newIDsWaitingForObjectIDs.removeValueAt(index);
					break;
				}
			}
			return placeholder;

		}

	}

	private class ThreadTrace implements Saveable {

		private final ThreadMetaData metadata;
		private final IntegerRange eventIDs;

		public ThreadTrace(ThreadMetaData meta) throws IOException {
			
			assert meta != null;
			
			this.metadata = meta;

			eventIDs = new IntegerRange(metadata.numberOfEventsInThread / 3);
			
		}
		
		public int getThreadID() { return metadata.threadID; }
		public String getName() { return metadata.name; }
		public int getFirstEventID() { return eventIDs.getFirst(); }
		public int getLastEventID() { return metadata.lastEventID; }
				
		public void trimToSize() {
			
			eventIDs.trimToSize();
			
		}

		public File getSerialFile() { return new File(SERIAL_THREAD_TRACES_FOLDER, getName() + Whyline.TRACE_FILE_SUFFIX); }

		public void readFromDisk(File folder) throws IOException {

			Util.load(this, new File(folder, getName()));
			
		}

		public void writeToDisk(File folder) throws IOException {

			Util.save(this, new File(folder, getName()));
			
		}

		public String toString() { return metadata.toString(); }

		public void write(DataOutputStream out) throws IOException {

			eventIDs.write(out);
			
		}

		public void read(DataInputStream in) throws IOException {

			eventIDs.read(in);
			
		}
		
	}
			
	private static abstract class Block {

		protected final int firstEventID;

		public abstract void readFromDisk(File folder);
		public abstract void writeToDisk(File folder) throws IOException;
		protected abstract String getBlockName();
		 
		public Block(int firstEventID) {
			
			this.firstEventID = firstEventID;
			
		}
				
		public String toString() { return getBlockName() + "[" + firstEventID + ", " + (firstEventID + EVENTS_PER_BLOCK - 1) + "]"; }

	}
		
	private static class ValueBlock extends Block {
		
		// These store the values produced by various value produced events.
		private final TIntIntHashMap integersProduced = new TIntIntHashMap(10);
		private final TIntLongHashMap longsProduced = new TIntLongHashMap(10);
		private final TIntFloatHashMap floatsProduced = new TIntFloatHashMap(10);
		private final TIntDoubleHashMap doublesProduced = new TIntDoubleHashMap(10);
		private final TIntIntHashMap charactersProduced = new TIntIntHashMap(10);
		private final TIntByteHashMap bytesProduced = new TIntByteHashMap(10);
		private final TIntShortHashMap shortsProduced = new TIntShortHashMap(10);
		private final TIntByteHashMap booleansProduced = new TIntByteHashMap(10);
		private final TIntIntHashMap valuesByIncrementID = new TIntIntHashMap(10); 	

		public ValueBlock(int firstEventID) { super(firstEventID); }
		
		protected String getBlockName() { return Whyline.VALUES_NAME + Integer.toString(firstEventID / EVENTS_PER_BLOCK); }

		public void readFromDisk(File folder) {
			
			File block = new File(folder, getBlockName());
			assert block.exists() : "If we're reading the cached block from disk, then it must be there! But its not!"; 
			
			// First read all of the events.
			try {
				DataInputStream stream = new DataInputStream(new BufferedInputStream(new FileInputStream(block), EVENTS_PER_BLOCK * BYTES_PER_EVENT));

				// Then read all of the cached values.
				Util.readIntIntMap(stream, integersProduced);
				
				int size = stream.readInt();
				longsProduced.ensureCapacity(size);
				for(int i = 0; i < size; i++) {
					longsProduced.put(stream.readInt(), stream.readLong());
				}
				size = stream.readInt();
				floatsProduced.ensureCapacity(size);
				for(int i = 0; i < size; i++)
					floatsProduced.put(stream.readInt(), stream.readFloat());
				size = stream.readInt();
				doublesProduced.ensureCapacity(size);
				for(int i = 0; i < size; i++)
					doublesProduced.put(stream.readInt(), stream.readDouble());
				size = stream.readInt();
				charactersProduced.ensureCapacity(size);
				for(int i = 0; i < size; i++)
					charactersProduced.put(stream.readInt(), stream.readChar());
				size = stream.readInt();
				bytesProduced.ensureCapacity(size);
				for(int i = 0; i < size; i++)
					bytesProduced.put(stream.readInt(), stream.readByte());
				size = stream.readInt();
				shortsProduced.ensureCapacity(size);
				for(int i = 0; i < size; i++)
					shortsProduced.put(stream.readInt(), stream.readShort());
				size = stream.readInt();
				booleansProduced.ensureCapacity(size);
				for(int i = 0; i < size; i++)
					booleansProduced.put(stream.readInt(), stream.readByte());
	
				Util.readIntIntMap(stream, valuesByIncrementID);

				stream.close();
			} catch(IOException e) {
				System.err.println("Tried to read from " + block.getAbsolutePath() + " but...");
				e.printStackTrace();
				System.exit(0);
			}

			// Then trim all of the tables of cached values.
			integersProduced.trimToSize();
			longsProduced.trimToSize();
			floatsProduced.trimToSize();
			doublesProduced.trimToSize();
			charactersProduced.trimToSize();
			bytesProduced.trimToSize();
			shortsProduced.trimToSize();
			booleansProduced.trimToSize();
			valuesByIncrementID.trimToSize(); 	

		}
		
		public void writeToDisk(File folder) throws IOException {

			File block = new File(folder, getBlockName());
			if(block.exists()) return;
			
			// First write all of the events.
			DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(block), EVENTS_PER_BLOCK * BYTES_PER_EVENT));
		
			// Then write all of the cached values.
			Util.writeIntIntMap(stream, integersProduced);
			
			stream.writeInt(longsProduced.size());
			for(int key : longsProduced.keys()) {
				stream.writeInt(key);
				stream.writeLong(longsProduced.get(key));
			}
			stream.writeInt(floatsProduced.size());
			for(int key : floatsProduced.keys()) {
				stream.writeInt(key);
				stream.writeFloat(floatsProduced.get(key));
			}
			stream.writeInt(doublesProduced.size());
			for(int key : doublesProduced.keys()) {
				stream.writeInt(key);
				stream.writeDouble(doublesProduced.get(key));
			}
			stream.writeInt(charactersProduced.size());
			for(int key : charactersProduced.keys()) {
				stream.writeInt(key);
				stream.writeChar(charactersProduced.get(key));
			}
			stream.writeInt(bytesProduced.size());
			for(int key : bytesProduced.keys()) {
				stream.writeInt(key);
				stream.writeByte(bytesProduced.get(key));
			}
			stream.writeInt(shortsProduced.size());
			for(int key : shortsProduced.keys()) {
				stream.writeInt(key);
				stream.writeShort(shortsProduced.get(key));
			}
			stream.writeInt(booleansProduced.size());
			for(int key : booleansProduced.keys()) {
				stream.writeInt(key);
				stream.writeByte(booleansProduced.get(key));
			}
			
			Util.writeIntIntMap(stream, valuesByIncrementID);

			stream.close();

		}
			
	}
	
	private static class CallsBlock extends Block {
		
		private final TIntIntHashMap startIDByReturnID = new TIntIntHashMap(10); 
		private final TIntIntHashMap startIDByInvocationID = new TIntIntHashMap(10); 
		private final TIntIntHashMap invocationIDByStartID = new TIntIntHashMap(10); 
		private final TIntIntHashMap returnIDByStartID = new TIntIntHashMap(10); 
		private final TIntIntHashMap initIDByNewID = new TIntIntHashMap(10);
		
		public CallsBlock(int firstEventID) { super(firstEventID); }

		protected String getBlockName() {  return Whyline.CALLS_NAME + Integer.toString(firstEventID / EVENTS_PER_BLOCK); }

		public void readFromDisk(File folder) {
			
			File block = new File(folder, getBlockName());
			
			// First read all of the events.
			try {
				DataInputStream stream = new DataInputStream(new BufferedInputStream(new FileInputStream(block), EVENTS_PER_BLOCK * BYTES_PER_EVENT));
				Util.readIntIntMap(stream, startIDByReturnID);
				Util.readIntIntMap(stream, startIDByInvocationID);
				Util.readIntIntMap(stream, invocationIDByStartID);
				Util.readIntIntMap(stream, returnIDByStartID);
				Util.readIntIntMap(stream, initIDByNewID);
				stream.close();
			} catch(IOException e) {
				e.printStackTrace();
				System.exit(0);
			}

			startIDByReturnID.trimToSize(); 
			startIDByInvocationID.trimToSize(); 
			invocationIDByStartID.trimToSize(); 
			returnIDByStartID.trimToSize(); 
			initIDByNewID.trimToSize(); 

		}

		public void writeToDisk(File folder) throws IOException {
			
			File block = new File(folder, getBlockName());
			if(block.exists()) 
				block.delete();
			
			DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(block), EVENTS_PER_BLOCK * BYTES_PER_EVENT));
			Util.writeIntIntMap(stream, startIDByReturnID);
			Util.writeIntIntMap(stream, startIDByInvocationID);
			Util.writeIntIntMap(stream, invocationIDByStartID);
			Util.writeIntIntMap(stream, returnIDByStartID);
			Util.writeIntIntMap(stream, initIDByNewID);

			stream.close();

		}

	}
	
	// Represents a series of events. We partition them like this so that we can load and unload them from
	// disk when we run low on memory.
	private static class IDBlock extends Block {
		
		// 8-bit flag representing the value of the EventKind enum.
		private final byte[] kindIDs;
		
		// 32-bit integer representing the class and instruction of each event of the given id.
		private final int[] instructionIDs;		

		public IDBlock(int firstEventID) {
			
			super(firstEventID);
			
			kindIDs = new byte[EVENTS_PER_BLOCK];
			instructionIDs = new int[EVENTS_PER_BLOCK];
			
		}

		protected String getBlockName() {  return Whyline.IDS_NAME + Integer.toString(firstEventID / EVENTS_PER_BLOCK); }
		
		public void readFromDisk(File folder) {
			
			File block = new File(folder, getBlockName());
			assert block.exists() : "If we're reading the cached block from disk, then it must be there! But its not!"; 
			
			// First read all of the events.
			try {
				DataInputStream stream = new DataInputStream(new BufferedInputStream(new FileInputStream(block), EVENTS_PER_BLOCK * BYTES_PER_EVENT));
				for(int i = 0; i < EVENTS_PER_BLOCK; i++) {
					kindIDs[i] = (byte) stream.readUnsignedByte();
					instructionIDs[i] = stream.readInt();
				}

				stream.close();
				
			} catch(IOException e) {
				System.err.println("Tried to read from " + block.getAbsolutePath() + " but...");
				e.printStackTrace();
				System.exit(0);
			}

		}

		public void writeToDisk(File folder) throws IOException {

			File block = new File(folder, getBlockName());
			if(block.exists())  
				block.delete();

			// First write all of the events.
			DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(block), EVENTS_PER_BLOCK * BYTES_PER_EVENT));
			for(int i = 0; i < EVENTS_PER_BLOCK; i++) {
				stream.writeByte(kindIDs[i]);
				stream.writeInt(instructionIDs[i]);
			}

			stream.close();

		}

	}
	
	private static class IDBlocks extends Blocks<IDBlock> {
		
		public IDBlocks(Trace trace, File folder, int numberOfBlocks, int maxBlocksInMemory) {
			
			super(trace, folder, numberOfBlocks, maxBlocksInMemory);
			blocks = new IDBlock[numberOfBlocks];

		}

		protected IDBlock makeBlock(int firstEventID) { return new IDBlock(firstEventID); }

	}

	private static class ValueBlocks extends Blocks<ValueBlock> {
		
		public ValueBlocks(Trace trace, File folder, int numberOfBlocks, int maxBlocksInMemory) {
			
			super(trace, folder, numberOfBlocks, maxBlocksInMemory);
			blocks = new ValueBlock[numberOfBlocks];

		}

		protected ValueBlock makeBlock(int firstEventID) { return new ValueBlock(firstEventID); }

	}

	private static class CallBlocks extends Blocks<CallsBlock> {
		
		public CallBlocks(Trace trace, File folder, int numberOfBlocks, int maxBlocksInMemory) {
			
			super(trace, folder, numberOfBlocks, maxBlocksInMemory);
			blocks = new CallsBlock[numberOfBlocks];

		}

		protected CallsBlock makeBlock(int firstEventID) { return new CallsBlock(firstEventID); }

	}

	private static abstract class Blocks<T extends Block> {
		
		private final Trace trace;
		public T[] blocks;
		private final boolean[] blocksCreated;
		private final boolean[] blocksWritten;
		private final boolean[] blocksLocked;
		private final int[] blockFrequencies;
		private int blocksInMemory;
		private final int maxBlocksInMemory;
		private final File folder; 

		public Blocks(Trace trace, File blockFolder, int numberOfBlocks, int maxBlocksInMemory) {

			this.trace = trace;
			this.folder = blockFolder;
			this.maxBlocksInMemory = maxBlocksInMemory;

			blocksCreated = new boolean[numberOfBlocks];
			blocksWritten = new boolean[numberOfBlocks];
			blocksLocked = new boolean[numberOfBlocks];
			blockFrequencies = new int[numberOfBlocks];

		}

		public void markAllBlocksWritten() {
			
			Arrays.fill(blocksCreated, true);
			Arrays.fill(blocksWritten, true);
			
		}
		
		protected abstract T makeBlock(int firstEventID);
		
		public int getNumberOfBlocks() { return blocks.length; }
		
		public File getFolder() { return folder; }
		
		// Prevents block from being unloaded
		public void lock(int blockID) { blocksLocked[blockID] = true; }
		public void unlock(int blockID) { blocksLocked[blockID] = false; }

		private T getBlockContaining(int eventID) {
			
			return getBlock(eventID / EVENTS_PER_BLOCK);
			
		}

		private T getBlock(int blockID) {
			
			T block = blocks[blockID];
			
			// We accessed the block, so increase its life span.
			blockFrequencies[blockID] = Short.MAX_VALUE;

			// If the block isn't loaded...
			if(block == null) {
				
				int firstEventID = blockID * EVENTS_PER_BLOCK;
				
				// We're about to use some memory. Do we need to make space?
				if(blocksInMemory + 1 > maxBlocksInMemory) {
					
					// Find a block to unload, saving its state.
					int blockIDToUnload = chooseBlockIDToUnload(blockID);
					if(blockIDToUnload >= 0) {

						// Do we need to write this to disk? Only if it hasn't been written, or it has, but we're still loading an unsaved trace.
						// an unsaved trace and the block is not written yet.
						if(!blocksWritten[blockIDToUnload] || (!trace.isSaved() && !trace.isDoneLoading())) {
							try {
								blocks[blockIDToUnload].writeToDisk(folder);
								blocksWritten[blockIDToUnload] = true;
							} catch(IOException e) {
								e.printStackTrace();
							}
						}
						blocks[blockIDToUnload] = null;
						blocksInMemory--;

					}

				}

				// Create a fresh block.
				block = makeBlock(firstEventID);

				// If we've cached the block on disk, then load the block from disk.
				if(blocksCreated[blockID]) {
					assert blocksWritten[blockID]: "Looked for block " + blockID + " but didn't find, but it says its been created, but not cached.";
					block.readFromDisk(folder);
				}
				else {
					assert !blocksCreated[blockID] : "But we've already created a block for " + blockID;
					blocksCreated[blockID] = true;
				}
				
				blocks[blockID] = block;
				blocksInMemory++;

			}
			
			return block;
			
		}

		private int chooseBlockIDToUnload(int desiredBlockID) {

			// Choose a block to unload that's not the requested block and that's already done loading.
			int blockIDToUnload = -1;
			int smallestFrequency = Short.MAX_VALUE;
			for(int i = 0; i < blocks.length; i++) {
				int freq = blockFrequencies[i];
				if(freq > 0) blockFrequencies[i]--;
				if(blocks[i] != null && i != desiredBlockID && !blocksLocked[i] && freq < smallestFrequency) {
					blockIDToUnload = i;
					smallestFrequency = freq;
				}
			}
			return blockIDToUnload;
			
		}
		
	}
		
}
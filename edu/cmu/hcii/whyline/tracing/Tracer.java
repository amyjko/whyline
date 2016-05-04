package edu.cmu.hcii.whyline.tracing;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import edu.cmu.hcii.whyline.Whyline;
import edu.cmu.hcii.whyline.bytecode.QualifiedClassName;
import edu.cmu.hcii.whyline.trace.ImmutableKind;
import edu.cmu.hcii.whyline.util.Util;
import edu.cmu.hcii.whyline.util.WeakLongHashMap;

/**
 * 
 * Calls to Tracer's static methods are inserted by Instrumenter. Tracer manages a few global databases, such as unique identifiers for objects and threads.
 * None of these methods should be called directly; they're all called by code inserted during instrumentation. 
 * 
 * @author Andrew J. Ko
 *
 */
public final class Tracer {

	public static boolean DEBUG_CLASSES_INSTRUMENTED = false;
	
	/**
	 * We may or may not instrument main(), so we set defaults.
	 */
	private static String mainName = "(unknown)";
	private static String[] mainArgs = new String[] {};
	private static long mainTime = -1;
	
	// Writes the definitions of immutable objects
	private static DataOutputStream immutables;

	// Writes the types of each object encountered
	private static DataOutputStream objectTypes;
	
	private static FileWriter debug;
			
	private static WeakLongHashMap<Object> objectIDs = new WeakLongHashMap<Object>(10000);

	private static gnu.trove.TLongHashSet immutablesWritten = new gnu.trove.TLongHashSet(10000);
	
	private static int nextThreadID = 0;
	private static long nextObjectID = 1;			// 0 represents null, so we start at 1.
	private static int nextEventID = 0;
	public static int numberOfClassfiles = 0;
	
	private static boolean shutdown = false, shuttingDown = false;

	// Keep all of the tracers around until shutdown.
	private static Set<ThreadTracer> tracerSet = new HashSet<ThreadTracer>();
	
	static {

		try {

			if(!Whyline.getWorkingSerialHistoryFolder().exists())
				Whyline.getWorkingSerialHistoryFolder().mkdir();
			
			immutables = Util.getWriterFor(Whyline.getWorkingImmutablesFile());
			objectTypes = Util.getWriterFor(Whyline.getWorkingObjectTypesFile());

			// Note that we DON'T delete the class ids file because we need this to persist across executions, since the instrumented classes persist.
			
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					Tracer.shutdown();
				}
			});

			File debugFile = new File("debug.log");
			if(debugFile.exists()) debugFile.delete();
			debugFile.createNewFile();
			debug = new FileWriter(debugFile);
			
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(Whyline.WHYLINE_FAILURE_EXIT_CODE);
		}

	}
	
	private static class ThreadTracer {
		
		public Thread thread;

		public String name;
		public int threadID;
		public long objectID;
		public int numberOfEvents = 0;
		/**
		 *  THIS IS NOT THREAD SAFE! THIS WILL NOT NECESSARILY BE THE FIRST ID!
		 */
		public int firstEventID;
		public int lastEventID = -2;
		
		// == 0 no requests to stop tracing for the given thread ID
		// != 0 one or more requests to stop tracing
		public int stopRequests = 0;

		public DataOutputStream trace;

		public ThreadTracer(Thread thread) throws IOException {
			
			this.thread = thread;
			this.threadID = nextThreadID++;
			this.objectID = getUniqueObjectID(thread);
			
			// THIS IS NOT THREAD SAFE! THIS WILL NOT NECESSARILY BE THE FIRST ID!
			this.firstEventID = nextEventID;
			
		}

		// The reason this init() method is separate from the constructor is that some programs
		// may instrument calls to some of the java.io classes we use below, causing infinite
		// recursion. We need the ThreadLocal to point to this instance BEFORE we call the constructors
		// below, so it doesn't repeatedly initialize a new ThreadTracer() each time.
		private void init() throws FileNotFoundException {

			stop();

			// We need a reasonable file name. Let's construct it from the thread name, but remove all punctuation and numbers and append the unique id.
			StringBuilder nameBuilder = new StringBuilder();
			String threadName = thread.getName();
			for(int i = 0; i < threadName.length(); i++) {
				char c = threadName.charAt(i);
				if(Character.isLetterOrDigit(c))
					nameBuilder.append(c);
			}
			nameBuilder.append("-");
			nameBuilder.append(threadID);
			this.name = nameBuilder.toString();
		
			this.trace = Util.getWriterFor(new File(Whyline.getWorkingSerialHistoryFolder(), name + Whyline.TRACE_FILE_SUFFIX));
								
			tracerSet.add(this);

			start();

		}

		public void stop() { stopRequests++; }

		public void start() { stopRequests--; }
		
		/**
		 * @param iid
		 * @throws IOException
		 * @see #MethodInstrumenter.addLoadAndTraceInstructions()
		 */
		private void header(long iid) throws IOException {
			
			int eventID;
			
			synchronized(Tracer.class) { 
				
				eventID = nextEventID++;
				
				if(eventID % 65536 == 0) {
					stop();
					flush();
					start();
				}
				
			}
			
			numberOfEvents++;

			// We're switching back if the eventID we're writing isn't right after the last one we wrote.
			boolean switchedBack = lastEventID + 1 != eventID;

			lastEventID = eventID;

			// Get the kind and flags bits. The last bit is set if we're switching back, otherwise, its not.
			// If we're switching back, mark the kind and flags byte by negating it.
			byte kindFlags = (byte)((iid >>> 32) << 1);
			if(switchedBack) kindFlags |= 1;
			
			trace.writeByte(kindFlags);

			// If we just switched back to this event, write the event ID.
			if(switchedBack)
				trace.writeInt(eventID);

			// Shift left and then right to erase the high 32 bits to write the class and instruction ID.
			trace.writeInt((int)((iid << 32) >>> 32));
			
		}

	}
	
	private static final ThreadLocal<ThreadTracer> tracers = new ThreadLocal<ThreadTracer>() {
        protected synchronized ThreadTracer initialValue() {
        	try {
				return new ThreadTracer(Thread.currentThread());
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(0);
				return null;
			}
        }
	};
		
	private static class ClassesToCache {
		
		public final Set<QualifiedClassName> classes;
		public final ClassLoader loader;
		
		public ClassesToCache(Set<QualifiedClassName> classes, ClassLoader loader) {
			
			this.classes = classes;
			this.loader = loader;
			
		}
		
	}

	public final static ArrayList<ClassesToCache> classesToCache = new ArrayList<ClassesToCache>();
	
	public static void addClassesToCacheOnExit(Set<QualifiedClassName> classes, ClassLoader loader) {
		
		classesToCache.add(new ClassesToCache(classes, loader));
		
	}

	public static boolean isShutdown() { return shutdown; }

	public static boolean isShuttingDown() { return shuttingDown; }
	
	public static synchronized void shutdown() {

		if(shutdown) return;
		shuttingDown = true;
		
		try {

			// Stop all threads from tracing.
			for(ThreadTracer tracer : tracerSet)
				tracer.stop();
	
			shutdown = true;

			Whyline.debugBreak();
			Whyline.debug("\tSaving thread recordings...");
			
			flush();

			// Close the thread recording streams.
			for(ThreadTracer tracer : tracerSet)
				tracer.trace.close();

			tracerSet.clear();

			immutables.close();
			Whyline.debug("\tClosed immutables file...");
		
			objectTypes.close();
			Whyline.debug("\tClosed types file...");
	
			Agent.classIDs.write();
			
			// Make the working class ids file the new global one.
			Util.copyFile(new File(Whyline.getWorkingTraceFolder(), Whyline.CLASSIDS_PATH), Whyline.getWorkingClassIDsFile());
			
			Whyline.debug("\tDumped class IDs...");
			
			Whyline.debugBreak();
			Whyline.debug("Done writing instrumentation files.");
			Whyline.debugBreak();
	
			if(DEBUG_CLASSES_INSTRUMENTED) 
				ClassInstrumenter.printInstrumentationStatistics();
	
			debug.flush();
			debug.close();
			
			synchronized(Agent.classes) {
				Agent.classes.flush();
				Agent.classes.close();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}

	}

	private static void flush() throws IOException {
		
		try {

			Whyline.getWorkingMetaFile().delete();
			DataOutputStream meta = Util.getWriterFor(Whyline.getWorkingMetaFile());

			meta.writeUTF(mainName);
			meta.writeInt(mainArgs.length);
			for(String arg : mainArgs) meta.writeUTF(arg);
			meta.writeLong(System.currentTimeMillis());
			
			// Write the number of events, objects, classes, and source files.
			meta.writeInt(nextEventID);
			meta.writeLong(nextObjectID);
			meta.writeInt(numberOfClassfiles);
			
			Set<ThreadTracer> tracers = new HashSet<ThreadTracer>(tracerSet);
			
			// Write the number of events in each thread trace
			meta.writeInt(tracers.size());
			for(ThreadTracer tracer : tracers) {
				
				meta.writeUTF(tracer.name);
				meta.writeInt(tracer.threadID);
				meta.writeLong(tracer.objectID);
				meta.writeInt(tracer.numberOfEvents);
				meta.writeInt(tracer.firstEventID);
				meta.writeInt(tracer.lastEventID);

				tracer.trace.flush();
				
			}

			meta.close();
			
			// Flush all of the other file's data
			immutables.flush();
			objectTypes.flush();
			debug.flush();

			Agent.classes.flush();
			Agent.classIDs.write();

		} catch(IOException e) {
			
			e.printStackTrace();
			
		}

	}
	
	private static long getUniqueObjectID(Object o) throws IOException {

		if(o == null) return 0;
		long id = objectIDs.get(o);
		
		// If we already recorded this, return the id
		if(id != 0) return id;

		// Otherwise, make a new id for this new object, put it in the table, and record its type.
		// We need to make sure to write to these streams sequentially, rather than in parallel, so we lock.
		synchronized(Tracer.class) {

			id = nextObjectID++;
			objectIDs.put(o, id);
			String classname = o.getClass().getName();
			int classID = classname == null ? 0 : Agent.classIDs.getIDOfClassname(QualifiedClassName.get(classname));
			objectTypes.writeLong(id);
			objectTypes.writeInt(classID);

		}
		return id;

	}
	
	private static ThreadTracer getActiveThreadTracer() throws FileNotFoundException {
		
		// Don't get the tracer if its initializing; this would lead to infinite recursion.
		ThreadTracer tracer = tracers.get();
		if(tracer.trace == null) tracer.init();
		if(tracer.stopRequests > 0) return null;
		return tracer;
		
	}
	
	///////////////////////////////////////////////////////////////////////////////
	//
	// All of these methods are called at runtime by the instrumentation code inserted.
	//
	///////////////////////////////////////////////////////////////////////////////

	public static void recordMain(String classname, String[] args) throws IOException {

		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer != null) tracer.stop();

		mainName = classname;
		mainArgs = new String[args.length];
		System.arraycopy(args, 0, mainArgs, 0, args.length);
		mainTime = System.currentTimeMillis();

		if(tracer != null) tracer.start();
		
	}
		
	public static void IINC(int value, long iid) throws IOException {

		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;

		tracer.header(iid);
		
		tracer.trace.writeInt(value);

	}

	public static void PUTFIELD(long iid) throws IOException {

		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		
		tracer.header(iid);

	}

	public static void PUTSTATIC(long iid) throws IOException {

		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		
		tracer.header(iid);

	}
	public static void SETARRAY(long iid) throws IOException {

		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		
		tracer.header(iid);

	}
	public static void SETLOCAL(long iid) throws IOException {

		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		
		tracer.header(iid);

	}
	
	public static void COMPINTS(long iid) throws IOException {
		
		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		
		tracer.header(iid);
		
	}

	public static void COMPZERO(long iid) throws IOException {
		
		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		
		tracer.header(iid);
		
	}

	public static void COMPREFS(long iid) throws IOException {
		
		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		
		tracer.header(iid);
		
	}

	public static void COMPNULL(long iid) throws IOException {
		
		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		
		tracer.header(iid);
		
	}

	public static void TABLEBRANCH(long iid) throws IOException {
		
		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		
		tracer.header(iid);
		
	}

	public static void INVOKE_VIRTUAL(long iid) throws IOException {

		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		
		tracer.header(iid);
		
	}

	public static void INVOKE_SPECIAL(long iid) throws IOException {

		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		
		tracer.header(iid);
		
	}

	public static void INVOKE_STATIC(long iid) throws IOException {

		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		
		tracer.header(iid);
		
	}

	public static void INVOKE_INTERFACE(long iid) throws IOException {

		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		
		tracer.header(iid);
		
	}

	public static void RETURN(long iid) throws IOException {

		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		
		tracer.header(iid);
	
	}

	public static void START_METHOD(long iid) throws IOException {

		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		
		tracer.header(iid);
	
	}	

	public static void EXCEPTION_THROWN(long iid) throws IOException {

		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		
		tracer.header(iid);
	
	}	

	public static void EXCEPTION_CAUGHT(long iid) throws IOException {

		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		
		tracer.header(iid);
	
	}	

	public static void MONITOR(long iid) throws IOException {

		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		
		tracer.header(iid);
	
	}	
	
	public static void INTEGER_PRODUCED(int value, long iid) throws IOException {

		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		
		tracer.header(iid);
		
		tracer.trace.writeInt(value);
		
	}

	public static void SHORT_PRODUCED(short value, long iid) throws IOException {

		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		
		tracer.header(iid);
		
		tracer.trace.writeShort(value);			
		
	}

	public static void BYTE_PRODUCED(byte value, long iid) throws IOException {

		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		
		tracer.header(iid);
		
		tracer.trace.writeByte(value);			

	}

	public static void FLOAT_PRODUCED(float value, long iid) throws IOException {
		
		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		
		tracer.header(iid);
		
		tracer.trace.writeFloat(value);			

	}

	public static void BOOLEAN_PRODUCED(boolean value, long iid) throws IOException {
		
		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;

		tracer.header(iid);
		tracer.trace.writeBoolean(value);			
		
	}

	public static void CHARACTER_PRODUCED(char value, long iid) throws IOException {

		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		
		tracer.header(iid);
		
		tracer.trace.writeChar(value);			
		
	}

	public static void DOUBLE_PRODUCED(double value, long iid) throws IOException {

		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		
		tracer.header(iid);
		
		tracer.trace.writeDouble(value);			
		
	}

	public static void LONG_PRODUCED(long value, long iid) throws IOException {

		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		
		tracer.header(iid);
		
		tracer.trace.writeLong(value);			
		
	}

	public static void OBJECT_PRODUCED(Object value, boolean inInit, long iid) throws IOException {

		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		
		tracer.header(iid);

		long id = getUniqueObjectID(value);
		tracer.trace.writeLong(id);

		ImmutableKind kind = null;
		if(!inInit && value != null && !immutablesWritten.contains(id) && (kind = ImmutableKind.classToType(value)) != null)
			synchronized(Tracer.class) {

				// Remember that we wrote it.
				immutablesWritten.add(id);
				
				// Write the type of object and its id, then write the object.
				immutables.writeByte(kind.ordinal());
				immutables.writeLong(id);
		
				tracer.stop();
				kind.writeObject(value, immutables);
				tracer.start();
				
			}

	}

	public static void CONSTANT_INTEGER_PRODUCED(long iid) throws IOException {
		
		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		
		tracer.header(iid);
		
	}

	public static void CONSTANT_SHORT_PRODUCED(long iid) throws IOException {

		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		
		tracer.header(iid);
		
	}

	public static void CONSTANT_BYTE_PRODUCED(long iid) throws IOException {

		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		
		tracer.header(iid);

	}

	public static void CONSTANT_FLOAT_PRODUCED(long iid) throws IOException {
		
		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		
		tracer.header(iid);

	}

	public static void CONSTANT_BOOLEAN_PRODUCED(long iid) throws IOException {
		
		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		
		tracer.header(iid);
		
	}

	public static void CONSTANT_CHARACTER_PRODUCED(long iid) throws IOException {

		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		
		tracer.header(iid);
		
	}

	public static void CONSTANT_DOUBLE_PRODUCED(long iid) throws IOException {

		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		
		tracer.header(iid);
		
	}

	public static void CONSTANT_LONG_PRODUCED(long iid) throws IOException {

		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		
		tracer.header(iid);
		
	}

	/**
	 * ACONST_NULL, LDC, and LDC_W can push nulls and strings, but these are all accessible in the class file and don't need to be traced.
	 * 
	 * @param iid
	 * @throws IOException
	 */
	public static void CONSTANT_OBJECT_PRODUCED(long iid) throws IOException {
		
		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		
		tracer.header(iid);

	}

	public static void THIS_PRODUCED(long iid) throws IOException {
		
		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		
		tracer.header(iid);

	}

	public static void INITIALIZER(long iid) throws IOException {

		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;

		tracer.header(iid);
		
	}
	
	public static void NEW_OBJECT(Object value, boolean inInit, long iid) throws IOException {

		OBJECT_PRODUCED(value, inInit, iid);

	}

	public static void NEW_ARRAY(Object value, long iid) throws IOException {

		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;

		tracer.header(iid);

		DataOutputStream trace = tracer.trace;
		
		trace.writeLong(getUniqueObjectID(value));

	}

	public static void INTEGER_ARG(int value, long iid) throws IOException {
		
		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		
		tracer.header(iid);

		tracer.trace.writeInt(value);
		
	}

	public static void SHORT_ARG(short value, long iid) throws IOException {
		
		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		
		tracer.header(iid);

		tracer.trace.writeShort(value);
		
	}

	public static void BYTE_ARG(byte value, long iid) throws IOException {
		
		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		
		tracer.header(iid);

		tracer.trace.writeByte(value);
				
	}

	public static void FLOAT_ARG(float value, long iid) throws IOException {
		
		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;

		tracer.header(iid);

		tracer.trace.writeFloat(value);

	}

	public static void BOOLEAN_ARG(boolean value, long iid) throws IOException {
		
		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		
		tracer.header(iid);

		tracer.trace.writeBoolean(value);
		
	}

	public static void CHARACTER_ARG(char value, long iid) throws IOException {
		
		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		
		tracer.header(iid);

		tracer.trace.writeChar(value);
		
	}

	public static void DOUBLE_ARG(double value, long iid) throws IOException {
		
		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		
		tracer.header(iid);

		tracer.trace.writeDouble(value);
		
	}

	public static void LONG_ARG(long value, long iid) throws IOException {
		
		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;

		tracer.header(iid);

		tracer.trace.writeLong(value);
		
	}

	public static void OBJECT_ARG(Object value, long iid) throws IOException {
		
		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;

		tracer.header(iid);

		long id = getUniqueObjectID(value);
		tracer.trace.writeLong(id);
		
		if(value instanceof String && !immutablesWritten.contains(id))
			synchronized(Tracer.class) {

				// Remember that we wrote it.
				immutablesWritten.add(id);
				
				// Write the type of object and its id, then write the object.
				immutables.writeByte(ImmutableKind.STRING.ordinal());
				immutables.writeLong(id);
		
				tracer.stop();
				ImmutableKind.STRING.writeObject(value, immutables);
				tracer.start();
				
			}

	}
	
	/**
	 * We use this to mark the precursor to painting a window, to associate graphics contexts
	 * with windows, and to get the current size of the window.
	 * 
	 * There's a reason why "g" down below isn't of type Graphics. It's very important that this doesn't 
	 * reference java.awt.Graphics in the signature, because we need to instrument that class, and KindOfEvent 
	 * is doing reflection on this class, which would cause it to load prematurely.
	 *
	 * @param c
	 * @param iid
	 * @return The Graphics for the object that was requested.
	 * @throws IOException
	 */
	public static java.awt.Graphics2D GETGRAPHICS(Object c, long iid) throws IOException {

		ThreadTracer tracer = getActiveThreadTracer();

		if(tracer != null) tracer.stop();

		java.awt.Graphics2D g = 
			
			(java.awt.Graphics2D) (
					
				c instanceof java.awt.Component ? 
					((java.awt.Component)c).getGraphics() : 
				c instanceof java.awt.image.VolatileImage ?
					// It's okay to call createGraphics() instead of getGraphics() here, because VolatileImage.getGraphics() just calls createGraphics().
					((java.awt.image.VolatileImage)c).createGraphics() :
				c instanceof java.awt.image.BufferedImage ? 
					// It's okay to call createGraphics() instead of getGraphics() here, because BufferedImage.getGraphics() just calls createGraphics().
					((java.awt.image.BufferedImage)c).createGraphics() :
				c instanceof java.awt.Image ?
					((java.awt.Image)c).getGraphics() : null

			);

		if(tracer != null) tracer.start();

		if(tracer == null) return g;

		tracer.stop();
		
		// We aren't necessarily getting graphics on a window; it may be a component within a window
		// that's being repainted. So we need to find out what the translation origin is based on the components window.
		int translateX = 0, translateY = 0;
		int width = 0, height = 0;
		java.awt.Window window = null;
		int windowX = 0, windowY = 0;
		long windowID = 0;

		if(c instanceof java.awt.Component) {
			
			java.awt.Component comp = (java.awt.Component)c;
			
			if(c instanceof java.awt.Window) {
				window = (java.awt.Window)c;
			}
			else {
				window = javax.swing.SwingUtilities.getWindowAncestor(comp);
				if(window != null) {
					java.awt.Point p = javax.swing.SwingUtilities.convertPoint(((java.awt.Component)c).getParent(), comp.getX(), comp.getY(), window.getComponent(0));
					translateX = (int)p.getX();
					translateY = (int)p.getY();
				}
				else {
					translateX = comp.getX();
					translateY = comp.getY();
				}
			}
		}
		
		tracer.header(iid);
		DataOutputStream trace = tracer.trace;

		long id = 0;
		boolean representsWindow = false;
		
		if(c instanceof java.awt.Component) {
			if(window == null) {
				id = getUniqueObjectID(c);
				width = 0;
				height = 0;
			}
			else {
				representsWindow = true;
				id = getUniqueObjectID(window);
				width = window.getComponent(0).getWidth();
				height = window.getComponent(0).getHeight();
			}
		}
		else if(c instanceof java.awt.Image) {
			id = getUniqueObjectID(c);
			width = ((java.awt.Image)c).getWidth(null);
			height = ((java.awt.Image)c).getHeight(null);
		}
		else if(c instanceof java.awt.peer.ComponentPeer){
			id = getUniqueObjectID(c);
			width = ((java.awt.peer.ComponentPeer)c).getBounds().width;
			height = ((java.awt.peer.ComponentPeer)c).getBounds().height;
		}
		else {
			
			debug.write("Not handling " + c + "\n");
			debug.write("Class is " + c.getClass() + "\n");
			debug.write("Instanceof java.awt.Component? " + (c instanceof java.awt.Component) + "\n");

			id = getUniqueObjectID(c);
		}

		if(window != null) {
			windowX = window.getX();
			windowY = window.getY();
			windowID = getUniqueObjectID(window);
		}
		
		trace.writeBoolean(representsWindow);
		
		trace.writeLong(id);
		trace.writeLong(getUniqueObjectID(g));

		trace.writeShort(width);
		trace.writeShort(height);
		
		trace.writeShort(translateX);
		trace.writeShort(translateY);

		trace.writeLong(windowID);

		trace.writeShort(windowX);
		trace.writeShort(windowY);

		tracer.start();

		return g;

	}

	
	/**
	 * There's a reason why "g" down below isn't of type Graphics. It's very important that this doesn't 
	 * reference java.awt.Graphics in the signature, because we need to instrument that class, and KindOfEvent 
	 * is doing reflection on this class, which would cause it to load prematurely.
	 * 
	 * @param g
	 * @param iid
	 * @return The Graphics object that was originally requested.
	 * @throws IOException
	 */
	public static java.awt.Graphics2D CREATEGRAPHICS(Object g, long iid) throws IOException {

		ThreadTracer tracer = getActiveThreadTracer();

		if(tracer != null) tracer.stop();
		
		java.awt.Graphics2D newG = (java.awt.Graphics2D)((java.awt.Graphics)g).create();
		
		if(tracer != null) tracer.start();
		
		if(tracer == null) return newG;

		tracer.header(iid);

		DataOutputStream trace = tracer.trace;

		trace.writeLong(getUniqueObjectID(g));
		trace.writeLong(getUniqueObjectID(newG));
		
		return newG;
		
	}

	public static void MOUSE_EVENT(Object source, int id, int x, int y, int button, long iid) throws IOException {
		
		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;

		tracer.header(iid);

		DataOutputStream trace = tracer.trace;

		trace.writeLong(getUniqueObjectID(source));
		trace.writeInt(id);
		trace.writeInt(x);
		trace.writeInt(y);
		trace.writeInt(button);
		
	}
	
	// We're recording the arguments passed to
	//
	//		public KeyEvent(Component source, int id, long when, int modifiers, int keyCode, char keyChar, int keyLocation);
	//
	// Note that we're skipping "when".
	public static void KEY_EVENT(Object source, int id, int modifiers, int keyCode, char keyChar, int keyLocation, long iid) throws IOException {

		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;

		tracer.header(iid);

		DataOutputStream trace = tracer.trace;
			
		trace.writeLong(getUniqueObjectID(source));
		trace.writeInt(id);
		trace.writeInt(modifiers);
		trace.writeInt(keyCode);
		trace.writeChar(keyChar);
		trace.writeInt(keyLocation);
		
	}
	
	public static void WINDOW(Object window, long iid) throws IOException {
		
		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;

		tracer.header(iid);

		tracer.trace.writeLong(getUniqueObjectID(window));
		
	}

	public static void IMAGE_SIZE(Object image, long iid) throws IOException {
		
		ThreadTracer tracer = getActiveThreadTracer();
		if(tracer == null) return;
		if(image == null) return;

		tracer.stop();

		tracer.header(iid);
		tracer.trace.writeLong(getUniqueObjectID(image));
		tracer.trace.writeInt(((java.awt.Image)image).getWidth(null));
		tracer.trace.writeInt(((java.awt.Image)image).getHeight(null));

		tracer.start();
	
	}

}
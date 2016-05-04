
package edu.cmu.hcii.whyline;

import java.io.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import edu.cmu.hcii.whyline.ui.UI;
import edu.cmu.hcii.whyline.ui.WhylineUI;
import edu.cmu.hcii.whyline.ui.launcher.MacLauncherUI;

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// ANYTHING IMPORTED AND LOADED BY THIS FILE CANNOT BE INSTRUMENTED. 
// IT WILL BE LOADED BEFORE THE APP LOADS!
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Contains the main() that invokes the Whyline as well as a method to invoke the user's program with the tracing agent.
 * 
 * @author Andrew J. Ko
 *
 */
public final class Whyline {
	
	public static final int WHYLINE_FAILURE_EXIT_CODE = 711;

	private static final String JDK_SOURCE_PATH = "JDK_SOURCE_PATH";
	
	private static final String WHYLINE_HOME_PATH_KEY = "whylineHomePath";
	private static String WHYLINE_HOME;
	
	private static File WHYLINE_FOLDER;
	
	// NAMES
	public static final String IDS_NAME = "ids";
	public static final String CALLS_NAME = "calls";
	public static final String VALUES_NAME = "values";
	public static final String CLASSIDS_NAME = "classids";

	public static final String TRACE_FILE_SUFFIX = ".history";

	// TOP-LEVEL TRACE NAMES
	public static final String STATIC_FOLDER_PATH = "static";
	public static final String DYNAMIC_FOLDER_PATH = "dynamic";
	public static final String META_PATH = "meta";
	public static final String USAGE_PATH = "usage.log";

	// DYNAMIC INFO TRACE NAMES
	public static final String IMMUTABLES_PATH = DYNAMIC_FOLDER_PATH + File.separatorChar + "immutables";
	public static final String OBJECT_TYPES_PATH = DYNAMIC_FOLDER_PATH + File.separatorChar + "objects";
	public static final String HISTORY_PATH = DYNAMIC_FOLDER_PATH + File.separatorChar + "history";

	// HISTORY NAMES
	public static final String RANDOM_PATH = HISTORY_PATH + File.separatorChar + "random";
	public static final String SERIAL_PATH = HISTORY_PATH + File.separatorChar + "serial";

	// RANDOM ACCESS NAMES
	public static final String IDS_PATH = RANDOM_PATH + File.separatorChar + IDS_NAME;
	public static final String CALLS_PATH = RANDOM_PATH + File.separatorChar + CALLS_NAME;
	public static final String VALUES_PATH = RANDOM_PATH + File.separatorChar + VALUES_NAME;
	public static final String RANGES_PATH = RANDOM_PATH + File.separatorChar + "ranges";
	public static final String IO_PATH = RANDOM_PATH + File.separatorChar + "io";
	public static final String EXCEPTIONS_PATH = RANDOM_PATH + File.separatorChar + "exceptions";
	public static final String STATIC_ASSIGNMENTS_PATH = RANDOM_PATH + File.separatorChar + "globals";
	public static final String FIELD_ASSIGNMENTS_PATH = RANDOM_PATH + File.separatorChar + "fields";
	public static final String ARRAY_ASSIGNMENTS_PATH = RANDOM_PATH + File.separatorChar + "arrays";
	public static final String INSTANTIATIONS_PATH = RANDOM_PATH + File.separatorChar + "instantiations";
	public static final String INITIALIZATIONS_PATH = RANDOM_PATH + File.separatorChar + "initializations";
	public static final String RUNS_PATH = RANDOM_PATH + File.separatorChar + "runs";
	public static final String INVOCATIONS_PATH = RANDOM_PATH + File.separatorChar + "invocations";
	public static final String ARGUMENTS_PATH = RANDOM_PATH + File.separatorChar + "arguments";
	public static final String IMAGE_PATH = ARGUMENTS_PATH + File.separatorChar + "image";
	public static final String KEY_PATH = ARGUMENTS_PATH + File.separatorChar + "key";
	public static final String MOUSE_PATH = ARGUMENTS_PATH + File.separatorChar + "mouse";
	public static final String REPAINT_PATH = ARGUMENTS_PATH + File.separatorChar + "repaint";
	public static final String CREATE_PATH = ARGUMENTS_PATH + File.separatorChar + "create";

	// STATIC INFO TRACE NAMES
	public static final String SOURCE_PATH = STATIC_FOLDER_PATH + File.separatorChar + "source";
	public static final String CLASSNAMES_PATH = STATIC_FOLDER_PATH + File.separatorChar + "classnames";
	public static final String CLASSES_PATH = STATIC_FOLDER_PATH + File.separatorChar + "classes";
	public static final String CLASSIDS_PATH = STATIC_FOLDER_PATH + File.separatorChar + CLASSIDS_NAME;
	public static final String CALL_GRAPH_PATH = STATIC_FOLDER_PATH + File.separatorChar + "callgraph";
	public static final String OUTPUT_PATH = STATIC_FOLDER_PATH + File.separatorChar + "output";

	// GLOBAL FILE AND FOLDER NAMES
	public static final String CLASS_CACHE_PATH = "classes";
	public static final String ANALYZED_CLASS_CACHE_FOLDER_NAME = "uninstrumented";
	public static final String INSTRUMENTED_CLASS_CACHE_FOLDER_NAME = "instrumented";
	public static final String EXECUTIONS_FILE_NAME = "configurations.xml";
	public static final String SAVED_TRACES_FOLDER_NAME = "saved";
	public static final String WORKING_TRACE_FOLDER_NAME = "recent";

	private static File WORKING_TRACE_FOLDER;
	private static File WORKING_CLASSIDS_FILE;

	private static File WORKING_IMMUTABLES_FILE;
	private static File WORKING_CLASSNAMES_FILE;
	private static File WORKING_META_FILE;
	private static File WORKING_OBJECT_TYPES_FILE;

	private static File WORKING_SERIAL_HISTORY_FOLDER;
	private static File WORKING_SOURCE_FOLDER;

	private static File CLASS_CACHE_FOLDER;	
	private static File UNINSTRUMENTED_CLASS_CACHE_FOLDER;	
	private static File INSTRUMENTED_CLASS_CACHE_FOLDER;	
	private static File SAVED_TRACES_FOLDER;
	
	public static File WHYLINE_JAR_PATH = null;

	// These must absolutely execute at this position in this file, after all of the fields above are initialized.
	static {
		
		try {
		
			initializeSystemProperties();
			String home = loadPreferences();
			setHome(new File(home));
			
		}
		catch(Exception e) {
			e.printStackTrace();
		}

	}
	
	public static final void main(String[] args) throws Exception {
				
		if(args.length  == 0) {

			WHYLINE_JAR_PATH = new File(System.getProperty("user.dir"), "whyline.jar");
			if(!WHYLINE_JAR_PATH.exists()) {
				
				javax.swing.JOptionPane.showMessageDialog(null, "<html>Couldn't find \"whyline.jar\" at \n\n" + WHYLINE_JAR_PATH, "Problem", javax.swing.JOptionPane.ERROR_MESSAGE);
				System.exit(0);
				
			}
			
			if(System.getProperty("os.name").startsWith("Mac")) {
				new MacLauncherUI();
			}
			else {
				new edu.cmu.hcii.whyline.ui.launcher.LauncherUI();
			}
			
		}
		else {

			if(args.length > 1)
				Whyline.debug("Ignoring extra arguments...");

			File trace = new File(args[0]);
			if(!trace.exists()) {

				javax.swing.JOptionPane.showMessageDialog(null, "<html>Couldn't find a trace at "+ trace, "Problem", javax.swing.JOptionPane.ERROR_MESSAGE);
				System.exit(0);

			}

			debug("Loading trace at \"" + trace.getPath());

			new WhylineUI(null, trace, WhylineUI.Mode.WHYLINE);	
			
		}
		
	}
	
	private static void initializeSystemProperties() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		
		System.setProperty("swing.defaultlaf", "javax.swing.plaf.metal.MetalLookAndFeel");
	    System.setProperty("swing.aatext", "true");
	    System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
	    System.setProperty("awt.useSystemAAFontSettings", "true");

	    // Initialize the UI settings.
		UI.class.getName();

	}

	private static Preferences getPreferences() { 
		
		return Preferences.userNodeForPackage(Whyline.class); 
		
	}

	private static String loadPreferences() {

		UI.class.getName();
		
		// Load the preferences, if there are any.
		Preferences userPrefs = getPreferences();

		// If no preference is set, use the current working directory.
		String WHYLINE_HOME = userPrefs.get(WHYLINE_HOME_PATH_KEY, System.getProperty("user.dir") + File.separatorChar + "whyline" + File.separatorChar);

		// Now store the preference, in case it wasn't stored before
		userPrefs.put(WHYLINE_HOME_PATH_KEY, WHYLINE_HOME);
		try {
			userPrefs.flush();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}

		return WHYLINE_HOME;
		
	}
	
	public static String getJDKJavaDocPath() {
		
		return getPreferences().get("JDK_JAVADOC_PATH", "http://java.sun.com/j2se/1.5.0/docs/api/");
		
	}
	
	public static void setJDKSourcePath(String sourcePath) {

		Preferences userPrefs = getPreferences();
		userPrefs.put(JDK_SOURCE_PATH, sourcePath);
		try { userPrefs.flush(); } catch(BackingStoreException e) { e.printStackTrace(); }

	}
	
	public static String getJDKSourcePath() {

		Preferences userPrefs = getPreferences();

		String sourcePath = userPrefs.get(JDK_SOURCE_PATH, null);

		if(sourcePath == null) {
			
			String OSXPathToSource = "/System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Home/src.jar";
			String WindowsPathToSource = "C:/jdk1.5.0/src.zip";

			String osName = System.getProperty("os.name");
			if(osName.contains("OS X")) sourcePath = OSXPathToSource;
			else if(osName.contains("Windows")) sourcePath = WindowsPathToSource;
			
			userPrefs.put(JDK_SOURCE_PATH, sourcePath);
			try { userPrefs.flush(); } catch(BackingStoreException e) { e.printStackTrace(); }
			
		}
			
		return sourcePath;
		
	}
	
	public static void setHome(File home) {

		Preferences userPrefs = getPreferences();
		userPrefs.put(WHYLINE_HOME_PATH_KEY, home.getAbsolutePath());
		try {
			userPrefs.flush();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}

		WHYLINE_FOLDER = home;
		WHYLINE_FOLDER.mkdir();	

		WORKING_TRACE_FOLDER = new File(WHYLINE_FOLDER, WORKING_TRACE_FOLDER_NAME);
		WORKING_CLASSIDS_FILE = new File(Whyline.WHYLINE_FOLDER, CLASSIDS_NAME);
		WORKING_IMMUTABLES_FILE = new File(WORKING_TRACE_FOLDER, IMMUTABLES_PATH);
		WORKING_META_FILE = new File(WORKING_TRACE_FOLDER, META_PATH);
		WORKING_OBJECT_TYPES_FILE = new File(Whyline.WORKING_TRACE_FOLDER, OBJECT_TYPES_PATH);
		WORKING_SERIAL_HISTORY_FOLDER = new File(Whyline.WORKING_TRACE_FOLDER, SERIAL_PATH);
		WORKING_SOURCE_FOLDER  = new File(WORKING_TRACE_FOLDER, SOURCE_PATH);
		WORKING_CLASSNAMES_FILE = new File(WORKING_TRACE_FOLDER, CLASSNAMES_PATH);

		CLASS_CACHE_FOLDER = new File(WHYLINE_FOLDER, CLASS_CACHE_PATH);	
		UNINSTRUMENTED_CLASS_CACHE_FOLDER = new File(getClassCacheFolder(), ANALYZED_CLASS_CACHE_FOLDER_NAME);	
		INSTRUMENTED_CLASS_CACHE_FOLDER = new File(getClassCacheFolder(), INSTRUMENTED_CLASS_CACHE_FOLDER_NAME);	
		SAVED_TRACES_FOLDER = new File(WHYLINE_FOLDER, SAVED_TRACES_FOLDER_NAME);
		
	}
	
	public static File getHome() { return WHYLINE_FOLDER; }

	public static File getWorkingTraceFolder() { return WORKING_TRACE_FOLDER; }
	public static File getWorkingClassIDsFile() { return WORKING_CLASSIDS_FILE; }
	public static File getWorkingImmutablesFile() { return WORKING_IMMUTABLES_FILE; }
	public static File getWorkingClassnamesFile() { return WORKING_CLASSNAMES_FILE; }
	public static File getWorkingMetaFile() { return WORKING_META_FILE; }
	public static File getWorkingObjectTypesFile() { return WORKING_OBJECT_TYPES_FILE; }
	public static File getWorkingSerialHistoryFolder() { return WORKING_SERIAL_HISTORY_FOLDER; }
	public static File getWorkingSourceFolder() { return WORKING_SOURCE_FOLDER; }
	public static File getClassCacheFolder() { return CLASS_CACHE_FOLDER; }
	public static File getUninstrumentedClassCacheFolder() { return UNINSTRUMENTED_CLASS_CACHE_FOLDER; }
	public static File getInstrumentedClassCacheFolder() { return INSTRUMENTED_CLASS_CACHE_FOLDER; }
	public static File getSavedTracesFolder() { return SAVED_TRACES_FOLDER; }

	public static final void debug(String message) {
		
		System.out.println("whyline >\t" + message);
		
	}
	
	public static final void debugBreak() {
		
		debug("");
		System.out.println("whyline >\t>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
		debug("");
		
	}
			
}
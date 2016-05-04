package edu.cmu.hcii.whyline.tracing;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import edu.cmu.hcii.whyline.bytecode.QualifiedClassName;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntObjectHashMap;

/**
 * Loads and stores classes that the Whyline has instrumented during previous executions, to avoid future 
 * instrumentation while a program is loading. The first run can be very slow, but later runs are quite fast.
 * This cache is used by Instrumenter.
 * 
 * @author Andrew J. Ko
 * @see edu.cmu.hcii.whyline.tracing.ClassInstrumenter
 *
 */
public final class ClassIDs {

	private static Pattern spacePattern = Pattern.compile(" ");

	private final TIntHashSet subclassesOfTextualOutputProducers;

	private class ClassID {
		
		public final QualifiedClassName name;
		public QualifiedClassName superclass = null;
		public final int id;
		private long lastModified = 0L;
		public boolean updated = false;
		public boolean instrumented = false;
		public final Set<String> uninstrumentedMethods = new HashSet<String>();
	
		public ClassID(QualifiedClassName name, int id) {
	
			this.name = name;
			this.id = id;
			
		}
				
		public ClassID(String line) throws IOException {
			
			String[] breaks = spacePattern.split(line);

			if(breaks.length < 5)
				throw new IOException("Class ID format error; there was missing data at line " + line);
			
			this.name = QualifiedClassName.get(breaks[0]);
			this.superclass = breaks[1].equals("null") ? null : QualifiedClassName.get(breaks[1]);
			this.id = Integer.parseInt(breaks[2]);
			this.instrumented = Boolean.parseBoolean(breaks[3]);
			this.lastModified = Long.parseLong(breaks[4]);
			
			for(int i = 5; i < breaks.length; i++)
				uninstrumentedMethods.add(breaks[i]);							

		}
		
		public void updateModificationDate(long time) {
			
			if(lastModified != time)
				updated = true;
			
			this.lastModified = time;

		}
		
		public String toString() {

			StringBuilder builder = new StringBuilder();

			builder.append(name.getText());
			builder.append(" ");
			builder.append(superclass == null ? "null" : superclass.getText());
			builder.append(" ");
			builder.append(id);
			builder.append(" ");
			builder.append(instrumented);
			builder.append(" ");
			builder.append(lastModified);

			for(String method : uninstrumentedMethods) {
				builder.append(" ");
				builder.append(method);
			}
			
			return builder.toString();

		}
		
	}
	
	// Its important that this table is synchronized.
	private final HashMap<QualifiedClassName, ClassID> classesByName = new HashMap<QualifiedClassName,ClassID>(10000);
	private final gnu.trove.TIntObjectHashMap<ClassID> classesByID = new gnu.trove.TIntObjectHashMap<ClassID>(10000);

	private final File classIDFile;
	private int nextClassID = 1;
	
	/**
	 * Given a path to search for the id file, loads the mappings from the file if found
	 * 
	 * @param classIDsFile
	 * @throws Exception 
	 */
	public ClassIDs(File classIDsFile) throws IOException {
		
		// Load the class ids file for this trace.
		this.classIDFile = classIDsFile; 
		
		BufferedReader reader  = null;

		if(classIDFile.isFile()) {
			
			reader = new BufferedReader(new FileReader(classIDFile));
			
			if(classIDFile.length() > 0) {
				
				while(true) {
					
					String entry = reader.readLine();
					if(entry == null) break;
					ClassID id = new ClassID(entry);
					
					if(classesByID.containsKey(id.id)) throw new RuntimeException("This classid cache is corrupt! " + classesByID.get(id.id) + " is already assigned id " + id);
					classesByName.put(id.name, id);
					classesByID.put(id.id, id);

					if(id.id > nextClassID) nextClassID = id.id;
					
				}
				nextClassID++;

			}

			reader.close();
		
		}
		else throw new IOException("Couldn't find file " + classIDsFile);
					
		subclassesOfTextualOutputProducers = new TIntHashSet();
		subclassesOfTextualOutputProducers.add(QualifiedClassName.STRING_BUILDER.getID());
		subclassesOfTextualOutputProducers.addAll(getSubclassesOf(QualifiedClassName.STRING_BUILDER).toArray());
		subclassesOfTextualOutputProducers.add(QualifiedClassName.OUTPUT_STREAM.getID());
		subclassesOfTextualOutputProducers.addAll(getSubclassesOf(QualifiedClassName.OUTPUT_STREAM).toArray());
		subclassesOfTextualOutputProducers.add(QualifiedClassName.WRITER.getID());
		subclassesOfTextualOutputProducers.addAll(getSubclassesOf(QualifiedClassName.WRITER).toArray());
		
	}

	public boolean isOrIsSubclassOfTextualOutputProducer(QualifiedClassName name) { return subclassesOfTextualOutputProducers.contains(name.getID()); }
	
	private ClassID getClassID(QualifiedClassName classname) {
		
		ClassID classID = classesByName.get(classname);
		if(classID == null) {

			int id = nextClassID++;
			
			if(nextClassID > MethodInstrumenter.MAXIMUM_CLASS_IDS)
				throw new RuntimeException("Surpassed maximum number of classes supported by the Whyline: " + MethodInstrumenter.MAXIMUM_CLASS_IDS);

			classID = new ClassID(classname, id);
			
			classesByName.put(classname, classID);
			classesByID.put(classID.id, classID);

		}

		return classID;
		
	}
	
	public void markClassnameAsInstrumented(QualifiedClassName classname) { 
		
		getClassID(classname).instrumented = true; 
		
	}
	
	public void markMethodAsNotInstrumented(QualifiedClassName classname, String methodNameAndDescriptor) {
		
		ClassID classID = getClassID(classname);
		classID.uninstrumentedMethods.add(methodNameAndDescriptor);
				
	}
	
	public void markClassnameModificationDate(QualifiedClassName classname, long modificationDate) {
	
		ClassID classID = getClassID(classname);
		classID.updateModificationDate(modificationDate);

	}
	
	public long getModificationDateOfClassname(QualifiedClassName classname) { return getClassID(classname).lastModified; }
	
	public boolean classHasBeenUpdated(QualifiedClassName classname) { return getClassID(classname).updated; }
	
	public QualifiedClassName getNameOfClassID(int classID) { return classesByID.get(classID).name; }
	
	public int getNumberOfClasses() { 
		
		return nextClassID; 
		
	}
	
	public synchronized int getIDOfClassname(QualifiedClassName classname) {

		return getClassID(classname).id;
			
	}

	public boolean classWasInstrumented(QualifiedClassName classname) { 
		
		return getClassID(classname).instrumented;
		
	}
	
	public boolean methodWasInstrumented(QualifiedClassName classname, String methodNameAndDescriptor) {

		return !getClassID(classname).uninstrumentedMethods.contains(methodNameAndDescriptor);
				
	}
	
	/**
	 * This is synchronized so that two threads don't assign a class the same id
	 * 
	 * @param classname
	 */
	public void includeClassName(QualifiedClassName classname) {
		
		if(classname == null) throw new NullPointerException("Can't get the class id of null");

		getClassID(classname);
		
	}
	
	public void markSuperclass(QualifiedClassName classname, QualifiedClassName superclass) {

		getClassID(classname).superclass = superclass;
		
	}

	private final TIntObjectHashMap<TIntHashSet> subclassesBySuperclass = new TIntObjectHashMap<TIntHashSet>(100); 
	
	public QualifiedClassName getBaseClassOf(QualifiedClassName name) {

		ClassID cid = getClassID(name);
		return cid == null || cid.superclass == null || cid.superclass == QualifiedClassName.JAVA_LANG_OBJECT ? name : getBaseClassOf(cid.superclass);
		
	}

	public TIntHashSet getSubclassesOf(QualifiedClassName superclass) {

		TIntHashSet subclasses = subclassesBySuperclass.get(superclass.getID());
		if(subclasses == null) {
		
			subclasses = new TIntHashSet(7);
			subclassesBySuperclass.put(superclass.getID(), subclasses);
		
			for(Object o : classesByID.getValues()) {
				
				ClassID classID = (ClassID)o;
				if(isOrIsSubclassOfHelper(classID.name, superclass))
					subclasses.add(classID.name.getID());			
				
			}
			
		}
		return subclasses;
		
	}
	
	private boolean isOrIsSubclassOfHelper(QualifiedClassName name, QualifiedClassName potentialSuperclass) {

		if(name == null) return false;
		else if(name == potentialSuperclass) return true;
		else return isOrIsSubclassOfHelper(getClassID(name).superclass, potentialSuperclass);
		
	}
	
	public boolean isOrIsSubclassOf(QualifiedClassName name, QualifiedClassName superclass) {
		
		if(name == null) return false;
		if(name == superclass) return true;
		TIntHashSet subclasses = getSubclassesOf(superclass);
		return subclasses.contains(name.getID());		
		
	}
		
	/**
	 * Writes this class name to id mapping to the given folder on disk to the same folder from which its data was read.
	 * 
	 * @throws IOException
	 */
	public synchronized void write() throws IOException {

		if(classIDFile.exists())
			if(!classIDFile.delete()) 
				throw new IOException("Unable to delete the class IDs file.");

		classIDFile.createNewFile();
		FileWriter writer = new FileWriter(classIDFile);

		try {
			
			for(ClassID classID : classesByName.values()) {

				writer.write(classID.toString());
				writer.write("\n");
				
			}
			writer.close();
			
		}
		finally {
			writer.close();
		}
		
	}
		
}

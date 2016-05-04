package edu.cmu.hcii.whyline.trace;

import java.io.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import edu.cmu.hcii.whyline.Whyline;
import edu.cmu.hcii.whyline.bytecode.Classfile;
import edu.cmu.hcii.whyline.source.JavaSourceFile;

/**
 * Represents the source files stored in "src.jar", which comes with a standard install of a Java SDK.
 * 
 * @author Andrew J. Ko
 *
 */ 
public class JDKSource {
	
	private static JarFile sourceJAR;
	
	// Source file names are prefixed by the fully qualified, internally formatted (meaning / for separators) package name.
	private static Map<String,JavaSourceFile> sourceByName = new HashMap<String,JavaSourceFile>();

	private static Set<String> knownSourceFileNames;
	private static String lastPathIndexed = null;
	
	private static Set<String> getSourceFileNames() {
		
		indexSource();
		return knownSourceFileNames;
		
	}
	
	public static JavaSourceFile getSourceFor(Classfile classfile) {

		indexSource();
		
		if(!classfile.hasSourceFileAttribute()) return null;

		String qualifiedName = classfile.getQualifiedSourceFileName();

		return qualifiedName == null ? null : getSourceForQualifiedName(qualifiedName);
		
	}

	private static void indexSource() {

		try {

			String sourcePath = Whyline.getJDKSourcePath();

			// If the path changed since we last indexed, then we will reindex using the new path.
			if(lastPathIndexed != null && sourcePath != null && !lastPathIndexed.equals(sourcePath))
				knownSourceFileNames = null;
			else
				lastPathIndexed = sourcePath;
			
			// If we have already indexed the names, don't bother doing it again.
			if(knownSourceFileNames != null) return;
			else knownSourceFileNames = new HashSet<String>();

			// If there is no path, return.
			if(sourcePath == null) return;
			
			File sourceJARFile = new File(sourcePath);
			if(!sourceJARFile.exists()) return;
	
			sourceJAR = new JarFile(sourceJARFile);
	
			Enumeration<JarEntry> e = sourceJAR.entries();
			while(e.hasMoreElements()) {
				JarEntry entry = e.nextElement();
				if(entry.getName().endsWith(".java")) {
	
					assert entry.getName().startsWith("src/") : "I don't know how to handle jar entry names that don't start with \"src/\" because I can't parse the package name.";
					
					String filename = entry.getName().substring(4);
					
					knownSourceFileNames.add(filename);
					
				}
				
			}
			
		} catch(IOException e) {
			
			e.printStackTrace();
			
		}
		
	}
	
	public static JavaSourceFile getSourceForQualifiedName(String qualifiedSourcefileName) {
		
		JavaSourceFile source = sourceByName.get(qualifiedSourcefileName);
		
		if(source == null && getSourceFileNames().contains(qualifiedSourcefileName)) source = findAndLoadSource(qualifiedSourcefileName);
		
		return source;
		
	}
	
	public static int getNumberOfSourceFilesKnown() { return getSourceFileNames().size(); }
	
	public static Collection<JavaSourceFile> getAllSource() { 
		
		for(String name : getSourceFileNames())
			findAndLoadSource(name);
		
		return Collections.<JavaSourceFile>unmodifiableCollection(sourceByName.values()); 
		
	}
	
	public static JavaSourceFile findAndLoadSource(String qualifiedName) {

		if(sourceJAR == null) return null;

		JavaSourceFile sf = sourceByName.get(qualifiedName);
		
		if(sf != null) return sf;
		
		try {
		
			ZipEntry entry = sourceJAR.getEntry("src/" + qualifiedName);
			DataInputStream stream = new DataInputStream(sourceJAR.getInputStream(entry));
			byte[] bytes = new byte[(int)entry.getSize()];
			stream.readFully(bytes);
			sf = new JavaSourceFile(qualifiedName, bytes, false);

			if(sourceByName.containsKey(qualifiedName)) throw new RuntimeException("Ahhhh! Two source files with the same name! " + qualifiedName);
			
			sourceByName.put(qualifiedName, sf);
			
			return sf;
			
		} catch(IOException e) {}
		
		return null;
		
	}
	
}

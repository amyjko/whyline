/**
 * 
 */
package edu.cmu.hcii.whyline.trace;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.hcii.whyline.bytecode.Classfile;
import edu.cmu.hcii.whyline.bytecode.MethodInfo;
import edu.cmu.hcii.whyline.bytecode.QualifiedClassName;
import edu.cmu.hcii.whyline.util.Util;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class TraceMetaData {

	private final QualifiedClassName mainClassName;
	private final List<String> mainArguments;
	private final long timeOfInvocation;

	private final int numberOfEvents;
	private final long numberOfObjects;
	private final int numberOfClasses;
	private final int numberOfThreads;
	
	private final ThreadMetaData[] threadMetaData;

	private MethodInfo main;
	
	public TraceMetaData(File file) throws IOException {
		
		DataInputStream data = Util.getReaderFor(file);

		this.mainClassName = QualifiedClassName.get(data.readUTF());

		mainArguments = new ArrayList<String>(5);
		int numberOfArgs = data.readInt();
		for(int i = 0; i < numberOfArgs; i++)
			mainArguments.add(data.readUTF());

		timeOfInvocation = data.readLong();

		numberOfEvents = data.readInt();
		assert numberOfEvents <= Integer.MAX_VALUE : "The number of events exceeds the largest possible array we can create."; 

		numberOfObjects = data.readLong();
		numberOfClasses = data.readInt();
		numberOfThreads = data.readInt();

		threadMetaData = new ThreadMetaData[numberOfThreads];
		for(int i = 0; i < numberOfThreads; i++)
			threadMetaData[i] = new ThreadMetaData(data);
		
		data.close();
		
	}

	public int getNumberOfClasses() { return numberOfClasses; }
	public long getNumberOfObjects() { return numberOfObjects; }
	public int getNumberOfEvents() { return numberOfEvents; }
	public int getNumberOfThreads() { return numberOfThreads; }
	public Iterable<String> getMainArguments() { return mainArguments; }
	public ThreadMetaData getThreadMetaData(int i) { return threadMetaData[i]; }

	public MethodInfo getMain(Trace trace) {
		
		if(main == null) {
		
			Classfile classfileWithMain = trace.getClassfileByName(mainClassName);
			main = classfileWithMain != null ? classfileWithMain.getMain() : null;
			
		}
		return main;

	}
	
}
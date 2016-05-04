package edu.cmu.hcii.whyline.trace;

import edu.cmu.hcii.whyline.io.WindowState;
import edu.cmu.hcii.whyline.source.JavaSourceFile;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public interface TraceListener {

	public void loadingMetadata();
	public void doneLoadingMetadata();
	public void loadingClassFiles();
	public void doneLoadingClassFiles();
	public void loadingProgress(String message, double percentLoaded);
	public void doneLoading(long time);
	public void windowParsed(WindowState window);
	public void ioEventsParsed(int inputTime);
	public void exceptionDuringLoading(Exception e);

	public void additionalSourceLoaded(JavaSourceFile source);
	
	public void blockEvent(boolean loaded, int blockID, int frequency);
	
}

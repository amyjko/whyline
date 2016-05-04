package edu.cmu.hcii.whyline.tracing;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public interface ProcessListener {

	public void processDone(String message, int exitValue);
	public void outputStream(String out);
	public void errorStream(String err);

}
package edu.cmu.hcii.whyline.tracing;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import edu.cmu.hcii.whyline.Whyline;

/**
 * Runs a Java process in a platform independent manner.
 * 
 * @author Andrew J. Ko
 *
 */ 
public final class Runner {

	private final String[] args;
	private Process process;
	private final ProcessListener listener;
	private final ExecutionThread thread;
	
	public Runner(File whylineJARPath, ExecutionConfiguration config, AgentOptions options, ProcessListener listener) {
		
		this.args = config.getExecutionCommand(whylineJARPath, options, false);
		this.listener = listener;
		thread = new ExecutionThread();
		
	}

	public void execute() {

		thread.start();

	}
	
	public void kill() {
		
		if(process != null) 
			process.destroy();
		
	}
	
	/**
	 * This thread waits for the app to finish and notifies the listener when its done.
	 * @author Andrew J. Ko
	 *
	 */
	private class ExecutionThread extends Thread {
		
		public void run() {
			
			process = null;
			try {
				process = Runtime.getRuntime().exec(args);
			} catch (IOException e2) {
				e2.printStackTrace();
				listener.processDone(e2.toString(), -1);
				return;
			}
		
		    StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), Type.ERR);            
		    StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), Type.OUT);
		
		    int exitValue = 0;
			try {
		
		        errorGobbler.start();
		        outputGobbler.start();
		                                
		        // Wait for the procedure to finish
		        exitValue = process.waitFor();
		        
		        if(exitValue == Whyline.WHYLINE_FAILURE_EXIT_CODE) {
		        	
		        	String errorMessage = "The program failed because the Whyline messed it up. Don't worry it's not your fault.";
		        	if(errorGobbler.getError() != null) errorMessage = errorMessage + "\nThe error stream had this line in it: " + errorGobbler.getError();
		        	if(outputGobbler.getError() != null) errorMessage = errorMessage + "\nThe output stream had this line in it: " + outputGobbler.getError();
		        	listener.processDone(errorMessage, exitValue);
		        	return;
		        	
		        }
		        
			} catch (InterruptedException e) {
				e.printStackTrace();
	        	listener.processDone(e.getMessage(), exitValue);
	        	return;
			}
			
			listener.processDone(null, exitValue);
			
		}
				
	}
	
	private enum Type { OUT, ERR };

	/**
	 * Listens to the output and error streams of the user's program and prints them to this processes output and error streams.
	 * 
	 * @author Andrew J. Ko
	 *
	 */
	private class StreamGobbler extends Thread {
		
		private final InputStream is;
	    private final Type type;
	    
	    private String error = null;
	    
	    StreamGobbler(InputStream is, Type type)
	    {
	    	setPriority(Thread.MIN_PRIORITY);
	        this.is = is;
	        this.type = type;
	    }
	    
	    public String getError() { return error; }
	    
	    public void run()
	    {
	        try {
	        
	        	InputStreamReader isr = new InputStreamReader(is);
	            BufferedReader br = new BufferedReader(isr);
	            String line=null;
	            while ( (line = br.readLine()) != null) {
	            	
//	            	System.out.println(line);
	            	if(type == Type.OUT)
	            		listener.outputStream(line);
	            	else if(type == Type.ERR)
	            		listener.errorStream(line);
	            	
	            	if(line.contains("Exception in thread"))
	            		error = line;
	            	
	            }
	        
	        } catch (IOException ioe) {
	            	ioe.printStackTrace();  
	        }

	    }
	    
	}
}


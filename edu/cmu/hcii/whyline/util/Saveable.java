package edu.cmu.hcii.whyline.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public interface Saveable {

	public void write(DataOutputStream out) throws IOException;
	public void read(DataInputStream in) throws IOException;
	
}

package edu.cmu.hcii.whyline.trace;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class CreateGraphicsArguments {

	public final long oldID, newID;
	
	public CreateGraphicsArguments(long oldID, long newID) {
		
		this.oldID = oldID;
		this.newID = newID;

	}

	public CreateGraphicsArguments(DataInputStream io) throws IOException {

		oldID = io.readLong();
		newID = io.readLong();
		
	}

	public void write(DataOutputStream io) throws IOException {

		io.writeLong(oldID);
		io.writeLong(newID);
		
	}

}

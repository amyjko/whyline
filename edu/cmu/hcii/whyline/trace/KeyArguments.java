package edu.cmu.hcii.whyline.trace;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class KeyArguments {

	public  final long source;
	public  final int type, modifiers, keyCode, keyChar, keyLocation;
	
	public KeyArguments(long source, int id, int modifiers, int keyCode, int keyChar, int keyLocation) {

		this.source = source;
		this.type = id;
		this.modifiers = modifiers;
		this.keyCode = keyCode;
		this.keyChar = keyChar;
		this.keyLocation = keyLocation;
		
	}

	public KeyArguments(DataInputStream io) throws IOException {

		source = io.readLong();
		type = io.readInt();
		modifiers = io.readInt();
		keyCode = io.readInt();
		keyChar = io.readInt();
		keyLocation = io.readInt();
		
	}

	public void write(DataOutputStream io) throws IOException {

		io.writeLong(source);
		io.writeInt(type);
		io.writeInt(modifiers);
		io.writeInt(keyCode);
		io.writeInt(keyChar);
		io.writeInt(keyLocation);
		
	}

}

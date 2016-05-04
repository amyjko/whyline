package edu.cmu.hcii.whyline.trace;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class MouseArguments {

	public final long source; 
	public final int type, x, y, button;
	
	public MouseArguments(long source, int id, int x, int y, int button) {
		
		this.source = source;
		this.type = id;
		this.x = x;
		this.y = y;
		this.button = button;
		
	}

	public MouseArguments(DataInputStream io) throws IOException {

		source = io.readLong();
		type = io.readInt();
		x = io.readInt();
		y = io.readInt();
		button = io.readInt();
		
	}

	public void write(DataOutputStream io) throws IOException {

		io.writeLong(source);
		io.writeInt(type);
		io.writeInt(x);
		io.writeInt(y);
		io.writeInt(button);
		
	}

}

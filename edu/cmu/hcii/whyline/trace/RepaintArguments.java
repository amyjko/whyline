package edu.cmu.hcii.whyline.trace;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class RepaintArguments {

	public final boolean representsWindow;
	public final long objectID, graphicsID, windowID;
	public final int width, height;
	public final int translateX, translateY, windowX, windowY;
	
	public RepaintArguments(
			boolean representsWindow, 
			long oID, 
			long gID, 
			short width, short height, 
			short translateX, short translateY,
			long wID,
			short windowX, short windowY
			) {

		this.representsWindow = representsWindow;
		this.objectID = oID;
		this.graphicsID = gID;
		this.width = width;
		this.height = height;
		this.translateX = translateX;
		this.translateY = translateY;
		this.windowID = wID;
		this.windowX = windowX;
		this.windowY = windowY;
		
	}

	public RepaintArguments(DataInputStream io) throws IOException {

		representsWindow = io.readBoolean();
		objectID = io.readLong();
		graphicsID = io.readLong();
		width = io.readInt();
		height = io.readInt();
		translateX = 	io.readShort();
		translateY = io.readShort();
		windowID = io.readLong();
		windowX = io.readShort();
		windowY = io.readShort();
		
	}

	public void write(DataOutputStream io) throws IOException {

		io.writeBoolean(representsWindow);
		io.writeLong(objectID);
		io.writeLong(graphicsID);
		io.writeInt(width);
		io.writeInt(height);
		io.writeShort(translateX);
		io.writeShort(translateY);
		io.writeLong(windowID);
		io.writeShort(windowX);
		io.writeShort(windowY);
		
	}

}

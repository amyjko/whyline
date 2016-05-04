/**
 * 
 */
package edu.cmu.hcii.whyline.trace;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class ImageData {
	
	private  final long imageID;
	private int width, height;

	public ImageData(long imageID) {
		
		this.imageID = imageID;
		
	}

	public ImageData(DataInputStream io) throws IOException {

		imageID = io.readLong();
		width = io.readInt();
		height = io.readInt();
		
	}

	public void addSize(int eventID, int width, int height) {
		
		this.width = width;
		this.height = height;
		
	}
	
	public long getImageID() { return imageID; }
	public int getWidth(int eventID) { return width; }
	public int getHeight(int eventID) { return height; }

	public void write(DataOutputStream io) throws IOException {

		io.writeLong(imageID);
		io.writeInt(width);
		io.writeInt(height);
		
	}

}
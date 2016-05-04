package edu.cmu.hcii.whyline.ui.views;

/**
 * @author Andrew J. Ko
 *
 */
public interface ScrollableView {

	public int getHorizontalSize();
	public int getHorizontalVisible();
	public int getHorizontalPosition();

	public int getVerticalSize();
	public int getVerticalVisible();
	public int getVerticalPosition();

	public void setHorizontalPosition(int x);
	public void setVerticalPosition(int y);
	
}

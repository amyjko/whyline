package edu.cmu.hcii.whyline.ui.views;

import java.awt.Cursor;

import java.util.List;
import javax.swing.JPopupMenu;



/**
 * An interface, which allows for multiple types of containers for instances of View. Currently, I've only implemented DynamicJComponent, but others could exist.
 * 
 * @author Andrew J. Ko
 *
 */
public interface ViewContainer {

	public void repaint();
	public View getView();
	public void focusMouseOn(View view);
	public boolean mouseIsFocused();
	public void releaseMouseFocus();
	public int getMouseFocusX();
	public int getMouseFocusY();
	public void moveMouseAgain();

	public boolean viewIsUnderMouse(View view);

	/**
	 * Returns false if the popup wasn't shown because the menu was empty.
	 */
	public boolean showPopup(JPopupMenu menu, int x, int y);
	
	public void setCursor(Cursor cursor);
	public List<View> getViewsUnderMouse();
	public int getMouseX();
	public int getMouseY();
	public void invokeRunnableLater(Runnable runnable);
	
	public boolean isMetaDown();
	public boolean isControlDown();
	public boolean isAltDown();
	public boolean isShiftDown();
	
	public void rootViewChangedSize();
	public boolean isModifierDown();
	
}
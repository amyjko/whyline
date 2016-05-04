package edu.cmu.hcii.whyline.ui.components;

import java.awt.*;
import java.awt.event.*;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.metal.MetalTabbedPaneUI;

import edu.cmu.hcii.whyline.ui.UI;

/**
 * @author Andrew J. Ko
 *
 */
public class WhylineTabbedPane extends JTabbedPane {
	
	private class WhylineTabbedPaneUI extends MetalTabbedPaneUI {

	    protected void paintTabBorder( Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {}
	    protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {}
	    protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect, Rectangle textRect, boolean isSelected) {}
	    protected void paintTabBackground( Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected ) {

	    	g = g.create();
	    	g.clipRect(x, y, w + 2, h);
	    	((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

	    	if(isSelected) {
	    		g.setColor(UI.getPanelDarkColor());
	    		g.fillRoundRect(x + 1, y, w - 2, h + UI.getBorderPadding(), UI.getRoundedness(), UI.getRoundedness());
	    		g.setColor(UI.getControlBorderColor());
	    		g.drawRoundRect(x + 1, y, w - 2, h + UI.getBorderPadding(), UI.getRoundedness(), UI.getRoundedness());
	    	}
	    	g.setColor(UI.getControlBorderColor());
	    	g.drawLine(x, y + h - 1, x + w, y + h - 1);
	    	
	    }
	    
		
	    protected JButton createScrollButton(int direction) {

	    	return new TabScrollButton(direction);
	    	
	    }

	}
	
	/**
	 * This needs to implement UIResource since the UI class checks for this in order to ignore these buttons as tabs.
	 *
	 */
	private class TabScrollButton extends WhylineButton implements UIResource {

		public TabScrollButton(int direction) {

			super("", null, "Scroll the tabs");
			
	    	char label = '-';
	    	switch(direction) {
		    	case SOUTH:
		    		label = UI.DOWN_ARROW;
		    		break;
		    	case NORTH:
		    		label = UI.UP_ARROW;
		    		break;
		    	case EAST:
		    		label = UI.RIGHT_ARROW;
		    		break;
		    	case WEST:
		    		label = UI.LEFT_ARROW;
		    		break;
	    		default:
		            throw new IllegalArgumentException("Direction must be one of: " + "SOUTH, NORTH, EAST or WEST");
		    }
	    	setText(Character.toString(label));
			
		}
		
	}
	
	public WhylineTabbedPane() {
		
		super();
		
		setUI(new WhylineTabbedPaneUI());

		setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		setFocusable(false);
		
		// Code to respond to clicks on the close icon in a tab.
		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				int index = indexAtLocation(e.getX(), e.getY());
				if(index == getSelectedIndex()) {
					Icon icon = getIconAt(index);
					if(icon != null) {
						Rectangle bounds = getBoundsAt(index);
						int width = icon.getIconWidth();
						Object ins = UIManager.get("TabbedPane.tabInsets");
						if(ins instanceof InsetsUIResource) {
							InsetsUIResource insets = (InsetsUIResource)ins;
							if(e.getX() > bounds.getX() + insets.left  && e.getX() < bounds.getX() + width + insets.left)
								selectedTabIconPressed(index);
						}
					}
				}
			}
		});
		
	}
	
	public Color getBackground() { return UI.getPanelLightColor(); }
	public Color getForeground() { return UI.getPanelTextColor(); }

	public void selectedTabIconPressed(int index) {} 
	
}
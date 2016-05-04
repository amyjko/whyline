package edu.cmu.hcii.whyline.ui.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.plaf.metal.MetalScrollBarUI;
import javax.swing.plaf.metal.MetalScrollButton;

import edu.cmu.hcii.whyline.ui.UI;

/**
 * @author Andrew J. Ko
 *
 */
public class WhylineScrollPane extends JScrollPane {
	
	public WhylineScrollPane(Component view) {

		this(view, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED);
		
	}

	public WhylineScrollPane(Component view, int vsbPolicy, int hsbPolicy) {
		
		super(view, vsbPolicy, hsbPolicy);

		getViewport().setBackground(null);
		getViewport().setOpaque(false);
		setBorder(null);

		setOpaque(true);
		
		getHorizontalScrollBar().setUI(new WhylineScrollBarUI());
		getVerticalScrollBar().setUI(new WhylineScrollBarUI());

		getHorizontalScrollBar().setBackground(UI.getControlBackColor());
		getVerticalScrollBar().setBackground(UI.getControlBackColor());
		
	}
	
	public Color getBackground() { 
		
		Component view = getViewport().getView();
		if(view ==  null || !view.isBackgroundSet())
			return UI.getControlBackColor();
		else
			return view.getBackground();
		
	}

	public static class WhylineScrollBarUI extends MetalScrollBarUI {
		
		public WhylineScrollBarUI() {}
		
	    protected void paintTrack( Graphics g, JComponent c, Rectangle trackBounds ) {

			((Graphics2D)g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

	    	g.setColor(UI.getControlCenterColor());
	    	g.fillRoundRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height, UI.getRoundedness(), UI.getRoundedness());

	    	g.setColor(UI.getControlFrontColor());
	    	g.drawRoundRect(trackBounds.x, trackBounds.y, trackBounds.width - 1, trackBounds.height - 1, UI.getRoundedness(), UI.getRoundedness());
	    	
	    }

	    protected void paintThumb( Graphics g, JComponent c, Rectangle thumbBounds ) {
	    	
	    	((Graphics2D)g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
	    	((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        	g.setColor(UI.getControlFrontColor());
	    	g.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2, thumbBounds.width - 4, thumbBounds.height - 4, UI.getRoundedness(), UI.getRoundedness());
	    	
	    }

	    protected JButton createDecreaseButton( int orientation ) {
	    	
	        decreaseButton = new WhylineScrollButton( orientation, scrollBarWidth, isFreeStanding );
	    	return decreaseButton;

	    }

	    protected JButton createIncreaseButton( int orientation ) {

	    	increaseButton =  new WhylineScrollButton( orientation, scrollBarWidth, isFreeStanding );
	    	return increaseButton;

	    }

	}
	
	public static class WhylineScrollButton extends MetalScrollButton {

		public WhylineScrollButton(int direction, int width, boolean freeStanding) {

			super(direction, 1, freeStanding);

			setVisible(false);
			
		}
		
	}
			
}
package edu.cmu.hcii.whyline.ui.annotations;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JTextArea;

import edu.cmu.hcii.whyline.ui.UI;
import edu.cmu.hcii.whyline.ui.WhylineUI;
import edu.cmu.hcii.whyline.ui.components.WhylineControlBorder;

/**
 * @author Andrew J. Ko
 *
 */
public abstract class NarrationTextArea extends JTextArea {

	private final WhylineUI whylineUI;

	public NarrationTextArea(WhylineUI whylineUI) {
		
		this.whylineUI = whylineUI;

		setOpaque(false);
		setBackground(null);
		setForeground(UI.getControlTextColor());
		setFont(UI.getSmallFont());
		setLineWrap(true);
		setWrapStyleWord(true);
		setCaretColor(UI.getControlTextColor());
		setBorder(new WhylineControlBorder());
		addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) { repaint(); }
			public void focusLost(FocusEvent e) { 
				repaint();
			}
		});

	}
	
	public abstract String getDescriptionOfContext();
	
}

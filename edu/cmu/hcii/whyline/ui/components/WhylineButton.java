package edu.cmu.hcii.whyline.ui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.Action;
import javax.swing.DefaultButtonModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.UIManager;

import edu.cmu.hcii.whyline.ui.UI;

/**
 * @author Andrew J. Ko
 *
 */
public class WhylineButton extends JButton {
	
	public WhylineButton(Icon icon, Action action, String tooltip) {
		
		this(action, tooltip);
		
		setIcon(icon);
		setText(null);
		
	}

	public WhylineButton(String text, Action action, String tooltip) {
		
		this(action, tooltip);
		
		setText(text);
		
	}

	public WhylineButton(String text, Action action, Dimension fixedSize, Font font, String tooltip) {
		
		this(action, tooltip);
		
		setText(text);
		setMaximumSize(fixedSize);
		setFont(font);
		
	}

	public WhylineButton(Action action, Dimension fixedSize, Font font, String tooltip) {
		
		this(action, tooltip);
		
		setMaximumSize(fixedSize);
		setFont(font);
		
	}

	public WhylineButton(String text, float fontsize, Action action, String tooltip) {
		
		this(action, tooltip);
		
		setFont(UI.getMediumFont().deriveFont(fontsize));
		setText(text);
		
	}

	public WhylineButton(Action action, Font font, String tooltip) {
		
		this(action, tooltip);
		
		setFont(font);
		
	}

	public WhylineButton(Action abstractAction, String tooltip) {
		
		super(abstractAction);

		setFont(UI.getMediumFont());
		setFocusable(false);
		setOpaque(false);
		setToolTipText(tooltip);
		setContentAreaFilled(true);
		setRolloverEnabled(true);
		
		// Hack so that toolbar buttons in MetalLookAndFeel always show their background.
		setModel(new DefaultButtonModel() {
			public boolean isRollover() { return true; }
		});
		
	}
	
	public void setEnabled(boolean enabled) {
		
		super.setEnabled(enabled);
		
		// This is a workaround for a bug in JDK 5 in which disabled toolbar buttons aren't greyed out.
		setForeground((Color)UIManager.get(enabled ? "Button.foreground" : "Button.disabledText"));
		
	}
		
}
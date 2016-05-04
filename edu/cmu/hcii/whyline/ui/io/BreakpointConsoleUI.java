package edu.cmu.hcii.whyline.ui.io;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;

import javax.swing.JTextPane;
import javax.swing.text.*;

import edu.cmu.hcii.whyline.ui.UI;
import edu.cmu.hcii.whyline.ui.UserTimeListener;
import edu.cmu.hcii.whyline.ui.WhylineUI;
import edu.cmu.hcii.whyline.ui.components.WhylineControlBorder;
import edu.cmu.hcii.whyline.ui.components.WhylinePanel;
import edu.cmu.hcii.whyline.ui.components.WhylineScrollPane;

/**
 * @author Andrew J. Ko
 *
 */
public class BreakpointConsoleUI extends WhylinePanel implements UserTimeListener {

	private final WhylineUI whylineUI;
	private final JTextPane console;
	
	private int lastTimeUpdatedTo;
	private int debuggingPrintIndex;

	private final MutableAttributeSet debugAttributes, regularAttributes;
	
	public BreakpointConsoleUI(WhylineUI whylineUI) {
		
		this.whylineUI = whylineUI;

		setBorder(new WhylineControlBorder());

		console = new JTextPane() {
			public boolean getScrollableTracksViewportWidth() { return false; }
		};
		console.setBackground(UI.getConsoleBackColor());
		console.setForeground(UI.getConsoleTextColor());
		console.setFont(UI.getFixedFont());
		console.setEditable(false);
		
		console.setBackground(UI.getControlBackColor());
		console.setOpaque(true);

		debugAttributes = new SimpleAttributeSet();
		StyleConstants.setItalic(debugAttributes, false);
		StyleConstants.setForeground(debugAttributes, UI.getConsoleTextColor());

		regularAttributes = new SimpleAttributeSet();
		StyleConstants.setItalic(regularAttributes, false);
		StyleConstants.setForeground(regularAttributes, UI.getConsoleTextColor());

		setLayout(new BorderLayout(0, UI.getPanelPadding()));

		add(new WhylineScrollPane(console), BorderLayout.CENTER);

		setPreferredSize(new Dimension(0, UI.getDefaultInfoPaneHeight(whylineUI)));
		
		clear();
		
	}
	
	public void clear() {
		
		console.setText("");
		lastTimeUpdatedTo = -1;
		debuggingPrintIndex = 0;
		
	}
	
	public void print(String text, boolean debug) {
		
		try {
			console.getStyledDocument().insertString(console.getStyledDocument().getLength(), text, debug ? debugAttributes : regularAttributes);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		
	}

	public void inputTimeChanged(int eventID) {

		if(eventID != 0) clear();

		List<BreakpointDebugger.Output> debuggingPrints = whylineUI.getBreakpointDebugger().getPrintStatementOutput();

		for(BreakpointDebugger.Output print : debuggingPrints) {

			if(print.eventID < eventID && print.output != null)
				print(print.output + "\n", true);
			
		}

		lastTimeUpdatedTo = eventID;
		
	}

	public void outputTimeChanged(int time) {}
	
}

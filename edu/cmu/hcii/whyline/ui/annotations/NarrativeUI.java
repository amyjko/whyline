package edu.cmu.hcii.whyline.ui.annotations;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Hashtable;

import javax.swing.*;

import edu.cmu.hcii.whyline.qa.Explanation;
import edu.cmu.hcii.whyline.ui.UI;
import edu.cmu.hcii.whyline.ui.WhylineUI;
import edu.cmu.hcii.whyline.ui.components.*;

/**
 * @author Andrew J. Ko
 *
 */
public final class NarrativeUI extends WhylinePanel {

	private final WhylineUI whylineUI;
	
	private final WhylinePanel entries;
	
	private final Hashtable<Explanation, AnnotatedEventExplanationUI> viewsByExplanation = new Hashtable<Explanation, AnnotatedEventExplanationUI>(); 
	
	public NarrativeUI(WhylineUI whylineUI) {

		super(new BorderLayout());
		
		this.whylineUI = whylineUI;
		
		setPreferredSize(new Dimension(UI.getDefaultInfoPaneWidth(whylineUI), UI.getDefaultInfoPaneHeight(whylineUI)));

		entries = new WhylinePanel();
		entries.setLayout(new BoxLayout(entries, BoxLayout.Y_AXIS));
		entries.setBackground(UI.getControlBackColor());
		
		WhylineScrollPane scroller = new WhylineScrollPane(entries, WhylineScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, WhylineScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		setBorder(new WhylineControlBorder());
		setBackground(UI.getControlBackColor());
		setOpaque(true);
		
		add(new WhylineTitleLabel(UI.EXPLANATION_BOX_TITLE), BorderLayout.NORTH);
		add(scroller, BorderLayout.CENTER);
		
		if(whylineUI.getMode() == WhylineUI.Mode.BREAKPOINT)
			entries.add(new NarrationTextArea(whylineUI) {
				public String getDescriptionOfContext() {
					
					int event = NarrativeUI.this.whylineUI.getBreakpointDebugger().getCurrentEventID();
					if(event < 0) return "none";
					else return "" + event;
					
				}
			});
		
	}

	public void addEntry(Explanation explanation) {
		
		AnnotatedEventExplanationUI viewToShow = null;
		
		if(whylineUI.getTrace().addNarrativeExplanation(explanation)) {

			viewToShow = new AnnotatedEventExplanationUI(this, explanation);
			entries.add(viewToShow);
			viewsByExplanation.put(explanation, viewToShow);
	
			validate();
			repaint();
			
		}
		else
			viewToShow = viewsByExplanation.get(explanation);
		
		viewToShow.makeVisible();
		
	}

	public WhylineUI getWhylineUI() { return whylineUI; }

}
package edu.cmu.hcii.whyline.ui.annotations;

import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import edu.cmu.hcii.whyline.qa.Explanation;
import edu.cmu.hcii.whyline.ui.UI;
import edu.cmu.hcii.whyline.ui.components.WhylineLabel;
import edu.cmu.hcii.whyline.ui.components.WhylinePanel;

/**
 * @author Andrew J. Ko
 *
 */
public class AnnotatedEventExplanationUI extends WhylinePanel {

	private final Explanation explanation;
	private final NarrativeUI narrativeUI;
	
	private final JTextArea text;
	
	public AnnotatedEventExplanationUI(NarrativeUI narrativeUI, Explanation explanation) {
		
		this.narrativeUI = narrativeUI;
		this.explanation = explanation;
		
		setLayout(new FlowLayout(FlowLayout.LEFT));

		setBackground(UI.getControlBackColor());
		
		setBorder(new EmptyBorder(5, 5, 5, 5));
		
		text = new NarrationTextArea(narrativeUI.getWhylineUI()) {
			public String getDescriptionOfContext() {
				return Integer.toString(AnnotatedEventExplanationUI.this.explanation.getEventID());
			}
		};
		
		WhylineLabel label = new WhylineLabel(narrativeUI.getWhylineUI().getTrace().getDescription(explanation.getEventID()));
		label.setPreferredSize(new Dimension(UI.getDefaultInfoPaneWidth(narrativeUI.getWhylineUI()) / 3, 25));
		
		add(label);
		add(text);
		
	}

	public void makeVisible() {
		
		scrollRectToVisible(getBounds());
		text.requestFocusInWindow();
		
	}
	
}
package edu.cmu.hcii.whyline.ui.qa;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.LinkedList;

import javax.swing.JOptionPane;
import javax.swing.border.EmptyBorder;

import edu.cmu.hcii.whyline.qa.Question;
import edu.cmu.hcii.whyline.ui.UI;
import edu.cmu.hcii.whyline.ui.WhylineUI;
import edu.cmu.hcii.whyline.ui.components.WhylineLabel;
import edu.cmu.hcii.whyline.ui.components.WhylinePanel;
import edu.cmu.hcii.whyline.util.CloseIcon;

/**
 * @author Andrew J. Ko
 *
 */
public class QuestionTabsUI extends WhylinePanel {

	private final WhylineUI whylineUI;

	private final QuestionLabel askAnotherLabel;
	private final HashSet<Question<?>> questionsAdded = new HashSet<Question<?>>();
	private final LinkedList<QuestionLabel> buttons = new LinkedList<QuestionLabel>();
	
	public QuestionTabsUI(WhylineUI whylineUI) {
		
		this.whylineUI = whylineUI;

		setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));

		askAnotherLabel = new QuestionLabel(null);
		buttons.add(askAnotherLabel);

		add(askAnotherLabel);

		select(null);
		
		setBorder(new EmptyBorder(0, UI.getBorderPadding(), 0, UI.getBorderPadding()));
		
	}

	private GradientPaint getGradient(Color color) { 
		
		return new GradientPaint(0, 0, color.darker(), 0, 10, color);

	}
	
	public void paintComponent(Graphics g) {

		GradientPaint gradient = getGradient(UI.getPanelDarkColor());
		
		((Graphics2D)g).setPaint(gradient);
		g.fillRect(0, 0, getWidth(), getHeight());
		
		g.setColor(UI.getControlBorderColor());
		g.drawLine(0, 0, getWidth(), 0);
		
		super.paintComponent(g);
		
	}
	
	public void removeQuestion(Question<?> question) {

		QuestionLabel button = null;
		for(QuestionLabel b : buttons)
			if(b.question == question)
				button = b;
		
		if(button != null) {
			remove(button);
			buttons.remove(button);
			questionsAdded.remove(question);
		}

		Question<?> questionToSelect = getComponentCount() > 1 ? buttons.get(1).question : null;
		
		whylineUI.setQuestion(questionToSelect);

		validate();
		repaint();
		
	}	

	public void addQuestion(final Question<?> question) {

		if(!questionsAdded.contains(question) && question != null) {

			questionsAdded.add(question);
	
			QuestionLabel button = new QuestionLabel(question);
	
			add(button);
			
			buttons.add(button);
			
		}
		
		select(question);
		
	}

	private void select(Question<?> question) {

		for(QuestionLabel button : buttons)
			button.setSelected(button.question == question);
		
	}
	
	private class QuestionLabel extends WhylineLabel {

		private Question<?> question;
		boolean selected;
		
		public QuestionLabel(Question<?> question) {
			
			super("");

			setFont(UI.getSmallFont().deriveFont(Font.BOLD));
			
			String label;
			if(question == null)
				label = "Ask";
			else {
				label = "<html>" + question.getQuestionText();
			}
			
			setText(label);
			
			if(question != null)
				setIcon(closeIcon);
			
			this.question = question;

			addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					
					if(QuestionLabel.this.question != null && e.getX() < getBorder().getBorderInsets(QuestionLabel.this).left + CloseIcon.SIZE) {
						
						String[] options = { "Yes, close this question.", "Don't close this!"  };
						int answer = JOptionPane.showOptionDialog(whylineUI, "Are you sure you want to close this question?", "Close this question?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, "Don't close this!");
						
						if(answer == 0)
							whylineUI.removeQuestion(QuestionLabel.this.question);
						
					}
					else {
					
						whylineUI.setQuestion(QuestionLabel.this.question);
						
					}
					
				}
			});
			
			setBorder(new EmptyBorder(UI.getBorderPadding() * 2, UI.getBorderPadding() * 2, UI.getBorderPadding() * 2, UI.getBorderPadding() * 2));
			
		}

		public void setSelected(boolean b) { 
			
			this.selected = b; 
			repaint();
			
		}
		
		public void paintComponent(Graphics g) {

	    	((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			
			int left = UI.getBorderPadding();
			int top = -UI.getBorderPadding();
			int right = getWidth() - UI.getBorderPadding();
			int bottom = getHeight() - UI.getBorderPadding() / 2 - 1;
			
			g.setColor(selected ? UI.getPanelLightColor() : UI.getPanelLightColor());
			g.fillRoundRect(left, top, right - left, bottom - top, UI.getRoundedness(), UI.getRoundedness());

			if(!selected) {
				g.setColor(UI.getControlBorderColor());
				g.drawLine(left, top + UI.getBorderPadding(), right, top + UI.getBorderPadding());
				((Graphics2D)g).setPaint(getGradient(UI.getPanelLightColor()));
				g.fillRect(left, top, right - left, 20);
			}
			else {
				g.setColor(UI.getControlBorderColor());
				g.drawRoundRect(left, top, right - left, bottom - top, UI.getRoundedness(), UI.getRoundedness());

				Shape s = g.getClip();
				g.setClip(null);
				g.setColor(UI.getPanelLightColor());
				g.fillRect(left + 1, -3, right - left - 1, 6);
				g.setClip(s);
			}

			setForeground(selected ? UI.getControlTextColor() : UI.getControlDisabledColor());
			
			super.paintComponent(g);
			
		}

	}
	
	private static final CloseIcon closeIcon = new CloseIcon() {
		public boolean isSelected(Component c) {
			return ((QuestionLabel)c).selected;
		}
	};

}
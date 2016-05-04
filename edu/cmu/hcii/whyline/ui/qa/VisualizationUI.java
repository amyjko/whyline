package edu.cmu.hcii.whyline.ui.qa;

import java.util.*;

import edu.cmu.hcii.whyline.qa.Answer;
import edu.cmu.hcii.whyline.qa.Explanation;
import edu.cmu.hcii.whyline.ui.WhylineUI;
import edu.cmu.hcii.whyline.ui.views.DynamicComponentWithSelection;
import edu.cmu.hcii.whyline.ui.views.View;

/**
 * Responsible for displaying the currently selected answer within scroll bars, and receving events.
 * 
 * @author Andrew J. Ko
 *
 */
public final class VisualizationUI extends DynamicComponentWithSelection<View> {

	private final SituationUI situationUI;
	private final WhylineUI whylineUI;

	private final Visualization visualization;
	
	
	private Timer timerForTrackingScrolling;
	
	public VisualizationUI(SituationUI ui, Answer answer) {

		super(ui.getWhylineUI(), Sizing.SCROLL_OR_FIT_IF_SMALLER, Sizing.SCROLL_OR_FIT_IF_SMALLER);

		this.situationUI = ui;
		this.whylineUI = situationUI.getWhylineUI();
		
		visualization = new Visualization(whylineUI, this, answer);
		
		setView(visualization);

		setRequestFocusOnEnter(true);
		setRequestFocusOnClick(true);
		
	}
	
	public Visualization getVisualization() { return visualization; }
	public Answer getAnswer() { return situationUI.getAnswer(); }
	public SituationUI getSituationUI() { return situationUI; }
	
	public void scaleVisualizationToViewport() {
		
		visualization.fitToChildrenAndScaleToViewport();
		
	}

	public View getEventAfter(View selection) {

		if(selection instanceof EventView) {
			EventView eventView = visualization.getUncollapsedEventViewAfter((EventView)selection);
			if(eventView != null) return eventView; 
			else return visualization.getFirstUnexecutedInstructionView();
		}
		else if(selection instanceof UnexecutedInstructionView)
			return visualization.getUnexecutedInstructionAt(((UnexecutedInstructionView)selection).getRow(), ((UnexecutedInstructionView)selection).getColumn() - 1);
		else 
			return null;

	}

	public View getEventBefore(View selection) {

		if(selection instanceof EventView)
			return visualization.getUncollapsedEventViewBefore((EventView)selection);
		else if(selection instanceof UnexecutedInstructionView) {
			View viewBefore = visualization.getUnexecutedInstructionAt(((UnexecutedInstructionView)selection).getRow(), ((UnexecutedInstructionView)selection).getColumn() + 1);
			if(viewBefore != null) return viewBefore;
			else {
				Explanation lastEvent = visualization.getLastExplanation();
				if(lastEvent != null) return visualization.getViewOfExplanation(lastEvent);
				else return null;
			}
		}
		else return null;

	}

	public View getEnclosingBlock(View selection) {

		if(selection instanceof EventView) {
			EventView eventView = (EventView)selection;
			EventBlockView<?> parent = eventView.getBlockView();
			return parent;
		}
		else if(selection instanceof UnexecutedInstructionView)
			return visualization.getUnexecutedInstructionAt(((UnexecutedInstructionView)selection).getRow() - 1, ((UnexecutedInstructionView)selection).getColumn());
		else return null;
		
	}
	
	public View getNextEnclosedBlock(View selection) {

		if(selection instanceof EventView) {

			EventView eventView = (EventView)selection;

			// If it's a block, find the first block in the block.
			if(eventView instanceof EventBlockView) {
				
				for(View child : eventView.getChildren())
					if(child instanceof EventBlockView)
						return child;
				
				return getEventAfter(eventView.getLastChild());

			}
			else return getEventAfter(selection);
					
		}
		else if(selection instanceof UnexecutedInstructionView)
			return visualization.getUnexecutedInstructionAt(((UnexecutedInstructionView)selection).getRow() + 1, ((UnexecutedInstructionView)selection).getColumn());
		else return null;

	}
		
	public void handleNewSelection(final View selection, final boolean scroll, String ui) {

			visualization.selectAndScrollToView(selection, scroll, ui);
				
			situationUI.updateHintsWith(selection);
			
	}
	
	public void show(Explanation subject) {

		EventView view = visualization.getViewOfExplanation(subject);
		if(view != null)
			setSelection(view, true);
		
	} 

	public int getVerticalScrollIncrement() { return 25; }
	public int getHorizontalScrollIncrement() { return 50; }
	
}

package edu.cmu.hcii.whyline.ui.qa;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.util.*;

import javax.swing.JOptionPane;

import edu.cmu.hcii.whyline.bytecode.*;
import edu.cmu.hcii.whyline.qa.*;
import edu.cmu.hcii.whyline.source.Line;
import edu.cmu.hcii.whyline.trace.*;
import edu.cmu.hcii.whyline.ui.*;
import edu.cmu.hcii.whyline.ui.arrows.CausalArrowView;
import edu.cmu.hcii.whyline.ui.arrows.VisualizationArrow;
import edu.cmu.hcii.whyline.ui.views.View;
import edu.cmu.hcii.whyline.ui.views.ViewContainer;

/**
 * The visualization of the causal explanation for a question, consisting of a
 * sequence of execution events. Contains the state for the time controller and
 * event selection. It's responsible for laying out the events.
 * 
 * @author Andrew J. Ko
 * 
 */
public final class Visualization extends View implements AnswerChangeListener {

	private final Answer answer;
	
	public final GlyphVector LEFT_PAREN, RIGHT_PAREN, OPEN_BRACE, CLOSING_BRACE;
	public final double PAREN_AND_BRACE_WIDTH, PAREN_WIDTH, PAREN_HEIGHT, PAREN_DESCENT, PAREN_ASCENT;

	private final UnexecutedInstructionsView unexecutedInstructionsView;
	private final Map<UnexecutedInstruction, UnexecutedInstructionView> unexecutedViews = new Hashtable<UnexecutedInstruction, UnexecutedInstructionView>();
	private final ArrayList<ArrayList<UnexecutedInstructionView>> unexecutedGrid = new ArrayList<ArrayList<UnexecutedInstructionView>>();

	private final gnu.trove.TIntIntHashMap rowsByThreadIDs = new gnu.trove.TIntIntHashMap();
	private int nextRow = 0;
	
	private final WhylineUI whylineUI;
	private final VisualizationUI visualizationUI;
	private final Trace trace;
	
	private boolean isMetaDown = false;

	private final ArrayList<VisualizationArrow> arrows = new ArrayList<VisualizationArrow>();
	private EventView lastArrowSelectionComputed = null;
	private boolean lastArrowMetaState = false;
	
	private boolean threadsVisible = false;
	
	public Visualization(WhylineUI ui, VisualizationUI vui, Answer answer) {

		this.whylineUI = ui;
		this.visualizationUI = vui;
		this.answer = answer;
		this.trace = ui.getTrace();

		// Initialize a few commonly used glyph vectors
		Graphics2D g = (Graphics2D)visualizationUI.getSituationUI().getWhylineUI().getGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		
		Font delimiterFont = new Font("Arial Narrow", Font.PLAIN, 28);
		
		LEFT_PAREN = delimiterFont.createGlyphVector(g.getFontRenderContext(), "(");
		RIGHT_PAREN = delimiterFont.createGlyphVector(g.getFontRenderContext(), "){");
		OPEN_BRACE = delimiterFont.createGlyphVector(g.getFontRenderContext(), "{");
		CLOSING_BRACE = delimiterFont.createGlyphVector(g.getFontRenderContext(), "}");
		PAREN_WIDTH = LEFT_PAREN.getLogicalBounds().getWidth();
		PAREN_AND_BRACE_WIDTH = RIGHT_PAREN.getLogicalBounds().getWidth();
		PAREN_HEIGHT = LEFT_PAREN.getLogicalBounds().getHeight();
		PAREN_DESCENT = g.getFontMetrics(delimiterFont).getDescent();
		PAREN_ASCENT = g.getFontMetrics(delimiterFont).getAscent();
		
		// First add the unexecuted instructions.
		unexecutedInstructionsView = new UnexecutedInstructionsView();
		addChild(unexecutedInstructionsView);

		layoutThreads(false);

		answer.addChangeListener(this);

		// Add each of the known thread blocks.
		for(ThreadBlock block : answer.getThreadBlocks())
			threadBlockAdded(block);
		
		initializeCollapsedState();
		
		layoutEvents(true, false);
		
	}

	public WhylineUI getWhylineUI() { return whylineUI; }
	public Trace getTrace() { return trace; }

	public void setThreadsVisible(boolean threadsVisible) {

		this.threadsVisible = threadsVisible;
		layoutEvents(false, true);
		
	}
	
	public boolean areThreadsVisible() { return threadsVisible; }

	public VisualizationUI getVisualizationUI() { return visualizationUI; }

	public GlyphVector getLeftParenthesis() { return LEFT_PAREN; }
	public GlyphVector getRightParenthesisAndOpenBrace() { return RIGHT_PAREN; }
	public GlyphVector getOpenBrace() { return OPEN_BRACE; }
	public GlyphVector getClosingBrace() { return CLOSING_BRACE; }

	public int getRowForThread(int threadID) { 

		if(rowsByThreadIDs.containsKey(threadID)) return rowsByThreadIDs.get(threadID);

		int row = nextRow++;
		rowsByThreadIDs.put(threadID, row);
		return row;
		
	}
	
	public int getNumberOfRows() { return rowsByThreadIDs.size(); }
	
	public Answer getAnswer() {	return answer; }

	public Explanation getFirstExplanation() { return viewSequence.isEmpty() ? null : viewSequence.first().getExplanation(); }
	public Explanation getLastExplanation() { return viewSequence.isEmpty() ? null : viewSequence.last().getExplanation(); }

	public UnexecutedInstructionView getFirstUnexecutedInstructionView() { 
	
		if(unexecutedGrid.isEmpty())
			return null;
		else {
			for(int index = unexecutedGrid.size() - 1; index >= 0; index--) {
				ArrayList<UnexecutedInstructionView> views = unexecutedGrid.get(index);
				if(!views.isEmpty()) return views.get(0);
			}
			return null;
		}
		
	}

	public void scrollToView(View viewToScrollTo) {
		
		if(viewToScrollTo == null) return;
		
		double viewX = viewToScrollTo.getGlobalLeft() - visualizationUI.getViewportWidth() / 2;
		double viewY = viewToScrollTo.getGlobalTop() - visualizationUI.getViewportHeight() / 2;

		viewX = Math.min(Math.max(0, viewX), getLocalWidth() - visualizationUI.getViewportWidth());
		viewY = Math.min(Math.max(0, viewY), getLocalHeight() - visualizationUI.getViewportHeight());

		visualizationUI.setViewPosition((int)viewX, (int)viewY);

	}
	
	public void selectAndScrollToView(final View newSelection, final boolean scroll, String ui) {
		
		if(newSelection == null) return;

		answer.getQuestion().getAsker().processing(true);
		
		if(scroll) scrollToView(newSelection);

		// If we navigated to an event, make sure its visible, show data dependencies.
		if(newSelection instanceof EventView) {
			
			// Make sure the block is uncollapsed (but not necessarily visible).
			EventBlockView<?> blockView = ((EventView)newSelection).getBlockView();
			if(blockView != null)
				blockView.uncollapseAncestors();

			if(newSelection instanceof EventView)
				((EventView)newSelection).setHidden(false);
			
			Explanation explanation = ((EventView)newSelection).getExplanation();
			
			// Tell the Whyline to select the explanation.
			whylineUI.selectExplanation(explanation, true, ui);
			
			answer.getTerminalDataDependencies(explanation);
			answer.broadcastChanges();
			layoutEvents(scroll, true);

		}
		// The whyline that we're selecting this, so it can update other UI's accordingly.
		else if(newSelection instanceof UnexecutedInstructionView) {

			// Explain this unexecuted view, now that it's selected.
			((UnexecutedInstructionView)newSelection).getUnexecutedInstruction().explain();
			((UnexecutedInstructionView)newSelection).update();
			unexecutedInstructionsView.layout();
			
			whylineUI.selectUnexecutedInstruction(((UnexecutedInstructionView)newSelection).getUnexecutedInstruction(), true, ui);
			
		}

		answer.getQuestion().getAsker().processing(false);

		visualizationUI.moveMouseAgain();
		
	}
	
	private void initializeCollapsedState() {

		// Get the view of the subject of the question
		int latestEventID = answer.getLatestEventID();

		// If there is a last event, reveal it!
		if(latestEventID >= 0) {

			// Initialize all of the views in the answer.
			for(EventView view : viewSequence)
				view.initializeVisibility();

		}
		
	}
	
	private boolean initializedToLastEvent = false;
	
	public void initializeToLastEvent() {
		
		if(initializedToLastEvent) return;
		initializedToLastEvent = true;

		if(unexecutedInstructionsView.getNumberOfChildren() > 0)
			visualizationUI.setSelection(unexecutedInstructionsView.getFirstChild(), true, UI.INITIALIZATION_UI);
		else if(viewSequence.size() > 0) {
			EventView selection = viewSequence.last();
			if(selection != null)
				visualizationUI.setSelection(selection, true, UI.INITIALIZATION_UI);
		}

	}
	
	public void handleContainerResize() {
		
		layoutEvents(false, false);
		
	}
	
	private double paddingBetweenThreads = 0;
	
	private void layoutThreads(boolean animate) {
		
		double totalThreadHeight = 0;
		double maxThreadHeight = 0;
		
		// Place the thread blocks!
		for(int row = 0; row < threadViewsByRow.size(); row++) {
			
			ThreadBlockView threadView = threadViewsByRow.get(row);

			threadView.determineHeight(animate);
			totalThreadHeight += threadView.getLocalHeight();
			maxThreadHeight = Math.max(maxThreadHeight, threadView.getLocalHeight());
		
		}
		
		if(areThreadsVisible()) {
			
			// Now that we know the total thread height, arrange the threads if we can.
			if(totalThreadHeight < visualizationUI.getViewportHeight()) {
	
				paddingBetweenThreads = (visualizationUI.getViewportHeight() - totalThreadHeight) / (threadViewsByRow.size() + 1);
				
				double previousThreadBottom = 0;
				
				for(int row = 0; row < threadViewsByRow.size(); row++) {
					
					ThreadBlockView threadView = threadViewsByRow.get(row);
					double newTop = previousThreadBottom + paddingBetweenThreads;

					threadView.setLocalTop(newTop, animate);
					
					previousThreadBottom = threadView.getLocalBottom();
					
				}
	
			}
			
		}
		// If the threads aren't visible, put it at the bottom third of the screen.
		else {

			int offset = 0;
			if(maxThreadHeight < visualizationUI.getViewportHeight())
				offset = (int) ((visualizationUI.getViewportHeight() - maxThreadHeight) / 3) * 2;
			
			for(int row = 0; row < threadViewsByRow.size(); row++) {
				
				ThreadBlockView threadView = threadViewsByRow.get(row);
				double newTop = offset + (maxThreadHeight - threadView.getLocalHeight()) / 2;
				
				threadView.setLocalTop(newTop, animate);
				
			}
			
		}

		double newNotExecutedTop = 0;

		// Now position the top of the uninstrumented instructions.
		// If there's space, put it at the bottom third of the screen.
		if(unexecutedInstructionsView.getLocalHeight() < visualizationUI.getViewportHeight())
			newNotExecutedTop = 2 * (visualizationUI.getViewportHeight() - unexecutedInstructionsView.getLocalHeight()) / 3;
		// Otherwise, just put it on top.
		else {
			newNotExecutedTop = 0;
			
		}

		unexecutedInstructionsView.setLocalTop(newNotExecutedTop, animate);				
		
		if(animate)
			animate(UI.getDuration(), false);
		
	}
		
	private ArrayList<ThreadBlockView> threadViewsByRow = new ArrayList<ThreadBlockView>();
	
	public void threadBlockAdded(ThreadBlock top) {
		
		int row = getRowForThread(top.getThreadID());
		ThreadBlockView topView = new ThreadBlockView(this, top, row); 
		addChild(topView);
		threadViewsByRow.add(topView);
		layoutThreads(false);
		
	}
	
	public java.util.List<ThreadBlockView> getThreadViews() { return Collections.<ThreadBlockView>unmodifiableList(threadViewsByRow); }
	
	public ThreadBlockView getThreadViewOnRow(int row) { return threadViewsByRow.get(row); }

	public int getNumberOfThreadRows() { return threadViewsByRow.size(); }
	
	public EventView getSelectedEventView() { return visualizationUI.getSelection() instanceof EventView ? (EventView)visualizationUI.getSelection() : null; }
	public UnexecutedInstructionView getSelectedUnexecutedInstructionView() { return visualizationUI.getSelection() instanceof UnexecutedInstructionView ? (UnexecutedInstructionView)visualizationUI.getSelection() : null; }
	

	private Point getOnscreenLocationOf(View view) { 
		
		return new Point(	(int)(view.getGlobalLeft() + view.getGlobalWidth() / 2 - visualizationUI.getViewportX()), 
								(int)(view.getGlobalTop() + view.getGlobalHeight() / 2 - visualizationUI.getViewportY())); 
		
	}
			
	public void eventBlocksChanged(final Set<ExplanationBlock> blocksChanged) {

		assert EventQueue.isDispatchThread();
		
		EventView selection = getSelectedEventView();

		double windowOffsetXOfThisBeforeClick = 0;
		double windowOffsetYOfThisBeforeClick = 0;
		if(selection != null) {
			windowOffsetXOfThisBeforeClick = selection.getGlobalLeft() - getVisualizationUI().getViewportX();
			windowOffsetYOfThisBeforeClick = selection.getGlobalTop() - getVisualizationUI().getViewportY();
		}				
		
		for(ExplanationBlock block : blocksChanged) {
			
			// Find the view for this event block
			EventBlockView<?> view = getViewOfBlock(block);
			
			if(view != null) view.synchronizeWithModel();
			
		}

		// Initialize the collapsed state of new views.
		for(EventView view : viewSequence) {
			view.initializeVisibility();
		}
		
		layoutEvents(true, false);

		if(selection != null) {
			double newViewportX = selection.getGlobalLeft() - windowOffsetXOfThisBeforeClick;
			double newViewportY = selection.getGlobalTop() - windowOffsetYOfThisBeforeClick;
			getVisualizationUI().setViewPosition((int)newViewportX, (int)newViewportY);
		}
		
	}
	
	// This is a global table for looking up the view for an execution event.
	// I don't want to store these with the execution event, because I want them
	// to be garbage collected when this visualization state is garbage collected.
	private gnu.trove.TIntObjectHashMap<EventView> viewsByEvent= new gnu.trove.TIntObjectHashMap<EventView>(100);
	private Hashtable<Explanation, EventView> viewsByExplanation = new Hashtable<Explanation, EventView>(100);
	private Hashtable<ExplanationBlock, EventBlockView<?>> viewsByBlock = new Hashtable<ExplanationBlock, EventBlockView<?>>(100);

	// Contains all of the primitive and block views, in the order of execution.
	private SortedSet<EventView> viewSequence = new TreeSet<EventView>();
	
	public EventView getEventViewBefore(EventView view) {

		SortedSet<EventView> eventsBefore = viewSequence.headSet(view);
		if(eventsBefore.isEmpty()) return null;
		else {
			EventView last = eventsBefore.last();
			if(last instanceof ThreadBlockView) return getEventViewBefore(last);
			else return last;
		}
		
	}

	public EventView getUncollapsedEventViewBefore(EventView view) {
		
		EventView before = getEventViewBefore(view);
		while(before != null && (before.ancestorIsCollapsed() || before.isHidden())) before = getEventViewBefore(before);
		return before;
		
	}
	
	public EventView getEventViewAfter(EventView view) {

		// The tail set returned contains the view given to tailSet()
		SortedSet<EventView> eventsAfter = viewSequence.tailSet(view);
		EventView eventAfter = eventsAfter.first();
		if(eventAfter == view) {
	        Iterator<EventView> i = eventsAfter.iterator();
	        i.next();  // skip first element
	        EventView next = i.hasNext() ? i.next() : null;
			if(next instanceof ThreadBlockView) return getEventViewAfter(next);
			else return next;
		}
		else return eventAfter;

	}

	public EventView getUncollapsedEventViewAfter(EventView view) {
		
		EventView after = getEventViewAfter(view);
		while(after != null && (after.ancestorIsCollapsed() || after.isHidden())) after = getEventViewAfter(after);
		return after;
		
	}

	public EventView getViewOfExplanation(Explanation event) { return viewsByExplanation.get(event); }

	public EventView getViewOfEvent(int eventID) { return viewsByEvent.get(eventID); }
	
	public EventBlockView<?> getViewOfBlock(ExplanationBlock block) { return viewsByBlock.get(block); }

	public EventView createViewFor(Explanation explanation) {
				
		final EventView child;

		EventKind kind = trace.getKind(explanation.getEventID());
		
		if(explanation instanceof InvocationBlock) child = new InvocationBlockView(this, (InvocationBlock)explanation);
		else if(explanation instanceof LoopBlock) child = new LoopBlockView(this, (LoopBlock)explanation);
		else if(explanation instanceof BranchBlock) child = new BranchBlockView(this, (BranchBlock)explanation);
		else if(explanation instanceof StartMethodBlock) child = new StartMethodBlockView(this, (StartMethodBlock)explanation);
		else if(explanation instanceof ExceptionBlock) child = new ExceptionBlockView(this, (ExceptionBlock)explanation);
		else if(kind.isValueProduced) child = new ValueProducedView(this, explanation);
		else if(kind.isArgument) child = new ArgumentEventView(this, explanation);
		else if(kind.isDefinition) child = new DefinitionEventView(this, explanation);
		else child = new GenericEventView(this, explanation);
		
		return child;
		
	}

	public void associateExplanationWithView(Explanation explanation, EventView view) {
			
		viewsByExplanation.put(explanation, view);
		viewsByEvent.put(explanation.getEventID(), view);

		assert !viewSequence.contains(view) : "The view sequence already contains something equivalent to " + view;
		
		viewSequence.add(view);
		
	}
	
	public void associateBlockWithView(ExplanationBlock explanation, EventBlockView<?> view) {
		
		viewsByBlock.put(explanation, view);
		
	}
		
	public int getNumberOfArrows() { return arrows.size(); }
	
	private void updateArrows() {
		
		EventView selection = getSelectedEventView();
		
		boolean changeArrows = selection != lastArrowSelectionComputed || isMetaDown != lastArrowMetaState;
		
		lastArrowSelectionComputed = selection;
		lastArrowMetaState = isMetaDown;

		if(changeArrows) {
		
			// Remove all arrow views from the viz.
			for(VisualizationArrow arrow : arrows)
				removeChild(arrow);
	
			// Clear the list.
			arrows.clear();
	
			// Add arrows if there's a valid event explanation
			if(selection != null) {
	
				SortedMap<Explanation,Explanation> causes = getAnswer().getTerminalDataDependencies(selection.getExplanation()); 
	
				ExplanationBlock controlDependency = selection.getExplanation().getBlock();
				if(controlDependency != null && getViewOfExplanation(controlDependency) != null)
					arrows.add(new VisualizationArrow(this, null, controlDependency, selection.getExplanation(), 0, CausalArrowView.Relationship.CONTROL));
	
				if(causes != null) {
	
					int dependencyNumber = 1;
					for(Explanation cause : causes.keySet()) {
	
						Explanation original = cause;
						if(!isMetaDown) {
							Explanation source = getVisibleSourceOfExplanation(cause);
							if(source != null) cause = source;
						}
						
						if(cause != null)
							if(getViewOfExplanation(cause) != null)
								arrows.add(new VisualizationArrow(this, original, cause, selection.getExplanation(), dependencyNumber++, CausalArrowView.Relationship.DATA));
						
					}
				
				}

				for(VisualizationArrow arrow : arrows)
					addChild(arrow);

			}			

		}
		
		// Add all of the arrows to the viz, bringing them all to the front.
		// We only lay these out once since the whyline viz is stable.
		for(VisualizationArrow arrow : arrows) {
			arrow.bringToFront();
			arrow.layout();
		}
		
		repaint();
		
	}	
		
	public void layoutEvents(boolean scroll, boolean animate) {
		
		// Remember a bit about what the screen was showing, so we can show the exact same position after the layout.
		Point onscreenLocation = visualizationUI.getSelection() == null ? null : getOnscreenLocationOf(visualizationUI.getSelection());

		// Determine the new thread row heights.
		layoutThreads(animate);
				
		double nextGlobalPosition = UI.PADDING_BETWEEN_EVENTS;
		boolean lastViewWasCollapsed = false;
		boolean lastViewHidden = false;
		EventView lastView = null;
		
		// Go in order of events, including blocks.
		for(EventView view : viewSequence) {
						
			EventBlockView<?> parent = view.getBlockView();

			boolean isHidden = view.isHidden();
			
			// Choose the position for the next view based on whether its in a collapsed view.
			// If it is in a collapsed view, cluster all of the views within a collapsed view just after the 
			// collapsed view's inside left.
			boolean ancestorIsCollapsed = view.ancestorIsCollapsed(); 
			EventBlockView<?> eldestCollapsedAncestor = ancestorIsCollapsed ? view.getEldestCollapsedAncestor() : null;
			
			// If we just placed a collapsed view, then there won't be any space after it yet. Add some if the current view isn't collapsed.
			boolean needsSpaceAfterCollapsedView = lastViewWasCollapsed && !ancestorIsCollapsed && !isHidden; 
			boolean needsSpaceForUnexplainedEvents = !ancestorIsCollapsed && view.needsToBeExplained() && !isHidden;

			boolean appearsAfterHiddenView = lastViewHidden && !ancestorIsCollapsed && !isHidden;
			boolean appearsAfterVisibleView= !ancestorIsCollapsed && !lastViewHidden && !isHidden;
			
			view.setAppearsAfterHiddenEvent(appearsAfterHiddenView);

			boolean needsSpaceForHiddenEvents = appearsAfterHiddenView;
			
			// Add padding if necessary for any of these reasons.
			if(appearsAfterVisibleView)
					nextGlobalPosition += UI.PADDING_BETWEEN_EVENTS;
			// Add space for the elision if necessary.
			else if(needsSpaceAfterCollapsedView || needsSpaceForUnexplainedEvents || needsSpaceForHiddenEvents)
				nextGlobalPosition += UI.ELISION_PADDING;

			// Remember for next time.
			lastViewHidden = view.isHidden();

			// Now that we've placed any blocks, place this view at the global left determined above.
			view.setLocalLeft(view.getParent().globalLeftToLocal(nextGlobalPosition), animate);

			// Vertically center the view in the parent if this isn't a thread block view.
			if(!(view instanceof ThreadBlockView))
				view.setLocalTop(view.getAppropriateTop(), animate);

			// If this view has no children, determine the view's width based on the collapsed state of the blocks that contain it.
			if(view.getNumberOfChildren() == 0) {
				double newWidth = view.getWidthBasedOnBlocksCollapsedState();
				view.setLocalWidth(newWidth, animate);
			}

			// if 
			// (1) view is last child of parent
			// (2) view has no more children to lay out itself
			//  then fit parent of view to its children.
			
			EventView viewWhoseParentToFit = view;
			while(true) {

				EventBlockView<?> blockParent = viewWhoseParentToFit.getBlockView();
				if(blockParent == null) break;
				boolean viewIsLastChild = blockParent.getLastChild() == viewWhoseParentToFit;
				boolean viewIsDoneWithChildren = viewWhoseParentToFit.getNumberOfChildren() == 0 || ((EventView)viewWhoseParentToFit.getLastChild()).getEventID() <= view.getEventID();
				if(viewIsLastChild && viewIsDoneWithChildren) {
					blockParent.setLocalWidth(blockParent.getWidthBasedOnBlocksCollapsedState(), animate);
					viewWhoseParentToFit = blockParent;
				}
				else break;
				
			}			
			
			// Where's the next position?
			double lastGlobalPosition = nextGlobalPosition;
			lastViewWasCollapsed = ancestorIsCollapsed || (view instanceof EventBlockView && ((EventBlockView<?>)view).isCollapsed());
			
			// If this view is in a collapsed view, then the next position should be the greater of
			// (1) the right edge of collapsed view.
			// (2) the previous global position.
			if(ancestorIsCollapsed)
				nextGlobalPosition = Math.max(lastGlobalPosition, eldestCollapsedAncestor.getGlobalRight());

			// If this is a block view, then next view either goes after its label, or after its right edge.
			else if(view instanceof EventBlockView)
				nextGlobalPosition = view.getGlobalLeft() + ((EventBlockView<?>)view).getGlobalOffsetForFirstView();

			// Otherwise, its after this view's right edge.
			else if(!isHidden) {
				
				nextGlobalPosition = view.getGlobalRight();
				
			}

			// If this is the last argument event view (and its not collapsed and its view isn't hidden), add some space for the closing paren.
			if(view instanceof ArgumentEventView && !ancestorIsCollapsed && !view.getBlockView().isHidden() && ((ArgumentEventView)view).isLastVisibleArgument())
				nextGlobalPosition += PAREN_AND_BRACE_WIDTH;

			// If this is the last child of a visible invocation view, add space for its closing brace
			if(!isHidden && parent != null && view.getChildAfter() == null && (parent instanceof InvocationBlockView || parent instanceof StartMethodBlockView) && !ancestorIsCollapsed && !parent.isHidden())
				nextGlobalPosition += PAREN_WIDTH + UI.PADDING_WITHIN_EVENTS * 2;
			
			assert lastGlobalPosition <= nextGlobalPosition :
				"\n\n" +
				"The last position was " + lastGlobalPosition + " but the new one is " + nextGlobalPosition + "\n" +
				"The view we just placed was " + view + ", \nwhich had collapsed ancestor" + 
				eldestCollapsedAncestor + " and\n" +
				"right edge = " + (ancestorIsCollapsed ? eldestCollapsedAncestor.getGlobalRight() : "N/A");
			
			lastView =view;
			
		}

		// Now place the unexecuted instruction views to the right of all of the thread views.
		double rightmostThreadViewsRight = 0;
		double bottommostThreadViewsBottom = 0;
		for(View child : getChildren()) {
			if(child instanceof ThreadBlockView) {
				if(child.getLocalRight() > rightmostThreadViewsRight) 
					rightmostThreadViewsRight = child.getLocalRight();
				if(child.getLocalBottom() > bottommostThreadViewsBottom) 
					bottommostThreadViewsBottom = child.getLocalBottom();
			}
		}
		
		unexecutedInstructionsView.setLocalLeft(rightmostThreadViewsRight + UI.PADDING_BETWEEN_EVENTS * 5, false);
		unexecutedInstructionsView.setLocalHeight(unexecutedInstructionsView.getBottommostChildsBottom(), false);
		
		fitToChildrenAndScaleToViewport();

		updateArrows();

		// Animate all of the changes we made.
		if(animate)
			animate(UI.getDuration(), true);
		
		if(scroll) scrollToView(visualizationUI.getSelection());		
		
	}
	
	public void fitToChildrenAndScaleToViewport() {

//		double scale = Math.min(1.0, (visualizationUI.getViewportHeight() - 2) / getBottommostChildsBottom());
		double scale = 1.0;
		
		// Make sure all of the threads fit in the window.
		setPercentToScaleChildren(scale);
		
		// Now fit this visualization around the children, accounting for any scaling done to this visualization view.
		setPreferredSize(
				getRightmostChildsRight() * getPercentToScaleChildren() + UI.PADDING_BETWEEN_EVENTS,
				getBottommostChildsBottom() * getPercentToScaleChildren());

	}
	
	public static char getCharacterShortcutForNumber(int number) {
		
		if(number < 10) return ("" + number).charAt(0);
		else return (char)('a' + (number - 10));
		
	}
	
	private int getNumberForCharacterShortcut(char shortcut) {

		if(shortcut == '0') return 0;
		else if(Character.isDigit(shortcut)) return (shortcut - '1') + 1;
		else return shortcut + 10 - 'a';
		
	}
	
	public boolean collapseSelectedBlockView() {

		EventView eventView = getSelectedEventView();
		if(eventView != null && eventView instanceof EventBlockView) {

			EventBlockView<?> blockView = eventView instanceof EventBlockView ? ((EventBlockView<?>)eventView) : eventView.getBlockView();
			if(blockView != null) {
				
				// Was this block instrumented? If not, don't allow uncollapsing.
				if(blockView instanceof InvocationBlockView) {
					InvocationBlock invocationBlock = ((InvocationBlockView)blockView).getBlock();
					if(!invocationBlock.invocationWasInstrumented()) {
					     Toolkit.getDefaultToolkit().beep();     
						return true;
					}
				}				
				
				blockView.setCollapsed(!blockView.isCollapsed());
				layoutEvents(false, true);
				visualizationUI.setSelection(blockView, false, UI.COLLAPSE_UI);
			}
			else
			     Toolkit.getDefaultToolkit().beep();     

			return true;
			
		}
		else {
		     Toolkit.getDefaultToolkit().beep();     
			return false;
		}

	}
	
	private EventView getEventViewJustAfter(int localX) {

		for(EventView view : viewSequence) {
			if(view.getGlobalLeft() > localX)
				return view;
		}
		return null;
		
	}

	private EventView getEventViewJustBeforePosition(int localX) {

		EventView previous = null;
		for(EventView view : viewSequence) {
			if(view.getGlobalLeft() > localX)
				return previous;
			else
				previous = view;
		}
		return null;
		
	}

	// Draw a time axis
	public void paintBelowChildren(Graphics2D g) {
		
		int whitespace = UI.PADDING_BETWEEN_EVENTS;
		int leftEdgeOfWindow = visualizationUI.getViewportX();
		int rightEdgeOfWindow = leftEdgeOfWindow + visualizationUI.getViewportWidth();
		int edgeOfEvents = (int) (unexecutedInstructionsView.getLocalLeft() - whitespace);
		int right = Math.min(rightEdgeOfWindow, edgeOfEvents) - whitespace;
		int bottom = (int) (getLocalBottom() - UI.getBorderPadding());

		boolean terminationVisible = edgeOfEvents < rightEdgeOfWindow;  
		boolean startVisible = leftEdgeOfWindow < 50;  

		String startLabel = "start of program";
		if(!startVisible) {
			EventView event = getEventViewJustAfter(leftEdgeOfWindow);
			MethodInfo method = event == null ? null : event.getExplanation().getMethod();
			if(event != null) 
				startLabel = method.getJavaName() + "()";
			else
				startLabel = "";
		}
		
		EventView event = getEventViewJustAfter(rightEdgeOfWindow);
		MethodInfo method = event == null ? null : event.getExplanation().getMethod();
		String endLabel = event != null ? method.getJavaName() + "()" : "";
		
		g.setFont(UI.getLargeFont());
		FontMetrics metrics = g.getFontMetrics();

		Rectangle2D startLabelBounds = metrics.getStringBounds(startLabel, g);
		Rectangle2D endLabelBounds = metrics.getStringBounds(endLabel, g);

		leftEdgeOfWindow += whitespace;
		right -= endLabelBounds.getWidth() + whitespace;

		if(right < leftEdgeOfWindow + startLabelBounds.getWidth() + whitespace) return;
		
		g.drawString(startLabel, leftEdgeOfWindow, (bottom + metrics.getDescent()));
		g.drawString(endLabel, right + whitespace, (bottom + metrics.getDescent()));

		leftEdgeOfWindow += startLabelBounds.getWidth() + whitespace;

		g.drawLine(leftEdgeOfWindow, bottom, right, bottom);
		
		Polygon arrowhead = new Polygon();
		arrowhead.addPoint(right - whitespace, bottom + 5);
		arrowhead.addPoint(right - whitespace, bottom - 6);
		arrowhead.addPoint(right + 3, bottom);
		
		g.setColor(UI.getControlBorderColor());
		g.fill(arrowhead);
		
	}
	
	public boolean addNarrativeEntry() {
		
		EventView eventView = getSelectedEventView();
		if(eventView != null) {

			whylineUI.getNarrativeUI().addEntry(eventView.getExplanation());
			return true;
			
		}
		else return false;
		
	}
		
	public boolean goToDataDependency(char c, boolean toSource) {
		
		return goToDataDependencyNumber(getNumberForCharacterShortcut(c), toSource);
		
	}

	// 1 - | number of dependencies |
	public boolean goToDataDependencyNumber(int number, boolean toSource) {
		
		number--;
		
		final EventView eventView = getSelectedEventView();
		if(getSelectedEventView() == null) return false;
		
		SortedMap<Explanation,Explanation> dependencies = answer.getTerminalDataDependencies(eventView.getExplanation());
		
		// Its indexed at 0, so if there are 5 dependencies, the max desired number is 4.
		if(dependencies == null || number >= dependencies.size()) {
			Toolkit.getDefaultToolkit().beep();
			return false;
		}

		Iterator<Explanation> dependencyIterator = dependencies.keySet().iterator();
		Explanation desiredEvent = null;
		for(int i = 0; i <= number; i++) desiredEvent = dependencyIterator.next();

		if(desiredEvent == null) {
			Toolkit.getDefaultToolkit().beep();
			return false;
		}

		// If we want to find the origin of the event, find it now!
		if(toSource) {
			
			// If we can find a view of the source of the value, then this is the event we want. Otherwise
			// we just go to the direct dependency.
			Explanation source = getVisibleSourceOfExplanation(desiredEvent);
			if(source != null)  desiredEvent = source;
			
		}

		EventView viewToShow = getViewOfExplanation(desiredEvent);
		if(viewToShow != null)
			visualizationUI.setSelection(viewToShow, true, UI.DATA_DEPENDENCY_UI);
		else {
			Toolkit.getDefaultToolkit().beep();
			JOptionPane.showMessageDialog(whylineUI, "Oops. Couldn't find a view of the selection you chose.", "Oops.", JOptionPane.ERROR_MESSAGE);
		}
		
		return true;
		
	}

	private Explanation getVisibleSourceOfExplanation(Explanation desiredEvent) {

		// Make sure any pending explanations are processed, so that views of the given event are created, if at all.
		answer.broadcastChanges();
		
		// If we can find a view of the source of the value, then this is the event we want.
		Explanation source = answer.getSourceOfExplanationsValue(desiredEvent);
		return source != null && getViewOfExplanation(source) != null ? source : null;
		
	}

	public boolean handleKeyReleased(KeyEvent event) {
		
		return updateMeta();
		
	}

	private boolean updateMeta() {
	
		ViewContainer container = getContainer();
		boolean old = isMetaDown;
		isMetaDown = container.isShiftDown();
		
		// If the state changed, update everything that needs to be repainted.
		if(old != isMetaDown) {
			// Re-show explanation, since file views depends on meta key state
			if(getSelectedEventView() != null) whylineUI.getFilesView().showExplanation(getSelectedEventView().getExplanation());
			// Update the arrows and their labels
			updateArrows();
		}
		return isMetaDown;
		
	}
	
	public boolean isMetaDown() { return isMetaDown; }

	public boolean showPreviousOrNextEventInThreadOrMethod(boolean previous, boolean inCall) {
		
		EventView view = getPreviousOrNextEventInThreadOrMethod(previous, inCall);
		if(view != null) {
			visualizationUI.setSelection(view, true);
			return true;
		}
		else return false;
		
	}
	
	public EventView getPreviousOrNextEventInThreadOrMethod(boolean previous, boolean inCall) {
	
		View selection = visualizationUI.getSelection();
		if(selection instanceof EventView)
			return getPreviousOrNextEventInThreadOrMethod((EventView)selection, previous, inCall);
		else
			return null;
	
	}
	
	public EventView getPreviousOrNextEventInThreadOrMethod(EventView selection, boolean previous, boolean inCall) {	
				
		int eventID = selection.getEventID();
		int newID = -1;
		do {

			newID = 
				previous ? 
					inCall ? 
						trace.getPreviousEventIDInMethod(eventID) :	
						trace.getPreviousEventInThread(eventID) 
					:
					inCall ?
						trace.getNextEventIDInMethod(eventID) :
						trace.getNextEventIDInThread(eventID);
			
			
			if(newID >= 0) {
				// Skip start events
				if(trace.getKind(newID) != EventKind.START_METHOD) {
					Explanation explanation = answer.getExplanationFor(newID);
					answer.broadcastChanges();
					EventView view = getViewOfExplanation(explanation);
					if(view != null)
						return view;
				}
			}
			// If there's nothing before this, then return false.
			else return null;
			
			// If there was something, but we didn't make a view of it, try the next/previous one.
			eventID = newID;
			
		} while(true);
		
	}
		
	public void handleArrowOverChanged() {

		repaint();
		
	}

	public EventView getVisibleEventViewAtAfter(int localX) {

		for(EventView view : viewSequence) {
			
			if(!view.isHidden() && view.getGlobalLeft() > localX)
				return view;
			
		}
		return null;		
		
	}
	
	public boolean handleMouseDown(int x, int y, int button) {

		// Show the event just before the visible event clicked before
		EventView view = getVisibleEventViewAtAfter(visualizationUI.getViewportX() + x);
		
		if(view != null) {
			EventView before = getPreviousOrNextEventInThreadOrMethod(view, true, false);
			if(before != null) {
				visualizationUI.setSelection(before, true);
				return true;
			}
		}

		return false;
		
	}
	
	
	public boolean handleMouseMoved(int x, int y) {
		
		whylineUI.setArrowOver(-1);
		return true;
		
	}
	
	public boolean handleKeyPressed(KeyEvent event) {

		updateMeta();
		
		switch(event.getKeyCode()) {

		case KeyEvent.VK_T :
			whylineUI.getActions().showHideThreads.actionPerformed(null);
			return true;

		case KeyEvent.VK_B :
			
			EventView view = getSelectedEventView();
			if(view != null) {
				Instruction inst = trace.getInstruction(view.getEventID());
				if(inst != null) {
					Line line = inst.getLine();
					if(line != null) {
						boolean wasNew = whylineUI.getPersistentState().addRelevantLine(line);
						if(wasNew)
							return true;
					}
				}
			}
			Toolkit.getDefaultToolkit().beep();
			return true;

		case KeyEvent.VK_ESCAPE :
			getWhylineUI().getActions().collapseBlock.execute();
			return true;

		case KeyEvent.VK_ENTER :
			getWhylineUI().getActions().addToExplanation.execute();
			return true;

		case KeyEvent.VK_COMMA :

			if(!showPreviousOrNextEventInThreadOrMethod(true, true))
				Toolkit.getDefaultToolkit().beep();
			return true;

		case KeyEvent.VK_PERIOD :

			if(!showPreviousOrNextEventInThreadOrMethod(false, true))
				Toolkit.getDefaultToolkit().beep();
			return true;

		case KeyEvent.VK_LEFT :
			
			if(isMetaDown) {
				if(!showPreviousOrNextEventInThreadOrMethod(true, false))
					Toolkit.getDefaultToolkit().beep();
			}
			else
				getWhylineUI().getActions().goToPreviousEvent.execute();
			return true;

		case KeyEvent.VK_RIGHT :
			
			if(isMetaDown) {
				if(!showPreviousOrNextEventInThreadOrMethod(false, false))
					Toolkit.getDefaultToolkit().beep();
			}
			else
				getWhylineUI().getActions().goToNextEvent.execute();
			return true;

		case KeyEvent.VK_UP :
			getWhylineUI().getActions().goToPreviousBlock.execute();
			return true;

		case KeyEvent.VK_DOWN : 
			getWhylineUI().getActions().goToNextBlock.execute();
			return true;

		}
		
		if(Character.isLetterOrDigit(event.getKeyChar()))
			return goToDataDependency(event.getKeyChar(), !isMetaDown);

		return false;
		
	}
	
	public UnexecutedInstructionView getUnexecutedInstructionView(UnexecutedInstruction inst) {
	
		return unexecutedViews.get(inst);
		
	}
	
	public UnexecutedInstructionView getUnexecutedInstructionAt(int row, int column) {
		
		if(column >= unexecutedGrid.size()) return null;
		else if(column < 0) return null;
		
		ArrayList<UnexecutedInstructionView> viewColumn = unexecutedGrid.get(column);
		
		if(row < 0) return null;
		else if(viewColumn.size() == 0) return null;
		else if(row >= viewColumn.size()) row = viewColumn.size() - 1;

		return viewColumn.get(row);		
		
	}
	
	/**
	 * Used to sort unexecuted instructions in a useful order.
	 */
	private final int compareUnexecutedInstructions(UnexecutedInstruction o1, UnexecutedInstruction o2) {
		
		Instruction i1 = o1.getInstruction();
		Instruction i2 = o2.getInstruction();
		
		if(i1 == i2) return 0;
		
		QualifiedClassName thisName = i1.getClassfile().getInternalName();
		QualifiedClassName thatName = i2.getClassfile().getInternalName();
		
		boolean thisIsFamiliar = trace.classIsReferencedInFamiliarSourceFile(thisName);
		boolean thatIsFamiliar = trace.classIsReferencedInFamiliarSourceFile(thatName);
		
		// Familiar instructions come first
		if(thisIsFamiliar && !thatIsFamiliar) return -1;
		else if(!thisIsFamiliar && thatIsFamiliar) return 1;
			
		int diff = o1.getIncoming().size() - o2.getIncoming().size();
		if(diff != 0) return -diff;
		
		// If neither has more incoming, then alphabetically by class.
		int classNameComparison = thisName.compareTo(thatName);
		if(classNameComparison != 0) return classNameComparison;
		
		// Then by method name.
		int methodNameComparison = i1.getMethod().getJavaName().compareTo(i2.getMethod().getJavaName());
		if(methodNameComparison != 0) return methodNameComparison;
		
		// If its the same method, then by instruction index.
		return i1.getIndex() - i2.getIndex();

	}
	
	private class UnexecutedInstructionsView extends View {
		
		public UnexecutedInstructionsView() {
		
			layout();
			
		}
		
		// Returns true if a view was created
		private void layout() {

			// Start from scratch
			unexecutedGrid.clear();
			removeChildren();
			
			// Width of view = [max depth of tree] x [standard width of view]
			final UnexecutedInstruction[] unexecuted = answer.getUnexecutedInstructions();

			// If there's no unexecuted instruction, give this no size.
			if(unexecuted.length == 0) {
				
				setLocalWidth(0, false);
				setLocalHeight(0, false);
				return;
				
			}

			// Sort unexecuted instructions  by familiarity.
			Comparator<UnexecutedInstruction> comparator = new Comparator<UnexecutedInstruction>() {
				public int compare(UnexecutedInstruction o1, UnexecutedInstruction o2) { return compareUnexecutedInstructions(o1, o2); }
			};
			
			Set<UnexecutedInstruction> visited = new HashSet<UnexecutedInstruction>();
			SortedSet<UnexecutedInstruction> instructionsToLayout = new TreeSet<UnexecutedInstruction>(comparator);
			SortedSet<UnexecutedInstruction> instructionsToLayoutNext = new TreeSet<UnexecutedInstruction>(comparator);

			// Start by laying out the first unexecuted instruction.
			for(UnexecutedInstruction i : unexecuted)
				instructionsToLayout.add(i);
						
			int depth = 0;
			
			// Traverse the subgraph, bread-first.
			while(instructionsToLayout.size() > 0) {

				ArrayList<UnexecutedInstructionView> column = new ArrayList<UnexecutedInstructionView>();
				unexecutedGrid.add(column);
				
				// Reset the top for each successive layer of the tree to layout views from top to bottom.
				int top = 0;
				
				for(UnexecutedInstruction inst : instructionsToLayout) {

					UnexecutedInstructionView view = unexecutedViews.get(inst);

					// If there's not a view yet, make one, and lay it out.
					if(view == null) {
						
						view = new UnexecutedInstructionView(Visualization.this, inst);

						// Map the instruction not executed to the existing view.
						unexecutedViews.put(inst, view);
						
					}

					// If we haven't added the child yet, add it.
					if(view.getParent() == null) {
						view.setGridLocation(column.size(), unexecutedGrid.size() - 1);
						addChild(view);
					}

					view.setLocalTop(top, false);

					column.add(view);
					
					// Now initialize each incoming view, moving down as we go.
					for(UnexecutedInstruction incoming : inst.getIncoming()) {
						
						assert incoming != null;
						instructionsToLayoutNext.add(incoming);
						
					}

					top += UnexecutedInstructionView.ICON_SIZE + UnexecutedInstructionView.LABEL_HEIGHT;
					
				}

				instructionsToLayout.clear();
				
				SortedSet<UnexecutedInstruction> temp = instructionsToLayout;
				instructionsToLayout = instructionsToLayoutNext;
				instructionsToLayoutNext = temp;
				
				depth++;
				
			}

			// Now that we've added all of the views, lay them out using their column information.
			for(View child : getChildren()) {
				int realDepth = depth - ((UnexecutedInstructionView)child).getColumn();
				child.setLocalLeft((realDepth - 1) * UnexecutedInstructionView.STANDARD_WIDTH, false);
			}
			
			setLocalWidth(getRightmostChildsRight(), false);
			setLocalHeight(getBottommostChildsBottom(), false);

		}
				
		public void paintBelowChildren(Graphics2D g) {
		
			for(View child : getChildren())
				((UnexecutedInstructionView)child).selectedOrPointedToFromSelection = false;
	
			// If something is selected, tell it and its incoming and outgoing that they should draw opaque
			UnexecutedInstructionView selection = getSelectedUnexecutedInstructionView(); 
			if(selection != null) {

				selection.selectedOrPointedToFromSelection = true;

				for(UnexecutedInstruction in : selection.getUnexecutedInstruction().getIncoming()) {

					UnexecutedInstructionView view = getUnexecutedInstructionView(in);
					if(view != null) view.selectedOrPointedToFromSelection = true;

				}
				
				for(UnexecutedInstruction out : selection.getUnexecutedInstruction().getOutgoing()) {
					
					UnexecutedInstructionView view = getUnexecutedInstructionView(out);
					if(view != null) view.selectedOrPointedToFromSelection = true;

				}
				
			}
			
		}

	}
	
}
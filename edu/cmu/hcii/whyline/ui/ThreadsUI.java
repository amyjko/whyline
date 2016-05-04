package edu.cmu.hcii.whyline.ui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.tree.TreePath;

import edu.cmu.hcii.whyline.qa.Explanation;
import edu.cmu.hcii.whyline.qa.Question;
import edu.cmu.hcii.whyline.qa.WhyDidEventOccur;
import edu.cmu.hcii.whyline.trace.Trace;
import edu.cmu.hcii.whyline.trace.nodes.*;
import edu.cmu.hcii.whyline.ui.components.HeadlinedTreePanel;
import edu.cmu.hcii.whyline.ui.components.WhylineButton;

/**
 * @author Andrew J. Ko
 *
 */
public class ThreadsUI extends HeadlinedTreePanel<ThreadState> {

	private ThreadState[] threads = null;
	
	private int currentEventID;
	
	private final Action watchAction = new AbstractAction("watch " + UI.UP_ARROW) {
		public void actionPerformed(ActionEvent e) {
			whylineUI.getObjectsUI().addObject(getSelectedObjectID());
			setEnabled(false);
		}
	};
	
	private final Action explainAction = new AbstractAction("explain") {
		public void actionPerformed(ActionEvent e) {
			int eventID = getSelectedDynamicNode().getAssociatedEventID();
			Question<?> q = new WhyDidEventOccur(whylineUI, eventID, getTrace().getDescription(eventID));
			whylineUI.answer(q);
		}
	};

	private final Action showCallAction = new AbstractAction("show call") {
		public void actionPerformed(ActionEvent e) {
			int eventID = ((FrameState)getSelectedDynamicNode()).getInvocationID();
			Explanation call = whylineUI.getVisualizationUIVisible().getAnswer().getExplanationFor(eventID);
			whylineUI.selectExplanation(call, true, "threads");
		}
	};

	public ThreadsUI(WhylineUI whylineUI) {
	
		super("threads", whylineUI);

		watchAction.setEnabled(false);
		
		setMinimumSize(new Dimension(UI.getDefaultInfoPaneWidth(whylineUI), 0));
		setPreferredSize(new Dimension(UI.getDefaultInfoPaneWidth(whylineUI), UI.getDefaultInfoPaneHeight(whylineUI)));

		addTool(new WhylineButton(watchAction, "watch the selected object"));		
		if(whylineUI.getMode() == WhylineUI.Mode.WHYLINE) {
			addTool(new WhylineButton(explainAction, "explain why the selected field has its current value"));
			addTool(new WhylineButton(showCallAction, "show the selected call in the visualization"));
		}
		
	}

	protected void handleSelection() {

		long objectID = getSelectedObjectID();
		watchAction.setEnabled(objectID > 0 && !whylineUI.getObjectsUI().isWatching(objectID));

		if(whylineUI.isWhyline()) {

			DynamicNode<?> node = getSelectedDynamicNode();
			explainAction.setEnabled(node != null && node.getAssociatedEventID() > 0);

			if(objectID > 0) 
				whylineUI.getGraphicsUI().setHighlight(whylineUI.getTrace().getObjectNode(objectID));
			
			if(whylineUI.getVisualizationUIVisible() != null) {

				showCallAction.setEnabled(false);
				DynamicNode<?> frame = getSelectedDynamicNode();
				if(node instanceof FrameState) {
					int invocationID = ((FrameState)frame).getInvocationID();
					showCallAction.setEnabled(true);
				}
				
			}
			repaint();

		}
		
	}
	
	protected void handleExpansion() {

		if(getSelectedDynamicNode() instanceof ReferenceState) {
			ReferenceState ref = (ReferenceState)getSelectedDynamicNode();
			for(ReferenceState node : ref.getChildren())
				node.propagateCurrentEventID(ref.getCurrentEventID());
		}

	}

	public long getSelectedObjectID() {
		
		if(getSelectionPath() == null) return 0;
		Object selection = getSelectionPath().getLastPathComponent();
		return selection instanceof ReferenceState ? ((ReferenceState)selection).getObjectIDForChildren() : 0;
		
	}

	private Trace getTrace() { return whylineUI.getTrace(); }

	public void showEventID(int eventID) {
		
		if(currentEventID == eventID) return;

		if(!whylineUI.isDynamicInfoShowing()) return;
		
		currentEventID = eventID;
		
		getRoot().resetChildren();
		
		// Make the thread states if necessary
		if(threads == null && whylineUI.getTrace().isDoneLoading()) {
			
			threads = new ThreadState[whylineUI.getTrace().getNumberOfThreads()];
			for(int id = 0; id < threads.length; id++)
				threads[id] = new ThreadState(whylineUI.getTrace(), id);

		}

		// Include each thread only if it is active at this eventID.
		for(ThreadState thread : threads) {
			
			if(getTrace().getThreadFirstEventID(thread.getThreadID()) <= currentEventID &&
					getTrace().getThreadLastEventID(thread.getThreadID()) >= currentEventID) {
				getRoot().addChild(thread);
				thread.showEventID(currentEventID);
			}
			
		}

		// Show all of the active threads.
		updateTree();
		
		if(currentEventID >= 0) {
		
			// Show the thread and call. We create these quite lazily, so we first check if they're here.
			ThreadState currentThread = threads[whylineUI.getTrace().getThreadID(currentEventID)];
			FrameState currentFrame = currentThread.getFrameFor(currentEventID);
			LocalState currentLocal = currentFrame == null ? null : currentFrame.getLocalFor(currentEventID);
			
			// Show as much as we can based on the information already derived.
			TreePath threadPath = 
				new TreePath(
						currentLocal != null ? new Object[] { getRoot(), currentThread, currentFrame, currentLocal } :
						currentFrame != null ? new Object[] { getRoot(), currentThread, currentFrame } :
						new Object[] { getRoot(), currentThread  });
			
			show(threadPath, true);
			
		}
		
	}

}
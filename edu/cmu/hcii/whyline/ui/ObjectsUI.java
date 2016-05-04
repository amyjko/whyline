package edu.cmu.hcii.whyline.ui;

import java.awt.Dimension;
import java.awt.Toolkit;
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
public final class ObjectsUI extends HeadlinedTreePanel<ObjectState> {

	private final Action explainAction = new AbstractAction("explain") {
		public void actionPerformed(ActionEvent e) {
			DynamicNode<?> node = getSelectedDynamicNode();
			if(node != null) {
				int eventID = node.getAssociatedEventID();
				Question<?> q = new WhyDidEventOccur(whylineUI, eventID, getTrace().getDescription(eventID));
				whylineUI.answer(q);
			}
		}
	};

	private final Action removeAction = new AbstractAction("" + UI.MINUS) {
		public void actionPerformed(ActionEvent e) {
			whylineUI.getObjectsUI().removeObject(getSelectedObjectID());
			setEnabled(false);
		}
	};

	private final Action previousAssignment = new AbstractAction("previous =") {
		public void actionPerformed(ActionEvent e) {

			Explanation explanation = getExplanationOfPreviousOrNextFieldAssignment(false);
			if(explanation == null) Toolkit.getDefaultToolkit().beep();
			else	whylineUI.selectExplanation(explanation,true, "objects");

		}
	};

	private final Action nextAssignment = new AbstractAction("next =") {
		public void actionPerformed(ActionEvent e) {

			Explanation explanation = getExplanationOfPreviousOrNextFieldAssignment(true);
			if(explanation == null) Toolkit.getDefaultToolkit().beep();
			else	whylineUI.selectExplanation(explanation,true, "objects");
			
		}
	};

	public ObjectsUI(WhylineUI whylineUI) {
	
		super("objects", whylineUI);
		
		setMinimumSize(new Dimension(UI.getDefaultInfoPaneWidth(whylineUI), 0));
		setPreferredSize(new Dimension(UI.getDefaultInfoPaneWidth(whylineUI), UI.getDefaultInfoPaneHeight(whylineUI)));

		removeAction.setEnabled(false);
		
		addTool(new WhylineButton(removeAction, "remove the selected object from the watch list"));
		if(whylineUI.isWhyline()) {
			addTool(new WhylineButton(explainAction, "explain why the selected field has its value"));
			addTool(new WhylineButton(previousAssignment, "find the previous assignment to this field"));
			addTool(new WhylineButton(nextAssignment, "find the next assignment to this field"));
		}
		
	}

	private Explanation getExplanationOfPreviousOrNextFieldAssignment(boolean next) {
		
		if(!(getSelectedDynamicNode() instanceof FieldState))
			return null;
		FieldState field = (FieldState)getSelectedDynamicNode();
		int eventID = field.getCurrentEventID();
		int assignmentID = 
			next ?
				getTrace().findFieldAssignmentAfter(field.getField(), field.getObjectID(), eventID + 1) :
				getTrace().findFieldAssignmentBefore(field.getField(), field.getObjectID(), eventID - 1);
		if(assignmentID == eventID || assignmentID < 0)
			return null;
		else
			return whylineUI.getVisualizationUIVisible().getAnswer().getExplanationFor(assignmentID);
		
	}
	
	public boolean isWatching(long objectID) { return getRoot().getChildren().contains(getTrace().getObjectNode(objectID)); }
	
	protected void handleSelection() {
		
		Node<?> selection = getSelectedNode(); 
					
		long objectID = getSelectedObjectID();
		removeAction.setEnabled(objectID > 0);
		
		if(whylineUI.isWhyline()) {
		
			DynamicNode<?> node = getSelectedDynamicNode();
			explainAction.setEnabled(node != null && node.getAssociatedEventID() > 0);
	
			if(objectID > 0) whylineUI.getGraphicsUI().setHighlight(whylineUI.getTrace().getObjectNode(objectID));
			
			previousAssignment.setEnabled(node instanceof FieldState);
			nextAssignment.setEnabled(node instanceof FieldState);
			
		}

	}

	public long getSelectedObjectID() {
		
		TreePath path = getSelectionPath();
		if(path== null || path.getPathCount() <= 1) return 0;
		Object selection = path.getPathComponent(1);
		return selection instanceof ReferenceState ? ((ReferenceState)selection).getObjectIDForChildren() : 0;
		
	}

	public void showEventID(int eventID) {
		
		if(!whylineUI.isDynamicInfoShowing()) return;

		for(ObjectState obj : getRoot().getChildren())
			obj.propagateCurrentEventID(eventID);

		DynamicNode<?> state = getSelectedDynamicNode();
		if(state != null) state.update();
		
		updateTree();
		
	}
	
	public void addObject(long objectID) {

		ObjectState obj = getTrace().getObjectNode(objectID);
		getRoot().addChild(obj);
		updateTree();

		whylineUI.getPersistentState().addRelevantObject(objectID);

	}
	
	public void removeObject(long objectID) {
		
		getRoot().removeChild(getTrace().getObjectNode(objectID));
		updateTree();

		whylineUI.getPersistentState().removeRelevantObject(objectID);
		
	}

	public void removeAllObjects() {

		getRoot().resetChildren();
		updateTree();
		
	}

	protected boolean isLeaf(Node<?> node) { return node instanceof FieldState; }

	public Trace getTrace() { return whylineUI.getTrace(); }

	protected void handleExpansion() {

		if(getSelectedDynamicNode() instanceof ReferenceState) {
			ReferenceState ref = (ReferenceState)getSelectedDynamicNode();
			
			for(ReferenceState node : ref.getChildren())
				node.propagateCurrentEventID(ref.getCurrentEventID());
			
		}

	}

}
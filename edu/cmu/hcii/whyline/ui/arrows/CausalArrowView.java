package edu.cmu.hcii.whyline.ui.arrows;

import java.awt.Color;
import java.awt.Stroke;

import edu.cmu.hcii.whyline.bytecode.Instruction;
import edu.cmu.hcii.whyline.qa.Explanation;
import edu.cmu.hcii.whyline.trace.*;
import edu.cmu.hcii.whyline.ui.*;
import edu.cmu.hcii.whyline.ui.qa.Visualization;

/**
 * @author Andrew J. Ko
 *
 */
public abstract class CausalArrowView extends ArrowView {

	protected final Explanation value, from, to;
	
	protected final Relationship relationship;
	
	protected static final int END_POINT_RADIUS = 3;

	protected static final int SELECTION_THRESHOLD = 5;

	protected final int fromEventID;

	final int toEventID;

	protected final Instruction fromInstruction;
	protected final Instruction toInstruction;
		
	protected final String label;

	public CausalArrowView(WhylineUI whylineUI, Explanation value, Explanation from, Explanation to, Relationship relationship, int number) {

		super(whylineUI, number);
		
		this.value = value;
		this.from = from;
		this.to = to;

		this.relationship = relationship;
		
		int fromID = from.getEventID();
		int toID = to.getEventID();		

		this.fromInstruction = whylineUI.getTrace().getInstruction(fromID);
		this.toInstruction = whylineUI.getTrace().getInstruction(toID);

		this.fromEventID = fromID;
		this.toEventID = toID;

		Trace trace = whylineUI.getTrace();

		final char shortcut;
		final String event;
		final String note;
		StringBuilder builder = new StringBuilder();
		if(relationship == Relationship.CONTROL) {
			
			shortcut = UI.UP_WHITE_ARROW;
			event = "this execute";
			note = null;
			
		}
		else {
			
			EventKind kind = trace.getKind(value.getEventID());

			int valueID = value.getEventID();
			shortcut = Visualization.getCharacterShortcutForNumber(dependencyNumber);
			String name = trace.getNameAssociatedWithEvent(valueID);
			if(name == null) name = "this expression";
			final String action;
			final String valueDescription;
			if(kind == EventKind.SETLOCAL) {
				action = "=";
				Value val = trace.getDefinitionValueSet(valueID);
				valueDescription = val.getDisplayName(false);
			}
			else if(kind == EventKind.RETURN) {
				action = "return";
				valueDescription = trace.getReturnValueReturned(valueID).getDisplayName(false);
			}
			else if(kind.isArgument) {
				action = "=";
				valueDescription = trace.getArgumentValueDescription(valueID);
			}
			else if(kind.isValueProduced) {
				action = "=";
				valueDescription = trace.getDescription(valueID);
			}
			else if(kind.isDefinition) {
				action = "=";
				Value val = trace.getDefinitionValueSet(valueID);
				valueDescription = val == null ? "?" : val.getDisplayName(false);
			}
			else if(kind == EventKind.INVOKE_SPECIAL) {
				action = "instantiate";
				valueDescription = "";
			}
			else if(kind.isInvocation) {
				action = "return";
				valueDescription = "";
			}
			else {
				action ="";
				valueDescription = "-";
			}

			boolean toSource = value != from;
			
			event = name + " " + action + " " + valueDescription;

			note = toSource ? "(source)" : "(producer)";
			
		}
	
		builder.append("(");
		builder.append(shortcut);
		builder.append(") ");
		builder.append("why did ");
		builder.append(event);
		builder.append("?");
		if(note != null) { builder.append(" "); builder.append(note); }
		label = builder.toString();  
		
	}

	protected void clicked() {
		
		if(dependencyNumber == 0)
			whylineUI.getActions().goToPreviousBlock.execute();
		else {
			Visualization viz = whylineUI.getVisualizationUIVisible().getVisualization();
			viz.goToDataDependencyNumber(dependencyNumber, !viz.isMetaDown());
		}
		
	}
	
	public static enum Relationship {

		CONTROL() { 
			public Color getColor(boolean selected) { return UI.CONTROL_COLOR; }
			public Stroke getStroke(boolean selected) { return selected ? UI.SELECTED_STROKE: UI.UNSELECTED_STROKE; }
		}, 
		DATA() { 
			public Color getColor(boolean selected) { return UI.DATA_COLOR; }
			public Stroke getStroke(boolean selected) { return selected ? UI.SELECTED_DASHED_STROKE: UI.UNSELECTED_DASHED_STROKE; }
		},
		;
		
		private Relationship() {}
		
		public abstract Color getColor(boolean selected);
		public abstract Stroke getStroke(boolean selected);
		
	}

}

package edu.cmu.hcii.whyline.ui.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Andrew J. Ko
 *
 */
public class WhylineMultiStateButton<T extends Enum<T>> extends WhylineButton {

	private T[] states;
	private Enum<T> currentState;
	
	public WhylineMultiStateButton(T[] states) {
		
		super(null, null);

		this.states = states; 
		
		setState(states[0]);
		
		addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				toggle();
			}
		});
		
	}
	
	public void setState(Enum<T> state) {
		
		currentState = state;
		setText(state.toString());
		
	}
	
	public void toggle() {

		int ord = currentState.ordinal();
		ord++;
		if(ord >= states.length)
			ord = 0;
		
		setState(states[ord]);
		
	}
	
	public Enum<T> getState() { return currentState; } 
	
}
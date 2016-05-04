package edu.cmu.hcii.whyline.ui.events;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import edu.cmu.hcii.whyline.ui.WhylineUI;

/**
 * @author Andrew J. Ko
 *
 */
public abstract class LoggedAction extends AbstractAction {

	private final WhylineUI whylineUI;
	
	public LoggedAction(WhylineUI whylineUI) {
		
		this(whylineUI, "");
		
	}

	public LoggedAction(WhylineUI whylineUI, String name) {
		
		super(name);
		
		this.whylineUI = whylineUI;
		
	}

	public void execute() {
		
		AbstractUIEvent<?> event = act();
		if(event != null) whylineUI.log(event);
		
	}
	
	protected abstract AbstractUIEvent<?> act();
	
	public final void actionPerformed(ActionEvent e) {
				
		execute();
		
	}
	
}

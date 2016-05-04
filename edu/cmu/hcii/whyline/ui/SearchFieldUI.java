package edu.cmu.hcii.whyline.ui;

import edu.cmu.hcii.whyline.analysis.TextSearch;
import edu.cmu.hcii.whyline.ui.components.WhylineTextField;
import edu.cmu.hcii.whyline.ui.events.AbstractUIEvent;
import edu.cmu.hcii.whyline.ui.events.LoggedAction;
import edu.cmu.hcii.whyline.ui.events.Note;

/**
 * @author Andrew J. Ko
 *
 */
public class SearchFieldUI extends WhylineTextField {

	private final WhylineUI whylineUI;

	public SearchFieldUI(WhylineUI ui) {
		
		super("", 0, "search code...");
		
		this.whylineUI = ui;

		addActionListener(new LoggedAction(whylineUI) {
			protected AbstractUIEvent<?> act() {
				if(!getText().equals(""))
					SearchFieldUI.this.whylineUI.getLinesUI().addResults(new TextSearch(SearchFieldUI.this.whylineUI, getText(), SearchFieldUI.this.whylineUI.getSearchMode()));
				return new Note("search:" + getText());
			}

		});
		
	}
	
}

package edu.cmu.hcii.whyline.ui.source;

import java.awt.Color;
import java.awt.Dimension;

import edu.cmu.hcii.whyline.ui.UI;
import edu.cmu.hcii.whyline.ui.WhylineUI;
import edu.cmu.hcii.whyline.ui.views.DynamicComponent;

/**
 * @author Andrew J. Ko
 *
 */
public class FilesUI extends DynamicComponent {

	private final WhylineUI whylineUI;

	private final FilesView files;
	
	public FilesUI(WhylineUI whylineUI) {
		
		super(whylineUI, Sizing.FIT, Sizing.FIT);

		this.whylineUI = whylineUI;

		this.files = new FilesView(whylineUI);
		
		setView(files);
		
		setMinimumSize(new Dimension(UI.getDefaultInfoPaneWidth(whylineUI), UI.getDefaultInfoPaneHeight(whylineUI)));
		
		setBorder(null);
		getScrollPane().setOpaque(false);		
		
	}
		
	public Color getBackground() { return null; }
	
	public FilesView getFilesView() { return files; }

	public int getHorizontalScrollIncrement() { return 0; }
	public int getVerticalScrollIncrement() { return 0; }

	public void handleArrowOverChanged() { files.handleArrowOverChanged(); }
		
}

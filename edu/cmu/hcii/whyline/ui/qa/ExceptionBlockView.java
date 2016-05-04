package edu.cmu.hcii.whyline.ui.qa;

import java.awt.Color;

import edu.cmu.hcii.whyline.qa.ExceptionBlock;
import edu.cmu.hcii.whyline.ui.UI;

/**
 * @author Andrew J. Ko
 *
 */
public class ExceptionBlockView extends EventBlockView<ExceptionBlock> {

	public ExceptionBlockView(Visualization visualization, ExceptionBlock block) {
		
		super(visualization, block);
			
	}
	
	public String determineFirstLine() { 
		
		return "throw"; 
		
	}

	public String determineSecondLine() { return null; }
	
	protected Color determineBorderColor() { 
		
		return UI.ERROR_COLOR; 
		
	}

}

package edu.cmu.hcii.whyline.ui.arrows;

import java.awt.Graphics2D;
import java.util.List;

import edu.cmu.hcii.whyline.source.TokenRange;
import edu.cmu.hcii.whyline.ui.WhylineUI;
import edu.cmu.hcii.whyline.ui.views.View;
import edu.cmu.hcii.whyline.ui.views.ViewContainer;

/**
 * @author Andrew J. Ko
 *
 */
public abstract class ArrowView extends View {

	protected final WhylineUI whylineUI;

	protected final int dependencyNumber;

	public ArrowView(WhylineUI whylineUI , int number) {
		
		this.whylineUI = whylineUI;
		this.dependencyNumber = number;

	}
	
	public final boolean handleMouseMove(int x, int y) {

		if(getContainer().mouseIsFocused()) return true;
		
		boolean contains = containsLocalPoint(x, y);
		whylineUI.setArrowOver(contains ? dependencyNumber : -1);
		return contains;
		
	}

	public void handleMouseExit() { 

		ViewContainer c = getContainer();
		if(c != null && c.mouseIsFocused()) return;
		whylineUI.setArrowOver(-1);
		
	}

	public final boolean handleMouseDown(int x, int y, int button) {
		
		if(!containsLocalPoint(x, y)) return false;
		
		clicked();
		return true;
		
	}
	
	public abstract List<TokenRange> getViableTargetTokenRanges(); 

	protected abstract void clicked();

	public abstract void paintAboveChildren(Graphics2D g);
	
	public abstract boolean containsLocalPoint(int x, int y);

	public abstract void layout();

}

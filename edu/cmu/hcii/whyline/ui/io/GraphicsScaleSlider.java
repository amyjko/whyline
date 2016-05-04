package edu.cmu.hcii.whyline.ui.io;

import java.util.Hashtable;

import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.cmu.hcii.whyline.ui.UI;
import edu.cmu.hcii.whyline.ui.WhylineUI;
import edu.cmu.hcii.whyline.ui.components.WhylineLabel;

/**
 * @author Andrew J. Ko
 *
 */
public class GraphicsScaleSlider extends JSlider {

	private final WhylineUI whylineUI;
	
	private boolean fitGraphicsToWindow;

	private final ChangeListener scaleListener = new ChangeListener() {
		public void stateChanged(ChangeEvent e) { 
			whylineUI.getGraphicsUI().updateScale(); 
		}
	};

	public GraphicsScaleSlider(WhylineUI whylineUI) {
		
		super(HORIZONTAL, UI.GRAPHICS_SCALE_MIN, UI.GRAPHICS_SCALE_MAX, 100);
		
		this.whylineUI = whylineUI;
		
		int min = UI.GRAPHICS_SCALE_MIN;
		int max = UI.GRAPHICS_SCALE_MAX;
		Hashtable<Integer,JComponent> labels = new Hashtable<Integer,JComponent>();
		labels.put(min, new WhylineLabel("" + min + "%", UI.getSmallFont()));
		labels.put(100, new WhylineLabel("100%", UI.getSmallFont()));
		labels.put(max, new WhylineLabel("" + max + "%", UI.getSmallFont()));
		setLabelTable(labels);
		setFocusable(false);
		setPaintLabels(true);
		setPaintTicks(true);
		
		setToolTipText("controls the zoom level of graphical output");
		
	}
	
	public void setFitToWindow(boolean fitToWindow) {
		
		this.fitGraphicsToWindow = fitToWindow;
		if(fitGraphicsToWindow) {
			setEnabled(false);
			removeChangeListener(scaleListener);
			setVisible(false);
		}
		else {
			setValue(100);
			setEnabled(true);
			addChangeListener(scaleListener);
			setVisible(true);
		}
		whylineUI.getGraphicsUI().updateScale();

	}

	public boolean isFitToWindow() { return fitGraphicsToWindow; }
	
}

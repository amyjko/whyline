package edu.cmu.hcii.whyline.ui.views;

import java.awt.Graphics2D;

import edu.cmu.hcii.whyline.ui.UI;

/**
 * @author Andrew J. Ko
 *
 */
public class ScrollBarView extends View {

	private final ScrollableView scrollable;
	private final Bar bar;
	private final boolean vertical;
	
	public ScrollBarView(ScrollableView scrollable, boolean vertical) {
		
		this.scrollable = scrollable;
		this.vertical = vertical;
		this.bar = new Bar();

		addChild(bar);
		
		if(vertical) setLocalWidth(UI.SCROLL_BAR_SIZE, false);
		else setLocalHeight(UI.SCROLL_BAR_SIZE, false);

	}
	
	public void paintBelowChildren(Graphics2D g) {
		
		// Draw the track
		fillRoundBoundaries(UI.getControlCenterColor(), g, UI.getRoundedness(), UI.getRoundedness());		
		drawRoundBoundaries(UI.getControlFrontColor(), g, UI.getRoundedness(), UI.getRoundedness());		
		
	}
		
	public void layout(boolean animate) {
				
		bar.layout(animate);
		
	}
	
	public boolean handleMouseDown(int localX, int localY, int mouseButton) { 
		
		if(vertical) {
//			int newY = scrollable.getVerticalPosition() + scrollable.getVerticalVisible() * (localY < bar.getLocalTop() ? -1 : 1);
			double newY = ((localY - getLocalTop()) / getLocalHeight()) * scrollable.getVerticalSize() - scrollable.getVerticalVisible() / 2;
			scrollable.setVerticalPosition((int)boundVerticalBarPosition(newY));
		}
		else {
//			int newX = scrollable.getHorizontalPosition() + scrollable.getHorizontalVisible() * (localX < bar.getLocalLeft() ? -1 : 1);
			double newX = ((localX - getLocalLeft()) / getLocalWidth()) * scrollable.getHorizontalSize() - scrollable.getHorizontalVisible() / 2;
			scrollable.setHorizontalPosition((int)boundHorizontalBarPosition(newX));
		}
		
		layout(false);
		
		return true;
		
	}

	private double boundVerticalBarPosition(double proposal) {
		
		return Math.max(0.0, Math.min(scrollable.getVerticalSize() - scrollable.getVerticalVisible(), proposal));
		
	}
	
	private double boundHorizontalBarPosition(double proposal) {
		
		return Math.max(0.0, Math.min(scrollable.getHorizontalSize() - scrollable.getHorizontalVisible(), proposal));

	}
	
	private class Bar extends View {
		
		public Bar() {

			if(vertical) {
				
				setLocalWidth(UI.SCROLL_BAR_SIZE - 3, false);
				setLocalLeft(2, false);
				
			}
			else {
				
				setLocalHeight(UI.SCROLL_BAR_SIZE - 3, false);
				setLocalTop(2, false);
	
			}
			
			layout(false);
			
		}
		
		public void layout(boolean animate) {
			
			// Base size on scrollable's % visible
			// and position on scrollable's position visible
			if(vertical) {
				
				double percentVisible = getPercent(scrollable.getVerticalVisible(), scrollable.getVerticalSize());
				double positionVisible = getPercent(scrollable.getVerticalPosition(), scrollable.getVerticalSize());

				double newHeight = Math.max(UI.SCROLL_BAR_SIZE, ScrollBarView.this.getLocalHeight() * percentVisible);
				
				setLocalHeight(newHeight, animate);
				setLocalTop(ScrollBarView.this.getLocalHeight() * positionVisible, animate);
				
			}
			else {

				double percentVisible = getPercent(scrollable.getHorizontalVisible(), scrollable.getHorizontalSize());
				double positionVisible = getPercent(scrollable.getHorizontalPosition(), scrollable.getHorizontalSize());

				double newWidth = Math.max(UI.SCROLL_BAR_SIZE, ScrollBarView.this.getLocalWidth() * percentVisible);
				
				setLocalWidth(newWidth, animate);
				setLocalLeft(ScrollBarView.this.getLocalWidth() * positionVisible, animate);

			}
			
			if(animate) animate(UI.getDuration(), false);
			
		}
		
		private double getPercent(double visible, double total) {
			
			return Math.min(1.0, Math.max(0.0, visible / total));
			
		}
		
		public boolean handleMouseDrag(int localX, int localY, int mouseButton) { 
		
			if(vertical) {
				
				double newPosition = localY - getContainer().getMouseFocusY();
				double newPercent = newPosition / ScrollBarView.this.getLocalHeight();
				double newWindowPosition = newPercent * scrollable.getVerticalSize();
				double boundedPosition = boundVerticalBarPosition(newWindowPosition);
				
				scrollable.setVerticalPosition((int)boundedPosition);
				
			}
			else {
				
				double newPosition = localX - getContainer().getMouseFocusX();
				double newPercent = newPosition / ScrollBarView.this.getLocalWidth();
				double newWindowPosition = newPercent * scrollable.getHorizontalSize();
				double boundedPosition = boundHorizontalBarPosition(newWindowPosition);
				
				scrollable.setHorizontalPosition((int)boundedPosition);
				
			}

			layout(false);
			
			return true; 
			
		}

		public boolean handleMouseUp(int localX, int localY, int mouseButton) { 
		
			getContainer().releaseMouseFocus();
			return true; 
			
		}

		public boolean handleMouseDown(int localX, int localY, int mouseButton) { 
			
			getContainer().focusMouseOn(this);
			return true;
			
		}

		public void paintBelowChildren(Graphics2D g) {
			
			int x1 = (int)getVisibleLocalLeft(), y1 = (int)getVisibleLocalTop();
			int width = (int)getVisibleLocalWidth(), height = (int)getVisibleLocalHeight();
			
			g = (Graphics2D)g.create();
			
        	g.setColor(UI.getControlFrontColor());
        	g.fillRoundRect(x1, y1, width, height, UI.getRoundedness(), UI.getRoundedness());

		}
		
	}
	
}

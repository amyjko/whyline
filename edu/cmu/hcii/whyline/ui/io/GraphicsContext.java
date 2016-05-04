/**
 * 
 */
package edu.cmu.hcii.whyline.ui.io;

import java.awt.Graphics2D;
import java.awt.Rectangle;

import edu.cmu.hcii.whyline.io.*;

/**
 * @author Andrew J. Ko
 *
 */
public class GraphicsContext {
	
	private final WindowState windowState;
	private final Graphics2D context;
	private int originX = 0, originY = 0;
	private final Rectangle clip;
	private SetPaintEvent latestPaintChange;
	private SetFontEvent latestFontChange;
	private SetStrokeEvent latestStrokeChange;
	private ModifyTransformEvent latestTransformChange;
	private SetCompositeEvent latestCompositeChange;
	private ModifyClipEvent latestClipChange;
	private SetBackgroundEvent latestBackgroundChange;

	private final boolean representsWindow;
	
	public GraphicsContext(WindowState windowState, Graphics2D context, int originX, int originY, boolean representsWindow) {
		
		this.windowState = windowState;
		this.context = context;
		this.originX = originX;
		this.originY = originY;
		this.clip = context.getClipBounds() == null ? null : new Rectangle(context.getClipBounds());

		this.representsWindow = representsWindow;
		
	}
	
	public GraphicsContext(GraphicsContext contextToCopy) {

		windowState = contextToCopy.windowState;
		context = (Graphics2D)contextToCopy.context.create();
		originX = contextToCopy.originX;
		originY = contextToCopy.originY;
		clip = contextToCopy.clip;
		latestPaintChange = contextToCopy.latestPaintChange;
		latestFontChange = contextToCopy.latestFontChange;
		latestStrokeChange = contextToCopy.latestStrokeChange;
		latestTransformChange = contextToCopy.latestTransformChange;
		latestCompositeChange = contextToCopy.latestCompositeChange;
		latestClipChange = contextToCopy.latestClipChange;
		latestBackgroundChange = contextToCopy.latestBackgroundChange;
		representsWindow = contextToCopy.representsWindow;
		
	}
	
	public boolean representsWindow() { return representsWindow; }
	
	public WindowState getWindowState() { return windowState; }
	
	public Graphics2D getGraphics() { return context; }
	
	public int getOriginX() { return originX; }
	public int getOriginY() { return originY; }
	
	public Rectangle getClipBounds() { return clip; }
	
	public ModifyClipEvent getLatestClipChange() { return latestClipChange; }
	public void setLatestClipChange(ModifyClipEvent latestClipChange) { this.latestClipChange = latestClipChange; }

	public SetCompositeEvent getLatestCompositeChange() { return latestCompositeChange; }
	public void setLatestCompositeChange(SetCompositeEvent latestCompositeChange) { this.latestCompositeChange = latestCompositeChange; }

	public SetFontEvent getLatestFontChange() { return latestFontChange; }
	public void setLatestFontChange(SetFontEvent latestFontChange) { this.latestFontChange = latestFontChange; }

	public SetPaintEvent getLatestPaintChange() { return latestPaintChange; }
	public void setLatestPaintChange(SetPaintEvent latestPaintChange) { this.latestPaintChange = latestPaintChange; }

	public SetStrokeEvent getLatestStrokeChange() { return latestStrokeChange; }
	public void setLatestStrokeChange(SetStrokeEvent latestStrokeChange) { this.latestStrokeChange = latestStrokeChange; }

	public ModifyTransformEvent getLatestTransformChange() { return latestTransformChange; }
	public void setLatestTransformChange(ModifyTransformEvent latestTransformChange) { 
		
		if(latestTransformChange instanceof TranslateEvent) {
			originX += ((TranslateEvent)latestTransformChange).getTranslateX();
			originY += ((TranslateEvent)latestTransformChange).getTranslateY();
		}
		this.latestTransformChange = latestTransformChange; 
		
	}

	public SetBackgroundEvent getLatestBackgroundChange() { return latestBackgroundChange; }
	public void setLatestBackgroundChange(SetBackgroundEvent latestBackgroundChange) { this.latestBackgroundChange = latestBackgroundChange; }

}
package edu.cmu.hcii.whyline.ui.source;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;

import java.util.*;

import edu.cmu.hcii.whyline.analysis.SearchResultsInterface;
import edu.cmu.hcii.whyline.source.*;
import edu.cmu.hcii.whyline.ui.UI;
import edu.cmu.hcii.whyline.ui.views.*;

/**
 * @author Andrew J. Ko
 *
 */
public class FileWindow extends View implements Comparable<FileWindow>, ScrollableView {
	
	private final FilesView filesView;
	private final FileInterface file;

	private final FileView fileView;
	private final Window window;
	private final ScrollBarView vertical, horizontal;
	
	private final GlyphVector filename;
	private final Rectangle2D filenameBounds;
	private final int filenameDescent;
	
	private final double headerHeight;
	
	public FileWindow(FilesView filesView, ViewContainer container, FileInterface file) {

		this.filesView = filesView;
		this.file = file;

		Graphics2D g = (Graphics2D)filesView.getWhylineUI().getGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		Font font = UI.getMediumFont().deriveFont(Font.BOLD);
		filename = font.createGlyphVector(g.getFontRenderContext(), file.getShortFileName());
		filenameBounds = filename.getLogicalBounds();
		filenameDescent = g.getFontMetrics(font).getDescent();
		
		vertical = new ScrollBarView(this, true);
		horizontal = new ScrollBarView(this, false);
		
		fileView = new FileView(filesView.getWhylineUI(), file);
		window = new Window(fileView);

		headerHeight = fileView.getLineHeight() + UI.getBorderPadding() * 2;
		
		// We only set these ones, since these are the scroll view values.
		fileView.setLocalLeft(0, false);
		fileView.setLocalTop(0, false);
		
		setClipsChildren(true);
		
		addChild(window);
		addChild(vertical);
		addChild(horizontal);

	}
	
	public int getVerticalVisible() { return window == null ? 0 : (int)window.getLocalHeight(); }
	public int getVerticalPosition() { return window == null ? 0 : -(int)fileView.getLocalTop(); }
	public int getVerticalSize() { return window == null ? 0 : (int)fileView.getLocalHeight(); }

	public int getHorizontalVisible() { return window == null ? 0 : (int)window.getLocalWidth(); }
	public int getHorizontalPosition() { return window == null ? 0 : -(int)fileView.getLocalLeft(); }
	public int getHorizontalSize() { return window == null ? 0 : (int)fileView.getLocalWidth(); }
	
	public void setHorizontalPosition(int x) {
		
		fileView.setLocalLeft(-x, false);
		repaint();
		
	}
	
	public void setVerticalPosition(int y) {

		fileView.setLocalTop(-y, false);
		repaint();
		
	}
	
	public FileView getFileView() {  return fileView; }
		
	public void layout(double left, double top, double width, double height, boolean animate) {
		
		setLocalLeft(left, animate);
		setLocalTop(top, animate);
		setLocalWidth(width, animate);
		setLocalHeight(height, animate);

		if(window != null) {
			
			window.setLocalLeft(0, animate);
			window.setLocalTop(headerHeight, animate);
			window.setLocalWidth(getLocalWidth() - vertical.getLocalWidth(), animate);
			window.setLocalHeight(getLocalHeight() - headerHeight - horizontal.getLocalHeight(), animate);
		
			horizontal.setLocalLeft(0, true);
			horizontal.setLocalTop(window.getLocalBottom(), animate);
			horizontal.setLocalWidth(window.getLocalWidth(), animate);
			
			vertical.setLocalLeft(window.getLocalRight(), animate);
			vertical.setLocalTop(headerHeight, animate);
			vertical.setLocalHeight(window.getLocalHeight(), animate);
			
			horizontal.layout(animate);
			vertical.layout(animate);
			
		}
		
		if(animate) {
			// We do these all separately to avoid animating the source file in the window.
			animate(UI.getDuration(), false);
			window.animate(UI.getDuration(), false);
			horizontal.animate(UI.getDuration(), true);
			vertical.animate(UI.getDuration(), true);
		}
		
	}
	
	public void scrollToToken(TokenView view) {

		LineView lineView = view.getLineView();
		scrollToBoundary(lineView.getLocalTop(), lineView.getLocalBottom(), view.getLocalLeft(), view.getLocalRight());
		
	}
	
	public void scrollToLine(LineNumber lineNumber) {
	
		LineView line = fileView.getLine(lineNumber);
		if(line != null)
			scrollToBoundary(line.getLocalTop(), line.getLocalBottom(), 0, 0);
		
	}
	
	public void scrollToBoundary(double top, double bottom, double left, double right) {

		if(window == null) return;
		
		double verticalPosition = (top + bottom) / 2;

		double fileOffset = verticalPosition - window.getLocalHeight() / 2;
		fileOffset = Math.min(fileView.getLocalHeight() - window.getLocalHeight(), fileOffset);
		fileOffset = Math.max(0, fileOffset);

		fileView.setLocalTop(-fileOffset, true);
		fileView.animate(UI.getDuration(), false);
		
		vertical.layout(true);

		
		double horizontalPosition = (left + right) / 2;

		fileOffset = horizontalPosition - window.getLocalWidth() / 2;
		fileOffset = Math.min(fileView.getLocalWidth() - window.getLocalWidth(), fileOffset);
		fileOffset = Math.max(0, fileOffset);

		fileView.setLocalLeft(-fileOffset, true);
		fileView.animate(UI.getDuration(), false);

		horizontal.layout(true);
		
	}
	
	public void paintBelowChildren(Graphics2D g) {

		// Draw the background of the source file window.
		g.setColor(UI.getFileColor());
		g.fillRoundRect((int)getVisibleLocalLeft(), (int)(getVisibleLocalTop() + headerHeight), (int)getVisibleLocalWidth(), (int)(getVisibleLocalHeight() - headerHeight), UI.getRoundedness(), UI.getRoundedness()); 
		
	}
		
	public void paintAboveChildren(Graphics2D g) {
		
		// Draw a border around the edge of the window.
		g.setColor(UI.getControlBorderColor());
		g.drawRoundRect((int)getVisibleLocalLeft(), (int)(getVisibleLocalTop() + headerHeight), (int)getVisibleLocalWidth(), (int)(getVisibleLocalHeight() - headerHeight), UI.getRoundedness(), UI.getRoundedness()); 

		// Draw boxes over the scroll bar to represent highlighted lines. 
		if(window == null || fileView == null)
			return;

		drawMarginHighlights(g);

		drawFileName(g);
		
	}
	
	private void drawMarginHighlights(Graphics2D g) {

		// Collect all of the highlighted lines.
		Map<LineView, Set<Color>> lines = new HashMap<LineView,Set<Color>>();

		// Go through emphasized tokens and mark them.
		for(TokenView t : fileView.getEmphasizedTokens()) {
			Color colorToAdd = fileView.getEmphasizedTokenColor(t);
			Set<Color> colors = lines.get(t.getLineView());
			if(colors == null) {
				colors = new HashSet<Color>();
				lines.put(t.getLineView(), colors);
			}
			colors.add(colorToAdd);
		}

		// Go through any token results in the currently visible results set.
		SearchResultsInterface results = filesView.getWhylineUI().getLinesUI().getSelectedResults();
		if(results != null) {
			for(Token t : results.getResults()) {
				
				LineView line = fileView.getViewOf(t.getLine());
				if(line != null) {
					Set<Color> colors = lines.get(line);
					if(colors == null) {
						colors = new HashSet<Color>();
						lines.put(line, colors);
					}
					colors.add(UI.getHighlightColor());
				}
			}
		}				
		
		int right = (int)(vertical.getLocalLeft() + getLocalLeft());
		int size = (int)vertical.getLocalWidth(); 

		// Find a point in the window's margin to represent each highlighted line
		for(LineView line : lines.keySet()) {

			double y = getVerticalPositionOfLine(line);
			
			Set<Color> colors = lines.get(line);
			int r = right;
			for(Color color : colors) {
			
				g.setColor(color);
				g.fillRect(r, (int)y, size / colors.size(), size / 2);
				g.setColor(Color.black);
				g.drawRect(r, (int)y, size / colors.size(), size / 2);
				r += size / colors.size();
				
			}
			
		}

	}
	
	private void drawFileName(Graphics2D g) {
		
		// Draw the name of the source file.
		g = (Graphics2D)g.create();
		int x = (int)(getLocalLeft() + window.getLocalRight() - filenameBounds.getWidth()) -2;
		int y = (int)(getLocalTop() + filenameDescent) - 1; 
		g.clipRect(x - UI.getPanelPadding(), y - UI.getPanelPadding() , (int)filenameBounds.getWidth() + UI.getPanelPadding() * 2, (int)filenameBounds.getHeight() + UI.getPanelPadding() * 2 - filenameDescent - 1);
		g.setColor(UI.getPanelDarkColor());
		g.fillRoundRect(
				x - UI.getPanelPadding(), 
				y, 
				(int)filenameBounds.getWidth() + UI.getPanelPadding() + UI.getRoundedness(), 
				(int)filenameBounds.getHeight() +  UI.getPanelPadding() * 2, UI.getRoundedness(), UI.getRoundedness());
		g.setColor(UI.getControlBorderColor());
		g.drawRoundRect(
				x - UI.getPanelPadding(), 
				y, 
				(int)filenameBounds.getWidth() + UI.getPanelPadding() + UI.getRoundedness(), 
				(int)filenameBounds.getHeight() +  UI.getPanelPadding() * 2, UI.getRoundedness(), UI.getRoundedness());
		g.setColor(UI.getPanelTextColor());
		g.drawGlyphVector(filename, x, (float) (y + filenameBounds.getHeight()));

	}	
	
	private double getVerticalPositionOfLine(LineView line) {

		double percent = line.getLocalTop() / fileView.getLocalHeight();
		return percent * getLocalHeight() + getLocalTop();
		
	}

	public void handleMouseExit() {
		
		setClipsChildren(true);
		
	}
	
	private class Window extends View {
		
		public Window(FileView view) {
			
			setClipsChildren(true);
			
			addChild(view);
			
		}
		
		public boolean handleWheelMove(int units) {

			double newPosition = -fileView.getLocalTop() + fileView.getLineHeight() * units;
			double boundedPosition = Math.max(0.0, Math.min(getVerticalSize() - getVerticalVisible(), newPosition));
			
			fileView.setLocalTop(-boundedPosition, false);
			vertical.layout(false);
			repaint();
			
			return true;
			
		}
		
	}

	public int compareTo(FileWindow o) {
		
		return file.getFileName().compareTo(o.file.getFileName());

	}
	
	public String toString() { return "Window of " + fileView.getFile().getFileName(); }

	public double getHeaderHeight() { return headerHeight; }

	public double getScrollBarMargin() { return vertical.getLocalWidth(); }
		
}
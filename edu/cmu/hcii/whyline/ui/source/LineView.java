
package edu.cmu.hcii.whyline.ui.source;

import java.awt.*;
import java.awt.font.*;

import edu.cmu.hcii.whyline.bytecode.Instruction;
import edu.cmu.hcii.whyline.qa.SourceFileMenuFactory;
import edu.cmu.hcii.whyline.source.Line;
import edu.cmu.hcii.whyline.source.Token;
import edu.cmu.hcii.whyline.ui.*;
import edu.cmu.hcii.whyline.ui.components.WhylinePopup;
import edu.cmu.hcii.whyline.ui.io.BreakpointDebugger;
import edu.cmu.hcii.whyline.ui.views.View;

/**
 * @author Andrew J. Ko
 *
 */
public final class LineView extends View {

	private static final boolean DRAW_LINE_NUMBERS = true; 
	private static final AlphaComposite FADE = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
	private static final AlphaComposite NORMAL = AlphaComposite.getInstance(AlphaComposite.SRC);
	
	private final FileView fileView;

	private final Line line;

	private int leftOfFirstNonWhitespaceToken = Integer.MIN_VALUE;
	
	public LineView(FileView fileView, Line line) {

		this.line = line;
		this.fileView = fileView;

		setLocalHeight(fileView.getLineHeight(), false);
		setLocalWidth(fileView.getWidthOfCharacter() * line.getNumberOfCharacters(), false);

		WhylineUI whylineUI = this.fileView.getWhylineUI();
		
		int charWidth = fileView.getWidthOfCharacter();
		int rightEdge = fileView.getWidthOfCharacter() * 5;
		for(Token token : line.getTokens()) {

			if(token.isWhitespace()) {
				
				String text = token.getText();
				int length = text.length();
				int numberOfCharacters = 0;
				for(int i = 0; i < length; i++)
					numberOfCharacters += text.charAt(i) == '\t' ? UI.TAB_SPACES.length() : 1;

					int width = charWidth * numberOfCharacters;
					rightEdge += width;

			}
			else {
			
				GlyphVector glyphs = token.isWhitespace() ? null : whylineUI.getTokenGlyphs(token);
				
				TokenView newTokenView = new TokenView(this, token, glyphs); 
	
				if(token.isCode() && leftOfFirstNonWhitespaceToken < 0)
					leftOfFirstNonWhitespaceToken = rightEdge;
				
				newTokenView.setLocalLeft(rightEdge, false);
				newTokenView.setLocalTop(0, false);
				
				int width = charWidth * glyphs.getNumGlyphs();

				// We assume its fixed width to speed up calculations.
				newTokenView.setLocalWidth(width, false);
				newTokenView.setLocalHeight((int) getLocalHeight(), false);
	
				addChild(newTokenView);

				rightEdge += width;

			}
		
		}

		setLocalWidth(rightEdge, false);

		trim();

	}
	
	public TokenView getViewOf(Token token) {
		
		for(View child : getChildren()) 
			if(child instanceof TokenView && ((TokenView)child).getToken() == token)
				return (TokenView)child;
		return null;
		
	}
	
	public double getAscent() { return fileView.getAscent(); }
	
	public FileView getFileView() { return fileView; }
	
	public WhylineUI getWhylineUI() { return fileView.getWhylineUI(); } 

	public Instruction getFirstInstrumentedInstructionOnLine() { return fileView.getFirstInstrumentedInstructionOnLine(this); }
	
	public boolean handleMouseClick(int x, int y, int button) {
		
		WhylineUI whylineUI = getFileView().getWhylineUI();
		int breakpointLeft = getBreakpointLeft();
		if(whylineUI.getMode() == WhylineUI.Mode.BREAKPOINT && x < breakpointLeft + getBreakpointDiameter()) {
			
			final Line nearestLine = whylineUI.getBreakpointDebugger().getNearestBreakpointLine(line);
			if(nearestLine != null) {
				whylineUI.getBreakpointDebugger().toggleBreakpoint(nearestLine);
			}
			else
				Toolkit.getDefaultToolkit().beep();
			
			return true;
			
		}
		else {

			if(getFileView().getHoveredLine() != this) return false;

			int menuX = (int)getGlobalLeft() + (int)(x - getLocalLeft());
			int menuY = (int)getGlobalTop() + (int)(y - getLocalTop());
	
			WhylinePopup popup = new WhylinePopup("");
			
			SourceFileMenuFactory.addQuestionsForLine(getWhylineUI(), popup, line);
			SourceFileMenuFactory.addQuestionsForMethod(getWhylineUI(), popup, line);
	
			return getContainer().showPopup(popup, menuX, menuY);
			
		}

	}
	
	public void setEmphasized(boolean highlighted) {
		
		fileView.emphasizeLine(this, highlighted);
		repaint();
		
	}
	
	public int getLeftAfterWhitespace() { return leftOfFirstNonWhitespaceToken; }
	
	public Line getLine() { return line; }	

	public void paintChildren(Graphics2D g) {
		
		AlphaComposite fade = getWhylineUI().getFilesView().getCurrentFade();
		
		if(fade != null) {
		
			boolean lineEmphasized = fileView.isLineEmphasized(this) || fileView.getHoveredLine() == this || fileView.doesHoveredMethodContain(this);
			boolean nothingEmphasized = fileView.isNothingEmphasized();
			boolean childSelected = false;
			boolean childEmphasized = false;
			TokenView selectedToken = fileView.getTokenUnderMouse(); 
			for(View v : getChildren()) {
				if(selectedToken == v)
					childSelected = true;
				if(fileView.isTokenEmphasized((TokenView)v))
					childEmphasized = true;
			}		
			
			boolean normal = childSelected || childEmphasized || lineEmphasized || nothingEmphasized;
			g.setComposite(normal ? NORMAL : fade);
			
		}

		super.paintChildren(g);

		g.setComposite(NORMAL);
		
	}
	
	private int getBreakpointLeft() { return fileView.getWidthOfCharacter() * (DRAW_LINE_NUMBERS ? (int)Math.log10(line.getLineNumber().getNumber()) + 3 : 1); }
	private int getBreakpointDiameter() { return (int)getLocalHeight() - 2; }
	
	public void paintBelowChildren(Graphics2D g) {

		WhylineUI whylineUI = getWhylineUI();
		
		if(whylineUI.getLinesUI().selectedTabContains(line)) {
			g.setColor(UI.getHighlightColor());
			if(whylineUI.getLinesUI().getSelectedLine() == line)
				g.fillRoundRect(leftOfFirstNonWhitespaceToken, (int)getVisibleLocalTop(), (int)getLocalWidth(), (int)getLocalHeight(), UI.getRoundedness(), UI.getRoundedness());
			else
				g.drawRoundRect(leftOfFirstNonWhitespaceToken, (int)getVisibleLocalTop(), (int)getLocalWidth(), (int)getLocalHeight(), UI.getRoundedness(), UI.getRoundedness());
		}
		
		if(whylineUI.getMode() == WhylineUI.Mode.BREAKPOINT) {

			BreakpointDebugger debugger = whylineUI.getBreakpointDebugger();

			boolean isExecuting = debugger.isRunning() && debugger.getCurrentLine() == line; 
			
			if(isExecuting) {

				g.setColor(UI.getRunningColor());
				g.fillRoundRect(0, (int)getVisibleLocalTop(), (int)getParent().getParent().getLocalWidth(), (int)getVisibleLocalHeight() + 1, UI.getRoundedness(), UI.getRoundedness());
				
			}

			// Put this after the line numbers, if there are any.
			int breakpointLeft = getBreakpointLeft();

			boolean isLineOver = getFileView().getLineOver() == line;
			boolean breakpointSet = debugger.hasBreakpoint(line); 

			// Draw a circle if the breakpoint is set, or this is the line we're over and it's breakable.
			if(breakpointSet || (isLineOver && debugger.getNearestBreakpointLine(line) == line)) {

				int size = getBreakpointDiameter();
				g.setColor(UI.BREAKPOINT_COLOR);
				int y = (int)(getVisibleLocalTop() + 1);
				if(breakpointSet)
					g.fillOval(breakpointLeft, y, size, size);
				g.setColor(Color.black);
				g.drawOval(breakpointLeft, y, size - 1, size - 1);
			}
						
		}

	}
	
	public void paintAboveChildren(Graphics2D g) {

		// Paint the line number
		if(DRAW_LINE_NUMBERS) {
			g.setColor(UI.getControlBorderColor());
			g.setFont(UI.getFixedFont());
			g.drawString(Integer.toString(line.getLineNumber().getNumber()), fileView.getWidthOfCharacter(), (int)(getVisibleLocalTop() + getAscent() + 1));
		}

		if(fileView.getHoveredLine() == this) {
			
			int padding = 2;
			
			g.setStroke(UI.SELECTED_STROKE);
			g.setColor(UI.getHighlightColor());
			g.drawRoundRect(
					(int)(leftOfFirstNonWhitespaceToken + getVisibleLocalLeft()) - padding, (int)getVisibleLocalTop() - padding, 
					(int)(getVisibleLocalWidth() - leftOfFirstNonWhitespaceToken) + padding * 2 - 1, (int)getVisibleLocalHeight() + padding * 2 - 1, 
					5, 5);
			
		}
		
	}
	
}
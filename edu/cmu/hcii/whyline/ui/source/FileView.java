package edu.cmu.hcii.whyline.ui.source;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;

import edu.cmu.hcii.whyline.bytecode.*;
import edu.cmu.hcii.whyline.qa.SourceFileMenuFactory;
import edu.cmu.hcii.whyline.source.*;
import edu.cmu.hcii.whyline.ui.*;
import edu.cmu.hcii.whyline.ui.arrows.CausalArrowView;
import edu.cmu.hcii.whyline.ui.components.WhylinePopup;
import edu.cmu.hcii.whyline.ui.events.LineHover;
import edu.cmu.hcii.whyline.ui.events.NoLineHover;
import edu.cmu.hcii.whyline.ui.views.View;
import edu.cmu.hcii.whyline.util.Util;

/**
 * @author Andrew J. Ko
 *
 */
public final class FileView extends View {
	
	private static final int LEFT_MARGIN_SPACES = 3;
	
	private final WhylineUI whylineUI;

	private final FontMetrics metrics;
	private final int margin;
	private final int ascent, descent;
	private final int characterWidth;
	private final int lineHeight;

	private final FileInterface file;

	private final ArrayList<LineView> lines;
	
	private Hashtable<LineView, Instruction> firstInstrumentedInstructionOnLine = new Hashtable<LineView, Instruction>();
	
	private final Vector<MethodInfo> methodsToEmphasize = new Vector<MethodInfo>();	
	
	public int getWidthOfCharacter() { return characterWidth; }
	
	public int getLineHeight() { return lineHeight; }
	
	private View hoveredView;
	private MethodInfo hoveredMethod;
	private MethodInfo methodMouseIsOver;
	private LineSpan hoveredMethodSpan;
	private Line lineOver = null;
	
	private final Set<LineView> emphasizedLines = new HashSet<LineView>();
	private final Map<TokenView,Color> emphasizedTokens = new HashMap<TokenView,Color>();
		
	public FileView(WhylineUI whylineUI, FileInterface source) {
		
		this.whylineUI = whylineUI;
		this.file = source;

		Graphics2D g = (Graphics2D)whylineUI.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		assert g != null : "Why is whylineUI.getGraphics() " + whylineUI.getGraphics() + "? WhylineUI = " + whylineUI + " and isDisplayable = " + whylineUI.isDisplayable(); 
		
		metrics = g.getFontMetrics(UI.getFixedFont());

		ascent = metrics.getMaxAscent();
		descent = metrics.getMaxDescent();
		lineHeight = metrics.getHeight();

		characterWidth = metrics.charWidth('e');
		
		margin = characterWidth * LEFT_MARGIN_SPACES;
		
		lines = new ArrayList<LineView>();
		try {
		
			lines.ensureCapacity(source.getNumberOfLines());
			// Add line views for every line.
			if(source.getLines() != null) {
				for(Line line : source.getLines()) {
		
					LineView view = new LineView(this, line);
					addChild(view);
					lines.add(view);
					
				}
			}

		} catch(ParseException e) {
			e.printStackTrace();
		}

		trim();
			
		// Layout the children
		layoutLines();
		
	}

	public FontMetrics getFontMetrics() { return metrics; }
	
	public int getCharacterWidth() { return characterWidth; }
	
	public Set<TokenView> getEmphasizedTokens() { return emphasizedTokens.keySet(); }
	
	public void emphasizeLine(LineView line, boolean yes) { 
		
		if(yes) emphasizedLines.add(line); 
		else emphasizedLines.remove(line);
		
	}

	public void emphasizeToken(TokenView token, Color color) { 

		if(color == null) emphasizedTokens.remove(token);
		else emphasizedTokens.put(token, color); 
		
	}

	public boolean isLineEmphasized(LineView line) { return emphasizedLines.contains(line); }
	public boolean isTokenEmphasized(TokenView token) { return emphasizedTokens.containsKey(token); }
	public boolean isNothingEmphasized() { return emphasizedTokens.isEmpty() && emphasizedLines.isEmpty(); }
	
	public Color getEmphasizedTokenColor(TokenView token) { return emphasizedTokens.get(token); }

	public FileInterface getFile() { return file; }

	public WhylineUI getWhylineUI() { return whylineUI; }

	public double getBaseline() { return metrics.getAscent(); }
	
	public double getAscent() { return ascent; }
	
	private void setHoveredView(View view) {
		
		hoveredView = view;
		hoveredMethod = null;
		hoveredMethodSpan = null;
		methodMouseIsOver = null;
		repaint();
		
	}
	
	/**
	 * Based strictly on vertical position and not on the whitespace to the left of each line.
	 */
	public Line getLineOver() { return lineOver; }
	
	public LineView getHoveredLine() { return hoveredView instanceof LineView ? (LineView)hoveredView : null; }

	public TokenView getTokenUnderMouse() { return hoveredView instanceof TokenView ? (TokenView)hoveredView : null; }

	public MethodInfo getHoveredMethod() { return hoveredMethod; }
	
	public MethodInfo getMethodMouseIsOver() { return methodMouseIsOver; }
	
	public boolean doesHoveredMethodContain(LineView line) {
		
		if(hoveredMethodSpan == null) return false;
		return hoveredMethodSpan.contains(line);
		
	}
	
	public void emphasizeMethod(MethodInfo method) {

		if(file instanceof JavaSourceFile)
			assert method.getClassfile().getSourceFile() == file : "But " + file + " isn't the source file for " + method.getQualifiedNameAndDescriptor();
		
		methodsToEmphasize.add(method);
		
	}

	public void layoutLines() {

		int maxRight, maxBottom;

		Graphics2D g = (Graphics2D)whylineUI.getGraphics();

		int firstLineOfEllisionBlock = -1;
		
		maxRight = 0;
		int baseline = metrics.getMaxAscent() + margin;
		int lineNumber = 0;
		for(View view : getChildren()) {

			LineView line = (LineView)view;
			
			// Position the line properly.
			line.setLocalLeft(margin, false);
			line.setLocalTop(baseline - metrics.getMaxAscent(), false);
		
			// The next baseline is below this line.
			baseline += line.getLocalHeight();
			
			if(view.getLocalRight() > maxRight) maxRight = (int)view.getLocalRight() + margin;
			
			lineNumber++;
			
		}

		maxBottom = baseline + metrics.getMaxDescent() + margin;
		
		setLocalWidth(maxRight, false);
		setLocalHeight(maxBottom, false);

	}
	
	public void removeEmphasis() {
		
		methodsToEmphasize.removeAllElements();

		emphasizedLines.clear(); 
		emphasizedTokens.clear(); 
		repaint(); 
	
		repaint();
		
	}
	
	public LineView getLineViewOf(Instruction inst) {

		if(inst != null && inst.getLineNumber() != null)
			return (LineView)getChildAtIndex(inst.getLineNumber().getNumber() - 1);
		else
			return null;
		
	}

	public Instruction getFirstInstrumentedInstructionOnLine(LineView lineView) {
		
		Instruction inst = firstInstrumentedInstructionOnLine.get(lineView);
		if(inst == null) {
			
			SortedSet<Instruction> instructionsOnLine = lineView.getLine().getInstructions();
			if(instructionsOnLine == null) return null;
			for(Instruction candidate : instructionsOnLine)
				if(candidate instanceof PushConstant)
					continue;
				else {
					inst = candidate;
					break;
				}

			if(inst != null)
				firstInstrumentedInstructionOnLine.put(lineView, inst);
			
		}
		return inst;		
		
	}
				
	public LineView getViewOf(Line line) {
		
		if(line.getFile() != file) return null;
		else return lines.get(line.getLineNumber().getNumber() - 1);
		
	}
	
	public TokenView getViewOf(Token token) {

		LineView lineView = getLine(token.getLineNumber());
		
		do {

			for(View tokenView : lineView.getChildren())
				if(((TokenView)tokenView).getToken() == token)
					return (TokenView)tokenView;

			int nextLineNumber = lineView.getLine().getLineNumber().getNumber();
			lineView = nextLineNumber >= lines.size() ? null : lines.get(nextLineNumber);
			
		} while(lineView != null);
		
		return null;
		
	}
		
	public LineView getLine(LineNumber number) { 
		
		// Line numbers start at 1, the vector starts at 0.
		int index = number.getNumber() - 1;
		if(index < 0 || index >= lines.size())
			return null;
		else
			return lines.get(index); 
		
	}
	
	private static class LineSpan {
		
		public final LineView first;
		public final LineView last;
		private final int firstNumber, lastNumber;

		public LineSpan(LineView first, LineView last) {
			this.first = first;
			this.last = last;
			this.firstNumber = first.getLine().getLineNumber().getNumber();
			this.lastNumber = last.getLine().getLineNumber().getNumber();
		}

		public boolean contains(LineView line) {
			
			int number = line.getLine().getLineNumber().getNumber();
			return firstNumber <= number && number <= lastNumber;
			
		}
		
		public String toString() { return "" + firstNumber + " to "+ lastNumber; }
	
	}
	
	private SortedSet<LineSpan> spans = new TreeSet<LineSpan>(new Comparator<LineSpan>() {
		public int compare(LineSpan o1, LineSpan o2) {
			return o1.first.getLine().getLineNumber().getNumber() - o2.first.getLine().getLineNumber().getNumber();
		}
	});

	private LineSpan getMethodSpan(MethodInfo method) {
		
		if(!(file instanceof JavaSourceFile)) return null;
		
		Token first = ((JavaSourceFile)file).getFirstTokenOfMethodHeader(method);
		Token last = ((JavaSourceFile)file).getLastTokenOfMethod(method);
		
		if(first != null && last != null) {
			
			LineView firstView = lines.get(first.getLineNumber().getNumber() - 1);
			LineView lastView = lines.get(last.getLineNumber().getNumber() - 1);
			
			return new LineSpan(firstView, lastView);
			
		}
		
		return null;
		
	}

	private static Composite FADED_LINE_COMPOSITE = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f);
	private static int FADE_HEIGHT = 30;
	
	public final void paintChildren(Graphics2D g) {
		
		Rectangle clip = g.getClipBounds();
		
		g = (Graphics2D)g.create();
		
		// If the mouse is over a method, give a little feedback about its extent.
		if(hoveredMethodSpan != null) {

			g.setColor(UI.getHighlightColor());
			Paint oldPaint = g.getPaint();
			int width = characterWidth * 5;	// Max number of characters in a line number?
			g.setPaint(new GradientPaint(0, 0, UI.getHighlightColor(), width, 0, UI.getFileColor()));
			int top =  (int)hoveredMethodSpan.first.getLocalTop();
			int bottom =  (int)hoveredMethodSpan.last.getLocalTop();
			g.fillRect(2, top, width, bottom - top);
			g.setPaint(oldPaint);
			
		}

		// If a method is truncated of the top of the screen, redraw the header at the top of the screen.
		MethodInfo clippedMethod = getMethodAtVerticalPosition((int)-getLocalTop());
		if(clippedMethod != null && !clippedMethod.isSynthetic())
			drawClippedHeader(g, clippedMethod);
		
		// Don't draw all of the lines. Just the ones in view.
		int firstIndex = (int) Math.max(0, clip.getY() / getLineHeight() - 1); 
		int lastIndex = (int) Math.min(getNumberOfChildren() - 1, (clip.getY() + clip.getHeight()) / getLineHeight()); 

		// Give a little visual indicator of the file's familiarity.
		if(!file.isFamiliar()) {
			int spacing = UI.getCrosshatchSpacing() * 15;
			Util.drawCrosshatch(g, UI.FILE_FADED_COLOR, clip.x, clip.x + clip.width, clip.y, clip.height, spacing, clip.y % spacing);
		}

		for(int i = firstIndex; i <= lastIndex; i++)
			getChildAtIndex(i).paintBelowChildren(g);

		for(int i = firstIndex; i <= lastIndex; i++)
			getChildAtIndex(i).paint(g);

		for(int i = firstIndex; i <= lastIndex; i++)
			getChildAtIndex(i).paintAboveChildren(g);

	}

	private void drawClippedHeader(Graphics2D g, MethodInfo method) {
		
		Rectangle clip = g.getClipBounds();

		int topOfFrame = (int)-getLocalTop();
		
		LineSpan span = getMethodSpan(method);
		if(span != null && span.first.getLocalTop() < topOfFrame) {
			
			// Is the header partially in view? Make the header we're about to draw partially in view.
			int offset = 0;
			if(span.first.getLocalTop() < topOfFrame && span.first.getLocalBottom() > topOfFrame)
				offset = (int)(span.first.getLocalBottom() - topOfFrame) + 1;
			
			Graphics2D oldg = g;
			g = (Graphics2D)g.create();

			g.setClip(new Rectangle(clip.x + UI.getBorderPadding(), topOfFrame - lineHeight * 2, clip.width - UI.getBorderPadding(), lineHeight * 2));

			LineView methodHeader = span.first; 
			int left = (int) (methodHeader.getLocalLeft() + methodHeader.getLeftAfterWhitespace()) - UI.getPanelPadding();
			int top = topOfFrame - ascent - 2 + offset;
			int width = (int)methodHeader.getLocalRight() - left + UI.getPanelPadding() * 2;
			int height = (int)methodHeader.getLocalHeight();
			
			g.setColor(UI.getFileColor());
			g.fillRoundRect(left, top - UI.getBorderPadding() * 2, width, height + UI.getRoundedness() / 3 + lineHeight, UI.getRoundedness(), UI.getRoundedness());
			g.setColor(UI.getControlBorderColor());
			g.drawRoundRect(left, top - UI.getBorderPadding() * 2, width, height + UI.getRoundedness() / 3 + lineHeight, UI.getRoundedness(), UI.getRoundedness());
			
			int delta = (int) (topOfFrame - methodHeader.getLocalTop() - ascent - 2) - UI.getBorderPadding() + offset;

			g.translate(0, delta);
			methodHeader.paint(g);
			g.translate(0, -delta);

			g = oldg;
		}

	}
	
	public boolean handleMouseClick(int x, int y, int button) {

		int localY = (int) globalTopToLocal(y);
		
		Line line = getLineAtVerticalPosition(localY);
		
		WhylinePopup popup = new WhylinePopup("");

		SourceFileMenuFactory.addQuestionsForMethod(whylineUI, popup, line);
		
		return getContainer().showPopup(popup, (int)getGlobalLeft() + (int)(x - getLocalLeft()), (int)getGlobalTop() + (int)(y - getLocalTop()));
		
	}

	public void handleMouseNoLongerDirectlyOver(int x, int y) {
		
		setHoveredView(null);
		
	}

	public boolean handleMouseMove(int x, int y) {
		
		if(getWhylineUI().userIsAskingQuestion()) return false;
		
		setHoveredView(null);
		
		int fileY = (int)globalTopToLocal(y);
		
		methodMouseIsOver = getMethodAtVerticalPosition(fileY);

		java.util.List<View> views = getContainer().getViewsUnderMouse();
		
		// Log the line line under the mouse if it's changed.
		Line line = getLineAtVerticalPosition(y - (int)getLocalTop());
		if(line != null)  {
			if(line != lineOver) {
				lineOver = line;
				getWhylineUI().log(new LineHover(lineOver, UI.SOURCE_UI));
			}
		}
		// If this is newly null, log it.
		else {
			if(lineOver != null) {
				lineOver = null;
				getWhylineUI().log(new NoLineHover(UI.SOURCE_UI));
			}
		}			

		// Find the view directly under the mouse.
		for(View view : views) {

			if(view instanceof CausalArrowView) {
				
				return false;
				
			}
			// We allow interaction with a token if there's an associated instruction or class file.
			else if(view instanceof TokenView) {
				
				Token token = ((TokenView)view).getToken();
				Instruction code = file.getInstructionFor(token);
				QualifiedClassName classname = file.getClassnameFor(token);
				Parameter parameter = file.getMethodParameterFor(token);
				if(code != null || classname !=null || parameter != null) {

					setHoveredView(view);
					return true;
					
				}
				
			}
			else if(
				view instanceof LineView && 
				x > view.getLocalLeft() + ((LineView)view).getLeftAfterWhitespace() && 
				((LineView)view).getFirstInstrumentedInstructionOnLine() != null) {

				setHoveredView(view);
				return true;
			
			}
			else if(view == this) {
								
				if(methodMouseIsOver != null) {

					if(hoveredMethod != methodMouseIsOver) {
					
						hoveredMethod = methodMouseIsOver;
						hoveredMethodSpan = getMethodSpan(hoveredMethod);
						
					}
					
				}

				return false;
				
			}
			
		}

		return false;
		
	}
	
	public void handleMouseExit() {

		if(getWhylineUI().userIsAskingQuestion()) return;

		setHoveredView(null);

		getWhylineUI().log(new NoLineHover(UI.SOURCE_UI));			

	}
	
	public Line getLineAtVerticalPosition(int y) {
		
		Line line = null;
				
		for(View view : getChildren()) {
			
			if(view instanceof LineView && view.getLocalTop() <= y && view.getLocalBottom() >= y) {
				
				line = ((LineView)view).getLine();
				break;
				
			}
			
		}
		return line;
		
	}
	
	public MethodInfo getMethodAtVerticalPosition(int y) {

		if(!(file instanceof JavaSourceFile)) return null;
		Line line = getLineAtVerticalPosition(y);
		if(line != null) return ((JavaSourceFile)file).getMethodOfLine(line);
		else return null;
		
	}
	
	/**
	 * Returns a shape that outlines the range of tokens specified. Returns a shape in the coordinate system of the
	 * root container of this file view.
	 */
	public Area getTokenRangeOutline(TokenRange range) {

		int padding = (int) (characterWidth *.66);
		Area area = new Area();
		
		for(Token token : range) {
			
			LineView lineView = getLine(token.getLineNumber());			
			TokenView tokenView = lineView == null ? null : lineView.getViewOf(token);
			
			if(tokenView != null) {
				Rectangle2D bounds = tokenView.getGlobalBoundaries();
				RoundRectangle2D rect = 
					new RoundRectangle2D.Double(
						bounds.getX() - padding, bounds.getY() - padding, 
						bounds.getWidth() + padding * 2, bounds.getHeight() + padding * 2, 
						padding / 2, padding / 2);
				area.add(new Area(rect));
			}
			
		}
		return area;
		
	}
	
}
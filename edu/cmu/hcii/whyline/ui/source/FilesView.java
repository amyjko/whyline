package edu.cmu.hcii.whyline.ui.source;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.util.*;

import edu.cmu.hcii.whyline.bytecode.*;
import edu.cmu.hcii.whyline.qa.*;
import edu.cmu.hcii.whyline.source.*;
import edu.cmu.hcii.whyline.trace.*;
import edu.cmu.hcii.whyline.ui.*;
import edu.cmu.hcii.whyline.ui.arrows.*;
import edu.cmu.hcii.whyline.ui.components.SimpleHTML;
import edu.cmu.hcii.whyline.ui.qa.UnexecutedInstructionView;
import edu.cmu.hcii.whyline.ui.qa.Visualization;
import edu.cmu.hcii.whyline.ui.views.View;
import edu.cmu.hcii.whyline.util.IntegerVector;
import edu.cmu.hcii.whyline.util.Util;

/**
 * @author Andrew J. Ko
 *
 */
public final class FilesView extends View implements UserFocusListener {

	// Based on empirical data, windows are an average of 4 MB a piece. Give
	// views 10% of memory.
	private static int MAX_FILES_VIEWS_LOADED = (int) (Runtime.getRuntime()
			.maxMemory() / 10 / 1024 / 1024 / 4);

	/**
	 * The most recent files shown, used to maintain the file view cache.
	 */
	private final ArrayList<FileInterface> recentFiles = new ArrayList<FileInterface>();

	/**
	 * A cache of the most recent files viewed. We throw them away to save space
	 * once we have too many.
	 */
	private final TreeMap<FileInterface, FileWindow> windowsByFile = new TreeMap<FileInterface, FileWindow>();

	private Configuration selectionConfiguration = null;

	private SimpleHTML selectionExplanation;

	private final WhylineUI whylineUI;

	private final ArrowBox arrows;

	/**
	 * These are used to layout the floating dependency box within the view of a
	 * file.
	 */
	private double windowHeaderHeight, windowScrollBarMargin;

	public FilesView(WhylineUI whylineUI) {

		this.whylineUI = whylineUI;

		arrows = new ArrowBox();

		addChild(arrows);

	}

	public WhylineUI getWhylineUI() {
		return whylineUI;
	}

	public Trace getTrace() {
		return whylineUI.getTrace();
	}

	public ArrowView getSelectedFileArrow() {

		int arrowNumber = whylineUI.getArrowOver();
		if (arrowNumber >= 0 && arrowNumber <= arrows.getNumberOfChildren()) {
			View view = arrows.getChildAtIndex(arrowNumber);
			if (view instanceof ArrowView)
				return (ArrowView) view;
		}
		return null;

	}

	public void handleContainerResize() {

		if (selectionConfiguration != null)
			selectionConfiguration.arrange(false);

	}

	public void handleArrowOverChanged() {

		if (selectionConfiguration != null)
			selectionConfiguration.arrange(true);

	}

	public boolean handleMouseMove(int x, int y) {

		if (whylineUI.getVisualizationUIVisible() == null)
			return false;

		// If no arrows handled this, then no arrow is hovered.
		whylineUI.setArrowOver(-1);

		repaint();

		return false;

	}

	public void paintChildren(Graphics2D g) {

		// Arrows depend on window location and size, so we constantly
		// reposition the arrows.
		arrows.layoutArrows(true);

		super.paintChildren(g);

	}

	public void paintAboveChildren(Graphics2D g) {

		g = (Graphics2D) g.create();

		if (selectionConfiguration == null)
			return;

		// Draw some X's over the causes.
		if (selectionConfiguration.focus instanceof UnexecutedInstruction) {

			UnexecutedInstruction inst = (UnexecutedInstruction) selectionConfiguration.focus;

			LineView line = getLineViewOfInstruction(inst.getInstruction());

			if (line == null)
				return;

			int xWidth = (int) UnexecutedInstructionView.XMARK_BOUNDS.getWidth();
			int xLeft = (int) (line.getVisibleGlobalLeft() + line.getLeftAfterWhitespace() - xWidth);
			int xBottom = (int) (line.getVisibleGlobalTop() - (UnexecutedInstructionView.XMARK_BOUNDS.getHeight() - line.getVisibleGlobalHeight()) / 2 + UnexecutedInstructionView.GLYPH_ASCENT);

			if (inst.getReason() == UnexecutedInstruction.Reason.DID_EXECUTE) {
				g.setColor(UI.CORRECT_COLOR);
				g.drawGlyphVector(UnexecutedInstructionView.CHECKMARK, xLeft, xBottom);
			} else {
				g.setColor(UI.ERROR_COLOR);
				g.drawGlyphVector(UnexecutedInstructionView.XMARK, xLeft, xBottom);
			}

		}

	}

	public void removeWindowsArrowsAndHighlights() {

		// Remove any emphasis on file windows.
		Iterator<View> children = getChildren().iterator();
		while (children.hasNext()) {
			View child = children.next();
			if (child instanceof FileWindow) {
				if (((FileWindow) child).getFileView() != null)
					((FileWindow) child).getFileView().removeEmphasis();
			}
		}

		selectionExplanation = null;

		arrows.clear();

		removeWindows();

	}
	
	private void removeWindows() {
		
		removeChildren();

		// The arrows should always be part of the display, even if no files are showing.
		addChild(arrows);
		
	}

	private FileInterface getFileFor(Instruction inst) {

		if (inst == null)
			return null;
		Classfile classfile = inst.getCode().getMethod().getClassfile();
		FileInterface source = classfile;
		if (classfile.getSourceFile() != null)
			source = classfile.getSourceFile();
		return source;

	}

	public FileView getViewOf(FileInterface source) {
		return getWindowViewOf(source).getFileView();
	}

	public FileWindow getWindowViewOf(FileInterface source) {

		if (source == null)
			return null;

		FileWindow fileView = windowsByFile.get(source);
		if (fileView == null) {

			fileView = new FileWindow(this, getContainer(), source);
			windowsByFile.put(source, fileView);

		}

		// Keep the most recently accessed files at the end of the list.
		if (recentFiles.contains(source))
			recentFiles.remove(source);
		recentFiles.add(source);

		// If the list gets longer than a certain length, remove other files.
		if (recentFiles.size() > MAX_FILES_VIEWS_LOADED) {

			FileInterface file = recentFiles.remove(0);
			if (file != source) {
				FileWindow window = windowsByFile.get(file);
				windowsByFile.remove(file);
				removeChild(window);
			}

		}

		return fileView;

	}

	public FileView getFileViewOfInstruction(Instruction inst) {

		FileView fileView = getWindowViewOf(getFileFor(inst)).getFileView();
		return fileView;

	}

	public LineView getLineViewOfInstruction(Instruction instruction) {

		if (instruction == null)
			return null;
		return getFileViewOfInstruction(instruction).getLineViewOf(instruction);

	}

	private void placeWindow(FileWindow window, double left, double top,
			double width, double height, boolean animate) {

		double horizontalPadding = 0;// UIConstants.PANEL_PADDING;
		double verticalPadding = 0;// UIConstants.PANEL_PADDING;

		// If the left intrudes on the minimized file space, adjust it.
		if (left < 0) {
			double lostWidth = -left;
			left = 0;
			width -= lostWidth;
		}

		// If the top intrudes into the top border, adjust it.
		if (top < verticalPadding) {

			double lostHeight = verticalPadding - top;
			top = verticalPadding;
			height -= lostHeight;

		}

		// If the bottom intrudes the explanation space, adjust it.

		double verticalBoundary = getLocalHeight() - 1;

		if (top + height > verticalBoundary) {

			height -= (top + height) - verticalBoundary;

		}

		// If the right intrudes on the right edge, adjust it.
		if (left + width > getLocalWidth() - horizontalPadding) {

			double lostWidth = (left + width) - getLocalWidth();
			width -= lostWidth;

		}

		left += horizontalPadding;
		top += 0;
		width -= horizontalPadding * 2 + 1;
		height -= verticalPadding;

		window.layout(left, top, width, height, animate);

	}

	private static void addInstructionToWindowSet(
			Hashtable<FileWindow, Set<Instruction>> table, FileWindow window,
			Instruction instruction) {

		Set<Instruction> instructionsToShow = table.get(window);
		if (instructionsToShow == null) {

			instructionsToShow = new HashSet<Instruction>();
			table.put(window, instructionsToShow);

		}
		instructionsToShow.add(instruction);

	}

	private void emphasizeMethod(MethodInfo method) {

		if (method == null)
			return;
		JavaSourceFile source = method.getClassfile().getSourceFile();
		if (source != null)
			getWindowViewOf(source).getFileView().emphasizeMethod(method);

	}

	public void showInstruction(Instruction instruction) {

		removeWindowsArrowsAndHighlights();
		emphasizeMethod(instruction.getMethod());
		selectionConfiguration = new Configuration(instruction, instruction.getFile().getTokenRangeFor(instruction));
		selectionConfiguration.arrange(true);

		getWhylineUI().getOutlineUI().showMethod(instruction.getMethod());

	}

	public void showInstructions(Iterable<? extends Instruction> instructions) {

		removeWindowsArrowsAndHighlights();
		for (Instruction inst : instructions) {
			emphasizeMethod(inst.getMethod());
		}
		selectionConfiguration.arrange(true);

	}

	public void showEvent(int eventID) {

		if (eventID < 0 || eventID >= getTrace().getNumberOfEvents())
			return;

		removeWindowsArrowsAndHighlights();

		MethodInfo method = getTrace().getInstruction(eventID).getMethod();
		emphasizeMethod(method);
		selectionConfiguration = new Configuration(eventID, getRangeFor(eventID));
		selectionConfiguration.arrange(true);

		getWhylineUI().getOutlineUI().showMethod(method);

	}

	public void showExplanation(Explanation selection) {

		if (whylineUI.getVisualizationUIVisible() == null)
			return;

		boolean showSources = !whylineUI.getVisualizationUIVisible().getVisualization().isMetaDown();
		Answer answer = whylineUI.getVisualizationUIVisible().getAnswer();

		removeWindowsArrowsAndHighlights();

		selectionExplanation = new SimpleHTML(getTrace().getHTMLDescription(selection.getEventID()), (Graphics2D) whylineUI.getGraphics(),UI.getSmallFont());

		selection.explain();

		Instruction instruction = getTrace().getInstruction(selection.getEventID());
		EventKind kind = getTrace().getKind(selection.getEventID());

		emphasizeMethod(instruction.getMethod());

		int param = -1;
		if (kind.isArgument)
			param = getTrace().getArgumentLocalIDSet(selection.getEventID());

		selectionConfiguration = new Configuration(selection,getRangeFor(selection.getEventID()));

		int controlID = getTrace().getControlID(selection.getEventID());

		if (controlID >= 0)
			arrows.addArrow(new FileControlArrow(arrows, selection.getAnswer().getExplanationFor(controlID), selection));

		int dependencyNumber = 1;
		SortedMap<Explanation, Explanation> terminalCauses = selection.getAnswer().getTerminalDataDependencies(selection);
		if (terminalCauses != null) {

			for (Explanation cause : terminalCauses.keySet()) {

				Explanation effect = terminalCauses.get(cause);
				Explanation source = cause;

				if (showSources) {
					Explanation temp = answer.getSourceOfExplanationsValue(source);
					if (temp != null)
						source = temp;
				}

				Instruction useInstruction = getTrace().getInstruction(source.getEventID());
				Instruction defInstruction = getTrace().getInstruction(cause.getEventID());

				emphasizeMethod(useInstruction.getMethod());
				emphasizeMethod(defInstruction.getMethod());

				// Definition is used to show a value for the label.
				arrows.addArrow(new FileDataArrow(arrows, cause, source, effect, dependencyNumber));

				dependencyNumber++;

			}

		}

		selectionConfiguration.arrange(true);

		getWhylineUI().getOutlineUI().showMethod(instruction.getMethod());

	}

	public void showMethod(MethodInfo method) {

		FileInterface file = method.getClassfile().getSourceFile();
		if (file == null)
			file = method.getClassfile();

		removeWindowsArrowsAndHighlights();
		emphasizeMethod(method);

		TokenRange range = file.getTokenRangeForMethod(method);

		selectionConfiguration = new Configuration(method, range);
		selectionConfiguration.arrange(true);

		getWhylineUI().getOutlineUI().showMethod(method);

	}

	private void showWindow(FileWindow window) {

		if (window.getParent() == null) {

			addChild(window);

			// Update the window metrics here for the arrow box layout.
			windowHeaderHeight = window.getHeaderHeight();
			windowScrollBarMargin = window.getScrollBarMargin();

		}
		window.bringToFront();

	}

	public void showFile(FileInterface file) {

		removeWindowsArrowsAndHighlights();

		FileWindow window = getWindowViewOf(file);

		if (window == null)
			return;

		showWindow(window);

		placeWindow(window, 0, 0, getLocalWidth(), getLocalHeight(), true);

		whylineUI.getOutlineUI().showFile(file);

	}

	public void showLine(Line line) {

		removeWindowsArrowsAndHighlights();

		TokenRange range = line.getRange();
		while (line != null && range == null) {
			try {
				line = line.getLineAfter();
			} catch (ParseException e) {
				e.printStackTrace();
				range = null;
			}
			if (line != null)
				range = line.getRange();
		}
		if (range == null)
			return;

		selectionConfiguration = new Configuration(line, range);
		selectionConfiguration.arrange(true);

		FileWindow window = getWindowViewOf(line.getFile());
		LineView view = window.getFileView().getViewOf(line);
		view.setEmphasized(true);

		showWindow(window);
		placeWindow(window, 0, 0, getLocalWidth(), getLocalHeight(), true);

	}

	public void showUnexecutedInstruction(UnexecutedInstruction inst) {

		removeWindowsArrowsAndHighlights();

		selectionExplanation = new SimpleHTML("<b>"
				+ inst.getInstruction().getLineNumber()
				+ "</b> didn't execute because " + inst.getVerbalExplanation(),
				(Graphics2D) whylineUI.getGraphics(), UI.getSmallFont());

		// Add an arrow for each incoming prevention
		int number = 0;
		for (UnexecutedInstruction incoming : inst.getIncoming()) {
			arrows.addArrow(new UnexecutedArrowView(arrows, incoming, inst,
					number++));
		}

		emphasizeMethod(inst.getInstruction().getMethod());

		SortedSet<TokenView> tokens = new TreeSet<TokenView>();

		selectionConfiguration = new Configuration(inst, inst.getInstruction()
				.getFile().getTokenRangeFor(inst.getInstruction()));

		// Show the instruction of each deciding event, gathering their views.
		if (inst.getDecidingEvents() != null) {

			IntegerVector decidingEvents = inst.getDecidingEvents();
			for (int i = 0; i < decidingEvents.size(); i++) {

				Instruction decidingInstruction = getTrace().getInstruction(
						decidingEvents.get(i));
				emphasizeMethod(decidingInstruction.getMethod());

			}

		}
		// Highlight the two instructions in the file.
		else if (inst.getDecidingInstruction() != null) {
			emphasizeMethod(inst.getDecidingInstruction().getMethod());
		}

		// Show the instruction and its method.
		else if (inst.getDecidingMethod() != null) {
			emphasizeMethod(inst.getDecidingMethod());
		}

		// Show the instruction and its causes
		else if (inst.getDecidingInstructions() != null)
			for (Instruction i : inst.getDecidingInstructions()) {
				emphasizeMethod(i.getMethod());
			}

		else if (inst.getDecidingEventID() >= 0) {
			Instruction decidingEventInstruction = getTrace().getInstruction(
					inst.getDecidingEventID());
			emphasizeMethod(decidingEventInstruction.getMethod());
		}

		selectionConfiguration.arrange(true);

	}

	public void showClass(Classfile classfile) {

		FileInterface file = classfile.getSourceFile();
		if (file == null)
			file = classfile;

		TokenRange range = file.getTokenRangeFor(classfile);
		if (range == null) {
			showFile(file);
		} else {

			selectionConfiguration = new Configuration(classfile, range);

			removeWindowsArrowsAndHighlights();
			selectionConfiguration.arrange(true);

		}

	}

	private class WindowConfiguration {

		private final TokenRange range;
		private FileInterface file;
		private FileWindow window;
		private Token firstCodeToken;

		public WindowConfiguration(TokenRange range) {

			this.range = range;

			// If we have a range to show...
			if (range != null && range.first != null) {

				// Find the first non-whitespace token in the range...
				firstCodeToken = null;
				for (Token t : range)
					if (t.isCode()) {
						firstCodeToken = t;
						break;
					}

				// Find the window of that token...
				file = range.first.getFile();
				window = getWindowViewOf(file);

				// Emphasize all focus tokens to show the highlights in the
				// margin...
				for (Token t : range) {
					TokenView view = window.getFileView().getViewOf(t);
					if (view != null)
						view.setEmphasized(UI.getHighlightColor());
				}

			}

		}

		public void scroll() {

			if (firstCodeToken == null)
				return;

			// Now make sure the focus token is in the center of the screen.
			TokenView view = window.getFileView().getViewOf(firstCodeToken);
			if (view != null)
				window.scrollToToken(view);

		}

	}

	public class Configuration {

		private final Object focus;

		private final WindowConfiguration focusConfig;
		private final ArrayList<WindowConfiguration> otherConfigs = new ArrayList<WindowConfiguration>();

		public Configuration(Object focus, TokenRange focusRange) {

			assert focusRange != null : "Must provide legal token range. The user has to look at SOMETHING.";

			this.focus = focus;

			focusConfig = new WindowConfiguration(focusRange);

		}

		/**
		 * This goes through the tokens we're supposed to show and finds their windows, so that we can show them to the user.
		 * This should be called after every rearrangement of arrows, since the user's selection can change what files are shown.
		 */
		private void updateOtherWindows() {

			otherConfigs.clear();

			java.util.List<TokenRange> otherRanges = getExternalArrowTargetRange();
			if (otherRanges != null) {
				for (TokenRange range : otherRanges) {

					boolean alreadyShowing = false;

					for (WindowConfiguration config : otherConfigs)
						if (config.file == range.first.getFile())
							alreadyShowing = true;

					if (focusConfig != null && focusConfig.file == range.first.getFile())
						alreadyShowing = true;

					if (!alreadyShowing) {
						WindowConfiguration other = new WindowConfiguration(range);
						if (other.window != null)
							otherConfigs.add(other);

					}
				}
			}

		}

		private java.util.List<TokenRange> getExternalArrowTargetRange() {

			// Handle the hovered arrow. Will it point to a file other than the
			// focus window?
			ArrowView arrowView = getSelectedFileArrow();
			if (arrowView != null) {
				java.util.List<TokenRange> ranges = arrowView.getViableTargetTokenRanges();
				return ranges;
			}
			return null;

		}

		public void arrange(boolean animate) {
			
			updateOtherWindows();

			// We're about to add all of the windows, so we need to clear out all of the windows to make room for the new ones.
			// Unfortunately, there are also lots of arrows.
			removeWindows();			
			
			if (focusConfig == null)
				return;

			showWindow(focusConfig.window);

			// If there are no other windows, give the focus window all the
			// space
			if (otherConfigs.isEmpty()) {

				placeWindow(focusConfig.window, 0, 0, getLocalWidth(), getLocalHeight(), animate);

			}
			// Otherwise, put the other windows on the left, giving the focus
			// most of the space.
			else {

				// This lays windows out with dependencies on the bottom, focus
				// in the center.
				double availableWidth = getLocalWidth();

				placeWindow(focusConfig.window, 0, 0, availableWidth, getLocalHeight() * (.66), animate);

				double left = 0;
				double top = getLocalHeight() * .66;
				double width = availableWidth / otherConfigs.size();
				double height = getLocalHeight() / 3;

				for (WindowConfiguration config : otherConfigs) {

					showWindow(config.window);
					placeWindow(config.window, left, top, width, height,animate);
					left += width;

				}

			}

			focusConfig.scroll();
			for (WindowConfiguration other : otherConfigs)
				other.scroll();

			// Temporarily fade the irrelevant lines.
			if (currentFader != null) {
				currentFader.cancel();
				currentFader = null;
			}
			currentFader = new TimerTask() {
				private long start = System.currentTimeMillis();

				public void run() {
					double now = System.currentTimeMillis();
					double delta = now - start;
					double transparency = fadeFactor + (1 - fadeFactor) * Math.min(1, Math.max(0, delta - FADED_TIME) / (FADE_IN_TIME));
					fade = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
							(float) transparency);
					if (delta > TOTAL_FADE_TIME) {
						synchronized (currentFader) {
							currentFader.cancel();
							currentFader = null;
						}
					}
					repaint();
				}
			};
			fadeTimer.scheduleAtFixedRate(currentFader, 0, 50);

			arrows.bringToFront();

		}

		public TokenRange getFocusRange() {
			return focusConfig == null ? null : focusConfig.range;
		}

	}

	public TokenRange getRangeFor(int eventID) {

		Trace trace = whylineUI.getTrace();
		EventKind kind = trace.getKind(eventID);
		Instruction inst = trace.getInstruction(eventID);

		final TokenRange range;

		if (kind.isArtificial) {

			if (kind.isArgument) {

				int localID = trace.getArgumentLocalIDSet(eventID);
				int parameterNumber = inst.getMethod().getArgumentNumberOfLocalID(localID);
				if (inst.getMethod().isStatic())
					parameterNumber++;
				range = inst.getFile().getTokenRangeForParameter(inst.getMethod(), parameterNumber);

			} else if (kind == EventKind.START_METHOD) {

				range = inst.getFile().getTokenRangeForMethod(inst.getMethod());

			} else
				return inst.getFile().getTokenRangeFor(inst);

		} else {
			range = inst.getFile().getTokenRangeFor(inst);

		}

		// If we couldn't find something suitable, just return the line for the event.
		if (range == null)
			return inst.getLine().getRange();
		else
			return range;

	}

	public Area getAreaForTokenRange(TokenRange range) {

		if (range == null || range.first == null)
			return null;

		FileInterface file = range.first.getFile();
		return getWindowViewOf(file).getFileView().getTokenRangeOutline(range);

	}

	/**
	 * Assumes that the graphics context is in this view's coordinate system.
	 */
	public void outline(Graphics2D g, Instruction inst) {

		// Draw a border around the tokens for the given instruction.
		TokenRange range = inst.getFile().getTokenRangeFor(inst);
		if (range != null) {
			outline(g, range);
		}

	}

	public Area outline(Graphics2D g, TokenRange range) {

		if (range == null || range.first == null)
			return null;

		FileInterface file = range.first.getFile();
		FileWindow window = getWindowViewOf(file);
		if (window.getFileView() != null) {

			g = (Graphics2D) g.create();
			g.setStroke(UI.SELECTED_STROKE);
			Area path = window.getFileView().getTokenRangeOutline(range);
			g.draw(path);
			return path;

		}
		return null;

	}

	private TimerTask currentFader = null;
	private final Timer fadeTimer = new Timer(true);
	private final double fadeFactor = 0.33;
	private AlphaComposite fade = null;
	private final int FADED_TIME = 250;
	private final int FADE_IN_TIME = 500;
	private final int TOTAL_FADE_TIME = FADED_TIME + FADE_IN_TIME;

	public AlphaComposite getCurrentFade() {
		return fade;
	}

	public class ArrowBox extends View {

		private final ArrayList<ArrowView> arrows = new ArrayList<ArrowView>(5);

		private boolean atInitialPosition = true;

		public ArrowBox() {
		}

		public void clear() {

			arrows.clear();
			removeChildren();
			layoutArrows(false);

		}

		public TokenRange getFocusRange() {

			if (selectionConfiguration == null)
				return null;
			else
				return selectionConfiguration.getFocusRange();

		}

		public void layoutArrows(boolean painting) {

			for (ArrowView arrow : arrows)
				arrow.layout();

			int padding = getPadding();

			int width = (int) (Math.max(selectionExplanation == null ? 0 : selectionExplanation.getWidth(), getRightmostChildsRight()) + padding * 2);
			int height = (int) (Math.max(selectionExplanation == null ? 0 : selectionExplanation.getHeight(), getBottommostChildsBottom()) + padding * 2);

			if (atInitialPosition) {
				setLocalLeft(getParent().getLocalWidth() - width - UI.getPanelPadding() - windowScrollBarMargin, true);
				setLocalTop(windowHeaderHeight + UI.getPanelPadding(), true);
			}

			if (width < getParent().getLocalWidth() && getLocalLeft() + width > getParent().getLocalWidth())
				setLocalLeft(getParent().getLocalWidth() - width - UI.getPanelPadding(), true);

			if (height < getParent().getLocalHeight() && getLocalTop() + height > getParent().getLocalHeight())
				setLocalTop(getParent().getLocalHeight() - height - UI.getPanelPadding(), true);

			setLocalWidth(width, !painting);
			setLocalHeight(height, !painting);

			if (!painting)
				animate(UI.getDuration(), true);

		}

		public int getPadding() {
			return 5;
		}

		public void addArrow(ArrowView arrow) {

			addChild(arrow);
			arrows.add(arrow);

		}

		public FilesView getFilesView() {
			return FilesView.this;
		}

		public void paintBelowChildren(Graphics2D g) {

			if (whylineUI.getVisualizationUIVisible() == null)
				return;

			g = (Graphics2D) g.create();
			Composite old = g.getComposite();
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
					0.75f));
			fillRoundBoundaries(UI.IDENTIFIER_COLOR, g, UI.getRoundedness(), UI
					.getRoundedness());
			g.setComposite(old);

			TokenRange range = getFocusRange();
			Area area = getAreaForTokenRange(range);

			if (area != null) {

				g.setColor(UI.getHighlightColor());
				int lineX = (int) area.getBounds().getMaxX();
				int lineY = (int) area.getBounds().getMaxY();

				Line2D line = Util.getLineBetweenRectangleEdges(lineX, lineX,
						lineY, lineY, getVisibleLocalLeft(),
						getVisibleLocalRight(), getVisibleLocalTop(),
						getVisibleLocalBottom());

				// Point to the focus range.
				int offset = line.getX2() > lineX ? 10 : -10;
				g.drawLine(lineX, lineY, (int) line.getX2() - offset, lineY);
				g.drawLine((int) line.getX2() - offset, lineY, (int) line
						.getX2(), (int) line.getY2());

			}

			// Outline the focus range! Unless another arrow is highlighting it
			// already.
			TokenRange focusRange = getFocusRange();
			if (focusRange != null) {

				// Is the selected arrow highlighting the same line.
				boolean lineOtherwiseHighlighted = false;
				java.util.List<TokenRange> ranges = selectionConfiguration
						.getExternalArrowTargetRange();
				if (ranges != null) {
					for (TokenRange r : ranges)
						if (r.first.getLine() == focusRange.first.getLine())
							lineOtherwiseHighlighted = true;
				}

				if (!lineOtherwiseHighlighted)
					outline(g, getFocusRange());

			}

		}

		public void paintAboveChildren(Graphics2D g) {

			Visualization viz = whylineUI.getVisualizationUIVisible() == null ? null
					: whylineUI.getVisualizationUIVisible().getVisualization();
			if (selectionExplanation != null && viz != null) {

				int x = (int) getVisibleLocalLeft() + getPadding();
				int y = (int) getVisibleLocalTop() + getPadding();

				g.setColor(UI.getHighlightColor());
				selectionExplanation.paint(g, x, y);

			}

		}

		public boolean handleMouseDown(int x, int y, int button) {

			getContainer().focusMouseOn(this);
			return true;

		}

		public boolean handleMouseDrag(int x, int y, int button) {

			atInitialPosition = false;

			int focusX = getContainer().getMouseFocusX();
			int focusY = getContainer().getMouseFocusY();

			setLocalLeft(x - focusX, false);
			setLocalTop(y - focusY, false);

			layoutArrows(false);

			return true;

		}

		public boolean handleMouseUp(int x, int y, int button) {

			getContainer().releaseMouseFocus();
			return true;

		}

		public boolean handleMouseMove(int x, int y) {
			return true;
		}

		public void handleMouseEnter() {
			getContainer().setCursor(
					Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		}

		public void handleMouseExit() {
			getContainer().setCursor(
					Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}

		public boolean handleMouseClick(int x, int y, int button) {
			return true;
		}

	}

	public int getNumberOfArrows() {
		return arrows.getNumberOfChildren();
	}

}
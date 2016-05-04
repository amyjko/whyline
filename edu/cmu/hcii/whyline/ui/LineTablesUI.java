package edu.cmu.hcii.whyline.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.*;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import edu.cmu.hcii.whyline.analysis.SearchResultsInterface;
import edu.cmu.hcii.whyline.source.Line;
import edu.cmu.hcii.whyline.source.Token;
import edu.cmu.hcii.whyline.ui.components.*;
import edu.cmu.hcii.whyline.util.CloseIcon;
import edu.cmu.hcii.whyline.util.Util;

/**
 * @author Andrew J. Ko
 *
 */
public class LineTablesUI extends WhylinePanel {

	private WhylineUI whylineUI;

	private WhylineTabbedPane tabs;
	
	private final RelevantLines relevantLines;
	private BreakpointLines breakpointLines;
	private History history; 
	
	private Icon closeIcon = new CloseIcon() {
		public boolean isSelected(Component c) {
			return tabs.getSelectedComponent() == c;
		}
	};
	
	public LineTablesUI(WhylineUI ui) {
		
		this.whylineUI = ui;

		tabs = new WhylineTabbedPane() {
			public void selectedTabIconPressed(int index) {
				removeResults(index);
			} 
		};
		tabs.setBorder(null);
		
		relevantLines = new RelevantLines();
		breakpointLines = new BreakpointLines();
		history = new History();
		
		setLayout(new BorderLayout(0, UI.getPanelPadding()));

		add(tabs, BorderLayout.CENTER);

		tabs.insertTab("<html><center>bookmarks", null, relevantLines, null, 0);
		tabs.insertTab("<html><center>history", null, history, null, 1);

		if(whylineUI.getMode() == WhylineUI.Mode.BREAKPOINT)
			tabs.insertTab("<html><center>breakpoints", null, breakpointLines, null, 2);
		
		setMinimumSize(new Dimension(UI.getDefaultInfoPaneWidth(whylineUI), 0));

	}
	
	private void removeResults(int index) {
		
		if(tabs.getSelectedComponent() instanceof SearchResultsLines) {
			tabs.remove(index); 
			// We repaint here because the file view may be showing search results.
			whylineUI.getFilesView().repaint();
		}
		
	}
	
	public SearchResultsInterface getSelectedResults() {
		
		Component selection = tabs.getSelectedComponent();
		if(selection instanceof SearchResultsLines)
			return ((SearchResultsLines)selection).results;
		else
			return null;
		
	}
	
	public boolean selectedTabContains(Line line) {

		Component selection = tabs.getSelectedComponent();
		if(!(selection instanceof SearchResultsLines)) return false;
		return ((SearchResultsLines)selection).lines.contains(line);
		
	}
	
	public Line getSelectedLine() {

		Component selection = tabs.getSelectedComponent();
		if(!(selection instanceof SearchResultsLines)) return null;
		return ((SearchResultsLines)selection).lines.getSelection();
	
	}

	public void addResults(SearchResultsInterface searchResults) {

		whylineUI.showStaticInfo(true);
		
		SearchResultsLines tab = new SearchResultsLines(searchResults);
		
		int newIndex = tabs.getTabCount() ;
		tabs.insertTab(Util.elide(searchResults.getResultsDescription(), 10),closeIcon , tab, null, newIndex);
		tabs.setSelectedIndex(newIndex);
		
	}
	
	class LineRenderer extends JLabel implements ListCellRenderer {

		private LineRenderer() {}
		
	     public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
	    	 
	    	 Line line = (Line)value;
	    	 
	    	 String label = "<html><b>" + Util.fillOrTruncateString(line.getFile().getShortFileName(), 15) + ": " + Util.fillOrTruncateString(Integer.toString(line.getLineNumber().getNumber()), 10) + "</b>" + line.getLineText();
	    	 label = label.replace(" ", "&nbsp;");
	         setText(label);
	                  
	         if (isSelected) {
	        	 setBackground(UI.getHighlightColor());
	        	 setForeground(java.awt.Color.white);
	         }
	         else {
	        	 setBackground(list.getBackground());
	        	 setForeground(list.getForeground());
	         }
	         setEnabled(list.isEnabled());
	         setFont(list.getFont());
	         setOpaque(true);
	         return this;
	    }		

	}

	private class SearchResultsLines extends WhylinePanel {
		
		private final LineTableUI lines;
		private final WhylineLabel status;

		private List<Line> resultLines = new ArrayList<Line>();
		
		private SearchResultsInterface results;

		private Timer timer;

		public SearchResultsLines(SearchResultsInterface searchResults) {
			
			super(new BorderLayout());

			this.results = searchResults;
			
			float fontSize = 10.0f;
			
			status = new WhylineLabel("", fontSize);
			status.setHorizontalAlignment(SwingConstants.CENTER);

			lines = new LineTableUI(whylineUI, resultLines, UI.SEARCH_RESULTS_UI, true);
			lines.setFocusable(true);

			WhylineScrollPane scroller = new WhylineScrollPane(lines);
			scroller.setBorder(new WhylineControlBorder());

			WhylineLabel title = new WhylineLabel("<html><i>" + searchResults.getResultsDescription() + "</i>...", UI.getMediumFont().deriveFont(Font.BOLD));
			title.setHorizontalAlignment(SwingConstants.CENTER);
			title.setBorder(new EmptyBorder(UI.getPanelPadding(), 0, 0, 0));
			
			add(title, BorderLayout.NORTH);
			add(scroller, BorderLayout.CENTER);
			add(status, BorderLayout.SOUTH);
			setPreferredSize(new Dimension(UI.getDefaultInfoPaneWidth(whylineUI), (UI.getDefaultInfoPaneHeight(whylineUI) * 2) / 3));

			timer = new Timer("Search results feedback", true);
			timer.schedule(new TimerTask() {
				public void run() {

					updateCurrentResults();
					if(results.isDone()) {
						timer.cancel();
						timer = null;
					}
				}
			}, 0, 500);

		}
	
		public void updateCurrentResults() {
			
			status.setText(results.getCurrentStatus());
			
			SortedSet<Token> tokens = results.getResults();
			
			Set<Line> resultSet = new TreeSet<Line>();
			for(Token t : tokens)
				resultSet.add(t.getLine());		
			
			resultLines.clear();
			resultLines.addAll(resultSet);
			
			lines.updateLines(resultLines);

		}

	}
		
	public void updateRelevantLines(Line line) {
	
		relevantLines.lines.updateLines(whylineUI.getPersistentState().getRelevantLines());
		tabs.setSelectedIndex(0);
		relevantLines.lines.select(line);
		
	}

	public void updateHistory(Line line) {

		history.lines.updateLines(whylineUI.getNavigationHistory());
		history.lines.select(0);
		
	}

	private class LinePanel extends WhylinePanel {
		
		protected LineTableUI lines;

		public LinePanel(String uiString, boolean addSelectionsToHistory) {

			lines = new LineTableUI(whylineUI, new ArrayList<Line>(0), uiString, addSelectionsToHistory);
			
			setLayout(new BorderLayout());
			setBackground(UI.getControlBackColor());
			setOpaque(true);
			setBorder(new WhylineControlBorder());

			add(new WhylineScrollPane(lines), BorderLayout.CENTER);

		}
		
	}
	
	private class RelevantLines extends LinePanel {
		
		private RelevantLines() {
			
			super(UI.RELEVANT_LINES_UI, true);

		}
		
	}

	private class History extends LinePanel {

		private History() {
			
			super(UI.NAVIGATION_HISTORY_UI, false);

		}
		
	}

	private class BreakpointLines extends LinePanel {
		
		private BreakpointLines() {
			
			super(UI.BREAKPOINT_LINES_UI, true);

			add(lines, BorderLayout.CENTER);
			add(new WhylineButton(whylineUI.getActions().clearBreakpoints, "clear all breakpoints"), BorderLayout.SOUTH);

		}
		
	}

	public void updateBreakpointLines(Line line) {

		List<Line> linesSet = whylineUI.getBreakpointDebugger().getLinesWithBreakpointsOrPrints();
		
		breakpointLines.lines.updateLines(linesSet);

		tabs.setSelectedIndex(2);
		breakpointLines.lines.select(line);
		
		whylineUI.getFilesView().repaint();
		
	}

}
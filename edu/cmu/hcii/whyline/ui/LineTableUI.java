package edu.cmu.hcii.whyline.ui;

import java.awt.Component;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.*;

import edu.cmu.hcii.whyline.source.Line;

/**
 * @author Andrew J. Ko
 *
 */
public class LineTableUI extends JTable {

	private final WhylineUI whylineUI;
	private List<Line> lines;
	private final String uiString;
	private final boolean addSelectionsToNavigationHistory;
	
	private enum Column { FILE, NUMBER, TEXT };
	
	private boolean programmaticSelection = false;
	
	private Line lastSelection;
	
	private final AbstractTableModel model = new DefaultTableModel() {

		public int getColumnCount() { return Column.values().length; }
		public int getRowCount() { return lines == null ? 0 : lines.size(); }
		public Object getValueAt(int row, int column) {

			if(lines == null) return null; 
			
			Line line = lines.get(row);
			
			if(column == Column.NUMBER.ordinal())
				return line.getLineNumber().getNumber();
				
			else if(column == Column.FILE.ordinal())
				return line.getFile().getShortFileName();

			else if(column == Column.TEXT.ordinal())
				return "<html><b>" + line.getLineText() + "</b>";

			else return null;

		}
		public String getColumnName(int col) { return Column.values()[col].name().toLowerCase(); }
		public boolean isCellEditable(int row, int col) { return false; }
		
	};
		
	public LineTableUI(WhylineUI whylineUI, List<Line> lines, String uiString, boolean addSelectionsToNavigationHistory) {
		
		super();

		this.uiString = uiString;
		this.addSelectionsToNavigationHistory = addSelectionsToNavigationHistory;
		
		setModel(model);
		setBackground(null);
		setOpaque(false);
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		getColumnModel().setColumnSelectionAllowed(false);
		setShowGrid(false);

		JTableHeader header = getTableHeader();
		header.setFont(UI.getMediumFont().deriveFont(Font.BOLD));
		header.setBackground(UI.getPanelDarkColor());
		header.setBorder(new LineBorder(UI.getControlBorderColor()));
		
		for(int i = 0; i < Column.values().length; i++) {
			TableColumn column = getColumnModel().getColumn(i); 
			column.setHeaderRenderer(new DefaultTableCellRenderer() {
			    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			    	Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			    	setHorizontalAlignment(SwingConstants.LEFT);
			    	setFont(UI.getMediumFont().deriveFont(Font.BOLD));
			    	setBackground(UI.getPanelDarkColor());
			    	return c;
			    }
			});
		}
		
		this.whylineUI = whylineUI;
		this.lines = lines;

		// Force an update to the selection on a click, even if the selection hasn't changed.
		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				int row = getSelectedRow();
				clearSelection();
				addRowSelectionInterval(row, row);
			}
		});
		
		getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				
				lastSelection = null;
				if(programmaticSelection) programmaticSelection = false;
				else {
					int row = getSelectedRow();
					if(row >= 0 && row < LineTableUI.this.lines.size()) {
						lastSelection = LineTableUI.this.lines.get(row);
						LineTableUI.this.whylineUI.selectLine(lastSelection, LineTableUI.this.addSelectionsToNavigationHistory, LineTableUI.this.uiString);
					}
				}
			}
		});
	
		// Flash this for a bit after adding it to a panel, so that in case it was far from the search, the user notices it.
		final Timer timer = new Timer(true);
		timer.schedule(new TimerTask() {
			int count = 0;
			public void run() {
				Component parent = LineTableUI.this.getParent();
				if(parent != null) {
					LineTableUI.this.getParent().setBackground(LineTableUI.this.getBackground() == UI.getHighlightColor() ? UI.getControlBackColor() : UI.getHighlightColor());
					count++;
					if(count > 5) {
						timer.cancel();
						parent.setBackground(UI.getControlBackColor());
					}
				}
			}
		}, 100, 200);
		
	}
	
	public void updateLines(List<Line> lines) {
		
		this.lines = lines;
		model.fireTableDataChanged();
		
	}

	public void select(Line line) {
		
		if(line == null) return;
		int index = lines.indexOf(line);
		select(index);
		
	}
	
	public Line getSelection() { 
		
		int row = getSelectedRow();
		if(row >= 0 && row < lines.size())
			return lines.get(row);
		else
			return null;
		
	}
	
	public void select(int index) {

		if(index >= 0 && index < lines.size()) {
			programmaticSelection = true;
			getSelectionModel().setSelectionInterval(index, index);
		}
		
	}

	public boolean contains(Line line) { return lines.contains(line); }
	
}
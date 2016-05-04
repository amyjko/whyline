package edu.cmu.hcii.whyline.ui.io;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.*;

import javax.swing.AbstractListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.ListCellRenderer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import edu.cmu.hcii.whyline.qa.ExceptionMenuFactory;
import edu.cmu.hcii.whyline.qa.QuestionMenu;
import edu.cmu.hcii.whyline.trace.*;
import edu.cmu.hcii.whyline.ui.UI;
import edu.cmu.hcii.whyline.ui.WhylineUI;
import edu.cmu.hcii.whyline.ui.components.WhylineControlBorder;
import edu.cmu.hcii.whyline.ui.components.WhylineList;
import edu.cmu.hcii.whyline.ui.components.WhylinePanel;
import edu.cmu.hcii.whyline.ui.components.WhylineScrollPane;
import edu.cmu.hcii.whyline.util.IntegerVector;

/**
 * @author Andrew J. Ko
 *
 */
public final class ExceptionsUI extends WhylinePanel {

	private final WhylineUI whylineUI;
	// Full of eventIDs that point to exceptions thrown or caught
	private final IntegerVector exceptions = new IntegerVector(20);
	private final WhylineList list;
	private boolean parsedExceptions = false;
	private MouseEvent recentMouseEvent = null;
	
	public ExceptionsUI(WhylineUI whylineUI) {

		super(new BorderLayout());
		
		setBorder(new WhylineControlBorder());
		
		this.whylineUI = whylineUI;

		list = new WhylineList();
		list.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
		
		list.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) { recentMouseEvent = e; }
		});

		list.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				
				if(ExceptionsUI.this.whylineUI.getVisualizationUIVisible() != null) return;
				
				int exceptionEventID = (Integer)list.getSelectedValue();
				
				ExceptionsUI.this.whylineUI.selectInstruction(ExceptionsUI.this.whylineUI.getTrace().getInstruction(exceptionEventID), true, UI.EXCEPTIONS_UI);

				QuestionMenu menu = ExceptionMenuFactory.getQuestionMenuForException(ExceptionsUI.this.whylineUI, exceptionEventID);
				
				JPopupMenu popup = menu.generatePopupMenu();

				popup.show(list, recentMouseEvent == null ? 0 : recentMouseEvent.getX(), recentMouseEvent == null ? 0 : recentMouseEvent.getY());
				
			}
		});
		
		add(new WhylineScrollPane(list), BorderLayout.CENTER);

		addComponentListener(new ComponentAdapter() {
		    public void componentShown(ComponentEvent e) { parseExceptions(); }
		});

	}	
	
	private void parseExceptions() {

		if(parsedExceptions) return;
		parsedExceptions = true;
		
		Trace trace = whylineUI.getTrace();
		
		IntegerVector times = trace.getExceptionHistory().getTimes();
		
		for(int i = 0; i < times.size(); i++) {
						
			exceptions.append(times.get(i));
			
		}

		list.setModel (
            new AbstractListModel() {
                public int getSize() { return exceptions.size(); }
                public Object getElementAt(int i) { return new Integer(exceptions.get(i)); }
            }
        );
		
		if(exceptions.isEmpty()) {

			list.setFont(UI.getLargeFont());
			list.setListData(new String[] { "Program didn't throw or catch any exceptions." });
			list.setEnabled(false);
			
		}
		else {
			
			list.setCellRenderer(new ExceptionRenderer());

		}
		
	}
	
	class ExceptionRenderer extends JLabel implements ListCellRenderer {

		private ExceptionRenderer() {}
		
	     public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
	    	 
	    	 int exceptionEventID = (Integer)value;
	    	 
	         setText("<html>" + whylineUI.getTrace().getHTMLDescription(exceptionEventID));
	                  
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

}

package edu.cmu.hcii.whyline.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.*;
import java.io.*;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.cmu.hcii.whyline.analysis.Usage;
import edu.cmu.hcii.whyline.bytecode.Classfile;
import edu.cmu.hcii.whyline.bytecode.MethodInfo;
import edu.cmu.hcii.whyline.bytecode.QualifiedClassName;
import edu.cmu.hcii.whyline.ui.components.*;

/**
 * @author Andrew J. Ko
 *
 */
public class DebugMenu extends JLabel {

	private final WhylineUI whylineUI;
	
	public DebugMenu(WhylineUI whylineUI) {
	
		super(UI.WHYLINE_ICON);
		
		this.whylineUI = whylineUI;
		
		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				showMenu(e.getX(), e.getY());
			}
		});
		
	}

	private void showMenu(int x, int y) {
		
		WhylinePopup popup = new WhylinePopup("");

		boolean done = whylineUI.getTrace().isDoneLoading();
		
		WhylineMenuItem saveas = new WhylineMenuItem("Save as...", new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				DebugMenu.this.whylineUI.save(false); 
			}});
		saveas.setEnabled(done);
		

		WhylineMenuItem breakdown = new WhylineMenuItem("Show trace memory breakdown...", new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				DebugMenu.this.whylineUI.showTraceBreakdown(); 
			}});
		breakdown.setEnabled(done);

		WhylineMenuItem usage = new WhylineMenuItem("Generate usage statistics from usage logs...", new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				try {
					generateUsageStatistics();
				} catch (IOException  ex) {
					ex.printStackTrace();
				}
			}});

		WhylineMenuItem browser = new WhylineMenuItem("Show event list...", new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				new Browser();
			}});

		WhylineMenuItem memory = new WhylineMenuItem("Show memory usage...", new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				whylineUI.showMemoryUsage();
			}});

		WhylineMenuItem showfile = new WhylineMenuItem("Show a file...", new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				showFile();
			}});

		popup.add(saveas);
		popup.add(breakdown);
		popup.add(usage);
		popup.add(browser);
		popup.add(memory);
		popup.add(showfile);

		popup.show(DebugMenu.this, x, y);
				
	}
	
	private void showFile() {
		
		String name = JOptionPane.showInputDialog(whylineUI, "What's the qualified file class name?");
		if(name != null)
			whylineUI.selectFile(whylineUI.getTrace().getSourceByQualifiedName(name), true, "debug");
		
	}
	
	private void generateUsageStatistics() throws IOException {

		String methodname = JOptionPane.showInputDialog(whylineUI, "Which method is the bug in (e.g., \"java/lang/Character.isDefined(C)Z\")?");
		if(methodname == null) return;
		String[] names = methodname.split("\\.");
		
		if(names.length != 2) {
			JOptionPane.showMessageDialog(whylineUI, "Couldn't split into class and method name");
			return;
		}
		Classfile buggyClass = whylineUI.getTrace().getClassfileByName(QualifiedClassName.get(names[0]));
		if(buggyClass == null) {
			JOptionPane.showMessageDialog(whylineUI, "Couldn't find class " + names[0]);
			return;
		}
		MethodInfo buggyMethod = buggyClass.getDeclaredMethodByNameAndDescriptor(names[1]);
		if(buggyMethod == null) {
			JOptionPane.showMessageDialog(whylineUI, "Couldn't find method " + names[1] + " in " + names[0]);
			return;
		}
				
		JFileChooser chooser = new JFileChooser(whylineUI.getTrace().getPath());
		chooser.setDialogTitle("Select a folder that contains the usage logs to analyze");
		chooser.setFileHidingEnabled(true);
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		int choice = chooser.showOpenDialog(whylineUI);
		
		if(choice != JFileChooser.APPROVE_OPTION) return;
		
		File folder = chooser.getSelectedFile();

		Usage usage = new Usage(whylineUI.getTrace(), folder, buggyMethod);

		JOptionPane.showMessageDialog(whylineUI, "Saved 'results.csv' in " + folder.getName());

	}
	
	private class Browser extends JFrame {
		
		private final int lineHeight;
		private final WhylineList list;
		private final JSlider slider;
		
		public Browser() {
			
			super("Event browser");

			Border padding = new EmptyBorder(UI.getPanelPadding(), UI.getPanelPadding(), UI.getPanelPadding(), UI.getPanelPadding());
			
			Font font = UI.getFixedFont();
			lineHeight = whylineUI.getGraphics().getFontMetrics(font).getHeight();
			
			list = new WhylineList();
			list.setFont(font);
			list.setBorder(padding);
			
			slider = new JSlider(JSlider.VERTICAL, 0, whylineUI.getTrace().getNumberOfEvents() - 1, 0); 
			slider.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					refocus();
				}
			});

			WhylineToolbar toolbar = new WhylineToolbar(WhylineToolbar.VERTICAL);
			toolbar.setBorder(padding);

			toolbar.add(slider);
			toolbar.add(new AbstractAction(String.valueOf(UI.RIGHT_ARROW)) {
				public void actionPerformed(ActionEvent e) {
					slider.setValue(slider.getValue() - getWindow());
				}
			});
			toolbar.add(new AbstractAction(String.valueOf(UI.LEFT_ARROW)) {
				public void actionPerformed(ActionEvent e) {
					slider.setValue(slider.getValue() + getWindow());
				}
			});
			
			getContentPane().addComponentListener(new ComponentAdapter() {
				public void componentResized(ComponentEvent e) {
					refocus();
				}
			});
			
			getContentPane().setLayout(new BorderLayout());
			getContentPane().add(new WhylineScrollPane(list), BorderLayout.CENTER);
			getContentPane().add(toolbar, BorderLayout.EAST);

			refocus();
			
			setSize(640,480);
			setVisible(true);
			
		}
		
		public int getWindow() { return (list.getParent().getHeight())  / (lineHeight + 3) / 2; }
		
		private void refocus() {
			
			int window =  getWindow();
			
			int min = window;
			int max = whylineUI.getTrace().getNumberOfEvents() - window - 1;
			
			int eventID = Math.max(min, Math.min(max, max - slider.getValue()));

			String[] events = new String[window * 2];
			
			eventID -= window;
			for(int i = 0; i <  window * 2; i++) {
				events[i] = whylineUI.getTrace().eventToString(eventID);
				eventID++;
			}

			list.setListData(events);
			
		}
		
	}
	
}
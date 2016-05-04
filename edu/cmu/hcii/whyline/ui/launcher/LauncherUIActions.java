package edu.cmu.hcii.whyline.ui.launcher;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

/**
 * @author Andrew J. Ko
 *
 */
public class LauncherUIActions {

	private final LauncherUI launcher;

	public final Action createConfiguration, clearConfigurations, removeConfiguration, editConfiguration;
	public final Action clearClassCache;
	public final Action deleteSelectedTrace, deleteAllSavedTraces;
	public final Action cancelRunning;
	
	public LauncherUIActions(LauncherUI ui) {
		
		this.launcher = ui;
		
		createConfiguration = new AbstractAction("Create config") { 
		
			public void actionPerformed(ActionEvent arg0) {

				launcher.createConfig();
				
			}
		};

		editConfiguration = new AbstractAction("Edit config") { 
		
			public void actionPerformed(ActionEvent arg0) {

		    	launcher.editSelectedConfiguration();
				
			}
		};
		
		clearConfigurations = new AbstractAction("Delete all configs") { 
			public void actionPerformed(ActionEvent arg0) {
				
				int choice = JOptionPane.showConfirmDialog(launcher, "Are you sure you want to delete all executions?", "Are you sure?", JOptionPane.YES_NO_OPTION);
				if(choice == JOptionPane.YES_OPTION)
					launcher.clearAllConfigurations();
				
			}
		};

		
		
		removeConfiguration = new AbstractAction("Delete config") {
			public void actionPerformed(ActionEvent arg) {

				if(launcher.getConfigurationBeingEdited() == null) return;
				
				int choice = JOptionPane.showConfirmDialog(launcher, "<html>Are you sure you want to delete <i>" + launcher.getConfigurationBeingEdited().getName() + "</i>?</html>", "Are you sure?", JOptionPane.YES_NO_OPTION);
				if(choice == JOptionPane.YES_OPTION)
					launcher.removeConfigurationBeingEdited();

			}
		};
		
		clearClassCache = new AbstractAction("Clear class cache") {
			public void actionPerformed(ActionEvent arg) {

				String[] options = { "Yes, delete the cache!", "Don't delete it!" };
				int answer = JOptionPane.showOptionDialog(launcher, "Are you sure you want to delete the class cache?", "Delete the cache?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, "Don't delete it!");

				if(answer == 0)
					launcher.clearClassCache();

			}
		};
	
		deleteSelectedTrace = new AbstractAction("Delete trace") {
			public void actionPerformed(ActionEvent e) {
				
				Object trace = launcher.getSelectedSavedTrace();
				
				int choice = JOptionPane.showConfirmDialog(launcher, "<html>Are you sure you want to delete <i>" + trace + "</i>?</html>", "Are you sure?", JOptionPane.YES_NO_OPTION);
				if(choice == JOptionPane.YES_OPTION)
					launcher.deleteSelectedTrace();

			}
		};
	
		deleteAllSavedTraces = new AbstractAction("Delete all traces") {
			public void actionPerformed(ActionEvent e) {
				
				int choice = JOptionPane.showConfirmDialog(launcher, "<html>Are you sure you want to delete <i>all of the traces</i>?", "Are you sure?", JOptionPane.YES_NO_OPTION);
				if(choice == JOptionPane.YES_OPTION)
					launcher.deleteAllTraces();

			}
		};
		
		cancelRunning = new AbstractAction("Cancel") {
			public void actionPerformed(ActionEvent e) {

				launcher.cancelCurrentExecution();

			}
		};
		
	}
	
	
}

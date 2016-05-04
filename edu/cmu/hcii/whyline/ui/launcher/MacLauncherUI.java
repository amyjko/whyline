package edu.cmu.hcii.whyline.ui.launcher;

import java.lang.reflect.InvocationTargetException;
import java.util.Vector;

import edu.cmu.hcii.whyline.ui.WhylineUI;

/**
 * @author Andrew J. Ko
 *
 */
public class MacLauncherUI extends LauncherUI {

	@SuppressWarnings("deprecation")
	public MacLauncherUI() throws InterruptedException, InvocationTargetException {
		
		super();
		
		com.apple.mrj.MRJApplicationUtils.registerQuitHandler(new com.apple.mrj.MRJQuitHandler() {
			public void handleQuit() throws IllegalStateException {
				
				saveConfigurations();
				
				Vector<WhylineUI> windows = new Vector<WhylineUI>(openWindows);
				for(WhylineUI whylineUI : windows)
					if(whylineUI.isVisible())
						if(whylineUI.saveIfNecessaryThenClose())
							return;
				System.exit(0);
			}
		});

	}
	
}
package edu.cmu.hcii.whyline.ui;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.TimerTask;

import javax.swing.*;

import edu.cmu.hcii.whyline.util.Util;

/**
 * @author Andrew J. Ko
 *
 */
public class JVMMemoryUI extends JComponent {

	private String label = ""; 
	private int descent;
	private double usedPercent;
	private java.util.Timer timer;

	public JVMMemoryUI() {

		timer = new java.util.Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				
				long free = Runtime.getRuntime().freeMemory();
				long total = Runtime.getRuntime().totalMemory();
				long max = Runtime.getRuntime().maxMemory();
				
				usedPercent = (double)(total - free) / max;

				Graphics2D g = (Graphics2D)getGraphics();
				if(g != null) {
					int usedMB = (int)((total - free) / 1024 / 1024);
					int totalMB = (int)(max / 1024 / 1024);
					label = "" + Util.commas(usedMB) + " / " + Util.commas(totalMB) + " MB";
					repaint();

				}
				
			}
		}, 250, 2500);

		Dimension size = new Dimension(80, (int) (UI.getToolbarHeight() * .6));
		setPreferredSize(size);
		setMaximumSize(size);

	}
	
	private long lastFree = 0;
	
	public void paintComponent(Graphics g) {
		
		((Graphics2D)g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		((Graphics2D)g).setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		Graphics2D g2d = (Graphics2D)g.create();
		
		g2d.setColor(UI.getControlCenterColor());
		g2d.fillRect(0, 0, getWidth(), getHeight() - 1);
		
		g2d.setColor(UI.getControlFrontColor());
		g2d.fillRect(0, 0, (int)(getWidth() * usedPercent), getHeight() - 1);

		g2d.setColor(UI.getControlBorderColor());
		g2d.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

		Font font = UI.getMediumFont().deriveFont(10.0f);

		g.setColor(UI.getControlTextColor());
		g.setFont(font);

		FontMetrics metrics = g.getFontMetrics(font); 
		Rectangle2D bounds = metrics.getStringBounds(label, g);

		int x = (int) ((getWidth() - bounds.getWidth()) / 2);
		int y = getHeight() - (int) ((getHeight() - bounds.getHeight()) / 2) - metrics.getDescent();
		
		g.drawString(label, x, y);
		
	}

	public void dispose() {

		if(timer != null) {
			timer.cancel();
			timer.purge();
			timer = null;
		}
		
	}
	
}

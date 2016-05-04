package edu.cmu.hcii.whyline.ui.arrows;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Line2D;

import edu.cmu.hcii.whyline.qa.Explanation;
import edu.cmu.hcii.whyline.ui.source.*;
import edu.cmu.hcii.whyline.util.Util;

/**
 * @author Andrew J. Ko
 *
 */
public class FileControlArrow extends FileCausalArrow {

	public FileControlArrow(FilesView.ArrowBox arrows, Explanation from, Explanation to) {
		
		super(arrows, null, from, to, Relationship.CONTROL, 0);

		toRange = arrows.getFilesView().getRangeFor(from.getEventID());

	}

	protected void paintSelectedArrow(Graphics2D g, int labelLeft, int labelRight, int labelTop, int labelBottom) {
		
		if(toRange != null && toRange.first != null) {

			Area area = files.getAreaForTokenRange(toRange);
			if(area != null) {
				Rectangle tokens = area.getBounds();

				g.setColor(relationship.getColor(true));

				// Outline the tokens
				files.outline(g, toRange);
			
				// Get the boundaries of the selection.
				int tokenLeft = (int)tokens.getMinX();
				int tokenRight = (int) tokens.getMaxX();
				int tokenTop = (int) tokens.getMinY();
				int tokenBottom = (int) tokens.getMaxY();
				
				int labelX = tokenRight < (labelLeft + labelRight) / 2  ? labelLeft : labelRight;
				int labelY = labelBottom - descent;
				
				Line2D line = Util.getLineBetweenRectangleEdges(
						labelX, labelX + 1,
						labelY, labelY + 1,
						tokenLeft, tokenRight,
						tokenTop, tokenBottom
				);
	
				int xOff = 0;
				int yOff = 0;

				Util.drawQuadraticCurveArrow(g, (int)line.getX1(), (int)line.getY1(), (int)line.getX2(), (int)line.getY2(), xOff, yOff, true, relationship.getStroke(true));

				g.drawLine(labelLeft, labelY, labelRight, labelY);
				
			}
			
		}
		
	}

}
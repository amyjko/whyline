package edu.cmu.hcii.whyline.ui.arrows;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Line2D;

import edu.cmu.hcii.whyline.bytecode.Instruction;
import edu.cmu.hcii.whyline.qa.Explanation;
import edu.cmu.hcii.whyline.ui.source.*;
import edu.cmu.hcii.whyline.util.Util;

/**
 * @author Andrew J. Ko
 *
 */
public class FileDataArrow extends FileCausalArrow {
	
	/**
	 * @param files
	 * @param selection The event that's selected in the visualization.
	 * @param definition The event on which to base the label
	 * @param from Where to draw an arrow from
	 * @param to Where to draw an arrow to
	 * @param dependencyNumber
	 */
	public FileDataArrow(FilesView.ArrowBox arrows, Explanation definition, Explanation from, Explanation to, int dependencyNumber) {
		
		super(arrows, definition, from, to, Relationship.DATA, dependencyNumber);
		
		Instruction consumer = to.getProducerFor(definition);
		if(consumer == null || whylineUI.getTrace().getKind(to.getEventID()).isArtificial)
			toRange = files.getRangeFor(to.getEventID());
		else 
			toRange = consumer.getFile().getTokenRangeFor(consumer);

		// See if the from is in a different file. If it's not, 
		fromRange = files.getRangeFor(from.getEventID());
		if(fromRange == null || fromRange.getFile() == null || (toRange != null && fromRange.first.getLine() == toRange.first.getLine()))
			fromRange = null;
		
	}
		
	protected void paintSelectedArrow(Graphics2D g, int labelLeft, int labelRight, int labelTop, int labelBottom) {
				
		g = (Graphics2D)g.create();
				
		// If this arrow is selected, draw a line from the label to the token range.
		if(to != null) {

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

					if(fromRange != null) {
						
						files.outline(g, fromRange);

						Area fromArea = files.getAreaForTokenRange(fromRange);
						if(fromArea != null) {
							Rectangle fromTokens = fromArea.getBounds();

							int fromTokenLeft = (int)fromTokens.getMinX();
							int fromTokenRight = (int) fromTokens.getMaxX();
							int fromTokenTop = (int) fromTokens.getMinY();
							int fromTokenBottom = (int) fromTokens.getMaxY();

							Line2D line = Util.getLineBetweenRectangleEdges(
									fromTokenLeft, fromTokenRight,
									fromTokenTop, fromTokenBottom,
									tokenLeft, tokenRight,
									tokenTop, tokenBottom
							);
							
							Util.drawQuadraticCurveArrow(g, (int)line.getX1(), (int)line.getY1(), (int)line.getX2(), (int)line.getY2(), 0, 0, true, relationship.getStroke(true));
						}

					}
					else {
																		
						Line2D line = Util.getLineBetweenRectangleEdges(
								labelX, labelX + 1,
								labelY, labelY + 1,
								tokenLeft, tokenRight,
								tokenTop, tokenBottom
						);
			
						int xOff = 0;
						int yOff = 0;
	
						Util.drawQuadraticCurveArrow(g, (int)line.getX1(), (int)line.getY1(), (int)line.getX2(), (int)line.getY2(), xOff, yOff, true, relationship.getStroke(true));
						
					}
					
					g.drawLine(labelLeft, labelY, labelRight, labelY);

				}
				
			}
						
		}

	}
		
}
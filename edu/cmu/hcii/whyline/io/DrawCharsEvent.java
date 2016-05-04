package edu.cmu.hcii.whyline.io;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;

import edu.cmu.hcii.whyline.trace.*;
import edu.cmu.hcii.whyline.util.Util;

/**
 * @author Andrew J. Ko
 *
 */
public final class DrawCharsEvent extends RenderEvent {

	private static final String[] ARGUMENT_NAMES = { "graphics", "characters", "offset", "length", "x", "y"}; 
	public String getArgumentName(int index) { return ARGUMENT_NAMES[index]; }

	private Graphics2D renderContext;
	private GlyphVector glyphs;
	private String string;
	
	public DrawCharsEvent(Trace trace, int eventID) {
		
		super(trace, eventID);
		
	}
	
	private void fillChars(Graphics2D g) {
		
		if(glyphs != null) return;
		
		long charsID = getChars();
		int length = getLength();
		int offset = getOffset();
		StringBuilder builder = new StringBuilder();
		for(int index = 0; index < length; index++) {

			int indexInArray = offset + index;
			
			Object value = trace.getArrayAssignmentHistory().getValueOfIndexAtTime(charsID, indexInArray, getEventID());
			if(value != null)
				builder.append(
					value instanceof Character ? 
						((Character)value).charValue() :
						(char)((Number)value).intValue());
			else 
				builder.append('-');
							
		}

		string = builder.toString();
		glyphs = g.getFont().createGlyphVector(g.getFontRenderContext(), string);
		
	}
	
	public void paint(Graphics2D g) {

//		fillChars(g);
		if(glyphs == null) {
			glyphs = g.getFont().createGlyphVector(g.getFontRenderContext(), Util.fillString('x', getLength()));
		}
		
		if(renderContext == null) renderContext = g;
		
		g.drawGlyphVector(glyphs, getX(), getY());

	}

	protected Shape makeShape() {

		if(glyphs == null) return null;
		Rectangle2D bounds = glyphs.getLogicalBounds();
		int ascent = renderContext.getFontMetrics(glyphs.getFont()).getAscent();

		return glyphs.getOutline(getX() + paintState.getOriginX(), getY() + paintState.getOriginY());
		
	}

	public long getChars() { return getLong(1); }
	public int getOffset() { return getInteger(2); }
	public int getLength() { return getInteger(3); }
	public int getX() { return getInteger(4); }
	public int getY() { return getInteger(5); }
	
	public String getHumanReadableName() { return "characters"; }

	public boolean canOcclude() { return false; }

	public String toString() { return super.toString() + getGraphicsID() + "\tdrawChars " + getX() + " " + getY(); }

}

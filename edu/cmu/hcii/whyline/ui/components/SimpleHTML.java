package edu.cmu.hcii.whyline.ui.components;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.util.Vector;

/**
 * Supports <b>, <em> and <i> and a single line.
 * 
 * @author Andrew J. Ko
 *
 */
public class SimpleHTML {

	private class Span {
		
		private final String text;
		private final GlyphVector vector;
		private final int left, baseline;

		public Span(String text, Font font, FontRenderContext context, int left, int baseline) {

			this.text = text;
			vector = font.createGlyphVector(context, text);
			this.left = left;
			this.baseline = baseline;
			
		}

		public int getWidth() { return (int)vector.getLogicalBounds().getWidth(); }
		
		public void paint(Graphics2D g, int x, int y) {

			g.drawGlyphVector(vector, (x + left), (y + baseline));
			
		}
		
	}

	private Vector<String> tokens;
	private Vector<Span> spans;
	private final Graphics2D context;
	private final Font font;
	private int width, height;
	
	public SimpleHTML(String html, Graphics2D context, Font font) {
		
		this.context = (Graphics2D)context.create();
		this.context.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		this.font = font;
		
		// Tokenize
		tokenize(html);
		
		// Parse the string.
		parse();
		
	}
	
	private void tokenize(String html) {

		tokens = new Vector<String>();
		
		StringBuffer buffer = new StringBuffer();

		char[] characters = html.toCharArray();
		int i = 0;
		while(i < characters.length) {

			char c = characters[i];
			if(c == '<') {

				int open = i;
				i++;
				while(i < characters.length && characters[i] != '>') i++;
				buffer.append(characters, open, i - open + 1);
				i++;
				tokens.add(buffer.toString());
				buffer.delete(0, buffer.length());
				
			}
			else {
				
				int first = i;
				while(i < characters.length && characters[i] != '<')
					i++;

				buffer.append(characters, first, i - first);
				tokens.add(buffer.toString());
				buffer.delete(0, buffer.length());
				
			}
			
		}
		
	}

	private void parse() {
		
		spans = new Vector<Span>();
	
		int left = 0;
		FontMetrics metrics = context.getFontMetrics(font); 
		int baseline = metrics.getAscent();

		int i = 0;
		while(i < tokens.size()) {
			
			String token = tokens.get(i);
			
			if(token.startsWith("<")) {
			
				if(i + 1 >= tokens.size()) System.err.println("Invalid index! " + tokens);

				String middle = tokens.get(i + 1);

				middle = translateText(middle);
				
				if(token.equals("<b>")) {
					
					spans.add(new Span(middle, font.deriveFont(Font.BOLD), context.getFontRenderContext(), left, baseline));
					
				}
				else if(token.equals("<i>") || token.equals("<em>")) {

					spans.add(new Span(middle, font.deriveFont(Font.ITALIC), context.getFontRenderContext(), left, baseline));

				}
				else if(token.equals("<font>")) {

					spans.add(new Span(middle, font, context.getFontRenderContext(), left, baseline));

				}
				else {

					spans.add(new Span(middle, font, context.getFontRenderContext(), left, baseline));
					
				}

				i++;
				i++;
				i++;
				
			}
			else {

				token = translateText(token);
				
				spans.add(new Span(token, font, context.getFontRenderContext(), left, baseline));
				i++;
				
			}
			
			left += spans.lastElement().getWidth();

		}
		
		width = left;
		height = metrics.getHeight();
		
	}
	
	private String translateText(String text) {
		
		text = text.replace("&lt;", "<");
		text = text.replace("&gt;", ">");
		return text;
		
	}
	
	public int getWidth() { return width; }
	public int getHeight() { return height; }
	
	
	public void paint(Graphics2D g, int x, int y) {
		
		for(Span span : spans) {
			
			span.paint(g, x, y);
			
		}
		
	}
	
}

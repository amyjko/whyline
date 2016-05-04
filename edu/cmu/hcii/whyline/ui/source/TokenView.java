package edu.cmu.hcii.whyline.ui.source;

import java.awt.*;
import java.awt.font.GlyphVector;

import edu.cmu.hcii.whyline.qa.SourceFileMenuFactory;
import edu.cmu.hcii.whyline.source.*;

import static edu.cmu.hcii.whyline.source.JavaParserConstants.*;

import edu.cmu.hcii.whyline.ui.UI;
import edu.cmu.hcii.whyline.ui.WhylineUI;
import edu.cmu.hcii.whyline.ui.components.WhylinePopup;
import edu.cmu.hcii.whyline.ui.events.LoggedAction;
import edu.cmu.hcii.whyline.ui.io.BreakpointDebugger;
import edu.cmu.hcii.whyline.ui.views.View;

/**
 * @author Andrew J. Ko
 *
 */
public final class TokenView extends View implements Comparable<TokenView> {

	private final LineView lineView;
	private final Token token;
	private final GlyphVector glyphs;
	
	public TokenView(LineView lineView, Token token, GlyphVector glyphs) {

		super();
		
		this.token = token;
		this.lineView = lineView;
		this.glyphs = glyphs;
				
	}

	public void setEmphasized(Color color) {

		lineView.getFileView().emphasizeToken(this, color);
		
	}
	
	public Color getEmphasizedColor() { return lineView.getFileView().getEmphasizedTokenColor(this); }
	
	public LineView getLineView() { return lineView; }
	
	public Token getToken() { return token; }
	
	public FileInterface getFile() { return token.getLine().getFile(); }
	public JavaSourceFile getJavaFile() { return (JavaSourceFile)getFile(); }
	
	public WhylineUI getWhylineUI() { return lineView.getFileView().getWhylineUI(); }
		
	public void paintBelowChildren(Graphics2D g) {
				
		if(getWhylineUI().getMode() == WhylineUI.Mode.BREAKPOINT) {

			BreakpointDebugger debugger = getWhylineUI().getBreakpointDebugger();
			if(debugger.hasPrint(token)) {
				int left= (int)getVisibleLocalLeft(), top = (int)getVisibleLocalTop(), width = (int)getVisibleLocalWidth(), height = (int)getVisibleLocalHeight(); 
				g.setColor(UI.BREAKPOINT_COLOR);
				int padding = 3;
				g.fillRect(left - padding, top - padding, width  + padding * 2, height + padding * 2);
			}

		}
		
		if(representsJavaIdentifier()) {
			
			if(getJavaFile().getInstructionFor(token) != null) {
			
				g.setColor(UI.IDENTIFIER_COLOR);
				int y = (int)(getLocalTop() + lineView.getAscent() + 1);
				g.setStroke(UI.INTERACTIVE_UNDERLINE_STROKE);
				g.drawLine((int)getLocalLeft(), y, (int)getLocalRight() - 1, y);
				
			}
			
		}

		// We don't make glyphs for whitespace only tokens.
		if(glyphs != null) {
			g.setColor(getColor(token.getKind()));
			g.drawGlyphVector(glyphs, (int)getVisibleLocalLeft(), (int)(getVisibleLocalTop() + lineView.getAscent()));
		}

	}
	
	public void paintAboveChildren(Graphics2D g) {
		
		if(lineView.getFileView().getTokenUnderMouse() == this) {
			
			int padding = 2;

			g.setStroke(UI.SELECTED_STROKE);
			g.setColor(UI.getHighlightColor());
			g.drawRoundRect(
					(int)getVisibleLocalLeft() - padding, (int)getVisibleLocalTop() - padding, 
					(int)getVisibleLocalWidth() + padding * 2 - 1, (int)getVisibleLocalHeight() + padding * 2 - 1, 
					5, 5);
			
		}

	}
	
	public boolean handleMouseClick(int x, int y, int button) {

		if(lineView.getFileView().getTokenUnderMouse() != this) return false;
		else if(!representsJavaIdentifier()) return false;
		else if(!(getFile() instanceof JavaSourceFile)) return false;
		
		LoggedAction action = null;
		WhylinePopup popup = new WhylinePopup("");

		SourceFileMenuFactory.addQuestionsForToken(getWhylineUI(), popup, token);
		SourceFileMenuFactory.addQuestionsForLine(getWhylineUI(), popup, getLineView().getLine());
		SourceFileMenuFactory.addQuestionsForMethod(getWhylineUI(), popup, getLineView().getLine());
		
		return getContainer().showPopup(popup, (int)getGlobalLeft() + (int)(x - getLocalLeft()), (int)getGlobalTop() + (int)(y - getLocalTop()));
		
	}
	
	public boolean representsJavaIdentifier() { return (token.kind == JavaParserConstants.IDENTIFIER || token.kind == JavaParserConstants.THIS) && getFile() instanceof JavaSourceFile; }

	public int compareTo(TokenView tokenView) {

		return token.compareTo(tokenView.token);
	
	}
	
	public String toString() {
		
		return token.toString();
		
	}
	
	public static Color getColor(int kind) {
		
		switch(kind) {
		
		case SINGLE_LINE_COMMENT :
		case MULTI_LINE_COMMENT :
			
			return UI.COMMENT_COLOR;
		
		case BYTE : 
		case SHORT :
		case CHAR :
		case INT :
		case LONG :
		case FLOAT :
		case DOUBLE :
		case BOOLEAN  :
			
			return UI.KEYWORD_COLOR;
		  
		case NULL : 
		case INTEGER_LITERAL :
		case DECIMAL_LITERAL :
		case HEX_LITERAL :
		case OCTAL_LITERAL :
		case FLOATING_POINT_LITERAL :
		case DECIMAL_FLOATING_POINT_LITERAL :
		case HEXADECIMAL_FLOATING_POINT_LITERAL :
		case CHARACTER_LITERAL :
		case STRING_LITERAL :
		case TRUE :
		case FALSE :

			return UI.LITERAL_COLOR;
			
	  case LT:
	  case BANG:
	  case TILDE:
	  case HOOK:
	  case COLON:
	  case EQ:
	  case LE:
	  case GE:
	  case NE:
	  case SC_OR:
	  case SC_AND:
	  case INCR:
	  case DECR:
	  case PLUS:
	  case MINUS:
	  case STAR:
	  case SLASH:
	  case BIT_AND:
	  case BIT_OR:
	  case XOR:
	  case REM:
	  case LSHIFT:
	  case PLUSASSIGN:
	  case MINUSASSIGN:
	  case STARASSIGN:
	  case SLASHASSIGN:
	  case ANDASSIGN:
	  case ORASSIGN:
	  case XORASSIGN:
	  case REMASSIGN:
	  case LSHIFTASSIGN:
	  case RSIGNEDSHIFTASSIGN:
	  case RUNSIGNEDSHIFTASSIGN:
	  case RUNSIGNEDSHIFT:
	  case RSIGNEDSHIFT:
	  case GT:

		  return UI.OPERATOR_COLOR;
		  
	  case LPAREN :
	  case RPAREN :
	  case LBRACE :
	  case RBRACE :
	  case LBRACKET :
	  case RBRACKET :
	  case SEMICOLON :
	  case COMMA : 

		  return UI.DELIMITER_COLOR;

	  case IDENTIFIER :
		  
		  return UI.IDENTIFIER_COLOR;
		  
	 default :
			 
		 return UI.KEYWORD_COLOR;

		}
		
	}
	
}
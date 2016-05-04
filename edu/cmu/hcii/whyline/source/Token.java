package edu.cmu.hcii.whyline.source;

import edu.cmu.hcii.whyline.bytecode.MethodInfo;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class Token implements Comparable<Token>, JavaParserConstants {

	private int indexOnLine = -1;
	private final Line line;
	private String token;
	public final int kind;

	public Token(Line line, String token, int kind) {
		
		this.line = line;
		this.token = token.intern();
		this.kind = kind;
		
	}

	public Line getLine() { return line; }
	
	/**
	 * A constant from @see JavaParserConstants.
	 */
	public int getKind() { return kind; }
	
	public String getText() { return token; }
	
	public void setText(String text) { this.token = text; }

	public FileInterface getFile() { return line.getFile(); }

	public Token getNext() { 
		
		Token next = line.getTokenAfter(this);
		if(next != null) return next;
		
		try {
			Line nextLine = line.getLineAfter();
			if(nextLine != null) return nextLine.getFirstToken();
		} catch (ParseException e) {}
		return null;
		
	}

	public Token getNextCodeToken() throws ParseException { return line.getFile().getCodeTokenAfter(this);  }

	public Token getPreviousCodeToken() throws ParseException { return line.getFile().getCodeTokenBefore(this);  }

	public void setLineIndex(int index) { indexOnLine = index; }
		
	public int getLineIndex() { return indexOnLine; }
	
	public boolean isWhitespace() { return kind == JavaParserConstants.WHITESPACE; }
	
	public boolean isComment() { return kind == JavaParserConstants.SINGLE_LINE_COMMENT || kind == JavaParserConstants.MULTI_LINE_COMMENT; }
	
	public boolean isCode() { return !isWhitespaceOrComment(); }
	
	public boolean isWhitespaceOrComment() { return isWhitespace() || isComment(); }
		
	public LineNumber getLineNumber() { return line.getLineNumber(); }
	
	public MethodInfo getMethod() { return line.getMethod(); }
	
	public boolean isAfter(Token t) {
		
		assert line.getFile() == t.getLine().getFile() : "Can't compare tokens in different files.";
		
		if(line.getLineNumber().is(t.getLineNumber())) return indexOnLine > t.indexOnLine;
		else return line.getLineNumber().isAfter(t.getLineNumber());
		
	}

	public boolean isIdentifier() { return kind == IDENTIFIER; }

	public boolean isPrimitive() {

		switch(kind) {
		case BYTE : 
		case SHORT :
		case CHAR :
		case INT :
		case LONG :
		case FLOAT :
		case DOUBLE :
		case BOOLEAN  :
			return true;
		default:
			return false;
		}

	}

	public boolean isLiteral() {
		  
		switch(kind) {
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
			return true;
		default :
			return false;
		}
			  
	}

	public boolean isTestOperator() {
		
		switch(kind) {
		  case LT:
		  case EQ:
		  case LE:
		  case GE:
		  case NE:
		  case GT:
			return true;
		default :
			return false;
		}

	}
	
	public boolean isOperator() {
		  
		switch(kind) {
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
			return true;
		default :
			return false;
		}
			  
	}

	public boolean isDelimiter() {
		
		switch(kind) {
		  case LPAREN :
		  case RPAREN :
		  case LBRACE :
		  case RBRACE :
		  case LBRACKET :
		  case RBRACKET :
		  case SEMICOLON :
		  case COMMA : 
			  return true;
		  default : 
			return false;
		}
		
	}
	
	public boolean isModifier() {
		
		switch(kind) {
		case PUBLIC :
		case PROTECTED :
		case PRIVATE :
		case STATIC :
		case ABSTRACT :
		case FINAL :
		case NATIVE :
		case SYNCHRONIZED:
		case TRANSIENT :
		case VOLATILE :
		case STRICTFP:
			return true;
		default:
			return false;
		}
		
	}
	
	public String toString() { return token; }
	
	public static class PairedToken extends Token {
		
		private Token.PairedToken associatedToken;

		public PairedToken(Line line, String token, int kind) {
			
			super(line, token,  kind);
			
		}
		
		public void setAssociatedToken(Token.PairedToken token) { this.associatedToken = token; }

		public Token.PairedToken getAssociatedToken() { return associatedToken; }

		public boolean isLeft() { return kind == LBRACE || kind == LBRACKET || kind == LPAREN || kind == LT; } 
		
	}
	
	public static class GreaterThanToken extends Token {
		
		public final int realKind;

		public GreaterThanToken(Line line, String token, int kind, int realKind) {
			super(line, token, kind);
			this.realKind = realKind;
		}
		
	}

	public int compareTo(Token token) {
		
		if(line.getFile() == token.line.getFile()) {
		
			LineNumber thisLine = line.getLineNumber(), thatLine = token.line.getLineNumber();
			
			if(thisLine == thatLine) return indexOnLine - token.indexOnLine;
			else return thisLine.compareTo(thatLine);
			
		}
		else return line.getFile().compareTo(token.line.getFile());
		
	}
	
}
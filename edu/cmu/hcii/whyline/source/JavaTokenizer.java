package edu.cmu.hcii.whyline.source;

import java.io.EOFException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import edu.cmu.hcii.whyline.source.PeekReader;
import edu.cmu.hcii.whyline.source.Token.PairedToken;
import static edu.cmu.hcii.whyline.source.JavaParserConstants.*;
import gnu.trove.TObjectIntHashMap;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public final class JavaTokenizer {

	private final JavaSourceFile source;
	
	private final PeekReader reader;

	private final ArrayList<Line> lines = new ArrayList<Line>();
	private final ArrayList<Token> identifiers = new ArrayList<Token>();
	private final ArrayList<Token> code = new ArrayList<Token>();
	private final Map<Token,String> javadocs = new HashMap<Token,String>();
	private final TObjectIntHashMap<Line> codeTokenIndicesByLine = new TObjectIntHashMap<Line>(50);

	private Line currentLine;
	private boolean currentLineIndents = false;
	private Stack<Token> indentationTokens = new Stack<Token>();
	private boolean currentLineHasBrace = false;
	private boolean currentLineHasSemiColon = false;
	private boolean currentLineHasCode = false;
	private boolean currentLineHasColon = false;
	private boolean waitingForSemiColons = false;
	
	public JavaTokenizer(JavaSourceFile source, byte[] bytes) throws ParseException {
		
		this.source = source;
		reader = new PeekReader(bytes);
		tokenize();
		
	}

	public TObjectIntHashMap<Line> getCodeTokenIndiciesByLine() { return codeTokenIndicesByLine; }

	public Line[] getLines() { 

		Line[] array = new Line[lines.size()];
		lines.toArray(array);
		return array; 
		
	}
	
	public Token[] getIdentifiers() { 

		Token[] array = new Token[identifiers.size()];
		identifiers.toArray(array);
		return array; 
		
	}

	public Token[] getCode() { 

		Token[] array = new Token[code.size()];
		code.toArray(array);
		return array; 
		
	}
	
	public Map<Token, String> getJavaDocsByToken() { return javadocs; }
	
	private void tokenize() throws ParseException {
		
		codeTokenIndicesByLine.clear();
		
		currentLine = new Line(new LineNumber(source, 1));
		
		Stack<Token.PairedToken> tokenStack = new Stack<Token.PairedToken>();

		try {

			while(true) {
				
				// Read whitespace, comments, and documentation
				String javadoc = readWhitespace();
				
				// Now that we've read through all of the whitespace, comments, and new lines, let's read some tokens.
				Token newToken = readCodeToken();
				
				assert newToken != null : 
					"Somehow, we didn't construct a token from the next character, which was '" + reader.next() + "'" + ".\n" +
					"The file was " + source.getFileName() + "\n" +
					"The context was\n\n" + currentLine;

				currentLineHasCode = true;

				int kind = newToken.kind;
				
				// Associate the javadoc and token
				if(javadoc != null)
					javadocs.put(newToken, javadoc);
				
				// Append the new token to the current line.
				currentLine.addToken(newToken);

				// Add the token to the list of "code" tokens that don't include whitespace.
				code.add(newToken);

				switch(kind) {
					// If its an identifier, keep track of it to help searching.
					case IDENTIFIER :
					case THIS :
						identifiers.add(newToken);											
						break;
					case LBRACE :
					case RBRACE :
						currentLineHasBrace = true;
						break;
					case SEMICOLON :
						currentLineHasSemiColon = true;
						break;
					case COLON :
						currentLineHasColon = true;
						break;
				}

				// Paired delimiter handling
				switch(kind) {
					case LPAREN :
					case LBRACKET :
					case LBRACE :
						tokenStack.push((Token.PairedToken)newToken);
						break;
					case RPAREN :
					case RBRACKET :
					case RBRACE :
						PairedToken open = tokenStack.pop();
						if((open.kind == LPAREN && newToken.kind != RPAREN) ||
							 (open.kind == LBRACKET && newToken.kind != RBRACKET) ||
							 (open.kind == LBRACE && newToken.kind != RBRACE)) {
							
							throw new ParseException("\n\nOn" + newToken.getLineNumber() + "\n\tCan't match " + open.getLineNumber() + " " + open + " with "  + newToken.getLineNumber() + " " + newToken, null);
							
						}
						open.setAssociatedToken(((PairedToken)newToken));
						((PairedToken)newToken).setAssociatedToken(open);
						break;
				}
				
				// Managing indentation level
				switch(getIndentationChangeFor(kind)) {
					case 1:
						indentationTokens.push(newToken); 
						break;
					case -1 :
						indentationTokens.pop();
						break;
				}
				
				// If this is the first token of this line, remember the first index of the line.
				if(!codeTokenIndicesByLine.containsKey(currentLine))
					codeTokenIndicesByLine.put(currentLine, code.size() - 1);

			}
			
		} 
		catch(EOFException ex) {}

		nextLine();
		
		if(tokenStack.size() > 0) {
			System.err.println("Warning: parens, brackets, and braces didn't match up. Here's the stack:");
			for(Token t : tokenStack) {
				
				System.err.println("\t'" + t.getText() + "\"");
				
			}
		}

		codeTokenIndicesByLine.trimToSize();
		
	}

	private int getIndentationChangeFor(int kind) {
		
		switch(kind) {
		case LBRACE :
			return 1;
		case RBRACE :
			return -1;
		case CASE :			
		default :
			return 0;
		}
		
	}
		
	private Token readCodeToken() throws EOFException {

		// Annotation
		if(reader.nextIs('@')) {
			
			reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), AT);
			
		}
		// String
		else if(reader.nextIs('"')) {
			
			reader.next();
			while(true) {
				if(reader.nextIs('\\')) {
					reader.next();
					reader.next();
				}
				else if(reader.nextIs('"')) break;
				else reader.next();
				
			}
			reader.next();
			
			return new Token(currentLine, reader.eraseAccumulation(), STRING_LITERAL);
			
		}
		// Character 
		else if(reader.nextIs('\'')) {

			// Read the open '
			reader.next();
			// Is an escape next? (java spec 3.10.6)
			if(reader.nextIs('\\')) {
				reader.next();
				char next = reader.peekAtNext();
				switch(next) {
				case 'b' :
				case 't' :
				case 'n' :
				case 'f' :
				case 'r' :
				case '\'' :
				case '\"' :
				case '\\' :
					reader.next();
					break;
				// Octal escape
				case '0':
				case '1':
				case '2':
				case '3':
				case '4':
				case '5':
				case '6':
				case '7':
					reader.next();
					next = reader.peekAtNext();
					if(Character.isDigit(next) && next != '8' && next != '9') {
						reader.next();
						next = reader.peekAtNext();
						if(Character.isDigit(next) && next != '8' && next != '9') {
							reader.next();
						}
					}
					break;
				}
			}
			// Otherwise, just read the character
			else reader.next();
			
			// Read the close '
			reader.next();

			return new Token(currentLine, reader.eraseAccumulation(), CHARACTER_LITERAL);
			
		}
		// Integer
		// Floating point
		else if(Character.isDigit(reader.peekAtNext())) {
			
			reader.next();
			while(Character.isDigit(reader.peekAtNext()) || reader.nextIs('.') || Character.isLetter(reader.peekAtNext())) reader.next();
			String number = reader.eraseAccumulation();
			return new Token(currentLine, number, number.contains(".") ? FLOATING_POINT_LITERAL : INTEGER_LITERAL);
			
		}
		// Group Separators
		else if(reader.nextIs('(')) {

				reader.next();
				return new Token.PairedToken(currentLine, reader.eraseAccumulation(), LPAREN);
				
			}	
		else if(reader.nextIs(')')) {

				reader.next();
				return new Token.PairedToken(currentLine, reader.eraseAccumulation(), RPAREN);
				
			}	
		else if(reader.nextIs('[')) {

				reader.next();
				return new Token.PairedToken(currentLine, reader.eraseAccumulation(), LBRACKET);
				
			}	
		else if(reader.nextIs(']')) {

				reader.next();
				return new Token.PairedToken(currentLine, reader.eraseAccumulation(), RBRACKET);
				
			}	
		else if(reader.nextIs('{')) {

				reader.next();
				return new Token.PairedToken(currentLine, reader.eraseAccumulation(), LBRACE);
				
			}	
		else if(reader.nextIs('}')) {

				reader.next();
				return new Token.PairedToken(currentLine, reader.eraseAccumulation(), RBRACE);
				
			}	
		//
		else if(reader.nextIs('.', '.', '.')) {
			
			reader.next();
			reader.next();
			reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), ELLIPSIS);
			
		}
		else if(reader.nextIs('.')) {

			reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), DOT);
			
		}	
		else if(reader.nextIs(';')) {

			reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), SEMICOLON);
			
		}	
		else if(reader.nextIs(',')) {

			reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), COMMA);
			
		}	
		// Identifier, Keywords, Null, Boolean
		else if(Character.isJavaIdentifierStart(reader.peekAtNext())) {
			
			reader.next();
			while(Character.isJavaIdentifierPart(reader.peekAtNext())) reader.next();

			String text = reader.eraseAccumulation();
			int type = getTypeOfIdentifier(text);
			return new Token(currentLine, text, type);
						
		}
		// Four character operators
		else if(reader.nextIs('>', '>', '>', '=')) {

			reader.next();
			reader.next();
			reader.next();
			reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), RUNSIGNEDSHIFTASSIGN);

		}
		// Three character operators
		else if(reader.nextIs('>', '>', '=')) {

			reader.next();
			reader.next();
			reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), RSIGNEDSHIFTASSIGN);

		}
		else if(reader.nextIs('<', '<', '=')) {

			reader.next();
			reader.next();
			reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), LSHIFTASSIGN);

		}
//		 We don't match on these; they're tokenized individually.
//		else if(reader.nextIs('>', '>', '>')) {
//
//			reader.next();
//			reader.next();
//			reader.next();
//			return new Token(line, reader.eraseAccumulation(), "", RUNSIGNEDSHIFT);
//
//		}
//		 We don't match on these; they're tokenized individually.
//		else if(reader.nextIs('>', '>')) {
//				
//				reader.next(); reader.next();
//				return new Token(line, reader.eraseAccumulation(), "", RSIGNEDSHIFT);
//				
//		}
		else if(reader.nextIs('=', '=')) {
			
			reader.next();
			reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), EQ);
			
		}
		else if(reader.nextIs('<', '=')) {
			
			reader.next();
			reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), LE);
			
		}
		else if(reader.nextIs('>', '=')) {
			
			reader.next();
			reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), GE);
			
		}
		else if(reader.nextIs('!', '=')) {
			
			reader.next();
			reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), NE);
			
		}
		// Two character computational operators
		else if(reader.nextIs('&', '&')) {
			
			reader.next(); reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), SC_AND);
			
		}
		else if(reader.nextIs('|', '|')) {
				
				reader.next(); reader.next();
				return new Token(currentLine, reader.eraseAccumulation(), SC_OR);
				
		}
		else if(reader.nextIs('<', '<')) {
				
				reader.next(); reader.next();
				return new Token(currentLine, reader.eraseAccumulation(), LSHIFT);
				
		}
		else if(reader.nextIs('+', '+')) {
			
			reader.next(); reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), INCR);
			
		}
		else if(reader.nextIs('-', '-')) {
			
			reader.next(); reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), DECR);
			
		}
		else if(reader.nextIs('+', '=')) {
			
			reader.next(); reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), PLUSASSIGN);
			
		}
		else if(reader.nextIs('-', '=')) {
			
			reader.next(); reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), MINUSASSIGN);
			
		}
		else if(reader.nextIs('*', '=')) {
			
			reader.next(); reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), STARASSIGN);
			
		}
		else if(reader.nextIs('/', '=')) {
			
			reader.next(); reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), SLASHASSIGN);
			
		}
		else if(reader.nextIs('&', '=')) {
			
			reader.next(); reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), ANDASSIGN);
			
		}
		else if(reader.nextIs('|', '=')) {
			
			reader.next(); reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), ORASSIGN);
			
		}
		else if(reader.nextIs('^', '=')) {
			
			reader.next(); reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), XORASSIGN);
			
		}
		else if(reader.nextIs('%', '=')) {
			
			reader.next(); reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), REMASSIGN);
			
		}
		else if(reader.nextIs('%', '=')) {
			
			reader.next(); reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), REMASSIGN);
			
		}
		else if(reader.nextIs('%', '=')) {
			
			reader.next(); reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), REMASSIGN);
			
		}
		else if(reader.nextIs('%', '=')) {
			
			reader.next(); reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), REMASSIGN);
			
		}
		else if(reader.nextIs('%', '=')) {
			
			reader.next(); reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), REMASSIGN);
			
		}
		else if(reader.nextIs('%', '=')) {
			
			reader.next(); reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), REMASSIGN);
			
		}
		else if(reader.nextIs('%', '=')) {
			
			reader.next(); reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), REMASSIGN);
			
		}
		else if(reader.nextIs('%', '=')) {
			
			reader.next(); reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), REMASSIGN);
			
		}
		else if(reader.nextIs('%', '=')) {
			
			reader.next(); reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), REMASSIGN);
			
		}
		else if(reader.nextIs('%', '=')) {
			
			reader.next(); reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), REMASSIGN);
			
		}
		else if(reader.nextIs('>')) {
			
			reader.next();
			return new Token.GreaterThanToken(currentLine, reader.eraseAccumulation(), GT,
					reader.nextIs('>', '>') ? RSIGNEDSHIFT :
					reader.nextIs('>', '>', '>') ? RUNSIGNEDSHIFT : GT);
			
		}
		else if(reader.nextIs('<')) {
			
			reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), LT);
			
		}
		else if(reader.nextIs(':')) {
			
			reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), COLON);
			
		}
		else if(reader.nextIs('?')) {
			
			reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), HOOK);
			
		}
		// One character computational operators
		else if(reader.nextIs('!')) {
			
			reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), BANG);
			
		}
		else if(reader.nextIs('~')) {
			
			reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), TILDE);
			
		}
		else if(reader.nextIs('+')) {
			
			reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), PLUS);
			
		}
		else if(reader.nextIs('-')) {
			
			reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), MINUS);
			
		}
		else if(reader.nextIs('&')) {
			
			reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), BIT_AND);
			
		}
		else if(reader.nextIs('|')) {
			
			reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), BIT_OR);
			
		}
		else if(reader.nextIs('^')) {
			
			reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), XOR);
			
		}
		else if(reader.nextIs('*')) {
			
			reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), STAR);
			
		}
		else if(reader.nextIs('/')) {
			
			reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), SLASH);
			
		}
		else if(reader.nextIs('%')) {
			
			reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), REM);
			
		}
		// One character definition operators
		else if(reader.nextIs('=')) {

			reader.next();
			return new Token(currentLine, reader.eraseAccumulation(), ASSIGN);
			
		}	
		else throw new RuntimeException("Unknown type of Java token: " + reader.eraseAccumulation());
		
	}
	
	private String readWhitespace() throws EOFException {
		
		String javadoc = null;
		
		// While the next sequence of characters is java whitespace or a comment prefix, read!
		while(reader.nextIs('/', '/') || reader.nextIs('/', '*') || reader.nextIs('\u000C') || reader.nextIs(' ') || reader.nextIs('\t') || reader.nextIs('\n') || reader.nextIs('\r')) {

			// Read a single line comment.
			if(reader.nextIs('/', '/')) {

				reader.next();
				reader.next();
				while(!reader.nextIs('\n') && !reader.nextIs('\r')) reader.next();

				currentLine.addToken(new Token(currentLine, reader.eraseAccumulation(), SINGLE_LINE_COMMENT));
				
			}
			// Read a sequence of multi line comments.
			else if(reader.nextIs('/', '*')) {

				StringBuilder javadocBuilder = new StringBuilder();
				
				reader.next();
				reader.next();
				while(!reader.nextIs('*', '/')) {

					if(reader.nextIs('\r') || reader.nextIs('\n')) {

						if(reader.nextIs('\r', '\n')) { reader.next(); reader.next(); }
						else reader.next();

						String multiLineComment = reader.eraseAccumulation();
						currentLine.addToken(new Token(currentLine, multiLineComment, MULTI_LINE_COMMENT));

						nextLine();

						javadocBuilder.append(multiLineComment);

					}
					else reader.next();
					
				}
				
				// Read the closing brackets.
				reader.next();
				reader.next();
				
				String multiLineComment = reader.eraseAccumulation();
				Token token = new Token(currentLine, multiLineComment, MULTI_LINE_COMMENT);
				currentLine.addToken(token);

				javadocBuilder.append(multiLineComment);
				
				String doc = javadocBuilder.toString();
				if(doc.trim().startsWith("/**"))
					javadoc = doc;

			}
			// Read whitespace and tabs until reaching something else.
			else if(reader.nextIs('\u000C') || reader.nextIs(' ') || reader.nextIs('\t')) {
				
				while(reader.nextIs('\u000C') || reader.nextIs(' ') || reader.nextIs('\t')) reader.next();
				String whitespace = reader.eraseAccumulation();
				currentLine.addToken(new Token(currentLine, whitespace, WHITESPACE));
				
			}
			// Read a new line, and optinally, the extra carriage return.
			else if(reader.nextIs('\r') || reader.nextIs('\n')) {
				
				reader.next();
				if(reader.lastCharReturned() == '\r' && reader.nextIs('\n')) reader.next();

				nextLine();

				reader.eraseAccumulation();

			}
			
		}
		
		return javadoc;
		
	}
	
	private void nextLine() {

		// Manipulate indentation of the current line.
		if(waitingForSemiColons && currentLineHasBrace) {
				indentationTokens.pop();
				waitingForSemiColons = false;
		}

		// Are there any unmatched indentation tokens on this line?
		boolean currentLineIndents = indentationTokens.size() > 0 && indentationTokens.peek().getLine() == currentLine;
		
		// Indent and add the current line.
		currentLine.trim();
		format(currentLine, indentationTokens.size() - (currentLineIndents ? 1 : 0));
		lines.add(currentLine);

		// Post process indentation attributes
		if(currentLineHasCode && !currentLineHasBrace && !currentLineHasColon && !currentLineHasSemiColon) {
			// We only indent once if waiting for a semi colon.
			if(!waitingForSemiColons) {
				indentationTokens.push(currentLine.getToken(0));
				waitingForSemiColons = true;
			}
		}
		else if(waitingForSemiColons && currentLineHasSemiColon) {
			indentationTokens.pop();
			waitingForSemiColons = false;
		}

		// Make a new line.
		currentLine = new Line(new LineNumber(source, lines.size() + 1));		

		// Reset all of the current line attributes.
		currentLineHasBrace = false;
		currentLineHasSemiColon = false;
		currentLineHasCode = false;
		currentLineHasColon = false;
		
	}
	
	private void format(Line line, int depth) {

		Token leadingWhitespace = line.getToken(0);
		if(leadingWhitespace != null) {

			String text = leadingWhitespace.getText();
			int length = text.length();
			int index = 0;
			for(;index < length; index++) {
				char c = text.charAt(index); 
				if(c != ' ' && c != '\t')
					break;
			}
			
			String rest = text.substring(index);			
			StringBuilder formatted = new StringBuilder();
			for(int i = 0; i < depth; i++) formatted.append('\t');
			formatted.append(rest);
			leadingWhitespace.setText(formatted.toString());
		
		}
		
	}	

	// This is separated from tokenize() so that I can optimize this.
	private static final int getTypeOfIdentifier(String text) {
		
		final int type;
		
		char firstChar = text.charAt(0);

		switch(firstChar) {

		case 'a':
			if(text.equals("abstract")) return ABSTRACT;
			else if(text.equals("assert")) return ASSERT;
			break;
		case 'b':
			if(text.equals("boolean")) return BOOLEAN;
			else if(text.equals("break")) return BREAK;
			else if(text.equals("byte")) return BYTE;
			break;
		case 'c':
			if(text.equals("case")) return CASE;
			else if(text.equals("catch")) return CATCH;
			else if(text.equals("char")) return CHAR;
			else if(text.equals("class")) return CLASS;
			else if(text.equals("const")) return CONST;
			else if(text.equals("continue")) return CONTINUE;
			break;
		case 'd':
			if(text.equals("default")) return DEFAULT;
			else if(text.equals("do")) return DO;
			else if(text.equals("double")) return DOUBLE;
			break;
		case 'e':
			if(text.equals("else")) return ELSE;
			else if(text.equals("extends")) return EXTENDS;
			break;
		case 'f':
			if(text.equals("false")) return FALSE;
			else if(text.equals("final")) return FINAL;
			else if(text.equals("finally")) return FINALLY;
			else if(text.equals("float")) return FLOAT;
			else if(text.equals("for")) return FOR;
			break;
		case 'g':
			if(text.equals("goto")) return GOTO;
			break;
		case 'h':
			break;
		case 'i':
			if(text.equals("if")) return IF;
			else if(text.equals("implements")) return IMPLEMENTS;
			else if(text.equals("import")) return IMPORT;
			else if(text.equals("instanceof")) return INSTANCEOF;
			else if(text.equals("int")) return INT;
			else if(text.equals("interface")) return INTERFACE;
			break;
		case 'j':
			break;
		case 'k':
			break;
		case 'l':
			if(text.equals("long")) return LONG;
			break;
		case 'm':
			break;
		case 'n':
			if(text.equals("null")) return NULL;
			else if(text.equals("native")) return NATIVE;
			else if(text.equals("new")) return NEW;
			break;
		case 'o':
			break;
		case 'p':
			if(text.equals("package")) return PACKAGE;
			else if(text.equals("private")) return PRIVATE;
			else if(text.equals("protected")) return PROTECTED;
			else if(text.equals("public")) return PUBLIC;
			break;
		case 'q':
			break;
		case 'r':
			if(text.equals("return")) return RETURN;
			break;
		case 's':
			if(text.equals("short")) return SHORT;
			else if(text.equals("static")) return STATIC;
			else if(text.equals("strictfp")) return STRICTFP;
			else if(text.equals("super")) return SUPER;
			else if(text.equals("switch")) return SWITCH;
			else if(text.equals("synchronized")) return SYNCHRONIZED;
			break;
		case 't':
			if(text.equals("true")) return TRUE;
			else if(text.equals("this")) return THIS;
			else if(text.equals("throw")) return THROW;
			else if(text.equals("throws")) return THROWS;
			else if(text.equals("transient")) return TRANSIENT;
			else if(text.equals("try")) return TRY;
			break;
		case 'u':
			break;
		case 'v':
			if(text.equals("void")) return VOID;
			else if(text.equals("volatile")) return VOLATILE;
			break;
		case 'w':
			if(text.equals("while")) return WHILE;
			break;
		case 'y':
			break;
		case 'x':
			break;
		case 'z':
			break;
		default:
			break;
		}
		
		return IDENTIFIER;
		
	}

}

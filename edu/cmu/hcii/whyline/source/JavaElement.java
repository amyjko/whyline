package edu.cmu.hcii.whyline.source;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import edu.cmu.hcii.whyline.source.JavaParser.TokenIterator;
import edu.cmu.hcii.whyline.source.Token.PairedToken;
import static edu.cmu.hcii.whyline.source.JavaParserConstants.*;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public abstract class JavaElement {

	protected final JavaElement parent;
	protected final Token firstToken;
	protected final Token lastToken;
	private boolean parsed = false;
	
	public JavaElement(JavaElement parent, Token first, Token last) {

		this.parent = parent;
		assert first != null;
		assert last != null;
		this.firstToken = first;
		this.lastToken = last;
		
	}
	
	public JavaElement getParent() { return parent; }
	
	public ClassElement getEnclosingClass() { return parent == null ? null : parent.getEnclosingClass(); }

	public FileElement getRoot() { return parent.getRoot(); }
	
	public JavaSourceFile getSource() { return parent.getSource(); }
	
	public final Token getFirstToken() { return firstToken; }
	public final Token getLastToken() { return lastToken; }
	
	public boolean contains(Token t) {
		
		if(t.getFile() != getSource()) return false;
		
		Line startLine = firstToken.getLine();
		Line endLine = lastToken.getLine();
		Line givenLine = t.getLine();
		
		if(startLine.getLineNumber().getNumber() > givenLine.getLineNumber().getNumber()) return false;
		if(endLine.getLineNumber().getNumber() < givenLine.getLineNumber().getNumber()) return false;

		// If we made it this far, the token is definitely within line range. Is it at the right place on the line?
		
		if(givenLine == startLine && firstToken.getLineIndex() > t.getLineIndex()) return false;
		if(givenLine == endLine && lastToken.getLineIndex() < t.getLineIndex()) return false;
		
		return true;
		
	}
	
	protected TokenIterator getIterator() throws ParseException { return getSource().getTokenIterator(firstToken, lastToken); }
	
	public final void parse() {
		
		if(parsed) return;
		parsed = true;
		
		try {
			parse(getIterator());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
	}

	protected abstract void parse(TokenIterator tokens) throws ParseException; 

	/**
	 * Returns a list of left braces found, which are probably array initializers and anonymous inner classes. Stops just before a semicolon.
	 */
	protected List<PairedToken> passAnonymousInnerClassesUntilSemiColon(TokenIterator tokens) throws ParseException {
		
		List<PairedToken> openBraces = new LinkedList<PairedToken>();
		Stack<Token> recentNewTokenNotFollowedByBrackets = new Stack<Token>();
		// Just jumping to the next semicolon won't work if there's an anonymous inner class. We need to find
		// the delimiters and jump past them until reaching the semicolon at this depth.
		while(tokens.hasNext() && !tokens.nextKindIs(SEMICOLON)) {

			Token next = tokens.getNext();
			switch(next.kind) {

			case LBRACE :
				// If there was a new preceding this that was not followed by []'s, its an anonymous inner class.
				if(recentNewTokenNotFollowedByBrackets != null) {
					openBraces.add((PairedToken) next);
					if(!recentNewTokenNotFollowedByBrackets.isEmpty())
						recentNewTokenNotFollowedByBrackets.pop();
					tokens.jumpPast(((PairedToken)next).getAssociatedToken());
				}
				break;

			case NEW :
				recentNewTokenNotFollowedByBrackets.push(next);
				break;
			
			case RBRACKET:
				if(!recentNewTokenNotFollowedByBrackets.isEmpty())
					recentNewTokenNotFollowedByBrackets.pop();
				break;
			}
			
		}
		return openBraces;
		
	}
	
	/**
	 *	Identifier [TypeArguments]{   .   Identifier [TypeArguments]} {[]} 
	 *	BasicType
	 * @throws ParseException 
	 */
	protected String type(TokenIterator tokens) throws ParseException {

		StringBuilder type = new StringBuilder();

		if(tokens.nextIsPrimitive()) {
			type.append(tokens.getNext().getText());
		}
		else {
			
			// Read the identifier and optional type arguments
			Token id = tokens.getNext();
			typeArguments(tokens);
			while(tokens.nextKindIs(DOT)) {
				// Read dot, identifier, and type args.
				tokens.getNext();
				id = tokens.getNext();
				typeArguments(tokens);
			}
			
			type.append(id.getText());
			
		}
		
		type.insert(0, optionalBrackets(tokens));
		return type.toString();
		
	}
	
	protected String optionalBrackets(TokenIterator tokens) throws ParseException {
		
		StringBuilder brackets = new StringBuilder();
		// Read []'s
		while(tokens.hasNext() && tokens.nextKindIs(LBRACKET)) {
			tokens.getNext();
			tokens.getNext();
			brackets.append("[");
		}
		return brackets.toString();

	}
	
	protected void variableInitializer(TokenIterator tokens) throws ParseException {
		
		// Array initializer
		if(tokens.nextKindIs(LBRACE)) {
			
			PairedToken open = (PairedToken)tokens.getNext();
			tokens.jumpPast(open.getAssociatedToken());
			
		}
		else {

			System.err.println("Not parsing expression starting at " + tokens);
			expression(tokens);
			
		}
		
	}
			
	protected void parenthesizedExpression(TokenIterator tokens) throws ParseException {
		
		PairedToken open = tokens.nextPaired(LPAREN);
		tokens.jumpPast(open.getAssociatedToken());
		
	}
	
	protected void expression(TokenIterator tokens) {
						
	}
	
	protected void optionalModifiers(TokenIterator tokens) throws ParseException {
		
		while(tokens.hasNext() && (tokens.nextIsModifier() || tokens.nextKindIs(AT))) {
			if(tokens.nextKindIs(AT))
				optionalAnnotations(tokens);
			else
				tokens.getNext();
		}
		
	}

	protected void typeArguments(TokenIterator tokens) throws ParseException {

		if(!tokens.nextKindIs(LT)) return;

		// Read over all of the type arguments.
		tokens.getNext();
		int lessThans = 1;
		while(tokens.hasNext() && lessThans > 0) {
			if(!tokens.hasNext()) break;
			Token next = tokens.getNext();
			switch(next.kind) {
			case GT : lessThans--; break;
			case LT : lessThans++; break;
			}
		}
		
		
	}
	
	protected void optionalAnnotations(TokenIterator tokens) throws ParseException {
		
		while(tokens.hasNext() && tokens.nextKindIs(AT)) {
			
			tokens.getNext();
			tokens.getNext();
			if(tokens.nextKindIs(LPAREN)) {
				PairedToken leftParen = (PairedToken) tokens.getNext();
				// Read all of the tokens inside the annotation element value until we reach the associated right parenthesis.
				while(tokens.hasNext()) {
					if(tokens.hasNext()) {
						Token rightParen = tokens.getNext();
						if(leftParen.getAssociatedToken() == rightParen)
							break;
					}
					else break;
				}
			}
			
		}
		
	}
	
}
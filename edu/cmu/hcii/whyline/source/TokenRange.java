package edu.cmu.hcii.whyline.source;

import java.util.Iterator;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public final class TokenRange implements Iterable<Token> {

	public final Token first, last;
	
	public TokenRange(Token first, Token last) {
		
		this.first = first;
		this.last = last;
		
		assert first != null : "First token was null";
		assert last != null : "Second token was null";
		
	}

	public FileInterface getFile() { return first.getFile(); }

	public Iterator<Token> iterator() {

		return new Iterator<Token>() {

			private Token token = first;

			public boolean hasNext() { return token != null; }

			public Token next() {

				Token tokenToReturn = token;
				// If the last is null, or there is no last, then there is no next token.
				if(token == last || last == null)
					token = null;
				else
					token = token.getNext();
				return tokenToReturn;
				
			}

			public void remove() { throw new UnsupportedOperationException("Token ranges are immutable."); }
		};
	
	}
	
	public boolean onSingleLine() {
		
		Line one = first.getLine();
		Line two = last.getLine();
		
		return one == two;
		
		
	}
	
	public String toString() { return (first == null ? "?" : "\"" + first.getText()+ "\"") + " to " + (last == null ? "?" : "\"" + last.getText()+ "\""); }
	
}

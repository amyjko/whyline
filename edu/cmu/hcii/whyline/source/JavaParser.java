package edu.cmu.hcii.whyline.source;

import java.util.*;

import edu.cmu.hcii.whyline.bytecode.Classfile;
import edu.cmu.hcii.whyline.bytecode.FieldInfo;
import edu.cmu.hcii.whyline.bytecode.MethodInfo;
import edu.cmu.hcii.whyline.source.Token.PairedToken;
import gnu.trove.TObjectIntHashMap;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public final class JavaParser implements JavaParserConstants {

	private final JavaSourceFile source;
	private String string = null;
	
	private final Line[] lines;
	private final Token[] code;
	private final Token[] identifiers;
	
	private final TObjectIntHashMap<Line> codeTokenIndicesByLine;
	
	private final FileElement file;
	
	/**
	 * JavaDoc comments found preceding the token keys.
	 */
	private final Map<Token,String> javadocs;
	
	public JavaParser(JavaSourceFile source, byte[] bytes) throws ParseException {
		
		this.source = source;

		// This tokenizes the file and extracts useful information for parsing.
		JavaTokenizer tokenizer = new JavaTokenizer(source, bytes);
		
		lines = tokenizer.getLines();
		codeTokenIndicesByLine = tokenizer.getCodeTokenIndiciesByLine();
		identifiers = tokenizer.getIdentifiers();
		code = tokenizer.getCode();
		javadocs = tokenizer.getJavaDocsByToken();

		if(lines != null) {

			StringBuilder builder = new StringBuilder();
			// Find all tokens intersecting these ranges
			for(Line line : lines)
				for(Token token : line.getTokens())
					builder.append(token.getText());
			string = builder.toString();

		}
		else string = new String(bytes);
		
		file = new FileElement(source, code[0], code[code.length -1]);
		
	}
		
	public String getString() { return string; }
	public Line[] getLines() { return lines; }
	public Token[] getIdentifiers() { return identifiers; }
	public Token[] getCodeTokens() { return code; }
	
	public TokenIterator getTokenIterator(Token first, Token last) throws ParseException {
		
		return new TokenIterator(first, last);
		
	}
	
	private int getIndexOfCodeToken(Token token) {

		if(!token.isCode())return -1;
		
		Line line = token.getLine();
		int lineIndex = line.getIndexOfCodeToken(token);
		assert lineIndex >= 0;
		int firstIndex = codeTokenIndicesByLine.get(line);
		int indexOfCodeToken = firstIndex + lineIndex;
		
		return indexOfCodeToken;
		
	}
	
	private int getIndexOfCodeTokenAtOrAfter(Token token) throws ParseException {
		
		if(token.isCode()) return getIndexOfCodeToken(token);

		// Find the next code token on this line, if there is one.
		Token code = token.getLine().getCodeTokenAtOrAfter(token);
		if(code != null) return getIndexOfCodeToken(code);

		// If there isn't keep searching lines until we find a code token.
		Line line = token.getLine().getLineAfter();
		while(line != null) {
			code = line.getFirstCodeToken();
			if(code != null) return getIndexOfCodeToken(code);
			else line = line.getLineAfter();
		}
		return -1;
		
	}
	
	public boolean isCodeTokenBeforeCodeToken(Token token, Token tokenBefore) {

		return getIndexOfCodeToken(token) < getIndexOfCodeToken(tokenBefore);
		
	}
	
	public Token getCodeTokenAfter(Token token) { 
		
		if(token == null) return null;
		assert token.getLine().getFile() == source : "But this token \"" + token + "\" isn't in this file " + source;
		int index = getIndexOfCodeToken(token);
		if(index < 0) return null;
		if(index + 1 >= code.length) return null;
		else return code[index + 1];
		
	}

	public Token getCodeTokenBefore(Token token) {
		
		if(token == null) return null;
		assert token.getLine().getFile() == source : "But this token \"" + token + "\" isn't in this file " + source;
		int index = getIndexOfCodeToken(token);
		if(index - 1 < 0) return null;
		else return code[index - 1];
		
	}

	public ClassElement getClassElement(Classfile classfile) {
		
		return file.getClassElement(classfile);
		
	}
	
	public void parseBlocks() {

		file.parseBlocks();
		
	}
	
	public ClassBodyElement getFieldElement(FieldInfo field) {

		ClassElement c = getClassElement(field.getClassfile());
		ClassBodyElement f = c == null ? null : c.getFieldElement(field);
		return f;
	
	}

	public ClassBodyElement getMethodElement(MethodInfo method) {

		ClassElement c = getClassElement(method.getClassfile());
		ClassBodyElement m = c == null ? null : c.getMethodElement(method);
		return m;

	}

	public final class TokenIterator {
	
		private int nextIndex;
		private int lastIndex;
		
		public TokenIterator(Token first, Token last) throws ParseException {

			assert first != null;
			
			// Either may be -1, which indicates that there are no code tokens after them.
			nextIndex = getIndexOfCodeTokenAtOrAfter(first);
			lastIndex = last == null ? -1 : getIndexOfCodeTokenAtOrAfter(last);
			
			// A small correction: we don't want the code token after the last one. We want the one before the last token.
			// Therefore, if the last is not code, we step one before code token after the whitespace.
			if(lastIndex < 0) lastIndex = code.length - 1;
			else if(!last.isCode()) lastIndex--;
			
		}
		
		public boolean nextKindIs(int kind) { return code[nextIndex].kind == kind; }
		
		public boolean hasKindBefore(int kind, int stoppingKind) {
			
			int stopIndex = lastIndex < 0 ? code.length : lastIndex;
			for(int i = nextIndex; i < lastIndex; i++) {
				int currentKind = code[i].kind;
				if(currentKind == kind) return true;
				else if(currentKind == stoppingKind) return false;
			}
			return false;
			
		}
		
		public boolean hasNext() { return nextIndex >= 0 && nextIndex <= lastIndex; }
		
		public Token getNext() throws ParseException { 
		
			if(nextIndex >= code.length) throw new ParseException("There was no next token...", this);
			return code[nextIndex++]; 
			
		}

		public Token getNext(int kind) throws ParseException { 

			Token t = getNext();
			if(t.kind != kind) throw new ParseException("Expected a " + JavaParserConstants.tokenImage[kind] + " but found " + t, this);
			else return t;
			
		}

		public PairedToken nextPaired(int kind) throws ParseException {

			Token t = getNext();
			if(t.kind != kind) throw new ParseException("Expected " + JavaParserConstants.tokenImage[kind], this);
			else if(t instanceof PairedToken) return (PairedToken)t;
			else throw new ParseException("Expected paired token", this);			
			
		}
		
		public PairedToken peekPaired() throws ParseException {
			
			Token t = peek();
			if(t instanceof PairedToken) return (PairedToken)t;
			else throw new ParseException("Expected paired token", this);			
			
		}

		public PairedToken peekPairedPartner() throws ParseException {
			
			Token t = peek();
			if(t instanceof PairedToken) return ((PairedToken)t).getAssociatedToken();
			else throw new ParseException("Expected paired token", this);			
			
		}

		public boolean nextIsModifier() { return code[nextIndex].isModifier(); }

		public Token jumpPastNext(int kind) throws ParseException {
		
			while(hasNext() && !nextKindIs(kind))
				getNext();
			if(hasNext())
				return getNext();
			else 
				return null;
		
		}

		public Token getNextOr(int kind1, int kind2) throws ParseException {
			
			while(hasNext() && !nextKindIs(kind1) && !nextKindIs(kind2))
				getNext();
			if(hasNext())
				return getNext();
			else 
				return null;
		
		}

		public Token peek() { return nextIndex >= code.length ? null : code[nextIndex]; }

		public Token peekNext() { return hasNext() ? code[nextIndex + 1] : null; } 

		public Token peekNext(int kind) {
			
			for(int i = nextIndex; i < code.length; i++)
				if(code[i].kind == kind)
					return code[i];
			return null;
			
		}

		public Token peekBeforeNext(int kind) {
			
			for(int i = nextIndex; i < code.length - 1; i++)
				if(code[i + 1].kind == kind)
					return code[i];
			return null;
			
		}

		public void jumpPast(Token token) throws ParseException {

			assert token != null;
			while(hasNext() && peek() != token)
				getNext();
			if(hasNext() && peek() == token)
				getNext();
			else throw new ParseException("Expected to find specific '" + token + "'", this);
			
		}
	
		public String toString() { return "" + code[nextIndex].getLine() +"\n\tprevious = " + (nextIndex > 0 ? code[nextIndex - 1] : "null") + "\n\tnext = " + (hasNext() ? peek() : "null"); }

		public boolean nextIsPrimitive() { return peek().isPrimitive(); }
		
	}
	
}
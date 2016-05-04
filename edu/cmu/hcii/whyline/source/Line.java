package edu.cmu.hcii.whyline.source;

import java.util.*;

import edu.cmu.hcii.whyline.bytecode.Instruction;
import edu.cmu.hcii.whyline.bytecode.MethodInfo;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public final class Line implements Comparable<Line> {

	private final LineNumber lineNumber;
	private final FileInterface file;
	private ArrayList<Token> tokens = new ArrayList<Token>(10);
	private int numberOfCharacters = 0;
	
	public Line(LineNumber lineNumber, Token ... newTokens) {

		this.lineNumber = lineNumber;
		this.file = lineNumber.getFile();
		
		for(Token t : newTokens)
			addToken(t);
		
	}
	
	public static Line createBlankLine(LineNumber lineNumber) { 
		
		Line newLine = new Line(lineNumber);
		Token blank = new Token(newLine, "", 0);
		newLine.addToken(blank);
		return newLine;
		
	}
	
	public Token getToken(int index) { return index < 0 || index >= tokens.size() ? null : tokens.get(index); }
	
	public void addToken(Token t) {
		
		t.setLineIndex(tokens.size());
		tokens.add(t);
		numberOfCharacters += t.getText().length();
		
	}
	
	public int getNumberOfCharacters() { return numberOfCharacters; }

	public int getIndexOfCodeToken(Token token) {

		int index = 0;
		for(Token t : tokens) {
			if(t == token) return index;
			else if(t.isCode()) index++;
		}
		return -1;
		
	}
	
	public Token getFirstCodeToken() {
		
		for(Token token : tokens)
			if(!token.isWhitespaceOrComment()) 
				return token;
		return null;
		
	}

	public Token getLastCodeToken() {
		
		Token lastNonWhitespaceToken = null;
		for(Token token : tokens)
			if(!token.isWhitespaceOrComment()) 
				lastNonWhitespaceToken = token;
		return lastNonWhitespaceToken;
		
	}

	public Line getLineAfter() throws ParseException {
		
		return file.getLine(getLineNumber().getNumber() + 1);
		
	}
	
	public List<Token> getTokens() {
		return Collections.<Token>unmodifiableList(tokens);
	}

	public Iterator<Token> iterator() {
		return tokens.iterator();
	}

	public SortedSet<Instruction> getInstructions() { return file.getInstructionsOnLine(this); }

	public Instruction getFirstInstruction() {
		
		SortedSet<Instruction> instructions = getInstructions();
		if(instructions.isEmpty()) return null;
		else return instructions.first();
		
	}
	
	public FileInterface getFile() { return file; }

	public MethodInfo getMethod() {
		
		SortedSet<Instruction> instructions = getInstructions();
		if(instructions.isEmpty()) return null;
		else return instructions.first().getMethod();
		
	}

	public LineNumber getLineNumber() { return lineNumber; }

	public int compareTo(Line line) {
		
		if(file != line.file) return file.getFileName().compareTo(line.file.getFileName());
		else return lineNumber.compareTo(line.lineNumber);		
		
	}

	public void trim() { tokens.trimToSize(); }

	public String getLineText() {

		StringBuilder builder = new StringBuilder();
		for(Token t : tokens) {
			builder.append(t.getText());
		}
		return builder.toString();
		
	}
	
	public String toString() {
	
		return getLineNumber().toString() + "\t" + getLineText();
		
	}

	public List<Token> getTokensAfterFirstNonWhitespaceToken() {
		
		List<Token> tokens = new ArrayList<Token>();
		try {
			Token token = getFirstCodeToken();
			while(token != null && token.getLine() == this) {
				tokens.add(token);
				token = token.getNextCodeToken();			
			}
		} catch(ParseException e) {
			e.printStackTrace();
		}
		return tokens;
			
		
	}

	public Token getCodeTokenAtOrAfter(Token token) {

		int index = tokens.indexOf(token);
		if(index < 0) return null;
		while(!token.isCode() && index < tokens.size() - 1) {
			index++;
			token = tokens.get(index);
		}
		if(token.isCode()) return token;
		else return null;
		
	}

	public Token getTokenAfter(Token token) {
		
		int index = tokens.indexOf(token);
		if(index < 0 || index >= tokens.size() - 1) return null;
		else return tokens.get(index + 1);
		
	}

	public Token getTokenBefore(Token token) {
		
		int index = tokens.indexOf(token);
		if(index <= 0 || index > tokens.size() - 1) return null;
		else return tokens.get(index - 1);
		
	}

	public TokenRange getRange() { 
		
		// This code finds the code range.
		Token first = getFirstCodeToken();
		Token last = getLastCodeToken();
		if(first == null && last != null) first = last;
		if(last == null && first != null) last = first;
		if(first == null && last == null) return null;
		else return new TokenRange(first, last); 
		
	}
	
	public Token getFirstToken() { return tokens.size() > 0 ? tokens.get(0) : null; }
	public Token getLastToken() { return tokens.size() > 0 ? tokens.get(tokens.size() - 1) : null; }
	
}
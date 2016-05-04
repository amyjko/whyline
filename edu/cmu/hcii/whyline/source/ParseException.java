package edu.cmu.hcii.whyline.source;

import edu.cmu.hcii.whyline.source.JavaParser.TokenIterator;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class ParseException extends Exception {

	private final TokenIterator tokens;
	
	public ParseException(String message, TokenIterator tokens) {

		super(message + ": " + tokens);
		
		this.tokens = tokens;
		
	}
		
}

package edu.cmu.hcii.whyline.source;

import java.io.EOFException;

/**
 * Read as one byte ASCII characters. But I like the way DataInputStream throws an 
 * EOFException instead of returning -1. It makes it easier to write this.
 *
 * @author Andrew J. Ko
 */
public class PeekReader {

	private byte[] chars;
	private int index = 0;
	private StringBuilder accumulation = new StringBuilder(20);
	
	public PeekReader(byte[] ascii) {

		chars = ascii;
		
	}

	public char peekAtNext() throws EOFException { return getNextChar(); }
	public boolean nextIs(char c) throws EOFException { return getNextChar() == c; }
	public boolean nextIs(char c1, char c2) throws EOFException { return getNextChar() == c1 && getCharAfterNext() == c2; }
	public boolean nextIs(char c1, char c2, char c3) throws EOFException { return getNextChar() == c1 && getCharAfterNext() == c2 && getCharAfterCharAfterNext() == c3; }
	public boolean nextIs(char c1, char c2, char c3, char c4) throws EOFException { return getNextChar() == c1 && getCharAfterNext() == c2 && getCharAfterCharAfterNext() == c3 && getCharAfterCharAfterCharAfterNext() == c4; }
	public char next() throws EOFException { 

		char c = getNextChar();
		if(c == '\t')
			accumulation.append(JavaSourceFile.TAB);
		else
			accumulation.append(c);
		if(charAtIndexIsUnicodeEscape(index)) index += 6;
		else index += 1;
		
		// Invalidate the cache.
		currentIndexIsUnicodeEscapeCache = -1;
		nextCharCache = Character.MAX_VALUE;
		
		return c;
		
	}

	// -1 = don't know, 0 = no, 1 = yes
	private int currentIndexIsUnicodeEscapeCache = -1;
	
	private boolean charAtIndexIsUnicodeEscape(int i) { 

		if(currentIndexIsUnicodeEscapeCache < 0)
			currentIndexIsUnicodeEscapeCache =
				((char)chars[i] == '\\' && 
				i + 5 < chars.length &&
				(char)chars[i + 1] == 'u' &&
				Character.isLetterOrDigit((char)chars[i + 2]) &&
				Character.isLetterOrDigit((char)chars[i + 3]) &&
				Character.isLetterOrDigit((char)chars[i + 4]) &&
				Character.isLetterOrDigit((char)chars[i + 5]))
				? 1 : 0;
		
		return currentIndexIsUnicodeEscapeCache == 1;
	
	}
	
	private char readUnicodeEscapeAt(int i) {

		int one = chars[i + 2];
		int two = chars[i + 3] * 16 + one;
		int three = chars[i + 4] * 256 + two;
		int four = chars[i + 5] * 4096 + three;
		char c = (char)four;
		return c;
		
	}

	private int nextCharCache = Character.MAX_VALUE;
	
	private char getNextChar() throws EOFException {

		if(nextCharCache == Character.MAX_VALUE) {
		
			// Convert unicode escapes to actual unicode characters
			if(index >= chars.length) throw new EOFException();
			if(charAtIndexIsUnicodeEscape(index)) nextCharCache = readUnicodeEscapeAt(index);
			else nextCharCache = chars[index];
			
		}
		return (char) nextCharCache;
		
	}
	
	private char getCharAfterNext() {
		
		int newIndex = index;
		if(charAtIndexIsUnicodeEscape(newIndex)) newIndex += 6;
		else newIndex += 1;

		// Convert unicode escapes to actual unicode characters
		if(charAtIndexIsUnicodeEscape(newIndex)) return readUnicodeEscapeAt(newIndex);
		else return (char)chars[newIndex];
		
	}

	private char getCharAfterCharAfterNext() {
		
		int newIndex = index;
		if(charAtIndexIsUnicodeEscape(newIndex)) newIndex += 6;
		else newIndex += 1;
		if(charAtIndexIsUnicodeEscape(newIndex)) newIndex += 6;
		else newIndex += 1;

		// Convert unicode escapes to actual unicode characters
		if(charAtIndexIsUnicodeEscape(newIndex)) return readUnicodeEscapeAt(newIndex);
		else return (char)chars[newIndex];
		
	}

	private char getCharAfterCharAfterCharAfterNext() {
		
		int newIndex = index;
		if(charAtIndexIsUnicodeEscape(newIndex)) newIndex += 6;
		else newIndex += 1;
		if(charAtIndexIsUnicodeEscape(newIndex)) newIndex += 6;
		else newIndex += 1;
		if(charAtIndexIsUnicodeEscape(newIndex)) newIndex += 6;
		else newIndex += 1;

		// Convert unicode escapes to actual unicode characters
		if(charAtIndexIsUnicodeEscape(newIndex)) return readUnicodeEscapeAt(newIndex);
		else return (char)chars[newIndex];
		
	}

	public String eraseAccumulation() {
		
		String acc = accumulation.toString();
		accumulation.delete(0, accumulation.length());
		return acc;
		
	}
	
	public char lastCharReturned() { return (char)chars[index - 1]; } 

}

package edu.cmu.hcii.whyline.source;

import java.util.List;

import edu.cmu.hcii.whyline.bytecode.GETSTATIC;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class GetStaticExpression extends Expression<GETSTATIC> {

	public GetStaticExpression(Decompiler decompiler, GETSTATIC getstatic) {
		
		super(decompiler, getstatic);
		
	}

	public String getJavaName() { return code.getFieldref().getName(); }

	public boolean alwaysAppearsInSource() { return true; }
	public boolean mayAppearInSource() { return true; }

	protected Token parseHelper(List<Token> tokens) {
		
		// Need to read through qualified name until reaching identifier that matches method called.
		Token tokenParsed = null;
		do {
			tokenParsed = parseThis(tokens);
		} while(tokens.size() > 0 && !tokenParsed.getText().equals(getJavaName()));
		return tokenParsed;
		
	}

}

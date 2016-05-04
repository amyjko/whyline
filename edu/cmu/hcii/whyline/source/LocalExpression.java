package edu.cmu.hcii.whyline.source;

import java.util.List;

import edu.cmu.hcii.whyline.bytecode.GetLocal;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class LocalExpression extends Expression<GetLocal> {

	public LocalExpression(Decompiler decompiler, GetLocal local) {
		
		super(decompiler, local);
		
	}
	
	public String getJavaName() {

		return code.getLocalIDName();
	
	}
		
	public boolean alwaysAppearsInSource() { return true; }
	public boolean mayAppearInSource() { return true; }

	protected Token parseHelper(List<Token> tokens) {
		
		// If this gets "this", only parse the next token if it's a "this" token.
		if(code.getMethod().isVirtual() && code.getLocalID() == 0) {
			if(tokens.isEmpty()) return null;	
			else if(tokens.get(0).kind == JavaParserConstants.THIS)
				return parseThis(tokens);
			else
				return tokens.get(0);
		}
		else {

			// If this pushes an an enclosing instance expression, don't parse any tokens.
			if(code.getMethod().isInstanceInitializer() && code.getClassfile().isInnerClass()) {
				int arg = code. getMethod().getArgumentNumberOfLocalID(code.getLocalID());
				if(arg < code.getMethod().getFirstArgumentAppearingInSource() - 1)
					return tokens.get(0);
			}
			return parseThis(tokens);
			
		}
		
	}

}

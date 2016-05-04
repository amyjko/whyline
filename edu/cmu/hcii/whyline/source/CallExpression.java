package edu.cmu.hcii.whyline.source;

import java.util.List;

import edu.cmu.hcii.whyline.bytecode.*;
import edu.cmu.hcii.whyline.source.Token.PairedToken;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class CallExpression extends ConsumerExpression<Invoke> {

	public CallExpression(Decompiler decompiler, Invoke producer) {

		super(decompiler, producer);
		
	}

	public String getJavaName() { return code.getJavaMethodName(); }

	public boolean alwaysAppearsInSource() { return !(code instanceof INVOKESPECIAL); }
	public boolean mayAppearInSource() { return true; }

	protected Token parseHelper(List<Token> tokens) {

		String name = code.getJavaMethodName();
		
		if(code instanceof INVOKESTATIC) { 

			Token last = null;
			
			// Parse qualifications
			while(tokens.size() > 0 && !tokens.get(0).getText().equals(getJavaName()))
				last = parseThis(tokens);
			// Parse name
			last = parseThis(tokens);
			// Parse arguments.
			for(int i = 0; i < getNumberOfArguments(); i++)
				last = parseArgument(tokens, i);
			return last;
			
		}
		else {
			
			if(code instanceof INVOKESPECIAL && tokens.size() > 0 && tokens.get(0).kind == JavaParserConstants.SUPER)
				tokens.remove(0);
			
			// Is this qualified? If so, read the instance expression.
			parseArgument(tokens, 0);
			
			// We should be at the method name now.
			Token last = parseThis(tokens);

			// There may be some extra instructions here if this is an anonymous inner class creation expression.
			// If so, we need to start at the first argument that appears in source. 			
			int firstArgumentAppearingInSource = code.getFirstArgumentAppearingInSource();
			
			for(int i = firstArgumentAppearingInSource; i < getNumberOfArguments(); i++)
				last = parseArgument(tokens, i);
			
			// This could be an anonymous class declaration. If so, there will be a { brace next. We need to skip all of the tokens from it to its
			// pair. We do this by eating tokens until reaching the associated delimiter. These tokens will have already been parsed by
			// the ClassElement that represents this anonymous class.
			if(tokens.size() > 0 && tokens.get(0).kind == JavaParserConstants.LBRACE) {

				PairedToken brace = (PairedToken)tokens.get(0);
				PairedToken pair = brace.getAssociatedToken();
				while(tokens.size() > 0 && tokens.get(0) != pair)
					tokens.remove(0);
				if(tokens.size() > 0 && tokens.get(0) == pair)
					tokens.remove(0);
				
			}
			
			return last;
			
		}
		
	}

}

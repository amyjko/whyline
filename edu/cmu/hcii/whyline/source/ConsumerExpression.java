package edu.cmu.hcii.whyline.source;

import java.util.LinkedList;

import static edu.cmu.hcii.whyline.source.JavaParserConstants.*;

import edu.cmu.hcii.whyline.bytecode.*;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public abstract class ConsumerExpression<T extends Instruction> extends Expression<T> {
	
	public ConsumerExpression(Decompiler decompiler, T consumer) {
		
		super(decompiler, consumer);
		
	}

	public abstract String getJavaName();
	
	public void setTokenRange(Token first, Token last) throws ParseException {
				
		// Create a list of tokens representing this expression
		LinkedList<Token> tokens = new LinkedList<Token>();

		Token t = first;
		while(true) {
			switch(t.kind) {
			
			case DOT :
			case LPAREN :
			case RPAREN :
			case COMMA :

				break;

			default:
				
				boolean primitive = t.isPrimitive();
				if(!primitive)
					tokens.add(t);

			}
			if(t == last) break;
			t = t.getNextCodeToken();
		}
		
//		System.err.println("Parsing " + code);

		// Create a list of expression nodes, in order of appearance.
		parse(tokens);
		
//		System.err.println("\n");

	}
	
}

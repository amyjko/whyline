package edu.cmu.hcii.whyline.source;

import java.util.List;

import edu.cmu.hcii.whyline.bytecode.IINC;
import edu.cmu.hcii.whyline.bytecode.SetLocal;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class SetLocalExpression extends ConsumerExpression<SetLocal> {

	public SetLocalExpression(Decompiler decompiler, SetLocal consumer) {

		super(decompiler, consumer);
	
	}

	public String getJavaName() {
		
		if(code instanceof IINC) {
			
			String prefix = ((IINC)code).getLocalIDName();
			int inc = ((IINC)code).getIncrement();
			if(inc == 1) return prefix + "++";
			else if(inc < -1) return prefix + "-=" + inc;
			else if(inc == -1) return prefix + "--";
			else return prefix + "+=" + inc;

		}
		else {
			String name = code.getLocalIDName();
			return name + " = ";
		}

	}

	public boolean alwaysAppearsInSource() { return false; }

	// Sometimes there are hidden local assignments of exceptions at the top of exception handlers.
	// No instruction pushes these onto the operand stack, so if we have no operands, then  
	public boolean mayAppearInSource() {  return code instanceof IINC || hasOperands(); }
	
	protected Token parseHelper(List<Token> tokens) {

		if(code instanceof IINC) {

			int inc = ((IINC)code).getIncrement();
			if(Math.abs(inc) == 1) {
				// Parse the name and the increment.
				parseThis(tokens);
				return parseThis(tokens);
			}
			else {
				// Parse the name, the equals, the name, the plus, the value.
				parseThis(tokens);
				parseThis(tokens);
				parseThis(tokens);
				parseThis(tokens);
				return parseThis(tokens);
			}
			
		}
		else {
			
			// Parse until reaching the =, skipping over names and types.
			while(tokens.size() > 0 && tokens.get(0).kind != JavaParserConstants.ASSIGN)
				parseThis(tokens);

			// Parse the =
			parseThis(tokens);
			
			// Then parse the value.
			return parseArgument(tokens, 0);
			
		}
		
	}

}

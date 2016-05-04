package edu.cmu.hcii.whyline.source;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.hcii.whyline.bytecode.INVOKESPECIAL;
import edu.cmu.hcii.whyline.bytecode.Instruction;
import edu.cmu.hcii.whyline.bytecode.NEW;
import edu.cmu.hcii.whyline.bytecode.QualifiedClassName;
import edu.cmu.hcii.whyline.bytecode.StackDependencies;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public abstract class Expression<T extends Instruction> {

	protected final T code;
	private final Decompiler decompiler;

	protected Token first, last;

	private final ArrayList<Expression<?>>[] argumentExpressions;
	private boolean hasArguments;

	@SuppressWarnings({ "cast", "unchecked" })
	public Expression(Decompiler decompiler, T code) {
		
		this.decompiler = decompiler;
		this.code = code;

		argumentExpressions = (ArrayList<Expression<?>>[])new ArrayList[code.getNumberOfArgumentProducers()];
		hasArguments = false;

		// We aren't trying to get these one for one with the instruction's arguments. We just want them in order of appearance.
		for(int arg = 0; arg < code.getNumberOfArgumentProducers(); arg++) {
			StackDependencies.Producers producers = code.getProducersOfArgument(arg);
			Instruction[] prods = producers.getProducers();
			if(prods != null) {
				
				argumentExpressions[arg] = new ArrayList<Expression<?>>(1);
				for(Instruction producer : prods) {
					argumentExpressions[arg].add(decompiler.getExpression(code, producer));
					hasArguments = true;
				}
			
			}
		}
		
	}
	
	public Instruction getCode() { return code; }
	
	public abstract String getJavaName();
	
	public abstract boolean mayAppearInSource();

	public abstract boolean alwaysAppearsInSource();

	/**
	 * This establishes mapping between one Instruction and multiple Tokens.
	 */
	protected final Token parse(List<Token> tokens) {
		
		first = tokens.isEmpty() ? null : tokens.get(0);
		last = parseHelper(tokens);

//		assert first != null : "Why couldn't we find a first token before parsing "  + getClass().getSimpleName() + this ;
//		assert last != null : getClass().getSimpleName() + " " + this + " didn't return a last token.";
		
		if(first != null && last != null)
			decompiler.getSource().linkInstructionWithTokenRange(code, first, last);

		return last;
		
	}
	
	protected abstract Token parseHelper(List<Token> tokens);
	
	protected Token parseArgument(List<Token> tokens, int arg) {
		
		if(arg < 0 || arg >= argumentExpressions.length) return null;
		
		List<Expression<?>> args = argumentExpressions[arg];
		Token last = null;
		if(args != null) {
			for(Expression<?> expr : args)
				if(expr != null)
					last = expr.parse(tokens);
		}
		return last;
		
	}
	
	/**
	 * Establishes a link between the first token in this list and this expression's instruction.
	 * 
	 *  This is the token -> instruction mapping used to support clicking on tokens in source files.
	 */
	protected Token parseThis(List<Token> tokens) {

		// If this expression is a "new java.lang.StringBuilder", but the token is not of type NEW, this instruction does not appear in source.
		if(code instanceof NEW && 
			((NEW)code).getClassnameOfTypeProduced() == QualifiedClassName.STRING_BUILDER &&
			!tokens.isEmpty() &&
			tokens.get(0).kind != JavaParserConstants.NEW)
			return tokens.get(0);
		
		// If this expression is a "<init>StringBuilder() call," but the token does not match, this instruction does not appear in source.
		if(code instanceof INVOKESPECIAL && 
				((INVOKESPECIAL)code).getMethodInvoked().matchesClassAndName(QualifiedClassName.STRING_BUILDER, "<init>") &&
				!tokens.isEmpty() &&
				!tokens.get(0).getText().equals("StringBuilder"))
				return tokens.get(0);
		
		Token token = tokens.isEmpty() ? null : tokens.remove(0);
		if(token != null) {
//			System.err.println(Util.fillOrTruncateString(token.getText(), 20) + "\t> " + code);
			decompiler.getSource().linkTokenWithInstruction(token, code);
		}
		else {
//			System.err.println(Util.fillOrTruncateString("- no token - ", 20) + "\t> " + code);
		}
		return token;
		
	}
	
	public int getNumberOfArguments() { return code.getNumberOfArgumentProducers(); }
	
	public boolean hasOperands() { return hasArguments; }
	
	public final String toString() {

		if(code.getNumberOfOperandsConsumed() == 0)
			return getJavaName();
		
		StringBuilder builder = new StringBuilder();
		builder.append("(");
		builder.append(getJavaName()); 
		for(ArrayList<Expression<?>> args : argumentExpressions)
			for(Expression<?> expr : args) {
				builder.append(" " );
				builder.append(expr);
			}
		builder.append(")");

		return builder.toString();
		
	}
	
}

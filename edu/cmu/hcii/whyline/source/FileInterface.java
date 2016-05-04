package edu.cmu.hcii.whyline.source;

import java.util.SortedSet;

import edu.cmu.hcii.whyline.bytecode.Classfile;
import edu.cmu.hcii.whyline.bytecode.Instruction;
import edu.cmu.hcii.whyline.bytecode.MethodInfo;
import edu.cmu.hcii.whyline.bytecode.QualifiedClassName;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public interface FileInterface extends Comparable<FileInterface> {

	public Line[] getLines() throws ParseException;
	public Line getLine(int lineNumber) throws ParseException;
	public int getNumberOfLines() throws ParseException;
	
	public SortedSet<Instruction> getInstructionsOnLine(Line line);
	
	public Token getTokenForMethodName(MethodInfo method);

	public Token getCodeTokenAfter(Token t) throws ParseException;
	public Token getCodeTokenBefore(Token t) throws ParseException;
	public String getFileName();
	public String getShortFileName();
	
	public boolean isFamiliar();

	/**
	 * Parameter numbers start at 1, and go to N, where N is the number of parameters in the method. This does not include the instance passed to the method, if a virtual method.
	 */
	public TokenRange getTokenRangeForParameter(MethodInfo method, int parameterNumber);
	public TokenRange getTokenRangeFor(Instruction inst);
	public TokenRange getTokenRangeForMethod(MethodInfo method);
	public TokenRange getTokenRangeFor(Classfile classfile);

	public Instruction getInstructionFor(Token token);
	public QualifiedClassName getClassnameFor(Token token);
	public Parameter getMethodParameterFor(Token token);
	
}
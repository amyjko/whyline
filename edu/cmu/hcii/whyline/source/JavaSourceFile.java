package edu.cmu.hcii.whyline.source;

import java.io.*;
import java.util.*;

import edu.cmu.hcii.whyline.bytecode.*;
import edu.cmu.hcii.whyline.source.Token.PairedToken;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public final class JavaSourceFile  implements FileInterface, Comparable<FileInterface>, JavaParserConstants {

	public static final String TAB = "    ";
	
	private String filename;
	private byte[] fileBytes;
	
	private final boolean isFamiliar;

	private JavaParser parser;
	private ParseException tokenizationProblem = null;

	private final Set<Classfile> classfiles = new HashSet<Classfile>(1);
	
	private final Map<Token, QualifiedClassName> classnamesByToken = new HashMap<Token,QualifiedClassName>();
	private final Map<Token, Instruction> instructionsByToken = new HashMap<Token,Instruction>();
	private final Map<Instruction, TokenRange> tokensByInstruction = new HashMap<Instruction, TokenRange>();
	private final Map<Token,Parameter> parametersByToken = new HashMap<Token,Parameter>();
	
	public JavaSourceFile(String filename, byte[] bytes, boolean isFamiliar) throws IOException {

		this.filename = filename;
		this.fileBytes= bytes;
		this.isFamiliar = isFamiliar;
		
	}

	public boolean isFamiliar() { return isFamiliar; }
	
	private JavaParser getTokens() throws ParseException { 

		if(tokenizationProblem != null) {
			tokenizationProblem.printStackTrace();
			return null;
		}
		
		// Tokenize, then release the file bytes
		if(parser == null) {
			
			try {
				parser = new JavaParser(this, fileBytes);
			} catch (ParseException e) {
				tokenizationProblem = e;
				throw e;
			}
			fileBytes = null;
			
		}
		return parser;
		
	}
	
	public JavaParser.TokenIterator getTokenIterator(Token first, Token last) throws ParseException {
		
		return getTokens().getTokenIterator(first, last);
		
	}
	
	//////////////////////////////////////////////
	// Comparable interface
	//////////////////////////////////////////////

	public int compareTo(FileInterface o) {
		
		return getShortFileName().compareTo(o.getShortFileName());
		
	}
	
	//////////////////////////////////////////////
	// File interface
	//////////////////////////////////////////////
	
	public Line[] getLines() throws ParseException { return getTokens() != null ? getTokens().getLines() : null; }

	public Token[] getIdentifiers() throws ParseException { return getTokens().getIdentifiers(); }
	
	public Line getLine(int lineNumber) throws ParseException {
		
		Line[] lines = getLines();
		if(lines == null) return null;
		
		// Vectors are 0-indexed, but numbers start at 1.
		int zeroIndexedLineNumber = lineNumber - 1;
		if(zeroIndexedLineNumber < 0 || zeroIndexedLineNumber >= lines.length) return null;
		else return lines[zeroIndexedLineNumber];
		
	}
	
	public int getNumberOfLines() throws ParseException { return getTokens() == null ? 0 : getTokens().getLines().length; }

	public Token getCodeTokenAfter(Token token) throws ParseException { return getTokens().getCodeTokenAfter(token); } 
	public Token getCodeTokenBefore(Token token) throws ParseException { return getTokens().getCodeTokenBefore(token); } 
	
	public String getFileName() { return filename; }
	
	public String getShortFileName() { 
	
		int slashIndex = filename.lastIndexOf('/');
		if(slashIndex < 0) return filename;
		else return filename.substring(slashIndex + 1);
		
	}

	public String getPackageName() {
		
		int slashIndex = filename.lastIndexOf('/');
		if(slashIndex < 0) return "(default package)";
		else return filename.substring(0, slashIndex).replace('/', '.');
		
	}
	
	//////////////////////////////////////////////
	
	public void linkTokenWithClassname(Token token, QualifiedClassName name) {
		
		classnamesByToken.put(token, name);
		
	}
	
	public QualifiedClassName getClassnameFor(Token token) {
		
		return classnamesByToken.get(token);
		
	}
	
	public Parameter getMethodParameterFor(Token token) {
		
		return parametersByToken.get(token);
		
	}

	public void linkTokenWithParameter(Token token, Parameter parameter) {
		
		parametersByToken.put(token, parameter);
		
	}

	public void linkTokenWithInstruction(Token token, Instruction instruction) {
		
		instructionsByToken.put(token, instruction);
		
	}
	
	public void linkInstructionWithTokenRange(Instruction instruction, Token first, Token last) {
		
		tokensByInstruction.put(instruction, new TokenRange(first, last));
		
	}
	
	public TokenRange getTokenRangeFor(Instruction instruction) { 
		
		try {
			ClassBodyElement m = getTokens().getMethodElement(instruction.getMethod());
			if(m != null) m.parseBlock();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		TokenRange range = tokensByInstruction.get(instruction);
		
		if(range == null) return instruction.getLine().getRange();
		else return range;
		
	}

	public Instruction getInstructionFor(Token token) { 

		try {
			getTokens().parseBlocks();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return instructionsByToken.get(token); 
		
	}

	public TokenRange getTokenRangeForField(FieldInfo field) {

		try {
			getTokens();
		} catch (ParseException e) { return null; }
		if(parser != null) {
			ClassBodyElement f = parser.getFieldElement(field);
			return f == null ? null : new TokenRange(f.firstToken, f.lastToken);
		}
		
		return null;
		
	}

	public TokenRange getTokenRangeForMethod(MethodInfo method) {
		
		try {
			getTokens();
		} catch (ParseException e) { return null; }

		TokenRange range = null;
		
		if(parser != null) {
			ClassBodyElement m = parser.getMethodElement(method);
			range = m == null ? null : m.getTokenRangeOfHeader();
		}

		if(range == null)
			range = method.getCode().getFirstInstruction().getLine().getRange();

		return range;
		
	}
	
	public TokenRange getTokenRangeForParameter(MethodInfo method, int parameter) {

		try {
			getTokens();
			ClassBodyElement m = parser.getMethodElement(method); 
			return m == null ? null : m.getTokenRangeForParameter(parameter);
		} catch (ParseException e) {
			return null;
		}
		
	}
	
	public TokenRange  getTokenRangeForReturnType(MethodInfo method) {
		try {
			getTokens();
			ClassBodyElement m = parser.getMethodElement(method); 
			return m == null ? null : m.getTokenRangeForReturnType();
		} catch (ParseException e) {
			return null;
		}
	}

	public TokenRange getTokenRangeFor(Classfile classfile) {

		try {
			getTokens();
			ClassElement c = parser.getClassElement(classfile);
			return c== null ? null : c.getHeaderRange();
		} catch (ParseException e) {
			return null;
		}
		
	}
	
	public void linkClassfile(Classfile classfile) { classfiles.add(classfile); }
	
	public Iterable<Classfile> getClassfiles() { return classfiles; }
	
	public List<MethodInfo> getMethods() {

		ArrayList<MethodInfo> methods = new ArrayList<MethodInfo>();
		
		for(Classfile c : classfiles)
			for(MethodInfo method : c.getDeclaredMethods())
				methods.add(method);

		return Collections.<MethodInfo>unmodifiableList(methods);
		
	}
	
	public Token getTokenOfArithmetic(Computation inst) {
		
		Line line = inst.getLine();
		SortedSet<Instruction> instructions = getInstructionsOnLine(line);
		List<Token> tokens = line.getTokens();

		// Which number of array reference is this in the method?
		int number = 0;
		boolean found = false;
		for(Instruction i : instructions)
			if(i instanceof Computation) {
				if(i == inst) { found = true; break; }
				else number++;
			}

		if(!found) return null;

		int operatorNumber = 0;
		for(Token token : tokens) {
			if(token.isOperator()) {
				if(operatorNumber == number) return token;
				operatorNumber++;
			}
		}		
		return null;
		
	}
	
	public Token getTokenOfReturn(AbstractReturn inst) {

		try {
			Line line = inst.getLine();
			SortedSet<Instruction> instructions = getInstructionsOnLine(line);
			List<Token> tokens = line.getTokens();
	
			int number = 0;
			boolean found = false;
			for(Instruction i : instructions)
				if(i instanceof AbstractReturn) {
					if(i == inst) { found = true; break; }
					else number++;
				}
	
			if(!found) return null;
	
			int returnNumber = 0;
			Token lastToken = null;
			for(Token token : tokens) {
				if(token.kind == RETURN) {
					if(returnNumber == number) return token;
					returnNumber++;
				}
				lastToken = token;
			}
			// If we found no return token, use the right brace.
			ClassBodyElement structure = getTokens().getMethodElement(inst.getMethod());
			return structure == null ? null : structure.getLastToken();
			
		} catch(ParseException e) { return null; }
		
	}
	
	public Token.PairedToken getLeftBracketOfArrayReference(Instruction inst) {
		
		Line line = inst.getLine();
		SortedSet<Instruction> instructions = getInstructionsOnLine(line);
		List<Token> tokens = line.getTokens();

		// Which number of array reference is this in the method?
		int number = 0;
		boolean found = false;
		for(Instruction i : instructions)
			if(i instanceof GetArrayValue || i instanceof SetArrayValue) {
				if(i == inst) { found = true; break; }
				else number++;
			}

		if(!found) return null;
		
		// Now find the corresponding left bracket in the tokens for the method.
		int leftBracketNumber = 0;
		for(Token token : tokens) {
			if(token.kind == JavaParserConstants.LBRACKET) {
				if(leftBracketNumber == number) return (PairedToken) token;
				leftBracketNumber++;
			}
		}
		return null;
		
	}
	
	public Token getTokenOfConstant(PushConstant<?> inst) {

		Line line = inst.getLine();
		SortedSet<Instruction> instructions = getInstructionsOnLine(line);
		List<Token> tokens = line.getTokens();

		int number = 0;
		boolean found = false;
		for(Instruction i : instructions)
			if(i instanceof PushConstant) {
				if(i == inst) { found = true; break; }
				else number++;
			}

		if(!found) return null;
		
		int constantNumber = 0;
		for(Token token : tokens) {
			if(token.isLiteral()) {
				if(constantNumber == number) return token;
				constantNumber++;
			}
		}
		return null;		

	}
	
	public Token getFirstTokenOfMethod(MethodInfo method) {

		Token name = getTokenForMethodName(method);
		if(name != null) return name.getLine().getFirstCodeToken();
		else return null;
		
	}

	public Token getLastTokenOfMethod(MethodInfo method) {

		try {
			ClassBodyElement m = getTokens().getMethodElement(method);
			if(m != null) return m.getLastToken();
			else return null;
		} catch(ParseException e) {
			e.printStackTrace();
			return null;
		}
		
	}
	
	public Token getTokenForClassName(Classfile classfile) {

		try {
			ClassElement c = getTokens().getClassElement(classfile);
			if(c != null) return c.getNameToken();
			else return null;
		} catch(ParseException e) {
			e.printStackTrace();
			return null;
		}
		
	}
	
	public Token getTokenForMethodName(MethodInfo method) {

		try {
			assert method.getClassfile().getSourceFile() == this : "But " + this + " isn't the source file for " + method.getQualifiedNameAndDescriptor();
			ClassBodyElement m = getTokens().getMethodElement(method);
			return m == null ? null : m.getNameToken();
		} catch(ParseException e) {
			e.printStackTrace();
			return null;
		}
				
	}
	
	public Token getFirstTokenOfMethodHeader(MethodInfo method) {
		
		Token token = getTokenForMethodName(method);
		
		if(token == null) return null;
		
		return token.getLine().getFirstCodeToken();
		
	}
	
	public String getMethodJavaDoc(MethodInfo method) {

		try {
			ClassBodyElement methodStructure = getTokens().getMethodElement(method);		
			return methodStructure == null ? "" : methodStructure.getJavaDoc();
		} catch(ParseException e) {
			return null;
		}
		
	}

	public String getClassJavaDoc(Classfile classfile) {

		try {
			if(getTokens() == null) return null;
			ClassElement classStructure = getTokens().getClassElement(classfile);		
			return classStructure == null ? "" : classStructure.getJavaDoc();
		} catch(ParseException e) {
			return null;
		}
		
	}

	/**
	 * 1= first parameter of method
	 */
	public String getNameOfParameterNumber(MethodInfo method, int number) {
		
		assert number > 0 && number <= method.getNumberOfArguments() : "" + number + " is an illegal argument number for " + method.getDescriptor();

		try {
			ClassBodyElement m = getTokens().getMethodElement(method);
			Token name = m == null ? null : m.getParameterNameToken(number);
			if(name != null) return name.getText();
			else return "arg" + number;
		} catch(ParseException e) {
			return null;
		}
		
	}
		
	public Token[] getCodeTokens() throws ParseException {  
		
		getTokens();
		if(parser == null) return null;
		Token[] t = parser.getCodeTokens();
		return t == null ? null : t;
		
	}

	public String getSourceAsString() throws ParseException {
		
		getTokens();
		if(parser != null) return parser.getString();
		else return null;
		
	}

	
	public SortedSet<Instruction> getInstructionsOnLine(Line line) {
		
		for(Classfile cf : classfiles) {
			
			SortedSet<Instruction> instructionsOnLine = cf.getInstructionsOnLineNumber(line.getLineNumber());
			if(!instructionsOnLine.isEmpty()) return instructionsOnLine;
			
		}
		
		return new TreeSet<Instruction>();
		
	}
		
	public MethodInfo getMethodOfLine(Line line) {
		
		try {
		
			SortedSet<Instruction> instructions = getInstructionsOnLine(line);
			while(instructions == null || instructions.isEmpty()) {
				line = line.getLineAfter();
				if(line == null) break;
				instructions = getInstructionsOnLine(line);		
			}
	
			if(instructions != null && !instructions.isEmpty()) {
			
				Instruction inst = instructions.first();
				if(inst != null) return inst.getMethod();
				else return null;
			
			}
			else return null;
			
		} catch(ParseException e) {
			e.printStackTrace();
			return null;
		}

	
	}
	
	public String getLocalIDNameRelativeToInstruction(int localID, Instruction inst) {

		try {
			List<SetLocal> sets = inst.getCode().getLocalDependencies().getPotentialDefinitionsOfLocalIDBefore(inst, localID);
			for(SetLocal set : sets) {
				for(Token token : set.getLine().getTokens()) {
					if(token.kind == ASSIGN) {
						Token localIdentifier = token.getPreviousCodeToken();
						if(localIdentifier.kind == IDENTIFIER)
							return localIdentifier.getText();
					}
				}
			}
		} catch(ParseException e) {
			e.printStackTrace();
			return null;
		}
		
		return null;
		
	}
	
	public String toString() { return filename; }

}
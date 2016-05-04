package edu.cmu.hcii.whyline.source;

import java.util.List;

import edu.cmu.hcii.whyline.bytecode.Classfile;
import edu.cmu.hcii.whyline.bytecode.FieldInfo;
import edu.cmu.hcii.whyline.bytecode.MethodInfo;
import edu.cmu.hcii.whyline.bytecode.NameAndTypeInfo;
import edu.cmu.hcii.whyline.bytecode.QualifiedClassName;
import edu.cmu.hcii.whyline.source.JavaParser.TokenIterator;
import edu.cmu.hcii.whyline.source.Token.PairedToken;
import static edu.cmu.hcii.whyline.source.JavaParserConstants.*;

/**
 * Represents methods, fields, and other declarations in a class body.
 * 
 * @author Andrew J. Ko
 * 
 */
public class ClassBodyElement extends JavaElement {

	private boolean isMethod;
	
	private Token name;
	private Token tokenAfterHeader;
	private ParametersElement parameters;
	private BlockElement block;
	private MethodInfo method;
	private FieldInfo field;
	
	private String simpleReturnType;
	private TokenRange returnTypeRange;
	
	public ClassBodyElement(JavaElement parent, Token first, Token last) {
	
		super(parent, first, last);
		
	}
	
	public boolean isMethod() { return isMethod; }
	
	protected void parse(TokenIterator tokens) throws ParseException {
		
		optionalModifiers(tokens);

		// Skip over type arguments.
		typeArguments(tokens);

		Token lastTypeToken = null;
		
		// If this is a constructor, there will be an identifier then a left paren.
		if(tokens.peek().isIdentifier() && tokens.peekNext().kind == LPAREN) {
			
			simpleReturnType = "V";
			
		}
		// Otherwise, we need to read the return type.
		else {
			
			if(tokens.nextKindIs(VOID)) {
				Token returnType = tokens.getNext();
				simpleReturnType = "V";
				returnTypeRange = new TokenRange(returnType, returnType);
			}
			else {
				Token firstReturnTypeToken = tokens.peek();
				simpleReturnType = type(tokens);
				simpleReturnType = NameAndTypeInfo.sourceTypeToDescriptorType(simpleReturnType);
				lastTypeToken = tokens.peek().getPreviousCodeToken();
				returnTypeRange = new TokenRange(firstReturnTypeToken, lastTypeToken);
			}
			
		}
		
		name = tokens.getNext();

		isMethod = false;
		
		// Now that we've read the modifiers and optional type, is it a method or field?
		if(tokens.nextKindIs(LPAREN)) {
	
			isMethod = true;
			
			parameters = parameters(tokens);

			Classfile classfile = getEnclosingClassfile();
			
			// Resolve this method to actual code, so that we can map the this with instructions.
			if(classfile != null) {
				String desc = getSimpleDescriptor();
				for(MethodInfo m : classfile.getDeclaredMethods()) {
					if(m.getJavaName().equals(name.getText()) && m.getSimpleDescriptor().equals(desc)) {
						method = m;
						break;
					}
				}
			}

			// Associate the type names with classes, if possible
			if(method != null)
				parameters.associateTokens(method);				
			else {
				System.err.println("Failed to resolve " + name + getSimpleDescriptor() + " in class " + classfile);
				if(classfile == null) System.err.println("\tNo class passed to this method");
				else 
					for(MethodInfo m : classfile.getDeclaredMethods()) 
						System.err.println("\t" + m.getJavaName() + m.getSimpleDescriptor());
			}
			
			if(tokens.nextKindIs(THROWS)) {

				// Read a throws declaration if there is one.
				while(tokens.hasNext() && !tokens.nextKindIs(LBRACE) && !tokens.nextKindIs(SEMICOLON))
					tokens.getNext();
				
			}

			// There's either a block or a semicolon.
			if(tokens.nextKindIs(LBRACE)) {
			
				Decompiler code = method == null || method.getCode() == null ? null : new Decompiler(getSource(), method.getCode());

				PairedToken open = (PairedToken)tokens.getNext(LBRACE);
				tokens.jumpPast(open.getAssociatedToken());
				block = new BlockElement(method, this, open, open.getAssociatedToken(), code);
				tokenAfterHeader = open;
				
			}
			else {
								
				// Read the semi colon of the abstract method.
				tokenAfterHeader = tokens.getNext(SEMICOLON);

			}
			
		}
		// Read a field.
		else {

			Classfile classfile = getEnclosingClassfile();

			// Match this field with a declared field in the given classfile.
			if(classfile != null)
				for(FieldInfo f : classfile.getDeclaredFields())
					if(f.getName().equals(name.getText())) {
						field = f;
						break;
					}

			// Match the type token with the field's class. 
			if(field != null) {
				if(lastTypeToken != null)
					getSource().linkTokenWithClassname(lastTypeToken, field.getTypeName());
			}

			// Jumps past array initializers and anonymous class declarations. 
			List<PairedToken> openBraces = passAnonymousInnerClassesUntilSemiColon(tokens);

			// We should have stopped at a semicolon.
			Token last = tokens.getNext(SEMICOLON);
			
			// Go through the open braces and look for anonymous inner classes.
			for(PairedToken brace : openBraces)
				getEnclosingClass().addInnerClass(new ClassElement(this, brace, brace.getAssociatedToken(), true));
			
			// Skip the rest of the field
			tokens.jumpPastNext(SEMICOLON);
			
		}
		
	}

	public Classfile getEnclosingClassfile() {
		
		ClassElement enclosing = parent.getEnclosingClass();
		return enclosing == null ? null : enclosing.getClassfile();
		
	}
	
	public void parseBlock() {
		
		if(block != null)
			block.parse();
		
	}
	
	public Token getNameToken() {
		
		parse();
		return name;
		
	}

	public String getJavaDoc() { return null; }

	/**
	 * 1 = first parameter of declaration
	 */
	public Token getParameterNameToken(int number) { 
		
		return parameters == null ? null : parameters.getIdentifierOfParameter(number);
		
	}

	public Token getTokenAfterHeader() { return tokenAfterHeader; }

	public TokenRange getTokenRangeForParameter(int parameter) {
		
		parse();
		
		// If asking for parameter 0, return the method name to represent the instance.
		if(parameter == 0)
			return new TokenRange(name, name);
		
		Token name = parameters.getIdentifierOfParameter(parameter);
		return new TokenRange(name, name);
		
	}

	public TokenRange  getTokenRangeOfHeader() {
		
		parse();
		return new TokenRange(firstToken, tokenAfterHeader);
		
	}

	public TokenRange getTokenRangeForReturnType() {

		return returnTypeRange;
		
	}

	protected ParametersElement parameters(TokenIterator tokens) throws ParseException {
		
		PairedToken open = (PairedToken)tokens.getNext();
		tokens.jumpPast(open.getAssociatedToken());

		return new ParametersElement(this, open, open.getAssociatedToken());
		
	}

	public boolean isInnerClassConstructor() {
		
		Classfile classfile = getEnclosingClassfile();
		return classfile != null && classfile.isInnerClass() && !classfile.isStatic() && name != null && classfile.getSimpleName().equals(name.getText());
		
	}
	
	/**
	 * If a field, returns the field's type descriptor. Otherwise, returns the methods type descriptor.
	 */
	public String getSimpleDescriptor() {

		parse();
		
		if(parameters != null) {
			
			String descriptor = parameters.getSimpleDescriptor();
			
			// Descriptors get tricky here because constructor methods of inner classes can have parameters that do not appear in source.
			// (see Java spec 15.9.2). We need to artificially insert these in the appropriate situations.
			if(isInnerClassConstructor()) {

				Classfile classfile = getEnclosingClassfile();
				QualifiedClassName outerClassName = classfile.getInternalName().getOuterClassName();				
				
				// Add everything from the descriptor derived from source except for the first paren, inserting the outer class name after the first paren.
				StringBuilder newDescriptor = new StringBuilder();
				newDescriptor.append("(");
				newDescriptor.append("L");
				newDescriptor.append(outerClassName.getText());
				newDescriptor.append(";");
				newDescriptor.append(descriptor.substring(1));
				
				// Update the return value.
				descriptor = newDescriptor.toString();
				
			}
			return descriptor;
			
		}
		else return simpleReturnType;

	}

	public MethodInfo getMethod() { return method; }

	public String toString() { return "MethodOrField " + getNameToken(); }

}
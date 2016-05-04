package edu.cmu.hcii.whyline.source;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.hcii.whyline.bytecode.Classfile;
import edu.cmu.hcii.whyline.bytecode.FieldInfo;
import edu.cmu.hcii.whyline.bytecode.MethodInfo;
import edu.cmu.hcii.whyline.source.JavaParser.TokenIterator;
import edu.cmu.hcii.whyline.source.Token.PairedToken;
import static edu.cmu.hcii.whyline.source.JavaParserConstants.*;

/**
 *
 *	ClassOrInterfaceDeclaration: 
 *		ModifiersOpt (ClassDeclaration | InterfaceDeclaration)
 *
 * @author Andrew J. Ko
 * 
 */
public class ClassElement extends JavaElement implements ClassElementContainer {

	/**
	 * True if this shouldn't expect a header and name
	 */
	private final boolean anonymous;
	
	private Token name;
	private Token lastOfHeader;

	private Classfile classfile;
		
	private final List<ClassElement> classes = new ArrayList<ClassElement>(1);
	private final List<ClassElement> enums = new ArrayList<ClassElement>(1);
	private final List<ClassElement> interfaces = new ArrayList<ClassElement>(1);
	private final List<ClassElement> annotations = new ArrayList<ClassElement>(1);
	private final List<ClassBodyElement> methodsFields = new ArrayList<ClassBodyElement>(1);

	public ClassElement(JavaElement parent, Token first, Token last, boolean anonymous) {
	
		super(parent, first, last);
		
		this.anonymous = anonymous;
		
	}

	public ClassElement getEnclosingClass() { return this; }

	public void parseBlocks() {

		for(ClassElement el : classes)
			el.parseBlocks();
		for(ClassElement el : enums)
			el.parseBlocks();
		for(ClassElement el : interfaces)
			el.parseBlocks();
		for(ClassElement el : annotations)
			el.parseBlocks();
		for(ClassBodyElement el : methodsFields)
			el.parseBlock();
		
	}

	public TokenRange getHeaderRange() {

		return new TokenRange(firstToken, lastOfHeader);
		
	}

	public ClassElement getClassBySimpleName(String name) {

		parse();

		// Before searching these classes, parse all methods and fields to find declared classes.
		for(ClassBodyElement e : methodsFields)
			e.parse();

		for(ClassElement c : classes)
			if(c.getSimpleName().equals(name))
				return c;

		return null;
	
	}

	public ClassBodyElement getFieldElement(FieldInfo field) {

		parse();
		for(ClassBodyElement element : methodsFields) {
			if(!element.isMethod() && element.getNameToken().getText().equals(field.getName()))
				return element;
		}
		return null;
	
	}

	public ClassBodyElement getMethodElement(MethodInfo method) {
		
		String simpleDescriptor = method.getSimpleDescriptor();
		
		parse();
		for(ClassBodyElement element : methodsFields) {
			if(element.getNameToken().getText().equals(method.getJavaName()) &&
				 element.getSimpleDescriptor().equals(simpleDescriptor)) {
				return element;
			}
		}
		return null;

	}
	
	protected void parse(TokenIterator tokens) throws ParseException {

		optionalModifiers(tokens);

		if(anonymous) {

			classDeclaration(tokens, true);
			
		}
		else {
		
			Token keyword = tokens.getNext();
	
			switch(keyword.kind) {
			case CLASS :
				classDeclaration(tokens, false);
				break;
			case ENUM :
				enumDeclaration(tokens);
				break;
			case INTERFACE :
				interfaceDeclaration(tokens);
				break;
			case AT:
				annotationDeclaration(tokens);
				break;
			}		
			
		}
		
	}
	
	private void classDeclaration(TokenIterator tokens, boolean anonymous) throws ParseException {

		if(anonymous) {

			tokens.getNext(LBRACE);
			
		}
		else {
		
		name = tokens.getNext();
		lastOfHeader = tokens.jumpPastNext(LBRACE);
		
		}

		// Resolve this class.
		String simpleClassQualifiedName = getSimpleClassQualifiedName();
		FileElement file = getRoot();
		for(Classfile c : file.getSource().getClassfiles()) {
			if(c.getInternalName().getSimpleClassQualifiedName().equals(simpleClassQualifiedName)) {
				classfile = c;
				break;
			}
		}

		if(classfile == null)
			System.err.println("Failed to resolve " + simpleClassQualifiedName);
		
		while(tokens.hasNext()) {

			// Done with the class!
			if(tokens.nextKindIs(RBRACE)) {
				tokens.getNext();
				break;
			}
			// Empty declaration
			else if(tokens.nextKindIs(SEMICOLON))
				tokens.getNext();
			// Class or instance initializer
			else if((tokens.nextKindIs(STATIC) && tokens.peekNext().kind == LBRACE) || tokens.nextKindIs(LBRACE)) {
				
				Token keyword = tokens.getNext();
				PairedToken blockOpen = tokens.nextPaired(LBRACE);
				tokens.jumpPast(blockOpen.getAssociatedToken());
				
			}
			// One of many other kinds of declarations.
			else {
				
				Token first = tokens.peek();
				Token last;

				optionalModifiers(tokens);

				// Where does this declaration end?
				switch(tokens.peek().kind) {
				
				// If its any of these, find the next open brace and skip over to its pair.
				case CLASS :
				case ENUM :
				case INTERFACE :
					PairedToken open = (PairedToken) tokens.jumpPastNext(LBRACE);
					last = open.getAssociatedToken();
					tokens.jumpPast(last);
					ClassElement classElement = new ClassElement(this, first, last, false);
					classes.add(classElement);
					
					break;

				case AT:
					annotationDeclaration(tokens);
					break;
					
				// For all other declarations, constructor, void methods, fields, methods, just read until the next { or ;
				default :
					
					last = tokens.getNextOr(LBRACE, SEMICOLON);
					if(last instanceof PairedToken) {
						last = ((PairedToken)last).getAssociatedToken();
						tokens.jumpPast(last);
					}
					
					ClassBodyElement el = new ClassBodyElement(this, first, last);
					methodsFields.add(el);

					break;
					
				}
				
			}
			
		}
		
	}

	private void interfaceDeclaration(TokenIterator tokens) throws ParseException {

		name = tokens.getNext();
		
		PairedToken open = (PairedToken) tokens.jumpPastNext(LBRACE);
		tokens.jumpPast(open.getAssociatedToken());
		
	}

	private void enumDeclaration(TokenIterator tokens) throws ParseException {

		name = tokens.getNext();
		
		PairedToken open = (PairedToken) tokens.jumpPastNext(LBRACE);

		tokens.jumpPast(open.getAssociatedToken());
		
	}

	private void annotationDeclaration(TokenIterator tokens) throws ParseException {

		// "interface" keyword
		tokens.getNext();
		
		name = tokens.getNext();
		
		if(tokens.nextKindIs(LPAREN)) {
			PairedToken open = tokens.nextPaired(LPAREN);
			tokens.jumpPast(open.getAssociatedToken());
		}
		
	}

	public String getSimpleName() {

		if(getNameToken() != null) return getNameToken().getText();

		ClassElement enclosing = parent.getEnclosingClass();
		if(enclosing == null) return null;
		return String.valueOf(enclosing.getIndexOf(this) + 1);
		
	}

	private String getSimpleClassQualifiedName() {
		
		ClassElement enclosing = parent.getEnclosingClass();
		if(enclosing == null) return getSimpleName();
		else {
			StringBuilder builder = new StringBuilder(enclosing.getSimpleClassQualifiedName());
			builder.append('$');
			builder.append(getSimpleName());
			return builder.toString();
		}		
		
	}

	public void addInnerClass(ClassElement c) {

		classes.add(c);
		
	}

	public int getIndexOf(ClassElement classElement) {
		
		return classes.indexOf(classElement);
		
	}

	public Token getNameToken() {
		
		parse();
		return name;
		
	}

	public Classfile getClassfile() { return classfile; }
	
	public String getJavaDoc() { return null; }
	
	public String toString() { return "ClassElement " + getSimpleName(); }

}

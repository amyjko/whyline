package edu.cmu.hcii.whyline.source;

import java.util.*;

import edu.cmu.hcii.whyline.bytecode.Classfile;
import edu.cmu.hcii.whyline.bytecode.MethodInfo;
import edu.cmu.hcii.whyline.source.JavaParser.TokenIterator;
import edu.cmu.hcii.whyline.source.Token.PairedToken;
import static edu.cmu.hcii.whyline.source.JavaParserConstants.*;

/**
 * CompilationUnit: 
 * [package QualifiedIdentifier   ;  ] {ImportDeclaration} {TypeDeclaration}
 *
 * @author Andrew J. Ko
 * 
 */
public class FileElement extends JavaElement implements ClassElementContainer {
	
	private final JavaSourceFile source;
	private final List<ClassElement> classes = new ArrayList<ClassElement>(1);
	
	public FileElement(JavaSourceFile source, Token first, Token last) {
		
		super(null, first, last);
		
		this.source = source;
		
	}
	
	public FileElement getRoot() { return this; }

	public JavaSourceFile getSource() { return source; }

	/**
	 * The main entry point to getting a source version of a class file (as opposed to asking a ClassElement directly).
	 * Gets a class qualified version of the given class's name, stripped of the package qualification, and searches for a class matching the path.
	 */
	public ClassElement getClassElement(Classfile classfile) {

		String name = classfile.getInternalName().getSimpleClassQualifiedName();
		String[] path = name.indexOf('$') >= 0 ? name.split("\\$") : new String[] { name };

		ClassElementContainer classContainer = this;
		for(int index = 0; index < path.length; index++)
			classContainer = classContainer.getClassBySimpleName(path[index]);
		
		return (ClassElement)classContainer;
		
	}

	public ClassElement getClassBySimpleName(String name) {

		parse();
		for(ClassElement c : classes)
			if(c.getSimpleName().equals(name))
				return c;
		return null;

	}
	
	public ClassBodyElement getMethodElement(MethodInfo method) {
		
		parse();
		ClassElement c = getClassElement(method.getClassfile());
		return c == null ? null : c.getMethodElement(method);
		
	}

	protected void parse(TokenIterator tokens) throws ParseException {

		if(tokens.nextKindIs(PACKAGE))
			packageDeclaration(tokens);

		while(tokens.hasNext() && tokens.nextKindIs(IMPORT))
			importDeclaration(tokens);
			
		while(tokens.hasKindBefore(CLASS, LBRACE) || tokens.hasKindBefore(INTERFACE, LBRACE) || tokens.hasKindBefore(ENUM, LBRACE)) {

			Token firstClassToken = tokens.getNext();
			while(tokens.hasNext() && !tokens.nextKindIs(LBRACE))
				tokens.getNext();
			PairedToken openBrace = (PairedToken)tokens.getNext();
			Token closingBrace = openBrace.getAssociatedToken();
			assert closingBrace != null : "Couldn't find a closing brace at " + tokens;
			ClassElement classElement = new ClassElement(this, firstClassToken, closingBrace, false);
			classes.add(classElement);

			tokens.jumpPast(closingBrace);
			
		}
		
	}

	private void packageDeclaration(TokenIterator tokens) throws ParseException {
		
		while(tokens.hasNext() && !tokens.nextKindIs(SEMICOLON))
			tokens.getNext();
		tokens.getNext();
		
	}

	private void importDeclaration(TokenIterator tokens) throws ParseException {

		while(tokens.hasNext() && !tokens.nextKindIs(SEMICOLON))
			tokens.getNext();
		tokens.getNext();

	}

	public void parseBlocks() {

		for(ClassElement element : classes)
			element.parseBlocks();

	}

	public String toString() { return "FileElement: " + source; }


}
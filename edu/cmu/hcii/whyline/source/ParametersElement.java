package edu.cmu.hcii.whyline.source;

import java.util.ArrayList;

import edu.cmu.hcii.whyline.bytecode.MethodInfo;
import edu.cmu.hcii.whyline.bytecode.NameAndTypeInfo;
import edu.cmu.hcii.whyline.bytecode.QualifiedClassName;
import edu.cmu.hcii.whyline.source.JavaParser.TokenIterator;
import static edu.cmu.hcii.whyline.source.JavaParserConstants.*;

/**
 * 
 *
 *	FormalParameters:
 *		 ( [FormalParameterDecls] )
 *
 *	FormalParameterDecls:
 *		[final] [Annotations] Type FormalParameterDeclsRest]
 *
 *	FormalParameterDeclsRest:
 *		VariableDeclaratorId [ , FormalParameterDecls] ... VariableDeclaratorId
 *
 *	VariableDeclaratorId:
 *		Identifier {[]}
 *
 * @author Andrew J. Ko
 */
public class ParametersElement extends JavaElement {

	private ArrayList<Token> identifiers;
	private ArrayList<String> types;
	
	public ParametersElement(ClassBodyElement parent, Token first, Token last) {
	
		super(parent, first, last);
		
	}

	protected void parse(TokenIterator tokens) throws ParseException {

		identifiers = new ArrayList<Token>();
		types = new ArrayList<String>();

		 tokens.getNext();
		 
		 while(tokens.hasNext() && !tokens.nextKindIs(RPAREN)) {
		 
			 if(tokens.nextKindIs(FINAL))
				 tokens.getNext();
			 
			 optionalAnnotations(tokens);
		 
			 String type = type(tokens);
			 
			 // Ellipsis get converted to arrays, so add an array type descriptor.
			 if(tokens.nextKindIs(ELLIPSIS)) {
				 tokens.getNext();
				 type = "[" + type;
			 }
			 
			 Token name = tokens.getNext();

			 // Why does java support both of this [] styles, before and after the name???
			 String moreBrackets = optionalBrackets(tokens);

			 types.add(moreBrackets + type);

			 identifiers.add(name);
			 			 
			 if(tokens.nextKindIs(COMMA))
				 tokens.getNext();
			 
		 }
		
		 tokens.getNext();
		
	}

	public String getSimpleDescriptor() {
		
		parse();
		StringBuilder builder = new StringBuilder("(");
		for(String type : types)
			builder.append(NameAndTypeInfo.sourceTypeToDescriptorType(type));
		builder.append(")");
		return builder.toString();
		
	}
	
	/**
	 * 1 refers to the first parameter in a method's header.
	 */
	public Token getIdentifierOfParameter(int number) {
		
		parse();
		
		return number < 1 || number > identifiers.size() ? null : identifiers.get(number - 1);
		
	}

	public void associateTokens(MethodInfo method) throws ParseException {

		int arg = 0;
		for(Token name : identifiers) {

			Token type = name.getPreviousCodeToken();
			while(type != null && type.kind != IDENTIFIER && type != firstToken)
				type = type.getPreviousCodeToken();

			if(type != null && type != firstToken) {
				
				QualifiedClassName classname = method.getParsedDescriptor().getTypeOfArgumentNumber(arg);
				getSource().linkTokenWithClassname(type, classname);
				
			}
			
			getSource().linkTokenWithParameter(name, new Parameter(method, method.isStatic() ? arg : arg + 1));
			
			arg++;
			
		}
		 
	}
	
}

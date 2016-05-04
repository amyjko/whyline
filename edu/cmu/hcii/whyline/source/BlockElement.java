package edu.cmu.hcii.whyline.source;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.hcii.whyline.bytecode.*;

import edu.cmu.hcii.whyline.source.JavaParser.TokenIterator;
import edu.cmu.hcii.whyline.source.Token.PairedToken;
import static edu.cmu.hcii.whyline.source.JavaParserConstants.*;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class BlockElement extends JavaElement {

	private final MethodInfo method;
	
	private ArrayList<JavaElement> elements;
	
	private final Decompiler code;
	
	public BlockElement(MethodInfo method, JavaElement parent, Token first, Token last, Decompiler code) {
	
		super(parent, first, last);

		this.method = method;
		this.code = code;
		
	}

	protected void parse(TokenIterator tokens) throws ParseException {
		
		elements = new ArrayList<JavaElement>(10);
		
		tokens.getNext();

		while(tokens.hasNext() && !tokens.nextKindIs(RBRACE)) {
			JavaElement element = blockStatement(tokens);
			if(element != null)
				elements.add(element);
		}
		
		tokens.getNext();
		
	}
	
	protected ConsumerExpression<?> consumeBytecodeExpression(Class<? extends ConsumerExpression<?>> expectedClass,Token first, Token last) throws ParseException {
		
		if(code == null) return null;

		Line expectedLine = first.getLine();

		// Don't consume anything if the next expression to consume is past this token.
		if(code.peekConsumer() != null && code.peekConsumer().getCode().getLine().getLineNumber().getNumber() > expectedLine.getLineNumber().getNumber())
			return null;
		
		ConsumerExpression<?> expr = code.consume();
		// Keep consuming expressions until we find one that matches the current token's line and expected expression type.
		while(expr != null) {

			Line expressionLine = expr.getCode().getLine();

			boolean pastLine = expressionLine.getLineNumber().getNumber() > expectedLine.getLineNumber().getNumber(); 
			boolean sameLine = expressionLine == expectedLine;
			boolean rightType = expectedClass == null || expectedClass == expr.getClass(); 
			
			if(pastLine || (expr.mayAppearInSource() && sameLine && rightType))
				break;
			
			expr = code.consume();
			
		}

		if(expr != null)
			expr.setTokenRange(first, last);
		
		return expr;
		
	}
	
	/**
	 *	BlockStatement :
	 *		LocalVariableDeclarationStatement
	 *		ClassOrInterfaceDeclaration
	 *		[Identifier :] Statement
	 *
	 * 	Statement:
	 * 		Block
	 * 		assert Expression [ : Expression] ;	
	 * 		if ParExpression Statement [else Statement]
	 * 		for ( ForControl ) Statement
	 * 		while ParExpression Statement
	 * 		do Statement while ParExpression   ;
	 * 		try Block ( Catches | [Catches] finally Block )
	 * 		switch ParExpression { SwitchBlockStatementGroups }
	 * 		synchronized ParExpression Block
	 * 		return [Expression] ;
	 * 		throw Expression   ;
	 * 		break [Identifier]
	 * 		continue [Identifier]
	 * 		;
	 * 		StatementExpression ;
	 * 		Identifier   :   Statement
	 * 
	 * @throws ParseException 
	 */
	protected JavaElement blockStatement(TokenIterator tokens) throws ParseException {

		switch(tokens.peek().kind) {
		
		case LBRACE :
			return block(tokens);

		case ASSERT :

//			tokens.getNext(ASSERT);
//
//			ConsumerExpression<?> assertion = consume(tokens, null, "assert x");
//
//			if(tokens.hasKindBefore(COLON, SEMICOLON)) {
//				tokens.jumpPastNext(COLON);
//				ConsumerExpression<?> reaction = consume(tokens, null, "assert x");
//			}
//						
			tokens.jumpPastNext(SEMICOLON);
			return null;

		case IF :

			tokens.getNext(IF);

			// Get the next expression from the class and tell it what it's token range is.
			ConsumerExpression<?> ifCondition = 
				consumeBytecodeExpression(BranchExpression.class, 
						tokens.peekPaired().getNextCodeToken(), 
						tokens.peekPairedPartner().getPreviousCodeToken());
			
			parenthesizedExpression(tokens);			
			
			blockStatement(tokens);
			if(tokens.nextKindIs(ELSE)) {
				tokens.getNext(ELSE);
				blockStatement(tokens);
			}
			return null;

		case WHILE :

			tokens.getNext(WHILE);

			ConsumerExpression<?> whleCondition = 
				consumeBytecodeExpression(BranchExpression.class,
						tokens.peekPaired().getNextCodeToken(), 
						tokens.peekPairedPartner().getPreviousCodeToken());

			parenthesizedExpression(tokens);
			blockStatement(tokens);			
			return null;

		case DO:

			tokens.getNext(DO);
			blockStatement(tokens);
			tokens.getNext(WHILE);
			
			ConsumerExpression<?> doCondition = 
				consumeBytecodeExpression(BranchExpression.class, 
						tokens.peekPaired().getNextCodeToken(), 
						tokens.peekPairedPartner().getPreviousCodeToken());

			parenthesizedExpression(tokens);
			tokens.getNext(SEMICOLON);
			return null;

		case FOR :			

			tokens.getNext(FOR);

			PairedToken openParen = tokens.nextPaired(LPAREN);
			
			// Is this an iterator or a loop?
			if(tokens.hasKindBefore(COLON, RPAREN)) {

				tokens.jumpPastNext(COLON);
				
				consumeBytecodeExpression(null, 
						tokens.peek(),
						tokens.peekBeforeNext(RPAREN));
				
				tokens.jumpPast(openParen.getAssociatedToken());

				return null;
				
			}
			else {
				
				consumeBytecodeExpression(null, 
						tokens.peek(),
						tokens.peekBeforeNext(SEMICOLON));
	
				tokens.jumpPastNext(SEMICOLON);
				
				// Optional
				if(!tokens.nextKindIs(SEMICOLON)) {
					
					consumeBytecodeExpression(BranchExpression.class, 
							tokens.peek(),
							tokens.peekBeforeNext(SEMICOLON));
					
				}
	
				tokens.jumpPastNext(SEMICOLON);
				
				// Optional
				if(!tokens.nextKindIs(RPAREN)) {
				
					consumeBytecodeExpression(null, 
							tokens.peek(),
							openParen.getAssociatedToken().getPreviousCodeToken());
					
				}

				tokens.jumpPast(openParen.getAssociatedToken());
				
				blockStatement(tokens);

				return null;
				
			}
					
		case TRY :
			tokens.getNext(TRY);
			block(tokens);
			while(tokens.nextKindIs(CATCH))  {
				
				tokens.getNext(CATCH);
				PairedToken token = tokens.nextPaired(LPAREN);
				tokens.jumpPast(token.getAssociatedToken());
				block(tokens);				
				
			}
			if(tokens.nextKindIs(FINALLY)) {
				
				tokens.getNext(FINALLY);
				block(tokens);
				
			}
			return null;

		case SWITCH :
			
			tokens.getNext(SWITCH);

			ConsumerExpression<?> switchCondition = 
				consumeBytecodeExpression(null, 
						tokens.peekPaired().getNextCodeToken(), 
						tokens.peekPairedPartner().getPreviousCodeToken());
			
			parenthesizedExpression(tokens);

			PairedToken brace = tokens.nextPaired(LBRACE);

			// Read a switch label, then read one or more block statements.
			while(tokens.nextKindIs(CASE) || tokens.nextKindIs(DEFAULT)) {

				// Read the keyword, then jump past the colon
				tokens.getNext();
				tokens.jumpPastNext(COLON);

				// Read all of the statements until finding another switch label or the closing brace. 
				while(!(tokens.nextKindIs(CASE) || tokens.nextKindIs(DEFAULT) || tokens.nextKindIs(RBRACE)))
						blockStatement(tokens);
				
			}

			tokens.nextPaired(RBRACE);

			return null;

		case SYNCHRONIZED :

			tokens.getNext(SYNCHRONIZED);

			ConsumerExpression<?> syncObject = 
				consumeBytecodeExpression(MonitorEnterExpression.class, 
						tokens.peekPaired().getNextCodeToken(), 
						tokens.peekPairedPartner().getPreviousCodeToken());

			parenthesizedExpression(tokens);
			BlockElement block = block(tokens);
			
			return block;

		case RETURN :			
			
			tokens.getNext(RETURN);
			
			if(tokens.nextKindIs(SEMICOLON)) {}
			else {
				ConsumerExpression<?> returnExpression = 
					consumeBytecodeExpression(ReturnExpression.class, 
							tokens.peek(),
							tokens.peekBeforeNext(SEMICOLON));
			}

			tokens.jumpPastNext(SEMICOLON);
			return null;
			
		case THROW :

			tokens.getNext(THROW);

			ConsumerExpression<?> throwExpression = 
				consumeBytecodeExpression(ThrowExpression.class, 
						tokens.peek(),
						tokens.peekBeforeNext(SEMICOLON));

			tokens.jumpPastNext(SEMICOLON);
			return null;

		case BREAK :
		case CONTINUE :
			tokens.getNext();
			if(tokens.nextKindIs(SEMICOLON)) {}
			else tokens.getNext();
			tokens.getNext();
			return null;

		case SEMICOLON :
			tokens.getNext();
			return null;
			
		}

		// If it wasn't any of the above, try these.
		
		// Labeled statement.
		if(tokens.nextKindIs(IDENTIFIER) && tokens.peekNext().kind == COLON) {
			
			tokens.getNext();
			tokens.getNext();
			return blockStatement(tokens);
			
		}
		// Class, interface, enum declaration
		else if(tokens.hasKindBefore(CLASS, LBRACE) || tokens.hasKindBefore(INTERFACE, LBRACE) || tokens.hasKindBefore(ENUM, LBRACE)) {

			Token first = tokens.getNext();
			PairedToken open = (PairedToken)tokens.jumpPastNext(LBRACE);
			if(open == null) {
				return null;
			}
			getEnclosingClass().addInnerClass(new ClassElement(this, open, open.getAssociatedToken(), false));
			return null;
			
		}
		// Statement expression. These are mostly straightforward, except for anonymous inner classes and array initializations.
		// We detect these two complicated cases and jump over them.
		else {

			Token first = tokens.peek();

			List<PairedToken> openBraces = passAnonymousInnerClassesUntilSemiColon(tokens);
			
			// We should have stopped at a semicolon.
			Token last = tokens.getNext(SEMICOLON);
			
			// Go through the open braces and look for anonymous inner classes.
			for(PairedToken brace : openBraces) {
				
				getEnclosingClass().addInnerClass(new ClassElement(this, brace, brace.getAssociatedToken(), true));
				
			}

			ConsumerExpression<?> expr = consumeBytecodeExpression(null, first,last);
			
			return null;
			
		}
		
	}

	protected BlockElement block(TokenIterator tokens) throws ParseException {
		
		PairedToken token = tokens.nextPaired(LBRACE);
		tokens.jumpPast(token.getAssociatedToken());
		BlockElement block = new BlockElement(method, this, token, token.getAssociatedToken(), code);
		
		// We need it to process the instructions.
		block.parse();
		
		return block;
		
	}
	
	/**
	 * LocalVariableDeclarationStatement: [final] Type VariableDeclarators   ;
	 * @throws ParseException 
	 */
	protected void local(TokenIterator tokens) throws ParseException {

		tokens.jumpPastNext(SEMICOLON);
		
	}
	
}
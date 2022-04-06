grammar Efx;

options { tokenVocab = EfxLexer;}

/* 
 * A single-expression is typically used to evaluate a condition.
 * If you do not need to process EFX templates, then you can create a full EFX parser that parses these expressions.
 * A single-expression contains two parts: a context-declaration and an expression-block.
 * Currently we only allow a field-identifier or a node-identifier in the context-declaration.
 * We may also add support for adding one or more predicates to the context-declaration in the future.
 */
singleExpression: (FieldContext | NodeContext) ColonColon expressionBlock EOF;

/* 
 * A template-file is a series of template-lines.
 */
templateFile: (templateLine /* additonalTemplateLine* */)* EOF;

/* 
 * A template line contains three parts: indentation, context-declaration and template.
 * Python-style indentation is used to structure the template-lines hierarchicaly.
 * The context-declaration part specifies the XML element(s) that will trigger the generation 
 * of output for this template-line. The template-line will appear in the final output as many 
 * times as the number of XML elements matched by the context-declaration.
 * Furthermore, all the expression-blocks in the template part of this template-line will
 * be evaluated relative to the context indicated by the context-declaration. 
 */
templateLine: indent = (Tabs | Spaces)? contextExpressionBlock ColonColon template CRLF;
// additonalTemplateLine: indent=(Tabs | Spaces)? ColonColon txt=template CRLF;

contextDeclaration: contextExpressionBlock;
template: templateFragment;

/*
 * A template is a combination of free-text, labels and expressions to be evaluated.
 * Whitespace is significant within the template, but is ignored when present at its begining or end.
 */
templateFragment
	: text templateFragment?				# textTemplate
	| labelBlock templateFragment?			# labelTemplate
	| expressionBlock templateFragment?		# valueTemplate
	;

/*
 * A label-block starts with a # and contains a label identifier inside curly braces.
 */
labelBlock
	: StartLabel assetType Pipe labelType Pipe assetId EndLabel			# standardLabelReference
	| StartLabel labelType Pipe BtAssetId EndLabel						# shorthandBtLabelTypeReference
	| StartLabel labelType Pipe FieldAssetId EndLabel					# shorthandFieldLabelTypeReference
	| StartLabel BtAssetId EndLabel										# shorthandBtLabelReference
	| StartLabel FieldAssetId EndLabel									# shorthandFieldLabelReference
	| StartLabel OpenValueBlock FieldAssetId CloseValueBlock EndLabel	# shorthandFieldValueLabelReference
	| SelfLabel 														# selfLabeleReference
	;

/* 
 * An expression-block starts with a $ and contains the expression to be evaluated inside curly braces.
 */
expressionBlock
	: StartExpression condition EndExpression
	| StartNestedExpression condition EndExpression
	| SelfValue
	;

/*
 * A context-declaration is contained within curly braces and can be either 
 * a field-identifier or a node-identifier followed by an optional predicate.
 */
contextExpressionBlock
	: StartContextExpression fieldReference EndExpression
	| StartContextExpression nodeReference EndExpression
	;

assetType: AssetType | expressionBlock;
labelType: LabelType | expressionBlock;
assetId
	: BtAssetId
	| FieldAssetId
	| CodelistAssetId
	| OtherAssetId
	| expressionBlock
	;

text: whitespace | FreeText+ text*;

whitespace: Whitespace+;

context: field = FieldId Colon Colon;

/*
 * Conditions
 */
condition
	: condition operator = Or condition					# logicalOrCondition
	| condition operator = And condition				# logicalAndCondition
	| Not condition										# logicalNotCondition
	| OpenParenthesis condition CloseParenthesis		# parenthesizedCondition
	| Always											# alwaysCondition
	| Never												# neverCondition
	| expression operator = Comparison expression		# comparisonCondition
	| expression modifier = Not? In list				# inListCondition
	| expression Is modifier = Not? Empty				# emptinessCondition
	| reference Is modifier = Not? Present				# presenceCondition
	| expression modifier = Not? Like pattern = STRING	# likePatternCondition
	| expression										# expressionCondition
	;

/*
 * Expressions
 */
expression
	: expression operator = Multiplication expression	# multiplicationExpression
	| expression operator = Addition expression			# additionExpression
	| OpenParenthesis expression CloseParenthesis		# parenthesizedExpression
	| value												# valueExpression
	;

list
	: OpenParenthesis value (Comma value)* CloseParenthesis	# explicitList
	| codelistReference										# codeList
	;

value: literal | reference | functionCall;

literal: STRING | INTEGER | DECIMAL | UUIDV4;

predicate: condition;

/*
 * References
 */
reference
	: fieldReference (SlashAt attribute = Identifier)?		# simpleReference
	| ctx = context reference								# referenceWithContextOverride
	;

fieldReference
	: fieldReference OpenBracket predicate CloseBracket		# fieldReferenceWithPredicate
	| noticeReference Slash fieldReference					# fieldInNoticeReference
	| FieldId												# simpleFieldReference
	;

nodeReference
	: nodeReference OpenBracket predicate CloseBracket		# nodeReferenceWithPredicate
	| NodeId												# simpleNodeReference
	;

noticeReference: Notice OpenParenthesis noticeId=expression CloseParenthesis;
codelistReference: Codelist? OpenParenthesis codeListId=codelistId CloseParenthesis;
codelistId: CodelistId;

/*
 * Function calls
 */
functionCall: FunctionName OpenParenthesis arguments? CloseParenthesis;
arguments: argument (Comma argument)*;
argument: condition;


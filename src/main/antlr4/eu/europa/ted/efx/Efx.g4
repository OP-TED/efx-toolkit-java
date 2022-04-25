grammar Efx;

options { tokenVocab = EfxLexer;}

/*** Using the lexer's DEFAULT_MODE ***/

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
templateFile: (templateLine)* EOF;

/* 
 * A template line contains three parts: indentation, context-declaration and template.
 * Python-style indentation is used to structure the template-lines hierarchicaly.
 * The context-declaration part specifies the XML element(s) that will trigger the generation 
 * of output for this template-line. The template-line will appear in the final output as many 
 * times as the number of XML elements matched by the context-declaration.
 * Furthermore, all the expression-blocks in the template part of this template-line will
 * be evaluated relative to the context indicated by the context-declaration. 
 */
templateLine: indent = (Tabs | Spaces)? contextDeclarationBlock ColonColon template CRLF;


/*** Templates are matched when the lexical analyser is in LABEL mode ***/

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


text: whitespace | FreeText+ text*;

whitespace: Whitespace+;

/*** Labels are matched when the lexical analyser is in LABEL mode ***/


/*
 * A label-block starts with a # and contains a label identifier inside curly braces.
 */
labelBlock
	: StartLabel assetType Pipe labelType Pipe assetId EndLabel			# standardLabelReference
	| StartLabel labelType Pipe BtAssetId EndLabel						# shorthandBtLabelReference
	| StartLabel labelType Pipe FieldAssetId EndLabel					# shorthandFieldLabelReference
	| StartLabel FieldAssetId EndLabel									# shorthandFieldValueLabelReference
	| StartLabel LabelType EndLabel										# shorthandContextLabelReference
	| ShorthandContextFieldLabelReference								# shorthandContextFieldLabelReference
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


/*** Expressions are matched when the lexical analyser is in EXPRESSION mode ***/


/* 
 * An expression-block starts with a $ and contains the expression to be evaluated inside curly braces.
 */
expressionBlock
	: StartExpression expression EndExpression	# standardExpressionBlock
	| ShorthandContextFieldValueReference		# shorthandContextFieldValueReference
	;

/*
 * A context-declaration is contained within curly braces and can be either 
 * a field-identifier or a node-identifier followed by an optional predicate.
 */
contextDeclarationBlock
	: StartExpression fieldReference EndExpression
	| StartExpression nodeReference EndExpression
	;



context: field = FieldId Colon Colon;

/*
 * Expressions
 */

expression: numericExpression | stringExpression | booleanExpression | dateExpression | timeExpression | durationExpression;

booleanExpression
	: OpenParenthesis booleanExpression CloseParenthesis			# parenthesizedBooleanExpression
	| booleanExpression operator=Or booleanExpression				# logicalOrCondition
	| booleanExpression operator=And booleanExpression				# logicalAndCondition
	| stringExpression modifier = Not? In list						# inListCondition
	| stringExpression Is modifier = Not? Empty						# emptinessCondition
	| setReference Is modifier = Not? Present						# presenceCondition
	| stringExpression modifier = Not? Like pattern = STRING		# likePatternCondition
	| fieldValueReference operator = Comparison fieldValueReference	# fieldValueComparison
	| booleanExpression operator = Comparison booleanExpression		# booleanComparison
	| numericExpression operator = Comparison numericExpression		# numericComparison
	| stringExpression operator = Comparison stringExpression		# stringComparison
	| dateExpression operator = Comparison dateExpression			# dateComparison
	| timeExpression operator = Comparison timeExpression			# timeComparison
	| durationExpression operator = Comparison durationExpression	# durationComparison
	| booleanLiteral 												# booleanLiteralExpression
	| booleanFunction 												# booleanFunctionExpression
	| fieldValueReference 											# booleanReferenceExpression
	;
	
numericExpression
	: numericExpression operator=Multiplication numericExpression	# multiplicationExpression
	| numericExpression operator=Addition numericExpression			# additionExpression
	| OpenParenthesis numericExpression CloseParenthesis			# parenthesizedNumericExpression
	| numericLiteral 												# numericLiteralExpression
	| numericFunction 												# numericFunctionExpression
	| fieldValueReference											# numericReferenceExpression
	;

stringExpression: stringLiteral | stringFunction | fieldValueReference;

dateExpression: dateLiteral | dateFunction | fieldValueReference;

timeExpression: timeLiteral | timeFunction | fieldValueReference;

durationExpression: durationLiteral | durationFunction | fieldValueReference;

list: OpenParenthesis expression (Comma expression)* CloseParenthesis	# explicitList
	| codelistReference													# codeList
	;

predicate: booleanExpression;

/*
 * Literals
 */

literal: numericLiteral | stringLiteral | booleanLiteral | dateLiteral | timeLiteral | durationLiteral;
stringLiteral: STRING | UUIDV4;
numericLiteral: INTEGER | DECIMAL;
booleanLiteral: trueBooleanLiteral | falseBooleanLiteral;
trueBooleanLiteral: Always | True;
falseBooleanLiteral: Never | False;
dateLiteral: DATE;
timeLiteral: TIME;
dateTimeLiteral: DATETIME;
durationLiteral: DURATION;


/*
 * References
 */

fieldValueReference
	: fieldReference						# untypedFieldValueReference
	| fieldReference SlashAt Identifier		# untypedAttributeValueReference 
	;

setReference: fieldReference;

fieldReference
	: fieldReference OpenBracket predicate CloseBracket		# fieldReferenceWithPredicate
	| noticeReference Slash fieldReference					# fieldInNoticeReference
	| ctx = context fieldReference							# referenceWithContextOverride
	| FieldId												# simpleFieldReference
	;

nodeReference
	: nodeReference OpenBracket predicate CloseBracket		# nodeReferenceWithPredicate
	| NodeId												# simpleNodeReference
	;

noticeReference: Notice OpenParenthesis noticeId=stringExpression CloseParenthesis;

codelistReference: Codelist? OpenParenthesis codeListId=codelistId CloseParenthesis;
codelistId: CodelistId;


/*
 * Function calls
 */

booleanFunction
	: Not OpenParenthesis booleanExpression CloseParenthesis																# notFunction
	| ContainsFunction OpenParenthesis haystack=stringExpression Comma needle=stringExpression CloseParenthesis 			# containsFunction
	| StartsWithFunction OpenParenthesis haystack=stringExpression Comma needle=stringExpression CloseParenthesis			# startsWithFunction
	| EndsWithFunction OpenParenthesis haystack=stringExpression Comma needle=stringExpression CloseParenthesis				# endsWithFunction
	;

numericFunction
	: CountFunction OpenParenthesis setReference CloseParenthesis 										# countFunction
	| NumberFunction OpenParenthesis (stringExpression | fieldValueReference) CloseParenthesis			# numberFunction
	| SumFunction OpenParenthesis setReference CloseParenthesis 										# sumFunction
	| StringLengthFunction OpenParenthesis (stringExpression | fieldValueReference) CloseParenthesis	# stringLengthFunction
	;

stringFunction
	: SubstringFunction OpenParenthesis stringExpression Comma start=numericExpression (Comma length=numericExpression)? CloseParenthesis 	# substringFunction
	| StringFunction OpenParenthesis numericExpression CloseParenthesis																		# toStringFunction
	| ConcatFunction OpenParenthesis stringExpression (Comma stringExpression)* CloseParenthesis											# concatFunction
	| FormatNumberFunction OpenParenthesis numericExpression (Comma format=stringExpression)? CloseParenthesis								# formatNumberFunction
	;


dateFunction
	: DateFunction OpenParenthesis stringExpression CloseParenthesis		# dateFromStringFunction
	;

timeFunction
	: TimeFunction OpenParenthesis stringExpression CloseParenthesis		# timeFromStringFunction
	;


durationFunction
	: DurationFunction OpenParenthesis start=dateExpression Comma end=dateExpression CloseParenthesis 	# durationFromDatesFunction
	;
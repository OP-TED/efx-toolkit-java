grammar Efx;
	
options { tokenVocab = EfxLexer; }

singleExpression: Context ColonColon expressionBlock EOF;

// A template file contains template lines.
templateFile: (templateLine /* additonalTemplateLine* */)* EOF;

// A template line contains a template which may or may not be indented with tabs or spaces.
templateLine: indent=(Tabs | Spaces)? Context ColonColon txt=template CRLF;
// additonalTemplateLine: indent=(Tabs | Spaces)? ColonColon txt=template CRLF;

// A template is a combination of free-text, label placeholders and expressions to be evaluated.
template: txt=text template? 			# textTemplate
	| lbl=labelBlock template? 			# labelTemplate
	| val=expressionBlock template? 	# valueTemplate
	;

// A label block starts with a # and contains a label identifier inside curly braces.
labelBlock
	: StartLabel assetType Pipe labelType Pipe assetId EndLabel 		# standardLabelReference
	| StartLabel labelType Pipe BtAssetId EndLabel 						# shorthandBtLabelTypeReference
	| StartLabel labelType Pipe FieldAssetId EndLabel 					# shorthandFieldLabelTypeReference
	| StartLabel BtAssetId EndLabel 									# shorthandBtLabelReference
	| StartLabel FieldAssetId EndLabel 		    						# shorthandFieldLabelReference
	| StartLabel OpenValueBlock FieldAssetId CloseValueBlock EndLabel	# shorthandFieldValueLabelReference
	| SelfLabel															# selfLabeleReference
	;

// A value reference starts with a $ and contains an expression to be evaluated inside curly braces.
expressionBlock
	: StartExpression condition EndExpression
	| StartNestedExpression condition EndExpression
	| SelfValue
	;

assetType: AssetType | expressionBlock;
labelType: LabelType | expressionBlock;
assetId: BtAssetId | FieldAssetId | CodelistAssetId | expressionBlock;

text: whitespace | FreeText+ text*;

whitespace: Whitespace+;

context: field=FieldId Colon Colon;

/*
 * Conditions
 */
condition: condition operator=Or condition			# logicalOrCondition
	| condition operator=And condition				# logicalAndCondition
	| Not condition									# logicalNotCondition
	| OpenParenthesis condition CloseParenthesis	# parenthesizedCondition
	| Always										# alwaysCondition
	| Never											# neverCondition
	| expression operator=Comparison expression 	# comparisonCondition
	| expression modifier=Not? In list				# inListCondition
	| expression Is modifier=Not? Empty				# emptinessCondition
	| reference Is modifier=Not? Present			# presenceCondition
	| expression modifier=Not? Like pattern=STRING	# likePatternCondition
	| expression									# expressionCondition
	;

/*
 * Expressions
 */
expression: expression operator=Multiplication expression 	# multiplicationExpression
	| expression operator=Addition expression				# additionExpression
	| OpenParenthesis expression CloseParenthesis			# parenthesizedExpression
	| value													# valueExpression
	;
	
list: OpenParenthesis value (Comma value)* CloseParenthesis	# explicitList 
	| codelistReference										# codeList
	;

value: literal | reference | functionCall;

literal: STRING | INTEGER | DECIMAL | UUIDV4;

/*
 * References
 */
reference: 
	fieldReference (SlashAt attribute=Identifier)?		# simpleReference
	| ctx=context reference								# referenceWithContextOverride
	;

fieldReference:
	ref=fieldReference OpenBracket pred=predicate CloseBracket	#fieldReferenceWithPredicate
	| notice=noticeReference Slash fieldReference				#fieldInNoticeReference
	| field=FieldId												#simpleFieldReference
	;

predicate: condition;

nodeReference: node=NodeId;

noticeReference: Notice OpenParenthesis noticeId=expression CloseParenthesis;
codelistReference: Codelist? OpenParenthesis codeListId=codelistId CloseParenthesis;
codelistId: CodelistId;

/*
 * Function calls
 */
functionCall: FunctionName OpenParenthesis arguments? CloseParenthesis;
arguments: argument (Comma argument)*;
argument: condition;


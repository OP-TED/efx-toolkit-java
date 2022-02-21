grammar Efx;
	
testfile: (statement | lineComment)+ EOF;
lineComment: Comment;

statement: context condition ';';

axis: '.';

context: (axis '//')? field=FieldId ':';

/*
 * Conditions
 */
condition: condition operator='or' condition			# logicalOrCondition
	| condition operator='and' condition				# logicalAndConditon
	| 'not' condition									# logicalNotCondition
	| '(' condition ')'									# parenthesizedCondition
	| 'ALWAYS'											# alwaysCondition
	| 'NEVER'											# neverCondition
	| expression operator=('==' | '!=' | '>' | '>=' | '<' | '<=') expression 	# comparisonCondition
	| expression modifier='not'? 'in' list				# inListCondition
	| expression 'is' modifier='not'? 'empty'			# emptynessCondition
	| reference 'is' modifier='not'? 'present'			# presenceCondition
	| expression modifier='not'? 'like' pattern=STRING	# likePatternCondition
	| expression										# expressionCondition
	;

/*
 * Expressions
 */
expression: expression operator=('*' | '/' | '%') expression 	#multiplicationExpression
	| expression operator=('+' | '-') expression				#additionExpression
	| '(' expression ')'										#parenthesizedExpression
	| value														#valueExpression
	;
	
list: '{' value (',' value)* '}';

value: literal | reference | functionCall;

literal: STRING | INTEGER | DECIMAL | UUIDV4;

/*
 * References
 */
reference: 
	fieldReference ('/@' attribute=Identifier)?		# simpleReference
	| ctx=context reference							# referenceWithContextOverride
	;

fieldReference:
	ref=fieldReference '[' pred=predicate ']'	#fieldReferenceWithPredicate
	| notice=noticeReference '/' fieldReference	#fieldInNoticeReference
	| field=FieldId								#simpleFieldReference
	;

predicate: condition;

nodeReference: node=NodeId;

noticeReference: 'notice' '(' noticeId=expression ')';

/*
 * Function calls
 */
functionCall: FunctionName '(' arguments? ')';
arguments: argument (',' argument)*;
argument: expression;


FunctionName: 'TODAY' | 'NOW' | 'PARENT';
NodeId: 'ND' '-' DIGIT+;
FieldId: ('BT' | 'OPP' | 'OPT') '-' INTEGER ('(' (BtId | [a-z]) ')')? ('-' Identifier)+;
BtId: 'BT-' DIGIT+;

Identifier: LETTER (LETTER | DIGIT)* ;


INTEGER: DIGIT+;
DECIMAL: DIGIT? '.' DIGIT+;
STRING: ('"' CHAR_SEQ? '"') | ('\'' CHAR_SEQ? '\'');
UUIDV4: '{' HEX4 HEX4 '-' HEX4 '-' HEX4 '-' HEX4 '-' HEX4 HEX4 HEX4 '}';

fragment HEX4: HEX HEX HEX HEX;
fragment HEX: [0-9a-fA-F];
fragment CHAR_SEQ: CHAR+;
fragment CHAR: ~["'\\\r\n] | ESC_SEQ;
fragment ESC_SEQ: '\\' ["'\\];
fragment LETTER: [a-zA-Z_];
fragment DIGIT: [0-9];

WS: [ \t]+  -> skip;
CRLF:('\r' '\n'? | '\n') -> skip;
Comment:   '//' ~[\r\n]*;
lexer grammar EfxLexer;

/*
 * DEFAULT mode
 */

// The Context has the same definition as a FieldId or NodeId in EXPRESSION mode
FieldContext: ('BT' | 'OPP' | 'OPT') '-' [0-9]+ ('(' (('BT' '-' [0-9]+) | [a-z]) ')')? ('-' ([a-zA-Z_] ([a-zA-Z_] | [0-9])*))+;
NodeContext: 'ND' '-' [0-9]+;

// Empty lines and comment lines are to be ignored by the parser.
Comment: [ \t]* '//' ~[\r\n\f]* EOL* -> skip;
EmptyLine: [ \t]* EOL+ -> skip;

// Tabs and spaces are used to express structure through indentation (like in Python).  
Tabs: Tab+;
Spaces: Space+;
fragment Tab: [\t];
fragment Space: [ ];

EOL: ('\r'? '\n' | '\r' | '\f');

// A double colon triggers a mode change (from default mode to TEMPLATE mode).
ColonColon: [ \t]* '::' [ \t]* -> pushMode(TEMPLATE);

/*
 * The context of a row can be either a field or a node refernece, followed by one or more optional
 * predicates. In order to be able to parse any predicates, we need to treat the context declaration
 * as an expression block. Therefore the curly brace that opens a context declaration block, switches
 * the lexer to EXPRESSION mode.
 */
StartContextExpression: '{' -> pushMode(EXPRESSION), type(StartExpression);

/*
 * TEMPLATE mode In template mode, whitespace is significant. In this mode we are looking for the
 * text that is tho be displayed. The text can contain placeholders for labels and expressions.
 */
mode TEMPLATE;

// A newline terminates TEMPLATE mode and switches back to DEFAULT mode.
CRLF: ('\r'? '\n' | '\r' | '\f') -> popMode;

FreeText: CharSequence+;
fragment CharSequence: Char+;
fragment Char: ~[\r\n\f\t #$}{];

fragment Dollar: '$';	// Used for label placeholders
fragment Sharp: '#';	// Used for expression placeholders

ShorthandContextFieldValueReference: Dollar 'value';
ShorthandContextFieldLabelReference: Sharp 'value';

ShorthandLabelType: LabelType -> type(LabelType);

fragment OpenBrace: '{';

StartExpression: Dollar OpenBrace -> pushMode(EXPRESSION);
StartLabel: Sharp OpenBrace -> pushMode(LABEL);

Whitespace: [\t ];

/*
 * 
 */
mode LABEL;

Pipe: '|';

EndLabel: '}' -> popMode;

StartNestedExpression: '$' '{' -> pushMode(EXPRESSION), type(StartExpression);

AssetType: ASSET_TYPE_BT | ASSET_TYPE_FIELD | ASSET_TYPE_CODE | ASSET_TYPE_INDICATOR | ASSET_TYPE_DECORATION;
ASSET_TYPE_BT: 'business_term';
ASSET_TYPE_FIELD: 'field';
ASSET_TYPE_CODE: 'code';
ASSET_TYPE_INDICATOR: 'indicator';
ASSET_TYPE_DECORATION: 'decoration';

LabelType: LABEL_TYPE_NAME | LABEL_TYPE_VALUE;
LABEL_TYPE_NAME: 'name';
LABEL_TYPE_VALUE: 'value';

FieldAssetId: BtAssetId ('(' (('BT' '-' [0-9]+) | [a-z]) ')')? ('-' [a-zA-Z_] [a-zA-Z0-9_]*)+;// ->type(FieldId);
BtAssetId: ('BT' | 'OPP' | 'OPT') '-' [0-9]+;// -> type(BtId);
CodelistAssetId: 'CL' ('-' [a-zA-Z_] [a-zA-Z0-9_]*)+;
OtherAssetId: [a-z]+ ('-' [a-z0-9]*)*;

/*
 * EXPRESSION mode
 * 
 * This lexer mode is used in efx expression blocks and context expression blocks.
 */

mode EXPRESSION;

OpenParenthesis: '(';
CloseParenthesis: ')';
OpenBracket: '[';
CloseBracket: ']';

Dash: '-';

/*
 * Curly braces are not used by expressions themselves. So we use them to indicate the start and end
 * of an expression block, and to switch in and out of EXPRESSION mode.
 */
EndExpression: '}' -> popMode;

/*
 * Keywords
 */

And: 'and';
Or: 'or';
Is: 'is';
In: 'in';
Like: 'like';
Present: 'present';
Empty: 'empty';
Always: 'ALWAYS';
Never: 'NEVER';
True: 'TRUE';
False: 'FALSE';
Notice: 'notice';
Codelist: 'codelist';


/*
 * Functions
 */

Not: 'not';
CountFunction: 'count';
SubstringFunction: 'substring';
StringFunction: 'string';
NumberFunction: 'number';
ContainsFunction: 'contains';
StartsWithFunction: 'starts-with';
EndsWithFunction: 'ends-with';
StringLengthFunction: 'string-length';
SumFunction: 'sum';
FormatNumberFunction: 'format-number';
ConcatFunction: 'concat';
DateFunction: 'date';
TimeFunction: 'time';
DateTimeFunction: 'date-time';
DurationFunction: 'duration';
AddDaysFunction: 'add-days';
AddWeeksFunction: 'add-weeks';
AddMonthsFunction: 'add-months';
AddYearsFunction: 'add-years';


NodeId: 'ND' Dash DIGIT+;
FieldId: ('BT' | 'OPP' | 'OPT') Dash INTEGER (OpenParenthesis (BtId | [a-z]) CloseParenthesis)? (Dash Identifier)+;
BtId: 'BT' Dash DIGIT+;
CodelistId: Identifier (Dash Identifier)*;

Identifier: LETTER (LETTER | DIGIT)*;

INTEGER: DIGIT+;
DECIMAL: DIGIT? '.' DIGIT+;
STRING: ('"' CHAR_SEQ? '"') | ('\'' CHAR_SEQ? '\'');
UUIDV4: '{' HEX4 HEX4 Dash HEX4 Dash HEX4 Dash HEX4 Dash HEX4 HEX4 HEX4 '}';
DATE: DIGIT DIGIT DIGIT DIGIT Dash DIGIT DIGIT Dash DIGIT DIGIT;
TIME: DIGIT DIGIT Colon DIGIT DIGIT Colon DIGIT DIGIT;
DATETIME: DATE WS TIME;
DURATION: 'P' INTEGER ('Y' | 'M' | 'W' | 'D');

Comparison: '==' | '!=' | '>' | '>=' | '<' | '<=';
Multiplication: '*' | '/' | '%';
Addition: '+' | '-';
Comma: ',';
Slash: '/';
SlashAt: '/@';
Colon: ':';

fragment HEX4: HEX HEX HEX HEX;
fragment HEX: [0-9a-fA-F];
fragment CHAR_SEQ: CHAR+;
fragment CHAR: ~["'\\\r\n] | ESC_SEQ;
fragment ESC_SEQ: '\\' ["'\\];
fragment LETTER: [a-zA-Z_];
fragment DIGIT: [0-9];

WS: [ \t]+ -> skip;

lexer grammar EfxLexer;

/*  
 * DEFAULT mode
*/

// The Context has the same definition as a FieldId or NodeId in EXPRESSION mode
FieldContext: ('BT' | 'OPP' | 'OPT') '-' [0-9]+ ('(' (('BT' '-' [0-9]+) | [a-z]) ')')? ('-' ([a-zA-Z_] ([a-zA-Z_] | [0-9])*))+;
NodeContext: 'ND' '-' [0-9]+;

// Empty lines and comment lines are to be ignored by the parser.
Comment: [ \t]* '//' ~[\r\n\f]* EOL* ->skip;
EmptyLine: [ \t]* EOL+ ->skip;

// Tabs and spaces are used to express structure through indentation (like in Python).  
Tabs: Tab+;
Spaces: Space+;
fragment Tab: [\t];
fragment Space: [ ];

EOL: ('\r'? '\n' | '\r' | '\f');

// A double colon triggers a mode change (from default mode to TEMPLATE mode).
ColonColon: [ \t]* '::' [ \t]* -> pushMode(TEMPLATE);


/*
 * TEMPLATE mode
 * In template mode, whitespace is significant. 
 * In this mode we are looking for the text that is tho be displayed.
 * The text can contain placeholders for labels and expressions.
 */
mode TEMPLATE;

// A newline terminates TEMPLATE mode and switches back to DEFAULT mode.
CRLF: ( '\r'? '\n' | '\r' | '\f' ) -> popMode;


FreeText: CharSequence+;
fragment CharSequence: Char+;
fragment Char: ~[\r\n\f\t #$}{];

fragment Dollar: '$';    // Used for label placeholders
fragment Sharp: '#';     // Used for expression placeholders

SelfLabel: Sharp 'label';    
SelfValue: Dollar 'value';


fragment OpenBrace : '{';

StartExpression: Dollar OpenBrace -> pushMode(EXPRESSION);
StartLabel: Sharp OpenBrace -> pushMode(LABEL);

Whitespace: [\t ];

/*
 *
 */
mode LABEL;

Pipe: '|';

OpenValueBlock: '[';
CloseValueBlock: ']';

EndLabel : '}' -> popMode;

StartNestedExpression: NestedDollar NestedOpenBrace -> pushMode(EXPRESSION);
fragment NestedDollar: '$';
fragment NestedOpenBrace: '{';

AssetType: 'business_term' | 'field' | 'code' | 'decoration';
LabelType: 'name' | 'value';
FieldAssetId: BtAssetId ('(' (('BT' '-' [0-9]+) | [a-z]) ')')? ('-' [a-zA-Z_] [a-zA-Z0-9_]*)+;
BtAssetId: ('BT' | 'OPP' | 'OPT') '-' [0-9]+;
CodelistAssetId: 'CL' ('-' [a-zA-Z_] [a-zA-Z0-9_]*)+;
OtherAssetId: [a-z]+ ('-' [a-z0-9]*)*;

/*
 *
 */

mode EXPRESSION;

OpenParenthesis: '(';
CloseParenthesis: ')';
OpenBracket: '[';
CloseBracket: ']';

Dash: '-';
EndExpression : '}' -> popMode;

/*
 * Keywords
 */

And: 'and';
Or: 'or';
Not: 'not';
Is: 'is';
In: 'in';
Like: 'like';
Present: 'present';
Empty: 'empty';
Always: 'ALWAYS';
Never: 'NEVER';
Notice: 'notice';
Codelist: 'codelist';


FunctionName: 'substring' | 'substring-after' | 'substring-before' 
    | 'contains' | 'starts-with' | 'ends-with' | 'matches'
    | 'concat' | 'normalize-space' | 'format-number'
    | 'escape-html-uri'
    | 'string-length' | 'count'
    | 'upper-case' | 'lower-case'
    | 'ceiling' | 'floor' | 'sum'
    | 'string' | 'number'
    | 'true' | 'false'
    ;

NodeId: 'ND' Dash DIGIT+;
FieldId: ('BT' | 'OPP' | 'OPT') Dash INTEGER (OpenParenthesis (BtId | [a-z]) CloseParenthesis)? (Dash Identifier)+;
BtId: 'BT' Dash DIGIT+;
CodelistId: Identifier (Dash Identifier)*;

Identifier: LETTER (LETTER | DIGIT)*;

INTEGER: DIGIT+;
DECIMAL: DIGIT? '.' DIGIT+;
STRING: ('"' CHAR_SEQ? '"') | ('\'' CHAR_SEQ? '\'');
UUIDV4: '{' HEX4 HEX4 Dash HEX4 Dash HEX4 Dash HEX4 Dash HEX4 HEX4 HEX4 '}';


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

WS: [ \t]+  -> skip;

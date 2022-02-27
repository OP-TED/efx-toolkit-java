lexer grammar AbstractXpathLexer;

DoubleSlash: '//';
Slash: '/';
Coma: ',';

EnterPredicate: '[' ->  pushMode(PREDICATE);
StepText: StepChar+;
StepChar: ~[/\\[\], \t\r\n];
WhiteSpace: [ \t] -> skip;
EOL: [\n\r];

mode PREDICATE;

EnterNestedPredicate: '[' ->  pushMode(PREDICATE);
ExitPredicate: ']' ->  popMode;
PredicateText: . ;

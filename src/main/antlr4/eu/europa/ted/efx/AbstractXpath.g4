grammar AbstractXpath;

options { tokenVocab=AbstractXpathLexer; }

file: pair (EOL+ pair EOL*)* EOF;
pair: context=path Coma xpath=path;
path: step+;
step : (Slash | DoubleSlash)? StepText+ predicate*;
predicate: EnterPredicate PredicateText* ExitPredicate;

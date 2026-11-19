lexer grammar RuleLanguageLexer;

RULE    : 'rule';
WHEN    : 'when';
THEN    : 'then';

TRUE    : 'true';
FALSE   : 'false';

EQ      : '==';
NE      : '!=';
GE      : '>=';
LE      : '<=';
GT      : '>';
LT      : '<';

ASSIGN  : '=';
DOT     : '.';
LBRACE  : '{';
RBRACE  : '}';

NUMBER
    : '-'? [0-9]+ ('.' [0-9]+)?
    ;

STRING
    : '"' (~["\\\r\n] | '\\' ["\\])* '"'
    ;

IDENTIFIER
    : [a-zA-Z_] [a-zA-Z_0-9]*
    ;

WS
    : [ \t\r\n]+ -> skip
    ;

LINE_COMMENT
    : '//' ~[\r\n]* -> skip
    ;

lexer grammar RuleLanguageLexer;

RULE    : 'rule';
WHEN    : 'when';
THEN    : 'then';
AND     : 'and';
OR      : 'or';
NOT     : 'not';

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
LPAREN  : '(';
RPAREN  : ')';

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

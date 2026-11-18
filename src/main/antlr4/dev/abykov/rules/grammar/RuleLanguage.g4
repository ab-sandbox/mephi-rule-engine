grammar RuleLanguage;

program
    : ruleDeclaration+ EOF
    ;

ruleDeclaration
    : RULE STRING LBRACE
        WHEN condition
        THEN assignment
      RBRACE
    ;

condition
    : property comparisonOperator literal
    ;

assignment
    : property ASSIGN literal
    ;

property
    : IDENTIFIER (DOT IDENTIFIER)*
    ;

comparisonOperator
    : EQ | NE | GE | LE | GT | LT
    ;

literal
    : NUMBER
    | STRING
    | TRUE
    | FALSE
    ;

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
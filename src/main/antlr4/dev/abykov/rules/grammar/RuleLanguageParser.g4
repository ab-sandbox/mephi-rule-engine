parser grammar RuleLanguageParser;

options {
    tokenVocab = RuleLanguageLexer;
}

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
    : orExpression
    ;

orExpression
    : andExpression (OR andExpression)*
    ;

andExpression
    : notExpression (AND notExpression)*
    ;

notExpression
    : NOT notExpression
    | primaryExpression
    ;

primaryExpression
    : LPAREN condition RPAREN
    | comparison
    ;

comparison
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

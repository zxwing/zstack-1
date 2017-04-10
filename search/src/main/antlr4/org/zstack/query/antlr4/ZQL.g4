grammar ZQL;

ID
   : ( 'a' .. 'z' | 'A' .. 'Z' | '_' )+
   ;

INT
   : '0' .. '9'+
   ;


NEWLINE
   : '\r'? '\n' -> skip
   ;

WS
   : ( ' ' | '\t' | '\n' | '\r' )+ -> skip
   ;



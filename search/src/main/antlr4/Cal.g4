grammar Cal;
s : e ;
e : e op=MULT e // MULT is '*'
| e op=ADD e // ADD is '+'
| INT
;

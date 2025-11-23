; Test rapide des instructions INH
LDA #$AA    ; A = 170
TSTA        ; Test A
COMA        ; A = ~170 = 85
CLRA        ; A = 0
LDB #$55    ; B = 85
TSTB        ; Test B
COMB        ; B = ~85 = 170
CLRB        ; B = 0
NOP

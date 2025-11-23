; Test des instructions INH (Implicites)
; ====================================
; Ces instructions opèrent sur A et B de manière implicite

; Test des instructions sur A
LDA #$7F        ; A = 127 (positif)
NOP             ; Pause pour vérifier A

TSTA            ; Tester A (flags seulement, Z=0, N=0)
NOP

COMA            ; Complement A: A = ~127 = 128 (0x80)
NOP             ; A devrait valoir 128, N=1, C=1

NEGA            ; Négation A: A = -128 = -128 (reste -128, overflow)
NOP             ; A devrait valoir 128, V=1, C=1

CLRA            ; Clear A: A = 0
NOP             ; A = 0, Z=1, N=0, V=0, C=0

LDA #$AA        ; A = 170 (10101010)
NOP

ASLA            ; Shift left arithmétique: A = 84 (01010100), C=1
NOP

LSRA            ; Shift right logique: A = 42 (00101010), C=0
NOP

LDA #$81        ; A = 129 (10000001)
NOP

ROLA            ; Rotate left through carry: bit7->C, C->bit0
NOP             ; A = 3 (00000011), C=1

RORA            ; Rotate right through carry
NOP             ; A = 129 (10000001), C=1

; Test des instructions sur B
LDB #$0F        ; B = 15
NOP

TSTB            ; Tester B (Z=0, N=0)
NOP

COMB            ; Complement B: B = ~15 = 240 (0xF0), N=1, C=1
NOP

NEGB            ; Négation B: B = -240 = 16 (complément à 2), C=1
NOP

CLRB            ; Clear B: B = 0, Z=1
NOP

LDB #$55        ; B = 85 (01010101)
NOP

ASLB            ; Shift left: B = 170 (10101010), C=0
NOP

LSRB            ; Shift right: B = 85 (01010101), C=0
NOP

ROLB            ; Rotate left: B = 170 (10101010), C=0
NOP

RORB            ; Rotate right: B = 85 (01010101), C=0
NOP

; Test combiné A et B
LDA #$12        ; A = 18
LDB #$34        ; B = 52
ADDD #$0000     ; D = A:B = $1234
NOP

; Fin du test
NOP

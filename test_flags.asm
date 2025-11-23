; Programme de test complet des flags 6809
; =====================================

; Test du flag Z (Zero)
LDA #$00        ; A=0, devrait donner Z=1, N=0
NOP             ; Pause pour observer

; Test du flag N (Negative)
LDA #$80        ; A=128 (négatif), Z=0, N=1
NOP

LDA #$7F        ; A=127 (positif), Z=0, N=0
NOP

; Test INCA avec overflow positif
LDA #$7F        ; A=127
INCA            ; A=128, devrait donner N=1, V=1 (overflow)
NOP

; Test INCA avec carry
LDA #$FF        ; A=255
INCA            ; A=0, devrait donner Z=1, C=1 (carry)
NOP

; Test DECA avec overflow négatif
LDA #$80        ; A=128
DECA            ; A=127, devrait donner N=0, V=1 (overflow)
NOP

; Test ADDD 16 bits
LDD #$0000      ; D=0, Z=1, N=0
NOP

LDD #$FFFF      ; D=65535 (négatif), N=1, Z=0
NOP

LDD #$7FFF      ; D=32767 (positif max)
ADDD #$0001     ; D=32768, devrait donner N=1, V=1 (overflow)
NOP

LDD #$FFFF      ; D=65535
ADDD #$0001     ; D=0, devrait donner Z=1, C=1 (carry)
NOP

; Fin du test
NOP

; Test rapide des flags principaux
LDA #$00    ; Z=1 (Zero)
INCA        ; Z=0
LDA #$7F    ; Positif
INCA        ; N=1, V=1 (Overflow)
LDA #$FF    ; N=1
INCA        ; Z=1, C=1 (Carry)
NOP

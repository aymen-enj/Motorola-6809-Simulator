; Test du mode d'adressage Direct (DIR)
; ====================================
; Le mode direct utilise DP (Direct Page) pour former des adresses
; Adresse effective = (DP * 256) + offset_8bits

; Initialisation
LDA #$20     ; DP = $20 (page directe = $2000)
STA $00      ; Stocker dans la mémoire pour référence
TFR A,DP     ; Copier A vers DP (DP = $20)

; Test chargement direct
LDA #$AA     ; Valeur de test
STA <$10     ; Stocker en mode direct: adresse = $20 * 256 + $10 = $2010

LDB #$BB     ; Autre valeur de test
STB <$20     ; Stocker à $2020

; Test chargement direct
LDA <$10     ; Charger depuis $2010, A devrait valoir $AA
NOP          ; Pause pour vérifier A

LDB <$20     ; Charger depuis $2020, B devrait valoir $BB
NOP          ; Pause pour vérifier B

; Test avec X et Y
LDX #$1234   ; X = $1234
STX <$30     ; Stocker X à $2030

LDY <$30     ; Charger Y depuis $2030, Y devrait valoir $1234
NOP

; Test avec D
LDD #$5678   ; D = $5678
STD <$40     ; Stocker D à $2040

LDD <$40     ; Charger D depuis $2040, D devrait valoir $5678
NOP

; Test arithmétique avec données directes
LDA <$10     ; A = $AA
ADDA <$20    ; A = $AA + $BB
NOP          ; A devrait valoir $AA + $BB = $165 (overflow!)

; Fin du test
NOP

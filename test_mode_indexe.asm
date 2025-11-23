; Test du mode d'adressage Indexé (IDX)
; ====================================
; Le mode indexé utilise les registres X, Y, U, S comme base
; Adresse effective = valeur_registre + offset

; Initialisation des registres d'index
LDX #$1000     ; X = $1000
LDY #$2000     ; Y = $2000
LDU #$3000     ; U = $3000
LDS #$4000     ; S = $4000 (mais S est modifié par l'exécution)

; Test avec X
LDA #$AA       ; Valeur de test
STA 5,X        ; Mode indexé: adresse = X + 5 = $1000 + 5 = $1005
LDB #$BB       ; Autre valeur
STB 10,X       ; adresse = X + 10 = $1010

; Vérifier les valeurs stockées
LDA 5,X        ; Charger depuis $1005, A devrait valoir $AA
NOP            ; Pause pour vérifier A

LDB 10,X       ; Charger depuis $1010, B devrait valoir $BB
NOP            ; Pause pour vérifier B

; Test avec Y
LDA #$CC       ; Nouvelle valeur
STA 3,Y        ; adresse = Y + 3 = $2000 + 3 = $2003
LDA 3,Y        ; Vérifier, A devrait valoir $CC
NOP

; Test avec offset négatif
LDA #$DD       ; Valeur pour test négatif
STA -2,X       ; adresse = X - 2 = $1000 - 2 = $0FFE
LDA -2,X       ; Vérifier, A devrait valoir $DD
NOP

; Test avec U
STD 8,U        ; Stocker D à U + 8 = $3000 + 8 = $3008
LDD 8,U        ; Recharger, D devrait être inchangé
NOP

; Test arithmétique avec données indexées
LDA 5,X        ; A = valeur à $1005 ($AA)
ADDA 10,X      ; A = $AA + valeur à $1010 ($BB)
NOP            ; A devrait valoir $AA + $BB = $165

; Fin du test
NOP

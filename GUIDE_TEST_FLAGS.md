# Guide de Test des Flags 6809

## Méthode 1 : Test Automatique (Programme intégré)

Le simulateur contient déjà un programme de test des flags. Pour le lancer :

1. **Compiler** : `javac src/sim/Simulateur6809.java`
2. **Lancer** : `java -cp src sim.Simulateur6809`
3. **Exécuter pas à pas** : Cliquez sur "Pas à Pas" et observez les flags

### Programme de test intégré :
```
LDA #$00     ; Z=1 (résultat nul)
INCA         ; Z=0 (plus nul)
LDA #$7F     ; A=127 positif
INCA         ; N=1, V=1 (overflow positif)
LDA #$FF     ; A=255 négatif
INCA         ; Z=1, C=1 (carry)
LDA #$80     ; A=128 négatif
DECA         ; N=0, V=1 (overflow négatif)
[tests 16 bits avec ADDD...]
```

## Méthode 2 : Test Manuel Interactif

### Test du Flag Z (Zero)
1. Ouvrir le simulateur
2. Dans l'éditeur, écrire : `LDA #$00`
3. "Pas à Pas" → Regarder CC : doit finir par `0100` (Z=1)
4. Écrire : `LDA #$01`
5. "Pas à Pas" → CC doit finir par `0000` (Z=0)

### Test du Flag N (Negative)
1. `LDA #$7F` → CC : `0000` (positif, N=0)
2. `LDA #$80` → CC : `1000` (négatif, N=1)

### Test du Flag V (Overflow)
1. `LDA #$7F` (127, positif max)
2. `INCA` → CC : `1010` (N=1, V=1 - dépassement)
3. `LDA #$80` (128, négatif min)
4. `DECA` → CC : `0010` (V=1 - dépassement négatif)

### Test du Flag C (Carry)
1. `LDA #$FF` (255)
2. `INCA` → CC : `0101` (Z=1, C=1 - carry)

### Test du Flag H (Half Carry)
1. `LDA #$0F` (15)
2. `ADDA #$01` → CC : `0010 0000` (H=1 si implémenté)

## Méthode 3 : Test avec Breakpoints

1. Dans le champ "Breakpoints", entrer : `0005,000A,000F`
2. Lancer en "RUN"
3. Le programme s'arrête aux adresses spécifiées
4. Vérifier les flags à chaque breakpoint

## Méthode 4 : Édition Directe des Registres

1. Cliquer dans le champ "CC (Flags)"
2. Entrer une valeur binaire comme `0100` (Z=1)
3. Voir si elle s'affiche correctement
4. Modifier manuellement A et voir CC se mettre à jour

## Valeurs CC Attendues

| Opération | Résultat | CC Attendu | Signification |
|-----------|----------|------------|---------------|
| `LDA #$00` | A=0 | `0100` | Z=1 (Zero) |
| `LDA #$80` | A=128 | `1000` | N=1 (Negative) |
| `LDA #$7F; INCA` | A=128 | `1010` | N=1, V=1 (Overflow) |
| `LDA #$FF; INCA` | A=0 | `0101` | Z=1, C=1 (Carry) |
| `LDA #$80; DECA` | A=127 | `0010` | V=1 (Overflow négatif) |

## Dépannage

### Les flags ne se mettent pas à jour ?
- Vérifier que l'instruction est supportée
- Regarder dans la console pour les erreurs

### Valeurs inattendues ?
- Vérifier la taille des opérandes (8 vs 16 bits)
- Certains flags ne s'appliquent qu'aux opérations arithmétiques

### Comment voir les flags en détail ?
- CC s'affiche en binaire (8 bits)
- Bit 0=C, 1=V, 2=Z, 3=N, 4=I, 5=H, 6=F, 7=E

## Tests Supplémentaires Recommandés

Créer des programmes testant :
- Additions avec carry
- Soustractions (quand implémentées)
- Opérations logiques (AND, OR, etc.)
- Instructions de comparaison (CMP)

## Validation Finale

Pour confirmer que tout fonctionne :
1. Exécuter tous les tests ci-dessus
2. Vérifier que les flags correspondent à la documentation 6809
3. Tester avec des programmes réels du 6809
4. Comparer avec d'autres simulateurs 6809

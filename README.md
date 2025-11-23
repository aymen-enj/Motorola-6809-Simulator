# Simulateur Motorola 6809 - Édition Registres Complets

Un simulateur complet du processeur Motorola 6809 avec interface graphique Java Swing. Ce projet éducatif permet d'apprendre l'architecture des microprocesseurs et la programmation en assembleur 6809.

## 📋 Table des matières

- [Fonctionnalités](#-fonctionnalités)
- [Installation](#-installation)
- [Utilisation](#-utilisation)
- [Architecture du code](#-architecture-du-code)
- [Instructions supportées](#-instructions-supportées)
- [Registres du 6809](#-registres-du-6809)
- [Interface utilisateur](#-interface-utilisateur)
- [Système I/O](#-système-io)
- [Exemples](#-exemples)
- [Limitations](#-limitations)
- [Améliorations futures](#-améliorations-futures)

## 🚀 Fonctionnalités

- **Simulation complète** : Tous les registres du 6809 (A, B, D, DP, X, Y, U, S, PC, CC)
- **Assembleur intégré** : Conversion automatique assembleur → code machine
- **Interface graphique** : Édition en temps réel des registres et mémoire
- **Débogueur intégré** : Exécution pas à pas et breakpoints
- **Terminal virtuel** : Sortie I/O sur l'adresse `$D000`
- **Interruption NMI** : Simulation des interruptions matérielles
- **Édition mémoire** : Modification directe des valeurs mémoire

## 🛠️ Installation

### Prérequis
- Java JDK 8 ou supérieur
- Environnement Eclipse ou tout IDE Java

### Compilation et exécution

1. **Depuis Eclipse** :
   - Ouvrir le projet dans Eclipse
   - Compiler et exécuter `Simulateur6809.java`

2. **Depuis la ligne de commande** :
   ```bash
   cd src
   javac sim/Simulateur6809.java
   java sim.Simulateur6809
   ```

## 🎮 Utilisation

### Lancement
L'application s'ouvre avec un exemple de code assembleur qui affiche "HI" sur le terminal.

### Interface principale

#### Panneau gauche : Registres
- **PC** : Program Counter (pointeur d'instruction)
- **A, B** : Accumulateurs 8 bits
- **D** : Accumulateur virtuel 16 bits (A:B)
- **DP** : Direct Page register
- **X, Y** : Registres d'index 16 bits
- **U, S** : Piles utilisateur/système
- **CC** : Condition Code (drapeaux binaires)

#### Centre : Éditeur et Terminal
- **Éditeur assembleur** : Écrivez votre code assembleur
- **Terminal** : Affiche la sortie I/O ($D000)

#### Droite : Mémoire
- Table des 100 premiers octets de mémoire
- Édition directe des valeurs (colonne "Value (Hex)")

#### Bas : Contrôles
- **Pas à Pas** : Exécute une instruction à la fois
- **RUN/STOP** : Exécution continue avec breakpoints
- **NMI** : Déclenche une interruption NMI
- **RESET** : Remet le CPU à zéro

### Édition des registres
Cliquez sur n'importe quel champ de registre pour le modifier manuellement (format hexadécimal).

### Breakpoints
Entrez les adresses séparées par des virgules dans le champ "Breakpoints" (ex: `0010,0020,0030`).

## 🏗️ Architecture du code

Le projet suit une architecture modulaire en 4 composants :

### 1. CPU6809_V6 (Modèle)
- Simulation du hardware 6809
- Gestion des registres et mémoire 64KB
- Système I/O extensible

### 2. MiniAssembler_V6 (Assembleur)
- Conversion assembleur → code machine
- Support des modes d'adressage : IMM, EXT, REL, INH
- Gestion des préfixes d'instructions

### 3. InstructionDecoder_V6 (Décodeur)
- Exécution des instructions machine
- Gestion des flags et conditions
- Support des interruptions

### 4. Simulateur6809 (Interface)
- GUI Swing complète
- Gestion des événements utilisateur
- Mise à jour temps réel de l'état

## 📚 Instructions supportées

### Modes d'Adressage Disponibles
- ✅ **IMM** : Immédiat (`#$valeur`) - valeur constante
- ✅ **DIR** : Direct (`<$addr>` ou `addr≤255`) - adresse = (DP×256) + offset
- ✅ **EXT** : Étendu (`$adresse`) - adresse 16 bits complète
- ✅ **INH** : Implicite (pas d'opérande) - instruction autonome
- ✅ **REL** : Relatif (pour sauts) - offset par rapport à PC
- ✅ **IDX** : Indexé (`offset,reg`) - adresse = reg + offset (±127)
- ❌ **IND** : Indirect (pointeurs)

### Chargement (Load)
- `LDA #imm` / `LDA ext` : Charge A
- `LDB #imm` / `LDB ext` : Charge B
- `LDX #imm` / `LDX ext` : Charge X
- `LDY #imm` / `LDY ext` : Charge Y
- `LDD #imm` / `LDD ext` : Charge D (A:B)
- `LDS #imm` : Charge S
- `LDU #imm` : Charge U

### Stockage (Store)
- `STA ext` : Stocke A
- `STB ext` : Stocke B
- `STD ext` : Stocke D
- `STX ext` : Stocke X

### Arithmétique
- `ADDD #imm` : Addition 16 bits
- `INCA` : Incrémente A
- `DECA` : Décrémente A

### Contrôle de flux
- `JMP ext` : Saut absolu
- `BRA rel` : Saut relatif toujours
- `BEQ rel` : Saut si Z=1 (égal)
- `BNE rel` : Saut si Z=0 (différent)

### Arithmétiques/Logiques INH
- `CLRA/CLRB` : Clear accumulateur (A/B = 0)
- `COMA/COMB` : Complement accumulateur (~A/~B)
- `NEGA/NEGB` : Négation accumulateur (-A/-B)
- `TSTA/TSTB` : Test accumulateur (flags seulement)

### Décalages/Rotations INH
- `ASLA/ASLB` : Shift arithmétique gauche
- `LSRA/LSRB` : Shift logique droite
- `ROLA/ROLB` : Rotation gauche through carry
- `RORA/RORB` : Rotation droite through carry

### Divers
- `TFR reg,reg` : Transfert registre
- `NOP` : Pas d'opération

## 🔢 Registres du 6809

| Registre | Taille | Description |
|----------|--------|-------------|
| A | 8 bits | Accumulateur principal |
| B | 8 bits | Accumulateur secondaire |
| D | 16 bits | Accumulateur virtuel (A:B) |
| DP | 8 bits | Page directe pour adressage |
| X | 16 bits | Registre d'index |
| Y | 16 bits | Registre d'index |
| U | 16 bits | Pile utilisateur |
| S | 16 bits | Pile système |
| PC | 16 bits | Compteur de programme |
| CC | 8 bits | Code condition (flags) |

### Flags du registre CC
- Bit 0 : C (Carry) - Report des opérations arithmétiques
- Bit 1 : V (Overflow) - Débordement arithmétique
- Bit 2 : Z (Zero) - Résultat nul
- Bit 3 : N (Negative) - Bit de poids fort à 1
- Bit 4 : I (Interrupt mask) - Masquage des interruptions
- Bit 5 : H (Half carry) - Report du 4ème bit (additions)
- Bit 6 : F (Fast interrupt mask) - Masquage des interruptions rapides
- Bit 7 : E (Entire state on stack) - État complet sauvegardé

✅ **Tous les flags sont maintenant implémentés et fonctionnels !**

## 🖥️ Système I/O

Le simulateur utilise un système I/O extensible basé sur des monitors :

```java
cpu.ioMonitor = (addr, val) -> {
    if (addr == 0xD000) {
        // Afficher le caractère sur le terminal
        terminalOutput.append(String.valueOf((char)val));
    }
};
```

- **Adresse $D000** : Terminal de sortie (caractères ASCII)
- **Extensible** : Ajoutez facilement de nouveaux périphériques

## 🧪 Test des Flags

### Lancement rapide des tests
```bash
# Windows
run_simulator.bat

# Linux/Mac
javac src/sim/Simulateur6809.java && java -cp src sim.Simulateur6809
```

### Fichiers de test disponibles
- `test_flags.asm` : Test complet de tous les flags
- `test_flags_simple.asm` : Test rapide des flags principaux
- `GUIDE_TEST_FLAGS.md` : Guide détaillé pour tester les flags

### Test manuel rapide
1. **Flag Z** : `LDA #$00` → CC=`0100` (Z=1)
2. **Flag N** : `LDA #$80` → CC=`1000` (N=1)
3. **Flag V** : `LDA #$7F; INCA` → CC=`1010` (V=1)
4. **Flag C** : `LDA #$FF; INCA` → CC=`0101` (C=1)

## 💡 Exemples

### Hello World
```
LDX #$1000     ; Initialiser X
LDY #$2000     ; Initialiser Y
LDA #$48       ; 'H'
STA $D000      ; Afficher
LDA #$49       ; 'I'
STA $D000      ; Afficher
NOP
```

### Boucle simple
```
LDA #$00       ; Compteur = 0
LOOP INCA      ; Incrémenter
STA $D001      ; Afficher compteur
BRA LOOP       ; Boucle infinie
```

### Utilisation des registres D
```
LDD #$1234     ; Charger 1234 dans D (A=12, B=34)
ADDD #$1111    ; Additionner 1111 (Z=0, N=1, C=0, V=0)
STD $D002      ; Stocker le résultat
```

### Mode d'adressage Direct
```
LDA #$10       ; DP = $10 (page directe)
TFR A,DP       ; Configurer DP
LDA #$AA       ; Valeur à stocker
STA <$20       ; Mode direct: adresse = $10*256 + $20 = $1020
LDA <$20       ; Charger depuis $1020, A=$AA
```

### Mode d'adressage Indexé
```
LDX #$1000     ; Initialiser X
LDA #$42       ; Valeur à stocker
STA 5,X        ; IDX: adresse = X + 5 = $1005
LDA 5,X        ; Charger depuis $1005, A=$42
```

### Instructions INH sur accumulateurs
```
LDA #$7F       ; A = 127
TSTA           ; Tester A (Z=0, N=0)
COMA           ; A = ~127 = 128 (N=1, C=1)
NEGA           ; A = -128 = -128 (V=1, C=1)
CLRA           ; A = 0 (Z=1)
ASLA           ; A = 0 << 1 = 0 (C=0)
```

### Comparaison des modes d'adressage
```
LDA #$42       ; IMM: charger la valeur 42
LDA <$10       ; DIR: charger depuis (DP*256)+$10
LDA 5,X        ; IDX: charger depuis X + 5
LDA $1234      ; EXT: charger depuis l'adresse $1234
CLRA           ; INH: clear A (pas d'opérande)
```

### Test des flags
```
LDA #$7F       ; A=127 (N=0, Z=0)
INCA           ; A=128 (N=1, V=1 - dépassement positif)
LDA #$FF       ; A=255 (N=1, Z=0)
INCA           ; A=0 (Z=1, C=1 - carry, N=0)
LDA #$80       ; A=128 (N=1, Z=0)
DECA           ; A=127 (N=0, V=1 - dépassement négatif)
```

## ⚠️ Limitations

- **Modes d'adressage** : Indirect non implémenté, Indexé partiel (offset 8 bits seulement)
- **Instructions disponibles** : ~35 instructions implémentées (arithmétique complète sur A/B)
- **Instructions limitées** : ~15 instructions sur ~200 disponibles
- **Adressage restreint** : Principalement immédiat et étendu
- **Pas de pile** : PUSH/PULL non implémentés
- **Pas de sous-routines** : JSR/RTS manquants
- **I/O minimal** : Un seul port ($D000)

## 🚀 Améliorations futures

### Priorité haute
- [ ] Implémenter tous les flags (V, C, H, I, F)
- [ ] Ajouter PUSH/PULL (pile)
- [ ] Modes d'adressage indexés
- [ ] Instructions JSR/RTS (sous-routines)

### Priorité moyenne
- [ ] Instructions arithmétiques (SUB, MUL, DIV)
- [ ] Instructions logiques (AND, OR, EOR)
- [ ] Instructions de bits (ASL, LSR, ROL, ROR)
- [ ] Adressage direct (DP)

### Priorité basse
- [ ] Interface série/parallele
- [ ] Timers et interruptions
- [ ] Système de fichiers virtuel
- [ ] Sauvegarde/chargement d'état
- [ ] Mode batch (exécution sans GUI)

## 🤝 Contribution

Ce projet est éducatif. Pour contribuer :

1. Fork le projet
2. Créez une branche pour votre fonctionnalité
3. Ajoutez des tests pour vos nouvelles instructions
4. Documentez vos ajouts
5. Soumettez une pull request

### Ajout d'instructions
1. Ajoutez l'opcode dans `MiniAssembler_V6.OPCODES`
2. Implémentez l'exécution dans `InstructionDecoder_V6.executeNext()`
3. Mettez à jour le README

## 📄 Licence

Ce projet est open source et destiné à l'éducation. Utilisez-le librement pour apprendre l'architecture des microprocesseurs.

---

**Note** : Ce simulateur est une implémentation pédagogique du Motorola 6809. Il n'est pas destiné à une utilisation en production et peut contenir des inexactitudes par rapport au comportement réel du processeur.

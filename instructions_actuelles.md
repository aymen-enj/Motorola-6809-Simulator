# Instructions 6809 Implémentées - Analyse Complète

## 📊 **INSTRUCTIONS ACTUELLEMENT IMPLÉMENTÉES**

### Modes d'Adressage Disponibles
- ✅ **IMM** : Immédiat (`#$valeur`)
- ✅ **EXT** : Étendu (`$adresse`)
- ✅ **INH** : Implicite (pas d'opérande)
- ✅ **REL** : Relatif (pour les sauts)
- ❌ **DIR** : Direct (adresse 8 bits via DP)
- ❌ **IDX** : Indexé (via X, Y, U, S)
- ❌ **IND** : Indirect (pointeurs)

---

## 🔍 **INSTRUCTIONS PAR CATÉGORIE**

### **1. CHARGEMENT (LOAD)**
| Instruction | Modes Implémentés | Description |
|-------------|------------------|-------------|
| `LDA` | IMM, EXT | Charge A depuis mémoire |
| `LDB` | IMM, EXT | Charge B depuis mémoire |
| `LDX` | IMM, EXT | Charge X depuis mémoire |
| `LDY` | IMM, EXT | Charge Y depuis mémoire |
| `LDD` | IMM, EXT | Charge D (A:B) depuis mémoire |
| `LDS` | IMM | Charge S depuis mémoire |
| `LDU` | IMM | Charge U depuis mémoire |

### **2. STOCKAGE (STORE)**
| Instruction | Modes Implémentés | Description |
|-------------|------------------|-------------|
| `STA` | EXT | Stocke A en mémoire |
| `STB` | EXT | Stocke B en mémoire |
| `STD` | EXT | Stocke D (A:B) en mémoire |
| `STX` | EXT | Stocke X en mémoire |

### **3. ARITHMÉTIQUE**
| Instruction | Modes Implémentés | Description |
|-------------|------------------|-------------|
| `ADDD` | IMM | Addition 16 bits à D |
| `INCA` | INH | Incrémente A |
| `DECA` | INH | Décrémente A |

### **4. CONTRÔLE DE FLUX**
| Instruction | Modes Implémentés | Description |
|-------------|------------------|-------------|
| `JMP` | EXT | Saut absolu |
| `BRA` | REL | Saut relatif toujours |
| `BEQ` | REL | Saut si Z=1 |
| `BNE` | REL | Saut si Z=0 |

### **5. ARITHMÉTIQUES/LOGIQUES INH**
| Instruction | Modes Implémentés | Description |
|-------------|------------------|-------------|
| `CLRA` | INH | Clear A (A = 0) |
| `CLRB` | INH | Clear B (B = 0) |
| `COMA` | INH | Complement A (~A) |
| `COMB` | INH | Complement B (~B) |
| `NEGA` | INH | Negate A (-A) |
| `NEGB` | INH | Negate B (-B) |
| `TSTA` | INH | Test A (flags seulement) |
| `TSTB` | INH | Test B (flags seulement) |

### **6. DÉCALAGES/ROTATIONS INH**
| Instruction | Modes Implémentés | Description |
|-------------|------------------|-------------|
| `ASLA` | INH | Arithmetic Shift Left A |
| `ASLB` | INH | Arithmetic Shift Left B |
| `LSRA` | INH | Logical Shift Right A |
| `LSRB` | INH | Logical Shift Right B |
| `ROLA` | INH | Rotate Left A through Carry |
| `ROLB` | INH | Rotate Left B through Carry |
| `RORA` | INH | Rotate Right A through Carry |
| `RORB` | INH | Rotate Right B through Carry |

### **7. DIVERS**
| Instruction | Modes Implémentés | Description |
|-------------|------------------|-------------|
| `TFR` | INH (partiel) | Transfert entre registres |
| `NOP` | INH | Pas d'opération |

---

## 🎯 **ANALYSE DES MODES MANQUANTS**

### **DIRECT (DIR)** - ✅ IMPLÉMENTÉ
Adresse 8 bits + DP (registre page directe)
```asm
LDA <$10    ; Adresse = (DP * 256) + $10
LDA $10     ; Automatique si adresse <= 255
```

**Instructions implémentées :**
- ✅ Toutes les LDA, LDB, LDX, LDY, LDD, LDS, LDU
- ✅ Toutes les STA, STB, STX, STD
- ✅ Calcul d'adresse : `getDirectAddr(offset) = (DP << 8) | offset`

### **INDEXÉ (IDX)** - ✅ PARTIELLEMENT IMPLÉMENTÉ
Adressage via registres d'index (X, Y, U, S)
```asm
LDA 5,X     ; ✅ Adresse = X + 5 (offset 8 bits)
LDA -2,Y    ; ✅ Adresse = Y - 2 (offset négatif)
STA 10,U    ; ✅ Adresse = U + 10
LDD 3,S     ; ✅ Adresse = S + 3
```

**Modes implémentés :**
- ✅ Offset constant 8 bits signé (±127)
- ✅ Registres X, Y, U, S
- ✅ Toutes les instructions Load/Store
- ❌ Auto-incrément/décrément (`X+`, `-X`)
- ❌ Offset 5 bits, 16 bits
- ❌ Mode indirect

### **INDIRECT (IND)** - À IMPLÉMENTER
Indirect via pointeurs
```asm
LDA [10,X]  ; Adresse pointée par (X + 10)
```
**Instructions à ajouter :**
- Toutes les instructions supportant l'indexé

---

## 📋 **PLAN D'IMPLÉMENTATION**

### **Phase 1 : Mode Direct (DIR)**
1. Ajouter reconnaissance dans assembleur
2. Implémenter calcul d'adresse : `adresse = (DP << 8) + offset8`
3. Ajouter opcodes pour toutes les instructions

### **Phase 2 : Mode Indexé (IDX)**
1. Parser les modes indexés (offset, auto-inc/déc)
2. Implémenter calcul d'adresse pour chaque mode
3. Ajouter post-byte pour spécifier le mode

### **Phase 3 : Mode Indirect (IND)**
1. Étendre les modes indexés avec indirect
2. Ajouter calcul d'adresse à deux niveaux

### **Phase 4 : Instructions Supplémentaires**
1. Ajouter SUB, MUL, DIV
2. Ajouter AND, OR, EOR
3. Ajouter CMP, BIT
4. Ajouter PUSH/PULL

---

## 🔧 **OPCODES MANQUANTS À AJOUTER**

### Pour Direct (DIR) :
```java
OPCODES.put("LDA_DIR", 0x96);
OPCODES.put("LDB_DIR", 0xD6);
OPCODES.put("STA_DIR", 0x97);
OPCODES.put("STB_DIR", 0xD7);
// ... et ainsi de suite
```

### Pour Indexé (IDX) :
```java
OPCODES.put("LDA_IDX", 0xA6);
OPCODES.put("LDB_IDX", 0xE6);
OPCODES.put("STA_IDX", 0xA7);
OPCODES.put("STB_IDX", 0xE7);
// ... et ainsi de suite
```

---

## 📈 **PROCHAINES ÉTAPES**

1. **Commencer par DIR** : Plus simple à implémenter
2. **Puis IDX** : Modes de base (offset constant)
3. **Puis IND** : Extension des modes indexés
4. **Ajouter instructions** : Logiques, comparaisons, etc.

**Priorité** : DIR → IDX → IND → Nouvelles Instructions

package sim;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// ==========================================
// 1. LE MODÈLE (HARDWARE COMPLET & I/O)
// ==========================================
class CPU6809_V6 {
    // Registres 8 bits
    public int A = 0, B = 0, DP = 0, CC = 0;
    // Registres 16 bits
    public int X = 0, Y = 0, U = 0, S = 0, PC = 0;

    // Constantes pour les flags du registre CC
    public static final int FLAG_C = 0x01; // Carry
    public static final int FLAG_V = 0x02; // Overflow
    public static final int FLAG_Z = 0x04; // Zero
    public static final int FLAG_N = 0x08; // Negative
    public static final int FLAG_I = 0x10; // Interrupt mask
    public static final int FLAG_H = 0x20; // Half carry
    public static final int FLAG_F = 0x40; // Fast interrupt mask
    public static final int FLAG_E = 0x80; // Entire state on stack
    
    // Mémoire 64KB
    public int[] memory = new int[65536];

    // Interface pour les Périphériques (I/O)
    public interface IOMonitor {
        void onWrite(int address, int value);
    }
    public IOMonitor ioMonitor;

    public CPU6809_V6() {
        reset();
    }

    public void reset() {
        Arrays.fill(memory, 0);
        A = B = DP = CC = 0;
        X = Y = U = 0;
        S = 0x0100; // Pile standard
        PC = 0x0000;
    }

    // Gestion 16 bits (D est virtuel : concaténation A:B)
    public int getD() { return ((A & 0xFF) << 8) | (B & 0xFF); }
    public void setD(int value) { 
        A = (value >> 8) & 0xFF; 
        B = value & 0xFF; 
    }

    // Méthode générale pour les chargements (N, Z seulement)
    public void updateFlags(int value, boolean is16Bit) {
        updateFlagsNZ(value, is16Bit);
    }

    // Mise à jour des flags N et Z (utilisé pour les chargements)
    public void updateFlagsNZ(int value, boolean is16Bit) {
        // Reset N, Z
        CC &= ~(FLAG_N | FLAG_Z);

        // Test Zero
        int mask = is16Bit ? 0xFFFF : 0xFF;
        if ((value & mask) == 0) {
            CC |= FLAG_Z;
        }

        // Test Negative (bit de poids fort)
        int msb = is16Bit ? 0x8000 : 0x80;
        if ((value & msb) != 0) {
            CC |= FLAG_N;
        }
    }

    // Mise à jour des flags pour l'addition 8 bits (N, Z, V, C, H)
    public void updateFlagsAdd8(int operand1, int operand2, int result) {
        // Reset N, Z, V, C, H
        CC &= ~(FLAG_N | FLAG_Z | FLAG_V | FLAG_C | FLAG_H);

        // Calcul du résultat sur 8 bits
        result &= 0xFF;

        // Test Zero
        if (result == 0) {
            CC |= FLAG_Z;
        }

        // Test Negative (bit 7)
        if ((result & 0x80) != 0) {
            CC |= FLAG_N;
        }

        // Test Carry (bit 8)
        int sum = (operand1 & 0xFF) + (operand2 & 0xFF);
        if (sum > 0xFF) {
            CC |= FLAG_C;
        }

        // Test Half Carry (bit 4)
        if (((operand1 & 0x0F) + (operand2 & 0x0F)) > 0x0F) {
            CC |= FLAG_H;
        }

        // Test Overflow (V)
        // V = 1 si les deux opérandes ont le même bit 7 et que le résultat a un bit 7 différent
        boolean op1_neg = (operand1 & 0x80) != 0;
        boolean op2_neg = (operand2 & 0x80) != 0;
        boolean res_neg = (result & 0x80) != 0;
        if (op1_neg == op2_neg && op1_neg != res_neg) {
            CC |= FLAG_V;
        }
    }

    // Mise à jour des flags pour l'addition 16 bits (N, Z, V, C)
    public void updateFlagsAdd16(int operand1, int operand2, int result) {
        // Reset N, Z, V, C
        CC &= ~(FLAG_N | FLAG_Z | FLAG_V | FLAG_C);

        // Calcul du résultat sur 16 bits
        result &= 0xFFFF;

        // Test Zero
        if (result == 0) {
            CC |= FLAG_Z;
        }

        // Test Negative (bit 15)
        if ((result & 0x8000) != 0) {
            CC |= FLAG_N;
        }

        // Test Carry (bit 16)
        long sum = (operand1 & 0xFFFFL) + (operand2 & 0xFFFFL);
        if (sum > 0xFFFFL) {
            CC |= FLAG_C;
        }

        // Test Overflow (V)
        // V = 1 si les deux opérandes ont le même bit 15 et que le résultat a un bit 15 différent
        boolean op1_neg = (operand1 & 0x8000) != 0;
        boolean op2_neg = (operand2 & 0x8000) != 0;
        boolean res_neg = (result & 0x8000) != 0;
        if (op1_neg == op2_neg && op1_neg != res_neg) {
            CC |= FLAG_V;
        }
    }

    // Mise à jour des flags pour la décrémentation 8 bits (N, Z, V)
    public void updateFlagsDec8(int original, int result) {
        // Reset N, Z, V
        CC &= ~(FLAG_N | FLAG_Z | FLAG_V);

        result &= 0xFF;

        // Test Zero
        if (result == 0) {
            CC |= FLAG_Z;
        }

        // Test Negative (bit 7)
        if ((result & 0x80) != 0) {
            CC |= FLAG_N;
        }

        // Test Overflow : V = 1 si décrémentation de 0x80 (cas spécial 6809)
        if (original == 0x80) {
            CC |= FLAG_V;
        }
    }

    // Mise à jour des flags pour l'incrémentation 8 bits (N, Z, V)
    public void updateFlagsInc8(int original, int result) {
        // Reset N, Z, V
        CC &= ~(FLAG_N | FLAG_Z | FLAG_V);

        result &= 0xFF;

        // Test Zero
        if (result == 0) {
            CC |= FLAG_Z;
        }

        // Test Negative (bit 7)
        if ((result & 0x80) != 0) {
            CC |= FLAG_N;
        }

        // Test Overflow : V = 1 si incrémentation de 0x7F (cas spécial 6809)
        if (original == 0x7F) {
            CC |= FLAG_V;
        }
    }

    // Interruption Matérielle (Simulation C.1)
    public void triggerNMI() {
        // Vecteur NMI standard 6809 : $FFFC
        int vector = (memory[0xFFFC] << 8) | memory[0xFFFD];
        if (vector == 0) vector = 0x1000; // Fallback si non défini
        PC = vector;
    }
}

// ==========================================
// 2. L'ASSEMBLEUR (COMPLET)
// ==========================================
class MiniAssembler_V6 {
    private static final Map<String, Integer> OPCODES = new HashMap<>();

    static {
        // Chargements
        OPCODES.put("LDA_IMM", 0x86); OPCODES.put("LDA_DIR", 0x96); OPCODES.put("LDA_IDX", 0xA6); OPCODES.put("LDA_EXT", 0xB6);
        OPCODES.put("LDB_IMM", 0xC6); OPCODES.put("LDB_DIR", 0xD6); OPCODES.put("LDB_IDX", 0xE6); OPCODES.put("LDB_EXT", 0xF6);
        OPCODES.put("LDX_IMM", 0x8E); OPCODES.put("LDX_DIR", 0x9E); OPCODES.put("LDX_IDX", 0xAE); OPCODES.put("LDX_EXT", 0xBE);
        OPCODES.put("LDY_IMM", 0x108E); OPCODES.put("LDY_DIR", 0x109E); OPCODES.put("LDY_IDX", 0x10AE); OPCODES.put("LDY_EXT", 0x10BE); // Y utilise un préfixe 10
        OPCODES.put("LDD_IMM", 0xCC); OPCODES.put("LDD_DIR", 0xDC); OPCODES.put("LDD_IDX", 0xEC); OPCODES.put("LDD_EXT", 0xFC);
        OPCODES.put("LDS_IMM", 0x10CE); OPCODES.put("LDS_DIR", 0x10DE); OPCODES.put("LDS_IDX", 0x10EE);
        OPCODES.put("LDU_IMM", 0xCE); OPCODES.put("LDU_DIR", 0xDE); OPCODES.put("LDU_IDX", 0xEE);

        // Stockage
        OPCODES.put("STA_IDX", 0xA7); OPCODES.put("STA_DIR", 0x97); OPCODES.put("STA_EXT", 0xB7);
        OPCODES.put("STB_IDX", 0xE7); OPCODES.put("STB_DIR", 0xD7); OPCODES.put("STB_EXT", 0xF7);
        OPCODES.put("STD_IDX", 0xED); OPCODES.put("STD_DIR", 0xDD); OPCODES.put("STD_EXT", 0xFD);
        OPCODES.put("STX_IDX", 0xAF); OPCODES.put("STX_DIR", 0x9F); OPCODES.put("STX_EXT", 0xBF);
        
        // Arithmétique
        OPCODES.put("ADDD_IMM", 0xC3);
        OPCODES.put("INCA_INH", 0x4C);
        OPCODES.put("DECA_INH", 0x4A);

        // Instructions arithmétiques/logiques INH sur A
        OPCODES.put("NEGA_INH", 0x40);
        OPCODES.put("COMA_INH", 0x43);
        OPCODES.put("LSRA_INH", 0x44);
        OPCODES.put("RORA_INH", 0x46);
        OPCODES.put("ASLA_INH", 0x48);
        OPCODES.put("ROLA_INH", 0x49);
        OPCODES.put("TSTA_INH", 0x4D);
        OPCODES.put("CLRA_INH", 0x4F);

        // Instructions arithmétiques/logiques INH sur B
        OPCODES.put("NEGB_INH", 0x50);
        OPCODES.put("COMB_INH", 0x53);
        OPCODES.put("LSRB_INH", 0x54);
        OPCODES.put("RORB_INH", 0x56);
        OPCODES.put("ASLB_INH", 0x58);
        OPCODES.put("ROLB_INH", 0x59);
        OPCODES.put("TSTB_INH", 0x5D);
        OPCODES.put("CLRB_INH", 0x5F);

        // Contrôle de flux
        OPCODES.put("JMP_EXT", 0x7E); 
        OPCODES.put("BRA_REL", 0x20); 
        OPCODES.put("BEQ_REL", 0x27); 
        OPCODES.put("BNE_REL", 0x26); 
        
        // Divers
        OPCODES.put("TFR_INH", 0x1F);
        OPCODES.put("NOP_INH", 0x12);
    }

    public boolean assemble(String sourceCode, CPU6809_V6 cpu) throws Exception {
        Arrays.fill(cpu.memory, 0, 1000, 0); 
        
        String[] lines = sourceCode.split("\n");
        int addr = 0x0000;

        for (String line : lines) {
            line = line.trim().toUpperCase();
            if (line.isEmpty() || line.startsWith(";")) continue;
            if (line.contains(";")) line = line.substring(0, line.indexOf(";")).trim();

            String[] parts = line.split("\\s+", 2);
            String mnemonic = parts[0];
            String operand = (parts.length > 1) ? parts[1] : "";

            String mode = "EXT";
            int val = 0;

            if (OPCODES.containsKey(mnemonic + "_INH")) {
                mode = "INH";
            } else if (operand.startsWith("#")) {
                mode = "IMM";
                val = parseHex(operand.substring(1));
            } else if (OPCODES.containsKey(mnemonic + "_REL")) {
                mode = "REL";
                val = parseHex(operand);
            } else if (operand.startsWith("<") && OPCODES.containsKey(mnemonic + "_DIR")) {
                // Mode Direct : <adresse (8 bits)
                mode = "DIR";
                val = parseHex(operand.substring(1));
                if (val > 0xFF) throw new Exception("Adresse directe > 255 : " + operand);
            } else if (operand.contains(",") && operand.matches(".*,[XYUS]$")) {
                // Mode Indexé : offset,registre (ex: 10,X ou -5,Y)
                mode = "IDX";
                // Pour l'instant, on stocke l'opérande complet pour parsing dans le décodeur
                val = 0; // Sera parsé plus tard
            } else if (!operand.isEmpty() && OPCODES.containsKey(mnemonic + "_DIR")) {
                // Vérifier si c'est une adresse 8 bits (mode direct automatique)
                int parsedAddr = parseHex(operand);
                if (parsedAddr <= 0xFF && !OPCODES.containsKey(mnemonic + "_EXT")) {
                    mode = "DIR";
                    val = parsedAddr;
                } else {
                    mode = "EXT";
                    val = parsedAddr;
                }
            } else if (!operand.isEmpty()) {
                mode = "EXT";
                val = parseHex(operand);
            }

            String key = mnemonic + "_" + mode;
            if (!OPCODES.containsKey(key)) continue;

            int opcode = OPCODES.get(key);
            
            // Gestion Prefix (ex: LDY = 10 8E)
            if (opcode > 0xFF) {
                cpu.memory[addr++] = (opcode >> 8) & 0xFF;
                cpu.memory[addr++] = opcode & 0xFF;
            } else {
                cpu.memory[addr++] = opcode;
            }

            // Gestion Opérandes
            if (mode.equals("IDX")) {
                // Mode indexé : post-byte + offset éventuel
                // Pour l'instant : offset 8 bits constant avec registre X,Y,U,S
                if (operand.contains(",X")) {
                    cpu.memory[addr++] = 0x84; // Post-byte pour offset 8 bits + X
                } else if (operand.contains(",Y")) {
                    cpu.memory[addr++] = 0xA4; // Post-byte pour offset 8 bits + Y
                } else if (operand.contains(",U")) {
                    cpu.memory[addr++] = 0xC4; // Post-byte pour offset 8 bits + U
                } else if (operand.contains(",S")) {
                    cpu.memory[addr++] = 0xE4; // Post-byte pour offset 8 bits + S
                }
                // Extraire l'offset de l'opérande (avant la virgule)
                String offsetStr = operand.substring(0, operand.indexOf(','));
                int offset = parseHex(offsetStr);
                cpu.memory[addr++] = offset & 0xFF;
            } else if (mode.equals("DIR")) {
                // Mode direct : 1 octet (adresse 8 bits)
                cpu.memory[addr++] = val & 0xFF;
            } else if (mode.equals("EXT") || (mode.equals("IMM") && is16Bit(mnemonic))) {
                // Mode étendu : 2 octets (adresse 16 bits)
                cpu.memory[addr++] = (val >> 8) & 0xFF;
                cpu.memory[addr++] = val & 0xFF;
            } else if (mode.equals("IMM")) {
                // Mode immédiat 8 bits
                cpu.memory[addr++] = val & 0xFF;
            } else if (mode.equals("REL")) {
                // Mode relatif : offset 8 bits
                int offset = val - (addr + 1);
                cpu.memory[addr++] = offset & 0xFF;
            } else if (mnemonic.equals("TFR")) {
                 cpu.memory[addr++] = 0x8B;
            }
        }
        return true;
    }

    private boolean is16Bit(String mnemo) {
        return mnemo.endsWith("X") || mnemo.endsWith("Y") || mnemo.endsWith("U") || 
               mnemo.endsWith("S") || mnemo.endsWith("D") || mnemo.equals("ADDD");
    }

    private int parseHex(String s) {
        s = s.replace("$", "").trim();
        try { return Integer.parseInt(s, 16); } catch (Exception e) { return 0; }
    }
}

// ==========================================
// 3. DÉCODEUR
// ==========================================
class InstructionDecoder_V6 {
    CPU6809_V6 cpu;

    public InstructionDecoder_V6(CPU6809_V6 cpu) {
        this.cpu = cpu;
    }

    public void executeNext() throws Exception {
        int opcode = fetchByte();

        // Gestion Prefixe (Page 2)
        if (opcode == 0x10) {
            opcode = (opcode << 8) | fetchByte();
        }

        switch (opcode) {
            // Load A
            case 0x86: cpu.A = fetchByte(); cpu.updateFlags(cpu.A, false); break;         // IMM
            case 0x96: cpu.A = readMem(getDirectAddr(fetchByte())); cpu.updateFlags(cpu.A, false); break; // DIR
            case 0xA6: cpu.A = readMem(getIndexedAddr(fetchByte())); cpu.updateFlags(cpu.A, false); break; // IDX
            case 0xB6: cpu.A = readMem(fetchWord()); cpu.updateFlags(cpu.A, false); break; // EXT

            // Load B
            case 0xC6: cpu.B = fetchByte(); cpu.updateFlags(cpu.B, false); break;         // IMM
            case 0xD6: cpu.B = readMem(getDirectAddr(fetchByte())); cpu.updateFlags(cpu.B, false); break; // DIR
            case 0xE6: cpu.B = readMem(getIndexedAddr(fetchByte())); cpu.updateFlags(cpu.B, false); break; // IDX
            case 0xF6: cpu.B = readMem(fetchWord()); cpu.updateFlags(cpu.B, false); break; // EXT

            // Load D
            case 0xCC: cpu.setD(fetchWord()); cpu.updateFlags(cpu.getD(), true); break;   // IMM
            case 0xDC: cpu.setD(readWord(getDirectAddr(fetchByte()))); cpu.updateFlags(cpu.getD(), true); break; // DIR
            case 0xEC: cpu.setD(readWord(getIndexedAddr(fetchByte()))); cpu.updateFlags(cpu.getD(), true); break; // IDX
            case 0xFC: cpu.setD(readWord(fetchWord())); cpu.updateFlags(cpu.getD(), true); break; // EXT

            // Load X
            case 0x8E: cpu.X = fetchWord(); cpu.updateFlags(cpu.X, true); break;          // IMM
            case 0x9E: cpu.X = readWord(getDirectAddr(fetchByte())); cpu.updateFlags(cpu.X, true); break; // DIR
            case 0xAE: cpu.X = readWord(getIndexedAddr(fetchByte())); cpu.updateFlags(cpu.X, true); break; // IDX
            case 0xBE: cpu.X = readWord(fetchWord()); cpu.updateFlags(cpu.X, true); break; // EXT

            // Load Y
            case 0x108E: cpu.Y = fetchWord(); cpu.updateFlags(cpu.Y, true); break;        // IMM
            case 0x109E: cpu.Y = readWord(getDirectAddr(fetchByte())); cpu.updateFlags(cpu.Y, true); break; // DIR
            case 0x10AE: cpu.Y = readWord(getIndexedAddr(fetchByte())); cpu.updateFlags(cpu.Y, true); break; // IDX
            case 0x10BE: cpu.Y = readWord(fetchWord()); cpu.updateFlags(cpu.Y, true); break; // EXT

            // Load U
            case 0xCE: cpu.U = fetchWord(); cpu.updateFlags(cpu.U, true); break;          // IMM
            case 0xDE: cpu.U = readWord(getDirectAddr(fetchByte())); cpu.updateFlags(cpu.U, true); break; // DIR
            case 0xEE: cpu.U = readWord(getIndexedAddr(fetchByte())); cpu.updateFlags(cpu.U, true); break; // IDX

            // Load S
            case 0x10CE: cpu.S = fetchWord(); cpu.updateFlags(cpu.S, true); break;        // IMM
            case 0x10DE: cpu.S = readWord(getDirectAddr(fetchByte())); cpu.updateFlags(cpu.S, true); break; // DIR
            case 0x10EE: cpu.S = readWord(getIndexedAddr(fetchByte())); cpu.updateFlags(cpu.S, true); break; // IDX

            // Store A
            case 0xA7: writeMem(getIndexedAddr(fetchByte()), cpu.A); cpu.updateFlags(cpu.A, false); break; // IDX
            case 0x97: writeMem(getDirectAddr(fetchByte()), cpu.A); cpu.updateFlags(cpu.A, false); break; // DIR
            case 0xB7: writeMem(fetchWord(), cpu.A); cpu.updateFlags(cpu.A, false); break; // EXT

            // Store B
            case 0xE7: writeMem(getIndexedAddr(fetchByte()), cpu.B); cpu.updateFlags(cpu.B, false); break; // IDX
            case 0xD7: writeMem(getDirectAddr(fetchByte()), cpu.B); cpu.updateFlags(cpu.B, false); break; // DIR
            case 0xF7: writeMem(fetchWord(), cpu.B); cpu.updateFlags(cpu.B, false); break; // EXT

            // Store D
            case 0xED: writeWord(getIndexedAddr(fetchByte()), cpu.getD()); cpu.updateFlags(cpu.getD(), true); break; // IDX
            case 0xDD: writeWord(getDirectAddr(fetchByte()), cpu.getD()); cpu.updateFlags(cpu.getD(), true); break; // DIR
            case 0xFD: writeWord(fetchWord(), cpu.getD()); cpu.updateFlags(cpu.getD(), true); break; // EXT

            // Store X
            case 0xAF: writeWord(getIndexedAddr(fetchByte()), cpu.X); cpu.updateFlags(cpu.X, true); break; // IDX
            case 0x9F: writeWord(getDirectAddr(fetchByte()), cpu.X); cpu.updateFlags(cpu.X, true); break; // DIR
            case 0xBF: writeWord(fetchWord(), cpu.X); cpu.updateFlags(cpu.X, true); break; // EXT

            case 0xC3: {
                int operand = fetchWord();
                int original = cpu.getD();
                int r = original + operand;
                cpu.setD(r);
                cpu.updateFlagsAdd16(original, operand, r);
                break;
            }
            case 0x4C: {
                int original = cpu.A;
                cpu.A = (cpu.A + 1) & 0xFF;
                cpu.updateFlagsInc8(original, cpu.A);
                break;
            }
            case 0x4A: {
                int original = cpu.A;
                cpu.A = (cpu.A - 1) & 0xFF;
                cpu.updateFlagsDec8(original, cpu.A);
                break;
            }

            // Instructions INH sur A
            case 0x40: { // NEGA
                int original = cpu.A;
                cpu.A = ((~original) + 1) & 0xFF; // Négation en complément à 2
                cpu.updateFlagsNZ(cpu.A, false);
                cpu.CC |= CPU6809_V6.FLAG_C; // Carry toujours mis pour NEGA
                if (original == 0x80) cpu.CC |= CPU6809_V6.FLAG_V; // Overflow si -128
                break;
            }
            case 0x43: { // COMA
                cpu.A = (~cpu.A) & 0xFF;
                cpu.updateFlagsNZ(cpu.A, false);
                cpu.CC |= CPU6809_V6.FLAG_C; // Carry toujours mis pour COM
                break;
            }
            case 0x44: { // LSRA
                int carry = cpu.A & 0x01;
                cpu.A = (cpu.A >> 1) & 0x7F; // Shift right logique
                cpu.updateFlagsNZ(cpu.A, false);
                if (carry != 0) cpu.CC |= CPU6809_V6.FLAG_C; else cpu.CC &= ~CPU6809_V6.FLAG_C;
                break;
            }
            case 0x46: { // RORA
                int carry = cpu.A & 0x01;
                int oldCarry = (cpu.CC & CPU6809_V6.FLAG_C) != 0 ? 0x80 : 0;
                cpu.A = ((cpu.A >> 1) | oldCarry) & 0xFF;
                cpu.updateFlagsNZ(cpu.A, false);
                if (carry != 0) cpu.CC |= CPU6809_V6.FLAG_C; else cpu.CC &= ~CPU6809_V6.FLAG_C;
                break;
            }
            case 0x48: { // ASLA
                int carry = (cpu.A & 0x80) != 0 ? 1 : 0;
                cpu.A = (cpu.A << 1) & 0xFF;
                cpu.updateFlagsNZ(cpu.A, false);
                if (carry != 0) cpu.CC |= CPU6809_V6.FLAG_C; else cpu.CC &= ~CPU6809_V6.FLAG_C;
                // Overflow pour ASLA : V = 1 si bit 6 et 7 étaient différents avant
                int bit6 = (cpu.A & 0x40) != 0 ? 1 : 0;
                int bit7 = (cpu.A & 0x80) != 0 ? 1 : 0;
                if (bit6 != bit7) cpu.CC |= CPU6809_V6.FLAG_V; else cpu.CC &= ~CPU6809_V6.FLAG_V;
                break;
            }
            case 0x49: { // ROLA
                int oldCarry = (cpu.CC & CPU6809_V6.FLAG_C) != 0 ? 1 : 0;
                int newCarry = (cpu.A & 0x80) != 0 ? CPU6809_V6.FLAG_C : 0;
                cpu.A = ((cpu.A << 1) | oldCarry) & 0xFF;
                cpu.updateFlagsNZ(cpu.A, false);
                cpu.CC = (cpu.CC & ~CPU6809_V6.FLAG_C) | newCarry;
                break;
            }
            case 0x4D: { // TSTA
                cpu.updateFlagsNZ(cpu.A, false);
                break;
            }
            case 0x4F: { // CLRA
                cpu.A = 0;
                cpu.updateFlagsNZ(cpu.A, false);
                cpu.CC &= ~(CPU6809_V6.FLAG_V | CPU6809_V6.FLAG_C); // V et C remis à 0
                break;
            }

            // Instructions INH sur B
            case 0x50: { // NEGB
                int original = cpu.B;
                cpu.B = ((~original) + 1) & 0xFF; // Négation en complément à 2
                cpu.updateFlagsNZ(cpu.B, false);
                cpu.CC |= CPU6809_V6.FLAG_C; // Carry toujours mis pour NEGB
                if (original == 0x80) cpu.CC |= CPU6809_V6.FLAG_V; // Overflow si -128
                break;
            }
            case 0x53: { // COMB
                cpu.B = (~cpu.B) & 0xFF;
                cpu.updateFlagsNZ(cpu.B, false);
                cpu.CC |= CPU6809_V6.FLAG_C; // Carry toujours mis pour COM
                break;
            }
            case 0x54: { // LSRB
                int carry = cpu.B & 0x01;
                cpu.B = (cpu.B >> 1) & 0x7F; // Shift right logique
                cpu.updateFlagsNZ(cpu.B, false);
                if (carry != 0) cpu.CC |= CPU6809_V6.FLAG_C; else cpu.CC &= ~CPU6809_V6.FLAG_C;
                break;
            }
            case 0x56: { // RORB
                int carry = cpu.B & 0x01;
                int oldCarry = (cpu.CC & CPU6809_V6.FLAG_C) != 0 ? 0x80 : 0;
                cpu.B = ((cpu.B >> 1) | oldCarry) & 0xFF;
                cpu.updateFlagsNZ(cpu.B, false);
                if (carry != 0) cpu.CC |= CPU6809_V6.FLAG_C; else cpu.CC &= ~CPU6809_V6.FLAG_C;
                break;
            }
            case 0x58: { // ASLB
                int carry = (cpu.B & 0x80) != 0 ? 1 : 0;
                cpu.B = (cpu.B << 1) & 0xFF;
                cpu.updateFlagsNZ(cpu.B, false);
                if (carry != 0) cpu.CC |= CPU6809_V6.FLAG_C; else cpu.CC &= ~CPU6809_V6.FLAG_C;
                // Overflow pour ASLB
                int bit6 = (cpu.B & 0x40) != 0 ? 1 : 0;
                int bit7 = (cpu.B & 0x80) != 0 ? 1 : 0;
                if (bit6 != bit7) cpu.CC |= CPU6809_V6.FLAG_V; else cpu.CC &= ~CPU6809_V6.FLAG_V;
                break;
            }
            case 0x59: { // ROLB
                int oldCarry = (cpu.CC & CPU6809_V6.FLAG_C) != 0 ? 1 : 0;
                int newCarry = (cpu.B & 0x80) != 0 ? CPU6809_V6.FLAG_C : 0;
                cpu.B = ((cpu.B << 1) | oldCarry) & 0xFF;
                cpu.updateFlagsNZ(cpu.B, false);
                cpu.CC = (cpu.CC & ~CPU6809_V6.FLAG_C) | newCarry;
                break;
            }
            case 0x5D: { // TSTB
                cpu.updateFlagsNZ(cpu.B, false);
                break;
            }
            case 0x5F: { // CLRB
                cpu.B = 0;
                cpu.updateFlagsNZ(cpu.B, false);
                cpu.CC &= ~(CPU6809_V6.FLAG_V | CPU6809_V6.FLAG_C); // V et C remis à 0
                break;
            } 

            // Branchements
            case 0x7E: cpu.PC = fetchWord(); break; 
            case 0x20: branch(true); break; 
            case 0x27: branch((cpu.CC & 0x04) != 0); break; 
            case 0x26: branch((cpu.CC & 0x04) == 0); break; 

            case 0x1F: fetchByte(); cpu.B = cpu.A; break; 
            case 0x12: break; 
            case 0x00: throw new Exception("HALT (Opcode 00)"); 

            default: break; 
        }
    }

    private void branch(boolean condition) {
        byte offset = (byte)fetchByte(); 
        if (condition) {
            cpu.PC = (cpu.PC + offset) & 0xFFFF;
        }
    }

    private int fetchByte() { return cpu.memory[cpu.PC++] & 0xFF; }
    private int fetchWord() { return (fetchByte() << 8) | fetchByte(); }
    // Calcul d'adresse pour le mode Direct
    private int getDirectAddr(int offset8) {
        return ((cpu.DP & 0xFF) << 8) | (offset8 & 0xFF);
    }

    // Calcul d'adresse pour le mode Indexé
    private int getIndexedAddr(int postByte) {
        // Extraire le numéro du registre (bits 0-1 du post-byte)
        int regNum = postByte & 0x03;

        // Déterminer la valeur du registre
        int regValue;
        switch (regNum) {
            case 0: regValue = cpu.X; break; // X
            case 1: regValue = cpu.Y; break; // Y
            case 2: regValue = cpu.U; break; // U
            case 3: regValue = cpu.S; break; // S
            default: regValue = 0; break;
        }

        // Pour l'instant, on ne gère que les offsets 8 bits constants
        // Post-byte 0x84+X, 0xA4+Y, 0xC4+U, 0xE4+S pour offset 8 bits
        if ((postByte & 0x80) == 0) { // Bit 7 = 0 pour offset 8 bits
            // Lire l'offset 8 bits suivant
            int offset8 = fetchByte();
            // L'offset est signé, on l'étend à 16 bits
            int signedOffset = (offset8 & 0x80) != 0 ? offset8 | 0xFF00 : offset8;
            return (regValue + signedOffset) & 0xFFFF;
        }

        // Pour les autres modes, retourner juste la valeur du registre
        return regValue & 0xFFFF;
    }

    private int readMem(int addr) { return cpu.memory[addr & 0xFFFF] & 0xFF; }
    private int readWord(int addr) { return (readMem(addr) << 8) | readMem(addr+1); }

    private void writeMem(int addr, int val) {
        addr &= 0xFFFF;
        cpu.memory[addr] = val & 0xFF;
        if (cpu.ioMonitor != null) cpu.ioMonitor.onWrite(addr, val);
    }

    private void writeWord(int addr, int val) {
        writeMem(addr, val >> 8);
        writeMem(addr+1, val);
    }
}

// ==========================================
// 4. GUI (AVEC TOUS LES REGISTRES)
// ==========================================
public class Simulateur6809 extends JFrame {
    private CPU6809_V6 cpu;
    private MiniAssembler_V6 assembler;
    private InstructionDecoder_V6 decoder;

    // UI Components : Ajout de tous les registres
    private JTextField txtPC, txtA, txtB, txtD, txtDP, txtX, txtY, txtU, txtS, txtCC, txtBreakpoints;
    private JTextArea codeEditor, terminalOutput;
    private DefaultTableModel memoryModel;
    private JLabel lblStatus;
    private boolean isRunning = false;
    private String lastCode = "";

    public Simulateur6809() {
        cpu = new CPU6809_V6();
        assembler = new MiniAssembler_V6();
        decoder = new InstructionDecoder_V6(cpu);

        // --- I/O Monitor ---
        cpu.ioMonitor = (addr, val) -> {
            if (addr == 0xD000) { 
                SwingUtilities.invokeLater(() -> terminalOutput.append(String.valueOf((char)val)));
            }
        };

        setTitle("Simulateur Motorola 6809 - Full Registers Edition");
        setSize(1350, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 1. PANNEAU DE GAUCHE : TOUS LES REGISTRES
        JPanel pnlLeft = new JPanel(new BorderLayout());
        
        // GridLayout (0, 2) permet d'ajouter autant de lignes que nécessaire
        JPanel pnlReg = new JPanel(new GridLayout(0, 2, 5, 5));
        pnlReg.setBorder(new TitledBorder("Registres (Editables)"));
        
        // --- Création des champs pour tous les registres ---
        txtPC = addReg(pnlReg, "PC (Progr Ctr)", "0000");
        txtA  = addReg(pnlReg, "A (Accum 8)", "00");
        txtB  = addReg(pnlReg, "B (Accum 8)", "00");
        txtD  = addReg(pnlReg, "D (Accum 16)", "0000"); // Virtuel
        txtDP = addReg(pnlReg, "DP (Direct Pg)", "00");
        txtX  = addReg(pnlReg, "X (Index)", "0000");
        txtY  = addReg(pnlReg, "Y (Index)", "0000");
        txtU  = addReg(pnlReg, "U (User Stack)", "0000");
        txtS  = addReg(pnlReg, "S (Sys Stack)", "0100");
        txtCC = addReg(pnlReg, "CC (Flags)", "00000000");
        
        // Breakpoints
        JPanel pnlBreak = new JPanel(new BorderLayout());
        pnlBreak.setBorder(new TitledBorder("Breakpoints (ex: 0010)"));
        txtBreakpoints = new JTextField("");
        pnlBreak.add(txtBreakpoints, BorderLayout.CENTER);
        
        pnlLeft.add(pnlReg, BorderLayout.NORTH);
        pnlLeft.add(pnlBreak, BorderLayout.SOUTH);
        pnlLeft.setPreferredSize(new Dimension(280, 0)); // Plus large pour tout afficher
        add(pnlLeft, BorderLayout.WEST);

        // 2. CENTRE : CODE & TERMINAL
        JSplitPane splitCenter = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        
        codeEditor = new JTextArea();
        codeEditor.setFont(new Font("Consolas", Font.PLAIN, 16));
        codeEditor.setText(
            "; Test des flags - Programme de démonstration\n" +
            "LDA #$00    ; A=0 (Z=1, N=0)\n" +
            "INCA        ; A=1 (Z=0, N=0)\n" +
            "LDA #$7F    ; A=127\n" +
            "INCA        ; A=128 (N=1, V=1 - dépassement positif)\n" +
            "LDA #$FF    ; A=255 (N=1)\n" +
            "INCA        ; A=0 (Z=1, C=1 - carry, N=0)\n" +
            "LDA #$80    ; A=128 (N=1)\n" +
            "DECA        ; A=127 (N=0, V=1 - dépassement négatif)\n" +
            "LDD #$0000  ; D=0 (Z=1, N=0)\n" +
            "ADDD #$FFFF ; D=65535 (N=1, C=0)\n" +
            "ADDD #$0001 ; D=0 (Z=1, C=1 - carry 16 bits)\n" +
            "NOP"
        );
        JPanel pnlCode = new JPanel(new BorderLayout());
        pnlCode.setBorder(new TitledBorder("Assembleur"));
        pnlCode.add(new JScrollPane(codeEditor));
        
        terminalOutput = new JTextArea();
        terminalOutput.setEditable(false);
        terminalOutput.setBackground(Color.BLACK);
        terminalOutput.setForeground(Color.GREEN);
        terminalOutput.setFont(new Font("Monospaced", Font.BOLD, 14));
        JPanel pnlTerm = new JPanel(new BorderLayout());
        pnlTerm.setBorder(new TitledBorder("Terminal Sortie ($D000)"));
        pnlTerm.add(new JScrollPane(terminalOutput));
        
        splitCenter.setTopComponent(pnlCode);
        splitCenter.setBottomComponent(pnlTerm);
        splitCenter.setDividerLocation(450);
        add(splitCenter, BorderLayout.CENTER);

        // 3. DROITE : MÉMOIRE
        String[] cols = {"Addr", "Value (Hex)"};
        memoryModel = new DefaultTableModel(cols, 0) {
            @Override 
            public boolean isCellEditable(int row, int column) { return column == 1; }
        };
        JTable table = new JTable(memoryModel);
        table.getModel().addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE) updateMemoryFromTable(e.getFirstRow());
        });
        add(new JScrollPane(table), BorderLayout.EAST);

        // 4. BAS : CONTRÔLES
        JPanel pnlBot = new JPanel(new FlowLayout());
        JButton btnStep = new JButton("Pas à Pas");
        JButton btnRun = new JButton("RUN");
        JButton btnNMI = new JButton("NMI (Interrupt)");
        JButton btnReset = new JButton("RESET");

        btnStep.addActionListener(e -> doStep());
        btnRun.addActionListener(e -> toggleRun(btnRun));
        btnNMI.addActionListener(e -> { cpu.triggerNMI(); updateUI(); terminalOutput.append("\n[NMI]\n"); });
        btnReset.addActionListener(e -> { cpu.reset(); lastCode=""; terminalOutput.setText(""); updateUI(); });

        pnlBot.add(btnStep);
        pnlBot.add(btnRun);
        pnlBot.add(btnNMI);
        pnlBot.add(btnReset);
        
        lblStatus = new JLabel(" Prêt.");
        add(pnlBot, BorderLayout.SOUTH);
        add(lblStatus, BorderLayout.NORTH);

        initMemTable();
        setupRegListeners(); // Active la modification manuelle
        updateUI();
    }

    private void doStep() {
        checkAssembly();
        try {
            decoder.executeNext();
            updateUI();
            lblStatus.setText("Step OK : " + String.format("%04X", cpu.PC));
        } catch (Exception ex) {
            lblStatus.setText("Erreur: " + ex.getMessage());
            stopRun();
        }
    }

    private void toggleRun(JButton btn) {
        if (isRunning) {
            stopRun();
            btn.setText("RUN");
            return;
        }
        
        isRunning = true;
        btn.setText("STOP");
        checkAssembly();
        
        Set<Integer> breaks = new HashSet<>();
        for(String s : txtBreakpoints.getText().split(",")) {
            try { if(!s.trim().isEmpty()) breaks.add(Integer.parseInt(s.trim(), 16)); } catch(Exception e){}
        }

        new Thread(() -> {
            while (isRunning) {
                try {
                    if (breaks.contains(cpu.PC)) {
                        SwingUtilities.invokeLater(() -> {
                            lblStatus.setText("Breakpoint atteint à " + String.format("%04X", cpu.PC));
                            stopRun();
                            btn.setText("RUN");
                        });
                        break;
                    }
                    decoder.executeNext();
                    Thread.sleep(2); 
                    SwingUtilities.invokeLater(this::updateUI);
                } catch (Exception ex) {
                    isRunning = false;
                }
            }
        }).start();
    }

    private void stopRun() { isRunning = false; }

    private void checkAssembly() {
        String current = codeEditor.getText();
        if (!current.equals(lastCode)) {
            try {
                cpu.reset();
                assembler.assemble(current, cpu);
                lastCode = current;
            } catch (Exception e) {}
        }
    }

    private JTextField addReg(JPanel p, String lbl, String val) {
        p.add(new JLabel(" " + lbl));
        JTextField tf = new JTextField(val);
        tf.setHorizontalAlignment(JTextField.CENTER);
        tf.setFont(new Font("Consolas", Font.BOLD, 14));
        p.add(tf);
        return tf;
    }

    // --- SETUP DES LISTENERS POUR MODIFIER MANUELLEMENT TOUS LES REGISTRES ---
    private void setupRegListeners() {
        // Registres 8 bits
        txtA.addActionListener(e -> { cpu.A = parse(txtA.getText()); updateUI(); });
        txtB.addActionListener(e -> { cpu.B = parse(txtB.getText()); updateUI(); });
        txtDP.addActionListener(e -> { cpu.DP = parse(txtDP.getText()); updateUI(); });
        
        // Registres 16 bits
        txtPC.addActionListener(e -> { cpu.PC = parse(txtPC.getText()); updateUI(); });
        txtX.addActionListener(e -> { cpu.X = parse(txtX.getText()); updateUI(); });
        txtY.addActionListener(e -> { cpu.Y = parse(txtY.getText()); updateUI(); });
        txtU.addActionListener(e -> { cpu.U = parse(txtU.getText()); updateUI(); });
        txtS.addActionListener(e -> { cpu.S = parse(txtS.getText()); updateUI(); });
        
        // Registre D (special : met à jour A et B)
        txtD.addActionListener(e -> { cpu.setD(parse(txtD.getText())); updateUI(); });
    }

    private int parse(String s) {
        try { return Integer.parseInt(s.trim(), 16); } catch(Exception e) { return 0; }
    }

    private void initMemTable() {
        for(int i=0; i<100; i++) memoryModel.addRow(new Object[]{String.format("%04X", i), "00"});
    }

    private void updateMemoryFromTable(int row) {
        try {
            int addr = Integer.parseInt((String)memoryModel.getValueAt(row, 0), 16);
            int val = Integer.parseInt((String)memoryModel.getValueAt(row, 1), 16);
            cpu.memory[addr] = val & 0xFF;
        } catch (Exception e) {}
    }

    // --- MISE A JOUR DE L'AFFICHAGE (TOUS LES CHAMPS) ---
    private void updateUI() {
        // On ne met à jour le texte que si l'utilisateur n'est pas en train d'écrire dedans (hasFocus)
        if (!txtA.hasFocus()) txtA.setText(String.format("%02X", cpu.A));
        if (!txtB.hasFocus()) txtB.setText(String.format("%02X", cpu.B));
        if (!txtD.hasFocus()) txtD.setText(String.format("%04X", cpu.getD()));
        if (!txtDP.hasFocus()) txtDP.setText(String.format("%02X", cpu.DP));
        
        if (!txtPC.hasFocus()) txtPC.setText(String.format("%04X", cpu.PC));
        if (!txtX.hasFocus()) txtX.setText(String.format("%04X", cpu.X));
        if (!txtY.hasFocus()) txtY.setText(String.format("%04X", cpu.Y));
        if (!txtU.hasFocus()) txtU.setText(String.format("%04X", cpu.U));
        if (!txtS.hasFocus()) txtS.setText(String.format("%04X", cpu.S));
        
        txtCC.setText(String.format("%8s", Integer.toBinaryString(cpu.CC)).replace(' ', '0'));

        for(int i=0; i<100; i++) {
            memoryModel.setValueAt(String.format("%02X", cpu.memory[i]), i, 1);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Simulateur6809().setVisible(true));
    }
}
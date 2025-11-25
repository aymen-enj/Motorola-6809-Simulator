package sim;

import java.util.Arrays;

/**
 * Modèle complet du CPU Motorola 6809 avec gestion mémoire et flags.
 */
public class CPU6809_V6 {
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
    public boolean[] memoryRevealed = new boolean[65536];

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
        Arrays.fill(memoryRevealed, false);
        A = B = DP = CC = 0;
        X = Y = U = 0;
        S = 0x0100; // Pile standard
        PC = 0x0000;
    }

    // Gestion 16 bits (D est virtuel : concaténation A:B)
    public int getD() {
        return ((A & 0xFF) << 8) | (B & 0xFF);
    }

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

        // Test Overflow : V = 1 si décrémentation de 0x80
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

        // Test Overflow : V = 1 si incrémentation de 0x7F
        if (original == 0x7F) {
            CC |= FLAG_V;
        }
    }

    // Interruption Matérielle (Simulation C.1)
    public void triggerNMI() {
        int vector = (memory[0xFFFC] << 8) | memory[0xFFFD];
        if (vector == 0) vector = 0x1000; // Fallback si non défini
        PC = vector;
    }

    public void revealAddress(int addr) {
        memoryRevealed[addr & 0xFFFF] = true;
    }
}


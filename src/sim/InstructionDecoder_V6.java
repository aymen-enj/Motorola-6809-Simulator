package sim;

/**
 * Décodeur / exécuteur d'instructions pour le CPU 6809.
 */
public class InstructionDecoder_V6 {
    private final CPU6809_V6 cpu;

    public InstructionDecoder_V6(CPU6809_V6 cpu) {
        this.cpu = cpu;
    }

    public void executeNext() throws Exception {
        int opcode = fetchByte();

        // Gestion Préfixe (Page 2)
        if (opcode == 0x10) {
            opcode = (opcode << 8) | fetchByte();
        }

        switch (opcode) {
            // --- Load ---
            case 0x86: cpu.A = fetchByte(); cpu.updateFlags(cpu.A, false); break;
            case 0x96: cpu.A = readMem(getDirectAddr(fetchByte())); cpu.updateFlags(cpu.A, false); break;
            case 0xA6: cpu.A = readMem(getIndexedAddr(fetchByte())); cpu.updateFlags(cpu.A, false); break;
            case 0xB6: cpu.A = readMem(fetchWord()); cpu.updateFlags(cpu.A, false); break;

            case 0xC6: cpu.B = fetchByte(); cpu.updateFlags(cpu.B, false); break;
            case 0xD6: cpu.B = readMem(getDirectAddr(fetchByte())); cpu.updateFlags(cpu.B, false); break;
            case 0xE6: cpu.B = readMem(getIndexedAddr(fetchByte())); cpu.updateFlags(cpu.B, false); break;
            case 0xF6: cpu.B = readMem(fetchWord()); cpu.updateFlags(cpu.B, false); break;

            case 0xCC: cpu.setD(fetchWord()); cpu.updateFlags(cpu.getD(), true); break;
            case 0xDC: cpu.setD(readWord(getDirectAddr(fetchByte()))); cpu.updateFlags(cpu.getD(), true); break;
            case 0xEC: cpu.setD(readWord(getIndexedAddr(fetchByte()))); cpu.updateFlags(cpu.getD(), true); break;
            case 0xFC: cpu.setD(readWord(fetchWord())); cpu.updateFlags(cpu.getD(), true); break;

            case 0x8E: cpu.X = fetchWord(); cpu.updateFlags(cpu.X, true); break;
            case 0x9E: cpu.X = readWord(getDirectAddr(fetchByte())); cpu.updateFlags(cpu.X, true); break;
            case 0xAE: cpu.X = readWord(getIndexedAddr(fetchByte())); cpu.updateFlags(cpu.X, true); break;
            case 0xBE: cpu.X = readWord(fetchWord()); cpu.updateFlags(cpu.X, true); break;

            case 0x108E: cpu.Y = fetchWord(); cpu.updateFlags(cpu.Y, true); break;
            case 0x109E: cpu.Y = readWord(getDirectAddr(fetchByte())); cpu.updateFlags(cpu.Y, true); break;
            case 0x10AE: cpu.Y = readWord(getIndexedAddr(fetchByte())); cpu.updateFlags(cpu.Y, true); break;
            case 0x10BE: cpu.Y = readWord(fetchWord()); cpu.updateFlags(cpu.Y, true); break;

            case 0xCE: cpu.U = fetchWord(); cpu.updateFlags(cpu.U, true); break;
            case 0xDE: cpu.U = readWord(getDirectAddr(fetchByte())); cpu.updateFlags(cpu.U, true); break;
            case 0xEE: cpu.U = readWord(getIndexedAddr(fetchByte())); cpu.updateFlags(cpu.U, true); break;

            case 0x10CE: cpu.S = fetchWord(); cpu.updateFlags(cpu.S, true); break;
            case 0x10DE: cpu.S = readWord(getDirectAddr(fetchByte())); cpu.updateFlags(cpu.S, true); break;
            case 0x10EE: cpu.S = readWord(getIndexedAddr(fetchByte())); cpu.updateFlags(cpu.S, true); break;

            // --- Store ---
            case 0xA7: writeMem(getIndexedAddr(fetchByte()), cpu.A); cpu.updateFlags(cpu.A, false); break;
            case 0x97: writeMem(getDirectAddr(fetchByte()), cpu.A); cpu.updateFlags(cpu.A, false); break;
            case 0xB7: writeMem(fetchWord(), cpu.A); cpu.updateFlags(cpu.A, false); break;

            case 0xE7: writeMem(getIndexedAddr(fetchByte()), cpu.B); cpu.updateFlags(cpu.B, false); break;
            case 0xD7: writeMem(getDirectAddr(fetchByte()), cpu.B); cpu.updateFlags(cpu.B, false); break;
            case 0xF7: writeMem(fetchWord(), cpu.B); cpu.updateFlags(cpu.B, false); break;

            case 0xED: writeWord(getIndexedAddr(fetchByte()), cpu.getD()); cpu.updateFlags(cpu.getD(), true); break;
            case 0xDD: writeWord(getDirectAddr(fetchByte()), cpu.getD()); cpu.updateFlags(cpu.getD(), true); break;
            case 0xFD: writeWord(fetchWord(), cpu.getD()); cpu.updateFlags(cpu.getD(), true); break;

            case 0xAF: writeWord(getIndexedAddr(fetchByte()), cpu.X); cpu.updateFlags(cpu.X, true); break;
            case 0x9F: writeWord(getDirectAddr(fetchByte()), cpu.X); cpu.updateFlags(cpu.X, true); break;
            case 0xBF: writeWord(fetchWord(), cpu.X); cpu.updateFlags(cpu.X, true); break;

            // --- Arithmétique ---
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

            // --- INH A ---
            case 0x40: {
                int original = cpu.A;
                cpu.A = ((~original) + 1) & 0xFF;
                cpu.updateFlagsNZ(cpu.A, false);
                cpu.CC |= CPU6809_V6.FLAG_C;
                if (original == 0x80) cpu.CC |= CPU6809_V6.FLAG_V;
                break;
            }
            case 0x43: cpu.A = (~cpu.A) & 0xFF; cpu.updateFlagsNZ(cpu.A, false); cpu.CC |= CPU6809_V6.FLAG_C; break;
            case 0x44: { int carry = cpu.A & 0x01; cpu.A = (cpu.A >> 1) & 0x7F; cpu.updateFlagsNZ(cpu.A, false); updateCarry(carry); break; }
            case 0x46: { int carry = cpu.A & 0x01; int oldCarry = (cpu.CC & CPU6809_V6.FLAG_C) != 0 ? 0x80 : 0; cpu.A = ((cpu.A >> 1) | oldCarry) & 0xFF; cpu.updateFlagsNZ(cpu.A, false); updateCarry(carry); break; }
            case 0x48: { int carry = (cpu.A & 0x80) != 0 ? 1 : 0; cpu.A = (cpu.A << 1) & 0xFF; cpu.updateFlagsNZ(cpu.A, false); updateCarry(carry); updateOverflowShift(cpu.A); break; }
            case 0x49: { int oldCarry = (cpu.CC & CPU6809_V6.FLAG_C) != 0 ? 1 : 0; int newCarry = (cpu.A & 0x80) != 0 ? CPU6809_V6.FLAG_C : 0; cpu.A = ((cpu.A << 1) | oldCarry) & 0xFF; cpu.updateFlagsNZ(cpu.A, false); cpu.CC = (cpu.CC & ~CPU6809_V6.FLAG_C) | newCarry; break; }
            case 0x4D: cpu.updateFlagsNZ(cpu.A, false); break;
            case 0x4F: cpu.A = 0; cpu.updateFlagsNZ(cpu.A, false); cpu.CC &= ~(CPU6809_V6.FLAG_V | CPU6809_V6.FLAG_C); break;

            // --- INH B ---
            case 0x50: {
                int original = cpu.B;
                cpu.B = ((~original) + 1) & 0xFF;
                cpu.updateFlagsNZ(cpu.B, false);
                cpu.CC |= CPU6809_V6.FLAG_C;
                if (original == 0x80) cpu.CC |= CPU6809_V6.FLAG_V;
                break;
            }
            case 0x53: cpu.B = (~cpu.B) & 0xFF; cpu.updateFlagsNZ(cpu.B, false); cpu.CC |= CPU6809_V6.FLAG_C; break;
            case 0x54: { int carry = cpu.B & 0x01; cpu.B = (cpu.B >> 1) & 0x7F; cpu.updateFlagsNZ(cpu.B, false); updateCarry(carry); break; }
            case 0x56: { int carry = cpu.B & 0x01; int oldCarry = (cpu.CC & CPU6809_V6.FLAG_C) != 0 ? 0x80 : 0; cpu.B = ((cpu.B >> 1) | oldCarry) & 0xFF; cpu.updateFlagsNZ(cpu.B, false); updateCarry(carry); break; }
            case 0x58: { int carry = (cpu.B & 0x80) != 0 ? 1 : 0; cpu.B = (cpu.B << 1) & 0xFF; cpu.updateFlagsNZ(cpu.B, false); updateCarry(carry); updateOverflowShift(cpu.B); break; }
            case 0x59: { int oldCarry = (cpu.CC & CPU6809_V6.FLAG_C) != 0 ? 1 : 0; int newCarry = (cpu.B & 0x80) != 0 ? CPU6809_V6.FLAG_C : 0; cpu.B = ((cpu.B << 1) | oldCarry) & 0xFF; cpu.updateFlagsNZ(cpu.B, false); cpu.CC = (cpu.CC & ~CPU6809_V6.FLAG_C) | newCarry; break; }
            case 0x5D: cpu.updateFlagsNZ(cpu.B, false); break;
            case 0x5F: cpu.B = 0; cpu.updateFlagsNZ(cpu.B, false); cpu.CC &= ~(CPU6809_V6.FLAG_V | CPU6809_V6.FLAG_C); break;

            // --- Branch ---
            case 0x7E: cpu.PC = fetchWord(); break;
            case 0x20: branch(true); break;
            case 0x27: branch((cpu.CC & CPU6809_V6.FLAG_Z) != 0); break;
            case 0x26: branch((cpu.CC & CPU6809_V6.FLAG_Z) == 0); break;

            case 0x1F: fetchByte(); cpu.B = cpu.A; break;
            case 0x12: break;
            case 0x00: throw new Exception("HALT (Opcode 00)");

            default: break;
        }
    }

    private void branch(boolean condition) {
        byte offset = (byte) fetchByte();
        if (condition) {
            cpu.PC = (cpu.PC + offset) & 0xFFFF;
        }
    }

    private int fetchByte() {
        int addr = cpu.PC & 0xFFFF;
        cpu.revealAddress(addr);
        return cpu.memory[cpu.PC++] & 0xFF;
    }

    private int fetchWord() {
        return (fetchByte() << 8) | fetchByte();
    }

    private int getDirectAddr(int offset8) {
        return ((cpu.DP & 0xFF) << 8) | (offset8 & 0xFF);
    }

    private int getIndexedAddr(int postByte) {
        int regNum = postByte & 0x03;
        int regValue;
        switch (regNum) {
            case 0: regValue = cpu.X; break;
            case 1: regValue = cpu.Y; break;
            case 2: regValue = cpu.U; break;
            case 3: regValue = cpu.S; break;
            default: regValue = 0; break;
        }

        if ((postByte & 0x80) == 0) {
            int offset8 = fetchByte();
            int signedOffset = (offset8 & 0x80) != 0 ? offset8 | 0xFF00 : offset8;
            return (regValue + signedOffset) & 0xFFFF;
        }
        return regValue & 0xFFFF;
    }

    private int readMem(int addr) {
        addr &= 0xFFFF;
        cpu.revealAddress(addr);
        return cpu.memory[addr] & 0xFF;
    }

    private int readWord(int addr) {
        return (readMem(addr) << 8) | readMem(addr + 1);
    }

    private void writeMem(int addr, int val) {
        addr &= 0xFFFF;
        cpu.memory[addr] = val & 0xFF;
        cpu.revealAddress(addr);
        if (cpu.ioMonitor != null) cpu.ioMonitor.onWrite(addr, val);
    }

    private void writeWord(int addr, int val) {
        writeMem(addr, val >> 8);
        writeMem(addr + 1, val);
    }

    private void updateCarry(int carry) {
        if (carry != 0) cpu.CC |= CPU6809_V6.FLAG_C;
        else cpu.CC &= ~CPU6809_V6.FLAG_C;
    }

    private void updateOverflowShift(int value) {
        int bit6 = (value & 0x40) != 0 ? 1 : 0;
        int bit7 = (value & 0x80) != 0 ? 1 : 0;
        if (bit6 != bit7) cpu.CC |= CPU6809_V6.FLAG_V;
        else cpu.CC &= ~CPU6809_V6.FLAG_V;
    }
}


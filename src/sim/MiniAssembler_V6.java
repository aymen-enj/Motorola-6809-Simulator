package sim;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Assembleur minimal pour convertir du code 6809 en code machine.
 */
public class MiniAssembler_V6 {
    private static final Map<String, Integer> OPCODES = new HashMap<>();

    static {
        // Chargements
        OPCODES.put("LDA_IMM", 0x86); OPCODES.put("LDA_DIR", 0x96); OPCODES.put("LDA_IDX", 0xA6); OPCODES.put("LDA_EXT", 0xB6);
        OPCODES.put("LDB_IMM", 0xC6); OPCODES.put("LDB_DIR", 0xD6); OPCODES.put("LDB_IDX", 0xE6); OPCODES.put("LDB_EXT", 0xF6);
        OPCODES.put("LDX_IMM", 0x8E); OPCODES.put("LDX_DIR", 0x9E); OPCODES.put("LDX_IDX", 0xAE); OPCODES.put("LDX_EXT", 0xBE);
        OPCODES.put("LDY_IMM", 0x108E); OPCODES.put("LDY_DIR", 0x109E); OPCODES.put("LDY_IDX", 0x10AE); OPCODES.put("LDY_EXT", 0x10BE);
        OPCODES.put("LDD_IMM", 0xCC); OPCODES.put("LDD_DIR", 0xDC); OPCODES.put("LDD_IDX", 0xEC); OPCODES.put("LDD_EXT", 0xFC);
        OPCODES.put("LDS_IMM", 0x10CE); OPCODES.put("LDS_DIR", 0x10DE); OPCODES.put("LDS_IDX", 0x10EE);
        OPCODES.put("LDU_IMM", 0xCE); OPCODES.put("LDU_DIR", 0xDE); OPCODES.put("LDU_IDX", 0xEE);

        // Stockage
        OPCODES.put("STA_IDX", 0xA7); OPCODES.put("STA_DIR", 0x97); OPCODES.put("STA_EXT", 0xB7);
        OPCODES.put("STB_IDX", 0xE7); OPCODES.put("STB_DIR", 0xD7); OPCODES.put("STB_EXT", 0xF7);
        OPCODES.put("STD_IDX", 0xED); OPCODES.put("STD_DIR", 0xDD); OPCODES.put("STD_EXT", 0xFD);
        OPCODES.put("STX_IDX", 0xAF); OPCODES.put("STX_DIR", 0x9F); OPCODES.put("STX_EXT", 0xBF);

        // Arithmétique / INH
        OPCODES.put("ADDD_IMM", 0xC3);
        OPCODES.put("INCA_INH", 0x4C);
        OPCODES.put("DECA_INH", 0x4A);

        // Instructions INH A
        OPCODES.put("NEGA_INH", 0x40);
        OPCODES.put("COMA_INH", 0x43);
        OPCODES.put("LSRA_INH", 0x44);
        OPCODES.put("RORA_INH", 0x46);
        OPCODES.put("ASLA_INH", 0x48);
        OPCODES.put("ROLA_INH", 0x49);
        OPCODES.put("TSTA_INH", 0x4D);
        OPCODES.put("CLRA_INH", 0x4F);

        // Instructions INH B
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
                mode = "DIR";
                val = parseHex(operand.substring(1));
                if (val > 0xFF) throw new Exception("Adresse directe > 255 : " + operand);
            } else if (operand.contains(",") && operand.matches(".*,[XYUS]$")) {
                mode = "IDX";
                val = 0;
            } else if (!operand.isEmpty() && OPCODES.containsKey(mnemonic + "_DIR")) {
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

            if (opcode > 0xFF) {
                cpu.memory[addr++] = (opcode >> 8) & 0xFF;
                cpu.memory[addr++] = opcode & 0xFF;
            } else {
                cpu.memory[addr++] = opcode;
            }

            if (mode.equals("IDX")) {
                if (operand.contains(",X")) {
                    cpu.memory[addr++] = 0x84;
                } else if (operand.contains(",Y")) {
                    cpu.memory[addr++] = 0xA4;
                } else if (operand.contains(",U")) {
                    cpu.memory[addr++] = 0xC4;
                } else if (operand.contains(",S")) {
                    cpu.memory[addr++] = 0xE4;
                }
                String offsetStr = operand.substring(0, operand.indexOf(','));
                int offset = parseHex(offsetStr);
                cpu.memory[addr++] = offset & 0xFF;
            } else if (mode.equals("DIR")) {
                cpu.memory[addr++] = val & 0xFF;
            } else if (mode.equals("EXT") || (mode.equals("IMM") && is16Bit(mnemonic))) {
                cpu.memory[addr++] = (val >> 8) & 0xFF;
                cpu.memory[addr++] = val & 0xFF;
            } else if (mode.equals("IMM")) {
                cpu.memory[addr++] = val & 0xFF;
            } else if (mode.equals("REL")) {
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
        try {
            return Integer.parseInt(s, 16);
        } catch (Exception e) {
            return 0;
        }
    }
}


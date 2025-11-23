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

    public void updateFlags(int value, boolean is16Bit) {
        CC &= ~(0x04 | 0x08); // Reset Z, N
        if (value == 0) CC |= 0x04; // Z
        int msb = is16Bit ? 0x8000 : 0x80;
        if ((value & msb) != 0) CC |= 0x08; // N
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
        OPCODES.put("LDA_IMM", 0x86); OPCODES.put("LDA_EXT", 0xB6);
        OPCODES.put("LDB_IMM", 0xC6); OPCODES.put("LDB_EXT", 0xF6);
        OPCODES.put("LDX_IMM", 0x8E); OPCODES.put("LDX_EXT", 0xBE);
        OPCODES.put("LDY_IMM", 0x108E); OPCODES.put("LDY_EXT", 0x10BE); // Y utilise un préfixe 10
        OPCODES.put("LDD_IMM", 0xCC); OPCODES.put("LDD_EXT", 0xFC);
        OPCODES.put("LDS_IMM", 0x10CE); 
        OPCODES.put("LDU_IMM", 0xCE); 
        
        // Stockage
        OPCODES.put("STA_EXT", 0xB7); OPCODES.put("STB_EXT", 0xF7);
        OPCODES.put("STD_EXT", 0xFD);
        OPCODES.put("STX_EXT", 0xBF);
        
        // Arithmétique
        OPCODES.put("ADDD_IMM", 0xC3); 
        OPCODES.put("INCA_INH", 0x4C); 
        OPCODES.put("DECA_INH", 0x4A);

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
            if (mode.equals("EXT") || (mode.equals("IMM") && is16Bit(mnemonic))) {
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
            case 0x86: cpu.A = fetchByte(); cpu.updateFlags(cpu.A, false); break; 
            case 0xB6: cpu.A = readMem(fetchWord()); cpu.updateFlags(cpu.A, false); break; 
            case 0xC6: cpu.B = fetchByte(); cpu.updateFlags(cpu.B, false); break; 
            case 0xCC: cpu.setD(fetchWord()); cpu.updateFlags(cpu.getD(), true); break; 
            
            // Indexés 16 bits
            case 0x8E: cpu.X = fetchWord(); cpu.updateFlags(cpu.X, true); break; 
            case 0xBE: cpu.X = readWord(fetchWord()); cpu.updateFlags(cpu.X, true); break;
            case 0x108E: cpu.Y = fetchWord(); cpu.updateFlags(cpu.Y, true); break; 
            case 0xCE: cpu.U = fetchWord(); cpu.updateFlags(cpu.U, true); break;
            case 0x10CE: cpu.S = fetchWord(); cpu.updateFlags(cpu.S, true); break;

            // Store
            case 0xB7: writeMem(fetchWord(), cpu.A); cpu.updateFlags(cpu.A, false); break; 
            case 0xF7: writeMem(fetchWord(), cpu.B); cpu.updateFlags(cpu.B, false); break; 
            case 0xFD: writeWord(fetchWord(), cpu.getD()); cpu.updateFlags(cpu.getD(), true); break; 
            case 0xBF: writeWord(fetchWord(), cpu.X); cpu.updateFlags(cpu.X, true); break;

            case 0xC3: int r = cpu.getD() + fetchWord(); cpu.setD(r); cpu.updateFlags(r, true); break; 
            case 0x4C: cpu.A = (cpu.A + 1) & 0xFF; cpu.updateFlags(cpu.A, false); break; 
            case 0x4A: cpu.A = (cpu.A - 1) & 0xFF; cpu.updateFlags(cpu.A, false); break; 

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
            "LDX #$1000  ; Init X\n" +
            "LDY #$2000  ; Init Y\n" +
            "LDA #$48    ; 'H'\n" +
            "STA $D000   ; Afficher\n" +
            "LDA #$49    ; 'I'\n" +
            "STA $D000\n" +
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
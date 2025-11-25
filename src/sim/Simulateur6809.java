package sim;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Interface graphique principale du simulateur 6809.
 */
public class Simulateur6809 extends JFrame {
    private final CPU6809_V6 cpu;
    private final MiniAssembler_V6 assembler;
    private final InstructionDecoder_V6 decoder;

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

        cpu.ioMonitor = (addr, val) -> {
            if (addr == 0xD000) {
                SwingUtilities.invokeLater(() -> terminalOutput.append(String.valueOf((char) val)));
            }
        };

        setTitle("Simulateur Motorola 6809 - Full Registers Edition");
        setSize(1350, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        buildLeftPanel();
        buildCenterPanel();
        buildRightPanel();
        buildBottomPanel();

        lblStatus = new JLabel(" Prêt.");
        add(lblStatus, BorderLayout.NORTH);

        initMemTable();
        setupRegListeners();
        updateUI();
    }

    private void buildLeftPanel() {
        JPanel pnlLeft = new JPanel(new BorderLayout());
        JPanel pnlReg = new JPanel(new GridLayout(0, 2, 5, 5));
        pnlReg.setBorder(new TitledBorder("Registres (Editables)"));

        txtPC = addReg(pnlReg, "PC (Progr Ctr)", "0000");
        txtA = addReg(pnlReg, "A (Accum 8)", "00");
        txtB = addReg(pnlReg, "B (Accum 8)", "00");
        txtD = addReg(pnlReg, "D (Accum 16)", "0000");
        txtDP = addReg(pnlReg, "DP (Direct Pg)", "00");
        txtX = addReg(pnlReg, "X (Index)", "0000");
        txtY = addReg(pnlReg, "Y (Index)", "0000");
        txtU = addReg(pnlReg, "U (User Stack)", "0000");
        txtS = addReg(pnlReg, "S (Sys Stack)", "0100");
        txtCC = addReg(pnlReg, "CC (Flags)", "00000000");

        JPanel pnlBreak = new JPanel(new BorderLayout());
        pnlBreak.setBorder(new TitledBorder("Breakpoints (ex: 0010)"));
        txtBreakpoints = new JTextField("");
        pnlBreak.add(txtBreakpoints, BorderLayout.CENTER);

        pnlLeft.add(pnlReg, BorderLayout.NORTH);
        pnlLeft.add(pnlBreak, BorderLayout.SOUTH);
        pnlLeft.setPreferredSize(new Dimension(280, 0));
        add(pnlLeft, BorderLayout.WEST);
    }

    private void buildCenterPanel() {
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
    }

    private void buildRightPanel() {
        String[] cols = {"Addr", "Value (Hex)"};
        memoryModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1;
            }
        };
        JTable table = new JTable(memoryModel);
        table.getModel().addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE) updateMemoryFromTable(e.getFirstRow());
        });
        add(new JScrollPane(table), BorderLayout.EAST);
    }

    private void buildBottomPanel() {
        JPanel pnlBot = new JPanel(new FlowLayout());
        JButton btnStep = new JButton("Pas à Pas");
        JButton btnRun = new JButton("RUN");
        JButton btnNMI = new JButton("NMI (Interrupt)");
        JButton btnReset = new JButton("RESET");

        btnStep.addActionListener(e -> doStep());
        btnRun.addActionListener(e -> toggleRun(btnRun));
        btnNMI.addActionListener(e -> {
            cpu.triggerNMI();
            updateUI();
            terminalOutput.append("\n[NMI]\n");
        });
        btnReset.addActionListener(e -> {
            cpu.reset();
            lastCode = "";
            terminalOutput.setText("");
            updateUI();
        });

        pnlBot.add(btnStep);
        pnlBot.add(btnRun);
        pnlBot.add(btnNMI);
        pnlBot.add(btnReset);
        add(pnlBot, BorderLayout.SOUTH);
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
        for (String s : txtBreakpoints.getText().split(",")) {
            try {
                if (!s.trim().isEmpty()) breaks.add(Integer.parseInt(s.trim(), 16));
            } catch (Exception ignored) {}
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

    private void stopRun() {
        isRunning = false;
    }

    private void checkAssembly() {
        String current = codeEditor.getText();
        if (!current.equals(lastCode)) {
            try {
                cpu.reset();
                assembler.assemble(current, cpu);
                lastCode = current;
            } catch (Exception ignored) {}
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

    private void setupRegListeners() {
        txtA.addActionListener(e -> { cpu.A = parse(txtA.getText()); updateUI(); });
        txtB.addActionListener(e -> { cpu.B = parse(txtB.getText()); updateUI(); });
        txtDP.addActionListener(e -> { cpu.DP = parse(txtDP.getText()); updateUI(); });
        txtPC.addActionListener(e -> { cpu.PC = parse(txtPC.getText()); updateUI(); });
        txtX.addActionListener(e -> { cpu.X = parse(txtX.getText()); updateUI(); });
        txtY.addActionListener(e -> { cpu.Y = parse(txtY.getText()); updateUI(); });
        txtU.addActionListener(e -> { cpu.U = parse(txtU.getText()); updateUI(); });
        txtS.addActionListener(e -> { cpu.S = parse(txtS.getText()); updateUI(); });
        txtD.addActionListener(e -> { cpu.setD(parse(txtD.getText())); updateUI(); });
    }

    private int parse(String s) {
        try {
            return Integer.parseInt(s.trim(), 16);
        } catch (Exception e) {
            return 0;
        }
    }

    private void initMemTable() {
        for (int i = 0; i < 100; i++) {
            memoryModel.addRow(new Object[]{String.format("%04X", i), "--"});
        }
    }

    private void updateMemoryFromTable(int row) {
        try {
            int addr = Integer.parseInt((String) memoryModel.getValueAt(row, 0), 16);
            int val = Integer.parseInt((String) memoryModel.getValueAt(row, 1), 16);
            cpu.memory[addr] = val & 0xFF;
            cpu.revealAddress(addr);
        } catch (Exception ignored) {}
    }

    private void updateUI() {
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

        for (int i = 0; i < 100; i++) {
            String value = cpu.memoryRevealed[i] ? String.format("%02X", cpu.memory[i]) : "--";
            memoryModel.setValueAt(value, i, 1);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Simulateur6809().setVisible(true));
    }
}


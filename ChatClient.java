import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class ChatClient {
    private static Socket socket;
    private static BufferedReader in;
    private static PrintWriter out;
    private static JTextArea logArea;

    public static void main(String[] args) throws Exception {
        String host = (args.length>0)?args[0]:"localhost";
        int port = 9000;
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        // identify as chat client
        out.println("CLIENT_CHAT|chat1");
        // consume welcome
        String welcome = in.readLine();
        System.out.println("Server: " + welcome);

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Sistema Bancario - Cliente GUI");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(700, 600);

            // Área de log común para todas las operaciones
            logArea = new JTextArea(10, 60);
            logArea.setEditable(false);
            logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            JScrollPane logScroll = new JScrollPane(logArea);
            logScroll.setBorder(BorderFactory.createTitledBorder("Registro de Operaciones"));

            // Crear pestañas
            JTabbedPane tabbedPane = new JTabbedPane();

            // Pestaña 1: Consultar Cuenta
            tabbedPane.addTab("Consultar Saldo", createConsultPanel());

            // Pestaña 2: Transferencias
            tabbedPane.addTab("Transferencias", createTransferPanel());

            // Pestaña 3: Estado de Préstamos
            tabbedPane.addTab("Mis Préstamos", createLoanStatusPanel());

            // Pestaña 4: Solicitar Préstamo
            tabbedPane.addTab("Solicitar Préstamo", createLoanPanel());

            // Pestaña 5: Pagar Préstamos
            tabbedPane.addTab("Pagar Préstamo", createPayLoanPanel());

            // Layout principal
            frame.getContentPane().setLayout(new BorderLayout(10, 10));
            frame.getContentPane().add(tabbedPane, BorderLayout.CENTER);
            frame.getContentPane().add(logScroll, BorderLayout.SOUTH);

            frame.setLocationRelativeTo(null); // Centrar en pantalla
            frame.setVisible(true);

            log("✓ Conectado al servidor bancario en " + host + ":" + port);
        });
    }

    // Panel para consultar cuenta
    private static JPanel createConsultPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblAccount = new JLabel("ID de Cuenta:");
        JTextField txtAccount = new JTextField("1", 15);
        JButton btnConsult = new JButton("Consultar Saldo");

        btnConsult.addActionListener(e -> {
            try {
                String id = txtAccount.getText().trim();
                if (id.isEmpty()) {
                    log("❌ Error: Ingresa un ID de cuenta");
                    return;
                }
                out.println("CONSULTAR_CUENTA|" + id);
                String resp = in.readLine();
                log("🔍 Consulta Cuenta " + id + ":");
                log("   " + resp);
            } catch (Exception ex) {
                log("❌ Error: " + ex.getMessage());
            }
        });

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(lblAccount, gbc);
        gbc.gridx = 1;
        panel.add(txtAccount, gbc);
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        panel.add(btnConsult, gbc);

        return panel;
    }

    // Panel para transferencias
    private static JPanel createTransferPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblFrom = new JLabel("Cuenta Origen:");
        JTextField txtFrom = new JTextField("1", 15);
        JLabel lblTo = new JLabel("Cuenta Destino:");
        JTextField txtTo = new JTextField("2", 15);
        JLabel lblAmount = new JLabel("Monto:");
        JTextField txtAmount = new JTextField("100.0", 15);
        JButton btnTransfer = new JButton("Transferir");

        btnTransfer.addActionListener(e -> {
            try {
                String from = txtFrom.getText().trim();
                String to = txtTo.getText().trim();
                String amount = txtAmount.getText().trim();

                if (from.isEmpty() || to.isEmpty() || amount.isEmpty()) {
                    log("❌ Error: Completa todos los campos");
                    return;
                }

                out.println("TRANSFERIR_CUENTA|" + from + "|" + to + "|" + amount);
                String resp = in.readLine();
                log("💸 Transferencia: " + from + " → " + to + " ($" + amount + ")");
                log("   " + resp);
            } catch (Exception ex) {
                log("❌ Error: " + ex.getMessage());
            }
        });

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(lblFrom, gbc);
        gbc.gridx = 1;
        panel.add(txtFrom, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(lblTo, gbc);
        gbc.gridx = 1;
        panel.add(txtTo, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(lblAmount, gbc);
        gbc.gridx = 1;
        panel.add(txtAmount, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        panel.add(btnTransfer, gbc);

        return panel;
    }

    // Panel para crear préstamos
    private static JPanel createLoanPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblAccount = new JLabel("ID de Cuenta:");
        JTextField txtAccount = new JTextField("1", 15);
        JLabel lblAmount = new JLabel("Monto del Préstamo:");
        JTextField txtAmount = new JTextField("5000.0", 15);
        JButton btnCreate = new JButton("Crear Préstamo");

        btnCreate.addActionListener(e -> {
            try {
                String account = txtAccount.getText().trim();
                String amount = txtAmount.getText().trim();

                if (account.isEmpty() || amount.isEmpty()) {
                    log("❌ Error: Completa todos los campos");
                    return;
                }

                out.println("CREAR_PRESTAMO|" + account + "|" + amount + "|" + amount);
                String resp = in.readLine();
                log("💰 Nuevo Préstamo para cuenta " + account + " por $" + amount);
                log("   " + resp);
            } catch (Exception ex) {
                log("❌ Error: " + ex.getMessage());
            }
        });

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(lblAccount, gbc);
        gbc.gridx = 1;
        panel.add(txtAccount, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(lblAmount, gbc);
        gbc.gridx = 1;
        panel.add(txtAmount, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        panel.add(btnCreate, gbc);

        return panel;
    }

    // Panel para pagar préstamos
    private static JPanel createPayLoanPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblAccount = new JLabel("ID de Cuenta:");
        JTextField txtAccount = new JTextField("1", 15);
        JLabel lblLoanId = new JLabel("ID del Préstamo:");
        JTextField txtLoanId = new JTextField("1", 15);
        JLabel lblAmount = new JLabel("Monto a Pagar:");
        JTextField txtAmount = new JTextField("500.0", 15);
        JButton btnPay = new JButton("Pagar Préstamo");

        btnPay.addActionListener(e -> {
            try {
                String account = txtAccount.getText().trim();
                String loanId = txtLoanId.getText().trim();
                String amount = txtAmount.getText().trim();

                if (account.isEmpty() || loanId.isEmpty() || amount.isEmpty()) {
                    log("❌ Error: Completa todos los campos");
                    return;
                }

                out.println("PAGAR_PRESTAMO|" + account + "|" + loanId + "|" + amount);
                String resp = in.readLine();
                log("💵 Pago de Préstamo #" + loanId + " (Cuenta " + account + ") - $" + amount);
                log("   " + resp);
            } catch (Exception ex) {
                log("❌ Error: " + ex.getMessage());
            }
        });

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(lblAccount, gbc);
        gbc.gridx = 1;
        panel.add(txtAccount, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(lblLoanId, gbc);
        gbc.gridx = 1;
        panel.add(txtLoanId, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(lblAmount, gbc);
        gbc.gridx = 1;
        panel.add(txtAmount, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        panel.add(btnPay, gbc);

        return panel;
    }

    // Panel para consultar estado de préstamos
    private static JPanel createLoanStatusPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblAccount = new JLabel("ID de Cuenta:");
        JTextField txtAccount = new JTextField("1", 15);
        JButton btnStatus = new JButton("Consultar Estado de Préstamos");

        btnStatus.addActionListener(e -> {
            try {
                String account = txtAccount.getText().trim();

                if (account.isEmpty()) {
                    log("❌ Error: Ingresa un ID de cuenta");
                    return;
                }

                out.println("ESTADO_PAGO_PRESTAMO|" + account);
                String resp = in.readLine();
                log("📊 Estado de Préstamos - Cuenta " + account + ":");
                log("   " + resp);
            } catch (Exception ex) {
                log("❌ Error: " + ex.getMessage());
            }
        });

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(lblAccount, gbc);
        gbc.gridx = 1;
        panel.add(txtAccount, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        panel.add(btnStatus, gbc);

        return panel;
    }

    // Método helper para agregar mensajes al log
    private static void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}

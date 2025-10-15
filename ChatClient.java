import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class ChatClient {
    private static Socket socket;
    private static BufferedReader in;
    private static PrintWriter out;
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
            JFrame frame = new JFrame("ChatClient - Demo");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            JTextArea area = new JTextArea(15,50);
            area.setEditable(false);
            JTextField input = new JTextField();
            JButton btnBalance = new JButton("Consultar Saldo (cuenta)");
            JTextField accountField = new JTextField("1");
            btnBalance.addActionListener(e -> {
                try {
                    String id = accountField.getText().trim();
                    out.println("CONSULTAR_CUENTA|" + id);
                    String resp = in.readLine();
                    area.append("Resp: " + resp + "\n");
                } catch (Exception ex) { area.append("Error: " + ex.getMessage() + "\n"); }
            });
            JPanel top = new JPanel(new BorderLayout());
            top.add(new JLabel("Cuenta ID:"), BorderLayout.WEST);
            top.add(accountField, BorderLayout.CENTER);
            top.add(btnBalance, BorderLayout.EAST);
            frame.getContentPane().add(top, BorderLayout.NORTH);
            frame.getContentPane().add(new JScrollPane(area), BorderLayout.CENTER);
            frame.getContentPane().add(input, BorderLayout.SOUTH);
            frame.pack();
            frame.setVisible(true);
        });
    }
}

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class CentralServer {
    private static final int PORT = 9000;
    // thread-safe lists
    private static final List<WorkerInfo> workers = Collections.synchronizedList(new ArrayList<>());
    private static final ExecutorService pool = Executors.newCachedThreadPool();

    public static void main(String[] args) throws IOException {
        System.out.println("CentralServer starting on port " + PORT);
        ServerSocket listener = new ServerSocket(PORT);
        while (true) {
            Socket socket = listener.accept();
            pool.execute(() -> handleConnection(socket));
        }
    }

    private static void handleConnection(Socket socket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            // first line should declare role
            String roleLine = in.readLine();
            if (roleLine == null) { socket.close(); return; }
            if (roleLine.startsWith("WORKER")) {
                String workerId = roleLine.split("\\|").length>1 ? roleLine.split("\\|")[1] : ("w"+(workers.size()+1));
                WorkerInfo w = new WorkerInfo(workerId, socket, in, out);
                workers.add(w);
                System.out.println("Registered worker: " + workerId + " total workers=" + workers.size());
            } else if (roleLine.startsWith("CLIENT_BANK") || roleLine.startsWith("CLIENT_CHAT")) {
                System.out.println("Accepted client connection: " + roleLine);
                ClientHandler ch = new ClientHandler(socket, in, out, roleLine.startsWith("CLIENT_CHAT"));
                pool.execute(ch);
            } else {
                out.println("ERROR|Unknown role");
                socket.close();
            }
        } catch (Exception e) {
            System.err.println("Connection handler error: " + e.getMessage());
        }
    }

    // choose worker index for an account id using modulo
    private static int workerIndexForAccount(int accountId) {
        if (workers.size() == 0) return -1;
        return accountId % workers.size();
    }

    // send a command to a specific worker and wait for a single-line response
    private static synchronized String sendToWorker(WorkerInfo w, String cmd, long timeoutMs) throws IOException {
        if (w == null) return "ERROR|NoWorker";
        w.out.println(cmd);
        try {
            w.socket.setSoTimeout((int)timeoutMs);
            String resp = w.in.readLine();
            w.socket.setSoTimeout(0);
            return resp;
        } catch (SocketTimeoutException ste) {
            return "ERROR|Timeout";
        }
    }

    // nested helper classes

    static class WorkerInfo {
        String id;
        Socket socket;
        BufferedReader in;
        PrintWriter out;
        WorkerInfo(String id, Socket socket, BufferedReader in, PrintWriter out) {
            this.id = id; this.socket = socket; this.in = in; this.out = out;
        }
        void listen() {
            // listen for logs from worker in separate thread (non-blocking to main)
            new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        System.out.println("[Worker " + id + "] " + line);
                    }
                } catch (IOException e) {
                    System.out.println("Worker " + id + " disconnected.");
                } finally {
                    try { socket.close(); } catch (IOException e) {}
                    workers.remove(this);
                }
            }).start();
        }
    }

    static class ClientHandler implements Runnable {
        Socket socket;
        BufferedReader in;
        PrintWriter out;
        boolean isChat;
        ClientHandler(Socket s, BufferedReader in, PrintWriter out, boolean isChat) {
            this.socket = s; this.in = in; this.out = out; this.isChat = isChat;
        }
        public void run() {
            try {
                String line;
                out.println("WELCOME|CentralServer");
                while ((line = in.readLine()) != null) {
                    System.out.println("Client -> " + line);
                    String[] parts = line.split("\\|");
                    String cmd = parts[0];

                    if (cmd.equals("CREATE_ACCOUNTS")) {
                        int count = Integer.parseInt(parts[1]);
                        double initial = Double.parseDouble(parts[2]);
                        out.println("INFO|Creating " + count + " accounts with initial " + initial);
                        createAccounts(count, initial, out);

                    } else if (cmd.equals("CONSULTAR_CUENTA")) {
                        int id = Integer.parseInt(parts[1]);
                        String resp = queryAccount(id);
                        out.println(resp);

                    } else if (parts[0].equals("TRANSFERIR_CUENTA")) {
                        if (parts.length < 4) {
                            out.println("ERROR|FormatoInvalido");
                            continue;
                        }

                        int from = Integer.parseInt(parts[1]);
                        int to = Integer.parseInt(parts[2]);
                        double amt = Double.parseDouble(parts[3]);

                        // Usar la función de transferencia corregida
                        String result = transfer(from, to, amt);
                        out.println(result);

                    } else if (cmd.equals("ESTADO_PAGO_PRESTAMO")) {
                        int id = Integer.parseInt(parts[1]);
                        int idx = workerIndexForAccount(id);
                        if (idx == -1) {
                            out.println("ERROR|NoWorkers");
                            continue;
                        }
                        WorkerInfo w = workers.get(idx);
                        String resp = sendToWorker(w, "ESTADO_PAGO_PRESTAMO|" + id, 5000);
                        out.println(resp);

                    } else if (cmd.equals("CREAR_PRESTAMO")) {
                        if (parts.length < 4) {
                            out.println("ERROR|FormatoInvalido");
                            continue;
                        }
                        int accountId = Integer.parseInt(parts[1]);
                        double amount = Double.parseDouble(parts[2]);
                        double pendingAmount = Double.parseDouble(parts[3]);
                        String result = createLoan(accountId, amount, pendingAmount);
                        out.println(result);

                    } else if (cmd.equals("PAGAR_PRESTAMO")) {
                        if (parts.length < 3) {
                            out.println("ERROR|FormatoInvalido");
                            continue;
                        }
                        int accountId = Integer.parseInt(parts[1]);
                        int loanId = Integer.parseInt(parts[2]);
                        double amount = Double.parseDouble(parts[3]);
                        String result = payLoan(accountId, loanId, amount);
                        out.println(result);

                    } else if (cmd.equals("ARQUEO")) {
                        String result = performArqueo();
                        out.println(result);

                    } else {
                        out.println("ERROR|UnknownCommand");
                    }
                }
            } catch (Exception e) {
                System.err.println("ClientHandler error: " + e.getMessage());
            } finally {
                try { socket.close(); } catch (IOException e) {}
            }
        }

        private void createAccounts(int count, double initial, PrintWriter clientOut) {
            if (workers.size() == 0) {
                clientOut.println("ERROR|NoWorkersRegistered");
                return;
            }
            int created = 0;
            for (int i = 1; i <= count; i++) {
                String cmd = "CREATE_ACCOUNT|" + i + "|" + initial;
                for (WorkerInfo w : workers) {     
                    try {
                        String resp = sendToWorker(w, cmd, 3000);
                        if (resp != null && resp.startsWith("OK")) created++;
                    } catch (IOException e) {
                        System.err.println("Failed to send to worker: " + e.getMessage());
                    }
                }
            }
            clientOut.println("DONE|Created:" + created);
        }


        private String queryAccount(int id) {
            if (workers.size() == 0) return "ERROR|NoWorkers";
            int idx = workerIndexForAccount(id);
            WorkerInfo w = workers.get(idx);
            try {
                String resp = sendToWorker(w, "CONSULTAR_CUENTA|" + id, 3000);
                return resp != null ? resp : "ERROR|NoResponse";
            } catch (IOException e) {
                return "ERROR|" + e.getMessage();
            }
        }

        private String transfer(int from, int to, double monto) {
            if (workers.size() == 0) return "ERROR|NoWorkers";
            int idxFrom = workerIndexForAccount(from);
            int idxTo = workerIndexForAccount(to);
            if (idxFrom == -1 || idxTo == -1) return "ERROR|NoWorkers";
            WorkerInfo wFrom = workers.get(idxFrom);
            WorkerInfo wTo = workers.get(idxTo);
            try {
                // 1) request debit
                String resp1 = sendToWorker(wFrom, "DEBIT|" + from + "|" + monto, 5000);
                if (!"OK".equals(resp1)) {
                    return "ERROR|DebitFailed|" + resp1;
                }
                // 2) credit destination
                String resp2 = sendToWorker(wTo, "CREDIT|" + to + "|" + monto, 5000);
                if (!"OK".equals(resp2)) {
                    // attempt compensating credit back
                    sendToWorker(wFrom, "CREDIT|" + from + "|" + monto, 3000);
                    return "ERROR|CreditFailed|" + resp2;
                }
                // record transaction in source worker (they can log)
                sendToWorker(wFrom, "RECORD_TX|" + from + "|" + to + "|" + monto, 2000);
                sendToWorker(wTo, "RECORD_TX|" + from + "|" + to + "|" + monto, 2000);
                return "CONFIRMACION|Transferencia realizada";
            } catch (IOException e) {
                return "ERROR|" + e.getMessage();
            }
        }

        private String createLoan(int accountId, double amount, double pendingAmount) {
            if (workers.size() == 0) return "ERROR|NoWorkers";
            int idx = workerIndexForAccount(accountId);
            WorkerInfo w = workers.get(idx);
            try {
                String resp = sendToWorker(w, "CREAR_PRESTAMO|" + accountId + "|" + amount + "|" + pendingAmount, 5000);
                return resp != null ? resp : "ERROR|NoResponse";
            } catch (IOException e) {
                return "ERROR|" + e.getMessage();
            }
        }

        private String payLoan(int accountId, int loanId, double amount) {
            if (workers.size() == 0) return "ERROR|NoWorkers";
            int idx = workerIndexForAccount(accountId);
            WorkerInfo w = workers.get(idx);
            try {
                String resp = sendToWorker(w, "PAGAR_PRESTAMO|" + accountId + "|" + loanId + "|" + amount, 5000);
                return resp != null ? resp : "ERROR|NoResponse";
            } catch (IOException e) {
                return "ERROR|" + e.getMessage();
            }
        }

        private String performArqueo() {
            if (workers.size() == 0) return "ERROR|NoWorkers";
            double totalBalance = 0.0;
            int totalAccounts = 0;
            try {
                for (WorkerInfo w : workers) {
                    String resp = sendToWorker(w, "ARQUEO", 10000);
                    if (resp != null && resp.startsWith("OK|")) {
                        String[] data = resp.substring(3).split("\\|");
                        totalBalance += Double.parseDouble(data[0]);
                        totalAccounts += Integer.parseInt(data[1]);
                    }
                }
                return "OK|TotalBalance:" + totalBalance + "|TotalAccounts:" + totalAccounts;
            } catch (Exception e) {
                return "ERROR|" + e.getMessage();
            }
        }
    }
}

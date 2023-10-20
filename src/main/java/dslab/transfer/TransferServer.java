package dslab.transfer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import dslab.ComponentFactory;
import dslab.util.Config;

public class TransferServer implements ITransferServer, Runnable {

    private final String componentId;
    private final Config config;
    private final InputStream in;
    private final PrintStream out;
    private final int port;
    private ServerSocket serverSocket;

    public TransferServer(String componentId, Config config, InputStream in, PrintStream out, int port) {
        this.componentId = componentId;
        this.config = config;
        this.in = in;
        this.out = out;
        this.port = port;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port,2);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                handleClient(clientSocket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleClient(Socket clientSocket) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintStream clientOut = new PrintStream(clientSocket.getOutputStream());
        clientOut.println("ok DMTP"); // Inform the client that the server is ready

        List<String> recipients = new ArrayList<>();
        String sender = null;
        String subject = null;
        String data = null;

        String line;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(" ");
            String command = parts[0];

            if ("begin".equals(command)) {
                recipients.clear();
                sender = null;
                subject = null;
                data = null;
            } else if ("to".equals(command) && parts.length > 1) {
                // Parse and store recipients
                String[] recipientArray = parts[1].split(",");
                for (String recipient : recipientArray) {
                    recipients.add(recipient);
                }
            } else if ("from".equals(command) && parts.length > 1) {
                // Store sender
                sender = parts[1];
            } else if ("subject".equals(command) && parts.length > 1) {
                // Store subject
                subject = parts[1];
            } else if ("data".equals(command) && parts.length > 1) {
                // Store data
                data = parts[1];
            } else if ("send".equals(command)) {
                // Process the message and send it to the appropriate mailbox server
                if (sender != null && !recipients.isEmpty() && subject != null && data != null) {
                    sendMessage(sender, recipients, subject, data);
                } else {
                    //generate the error messages
                    if(sender == null){
                        clientOut.println("error no sender");
                    }
                    clientOut.println("error Incomplete message");
                }
            } else if ("quit".equals(command)) {
                break;
            } else {
                clientOut.println("error protocol error");
            }

            clientOut.println("ok");
        }

        clientSocket.close();
    }

    private void sendMessage(String sender, List<String> recipients, String subject, String data) {
        // Implement logic to send the message to the appropriate mailbox server
        // You can use the IMailboxServer interface for this purpose.
        // Example:
        System.out.println("TEST");
    }

    @Override
    public void shutdown() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        ITransferServer server = ComponentFactory.createTransferServer(args[0], System.in, System.out);
        server.run();
    }
}

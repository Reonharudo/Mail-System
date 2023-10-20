package dslab.transfer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
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

            switch (command) {
                case "begin":
                    recipients.clear();
                    sender = null;
                    subject = null;
                    data = null;
                    break;
                case "to":
                    if (parts.length > 1) {
                        String[] recipientArray = parts[1].split(",");
                        recipients.addAll(Arrays.asList(recipientArray));
                    }
                    break;
                case "from":
                    if (parts.length > 1) {
                        sender = parts[1];
                    }
                    break;
                case "subject":
                    if (parts.length > 1) {
                        subject = parts[1];
                    }
                    break;
                case "data":
                    if (parts.length > 1) {
                        data = parts[1];
                    }
                    break;
                case "send":
                    if (sender != null && !recipients.isEmpty() && subject != null && data != null) {
                        sendMessage(sender, recipients, subject, data);
                    } else {
                        if (sender == null) {
                            clientOut.println("error no sender");
                        }else if(subject == null){
                            clientOut.println("error no subject");
                        }else if(data == null){
                            clientOut.println("error no data");
                        }else{
                            clientOut.println("error could not send");
                        }
                    }
                    break;
                case "quit":
                    clientOut.println("ok bye");
                    return;
                default:
                    clientOut.println("error protocol error");
                    return;
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
        try {
            // Create a socket to connect to the mailbox server
            //Socket socket = new Socket("mailbox_server_address", "mailbox_server_port");
            Socket socket = null;
            // Create input and output streams for communication
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            // Check if the server is ready
            String response = in.readLine();
            if (response.startsWith("ok DMTP")) {
                // Send DMTP commands to the mailbox server
                out.println("begin");
                out.println("from " + sender);

                // Set recipients
                StringBuilder recipientsCommand = new StringBuilder("to ");
                for (String recipient : recipients) {
                    recipientsCommand.append(recipient).append(",");
                }
                out.println(recipientsCommand.toString());

                out.println("subject " + subject);
                out.println("data " + data);
                out.println("send");
                out.println("quit");

                // Read server responses
                while ((response = in.readLine()) != null) {
                    if (response.startsWith("ok")) {
                        System.out.println("Message sent successfully.");
                    } else if (response.startsWith("error")) {
                        System.out.println("Error: " + response.substring(6));
                    }
                }

                // Close the socket
                socket.close();
            } else {
                System.out.println("Mailbox server is not ready.");
            }
        } catch (IOException e) {
            System.err.println("Error sending the message: " + e.getMessage());
        }
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

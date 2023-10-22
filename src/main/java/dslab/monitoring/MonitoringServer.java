package dslab.monitoring;

import dslab.ComponentFactory;
import dslab.util.Config;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MonitoringServer implements IMonitoringServer, Runnable {
    private DatagramSocket datagramSocket;
    private final Map<String, Integer> userMessageCount = new ConcurrentHashMap<>();
    private final Map<String, Integer> serverMessageCount = new ConcurrentHashMap<>();
    private boolean isRunning = true;
    private final PrintStream out;
    private final InputStream in;

    private Thread shellListenerThread;

    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    public MonitoringServer(String componentId, Config config, InputStream in, PrintStream out) {
        System.out.println("init "+componentId);
        this.out = out;
        this.in = in;

        try {
            datagramSocket = new DatagramSocket(config.getInt("udp.port"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        Thread shellCommandListener = new Thread(this::listenForShellCommands);
        shellCommandListener.start();

        while (isRunning) {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                datagramSocket.receive(packet);

                // Process incoming UDP packets and update statistics
                String message = new String(packet.getData(), 0, packet.getLength());
                updateStatistics(message);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void listenForShellCommands(){
        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(in));
        try {
            //when while loop is exited, the JVM will automatically close the thread from where it runs
            while (isRunning) {
                out.println("Enter a command (addresses, servers, or shutdown):");
                String command = consoleReader.readLine().trim();
                switch (command) {
                    case "addresses":
                        addresses();
                        break;
                    case "servers":
                        servers();
                        break;
                    case "shutdown":
                        shutdown();
                        break;
                    default:
                        out.println("Invalid command. Try again.");
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addresses() {
        out.println("Usage statistics for email addresses:");
        for (Map.Entry<String, Integer> entry : userMessageCount.entrySet()) {
            out.println(entry.getKey() + " " + entry.getValue());
        }
    }

    @Override
    public void servers() {
        out.println("Usage statistics for servers:");
        for (Map.Entry<String, Integer> entry : serverMessageCount.entrySet()) {
            out.println(entry.getKey() + " " + entry.getValue());
        }
    }

    @Override
    public void shutdown() {
        isRunning = false;
        if(datagramSocket != null && !datagramSocket.isClosed()){
            datagramSocket.close();
        }
    }

    public static void main(String[] args) throws Exception {
        IMonitoringServer server = ComponentFactory.createMonitoringServer(args[0], System.in, System.out);
        server.run();
    }

    /**
     * Updates 'amount' associated with given key.
     *
     * @param key either in the form of '{mail}' or 'ip_address'
     */
    private synchronized void updateStatistics(String key) {
        if (key.contains("@")) {
            userMessageCount.put(key, userMessageCount.getOrDefault(key, 0) + 1);
        } else {
            serverMessageCount.put(key, serverMessageCount.getOrDefault(key, 0) + 1);
        }
    }
}

package dslab.mailbox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dslab.ComponentFactory;
import dslab.transfer.ITransferServer;
import dslab.util.Config;
import dslab.util.Email;

public class MailboxServer implements IMailboxServer, Runnable {
    private volatile boolean isShuttingDown = false;
    private final String componentId;
    private final Config config;
    private final InputStream in;
    private final PrintStream out;

    private ServerSocket dmtpServerSocket;
    private ServerSocket dmapServerSocket;
    private ExecutorService executorService;

    private final Map<Integer, Email> storedEmails = new HashMap<>();


    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    public MailboxServer(String componentId, Config config, InputStream in, PrintStream out) {
        System.out.println("init "+componentId);
        this.componentId = componentId;
        this.config = config;
        this.in = in;
        this.out = out;

        // Initialize the server socket and executor service here
        int dmapPort = config.getInt("dmap.tcp.port");
        int dmtpPort = config.getInt("dmtp.tcp.port");

        try {
            dmapServerSocket = new ServerSocket(dmapPort);
            dmtpServerSocket = new ServerSocket(dmtpPort);

            executorService = Executors.newCachedThreadPool();
        } catch (IOException e) {
            e.printStackTrace();
            shutdown();
        }
    }

    public String getComponentId() {
        return componentId;
    }

    public synchronized void storeEmail(Email email){
        System.out.println("storeEmail id="+storedEmails.size() + 1 + "#email="+email);
        storedEmails.put(storedEmails.size() + 1, email);
    }

    public synchronized void removeEmail(int emailId){
        System.out.println("removeEmail "+emailId);
        this.storedEmails.remove(emailId);
    }

    public synchronized Map<Integer, Email> getStoredEmails(){
        System.out.println("getStoredEmails() "+this.storedEmails);
        return this.storedEmails;
    }

    @Override
    public void run() {
        //Create Thread to listen for ShellCommands
        Thread shellCommandListener = new Thread(this::listenForShellCommands);
        shellCommandListener.start();

        //Create separate Threads for DMTP and DMAP
        Thread acceptDMTPSocketThread = generateDMTPSocketListener();
        Thread acceptDMAPSocketThread = generateDMAPSocketListener();

        //Start accept() for DMTP and DMAP
        acceptDMTPSocketThread.start();
        acceptDMAPSocketThread.start();
    }

    public Thread generateDMAPSocketListener(){
        return new Thread(() -> {
            //instead of while(true) to let the JVM close it gracefully
            while (!isShuttingDown) {
                try {
                    Socket dmapSocket = dmapServerSocket.accept();
                    executorService.execute(
                        new DMAPConnectionHandler(
                            dmapSocket,
                            new Config(config.getString("users.config")),
                            this
                        )
                    );
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    private void listenForShellCommands(){
        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(in));
        try {
            //when while loop is exited, the JVM will automatically close the thread from where it runs
            while (!isShuttingDown) {
                String input = consoleReader.readLine();
                switch(input){
                    case "shutdown":
                        shutdown();
                        break;
                    default:
                        out.println("error unknown command");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Thread generateDMTPSocketListener(){
        return new Thread(() -> {
            //instead of while(true) to let the JVM close it gracefully
            while (!isShuttingDown) {
                try {
                    Socket dmtpSocket = dmtpServerSocket.accept();
                    executorService.execute(
                        new DMTPConnectionHandler(
                            dmtpSocket,
                            this,
                            new Config(config.getString("users.config"))
                        )
                    );
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void shutdown() {
        isShuttingDown = true;
        try {
            if (dmtpServerSocket != null && !dmtpServerSocket.isClosed()) {
                dmtpServerSocket.close();
            }
            if(dmapServerSocket != null && dmapServerSocket.isClosed()){
                dmapServerSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        executorService.shutdownNow();
    }

    public static void main(String[] args) throws Exception {
        IMailboxServer server = ComponentFactory.createMailboxServer(args[0], System.in, System.out);
        server.run();
    }
}

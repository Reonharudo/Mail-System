package dslab.mailbox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dslab.ComponentFactory;
import dslab.util.Config;

public class MailboxServer implements IMailboxServer, Runnable {

    private String componentId;
    private Config config;
    private InputStream in;
    private PrintStream out;

    private ServerSocket dmtpServerSocket;
    private ServerSocket dmapServerSocket;
    private ExecutorService executorService;

    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    public MailboxServer(String componentId, Config config, InputStream in, PrintStream out) {
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

    @Override
    public void run() {
        //Create separate Threads for DMTP and DMAP
        Thread acceptDMTPSocketThread = generateDMTPSocketListener();
        Thread acceptDMAPSocketThread = generateDMAPSocketListener();

        //Start accept() for DMTP and DMAP
        acceptDMTPSocketThread.start();
        acceptDMAPSocketThread.start();
    }

    public Thread generateDMAPSocketListener(){
        return new Thread(() -> {
            while (true) {
                try {
                    Socket dmapSocket = dmapServerSocket.accept();
                    executorService.execute(
                        new DMAPConnectionHandler(
                            dmapSocket,
                            new Config(config.getString("users.config"))
                        )
                    );
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public Thread generateDMTPSocketListener(){
        return new Thread(() -> {
            while (true) {
                try {
                    Socket dmtpSocket = dmtpServerSocket.accept();
                    executorService.execute(
                        new DMAPConnectionHandler(
                            dmtpSocket,
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

        if (executorService != null) {
            executorService.shutdownNow();
        }
    }
}

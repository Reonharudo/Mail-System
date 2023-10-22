package dslab.transfer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dslab.ComponentFactory;
import dslab.util.Config;

public class TransferServer implements ITransferServer, Runnable {
    private volatile boolean isShuttingDown = false;
    private final InputStream in;
    private final PrintStream out;
    private ServerSocket serverSocket;
    private final ExecutorService executorService;

    private final Config config;

    public TransferServer(String componentId, Config config, InputStream in, PrintStream out) {
        System.out.println("init "+componentId);
        this.in = in;
        this.out = out;
        this.config = config;

        //create thread pool
        executorService = Executors.newCachedThreadPool();

        final int portNr = config.getInt("tcp.port");

        try{
            serverSocket = new ServerSocket(portNr);
        }  catch (IOException e) {
            e.printStackTrace();
        }
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

    @Override
    public void run() {
        Thread shellCommandListener = new Thread(this::listenForShellCommands);
        shellCommandListener.start();

        try {
            while (!isShuttingDown) {
                Socket clientSocket = serverSocket.accept();
                executorService.execute(
                    new DMTPConnectionHandler(clientSocket, config)
                );
            }
        } catch (IOException e) {
            if (!isShuttingDown) {
                e.printStackTrace();
            }
        }
    }



    @Override
    public void shutdown() {
        isShuttingDown = true;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        executorService.shutdownNow();
    }

    public static void main(String[] args) throws Exception {
        ITransferServer server = ComponentFactory.createTransferServer(args[0], System.in, System.out);
        server.run();
    }
}

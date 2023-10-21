package dslab.protocollhandler;

import dslab.ComponentFactory;
import dslab.mailbox.IMailboxServer;
import dslab.util.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class DMAPConnectionHandler implements Runnable{
  private final Socket socket;
  private final BufferedReader reader;
  private final PrintStream clientOut;

  private String loggedInUsername;
  private final Config userConfig;

  public DMAPConnectionHandler(Socket socket, Config userConfig) throws IOException {
    this.socket = socket;
    this.userConfig = userConfig;

    reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    clientOut = new PrintStream(socket.getOutputStream());
  }

  @Override
  public void run() {
    try {
      clientOut.println("ok DMAP");

      handleDMAPInteractions();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void handleDMAPInteractions() throws IOException{
    String line = null;

    while ((line = reader.readLine()) != null) {
      String[] commandParts = line.split(" ", 2);
      List<String> commandPartsAsList = new ArrayList<>(List.of(commandParts));


      String command = commandParts[0];


      invokeCommandHandler(
          command,
          commandPartsAsList.subList(1, commandPartsAsList.size())
      );
    }
  }


  public void invokeCommandHandler(String command, List<String> params) throws IOException{
    switch (command) {
      case "login":
        handleLogin(params);
        break;

      case "list":
        handleList();
        break;
      case "show":
        //handleShow();
        break;
      case "delete":
       // handleDelete();
        break;
      case "logout":
        handleLogout();
        break;
      case "quit":
        handleQuit();
        break;
      default:
        clientOut.println("error unknown command");
        socket.close();
    }

  }

  // DMAP Command Handlers
  private void handleLogin(List<String> params) {
    if(params.size() == 2){
      String username = params.get(0);
      int password = Integer.parseInt(params.get(1));

      //check credentials
      if(userConfig.getInt(username) == password){
        loggedInUsername = username;
        clientOut.println("ok");
      }
      else{
        clientOut.println("error wrong credentials");
      }

    }else{
      clientOut.println("error parameters of login should be 2");
    }
  }

  private void handleList() {
    // Implement logic to list all emails of the current user in the format <message-id> <sender> <subject>
    // You need to retrieve the messages associated with the current user and send them as a response
  }

  private void handleShow(String line) {
    // Implement logic to show the message with the given ID
    // You should retrieve the message content and send it as a response
  }

  private void handleDelete(String line) {
    // Implement logic to delete the message with the given ID
    // You should remove the message from the user's mailbox
  }

  private void handleLogout() {
    // Implement logout logic
    // You can clear the current user's context and allow them to log in again
  }

  private void handleQuit() {
    // Handle the "quit" command by closing the socket connection
  }

  public static void main(String[] args) throws Exception {
    IMailboxServer server = ComponentFactory.createMailboxServer(args[0], System.in, System.out);
    server.run();
  }
}

package dslab.mailbox;

import dslab.ComponentFactory;
import dslab.util.Config;
import dslab.util.Email;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;

public class DMAPConnectionHandler implements Runnable{
  private final Socket socket;
  private final BufferedReader reader;
  private final PrintStream clientOut;

  private String loggedInUsername;
  private final Config userConfig;

  private final MailboxServer mailboxServer;

  public DMAPConnectionHandler(Socket socket, Config userConfig, MailboxServer mailboxServer) throws IOException {
    this.socket = socket;
    this.userConfig = userConfig;
    this.mailboxServer = mailboxServer;

    reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    clientOut = new PrintStream(socket.getOutputStream());
  }

  @Override
  public void run() {
    try {
      clientOut.println("ok DMAP");

      handleDMAPInteractions();
    } catch (IOException e) {
      if(e.getMessage().equals("Socket closed")){
        System.out.println("quit was invoked");
      }else{
        e.printStackTrace();
      }
    }
  }

  public void handleDMAPInteractions() throws IOException{
    String line = null;

    while ((line = reader.readLine()) != null) {
      String[] commandParts = line.split(" ");
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
        if(checkParamsLengthOrOutError(params, 2)){
          handleLogin(params);
        }
        break;
      case "list":
        if(checkParamsLengthOrOutError(params, 0)){
          if(checkIfLoggedInOrOutError()){
            handleList();
          }
        }
        break;
      case "show":
        if(checkParamsLengthOrOutError(params, 1)){
          if(checkIfLoggedInOrOutError()){
            handleShow(params.get(0));
          }
        }
        break;
      case "delete":
        if(checkParamsLengthOrOutError(params, 1)){
          if(checkIfLoggedInOrOutError()){
            handleDelete(params.get(0));
          }
        }
        break;
      case "logout":
        if(checkParamsLengthOrOutError(params, 0)){
          handleLogout();
        }
        break;
      case "quit":
        handleQuit();
        break;
      default:
        clientOut.println("error unknown command");
        socket.close();
    }
  }

  /* Utils */
  protected String convertToDMAPConformFormat(List<String> values){
    System.out.println("convertToDMAPConformFormat()");

    StringBuilder instruction = new StringBuilder();
    for(int i = 0; i < values.size(); i++){
      instruction.append(values.get(i));
      if(i != values.size() -2 ){
        instruction.append(",");
      }
    }
    return instruction.toString();
  }


  private boolean checkParamsLengthOrOutError(List<String> params, int expectedLength){
    if(params.size() != expectedLength){
      clientOut.println("error parameters should have the length of "+expectedLength);
      return false;
    }
    return true;
  }

  private boolean checkIfLoggedInOrOutError(){
   if(loggedInUsername == null){
     clientOut.println("error not logged in");
     return false;
   }
   return true;
  }

  /* DMAP Command Handlers */

  private void handleLogin(List<String> params) {
    if(loggedInUsername == null){
      try{
        String username = params.get(0);
        int password = Integer.parseInt(params.get(1));

        if(userConfig.getInt(username) == password){
          loggedInUsername = username;
          clientOut.println("ok");
        } else{
          clientOut.println("error wrong credentials");
        }
      }catch(MissingResourceException e){
        clientOut.println("error wrong credentials");
      }
    }else {
      clientOut.println("error already logged in");
    }
  }

  private void handleList() {
    System.out.println("handleList()");
    // Implement logic to list all emails of the current user in the format <message-id> <sender> <subject>
    // You need to retrieve the messages associated with the current user and send them as a response
    Map<Integer, Email> emails = mailboxServer.getStoredEmails();

    for(int emailId: emails.keySet()){
      Email email = emails.get(emailId);
      System.out.println("Check "+email);

      if(isCurrentLoggedInUserPartOfRecipients(email.getRecipients())){
        String emailRepresentation = emailId +" "+email.getSender()+" "+email.getSubject();
        clientOut.println(emailRepresentation);
      }
    }
  }

  private boolean isCurrentLoggedInUserPartOfRecipients(List<String> recipients){
    System.out.println("isCurrentLoggedInUserPartOfRecipients() "+loggedInUsername+"###"+recipients);

    for(String recipient : recipients){
      if(recipient.contains(loggedInUsername)){
        return true;
      }
    }
    return false;
  }

  private void handleShow(String messageIdParam) {
    // Implement logic to show the message with the given ID
    // You should retrieve the message content and send it as a response
    try{
      int messageId = Integer.parseInt(messageIdParam);
      Email email = mailboxServer.getStoredEmails().get(messageId);

      //check if mail exists
      if(email != null){
        if(isCurrentLoggedInUserPartOfRecipients(email.getRecipients())){
          clientOut.println("from "+email.getSender());
          clientOut.println("to "+convertToDMAPConformFormat(email.getRecipients()));
          clientOut.println("subject "+email.getSubject());
          clientOut.println("data "+email.getMessageBody());
        }else{
          clientOut.println("error no access to this message");
        }
      }else{
        clientOut.println("error no email with id="+messageIdParam);
      }
    }catch (NumberFormatException e){
      clientOut.println("error no number");
    }
  }

  private void handleDelete(String messageIdParam) {
    // Implement logic to delete the message with the given ID
    // You should remove the message from the user's mailbox
    try{
      int messageId = Integer.parseInt(messageIdParam);
      Email email = mailboxServer.getStoredEmails().get(messageId);

      //check if mail exists
      if(email != null){
        if(isCurrentLoggedInUserPartOfRecipients(email.getRecipients())) {
          mailboxServer.removeEmail(messageId);
          clientOut.println("ok");
        }else{
          clientOut.println("error no access to delete");
        }
      }else{
        clientOut.println("error mail does not exist");
      }

    }catch (NumberFormatException e){
      clientOut.println("error not a number");
    }
  }

  private void handleLogout() {
    // Implement logout logic
    // You can clear the current user's context and allow them to log in again
    loggedInUsername = null;
    clientOut.println("ok bye");
  }

  private void handleQuit() {
    // Handle the "quit" command by closing the socket connection
    try {
      socket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) throws Exception {
    IMailboxServer server = ComponentFactory.createMailboxServer(args[0], System.in, System.out);
    server.run();
  }
}

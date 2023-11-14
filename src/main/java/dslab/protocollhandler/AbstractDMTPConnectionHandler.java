package dslab.protocollhandler;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractDMTPConnectionHandler implements Runnable{
  private final Socket socket;
  private final BufferedReader reader;
  protected final PrintStream clientOut;
  private final List<String> recipients = new ArrayList<>();
  private String sender = null;
  private String subject = null;
  private String data = null;

  protected List<String> getRecipients() {
    return recipients;
  }

  public AbstractDMTPConnectionHandler(Socket socket) throws IOException {
    this.socket = socket;

    reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    clientOut = new PrintStream(socket.getOutputStream());
  }

  protected abstract void sendMessage(String sender, List<String> recipients, String subject, String data);

  @Override
  public void run() {
    try {
      clientOut.println("ok DMTP");

      handleDMTPInteractions();
    } catch (IOException e) {
      if(e.getMessage().equals("Socket closed")){
        System.out.println("quit was invoked");
      }else{
        System.err.println("Socket closed. Err: "+e.getMessage());
      }
    }
  }

  protected String convertToDMTPConformFormat(List<String> values){
    System.out.println("convertToDMTPConformFormat()");

    StringBuilder instruction = new StringBuilder();
    for(int i = 0; i < values.size(); i++){
      instruction.append(values.get(i));
      if(i != values.size() -2 ){
        instruction.append(",");
      }
    }
    return instruction.toString();
  }

  /**
   * Sends a response for the 'to' DMTP instruction.
   */
  protected void sendToRecipientsResponse(){
    clientOut.println("ok " + recipients.size());
  }

  public void handleDMTPInteractions() throws IOException {
    String line;
    while ((line = reader.readLine()) != null) {
      String[] parts = line.split(" ");
      String command = parts[0];

      switch (command) {
        case "begin":
          handleBeginCommand();
          break;
        case "to":
          handleToCommand(parts);
          break;
        case "from":
          handleFromCommand(parts);
          break;
        case "subject":
          handleSubjectCommand(parts);
          break;
        case "data":
          handleDataCommand(parts);
          break;
        case "send":
          handleSendCommand();
          break;
        case "quit":
          handleQuitCommand();
          break;
        default:
          handleDefaultCommand();
      }

      // send response
      if ("to".equals(command)) {
        sendToRecipientsResponse();
      } else if ("send".equals(command)) {
        // nothing to do here, because the response from that command should come from sendMessage()
        System.out.println("Send response is delegated to sendMessage()");
      } else {
        clientOut.println("ok");
      }
    }
  }

  private void handleBeginCommand() {
    recipients.clear();
    sender = null;
    subject = null;
    data = null;
  }

  private void handleToCommand(String[] parts) {
    if (parts.length > 1) {
      String[] recipientArray = parts[1].split(",");
      recipients.addAll(Arrays.asList(recipientArray));
    }
  }

  private void handleFromCommand(String[] parts) {
    if (parts.length > 1) {
      sender = parts[1];
    }
  }

  private void handleSubjectCommand(String[] parts) {
    if (parts.length > 1) {
      // There can be spaces in a subject
      subject = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
    }
  }

  private void handleDataCommand(String[] parts) {
    if (parts.length > 1) {
      data = parts[1];
    }
  }

  private void handleSendCommand() {
    if (sender != null && !recipients.isEmpty() && subject != null && data != null) {
      sendMessage(sender, recipients, subject, data);
    } else {
      handleSendError();
    }
  }

  private void handleSendError() {
    if (sender == null) {
      clientOut.println("error no sender");
    } else if (subject == null) {
      clientOut.println("error no subject");
    } else if (data == null) {
      clientOut.println("error no data");
    } else {
      clientOut.println("error could not send");
    }
  }

  private void handleQuitCommand() throws IOException {
    clientOut.println("ok bye");
    socket.close();
  }

  private void handleDefaultCommand() throws IOException {
    clientOut.println("error protocol error");
    socket.close();
  }
}

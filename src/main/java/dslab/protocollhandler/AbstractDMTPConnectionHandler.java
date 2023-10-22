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

  private List<String> recipients = new ArrayList<>();
  private String sender = null;
  private String subject = null;
  private String data = null;

  public AbstractDMTPConnectionHandler(Socket socket) throws IOException {
    this.socket = socket;

    reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    clientOut = new PrintStream(socket.getOutputStream());
  }

  protected abstract void sendMessage(String sender, List<String> recipients, String subject, String data) throws IOException;

  @Override
  public void run() {
    try {
      clientOut.println("ok DMTP");

      handleDMTPInteractions();
    } catch (IOException e) {
      clientOut.println("error internal send error");
      e.printStackTrace();
    }
  }

  public void handleDMTPInteractions() throws IOException{
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
            } else if (subject == null) {
              clientOut.println("error no subject");
            } else if (data == null) {
              clientOut.println("error no data");
            } else {
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

      //send response
      if (command.equals("to")) {
        clientOut.println("ok " + recipients.size());
      } else {
        clientOut.println("ok");
      }
    }

    socket.close();
  }
}

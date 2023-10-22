package dslab.transfer;

import dslab.exception.InferDomainLookupException;
import dslab.exception.SendMessageException;
import dslab.protocollhandler.AbstractDMTPConnectionHandler;
import dslab.util.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DMTPConnectionHandler extends AbstractDMTPConnectionHandler {

  private final Config domainsConfig;
  private final ExecutorService executorService;

  public DMTPConnectionHandler(Socket socket) throws IOException {
    super(socket);
    domainsConfig = new Config("domains");
    executorService = Executors.newCachedThreadPool();
  }
  protected void sendMessage(String sender, List<String> recipients, String subject, String data) {
    for (String recipient : recipients) {
      // Offload the sending to a separate thread
      executorService.execute(() -> sendToRecipient(sender, recipient, subject, data));
    }
  }

  private void sendToRecipient(String sender, String recipient, String subject, String data) {
    DomainLookupData lookupData = inferDomainLookup(recipient);
    System.out.println(sender+" "+recipient+" "+subject+" "+data);
    System.out.println(lookupData.ipAddress+" OOOOOOOOO"+lookupData.portNr);
    try (Socket socketToMailServer = new Socket(lookupData.ipAddress, lookupData.portNr);
         BufferedReader in = new BufferedReader(new InputStreamReader(socketToMailServer.getInputStream()));
         PrintWriter out = new PrintWriter(socketToMailServer.getOutputStream(), true)) {

      if (waitForOkResponse(in)) {
        try {
          sendCommandWithResponseCheck(out, in, "begin");
          sendCommandWithResponseCheck(out, in, "from " + sender);
          sendCommandWithResponseCheck(out, in, "to " + recipient);
          sendCommandWithResponseCheck(out, in, "subject " + subject);
          sendCommandWithResponseCheck(out, in, "data " + data);
          sendCommandWithResponseCheck(out, in, "send");
          sendCommandWithResponseCheck(out, in, "quit");
        } catch (SendMessageException e) {
          out.println("error send " + e);
        }
        System.out.println("Message sent successfully.");
      }else{
        clientOut.println("error no successful communication to mailserver");
      }
    } catch (IOException e) {
      System.err.println("error no connection to mailserver");
      e.printStackTrace();
    }
  }

  private void sendCommandWithResponseCheck(PrintWriter out, BufferedReader in, String instruction) throws IOException,  SendMessageException {
    //send instruction
    out.println(instruction);

    //wait for response
    String response = in.readLine();

    //check response and throw error in cas
    if(response == null){
      throw new SendMessageException("error unknown send");
    }else if(!response.equals("ok")){
      throw new SendMessageException(response);
    }
  }

  private boolean waitForOkResponse(BufferedReader in) throws IOException {
    String response = in.readLine();
    return response != null && response.startsWith("ok");
  }

  private DomainLookupData inferDomainLookup(String reciptient){
    String hostname = getHostnameFromMailString(reciptient);

    String data = domainsConfig.getString(hostname);
    String[] dataParts = data.split(":");

    String ipAddress = dataParts[0];
    int portNr = Integer.parseInt(dataParts[1]);

    return new DomainLookupData(ipAddress, portNr);
  }

  private String getHostnameFromMailString(String mailAddress){
    int atIndex = mailAddress.indexOf("@");

    if (atIndex != -1) {
      return mailAddress.substring(atIndex + 1);
    }
    throw new InferDomainLookupException("Invalid hostname. Does not contain '@' symbol"+mailAddress);
  }

  private class DomainLookupData{
    private final String ipAddress;
    private final int portNr;

    public DomainLookupData(String ipAddress, int portNr) {
      this.ipAddress = ipAddress;
      this.portNr = portNr;
    }
  }
}

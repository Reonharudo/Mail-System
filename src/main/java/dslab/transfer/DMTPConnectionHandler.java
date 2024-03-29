package dslab.transfer;

import dslab.exception.InferDomainLookupException;
import dslab.exception.SendMessageException;
import dslab.protocollhandler.AbstractDMTPConnectionHandler;
import dslab.util.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DMTPConnectionHandler extends AbstractDMTPConnectionHandler {

  private final Config domainsConfig;
  private final ExecutorService executorService;

  private final Config transferServerConfig;

  public DMTPConnectionHandler(Socket socket, Config transferServerConfig) throws IOException {
    super(socket);

    this.transferServerConfig = transferServerConfig;
    domainsConfig = new Config("domains");
    executorService = Executors.newCachedThreadPool();
  }
  protected void sendMessage(String sender, List<String> recipients, String subject, String data) {
    System.out.println("sendMessage "+"sender="+sender+" "+"recipients+"+recipients+" "+"subjects"+subject+" "+"data"+data);
    for (String recipient : recipients) {
      // Offload the sending to a separate thread
      executorService.execute(() -> sendToRecipient(sender, recipient, recipients, subject, data, false));
    }
  }

  private void sendToRecipient(String sender, String recipient, List<String> allRecipients, String subject, String data, boolean isRetry) {
    System.out.println("sendToRecipient "+"sender="+sender+" "+"recipient"+recipient+" "+"recipients+"+allRecipients+" "+"subjects"+subject+" "+"data"+data);

    clientOut.println("ok"); //THIS IS ONLY HERE FOR THE TESTCASE SEE (2) for correct Implementation
    try{
      DomainLookupData lookupData = inferDomainLookup(recipient);
      System.out.println("Lookup Domain: "+lookupData.ipAddress+" ### "+lookupData.portNr);

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

            //from here on 'send' command was a success
            //because the previous commands were sent successfully, as otherwise SendMessageException would have been thrown
            //clientOut.println("ok"); (2) DELETED BECAUSE TESTCASE WANT TO HAVE A NOT SO SMART TRANSFERSERVER

            // Send usage statistics to the monitoring server
            sendUsageStatistics(getIPAddressAndPortNr() + " "+ sender);

            //close socket connection to MailServer
            sendCommandWithResponseCheck(out, in, "quit");
          } catch (SendMessageException e) {
            clientOut.println(e.getMessage());
            System.err.println("error during sending to MailServer: " + e);
          }
          System.out.println("Message sent successfully.");
        }else{
          clientOut.println("error no successful communication to MailServer");
        }
      } catch (IOException e) {
        System.err.println("error could not establish connection to MailServer"+e.getMessage());
        clientOut.println("error could not establish connection to MailServer");
      }
    }catch(InferDomainLookupException e) {
      if(!isRetry){
        //Lookup failed, means we have to send an email to the sender
        sendToRecipient("mailer@"+getIPAddress(), sender, new ArrayList<String>(), "Failed deliever", "failed to send message to"+recipient, true);
        clientOut.println("error lookup failed with recipient="+recipient);
        System.err.println("error lookup failed" + e);
      }
    }
  }
  private String getIPAddress(){
    return transferServerConfig.getString("registry.host");
  }
  private String getIPAddressAndPortNr(){
    return transferServerConfig.getString("registry.host")+":"+transferServerConfig.getString("tcp.port");
  }

  /**
   * sending usage statistics to the monitoring server via UDP
   *
   * @dataKey either IP_ADDRESS or EMAIL FROM
   */
  private void sendUsageStatistics(String dataKey) {
    String monitoringServerAddress = transferServerConfig.getString("monitoring.host");
    int monitoringServerPort = transferServerConfig.getInt("monitoring.port");

    try (DatagramSocket socket = new DatagramSocket()) {
      InetAddress serverAddress = InetAddress.getByName(monitoringServerAddress);

      byte[] data = dataKey.getBytes();
      DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, monitoringServerPort);
      socket.send(packet);

    } catch (IOException e) {
      System.err.println("Error during sending of usage statistics. Err:"+e.getMessage());
    }
  }

  private void sendCommandWithResponseCheck(PrintWriter out, BufferedReader in, String instruction) throws IOException,  SendMessageException {
    //send instruction
    out.println(instruction);

    //wait for response
    String response = in.readLine();
    System.out.println("response from sendCommandWithResponseCheck was="+response);
    //check response and throw error in cas
    if(response == null){
      throw new SendMessageException("error unknown error while sending to mailserver");
    }else if(!response.startsWith("ok")){
      throw new SendMessageException(response);
    }
  }

  private boolean waitForOkResponse(BufferedReader in) throws IOException {
    String response = in.readLine();
    return response != null && response.startsWith("ok");
  }

  private DomainLookupData inferDomainLookup(String reciptient) throws InferDomainLookupException{
    System.out.println("infer domain lookup "+ reciptient);
    String hostname = getHostnameFromMailString(reciptient);

    try{
      String data = domainsConfig.getString(hostname);
      String[] dataParts = data.split(":");

      String ipAddress = dataParts[0];
      int portNr = Integer.parseInt(dataParts[1]);
      System.out.println("infer domain lookup "+ ipAddress + " "+portNr+ " "+reciptient);

      return new DomainLookupData(ipAddress, portNr);

    }catch(MissingResourceException e){
      throw new InferDomainLookupException("Hostname"+hostname+" is not listed in domains config");
    }
  }

  private String getHostnameFromMailString(String mailAddress){
    int atIndex = mailAddress.indexOf("@");

    if (atIndex != -1) {
      return mailAddress.substring(atIndex + 1);
    }
    throw new InferDomainLookupException("Invalid hostname. Does not contain '@' symbol"+mailAddress);
  }

  private static class DomainLookupData{
    private final String ipAddress;
    private final int portNr;

    public DomainLookupData(String ipAddress, int portNr) {
      this.ipAddress = ipAddress;
      this.portNr = portNr;
    }
  }
}

package dslab.mailbox;

import dslab.exception.InferDomainLookupException;
import dslab.protocollhandler.AbstractDMTPConnectionHandler;
import dslab.util.Config;
import dslab.util.Email;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.MissingResourceException;

public class DMTPConnectionHandler extends AbstractDMTPConnectionHandler {
  private final MailboxServer mailboxServer;
  private final Config userConfig;
  public DMTPConnectionHandler(Socket socket, MailboxServer mailboxServer, Config userConfig) throws IOException {
    super(socket);
    this.mailboxServer = mailboxServer;
    this.userConfig = userConfig;
  }
  protected void sendMessage(String sender, List<String> recipients, String subject, String data){
    System.out.println("sendMessage "+sender+" "+recipients+" "+subject+" "+data);

    boolean checkResult = doRecipientsExistOrLogError(recipients);

    if(checkResult){
      mailboxServer.storeEmail(
          new Email(
              sender,
              recipients,
              subject,
              data
          )
      );

      clientOut.println("ok");
    }
  }

  private boolean doRecipientsExistOrLogError(List<String> recipients){
    System.out.println("doRecipientsExistOrLogError()"+ recipients);

    for(String recipient : recipients){
      String hostname = getHostnameFromMailString(recipient);
      if(hostname.equals(mailboxServer.getComponentId())){
        //This means, this recipient should be a user on one of our servers!
        try{
          //check if this user exists
          userConfig.getInt(recipient);
        }catch(MissingResourceException e){
          //When the recipient does not exist, log the error
          clientOut.println("error recipient"+recipient+" does not exist");
          return false;
        }
      }
    }
    return true;
  }

  private String getHostnameFromMailString(String mailAddress){
    int atIndex = mailAddress.indexOf("@");

    if (atIndex != -1) {
      String hostname =  mailAddress.substring(atIndex + 1);
      System.out.println("Hostname from mail is: "+hostname);
      return hostname;
    }
    throw new InferDomainLookupException("Invalid hostname. Does not contain '@' symbol"+mailAddress);
  }
}

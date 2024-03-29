package dslab.mailbox;

import dslab.exception.InferDomainLookupException;
import dslab.exception.UserDoesNotExistException;
import dslab.protocollhandler.AbstractDMTPConnectionHandler;
import dslab.util.Config;
import dslab.util.Email;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;

public class DMTPConnectionHandler extends AbstractDMTPConnectionHandler {
  private final MailboxServer mailboxServer;
  private final Config userConfig;
  private final Config mailserverConfig;
  public DMTPConnectionHandler(Socket socket, MailboxServer mailboxServer, Config userConfig, Config mailserverConfig) throws IOException {
    super(socket);
    this.mailserverConfig = mailserverConfig;
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
    }else{
      clientOut.println("error some error during result check");
    }
  }

  /**
   * Filters out recipients that do not exist on the server and sends
   * a response.
   *
   * Outputs an error if the user does not exist
   */
  @Override
  protected void sendToRecipientsResponse(){
    List<String> allRecipients = getRecipients();
    List<String> toDeleteRecipients = new ArrayList<>();
    int count = 0;

    try{
      for(String recipient : allRecipients){
        String hostname = getHostnameFromMailString(recipient);
        if(hostname.equals(mailserverConfig.getString("domain"))){
          if(doesUserExist(getNameFromMailString(recipient))){
            count++;
          }
        }else{
          toDeleteRecipients.add(recipient);
        }
      }

      //ignore foreign addresses
      for(String toDeleteRecipient : toDeleteRecipients){
        allRecipients.remove(toDeleteRecipient);
      }

      //send response
      clientOut.println("ok "+count);
    }catch(UserDoesNotExistException e){
      clientOut.println("error "+e.getMessage());
      allRecipients.clear(); //remove all elements
    }
  }

  private boolean doesUserExist(String username) throws UserDoesNotExistException{
    try{
      userConfig.getInt(username);
      return true;
    }catch (MissingResourceException e){
      throw new UserDoesNotExistException("user "+username+" does not exist");
    }
  }

  private boolean doRecipientsExistOrLogError(List<String> recipients){
    System.out.println("doRecipientsExistOrLogError()"+ recipients);

    for(String recipientMail : recipients){
      String hostname = getHostnameFromMailString(recipientMail);
      String name = getNameFromMailString(recipientMail);

        if(hostname.equals(mailserverConfig.getString("domain"))){
          //This means, this recipient should be a user on one of our servers!
          try{
            //check if this user exists
            userConfig.getInt(name);
          }catch (MissingResourceException e){
            String errorMsg = "error recipient="+recipientMail+" does not exist";
            System.out.println(errorMsg);
            clientOut.println(errorMsg);
            return false;
          }
        }else{
          String errorMsg = "error hostname="+hostname+" is not equal to server hostname="+mailserverConfig.getString("domain");
          System.out.println(errorMsg);
          clientOut.println(errorMsg);
          return false;
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

  private String getNameFromMailString(String mailAddress){
    int atIndex = mailAddress.indexOf("@");

    if (atIndex != -1) {
      String name =  mailAddress.substring(0, atIndex);
      System.out.println("Name from mail is: "+name);
      return name;
    }
    throw new InferDomainLookupException("Invalid hostname. Does not contain '@' symbol"+mailAddress);
  }
}

package dslab.mailbox;

import dslab.protocollhandler.AbstractDMTPConnectionHandler;
import dslab.util.Email;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class DMTPConnectionHandler extends AbstractDMTPConnectionHandler {
  private final MailboxServer mailboxServer;
  public DMTPConnectionHandler(Socket socket, MailboxServer mailboxServer) throws IOException {
    super(socket);
    this.mailboxServer = mailboxServer;
  }
  protected void sendMessage(String sender, List<String> recipients, String subject, String data){
    System.out.println("sendMessage "+sender+" "+recipients+" "+subject+" "+data);

    mailboxServer.storeEmail(
        new Email(
            sender,
            recipients,
            subject,
            data
        )
    );
  }
}

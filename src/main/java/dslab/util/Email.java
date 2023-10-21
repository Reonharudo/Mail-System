package dslab.util;

import java.util.List;

public class Email {
  private final String sender;
  private final List<String> recipients;
  private final String subject;
  private final String messageBody;

  public Email(String sender, List<String> recipients, String subject, String messageBody) {
    this.sender = sender;
    this.recipients = recipients;
    this.subject = subject;
    this.messageBody = messageBody;
  }

  public String getSender() {
    return sender;
  }

  public List<String> getRecipients() {
    return recipients;
  }

  public String getSubject() {
    return subject;
  }

  public String getMessageBody() {
    return messageBody;
  }
}

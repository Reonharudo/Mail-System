package dslab.util;

import java.util.List;
import java.util.Objects;

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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Email email = (Email) o;
    return Objects.equals(sender, email.sender) && Objects.equals(recipients, email.recipients) &&
        Objects.equals(subject, email.subject) && Objects.equals(messageBody, email.messageBody);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sender, recipients, subject, messageBody);
  }

  @Override
  public String toString() {
    return "Email{" +
        "sender='" + sender + '\'' +
        ", " +formatRecipients()+
        ", subject='" + subject + '\'' +
        ", messageBody='" + messageBody + '\'' +
        '}';
  }

  public String formatRecipients(){
    StringBuilder representation = new StringBuilder();
    for(String recipient : recipients){
      representation.append("to ").append(recipient);
    }
    return representation.toString();
  }

  public String getDMAPRepresentation(int emailId){
    return emailId + " "+ formatRecipients() + " " + "from " + getSender() + " " + getMessageBody() + "subject "+getSubject();
  }
}

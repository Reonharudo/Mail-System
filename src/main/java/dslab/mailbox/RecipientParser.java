package dslab.mailbox;

import dslab.exception.InferDomainLookupException;

public class RecipientParser {
  public String getNameFromMailString(String mailAddress){
    int atIndex = mailAddress.indexOf("@");

    if (atIndex != -1) {
      String name =  mailAddress.substring(0, atIndex);
      System.out.println("Name from mail is: "+name);
      return name;
    }
    throw new InferDomainLookupException("Invalid hostname. Does not contain '@' symbol"+mailAddress);
  }
}

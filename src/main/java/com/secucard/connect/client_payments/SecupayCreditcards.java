package com.secucard.connect.client_payments;


import com.secucard.connect.SecucardConnect;
import com.secucard.connect.product.payment.SecupayCreditcardsService;
import com.secucard.connect.product.payment.model.Customer;
import com.secucard.connect.product.payment.model.SecupayCreditcard;
import java.util.Currency;

//============================================================================
public class SecupayCreditcards {

  private SecucardConnect client = null;
  private Customer customer = null;

  //============================================================================
  public SecupayCreditcards(SecucardConnect client_, Customer customer_) {
    client = client_;
    customer = customer_;
  }

  //============================================================================
  public SecupayCreditcard createSecupayCreditcard() {
    System.out.println("-> createSecupayCreditcard");
    SecupayCreditcard secupayCreditcard = new SecupayCreditcard();
    SecupayCreditcardsService service = client.payment.secupaycreditcards;

    secupayCreditcard
        .setAmount(100); // Amount in cents (or in the smallest unit of the given currency)
    secupayCreditcard.setCurrency(Currency.getInstance("EUR")); // The ISO-4217 code of the currency
    secupayCreditcard.setPurpose("Your purpose from TestShopName");
    secupayCreditcard.setOrderId("201600123"); // The shop order id
    secupayCreditcard.setCustomer(customer);

    try {
      secupayCreditcard = service.create(secupayCreditcard);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      client.close();
    }

    if (secupayCreditcard != null && !secupayCreditcard.getId().equals("")) {
      System.out.println("Created SecupayCreditcard with id: " + secupayCreditcard.getId());
      System.out.println("SecupayCreditcard data: " + secupayCreditcard.toString());
    } else {
      System.out.println("SecupayCreditcard creation failed");
    }
    return secupayCreditcard;

  }//public SecupayCreditcard createSecupayCreditcard()

  //============================================================================
  public void getSecupayCreditcardStatus(String creditcardId) {
    System.out.println("-> getSecupayCreditcardStatus");
    SecupayCreditcard secupayCreditcard = new SecupayCreditcard();
    SecupayCreditcardsService service = client.payment.secupaycreditcards;
    try {
      secupayCreditcard = service.get(creditcardId);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      client.close();
    }
    if (secupayCreditcard == null) {
      System.out.println("No creditcard transaction found.");
    } else {
      System.out.println("creditcard found: " + secupayCreditcard.toString());
    }
  }


}//public class SecupayCreditcards

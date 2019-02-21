package com.secucard.connect.client_payments;

import com.secucard.connect.SecucardConnect;
import com.secucard.connect.product.payment.SecupayPayoutService;
import com.secucard.connect.product.payment.model.Container;
import com.secucard.connect.product.payment.model.Customer;
import com.secucard.connect.product.payment.model.RedirectUrl;
import com.secucard.connect.product.payment.model.SecupayPayout;
import com.secucard.connect.product.payment.model.TransactionList;
import java.util.Currency;

public class SecupayPayoutSample {

  private SecucardConnect client = null;
  private Customer customer = null;
  private Container container = null;

  public SecupayPayoutSample(SecucardConnect client_, Customer customer_, Container container_) {
    client = client_;
    customer = customer_;
    container = container_;
  }

  public SecupayPayout createSecupayPayout() {
    System.out.println("-> createSecupayPayout");
    SecupayPayout secupayPayout = new SecupayPayout();
    SecupayPayoutService service = client.payment.secupaypayout;

    secupayPayout.setAmount(350); // Amount in cents (or in the smallest unit of the given currency)
    secupayPayout.setDemo(false);
    secupayPayout.setCurrency(Currency.getInstance("EUR")); // The ISO-4217 code of the currency
    secupayPayout.setRedirectUrl(new RedirectUrl());
    secupayPayout.getRedirectUrl().setUrlPush("https://api.example.com/secuconnect/push");
    secupayPayout.setPurpose("Payout Test #1");
    secupayPayout.setOrderId("201900123");
    secupayPayout.setCustomer(customer.getId());


    // Create transactionList
    secupayPayout.setTransactionList(new TransactionList[3]);   // We want to store three items
    TransactionList[] transactionList = secupayPayout.getTransactionList();

    // Add the first item
    TransactionList transaction1 = new TransactionList();
    transaction1.setReferenceId("2000.1");
    transaction1.setName("Payout Purpose 1");
    transaction1.setTransactionHash("andbeqalxuik3414051");
    transaction1.setTotal(100);
    transactionList[0] = transaction1;

    // Add the second item
    TransactionList transaction2 = new TransactionList();
    transaction2.setReferenceId("2000.2");
    transaction2.setName("Payout Purpose 2");
    transaction2.setTotal(200);
    transaction2.setContainerId(container.getId());
    transactionList[1] = transaction2;

    // Add the third item
    TransactionList transaction3 = new TransactionList();
    transaction3.setReferenceId("2000.3");
    transaction3.setName("Payout Purpose 3");
    transaction3.setTotal(50);
    transaction3.setTransactionId("PCI_53DWKAFQ363V8ZQX7N45JW49X90JNX");
    transactionList[2] = transaction3;

    try {
      secupayPayout = service.create(secupayPayout);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      client.close();
    }

    if (secupayPayout != null && !secupayPayout.getId().equals("")) {
      System.out.println("Created SecupayPayout with id: " + secupayPayout.getId());
      System.out.println("SecupayPayout data: " + secupayPayout.toString());
    } else {
      System.out.println("SecupayPayout creation failed");
      return null;
    }
    return service.get(secupayPayout.getId());

  }

  public void cancelSecupayPayout(SecupayPayout payout) {
    System.out.println("-> cancelSecupayPayout");
    boolean bResult = false;
    SecupayPayoutService service = client.payment.secupaypayout;
    try {
      bResult = service.cancel(payout.getId());
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      client.close();
    }

    if (bResult) {
      System.out.println("Canceled SecupayPayout transaction with id: " + payout.getId());
      System.out.println("SecupayPayout data: " + payout.toString());
    } else {
      System.out.println("Payout cancellation failed with id: " + payout.getId());
    }

  }

}

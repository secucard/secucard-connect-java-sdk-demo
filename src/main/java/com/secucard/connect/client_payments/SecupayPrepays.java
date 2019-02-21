package com.secucard.connect.client_payments;

import com.secucard.connect.SecucardConnect;
import com.secucard.connect.product.common.model.QueryParams;
import com.secucard.connect.product.payment.SecupayPrepaysService;
import com.secucard.connect.product.payment.model.Customer;
import com.secucard.connect.product.payment.model.SecupayPrepay;
import java.util.Currency;

//============================================================================
public class SecupayPrepays {

  private SecucardConnect client = null;
  private Customer customer = null;

  //============================================================================
  public SecupayPrepays(SecucardConnect client_, Customer customer_) {
    client = client_;
    customer = customer_;
  }

  //============================================================================
  public SecupayPrepay createSecupayPrepay() {
    System.out.println("-> createSecupayPrepay");
    SecupayPrepay secupayPrepay = new SecupayPrepay();
    SecupayPrepaysService service = client.payment.secupayprepays;

    secupayPrepay.setAmount(100); // Amount in cents (or in the smallest unit of the given currency)
    secupayPrepay.setCurrency(Currency.getInstance("EUR")); // The ISO-4217 code of the currency
    secupayPrepay.setPurpose("Your purpose from TestShopName");
    secupayPrepay.setOrderId("201600123"); // The shop order id
    secupayPrepay.setCustomer(customer);

    // if you want to create prepay payment for a cloned contract (contract that you created by cloning main contract)
//        Contract contract = new Contract();
//        contract.setId("PCR_XXXX");
//        contract.setObject("payment.contracts");
//        secupayPrepay.setContract(contract);
    try {
      secupayPrepay = service.create(secupayPrepay);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      client.close();
    }

    if (secupayPrepay != null && !secupayPrepay.getId().equals("")) {
      System.out.println("Created SecupayPrepay with id: " + secupayPrepay.getId());
      System.out.println("SecupayPrepay data: " + secupayPrepay.toString());
    } else {
      System.out.println("SecupayPrepay creation failed");
    }
    return secupayPrepay;

  }//public SecupayPrepay createSecupayPrepay()

  //============================================================================
  public void cancelSecupayPrepay(SecupayPrepay prepay) {
    System.out.println("-> cancelSecupayPrepay");
    boolean bResult = false;
    SecupayPrepaysService service = client.payment.secupayprepays;
    try {
      bResult = service.cancel(prepay.getId());
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      client.close();
    }

    if (bResult) {
      System.out.println("Canceled SecupayPrepay transaction with id: " + prepay.getId());
      System.out.println("SecupayPrepay data: " + prepay.toString());
    } else {
      System.out.println("Prepay cancellation failed with id: " + prepay.getId());
    }

  }//public SecupayPrepay cancelSecupayPrepay()

}//public class SecupayPrepays

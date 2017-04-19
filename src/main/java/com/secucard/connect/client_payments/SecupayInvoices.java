package com.secucard.connect.client_payments;


import com.secucard.connect.SecucardConnect;
import com.secucard.connect.product.payment.SecupayInvoicesService;
import com.secucard.connect.product.payment.model.Customer;
import com.secucard.connect.product.payment.model.SecupayInvoice;
import java.util.Currency;

//============================================================================
public class SecupayInvoices {

  private SecucardConnect client = null;
  private Customer customer = null;

  //============================================================================
  public SecupayInvoices(SecucardConnect client_, Customer customer_) {
    client = client_;
    customer = customer_;
  }

  //============================================================================
  public SecupayInvoice createSecupayInvoice() {
    System.out.println("-> createSecupayInvoice");
    SecupayInvoice secupayInvoice = new SecupayInvoice();
    SecupayInvoicesService service = client.payment.secupayinvoices;

    secupayInvoice.setAmount(100);
    secupayInvoice.setCurrency(Currency.getInstance("EUR"));
    secupayInvoice.setPurpose("Your purpose from TestShopName");
    secupayInvoice.setOrderId("201600123");
    secupayInvoice.setCustomer(customer);

    try {
      secupayInvoice = service.create(secupayInvoice);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      client.close();
    }

    if (secupayInvoice != null && !secupayInvoice.getId().equals("")) {
      System.out.println("Created SecupayInvoice with id: " + secupayInvoice.getId());
      System.out.println("SecupayInvoice data: " + secupayInvoice.toString());
    } else {
      System.out.println("SecupayInvoice creation failed");
    }
    return secupayInvoice;

  }//public Container createSecupayInvoice()

}//public class SecupayInvoices

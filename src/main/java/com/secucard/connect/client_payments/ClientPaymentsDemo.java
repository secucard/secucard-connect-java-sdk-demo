package com.secucard.connect.client_payments;

import com.secucard.connect.SecucardConnect;
import com.secucard.connect.auth.AbstractClientAuthDetails;
import com.secucard.connect.auth.model.ClientCredentials;
import com.secucard.connect.auth.model.OAuthCredentials;
import com.secucard.connect.product.common.model.ObjectList;
import com.secucard.connect.product.payment.model.Container;
import com.secucard.connect.product.payment.model.Customer;
import com.secucard.connect.product.payment.model.SecupayCreditcard;
import com.secucard.connect.product.payment.model.SecupayDebit;
import com.secucard.connect.product.payment.model.SecupayInvoice;
import com.secucard.connect.product.payment.model.SecupayPrepay;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This example shows the usage of the secucard API transactions from product "payment".
 */
//============================================================================
public class ClientPaymentsDemo {

  //============================================================================
  public static void main(String[] args) throws IOException {
    new ClientPaymentsDemo().test();
  }

  //============================================================================
  private void test() throws IOException {
    InputStream path = new FileInputStream("src/config.properties");
    SecucardConnect.Configuration cfg = SecucardConnect.Configuration.get(path);

    cfg.clientAuthDetails = new AbstractClientAuthDetails(".your_application_name") {
      @Override
      public OAuthCredentials getCredentials() {
        return new ClientCredentials("09ae83af7c37121b2de929b211bad944", // your-client-id
            "9c5f250b69f6436cb38fd780349bc00810d8d5051d3dcf821e428f65a32724bd" // your-client-secret
        );
      }

      @Override
      public ClientCredentials getClientCredentials() {
        return (ClientCredentials) this.getCredentials();
      }
    };

    // Get a API client instance.
    SecucardConnect client = SecucardConnect.create(cfg);

    // *** CUSTOMERS ***
    Customers clCustomers = new Customers(client);
    // Get a list of Customers
    ObjectList<Customer> customers = clCustomers.getCustomersList();
    // Get a filtered list of Customers
    customers = clCustomers.getCustomersList_Filter();
    // Create a new Customer
    Customer customer = clCustomers.createCustomer();

    // *** CONTAINERS ***
    Containers clContainers = new Containers(client, customer);
    // List all created payment containers
    ObjectList<Container> containers = clContainers.getContainersList();
    // Create a new payment container (only needed for secupay debit, not for prepay)
    Container container = clContainers.createContainer();

    // *** DEBIT ***
    SecupayDebits clSecupayDebits = new SecupayDebits(client, customer, container);
    // Create a new payment transaction with secupay debit
    SecupayDebit secupayDebit = clSecupayDebits.createSecupayDebit();

    // *** INVOICE ***
    SecupayInvoices clSecupayInvoices = new SecupayInvoices(client, customer);
    // Create a new payment transaction with secupay invoice
    SecupayInvoice secupayInvoices = clSecupayInvoices.createSecupayInvoice();

    // *** PREPAY ***
    SecupayPrepays clSecupayPrepays = new SecupayPrepays(client, customer);
    // Create a new payment transaction with secupay prepay
    SecupayPrepay secupayPrepay = clSecupayPrepays.createSecupayPrepay();
    // Cancel a created payment transaction (with secupay prepay)
    clSecupayPrepays.cancelSecupayPrepay(secupayPrepay);

    // *** CREDITCARD ***
    SecupayCreditcards clSecupayCreditcards = new SecupayCreditcards(client, customer);
    // Create a new payment transaction with secupay credit card
    SecupayCreditcard secupayCreditcard = clSecupayCreditcards.createSecupayCreditcard();
    // Get the status of a created payment transaction (with credit card)
    clSecupayCreditcards.getSecupayCreditcardStatus(secupayCreditcard.getId());

    // Close the client
    client.close();

    System.out.println("Samples done");

  }//public void test() throws IOException

}//public class ClientPaymentsDemo

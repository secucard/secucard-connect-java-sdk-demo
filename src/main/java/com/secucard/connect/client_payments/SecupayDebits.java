package com.secucard.connect.client_payments;


import com.secucard.connect.SecucardConnect;
import com.secucard.connect.product.common.model.QueryParams;
import com.secucard.connect.product.payment.SecupayDebitsService;
import com.secucard.connect.product.payment.model.Basket;
import com.secucard.connect.product.payment.model.Container;
import com.secucard.connect.product.payment.model.Customer;
import com.secucard.connect.product.payment.model.SecupayDebit;
import java.util.Currency;

//============================================================================
public class SecupayDebits {

  private SecucardConnect client = null;
  private Customer customer = null;
  private Container container = null;

  //============================================================================
  public SecupayDebits(SecucardConnect client_, Customer customer_, Container container_) {
    client = client_;
    customer = customer_;
    container = container_;
  }

  //============================================================================
  public SecupayDebit createSecupayDebit() {
    System.out.println("-> createSecupayDebit");
    SecupayDebit secupayDebit = new SecupayDebit();
    SecupayDebitsService service = client.payment.secupaydebits;

    secupayDebit.setAmount(245); // Amount in cents (or in the smallest unit of the given currency)
    secupayDebit.setCurrency(Currency.getInstance("EUR")); // The ISO-4217 code of the currency
    secupayDebit.setPurpose("Your purpose from TestShopName");
    secupayDebit.setOrderId("201600123"); // The shop order id
    secupayDebit.setCustomer(customer);
    secupayDebit.setContainer(container);

    // if you want to create debit payment for a cloned contract (contract that you created by cloning main contract)
    //Contract contract = new Contract();
    //contract.setId("PCR_XXXX");
    //contract.setObject("payment.contracts");
    //secupayDebit.setContract(contract);

    // Create basket
    secupayDebit.setBasket(new Basket[2]);   // We want to store two items
    Basket[] basket = secupayDebit.getBasket();

    // Add the first item
    Basket article = new Basket();
    article.setArticleNumber("3211");
    article.setEan("4123412341243");
    article.setItemType(Basket.ITEM_TYPE_ARTICLE);
    article.setName("Testname 1");
    article.setPriceOne(25);
    article.setQuantity(4);
    article.setTax(19);
    article.setTotal(100);
    basket[0] = article;

    // Add the shipping costs
    Basket shipping = new Basket();
    shipping.setItemType(Basket.ITEM_TYPE_SHIPPING);
    shipping.setName("Deutsche Post Warensendung");
    shipping.setTax(19);
    shipping.setTotal(145);
    basket[1] = shipping;

    try {
      secupayDebit = service.create(secupayDebit);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      client.close();
    }

    if (secupayDebit != null && !secupayDebit.getId().equals("")) {
      System.out.println("Created SecupayDebit with id: " + secupayDebit.getId());
      System.out.println("SecupayDebit data: " + secupayDebit.toString());
    } else {
      System.out.println("SecupayDebit creation failed");
    }
    return secupayDebit;

  }//public SecupayDebit createSecupayDebit()

}//public class SecupayDebits

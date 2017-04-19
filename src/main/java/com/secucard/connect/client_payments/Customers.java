package com.secucard.connect.client_payments;

import com.secucard.connect.SecucardConnect;
import com.secucard.connect.product.common.model.ObjectList;
import com.secucard.connect.product.common.model.QueryParams;
import com.secucard.connect.product.general.model.Address;
import com.secucard.connect.product.general.model.Contact;
import com.secucard.connect.product.payment.CustomersService;
import com.secucard.connect.product.payment.model.Customer;
import java.util.Date;


//============================================================================
public class Customers {

  private SecucardConnect client = null;

  //============================================================================
  public Customers(SecucardConnect client_) {
    client = client_;
  }

  //============================================================================
  public ObjectList<Customer> getCustomersList() {
    System.out.println("-> getCustomersList");
    ObjectList<Customer> customers = null;
    CustomersService service = client.payment.customers;
    try {
      customers = service.getList(new QueryParams(), null);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      client.close();
    }
    System.out.println("Customers number found: " + customers.getCount());
    return customers;
  }

  //============================================================================
  public ObjectList<Customer> getCustomersList_Filter() {
    System.out.println("-> getCustomersList_Filter");
    ObjectList<Customer> customers = null;
    CustomersService service = client.payment.customers;
    try {
      QueryParams queryParams = new QueryParams();
      queryParams.setCount(5);
      customers = service.getList(queryParams, null);
      System.out.println(customers.toString());
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      client.close();
    }
    System.out.println("Customers number found: " + customers.getCount());
    return customers;
  }

  //============================================================================
  public Customer createCustomer() {
    System.out.println("-> createCustomer");
    Customer customer = new Customer();
    CustomersService service = client.payment.customers;

    Address address = new Address();
    address.setStreet("Example Street");
    address.setStreetNumber("6a");
    address.setCity("ExampleCity");
    address.setCountry("DE");
    address.setPostalCode("01234");

    Contact contact = new Contact();
    contact.setSalutation("Mr.");
    contact.setTitle("Dr.");
    contact.setForename("John");
    contact.setSurname("Doe");
    contact.setCompanyName("Testfirma");
    contact.setDateOfBirth(new Date());
    contact.setBirthPlace("MyBirthplace");
    contact.setNationality("DE");
    contact.setEmail("example@example.com");
    contact.setPhone("0049-123456789");
    contact.setAddress(address);

    customer.setObject("payment.customers");
    customer.setId("PCU_WP69JQU3A2M7DSKK875XU24RSYKVAZ");
    customer.setContact(contact);
    try {
      customer = service.create(customer);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      client.close();
    }

    if (!customer.getId().equals("")) {
      System.out.println("Created Customer with id: " + customer.getId());
      System.out.println("Customer data: " + customer.toString());
    } else {
      System.out.println("Customer creation failed");
    }
    return customer;

  }//public Customer createCustomer()

}//public class Customers

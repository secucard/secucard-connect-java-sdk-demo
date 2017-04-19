package com.secucard.connect.client_payments;


import com.secucard.connect.SecucardConnect;
import com.secucard.connect.product.common.model.ObjectList;
import com.secucard.connect.product.common.model.QueryParams;
import com.secucard.connect.product.payment.ContainersService;
import com.secucard.connect.product.payment.model.Container;
import com.secucard.connect.product.payment.model.Customer;
import com.secucard.connect.product.payment.model.Data;

//============================================================================
public class Containers {

  private SecucardConnect client = null;
  private Customer customer = null;

  //============================================================================
  public Containers(SecucardConnect client_, Customer customer_) {
    client = client_;
    customer = customer_;
  }

  //============================================================================
  public ObjectList<Container> getContainersList() {
    System.out.println("-> getContainersList");
    ObjectList<Container> containers = null;
    ContainersService service = client.payment.containers;
    try {
      containers = service.getList(new QueryParams(), null);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      client.close();
    }
    System.out.println("Containers number found: " + containers.getCount());
    return containers;

  }//public ObjectList<Container> getContainersList()

  //============================================================================
  public Container createContainer() {
    System.out.println("-> createContainer");
    Container container = new Container();
    ContainersService service = client.payment.containers;

    Data data = new Data();
    data.setIban("DE62100208900001317270");
    data.setOwner("Max Mustermann");

    container.setPrivateData(data);
    container.setCustomer(customer);
    try {
      container = service.create(container);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      client.close();
    }

    if (container != null && !container.getId().equals("")) {
      System.out.println("Created Container with id: " + container.getId());
      System.out.println("Container data: " + container.toString());
    } else {
      System.out.println("Container creation failed");
    }
    return container;

  }//public Container createContainer()

}//public class Containers

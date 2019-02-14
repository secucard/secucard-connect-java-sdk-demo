package com.secucard.connect.client_payments;


import com.secucard.connect.SecucardConnect;
import com.secucard.connect.product.common.model.ObjectList;
import com.secucard.connect.product.common.model.QueryParams;
import com.secucard.connect.product.payment.ContractsService;
import com.secucard.connect.product.payment.model.Contract;
import com.secucard.connect.product.payment.model.Contract.CloneParams;
import com.secucard.connect.product.payment.model.Data;

public class Contracts {

  private SecucardConnect client = null;

  public Contracts(SecucardConnect client_) {
    client = client_;
  }

  public ObjectList<Contract> getContractsList() {
    System.out.println("-> getContractsList");
    ObjectList<Contract> contracts = null;
    ContractsService service = client.payment.contracts;
    try {
      contracts = service.getList(new QueryParams(), null);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      client.close();
    }
    System.out.println("Contracts number found: " + contracts.getCount());
    return contracts;

  }

  public Contract cloneContract() {
    System.out.println("-> cloneContract");
    Contract contract = new Contract();
    ContractsService service = client.payment.contracts;

    Data data = new Data();
    data.setIban("DE62100208900001317270");
    data.setOwner("Max Mustermann");

    CloneParams params = new CloneParams();
    params.setProject("Unique project ID");
    params.setPaymentData(data); // bank account for payout

    try {
      contract = service.cloneMyContract(params, null);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      client.close();
    }

    if (contract != null && !contract.getId().equals("")) {
      System.out.println("Cloned Contract with id: " + contract.getId());
      System.out.println("Contract data: " + contract.toString());
    } else {
      System.out.println("cloneContract failed");
    }
    return contract;
  }
}

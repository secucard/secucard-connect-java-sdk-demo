package com.secucard.connect.client_payments;

import com.secucard.connect.SecucardConnect;
import com.secucard.connect.auth.AbstractClientAuthDetails;
import com.secucard.connect.auth.model.ClientCredentials;
import com.secucard.connect.auth.model.OAuthCredentials;
import com.secucard.connect.product.common.model.ObjectList;
import com.secucard.connect.product.common.model.QueryParams;
import com.secucard.connect.product.payment.CustomersService;
import com.secucard.connect.product.payment.model.Customer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This example shows the usage of the secucard API transactions from product "payment".
 */
public class ClientPaymentsDemo {
    public static void main(String[] args) throws IOException {
        new ClientPaymentsDemo().test();
    }

    public void test() throws IOException {

        InputStream path = new FileInputStream("src/config.properties");
        SecucardConnect.Configuration cfg = SecucardConnect.Configuration.get(path);

        cfg.clientAuthDetails = new AbstractClientAuthDetails(".your_application_name") {
            @Override
            public OAuthCredentials getCredentials() {
                return new ClientCredentials(
                        "09ae83af7c37121b2de929b211bad944", // your-client-id
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

        CustomersService service = client.payment.customers;

        try {
            ObjectList<Customer> customers = service.getList(new QueryParams(), null);

            System.out.println(customers);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            client.close();
        }
    }
}

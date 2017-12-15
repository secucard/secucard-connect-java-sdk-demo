package com.secucard.connect;

import com.secucard.connect.auth.AbstractClientAuthDetails;
import com.secucard.connect.auth.exception.AuthDeniedException;
import com.secucard.connect.auth.model.ClientCredentials;
import com.secucard.connect.auth.model.DeviceAuthCode;
import com.secucard.connect.auth.model.DeviceCredentials;
import com.secucard.connect.auth.model.OAuthCredentials;
import com.secucard.connect.client.*;
import com.secucard.connect.event.EventListener;
import com.secucard.connect.event.Events;
import com.secucard.connect.product.loyalty.CardGroupsService;
import com.secucard.connect.product.loyalty.Loyalty;
import com.secucard.connect.product.loyalty.MerchantCardsService;
import com.secucard.connect.product.loyalty.model.CardGroup;
import com.secucard.connect.product.smart.Smart;
import com.secucard.connect.product.smart.TransactionService;
import com.secucard.connect.product.smart.model.*;

import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

/**
 * This example shows the usage of the secucard API transactions from product "eGoods".
 * It's not supposed to be something that can be copied and put straight into cash register software, it rather explains
 * some basic principles when using the API.
 */
public class PrepaidDemo {

    public static void main(String[] args) throws Exception {

        // Use the local configuration instead of the server
        InputStream path = new FileInputStream("src/config.properties");
        // Get the default configuration from config.properties, but you may also use your own path or stream parameter.
        // Note: You can also add your own custom properties to the configuration, retrieve them by calling
        // cfg.property("your-prop")
        final SecucardConnect.Configuration cfg = SecucardConnect.Configuration.get(path);

        // Create your com.secucard.connect.auth.ClientAuthDetails implementation by extending the provided abstract class
        // which stores your obtained OAuth tokens to the local disk in default folder ".smartdemostore" in the current working directory.
        // You may also provide your own implementation which saves to database etc. and maybe gets the credentials from
        // custom properties in the configuration.
        AbstractClientAuthDetails authDetails = new AbstractClientAuthDetails(".smartdemostore") {
            @Override
            public OAuthCredentials getCredentials() {
                return new DeviceCredentials(
                        "clientId",
                        "clientSecret",
                        "deviceId");
            }

            @Override
            public ClientCredentials getClientCredentials() {
                return (ClientCredentials) this.getCredentials();
            }
        };

        cfg.clientAuthDetails = authDetails;


        // Get a API client instance.
        final SecucardConnect client = SecucardConnect.create(cfg);

        // Set up event listener, required to handle auth events!
        client.onAuthEvent(new EventListener() {
            @Override
            public void onEvent(Object event) {
                if (event instanceof DeviceAuthCode) {
                    // Device code retrieved successfully - present this data to user.
                    // User must visit URL in DeviceAuthCode.verificationUrl and must enter codes.
                    // Client polls auth server in background meanwhile until success or timeout (config: auth.waitTimeoutSec ).
                    DeviceAuthCode code = (DeviceAuthCode) event;
                    System.out.println("Please visit: " + code.getVerificationUrl() + " and enter code: " + code.getUserCode());
                }

                if ("AUTH_PENDING".equals(event)) {
                    // Present to the user - this event comes up periodically as long the authentication is not performed.
                    System.out.println("Please wait, authentication is pending.");
                }

                if ("AUTH_OK".equals(event)) {
                    // Present to the user - user has device codes codes typed in and the auth was successful.
                    System.out.println("Gratulations, you are now authenticated!");
                }
            }
        });

        // Add a listener
        client.onConnectionStateChanged(new EventListener<Events.ConnectionStateChanged>() {
            @Override
            public void onEvent(Events.ConnectionStateChanged event) {
                System.out.println((event.connected ? "Connected to" : "Disconnected from") + " the secucard server.");
            }
        });

        // Connect to the server.
        // open() will trigger a new authentication if no token is present and block execution until success or error.
        // Call client.cancelAuth(); to abort such a auth process (from another thread of course).
        do {
            try {
                client.open();
                break; // Success!
            } catch (AuthDeniedException e) {
                // Resolvable auth error like invalid credentials were given, let try again.
                System.err.println("Wrong credentials " + e.getMessage());
            } catch (AuthError e) {
                // Unresolvable auth error like wrong client id or secret
                System.err.println("Error during authentication:");
                e.printStackTrace();
                return;
            } catch (NetworkError e) {
                // either you are offline or the configuration settings are wrong
                System.err.println("Can't connect, you are offline or wrong secucard host configured.");
                return;
            } catch (ClientError e) {
                // Any other error caused by unexpected conditions (bugs, wrong config, etc.)
                System.err.println("Error opening client: ");
                e.printStackTrace();
                return;
            }
        } while (true);


        // Now the API client is ready!

        // Smart Transaction  ----------------------------------------------------------------------------------------
        // A transaction purpose is to charge a basket of products (of a shop etc.)

        TransactionService transactions = client.service(Smart.Transactions);

        try {

            // We show the first way: create a empty product basket and the basket summary and create a new transaction first.

            Basket basket = new Basket();
            BasketInfo basketInfo = new BasketInfo(0, "EUR");

            // Add products to the basket and update.
            Product p = new Product();
            p.setId(1);
            p.setEan("4260447140837");
            p.setSerialNumber("565658575656");
            p.setQuantity(new BigDecimal(1));
            p.setDesc("Amazon 25");
            p.setPriceOne(2500);

            basket.addProduct(p);
            basketInfo.setSum(2500);
            Transaction trans = transactions.create(new Transaction(basketInfo, basket, null));

            // demo|auto|cash, demo instructs the server to simulate a different (random) transaction for each invocation of
            // startTransaction, also different formatted receipt lines will be returned.
            String type = TransactionService.TYPE_CASH;

            System.out.println("Transaction started!");

            transactions.start(trans.getId(), type, new Callback<Transaction>() {
                @Override
                public void completed(Transaction result) {
                    // "Print" receipt
                    List<ReceiptLine> receiptLines = result.getReceiptLines();
                    for (ReceiptLine line : receiptLines) {
                        System.out.println("Receipt Line: " + line.getLineType() + ", " + line.getValue());
                    }
                    client.close();
                }

                @Override
                public void failed(Throwable cause) {
                    System.out.println(cause.getMessage());
                }
            });

        } catch (APIError err) {
            // The API server responds with an error, maybe your data are wrong or the API was not used correctly.
            // Depending on the error text its recoverable by editing the data an trying again.
            System.err.println("API Error:");
            err.printStackTrace();
        } catch (AuthError err) {
            // Heavy problem with the token or with token management (refresh), maybe due deactivated account.
            // Usually not recoverable. You may close client, clear token and open again (and thus authenticate new).
            System.err.println("Auth Error:");
            err.printStackTrace();
        } catch (NetworkError err) {
            // No need to close, try again next time if online ...
            System.err.println("You are offline.");
        } catch (ClientError err) {
            // Something bad happened due a bug or wrong config. Better close client, show error end exit.
            System.err.println("Internal error happened.");
            err.printStackTrace();
        } catch (Exception e) {
            // Caused direct by this demo code like NPE
            e.printStackTrace();
        }
    }
}

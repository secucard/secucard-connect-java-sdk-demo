package com.secucard.connect;

import com.secucard.connect.auth.AbstractClientAuthDetails;
import com.secucard.connect.auth.exception.AuthDeniedException;
import com.secucard.connect.auth.model.ClientCredentials;
import com.secucard.connect.auth.model.DeviceAuthCode;
import com.secucard.connect.auth.model.OAuthCredentials;
import com.secucard.connect.auth.model.RefreshCredentials;
import com.secucard.connect.client.APIError;
import com.secucard.connect.client.AuthError;
import com.secucard.connect.client.Callback;
import com.secucard.connect.client.ClientError;
import com.secucard.connect.client.NetworkError;
import com.secucard.connect.event.EventListener;
import com.secucard.connect.event.Events;
import com.secucard.connect.product.general.model.Notification;
import com.secucard.connect.product.smart.Smart;
import com.secucard.connect.product.smart.TransactionService;
import com.secucard.connect.product.smart.model.Basket;
import com.secucard.connect.product.smart.model.BasketInfo;
import com.secucard.connect.product.smart.model.Ident;
import com.secucard.connect.product.smart.model.Product;
import com.secucard.connect.product.smart.model.ReceiptLine;
import com.secucard.connect.product.smart.model.Transaction;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * This example shows the usage of the secucard API transactions from product "smart".
 * It's not supposed to be something that can be copied and put straight into cash register software, it rather explains
 * some basic principles when using the API.
 */
public class SmartLoyaltyDemo {

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
                return new RefreshCredentials(
                    "611c00ec6b2be6c77c2338774f50040b",
                    "dc1f422dde755f0b1c4ac04e7efbd6c4c78870691fe783266d7d6c89439925eb",
                    "..."
                );
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

                if (com.secucard.connect.auth.Events.EVENT_AUTH_PENDING.equals(event)) {
                    // Present to the user - this event comes up periodically as long the authentication is not performed.
                    System.out.println("Please wait, authentication is pending.");
                }

                if (com.secucard.connect.auth.Events.EVENT_AUTH_OK.equals(event)) {
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

        // This will clear an existing token and will trigger an new authentication process when calling client.open()!
        // authDetails.clear();

        // Connect to the server.
        // open() will trigger a new authentication if no token is present and block execution until success or error.
        // Call client.cancelAuth(); to abort such a auth process (from another thread of course).
        do {
            try {
                client.open();
                System.out.println("getToken: " + client.getToken());
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
        // A transaction purpose is to charge a basket of products (of a shop etc.) against an ident.
        // The ident is the reference to a medium of exchange belonging to a customer known to the secucard system.
        // The secucard system supports a number of ident types like cards for instance.
        // So basically all you have to do is get the ident from the customer (by scanning the card or by using the Check-In
        // Feature), sample the product basket and submit the basket along with the ident.

        TransactionService transactions = client.service(Smart.Transactions);

        transactions.onCashierDisplayChanged(new Callback<Notification>() {
            @Override
            public void completed(Notification result) {
                System.out.println("Cashier notification: " + result.getText());
            }

            @Override
            public void failed(Throwable cause) {
                System.err.println("Error processing cashier notification!");
                cause.printStackTrace();
            }
        });

        try {
            // Add some secucard
            Ident ident = new Ident();
            ident.setType(Ident.TYPE_CARD);
            ident.setId("smi_1");
            ident.setValue("927600...");

            // Create a transaction to discharge the secucard
            Transaction trans = new Transaction();
            trans.setIdents(Collections.singletonList(ident));

            Basket basket = new Basket();
            BasketInfo basketInfo = new BasketInfo(0, "EUR");
            trans.setBasket(basket);
            trans.setBasketInfo(basketInfo);

            Product product = new Product(1, null, "123", "5060215249804", "product1", "1", 5, 0, null);
            basket.addProduct(product);
            basketInfo.setSum(5);

            trans = transactions.create(trans);
            System.out.println("Trans.Id: " + trans.getId());
            assert (trans.getStatus().equals(Transaction.STATUS_CREATED));
            System.out.println("Trans.result (1): " + trans);

            // Execute the discharge
            trans = transactions.start(trans.getId(), TransactionService.TYPE_LOYALTY, null);
            assert (trans.getStatus().equals(Transaction.STATUS_OK));
            System.out.println("Trans.result (2): " + trans);

            // "Print" receipt
            List<ReceiptLine> receiptLines = trans.getReceiptLines();
            for (ReceiptLine line : receiptLines) {
                System.out.println("Receipt Line: " + line.getLineType() + ", " + line.getValue());
            }

            // Cancel the transaction, without using a callback
            /*
            Boolean ok = transactions.cancel(trans.getId(), null);
            assert(ok);
            */

            // Cancel the transaction, SDK version <= 2.7.1
            /*
            Boolean ok = transactions.cancel(trans.getId(), new Callback<Boolean>() {
                @Override
                public void completed(Boolean result) {
                    System.out.println("Cancel successful: " + result.toString());
                    assert(result);
                }

                @Override
                public void failed(Throwable cause) {
                    System.err.println("Error processing cancel request!");
                    cause.printStackTrace();
                }
            });
            assert(ok);
            */

            // Cancel the transaction, SDK version >= 2.8.0
            trans = transactions.cancel(trans.getId(), new Callback<Transaction>() {
                @Override
                public void completed(Transaction result) {
                    System.out.println("Cancel successful: " + result.getStatus());
                    assert (result.getStatus().equals(Transaction.STATUS_CANCELED));
                    // "Print" receipt
                    List<ReceiptLine> receiptLines = result.getReceiptLines();
                    for (ReceiptLine line : receiptLines) {
                        System.out.println("Receipt Line: " + line.getLineType() + ", " + line.getValue());
                    }
                }

                @Override
                public void failed(Throwable cause) {
                    System.err.println("Error processing cancel request!");
                    cause.printStackTrace();
                }
            });
            assert (trans.getStatus().equals(Transaction.STATUS_CANCELED));

            /*
            // Cancel a payment
            transactions.cancelPayment("receipt_number_to_cancel", new Callback<Transaction>() {
                @Override
                public void completed(Transaction result) {
                    System.out.println(result.toString());
                }

                @Override
                public void failed(Throwable cause) {
                }
            });

            // Start diagnosis of the terminal
            transactions.diagnosis(new Callback<Transaction>() {
                @Override
                public void completed(Transaction result) {
                }

                @Override
                public void failed(Throwable cause) {
                }
            });

            // Start end of day report
            transactions.endOfDay(new Callback<Transaction>() {
                @Override
                public void completed(Transaction result) {
                }

                @Override
                public void failed(Throwable cause) {
                }
            });
*/
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

        client.close();
    }
}

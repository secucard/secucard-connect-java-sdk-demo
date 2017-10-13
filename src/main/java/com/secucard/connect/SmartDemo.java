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
import com.secucard.connect.product.common.model.MediaResource;
import com.secucard.connect.product.common.model.QueryParams;
import com.secucard.connect.product.general.model.Notification;
import com.secucard.connect.product.loyalty.model.LoyaltyBonus;
import com.secucard.connect.product.smart.Smart;
import com.secucard.connect.product.smart.TransactionService;
import com.secucard.connect.product.smart.model.*;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

/**
 * This example shows the usage of the secucard API transactions from product "smart".
 * It's not supposed to be something that can be copied and put straight into cash register software, it rather explains
 * some basic principles when using the API.
 */
public class SmartDemo {

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
                        "611c00ec6b2be6c77c2338774f50040b",
                        "dc1f422dde755f0b1c4ac04e7efbd6c4c78870691fe783266d7d6c89439925eb",
                        "/vendor/unknown/cashier/dotnettest1");
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

        // Set an optional global exception handler - all exceptions thrown by service methods end up here.
        // If not set each method throws as usual, its up to the developer to catch accordingly.
        // If callback are used all exceptions go to the failed method.
    /*
    client.setServiceExceptionHandler(new ExceptionHandler() {
      @Override
      public void handle(Throwable exception) {
      }
    });
    */

        // This will clear an existing token and will trigger an new authentication process when calling client.open()!
        // authDetails.clear();

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

        // Check-In ------------------------------------------------------------------------------------------------

        // Set up callback to get notified when "Check-In" events happen (customer shows up in store).
        // We just print here for demo purposes, you would probably store in global list or so in real world.
        // You may trigger such an event by using the secucard app.
        client.smart.checkins.onCheckinsChanged(new Callback<List<Checkin>>() {
            @Override
            public void completed(List<Checkin> result) {
                for (Checkin checkin : result) {
                    System.out.println("Checked in: " + checkin.getCustomerName());
                    // Keep the checkin object id for setting as ident value.
                    // At this point all pictures are also downloaded, access binary content to create a image via:
                    MediaResource picture = checkin.getPictureObject();
                    if (picture != null) {
                        InputStream inputStream = picture.getInputStream();
                        // further stream processing ...
                    } else {
                        // You may additionally check if there was an error which caused the picture to be null and handle somehow.
                        Exception error = checkin.getError();
                    }
                }
            }

            @Override
            public void failed(Throwable cause) {
                // Something bad happened, you would probably want to show an error dialog and clear the checkin list.
                System.err.println("Error processing check-in:");
                cause.printStackTrace();
            }
        });


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

            // You may obtain a global list of allowed "idents templates" to cross check if current customers ident
            // is valid at all, this "manual" pre-validation avoids errors when actually submitting transactions later.
            List<Ident> allowedIdents = client.smart.idents.getSimpleList(new QueryParams());
            if (allowedIdents == null) {
                throw new RuntimeException("No idents found."); // Should not happen.
            }

            // Select an ident (card) which will be charged for the basket.
            // Usually the value is the id of a scanned card or the id of a Checkin object taken from the global Check-In list.
            Ident ident = new Ident();
            ident.setType(Ident.TYPE_CARD);
            ident.setId("smi_1");
            ident.setValue("9276004429945947");

            // Now you can proceed in two ways:
            // - creating a "empty" transaction first and adding products afterwards by updating this transaction step by step
            // - adding products to the basket first and creating a new transactions afterwards with the complete basket.
            // The second approach may be faster but you get product errors late and all at once while the first approach shows
            // possible errors immediately after each update.


            // We show the first way: create a empty product basket and the basket summary and create a new transaction first.
            Transaction trans = transactions.create(new Transaction());
            trans.setIdents(Collections.singletonList(ident));
            assert (trans.getStatus().equals(Transaction.STATUS_CREATED));

            Basket basket = new Basket();
            BasketInfo basketInfo = new BasketInfo(0, "EUR");
            trans.setBasket(basket);
            trans.setBasketInfo(basketInfo);


            // Add products to the basket and update.
            ProductGroup productGroup = new ProductGroup("group1", "beverages", 1);
            Product product = new Product(1, null, "123", "5060215249804", "product1", "2", 5000, 1900, Arrays.asList(productGroup));
            basket.addProduct(product);
            basketInfo.setSum(10000);
            Transaction result = transactions.update(trans);

            // Add other product again and update.
            product = new Product(2, null, "456", "1060215249800", "product2", "1", 1000, 1900, Arrays.asList(productGroup));
            basket.addProduct(product);
            basketInfo.setSum(11000);
            result = transactions.update(trans);

            transactions.appendLoyaltyBonusProducts(result.getId(), new Callback<LoyaltyBonus>() {
                @Override
                public void completed(LoyaltyBonus result) {
                    System.out.println(result);
                }

                @Override
                public void failed(Throwable cause) {

                }
            });

            // Um die smart Transaktion schlussendlich auszuführen:
            // trans = transactions.start(trans.getId(), type, null);


            /*
             * Bei Bedarf einkommentieren um Storno einer Payment-Transaktion zu testen
             *

            // demo|auto|cash, demo instructs the server to simulate a different (random) transaction for each invocation of
            // startTransaction, also different formatted receipt lines will be returned.
            String type = TransactionService.TYPE_LOYALTY;

            trans = transactions.start(trans.getId(), type, null);
            assert (trans.getStatus().equals(Transaction.STATUS_OK));

            System.out.println("Transaction started!");

            // "Print" receipt
            List<ReceiptLine> receiptLines = trans.getReceiptLines();
            for (ReceiptLine line : receiptLines) {
                System.out.println("Receipt Line: " + line.getLineType() + ", " + line.getValue());
            }

            // Cancel the transaction.
            boolean ok = transactions.cancel(trans.getId(), null);

            // Status has now changed.
            trans = transactions.get(trans.getId(), null);
            assert (trans.getStatus().equals(Transaction.STATUS_CANCELED));

            // Cancel a payment
            transactions.cancelPayment("number_to_cancel", new Callback<Transaction>() {
                @Override
                public void completed(Transaction result) {
                    System.out.println(result.toString());
                }

                @Override
                public void failed(Throwable cause) {
                }
            });

            */

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

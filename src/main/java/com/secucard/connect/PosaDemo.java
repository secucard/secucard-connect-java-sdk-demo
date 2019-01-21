package com.secucard.connect;

import com.secucard.connect.auth.AbstractClientAuthDetails;
import com.secucard.connect.auth.exception.AuthDeniedException;
import com.secucard.connect.auth.model.ClientCredentials;
import com.secucard.connect.auth.model.DeviceAuthCode;
import com.secucard.connect.auth.model.DeviceCredentials;
import com.secucard.connect.auth.model.OAuthCredentials;
import com.secucard.connect.client.APIError;
import com.secucard.connect.client.AuthError;
import com.secucard.connect.client.ClientError;
import com.secucard.connect.client.NetworkError;
import com.secucard.connect.event.EventListener;
import com.secucard.connect.event.Events;
import com.secucard.connect.product.smart.Smart;
import com.secucard.connect.product.smart.TransactionService;
import com.secucard.connect.product.smart.model.Basket;
import com.secucard.connect.product.smart.model.BasketInfo;
import com.secucard.connect.product.smart.model.Product;
import com.secucard.connect.product.smart.model.ReceiptLine;
import com.secucard.connect.product.smart.model.Transaction;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

/**
 * This example shows the usage of the secucard API transactions from product "smart".
 * It's not supposed to be something that can be copied and put straight into cash register software, it rather explains
 * some basic principles when using the API.
 */
public class PosaDemo {

    public static void main(String[] args) throws Exception {

        InputStream path = new FileInputStream("src/config.properties");
        // Get the default configuration from config.properties, but you may also use your own path or stream parameter.
        // Note: You can also add your own custom properties to the configuration, retrieve them by calling
        // cfg.property("your-prop")
        final SecucardConnect.Configuration cfg = SecucardConnect.Configuration.get(path);

        // Create your com.secucard.connect.auth.ClientAuthDetails implementation by extending the provided abstract class
        // which stores your obtained OAuth tokens to the local disk in default folder ".smartdemostore" in the current working directory.
        // You may also provide your own implementation which saves to database etc. and maybe gets the credentials from
        // custom properties in the configuration.
        cfg.clientAuthDetails = new AbstractClientAuthDetails(".smartdemostore") {
            @Override
            public OAuthCredentials getCredentials() {
                return new DeviceCredentials(
                    "...",
                    "...",
                    "/vendor/.../uuid/..."
                );
            }

            @Override
            public ClientCredentials getClientCredentials() {
                return (ClientCredentials) this.getCredentials();
            }
        };


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
                e.printStackTrace();
                return;
            } catch (AuthError e) {
                // Unresolvable auth error like wrong client id or secret
                System.err.println("Error during authentication:");
                e.printStackTrace();
                return;
            } catch (NetworkError e) {
                // either you are offline or the configuration settings are wrong
                System.err.println("Can't connect, you are offline or wrong secucard host configured.");
                e.printStackTrace();
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

        TransactionService transactions = client.service(Smart.Transactions);

        try {
            // Now you can proceed in two ways:
            // - creating a "empty" transaction first and adding products afterwards by updating this transaction step by step
            // - adding products to the basket first and creating a new transactions afterwards with the complete basket.
            // The second approach may be faster but you get product errors late and all at once while the first approach shows
            // possible errors immediately after each update.


            // We show the first way: create a empty product basket and the basket summary and create a new transaction first.
            Transaction trans = new Transaction();
            Basket basket = new Basket();
            BasketInfo basketInfo = new BasketInfo(0, "EUR");

            Product prod1 = new Product();
            prod1.setId(0);
            prod1.setArticleNumber("");
            prod1.setEan("4251404503734");
            prod1.setDesc("google play 15");
            prod1.setQuantity(new BigDecimal(1));
            prod1.setTax(0);
            prod1.setPriceOne(1500);
            prod1.setSerialNumber("1234");
            basket.addProduct(prod1);

            trans.setBasket(basket);
            basketInfo.setSum(1500);
            trans.setBasketInfo(basketInfo);

            trans.setMerchantRef("Kunde234235");
            trans.setTransactionRef("Beleg4536676");

            trans = transactions.create(trans);
            System.out.println("Trans.Id: " + trans.getId());
            assert (trans.getStatus().equals(Transaction.STATUS_CREATED));
            System.out.println("Trans.result: " + trans);
            /*
             * Sample output:
             * ==============
             * Trans.Id: STX_ZUYTKSY3E2N25VMQYH3G9C8N7DQQAJ
             * Trans.result: Transaction{basketInfo=BasketInfo{sum=1500, currency=EUR}, deviceSource=null, targetDevice=null, status='created', created=Mon Jan 21 18:13:25 CET 2019, updated=Mon Jan 21 18:13:25 CET 2019, idents=null, basket=Basket{products=[Product{id='0', parent='null', articleNumber='', ean='4251404503734', desc='google play 15', quantity=1, priceOne=1500, tax=0, productGroups=[], serialNumber=1234}], texts=[]}, merchantRef='Kunde234235', transactionRef='Beleg4536676', paymentMethod='null', receiptLines=null, receiptNumber='null', error='null'} com.secucard.connect.product.smart.model.Transaction@50378a4
             */

            // Um die smart Transaktion schlussendlich auszuführen:
            trans = transactions.start(trans.getId(), TransactionService.TYPE_CASH, null);
            assert (trans.getStatus().equals(Transaction.STATUS_OK));
            System.out.println("Transaction started!");


            // "Print" receipt
            List<ReceiptLine> receiptLines = trans.getReceiptLines();
            for (ReceiptLine line : receiptLines) {
                System.out.println("Receipt Line: " + line.getLineType() + ", " + line.getValue());
            }

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

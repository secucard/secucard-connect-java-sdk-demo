/*
 * Copyright (c) 2015. hp.weber GmbH & Co secucard KG (www.secucard.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import com.secucard.connect.product.common.model.QueryParams;
import com.secucard.connect.product.general.model.Notification;
import com.secucard.connect.product.smart.CheckinService;
import com.secucard.connect.product.smart.Smart;
import com.secucard.connect.product.smart.TransactionService;
import com.secucard.connect.product.smart.model.*;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class SmartDemo {

  public static void main(String[] args) throws Exception {

    // Get the default configuration from config.properties, but you may also use your own path or stream parameter.
    // Note: You can also add your own custom properties to the configuration, retrieve them by calling
    // cfg.property("your-prop")
    final SecucardConnect.Configuration cfg = SecucardConnect.Configuration.get();

    // Create your com.secucard.connect.auth.ClientAuthDetails implementation by extending the provided abstract class
    // which stores tokens to the local disk in default folder ".smartdemostore" in the current working directory.
    // You may also provide your own implementation which saves to database etc. and maybe gets the credentials from
    // custom properties in the configuration.
    AbstractClientAuthDetails authDetails = new AbstractClientAuthDetails(".smartdemostore") {
      @Override
      public OAuthCredentials getCredentials() {
        return new DeviceCredentials(
            "your-client-id",
            "your-client-secret",
            "your-device-id");
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

    // Checkins ------------------------------------------------------------------------------------------------

    CheckinService service = client.smart.checkins;
    // Alternatively use: CheckinService service = client.service(Smart.Checkins);

    // Set up callback to get notified when a check in event was processed.
    service.onCheckinsChanged(new Callback<List<Checkin>>() {
      @Override
      public void completed(List<Checkin> result) {
        // At this point  all pictures are downloaded.
        // Access binary content to create a image like:
        InputStream is = result.get(0).getPictureObject().getInputStream();
      }

      @Override
      public void failed(Throwable cause) {
        // Error happened, handle appropriate, no need to disconnect client here.
      }
    });


    // Smart Transaction  ----------------------------------------------------------------------------------------

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

      // Select an ident.
      List<Ident> availableIdents = client.smart.idents.getSimpleList(new QueryParams());
      if (availableIdents == null) {
        throw new RuntimeException("No idents found.");
      }

      Ident ident = Ident.find("smi_1", availableIdents);
      ident.setValue("pdo28hdal");

      Basket basket = new Basket();
      basket.addProduct(
          new Product(1, null, "3378", "5060215249804", "desc1", "1", 1, 20, Arrays.asList(new ProductGroup("group1", "beverages", 1)))
      );
      BasketInfo basketInfo = new BasketInfo(1, "EUR");

      Transaction newTrans = new Transaction(basketInfo, basket, Arrays.asList(ident));

      Transaction trans = transactions.create(newTrans, null);
      assert (trans.getStatus().equals(Transaction.STATUS_CREATED));


      // You may edit some transaction data and update.
      newTrans.setMerchantRef("merchant");
      trans.setTransactionRef("trans1");
      trans = transactions.update(trans, null);

      // demo|auto|cash, demo instructs the server to simulate a different (random) transaction for each invocation of
      // startTransaction, also different formatted receipt lines will be returned.
      String type = "demo";

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
      System.err.println("Internal error happened, ");
    } catch (Exception e){
      // Caused direct by this demo code like NPE
    }


    client.close();
  }
}

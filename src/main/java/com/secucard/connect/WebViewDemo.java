package com.secucard.connect;

import com.secucard.connect.auth.AbstractClientAuthDetails;
import com.secucard.connect.auth.model.*;
import com.secucard.connect.client.*;
import com.secucard.connect.event.EventListener;
import com.secucard.connect.product.smart.CheckinService;
import com.secucard.connect.product.smart.IdentService;
import com.secucard.connect.product.smart.Smart;
import com.secucard.connect.product.smart.TransactionService;
import com.secucard.connect.product.smart.model.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.io.*;
import java.net.*;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WebViewDemo extends Application {

    public static SecucardConnect client;
    private static CheckinService checkinService;
    private static IdentService identService;

    @Override
    public void start(final Stage stage) {
        // 3 individual windows to display a different frame
        final WebView smartCheckinButton = new WebView();
        final WebView userSelectionScreen = new WebView();
        final WebView identLink = new WebView();

        identService = client.service(Smart.Idents);
        checkinService = client.service(Smart.Checkins);

        /*
         * A STOMP message for the checkin button arrived
         */
        checkinService.onChanged(new Callback<List<ComponentInstruction>>() {
            @Override
            public void completed(final List<ComponentInstruction> results) {
                for (ComponentInstruction result : results) {
                    if (result.target.equals(ComponentInstruction.COMPONENT_TARGET_CHECKIN_BUTTON)) {
                        switch (result.action) {
                            // Show the smart checkin button
                            // Update the smart checkin button
                            case ComponentInstruction.COMPONENT_ACTION_OPEN:
                            case ComponentInstruction.COMPONENT_ACTION_UPDATE:
                                Platform.runLater(doUIStuff(result, smartCheckinButton, null));
                                break;
                        }
                    }
                }
            }

            @Override
            public void failed(Throwable cause) {
                System.out.println(cause.getMessage());
            }
        });

        /*
         * A STOMP message for the user-selection or ident-link arrived
         */
        identService.onChanged(new Callback<List<ComponentInstruction>>() {
            @Override
            public void completed(List<ComponentInstruction> results) {
                for (ComponentInstruction result : results) {
                    switch (result.target) {
                        case ComponentInstruction.COMPONENT_TARGET_USER_SELECTION: {
                            switch (result.action) {
                                case ComponentInstruction.COMPONENT_ACTION_OPEN:
                                    Platform.runLater(doUIStuff(result, userSelectionScreen, null));
                                    break;
                                case ComponentInstruction.COMPONENT_ACTION_UPDATE:
                                    if (!userSelectionScreen.getEngine().getTitle().equals("")) {
                                        Platform.runLater(doUIStuff(result, userSelectionScreen, null));
                                    }
                                    break;
                                case ComponentInstruction.COMPONENT_ACTION_CLOSE:
                                    Platform.runLater(doUIStuff(result, userSelectionScreen, "about:blank"));
                                    break;
                            }
                        }
                        break;
                        case ComponentInstruction.COMPONENT_TARGET_IDENT_LINK: {
                            switch (result.action) {
                                case ComponentInstruction.COMPONENT_ACTION_OPEN:
                                case ComponentInstruction.COMPONENT_ACTION_UPDATE:
                                    Platform.runLater(doUIStuff(result, identLink, null));
                                    break;
                                case ComponentInstruction.COMPONENT_ACTION_CLOSE:
                                    identLink.getEngine().load("about:blank");
                                    break;
                            }
                        }
                        break;
                    }
                }
            }

            @Override
            public void failed(Throwable cause) {
                System.out.println(cause.getMessage());
            }
        });

        Button buttonURL = new Button("Start");
        buttonURL.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                checkinService.initiateComponentCheckinButton();
            }
        });

        Button buttonSTX = new Button("create and start STX");
        buttonSTX.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    TransactionService transactions = client.service(Smart.Transactions);

                    // Add some secucard
                    Ident ident = new Ident();
                    ident.setType(Ident.TYPE_CARD);
                    ident.setId("smi_1");
                    ident.setValue("9276004429132943");

                    // Create a transaction to discharge the secucard
                    Transaction trans = new Transaction();
                    trans.setIdents(Collections.singletonList(ident));

                    Basket basket = new Basket();
                    BasketInfo basketInfo = new BasketInfo(0, "EUR");
                    trans.setBasket(basket);
                    trans.setBasketInfo(basketInfo);

                    // EAN 4260447149502 will charge a card
                    Product product = new Product(1, null, "123", "4260447149502", "product1", "1", 5, 0, null);
                    basket.addProduct(product);
                    basketInfo.setSum(5);

                    transactions.create(trans, new Callback<Transaction>() {
                        @Override
                        public void completed(Transaction trans2) {
                            System.out.println("Trans.Id: " + trans2.getId());
                            assert (trans2.getStatus().equals(Transaction.STATUS_CREATED));
                            System.out.println("Trans.result (1): " + trans2);

                            // Execute the charge
                            transactions.start(trans2.getId(), TransactionService.TYPE_CASH, new Callback<Transaction>() {
                                @Override
                                public void completed(Transaction trans3) {
                                    assert (trans3.getStatus().equals(Transaction.STATUS_OK));
                                    System.out.println("Trans.result (2): " + trans3);

                                    // "Print" receipt
                                    List<ReceiptLine> receiptLines = trans3.getReceiptLines();
                                    for (ReceiptLine line : receiptLines) {
                                        System.out.println("Receipt Line: " + line.getLineType() + ", " + line.getValue());
                                    }
                                }

                                @Override
                                public void failed(Throwable cause) {
                                    System.err.println("Error processing start STX!");
                                    cause.printStackTrace();
                                }
                            });
                        }

                        @Override
                        public void failed(Throwable cause) {
                            System.err.println("Error processing create STX!");
                            cause.printStackTrace();
                        }
                    });
                } catch (APIError err) {
                    // The API server responds with an error, maybe your data are wrong or the API was not used correctly.
                    // Depending on the error text it's recoverable by editing the data and trying again.
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
        });

        VBox root = new VBox();
        root.setPadding(new Insets(5));
        root.setSpacing(5);
        root.getChildren().addAll(buttonURL, buttonSTX, smartCheckinButton, userSelectionScreen, identLink);

        Scene scene = new Scene(root);
        stage.setTitle("SMART-Kasse Implementierungsdemo");
        stage.setScene(scene);
        stage.setWidth(600);
        stage.setHeight(600);

        stage.show();
    }

    /**
     * If the window should be closed
     */
    @Override
    public void stop() {
        client.close();
    }

    /*
     * To change the UI you have to do it in another thread
     */
    private Runnable doUIStuff(final ComponentInstruction result, final WebView frame, final String url) {
        return new Runnable() {
            @Override
            public void run() {
                if (url != null) {
                    frame.getEngine().load(url);
                } else {
                    frame.getEngine().loadContent(fetchHTML(result.url));
                }
            }
        };
    }

    /*
     * Fetch the content
     */
    private String fetchHTML(String url) {
        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("POST");
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("access_token", client.getToken());

            StringBuilder postData = new StringBuilder();
            for (Map.Entry<String, Object> param : params.entrySet()) {
                if (postData.length() != 0) postData.append('&');
                postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                postData.append('=');
                postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
            }
            byte[] postDataBytes = postData.toString().getBytes("UTF-8");

            con.setDoOutput(true);
            con.getOutputStream().write(postDataBytes);


            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            in.close();

            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static void main(final String[] args) throws FileNotFoundException {

        // Use the local configuration instead of the server
        InputStream path = new FileInputStream("src/config.properties");

        final SecucardConnect.Configuration cfg = SecucardConnect.Configuration.get(path);

        cfg.clientAuthDetails = new AbstractClientAuthDetails(".smartdemostore") {
            @Override
            public OAuthCredentials getCredentials() {
                return new DeviceCredentials(
                        "...",  // TODO add your custom credentials
                        "...",
                        "..."
                );
            }

            @Override
            public ClientCredentials getClientCredentials() {
                return (ClientCredentials) this.getCredentials();
            }
        };

        // Get a API client instance.
        client = SecucardConnect.create(cfg);

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
                    // Present to the user - user has device codes typed in and the auth was successful.
                    System.out.println("Gratulations, you are now authenticated!");
                }
            }
        });

        client.open();
        launch(args);
    }
}
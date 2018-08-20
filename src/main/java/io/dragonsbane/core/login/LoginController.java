package io.dragonsbane.core.login;

import io.dragonsbane.core.BaseController;
import io.onemfive.core.did.DIDService;
import io.onemfive.data.DID;
import io.onemfive.data.Envelope;
import io.onemfive.data.ServiceCallback;
import io.onemfive.data.util.DLC;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.security.NoSuchAlgorithmException;

/**
 * TODO: Add Description
 *
 * @author objectorange
 */
public class LoginController extends BaseController {

    public void enter(ActionEvent event) {
        boolean authorized = false;
        ObservableList<Node> nodes = ((GridPane)root).getChildren();
        final TextField aliasField = (TextField)nodes.get(1);
        final TextField passphraseField = (TextField)nodes.get(2);
        final Label mLabelTop = (Label)nodes.get(4);
        final Label mLabelBottom = (Label)nodes.get(5);
        String alias = aliasField.getText();
        String passphrase = passphraseField.getText();
        if(alias == null || alias.isEmpty() || passphrase == null || passphrase.isEmpty()) {
            mLabelTop.setText("Alias Required");
            mLabelBottom.setText("Passphrase Required");
            mLabelTop.setVisible(true);
            mLabelBottom.setVisible(true);
            return;
        } else {
            dApp.getUserDID().setAlias(alias);
            try {
                dApp.getUserDID().setPassphrase(passphrase);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            mLabelTop.setVisible(false);
            mLabelBottom.setVisible(false);
            mLabelTop.setText("");
            mLabelBottom.setText("");
        }
        // Verify credentials
//        authorized = "Bob".equals(alias) && "1234".equals(passphrase);
//        if(!authorized) {
//            mLabelTop.setText("Authentication Failed");
//            mLabelTop.setVisible(true);
//            mLabelBottom.setText("Please try again");
//            mLabelBottom.setVisible(true);
//            return;
//        } else {
//            dApp.getHomeController().view();
//        }
        // Authenticate and Load Credentials
        Envelope e = Envelope.headersOnlyFactory();
        e.setDID(dApp.getUserDID());
        DLC.addRoute(DIDService.class, DIDService.OPERATION_AUTHENTICATE,e);

        ServiceCallback cb = new ServiceCallback() {
            @Override
            public void reply(Envelope envelope) {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        DID did = envelope.getDID();
                        dApp.setUserDID(did);
                        if (did.getAuthenticated()) {
                            mLabelTop.setVisible(false);
                            mLabelBottom.setVisible(false);
                            mLabelTop.setText("");
                            mLabelBottom.setText("");
                            dApp.getHomeController().view();
                        }
                    }
                });
            }
        };

        dApp.send(e, cb);
    }

    public void view() {
        // Give a couple seconds to let sensors initialize
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {

        }
        Stage primaryStage = getPrimaryStage();
        primaryStage.setScene(scene);
        primaryStage.show();
    }

}

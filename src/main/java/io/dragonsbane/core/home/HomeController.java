package io.dragonsbane.core.home;

import io.dragonsbane.core.BaseController;
import io.dragonsbane.core.DApp;
import io.onemfive.core.client.ClientAppManager;
import io.onemfive.core.client.ClientStatusListener;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.util.Properties;

/**
 * TODO: Add Description
 *
 * @author objectorange
 */
public class HomeController extends BaseController implements ClientStatusListener {

    private Image stoppedImage = new Image("io/dragonsbane/core/home/circle-red.jpeg");
    private Image transitingImage = new Image("io/dragonsbane/core/home/circle-orange.jpeg");
    private Image runningImage = new Image("io/dragonsbane/core/home/circle-green.jpeg");
    private ImageView statusImage = null;
    private TabPane tabPane = null;
    private SplitPane splitPane = null;

    @Override
    public Boolean init(DApp dApp, Parent root, Scene scene, Properties p) {
        super.init(dApp, root, scene, p);
        return true;
    }

    public void view() {
        if(statusImage == null || tabPane == null || splitPane == null) {
            splitPane = (SplitPane) ((AnchorPane) root).getChildren().get(0);
            ObservableList<Node> nodes = splitPane.getItems();
            for (Node n : nodes) {
                if ("header".equals(n.getId())) {
                    statusImage = (ImageView) ((AnchorPane) n).getChildren().get(0);
                }
                if ("body".equals(n.getId())) {
                    tabPane = (TabPane) ((AnchorPane) n).getChildren().get(0);
                    tabPane.getSelectionModel().selectedItemProperty()
                            .addListener(new ChangeListener<Tab>() {
                                @Override
                                public void changed(ObservableValue<? extends Tab> old, Tab oldTab, Tab newTab) {
                                    switch (newTab.getText()) {
                                        case "Info": {
                                            dApp.getInfoController().view();
                                            break;
                                        }
                                        case "Settings": {
                                            dApp.getSettingsController().view();
                                            break;
                                        }
                                    }
                                }

                            });
                }
            }
        }
        Stage primaryStage = getPrimaryStage();
        primaryStage.setScene(scene);
        primaryStage.show();
        splitPane.lookupAll(".split-pane-divider")
                .stream()
                .forEach(div ->  div.setVisible(false));
        dApp.getInfoController().view();
    }

    @Override
    public void clientStatusChanged(ClientAppManager.Status status) {
        Task<Void> t = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        switch (status) {
                            case INITIALIZING: {
                                statusImage.setImage(transitingImage);break;
                            }
                            case READY: {
                                statusImage.setImage(runningImage);break;
                            }
                            case STOPPING: {
                                statusImage.setImage(transitingImage);break;
                            }
                            default: {
                                statusImage.setImage(stoppedImage);
                            }
                        }
                    }
                });
                return null;
            }
        };
        Thread th = new Thread(t);
        th.setDaemon(true);
        th.start();
    }
}

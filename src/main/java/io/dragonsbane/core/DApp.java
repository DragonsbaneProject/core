package io.dragonsbane.core;

import io.dragonsbane.core.home.HomeController;
import io.dragonsbane.core.info.InfoController;
import io.dragonsbane.core.login.LoginController;
import io.dragonsbane.core.settings.SettingsController;
import io.onemfive.core.Config;
import io.onemfive.core.OneMFiveAppContext;
import io.onemfive.core.client.Client;
import io.onemfive.core.client.ClientAppManager;
import io.onemfive.core.client.ClientStatusListener;
import io.onemfive.data.*;
import io.onemfive.data.util.DLC;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

public class DApp extends Application implements ClientStatusListener {

    private static Logger LOG = Logger.getLogger(DApp.class.getName());

    private static DApp dApp;

    private enum Status {INITIALIZING, STARTING, RUNNING, STOPPING, STOPPED}
    private Status status = Status.STOPPED;

    private Properties config = null;

    // 1M5 Privacy Network
    private OneMFiveAppContext oneMFiveAppContext;
    private DID userDID = new DID();
    private ClientAppManager clientAppManager;
    private Client client;
    private ServiceCallback serviceRegistrationExceptionsCallback;
    private ServiceCallback identityCallback;

    private Envelope.Sensitivity sensitivity = Envelope.Sensitivity.HIGH; // Default I2P

    private static int shutdownCode = 0;

    // UI - Menu
    private Stage primaryStage;
    // UI - Controllers
    private HomeController homeController;
    private InfoController infoController;
    private LoginController loginController;
    private SettingsController settingsController;

    public DApp() {
        dApp = this;
    }

    @Override
    public void clientStatusChanged(ClientAppManager.Status status) {
        switch (status) {
            case INITIALIZING: {
                LOG.info("1M5 Initializing...");
            }
            case READY: {
                LOG.info("1M5 Ready.");
            }
            case STOPPING: {
                LOG.info("1M5 Stopping...");
            }
            case STOPPED: {
                LOG.info("1M5 Stopped.");
            }
        }
    }

    @Override
    public void init() throws Exception {
        LOG.info("Initializing...");
        status = Status.INITIALIZING;

        LOG.info("Loading configuration...");

        config = new Properties();
        // Default to Linux
        config.put(Config.PROP_OPERATING_SYSTEM, Config.OS.Linux.name());
        // TODO: get screen dimensions

        // Directories
        String rootDir = "/usr/local/dgb";
        File ikFolder = new File(rootDir);
        if(!ikFolder.exists())
            if(!ikFolder.mkdir())
                throw new Exception("Unable to create Dragonsbane directory: "+rootDir);
        config.setProperty("dragonsbane.dir.base",rootDir);
        LOG.info("Dragonsbane Root Directory: "+rootDir);

        String oneMFiveDir = rootDir + "/.1m5";
        File oneMFiveFolder = new File(oneMFiveDir);
        if(!oneMFiveFolder.exists())
            if(!oneMFiveFolder.mkdir())
                throw new Exception("Unable to create 1M5 base directory: "+oneMFiveDir);
        config.setProperty("1m5.dir.base",oneMFiveDir);
        LOG.info("1M5 Root Directory: "+oneMFiveDir);

        // UI
        LOG.info("Loading UI components...");
        FXMLLoader loader;
        Parent root;
        Scene scene;

        // UI - Login
        loader = new FXMLLoader(getClass().getResource("login/login.fxml"));
        root = loader.load();
        scene = new Scene(root, 700, 500);
        loginController = loader.getController();
        loginController.init(this, root, scene, config);

        // UI - Home
        loader = new FXMLLoader(getClass().getResource("home/home.fxml"));
        root = loader.load();
        scene = new Scene(root, 700, 500);
        homeController = loader.getController();
        homeController.init(this, root, scene, config);
        SplitPane splitPane = (SplitPane)((AnchorPane)root).getChildren().get(0);
        ObservableList<Node> nodes = splitPane.getItems();
        for(Node n : nodes) {
            if("body".equals(n.getId())) {
                ObservableList<Node> bodyNodes = ((AnchorPane) n).getChildren();
                for (Node bn : bodyNodes) {
                    if (bn instanceof TabPane) {
                        ObservableList<Tab> tabs = ((TabPane) bn).getTabs();
                        for (Tab tab : tabs) {
                            switch (tab.getText()) {
                                case "Info": {
                                    loader = new FXMLLoader(getClass().getResource("info/info.fxml"));
                                    root = loader.load();
                                    tab.setContent(root);
                                    infoController = loader.getController();
                                    infoController.init(this, root, scene, config);
                                    break;
                                }
                                case "Settings": {
                                    loader = new FXMLLoader(getClass().getResource("settings/settings.fxml"));
                                    root = loader.load();
                                    tab.setContent(root);
                                    settingsController = loader.getController();
                                    settingsController.init(this, root, scene, config);
                                    break;
                                }
                                default: {
                                    LOG.warning("Tab not supported: " + tab.getText());
                                }
                            }
                        }
                    }
                }
            }
        }

        // Getting ClientAppManager starts 1M5 Bus
        LOG.info("Starting 1M5 Bus...");
        oneMFiveAppContext = OneMFiveAppContext.getInstance(config);
        clientAppManager = oneMFiveAppContext.getClientAppManager();
        client = clientAppManager.getClient(true);

        // Register Home Controller for Client Status updates
        client.registerClientStatusListener(getHomeController());

        serviceRegistrationExceptionsCallback = new ServiceCallback() {
            @Override
            public void reply(Envelope envelope) {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        List<Exception> exceptions = DLC.getExceptions(envelope);
                        if(exceptions != null && exceptions.size() > 0) {
                            // signal to shutdown as exceptions occurred during service registration
                            for(Exception ex : exceptions) {
                                LOG.severe(ex.getLocalizedMessage());
                            }
                            shutdownCode = -1;
                        }
                    }
                });
            }
        };

        // wait a couple seconds to let the bus and internal services start
        waitABit(2 * 1000);
        if(shutdownCode == -1) {
            stop();
            return;
        }

        // register Dragonsbane core services
//        Envelope e1 = Envelope.documentFactory();
//        List<Class> services = new ArrayList<>();
//        services.add(DragonsHealthService.class);
//        DLC.addEntity(services,e1);
//        DLC.addRoute(AdminService.class, AdminService.OPERATION_REGISTER_SERVICES,e1);
//        LOG.info("Registering Dragonsbane services with bus...");
//        send(e1, serviceRegistrationExceptionsCallback);

        Runtime.getRuntime().addShutdownHook(shutdownHook);

        // wait a bit to let the identities register
        waitABit(5 * 1000);
        if(shutdownCode == -1) {
            stop();
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception{
        LOG.info("Starting...");
        status = Status.STARTING;
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Dragonsbane");
        loginController.view();
        status = Status.RUNNING;
        LOG.info("Running");
    }

    @Override
    public void stop() throws Exception {
        LOG.info("Stopping...");
        status = Status.STOPPING;
        clientAppManager.stop();
        status = Status.STOPPED;
        LOG.info("Stopped");
    }

    public static void main(String[] args) {
        launch(args);
    }

    public void send(Envelope e) {
        Task t = new Task() {
            @Override
            protected Object call() throws Exception {
                client.request(e);
                return null;
            }
        };
        Thread th = new Thread(t);
        th.setDaemon(true);
        th.start();
    }

    public void send(Envelope e, ServiceCallback cb) {
        Task t = new Task() {
            @Override
            protected Object call() throws Exception {
                client.request(e, cb);
                return null;
            }
        };
        Thread th = new Thread(t);
        th.setDaemon(true);
        th.start();
    }

    Stage getPrimaryStage() {
        return primaryStage;
    }

    public HomeController getHomeController() {
        return homeController;
    }

    public InfoController getInfoController() {
        return infoController;
    }

    public LoginController getLoginController() {
        return loginController;
    }

    public SettingsController getSettingsController() {
        return settingsController;
    }

    public DID getUserDID() {
        return userDID;
    }

    public void setUserDID(DID userDID) {
        this.userDID = userDID;
    }

    public Envelope.Sensitivity getSensitivity() {
        return sensitivity;
    }

    public void setSensitivity(int sensitivityCode) {
        switch (sensitivityCode) {
            case 0: sensitivity = Envelope.Sensitivity.NONE;break;
            case 1: sensitivity = Envelope.Sensitivity.LOW;break;
            case 2: sensitivity = Envelope.Sensitivity.MEDIUM;break;
            case 3: sensitivity = Envelope.Sensitivity.HIGH;break;
            case 4: sensitivity = Envelope.Sensitivity.VERYHIGH;break;
            case 5: sensitivity = Envelope.Sensitivity.EXTREME;break;
            case 6: sensitivity = Envelope.Sensitivity.NEO;break;
        }
        LOG.info("Sensitivity set to: "+sensitivity.name());
    }

    private void waitABit(long waitTime) {
        try {
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {}
    }

    private Thread shutdownHook = new Thread() {

        @Override
        public void run() {
            try {
                dApp.stop();
            } catch (Exception e) {
                LOG.warning(e.getLocalizedMessage());
            }
        }
    };
}

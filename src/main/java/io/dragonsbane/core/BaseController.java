package io.dragonsbane.core;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Properties;
import java.util.logging.Logger;

/**
 * TODO: Add Description
 *
 * @author objectorange
 */
public abstract class BaseController implements Controller {

    protected Logger LOG = Logger.getLogger(this.getClass().getName());

    protected DApp dApp;
    protected Parent root;
    protected Scene scene;

    public Boolean init(DApp dApp, Parent root, Scene scene, Properties p) {
        this.dApp = dApp;
        this.root = root;
        this.scene = scene;
        return true;
    }

    protected Stage getPrimaryStage() {
        return dApp.getPrimaryStage();
    }
}

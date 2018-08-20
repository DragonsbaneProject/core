package io.dragonsbane.core.settings;

import io.dragonsbane.core.BaseController;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * TODO: Add Description
 *
 * @author objectorange
 */
public class SettingsController extends BaseController {

    private boolean initialized = false;
    private TextField currentAliasEncodedKey = null;
    private Slider settingsSensitivity = null;
    private Label settingsSensitivityName = null;
    private CheckBox shareStorage = null;
    private Slider maxStorage = null;
    private Label maxStorageLabel = null;
    private TextField tokenPricePerGB = null;
    private Label tokenPricePerGBLabel = null;
    private Label currentlyStoredLabel = null;
    private Label currentlyStored = null;

    public void setCurrentAliasEncodedKey(String currentAliasEncodedKey) {
        this.currentAliasEncodedKey.setText(currentAliasEncodedKey);
    }

    public void view() {
        if(!initialized) {
            ObservableList<Node> n = ((HBox) ((AnchorPane) root).getChildren().get(0)).getChildren();
            // Identity
            VBox v1 = (VBox) n.get(0);
            if(currentAliasEncodedKey == null) {
                currentAliasEncodedKey = (TextField) v1.getChildren().get(2);
            }

            // Sensitivity
            VBox v2 = (VBox) n.get(1);
            if (settingsSensitivity == null) {
                settingsSensitivity = (Slider) v2.getChildren().get(2);
                dApp.setSensitivity(settingsSensitivity.valueProperty().intValue());
                settingsSensitivity.valueProperty().addListener(new ChangeListener<Number>() {
                    @Override
                    public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                        dApp.setSensitivity(newValue.intValue());
                        switch(newValue.intValue()) {
                            case 0 : settingsSensitivityName.setTextFill(Color.DARKRED);break;
                            case 1 : settingsSensitivityName.setTextFill(Color.RED);break;
                            case 2 : settingsSensitivityName.setTextFill(Color.ORANGE);break;
                            case 3 : settingsSensitivityName.setTextFill(Color.GREEN);break;
                            case 4 : settingsSensitivityName.setTextFill(Color.BLUE);break;
                            case 5 : settingsSensitivityName.setTextFill(Color.PURPLE);break;
                            case 6 : settingsSensitivityName.setTextFill(Color.DARKSLATEBLUE);break;
                        }
                        settingsSensitivityName.setText(dApp.getSensitivity().name());
                    }
                });
            }
            if(settingsSensitivityName == null) {
                settingsSensitivityName = (Label) v2.getChildren().get(3);
                switch(settingsSensitivity.valueProperty().intValue()) {
                    case 0 : settingsSensitivityName.setTextFill(Color.DARKRED);break;
                    case 1 : settingsSensitivityName.setTextFill(Color.RED);break;
                    case 2 : settingsSensitivityName.setTextFill(Color.ORANGE);break;
                    case 3 : settingsSensitivityName.setTextFill(Color.GREEN);break;
                    case 4 : settingsSensitivityName.setTextFill(Color.BLUE);break;
                    case 5 : settingsSensitivityName.setTextFill(Color.PURPLE);break;
                    case 6 : settingsSensitivityName.setTextFill(Color.DARKSLATEBLUE);break;
                }
                settingsSensitivityName.setText(dApp.getSensitivity().name());
            }
        }
        if(dApp.getUserDID().getIdentityHash() != null)
            currentAliasEncodedKey.setText(new String(dApp.getUserDID().getIdentityHash()));
    }
}

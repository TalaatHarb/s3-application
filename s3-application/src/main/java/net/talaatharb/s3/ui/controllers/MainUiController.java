package net.talaatharb.s3.ui.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.s3.service.CredentialConfigService;
import net.talaatharb.s3.service.S3StorageService;

@Slf4j
public class MainUiController implements Initializable, SceneManager {

    @Getter(value = AccessLevel.PACKAGE)
    @Setter(value = AccessLevel.PACKAGE)
    @FXML
    private AnchorPane mainContainer;

    @Getter(value = AccessLevel.PACKAGE)
    @Setter(value = AccessLevel.PACKAGE)
    @FXML
    private ListView<String> bucketListView;

    @Getter(value = AccessLevel.PACKAGE)
    @Setter(value = AccessLevel.PACKAGE)
    @FXML
    private ListView<String> objectListView;
    
    @Getter(value = AccessLevel.PACKAGE)
    @Setter(value = AccessLevel.PACKAGE)
    @FXML
    private ComboBox<String> configCombo;

    private Stage primaryStage;
    
    @FXML
    private S3StorageService s3Service;

    @Getter(value = AccessLevel.PACKAGE)
    @Setter(value = AccessLevel.PACKAGE)
    @FXML
    private CredentialConfigService configService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.debug("Initializing UI application Main window controller...");
        try {
            if(configService == null) {
                configService = new CredentialConfigService();
            }
            var configNames = configService.listConfigs();
            // Fill configCombo with available credential configurations
            if (configCombo != null) {
                configCombo.setItems(FXCollections.observableArrayList(configNames));
            }
            if (configNames.size() == 1) {
                var config = configService.readConfig(configNames.get(0));
                var minioClient = configService.getS3Client(config);
                s3Service = new S3StorageService(minioClient);
                log.info("Using S3 configuration: {}", configNames.get(0));
            } else {
                s3Service = new S3StorageService();
                log.info("Using default S3 configuration");
            }
            var buckets = s3Service.listBuckets();
            bucketListView.getItems().setAll(buckets);
            // Add listener to bucket selection
            bucketListView.getSelectionModel().selectedItemProperty().addListener((_, _, newValue) -> {
                if (newValue != null && s3Service != null && objectListView != null) {
                    try {
                        var objects = s3Service.listObjects(newValue, "");
                        objectListView.setItems(FXCollections.observableArrayList(objects));
                    } catch (Exception e) {
                        log.error("Failed to list objects for bucket: " + newValue, e);
                        objectListView.setItems(FXCollections.observableArrayList());
                    }
                } else if (objectListView != null) {
                    objectListView.setItems(FXCollections.observableArrayList());
                }
            });
        } catch (Exception e) {
            log.error("Failed to list buckets", e);
        }
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
        log.debug("Primary stage set, starting with initial scene");
    }
}

// Note: Add a ListView with fx:id="bucketListView" to your FXML layout for bucket display.
// Note: Add a ListView with fx:id="objectListView" to your FXML layout for object display.
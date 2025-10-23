package net.talaatharb.s3.ui.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import io.minio.MinioClient;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.s3.config.HelperBeans;
import net.talaatharb.s3.dto.CredentialConfig;
import net.talaatharb.s3.service.CredentialConfigService;
import net.talaatharb.s3.service.S3StorageService;

@Slf4j
public class MainUiController implements Initializable, SceneManager {

    @Getter(value = AccessLevel.PACKAGE)
    @Setter(value = AccessLevel.PACKAGE)
    @FXML
    private AnchorPane mainContainer;

    @FXML
    private ListView<String> bucketListView;

    private Stage primaryStage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.debug("Initializing UI application Main window controller...");
        S3StorageService s3Service;
        try {
            CredentialConfigService configService = new CredentialConfigService();
            var configNames = configService.listConfigs();
            if (configNames.size() == 1) {
                CredentialConfig config = configService.readConfig(configNames.get(0));
                MinioClient minioClient = HelperBeans.buildMinioClient(config);
                s3Service = new S3StorageService(minioClient);
                log.info("Using S3 configuration: {}", configNames.get(0));
            } else {
                s3Service = new S3StorageService();
                log.info("Using default S3 configuration");
            }
            var buckets = s3Service.listBuckets();
            bucketListView.getItems().setAll(buckets);
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
package net.talaatharb.s3.ui.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
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
        
        // Initialize services
        S3StorageService s3Service = new S3StorageService();
        try {
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
package net.talaatharb.s3.ui.controllers;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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

    @Getter(value = AccessLevel.PACKAGE)
    @Setter(value = AccessLevel.PACKAGE)
    @FXML
    private VBox tasksVbox;

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
            if (configService == null) {
                configService = new CredentialConfigService();
            }
            var configNames = configService.listConfigs();
            // Fill configCombo with available credential configurations
            if (configCombo != null) {
                configCombo.setItems(FXCollections.observableArrayList(configNames));
                if (configNames.size() == 1) {
                    configCombo.getSelectionModel().select(0);
                }
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

    @FXML
    public void onConfigSelected() {
        String selectedConfigName = configCombo.getSelectionModel().getSelectedItem();
        if (selectedConfigName != null) {
            try {
                var config = configService.readConfig(selectedConfigName);
                var minioClient = configService.getS3Client(config);
                s3Service = new S3StorageService(minioClient);
                log.info("Switched to S3 configuration: {}", selectedConfigName);
                // Refresh bucket list
                var buckets = s3Service.listBuckets();
                bucketListView.getItems().setAll(buckets);
                objectListView.getItems().clear();
            } catch (Exception e) {
                log.error("Failed to switch S3 configuration to: " + selectedConfigName, e);
            }
        }
    }

    @FXML
    public void onDownloadObject() {
        String selectedBucket = bucketListView.getSelectionModel().getSelectedItem();
        String selectedObject = objectListView.getSelectionModel().getSelectedItem();
        if (selectedBucket != null && selectedObject != null) {
            try {
                var downloadTask = s3Service.downloadFile(selectedBucket, selectedObject);
                log.info("Downloading object: {} from bucket: {}", selectedObject, selectedBucket);
                tasksVbox.getChildren().add(createTaskPane(downloadTask, "Downloading " + selectedObject));

                // Handle completion
                downloadTask.thenAccept(inputStream -> {
                    log.info("Download completed for object: {} from bucket: {}", selectedObject, selectedBucket);
                    // Save to downloads folder
                    String downloadsDir = "./downloads/";
                    var outputPath = Paths.get(downloadsDir, selectedObject);

                    try {
                        Files.createDirectories(outputPath.getParent());
                        Files.write(outputPath, inputStream.readAllBytes());
                    } catch (IOException e) {
                        log.error("Failed to save downloaded object: " + selectedObject + " to downloads folder", e);
                    }

                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        log.error("Failed to close input stream for downloaded object: " + selectedObject, e);
                    }
                });
            } catch (Exception e) {
                log.error("Failed to download object: " + selectedObject + " from bucket: " + selectedBucket, e);
            }
        }
    }

    @FXML
    public void onUploadFile() {
        String selectedBucket = bucketListView.getSelectionModel().getSelectedItem();
        if (selectedBucket != null) {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Select File to Upload");
            java.io.File file = fileChooser.showOpenDialog(primaryStage);
            
            if (file != null) {
                String objectName = file.getName();
                var uploadTask = s3Service.uploadFileFromPath(selectedBucket, objectName, file.toPath());
                log.info("Uploading file: {} to bucket: {}", objectName, selectedBucket);
                tasksVbox.getChildren().add(createUploadTaskPane(uploadTask, "Uploading " + objectName));

                // Handle completion and refresh object list
                uploadTask.whenComplete((_, throwable) -> {
                    if (throwable == null) {
                        Platform.runLater(() -> {
                            try {
                                var objects = s3Service.listObjects(selectedBucket, "");
                                objectListView.setItems(FXCollections.observableArrayList(objects));
                            } catch (Exception e) {
                                log.error("Failed to refresh object list after upload", e);
                            }
                        });
                    }
                });
            }
        }
    }

    @FXML
    public void onUploadFolder() {
        String selectedBucket = bucketListView.getSelectionModel().getSelectedItem();
        if (selectedBucket != null) {
            javafx.stage.DirectoryChooser directoryChooser = new javafx.stage.DirectoryChooser();
            directoryChooser.setTitle("Select Folder to Upload");
            java.io.File folder = directoryChooser.showDialog(primaryStage);
            
            if (folder != null) {
                String folderName = folder.getName();
                var uploadTask = s3Service.uploadFolder(selectedBucket, folder.toPath(), folderName);
                log.info("Uploading folder: {} to bucket: {}", folderName, selectedBucket);
                tasksVbox.getChildren().add(createUploadTaskPane(uploadTask, "Uploading folder " + folderName));

                // Handle completion and refresh object list
                uploadTask.whenComplete((_, throwable) -> {
                    if (throwable == null) {
                        Platform.runLater(() -> {
                            try {
                                var objects = s3Service.listObjects(selectedBucket, "");
                                objectListView.setItems(FXCollections.observableArrayList(objects));
                            } catch (Exception e) {
                                log.error("Failed to refresh object list after upload", e);
                            }
                        });
                    }
                });
            }
        }
    }

    private Node createTaskPane(CompletableFuture<InputStream> downloadTask, String description) {
        var hbox = new HBox(10);
        var label = new Label(description);
        var progressIndicator = new ProgressIndicator();
        hbox.getChildren().addAll(label, progressIndicator);
        hbox.setUserData(downloadTask);

        // Handle completion and failure
        downloadTask.whenComplete((_, throwable) -> Platform.runLater(() -> {
            if (throwable != null) {
                label.setText(description + " - Failed");
                progressIndicator.setProgress(1);
            } else {
                label.setText(description + " - Completed");
                progressIndicator.setProgress(1);
            }
            // Optionally, remove the pane after a short delay
            var pause = new PauseTransition(javafx.util.Duration.seconds(2));
            pause.setOnFinished(_ -> tasksVbox.getChildren().remove(hbox));
            pause.play();
        }));
        return hbox;
    }

    private Node createUploadTaskPane(CompletableFuture<Void> uploadTask, String description) {
        var hbox = new HBox(10);
        var label = new Label(description);
        var progressIndicator = new ProgressIndicator();
        hbox.getChildren().addAll(label, progressIndicator);
        hbox.setUserData(uploadTask);

        // Handle completion and failure
        uploadTask.whenComplete((_, throwable) -> Platform.runLater(() -> {
            if (throwable != null) {
                label.setText(description + " - Failed: " + throwable.getMessage());
                progressIndicator.setProgress(1);
                log.error("Upload failed: " + description, throwable);
            } else {
                label.setText(description + " - Completed");
                progressIndicator.setProgress(1);
            }
            // Optionally, remove the pane after a short delay
            var pause = new PauseTransition(javafx.util.Duration.seconds(2));
            pause.setOnFinished(_ -> tasksVbox.getChildren().remove(hbox));
            pause.play();
        }));
        return hbox;
    }

}
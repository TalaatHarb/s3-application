package net.talaatharb.s3.ui.controllers;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.s3.dto.ObjectBrowserItem;
import net.talaatharb.s3.service.CredentialConfigService;
import net.talaatharb.s3.service.S3StorageService;

@Slf4j
public class MainUiController implements Initializable, SceneManager {

    private static final String DOWNLOADS_DIR = "./downloads";
    private static final String ROOT_PATH_LABEL = "/";

    @Getter(value = AccessLevel.PACKAGE)
    @Setter(value = AccessLevel.PACKAGE)
    @FXML
    private Pane mainContainer;

    @Getter(value = AccessLevel.PACKAGE)
    @Setter(value = AccessLevel.PACKAGE)
    @FXML
    private ListView<String> bucketListView;

    @Getter(value = AccessLevel.PACKAGE)
    @Setter(value = AccessLevel.PACKAGE)
    @FXML
    private ListView<ObjectBrowserItem> objectListView;

    @Getter(value = AccessLevel.PACKAGE)
    @Setter(value = AccessLevel.PACKAGE)
    @FXML
    private ComboBox<String> configCombo;

    @Getter(value = AccessLevel.PACKAGE)
    @Setter(value = AccessLevel.PACKAGE)
    @FXML
    private VBox tasksVbox;

    @FXML
    private Label currentBucketLabel;

    @FXML
    private Label pathLabel;

    @FXML
    private Label selectionInfoLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Label bucketStatsLabel;

    @FXML
    private Label currentFolderStatsLabel;

    @FXML
    private Button upButton;

    @FXML
    private Button downloadButton;

    @FXML
    private Button uploadFileButton;

    @FXML
    private Button uploadFolderButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button newFolderButton;

    @FXML
    private Button newBucketButton;

    @FXML
    private Button deleteBucketButton;

    @FXML
    private ListView<String> activityLogListView;

    private Stage primaryStage;
    private String currentPrefix = "";
    private String loadingBucketName;
    private BrowserTreeNode currentFolderNode;
    private final Map<String, BrowserTreeNode> bucketTrees = new HashMap<>();

    private S3StorageService s3Service;

    @Getter(value = AccessLevel.PACKAGE)
    @Setter(value = AccessLevel.PACKAGE)
    @FXML
    private CredentialConfigService configService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.debug("Initializing UI application Main window controller...");
        configureStaticUi();
        try {
            if (configService == null) {
                configService = new CredentialConfigService();
            }
            var configNames = Optional.ofNullable(configService.listConfigs()).orElse(List.of());
            if (configCombo != null) {
                configCombo.setItems(FXCollections.observableArrayList(configNames));
                if (configNames.size() == 1) {
                    configCombo.getSelectionModel().select(0);
                }
            }
            if (configNames.size() == 1) {
                switchConfiguration(configNames.get(0));
            } else if (s3Service == null) {
                s3Service = new S3StorageService();
                appendActivity("Using default S3-compatible endpoint.");
            }
            refreshBuckets();
            updateBreadcrumb();
            updateActionButtons();
        } catch (Exception e) {
            log.error("Failed to list buckets", e);
            showError("Could not load the S3 workspace.", e);
        }
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
        log.debug("Primary stage set, starting with initial scene");
    }

    @FXML
    public void onConfigSelected() {
        if (configCombo == null) {
            return;
        }

        String selectedConfigName = configCombo.getSelectionModel().getSelectedItem();
        if (selectedConfigName == null || selectedConfigName.isBlank()) {
            return;
        }

        try {
            switchConfiguration(selectedConfigName);
            refreshBuckets();
        } catch (Exception e) {
            log.error("Failed to switch S3 configuration to: {}", selectedConfigName, e);
            showError("Could not switch to the selected connection.", e);
        }
    }

    @FXML
    public void onDownloadObject() {
        String selectedBucket = getSelectedBucket();
        ObjectBrowserItem selectedItem = getSelectedObject();
        if (selectedBucket == null || selectedItem == null) {
            updateStatus("Select a file or folder to download.");
            return;
        }

        try {
            if (selectedItem.folder()) {
                Path localFolder = Paths.get(DOWNLOADS_DIR, selectedItem.name());
                CompletableFuture<Void> downloadTask = s3Service.downloadFolder(selectedBucket, selectedItem.objectKey(),
                        localFolder);
                addTaskPane(createTaskPane(downloadTask, "Downloading folder " + selectedItem.name()));
                downloadTask.whenComplete((unused, throwable) -> {
                    if (throwable == null) {
                        appendActivity("Downloaded folder " + selectedItem.objectKey() + " to " + localFolder + '.');
                    }
                });
            } else {
                CompletableFuture<InputStream> downloadTask = s3Service.downloadFile(selectedBucket,
                        selectedItem.objectKey());
                addTaskPane(createTaskPane(downloadTask, "Downloading " + selectedItem.name()));
                downloadTask.thenAccept(inputStream -> saveDownloadedFile(selectedItem.objectKey(), inputStream));
                downloadTask.whenComplete((unused, throwable) -> {
                    if (throwable == null) {
                        appendActivity("Downloaded file " + selectedItem.objectKey() + '.');
                    }
                });
            }
        } catch (Exception e) {
            log.error("Failed to download selection from bucket: {}", selectedBucket, e);
            showError("Could not download the selected item.", e);
        }
    }

    @FXML
    public void onUploadFile() {
        String selectedBucket = getSelectedBucket();
        if (selectedBucket == null) {
            updateStatus("Select a bucket before uploading files.");
            return;
        }

        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Select File to Upload");
        java.io.File file = fileChooser.showOpenDialog(primaryStage);

        if (file != null) {
            String objectName = buildObjectKey(file.getName());
            CompletableFuture<Void> uploadTask = s3Service.uploadFileFromPath(selectedBucket, objectName, file.toPath());
            addTaskPane(createTaskPane(uploadTask, "Uploading " + objectName));
            uploadTask.whenComplete((unused, throwable) -> handleAsyncRefresh(selectedBucket, throwable,
                    "Uploaded file " + objectName + '.'));
        }
    }

    @FXML
    public void onUploadFolder() {
        String selectedBucket = getSelectedBucket();
        if (selectedBucket == null) {
            updateStatus("Select a bucket before uploading folders.");
            return;
        }

        javafx.stage.DirectoryChooser directoryChooser = new javafx.stage.DirectoryChooser();
        directoryChooser.setTitle("Select Folder to Upload");
        java.io.File folder = directoryChooser.showDialog(primaryStage);

        if (folder != null) {
            String folderKey = buildFolderKey(folder.getName());
            CompletableFuture<Void> uploadTask = s3Service.uploadFolder(selectedBucket, folder.toPath(), folderKey);
            addTaskPane(createTaskPane(uploadTask, "Uploading folder " + folder.getName()));
            uploadTask.whenComplete((unused, throwable) -> handleAsyncRefresh(selectedBucket, throwable,
                    "Uploaded folder " + folderKey + '.'));
        }
    }

    @FXML
    public void onDeleteSelection() {
        String selectedBucket = getSelectedBucket();
        ObjectBrowserItem selectedItem = getSelectedObject();
        if (selectedBucket == null || selectedItem == null) {
            updateStatus("Select a file or folder to delete.");
            return;
        }

        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        prepareDialog(confirmationAlert);
        confirmationAlert.setTitle("Delete item");
        confirmationAlert.setHeaderText("Delete " + selectedItem.name() + "?");
        confirmationAlert.setContentText(selectedItem.folder()
                ? "This will remove the folder and everything inside it from the current bucket."
                : "This will permanently remove the selected file from the current bucket.");

        Optional<ButtonType> decision = confirmationAlert.showAndWait();
        if (decision.isEmpty() || decision.get() != ButtonType.OK) {
            return;
        }

        CompletableFuture<Void> deleteTask = selectedItem.folder()
                ? s3Service.deletePrefix(selectedBucket, selectedItem.objectKey())
                : CompletableFuture.runAsync(() -> {
                    try {
                        s3Service.deleteObject(selectedBucket, selectedItem.objectKey());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
        addTaskPane(createTaskPane(deleteTask, "Deleting " + selectedItem.name()));
        deleteTask.whenComplete((unused, throwable) -> handleAsyncRefresh(selectedBucket, throwable,
                "Deleted " + selectedItem.objectKey() + '.'));
    }

    @FXML
    public void onNavigateUp() {
        if (currentPrefix == null || currentPrefix.isBlank()) {
            return;
        }

        if (currentFolderNode != null && currentFolderNode.parent() != null) {
            currentFolderNode = currentFolderNode.parent();
            currentPrefix = currentFolderNode.objectKey();
            renderCurrentFolder();
            return;
        }

        String trimmedPrefix = currentPrefix.endsWith("/")
                ? currentPrefix.substring(0, currentPrefix.length() - 1)
                : currentPrefix;
        int lastSlash = trimmedPrefix.lastIndexOf('/');
        currentPrefix = lastSlash < 0 ? "" : trimmedPrefix.substring(0, lastSlash + 1);
        refreshObjects();
    }

    @FXML
    public void onRefresh() {
        String selectedBucket = getSelectedBucket();
        if (selectedBucket != null) {
            bucketTrees.remove(selectedBucket);
            currentFolderNode = null;
        }
        refreshBuckets();
        if (getSelectedBucket() != null) {
            refreshObjects();
        }
    }

    @FXML
    public void onAddConnection() {
        showConnectionDialog().ifPresent(connectionFormData -> {
            try {
                configService.addConfig(connectionFormData.configName(), connectionFormData.endpoint(),
                        connectionFormData.accessKey(), connectionFormData.secretKey());
                reloadConfigProfiles(connectionFormData.configName());
                switchConfiguration(connectionFormData.configName());
                refreshBuckets();
                appendActivity("Added connection profile " + connectionFormData.configName() + '.');
            } catch (Exception e) {
                log.error("Failed to add connection profile: {}", connectionFormData.configName(), e);
                showError("Could not add the connection profile.", e);
            }
        });
    }

    @FXML
    public void onDeleteConnection() {
        if (configCombo == null) {
            return;
        }

        String selectedConfigName = configCombo.getSelectionModel().getSelectedItem();
        if (selectedConfigName == null || selectedConfigName.isBlank()) {
            updateStatus("Select a connection profile to remove.");
            return;
        }

        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        prepareDialog(confirmationAlert);
        confirmationAlert.setTitle("Remove connection");
        confirmationAlert.setHeaderText("Remove connection profile " + selectedConfigName + "?");
        confirmationAlert.setContentText("This removes the saved credentials profile from the application.");

        Optional<ButtonType> decision = confirmationAlert.showAndWait();
        if (decision.isEmpty() || decision.get() != ButtonType.OK) {
            return;
        }

        try {
            configService.deleteConfig(selectedConfigName);
            bucketTrees.clear();
            currentFolderNode = null;
            currentPrefix = "";
            reloadConfigProfiles(null);

            List<String> remainingProfiles = Optional.ofNullable(configService.listConfigs()).orElse(List.of());
            if (remainingProfiles.size() == 1) {
                switchConfiguration(remainingProfiles.get(0));
            } else if (!remainingProfiles.isEmpty()) {
                switchConfiguration(remainingProfiles.get(0));
                if (configCombo != null) {
                    configCombo.getSelectionModel().select(remainingProfiles.get(0));
                }
            } else {
                s3Service = new S3StorageService();
                appendActivity("Using default S3-compatible endpoint.");
            }

            refreshBuckets();
            appendActivity("Removed connection profile " + selectedConfigName + '.');
        } catch (Exception e) {
            log.error("Failed to remove connection profile: {}", selectedConfigName, e);
            showError("Could not remove the connection profile.", e);
        }
    }

    @FXML
    public void onCreateFolder() {
        String selectedBucket = getSelectedBucket();
        if (selectedBucket == null) {
            updateStatus("Select a bucket before creating a folder.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        prepareDialog(dialog);
        dialog.setTitle("Create folder");
        dialog.setHeaderText("Create a new folder in " + displayCurrentPath());
        dialog.setContentText("Folder name:");

        dialog.showAndWait()
                .map(String::trim)
                .filter(name -> !name.isBlank())
                .ifPresent(folderName -> {
                    String folderKey = buildFolderKey(folderName);
                    CompletableFuture<Void> createFolderTask = s3Service.createFolder(selectedBucket, folderKey);
                    addTaskPane(createTaskPane(createFolderTask, "Creating folder " + folderName));
                    createFolderTask.whenComplete((unused, throwable) -> handleAsyncRefresh(selectedBucket, throwable,
                            "Created folder " + folderKey + '.'));
                });
    }

    @FXML
    public void onCreateBucket() {
        TextInputDialog dialog = new TextInputDialog();
        prepareDialog(dialog);
        dialog.setTitle("Create bucket");
        dialog.setHeaderText("Create a new bucket");
        dialog.setContentText("Bucket name:");

        dialog.showAndWait()
                .map(String::trim)
                .filter(name -> !name.isBlank())
                .ifPresent(bucketName -> {
                    CompletableFuture<Void> createBucketTask = s3Service.createBucket(bucketName);
                    addTaskPane(createTaskPane(createBucketTask, "Creating bucket " + bucketName));
                    createBucketTask.whenComplete((unused, throwable) -> Platform.runLater(() -> {
                        if (throwable != null) {
                            showError("Could not create bucket.", throwable);
                            return;
                        }

                        bucketTrees.remove(bucketName);
                        appendActivity("Created bucket " + bucketName + '.');
                        refreshBuckets();
                        if (bucketListView != null) {
                            bucketListView.getSelectionModel().select(bucketName);
                        }
                    }));
                });
    }

    @FXML
    public void onDeleteBucket() {
        String selectedBucket = getSelectedBucket();
        if (selectedBucket == null || selectedBucket.isBlank()) {
            updateStatus("Select a bucket to delete.");
            return;
        }

        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        prepareDialog(confirmationAlert);
        confirmationAlert.setTitle("Delete bucket");
        confirmationAlert.setHeaderText("Delete bucket " + selectedBucket + "?");
        confirmationAlert.setContentText("You can delete only the bucket or remove all contents and the bucket.");

        CheckBox recursiveDeleteCheckBox = new CheckBox("Delete all files and folders inside the bucket first");
        recursiveDeleteCheckBox.setSelected(true);
        DialogPane dialogPane = confirmationAlert.getDialogPane();
        dialogPane.setExpandableContent(recursiveDeleteCheckBox);
        dialogPane.setExpanded(true);

        Optional<ButtonType> decision = confirmationAlert.showAndWait();
        if (decision.isEmpty() || decision.get() != ButtonType.OK) {
            return;
        }

        CompletableFuture<Void> deleteBucketTask = s3Service.deleteBucketAsync(selectedBucket,
                recursiveDeleteCheckBox.isSelected());
        addTaskPane(createTaskPane(deleteBucketTask, "Deleting bucket " + selectedBucket));
        deleteBucketTask.whenComplete((unused, throwable) -> Platform.runLater(() -> {
            if (throwable != null) {
                showError("Could not delete bucket.", throwable);
                return;
            }

            bucketTrees.remove(selectedBucket);
            currentFolderNode = null;
            currentPrefix = "";
            appendActivity("Deleted bucket " + selectedBucket + '.');
            refreshBuckets();
        }));
    }

    @FXML
    public void onCloseApplication() {
        if (primaryStage != null) {
            primaryStage.close();
        }
    }

    @FXML
    public void onShowAboutDialog() {
        Alert aboutAlert = new Alert(Alert.AlertType.INFORMATION);
        prepareDialog(aboutAlert);
        aboutAlert.setTitle("About");
        aboutAlert.setHeaderText("S3 Browser");
        aboutAlert.setContentText(
                "A JavaFX desktop client for S3-compatible object storage powered by the MinIO Java SDK.");
        aboutAlert.showAndWait();
    }

    private void configureStaticUi() {
        if (bucketListView != null) {
            bucketListView.setPlaceholder(new Label("No buckets available"));
            bucketListView.setCellFactory(listView -> new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : "🪣  " + item);
                }
            });
            bucketListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                currentPrefix = "";
                refreshObjects();
            });
        }

        if (objectListView != null) {
            objectListView.setPlaceholder(new Label("No files or folders in this location"));
            objectListView.setCellFactory(listView -> createBrowserItemCell());
            objectListView.getSelectionModel().selectedItemProperty()
                    .addListener((observable, oldValue, newValue) -> updateActionButtons());
            objectListView.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    openSelectedItem();
                }
            });
            objectListView.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    openSelectedItem();
                } else if (event.getCode() == KeyCode.BACK_SPACE) {
                    onNavigateUp();
                }
            });
        }

        if (activityLogListView != null) {
            activityLogListView.setPlaceholder(new Label("Activity will appear here"));
        }

        updateStatus("Ready.");
        updateStats(null, null);
        updateBreadcrumb();
        updateActionButtons();
    }

    private ListCell<ObjectBrowserItem> createBrowserItemCell() {
        return new ListCell<>() {
            private final HBox container = new HBox(12);
            private final Label iconLabel = new Label();
            private final VBox textBox = new VBox(2);
            private final Label nameLabel = new Label();
            private final Label typeLabel = new Label();
            private final Label sizeLabel = new Label();
            {
                iconLabel.getStyleClass().add("item-icon");
                nameLabel.getStyleClass().add("item-name");
                typeLabel.getStyleClass().add("item-meta");
                sizeLabel.getStyleClass().add("item-size");
                textBox.getChildren().addAll(nameLabel, typeLabel);
                HBox.setHgrow(textBox, Priority.ALWAYS);
                container.setAlignment(Pos.CENTER_LEFT);
                container.getChildren().addAll(iconLabel, textBox, sizeLabel);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }

            @Override
            protected void updateItem(ObjectBrowserItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }

                iconLabel.setText(item.folder() ? "📁" : "📄");
                nameLabel.setText(item.name());
                typeLabel.setText(item.folder()
                    ? formatItemCount(item.directChildCount(), "direct") + " · "
                        + formatItemCount(item.itemCount(), "total")
                    : item.objectKey());
                sizeLabel.setText(item.folder() ? "" : formatBytes(item.size()));
                getStyleClass().removeAll("folder-item", "file-item");
                getStyleClass().add(item.folder() ? "folder-item" : "file-item");
                setGraphic(container);
            }
        };
    }

    private void switchConfiguration(String configName) throws Exception {
        var config = configService.readConfig(configName);
        s3Service = new S3StorageService(configService.getS3Client(config));
        bucketTrees.clear();
        currentFolderNode = null;
        currentPrefix = "";
        appendActivity("Connected using profile " + configName + '.');
    }

    private void refreshBuckets() {
        if (bucketListView == null || s3Service == null) {
            return;
        }

        try {
            List<String> buckets = s3Service.listBuckets();
            String previousSelection = bucketListView.getSelectionModel().getSelectedItem();
            bucketListView.getItems().setAll(buckets);
            if (previousSelection != null && buckets.contains(previousSelection)) {
                bucketListView.getSelectionModel().select(previousSelection);
            } else if (!buckets.isEmpty()) {
                bucketListView.getSelectionModel().select(0);
            } else {
                clearObjectView();
            }
            updateStatus(buckets.isEmpty() ? "No buckets found." : "Loaded " + buckets.size() + " bucket(s).");
        } catch (Exception e) {
            log.error("Failed to refresh buckets", e);
            clearObjectView();
            showError("Could not load buckets from the current connection.", e);
        }
    }

    private void refreshObjects() {
        if (objectListView == null) {
            return;
        }

        String selectedBucket = getSelectedBucket();
        currentBucketLabelSafe(selectedBucket == null ? "No bucket selected" : selectedBucket);
        if (selectedBucket == null || s3Service == null) {
            clearObjectView();
            return;
        }

        BrowserTreeNode rootNode = bucketTrees.get(selectedBucket);
        if (rootNode == null) {
            loadBucketTree(selectedBucket);
            return;
        }

        BrowserTreeNode resolvedNode = resolveNode(rootNode, currentPrefix);
        if (resolvedNode == null) {
            currentPrefix = "";
            currentFolderNode = rootNode;
            resolvedNode = rootNode;
        }

        currentFolderNode = resolvedNode;
        renderCurrentFolder();
    }

    private void clearObjectView() {
        currentPrefix = "";
        currentFolderNode = null;
        if (objectListView != null) {
            objectListView.getItems().clear();
        }
        updateStats(null, null);
        updateBreadcrumb();
        updateActionButtons();
        currentBucketLabelSafe("No bucket selected");
    }

    private void openSelectedItem() {
        ObjectBrowserItem selectedItem = getSelectedObject();
        if (selectedItem == null) {
            return;
        }

        if (selectedItem.folder()) {
            currentPrefix = selectedItem.objectKey();
            if (currentFolderNode != null) {
                currentFolderNode = currentFolderNode.children().get(selectedItem.name());
            }
            renderCurrentFolder();
        } else {
            onDownloadObject();
        }
    }

    private void updateBreadcrumb() {
        if (pathLabel != null) {
            pathLabel.setText(displayCurrentPath());
        }
        if (upButton != null) {
            upButton.setDisable(currentPrefix == null || currentPrefix.isBlank());
        }
    }

    private void updateActionButtons() {
        ObjectBrowserItem selectedItem = getSelectedObject();
        boolean hasBucket = getSelectedBucket() != null;
        boolean hasSelection = selectedItem != null;

        if (downloadButton != null) {
            downloadButton.setDisable(!hasSelection);
        }
        if (uploadFileButton != null) {
            uploadFileButton.setDisable(!hasBucket);
        }
        if (uploadFolderButton != null) {
            uploadFolderButton.setDisable(!hasBucket);
        }
        if (deleteButton != null) {
            deleteButton.setDisable(!hasSelection);
        }
        if (newFolderButton != null) {
            newFolderButton.setDisable(!hasBucket);
        }
        if (newBucketButton != null) {
            newBucketButton.setDisable(s3Service == null);
        }
        if (deleteBucketButton != null) {
            deleteBucketButton.setDisable(!hasBucket);
        }
        if (selectionInfoLabel != null) {
            selectionInfoLabel.setText(hasSelection
                    ? (selectedItem.folder() ? "Folder selected: " : "File selected: ") + selectedItem.objectKey()
                    : "Select a bucket and browse folders like a file manager.");
        }
    }

    private String getSelectedBucket() {
        return bucketListView == null ? null : bucketListView.getSelectionModel().getSelectedItem();
    }

    private ObjectBrowserItem getSelectedObject() {
        return objectListView == null ? null : objectListView.getSelectionModel().getSelectedItem();
    }

    private void handleAsyncRefresh(String bucketName, Throwable throwable, String successMessage) {
        if (throwable != null) {
            log.error("Background operation failed", throwable);
            Platform.runLater(() -> showError("The requested operation failed.", throwable));
            return;
        }

        Platform.runLater(() -> {
            bucketTrees.remove(bucketName);
            currentFolderNode = null;
            appendActivity(successMessage);
            if (bucketName.equals(getSelectedBucket())) {
                refreshObjects();
            }
        });
    }

    private void saveDownloadedFile(String objectKey, InputStream inputStream) {
        Path outputPath = Paths.get(DOWNLOADS_DIR, objectKey);
        try (InputStream stream = inputStream) {
            Files.createDirectories(outputPath.getParent());
            Files.write(outputPath, stream.readAllBytes());
        } catch (IOException e) {
            log.error("Failed to save downloaded file: {}", objectKey, e);
            Platform.runLater(() -> showError("Could not save the downloaded file.", e));
        }
    }

    private void addTaskPane(Node taskPane) {
        if (tasksVbox != null) {
            tasksVbox.getChildren().add(taskPane);
        }
    }

    private Node createTaskPane(CompletableFuture<?> task, String description) {
        var hbox = new HBox(10);
        hbox.getStyleClass().add("task-item");
        var label = new Label(description);
        label.getStyleClass().add("task-label");
        var progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(20, 20);
        hbox.getChildren().addAll(label, progressIndicator);
        hbox.setUserData(task);

        task.whenComplete((unused, throwable) -> Platform.runLater(() -> {
            if (throwable != null) {
                label.setText(description + " · failed");
                progressIndicator.setProgress(1);
            } else {
                label.setText(description + " · completed");
                progressIndicator.setProgress(1);
            }
            var pause = new PauseTransition(javafx.util.Duration.seconds(2));
            pause.setOnFinished(event -> {
                if (tasksVbox != null) {
                    tasksVbox.getChildren().remove(hbox);
                }
            });
            pause.play();
        }));
        return hbox;
    }

    private String buildObjectKey(String fileName) {
        return (currentPrefix == null ? "" : currentPrefix) + fileName;
    }

    private String buildFolderKey(String folderName) {
        return buildObjectKey(folderName) + '/';
    }

    private String displayCurrentPath() {
        return currentPrefix == null || currentPrefix.isBlank() ? ROOT_PATH_LABEL : '/' + currentPrefix;
    }

    private void appendActivity(String message) {
        log.info(message);
        if (activityLogListView != null) {
            Platform.runLater(() -> {
                activityLogListView.getItems().add(0, message);
                if (activityLogListView.getItems().size() > 200) {
                    activityLogListView.getItems().remove(200, activityLogListView.getItems().size());
                }
            });
        }
        updateStatus(message);
    }

    private void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }

    private void showError(String message, Throwable throwable) {
        String details = throwable == null ? "" : throwable.getMessage();
        updateStatus(message + (details == null || details.isBlank() ? "" : " " + details));
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> showError(message, throwable));
            return;
        }

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Operation failed");
        alert.setHeaderText(message);
        alert.setContentText(details == null || details.isBlank() ? "See logs for more details." : details);
        alert.show();
    }

    private void currentBucketLabelSafe(String value) {
        if (currentBucketLabel != null) {
            currentBucketLabel.setText(value);
        }
    }

    private String formatBytes(long size) {
        if (size < 1024) {
            return size + " B";
        }

        double value = size;
        String[] units = { "KB", "MB", "GB", "TB" };
        int unitIndex = -1;
        while (value >= 1024 && unitIndex < units.length - 1) {
            value /= 1024;
            unitIndex++;
        }
        return String.format("%.1f %s", value, units[unitIndex]);
    }

    private void loadBucketTree(String bucketName) {
        loadingBucketName = bucketName;
        if (objectListView != null) {
            objectListView.getItems().clear();
            objectListView.setPlaceholder(new Label("Loading bucket contents..."));
        }
        updateStatus("Loading bucket contents for " + bucketName + "...");

        CompletableFuture<BrowserTreeNode> loadTask = CompletableFuture.supplyAsync(() -> {
            try {
                return buildBucketTree(bucketName, s3Service.listAllObjectItems(bucketName));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        addTaskPane(createTaskPane(loadTask, "Loading bucket " + bucketName));
        loadTask.whenComplete((tree, throwable) -> Platform.runLater(() -> {
            if (throwable != null) {
                log.error("Failed to build bucket tree for {}", bucketName, throwable);
                showError("Could not load the selected bucket.", throwable);
                if (objectListView != null) {
                    objectListView.setPlaceholder(new Label("No files or folders in this location"));
                }
                return;
            }

            bucketTrees.put(bucketName, tree);
            if (!bucketName.equals(getSelectedBucket()) || !bucketName.equals(loadingBucketName)) {
                return;
            }

            currentFolderNode = resolveNode(tree, currentPrefix);
            if (currentFolderNode == null) {
                currentPrefix = "";
                currentFolderNode = tree;
            }
            if (objectListView != null) {
                objectListView.setPlaceholder(new Label("No files or folders in this location"));
            }
            appendActivity("Loaded bucket index for " + bucketName + '.');
            renderCurrentFolder();
        }));
    }

    private void renderCurrentFolder() {
        if (objectListView == null) {
            return;
        }

        if (currentFolderNode == null) {
            objectListView.getItems().clear();
            updateBreadcrumb();
            updateActionButtons();
            return;
        }

        List<ObjectBrowserItem> children = new ArrayList<>(currentFolderNode.children().values().stream()
                .map(BrowserTreeNode::toBrowserItem)
                .sorted((left, right) -> {
                    if (left.folder() != right.folder()) {
                        return left.folder() ? -1 : 1;
                    }
                    return left.name().compareToIgnoreCase(right.name());
                })
                .toList());
        objectListView.getItems().setAll(children);
        currentPrefix = currentFolderNode.objectKey();
            BrowserTreeNode bucketRoot = bucketTrees.get(getSelectedBucket());
            updateStats(bucketRoot, currentFolderNode);
        updateBreadcrumb();
        updateActionButtons();
        updateStatus("Showing " + children.size() + " item(s) in " + displayCurrentPath() + '.');
    }

    private BrowserTreeNode buildBucketTree(String bucketName, List<ObjectBrowserItem> objectItems) {
        BrowserTreeNode root = BrowserTreeNode.root(bucketName);
        for (ObjectBrowserItem item : objectItems) {
            if (item.folder()) {
                ensureFolderNode(root, item.objectKey());
                continue;
            }

            String[] segments = item.objectKey().split("/");
            BrowserTreeNode currentNode = root;
            StringBuilder pathBuilder = new StringBuilder();
            for (int i = 0; i < segments.length; i++) {
                String segment = segments[i];
                if (segment == null || segment.isBlank()) {
                    continue;
                }

                boolean isLeaf = i == segments.length - 1;
                if (isLeaf) {
                    currentNode.children().put(segment,
                            BrowserTreeNode.file(segment, item.objectKey(), item.size(), currentNode));
                } else {
                    pathBuilder.append(segment).append('/');
                    BrowserTreeNode parentNode = currentNode;
                    currentNode = currentNode.children().computeIfAbsent(segment,
                            key -> BrowserTreeNode.folder(segment, pathBuilder.toString(), parentNode));
                }
            }
        }
        populateCounts(root);
        return root;
    }

    private BrowserTreeNode ensureFolderNode(BrowserTreeNode root, String folderKey) {
        String normalizedKey = folderKey.endsWith("/") ? folderKey.substring(0, folderKey.length() - 1) : folderKey;
        if (normalizedKey.isBlank()) {
            return root;
        }

        String[] segments = normalizedKey.split("/");
        BrowserTreeNode currentNode = root;
        StringBuilder pathBuilder = new StringBuilder();
        for (String segment : segments) {
            if (segment == null || segment.isBlank()) {
                continue;
            }
            pathBuilder.append(segment).append('/');
            BrowserTreeNode parentNode = currentNode;
            currentNode = currentNode.children().computeIfAbsent(segment,
                    key -> BrowserTreeNode.folder(segment, pathBuilder.toString(), parentNode));
        }
        return currentNode;
    }

    private BrowserTreeNode resolveNode(BrowserTreeNode rootNode, String prefix) {
        if (rootNode == null || prefix == null || prefix.isBlank()) {
            return rootNode;
        }

        String normalizedPrefix = prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
        BrowserTreeNode currentNode = rootNode;
        for (String segment : normalizedPrefix.split("/")) {
            if (segment == null || segment.isBlank()) {
                continue;
            }
            currentNode = currentNode.children().get(segment);
            if (currentNode == null) {
                return null;
            }
        }
        return currentNode;
    }

    private int populateCounts(BrowserTreeNode node) {
        int total = 0;
        for (BrowserTreeNode child : node.children().values()) {
            total++;
            if (child.folder()) {
                total += populateCounts(child);
            }
        }
        node.setItemCount(total);
        return total;
    }

    private void reloadConfigProfiles(String preferredSelection) throws Exception {
        if (configCombo == null || configService == null) {
            return;
        }

        List<String> configNames = Optional.ofNullable(configService.listConfigs()).orElse(List.of());
        configCombo.setItems(FXCollections.observableArrayList(configNames));
        if (preferredSelection != null && configNames.contains(preferredSelection)) {
            configCombo.getSelectionModel().select(preferredSelection);
        } else if (!configNames.isEmpty()) {
            configCombo.getSelectionModel().select(0);
        } else {
            configCombo.getSelectionModel().clearSelection();
        }
    }

    private Optional<ConnectionFormData> showConnectionDialog() {
        Dialog<ConnectionFormData> dialog = new Dialog<>();
        prepareDialog(dialog);
        dialog.setTitle("Add connection");
        dialog.setHeaderText("Add a new S3-compatible connection profile");

        ButtonType saveButtonType = new ButtonType("Save", ButtonType.OK.getButtonData());
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField nameField = new TextField();
        nameField.setPromptText("Profile name");
        TextField endpointField = new TextField();
        endpointField.setPromptText("https://endpoint.example.com");
        TextField accessKeyField = new TextField();
        accessKeyField.setPromptText("Access key");
        PasswordField secretKeyField = new PasswordField();
        secretKeyField.setPromptText("Secret key");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Profile"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Endpoint"), 0, 1);
        grid.add(endpointField, 1, 1);
        grid.add(new Label("Access key"), 0, 2);
        grid.add(accessKeyField, 1, 2);
        grid.add(new Label("Secret key"), 0, 3);
        grid.add(secretKeyField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        Runnable validate = () -> saveButton.setDisable(nameField.getText().isBlank() || endpointField.getText().isBlank()
                || accessKeyField.getText().isBlank() || secretKeyField.getText().isBlank());
        nameField.textProperty().addListener((observable, oldValue, newValue) -> validate.run());
        endpointField.textProperty().addListener((observable, oldValue, newValue) -> validate.run());
        accessKeyField.textProperty().addListener((observable, oldValue, newValue) -> validate.run());
        secretKeyField.textProperty().addListener((observable, oldValue, newValue) -> validate.run());
        validate.run();

        dialog.setResultConverter(buttonType -> buttonType == saveButtonType
                ? new ConnectionFormData(nameField.getText().trim(), endpointField.getText().trim(),
                        accessKeyField.getText().trim(), secretKeyField.getText())
                : null);
        return dialog.showAndWait();
    }

    private void updateStats(BrowserTreeNode bucketRoot, BrowserTreeNode folderNode) {
        if (bucketStatsLabel != null) {
            bucketStatsLabel.setText(bucketRoot == null ? "Bucket: 0 items"
                    : "Bucket: " + formatItemCount(bucketRoot.itemCount(), "item"));
        }
        if (currentFolderStatsLabel != null) {
            currentFolderStatsLabel.setText(folderNode == null ? "Here: 0 items"
                    : "Here: " + formatItemCount(folderNode.directChildCount(), "item") + " · inside: "
                            + formatItemCount(folderNode.itemCount(), "item"));
        }
    }

    private String formatItemCount(int count, String label) {
        return count + " " + label + (count == 1 ? "" : "s");
    }

    private void prepareDialog(Dialog<?> dialog) {
        if (dialog == null || dialog.getDialogPane() == null) {
            return;
        }

        URL stylesheetUrl = MainUiController.class.getResource("/net/talaatharb/s3/ui/theme.css");
        if (stylesheetUrl != null) {
            String stylesheet = stylesheetUrl.toExternalForm();
            if (!dialog.getDialogPane().getStylesheets().contains(stylesheet)) {
                dialog.getDialogPane().getStylesheets().add(stylesheet);
            }
        }
        if (!dialog.getDialogPane().getStyleClass().contains("app-dialog")) {
            dialog.getDialogPane().getStyleClass().add("app-dialog");
        }
    }

    private static final class BrowserTreeNode {
        private final String name;
        private final String objectKey;
        private final boolean folder;
        private final long size;
        private final BrowserTreeNode parent;
        private final NavigableMap<String, BrowserTreeNode> children;
        private int itemCount;

        private BrowserTreeNode(String name, String objectKey, boolean folder, long size, BrowserTreeNode parent) {
            this.name = name;
            this.objectKey = objectKey;
            this.folder = folder;
            this.size = size;
            this.parent = parent;
            this.children = folder ? new TreeMap<>(String.CASE_INSENSITIVE_ORDER) : new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        }

        static BrowserTreeNode root(String bucketName) {
            return new BrowserTreeNode(bucketName, "", true, 0L, null);
        }

        static BrowserTreeNode folder(String name, String objectKey, BrowserTreeNode parent) {
            return new BrowserTreeNode(name, objectKey, true, 0L, parent);
        }

        static BrowserTreeNode file(String name, String objectKey, long size, BrowserTreeNode parent) {
            return new BrowserTreeNode(name, objectKey, false, size, parent);
        }

        String name() {
            return name;
        }

        String objectKey() {
            return objectKey;
        }

        NavigableMap<String, BrowserTreeNode> children() {
            return children;
        }

        boolean folder() {
            return folder;
        }

        BrowserTreeNode parent() {
            return parent;
        }

        int itemCount() {
            return itemCount;
        }

        int directChildCount() {
            return children.size();
        }

        void setItemCount(int itemCount) {
            this.itemCount = itemCount;
        }

        ObjectBrowserItem toBrowserItem() {
            return new ObjectBrowserItem(name, objectKey, folder, size, itemCount, directChildCount());
        }
    }

    private record ConnectionFormData(String configName, String endpoint, String accessKey, String secretKey) {
    }

}
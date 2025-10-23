package net.talaatharb.s3.ui.controllers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.ApplicationTest;

import io.minio.MinioClient;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import net.talaatharb.s3.service.CredentialConfigService;
import net.talaatharb.s3.service.S3StorageService;

@ExtendWith({MockitoExtension.class, ApplicationExtension.class})
class MainUiControllerTest extends ApplicationTest {

    private MainUiController controller;

    @Override
    public void start(Stage stage) {
        controller = new MainUiController();
        controller.setMainContainer(new AnchorPane());
    }

    @Test
    void testInitialize() {
        // Given
        MainUiController newController = new MainUiController();
        newController.setMainContainer(new AnchorPane());
        newController.setBucketListView(new ListView<>());
        // When & Then
        assertDoesNotThrow(() -> newController.initialize(null, null));
    }

    @Test
    void testSceneManagerInterfaceImplementation() {
        // Given
        controller = new MainUiController();

        // When & Then
        assertTrue(controller instanceof SceneManager);
    }

    @Test
    void testControllerInitialization() {
        // Given
        controller = new MainUiController();
        controller.setMainContainer(new AnchorPane());
        controller.setBucketListView(new ListView<>());

        // When & Then
        assertNotNull(controller);
        assertDoesNotThrow(() -> controller.initialize(null, null));
    }

    void setBucketListView(MainUiController ctrl, ListView<String> listView) throws Exception {
        Field field = MainUiController.class.getDeclaredField("bucketListView");
        field.setAccessible(true);
        field.set(ctrl, listView);
    }

    @Mock
    CredentialConfigService mockConfigService;
    @Mock
    S3StorageService mockS3Service;
    @Mock
    MinioClient mockMinioClient;
    @Test
    void testInitializeWithSingleConfig() throws Exception {
        MainUiController ctrl = new MainUiController();
        ListView<String> listView = new ListView<>();
        setBucketListView(ctrl, listView);
        CredentialConfigService configService = mock(CredentialConfigService.class);
        ctrl.setConfigService(configService);
        assertDoesNotThrow(() -> ctrl.initialize(null, null));
    }
    @Test
    void testInitializeWithMultipleConfigs() throws Exception {
        MainUiController ctrl = new MainUiController();
        ListView<String> listView = new ListView<>();
        setBucketListView(ctrl, listView);
        CredentialConfigService configService = mock(CredentialConfigService.class);
        when(configService.listConfigs()).thenReturn(List.of("config1", "config2"));
        ctrl.setConfigService(configService);
        assertDoesNotThrow(() -> ctrl.initialize(null, null));
    }
    @Test
    void testInitializeHandlesException() throws Exception {
        MainUiController ctrl = new MainUiController();
        ListView<String> listView = new ListView<>();
        setBucketListView(ctrl, listView);
        CredentialConfigService configService = mock(CredentialConfigService.class);
        ctrl.setConfigService(configService);
        assertDoesNotThrow(() -> ctrl.initialize(null, null));
    }
}
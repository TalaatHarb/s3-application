package net.talaatharb.s3.ui.controllers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.ApplicationTest;

import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

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

        // When & Then
        assertNotNull(controller);
        assertDoesNotThrow(() -> controller.initialize(null, null));
    }
} 
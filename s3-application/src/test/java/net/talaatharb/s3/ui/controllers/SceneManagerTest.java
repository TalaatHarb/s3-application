package net.talaatharb.s3.ui.controllers;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.ApplicationTest;

import javafx.stage.Stage;

@ExtendWith({MockitoExtension.class, ApplicationExtension.class})
class SceneManagerTest extends ApplicationTest {

    @Mock
    private SceneManager mockSceneManager;

    @Override
    public void start(Stage stage) {
        // Setup for JavaFX tests
    }

    @Test
    void testSceneManagerInterfaceImplementation() {
        // Given
        MainUiController controller = new MainUiController();

        // When & Then
        assertTrue(controller instanceof SceneManager);
        assertNotNull(controller);
    }
} 
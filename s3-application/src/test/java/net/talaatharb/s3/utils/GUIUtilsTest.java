package net.talaatharb.s3.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import javafx.scene.layout.AnchorPane;

@ExtendWith(ApplicationExtension.class)
class GUIUtilsTest {
    @Test
    void testSetAnchorAllDirections() {
        AnchorPane pane = new AnchorPane();
        GUIUtils.setAnchorAllDirections(pane, 10.0);
        assertEquals(10.0, AnchorPane.getBottomAnchor(pane));
        assertEquals(10.0, AnchorPane.getLeftAnchor(pane));
        assertEquals(10.0, AnchorPane.getRightAnchor(pane));
        assertEquals(10.0, AnchorPane.getTopAnchor(pane));
    }

    @Test
    void testSetAnchorZero() {
        AnchorPane pane = new AnchorPane();
        GUIUtils.setAnchorZero(pane);
        assertEquals(0.0, AnchorPane.getBottomAnchor(pane));
        assertEquals(0.0, AnchorPane.getLeftAnchor(pane));
        assertEquals(0.0, AnchorPane.getRightAnchor(pane));
        assertEquals(0.0, AnchorPane.getTopAnchor(pane));
    }
}
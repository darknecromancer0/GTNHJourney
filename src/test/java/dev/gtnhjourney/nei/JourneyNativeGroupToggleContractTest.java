package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import codechicken.nei.Button;

public class JourneyNativeGroupToggleContractTest {

    @Test
    public void journeyWidgetDoesNotOwnNeisNativeGroupToggle() {
        List<String> journeyButtons = new ArrayList<String>();
        for (Field field : JourneyNEIToggleWidget.class.getDeclaredFields()) {
            if (Button.class.isAssignableFrom(field.getType())) journeyButtons.add(field.getName());
        }
        Collections.sort(journeyButtons);

        List<String> expected = Arrays.asList(
            "debugToolButton",
            "deleteButton",
            "newestButton",
            "researchButton",
            "scanButton");
        assertEquals(expected, journeyButtons);
    }
}

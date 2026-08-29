package dev.gtnhjourney.client;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

public class ChatInputResolverTest {

    @Test
    public void resolvesLiveAssignableFieldFromSuperclassWhenRuntimeClassHasOtherFields() {
        WrappedFixture target = new WrappedFixture();

        Field resolved = ChatInputResolver.findLiveAssignableField(target, InputMarker.class);

        assertNotNull(resolved);
        assertSame(target.expected(), ChatInputResolver.readAssignable(target, resolved, InputMarker.class));
    }

    @Test
    public void ignoresNullAssignableDecoyBeforeSuperclassInput() {
        NullDecoyFixture target = new NullDecoyFixture();

        Field resolved = ChatInputResolver.findLiveAssignableField(target, InputMarker.class);

        assertNotNull(resolved);
        assertSame(target.expected(), ChatInputResolver.readAssignable(target, resolved, InputMarker.class));
    }

    @Test
    public void incompatibleFixtureFailsOpenWithoutThrowing() {
        assertNull(ChatInputResolver.findLiveAssignableField(new Object(), InputMarker.class));
        assertNull(ChatInputResolver.findLiveAssignableField(null, InputMarker.class));
    }

    private static final class InputMarker {}

    private static class BaseFixture {
        private final InputMarker input = new InputMarker();

        final InputMarker expected() {
            return input;
        }
    }

    private static class WrappedFixture extends BaseFixture {
        @SuppressWarnings("unused")
        private String unrelated = "before-input";
    }

    private static final class NullDecoyFixture extends WrappedFixture {
        @SuppressWarnings("unused")
        private InputMarker decoy;
    }
}

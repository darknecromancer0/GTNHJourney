package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

public class FluidContainerStatePolicyTest {

    @Test
    public void anyPositiveObservedAmountTargetsFullCapacity() throws Exception {
        Method target = Class.forName("dev.gtnhjourney.minecraft.FluidContainerStatePolicy")
            .getDeclaredMethod("targetFullAmount", int.class, int.class);
        target.setAccessible(true);

        assertEquals(1000, ((Integer) target.invoke(null, 1, 1000)).intValue());
        assertEquals(8000, ((Integer) target.invoke(null, 500, 8000)).intValue());
        assertEquals(8000, ((Integer) target.invoke(null, 7999, 8000)).intValue());
        assertEquals(8000, ((Integer) target.invoke(null, 8000, 8000)).intValue());
    }

    @Test
    public void emptyOrInvalidCapacityDoesNotCreateFilledEndpoint() throws Exception {
        Method target = Class.forName("dev.gtnhjourney.minecraft.FluidContainerStatePolicy")
            .getDeclaredMethod("targetFullAmount", int.class, int.class);
        target.setAccessible(true);

        assertEquals(0, ((Integer) target.invoke(null, 0, 8000)).intValue());
        assertEquals(0, ((Integer) target.invoke(null, -1, 8000)).intValue());
        assertEquals(0, ((Integer) target.invoke(null, 1, 0)).intValue());
    }
}

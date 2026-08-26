package dev.gtnhjourney.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

public class Area16PlannerTest {

    @Test
    public void plansRadiusSixteenInEveryDirectionIncludingBothBounds() {
        List<Area16Planner.Position> positions = Area16Planner.plan(10, 20, 30);

        assertEquals(35937, positions.size());
        assertEquals(35937, new HashSet<Area16Planner.Position>(positions).size());
        assertTrue(positions.contains(new Area16Planner.Position(-6, 4, 14)));
        assertTrue(positions.contains(new Area16Planner.Position(26, 36, 46)));
    }
}

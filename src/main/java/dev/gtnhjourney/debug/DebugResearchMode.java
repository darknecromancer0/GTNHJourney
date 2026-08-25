package dev.gtnhjourney.debug;

/** Operating modes for the admin-only migration research tool. */
public enum DebugResearchMode {
    BLOCK,
    CONTENTS,
    AREA_16;

    public DebugResearchMode next() {
        switch (this) {
            case BLOCK:
                return CONTENTS;
            case CONTENTS:
                return AREA_16;
            case AREA_16:
            default:
                return BLOCK;
        }
    }
}

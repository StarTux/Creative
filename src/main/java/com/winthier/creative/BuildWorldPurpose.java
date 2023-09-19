package com.winthier.creative;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum BuildWorldPurpose {
    UNKNOWN("Unknown"),
    MINIGAME("Minigame"),
    BUILD_EVENT("Build Event"),
    PLOT("Plot"), // Legacy
    ;

    public final String displayName;
}

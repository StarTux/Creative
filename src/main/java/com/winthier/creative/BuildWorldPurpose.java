package com.winthier.creative;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum BuildWorldPurpose {
    UNKNOWN("Unknown"),
    MINIGAME("Minigame"),
    BUILD_EVENT("Build Event"),
    PLOT("Plot"), // Legacy
    SPAWN("Spawn"),
    LOBBY("Lobby"),
    EVENT("Event"),
    TEST("Test"),
    FESTIVAL("Festival"),
    ;

    public final String displayName;
}

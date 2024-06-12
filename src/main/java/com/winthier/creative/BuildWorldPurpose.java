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
    RAID("Raid"),
    ADVENTURE("Adventure"),
    SKYBLOCK("Skyblock"),
    WINDICATOR("Windicator"),
    RED_LIGHT_GREEN_LIGHT("Red Light Green Light"),
    MOB_ARENA("Mob Arena");
    ;

    public final String displayName;
}

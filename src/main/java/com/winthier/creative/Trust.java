package com.winthier.creative;

public enum Trust {
    OWNER,
    WORLD_EDIT,
    BUILD,
    VISIT,
    NONE,
    ;

    static Trust of(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException iae) {
            return null;
        }
    }

    boolean isOwner() {
        return this == OWNER;
    }

    boolean canBuild() {
        if (isOwner()) return true;
        switch (this) {
        case WORLD_EDIT:
        case BUILD:
            return true;
        default:
            return false;
        }
    }

    boolean canVisit() {
        if (canBuild()) return true;
        switch (this) {
        case VISIT:
            return true;
        default:
            return false;
        }
    }
}

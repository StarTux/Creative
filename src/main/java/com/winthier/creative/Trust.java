package com.winthier.creative;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Trust {
    OWNER(4, "Owner"),
    WORLD_EDIT(3, "WorldEdit"),
    BUILD(2, "Builder"),
    VISIT(1, "Visitor"),
    NONE(0, "None");

    public final int priority;
    private final String nice;

    static Trust of(String name) {
        if (name == null) return null;
        name = name.toUpperCase();
        try {
            return valueOf(name);
        } catch (IllegalArgumentException iae) { }
        for (Trust trust: values()) {
            if (trust.name().replace("_", "").equals(name)) return trust;
        }
        return NONE;
    }

    public boolean isOwner() {
        return this == OWNER;
    }

    public boolean canUseWorldEdit() {
        if (isOwner()) return true;
        switch (this) {
        case WORLD_EDIT:
            return true;
        default:
            return false;
        }
    }

    public boolean canBuild() {
        if (isOwner()) return true;
        switch (this) {
        case WORLD_EDIT:
        case BUILD:
            return true;
        default:
            return false;
        }
    }

    public boolean canVisit() {
        if (canBuild()) return true;
        switch (this) {
        case VISIT:
            return true;
        default:
            return false;
        }
    }

    public String nice() {
        return this.nice;
    }
}

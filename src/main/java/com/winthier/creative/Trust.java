package com.winthier.creative;

enum Trust {
    OWNER(4),
    WORLD_EDIT(3),
    BUILD(2),
    VISIT(1),
    NONE(0);

    final int priority;

    Trust(int priority) {
        this.priority = priority;
    }

    static Trust of(String name) {
        name = name.toUpperCase();
        try {
            return valueOf(name);
        } catch (IllegalArgumentException iae) { }
        for (Trust trust: values()) {
            if (trust.name().replace("_", "").equals(name)) return trust;
        }
        return NONE;
    }

    boolean isOwner() {
        return this == OWNER;
    }

    boolean canUseWorldEdit() {
        if (isOwner()) return true;
        switch (this) {
        case WORLD_EDIT:
            return true;
        default:
            return false;
        }
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

    String nice() {
        switch (this) {
        case OWNER: return "Owner";
        case WORLD_EDIT: return "WorldEdit";
        case BUILD: return "Builder";
        case VISIT: return "Visitor";
        case NONE: return "None";
        default: return "N/A";
        }
    }
}

package com.winthier.creative;

import java.util.List;
import java.util.ArrayList;

public class PlayerWorldList {
    final List<BuildWorld> owner = new ArrayList<>();
    final List<BuildWorld> build = new ArrayList<>();
    final List<BuildWorld> visit = new ArrayList<>();

    int count() {
        return owner.size() + build.size() + visit.size();
    }

    List<BuildWorld> all() {
        List<BuildWorld> result = new ArrayList<>();
        result.addAll(owner);
        result.addAll(build);
        result.addAll(visit);
        return result;
    }
}

package com.winthier.creative;

import java.util.HashMap;
import java.util.Map;
import lombok.Value;
import org.bukkit.configuration.ConfigurationSection;

@Value
public class Trusted {
    Builder builder;
    Trust trust;

    Map<String, Object> serialize() {
        Map<String, Object> result = new HashMap<>();
        result.put("builder", builder.serialize());
        result.put("trust", trust.name());
        return result;
    }

    public static Trusted deserialize(ConfigurationSection config) {
        Builder builder = Builder.deserialize(config.getConfigurationSection("builder"));
        Trust trust = Trust.of(config.getString("trust"));
        return new Trusted(builder, trust);
    }
}

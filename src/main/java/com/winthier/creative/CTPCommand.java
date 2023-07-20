package com.winthier.creative;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.connect.NetworkServer;

public final class CTPCommand extends AbstractCommand<CreativePlugin> {
    protected CTPCommand(final CreativePlugin plugin) {
        super(plugin, "ctp");
    }

    @Override
    protected void onEnable() {
        rootNode.arguments("<world>")
            .remoteServer(NetworkServer.CREATIVE)
            .description("Teleport to world")
            .completers(plugin.getCreativeCommand()::completeWorldNames)
            .remotePlayerCaller(plugin.getCreativeCommand()::worldTeleport);
    }
}

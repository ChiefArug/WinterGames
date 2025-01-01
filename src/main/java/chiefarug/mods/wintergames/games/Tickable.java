package chiefarug.mods.wintergames.games;

import net.minecraft.server.MinecraftServer;

public interface Tickable {
    void tick(MinecraftServer server);
}

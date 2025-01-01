package chiefarug.mods.wintergames;

import chiefarug.mods.wintergames.games.Tickable;
import net.minecraft.server.MinecraftServer;

public record SingleUseTargetTickable(Runnable ticker, int targetTick) implements Tickable {
    @Override
    public void tick(MinecraftServer server) {
        if (server.getTickCount() >= targetTick) {
            ticker.run();
            WinterGames.removeTicker(this);
        }
    }
}

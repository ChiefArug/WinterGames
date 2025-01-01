package chiefarug.mods.wintergames;

import chiefarug.mods.wintergames.games.Tickable;
import chiefarug.mods.wintergames.games.snowball.SnowballDefense;
import com.mojang.serialization.Codec;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static chiefarug.mods.wintergames.WinterGames.MODID;

@Mod(MODID)
@EventBusSubscriber(modid = MODID)
public class WinterGames {
    public static final String MODID = "wintergames";
    public static final ResourceLocation MODRL = ResourceLocation.fromNamespaceAndPath(MODID, MODID);

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MODID);
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> HIGH_SCORE = ATTACHMENT_TYPES.register("high_score",
            () -> AttachmentType
                    .builder(() -> 0)
                    .serialize(Codec.INT)
                    .copyOnDeath()
                    .build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ServerHighScore>> SERVER_HIGH_SCORE = ATTACHMENT_TYPES.register("server_high_score",
            () -> AttachmentType
                    .builder(() -> new ServerHighScore(Util.NIL_UUID, 0))
                    .serialize(ServerHighScore.CODEC)
                    .copyOnDeath()
                    .build());

    public WinterGames(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }

    private static final Set<Tickable> tickers = new HashSet<>(8);
    private static final List<Tickable> toRemove = new ArrayList<>(2);
    private static final List<Tickable> toAdd = new ArrayList<>(2);
    private static boolean tickerating = false;
    public static void addTicker(Tickable ticker) {
        if (tickerating)
            toAdd.add(ticker);
        else
            tickers.add(ticker);
    }
    public static void removeTicker(Tickable ticker) {
        if (tickerating)
            toRemove.add(ticker);
        else
            tickers.remove(ticker);
    }

    @SubscribeEvent
    private static void serverTick(ServerTickEvent.Pre event) {
        tickerating = true;
        for (Tickable tickable : tickers) {
            tickable.tick(event.getServer());
        }
        tickerating = false;
        tickers.addAll(toAdd);
        toAdd.clear();
        toRemove.forEach(tickers::remove); // https://stackoverflow.com/questions/28671903/the-hashsett-removeall-method-is-surprisingly-slow
        toRemove.clear();
    }

    @SubscribeEvent
    private static void rightClick(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide) return;

        if (event.getItemStack().getItem() == Items.STICK) {
            new SnowballDefense((ServerPlayer) event.getEntity()).open();
        }
    }

    @SubscribeEvent
    private static void entityRightClick(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide) return;

        if (event.getTarget().getType() == EntityType.SNOW_GOLEM) {
            new SnowballDefense(((ServerPlayer) event.getEntity())).open();
        }
    }

}

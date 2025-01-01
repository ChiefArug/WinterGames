package chiefarug.mods.wintergames.games.snowball;

import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.container.GooeyContainer;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.slot.TemplateSlotDelegate;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import ca.landonjw.gooeylibs2.api.template.types.InventoryTemplate;
import chiefarug.mods.wintergames.Buttons;
import chiefarug.mods.wintergames.ServerHighScore;
import chiefarug.mods.wintergames.WinterGames;
import chiefarug.mods.wintergames.games.Tickable;
import com.mojang.logging.LogUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static chiefarug.mods.wintergames.WinterGames.HIGH_SCORE;
import static chiefarug.mods.wintergames.WinterGames.SERVER_HIGH_SCORE;

public class SnowballDefense implements Tickable {

    private static final Logger LGGR = LogUtils.getLogger();
    private static final int cooldown = 20;
    private static final int speed = 5;
    private static final Component title = Component.translatableWithFallback("wintergames.snowball.title", "Snowball Defense");

    private final HealthBar enemyHealthBar;
    private final HealthBar playerHealthBar;
    private final GooeyContainer container;
    private final GooeyPage page;
    private final ServerPlayer player;
    private final List<Ball> balls = new ArrayList<>(18);
    private final GooeyButton playerBall;
    private final GooeyButton enemyBall;
    private final GooeyButton fire;
    private int enemyCooldown;
    private int score;
    private boolean over = false;

    class Ball {
        static int minY = 0;
        static int maxY = 7;
        int x, y, dir;
        boolean alive = true;
        boolean collected = false;
        GooeyButton button;
        Runnable onCollect;

        Ball(int x, int y, int dir, GooeyButton button, Runnable onCollect) {
            this.x = x;
            this.y = y;
            this.dir = dir;
            this.button = button;
            this.onCollect = onCollect;
        }
        void init() {
            TemplateSlotDelegate current = getSlot(x, y);
            if (fire == current.getButton().get())
                this.collected = true;
            current.setButton(button);
            balls.add(this);
        }

        boolean move() {
            if (!alive) return true;
            if (collected) {
                collected = false;
                collectBonus();
                delete();
                return true;
            }

            int newY = y + dir;
            if (newY <= minY) {
                enemyHealthBar.hurt();
                delete();
                return true;
            } else if (newY >= maxY) {
                playerHealthBar.hurt();
                delete();
                return true;
            }
            TemplateSlotDelegate targetSlot = getSlot(x, newY);
            Button target = targetSlot.getButton().get();

            if (playerBall == target || enemyBall == target) {
                explode();
                balls.stream().filter(ball -> ball.x == x && ball.y == newY).forEach(Ball::delete);
                playSound(SoundEvents.DRAGON_FIREBALL_EXPLODE, 0.75f);
                score(1);
                return true;
            }

            if (fire == target) {
                targetSlot.setButton(Buttons.EMPTY);
                collectBonus();
                delete();
                return true;
            }

            getSlot(x, y).setButton(Buttons.EMPTY);
            y = newY;
            getSlot(x, newY).setButton(button);
            return false;
        }

        void collectBonus() {
            onCollect.run();
            if (button == playerBall) {
                score(2);
                playSound(SoundEvents.PLAYER_LEVELUP, 1);
            } else {
                score(-2);
                playSound(SoundEvents.BAT_DEATH, 1);
            }
        }

        void delete() {
            getSlot(x, y).setButton(Buttons.EMPTY);
            this.alive = false;
        }

        void explode() {
            getSlot(x, y).setButton(fire);
        }

    }
    public SnowballDefense(ServerPlayer player) {
        this.playerHealthBar = new HealthBar.Player(this);
        this.enemyHealthBar = new HealthBar(this);
        this.container = createContainer(this.player = player, this.page = createPage());


        ItemStack display = Items.SNOWBALL.getDefaultInstance();
        display.set(DataComponents.ITEM_NAME, Component.literal("ball"));
        display.set(DataComponents.HIDE_TOOLTIP, Unit.INSTANCE);
        enemyBall = GooeyButton.of(display);

        display = Items.FIRE_CHARGE.getDefaultInstance();
        display.set(DataComponents.ITEM_NAME, Component.literal("ball"));
        display.set(DataComponents.HIDE_TOOLTIP, Unit.INSTANCE);
        playerBall = GooeyButton.of(display);

        display = Items.ORANGE_DYE.getDefaultInstance();
        display.set(DataComponents.ITEM_NAME, Component.literal("fire"));
        display.set(DataComponents.HIDE_TOOLTIP, Unit.INSTANCE);
        fire = GooeyButton.of(display);
    }

    private GooeyContainer createContainer(ServerPlayer player, GooeyPage page) {
        return new GooeyContainer(player, page);
    }

    private GooeyPage createPage() {
        return new GooeyPage(
                createChestTemplate(),
                createInvTemplate(),
                title,
                _pa -> WinterGames.addTicker(this),
                _pa -> {WinterGames.removeTicker(this); this.onClose(); } // we DO NOT remove it here, as that would cause a CME as we close ourselves whilst being iterated
        );
    }

    private ChestTemplate createChestTemplate() {
        ChestTemplate.Builder builder = ChestTemplate.builder(4)
                .fill(Buttons.EMPTY);
        enemyHealthBar.fill(builder);

        return builder.build();
    }

    private InventoryTemplate createInvTemplate() {
        InventoryTemplate.Builder builder = InventoryTemplate.builder()
                .fill(Buttons.EMPTY);
        this.playerHealthBar.fill(builder);

        return builder.build();
    }

    public void open() {
        try {
            this.container.open();
        } catch (Exception e) {
            LGGR.error("Failed to open SnowballDefense", e);
        }
    }

    @Override
    public void tick(MinecraftServer server) {
        if (server.getTickCount() % speed != 0) return;
        try {
            balls.removeIf(Ball::move);
        } catch (Exception e) {
            LGGR.error("Failed to tick SnowballDefense", e);
        }
        try {
            int r = server.overworld().random.nextInt(36);
            if (r < 9)
                enemyFireFrom(r);
        } catch (Exception e) {
            LGGR.error("Failed to fire enemy ball", e);
        }

        if (playerHealthBar.isDead()) {
            if (enemyHealthBar.isDead())
                draw();
            else
                lose();
        } else if (enemyHealthBar.isDead()) {
            win();
        }
    }

    public TemplateSlotDelegate getSlot(int x, int y) {
        if (y < 4) {
            return page.getTemplate().getSlot(y * 9 + x);
        } else {
            return page.getInventoryTemplate().get().getSlot((y - 4) * 9 + x);
        }
    }

    public void score(int amount) {
        score += amount;
        page.setTitle(title.plainCopy().append(" ").append(Component.translatableWithFallback("wintergames.snowball.score", "Score: %s", score)));
    }

    public void playerFireFrom(int column) {
        if (player.getCooldowns().isOnCooldown(Items.GREEN_STAINED_GLASS_PANE)) return;

        player.sendSystemMessage(Component.literal("u fired from" + column));
        new Ball(column, 6, -1, playerBall, this::resetPlayerCooldowns).init();
        player.getCooldowns().addCooldown(Items.GREEN_STAINED_GLASS_PANE, cooldown);
        player.getCooldowns().addCooldown(Items.RED_STAINED_GLASS_PANE, cooldown);

        playSound(SoundEvents.SNOWBALL_THROW,1.1f);
    }

    private void resetPlayerCooldowns() {
        player.getCooldowns().removeCooldown(Items.GREEN_STAINED_GLASS_PANE);
        player.getCooldowns().removeCooldown(Items.RED_STAINED_GLASS_PANE);
    }

    public void enemyFireFrom(int column) {
        if (player.server.getTickCount() < enemyCooldown) return;

        player.sendSystemMessage(Component.literal("e fired from" + column));
        new Ball(column, 1, 1, enemyBall, () -> enemyCooldown = 0).init();
        enemyCooldown = player.server.getTickCount() + cooldown;

        playSound(SoundEvents.SNOW_GOLEM_SHOOT, 1.2f);
    }

    void playSound(SoundEvent sound, float volume) {
        player.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), SoundSource.PLAYERS, player.getX(), player.getY(), player.getZ(), volume, 1.0f, player.serverLevel().random.nextLong()));
    }

    public void win() {
        over = true;
        player.closeContainer();
        player.connection.send(new ClientboundSetTitleTextPacket(Component.translatableWithFallback("wintergames.snowball.win", "You won!")));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatableWithFallback("wintergames.snowball.win_subtitle", "Final score: %s", score)));
    }

    public void lose() {
        over = true;
        score = 0; // sorry, you lost.
        player.closeContainer();
        player.connection.send(new ClientboundSetTitleTextPacket(Component.translatableWithFallback("wintergames.snowball.lose", "You lost!")));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatableWithFallback("wintergames.snowball.lose_subtitle", "Better luck next time")));
    }

    public void draw() {
        over = true;
        player.closeContainer();
        player.connection.send(new ClientboundSetTitleTextPacket(Component.translatableWithFallback("wintergames.snowball.draw", "You drew!")));
    }

    public void onClose() {
        if (!over) return;

        int serverMax = player.server.overworld().getData(SERVER_HIGH_SCORE).score();

        if (score > serverMax) {
            player.setData(HIGH_SCORE, score);
            player.server.overworld().setData(SERVER_HIGH_SCORE, new ServerHighScore(player.getUUID(), score));
            player.sendSystemMessage(Component.translatableWithFallback("wintergames.snowball.server_high_score", "New server high score: %s!", score));
        } else {
            int playerMax = player.getData(HIGH_SCORE);
            if (score > playerMax) {
                player.setData(HIGH_SCORE, score);
                player.sendSystemMessage(Component.translatableWithFallback("wintergames.snowball.player_high_score", "New personal high score: %s!", score));
            }
        }
    }
}

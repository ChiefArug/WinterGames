package chiefarug.mods.wintergames.games.snowball;

import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import ca.landonjw.gooeylibs2.api.template.types.InventoryTemplate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Range;

public class HealthBar {
    private static final ItemStack red, green;
    static {
        red = Items.PINK_STAINED_GLASS_PANE.getDefaultInstance();
        red.set(DataComponents.HIDE_TOOLTIP, Unit.INSTANCE);
        green = Items.LIME_STAINED_GLASS_PANE.getDefaultInstance();
        green.set(DataComponents.HIDE_TOOLTIP, Unit.INSTANCE);
    }

    protected final SnowballDefense snowballDefense;
    int health = 9;
    final GooeyButton[] buttons = new GooeyButton[]{
            button(0),
            button(1),
            button(2),
            button(3),
            button(4),
            button(5),
            button(6),
            button(7),
            button(8)
    };

    public HealthBar(SnowballDefense snowballDefense) {
        this.snowballDefense = snowballDefense;
    }

    protected GooeyButton button(int n) {
        return GooeyButton.builder().display(getGreen()).build();
    }

    public void setHealth(@Range(from = 0, to = 9) int health) {
        this.health = health;
        for (int i = 0; i < health; i++) {
            buttons[i].setDisplay(getGreen());
            buttons[i].update();
        }
        for (int i = health; i < 9; i++) {
            buttons[i].setDisplay(getRed());
            buttons[i].update();
        }
    }

    public void fill(InventoryTemplate.Builder builder) {
        for (int i = 0; i < 9; i++)
            builder.set(27 + i, buttons[i]);
    }

    public void fill(ChestTemplate.Builder builder) {
        for (int i = 0; i < 9; i++)
            builder.set(i, buttons[i]);
    }

    public void hurt() {
        if (isDead()) return;
        buttons[--this.health].setDisplay(getRed());
        buttons[this.health].update();
        snowballDefense.playSound(hurtSound(), 1.1f);
    }

    public boolean isDead() {
        return health <= 0;
    }

    protected SoundEvent hurtSound() {
        return SoundEvents.SNOW_GOLEM_HURT;
    }

    protected ItemStack getGreen() {
        return green;
    }

    protected ItemStack getRed() {
        return red;
    }

    @Override
    public String toString() {
        return "HealthBar[health=" + health + "]";
    }

    public static class Player extends HealthBar {
        private static final ItemStack green, red;
        static {
            green = Items.GREEN_STAINED_GLASS_PANE.getDefaultInstance();
            green.set(DataComponents.HIDE_TOOLTIP, Unit.INSTANCE);
            red = Items.RED_STAINED_GLASS_PANE.getDefaultInstance();
            red.set(DataComponents.HIDE_TOOLTIP, Unit.INSTANCE);
        }

        public Player(SnowballDefense snowballDefense) {
            super(snowballDefense);
        }

        @Override
        protected GooeyButton button(int n) {
            return GooeyButton.builder().display(getGreen()).onClick(() -> clicked(n)).build();
        }

        private void clicked(int number) {
            snowballDefense.playerFireFrom(number);
        }

        @Override
        protected SoundEvent hurtSound() {
            return SoundEvents.PLAYER_HURT;
        }

        @Override
        protected ItemStack getGreen() {
            return green;
        }

        @Override
        protected ItemStack getRed() {
            return red;
        }
    }
}

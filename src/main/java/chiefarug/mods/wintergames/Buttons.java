package chiefarug.mods.wintergames;

import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class Buttons {
    static {
        ItemStack emptyIcon = Items.LIGHT_GRAY_STAINED_GLASS_PANE.getDefaultInstance();
        emptyIcon.set(DataComponents.HIDE_TOOLTIP, Unit.INSTANCE);
        EMPTY = GooeyButton.builder().display(emptyIcon).build();
    }
    public static final GooeyButton EMPTY;
}

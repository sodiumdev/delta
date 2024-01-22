package zip.sodium.delta.api.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import zip.sodium.delta.api.interfaces.DeltaItem;

public class DeltaBlockItem extends BlockItem implements DeltaItem {
    private final Item virtualItem;

    public DeltaBlockItem(final Item virtualItem, final Block block, final Properties settings) {
        super(block, settings);

        this.virtualItem = virtualItem;
    }

    @Override
    public Item getDelta(@NotNull ItemStack stack, @Nullable ServerPlayer player) {
        return virtualItem;
    }
}

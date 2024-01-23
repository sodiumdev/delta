package zip.sodium.delta.api.interfaces;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

public interface DeltaItem extends DeltaObject<Item, ItemStack> {
    String DELTA_ITEM_ID = "DeltaItemId";
    String DELTA_ITEM_REAL_NBT_ID = "ActualData";

    @ApiStatus.NonExtendable
    default Registry<Item> getRegistry() {
        return BuiltInRegistries.ITEM;
    }

    default Item getReplacement(@Nullable ServerPlayer player) {
        return getDelta(((Item) this).getDefaultInstance(), player);
    }

    default Integer getCustomModelData() {
        return null;
    }
}

package zip.sodium.delta.packet;

import io.netty.buffer.ByteBuf;
import net.kyori.adventure.translation.GlobalTranslator;
import net.minecraft.core.IdMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import zip.sodium.delta.helper.BlockHelper;
import zip.sodium.delta.api.interfaces.DeltaItem;
import zip.sodium.delta.api.interfaces.DeltaObject;

import java.util.Locale;

public class DeltaByteBuf extends FriendlyByteBuf {
    private final @NotNull ServerPlayer player;

    public DeltaByteBuf(final @NotNull ServerPlayer player, final ByteBuf parent) {
        super(parent);

        this.player = player;
    }

    public static ItemStack getDeltaItem(final ItemStack stack, final @Nullable ServerPlayer player) {
        final var nbt = new CompoundTag();

        nbt.putString(DeltaItem.DELTA_ITEM_ID, BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        nbt.putInt("HideFlags", 255);

        final var item = (DeltaItem) stack.getItem();
        if (item.getCustomModelData() != null)
            nbt.putInt("CustomModelData", item.getCustomModelData());

        nbt.put(DeltaItem.DELTA_ITEM_REAL_NBT_ID, stack.getOrCreateTag());

        final var translatable = stack.getItem().getDescriptionId();

        stack.setTag(nbt);
        stack.setHoverName(
                Component.translatableWithFallback(
                        translatable,
                        GlobalTranslator.translator().translate(translatable, player == null ? Locale.US : player.adventure$locale).toPattern()
                ).withStyle(style -> style.withItalic(false))
        );

        return stack;
    }

    @Override
    public <T> void writeId(IdMap<T> registry, @NotNull T value) {
        if (value instanceof DeltaObject<?, ?> deltaObject)
            value = (T) deltaObject.getReplacement(this.player);

        if (registry == Block.BLOCK_STATE_REGISTRY && value instanceof BlockState state)
            value = (T) BlockHelper.getReplacementBlockState(state, player);

        super.writeId(registry, value);
    }

    @Override
    public @NotNull DeltaByteBuf writeItem(@NotNull ItemStack stack) {
        if (stack.getItem() instanceof DeltaItem)
            getDeltaItem(stack, player);

        return (DeltaByteBuf) super.writeItem(stack);
    }

    @Override
    public @NotNull ItemStack readItem() {
        if (!this.readBoolean())
            return ItemStack.EMPTY;

        var item = (Item) this.readById((IdMap<?>) BuiltInRegistries.ITEM);

        final int count = this.readByte();
        var nbt = this.readNbt();

        if (nbt != null && nbt.contains(DeltaItem.DELTA_ITEM_ID)) {
            item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(nbt.getString(DeltaItem.DELTA_ITEM_ID)));

            nbt = nbt.getCompound(DeltaItem.DELTA_ITEM_REAL_NBT_ID);
        }

        final var itemstack = new ItemStack(item, count);
        itemstack.setTag(nbt);

        return itemstack;
    }
}

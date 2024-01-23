package zip.sodium.delta.api.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import zip.sodium.delta.Entrypoint;
import zip.sodium.delta.api.DeltaPlugin;
import zip.sodium.delta.api.interfaces.DeltaBlock;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class TexturedDeltaBlockEntity extends BlockEntity {
    public TexturedDeltaBlockEntity(BlockPos pos, BlockState state) {
        super(DeltaPlugin.getTexturedDeltaBlockEntity(), pos, state);
    }

    public List<String> ids;

    @Override
    protected void saveAdditional(final @NotNull CompoundTag nbt) {
        super.saveAdditional(nbt);

        nbt.put("ids", new ListTag(ids.stream().<Tag>map(StringTag::valueOf).toList(), (byte) 8));
    }

    @Override
    public void load(final @NotNull CompoundTag nbt) {
        super.load(nbt);

        ids = nbt.getList("ids", 8).stream().map(Tag::getAsString).toList();
    }

    @Override
    public void setRemoved() {
        ids.forEach(id -> {
            final var entity = ((ServerLevel) level).getEntity(UUID.fromString(id));
            if (entity != null)
                entity.discard();
        });

        super.setRemoved();
    }
}

package zip.sodium.delta.api.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface DeltaBlock extends DeltaObject<Block, BlockState> {
    default BlockState getDeltaBlockState(@NotNull BlockState state, @Nullable ServerPlayer player) {
        return getDelta(state, player).defaultBlockState();
    }

    @ApiStatus.NonExtendable
    static <T extends BlockEntity> T getBlockEntityAt(final Level world, final BlockPos pos) {
        return (T) world.getBlockEntity(pos);
    }

    @ApiStatus.NonExtendable
    default Registry<Block> getRegistry() {
        return BuiltInRegistries.BLOCK;
    }

    default Block getReplacement(final @Nullable ServerPlayer player) {
        return getDelta(cast().defaultBlockState(), player);
    }

    default boolean handlesMiningServerSide(final @Nullable ServerPlayer player, final @NotNull BlockState state, final @NotNull BlockPos pos) {
        return true;
    }

    default BlockState getParticleReplacement(final @Nullable ServerPlayer player, final @NotNull BlockState state, final @NotNull BlockPos pos) {
        return getDeltaBlockState(state, player);
    }
}

package zip.sodium.delta.builtin.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import zip.sodium.delta.api.block.TexturedDeltaBlock;

public final class BallsaltBlock extends TexturedDeltaBlock {
    public BallsaltBlock(Properties settings) {
        super(settings);
    }

    @Override
    public BlockState getParticleReplacement(@Nullable ServerPlayer player, @NotNull BlockState state, @NotNull BlockPos pos) {
        return Blocks.RED_MUSHROOM_BLOCK.defaultBlockState();
    }
}

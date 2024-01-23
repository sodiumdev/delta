package zip.sodium.delta.helper;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import zip.sodium.delta.api.interfaces.DeltaBlock;

public final class BlockHelper {
    private BlockHelper() {}

    public static BlockState getReplacementBlockState(final BlockState state, final ServerPlayer player) {
        if (state.getBlock() instanceof DeltaBlock deltaBlock)
            return deltaBlock.getDeltaBlockState(state, player);

        return state;
    }

    public static BlockState getReplacementParticleBlockState(final @Nullable ServerPlayer player, final @NotNull BlockState state, final @NotNull BlockPos pos) {
        if (state.getBlock() instanceof DeltaBlock deltaBlock)
            return deltaBlock.getParticleReplacement(player, state, pos);

        return state;
    }

    public static Block getBlockType(final Level level, final org.bukkit.block.Block block) {
        return level.getBlockState(getNMSPosition(block.getLocation())).getBlock();
    }

    public static BlockPos getNMSPosition(final Location location) {
        return new BlockPos(
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );
    }

    public static boolean handlesMiningServerSide(ServerPlayer player, BlockState state, BlockPos pos) {
        return state.getBlock() instanceof DeltaBlock deltaBlock
                && deltaBlock.handlesMiningServerSide(player, state, pos);
    }
}
